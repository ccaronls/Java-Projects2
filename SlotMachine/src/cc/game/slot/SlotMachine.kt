package cc.game.slot

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTKeyboardAnimationApplet

class SlotMachine : AWTKeyboardAnimationApplet() {
	private val wheelA = Wheel()
	private val wheelB = Wheel()
	private val wheelC = Wheel()
	val STATE_STOPPED = 0
	val STATE_SPINNING = 1
	var state = STATE_STOPPED
	override fun doInitialization() {
		// TODO Auto-generated method stub
		val cards = arrayOf(
			"BAR", "Cherry", "Orange", "Pirate", "Lime"
		)
		wheelA.generate(cards, 20)
		wheelB.generate(cards, 20)
		wheelC.generate(cards, 20)
	}

	override fun drawFrame(g: AGraphics) {

		//g.setFont(Font.getFont("Arial"));
		this.clearScreen()
		val maxX = this.screenWidth
		val maxY = this.screenHeight
		g.color = GColor.WHITE
		val x0 = 50
		val w0 = 50
		val y0 = 150
		val h0 = 150
		wheelA.draw(g, x0, y0 - 50, w0, h0 + 100)
		wheelB.draw(g, x0 + w0, y0 - 50, w0, h0 + 100)
		wheelC.draw(g, x0 + w0 * 2, y0 - 50, w0, h0 + 100)
		g.color = GColor.CYAN
		g.drawFilledRect(0f, 0f, maxX.toFloat(), y0.toFloat())
		g.drawFilledRect(0f, (y0 + h0).toFloat(), maxX.toFloat(), 300f)
		val cx = maxX / 2
		val cy = maxY / 2
		if (Utils.isDebugEnabled() && getKeyboard('q')) System.exit(0)
		when (state) {
			STATE_STOPPED -> {
				g.color = GColor.WHITE
				g.drawJustifiedString((maxX - 20).toFloat(), cy.toFloat(), Justify.RIGHT, Justify.CENTER, "Press spacebar to spin")
				val randFactor = 200
				if (getKeyboardReset(' ')) {
					wheelA.setVelocity(randFactor + Utils.randFloat(randFactor.toFloat()))
					wheelB.setVelocity(-randFactor - Utils.randFloat(randFactor.toFloat()))
					wheelC.setVelocity(randFactor + Utils.randFloat(randFactor.toFloat()))
					state = STATE_SPINNING
				}
				g.color = GColor.CYAN
				g.drawLine(0f, (y0 + h0 / 2).toFloat(), maxX.toFloat(), (y0 + h0 / 2).toFloat(), 1f)
				g.drawJustifiedString(cx.toFloat(), (y0 + h0 / 3).toFloat(), Justify.LEFT, Justify.CENTER, wheelA.centerCardAt)
				g.drawJustifiedString(cx.toFloat(), (y0 + h0 / 2).toFloat(), Justify.LEFT, Justify.CENTER, wheelB.centerCardAt)
				g.drawJustifiedString(cx.toFloat(), (y0 + h0 * 2 / 3).toFloat(), Justify.LEFT, Justify.CENTER, wheelC.centerCardAt)
			}
			STATE_SPINNING -> {
				val currentTime = System.currentTimeMillis()
				var delta = (currentTime - lastTime).toFloat()
				lastTime = currentTime
				delta *= 0.001f // convert from miliseconds to seconds
				wheelA.spin(delta)
				wheelB.spin(delta)
				wheelC.spin(delta)
				if (wheelA.isStopped &&
					wheelB.isStopped &&
					wheelC.isStopped) {
					state = STATE_STOPPED
				}
			}
		}
	}

	override fun onDimensionsChanged(g: AGraphics, width: Int, height: Int) {
		g.ortho()
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			val app: AWTKeyboardAnimationApplet = SlotMachine()
			val frame = AWTFrame("SlotMachine DEBUG")
			frame.add(app)
			frame.centerToScreen(600, 400)
			app.init()
			app.start()
			app.focusGained(null)
		}

		var lastTime = System.currentTimeMillis()
	}
}