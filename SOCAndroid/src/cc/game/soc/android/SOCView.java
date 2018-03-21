package cc.game.soc.android;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import cc.game.soc.core.Board;
import cc.game.soc.ui.UIBarbarianRenderer;
import cc.game.soc.ui.UIBoardRenderer;
import cc.game.soc.ui.UIComponent;
import cc.game.soc.ui.UIDiceRenderer;
import cc.game.soc.ui.UIEventCardRenderer;
import cc.game.soc.ui.UIPlayerRenderer;
import cc.game.soc.ui.UIRenderer;
import cc.lib.android.DroidGraphics;
import cc.lib.math.Vector2D;

/**
 * Created by chriscaron on 3/2/18.
 */

public class SOCView<T extends UIRenderer> extends View implements UIComponent {

    DroidGraphics g;
    int tx, ty;
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


    public SOCView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SOCView(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
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
                    public void doClick() {
                        // disable this method since it is hard to pull off easily on a device. Use accept button only.
                        //super.doClick();
                    }
                };
                r.initImages(R.drawable.desert, R.drawable.water, R.drawable.gold, R.drawable.undiscoveredtile, R.drawable.foresthex, R.drawable.hillshex, R.drawable.mountainshex, R.drawable.pastureshex, R.drawable.fieldshex, R.drawable.knight_basic_inactive, R.drawable.knight_basic_active, R.drawable.knight_strong_inactive, R.drawable.knight_strong_active, R.drawable.knight_mighty_inactive, R.drawable.knight_mighty_active, R.drawable.card_frame);
                Board board = new Board();
                board.generateDefaultBoard();
                board.trim();
                r.board = board;
                renderer = (T)r;
                break;
            }
            case R.id.soc_dice:
                renderer = (T)new UIDiceRenderer(this);
                break;
            case R.id.soc_user:
            case R.id.soc_player_top:
            case R.id.soc_player_middle:
            case R.id.soc_player_bottom:
                renderer = (T)new UIPlayerRenderer(this); break;
            case R.id.soc_event_cards:
                renderer = (T)new UIEventCardRenderer(this); break;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (renderer != null) {
            if (g == null) {
                g = new DroidGraphics(getContext(), canvas);
            } else {
                g.setCanvas(canvas);
            }
            renderer.draw(g, tx, ty);
        }
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
                    renderer.doClick();
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
}
