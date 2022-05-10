package cc.game.zombicide.android

import android.app.Dialog
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import cc.game.zombicide.android.databinding.AssignDialogItemBinding
import cc.game.zombicide.android.databinding.AssignDialogP2pBinding
import cc.lib.zombicide.ZUser

/**
 * Created by Chris Caron on 7/21/21.
 */
abstract class CharacterChooserDialogMP(val activity: ZombicideActivity, val selectedPlayers: List<Assignee>, val maxPlayers: Int) : RecyclerView.Adapter<CharacterChooserDialogMP.Holder>(), View.OnClickListener, OnLongClickListener, Runnable {
	@JvmField
    val dialog: Dialog
	protected abstract fun onAssigneeChecked(a: Assignee, checked: Boolean)
	protected abstract fun onStart()
	protected fun onDisconnect() {
		activity.p2pShutdown()
		dialog.dismiss()
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
		val ib = AssignDialogItemBinding.inflate(LayoutInflater.from(activity), parent, false)
		return Holder(ib, this)
	}

	override fun onBindViewHolder(holder: Holder, position: Int) {
		val a = selectedPlayers[position]
		holder.bind(a, this)
	}

	override fun getItemCount(): Int {
		return selectedPlayers.size
	}

	val numSelected: Int
		get() = selectedPlayers.count { it.isAssingedToMe }

	override fun onClick(v: View) {
		when (v.id) {
			R.id.bStart -> {
				if (numSelected <= 0) {
					Toast.makeText(activity, "Please select at least one character", Toast.LENGTH_LONG).show()
				} else {
					dialog.dismiss()
					onStart()
				}
				return
			}
			R.id.bDisconnect -> {
				if (activity.isP2PConnected) {
					activity.newDialogBuilder().setTitle("Confirm")
						.setMessage("Are you sure you want to cancel P2P game?")
						.setNegativeButton(R.string.popup_button_no, null)
						.setPositiveButton(R.string.popup_button_yes) { dialog, which -> onDisconnect() }.show()
				} else {
					dialog.dismiss()
				}
				return
			}
		}
		v.tag?.let {
			if (it is Assignee) {
				if (!it.checked && numSelected >= maxPlayers) {
					Toast.makeText(activity, "Can only have $maxPlayers at a time", Toast.LENGTH_LONG).show()
					return
				}
				onAssigneeChecked(it, !it.checked)
				notifyDataSetChanged()
			}
		}
	}

	override fun onLongClick(v: View): Boolean {
		v.tag?.let {
			if (it is Assignee) {
				val iv = ImageView(activity)
				iv.setImageResource(it.name.cardImageId)
				activity.newDialogBuilder().setTitle(it.name.label)
					.setView(iv).setNegativeButton(R.string.popup_button_cancel, null).show()
			}
		}
		return true
	}

	override fun run() {
		notifyDataSetChanged()
	}

	@Synchronized
	fun postNotifyUpdateAssignee(a: Assignee) {
		val idx = selectedPlayers.indexOf(a)
		if (idx >= 0) {
			val c = selectedPlayers[idx]
			c.copyFrom(a)
			c.isAssingedToMe = a.isAssingedToMe
		}
		activity.runOnUiThread(this)
	}

	class Holder(val ib: AssignDialogItemBinding, listener: View.OnClickListener) : RecyclerView.ViewHolder(ib.root) {
		var assignee: Assignee? = null
		fun bind(a: Assignee, cl: CharacterChooserDialogMP?) {
			assignee = a
			ib.tvP2PName.text = a.userName
			if (a.color >= 0) {
				ib.tvP2PName.setTextColor(ZUser.USER_COLORS[a.color].toARGB())
				ib.tvP2PName.text = a.userName
			} else {
				ib.tvP2PName.setTextColor(Color.WHITE)
				ib.tvP2PName.setText(R.string.p2p_name_unassigned)
			}
			ib.checkbox.isChecked = a.checked
			ib.checkbox.isClickable = false
			ib.image.setOnLongClickListener(cl)
			if (!a.isUnlocked) {
				ib.lockedOverlay.visibility = View.VISIBLE
				if (a.lock!!.unlockMessageId != 0) {
					ib.tvLockedReason.visibility = View.VISIBLE
					ib.tvLockedReason.setText(a.lock!!.unlockMessageId)
				}
				ib.checkbox.isEnabled = false
				ib.image.setOnClickListener(null)
			} else {
				ib.lockedOverlay.visibility = View.INVISIBLE
				ib.checkbox.isEnabled = true
				ib.tvLockedReason.visibility = View.GONE
				ib.image.setOnClickListener(cl)
			}
			ib.image.setImageResource(a.name.cardImageId)
			ib.image.tag = assignee
		}
	}

	companion object {
		@JvmField
        val TAG = CharacterChooserDialogMP::class.java.simpleName
	}

	init {
		val ab = AssignDialogP2pBinding.inflate(activity.layoutInflater)
		ab.bStart.setOnClickListener(this)
		ab.bDisconnect.setOnClickListener(this)
		ab.recyclerView.adapter = this
		dialog = activity.newDialogBuilder().setTitle(R.string.popup_title_assign)
			.setView(ab.root).show()
	}
}