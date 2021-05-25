package cc.lib.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.math.Vector2D;
import cc.lib.ui.UIComponent;
import cc.lib.ui.UIRenderer;

public abstract class UIComponentView<T extends UIRenderer> extends View implements UIComponent {


    private DroidGraphics g;
    private int tx = -1, ty = -1;
    private T renderer = null;
    private float borderThickness = 0;
    private int borderColor = 0;
    private Paint borderPaint = new Paint();

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

    protected final void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.UIComponentView);
        borderThickness = a.getDimension(R.styleable.UIComponentView_borderThickness, borderThickness);
        borderColor = a.getColor(R.styleable.UIComponentView_borderColor, borderColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(borderThickness);
        borderPaint.setColor(borderColor);
        a.recycle();
    }

    protected float getProgress() {
        return 1;
    }

    protected void loadAssets(DroidGraphics g) {}

    protected void preDrawInit(DroidGraphics g) {}

    Runnable loadAssetsRunnable = null;

    @Override
    protected void onDraw(Canvas canvas) {
        float progress = getProgress();
        int width = Math.round(getWidth() - borderThickness*2);
        int height = Math.round(getHeight() - borderThickness*2);
        if (g == null) {
            g = new DroidGraphics(getContext(), canvas, width, height);
            g.setCaptureModeSupported(!isInEditMode());
            preDrawInit(g);
        } else {
            g.setCanvas(canvas, width, height);
        }

        if (borderThickness > 0) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), borderPaint);
            g.translate(borderThickness, borderThickness);
        }

        if (progress < 1) {
            if (loadAssetsRunnable == null) {
                new Thread(loadAssetsRunnable = () -> loadAssets(g)).start();
            }

            g.setColor(GColor.RED);
            g.ortho();
            GRectangle rect = new GRectangle(0, 0, new GDimension(getWidth()*3/4, getHeight()/6)).withCenter(new Vector2D(getWidth()/2, getHeight()/2));
            g.drawRect(rect, 3);
            rect.w *= progress;
            g.drawFilledRect(rect);
            float hgt = g.getTextHeight();
            g.setTextHeight(rect.h*3/4);
            g.setColor(GColor.WHITE);
            g.drawJustifiedString(getWidth()/2,getHeight()/2, Justify.CENTER,Justify.CENTER,"LOADING");
            g.setTextHeight(hgt);

        } else if (renderer != null) {
            loadAssetsRunnable = null;
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

        g.translate(-borderThickness, -borderThickness);

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
                    renderer.onDragEnd();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                tx = Math.round(event.getX());
                ty = Math.round(event.getY());
                if (touchDownRunnable == null) {
                    renderer.onDragStart(event.getX(), event.getY());
                } else {
                    renderer.onDragMove(event.getX(), event.getY());
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

        GDimension dim = renderer == null ? new GDimension(32, 32) : renderer.getMinDimension();

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

