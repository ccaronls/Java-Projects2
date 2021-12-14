package cc.lib.android;

import android.view.View;
import android.widget.CompoundButton;

import androidx.databinding.BindingAdapter;

/**
 * Created by Chris Caron on 12/14/21.
 */
public final class BindingAdapters {

    @BindingAdapter("onCheckChanged")
    public static void setOnCheckChanged(CompoundButton cb, CompoundButton.OnCheckedChangeListener listener) {
        cb.setOnCheckedChangeListener(listener);
    }

    @BindingAdapter("goneIf")
    public static void setGoneIf(View view, boolean predicate) {
        view.setVisibility(predicate ? View.GONE : View.VISIBLE);
    }

    @BindingAdapter("goneIfNot")
    public static void setGoneIfNot(View view, boolean predicate) {
        view.setVisibility(!predicate ? View.GONE : View.VISIBLE);
    }

}
