package cc.lib.android;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.NumberPicker;

import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;

/**
 * Created by Chris Caron on 12/14/21.
 */
public final class BindingAdapters {

    @BindingAdapter("comment")
    public static void setComment(View view, String comment) {
    }


    @BindingAdapter("onCheckChanged")
    public static void setOnCheckChanged(CompoundButton cb, CompoundButton.OnCheckedChangeListener listener) {
        cb.setOnCheckedChangeListener(listener);
    }

    /**
     * Set visible or invisible
     * @param view
     * @param predicate
     */
    @BindingAdapter("visibleIf")
    public static void setVisibleIf(View view, boolean predicate) {
        view.setVisibility(predicate ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Set visible or invisible
     * @param view
     * @param predicate
     */
    @BindingAdapter("visibleIfNot")
    public static void setVisibleIfNot(View view, boolean predicate) {
        view.setVisibility(!predicate ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Set visible or gone
     * @param view
     * @param predicate
     */
    @BindingAdapter("goneIf")
    public static void setGoneIf(View view, boolean predicate) {
        view.setVisibility(predicate ? View.GONE : View.VISIBLE);
    }

    /**
     * Set visible or gone
     * @param view
     * @param predicate
     */
    @BindingAdapter("goneIfNot")
    public static void setGoneIfNot(View view, boolean predicate) {
        view.setVisibility(!predicate ? View.GONE : View.VISIBLE);
    }

    @BindingAdapter("enabledIf")
    public static void setEnabledIf(View view, boolean predicate) {
        view.setEnabled(predicate);
    }

    @BindingAdapter("enabledIfNot")
    public static void setEnabledIfNot(View view, boolean predicate) {
        view.setEnabled(!predicate);
    }

    @BindingAdapter("adapter")
    public static void setAdapter(ListView view, ListAdapter adapter) {
        view.setAdapter(adapter);
    }

    @BindingAdapter("bind:minValue")
    public static void setNPMinValue(NumberPicker np, int minValue) {
        np.setMinValue(minValue);
    }

    @BindingAdapter("bind:maxValue")
    public static void setNPMaxValue(NumberPicker np, int maxValue) {
        np.setMaxValue(maxValue);
    }

    @BindingAdapter("bind:formatter")
    public static void setNPFormatter(NumberPicker np, NumberPicker.Formatter formatter) {
        int num = np.getMaxValue()+1-np.getMinValue();
        int idx = 0;
        String [] values = new String[num];
        for (int i=np.getMinValue(); i<= np.getMaxValue(); i++) {
            values[idx++] = formatter.format(i);
        }
        np.setDisplayedValues(values);
        //np.setFormatter(formatter); <-- this way causes visual glitches
    }

    @BindingAdapter("bind:value")
    public static void setNPValue(NumberPicker np, int value) {
        if (np.getValue() != value) { // break inf loops
            np.setValue(value);
        }
    }

    @InverseBindingAdapter(attribute = "bind:value")
    public static int getNPValue(NumberPicker np) {
        return np.getValue();
    }

    @BindingAdapter("valueAttrChanged")
    public static void setNPAttrListeners(NumberPicker np, InverseBindingListener attrChange) {
        np.setOnValueChangedListener((picker, oldVal, newVal) -> attrChange.onChange());
    }

    @BindingAdapter("onLongClick")
    public static void setOnLongClickListener(View view, View.OnClickListener listener) {
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                listener.onClick(v);
                return true;
            }
        });
    }
}
