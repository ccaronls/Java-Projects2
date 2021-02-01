package cc.lib.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Draw text in a circle
 */
public class CircleTextView extends TextView {

    private Path path;
    private Paint paint = new Paint();
    private int textCenterAngle = 270;
    private boolean reverseDirection = false;

    public CircleTextView(Context context) {
        super(context);
    }

    public CircleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.CircleTextView);
        textCenterAngle = arr.getInt(R.styleable.CircleTextView_textCenterAngle, textCenterAngle);
        reverseDirection = arr.getBoolean(R.styleable.CircleTextView_textDirectionReverse, reverseDirection);
        arr.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float direction = reverseDirection ? -1 : 1;
        float textHeight = getPaint().getTextSize();
        float radius = Math.min(getWidth(), getHeight())/2-textHeight/2;
        if (path == null) {
            path = new Path();
            RectF rect = new RectF(getWidth()/2-radius, getHeight()/2-radius, getWidth()/2+radius, getHeight()/2+radius);
            path.addArc(rect, (textCenterAngle+180)%360, 360 * direction);
            //path.addCircle(getWidth()/2, getHeight()/2, radius, Path.Direction.CW);
        }

        String text = getText().toString();
        float textWidth = getPaint().measureText(text);
        float circum  = Math.round(Math.PI * 2 * radius);
        float hOffset = circum*.5f - textWidth/2;

        canvas.drawTextOnPath(text, path, hOffset, textHeight * 0.5f, getPaint());
        if (isInEditMode()) {
            paint.setStrokeWidth(5);
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(path, paint);
        }
    }
}