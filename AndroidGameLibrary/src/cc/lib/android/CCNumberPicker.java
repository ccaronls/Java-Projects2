package cc.lib.android;

import android.content.Context;
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
	
	private void init() {
		if (findET(this)) {
			et.setFilters(new InputFilter[0]);
		}
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
}
