package cc.applets.misslecommand

import cc.lib.game.AGraphics
import cc.lib.game.Utils
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTGraphics
import cc.lib.swing.AWTKeyboardAnimationApplet
import cc.misslecommand.MissleCommand
import cc.misslecommand.MissleCommand.City
import java.awt.event.KeyEvent

/**
 * Created by chriscaron on 10/26/17.
 */
class MissleCommandApplet : AWTKeyboardAnimationApplet() {
	var mc: MissleCommand = object : MissleCommand() {
		override val screenWidth: Int
			get() = this@MissleCommandApplet.screenWidth

		override fun getScreenHeight(): Int {
			return this@MissleCommandApplet.screenHeight
		}

		override fun getPointerX(): Int {
			return mouseX
		}

		override fun getPointerY(): Int {
			return mouseY
		}

		override fun getFrameNumber(): Int {
			return frameNumber
		}

		override fun checkPlayerInput() {
			this@MissleCommandApplet.checkPlayerInput()
		}

		override fun setFrameNumber(frameNum: Int) {
			frameNumber = frameNum
		}
	}

	override fun doInitialization() {
		mc.doInitialization()
	}

	override fun drawFrame(g: AWTGraphics) {
		mc.drawFrame(g)
	}

	override fun onDimensionsChanged(g: AGraphics, width: Int, height: Int) {
		mc.onDimensionsChanged(g, width, height)
	}

	private var qPressed = false
	override fun onKeyPressed(ev: KeyEvent) {
		// make so 2 consecutive Q presses quit the game
		if (ev.keyCode == KeyEvent.VK_Q) {
			if (qPressed) System.exit(0) else {
				qPressed = true
				return
			}
		}
		qPressed = false

		// look for:
		// R restart level
		// L launch wave
		// A,S,D fire missle
		if (ev.keyCode == KeyEvent.VK_R) {
		} else if (ev.keyCode == KeyEvent.VK_L) mc.startMissleWave()
		val cityKeys = intArrayOf(KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D)
		for (i in cityKeys.indices) {
			if (ev.keyCode == cityKeys[i] && mc.getMissleCity(i)!!.numMissles > 0) {
				mc.startPlayerMissle(mc.getMissleCity(i))
			}
		}
	}

	fun checkPlayerInput() {
		var missle: City? = null
		for (i in 0..2) {
			if (getMouseButtonClicked(i) && mc.getMissleCity(i)!!.numMissles > 0) {
				missle = mc.getMissleCity(i)
			}
		}
		if (missle != null) {
			mc.startPlayerMissle(missle)
		}
	}

	companion object {
		// --------------------------------------------------------------
		// MAIN - DEBUGGING ENABLED
		// In Applet mode, debugging is off by default
		// --------------------------------------------------------------
		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			val frame = AWTFrame("Missle Command DEBUG MODE")
			val applet = MissleCommandApplet()
			frame.add(applet)
			frame.centerToScreen(820, 620)
			applet.init()
			applet.start()
		}
	}
}
