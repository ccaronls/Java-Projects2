package cc.lib.android

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatRadioButton

/**
 * See SortButtonGroup for details on how to use.
 *
 * @author chriscaron
 */
class SortButton(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
	AppCompatRadioButton(context, attrs, defStyle), View.OnClickListener {
	init {
		attrs?.let {
			val arr = context.obtainStyledAttributes(it, R.styleable.SortButton)
			sortAscending = arr.getBoolean(R.styleable.SortButton_sortAscending, false)
			sortField = arr.getString(R.styleable.SortButton_sortField)
			arr.recycle()
		}
		super.setOnClickListener(this)
	}

	private var sortField: String? = null
	private var sortAscending = false

	fun isSortAscending(): Boolean {
		return sortAscending
	}

	fun setSortAscending(sortAscending: Boolean) {
		this.sortAscending = sortAscending
		invalidate()
	}

	fun getSortField(): String = sortField ?: text.toString()

	val sortSQL: String
		get() = getSortField() + if (isSortAscending()) " ASC" else " DESC"

	fun setSortField(sortField: String?) {
		this.sortField = sortField
	}

	override fun onCreateDrawableState(extraSpace: Int): IntArray {
		val state = super.onCreateDrawableState(extraSpace + 1)
		if (isSortAscending()) mergeDrawableStates(state, SORT_ASCENDING)
		return state
	}

	override fun onClick(v: View) {
		val parent = parent as SortButtonGroup
		if (parent.previousButton == this) {
			sortAscending = !sortAscending
		} else if (parent.previousButton != null) {
			sortAscending = parent.previousButton!!.isSortAscending()
		}
		parent.triggerSort(this)
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		if (parent !is SortButtonGroup) {
			throw RuntimeException("SortButton can only be a child of SortButtonGroup")
		}
	}

	override fun setOnClickListener(listener: OnClickListener?) {
		throw RuntimeException("Cannot override onClickListener of SortButton, use SortButtonGroup.OnSortButtonListener instead")
	}

	companion object {
		private val SORT_ASCENDING = intArrayOf(R.attr.sortAscending)
	}
}
