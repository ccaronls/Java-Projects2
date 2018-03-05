package cc.game.soc.android;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;

import cc.game.soc.ui.MenuItem;
import cc.game.soc.ui.UIBarbarianRenderer;
import cc.game.soc.ui.UIBoardRenderer;
import cc.game.soc.ui.UIDiceRenderer;
import cc.game.soc.ui.UIEventCardRenderer;
import cc.game.soc.ui.UIPlayerRenderer;
import cc.game.soc.ui.UIProperties;
import cc.game.soc.ui.UISOC;

/**
 * Created by chriscaron on 2/15/18.
 */

public class SOCActivity extends Activity {

    UISOC soc = null;
    File saveFile;
    View content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        content = View.inflate(this, R.layout.soc_activity, null);
        setContentView(content);
        SOCView<UIBarbarianRenderer> barbarian = (SOCView)findViewById(R.id.soc_barbarian);
        SOCView<UIEventCardRenderer> event     = (SOCView)findViewById(R.id.soc_event_cards);
        SOCView<UIBoardRenderer> board     = (SOCView)findViewById(R.id.soc_board);
        SOCView<UIDiceRenderer> dice      = (SOCView)findViewById(R.id.soc_dice);
        SOCView<UIPlayerRenderer> user      = (SOCView)findViewById(R.id.soc_user);
        SOCView<UIPlayerRenderer> plTop     = (SOCView)findViewById(R.id.soc_player_top);
        SOCView<UIPlayerRenderer> plMiddle  = (SOCView)findViewById(R.id.soc_player_middle);
        SOCView<UIPlayerRenderer> plBottom  = (SOCView)findViewById(R.id.soc_player_bottom);
        ListView lvMenu   = (ListView)findViewById(R.id.soc_menu_list);


        UIProperties properties = new UIProperties();
        //    protected UISOC(UIProperties properties, UIBoardRenderer boardRenderer, UIDiceRenderer diceRenderer, UIConsoleRenderer console,
        // UIEventCardRenderer eventCardRenderer) {

        soc = new UISOC(properties, board.renderer, dice.renderer, null, event.renderer) {
            @Override
            protected void addMenuItem(MenuItem item, String title, String helpText, Object extra) {

            }

            @Override
            public void clearMenu() {

            }

            @Override
            public void redraw() {
                content.postInvalidate();
            }
        };

//        soc.setBoard(board);

        /*
        saveFile = new File(getFilesDir(), "soc.save");
        soc = new soc() {
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
            soc.loadFromFile(saveFile);
        } catch (FileNotFoundException e) {
            // ignore
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    void copyFileToExt() {
        try {
//            FileUtils.copyFile(saveFile, Environment.getExternalStorageDirectory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
        if (soc.getNumPlayers() > 0 && soc.getWinner() == null)
            soc.startGameThread();
        else if (!soc.isGameRunning())
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
*/
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
        soc.stopRunning();
        soc.trySaveToFile(saveFile);
    }

}
