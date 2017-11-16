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

import cc.lib.ik.AngleConstraint;
import cc.lib.ik.FixedConstraint;
import cc.lib.ik.IKArm;
import cc.lib.ik.IKConstraint;
import cc.lib.ik.IKHinge;
import cc.lib.math.Vector2D;

/**
 * Created by chriscaron on 10/19/17.
 */

public class RobotnixView extends View {

    IKArm arm = new IKArm();

    final Paint pFill = new Paint();
    final Paint pStroke = new Paint();
    final Paint pStrokeThin = new Paint();
    final Paint pText = new Paint();
    final Path path = new Path();

    float hingeRadius = 20;

    private void init(Context context, AttributeSet attrs) {
        pFill.setStyle(Paint.Style.FILL);
        pFill.setColor(Color.CYAN);
        pStroke.setStyle(Paint.Style.STROKE);
        pStroke.setColor(Color.RED);
        pText.setColor(Color.BLUE);
        pStrokeThin.setStyle(Paint.Style.STROKE);
        pStrokeThin.setStrokeWidth(3);
        pStrokeThin.setColor(Color.RED);

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

    long downTime = 0;
    float dragX, dragY;
    int draggingIndex = -1;
    int tappedIndex = -1;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        final float touchX = event.getX();
        final float touchY = event.getY();

        long dt = (int)(SystemClock.uptimeMillis() - downTime);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                tappedIndex = -1;
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
            for (int i=0; i<arm.getNumHinges(); i++) {
                IKHinge s = arm.getHinge(i);
                if (s.v.sub(v).magSquared() < hingeRadius*hingeRadius) {
                    draggingIndex = i;
                    break;
                }
            }
        }
        if (draggingIndex >= 0) {
            arm.moveHingeTo(draggingIndex, dragX, dragY);
        }
        invalidate();
    }

    private void onTap(float x, float y) {
        if (arm.getNumHinges() == 0) {
            arm.addHinge(x, y, new AngleConstraint(180));//, new FixedConstraint());
        } else if ((tappedIndex =arm.findHinge(x, y, hingeRadius)) < 0){
            tappedIndex = arm.getNumHinges();
            arm.addHinge(x, y, new AngleConstraint(120));
        }
    }

    private void onDragEnd(float x, float y) {

    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isInEditMode() && arm.getNumHinges() == 0) {
            arm.addHinge(20, canvas.getHeight()/2);
            arm.addHinge(150, canvas.getHeight()*2/3);
            arm.addHinge(300, canvas.getHeight()/3);
        }

        pFill.setColor(Color.GRAY);
        canvas.drawRect(0, getWidth(), 0, getHeight(), pFill);
        //canvas.save();
        IKHinge last = null;
        //path.reset();
        for (IKHinge s : arm.getHinges()) {
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
        if (draggingIndex >= 0) {
            IKHinge s = arm.getHinge(draggingIndex);
            pFill.setColor(Color.RED);
            canvas.drawCircle(s.v.X(), s.v.Y(), hingeRadius, pFill);
        }
        if (tappedIndex >= 0) {
            IKHinge s = arm.getHinge(tappedIndex);
            pStrokeThin.setColor(Color.RED);
            canvas.drawCircle(s.v.X(), s.v.Y(), hingeRadius*2, pStrokeThin);
        }
        int idx = 0;
        for (IKHinge s : arm.getHinges()) {
            canvas.drawCircle(s.v.X(), s.v.Y(), hingeRadius, pFill);
            drawConstraints(canvas, s);
            canvas.drawText(String.format("%d:%3.2f", idx, arm.getAngle(idx++)), s.v.X(), s.v.Y(), pText);
        }
    }

    private void drawConstraints(Canvas canvas, IKHinge s) {
        for (IKConstraint cons : s.constraints) {
            if (cons instanceof FixedConstraint) {
                pFill.setColor(Color.RED);
                canvas.drawCircle(s.v.X(), s.v.Y(), hingeRadius / 2, pFill);
            } else if (cons instanceof AngleConstraint) {
                AngleConstraint ac = (AngleConstraint) cons;
                rectF.set(s.v.X() - hingeRadius * 2, s.v.Y() - hingeRadius * 2, s.v.X() + hingeRadius * 2, s.v.Y() + hingeRadius * 2);
                pFill.setColor(Color.GREEN);
                canvas.drawArc(rectF, ac.lastStartAngle, ac.getSweep(), true, pFill);
            }
        }
    }

    private final RectF rectF = new RectF();
}
