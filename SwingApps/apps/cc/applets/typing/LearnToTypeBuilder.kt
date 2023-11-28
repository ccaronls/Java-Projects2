package cc.applets.typing

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.Justify
import cc.lib.math.Vector2D
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTGraphics
import cc.lib.utils.FileUtils
import cc.lib.utils.Reflector
import java.awt.Graphics
import java.awt.Point
import java.awt.event.*
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileNotFoundException
import javax.swing.JApplet

class LearnToTypeBuilder : JApplet(), MouseListener, MouseMotionListener, KeyListener, FocusListener {
	private var G: Graphics? = null
	private var AG: AGraphics? = null
	var keyboard: MutableMap<Int, GRectangle?> = HashMap()
	override fun init() {
		addFocusListener(this)
		addKeyListener(this)
		addMouseListener(this)
		addMouseMotionListener(this)
		try {
			Reflector.deserializeFromFile<Any>(keyBoardFile)
		} catch (e: FileNotFoundException) {
			// ignore
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	override fun paint(g: Graphics) {
		if (g !== G) {
			G = g
			with(AWTGraphics(g, this)) {
				AG = this
				setup(this)
			}
		}
		draw(AG)
	}

	var keybdId = -1
	protected fun setup(g: AGraphics) {
		if (keybdId < 0) keybdId = g.loadImage("small_keyboard.png")
	}

	var pressed: KeyEvent? = null
	var rect: GRectangle? = null
	var dragging = false
	var mouseX = 0
	var mouseY = 0
	protected fun draw(g: AGraphics?) {
		g!!.clearScreen(GColor.GREEN)
		if (keybdId >= 0) {
			val img = g.getImage(keybdId)
			g.drawImage(keybdId, 0f, 0f, g.viewportWidth.toFloat(), g.viewportWidth.toFloat() / img.aspect)
		}
		for ((key1, value) in keyboard) {
			g.color = GColor.WHITE
			g.drawFilledRoundedRect(value, 10f)
			g.color = GColor.BLACK
			val key = KeyEvent.getKeyText(key1)
			g.drawJustifiedString(value!!.center, Justify.CENTER, Justify.CENTER, key)
		}
		g.color = GColor.YELLOW
		g.setLineWidth(3f)
		if (dragging) {
			g.drawRect(rect)
		}
		if (pressed != null) {
			g.textHeight = 32f
			g.color = GColor.BLUE
			g.setTextStyles(AGraphics.TextStyle.BOLD)
			g.drawJustifiedString((g.viewportWidth / 2).toFloat(), (g.viewportHeight * 3 / 4).toFloat(), KeyEvent.getKeyText(pressed!!.keyCode))
		}

		// draw crosshais
		g.color = GColor.LIGHT_GRAY
		val SIZE = 10
		g.drawLine((mouseX - SIZE).toFloat(), mouseY.toFloat(), (mouseX + SIZE).toFloat(), mouseY.toFloat())
		g.drawLine(mouseX.toFloat(), (mouseY - SIZE).toFloat(), mouseX.toFloat(), (mouseY + SIZE).toFloat())
	}

	override fun keyTyped(e: KeyEvent) {
		pressed = e
		repaint()
	}

	override fun keyPressed(e: KeyEvent) {
		pressed = e
		repaint()
	}

	override fun keyReleased(e: KeyEvent) {
		pressed = e
		repaint()
	}

	override fun mouseClicked(e: MouseEvent) {}
	override fun mousePressed(e: MouseEvent) {
		if (pressed != null) {
			val v = Vector2D(mouseX.toFloat(), mouseY.toFloat())
			rect = GRectangle(v, v)
			repaint()
		}
	}

	override fun mouseReleased(e: MouseEvent) {
		if (dragging) {
			keyboard[pressed!!.keyCode] = rect
			repaint()
		}
	}

	override fun mouseEntered(e: MouseEvent) {}
	override fun mouseExited(e: MouseEvent) {}
	override fun mouseDragged(e: MouseEvent) {
		mouseMoved(e)
		if (pressed != null) {
			dragging = true
			val v = Vector2D(mouseX.toFloat(), mouseY.toFloat())
			rect!!.setEnd(v)
		}
	}

	val GRID = 5
	override fun mouseMoved(e: MouseEvent) {
		mouseX = e.x / GRID * GRID
		mouseY = e.y / GRID * GRID
		repaint()
	}

	override fun focusGained(e: FocusEvent) {}
	override fun focusLost(e: FocusEvent) {}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			val app = LearnToTypeBuilder()
			val frame: AWTFrame = object : AWTFrame("Learn to Type") {
				override fun onWindowClosing() {
					try {
						val resources = File("GameLibrary/resources")
						if (!resources.isDirectory) throw Exception("""resources folder:
 '$resources'
 not found in working directory:
 ${File(".").canonicalPath}""")
						Reflector.serializeToFile<Any>(app.keyboard, File(resources, keyBoardFile.name))
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}
			}
			app.cursor = app.toolkit.createCustomCursor(
				BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
				Point(),
				null)
			frame.add(app)
			val settings = FileUtils.getOrCreateSettingsDirectory(LearnToTypeBuilder::class.java)
			frame.setPropertiesFile(File(settings, "app.properties"))
			if (!frame.restoreFromProperties()) frame.centerToScreen(800, 600)
			app.init()
			app.start()
		}

		var keyBoardFile = File("keyboard.txt")
	}
}