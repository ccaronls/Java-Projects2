package cc.lib.android;

import android.annotation.TargetApi;
import android.content.Context;
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
	
	public CCNumberPicker(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public CCNumberPicker(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public CCNumberPicker(Context context) {
		super(context);
		init();
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
	    for (int i=0; i<values.length; i++) {
	        if (value.equals(values[i])) {
	            np.setValue(i);
	            break;
            }
        }
        np.setDisplayedValues(values);
	    return np;
    }
}
