package cc.lib.android;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.databinding.BindingAdapter;

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

    @BindingAdapter("visibleIf")
    public static void setVisibleIf(View view, boolean predicate) {
        view.setVisibility(predicate ? View.VISIBLE : View.INVISIBLE);
    }

    @BindingAdapter("visibleIfNot")
    public static void setVisibleIfNot(View view, boolean predicate) {
        view.setVisibility(!predicate ? View.VISIBLE : View.INVISIBLE);
    }

    @BindingAdapter("goneIf")
    public static void setGoneIf(View view, boolean predicate) {
        view.setVisibility(predicate ? View.GONE : View.VISIBLE);
    }

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

}
