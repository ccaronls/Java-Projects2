package cc.lib.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import org.jetbrains.annotations.NotNull;

import cc.lib.game.GColor;

public class DPadView extends ImageView {

    public interface OnDpadListener {
        default void dpadMoved(@NotNull DPadView view, float dx, float dy) {
        }

        default void dpadPressed(@NotNull DPadView view, @NotNull int dirFlag) {
        }

        default void dpadReleased(@NotNull DPadView view) {
        }
    }

    private float cx = 0, cy = 0, tx = 0, ty = 0, dx = 0, dy = 0;

    private boolean touched = false;

    public enum PadDir {
        LEFT(1),
        RIGHT(2),
        UP(4),
        DOWN(8);

        PadDir(int flag) {
            this.flag = flag;
        }

        private final int flag;

        boolean isFlagged(int flag) {
            return (flag & this.flag) != 0;
        }

        public static int toDx(int flag) {
            if (LEFT.isFlagged(flag))
                return -1;
            if (RIGHT.isFlagged(flag))
                return 1;
            return 0;
        }

        public static int toDy(int flag) {
            if (UP.isFlagged(flag))
                return -1;
            if (DOWN.isFlagged(flag))
                return 1;
            return 0;
        }

    }
    
    private Paint paint = new Paint();
    private int downFlag = 0;
    
    private float pressure = 0;
    private OnDpadListener listener;
    
    public float getPressure() {
        return pressure;
    }
    
    public boolean isLeftPressed() {
        return touched && (downFlag & PadDir.LEFT.flag) != 0;
    }
    public boolean isRightPressed() {
        return touched && (downFlag & PadDir.RIGHT.flag) != 0;
    }
    public boolean isUpPressed() {
        return touched && (downFlag & PadDir.UP.flag) != 0;
    }
    public boolean isDownPressed() {
        return touched && (downFlag & PadDir.DOWN.flag) != 0;
    }
    
    public float getDx() {
        return dx;
    }
    
    public float getDy() {
        return dy;
    }
    
    public void setOnDpadListener(OnDpadListener listener) {
        this.listener = listener;
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);

        int dim = (width > height ? height : width);
        int max = (width > height ? heightMeasureSpec : widthMeasureSpec);

        dim = roundUpToTile(dim, 1, max);

        cx = width / 2;
        cy = height / 2;

        setMeasuredDimension(dim, dim);
    }
    
    private int roundUpToTile(int dimension, int tileSize, int maxDimension) {
        return Math.min(((dimension + tileSize - 1) / tileSize) * tileSize, maxDimension);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (BuildConfig.DEBUG) {
            paint.setColor(GColor.GREEN.toARGB());
            paint.setStrokeWidth(4);
            canvas.drawCircle(cx + dx, cy + dy, 10, paint);
        }
        
        final float RECT_DIM = 16;
        
        paint.setColor(GColor.GREEN.toARGB());
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(3);
        if (isDownPressed()) {
            float left =  getWidth()/2 - RECT_DIM/2;
            float right = getWidth()/2 + RECT_DIM/2;
            float bottom = getHeight() / 2 + getWidth()/2;
            float top = bottom - RECT_DIM;
            canvas.drawRect(left, top, right, bottom, paint);
        }
        if (isLeftPressed()) {
            float left = 0;
            float right = RECT_DIM;
            float top = getHeight()/2 - RECT_DIM / 2;
            float bottom = top + RECT_DIM;
            canvas.drawRect(left, top, right, bottom, paint);
        }
        if (isRightPressed()) {
            float left = getWidth() - RECT_DIM;
            float right = getWidth();
            float top = getHeight()/2 - RECT_DIM / 2;
            float bottom = top + RECT_DIM;
            canvas.drawRect(left, top, right, bottom, paint);
        }
        if (isUpPressed()) {
            float left =  getWidth()/2 - RECT_DIM/2;
            float right = getWidth()/2 + RECT_DIM/2;
            float top = getHeight() / 2 - getWidth()/2;
            float bottom = top + RECT_DIM;
            canvas.drawRect(left, top, right, bottom, paint);
        }
        canvas.drawLine(cx, cy, cx + dx, cy + dy, paint);
    }

    public DPadView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public DPadView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DPadView(Context context) {
        super(context);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean touched = false;
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                touched = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
            default:
                return false;
        }
        if (!touched) {
            dx = dy = 0;
            listener.dpadReleased(this);
        } else {
            tx = x;
            ty = y;
            dx = tx - cx;
            dy = ty - cy;

            int flag = 0;

            listener.dpadMoved(this, dx, dy);

            float sensitivity = 4f;
            float horzRange = getWidth() / (sensitivity * 2);
            float vertRange = getHeight() / (sensitivity * 2);

            float x0 = cx - horzRange;
            float x1 = cx + horzRange;
            float y0 = cy - vertRange;
            float y1 = cy + vertRange;

            if (tx < x0) {
                flag |= PadDir.LEFT.flag;
            } else if (tx > x1) {
                flag |= PadDir.RIGHT.flag;
            }

            if (ty < y0) {
                flag |= PadDir.UP.flag;
            } else if (ty > y1) {
                flag |= PadDir.DOWN.flag;
            }
    
            if (listener != null) {
                listener.dpadPressed(this, flag);
            }
            downFlag = flag;
        }
        
        invalidate();
        return true;
    }
    
    
}
