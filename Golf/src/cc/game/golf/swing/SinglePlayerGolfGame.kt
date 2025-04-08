package cc.game.golf.swing

import cc.game.golf.ai.PlayerBot
import cc.game.golf.core.Card
import cc.game.golf.core.DrawType
import cc.game.golf.core.Golf
import cc.game.golf.core.State
import cc.game.golf.swing.GolfSwing
import cc.lib.utils.FileUtils
import cc.lib.utils.launchIn
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException

class SinglePlayerGolfGame internal constructor(val g: GolfSwing) : Golf(), IGolfGame {
	private val SAVE_FILE // = new File("savegame.txt");
		: File
	private val RULES_FILE // = new File("savedrules.txt");
		: File
	var running = false

	init {
		val settings = FileUtils.getOrCreateSettingsDirectory(GolfSwing::class.java)
		SAVE_FILE = File(settings, "savegame.txt")
		RULES_FILE = File(settings, "savedrules.txt")
		loadRules()
	}

	override val frontPlayer: Int
		get() {
			if (state == State.DEAL) return dealer
			return if (getPlayer(currentPlayer) is SwingPlayerUser) currentPlayer else 0
		}

	override fun getPlayerCards(player: Int): Array<Array<Card?>> {
		return getPlayer(player).getCards()
	}

	override fun getPlayerName(player: Int): String {
		return getPlayer(player).name
	}

	override fun getHandPoints(player: Int): Int {
		return getPlayer(player).getHandPoints(this)
	}

	override fun getPlayerPoints(player: Int): Int {
		return getPlayer(player).points
	}

	override fun getPlayerCard(player: Int, row: Int, col: Int): Card {
		return getPlayer(player).getCard(row, col)
	}

	override fun message(format: String, vararg params: Any?) {
		super.message(format, *params)
		g.setMessage(String.format(format, *params))
	}

	override fun onKnock(player: Int) {
		// TODO Auto-generated method stub
		super.onKnock(player)
	}

	override fun onCardSwapped(player: Int, dtstack: DrawType?, drawn: Card?, swapped: Card?, row: Int, col: Int) {
		g.startSwapCardAnimation(player, dtstack!!, drawn!!, row, col)
	}

	override fun onCardDiscarded(player: Int, dtstack: DrawType?, swapped: Card?) {
		g.startDiscardDrawnCardAnimation(swapped!!)
	}

	override fun onDealCard(player: Int, card: Card?, row: Int, col: Int) {
		g.startDealCardAnimation(player, card!!, row, col)
	}

	override fun onCardTurnedOver(player: Int, card: Card?, row: Int, col: Int) {
		g.startTurnOverCardAnimation(player, card!!, row, col)
	}

	override fun onDrawPileChoosen(player: Int, type: DrawType?) {
		if (type == DrawType.DTStack) {
			g.startTurnOverCardAnimationStack()
		}
	}

	override fun onChooseCardToSwap(player: Int, card: Card?, row: Int, col: Int) {
		if (!card!!.isShowing) g.startTurnOverCardAnimation(player, card, row, col)
	}

	suspend fun run() {
		var prevState = State.INIT
		try {
			while (running) {
				if (prevState != state) {
					prevState = state
					saveGame(SAVE_FILE)
				}
				if (prevState != state) println("Processing state: $state")
				when (state) {
					State.DEAL -> {}
					State.TURN_OVER_CARDS, State.SETUP_DISCARD_PILE, State.PLAY, State.DISCARD_OR_PLAY -> {}
					State.END_ROUND, State.GAME_OVER -> delay(20000)
					State.INIT -> {}
					State.PROCESS_ROUND -> {}
					State.SHUFFLE -> {}
					State.TURN -> {}
				}
				if (running)
					runGame()
			}
		} catch (e: Throwable) {
			e.printStackTrace()
		}
		println("Thread exiting")
		clear()
		running = false
	}

	private fun startThread() {
		if (running) return
		running = true
		launchIn {
			run()
		}
	}

	fun loadRules() {
		if (RULES_FILE.exists()) {
			try {
				rules.loadFromFile(RULES_FILE)
				clear()
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	fun saveRules() {
		rules.trySaveToFile(RULES_FILE)
		clear()
	}

	override fun canResume(): Boolean {
		return SAVE_FILE.exists()
	}

	override fun updateRules() {
		saveRules()
	}

	override fun quit() {
		running = false
	}

	override val isRunning: Boolean
		get() = running

	@Throws(IOException::class)
	override fun resume() {
		loadGame(SAVE_FILE)
		for (i in 0 until numPlayers) {
			val p = getPlayer(i)
			if (p != null && p is SwingPlayerUser) {
				p.setGolfSwing(g)
			}
		}
		startThread()
	}

	override fun startNewGame() {
//        addPlayer(new SwingPlayerUser("Chris0", g));
//        addPlayer(new SwingPlayerUser("Chris1", g));
//        addPlayer(new SwingPlayerUser("Chris2", g));
//        addPlayer(new SwingPlayerUser("Chris3", g));
		addPlayer(SwingPlayerUser("Chris", g))
		addPlayer(PlayerBot("Harry"))
		addPlayer(PlayerBot("Phil"))
		addPlayer(PlayerBot("Tom"))
		startThread()
	}
}
