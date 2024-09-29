package cc.lib.android;

import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.databinding.BindingAdapter;
import cc.lib.game.Utils;

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

    @BindingAdapter("enableAllIf")
    public static void setAllEnabledIf(View view, boolean predicate) {
        view.setEnabled(predicate);
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                setAllEnabledIf(vg.getChildAt(i), predicate);
            }
        }
    }

    @BindingAdapter("adapter")
    public static void setAdapter(ListView view, ListAdapter adapter) {
        view.setAdapter(adapter);
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

    @BindingAdapter(value = {"textOrAlternate", "alternate"}, requireAll = true)
    public static void setTextOrAlternate(TextView tv, String text, String alternateText) {
        tv.setText(Utils.isEmpty(text) ? alternateText : text);
    }

    @BindingAdapter("imageResId")
    public static void setImageResId(ImageView iv, int id) {
        if (id > 0) {
            iv.setImageResource(id);
        } else {
            iv.setImageDrawable(null);
        }
    }

    @BindingAdapter("textResId")
    public static void setTextResId(TextView iv, int id) {
        if (id > 0) {
            iv.setText(id);
        } else {
            iv.setText(null);
        }
    }

    @BindingAdapter("clickableIf")
    public static void setClickableIf(View view, boolean clickable) {
        view.setClickable(clickable);
    }

    @BindingAdapter("onFocussed")
    public static void setOnFocussedListener(View view, View.OnClickListener listener) {
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    listener.onClick(view);
            }
        });
    }

    @BindingAdapter("textColorInt")
    public static void setTextColor(TextView view, int color) {
        view.setTextColor(color);
    }
}
