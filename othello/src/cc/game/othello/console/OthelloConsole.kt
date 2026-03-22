package cc.game.othello.console

import cc.game.othello.ai.AiOthelloPlayer
import cc.game.othello.core.Othello
import cc.game.othello.core.OthelloBoard
import cc.game.othello.core.OthelloPlayer
import cc.lib.utils.KFileUtils.getOrCreateSettingsDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Locale
import kotlin.system.exitProcess

object OthelloConsole {

	suspend fun BufferedReader.readLineSuspending(): String? =
		withContext(Dispatchers.IO) {
			readLine()
		}

	@JvmStatic
	fun main(args: Array<String>) {
		val game = Othello()
		val consolePlayer = ConsolePlayer()
		val aiPlayer = AiOthelloPlayer()

		try {
			val file = File(OthelloConsole::class.java.getOrCreateSettingsDirectory(), "othello.txt")
			if (file.exists()) game.loadFromFile(file) else {
				game.intiPlayers(consolePlayer, aiPlayer)
				game.newGame()
			}
			while (!game.isGameOver) {
				drawGame(game)
				game.runGame()
				game.saveToFile(file)
			}
			println("Game Over")
		} catch (e: Exception) {
			e.printStackTrace()
			exitProcess(1)
		}
		exitProcess(0)
	}

	fun getCommand(): String {
		val input = BufferedReader(InputStreamReader(System.`in`))
		try {
			return input.readLine()
		} catch (e: Exception) {
			e.printStackTrace()
			exitProcess(1)
		}
	}

	val PC = arrayOf(arrayOf(
		"     ",
		"     ",
		"     "
	), arrayOf(
		".   .",
		"     ",
		".   ."
	), arrayOf(
		"/---\\",
		"|   |",
		"\\___/"
	), arrayOf(
		"#####",
		"#####",
		"#####"
	))

	fun drawGame(game: Othello) {
		val b = game.board
		val s = StringBuffer("  ")
		for (c in 0 until game.board.numCols) {
			s.append("+-----")
		}
		s.append("+\n")
		for (r in 0 until b.numRows) {
			s.append("" + (('A'.code + r).toChar()) + " ")
			for (i in 0..2) {
				for (c in 0 until game.board.numCols) {
					val p = b[r, c]
					s.append("|").append(PC[p][i])
				}
				s.append("|\n  ")
			}
			//s.append("  ");
			for (c in 0 until game.board.numCols) {
				s.append("+-----")
			}
			s.append("+\n")
		}
		s.append("  ")
		for (c in 0 until game.board.numCols) {
			s.append("   " + (('A'.code + c).toChar()) + "  ")
		}
		s.append("\n")
		s.append("\n\nWHITE = " + b.getCellCount(OthelloBoard.CELL_WHITE) + "\nBLACK = " + b.getCellCount(OthelloBoard.CELL_BLACK) + "\n\n>")
		println(s.toString())
	}

	class ConsolePlayer() : OthelloPlayer() {
		override fun chooseCell(board: OthelloBoard, rowColCell: IntArray): Boolean {
			getCommand()?.takeIf { it.length > 1 }?.let { cmd ->
				rowColCell[0] = cmd.lowercase(Locale.getDefault())[0].code - 'a'.code
				rowColCell[1] = cmd.lowercase(Locale.getDefault())[1].code - 'a'.code
				return true
			}
			return false
		}
	}
}
