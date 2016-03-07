package cc.lib.android;

import android.app.Activity;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

/**
 * Container class for managing 3 number pickers with a radio button.  Typically the radio button toggles units
 * for instance from metric to imperial.  See HeightPicker and WeightPicker
 * 
 * @author chriscaron
 *
 */
public class NumberPickerController implements OnValueChangeListener {

	public static interface OnValueChangedListener {
		void onValueChanged(int leftValue, int centerValue, int rightValue);
	};
	
	private final Activity context;
	private final View view;
	private final RadioGroup units;
	private final NumberPicker npLeft;
	private final NumberPicker npCenter;
	private final NumberPicker npRight;
	private final RadioButton radioLeft;
	private final RadioButton radioRight;
	
	private OnValueChangedListener listener;
	
	NumberPickerController(Activity context) {
		this(context, true);
	}
	
	public NumberPickerController(Activity context, boolean attachFormatter) {
		this.context = context;
		view = View.inflate(context, R.layout.popup_value_picker, null);
		units = (RadioGroup)view.findViewById(R.id.radioGroupUnits);
		npLeft = (NumberPicker)view.findViewById(R.id.numberPickerLeft);
		npCenter = (NumberPicker)view.findViewById(R.id.numberPickerCenter);
		npRight  = (NumberPicker)view.findViewById(R.id.numberPickerRight);
		if (attachFormatter) {
        	npLeft.setFormatter(new NumberPicker.Formatter() {
        		public String format(int value) {
        			return formatLeftPicker(value);
        		}
        	});
		}
    	npLeft.setOnValueChangedListener(this);
    	if (attachFormatter) {
            npCenter.setFormatter(new NumberPicker.Formatter() {
        		public String format(int value) {
        			return formatCenterPicker(value);
        		}
        	});
    	}
    	npCenter.setOnValueChangedListener(this);
    	if (attachFormatter) {
        	npRight.setFormatter(new NumberPicker.Formatter() {
        		public String format(int value) {
        			return formatRightPicker(value);
        		}
        	});
    	}
    	npRight.setOnValueChangedListener(this);
    	radioLeft = (RadioButton)view.findViewById(R.id.radioButtonLeft);
    	radioRight = (RadioButton)view.findViewById(R.id.radioButtonRight);
    	units.setOnCheckedChangeListener(new OnCheckedChangeListener() {
    		@Override
    		public void onCheckedChanged(RadioGroup group, int checkedId) {
    			if (checkedId == R.id.radioButtonLeft) {
    				onRadioLeftChecked(NumberPickerController.this);
    			} else if (checkedId == R.id.radioButtonRight) {
    				onRadioRightChecked(NumberPickerController.this);
    			}
    		}
    	});
	}
	
	/**
	 * Override to add logic when left radio button checked
	 * @param con
	 */
	protected void onRadioLeftChecked(NumberPickerController con) {}
	
	/**
	 * Override to add logic when right radio button checked
	 * @param con
	 */
	protected void onRadioRightChecked(NumberPickerController con) {}
	
	/**
	 * Override to customize left picker string format
	 * @param value
	 * @return
	 */
	protected String formatLeftPicker(int value) { return formatPicker(value); }
	
	/**
	 * Override to customize center picker string format
	 * @param value
	 * @return
	 */
	protected String formatCenterPicker(int value) { return formatPicker(value); }
	
	/**
	 * Override to customize right picker string format
	 * @param value
	 * @return
	 */
	protected String formatRightPicker(int value) { return formatPicker(value); }
	
	/**
	 * Default formatter for left/center/right number pickers
	 * @param value
	 * @return
	 */
	protected String formatPicker(int value) { return String.valueOf(value); }
	
	protected final Activity getContext() {
		return context;
	}
	
	public final View getView() {
		return view;
	}

	public final RadioGroup getUnits() {
		return units;
	}

	public final NumberPicker getNpLeft() {
		return npLeft;
	}

	public final NumberPicker getNpCenter() {
		return npCenter;
	}

	public final NumberPicker getNpRight() {
		return npRight;
	}

	public final RadioButton getRadioLeft() {
		return radioLeft;
	}

	public final RadioButton getRadioRight() {
		return radioRight;
	}

	synchronized public final void checkLeft() { 
		units.clearCheck();
		units.check(R.id.radioButtonLeft); 
	}
	
	synchronized public final void checkRight() { 
		units.clearCheck();
		units.check(R.id.radioButtonRight); 
	}
	
	public final void setLeftRange(int min, int max, int value) {
		npLeft.setMinValue(min);
		npLeft.setMaxValue(max);
		npLeft.setValue(value);
	}

	public final void setCenterRange(int min, int max, int value) {
		npCenter.setMinValue(min);
		npCenter.setMaxValue(max);
		npCenter.setValue(value);
	}

	public final void setRightRange(int min, int max, int value) {
		npRight.setMinValue(min);
		npRight.setMaxValue(max);
		npRight.setValue(value);
	}

	public final void setRadioText(int leftResId, int rightResId) {
		radioLeft.setText(leftResId);
		radioRight.setText(rightResId);
	}
	
	synchronized public final void setOnValueChangedListener(OnValueChangedListener listener) {
		this.listener = listener;
	}

	public final void setWrapSelectorWheels(boolean wrap) {
		npLeft.setWrapSelectorWheel(wrap);
		npCenter.setWrapSelectorWheel(wrap);
		npRight.setWrapSelectorWheel(wrap);
	}
	
	public final void setVisibility(int left, int center, int right) {
	    npLeft.setVisibility(left);
	    npLeft.setValue(npLeft.getValue());
        npCenter.setVisibility(center);
        npCenter.setValue(npCenter.getValue());
        npRight.setVisibility(right);
        npRight.setValue(npRight.getValue());
	}

	@Override
	synchronized public final void onValueChange(NumberPicker picker, int oldVal, int newVal) {
		if (listener != null) {
			listener.onValueChanged(npLeft.getValue(), npCenter.getValue(), npRight.getValue());
		}
	}


}
