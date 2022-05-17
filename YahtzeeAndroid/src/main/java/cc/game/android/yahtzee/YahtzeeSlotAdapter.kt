package cc.game.android.yahtzee

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import cc.lib.yahtzee.Yahtzee

internal class YahtzeeSlotAdapter(val activity: YahtzeeActivity, val yahtzee: Yahtzee) : BaseAdapter() {
	override fun getCount(): Int {
		return yahtzee.allSlots.size
	}

	override fun getItem(position: Int): Any? {
		// TODO Auto-generated method stub
		return null
	}

	override fun getItemId(position: Int): Long {
		// TODO Auto-generated method stub
		return 0
	}

	override fun getView(position: Int, view: View?, parent: ViewGroup): View {
		var view = view?:View.inflate(activity, R.layout.yahtzeeslotlistitem, null)
		val tvName = view.findViewById<View>(R.id.textViewSlotName) as TextView
		val tvPts = view.findViewById<View>(R.id.textViewSlotPoints) as TextView
		val ivUnavail = view.findViewById<View>(R.id.imageViewUnavailable) as ImageView
		val slot = yahtzee.allSlots[position]
		view.tag = slot
		val used = yahtzee.isSlotUsed(slot)
		view.isEnabled = !used
		tvName.text = slot.niceName
		tvPts.text = "${if (used) yahtzee.getSlotScore(slot) else slot.getScore(yahtzee)}"
		ivUnavail.visibility = if (used) View.VISIBLE else View.INVISIBLE
		return view
	}

	fun postNotifyDataSetChanged() {
		activity.runOnUiThread { notifyDataSetChanged() }
	}
}