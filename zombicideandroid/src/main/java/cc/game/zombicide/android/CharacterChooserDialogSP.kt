package cc.game.zombicide.android

import android.app.Dialog
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import cc.game.zombicide.android.ZombicideActivity.CharLock
import cc.game.zombicide.android.databinding.AssignDialogItemBinding
import cc.game.zombicide.android.databinding.AssignDialogSpBinding
import cc.game.zombicide.android.databinding.AssignDialogSpListviewItemBinding
import cc.lib.zombicide.ZQuests

/**
 * Created by Chris Caron on 3/22/22.
 */
internal abstract class CharacterChooserDialogSP(val activity: ZombicideActivity, val quest: ZQuests?) : BaseAdapter(), ViewPager.OnPageChangeListener {
	val inflater: LayoutInflater = LayoutInflater.from(activity)
	val charLocks: Array<CharLock> = activity.charLocks

	@JvmField
	val selectedPlayers: MutableSet<String>
	val binding: AssignDialogSpBinding
	override fun getCount(): Int {
		return charLocks.size
	}

	override fun getItem(position: Int): Any {
		return 0
	}

	override fun getItemId(position: Int): Long {
		return 0
	}

	override fun getView(position: Int, view: View?, parent: ViewGroup): View {
		val view = view ?: AssignDialogSpListviewItemBinding.inflate(inflater).root
		val charLocks = activity.charLocks
		val lock = charLocks[position]
		val cb = view.findViewById<CheckBox>(R.id.checkbox)
		val tv = view.findViewById<TextView>(R.id.textview)
		tv.text = lock.player.getLabel()
		cb.isChecked = selectedPlayers.contains(lock.player.name)
		if (cb.isChecked || lock.isUnlocked) {
			cb.isClickable = true
			cb.isEnabled = true
			cb.setOnTouchListener { v: View, event: MotionEvent ->
				if (event.action != MotionEvent.ACTION_DOWN) return@setOnTouchListener false
				if (cb.isChecked) {
					selectedPlayers.remove(lock.player.name)
					cb.isChecked = false
				} else if (selectedPlayers.size < ZombicideActivity.MAX_PLAYERS) {
					selectedPlayers.add(lock.player.name)
					cb.isChecked = true
				} else {
					Toast.makeText(activity, activity.getString(R.string.toast_msg_maxplayers, ZombicideActivity.MAX_PLAYERS), Toast.LENGTH_LONG).show()
				}
				true
			}
		} else {
			cb.isClickable = false
			cb.isEnabled = false
		}
		view.setOnClickListener { v: View? -> binding.viewPager.setCurrentItem(position, true) }
		return view
	}

	override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
	override fun onPageSelected(position: Int) {
		binding.listView.setItemChecked(position, true)
	}

	override fun onPageScrollStateChanged(state: Int) {}
	abstract fun onStarted()

	companion object {
		val TAG = CharacterChooserDialogSP::class.java.simpleName
	}

	init {
		selectedPlayers = HashSet(activity.storedCharacters)
		binding = AssignDialogSpBinding.inflate(inflater)
		binding.listView.adapter = this
		binding.viewPager.adapter = object : PagerAdapter() {
			override fun getCount(): Int {
				return charLocks.size
			}

			override fun isViewFromObject(view: View, o: Any): Boolean {
				return view === o
			}

			override fun instantiateItem(container: ViewGroup, position: Int): Any {
				val item = AssignDialogItemBinding.inflate(inflater)
				item.tvP2PName.visibility = View.GONE
				val lock = charLocks[position]
				item.checkbox.visibility = View.GONE
				if (!lock.isUnlocked && !selectedPlayers.contains(lock.player.name)) {
					item.lockedOverlay.visibility = View.VISIBLE
					item.tvLockedReason.visibility = View.VISIBLE
					item.tvLockedReason.setText(lock.unlockMessageId)
				} else {
					item.lockedOverlay.visibility = View.INVISIBLE
					item.tvLockedReason.visibility = View.GONE
				}
				item.image.setImageResource(lock.player.cardImageId)
				container.addView(item.root)
				return item.root
			}

			override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
				container.removeView(`object` as View)
			}

			override fun getItemPosition(`object`: Any): Int {
				return POSITION_NONE
			}
		}
		binding.viewPager.addOnPageChangeListener(this)
		val d: Dialog = activity.newDialogBuilder().setTitle(R.string.popup_title_choose_players).setView(binding.root).show()
		binding.bCancel.setOnClickListener { v: View? -> d.dismiss() }
		binding.bClear.setOnClickListener { v: View? ->
			selectedPlayers.clear()
			notifyDataSetChanged()
			binding.viewPager.adapter?.notifyDataSetChanged()
		}
		binding.bStart.setOnClickListener { v: View? ->
			Log.d(TAG, "Selected players: $selectedPlayers")
			if (selectedPlayers.size < 1) {
				Toast.makeText(activity, R.string.toast_msg_minplayers, Toast.LENGTH_LONG).show()
				return@setOnClickListener
			}
			onStarted()
			d.dismiss()
		}
	}
}