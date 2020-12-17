package cc.lib.android;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.math.Vector2D;
import cc.lib.ui.UIComponent;
import cc.lib.ui.UIRenderer;

public abstract class UIComponentView<T extends UIRenderer> extends View implements UIComponent {


    DroidGraphics g;
    int tx = -1, ty = -1;
    T renderer = null;

    private class DelayedTouchDown implements Runnable {

        final float x, y;
        DelayedTouchDown(MotionEvent ev) {
            this.x = ev.getX();
            this.y = ev.getY();
        }
        public void run() {
            //onTouchDown(x, y);
            renderer.startDrag(Math.round(x), Math.round(y));
            touchDownRunnable = null;
        }
    }

    private final int CLICK_TIME = 700;

    private long downTime = 0;

    private Runnable touchDownRunnable = null;

    public UIComponentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public UIComponentView(Context context) {
        super(context);
        init(context, null);
    }

    protected abstract void init(Context context, AttributeSet attrs);

    protected float getProgress() {
        return 1;
    }

    protected void loadAssets(DroidGraphics g) {}

    protected void preDrawInit(DroidGraphics g) {}

    Runnable loadAssetsRunnable = null;

    @Override
    protected void onDraw(Canvas canvas) {
        float progress = getProgress();
        if (g == null) {
            g = new DroidGraphics(getContext(), canvas, getWidth(), getHeight());
            g.setCaptureModeSupported(!isInEditMode());
            preDrawInit(g);
        } else {
            g.setCanvas(canvas, getWidth(), getHeight());
        }
        if (progress < 1) {
            if (loadAssetsRunnable == null) {
                new Thread(loadAssetsRunnable = () -> loadAssets(g)).start();
            }

            g.clearScreen(GColor.BLACK);
            g.setColor(GColor.CYAN);
            g.ortho();
            GRectangle rect = new GRectangle(new Vector2D(getWidth()/2, getHeight()/2), new GDimension(getWidth()*3/4, getHeight()/4));
            g.drawRect(rect, 3);
            rect.w *= progress;
            g.drawFilledRect(rect);

        } else if (renderer != null) {
            GDimension prev = renderer.getMinDimension();
            renderer.draw(g, tx, ty);
            GDimension next = renderer.getMinDimension();
            if (!next.equals(prev)) {
                if (isResizable()) {
                    requestLayout();
                    invalidate();
                }
            }
        }
    }

    boolean isResizable() {
        ViewGroup.LayoutParams lp = getLayoutParams();
        return lp.width == ViewGroup.LayoutParams.WRAP_CONTENT || lp.height == ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (renderer == null)
            return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downTime = SystemClock.uptimeMillis();
                tx = Math.round(event.getX());
                ty = Math.round(event.getY());
                break;
            case MotionEvent.ACTION_UP:
                tx = ty = -1;
                if (SystemClock.uptimeMillis() - downTime < CLICK_TIME) {
                    removeCallbacks(touchDownRunnable);
                    touchDownRunnable = null;
                    renderer.onClick();
                } else {
                    renderer.endDrag();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                tx = Math.round(event.getX());
                ty = Math.round(event.getY());
                if (touchDownRunnable == null) {
                    renderer.startDrag(event.getX(), event.getY());
                }
                break;
        }
        invalidate();
        return true;
    }

    @Override
    public void redraw() {
        postInvalidate();
    }

    @Override
    public void setRenderer(UIRenderer r) {
        this.renderer = (T)r;
    }

    @Override
    public Vector2D getViewportLocation() {
        int [] loc = new int[2];
        getLocationOnScreen(loc);
        return new Vector2D(loc[0], loc[1]);
    }

    public T getRenderer() {
        return renderer;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        int wSpec = MeasureSpec.getMode(widthMeasureSpec);
        int hSpec = MeasureSpec.getMode(heightMeasureSpec);

        GDimension dim = renderer.getMinDimension();

        switch (wSpec) {
            case MeasureSpec.AT_MOST:
                width = Math.min(width, Math.round(dim.width)); break;
            case MeasureSpec.UNSPECIFIED:
                width = Math.round(dim.width); break;
            case MeasureSpec.EXACTLY:
        }

        switch (hSpec) {
            case MeasureSpec.AT_MOST:
                height = Math.min(height, Math.round(dim.height)); break;
            case MeasureSpec.UNSPECIFIED:
                height = Math.round(dim.height); break;
            case MeasureSpec.EXACTLY:
        }

        super.setMeasuredDimension(width, height);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.g != null) {
            g.releaseBitmaps();
        }
    }
}

