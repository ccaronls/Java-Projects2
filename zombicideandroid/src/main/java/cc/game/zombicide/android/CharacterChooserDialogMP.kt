package cc.game.zombicide.android

import android.app.Dialog
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView
import cc.game.zombicide.android.databinding.AssignDialogItemBinding
import cc.game.zombicide.android.databinding.AssignDialogP2pBinding
import cc.lib.zombicide.ZUser

/**
 * Created by Chris Caron on 7/21/21.
 */
abstract class CharacterChooserDialogMP(
	val activity: ZombicideActivity,
	val selectedPlayers: List<Assignee>,
	shouldShowCountChooser: Boolean,
	var maxPlayers: Int
) : RecyclerView.Adapter<CharacterChooserDialogMP.Holder>(), View.OnClickListener,
	OnLongClickListener, ZMPCommon.CLListener {

	val handler = Handler(Looper.getMainLooper())

	val dialog: Dialog
	val binding: AssignDialogP2pBinding

	protected abstract fun onAssigneeChecked(a: Assignee, checked: Boolean)
	protected abstract fun onStart()
	protected fun onDisconnect() {
		activity.p2pShutdown()
		dialog.dismiss()
	}

	class Holder(val ib: AssignDialogItemBinding) : RecyclerView.ViewHolder(ib.root) {
		lateinit var assignee: Assignee
		fun bind(a: Assignee, cl: CharacterChooserDialogMP?) {
			assignee = a
			ib.tvP2PName.text = a.userName
			if (!a.isUnlocked) {
				ib.lockedOverlay.visibility = View.VISIBLE
				if (a.lock.unlockMessageId != 0) {
					ib.tvLockedReason.visibility = View.VISIBLE
					ib.tvLockedReason.setText(a.lock.unlockMessageId)
				}
				ib.image.setOnClickListener(null)
			} else {
				ib.lockedOverlay.visibility = View.INVISIBLE
				ib.tvLockedReason.visibility = View.GONE
				ib.image.setOnClickListener(cl)
			}
			if (a.color >= 0) {
				ib.tvP2PName.setTextColor(ZUser.USER_COLORS[a.color].toARGB())
				ib.tvP2PName.text = a.userName
			} else {
				ib.tvP2PName.setTextColor(Color.WHITE)
				ib.tvP2PName.setText(R.string.p2p_name_unassigned)
			}
			ib.checkbox.isChecked = a.checked
			ib.image.setOnLongClickListener(cl)
			ib.image.setImageResource(a.name.cardImageId)
			ib.image.isEnabled = assignee.isClickable
			ib.image.tag = assignee
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
		val ib = AssignDialogItemBinding.inflate(LayoutInflater.from(activity), parent, false)
		return Holder(ib)
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

	fun incrementNumCharsPerPlayer(delta: Int) {
		if (delta > 0 && maxPlayers < MAX_CHARS_PER_PLAYER) {
			maxPlayers++
		} else if (delta < 0 && maxPlayers > MIN_CHARS_PER_PLAYER) {
			maxPlayers--
		}
		updateMaxPlayers(maxPlayers)
	}

	@MainThread
	fun updateMaxPlayers(maxPlayers: Int) {
		this.maxPlayers = maxPlayers
		binding.tvSelected.text =
			activity.getString(R.string.p2p_select_x_characters, numSelected, maxPlayers)
		binding.tvCount.text = "$maxPlayers"
		binding.bMinus.isEnabled = maxPlayers > MIN_CHARS_PER_PLAYER
		binding.bPlus.isEnabled = maxPlayers < MAX_CHARS_PER_PLAYER
		binding.bStart.isEnabled = numSelected == maxPlayers
		notifyDataSetChanged()
		activity.serverMgr?.setMaxCharactersPerPlayer(maxPlayers)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.bStart -> {
				if (numSelected <= 0) {
					Toast.makeText(
						activity,
						"Please select at least one character",
						Toast.LENGTH_LONG
					).show()
				} else {
					onStart()
				}
			}

			R.id.bDisconnect -> {
				if (activity.isP2PConnected) {
					activity.newDialogBuilder().setTitle("Confirm")
						.setMessage("Are you sure you want to cancel P2P game?")
						.setNegativeButton(R.string.popup_button_no, null)
						.setPositiveButton(R.string.popup_button_yes) { dialog, which -> onDisconnect() }
						.show()
				} else {
					dialog.dismiss()
				}
			}

			R.id.bMinus -> {
				incrementNumCharsPerPlayer(-1)
			}

			R.id.bPlus -> {
				incrementNumCharsPerPlayer(1)
			}

			else -> {
				v.tag?.let {
					if (it is Assignee) {
						if (!it.checked && numSelected >= maxPlayers) {
							Toast.makeText(
								activity,
								"Can only have $maxPlayers at a time",
								Toast.LENGTH_LONG
							).show()
							return
						}
						onAssigneeChecked(it, !it.checked)
						notifyDataSetChanged()
					}
				}
			}
		}
	}

	override fun onLongClick(v: View): Boolean {
		v.tag?.let {
			if (it is Assignee) {
				val iv = ImageView(activity)
				iv.setImageResource(it.name.cardImageId)
				activity.newDialogBuilder().setTitle(it.name.getLabel())
					.setView(iv).setNegativeButton(R.string.popup_button_cancel, null).show()
			}
		}
		return true
	}

	override fun onMaxCharactersPerPlayerUpdated(max: Int) {
		handler.post { updateMaxPlayers(max) }
	}

	@Synchronized
	fun postNotifyUpdateAssignee(a: Assignee) {
		val idx = selectedPlayers.indexOf(a)
		if (idx >= 0) {
			val c = selectedPlayers[idx]
			c.copyFrom(a)
			c.isAssingedToMe = a.isAssingedToMe
		}
		handler.post { notifyDataSetChanged() }
	}

	companion object {
		@JvmField
		val TAG = CharacterChooserDialogMP::class.java.simpleName

		const val MAX_CHARS_PER_PLAYER = 4
		const val MIN_CHARS_PER_PLAYER = 1
	}

	init {
		binding = AssignDialogP2pBinding.inflate(activity.layoutInflater)
		binding.bStart.setOnClickListener(this)
		binding.bDisconnect.setOnClickListener(this)
		binding.bMinus.setOnClickListener(this)
		binding.bPlus.setOnClickListener(this)
		binding.recyclerView.adapter = this
		binding.layoutCharacterCount.visibility =
			if (shouldShowCountChooser) View.VISIBLE else View.GONE
		dialog = activity.newDialogBuilder().setTitle(R.string.popup_title_assign)
			.setView(binding.root).show()
		incrementNumCharsPerPlayer(0)
	}
}