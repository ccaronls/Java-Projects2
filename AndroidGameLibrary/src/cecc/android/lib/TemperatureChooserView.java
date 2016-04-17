package cecc.android.lib;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.NumberPicker;
import android.widget.NumberPicker.Formatter;
import android.widget.ToggleButton;
import cc.lib.android.R;
import cc.lib.utils.Convert;

public abstract class TemperatureChooserView implements OnClickListener, Formatter, OnCheckedChangeListener {

	private final CECCBaseActivity context;
	private final NumberPicker npDegrees;
	private final NumberPicker npCelcius;
	private final NumberPicker npCelciusDec;
	private final ToggleButton tbUnits;
	
	public TemperatureChooserView(CECCBaseActivity context) {
		this.context = context;
		View v = View.inflate(context, R.layout.popup_temp_picker, null);
		npDegrees = (NumberPicker)v.findViewById(R.id.numberPickerDegrees);
		npCelcius = (NumberPicker)v.findViewById(R.id.numberPickerCelcius);
		npCelciusDec = (NumberPicker)v.findViewById(R.id.numberPickerCelciusDecimal);
		tbUnits = (ToggleButton)v.findViewById(R.id.tbUnits);
		
		npDegrees.setMinValue(30);
		npDegrees.setMaxValue(100);
		npDegrees.setWrapSelectorWheel(false);
		npCelcius.setMinValue(0);
		npCelcius.setMaxValue(40);
		npCelcius.setWrapSelectorWheel(false);
		npCelciusDec.setMinValue(0);
		npCelciusDec.setMaxValue(9);
		npCelciusDec.setWrapSelectorWheel(true);
		npCelciusDec.setFormatter(this);
		
		tbUnits.setChecked(context.isMetricUnits());
		tbUnits.setOnCheckedChangeListener(this);
		
		context.newDialogBuilder().setView(v).setNegativeButton(R.string.popup_button_cancel, null)
			.setPositiveButton(R.string.popup_button_ok, this).show();
		
		setTempPickers(getInitialTempCelcius());
	}
	
	protected abstract float getInitialTempCelcius();
	
	private void setTempPickers(float tempCelcius) {
		int tempDegrees = Convert.celciusToDegrees(tempCelcius);
		int tempCelciusHigh = (int)tempCelcius;
		int tempCelciusLow  = (int)(10f * (tempCelcius - tempCelciusHigh));
		
		if (tbUnits.isChecked()) {
			npDegrees.setVisibility(View.GONE);
			npCelcius.setVisibility(View.VISIBLE);
			npCelciusDec.setVisibility(View.VISIBLE);
		} else {
			npDegrees.setVisibility(View.VISIBLE);
			npCelcius.setVisibility(View.GONE);
			npCelciusDec.setVisibility(View.GONE);
		}
		
		npDegrees.setValue(tempDegrees);
		npCelcius.setValue(tempCelciusHigh);
		npCelciusDec.setValue(tempCelciusLow);

	}
	
	public final float getTemp() {
		if (context.isMetricUnits()) {
			return 0.1f * npCelciusDec.getValue() + npCelcius.getValue();
		} else {
			return npDegrees.getValue();
		}
	}
	
	public final float getTempCelcius() {
		float temp = getTemp();
		if (context.isMetricUnits())
			return temp;
		return Convert.degreesToCelcius(temp);
	}

	@Override
	public final String format(int value) {
		return "." + String.valueOf(value);
	}

	@Override
	public final void onClick(DialogInterface dialog, int which) {
		onTemperature(getTemp());
	}
	
	@Override
	public final void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		float celcius = context.isMetricUnits() ? getTemp() : Convert.degreesToCelcius(getTemp());
		if (isChecked) {
			context.setUnitsMetric(true);
		} else {
			context.setUnitsMetric(false);
		}
		setTempPickers(celcius);
	}

	/**
	 * Returns temperature in the desired units
	 * 
	 * @param temp
	 */
	protected abstract void onTemperature(float tempCelcius);
}
