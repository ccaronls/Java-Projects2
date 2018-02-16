package cc.lib.android;

import android.app.Activity;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by chriscaron on 2/13/18.
 *
 * Convenience class for getting fullscreen game up without a layout file.
 *
 * Just override the draw method
 *
 */

public abstract class DroidActivity extends Activity {

    DroidGraphics g = null;
    View content = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        content = new View(this) {
            @Override
            protected void onDraw(Canvas canvas) {
                if (g == null) {
                    g = new DroidGraphics(canvas);
                } else {
                    g.setCanvas(canvas);
                }
                DroidActivity.this.onDraw(g);
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        onTouchDown(event.getX(), event.getY());
                        break;
                    case MotionEvent.ACTION_UP:
                        onTouchUp(event.getX(), event.getY());
                        break;
                    case MotionEvent.ACTION_MOVE:
                        onDrag(event.getX(), event.getY());
                        break;
                }

                return true;
            }
        };
        setContentView(content);
    }

    protected View getContent() {
        return content;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        g.shutDown();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    protected abstract void onDraw(DroidGraphics g);

    protected void onTouchDown(float x, float y) {}

    protected void onTouchUp(float x, float y) {}

    protected void onDrag(float x, float y) {}
}
