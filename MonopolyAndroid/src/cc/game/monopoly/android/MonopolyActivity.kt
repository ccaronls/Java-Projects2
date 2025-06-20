package cc.game.monopoly.android

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Point
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ListView
import android.widget.NumberPicker
import android.widget.TextView
import cc.game.monopoly.android.databinding.GameConfigDialogBinding
import cc.lib.android.CCNumberPicker
import cc.lib.android.DroidActivity
import cc.lib.android.DroidGraphics
import cc.lib.android.DroidView
import cc.lib.game.AGraphics
import cc.lib.monopoly.Card
import cc.lib.monopoly.Monopoly
import cc.lib.monopoly.MoveType
import cc.lib.monopoly.Piece
import cc.lib.monopoly.Player
import cc.lib.monopoly.Player.CardChoiceType
import cc.lib.monopoly.PlayerUser
import cc.lib.monopoly.Square
import cc.lib.monopoly.Trade
import cc.lib.monopoly.UIMonopoly
import cc.lib.utils.FileUtils
import cc.lib.utils.prettify
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

/**
 * Created by chriscaron on 2/15/18.
 */
class MonopolyActivity : DroidActivity() {
	private lateinit var saveFile: File

	private val monopoly: UIMonopoly = object : UIMonopoly() {
		override fun repaint() {
			redraw()
		}

		override fun runGame() {
			trySaveToFile(saveFile)
			if (BuildConfig.DEBUG) {
				FileUtils.tryCopyFile(saveFile, externalStorageDirectory)
			}
			super.runGame()
		}

		override fun getImageId(p: Piece): Int = when (p) {
			Piece.CAR -> R.drawable.car
			Piece.BOAT -> R.drawable.ship
			Piece.HAT -> R.drawable.tophat
			Piece.DOG -> R.drawable.dog
			Piece.THIMBLE -> R.drawable.thimble
			Piece.SHOE -> R.drawable.shoe
			Piece.WHEELBARROW -> R.drawable.wheelbarrow
			Piece.IRON -> R.drawable.iron
		}

		override val boardImageId: Int = R.drawable.board

		override fun showChooseMoveMenu(player: Player, moves: List<MoveType>): MoveType? = runBlocking {
			val items =Array(moves.size) { index ->
				when (moves[index]) {
					MoveType.PURCHASE -> {
						val sq = player.square
						"Purchase ${sq.name.prettify()} for $${sq.price}"
					}
					MoveType.PURCHASE_UNBOUGHT -> {
						val sq = getPurchasePropertySquare()
						"Purchase ${sq.name.prettify()} for $${sq.price}"
					}
					MoveType.PAY_BOND -> "${moves[index].prettify()} \$${player.jailBond}"
					else -> moves[index].prettify()
				}
			}
			suspendCoroutine { cont ->
				runOnUiThread {
					newDialogBuilderINGAME {
						cont.resume(null)
					}.setTitle("${player.piece.name.prettify()} Choose Move ")
						.setItems(items) { _, which: Int ->
							when (val mt: MoveType = moves[which]) {
								MoveType.PURCHASE_UNBOUGHT -> {
									val property: Square = getPurchasePropertySquare()
									showPurchasePropertyDialog("Buy Property", "Buy", property) {
										cont.resume(if (it) mt else null)
									}
								}
								MoveType.PURCHASE -> {
									val property: Square = player.square
									showPurchasePropertyDialog("Buy Property", "Buy", property) {
										cont.resume(if (it) mt else null)
									}
								}
								else -> {
									cont.resume(mt)
								}
							}
						}.setNegativeButton("Exit") { _, _ ->
							if (canCancel()) {
								cancel()
							} else {
								stopGameThread()
							}
							cont.resume(null)
						}.show()
				}
			}
		}

		override fun showChooseCardMenu(player: Player, cards: List<Card>, type: CardChoiceType): Card? = runBlocking {
			suspendCoroutine { cont ->
				runOnUiThread {
					val items = cards.prettify()
					newDialogBuilderINGAME { cont.resume(null) }.setTitle(player.piece.toString() + type.name.prettify())
						.setItems(items) { _, which: Int ->
							cont.resume(cards[which])
						}.setNegativeButton(R.string.popup_button_cancel) { _, _: Int ->
							if (canCancel()) {
								cancel()
							} else {
								stopGameThread()
							}
							cont.resume(null)
						}.show()
				}
			}
		}

		override fun showChooseTradeMenu(player: Player, list: List<Trade>): Trade? = runBlocking {
			suspendCoroutine { cont ->
				runOnUiThread {
					val items = list.prettify()
					newDialogBuilderINGAME { cont.resume(null) }
						.setTitle("${player.piece} Choose Trade")
						.setItems(items) { _, which: Int ->
							val t: Trade = list[which]
							showPurchasePropertyDialog("Buy ${t.card.property.name.prettify()} from ${t.trader.piece.name.prettify()}",
								"Buy for $${t.price}", t.card.property) {
								cont.resume(if (it) t else null)
							}
						}.show()
				}
			}
		}

		private fun openMarkSaleableMenu(playerUser: PlayerUser, list: List<Card>, onDoneCB: () -> Unit) {
			val view = ListView(this@MonopolyActivity)
			view.adapter = object : BaseAdapter() {
				override fun getCount(): Int {
					return list.size
				}

				override fun getItem(position: Int): Any {
					return list.get(position)
				}

				override fun getItemId(position: Int): Long {
					return position.toLong()
				}

				override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
					val v = convertView?:View.inflate(this@MonopolyActivity, R.layout.mark_sellable_listitem, null)
					val card: Card = list.get(position)
					val tvLabel: TextView = v.findViewById(R.id.tvLabel)
					val tvCost: TextView = v.findViewById(R.id.tvCost)
					tvLabel.text = card.property.name.prettify()
					val cost: Int = playerUser.getSellableCardCost(card)
					tvCost.text = if (cost <= 0) "Not For Sale" else cost.toString()
					return v
				}
			}
			view.onItemClickListener = OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, id: Long ->
				val card = list[position]
				val cost = playerUser.getSellableCardCost(card)
				val STEP = 50
				val np: NumberPicker = CCNumberPicker.newPicker(this@MonopolyActivity, cost, 0, 5000, STEP, null)
				newDialogBuilderINGAME { }.setTitle("Set Cost for " + card.property.name)
					.setView(np).setNeutralButton("Done") { _: DialogInterface?, _: Int ->
						playerUser.setSellableCard(card, np.value * STEP)
						openMarkSaleableMenu(playerUser, list, onDoneCB)
					}.show()
			}
			newDialogBuilderINGAME { }
				.setTitle("${playerUser.piece} Mark Saleable")
				.setView(view).setNeutralButton("Done") { _, _ -> onDoneCB.invoke() }
				.show()
		}

		override fun showMarkSellableMenu(playerUser: PlayerUser, list: List<Card>): Boolean = runBlocking {
			suspendCoroutine { cont ->
				runOnUiThread {
					openMarkSaleableMenu(playerUser, list) {
						cont.resume(true)
					}
				}
			}
		}

		override fun onError(t: Throwable) {
			t.printStackTrace()
			FileUtils.tryCopyFile(saveFile, File(externalStorageDirectory, "monopoly_error.txt"))
			stopGameThread()
			runOnUiThread {
				newDialogBuilder().setTitle(R.string.popup_title_error)
					.setMessage(t.toString())
					.setNegativeButton(R.string.popup_button_ok, null)
					.show()
			}
		}

		override fun drawPropertyCard(g: AGraphics, maxWidth: Float, topY: Float, property: Square, buyer: String?) {
			with(g as DroidGraphics) {
				//textHeight = resources.getDimension(R.dimen.dialog_purchase_text_size)
				paint.typeface = Typeface.DEFAULT_BOLD
			}
			super.drawPropertyCard(g, maxWidth, topY, property, buyer)
		}
	}

	private fun newDialogBuilderINGAME(onCancel: () -> Unit): AlertDialog.Builder {
		val builder = super.newDialogBuilder()
		if (monopoly.canCancel()) {
			builder.setNegativeButton(R.string.popup_button_cancel) { _, _ ->
				monopoly.cancel()
				onCancel.invoke()
			}
		} else {
			builder.setNegativeButton("Exit") { _, _ ->
				monopoly.stopGameThread()
				onCancel.invoke()
			}
		}
		return builder
	}

	private fun showPurchasePropertyDialog(title: String, buyLabel: String, property: Square, callback: (bought:Boolean)->Unit) {
		newDialogBuilder().setTitle(title).setView(object : DroidView(this, false) {
			override fun onPaint(g: DroidGraphics) {
				g.setTextHeight(32f, false)
				monopoly.drawPropertyCard(g, g.viewportWidth.toFloat(), 0f, property, null)
			}

			override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
				when (MeasureSpec.getMode(widthMeasureSpec)) {
					MeasureSpec.AT_MOST, MeasureSpec.EXACTLY -> {
						val width = MeasureSpec.getSize(widthMeasureSpec)
						setMeasuredDimension(width, width * 3 / 2)
						return
					}
				}
				super.onMeasure(widthMeasureSpec, heightMeasureSpec)
			}
		}).setNegativeButton("Don't Buy") { _, _ -> callback.invoke(false) }
			.setPositiveButton(buyLabel) { _, _ -> callback.invoke(true) }
			.show()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		//AndroidLogger.setLogFile(new File(getExternalStorageDirectory(), "monopoly.log"));
		saveFile = File(filesDir, "monopoly.save")
		checkPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
	}

	override fun onResume() {
		super.onResume()
		monopoly.repaint()
		if (saveFile.exists()) {
			showOptionsMenu()
		} else {
			showPlayerSetupMenu()
		}
	}

	override fun onPause() {
		super.onPause()
		if (monopoly.isGameRunning) {
			monopoly.stopGameThread()
		}
	}

	override fun shouldDialogAddBackButton(): Boolean {
		return !monopoly.isGameRunning
	}

	var tx = -1
	var ty = -1
	var dragging = false
	override fun onDraw(g: DroidGraphics) {
		g.setTextHeight(16f, false)
		monopoly.paint(g, tx, ty)
	}

	override fun onTouchDown(x: Float, y: Float) {
		tx = x.roundToInt()
		ty = y.roundToInt()
		redraw()
	}

	override fun onTouchUp(x: Float, y: Float) {
		if (dragging) {
			monopoly.stopDrag()
			dragging = false
		}
		tx = -1 //Math.round(x);
		ty = -1 //Math.round(y);
		redraw()
	}

	override fun onDrag(x: Float, y: Float) {
		if (!dragging) {
			monopoly.startDrag()
			dragging = true
		}
		tx = x.roundToInt()
		ty = y.roundToInt()
		redraw()
	}

	override fun onTap(x: Float, y: Float) {
		tx = x.roundToInt()
		ty = y.roundToInt()
		if (!monopoly.isGameRunning) {
			if (saveFile.exists()) {
				showOptionsMenu()
			} else {
				showPlayerSetupMenu()
			}
		} else {
			monopoly.onClick()
			for (i in 0 until monopoly.numPlayers) {
				if (monopoly.getPlayer(i) is PlayerUser) return
			}
			monopoly.stopGameThread()
		}
	}

	fun showOptionsMenu() {
		val fixed = File(externalStorageDirectory, "monopoly_fixed.txt")
		if (fixed.exists()) {
			FileUtils.tryCopyFile(fixed, saveFile)
			fixed.delete()
		}
		newDialogBuilder().setTitle("OPTIONS")
			.setItems(arrayOf("New Game", "Resume")) { _, which: Int ->
				when (which) {
					0 -> showGameSetupDialog()
					1 -> if (monopoly.tryLoadFromFile(saveFile)) {
						monopoly.repaint()
						monopoly.startGameThread()
					} else {
						saveFile.delete()
						newDialogBuilder().setTitle("ERROR").setMessage("Unable to load from save game.")
							.setNegativeButton("Ok") { _, _ -> showGameSetupDialog() }.show()
					}
				}
			}.show()
	}

	fun showPlayerSetupMenu() {
		val v = View.inflate(this, R.layout.players_setup_dialog, null)
		val realPlayers = v.findViewById<View>(R.id.npLivePlayers) as NumberPicker
		val compPlayers = v.findViewById<View>(R.id.npCompPlayers) as NumberPicker
		realPlayers.minValue = 0
		realPlayers.maxValue = Monopoly.MAX_PLAYERS
		realPlayers.value = 1
		compPlayers.minValue = 0
		compPlayers.maxValue = Monopoly.MAX_PLAYERS
		compPlayers.value = 1
		realPlayers.setOnValueChangedListener { _, _, newVal: Int ->
			val total: Int = newVal + compPlayers.value
			if (total > Monopoly.MAX_PLAYERS) {
				compPlayers.value = compPlayers.value - 1
			} else if (total < 2) {
				compPlayers.value = compPlayers.value + 1
			}
		}
		compPlayers.setOnValueChangedListener { _, _, newVal: Int ->
			val total: Int = newVal + realPlayers.value
			if (total > Monopoly.MAX_PLAYERS) {
				realPlayers.setValue(realPlayers.value - 1)
			} else if (total < 2) {
				realPlayers.setValue(realPlayers.value + 1)
			}
		}
		newDialogBuilder().setTitle("NEW GAME").setView(v)
			.setPositiveButton("Start") { _, _ ->
				monopoly.clear()
				for (i in 0 until realPlayers.value) monopoly.addPlayer(PlayerUser())
				for (i in 0 until compPlayers.value) monopoly.addPlayer(Player())
				showPieceChooser {
					monopoly.newGame()
					monopoly.startGameThread()
				}
			}.show()
	}

	fun showGameSetupDialog() {
		val rules = monopoly.rules
		val startMoneyValues = resources.getIntArray(R.array.start_money_values)
		val winMoneyValues = resources.getIntArray(R.array.win_money_values)
		val taxPercentValues = resources.getIntArray(R.array.tax_scale_percent_values)
		val maxJailTurnsValues = resources.getIntArray(R.array.max_turns_in_jail_values)
		val binding = GameConfigDialogBinding.inflate(layoutInflater)
		val startMoney = prefs.getInt("startMoney", rules.startMoney)
		val winMoney = prefs.getInt("winMoney", rules.valueToWin)
		val taxPercent = prefs.getInt("taxPercent", rules.taxScale.times(100).roundToInt())
		val jailBump = prefs.getBoolean("jailBump", rules.jailBumpEnabled)
		val jailMulti = prefs.getBoolean("jailMulti", rules.jailMultiplier)
		val maxJailTurns = prefs.getInt("maxJailTurns", rules.maxTurnsInJail)
		val doublesRules = prefs.getBoolean("doublesRule", rules.doublesRule)
		binding.npStartMoney.init(startMoneyValues, startMoney, { value: Int -> "$${value}" }, { _: NumberPicker?, _: Int, newVal: Int -> prefs.edit().putInt("startMoney", startMoneyValues[newVal]).apply() })
		binding.npWinMoney.init(winMoneyValues, winMoney, { value: Int -> "$$value" }, { _: NumberPicker?, _: Int, newVal: Int -> prefs.edit().putInt("winMoney", winMoneyValues[newVal]).apply() })
		binding.npTaxScale.init(taxPercentValues, taxPercent, { value: Int -> "$value%" }, { _: NumberPicker?, _: Int, newVal: Int -> prefs.edit().putInt("taxPercent", taxPercentValues[newVal]).apply() })
		binding.npJailMaxTurns.init(maxJailTurnsValues, maxJailTurns, { value: Int -> "$value" }, { _, _, newVal: Int -> prefs.edit().putInt("maxJailTurns", maxJailTurnsValues[newVal]).apply() })
		binding.cbJailBumpEnabled.isChecked = jailBump
		binding.cbJailBumpEnabled.setOnCheckedChangeListener { _, isChecked: Boolean -> prefs.edit().putBoolean("jailBump", isChecked).apply() }
		binding.cbJailMultiplierEnabled.isChecked = jailMulti
		binding.cbJailMultiplierEnabled.setOnCheckedChangeListener { _, isChecked: Boolean -> prefs.edit().putBoolean("jailMulti", isChecked).apply() }
		binding.cbDoublesRuleEnabled.isChecked = doublesRules
		binding.cbDoublesRuleEnabled.setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("doublesRule", isChecked).apply() }
		newDialogBuilder().setTitle("Configure").setView(binding.root)
			.setPositiveButton("Setup Players") { _, _ ->
				rules.startMoney = startMoneyValues[binding.npStartMoney.value]
				rules.valueToWin = winMoneyValues[binding.npWinMoney.value]
				rules.taxScale = 0.01f * taxPercentValues[binding.npTaxScale.value]
				rules.jailBumpEnabled = binding.cbJailBumpEnabled.isChecked
				rules.maxTurnsInJail = maxJailTurnsValues[binding.npJailMaxTurns.value]
				rules.jailMultiplier = binding.cbJailMultiplierEnabled.isChecked
				rules.doublesRule = binding.cbDoublesRuleEnabled.isChecked
				showPlayerSetupMenu()
			}.show().also {
				val width = (resources.displayMetrics.widthPixels * 0.75).toInt()
				val height = (resources.displayMetrics.heightPixels * 0.6).toInt()
				it.window?.setLayout(width, height)
			}
	}

	private var pieceDialog: Dialog? = null
	fun showPieceChooser(andThen: Runnable) {
		for (i in 0 until monopoly.numPlayers) {
			val p = monopoly.getPlayer(i)
			if (p is PlayerUser && !p.pieceChosen) {
				val listener = View.OnClickListener { v: View ->
					pieceDialog?.dismiss()
					v.tag?.let {
						if (it is Piece) {
							p.piece = it
							p.pieceChosen = true
							showPieceChooser(andThen)
						}
					}
				}
				val pieces = monopoly.unusedPieces
				val gl = GridLayout(this@MonopolyActivity)
				gl.columnCount = 4
				gl.rowCount = 2
				for (pc: Piece? in pieces) {
					val iv = ImageButton(this@MonopolyActivity)
					iv.tag = pc
					iv.setImageResource(monopoly.getImageId((pc)!!))
					gl.addView(iv)
					iv.setOnClickListener(listener)
				}
				pieceDialog = newDialogBuilder().setTitle("CHOOSE PIECE ${(i + 1)}").setView(gl).show()
				return
			}
		}
		andThen.run()
	}

	override val dialogTheme: Int
		get() = R.style.MonopolyDialogTheme

	override fun onDialogShown(d: Dialog) {

//        d.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		val display = windowManager.defaultDisplay
		val lp = d.window!!.attributes
		val size = Point()
		display.getSize(size)
		if (isPortrait) {
			//lp.gravity = Gravity.TOP;
			//lp.y = size.y/5;
			lp.width = size.x / 2
		} else {
			lp.width = size.y / 2
		}
		d.window!!.attributes = lp
	}

	companion object {
		private val TAG = MonopolyActivity::class.java.simpleName
	}
}