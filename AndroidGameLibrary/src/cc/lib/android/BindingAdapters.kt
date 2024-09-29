package cc.lib.android

import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import cc.lib.game.Utils

/**
 * Created by Chris Caron on 12/14/21.
 */
@BindingAdapter("comment")
fun setComment(view: View?, comment: String?) {
}

@BindingAdapter("onCheckChanged")
fun setOnCheckChanged(cb: CompoundButton, listener: CompoundButton.OnCheckedChangeListener?) {
	cb.setOnCheckedChangeListener(listener)
}

/**
 * Set visible or invisible
 * @param view
 * @param predicate
 */
@BindingAdapter("visibleIf")
fun setVisibleIf(view: View, predicate: Boolean) {
	view.visibility = if (predicate) View.VISIBLE else View.INVISIBLE
}

/**
 * Set visible or invisible
 * @param view
 * @param predicate
 */
@BindingAdapter("visibleIfNot")
fun setVisibleIfNot(view: View, predicate: Boolean) {
	view.visibility = if (!predicate) View.VISIBLE else View.INVISIBLE
}

/**
 * Set visible or gone
 * @param view
 * @param predicate
 */

@BindingAdapter("goneIf")
fun setGoneIf(view: View, predicate: Boolean) {
	view.visibility = if (predicate) View.GONE else View.VISIBLE
}

/**
 * Set visible or gone
 * @param view
 * @param predicate
 */

@BindingAdapter("goneIfNot")
fun setGoneIfNot(view: View, predicate: Boolean) {
	view.visibility = if (!predicate) View.GONE else View.VISIBLE
}


@BindingAdapter("enabledIf")
fun setEnabledIf(view: View, predicate: Boolean) {
	view.isEnabled = predicate
}

@BindingAdapter("enabledIfNot")
fun setEnabledIfNot(view: View, predicate: Boolean) {
	view.isEnabled = !predicate
}


@BindingAdapter("enableAllIf")
fun setAllEnabledIf(view: View, predicate: Boolean) {
	view.isEnabled = predicate
	if (view is ViewGroup) {
		val vg = view
		for (i in 0 until vg.childCount) {
			setAllEnabledIf(vg.getChildAt(i), predicate)
		}
	}
}


@BindingAdapter("adapter")
fun setAdapter(view: ListView, adapter: ListAdapter?) {
	view.adapter = adapter
}

@BindingAdapter("onLongClick")
fun setOnLongClickListener(view: View, listener: View.OnClickListener) {
	view.setOnLongClickListener { v ->
		listener.onClick(v)
		true
	}
}

@BindingAdapter(value = ["textOrAlternate", "alternate"], requireAll = true)
fun setTextOrAlternate(tv: TextView, text: String?, alternateText: String?) {
	tv.text = if (Utils.isEmpty(text)) alternateText else text
}

@BindingAdapter("imageResId")
fun setImageResId(iv: ImageView, id: Int) {
	if (id > 0) {
		iv.setImageResource(id)
	} else {
		iv.setImageDrawable(null)
	}
}

@BindingAdapter("textResId")
fun setTextResId(iv: TextView, id: Int) {
	if (id > 0) {
		iv.setText(id)
	} else {
		iv.text = null
	}
}

@BindingAdapter("clickableIf")
fun setClickableIf(view: View, clickable: Boolean) {
	view.isClickable = clickable
}

@BindingAdapter("onFocussed")
fun setOnFocussedListener(view: View, listener: View.OnClickListener) {
	view.onFocusChangeListener = OnFocusChangeListener { v, hasFocus -> if (hasFocus) listener.onClick(view) }
}

@BindingAdapter("textColorInt")
fun setTextColor(view: TextView, color: Int) {
	view.setTextColor(color)
}

@BindingAdapter("activatedIf")
fun View.setActivatedIf(active: Boolean) {
	isActivated = active
}
