package cc.lib.android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.SystemClock;
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

public abstract class DroidActivity extends CCActivityBase {

    DroidGraphics g = null;
    View content = null;

    private class DelayedTouchDown implements Runnable {

        final float x, y;
        DelayedTouchDown(MotionEvent ev) {
            this.x = ev.getX();
            this.y = ev.getY();
        }
        public void run() {
            onTouchDown(x, y);
            touchDownRunnable = null;
        }
    }

    private final int CLICK_TIME = 700;

    private long downTime = 0;

    private Runnable touchDownRunnable = null;

    private int margin = 0;
    public void setMargin(int margin) {
        this.margin = margin;
        //FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)content.getLayoutParams();
        //lp.setMargins(margin, margin, margin, margin);
        //content.setLayoutParams(lp);
        content.postInvalidate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        content = new View(this) {
            @Override
            protected void onDraw(Canvas canvas) {
                int width = canvas.getWidth()-margin*2;
                int height = canvas.getHeight()-margin*2;
                if (g == null) {
                    g = new DroidGraphics(getContext(), canvas, width, height);
                } else {
                    g.setCanvas(canvas, width, height);
                }
                canvas.save();
                canvas.translate(margin, margin);
                canvas.save();
                DroidActivity.this.onDraw(g);
                canvas.restore();
                canvas.restore();
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downTime = SystemClock.uptimeMillis();
                        onTouchDown(event.getX(), event.getY());
                        postDelayed(touchDownRunnable=new DelayedTouchDown(event), CLICK_TIME);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (SystemClock.uptimeMillis() - downTime < CLICK_TIME) {
                            removeCallbacks(touchDownRunnable);
                            touchDownRunnable = null;
                            onTap(event.getX(), event.getY());
                        } else {
                            onTouchUp(event.getX(), event.getY());
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        //if (touchDownRunnable != null) {
                        //    removeCallbacks(touchDownRunnable);
                        //    touchDownRunnable = null;
                        //    onTouchDown(event.getX(), event.getY());
                        if (touchDownRunnable == null) {
                            onDrag(event.getX(), event.getY());
                        }
                        break;
                }

                return true;
            }

        };
        setContentView(content);
    }

    public View getContent() {
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

    protected void onTap(float x, float y) {}

    protected void onTouchDown(float x, float y) {}

    protected void onTouchUp(float x, float y) {}

    protected void onDrag(float x, float y) {}

    private AlertDialog currentDialog = null;

    protected int getDialogTheme() {
        return R.style.DialogTheme;
    }

    public final AlertDialog.Builder newDialogBuilder() {
        final AlertDialog previous = currentDialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this, getDialogTheme()) {
            @Override
            public AlertDialog show() {
                if (currentDialog != null) {
                    currentDialog.dismiss();
                }
                return currentDialog = super.show();
            }
        }.setCancelable(false);
        if (currentDialog != null && currentDialog.isShowing()) {
            builder.setNeutralButton(R.string.popup_button_back, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    currentDialog = previous;
                    previous.show();
                }
            });
        }
        return builder;
    }

    public void dismissCurrentDialog() {
        if (currentDialog != null) {
            currentDialog.dismiss();
            currentDialog = null;
        }
    }

    public boolean isCurrentDialogShowing() {
        return currentDialog != null && currentDialog.isShowing();
    }
}
