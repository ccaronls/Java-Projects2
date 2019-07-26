package cc.lib.android;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

public abstract class DroidView extends View {

    public DroidView(Context context) {
        super(context);
    }

    private DroidGraphics g = null;

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
        onPaint(g);
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
                if (touchDownRunnable == null) {
                    onDrag(event.getX(), event.getY());
                }
                break;
        }

        return true;
    }

    private final int CLICK_TIME = 700;

    private long downTime = 0;

    private Runnable touchDownRunnable = null;

    private int margin = 0;

    public void setMargin(int margin) {
        this.margin = margin;
        postInvalidate();
    }

    protected void onTap(float x, float y) {}

    protected void onTouchDown(float x, float y) {}

    protected void onTouchUp(float x, float y) {}

    protected void onDrag(float x, float y) {}

    protected abstract void onPaint(DroidGraphics g);


}
