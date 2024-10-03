package cc.lib.android

import androidx.databinding.ObservableList
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * Created by Chris Caron on 4/11/24.
 */
abstract class ObservableListAdapter<VH : ViewHolder, T>(val list: ObservableList<T>) :
	RecyclerView.Adapter<VH>() {

	private val listChangeCallback =
		object : ObservableList.OnListChangedCallback<ObservableList<T>>() {
			override fun onChanged(sender: ObservableList<T>?) {
				notifyDataSetChanged()
				onUpdated()
			}

			override fun onItemRangeChanged(
				sender: ObservableList<T>,
				positionStart: Int,
				itemCount: Int
			) {
				notifyItemRangeChanged(positionStart, itemCount)
				onUpdated()
			}

			override fun onItemRangeInserted(
				sender: ObservableList<T>,
				positionStart: Int,
				itemCount: Int
			) {
				notifyItemRangeInserted(positionStart, itemCount)
				onUpdated()
			}

			override fun onItemRangeMoved(
				sender: ObservableList<T>,
				fromPosition: Int,
				toPosition: Int,
				itemCount: Int
			) {
				notifyItemRangeChanged(fromPosition, itemCount)
				notifyItemRangeChanged(toPosition, itemCount)
				onUpdated()
			}

			override fun onItemRangeRemoved(
				sender: ObservableList<T>,
				positionStart: Int,
				itemCount: Int
			) {
				notifyItemRangeRemoved(positionStart, itemCount)
				onUpdated()
			}
		}

	init {
		list.addOnListChangedCallback(listChangeCallback)
	}

	open fun onUpdated() {}

	override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
		list.removeOnListChangedCallback(listChangeCallback)
		super.onDetachedFromRecyclerView(recyclerView)
	}
}