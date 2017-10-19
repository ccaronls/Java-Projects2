package cc.android.game.robots;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import cc.lib.game.IKArm;
import cc.lib.math.Vector2D;

/**
 * Created by chriscaron on 10/19/17.
 */

public class RobotnixView extends View {

    IKArm arm = new IKArm();

    final Paint pFill = new Paint();
    final Paint pStroke = new Paint();
    final Paint pText = new Paint();
    final Path path = new Path();

    float hingeRadius = 20;

    private void init(Context context, AttributeSet attrs) {
        pFill.setStyle(Paint.Style.FILL);
        pFill.setColor(Color.CYAN);
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setColor(Color.RED);
        pText.setColor(Color.BLUE);

        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.RobotnixView);
        pText.setTextSize(arr.getDimension(R.styleable.RobotnixView_textSize, 32));
        pStroke.setStrokeWidth(arr.getDimension(R.styleable.RobotnixView_armThickness, 20));
        hingeRadius = arr.getDimension(R.styleable.RobotnixView_hingeRadius, hingeRadius);

        arr.recycle();
    }

    public RobotnixView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RobotnixView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private long downTime = 0;
    private float dragX, dragY;
    private int draggingIndex = -1;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        final float touchX = event.getX();
        final float touchY = event.getY();

        long dt = (int)(SystemClock.uptimeMillis() - downTime);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downTime = SystemClock.uptimeMillis();
                postDelayed(new Runnable() {
                    public void run() {
                        if (downTime > 0) {
                            onDrag(touchX, touchY);
                        }
                    }
                }, 200);
                break;
            case MotionEvent.ACTION_MOVE:
                if (dt > 50) {
                    onDrag(touchX, touchY);
                    //dragging = game.getPiece(touchRank, touchColumn);
                    //if (dragging.moves.size() == 0)
                    //    dragging = null;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (dt < 500 && draggingIndex < 0) { // && curColumn == touchColumn && curRank == touchRank) {
                    onTap(touchX, touchY);//curRank, curColumn);
                } else if (draggingIndex >= 0) {
                    onDragEnd(touchX, touchY);
                    //tapped = null;
                }
                draggingIndex = -1;
                downTime = 0;
                break;

            default:
                return false;

        }
        invalidate();
        return true;
    }

    private void onDrag(float x, float y) {
        dragX = x;
        dragY = y;
        if (draggingIndex < 0) {
            Vector2D v = new Vector2D(dragX, dragY);
            for (int i=0; i<arm.getNumSections(); i++) {
                IKArm.Section s = arm.getSection(i);
                if (s.v.sub(v).magSquared() < hingeRadius*2) {
                    draggingIndex = i;
                    break;
                }
            }
        }
        if (draggingIndex >= 0) {
            arm.moveSectionTo(draggingIndex, dragX, dragY);
        }
        invalidate();
    }

    private void onTap(float x, float y) {
        if (arm.getNumSections() == 0) {
            arm.addSection(x, y, new IKArm.FixedConstraint());
        } else {
            arm.addSection(x, y, new IKArm.AngleConstraint(90, 270));
        }
    }

    private void onDragEnd(float x, float y) {

    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isInEditMode() && arm.getNumSections() == 0) {
            arm.addSection(20, canvas.getHeight()/2);
            arm.addSection(150, canvas.getHeight()*2/3);
            arm.addSection(300, canvas.getHeight()/3);
        }

        pFill.setColor(Color.GRAY);
        canvas.drawRect(0, getWidth(), 0, getHeight(), pFill);
        //canvas.save();
        IKArm.Section last = null;
        //path.reset();
        for (IKArm.Section s : arm.getSections()) {
            if (last != null) {
                //path.lineTo(s.v.X(), s.v.Y());
                canvas.drawLine(last.v.X(), last.v.Y(), s.v.X(), s.v.Y(), pStroke);
            } else {
                //path.moveTo(s.v.X(), s.v.Y());

            }
            last = s;
        }
        canvas.drawPath(path, pStroke);
        pFill.setColor(Color.YELLOW);
        int idx = 0;
        for (IKArm.Section s : arm.getSections()) {
            canvas.drawCircle(s.v.X(), s.v.Y(), hingeRadius, pFill);
            drawConstraints(canvas, s);
            canvas.drawText(String.valueOf(arm.getAngle(idx++)), s.v.X(), s.v.Y(), pText);
        }
        if (draggingIndex >= 0) {
            IKArm.Section s = arm.getSection(draggingIndex);
            pFill.setColor(Color.RED);
            canvas.drawCircle(s.v.X(), s.v.Y(), hingeRadius, pFill);
        }
    }

    private void drawConstraints(Canvas canvas, IKArm.Section s) {
        for (IKArm.AConstraint cons : s.constraints) {
            if (cons instanceof IKArm.FixedConstraint) {
                pFill.setColor(Color.RED);
                canvas.drawCircle(s.v.X(), s.v.Y(), hingeRadius / 2, pFill);
            } else if (cons instanceof IKArm.AngleConstraint) {
                IKArm.AngleConstraint ac = (IKArm.AngleConstraint) cons;
                rectF.set(s.v.X() - hingeRadius * 2, s.v.Y() + hingeRadius * 2, s.v.Y() - hingeRadius * 2, s.v.Y() + hingeRadius * 2);
                pFill.setColor(Color.GREEN);
                canvas.drawArc(rectF, ac.lastMinAngle, ac.lastMaxAngle, true, pFill);
            }
        }
    }

    private final RectF rectF = new RectF();
}
