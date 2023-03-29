package cc.game.zombicide.android

import android.content.ClipData
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.BaseAdapter
import android.widget.ListView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData
import cc.game.zombicide.android.databinding.OrganizeDialogBinding
import cc.game.zombicide.android.databinding.OrganizeDialogListItemBinding
import cc.lib.android.LifecycleDialog
import cc.lib.android.LifecycleViewModel
import cc.lib.ui.IButton
import cc.lib.zombicide.*
import cc.lib.zombicide.ui.UIZombicide

const val TAG = "ORGANIZE"

fun <T> MutableLiveData<T>.refresh() {
	value = value
}

@BindingAdapter("charBackpack", "moves", "viewModel")
fun ListView.setBackpackItems(char : ZCharacter?, moves: List<ZMove>, viewModel: OrganizeViewModel) {
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
//			return (convertView?:View.inflate(context, R.layout.organize_dialog_list_item, parent)).also {
//				it.findViewById<TextView>(R.id.tvItemName).text = char?.getBackpackItem(position)?.label
//				it.setTagFromMoves(moves, char, ZEquipSlot.BACKPACK, viewModel)
//				it.setOnClickListener { viewModel.setSelected(it, getItem(position)) }
//				it.setOnLongClickListener { viewModel.startDrag(it, getItem(position) as ZEquipment<*>?) }
			}.root
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

@BindingAdapter("tagMove", "character", "slot")
fun View.setTagFromMoves(moves : List<ZMove>, _character : ZCharacter?, slot : ZEquipSlot) {
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
	}
}

@BindingAdapter("tagTrash")
fun View.setTagFromMoves(moves : List<ZMove>) {
	tag = moves.firstOrNull { it.type == ZMoveType.DISPOSE }
	Log.i(TAG, "tag for TRASH -> $tag")
}

@BindingAdapter("updateEnabled", "equipped")
fun View.setStatusForDrop(dragging : Boolean, equipped : Boolean) {
	if (!dragging) {
		isEnabled = equipped
		isActivated = false
		isSelected = false
	}
}

@BindingAdapter("dragAndDropSupported")
fun View.setDragAndDropSupported(viewModel : OrganizeViewModel) {
	Log.d(TAG, "dragAndDropSupported $viewModel")
	setOnDragListener(viewModel)
	setOnTouchListener(viewModel)
}

/**
 * Created by Chris Caron on 3/24/23.
 */
class OrganizeViewModel : LifecycleViewModel(), View.OnDragListener, View.OnTouchListener {

	val descriptionItem = MutableLiveData("")
	val descriptionText = MutableLiveData("")

	val primaryCharacter = MutableLiveData<ZCharacter?>(null)
	val secondaryCharacter = MutableLiveData<ZCharacter?>(null)

	val allOptions = MutableLiveData<List<ZMove>>(emptyList())
	val listOptions = MutableLiveData<List<ZMove>>(emptyList())
	val dragging = MutableLiveData(false)
	val dropTarget = MutableLiveData<View?>(null)

	private var currentSelectedView : View? = null
	val game by lazy { UIZombicide.instance }

	fun setSelected(view : View, _obj : Any?) {
		val obj = _obj?:return
		currentSelectedView?.isSelected = false
		currentSelectedView = view
		view.isSelected = true
		when (obj) {
			is ZCharacter -> {
				descriptionItem.value = obj.name()
				descriptionText.value = obj.getAllSkillsTable().toString()
			}
			is ZEquipment<*> -> {
				descriptionItem.value = obj.label
				descriptionText.value = obj.tooltipText
			}
			is IButton -> {
				descriptionItem.value = obj.label
				descriptionText.value = obj.tooltipText
			}
			else -> {
				descriptionItem.value = obj.javaClass.simpleName
				descriptionText.value = null
			}
		}
	}

	fun startDrag(view : View, equip : ZEquipment<*>?) : Boolean {
		Log.d(TAG, "startDrag equip:$equip tag:${view.tag}")

		return equip?.let {
			val name = equip.label
			dragging.value = true
			view.startDrag(
				ClipData.newPlainText(name, name),
				View.DragShadowBuilder(view),
				equip, 0)
			true
		}?:false
	}

	override fun onTouch(v: View, event: MotionEvent): Boolean {
		v.onTouchEvent(event)
		when (event.action) {
			MotionEvent.ACTION_DOWN -> {
				Log.d(TAG, "onTouch DOWN tag=${v.tag}")
				v.tag?.let {
					game.setResult(it)
				}
			}
			MotionEvent.ACTION_UP -> {
				Log.d(TAG, "onTouch UP")
				dragging.value = false
				game.setResult(null)
			}
		}
		return true
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
		setTitle("ORGANIZE")
		OrganizeDialogBinding.inflate(layoutInflater).also {
			binding = it
			it.viewModel = viewModel
			it.lifecycleOwner = this
			it.ivTrash.setOnDragListener(viewModel)
			listOf(Pair(it.primaryCharacter, viewModel.primaryCharacter), Pair(it.secondaryCharacter, viewModel.secondaryCharacter)).forEach { pair ->
				pair.first.viewModel = viewModel
				pair.first.lifecycleOwner = this
				pair.first.character = pair.second
				pair.first.vgLeftHand.setOnDragListener(viewModel)
				pair.first.vgBody.setOnDragListener(viewModel)
				pair.first.vgRightHand.setOnDragListener(viewModel)
				pair.first.vgBackpack.setOnDragListener(viewModel)
				pair.first.vgLeftHand.setOnTouchListener(viewModel)
				pair.first.vgBody.setOnTouchListener(viewModel)
				pair.first.vgRightHand.setOnTouchListener(viewModel)

			}
			setContentView(it.root)
			window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			setOnCancelListener {
				game.setResult(ZMove.newOrganizeDone())
			}
		}

		viewModel.allOptions.observe(this) { list ->
			// list options are 'TRADE', 'DONE', 'UNDO'
			list.filter { it.type == ZMoveType.TRADE }.toMutableList().also {
				it.add(ZMove.newOrganizeDone())
				viewModel.listOptions.postValue(it)
			}
			viewModel.primaryCharacter.refresh()
			viewModel.secondaryCharacter.refresh()
		}
	}

}