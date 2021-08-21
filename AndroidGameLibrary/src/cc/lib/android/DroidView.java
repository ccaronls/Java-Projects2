package cc.lib.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import cc.lib.game.GColor;

public class DroidView extends View {

    final static String TAG = "DroidView";

    private boolean touchable = false;

    public DroidView(Context context, boolean touchEnabled) {
        super(context);
        setClickable(touchEnabled);
        touchable = context instanceof DroidActivity;
    }

    public DroidView(Context context, AttributeSet attrs) {
        super(context, attrs);
        touchable = context instanceof DroidActivity;
    }

    public DroidView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DroidView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private DroidGraphics g = null;

    @Override
    protected void onDraw(Canvas canvas) {
        int width = canvas.getWidth()-margin*2;
        int height = canvas.getHeight()-margin*2;
        if (g == null) {
            GColor BACK;
            if (getBackground() instanceof ColorDrawable) {
                BACK = new GColor(((ColorDrawable)getBackground()).getColor());
            } else {
                BACK = GColor.LIGHT_GRAY;
            }
            g = new DroidGraphics(getContext(), canvas, width, height) {
                @Override
                public GColor getBackgroundColor() {
                    return BACK;
                }
            };
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

    private void checkStartDrag() {
        if (!dragging && downTime > 0) {
            dragging = true;
            Log.v(TAG, "startDrag " + tx + " x " + ty + " from checkStartDrag");
            onDragStart(tx, ty);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downTime = SystemClock.uptimeMillis();
                postDelayed(()->checkStartDrag(), CLICK_TIME);
                tx = event.getX();
                ty = event.getY();
                Log.v(TAG, "onTouchDown " + tx + " x " + ty);
                onTouchDown(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_UP:
                Log.v(TAG, "onTouchUp");
                if (!dragging && (SystemClock.uptimeMillis() - downTime < CLICK_TIME)) {
                    Log.v(TAG, "onTap");
                    //removeCallbacks(touchDownRunnable);
                    //touchDownRunnable = null;
                    onTap(event.getX(), event.getY());

                } else {
                    onTouchUp(event.getX(), event.getY());
                }
                if (dragging) {
                    Log.v(TAG, "onDragStop");
                    onDragStop(event.getX(), event.getY());
                }
                dragging = false;
                downTime = 0;
                tx = ty = -1;
                break;
            case MotionEvent.ACTION_MOVE: {
                float dx = event.getX() - tx;
                float dy = event.getY() - ty;
                float d = dx*dx + dy*dy;
                if (dragging || d > 100) {
                    if (!dragging) {
                        Log.v(TAG, "startDrag " + tx + " x " + ty + " from MOVE");
                        onDragStart(tx, ty);
                    } else {
                        Log.v(TAG, "drag " + tx + " x " + ty);
                        onDrag(event.getX(), event.getY());
                    }
                    dragging = true;
                }
                break;
            }
        }
        invalidate();
//        postDelayed(()->invalidate(), 50);
        return true;
    }

    private final int CLICK_TIME = 700;

    private long downTime = 0;

    private int margin = 0;

    public void setMargin(int margin) {
        this.margin = margin;
        postInvalidate();
    }

    protected void onTap(float x, float y) { if (touchable) ((DroidActivity)getContext()).onTap(x, y); }

    protected void onTouchDown(float x, float y) { if (touchable)((DroidActivity)getContext()).onTouchDown(x, y); }

    protected void onTouchUp(float x, float y) { if (touchable)((DroidActivity)getContext()).onTouchUp(x, y); }

    protected void onDragStart(float x, float y) { if (touchable)((DroidActivity)getContext()).onDragStart(x, y); }

    protected void onDragStop(float x, float y) { if (touchable)((DroidActivity)getContext()).onDragStop(x, y); }

    protected void onDrag(float x, float y) { if (touchable)((DroidActivity)getContext()).onDrag(x, y); }

    protected void onPaint(DroidGraphics g) {
        if (touchable) ((DroidActivity)getContext()).onDraw(g);
    }


}
