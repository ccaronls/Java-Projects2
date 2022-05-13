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
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import cc.game.monopoly.android.databinding.*
import cc.lib.android.CCNumberPicker
import cc.lib.android.DroidActivity
import cc.lib.android.DroidGraphics
import cc.lib.android.DroidView
import cc.lib.game.AGraphics
import cc.lib.monopoly.*
import cc.lib.monopoly.Player.CardChoiceType
import cc.lib.utils.FileUtils
import cc.lib.utils.prettify
import java.io.File
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.roundToInt

/**
 * Created by chriscaron on 2/15/18.
 */
class MonopolyActivity : DroidActivity() {
	private var saveFile: File? = null

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

		override fun showChooseMoveMenu(player: Player, moves: List<MoveType>): MoveType? {
			val lock = ReentrantLock()
			val cond = lock.newCondition()
			val items =Array(moves.size) { index ->
				when (moves[index]) {
					MoveType.PURCHASE_UNBOUGHT -> {
						val sq = getPurchasePropertySquare()
						"Purchase ${prettify(sq.name)}  for $${sq.price}"
					}
					MoveType.PAY_BOND -> "${prettify(moves[index])} \$${player.jailBond}"
					else -> prettify(moves[index])
				}
			}
			val result = arrayOfNulls<MoveType>(1)
			runOnUiThread {
				newDialogBuilderINGAME().setTitle("${prettify(player.piece.name)} Choose Move ")
					.setItems(items) { _, which: Int ->
						when (val mt: MoveType = moves[which]) {
							MoveType.PURCHASE_UNBOUGHT -> {
								val property: Square = getPurchasePropertySquare()
								showPurchasePropertyDialog("Buy Property", "Buy", property) {
									result[0] = mt
									lock.withLock { cond.signal() }
								}
							}
							MoveType.PURCHASE -> {
								val property: Square = player.square
								showPurchasePropertyDialog("Buy Property", "Buy", property) {
									result[0] = mt
									lock.withLock { cond.signal() }
								}
							}
							else                       -> {
								result[0] = mt
								lock.withLock { cond.signal() }
							}
						}
					}.setNegativeButton("Exit") { _, _ ->
						if (canCancel()) {
							cancel()
						} else {
							stopGameThread()
						}
						lock.withLock { cond.signal() }
					}.show()
			}
			lock.withLock { cond.await() }
			return result[0]
		}

		override fun showChooseCardMenu(player: Player, cards: List<Card>, type: CardChoiceType): Card? {
			val lock = ReentrantLock()
			val cond = lock.newCondition()
			val items = cards.prettify()
			val result = arrayOfNulls<Card>(1)
			runOnUiThread {
				newDialogBuilderINGAME().setTitle(player.piece.toString() + prettify(type.name))
					.setItems(items) { _, which: Int ->
						result[0] = cards.get(which)
						lock.withLock { cond.signal() }
					}.setNegativeButton(R.string.popup_button_cancel) { _, _: Int ->
						if (canCancel()) {
							cancel()
						} else {
							stopGameThread()
						}
						lock.withLock { cond.signal() }
					}.show()
			}
			lock.withLock { cond.await() }
			return result[0]
		}

		override fun showChooseTradeMenu(player: Player, list: List<Trade>): Trade? {
			val lock = ReentrantLock()
			val cond = lock.newCondition()
			val items = list.prettify()
			val result = arrayOfNulls<Trade>(1)
			runOnUiThread {
				newDialogBuilderINGAME().setTitle("${player.piece} Choose Trade")
					.setItems(items) { _, which: Int ->
						val t: Trade = list[which]
						showPurchasePropertyDialog("Buy ${prettify(t.card.property.name)} from ${prettify(t.trader.piece.name)}",
							"Buy for $${t.price}", t.card.property) {
							result[0] = t
							lock.withLock { cond.signal() }
						}
					}.show()
			}
			lock.withLock { cond.await() }
			return result[0]
		}

		private fun openMarkSaleableMenu(playerUser: PlayerUser, list: List<Card>, lock: ReentrantLock, cond: Condition) {
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
					tvLabel.text = prettify(card.property.name)
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
				newDialogBuilderINGAME().setTitle("Set Cost for " + card.property.name)
					.setView(np).setNeutralButton("Done") { _: DialogInterface?, _: Int ->
						playerUser.setSellableCard(card, np.value * STEP)
						openMarkSaleableMenu(playerUser, list, lock, cond)
					}.show()
			}
			newDialogBuilderINGAME()
				.setTitle("${playerUser.piece} Mark Saleable")
				.setView(view).setNeutralButton("Done") { _, _ -> lock.withLock { cond.signal() } }
				.show()
		}

		override fun showMarkSellableMenu(playerUser: PlayerUser, list: List<Card>): Boolean {
			val lock = ReentrantLock()
			val cond = lock.newCondition()
			runOnUiThread { openMarkSaleableMenu(playerUser, list, lock, cond) }
			lock.withLock { cond.await() }
			return true
		}

		override fun onError(t: Throwable?) {
			t?.printStackTrace()
			FileUtils.tryCopyFile(saveFile, File(externalStorageDirectory, "monopoly_error.txt"))
			stopGameThread()
			runOnUiThread {
				newDialogBuilder().setTitle(R.string.popup_title_error)
					.setMessage(t.toString())
					.setNegativeButton(R.string.popup_button_ok, null)
					.show()
			}
		}

		override fun drawPropertyCard(g: AGraphics, property: Square, w: Float, h: Float) {
			//(g as DroidGraphics).textHeight = resources.getDimension(R.dimen.dialog_purchase_text_size)
			with(g as DroidGraphics) {
				//textHeight = resources.getDimension(R.dimen.dialog_purchase_text_size)
				paint.typeface = Typeface.DEFAULT_BOLD
			}

			super.drawPropertyCard(g, property, w, h)
		}
	}

	fun newDialogBuilderINGAME(): AlertDialog.Builder {
		val builder = super.newDialogBuilder()
		if (monopoly.canCancel()) {
			builder.setNegativeButton(R.string.popup_button_cancel) { _, _ ->
				monopoly.cancel()
			}
		} else {
			builder.setNegativeButton("Exit") { _, _ -> monopoly.stopGameThread() }
		}
		return builder
	}

	fun showPurchasePropertyDialog(title: String, buyLabel: String, property: Square, onBuyRunnable: Runnable) {
		newDialogBuilder().setTitle(title).setView(object : DroidView(this, false) {
			override fun onPaint(g: DroidGraphics) {
				g.setTextModePixels(false)
				g.textHeight = 16f
				g.setTextModePixels(true)
				monopoly.drawPropertyCard(g, property, g.viewportWidth.toFloat(), g.viewportHeight.toFloat())
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
		}).setNegativeButton("Don't Buy", null)
		.setPositiveButton(buyLabel) { _, _ ->
			onBuyRunnable.run()
		}.show()
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
		if (saveFile!!.exists()) {
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
		g.setTextModePixels(false)
		g.textHeight = 16f
		g.setTextModePixels(true)
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
		/*
        redraw();
        getContent().postDelayed(new Runnable() {
            public void run() {
                tx = ty = -1;
                monopoly.onClick();
            }
        }, 100);*/if (!monopoly.isGameRunning) {
			if (saveFile!!.exists()) {
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
						saveFile!!.delete()
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
				compPlayers.setValue(compPlayers.value - 1)
			} else if (total < 2) {
				compPlayers.setValue(compPlayers.value + 1)
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
		val startMoneyValues = resources.getIntArray(R.array.start_money_values)
		val winMoneyValues = resources.getIntArray(R.array.win_money_values)
		val taxPercentValues = resources.getIntArray(R.array.tax_scale_percent_values)
		val maxJailTurnsValues = resources.getIntArray(R.array.max_turns_in_jail_values)
		val binding = GameConfigDialogBinding.inflate(layoutInflater)
		val startMoney = prefs.getInt("startMoney", 1000)
		val winMoney = prefs.getInt("winMoney", 5000)
		val taxPercent = prefs.getInt("taxPercent", 100)
		val jailBump = prefs.getBoolean("jailBump", false)
		val jailMulti = prefs.getBoolean("jailMulti", false)
		val maxJainTurns = prefs.getInt("maxJailTurns", 3)
		val rules = monopoly.rules
		binding.npStartMoney.init(startMoneyValues, startMoney, { value: Int -> "$${value}" }, { _: NumberPicker?, _: Int, newVal: Int -> prefs.edit().putInt("startMoney", startMoneyValues[newVal]).apply() })
		binding.npWinMoney.init(winMoneyValues, winMoney, { value: Int -> "$$value" }, { _: NumberPicker?, _: Int, newVal: Int -> prefs.edit().putInt("winMoney", winMoneyValues[newVal]).apply() })
		binding.npTaxScale.init(taxPercentValues, taxPercent, { value: Int -> "$value%" }, { _: NumberPicker?, _: Int, newVal: Int -> prefs.edit().putInt("taxPercent", taxPercentValues[newVal]).apply() })
		binding.npJailMaxTurns.init(maxJailTurnsValues, maxJainTurns, { value: Int -> "$value" }, { _, _, newVal: Int -> prefs.edit().putInt("maxJailTurns", maxJailTurnsValues[newVal]).apply() })
		binding.cbJailBumpEnabled.isChecked = jailBump
		binding.cbJailBumpEnabled.setOnCheckedChangeListener { _, isChecked: Boolean -> prefs.edit().putBoolean("jailBump", isChecked).apply() }
		binding.cbJailMultiplierEnabled.isChecked = jailMulti
		binding.cbJailMultiplierEnabled.setOnCheckedChangeListener { _, isChecked:Boolean -> prefs.edit().putBoolean("jailMulti", isChecked).apply() }
		newDialogBuilder().setTitle("Configure").setView(binding.root)
			.setPositiveButton("Setup Players") { _, _ ->
				rules.startMoney = startMoneyValues[binding.npStartMoney.value]
				rules.valueToWin = winMoneyValues[binding.npWinMoney.value]
				rules.taxScale = 0.01f * taxPercentValues[binding.npTaxScale.value]
				rules.jailBumpEnabled = binding.cbJailBumpEnabled.isChecked
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

	override fun getDialogTheme(): Int {
		return R.style.MonopolyDialogTheme
	}

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