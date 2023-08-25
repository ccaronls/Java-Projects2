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
import cc.lib.game.Utils
import cc.lib.risk.Action
import cc.lib.risk.Army
import cc.lib.risk.RiskBoard
import cc.lib.risk.UIRisk
import cc.lib.utils.prettify
import java.io.File

class RiskActivity : DroidActivity(), OnItemClickListener {

	private val board by lazy {
		RiskBoard().also {
			assets.open("risk.board").use { `in` -> it.deserialize(`in`) }
		}
	}

	val game by lazy {
		object : UIRisk(board) {
			override val saveGame: File = File(filesDir, "save.game")

			override fun showDiceDialog(attacker: Army, attackingDice: IntArray, defender: Army, defendingDice: IntArray, result: BooleanArray) {
				DiceDialog(this@RiskActivity, attacker, defender, attackingDice, defendingDice, result)
			}

			override fun initMenu(buttons: List<*>) = _initMenu(buttons)

			override val boardImageId = R.drawable.risk_board

			override fun redraw() {
				this@RiskActivity.redraw()
			}
		}
	}

	lateinit var listView: ListView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		listView = findViewById(R.id.list_view)
		listView.onItemClickListener = this
		hideNavigationBar()
	}

	override fun getContentViewId(): Int {
		return R.layout.activity_main
	}

	override fun onStart() {
		super.onStart()
		initHomeMenu()
	}

	override fun onStop() {
		game.stopGameThread()
		super.onStop()
	}

	internal enum class Buttons {
		NEW_GAME,
		RESUME,
		ABOUT
	}

	fun initHomeMenu() {
		if (game.saveGame.exists()) {
			_initMenu(Utils.toList(*Buttons.values()))
		} else {
			_initMenu(Utils.toList(Buttons.NEW_GAME, Buttons.ABOUT))
		}
	}

	override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
		Log.d(TAG, "onItemClick: ${view.tag}")
		when (view.tag) {
			is Action -> game.setGameResult(view.tag)
			Buttons.NEW_GAME -> PlayerChooserDialog(this)
			Buttons.ABOUT    -> newDialogBuilder().setTitle("About")
					.setMessage("Game written by Chris Caron")
				.setNegativeButton(R.string.popup_button_close, null)
				.show()
			Buttons.RESUME -> {
				if (game.tryLoadFromFile(game.saveGame)) {
					game.startGameThread()
				}
			}
		}
	}

	fun _initMenu(buttons: List<*>) {
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
					b.text = prettify(buttons[position] ?: "")
					return b
				}
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