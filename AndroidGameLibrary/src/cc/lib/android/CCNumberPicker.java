package cc.lib.android;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.NumberPicker;

public class CCNumberPicker extends NumberPicker {

	EditText et;

	private boolean findET(ViewGroup V) {
		for (int i=0; i<V.getChildCount(); i++) {
			View v = getChildAt(i);
			if (v instanceof EditText) {
				et = (EditText)v;
				return true;
			} else if (v instanceof ViewGroup) {
				if (findET((ViewGroup)v))
					return true;
			}
		}
		return false;
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void init() {
		if (findET(this)) {
			et.setFilters(new InputFilter[0]);
		}
		if (Build.VERSION.SDK_INT >= 11)
			setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
	}

	private void init(Context c, AttributeSet attrs) {
        TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.CCNumberPicker);
        setWrapSelectorWheel(a.getBoolean(R.styleable.CCNumberPicker_wrap, true));
        a.recycle();
        init();
    }

	public CCNumberPicker(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	public CCNumberPicker(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public CCNumberPicker(Context context) {
		super(context);
		init();
	}
    public static NumberPicker newPicker(Context c, int num, int min, int max, int step, OnValueChangeListener listener) {
	    if (step < 1)
	        throw new AssertionError("Invalid value for step : " +step);
	    if (step == 1)
	        return newPicker(c, num, min, max, listener);
	    int count = (max-min)/step+1;
	    String [] values = new String[count];
	    for (int i=0; i<values.length; i++) {
	        values[i] = String.valueOf(min + step*i);
        }
	    return newPicker(c, "" + num, values, listener);
    }

	public static NumberPicker newPicker(Context c, int num, int min, int max, OnValueChangeListener listener) {
        CCNumberPicker np = new CCNumberPicker(c);
        np.setMinValue(min);
        np.setMaxValue(max);
        np.setValue(num);
        np.setOnValueChangedListener(listener);
        return np;
    }

    public static NumberPicker newPicker(Context c, String value, String [] values, OnValueChangeListener listener) {
        CCNumberPicker np = new CCNumberPicker(c);
        np.setDisplayedValues(values);
	    np.setMinValue(0);
	    np.setMaxValue(values.length-1);
        for (int i=0; i<values.length; i++) {
            if (value.equals(values[i])) {
                np.setValue(i);
                break;
            }
        }
	    np.setOnValueChangedListener(listener);
	    return np;
    }

    public void init(final int [] values, int startValue, Formatter formatter, OnValueChangeListener listener) {
	    setOnValueChangedListener(listener);
	    setMinValue(0);
	    setMaxValue(values.length-1);
        String [] display = new String[values.length];
        for (int i=0; i<values.length; i++) {
            if (startValue >= values[i])
                setValue(i);
            display[i] = formatter == null ? String.valueOf(values[i]) :
                    formatter.format(values[i]);
        }
        setDisplayedValues(display);
    }
}
