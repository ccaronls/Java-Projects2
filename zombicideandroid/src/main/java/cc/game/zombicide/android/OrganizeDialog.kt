package cc.game.zombicide.android

import android.content.ClipData
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ListView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import cc.game.zombicide.android.databinding.OrganizeDialogBinding
import cc.game.zombicide.android.databinding.OrganizeDialogListItemBinding
import cc.lib.android.*
import cc.lib.ui.IButton
import cc.lib.zombicide.*
import cc.lib.zombicide.ui.UIZombicide

const val TAG = "ORGANIZE"

fun <T> MutableLiveData<T>.refresh() {
	value = value
}

@BindingAdapter("charBackpack", "viewModel")
fun ListView.setBackpackItems(char : ZCharacter?, viewModel: OrganizeViewModel) {
	adapter = object : BaseAdapter() {
		override fun getCount(): Int = char?.getBackpack()?.size?:0

		override fun getItem(position: Int): Any? = char?.getBackpackItem(position)

		override fun getItemId(position: Int): Long = 0

		override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
			return OrganizeDialogListItemBinding.inflate(LayoutInflater.from(context)).also {
				it.viewModel = viewModel
				it.lifecycleOwner = viewModel
				it.position = position
				it.character = char
			}.root.also {
				it.tag = getItem(position)
				Log.d(TAG, "${char?.type} Backpack at position $position has tag: $tag")
			}
		}
	}
}

@BindingAdapter("listOptions", "viewModel")
fun ListView.setListOptions(list : List<ZMove>, viewModel : OrganizeViewModel) {
	adapter = object : BaseAdapter() {
		override fun getCount(): Int = list.size

		override fun getItem(position: Int): Any = list[position]

		override fun getItemId(position: Int): Long = 0

		override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
			return ZButton.build(context, list[position], true).also {
				it.setOnClickListener { viewModel.game.setResult(list[position]) }
			}
		}
	}
}

@BindingAdapter("tagMove", "character", "slot", "dragging", "equipped")
fun View.setTagFromMoves(moves : List<ZMove>, _character : ZCharacter?, slot : ZEquipSlot, dragging : Boolean, equipped : Boolean) {
	_character?.let { character ->
		val options = moves.filter {
			it.character == character.type && ((it.toSlot == null && it.fromSlot == slot) || it.toSlot == slot)
		}
		if (options.size == 1) {
			tag = options[0]
			Log.i(TAG, "tag for ${character.name()}:${slot} -> $tag")
		} else {
			tag = null
			if (options.size > 1)
				Log.e(TAG, "ERROR: too many options for tag: ${options.joinToString()}")
			else {}
		}

		if (dragging) {
			isEnabled = tag != null
			isActivated = tag != null
			isSelected = false
		} else {
			isEnabled = equipped
			isActivated = false
			isSelected = false
		}
	}
}

@BindingAdapter("tagTrash")
fun View.setTagTrash(moves : List<ZMove>) {
	tag = moves.firstOrNull { it.type == ZMoveType.DISPOSE }
	Log.i(TAG, "tag for TRASH -> $tag")
}

@BindingAdapter("tagConsume")
fun View.setTagConsume(moves : List<ZMove>) {
	tag = moves.firstOrNull { it.type == ZMoveType.CONSUME }
	isEnabled = tag!=null
	isActivated = tag!=null
	Log.i(TAG, "tag for CONSUME -> $tag")
}

@BindingAdapter("tagBackpackItem", "backpackPosition")
fun View.setBackpackTag(char : ZCharacter?, position : Int) {
	tag = char?.getBackpackItem(position)
}

/**
 * Created by Chris Caron on 3/24/23.
 */
class OrganizeViewModel : LifecycleViewModel(),
	View.OnDragListener,
	AdapterView.OnItemLongClickListener,
	AdapterView.OnItemClickListener {

	val primaryCharacter = MutableLiveData<ZCharacter?>(null)
	val secondaryCharacter = MutableLiveData<ZCharacter?>(null)

	val descriptionItem = MutableLiveData<Any?>(null)
	val descriptionHeader = TransformedLiveData(descriptionItem) {
		when (it) {
			is ZCharacter    -> it.name()
			is ZEquipment<*> -> it.label
			else             -> it?.javaClass?.simpleName ?: ""
		}
	}
	val descriptionBody : LiveData<String?> = combine(descriptionItem, primaryCharacter, secondaryCharacter) { obj, primary, secondary ->
		when (obj) {
			is ZCharacter -> obj.getAllSkillsTable().toString()
			is ZWeapon    -> secondary?.let {
				obj.getComparisonInfo(game, primary!!, it).toString()
			}?:run {
				obj.getCardInfo(primary!!, game).toString()
			}
			is ZEquipment<*> -> obj.label
			is IButton -> obj.tooltipText
			else -> ""
		}
	}

	val allOptions = MutableLiveData<List<ZMove>>(emptyList())
	val listOptions = MutableLiveData<List<ZMove>>(emptyList())
	val dragging = MutableLiveData(false)
	val dropTarget = MutableLiveData<View?>(null)
	val undoPushes = MutableLiveData(0)

	private var currentSelectedView : View? = null
	val game by lazy { UIZombicide.instance }

	fun onGameSaved() {
		undoPushes.increment(1)
	}

	fun onUndo() {
		undoPushes.increment(-1)
		primaryCharacter.refresh()
		secondaryCharacter.refresh()
	}

	fun tryUndo() {
		game.undo()
	}

	fun setSelected(view : View, _obj : Any?) {
		val obj = _obj?:return
		currentSelectedView?.isSelected = false
		currentSelectedView = view
		view.isSelected = true
		descriptionItem.value = obj
	}

	fun startDrag(view : View, equip : ZEquipment<*>?) : Boolean {
		Log.d(TAG, "startDrag equip:$equip tag:${view.tag}")
		if (equip == null)
			return false
		view.tag?.let {
			if (it is ZMove) {
				it.list?.filterIsInstance<ZMove>()?.takeIf { it.isNotEmpty() }?.let {
					allOptions.value = it
					dragging.value = true
					view.post {
						val name = equip.label
						dragging.value = true
						view.startDrag(
							ClipData.newPlainText(name, name),
							View.DragShadowBuilder(view),
							equip, 0)
					}
					return true
				}
			}
		}
		return false
	}
	override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
		val equip = view.tag as ZEquipment<*>?
		view.tag = parent.tag
		return startDrag(view, equip)
	}

	override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
		setSelected(view, view.tag)
	}

	// A view state has
	//   default: off-white inset (drop not active)
	//   active white: inset (drop targets)
	//   disabled: gray inset (non-drop targets)
	//   selected: current drop targets (white enlarged)
	override fun onDrag(v: View, event: DragEvent): Boolean {
		when (event.action) {
			DragEvent.ACTION_DRAG_STARTED -> {
				Log.d(TAG, "Drag start ${v.tag}")
				v.isActivated = v.tag != null
				return v.tag != null
			}
			DragEvent.ACTION_DRAG_ENDED -> {
				v.isActivated = false
				Log.d(TAG, "Drag end ${v.tag}")
				if (dragging.value == true) {
					dragging.value = false
					game.setResult(null)
				}
			}
			DragEvent.ACTION_DROP -> {
				// perform the move accociated with the
				Log.d(TAG, "Drag drop ${v.tag}")
				dragging.value = false
				game.setResult(v.tag)
			}
			DragEvent.ACTION_DRAG_ENTERED -> {
				Log.d(TAG, "Drag entered ${v.tag} enabled: ${v.isEnabled}")
				// if the entered view is enabled its status becomes selected
				if (v.isEnabled && v.tag != null) {
					v.isSelected = true
					dropTarget.value = v
				}
			}
			DragEvent.ACTION_DRAG_EXITED -> {
				Log.d(TAG, "Drag exited dragging: ${dragging.value}")
				// unselect the view that was exited
				if (dropTarget.value == v) {
					v.isSelected = false
					dropTarget.value = null
				}
			}
			//DragEvent.ACTION_DRAG_LOCATION -> {}
			else -> return false
		}
		return true
	}
}

class OrganizeDialog(context : ZombicideActivity) : LifecycleDialog<OrganizeViewModel>(context, OrganizeViewModel::class.java) {

	lateinit var binding : OrganizeDialogBinding
	val game = UIZombicide.instance

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setTitle(R.string.popup_title_organize)
		OrganizeDialogBinding.inflate(layoutInflater).also {
			binding = it
			it.viewModel = viewModel
			it.lifecycleOwner = this
			//it.ivTrash.setOnDragListener(viewModel)
			it.tvConsume.setOnDragListener(viewModel)
			it.tvTrash.setOnDragListener(viewModel)
			listOf(Pair(it.primaryCharacter, viewModel.primaryCharacter), Pair(it.secondaryCharacter, viewModel.secondaryCharacter)).forEach { pair ->
				pair.first.viewModel = viewModel
				pair.first.lifecycleOwner = this
				pair.first.character = pair.second
				pair.first.vgLeftHand.setOnDragListener(viewModel)
				pair.first.vgBody.setOnDragListener(viewModel)
				pair.first.vgRightHand.setOnDragListener(viewModel)
				pair.first.vgBackpack.setOnDragListener(viewModel)
				pair.first.lvBackpack.onItemLongClickListener = viewModel
				pair.first.lvBackpack.onItemClickListener = viewModel
			}
			setContentView(it.root)
			window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
			setOnCancelListener {
				game.setResult(ZMove.newOrganizeDone())
			}
		}

		viewModel.allOptions.observe(this) { list ->
			// list options are 'TRADE', 'DONE', 'UNDO'
			list.filter { it.type == ZMoveType.ORGANIZE_TRADE }.toMutableList().also {
				it.add(ZMove.newOrganizeDone())
				viewModel.listOptions.postValue(it)
			}
			viewModel.primaryCharacter.refresh()
			viewModel.secondaryCharacter.refresh()
		}
	}

}