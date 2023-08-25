package cc.game.android.risk

import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import cc.game.android.risk.databinding.PlayerListItemBinding
import cc.lib.android.LinearRecyclerView
import cc.lib.risk.Army
import cc.lib.risk.RiskPlayer
import cc.lib.risk.UIRiskPlayer

class PL(var army: Army) {
	var checked = false
	var robot = false
}

class PlayerViewHolder(val binding: PlayerListItemBinding) : RecyclerView.ViewHolder(binding.root)

/**
 * Created by Chris Caron on 9/17/21.
 */
class PlayerChooserDialog internal constructor(val context: RiskActivity) : RecyclerView.Adapter<PlayerViewHolder>(), DialogInterface.OnClickListener {

	var players: MutableList<PL> = ArrayList()

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
		PlayerViewHolder = PlayerViewHolder(PlayerListItemBinding.inflate(LayoutInflater.from(parent.context)))

	override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
		holder.binding.pl = players[position]
		holder.binding.lifecycleOwner = context
	}

	override fun getItemCount(): Int = players.size

	override fun onClick(dialog: DialogInterface, which: Int) {
		if (players.count { it.checked } < 2) {
			Toast.makeText(context, "Not enough players", Toast.LENGTH_LONG).show()
			return
		}

		players.filter { it.checked }.map {
			if (it.robot)
				RiskPlayer(it.army)
			else
				UIRiskPlayer(it.army)
		}.apply {
			context.game.startGame(this)
		}
	}

	init {
		Army.choices().forEach {
			players.add(PL(it))
		}
		context.newDialogBuilder().setTitle("Choose Players")
			.setView(LinearRecyclerView(context).also {
				it.adapter = this
			})
			.setNegativeButton(R.string.popup_button_cancel, null)
			.setPositiveButton(R.string.popup_button_ok, this).show()
	}
}