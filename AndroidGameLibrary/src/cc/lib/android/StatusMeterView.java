package cc.lib.android;

import java.util.LinkedList;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.*;
import android.widget.TextView;

/**
 * This view can be used to show a meter bar, gps status is a typical usage.
 * Can be any one of 4 orientations.
 * @author chriscaron
 *
 */
public class StatusMeterView extends View {

    private static final int DIRECTION_LEFT_TO_RIGHT = 0;
    private static final int DIRECTION_RIGHT_TO_LEFT = 1;
    private static final int DIRECTION_TOP_TO_BOTTOM = 2;
    private static final int DIRECTION_BOTTOM_TO_TOP = 3;

    private final int METER_COLOR_INDEX_DEFAULT = 0;
    private final int METER_COLOR_INDEX_POOR    = 1;
    private final int METER_COLOR_INDEX_FAIR    = 2;
    private final int METER_COLOR_INDEX_GOOD    = 3;
    private final int NUM_METER_COLORS_INDICES  = 4; // MUST BE LAST!

    public enum MeterStyle {
    	ONE_COLOR,
    	MULTI_COLOR
    };
    
    private int direction = DIRECTION_LEFT_TO_RIGHT;
    private int meterTotal = 8;
    private int meterPoor  = 8/3;
    private int meterFair  = 8*2/3;
    private int textViewMeterValueId = -1;
    private final int [] meterColors = new int[NUM_METER_COLORS_INDICES];
    private float meterThickness = 0;
    private float meterSpacing = 5;
    private float meterValue = 0;
    private float meterMaxValue = 1;
    private boolean rounded = false;
    private final Paint paint = new Paint();
    private final RectF rectF = new RectF();
    private final Rect  rectI = new Rect();
    private float length = 0;
    private Drawable barDrawable;
    private MeterStyle meterStyle; 
    private boolean animatePeaks = false;
    private float peak = 0;

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes( attrs, R.styleable.StatusMeterView);
        direction = a.getInt(R.styleable.StatusMeterView_direction, DIRECTION_LEFT_TO_RIGHT);
        meterColors[0] = a.getColor(R.styleable.StatusMeterView_meterColor, Color.BLACK);
        meterColors[1] = a.getColor(R.styleable.StatusMeterView_poorColor, Color.RED);
        meterColors[2] = a.getColor(R.styleable.StatusMeterView_fairColor, Color.YELLOW);
        meterColors[3] = a.getColor(R.styleable.StatusMeterView_goodColor, Color.GREEN);
        meterTotal = a.getInt(R.styleable.StatusMeterView_meterTotal, meterTotal);
        meterFair = a.getInt(R.styleable.StatusMeterView_meterFair, meterTotal*2/3);
        textViewMeterValueId = a.getResourceId(R.styleable.StatusMeterView_textView, -1);
        meterMaxValue = a.getFloat(R.styleable.StatusMeterView_meterMaxValue, meterMaxValue);
        setMeterValue(a.getFloat(R.styleable.StatusMeterView_meterValue, meterValue));
        rounded = a.getBoolean(R.styleable.StatusMeterView_rounded, false);
        int id = a.getResourceId(R.styleable.StatusMeterView_drawable, -1);
        if (id != -1) {
            barDrawable = getResources().getDrawable(id);
        }
        meterThickness = a.getDimension(R.styleable.StatusMeterView_meterThickness, 0);
        meterSpacing = a.getDimension(R.styleable.StatusMeterView_meterSpacing, 0);
        meterStyle = MeterStyle.values()[a.getInt(R.styleable.StatusMeterView_meterStyle, MeterStyle.ONE_COLOR.ordinal())];
        animatePeaks = a.getBoolean(R.styleable.StatusMeterView_animatePeaks, false);
        a.recycle();
        paint.setStyle(Style.FILL);
        paint.setAntiAlias(true);
    }


    public StatusMeterView(Context context) {
        super(context);
    }

    public StatusMeterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public StatusMeterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    
    private long lastUpdateTime = 0;
    private float peakOffsetRampDown = 0;
    
    @Override
    protected void onDraw(Canvas canvas) {
    	super.onDraw(canvas); // draw background if any
    	
        int numMeters = Math.round(meterValue/meterMaxValue * meterTotal);
        final int secondsToFallEntireLength = 300; // higher number result in a quicker move of peak toward 0 
        
        // The below number have been tuned to look like iOS 24/7 as of 12/15/14
        if (peak < numMeters) {
        	peak = numMeters;
        	peakOffsetRampDown = (float)(Math.random() * 2 + 1);
        }
        
        if (animatePeaks && peak > numMeters) {
        	int rampDown = Math.round(peak - peakOffsetRampDown);
        	if (numMeters < rampDown) {
        		numMeters = rampDown;
        		rampDown += (float)(Math.random() * 0.5 + 0.5);
        	}
        }
        
        int meterColor = METER_COLOR_INDEX_DEFAULT;
        if (numMeters <= meterPoor) {
            meterColor = METER_COLOR_INDEX_POOR;
        } else if (numMeters <= meterFair) {
            meterColor = METER_COLOR_INDEX_FAIR;
        } else {
            meterColor = METER_COLOR_INDEX_GOOD;
        }

        paint.setColor(meterColors[meterColor]);
        Drawable drawable = null;
        if (barDrawable != null) {
        	drawable = barDrawable.getConstantState().newDrawable();
        }
        float sx=0;
        float sy=0;
        float dx=0;
        float dy=0;
        float w=0;
        float h=0;

        switch (direction) {
            case DIRECTION_LEFT_TO_RIGHT:
                sx=getPaddingLeft();
                sy=getPaddingTop();
                dy=0;
                dx=meterThickness+meterSpacing;
                w=meterThickness;
                h=getHeight() - getPaddingTop() - getPaddingBottom();
                break;
            case DIRECTION_RIGHT_TO_LEFT:
                sy=getPaddingTop();
                dy=0;
                dx=-(meterThickness+meterSpacing);
                sx=getPaddingLeft() + length-meterThickness;
                w=meterThickness;
                h=getHeight() - getPaddingTop() - getPaddingBottom();
                break;
            case DIRECTION_TOP_TO_BOTTOM:
                sx=getPaddingLeft();
                sy=getPaddingTop();
                dx=0;
                dy=meterThickness+meterSpacing;
                w=getWidth() - getPaddingLeft() - getPaddingRight();
                h=meterThickness;
                break;
            case DIRECTION_BOTTOM_TO_TOP:
                sx=getPaddingLeft();
                sy=getPaddingTop() + length - meterThickness;
                dx=0;
                dy=-(meterThickness+meterSpacing);
                w=getWidth() - getPaddingLeft() - getPaddingRight();
                h=meterThickness;
                break;
            default:
                throw new RuntimeException("Unhandled direction");
        }
        
        if (isInEditMode())
        	adtLog(this, "sx=" + sx + " sy=" + sy + " length=" + length + " h=" + h);
        
        int meter = 0;
        float radius = rounded ? meterThickness : 0;
        while (meter < numMeters) {
        	if (meterStyle == MeterStyle.MULTI_COLOR) {
        		if (meter <= meterPoor)
        			meterColor = METER_COLOR_INDEX_POOR;
        		else if (meter <= meterFair)
        			meterColor = METER_COLOR_INDEX_FAIR;
        		else
        			meterColor = METER_COLOR_INDEX_GOOD;
        		paint.setColor(meterColors[meterColor]);
        	}
        	final float x = sx + dx*meter;
        	final float y = sy + dy*meter;
            rectF.set(x, y, x+w, y+h);
            rectI.set(Math.round(x), Math.round(y), Math.round(x+w), Math.round(y+h));
            if (drawable != null) {
                drawable.setBounds(rectI);
            	drawable.setColorFilter(meterColors[meterColor], Mode.MULTIPLY);
                drawable.draw(canvas);
            } else if (radius > 0){
                canvas.drawRoundRect(rectF, radius, radius, paint);
            } else {
            	canvas.drawRect(rectF, paint);
            }
            meter++;
        }
        paint.setColor(meterColors[METER_COLOR_INDEX_DEFAULT]);
        meterColor = METER_COLOR_INDEX_DEFAULT;
        while (meter < meterTotal) {
        	final float x = sx + dx*meter;
        	final float y = sy + dy*meter;
            rectF.set(x, y, x+w, y+h);
            rectI.set(Math.round(x), Math.round(y), Math.round(x+w), Math.round(y+h));
            if (drawable != null) {
                drawable.setBounds(rectI);
                drawable.setColorFilter(meterColors[meterColor], Mode.MULTIPLY);
                drawable.draw(canvas);
            } else if (radius > 0) {
                canvas.drawRoundRect(rectF, radius, radius, paint);
            } else {
            	canvas.drawRect(rectF, paint);
            }
            meter++;
        }
        
        if (animatePeaks && lastUpdateTime != 0 && peak > 1) {
        	float dt = (float)(SystemClock.uptimeMillis() - lastUpdateTime)/1000;
        	float peakChange = dt*secondsToFallEntireLength/(float)meterTotal;
        	peak -= peakChange;
        	final float x = sx + dx*Math.round(peak);
        	final float y = sy + dy*Math.round(peak);
            rectF.set(x, y, x+w, y+h);
            rectI.set(Math.round(x), Math.round(y), Math.round(x+w), Math.round(y+h));
            if (meterStyle == MeterStyle.MULTI_COLOR) {
        		if (peak <= meterPoor)
        			meterColor = METER_COLOR_INDEX_POOR;
        		else if (peak <= meterFair)
        			meterColor = METER_COLOR_INDEX_FAIR;
        		else
        			meterColor = METER_COLOR_INDEX_GOOD;
        		paint.setColor(meterColors[meterColor]);
        	}
            if (drawable != null) {
            	drawable.setColorFilter(meterColors[meterColor], Mode.MULTIPLY);
                drawable.setBounds(rectI);
                drawable.draw(canvas);
            } else if (radius > 0) {
                canvas.drawRoundRect(rectF, radius, radius, paint);
            } else {
            	canvas.drawRect(rectF, paint);
            }
        }
        if (peak > 1 && animatePeaks && !isInEditMode()) {
        	lastUpdateTime = SystemClock.uptimeMillis();
        	postInvalidate();
        } else {
        	lastUpdateTime = 0;
        }
    }

    /**
     *
     * @return
     */
    public boolean isVertical() {
        return direction == DIRECTION_TOP_TO_BOTTOM || direction == DIRECTION_BOTTOM_TO_TOP;
    }
    
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	
		
    	final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    	final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

    	int widthSpec = MeasureSpec.getSize(widthMeasureSpec);
    	int heightSpec = MeasureSpec.getSize(heightMeasureSpec);

		adtLog(this, "--------------------------------------------");
		adtLog(this, "onMeasure: wMode=" + getModeString(widthMode) + " width=" + widthSpec + " heightMode=" + getModeString(heightMode) + " height=" + heightSpec);

    	if (barDrawable != null && meterThickness == 0) {
    		if (isVertical()) {
    			meterThickness = barDrawable.getIntrinsicHeight();
    		} else {
    			meterThickness = barDrawable.getIntrinsicWidth();
    		}
    	}
    	
    	// TODO: If we are unspecified, then return a desired dimension
    	do {
    		if (meterThickness > 0) {
    			if(meterSpacing <= 0) {
    				meterSpacing = meterThickness/3;
    			}
    			int desiredLength = Math.round(meterThickness * meterTotal + meterSpacing * (meterTotal - 1));
            	if (widthMode == MeasureSpec.UNSPECIFIED && !isVertical()) {
            		widthMeasureSpec = MeasureSpec.makeMeasureSpec(desiredLength, widthMode);
            	} else if (heightMode == MeasureSpec.UNSPECIFIED && isVertical()) {
            		heightMeasureSpec = MeasureSpec.makeMeasureSpec(desiredLength, heightMode);
            	} else if (widthMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.UNSPECIFIED) {
            		// exit out with current specs
            		desiredLength = 0;
            	} else
            		break;
    			adtLog(this, "UNSPECIFIED, so desiredLength=" + desiredLength);
            	super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            	return;
    		}        	
    	
    	} while (false);
    	
    	
    	//Log.d("StatsuMeterView", "onMeasure: wMode=" + getModeString(widthMode) + " width=" + widthSpec + " heightMode=" + getModeString(heightMode) + " height=" + heightSpec);
    	
        if (isVertical()) 
        	length = heightSpec - getPaddingTop() - getPaddingBottom();
        else
        	length = widthSpec - getPaddingLeft() - getPaddingRight();

        // we want to know the meter thickness and spacing to fill the length of the canavs
        //
        // algebra
        //
        // l = length (known)
        // mt = meter thickness (this is what we want to know)
        // ms = meter spacing = mt/3 (derived)
        // t = meter total (known)
        //
        // l = t*mt + (t-1)*ms
        //   = t*mt + (t-1)*mt/3
        //   = (t + (t-1)/3) * mt;
        // mt = l / (t + t/3 - 1/3)
        float actualLength = 0;
        for (int i=0; i<2; i++) {
        	
            if (meterSpacing > 0) {
                if (meterThickness <= 0) {
                    meterThickness = (length - (meterTotal-1)*meterSpacing) / meterTotal; 
                }
                //eclipseLog(this, "onMeasure: meterTotal=" + meterTotal + " length=" + length + " meterThickness = "+ meterThickness);
            } else {
                if (meterThickness <= 0) {
                    meterThickness = length / ((float)meterTotal + (float)meterTotal/3 - 1f/3);
                }
                meterSpacing = meterThickness/3;
            }
            
            // check that we are in the maxWidth
            actualLength = meterThickness * meterTotal + meterSpacing * (meterTotal-1);
            if (meterThickness > 0 && actualLength-0.01 <= length)
            	break;
            
            //adtLog(this, "onMeasure: constraint not met, redo");
            meterThickness = 0;
            meterSpacing = 0;
        }

        //adtLog(this, "length=" + length + " actualLength=" + actualLength);
        if (isVertical()) {
        	if (heightMode != MeasureSpec.EXACTLY) {
        		length=actualLength;
        		heightSpec = (int)actualLength + getPaddingTop() + getPaddingBottom();
        	}
        	if (widthMode != MeasureSpec.EXACTLY) {
        		widthSpec = Math.max(getSuggestedMinimumWidth(), heightSpec/4);
        	}
        } else {
        	if (widthMode != MeasureSpec.EXACTLY) {
        		length=actualLength;
        		widthSpec = (int)actualLength + getPaddingLeft() + getPaddingRight();
        	}
        	if (heightMode != MeasureSpec.EXACTLY) {
        		heightSpec = Math.max(getSuggestedMinimumHeight(), widthSpec/4);
        	}
        }
        
    	if (isInEditMode()) {
    		adtLog(this, "onMeasure: widthSpec=" + widthSpec + " heightSpec=" + heightSpec);//actualLength=" + actualLength + " length=" + length + " paddingL=" + getPaddingLeft() + " paddingR=" + getPaddingRight() + " paddingT=" + getPaddingTop() + " paddingB=" + getPaddingBottom());
    	}

        setMeasuredDimension(widthSpec, heightSpec);
    }

    /**
     *
     * @param value
     */
    public void setMeterValue(float value) {
        if (value < 0)
            value = 0;
        else if (value > meterMaxValue)
            value = meterMaxValue;
        meterValue = value;
        if (textViewMeterValueId >= 0) {
            TextView tv = ((TextView)this.getRootView().findViewById(textViewMeterValueId));
            if (tv != null)
                tv.setText(String.format("%.2f", meterValue));
        }
        invalidate();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (textViewMeterValueId >= 0) {
            TextView tv = ((TextView)this.getRootView().findViewById(textViewMeterValueId));
            if (tv != null)
                tv.setVisibility(visibility);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (textViewMeterValueId >= 0) {
            TextView tv = ((TextView)this.getRootView().findViewById(textViewMeterValueId));
            if (tv != null)
                tv.setEnabled(enabled);
        }
    }

    public final int getDirection() {
		return direction;
	}

	public final void setDirection(int direction) {
		this.direction = direction;
	}

	public final int getMeterTotal() {
		return meterTotal;
	}

	public final void setMeterTotal(int meterTotal) {
		this.meterTotal = meterTotal;
	}

	public final int getMeterPoor() {
		return meterPoor;
	}

	public final void setMeterPoor(int meterPoor) {
		this.meterPoor = meterPoor;
	}

	public final int getMeterFair() {
		return meterFair;
	}

	public final void setMeterFair(int meterFair) {
		this.meterFair = meterFair;
	}

	public final float getMeterThickness() {
		return meterThickness;
	}

	public final void setMeterThickness(float meterThickness) {
		this.meterThickness = meterThickness;
	}

	public final float getMeterSpacing() {
		return meterSpacing;
	}

	public final void setMeterSpacing(float meterSpacing) {
		this.meterSpacing = meterSpacing;
	}

	public final float getMeterMaxValue() {
		return meterMaxValue;
	}

	public final void setMeterMaxValue(float meterMaxValue) {
		this.meterMaxValue = meterMaxValue;
	}

	public final float getLength() {
		return length;
	}

	public final void setLength(float length) {
		this.length = length;
	}

	public final Drawable getBarDrawable() {
		return barDrawable;
	}

	public final void setBarDrawable(Drawable barDrawable) {
		this.barDrawable = barDrawable;
	}

	public final MeterStyle getMeterStyle() {
		return meterStyle;
	}

	public final void setMeterStyle(MeterStyle meterStyle) {
		this.meterStyle = meterStyle;
	}

	public final int[] getMeterColors() {
		return meterColors;
	}

	/**
     *
     * @return
     */
    public float getMeterValue() {
        return meterValue;
    }

    // ADT Debug logging support
    
    static LinkedList<String> lines = new LinkedList<String>();
    static int msgCount = 0;
    
    // this is useful to get debug logging from ADT layout editor.  If there is a textview in the layout with tag 'eclipseLog', then debug messages will be written to it.
    static void adtLog(View v, String msg) {
    	if (!v.isInEditMode())
    		return;

    	try {
    		TextView tv = (TextView)((ViewGroup)v.getRootView()).findViewWithTag("eclipseLog");
    		tv.setVisibility(View.VISIBLE);

    		lines.addFirst(String.valueOf(msgCount) + ":" + msg);
    		if (lines.size() > 20)
    			lines.removeLast();
    		msgCount++;
    		
    		String str = "";
    		for (String s: lines) {
    			if(s.length() > 0)
    				s += '\n';
    			str += s;
    		}
    		tv.setText(str);
    	} catch (Exception e) {}
    }
    
    String getModeString(int specMode) {
    	switch (specMode) {
    		case MeasureSpec.UNSPECIFIED:
    			return "UNSPECIFIED";
    		case MeasureSpec.EXACTLY:
    			return "EXACTLY";
    		case MeasureSpec.AT_MOST:
    			return "AT_MOST";
    	}
    	return "UNKNOWN";
    }
}
