package cc.game.dominos.android;

import android.os.Bundle;
import android.os.Environment;

import java.io.File;

import cc.game.dominos.core.Dominos;
import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;

/**
 * Created by chriscaron on 2/15/18.
 */

public class DominosActivity extends DroidActivity {

    final Dominos dominos = new Dominos() {
        @Override
        public void redraw() {
            getContent().postInvalidate();
        }
    };

    private File saveFile=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        saveFile = new File(getFilesDir(), "dominos.save");
        try {
            dominos.loadFromFile(saveFile);
        } catch (Exception e) {
            dominos.setNumPlayers(4);
            dominos.startNewGame(6, 150);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        dominos.startGameThread();
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
        dominos.draw(g,tx, ty);
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
        tx = Math.round(x);
        ty = Math.round(y);
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
}
