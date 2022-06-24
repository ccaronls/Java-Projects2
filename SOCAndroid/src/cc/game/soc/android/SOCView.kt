package cc.game.soc.android;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import cc.game.soc.core.Board;
import cc.game.soc.ui.UIBarbarianRenderer;
import cc.game.soc.ui.UIBoardRenderer;
import cc.game.soc.ui.UIConsoleRenderer;
import cc.game.soc.ui.UIDiceRenderer;
import cc.game.soc.ui.UIEventCardRenderer;
import cc.game.soc.ui.UIPlayerRenderer;
import cc.game.soc.ui.UIRenderer;
import cc.lib.android.DroidGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.math.Vector2D;
import cc.lib.ui.UIComponent;

/**
 * Created by chriscaron on 3/2/18.
 */

public class SOCView<T extends UIRenderer> extends View implements UIComponent {

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
            renderer.onDragStart(Math.round(x), Math.round(y));
            touchDownRunnable = null;
        }
    }

    private final int CLICK_TIME = 700;

    private long downTime = 0;

    private Runnable touchDownRunnable = null;

    public SOCView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SOCView(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        switch (getId()) {
            case R.id.soc_barbarian: {
                UIBarbarianRenderer r = new UIBarbarianRenderer(this);
                float border = getResources().getDimension(R.dimen.border_thin);
                r.initAssets(R.drawable.barbarians_tile, R.drawable.barbarians_piece);
                renderer = (T)r;
                break;
            }
            case R.id.soc_board: {
                UIBoardRenderer r = new UIBoardRenderer(this) {
                    @Override
                    public void onClick() {
                        // disable this method since it is hard to pull off easily on a device. Use accept button only.
                        //super.doClick();
                    }
                };
                r.initImages(R.drawable.desert,
                        R.drawable.water,
                        R.drawable.gold, R.drawable.undiscoveredtile, R.drawable.foresthex, R.drawable.hillshex, R.drawable.mountainshex, R.drawable.pastureshex, R.drawable.fieldshex, R.drawable.knight_basic_inactive, R.drawable.knight_basic_active, R.drawable.knight_strong_inactive, R.drawable.knight_strong_active, R.drawable.knight_mighty_inactive, R.drawable.knight_mighty_active);//, R.drawable.card_frame);
                Board board = new Board();
                board.generateDefaultBoard();
                r.board = board;
                renderer = (T)r;
                break;
            }
            case R.id.soc_dice:
                renderer = (T)new UIDiceRenderer(this, true);
                break;
            case R.id.soc_player_1:
            case R.id.soc_player_2:
            case R.id.soc_player_3:
            case R.id.soc_player_4:
            case R.id.soc_player_5:
            case R.id.soc_player_6:
                renderer = (T)new UIPlayerRenderer(this); break;
            case R.id.soc_event_cards:
                renderer = (T)new UIEventCardRenderer(this); break;
            case R.id.soc_console:
                renderer = (T)new UIConsoleRenderer(this); break;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (renderer != null) {
            if (g == null) {
                g = new DroidGraphics(getContext(), canvas, getWidth(), getHeight()) {
                    @Override
                    public GColor getBackgroundColor() {
                        return GColor.GRAY;
                    }
                };
                g.setCaptureModeSupported(!isInEditMode());
            } else {
                g.setCanvas(canvas, getWidth(), getHeight());
            }
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
                    renderer.onDragEnd();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                tx = Math.round(event.getX());
                ty = Math.round(event.getY());
                if (touchDownRunnable == null) {
                    renderer.onDragStart(event.getX(), event.getY());
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
    public void setRenderer(cc.lib.ui.UIRenderer r) {
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
