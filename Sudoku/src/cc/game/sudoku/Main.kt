package cc.game.sudoku

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Utils
import cc.lib.swing.AWTButton
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTKeyboardAnimationApplet
import java.awt.GridLayout
import java.awt.Menu
import java.awt.MenuBar
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.util.Random

class Main : AWTKeyboardAnimationApplet(), ActionListener {
	var sudoku = Sudoku(Random(System.currentTimeMillis()))
	var showHints = false
	var curRow = -1
	var curCol = -1
	var curNum = -1
	override fun onDimensionsChanged(g: AGraphics, width: Int, height: Int) {
		g.ortho(0, width, 0, height)
	}

	override fun doInitialization() {
		sudoku.generate(Sudoku.MEDIUM)
	}

	fun drawRect(g: AGraphics, x: Int, y: Int, w: Int, h: Int, thickness: Int) {
		val start = -thickness / 2
		val end = thickness / 2
		for (i in start..end) g.drawRect(x + i, y + i, w - 2 * i, h - 2 * i)
	}

	fun isInside(px: Int, py: Int, x: Int, y: Int, w: Int, h: Int): Boolean {
		return px > x && px < x + w && py > y && py < y + h
	}

	fun isMouseInside(x: Int, y: Int, w: Int, h: Int): Boolean {
		return isInside(mouseX, mouseY, x, y, w, h)
	}

	override fun drawFrame(g: AGraphics) {
		val width = screenWidth
		val height = screenHeight

		// clear screen
		g.color = GColor.BLACK
		g.drawFilledRect(0, 0, width, height)
		curCol = curNum - 1
		curRow = curCol
		val solved = sudoku.isSolved
		val dw = width / Sudoku.SUDOKU_DIM
		val dh = height / Sudoku.SUDOKU_DIM

		// draw the grid
		if (solved) g.color = GColor(sudoku.rand() % 256, sudoku.rand() % 256, sudoku.rand() % 256) else g.color =
			GColor.LIGHT_GRAY
		for (i in 0..Sudoku.SUDOKU_DIM) {
			g.drawLine(dw * i, 0, dw * i, height)
			g.drawLine(0, dh * i, width, dh * i)
		}

		// draw the sub-cells
		g.color = GColor.CYAN
		for (i in 0..Sudoku.SUDOKU_DIM / 3) {
			for (t in -1..1) {
				g.drawLine(dw * 3 * i + t, 0, dw * 3 * i + t, height)
				g.drawLine(0, dh * 3 * i + t, width, dh * 3 * i + t)
			}
		}
		var leftPressed = getMouseButtonClicked(0)

		// draw the numbers
		for (i in 0 until Sudoku.SUDOKU_DIM) {
			for (j in 0 until Sudoku.SUDOKU_DIM) {
				val x = i * dw
				val y = j * dh
				val inside = isMouseInside(x, y, dw, dh)
				val canEdit = sudoku.canEdit(i, j)
				val num = sudoku.getBoard(i, j)
				if (inside && canEdit) {
					// highlight this square in red
					g.color = GColor.RED
					drawRect(g, x, y, dw, dh, 3)
					curRow = i
					curCol = j
				}
				if (num > 0) {
					if (canEdit) {
						g.color = GColor.YELLOW
					} else {
						g.color = GColor.WHITE
					}
					drawNumber(g, num, x, y, dw, dh)
					if (inside && leftPressed && canEdit) {
						sudoku.setBoard(i, j, 0)
						leftPressed = false
					}
				} else if (inside && canEdit && showHints) {
					curNum = -1
					drawHints(g, x, y, dw, dh)
					if (leftPressed && curNum >= 0) {
						sudoku.setBoard(i, j, curNum)
						leftPressed = false
					}
				}
			}
		}
	}

	fun drawNumber(g: AGraphics, num: Int, x: Int, y: Int, w: Int, h: Int) {
		val size = g.textHeight
		val tx = x + w / 2 - size / 2
		val ty = y + h / 2 + size / 2
		g.drawString(num.toString(), tx, ty)
	}

	fun drawHints(g: AGraphics, x: Int, y: Int, w: Int, h: Int) {
		var x = x
		var y = y
		val startX = x
		val dw = w / 3
		val dh = h / 3
		var numHints = 0
		for (i in 1..9) if (sudoku.canSetSquare(i, curRow, curCol)) numHints++
		var numDrawn = 0
		for (i in 1..9) {
			if (sudoku.canSetSquare(i, curRow, curCol)) {
				if (numHints == 1 || isMouseInside(x, y, dw, dh)) {
					curNum = i
					g.color = GColor.YELLOW
				} else {
					g.color = GColor.GREEN
				}
				drawNumber(g, i, x, y, dw, dh)
				if (++numDrawn >= 3) {
					numDrawn = 0
					x = startX
					y += dh
				} else {
					x += dw
				}
			}
		}
	}

	override fun actionPerformed(ev: ActionEvent) {
		val cmd = ev.actionCommand
		if (cmd == "Easy") {
			sudoku.generate(Sudoku.EASY)
		} else if (cmd == "Medium") {
			sudoku.generate(Sudoku.MEDIUM)
		} else if (cmd == "Hard") {
			sudoku.generate(Sudoku.HARD)
		} else if (cmd == "Solve") {
			sudoku.solve()
		} else if (cmd == "Enable") {
			showHints = true
		} else if (cmd == "Disable") {
			showHints = false
		} else if (cmd == "Quit") {
			System.exit(0)
		} else {
			System.err.println("ERROR: Unhandled cmd [$cmd]")
		}
	}

	fun showNewMenu() {
		val frame: AWTFrame = object : AWTFrame("NEW") {
			fun onClosing() {
				hide()
			}
		}
		frame.layout = GridLayout(3, 1)
		val listener = ActionListener { ev ->
			val cmd = ev.actionCommand
			if (cmd == "EASY") {
				sudoku.generate(Sudoku.EASY)
			} else if (cmd == "MEDIUM") {
				sudoku.generate(Sudoku.MEDIUM)
			} else if (cmd == "HARD") {
				sudoku.generate(Sudoku.HARD)
			} else {
				System.err.println("Unhandled cmd [$cmd]")
			}
			frame.hide()
		}
		frame.add(AWTButton("EASY", listener))
		frame.add(AWTButton("MEDIUM", listener))
		frame.add(AWTButton("HARD", listener))
		frame.centerToScreen()
	}

	public override fun onKeyPressed(ev: KeyEvent) {
		if (ev.keyChar == 's' || ev.keyChar == 'S') {
			sudoku.solve()
		} else if (ev.keyChar == 'h' || ev.keyChar == 'H') {
			showHints = !showHints
		} else if (ev.keyChar == 'n' || ev.keyChar == 'N') {
			showNewMenu()
		} else if (ev.keyChar >= '0' && ev.keyChar <= '9' && curRow >= 0 && curCol >= 0) {
			val num = ev.keyChar.code - '0'.code
			sudoku.setBoard(curRow, curCol, num)
		}
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			val frame = AWTFrame()
			val main = Main()
			val menuBar = MenuBar()
			val newMenu = Menu("NEW")
			val hintsMenu = Menu("HINTS")
			var gameMenu = Menu("GAME")
			newMenu.add("Easy")
			newMenu.add("Medium")
			newMenu.add("Hard")
			newMenu.addActionListener(main)
			hintsMenu.add("Enable")
			hintsMenu.add("Disable")
			hintsMenu.addActionListener(main)
			gameMenu = Menu("GAME")
			gameMenu.add(newMenu)
			gameMenu.add(hintsMenu)
			gameMenu.add("Solve")
			gameMenu.add("Quit")
			gameMenu.addActionListener(main)
			menuBar.add(gameMenu)
			frame.menuBar = menuBar
			frame.add(main)
			frame.centerToScreen(640, 480)
			main.start()
			main.init()
		}
	}
}
