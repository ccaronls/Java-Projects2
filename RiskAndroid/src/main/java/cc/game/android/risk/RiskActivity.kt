package cc.game.android.risk

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import cc.lib.android.DroidActivity
import cc.lib.android.DroidGraphics
import cc.lib.reflector.RBufferedReader
import cc.lib.risk.Action
import cc.lib.risk.Army
import cc.lib.risk.RiskBoard
import cc.lib.risk.RiskGame
import cc.lib.risk.UIRisk
import cc.lib.risk.UIRisk.Buttons
import cc.lib.utils.FileUtils
import cc.lib.utils.prettify
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class RiskActivity : DroidActivity(), OnItemClickListener {

	private val board by lazy {
		RiskBoard().also {
			assets.open("risk.board").use { reader -> it.deserialize(reader) }
		}
	}

	val game by lazy {
		object : UIRisk(board) {
			override val saveGame: File = File(filesDir, "save.game")

			override val storedGames: Map<String, File>
				get() = filesDir.listFiles { file ->
					file.extension == "saved"
				}.map {
					FileUtils.stripExtension(it.name) to it
				}.toMap()

			override fun showDiceDialog(
				attacker: Army,
				attackingDice: IntArray,
				defender: Army,
				defendingDice: IntArray,
				result: BooleanArray
			) {
				DiceDialog(this@RiskActivity, attacker, defender, attackingDice, defendingDice, result)
			}

			override fun initMenu(buttons: List<*>) {
				runOnUiThread {
					listView.adapter = object : BaseAdapter() {
						override fun getCount(): Int = buttons.size
						override fun getItem(position: Int): Any? = null
						override fun getItemId(position: Int): Long = 0

						override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
							val convertView = convertView
								?: View.inflate(this@RiskActivity, R.layout.list_item, null)
							val b = convertView.findViewById<TextView>(R.id.text_view)
							b.tag = buttons[position]
							b.text = buttons[position]?.prettify() ?: ""
							return b
						}
					}
				}
			}

			override val boardImageId = R.drawable.risk_board

			override fun redraw() {
				this@RiskActivity.redraw()
			}

			override fun deserialize(`in`: RBufferedReader?) {
				super.deserialize(`in`)
				redraw()
			}
		}
	}

	lateinit var listView: ListView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		listView = findViewById(R.id.list_view)
		listView.onItemClickListener = this
		game.tryLoadFromFile(game.saveGame)
	}

	override val contentViewId: Int = R.layout.activity_main

	override fun onStart() {
		super.onStart()
		game.initHomeMenu()
	}

	override fun onStop() {
		game.stopGameThread()
		super.onStop()
	}

	override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
		Log.d(TAG, "onItemClick: ${view.tag}")
		when (view.tag) {
			is Action -> game.setGameResult(view.tag)
			Buttons.NEW_GAME -> PlayerChooserDialog(this)
			Buttons.ABOUT -> newDialogBuilder().setTitle("About")
				.setMessage("Game written by Chris Caron")
				.setNegativeButton(R.string.popup_button_close, null)
				.show()

			Buttons.RESUME -> {
				if (game.tryLoadFromFile(game.saveGame)) {
					game.startGameThread()
				}
			}

			Buttons.SAVE -> {
				if (game.saveGame.exists()) {
					val g = RiskGame()
					if (g.tryLoadFromFile(game.saveGame)) {
						val fileName = g.allPlayers.joinToString("_") {
							it.army.name
						} + SimpleDateFormat("MMM_dd_yy").format(Date())
						newEditTextDialog("Save current game as", "", fileName) {
							val target = File(filesDir, "$it.saved")
							if (target.exists()) {
								newDialogBuilder().setTitle(R.string.popup_title_error)
									.setMessage("File already exists")
									.setNegativeButton(R.string.popup_button_ok, null)
									.show()
							} else {
								game.saveGame.copyTo(target)
								game.tryLoadFromFile(game.saveGame)
							}
						}
					}
				}
			}

			Buttons.LOAD -> {
				val files = game.storedGames.map {
					it.key
				}.toTypedArray()
				newDialogBuilder()
					.setTitle("Load Game")
					.setItems(files) { _, fileIdx ->
						val file = game.storedGames[files[fileIdx]]
						game.tryLoadFromFile(file)
					}.setNegativeButton(R.string.popup_button_cancel, null)
					.show()
			}
		}
	}

	override fun onInit(g: DroidGraphics) {
		game.loadExplodAnim(g, R.drawable.blowup_anim)
	}
	
	
	override fun onDraw(g: DroidGraphics) {
		game.onDraw(g)
	}

	override fun onTap(x: Float, y: Float) {
		game.onTap(x, y)
	}

	override fun onDragStart(x: Float, y: Float) {
		game.onDragStart(x, y)
	}

	override fun onDrag(x: Float, y: Float) {
		game.onDrag(x, y)
	}

	override fun onBackPressed() {
		if (game.running) {
			game.stopGameThread()
		} else {
			super.onBackPressed()
		}
	}

}