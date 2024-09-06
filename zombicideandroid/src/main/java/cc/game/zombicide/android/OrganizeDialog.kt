package cc.game.zombicide.android

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.DialogInterface
import android.graphics.Canvas
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Checkable
import android.widget.FrameLayout
import android.widget.ListView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import cc.game.zombicide.android.databinding.OrganizeDialogBinding
import cc.game.zombicide.android.databinding.OrganizeDialogListItemBinding
import cc.lib.android.*
import cc.lib.ui.IButton
import cc.lib.utils.Table
import cc.lib.utils.launchIn
import cc.lib.utils.takeIfInstance
import cc.lib.zombicide.*
import cc.lib.zombicide.ui.UIZombicide
import kotlinx.coroutines.Dispatchers

const val TAG = "ORGANIZE"

/**
 * Support 'dragging' drawable state
 */
class OrganizeLayout(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs), Checkable {

	companion object {
		private val STATE_CHECKED = intArrayOf(android.R.attr.state_checked)
	}

	private var checked = false

	override fun onCreateDrawableState(extraSpace: Int): IntArray {
		return if (checked) {
			// We are going to add 1 extra state.
			val drawableState = super.onCreateDrawableState(extraSpace + 1)
			mergeDrawableStates(drawableState, STATE_CHECKED)
		} else {
			super.onCreateDrawableState(extraSpace)
		}
	}

	override fun setChecked(checked: Boolean) {
		if (this.checked != checked) {
			this.checked = checked
			refreshDrawableState()
		}
	}

	override fun isChecked(): Boolean = checked

	override fun toggle() {
		setChecked(!checked)
	}

	override fun onDraw(canvas: Canvas?) {
		super.onDraw(canvas)
	}
}

class ListMovesAdapter(val context: Context, val viewModel: OrganizeViewModel, val equipList: List<ZEquipment<*>>) :
	BaseAdapter() {

	override fun getCount(): Int {
		return equipList.size
	}

	override fun getItem(position: Int) = equipList.get(position)

	override fun getItemId(position: Int): Long = 0

	@SuppressLint("ViewHolder")
	override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
		return OrganizeDialogListItemBinding.inflate(LayoutInflater.from(context)).also {
			it.viewModel = viewModel
			it.lifecycleOwner = viewModel
			it.equip = equipList[position]
		}.root.also {
			it.tag = getItem(position)
		}
	}
}


@BindingAdapter("charBackpack", "viewModel")
fun ListView.setBackpackItems(char: ZCharacter?, viewModel: OrganizeViewModel) {
	if (char == null)
		return
	Log.d(TAG, "setBackpackItems")
	adapter = ListMovesAdapter(context, viewModel, char.getBackpack())
}

@BindingAdapter("vaultItems", "viewModel", "tagPickupMoves")
fun ListView.setVaultItems(game: ZGame?, viewModel: OrganizeViewModel, moves: List<ZMove>) {
	if (game == null)
		return
	Log.d(TAG, "setVaultItems")
	val list = game.quest.getVaultItems(game.requireCurrentCharacter.occupiedZone)
	adapter = ListMovesAdapter(context, viewModel, list)
	tag = moves.filter { it.type == ZMoveType.ORGANIZE_PICKUP }.firstOrNull()
}

class ListOptionsAdapter(val context: Context, val viewModel: OrganizeViewModel) : BaseAdapter() {

	var list = emptyList<ZMove>()
		set(value) {
			field = value
			notifyDataSetChanged()
		}

	override fun getCount(): Int = list.size

	override fun getItem(position: Int): Any = list[position]

	override fun getItemId(position: Int): Long = 0

	override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
		if (convertView != null) {
			(convertView as ZButton).init(list[position], true)
			return convertView
		}
		return ZButton.build(context, list[position], true).also {
			it.setOnClickListener {
				viewModel.game.setResult(list[position])
			}
		}
	}

}

@BindingAdapter("listOptions", "viewModel")
fun ListView.setListOptions(list: List<ZMove>, viewModel: OrganizeViewModel) {
	Log.d(TAG, "setListOptions")
	if (adapter == null)
		adapter = ListOptionsAdapter(context, viewModel)
	(adapter as ListOptionsAdapter).list = list
}

@BindingAdapter("tagMove", "character", "slot", "dragging", "equipped")
fun View.setTagFromMoves(moves: List<ZMove>, character: ZCharacter?, slot: ZEquipSlot, dragging: Boolean, equipped: Boolean) {
	character?.let { ch ->
		Log.d(
			TAG,
			"setTagFromMoves: char: $character, slot:$slot, equipped: $equipped, moves: ${moves.joinToString(separator = "\n")}"
		)
		val options = moves.filter {
			it.character == ch.type && ((it.toSlot == null && it.fromSlot == slot) || it.toSlot == slot)
		}
		tag = options.firstOrNull()
		tag?.let {
			Log.d(TAG, "tag for ${ch.name()}:${slot} -> $it")
		} ?: Log.d(TAG, "No move for slot $slot")

		if (dragging) {
			//isFocusable = tag != null
			isEnabled = tag != null
			isActivated = tag != null
			isSelected = false
		} else {
			//isFocusable = equipped
			isEnabled = equipped
			isActivated = false
			isSelected = false
		}
	}
}

@BindingAdapter("tagVault", "dragging")
fun View.setVaultTagFromMoves(moves: List<ZMove>, dragging: Boolean) {
	Log.d(
		TAG,
		"setTagFromMoves: moves: ${moves.joinToString(separator = "\n")}"
	)
	tag = moves.filter { it.type == ZMoveType.DROP_ITEM }.firstOrNull()

	if (dragging) {
		//isFocusable = tag != null
		isEnabled = tag != null
		isActivated = tag != null
		isSelected = false
	} else {
		//isFocusable = equipped
		isEnabled = true
		isActivated = false
		isSelected = false
	}
}

@BindingAdapter("tagTrash")
fun View.setTagTrash(moves: List<ZMove>) {
	tag = moves.firstOrNull { it.type == ZMoveType.DISPOSE }
	isEnabled = tag != null
	isActivated = tag != null
	isSelected = false
	Log.i(TAG, "tag for TRASH -> $tag")
}

@BindingAdapter("tagConsume")
fun View.setTagConsume(moves : List<ZMove>) {
	tag = moves.firstOrNull { it.type == ZMoveType.CONSUME }
	isEnabled = tag!=null
	isActivated = tag != null
	isSelected = false
	Log.i(TAG, "tag for CONSUME -> $tag")
}

/**
 * Created by Chris Caron on 3/24/23.
 */
class OrganizeViewModel : LifecycleViewModel(),
	View.OnDragListener,
	AdapterView.OnItemLongClickListener,
	AdapterView.OnItemClickListener,
	AdapterView.OnItemSelectedListener {

	val primaryCharacter = MutableLiveData<ZCharacter?>(null)
	val secondaryCharacter = MutableLiveData<ZCharacter?>(null)

	val descriptionItem = MutableLiveData<Any>(null)
	val descriptionHeader = TransformedLiveData(descriptionItem) {
		when (it) {
			is ZCharacter -> it.name()
			is ZEquipment<*> -> it.getLabel() // it.label
			else -> it?.javaClass?.simpleName ?: "INSTRUCTIONS"
		}
	}

	val descriptionBody: LiveData<Table?> = combine(descriptionItem, primaryCharacter, secondaryCharacter) { obj, primary, secondary ->
		when (obj) {
			is ZCharacter -> Table().setNoBorder().also {
				it.setModel(object : Table.Model {

				})
				it.addRow(
					obj.getStatsTable(game.rules).setNoBorder(),
					obj.getAllSkillsTable(game.rules).setNoBorder()
				)
			}
			is ZWeapon -> secondary?.let {
				obj.getComparisonInfo(game, primary!!, it)
			} ?: run {
				obj.getCardInfo(primary!!, game)
			}
			//is ZItem -> obj.type.description
			//is ZEquipment<*> -> obj.tooltipText
			is IButton -> Table().setNoBorder().addRow(obj.getTooltipText() ?: "")
			//is ZSpell -> obj.type.tooltipText
			else -> Table().setNoBorder().addRow(
				"""
> Select an item to get more info on that item.
> Long click on an item and drag to move equipment around your
  inventory and with other characters you can trade with."""
			)
		}
	}

	val instructionalText: String
		get() = if (isTV()) {
			"""
> Use DPAD to select an item. Info on the item will be shown.
> Center click on DPAD to choose item for relocating. DPAD will
  be used to select to to relocate. Center DPAD to relocate the item.
  BACK button cancels the relocate."""
		} else {
			"""
> Select an item to get more info on that item.
> Long click on an item and drag to move equipment around your
  inventory and with other characters you can trade with."""
		}

	val allOptions = MutableLiveData<List<ZMove>>(emptyList())
	val listOptions = MutableLiveData<List<ZMove>>(emptyList())
	val dragging = MutableLiveData(false)
	val dropTarget = MutableLiveData<View?>(null)
	val undoPushes = MutableLiveData(0)

	private var currentSelectedView: View? = null
	var currentDraggedView: OrganizeLayout? = null
	val game by lazy { UIZombicide.instance }
	val loading = MutableLiveData(false)
	val inVault = MutableLiveData(false)
	val vaultOptions = MutableLiveData<List<ZMove>>(emptyList())

	val undoButtonShowing = combine(undoPushes, dragging) { undo, drag ->
		((undo ?: 0) > 0) && drag == false
	}

	init {
		dragging.observe(this) {
			currentDraggedView?.isChecked = it
		}
	}

	fun tryUndo() {
		game.undo()
	}

	fun cancelDragging(): Boolean {
		if (dragging.value == true) {
			loading.value = true
			dragging.value = false
			game.setResult(null)
			return true
		}
		return false
	}

	fun setSelected(view: View, _obj: Any?) {
		if (isTV()) {
			if (dragging.value == true) {
				loading.value = true
				// perform the move associated with the
				Log.d(TAG, "Drag drop ${view.tag}")
				dragging.value = false
				game.setResult(view.tag)
			} else if (_obj is ZEquipment<*>) {
				if (startDrag(view, _obj)) {
					dragging.value = true
				}
			}
		} else {
			val obj = _obj ?: return
			currentSelectedView?.isSelected = false
			currentSelectedView = view
			view.isSelected = true
			(view as? Checkable?)?.isChecked = false
			descriptionItem.value = obj
		}
	}

	fun showInfo(obj: Any?) {
		Log.d(TAG, "showInfo $obj ")
		obj?.let {
			descriptionItem.value = it
		}
	}

	fun dropItem(view: View) {
		if (isTV() && dragging.value == true) {
			loading.value = true
			// perform the move associated with the
			Log.d(TAG, "Drag drop ${view.tag}")
			dragging.value = false
			game.setResult(view.tag)
		}
	}

	@SuppressWarnings("unchecked")
	fun startDrag(view: View, equip: ZEquipment<*>?): Boolean {
		view.tag?.takeIfInstance<ZMove>()?.let {
			return startDrag(view, it.list as List<ZMove>, equip)
		}
		return false
	}

	fun startDrag(view: View, options: List<ZMove>, equip: ZEquipment<*>?): Boolean {
		Log.d(TAG, "startDrag equip:$equip tag:${view.tag}")
		if (equip == null || options.isEmpty())
			return false
		allOptions.value = options
		if (view is OrganizeLayout) {
			currentDraggedView = view
		}
		dragging.value = true
		val name = equip.getLabel()
		if (isTV())
			return true
		return view.startDrag(
			ClipData.newPlainText(name, name),
			View.DragShadowBuilder(view),
			equip, 0
		)
	}

	// Long click on a backpack item. The item has the equipment
	// and the 'parent' adapter view holds the move options
	override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
		val equip = view.tag as ZEquipment<*>?
		Log.d(TAG, "onItemLongClick equip: $equip parent.tag: ${parent.tag}")
		return parent.tag?.takeIfInstance<ZMove>()?.takeIf { it.list != null }?.let { move ->
			val options = move.list as List<ZMove>
			startDrag(view, options.filter {
				it.equipment == equip
			}, equip)

		} ?: true
	}

	override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
		if (isTV()) {
			onItemLongClick(parent, view, position, id)
//			(parent as ListView).setItemChecked(position, true)
		} else {
			setSelected(view, view.tag)
		}
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
				loading.value = true
				// perform the move associated with the
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

	override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
		Log.d(TAG, "onItemSelected ${view.tag}")
//		TODO("Not yet implemented")
	}

	override fun onNothingSelected(parent: AdapterView<*>) {
		Log.d(TAG, "onNothingSelected")
//		TODO("Not yet implemented")
	}
}

class OrganizeDialog(context: ZombicideActivity) :
	LifecycleDialog<OrganizeViewModel>(context, OrganizeViewModel::class.java, R.style.ZOrganizeDialogTheme),
	DialogInterface.OnKeyListener {

	lateinit var binding: OrganizeDialogBinding
	val game = UIZombicide.instance

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setTitle(R.string.popup_title_organize)
		OrganizeDialogBinding.inflate(layoutInflater).also {
			binding = it
			it.viewModel = viewModel
			it.lifecycleOwner = this
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
				pair.first.lvBackpack.onItemSelectedListener = viewModel
			}
			setContentView(it.root)
			binding.lvOptions.itemsCanFocus = false
			binding.lvOptions.onItemSelectedListener = viewModel
			binding.lvVault.itemsCanFocus = false
			binding.lvVault.onItemClickListener = viewModel
			binding.lvVault.onItemLongClickListener = viewModel
			binding.lvVault.onItemSelectedListener = viewModel
			binding.vgVault.setOnDragListener(viewModel)
			if (isTV()) {
				binding.lvOptions.setOnItemClickListener { _, view, _, _ ->
					view.performClick()
				}
				setOnKeyListener(this)
			}
			window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
			setOnCancelListener {
				game.setResult(ZMove.newOrganizeDone())
			}
			setOnDismissListener {
				game.setResult(ZMove.newOrganizeDone())
			}
			binding.primaryCharacter.root.nextFocusLeftId = binding.lvOptions.id
			binding.primaryCharacter.root.nextFocusRightId = binding.secondaryCharacter.root.id
			binding.secondaryCharacter.root.nextFocusLeftId = binding.primaryCharacter.root.id
		}

		viewModel.allOptions.observe(this) { list ->
			// list options are 'TRADE', 'DONE', 'UNDO'
			list.filter { it.type == ZMoveType.ORGANIZE_TRADE }.toMutableList().also {
				it.add(ZMove.newOrganizeDone())
				viewModel.listOptions.postValue(it)
			}
			list.filter { it.type == ZMoveType.PICKUP_ITEM }.also {
				viewModel.vaultOptions.postValue(it)
			}
			refresh()
			viewModel.loading.value = false
		}

		binding.lvOptions.requestFocus()
		viewModel.dragging.observe(this) {
			if (!it) {
				binding.primaryCharacter.lvBackpack.clearChoices()
				binding.secondaryCharacter.lvBackpack.clearChoices()
				binding.lvVault.clearChoices()
			}
		}
		binding.root.viewTreeObserver.addOnGlobalFocusChangeListener { oldFocus, newFocus ->
			Log.d(TAG, "focus changed from ${oldFocus?.javaClass?.simpleName} to ${newFocus?.javaClass?.simpleName}")
		}

		game.board.getZone(game.currentCharacter!!.occupiedZone).takeIf { it.isVault }?.let { zone ->
			viewModel.inVault.value = true
		} ?: run {
			viewModel.inVault.value = false
		}

		viewModel.allOptions.observe(this) {
			refresh()
		}
	}

	override fun onKey(dialog: DialogInterface?, keyCode: Int, event: KeyEvent): Boolean {
		if (event.action != KeyEvent.ACTION_DOWN)
			return false
		when (keyCode) {
			//KeyEvent.KEYCODE_DPAD_CENTER -> {}
			KeyEvent.KEYCODE_BACK -> {
				if (!viewModel.cancelDragging()) {
					if (!binding.lvOptions.hasFocus()) {
						binding.lvOptions.requestFocus()
					} else {
						dismiss()
					}
				}
			}

			else -> return false
		}
		return true
	}

	fun refresh() {
		Log.d(TAG, "refresh")
		launchIn(Dispatchers.Main) {
			viewModel.primaryCharacter.refresh()
			viewModel.secondaryCharacter.refresh()
		}
	}

}