package cc.lib.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import cc.lib.game.GColor;

public class DPadView extends ImageView {

    public interface OnDpadListener {
        void dpadPressed(DPadView view, PadDir dir);
        void dpadReleased(DPadView view, PadDir dir);
    }
    
    private float cx, cy, tx, ty, dx, dy;
    
    private boolean touched = false;
    
    public enum PadDir {
        LEFT(1), 
        RIGHT(2), 
        UP(4), 
        DOWN(8);
        
        PadDir (int flag) {
            this.flag = flag;
        }
        
        private final int flag;
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
        final int width =  MeasureSpec.getSize(widthMeasureSpec);
        final int height =  MeasureSpec.getSize(heightMeasureSpec);

        int dim = (width > height ? height : width);
        int max = (width > height ? heightMeasureSpec : widthMeasureSpec);
        
        dim = roundUpToTile(dim, 1, max);
        
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

    private void init() {
        //setOnTouchListener(this);
    }
    
    public DPadView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public DPadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DPadView(Context context) {
        super(context);
        init();
    }

    public void doTouch(MotionEvent event, float x, float y) {
        boolean touched = false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                touched = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
            default:
                return;
        }
        if (!touched) {
            dx = dy = 0;
        } else {
            tx = x; 
            ty = y;
            cx = getWidth()/2; // + view.getLeft()
            cy = getHeight()/2; // + view.getTop()
            dx = tx - cx;
            dy = ty - cy;

            int flag = 0;
            
            float x0 = getWidth()/3;
            float x1 = x0*2;
            float y0 = getHeight()/2 - getWidth()/3;
            float y1 = getHeight()/2 + getWidth()/3;
    
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
                if ((flag & PadDir.LEFT.flag) != 0) {
                    if ((downFlag & PadDir.LEFT.flag) == 0)
                        listener.dpadPressed(this, PadDir.LEFT);
                } else {
                    if ((downFlag & PadDir.LEFT.flag) != 0)
                        listener.dpadReleased(this, PadDir.LEFT);
                }
    
                if ((flag & PadDir.RIGHT.flag) != 0) {
                    if ((downFlag & PadDir.RIGHT.flag) == 0)
                        listener.dpadPressed(this, PadDir.RIGHT);
                } else {
                    if ((downFlag & PadDir.RIGHT.flag) != 0)
                        listener.dpadReleased(this, PadDir.RIGHT);
                }
                
                if ((flag & PadDir.UP.flag) != 0) {
                    if ((downFlag & PadDir.UP.flag) == 0)
                        listener.dpadPressed(this, PadDir.UP);
                } else {
                    if ((downFlag & PadDir.UP.flag) != 0)
                        listener.dpadReleased(this, PadDir.UP);
                }
                
                if ((flag & PadDir.DOWN.flag) != 0) {
                    if ((downFlag & PadDir.DOWN.flag) == 0)
                        listener.dpadPressed(this, PadDir.DOWN);
                } else {
                    if ((downFlag & PadDir.DOWN.flag) != 0)
                        listener.dpadReleased(this, PadDir.DOWN);
                }
            }        
            
            downFlag = flag;
        }
        
        invalidate();
    }
    
    
}
