package cc.lib.android;

import android.app.Activity;
import android.graphics.Canvas;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View v = new View(this) {
            @Override
            protected void onDraw(Canvas canvas) {
                if (g == null) {
                    g = new DroidGraphics(canvas);
                }
                DroidActivity.this.onDraw(g);
            }
        };
        setContentView(v);
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
}
