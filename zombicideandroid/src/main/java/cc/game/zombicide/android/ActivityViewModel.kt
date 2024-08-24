package cc.game.zombicide.android

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * Created by Chris Caron on 12/13/21.
 */
class ActivityViewModel : ViewModel() {
    val consoleVisible = MutableLiveData(true)
    val loading = MutableLiveData(false)
    val playing = MutableLiveData(false)
	val processingMove = MutableLiveData(false)

	// TODO: Move this to ViewModel
	class ButtonAdapter internal constructor() : BaseAdapter() {
		private val buttons: MutableList<View>
		fun update(buttons: List<View>) {
			this.buttons.clear()
			this.buttons.addAll(buttons)
			notifyDataSetChanged()
		}

		override fun isEnabled(position: Int): Boolean {
			return buttons[position].isEnabled
		}

		override fun getCount(): Int {
			return buttons.size
		}

		override fun getItem(position: Int): Any {
			return 0
		}

		override fun getItemId(position: Int): Long {
			return 0
		}

		override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
			return buttons[position]
		}

		override fun areAllItemsEnabled(): Boolean = false

		init {
			buttons = ArrayList()
		}
	}

    val listAdapter = ButtonAdapter()
}