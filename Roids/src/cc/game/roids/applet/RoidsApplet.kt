package cc.game.roids.applet

import cc.game.roids.core.Roids
import cc.game.roids.core.Roids.DragMode
import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTKeyboardAnimationApplet
import cc.lib.utils.StopWatch

class RoidsApplet : AWTKeyboardAnimationApplet() {
	var roids: Roids? = Roids()
	override fun doInitialization() {}
	var sw: StopWatch? = null // new StopWatch();

	//long startTime = -1;
	var lastTime: Long = -1
	var fps = 0
	var drawFps = 0
	var fpsTime: Long = 0
	override fun drawFrame(g: AGraphics) {
		if (roids != null) {
			if (sw == null) {
				sw = StopWatch()
				sw!!.start()
			} else if (!roids!!.isPaused) {
				sw!!.unpause()
				sw!!.capture()
			} else {
				sw!!.pause()
			}
			roids!!.setPointer(mouseX, mouseY, mouseDX, mouseDY, getMouseButtonPressed(0))
			val curTime = sw!!.time
			var deltaTime = sw!!.deltaTime
			if (deltaTime > 1000) deltaTime = 1000
			fps++
			roids!!.updateAll(curTime, deltaTime)
			roids!!.drawAll(g)
			var ty = 5
			val tx = (screenWidth - 5).toFloat()
			val th = g.textHeight + 3
			if (drawFps > 0) {
				g.color = GColor.WHITE
				g.drawJustifiedString(tx, ty.toFloat(), Justify.RIGHT, Justify.TOP, "FPS: $drawFps")
				ty += th.toInt()
			}
			if (curTime - fpsTime > 1000) {
				fpsTime = curTime
				drawFps = fps
				//System.out.println("t=" + curTime + " dt=" + deltaTime + " FPS=" + fps);
				fps = 0
			}
			g.drawJustifiedString(tx, ty.toFloat(), Justify.RIGHT, Justify.TOP, "dx=$mouseDX dy=$mouseDY")
			ty += th.toInt()
			if (roids!!.pauseOnCollision) {
				g.drawJustifiedString(tx, ty.toFloat(), Justify.RIGHT, Justify.TOP, "Pause on collision")
				ty += th.toInt()
			}
			if (getKeyboard('1')) {
				roids!!.doTest1()
			}
			if (getKeyboard('2')) {
				roids!!.doTest2()
			}
			if (getKeyboard('3')) {
				roids!!.doTest3()
			}
			if (getKeyboard('4')) {
				roids!!.doTest4()
			}
			if (getKeyboardReset(' ')) {
				roids!!.addTestThingy()
			}
			if (getKeyboardReset('x')) {
				roids!!.toggleRotations()
			}
			if (getKeyboardReset('z')) {
				roids!!.clearThingys()
			}
			if (getKeyboardReset('p')) {
				roids!!.togglePause()
			}
			if (getKeyboardReset('P')) {
				roids!!.pauseOnCollision = !roids!!.pauseOnCollision
			}
			if (getKeyboardReset('S')) {
				roids!!.saveStateToFile("roids.state")
			}
			if (getKeyboardReset('R')) {
				roids!!.restoreStateFromFile("roids.state")
			}
			if (getKeyboardReset(',')) {
				roids!!.paused = true
				roids!!.historyStep(-1)
			} else if (getKeyboardReset('.')) {
				roids!!.paused = true
				roids!!.historyStep(1)
			}
			if (getKeyboard('o')) {
				roids!!.dragMode = DragMode.ORIENTATION
			} else if (getKeyboard('v')) {
				roids!!.dragMode = DragMode.VELOCITY
			} else if (getKeyboard('a')) {
				roids!!.dragMode = DragMode.ANGVELOCITY
			} else {
				roids!!.dragMode = DragMode.POSITION
			}
			if (getKeyboardReset('d')) {
				roids!!.deleteSelected()
			}
			if (getKeyboard('h')) {
				drawHelp(g)
			}
		}
	}

	override fun onDimensionsChanged(g: AGraphics, width: Int, height: Int) {
		g.ortho()
		roids!!.setScreenDimension(width, height)
	}

	@Synchronized
	public override fun onPauseChanged(paused: Boolean) {
		if (sw != null) {
			if (paused) {
				sw!!.pause()
				roids!!.paused = true
			} else sw!!.unpause()
		}
	}

	fun drawHelp(g: AGraphics) {
		val help = """
	       	1       - test 1
	       	2       - test 2
	       	3       - test 3
	       	4       - test 4
	       	<SP>    - add thingy
	       	x       - toggle rotations
	       	z       - clear thingys
	       	d       - delete selected
	       	p       - pause
	       	P       - pause on collision
	       	S       - save state to file
	       	R       - restore state from file
	       	<       - history back
	       	>       - history forward
	       	drag    - change position
	       	o+drag  - change orientation
	       	v+drag  - change velocity
	       	a+drag  - change ang velocity
	       	
	       	""".trimIndent()
		g.color = GColor.WHITE
		g.drawJustifiedString((g.viewportWidth / 3).toFloat(), (g.viewportHeight / 2).toFloat(), Justify.LEFT, Justify.CENTER, help)
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			Utils.setRandomSeed(0)
			val frame = AWTFrame("JavaRoids Debug Mode")
			val app: AWTKeyboardAnimationApplet = RoidsApplet()
			frame.add(app)
			frame.centerToScreen(640, 480)
			app.init()
			app.start()
			app.setMillisecondsPerFrame(20)
		}
	}
}