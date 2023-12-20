package cc.game.zombicide.android

import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import cc.game.zombicide.android.databinding.SaveGameDialogBinding

/**
 * Created by Chris Caron on 8/17/21.
 */
class SaveGameDialog(val activity: ZombicideActivity, val maxSaves: Int) : BaseAdapter(), View.OnClickListener {
	val list: MutableList<Pair<String, String>> = ArrayList()
	val dialog: Dialog
	val sb: SaveGameDialogBinding = SaveGameDialogBinding.inflate(activity.layoutInflater)

	fun updateSaves() {
		list.clear()
		list.addAll(activity.saves.toList())
		sb.bSave.isEnabled = list.size < maxSaves
		while (list.size < maxSaves) {
			list.add(Pair("EMPTY", ""))
		}
	}

	override fun getCount(): Int {
		return list.size
	}

	override fun getItem(position: Int): Any {
		return 0
	}

	override fun getItemId(position: Int): Long {
		return 0
	}

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
		val view = convertView ?: View.inflate(activity, R.layout.save_game_dialog_item, null)
		val tv_text = view.findViewById<TextView>(R.id.tv_text)
		val b_delete = view.findViewById<View>(R.id.b_delete)
		val item = list[position]
		b_delete.tag = position
		b_delete.setOnClickListener(this)
		b_delete.visibility = if (item.second.isBlank()) View.GONE else View.VISIBLE
		tv_text.text = item.first
		return view
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.b_delete -> {
				val position = v.tag as Int
				val name = list[position].first
				activity.deleteSave(name)
				updateSaves()
				notifyDataSetChanged()
				sb.bSave.isEnabled = true
			}
			R.id.b_save -> {
				run {
					activity.saveGame()
					dialog.dismiss()
				}
				dialog.dismiss()
			}
			R.id.b_cancel -> dialog.dismiss()
		}
	}

	init {
		sb.bSave.setOnClickListener(this)
		sb.bCancel.setOnClickListener(this)
		updateSaves()
		sb.listView.isScrollContainer = false
		sb.listView.adapter = this
		dialog = activity.newDialogBuilder().setTitle("Save Game")
			.setView(sb.root).show()
	}
}