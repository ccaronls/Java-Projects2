package cc.lib.android;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DroidView extends View {

    public DroidView(Context context, boolean touchEnabled) {
        super(context);
        setClickable(touchEnabled);
    }

    public DroidView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DroidView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DroidView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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

    float tx=-1, ty=-1;
    boolean dragging = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downTime = SystemClock.uptimeMillis();
                tx = event.getX();
                ty = event.getY();
                onTouchDown(event.getX(), event.getY());
                //postDelayed(touchDownRunnable=new DelayedTouchDown(event), CLICK_TIME);
                break;
            case MotionEvent.ACTION_UP:
                if (!dragging && (SystemClock.uptimeMillis() - downTime < CLICK_TIME)) {
                    //removeCallbacks(touchDownRunnable);
                    //touchDownRunnable = null;
                    onTap(event.getX(), event.getY());
                } else {
                    onTouchUp(event.getX(), event.getY());
                }
                dragging = false;
                tx = ty = -1;
                break;
            case MotionEvent.ACTION_MOVE: {
                float dx = event.getX() - tx;
                float dy = event.getY() - ty;
                float d = dx*dx + dy*dy;
                if (dragging || d > 100) {
                    dragging = true;
                    onDrag(event.getX(), event.getY());
                }
                break;
            }
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

    protected void onTap(float x, float y) { ((DroidActivity)getContext()).onTap(x, y); }

    protected void onTouchDown(float x, float y) { ((DroidActivity)getContext()).onTouchDown(x, y); }

    protected void onTouchUp(float x, float y) { ((DroidActivity)getContext()).onTouchUp(x, y); }

    protected void onDrag(float x, float y) { ((DroidActivity)getContext()).onDrag(x, y); }

    protected void onPaint(DroidGraphics g) {
        ((DroidActivity)getContext()).onDraw(g);
    }


}
