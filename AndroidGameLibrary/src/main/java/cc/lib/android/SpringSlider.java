package cc.lib.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Spring slider is a widget that the user can slide from a base position where is will spring back to 0 after the user releases
 * @author chriscaron
 *
 */
public class SpringSlider extends View {

    public interface OnSliderChangedListener {
        public void sliderMoved(SpringSlider slider, float position);
    }
    
    
    private float buttonRadius = 0;
    private float sliderLength;
    private boolean horizontal = false;
    private Drawable buttonIcon;
    private float sliderPosition = 0;
    private float springAccel = 0.01f;
    private float springVelocity = 0;
    private float buttonThickness = 5.0f;
    private int backgroundColor = Color.CYAN;
    private int buttonColor = Color.RED;
    private float targetPosition = 0;
    private OnSliderChangedListener listener;
    
    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes( attrs, R.styleable.SpringSlider );
        buttonRadius = a.getDimension(R.styleable.SpringSlider_buttonRadius, buttonRadius);
        horizontal = a.getBoolean(R.styleable.SpringSlider_horizontal, horizontal);
        buttonIcon = a.getDrawable(R.styleable.SpringSlider_buttonIcon);
        buttonThickness = a.getDimension(R.styleable.SpringSlider_buttonThickness, buttonThickness);
        backgroundColor = a.getColor(R.styleable.SpringSlider_backgroundColor, backgroundColor);
        buttonColor = a.getColor(R.styleable.SpringSlider_buttonColor, buttonColor);
        a.recycle();
    }
    
    public SpringSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public SpringSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SpringSlider(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    Paint paint = new Paint();
    
    @Override
    protected void onDraw(Canvas canvas) {
        final float radius = buttonRadius;
        if (getBackground() == null) {
            paint.setColor(backgroundColor);
            paint.setStyle(Style.FILL);
            if (horizontal) {
                canvas.drawCircle(radius, radius, radius, paint);
                canvas.drawCircle(radius+sliderLength, radius, radius, paint);
                canvas.drawRect(radius, 0, radius+sliderLength, radius*2, paint);
            } else {
                canvas.drawCircle(radius, radius, radius, paint);
                canvas.drawCircle(radius, radius+sliderLength, radius, paint);
                canvas.drawRect(0, radius, radius*2, radius+sliderLength, paint);
            }
        }
        
        if (buttonIcon == null) {
            paint.setStyle(Style.STROKE);
            paint.setStrokeWidth(buttonThickness);
            paint.setColor(buttonColor);
            if (horizontal) {
                canvas.drawCircle(radius + sliderLength*sliderPosition, radius, radius, paint);
            } else {
                canvas.drawCircle(radius, radius+sliderLength*(1-sliderPosition), radius, paint);
            }
        } else {
            buttonIcon.setColorFilter(buttonColor, PorterDuff.Mode.MULTIPLY);
            if (horizontal)
                buttonIcon.setBounds(Math.round(sliderLength*sliderPosition), 0, Math.round(sliderLength*sliderPosition+radius*2), Math.round(radius*2));
            else
                buttonIcon.setBounds(0, getHeight() - Math.round(sliderLength*sliderPosition + radius*2), Math.round(radius*2), getHeight() - Math.round(sliderLength*sliderPosition));
            buttonIcon.draw(canvas);
        }
        
        if (sliderPosition > 0 && targetPosition != sliderPosition) {
            springVelocity += springAccel;
            sliderPosition -= springVelocity;
            if (sliderPosition < 0)
                sliderPosition = 0;
            invalidate();
            if (listener != null)
                listener.sliderMoved(this, sliderPosition);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        
        if (buttonRadius <= 0) {
            if (horizontal) {
                // width must be greater than height
                if (height < 10)
                    height = 10;
                if (width < height*2)
                    width = height*2;
                buttonRadius = height/2;
                sliderLength = width-buttonRadius*2;
            } else {
                // height must be greater then width
                if (width < 10)
                    width = 10;
                if (height < width*2)
                    height = width*2;
                buttonRadius = width/2;
                sliderLength = height-buttonRadius*2;
            }
        } else {
            if (horizontal) {
                height = Math.round(buttonRadius*2);
                if (width < height*2)
                    width = height*2;
            } else {
                width = Math.round(buttonRadius*2);
                if (height < width*2)
                    height = width*2;
            }
        }
        setMeasuredDimension(width, height);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (horizontal) {
                    float x = event.getX();
                    if (x < buttonRadius)
                        targetPosition = sliderPosition = 0;
                    else if (x > buttonRadius+sliderLength)
                        targetPosition = sliderPosition = 1;
                    else {
                        targetPosition = sliderPosition = (x-buttonRadius)/sliderLength; 
                    }
                } else {
                    float y = event.getY();
                    if (y < buttonRadius)
                        targetPosition = sliderPosition = 1;
                    else if (y > buttonRadius+sliderLength)
                        targetPosition = sliderPosition = 0;
                    else
                        targetPosition = sliderPosition = 1f - (y-buttonRadius)/sliderLength;
                }
                if (listener != null)
                    listener.sliderMoved(this, sliderPosition);
                break;
            case MotionEvent.ACTION_UP:
                targetPosition = 0;
                springVelocity = 0;
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    public final float getSliderPosition() {
        return this.sliderPosition;
    }

    public final OnSliderChangedListener getOnSliderChangedListener() {
        return listener;
    }

    public final void setOnSliderChangedListener(OnSliderChangedListener listener) {
        this.listener = listener;
    }
    
    
}
