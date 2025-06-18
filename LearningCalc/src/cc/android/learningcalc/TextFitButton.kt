package cc.android.learningcalc;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.Button;

public class TextFitButton extends Button {

	public TextFitButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public TextFitButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TextFitButton(Context context) {
		super(context);
	}


    /* Re size the font so the specified text fits in the text box
     * assuming the text box is the specified width.
     */
    private void refitText(String text, int textWidth) {
        if (textWidth > 0) {
            int availableWidth = textWidth - this.getPaddingLeft() - this.getPaddingRight();
            float trySize = maxTextSize;

            // Using setTextSize on the paint object directly, or on a clone
            // of that paint object, does not work -- the measurements come
            // out wrong.  Instead, call the textview's setTextSize, which
            // will propogate the necessary info.

            // getTextSize returns pixels in device-specific units.
            // setTextSize expects pixels in scaled-pixel units, by default.
            // Specify TypedValues.COMPLEX_UNIT_PX so that setTextSize will
            // work with the same numbers that we get from getTextSize.
            // (An alternative solution would be to convert the value we get
            // from getTextSize into scaled-pixel units.)

            setTextSize(TypedValue.COMPLEX_UNIT_PX, trySize);
            while ((trySize > minTextSize) && enableMultiline ? !textFits(text, availableWidth) : getPaint().measureText(text) > availableWidth) { 
                trySize -= 1;
                if (trySize <= minTextSize) {
                    trySize = minTextSize;
                    break;
                }
                setTextSize(TypedValue.COMPLEX_UNIT_PX, trySize);
            }
        }
    }
    
    private boolean textFits(String text, int maxWidth)  {
    	
    	int numLines = 1;
    	if (getHeight() > 0) {
    		numLines = (int)((float)getHeight() / (getPaint().getTextSize()+getPaint().getFontMetrics().leading+getPaint().getFontMetrics().bottom));
    	}
    	
    	if (numLines == 1) {
    		return getPaint().measureText(text) <= maxWidth;
    	}
    	for (int i=0; i<numLines; i++) {
    		int numChars = getPaint().breakText(text, true, maxWidth, null);
    		if (numChars >= text.length())
    			return true;
    		text = text.substring(numChars);
    	}
    	return false;
    }

    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int before, final int after) {
        refitText(text.toString(), this.getWidth());
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        if (w != oldw) {
            refitText(this.getText().toString(), w);
        }
    }

    //Getters and Setters
    public float getMinTextSize() {
        return minTextSize;
    }

    public void setMinTextSize(int minTextSize) {
        this.minTextSize = minTextSize;
    }

    public float getMaxTextSize() {
        return maxTextSize;
    }

    public void setMaxTextSize(int minTextSize) {
        this.maxTextSize = minTextSize;
    }
    
    public void setEnabledMultiline(boolean enable) {
    	this.enableMultiline = enable;
    }

    //Attributes
    private float minTextSize = 11;
    private float maxTextSize = 20;
    private boolean enableMultiline = false;

	
	
}
