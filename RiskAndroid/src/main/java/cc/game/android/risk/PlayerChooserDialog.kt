package cc.game.android.risk

import android.content.DialogInterface
import android.view.View
import android.view.ViewGroup
import android.widget.*
import cc.lib.game.Utils
import cc.lib.risk.Army
import cc.lib.risk.RiskPlayer

class PL(var army: Army) {
	var checked = false
	var robot = false
}

/**
 * Created by Chris Caron on 9/17/21.
 */
class PlayerChooserDialog internal constructor(val context: RiskActivity) : BaseAdapter(), DialogInterface.OnClickListener {

	var players: MutableList<PL> = ArrayList()
	override fun getCount(): Int {
		return players.size
	}

	override fun getItem(position: Int): Any? {
		return null
	}

	override fun getItemId(position: Int): Long {
		return 0
	}

	override fun getView(position: Int, view: View?, parent: ViewGroup): View {
		val view = view?:View.inflate(context, R.layout.player_list_item, null)
		val cb = view.findViewById<CheckBox>(R.id.check_box)
		val tb = view.findViewById<ToggleButton>(R.id.toggle_button)
		cb.setOnCheckedChangeListener(null)
		tb.setOnCheckedChangeListener(null)
		val pl = players[position]
		cb.text = pl.army.name
		cb.isChecked = pl.checked
		tb.isChecked = pl.robot
		cb.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean -> pl.checked = !pl.checked }
		tb.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean -> pl.robot = !pl.robot }
		return view
	}

	override fun onClick(dialog: DialogInterface, which: Int) {
		if (Utils.count(players) { pl: PL -> pl.checked } < 2) {
			Toast.makeText(context, "Not enough players", Toast.LENGTH_LONG).show()
			return
		}
		val p: MutableList<RiskPlayer> = ArrayList()
		for (pl in players) {
			if (pl.checked) {
				if (pl.robot) {
					p.add(RiskPlayer(pl.army))
				} else {
					p.add(UIRiskPlayer(pl.army))
				}
			}
		}
		context.startGame(p)
	}

	init {
		Army.choices().forEach {
			players.add(PL(it))
		}
		val lv = ListView(context)
		lv.adapter = this
		context.newDialogBuilder().setTitle("Choose Players")
			.setView(lv)
			.setNegativeButton(R.string.popup_button_cancel, null)
			.setPositiveButton(R.string.popup_button_ok, this).show()
	}
}