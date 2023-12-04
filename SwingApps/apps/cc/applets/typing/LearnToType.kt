package cc.applets.typing

import cc.lib.game.*
import cc.lib.math.Vector2D
import cc.lib.reflector.Reflector
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTKeyboardAnimationApplet
import cc.lib.utils.FileUtils
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.io.File

class LearnToType : AWTKeyboardAnimationApplet() {
	var keysFile = File("keys.txt")
	override fun doInitialization() {
		try {
			keys = Reflector.deserializeFromFile(keysFile)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	var keyboardId = -1
	var key: Char? = 'a'
	var shiftPressed = false
	fun drawKeyboard(g: AGraphics) {
		if (keyboardId < 0) keyboardId = g.loadImage("small_keyboard.jpg")
		val img = g.getImage(keyboardId)
		val aspect = img.aspect
		val w = g.viewportWidth
		val h = Math.round(w.toFloat() / aspect)
		val x = 0
		val y = 0
		g.textHeight = 28f
		g.drawImage(keyboardId, x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())
	}

	var typeThis: Char? = null
	override fun drawFrame(g: AGraphics) {
		g.clearScreen(GColor.GREEN)
		if (mode == 0) {
			drawKeyboard(g)
			if (key != null) {
				g.color = GColor.BLACK
				g.drawJustifiedString(10f, (viewportHeight - 10).toFloat(), Justify.LEFT, Justify.BOTTOM, key.toString())
			}
			for ((key1, value) in keys) {
				g.color = GColor.YELLOW
				g.drawRect(value)
				g.drawJustifiedString(value.center, Justify.CENTER, Justify.CENTER, key1.toString())
			}
			if (dragging) {
				g.color = GColor.GREEN
				g.drawRect(rect)
			}
		}
		if (mode == 1) {
			drawKeyboardKeys(g)
			val txt = "Thanks for trying Learn to Type!\nPlace fingers on the F and J keys and press them simultaneously to begin."
			g.drawWrapString((viewportWidth / 2).toFloat(), (viewportHeight * 3 / 4).toFloat(), (viewportWidth * 2 / 3).toFloat(), Justify.CENTER, Justify.CENTER, txt)
		}
		if (mode == 2) {
			drawKeyboardKeys(g)
			if (pressed != null) {
				val r = keys[pressed]
				if (r != null) {
					g.color = GColor.MAGENTA
					g.drawRoundedRect(r, 1f, 10f)
				}
			}
			g.textHeight = 56f
			if (typeThis == null) {
				val chars = "abcdefghijklmnopqrstuvwxyz"
				typeThis = chars[Utils.rand() % chars.length]
			}
			g.color = GColor.BLUE
			g.drawWrapString((viewportWidth / 2).toFloat(), (viewportHeight * 3 / 4).toFloat(), viewportWidth.toFloat(), Justify.CENTER, Justify.CENTER, """
 	TYPE THIS
 	${typeThis.toString()}
 	""".trimIndent())
			val rect = keys[typeThis]
			g.drawRect(rect)
		}
	}

	fun drawKeyboardKeys(g: AGraphics) {
		g.textHeight = 20f
		//        g.setTextStyles(AGraphics.TextStyle.BOLD);
		for ((key1, value) in keys) {
			g.color = GColor.WHITE
			g.drawFilledRoundedRect(value, 10f)
			g.color = GColor.BLACK
			g.drawJustifiedString(value.center, Justify.CENTER, Justify.CENTER, key1.toString().toUpperCase())
		}
		g.color = GColor.RED
		g.drawRoundedRect(keys['f'], 1f, 10f)
		g.drawRoundedRect(keys['j'], 1f, 10f)
	}

	override fun keyTyped(evt: KeyEvent) {}
	var dragging = false
	var rect = GRectangle()
	override fun mouseDragged(ev: MouseEvent) {
		if (mode == 0) {
			val v = Vector2D(ev.x.toFloat(), ev.y.toFloat())
			rect.setEnd(v)
			dragging = true
		}
	}

	override fun onMousePressed(ev: MouseEvent) {
		if (mode == 0) {
			rect = GRectangle()
			val v = Vector2D(ev.x.toFloat(), ev.y.toFloat())
			rect[v] = v
		}
	}

	@Synchronized
	override fun mouseReleased(evt: MouseEvent) {
		if (mode == 0) {
			if (dragging) {
				keys[key] = rect
				dragging = false
				try {
					Reflector.serializeToFile<Any>(keys, keysFile)
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
		}
	}

	var pressed: Char? = null
	override fun keyPressed(evt: KeyEvent) {
		if (mode == 0) {
			when (evt.keyCode) {
				KeyEvent.VK_SHIFT -> shiftPressed = true
			}
		}
		if (mode == 1) {
			super.keyPressed(evt)
			if (getKeyboard('f') && getKeyboard('j')) {
				mode = 2
			}
		}
		if (mode == 2) {
			pressed = evt.keyChar
		}
	}

	override fun keyReleased(evt: KeyEvent) {
		if (mode == 0) {
			val code = evt.keyCode
			if (code >= KeyEvent.VK_0 && code <= KeyEvent.VK_Z) {
				key = evt.keyChar
			}
			when (evt.keyCode) {
				KeyEvent.VK_SHIFT -> shiftPressed = false
			}
		}
		if (mode == 1) {
			super.keyReleased(evt)
		}
		if (mode == 2 && evt.keyChar == typeThis) {
			typeThis = null
			pressed = null
		}
	}

	var keys: MutableMap<Char?, GRectangle> = HashMap()
	override fun onDimensionsChanged(g: AGraphics, width: Int, height: Int) {}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			//Utils.DEBUG_ENABLED = true;
			//Golf.DEBUG_ENABLED = true;
			//PlayerBot.DEBUG_ENABLED = true;
			//mode = 0;
			val frame = AWTFrame("Learn to Type")
			val app: AWTKeyboardAnimationApplet = LearnToType()
			frame.add(app)
			app.init()
			frame.centerToScreen(800, 600)
			app.start()
			app.setMillisecondsPerFrame(20)
			val settings = FileUtils.getOrCreateSettingsDirectory(LearnToType::class.java)
			frame.setPropertiesFile(File(settings, "app.properties"))
			frame.restoreFromProperties()
		}

		var mode = 1
	}
}