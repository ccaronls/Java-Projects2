package cc.game.android.checkerboard

import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import cc.lib.android.DroidActivity
import cc.lib.android.DroidGraphics
import cc.lib.checkerboard.*
import java.io.File

class MainActivity : DroidActivity() {

	lateinit var saveFile: File

	private val game: UIGame = object : UIGame() {
		override fun repaint(delayMs: Long) {
			redraw()
		}

		override val checkerboardImageId: Int = R.drawable.wood_checkerboard_8x8
		override val kingsCourtBoardId: Int = R.drawable.kings_court_board_8x8

		override fun getPieceImageId(p: PieceType, color: Color): Int {
			when (p) {
				PieceType.PAWN, PieceType.PAWN_IDLE, PieceType.PAWN_ENPASSANT, PieceType.PAWN_TOSWAP -> return if (color == Color.WHITE) R.drawable.wt_pawn else R.drawable.bk_pawn
				PieceType.BISHOP -> return if (color == Color.WHITE) R.drawable.wt_bishop else R.drawable.bk_bishop
				PieceType.KNIGHT_R, PieceType.KNIGHT_L -> return if (color == Color.WHITE) R.drawable.wt_knight else R.drawable.bk_knight
				PieceType.ROOK, PieceType.ROOK_IDLE -> return if (color == Color.WHITE) R.drawable.wt_rook else R.drawable.bk_rook
				PieceType.QUEEN -> return if (color == Color.WHITE) R.drawable.wt_queen else R.drawable.bk_queen
				PieceType.CHECKED_KING, PieceType.CHECKED_KING_IDLE, PieceType.UNCHECKED_KING, PieceType.UNCHECKED_KING_IDLE, PieceType.KING -> return if (color == Color.WHITE) R.drawable.wt_king else R.drawable.bk_king
				PieceType.DRAGON_R, PieceType.DRAGON_L, PieceType.DRAGON_IDLE_R, PieceType.DRAGON_IDLE_L -> return if (color == Color.WHITE) R.drawable.wt_dragon else R.drawable.bk_dragon
				PieceType.FLYING_KING -> {
				}
				PieceType.CHECKER, PieceType.DAMA_MAN, PieceType.DAMA_KING, PieceType.CHIP_4WAY -> return when (color) {
					Color.RED   -> R.drawable.red_checker
					Color.WHITE -> R.drawable.wt_checker
					Color.BLACK -> R.drawable.blk_checker
				}
			}
			return 0
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		saveFile = File(filesDir, "save.game")
		val orientation = prefs.getInt("orientation", -1)
		if (orientation >= 0) {
			requestedOrientation = orientation
		}
		if (!game.init(saveFile)) {
			saveFile.delete()
		}
	}

	override fun onResume() {
		super.onResume()
		if (saveFile.exists()) {
			newDialogBuilder().setTitle("Resume Previous " + game.rules.javaClass.simpleName + " Game?")
				.setItems(arrayOf("Resume", "New Game")) { dialog, which ->
					when (which) {
						0 -> {
							title = game.rules.javaClass.simpleName
							game.startGameThread()
							game.repaint(-1)
						}
						1 -> showNewGameDialog()
					}
				}.show()
			return
		}
		showNewGameDialog()
	}

	fun showNewGameDialog() {
		val items = arrayOf("Checkers", "Suicide", "Draughts", "Canadian Draughts", "Dama", "Chess", "Dragon Chess", "Ugolki", "Columns", "Kings Court", "Shashki")
		game.stopGameThread()
		newDialogBuilder().setItems(items) { dialog, which ->
			when (items[which]) {
				"Checkers" -> game.rules = Checkers()
				"Suicide" -> game.rules = Suicide()
				"Draughts" -> game.rules = Draughts()
				"Canadian Draughts" -> game.rules = CanadianDraughts()
				"Dama" -> game.rules = Dama()
				"Chess" -> game.rules = Chess()
				"Dragon Chess" -> game.rules = DragonChess()
				"Ugolki" -> game.rules = Ugolki()
				"Columns" -> game.rules = Columns()
				"Kings Court" -> game.rules = KingsCourt()
				"Shashki" -> game.rules = Shashki()
			}
			title = items[which]
			showChoosePlayersDialog()
		}.show()
	}

	fun showChoosePlayersDialog() {
		newDialogBuilder().setTitle("How many players?")
			.setItems(arrayOf("One", "Two")) { dialog, which ->
				when (which) {
					0 -> showChooseDifficultyDialog { dialog1: DialogInterface?, which1: Int -> showChoosePlayersDialog() }
					1 -> {
						game.setPlayer(Game.NEAR, UIPlayer(UIPlayer.Type.USER))
						game.setPlayer(Game.FAR, UIPlayer(UIPlayer.Type.USER))
						startGame()
					}
				}
			}.setNegativeButton("Back") { dialog, which -> showNewGameDialog() }.show()
	}

	fun showChooseDifficultyDialog(onCancelAction: DialogInterface.OnClickListener?) {
		newDialogBuilder().setTitle("Difficulty set to $difficultyString")
			.setItems(arrayOf("Easy", "Medium", "Hard")) { dialog, which ->
				prefs.edit().putInt("difficulty", which + 1).apply()
				game.setPlayer(Game.NEAR, UIPlayer(UIPlayer.Type.USER))
				game.setPlayer(Game.FAR, UIPlayer(UIPlayer.Type.AI, which + 1))
				startGame()
			}.setNegativeButton("Cancel", onCancelAction).show()
	}

	val difficulty: Int
		get() = prefs.getInt("difficulty", 2)
	val difficultyString: String
		get() {
			var d: Int
			when (difficulty.also { d = it }) {
				1 -> return "Easy"
				2 -> return "Medium"
				3 -> return "Hard"
			}
			return d.toString()
		}

	fun startGame() {
		game.newGame()
		game.trySaveToFile(saveFile)
		game.repaint(-1)
	}

	override fun onPause() {
		super.onPause()
		val saveFile = File(filesDir, "save.game")
		game.stopGameThread()
		game.trySaveToFile(saveFile)
	}

	override fun onDraw(g: DroidGraphics) {
		g.setIdentity()
		g.ortho()
		g.setTextHeightDips(18f)
		//        g.getPaint().setTextSize(getResources().getDimension(R.dimen.txt_size_normal));
		game.draw(g, touchX, touchY)
		if (clicked) {
			game.doClick()
			clicked = false
		}
	}

	var touchX = 0
	var touchY = 0
	var clicked = false
	override fun onTouchDown(x: Float, y: Float) {
		if ( game.isGameRunning) {
			game.startGameThread()
			game.repaint(-1)
		} else {
			touchX = x.toInt()
			touchY = y.toInt()
			super.onTouchDown(x, y)
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		game.stopGameThread()
		menu.add("New Game")
		menu.add("Rules")
		menu.add("Difficulty")
		menu.add("Players")
		menu.add("Instructions")
		menu.add("Rotate Screen")
		return true
	}

	override fun onPrepareOptionsMenu(menu: Menu): Boolean {
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.title.toString()) {
			"New Game" -> {
				game.stopGameThread()
				showNewGameDialog()
			}
			"Rules" -> game.stopGameThread()
			"Difficulty" -> {
				game.stopGameThread()
				showChooseDifficultyDialog(null)
			}
			"Players" -> showChoosePlayersDialog()
			"Rotate Screen" -> {
				requestedOrientation = if (isPortrait) {
					prefs.edit().putInt("orientation", ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE).apply()
					ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
				} else {
					prefs.edit().putInt("orientation", ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT).apply()
					ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
				}
			}
			"Instructions" -> {
				game.showInstructions = true
				game.stopGameThread()
			}
			else            -> return false
		}
		return true
	}

	override fun onTap(x: Float, y: Float) {
		touchX = x.toInt()
		touchY = y.toInt()
		clicked = true
	}

	override fun onDragStart(x: Float, y: Float) {
		touchX = x.toInt()
		touchY = y.toInt()
		game.startDrag()
	}

	override fun onDragStop(x: Float, y: Float) {
		touchX = x.toInt()
		touchY = y.toInt()
		game.stopDrag()
	}

	override fun onDrag(x: Float, y: Float) {
		touchX = x.toInt()
		touchY = y.toInt()
	}

	override fun onBackPressed() {
		if (game.canUndo()) {
			game.stopGameThread()
			game.undoAndRefresh()
		} else {
			super.onBackPressed()
		}
	}
}