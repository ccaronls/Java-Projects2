package cecc.android.lib;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.NumberPicker.Formatter;
import android.widget.NumberPicker.OnValueChangeListener;
import cc.lib.android.R;
import cc.lib.utils.Convert;

public abstract class TemperatureChooserView implements OnClickListener, OnValueChangeListener, Formatter {

	private final CECCBaseActivity context;
	private final NumberPicker npDegrees;
	private final NumberPicker npCelcius;
	private final NumberPicker npCelciusDec;
	private final NumberPicker npUnits;
	private final View tvDot;
	
	public TemperatureChooserView(CECCBaseActivity context, float tempCelcius) {
		this.context = context;
		View v = View.inflate(context, R.layout.popup_temp_picker, null);
		npDegrees = (NumberPicker)v.findViewById(R.id.numberPickerDegrees);
		npCelcius = (NumberPicker)v.findViewById(R.id.numberPickerCelcius);
		npCelciusDec = (NumberPicker)v.findViewById(R.id.numberPickerCelciusDecimal);
		npUnits = (NumberPicker)v.findViewById(R.id.numberPickerUnits);
		tvDot = v.findViewById(R.id.tvDot);
		
		npDegrees.setMinValue(30);
		npDegrees.setMaxValue(100);
		npDegrees.setWrapSelectorWheel(false);
		npCelcius.setMinValue(0);
		npCelcius.setMaxValue(40);
		npCelcius.setWrapSelectorWheel(false);
		npCelcius.setFormatter(this);
		npCelciusDec.setMinValue(0);
		npCelciusDec.setMaxValue(9);
		npCelciusDec.setWrapSelectorWheel(false);
		
		npUnits.setDisplayedValues(new String [] { "C", "F" });
		npUnits.setMinValue(0);
		npUnits.setMaxValue(1);
		
		npUnits.setOnValueChangedListener(this);
		
		context.newDialogBuilder().setView(v).setNegativeButton(R.string.popup_button_cancel, null)
			.setPositiveButton(R.string.popup_button_ok, this).show();
		
		setTempPickers(tempCelcius);
	}
	
	private void setTempPickers(float tempCelcius) {
		int tempDegrees = Convert.celciusToDegrees(tempCelcius);
		int tempCelciusHigh = (int)tempCelcius;
		int tempCelciusLow  = (int)(10f * (tempCelcius - tempCelciusHigh));
		npUnits.setValue(context.isMetricUnits() ? 0 : 1);
		
		if (context.isMetricUnits()) {
			npDegrees.setVisibility(View.GONE);
			npCelcius.setVisibility(View.VISIBLE);
			npCelciusDec.setVisibility(View.VISIBLE);
			tvDot.setVisibility(View.VISIBLE);
		} else {
			npDegrees.setVisibility(View.VISIBLE);
			npCelcius.setVisibility(View.GONE);
			npCelciusDec.setVisibility(View.GONE);
			tvDot.setVisibility(View.GONE);
		}
		
		npDegrees.setValue(tempDegrees);
		npCelcius.setValue(tempCelciusHigh);
		npCelciusDec.setValue(tempCelciusLow);

	}
	
	public float getTemp() {
		if (context.isMetricUnits()) {
			return 0.1f * npCelciusDec.getValue() + npCelcius.getValue();
		} else {
			return npDegrees.getValue();
		}
	}

	@Override
	public String format(int value) {
		return String.valueOf(value);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		onTemperature(getTemp());
	}
	
	@Override
	public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
		float celcius = context.isMetricUnits() ? getTemp() : Convert.degreesToCelcius(getTemp());
		if (newVal == 0) {
			context.setUnitsMetric(true);
		} else {
			context.setUnitsMetric(false);
		}
		setTempPickers(celcius);
	}
	
	protected abstract void onTemperature(float temp);
}
