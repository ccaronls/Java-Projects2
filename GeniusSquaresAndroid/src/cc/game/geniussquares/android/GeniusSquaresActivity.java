package cc.game.geniussquares.android;


import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;

import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;
import cc.lib.geniussqaure.UIGeniusSquares;

/**
 * Created by chriscaron on 2/15/18.
 */

public class GeniusSquaresActivity extends DroidActivity {

    private final static String TAG = GeniusSquaresActivity.class.getSimpleName();

    final UIGeniusSquares gs = new UIGeniusSquares() {
        @Override
        public void repaint() {
            getContent().postInvalidate();
        }
    };

    File saveFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View topBar = View.inflate(this, R.layout.menu_bar, null);
        getTopBar().addView(topBar);
        findViewById(R.id.buttonMenu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String [] options = {
                        "New Game",
                        "Reset Pieces",
                };
                gs.pauseTimer();
                newDialogBuilder().setTitle("Options").setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                gs.newGame();
                                break;
                            case 1:
                                gs.resetPieces();
                        }
                        gs.resumeTimer();
                    }
                }).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        saveFile = new File(getFilesDir(), "gs.save");
        if (!gs.tryLoadFromFile(saveFile))
            gs.newGame();
        gs.resumeTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gs.pauseTimer();
        gs.trySaveToFile(saveFile);
    }

    int tx=-1, ty=-1;
    boolean dragging = false;

    @Override
    protected void onDraw(DroidGraphics g) {
        g.getPaint().setAntiAlias(false);
        gs.paint(g, tx, ty);
        getContent().postInvalidateDelayed(500);
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
            gs.stopDrag();
            dragging = false;
        }
        tx = -1;//Math.round(x);
        ty = -1;//Math.round(y);
        getContent().postInvalidate();
    }

    @Override
    protected void onDrag(float x, float y) {
        if (!dragging) {
            gs.startDrag();
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
                tx = ty = -1;
                gs.doClick();
            }
        }, 100);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getTitle().toString()) {
            case "New Game":
                gs.newGame();
                break;
            default:
                gs.resetPieces();
        }
        getContent().postInvalidate();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Options");
        menu.addSubMenu("New Game");
        menu.addSubMenu("Reset Pieces");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }
}
