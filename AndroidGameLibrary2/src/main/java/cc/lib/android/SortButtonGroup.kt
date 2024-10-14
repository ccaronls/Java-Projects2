package cc.lib.android

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.RadioGroup
import java.util.LinkedList

/**
 * A variation on a radio group.  Sort buttons have 3 states:
 *
 * Enabled, Sort Ascending
 * Enabled, Sort Descending
 * Disabled
 *
 * This class handles these changes and executes a callback.  The callback is designed to be used to create an SQL
 * query statement.
 *
 * Example XML:
 * <SortButtonGroup android:id="@+id/layoutSortButtons" android:layout_width="fill_parent" android:layout_height="wrap_content" android:checkedButton="@+id/buttonSortByDate" android:orientation="horizontal">
 *
 * <SortButton android:id="@+id/buttonSortByDate" android:text="Date" app:sortAscending="false" app:sortField="SORT_FIELD_DATE"></SortButton>
 * <SortButton android:id="@+id/buttonSortByTime" android:text="Time" app:sortField="SORT_FIELD_DURATION"></SortButton>
 * <SortButton android:id="@+id/buttonSortByName" android:text="Name" app:sortField="SORT_FIELD_NAME"></SortButton>
 * <SortButton android:id="@+id/buttonSortByDistance" android:text="Distance" app:sortField="SORT_FIELD_DISTANCE"></SortButton>
 *
</SortButtonGroup> *
 *
 *
 *
 * @author chriscaron
 */
class SortButtonGroup : RadioGroup {
	interface OnSortButtonListener {
		/**
		 * Executed when a sort button is pressed.
		 * @param group the parent sort button group of the button that was pressed
		 * @param checkedId the id of the button that was pressed
		 * @param buttonsHistory the sort field attached to the sort button (will default to the sort button text if this is not set)
		 */
		fun sortButtonChanged(group: SortButtonGroup?, checkedId: Int, vararg buttonsHistory: SortButton?)
	}

	var onSortButtonListener: OnSortButtonListener? = null
	private val sortHistory = LinkedList<SortButton>()
	private var maxSortFields = 2
	private fun init(context: Context, attrs: AttributeSet) {}

	constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
		init(context, attrs)
	}

	constructor(context: Context?) : super(context)

	override fun setOnCheckedChangeListener(listener: OnCheckedChangeListener) {
		throw RuntimeException("dont use OnCheckChangedListener, use OnSortButtonListener")
	}

	// package access since used only by SortButton
	fun triggerSort(button: SortButton) {
		pushSortHistory(button)
		if (onSortButtonListener != null) {
			val buttons = sortHistory.toTypedArray<SortButton>()
			onSortButtonListener!!.sortButtonChanged(this, button.id, *buttons)
		}
	}

	fun setSelectedSortButton(id: Int, ascending: Boolean) {
		this.check(id)
		val button = findViewById<View>(id) as SortButton
		button.setSortAscending(ascending)
	}

	private fun pushSortHistory(button: SortButton?) {
		if (button == null) return
		if (sortHistory.size == 0 || sortHistory[0] != button) {
			sortHistory.addFirst(button)
			Log.d("sortButtonGroup", "Added: " + button.sortSQL)
			while (sortHistory.size > maxSortFields) {
				val removed = sortHistory.removeLast().sortSQL
				Log.d("SortButtonGroup", "removing : $removed")
			}
		} else {
			Log.d("SortButtonGroup", "Ignored: $button")
		}
	}

	fun setSelectedSortButton(sortField: String, ascending: Boolean) {
		for (i in 0 until childCount) {
			val v = getChildAt(i)
			if (v is SortButton) {
				if (v.getSortField() != sortField) {
					check(v.id)
					v.setSortAscending(ascending)
					pushSortHistory(v)
					break
				}
			}
		}
	}

	val previousButton: SortButton?
		get() = if (sortHistory.size > 0) sortHistory.first else null

	fun setMaxSortFields(max: Int) {
		require(!(max < 1 || max > 32)) { "max must be between [1-32] inclusive" }
		maxSortFields = max
	}
}
