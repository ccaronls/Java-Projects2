package cc.game.dominos.android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;

import cc.game.dominos.core.Dominos;
import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;
import cc.lib.utils.FileUtils;

/**
 * Created by chriscaron on 2/15/18.
 */

public class DominosActivity extends DroidActivity {

    Dominos dominos = null;

    private File saveFile=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        saveFile = new File(getFilesDir(), "dominos.save");
        dominos = new Dominos() {
            @Override
            public void redraw() {
                getContent().postInvalidate();
            }

            @Override
            protected void onGameOver() {
                getContent().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showNewGameDialog(false);
                    }
                }, 5000);
            }

            @Override
            protected void onNewGameClicked() {
                showNewGameDialog(true);
            }
        };

        try {
            dominos.loadFromFile(saveFile);
        } catch (FileNotFoundException e) {
            // ignore
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void copyFileToExt() {
        try {
            FileUtils.copyFile(saveFile, Environment.getExternalStorageDirectory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dominos.getNumPlayers() > 0 && dominos.getWinner() == null)
            dominos.startGameThread();
        else if (!dominos.isGameRunning())
            showNewGameDialog(false);

        if (saveFile.exists()) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (checkPermission()) {
                    copyFileToExt();
                } else {
                    requestPermission();
                }
            }
        }

    }

    final int PERMISSION_REQUEST_CODE = 1001;

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "Write External Storage permission allows us to do store images. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    copyFileToExt();
                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        dominos.stopGameThread();
        dominos.trySaveToFile(saveFile);
    }

    int tx, ty;
    boolean dragging = false;

    @Override
    protected void onDraw(DroidGraphics g) {
        synchronized (this) {
            dominos.draw(g, tx, ty);
        }
    }

    @Override
    protected void onTouchDown(float x, float y) {
        tx = Math.round(x);
        ty = Math.round(y);
        getContent().postInvalidate();
    }

    @Override
    protected void onTouchUp(float x, float y) {
        if (dragging) {
            dominos.stopDrag();
            dragging = false;
        }
        tx = -1;//Math.round(x);
        ty = -1;//Math.round(y);
        getContent().postInvalidate();
    }

    @Override
    protected void onDrag(float x, float y) {
        if (!dragging) {
            dominos.startDrag();
            dragging = true;
        }
        tx = Math.round(x);
        ty = Math.round(y);
        getContent().postInvalidate();
    }

    @Override
    protected void onTap(float x, float y) {
        tx = Math.round(x);
        ty = Math.round(y);
        getContent().postInvalidate();
        getContent().postDelayed(new Runnable() {
            public void run() {
                dominos.onClick();
            }
        }, 100);
    }

    private void showNewGameDialog(boolean cancleable) {
        final View v = View.inflate(this, R.layout.new_game_dialog, null);
        final RadioGroup rgNumPlayers = (RadioGroup)v.findViewById(R.id.rgNumPlayers);
        final RadioGroup rgDifficulty = (RadioGroup)v.findViewById(R.id.rgDifficulty);
        final RadioGroup rgTiles      = (RadioGroup)v.findViewById(R.id.rgTiles);
        final RadioGroup rgMaxPoints  = (RadioGroup)v.findViewById(R.id.rgMaxPoints);
        switch (dominos.getNumPlayers()) {
            case 2:
                rgNumPlayers.check(R.id.rbPlayersTwo); break;
            case 3:
                rgNumPlayers.check(R.id.rbPlayersThree); break;
            case 4:
                rgNumPlayers.check(R.id.rbPlayersFour); break;
        }
        switch (dominos.getDifficulty()) {
            case 0:
                rgDifficulty.check(R.id.rbDifficultyEasy); break;
            case 1:
                rgDifficulty.check(R.id.rbDifficultyMedium); break;
            case 2:
                rgDifficulty.check(R.id.rbDifficultyHard); break;
        }
        switch (dominos.getMaxPips()) {
            case 6:
                rgTiles.check(R.id.rbTiles6x6); break;
            case 9:
                rgTiles.check(R.id.rbTiles9x9); break;
            case 12:
                rgTiles.check(R.id.rbTiles12x12); break;
        }
        switch (dominos.getMaxScore()) {
            case 150:
                rgMaxPoints.check(R.id.rbMaxPoints150); break;
            case 200:
                rgMaxPoints.check(R.id.rbMaxPoints200); break;
            case 250:
                rgMaxPoints.check(R.id.rbMaxPoints250); break;
        }
        new AlertDialog.Builder(this).setTitle("New Game")
                .setView(v)
                .setPositiveButton("Start", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        int difficulty = 0;
                        int numPlayers = 4;
                        int maxPoints = 150;
                        int maxPips = 9;

                        switch (rgDifficulty.getCheckedRadioButtonId()) {
                            case R.id.rbDifficultyEasy:
                                difficulty = 0; break;
                            case R.id.rbDifficultyMedium:
                                difficulty = 1; break;
                            case R.id.rbDifficultyHard:
                                difficulty = 2; break;
                        }

                        switch (rgNumPlayers.getCheckedRadioButtonId()) {
                            case R.id.rbPlayersTwo:
                                numPlayers = 2; break;
                            case R.id.rbPlayersThree:
                                numPlayers = 3; break;
                            case R.id.rbPlayersFour:
                                numPlayers = 4; break;
                        }

                        switch (rgMaxPoints.getCheckedRadioButtonId()) {
                            case R.id.rbMaxPoints150:
                                maxPoints = 150; break;
                            case R.id.rbMaxPoints200:
                                maxPoints = 200; break;
                            case R.id.rbMaxPoints250:
                                maxPoints = 250; break;
                        }

                        switch (rgTiles.getCheckedRadioButtonId()) {
                            case R.id.rbTiles6x6:
                                maxPips = 6; break;
                            case R.id.rbTiles9x9:
                                maxPips = 9; break;
                            case R.id.rbTiles12x12:
                                maxPips = 12; break;
                        }

                        dominos.startNewGame(numPlayers, maxPips, maxPoints, difficulty);
                        dominos.startGameThread();

                    }


                }).setCancelable(cancleable).show();
    }


}
