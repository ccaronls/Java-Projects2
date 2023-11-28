package cc.applets.soap

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Utils
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTKeyboardAnimationApplet

/**
 * Snakes writhing on the desert plain (not plane!)
 * @author ccaron
 */
class SnakesOnAPlain : AWTKeyboardAnimationApplet() {
	override fun doInitialization() {
		// TODO Auto-generated method stub
	}

	override fun drawFrame(g: AGraphics) {
		this.clearScreen(GColor.WHITE)

		// TODO Auto-generated method stub
		if (getMouseButtonClicked(0) || getKeyboardReset('f')) {
			val food = SnakeFood(mouseX.toFloat(), mouseY.toFloat())
			snakeFood.add(food)
		}
		if (getMouseButtonClicked(1) || getKeyboardReset('s')) {
			addSnake(mouseX.toFloat(), mouseY.toFloat())
		}
		g.color = GColor.GRAY
		run {
			var i = 0
			while (i < snakeFood.size) {
				val food = snakeFood[i]
				if (food!!.eaten) {
					snakeFood.removeAt(i)
					continue
				}
				i++
				g.drawFilledCircle(food.x, food.y, 3f)
			}
		}
		for (i in snakes.indices) {
			val s = snakes[i]
			var target = s!!.target
			if (true) { //target == null) {
				var minD = Float.MAX_VALUE
				for (f in snakeFood.indices) {
					val food = snakeFood[f]
					val d = s.getDistanceTo(food!!.x, food.y)
					if (d < minD) {
						target = food
						minD = d
					}
				}
				s.target = target
			}
			s.move()
			s.draw(g)
		}
	}

	override fun onDimensionsChanged(g: AGraphics, width: Int, height: Int) {
		g.ortho(0f, width.toFloat(), 0f, height.toFloat())
	}

	fun addSnake(x: Float, y: Float) {
		val snake = Snake(x, y)
		snakes.add(snake)
	}

	private val snakes = ArrayList<Snake>()
	private val snakeFood = ArrayList<SnakeFood>()

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			val frame = AWTFrame("Snakes on a Plain?")
			val app: AWTKeyboardAnimationApplet = SnakesOnAPlain()
			frame.add(app)
			app.init()
			frame.centerToScreen(500, 500)
			app.start()
			app.setMillisecondsPerFrame(20)
		}
	}
}