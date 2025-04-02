package cc.experiments

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Utils
import cc.lib.swing.AWTComponent
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTGraphics
import cc.lib.utils.random
import cc.lib.utils.removeRandom

class ForestGenerator : AWTComponent() {

	init {
		setMouseEnabled(true)
	}

	var forest = Array(100) {
		Array(100) { -1f }
	}

	data class Cell(
		val x: Int,
		val y: Int,
		val density: Float
	)

	fun generate(g: AGraphics, x: Int, y: Int) {
		forest.forEach {
			//it.fill(-1f)
		}
		val fx = x * 100 / g.viewportWidth
		val fy = y * 100 / g.viewportHeight

		val list = mutableListOf<Cell>()

		fun test(tx: Int, ty: Int, d: Float) {
			if (d > 0f && tx in 0..99 && ty in 0..99 && forest[tx][ty] < 0) {
				list.add(Cell(tx, ty, d))
			}
		}

		list.add(Cell(fx, fy, 1f))

		// tuning variables
		val f1 = -.005f
		val f2 = .1f

		while (list.isNotEmpty()) {
			val cell = list.removeRandom()
			forest[cell.x][cell.y] = cell.density
			test(cell.x - 1, cell.y, (cell.density - (f1 + f2.random())).coerceAtLeast(0f))
			test(cell.x + 1, cell.y, (cell.density - (f1 + f2.random())).coerceAtLeast(0f))
			test(cell.x, cell.y + 1, (cell.density - (f1 + f2.random())).coerceAtLeast(0f))
			test(cell.x, cell.y - 1, (cell.density - (f1 + f2.random())).coerceAtLeast(0f))
		}
	}

	override fun onDimensionChanged(g: AWTGraphics, width: Int, height: Int) {
		super.onDimensionChanged(g, width, height)
	}

	override fun init(g: AWTGraphics) {
		super.init(g)
	}

	override fun paint(g: AWTGraphics) {
		val dx = g.viewportWidth / 100
		val dy = g.viewportHeight / 100
		forest.forEachIndexed { x, floats ->
			floats.forEachIndexed { y, fl ->
				g.setColor(GColor.GREEN.withAlpha(fl))
				g.drawFilledRect((x * dx).toFloat(), (y * dy).toFloat(), dx.toFloat(), dy.toFloat())
			}
		}
	}

	override fun onClick() {
		generate(aPGraphics, mouseX, mouseY)
		redraw()
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			val frame: AWTFrame = object : AWTFrame("Animation Maker") {
				override fun onWindowClosing() {
					try {
						//app.figures.saveToFile(app.figuresFile);
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}
			}
			frame.add(ForestGenerator())
			frame.centerToScreen(600, 500)
		}
	}
}
