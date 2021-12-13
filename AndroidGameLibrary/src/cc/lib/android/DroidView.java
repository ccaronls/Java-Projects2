package cc.lib.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import cc.lib.game.GColor;
import cc.lib.game.Utils;

public class DroidView extends View implements ScaleGestureDetector.OnScaleGestureListener {

    final static String TAG = "DroidView";

    private boolean isParentDroidActivity = false;
    private ScaleGestureDetector scaleDetector;
    private float gestureScale = 1;
    private float minScale=0.1f, maxScale = 5;
    private float pinchCenterX=0, pinchCenterY=0;
    private boolean scaling = false;

    public DroidView(Context context, boolean touchEnabled) {
        super(context);
        setClickable(touchEnabled);
        isParentDroidActivity = context instanceof DroidActivity;
    }

    public DroidView(Context context, AttributeSet attrs) {
        super(context, attrs);
        isParentDroidActivity = context instanceof DroidActivity;
    }

    public void setPinchZoomEnabled(boolean enabled) {
        if (enabled) {
            scaleDetector = new ScaleGestureDetector(getContext(), this);
            scaleDetector.setQuickScaleEnabled(true);
        } else {
            scaleDetector = null;
            gestureScale = 1;
        }
    }

    public void setZoomScaleBound(float minScale, float maxScale) {
        this.minScale = minScale;
        this.maxScale = maxScale;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        pinchCenterX = detector.getFocusX();
        pinchCenterY = detector.getFocusY();
        gestureScale = Utils.clamp(gestureScale * detector.getScaleFactor(), minScale, maxScale);
        invalidate();
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        scaling = true;
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        scaling = false;
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
        canvas.scale(gestureScale, gestureScale,pinchCenterX,pinchCenterY);
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

        if (scaleDetector != null) {
            scaleDetector.onTouchEvent(event);
            if (scaling)
                return true;
        }

        if (!isParentDroidActivity)
            return super.onTouchEvent(event);

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

    protected void onTap(float x, float y) { ((DroidActivity)getContext()).onTap(x, y); }

    protected void onTouchDown(float x, float y) { ((DroidActivity)getContext()).onTouchDown(x, y); }

    protected void onTouchUp(float x, float y) { ((DroidActivity)getContext()).onTouchUp(x, y); }

    protected void onDragStart(float x, float y) { ((DroidActivity)getContext()).onDragStart(x, y); }

    protected void onDragStop(float x, float y) { ((DroidActivity)getContext()).onDragStop(x, y); }

    protected void onDrag(float x, float y) { ((DroidActivity)getContext()).onDrag(x, y); }

    protected void onPaint(DroidGraphics g) {
        if (isParentDroidActivity) ((DroidActivity)getContext()).onDrawInternal(g);
    }


}
