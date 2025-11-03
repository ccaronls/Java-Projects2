package cc.applets.typing

import cc.lib.game.AGraphics
import cc.lib.game.GAnimation
import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.Justify
import cc.lib.math.Vector2D
import cc.lib.reflector.Reflector
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTKeyboardAnimationApplet
import cc.lib.utils.getOrCreateSettingsDirectory
import cc.lib.utils.interpolateColors
import cc.lib.utils.launchIn
import cc.lib.utils.openExistingFileOrResource
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.SwingUtilities
import kotlin.math.roundToInt

inline fun <reified T : Reflector<T>> addAllFields() {
	Reflector.addAllFields(T::class.java)
}

class KeyboardButton(val code: Int = 0, val key: Char = 'a', var shiftKey: Char? = null, var rect: GRectangle = GRectangle()) :
	Reflector<KeyboardButton>() {
	companion object {
		init {
			addAllFields<KeyboardButton>()
		}
	}

	fun getDisplayKey(shift: Boolean): Char = if (shift && shiftKey != null) shiftKey!! else key
}

class LearnToType : AWTKeyboardAnimationApplet(), TypingEngine.Listener {

	enum class Mode {
		LOADING,
		BUILD,
		READY,
		WAIT_FOR_KEYBOARD_READY,
		LEARN,
		ANIMATE
	}

	val keysFile = File("SwingApps/resources/keys.txt")
	override fun doInitialization() {

		fun getShiftKey(c: Char): Char? {
			return when {
				c.isLowerCase() -> c.uppercaseChar()
				else -> null
			}
		}

		try {
			"keysOld.txt".openExistingFileOrResource()?.let {
				val keysOld: MutableMap<Char, GRectangle> = Reflector.deserializeFromInputStream(it)
				keys = keysOld.toList().map { (c, rect) ->
					KeyboardButton(
						requireNotNull(codeMap[c]) {
							"No code for char $c"
						}, c, getShiftKey(c), rect)
				}
				Reflector.serializeToFile(keys, keysFile)
			} ?: run {
				keys = Reflector.deserializeFromFile(keysFile)
			}

		} catch (e: Exception) {
			e.printStackTrace()
		}

		launchIn {
			engine = TypingEngine(this@LearnToType, "common_words.txt").also {
				it.tryLoadFromFile(saveFile)
			}
			mode = Mode.READY
			SwingUtilities.invokeLater {
				grabFocus()
			}
		}
	}

	val settings by lazy {
		javaClass.getOrCreateSettingsDirectory()
	}

	val saveFile by lazy {
		File(settings, "typing.save")
	}

	var keyboardId = -1
	var keyCode: Int = 0
	private lateinit var engine: TypingEngine

	var progress = 0f

	fun save() {
		if (::engine.isInitialized)
			engine.trySaveToFile(saveFile)
	}

	var animation: GAnimation? = null

	override fun onProgress(current: Int, max: Int) {
		if (max > 0)
			progress = current.toFloat() / max
	}

	override fun onCorrect() {
		animation = object : GAnimation(500, 1, true) {
			override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.pushMatrix()
				g.scale(20f)
				g.scale(1 + position / 10)
				g.color = GColor.GREEN.withAlpha(.5f + position / 2)
				g.setLineWidth(5f)
				g.drawLine(0f, .5f, .5f, 1f)
				g.drawLine(.5f, 1f, 1f, 0f)
				g.popMatrix()
			}
		}.start()
	}

	override fun onIncorrect() {
		animation = object : GAnimation(1000, 1, true) {
			override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.pushMatrix()
				g.scale(20f)
				g.scale(1 + position / 10)
				g.color = GColor.RED.withAlpha(.5f + position / 2)
				g.setLineWidth(5f)
				g.drawLine(0f, 0f, 1f, 1f)
				g.drawLine(1f, 0f, 0f, 1f)
				g.popMatrix()
			}
		}.start()
	}

	fun drawKeyboard(g: AGraphics) {
		if (keyboardId < 0) keyboardId = g.loadImage("small_keyboard.jpg")
		val img = g.getImage(keyboardId)
		val aspect = img.aspect
		val w = g.viewportWidth
		val h = (w.toFloat() / aspect).roundToInt()
		val x = 0
		val y = 0
		g.setTextHeight(28f, false)
		g.drawImage(keyboardId, x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())
	}

	override fun drawFrame(g: AGraphics) {
		g.clearScreen(GColor.SKY_BLUE)
		when (mode) {
			Mode.LOADING -> {
				g.clearScreen(GColor.GRAY)
				g.color = GColor.GREEN.withAlpha(frameNumber.mod(256))
				g.setTextHeight(32f, true)
				val dim =
					g.drawJustifiedString(viewportWidth / 2, viewportHeight / 2, Justify.CENTER, Justify.BOTTOM, "LOADING")
				val rect = GRectangle(viewportWidth / 2 - dim.width / 2, viewportHeight / 2, dim.width, dim.height)
				g.color = GColor.BLUE
				rect.drawOutlined(g)
				rect.scale(progress, 1f)
				rect.drawFilled(g)
				repaint()
			}

			Mode.BUILD -> {
				drawKeyboard(g)

				charMap[keyCode]?.let { char ->
					g.color = GColor.BLACK
					g.drawJustifiedString(10f, (viewportHeight - 10).toFloat(), Justify.LEFT, Justify.BOTTOM, char.toString())
				}
				for (key in keys) {
					g.color = GColor.YELLOW
					g.drawRect(key.rect)
					g.drawJustifiedString(key.rect.center, Justify.CENTER, Justify.CENTER, key.getDisplayKey(isShiftDown).toString())
				}
				if (dragging) {
					g.color = GColor.GREEN
					g.drawRect(rect)
				}
			}

			Mode.WAIT_FOR_KEYBOARD_READY,
			Mode.READY -> {
				drawFeedback(g)
				drawKeyboardKeys(g)
				val txt =
					"Thanks for trying Learn to Type!\nPlace fingers on the F and J keys and press them simultaneously to begin."
				g.drawWrapString((viewportWidth / 2).toFloat(), (viewportHeight * 3 / 4).toFloat(), (viewportWidth * 2 / 3).toFloat(), Justify.CENTER, Justify.CENTER, txt)
			}

			Mode.ANIMATE,
			Mode.LEARN -> {
				drawFeedback(g)
				drawKeyboardKeys(g)
				g.setTextHeight(56f, false)
				val prompt = engine.getPrompt().map { (c, color) -> color.color.toARGBString() + c }.joinToString()
				g.color = GColor.BLUE
				val y = (viewportHeight * 3 / 4).toFloat()
				val x = viewportWidth / 2 - g.getTextWidth("TYPE THIS") / 2
				val dim = g.drawAnnotatedString("TYPE THIS:$prompt", x, y)
				if (animation?.isDone != true) {
					g.pushMatrix()
					g.translate(x + dim.width + 20, y + 56f / 2)
					if (animation?.update(g) == false) {
						animation = null
						engine.newPrompt()
					}
					g.popMatrix()
				}
			}
		}
	}

	fun drawFeedback(g: AGraphics) {
		g.color = GColor.WHITE
		g.setTextHeight(16f, true)
		g.drawJustifiedString(viewportWidth - 10, 10, Justify.RIGHT, Justify.TOP, engine.getFeedback().toDisplayString())
	}

	fun drawKeyboardKeys(g: AGraphics) {
		g.setTextHeight(20f, false)
		//        g.setTextStyles(AGraphics.TextStyle.BOLD);
		for (key in keys) {
			val weight = engine.getKeyWeight(key.key)
			g.color = interpolateColors(weight.toFloat(), GColor.RED, GColor.WHITE, GColor.GREEN)
			g.drawFilledRoundedRect(key.rect, 10f)
			g.color = GColor.BLACK
			g.drawJustifiedString(key.rect.center, Justify.CENTER, Justify.CENTER, key.getDisplayKey(isShiftDown).toString())
			if (getKeyboard(key.key)) {
				g.color = GColor.MAGENTA
				g.drawRoundedRect(key.rect, 1f, 10f)
			}
			if (key.code == KeyEvent.VK_J || key.code == KeyEvent.VK_F) {
				g.color = GColor.RED
				g.drawRoundedRect(key.rect, 1f, 10f)
			}
		}
	}

	var dragging = false
	var rect = GRectangle()
	override fun mouseDragged(ev: MouseEvent) {
		if (mode == Mode.BUILD) {
			val v = Vector2D(ev.x.toFloat(), ev.y.toFloat())
			rect.setEnd(v)
			dragging = true
		}
	}

	override fun onMousePressed(ev: MouseEvent) {
		if (mode == Mode.BUILD) {
			rect = GRectangle()
			val v = Vector2D(ev.x.toFloat(), ev.y.toFloat())
			rect[v] = v
		}
	}

	@Synchronized
	override fun mouseReleased(evt: MouseEvent) {
		if (mode == Mode.BUILD) {
			if (dragging) {
				keys.firstOrNull { it.code == keyCode }?.let {
					it.rect.set(rect)
				}
				dragging = false
				try {
					Reflector.serializeToFile(keys, keysFile)
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
		}
	}

	override fun onKeyPressed(evt: KeyEvent) {
		when (mode) {
			Mode.BUILD -> {
				if (evt.keyCode == KeyEvent.VK_ESCAPE) {
					mode = Mode.READY
				}
			}

			Mode.READY -> {
				if (getKeyboard('f') && getKeyboard('j')) {
					mode = Mode.WAIT_FOR_KEYBOARD_READY
				} else if (evt.keyCode == KeyEvent.VK_ESCAPE) {
					mode = Mode.BUILD
				}
			}

			Mode.WAIT_FOR_KEYBOARD_READY,
			Mode.LEARN -> {
				if (evt.keyCode == KeyEvent.VK_ESCAPE) {
					mode = Mode.READY
					engine.pause()
				}
			}

			Mode.ANIMATE,
			Mode.LOADING -> Unit
		}
	}

	override fun onKeyReleased(evt: KeyEvent) {
		println("keyReleased ${evt.keyChar}")
		when (mode) {
			Mode.BUILD -> {
				evt.keyCode.takeIf { it in KeyEvent.VK_0..KeyEvent.VK_Z }?.let { code ->
					keyCode = code
				}
			}

			Mode.READY -> {
				super.keyReleased(evt)
			}

			Mode.LEARN -> {
				keys.firstOrNull { it.code == evt.keyCode }?.let {
					engine.onKeyTyped(it.getDisplayKey(isShiftDown))
				}
			}

			Mode.WAIT_FOR_KEYBOARD_READY -> {
				if (keys.none { getKeyboard(it.key) }) {
					engine.start()
					mode = Mode.LEARN
				}
			}

			Mode.ANIMATE,
			Mode.LOADING -> Unit
		}
	}

	var keys: List<KeyboardButton> = listOf()

	override fun onDimensionsChanged(g: AGraphics, width: Int, height: Int) {}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			//Utils.DEBUG_ENABLED = true;
			//Golf.DEBUG_ENABLED = true;
			//PlayerBot.DEBUG_ENABLED = true;
			//mode = 0;
			val app = LearnToType()
			val frame = object : AWTFrame("Learn to Type") {
				override fun onWindowClosing() {
					app.save()
				}
			}
			frame.add(app)
			app.init()
			frame.centerToScreen(800, 600)
			app.start()
			app.millisecondsPerFrame = 20
			val settings = LearnToType::class.java.getOrCreateSettingsDirectory()
			frame.setPropertiesFile(File(settings, "app.properties"))
			frame.restoreFromProperties()
		}

		var mode = Mode.LOADING
	}

	val codeMap = mapOf(
		'0' to KeyEvent.VK_0,
		'1' to KeyEvent.VK_1,
		'2' to KeyEvent.VK_2,
		'3' to KeyEvent.VK_3,
		'4' to KeyEvent.VK_4,
		'5' to KeyEvent.VK_5,
		'6' to KeyEvent.VK_6,
		'7' to KeyEvent.VK_7,
		'8' to KeyEvent.VK_8,
		'9' to KeyEvent.VK_9,

		'a' to KeyEvent.VK_A,
		'b' to KeyEvent.VK_B,
		'c' to KeyEvent.VK_C,
		'd' to KeyEvent.VK_D,
		'e' to KeyEvent.VK_E,
		'f' to KeyEvent.VK_F,
		'g' to KeyEvent.VK_G,
		'h' to KeyEvent.VK_H,
		'i' to KeyEvent.VK_I,
		'j' to KeyEvent.VK_J,
		'k' to KeyEvent.VK_K,
		'l' to KeyEvent.VK_L,
		'm' to KeyEvent.VK_M,
		'n' to KeyEvent.VK_N,
		'o' to KeyEvent.VK_O,
		'p' to KeyEvent.VK_P,
		'q' to KeyEvent.VK_Q,
		'r' to KeyEvent.VK_R,
		's' to KeyEvent.VK_S,
		't' to KeyEvent.VK_T,
		'u' to KeyEvent.VK_U,
		'v' to KeyEvent.VK_V,
		'w' to KeyEvent.VK_W,
		'x' to KeyEvent.VK_X,
		'y' to KeyEvent.VK_Y,
		'z' to KeyEvent.VK_Z,
		' ' to KeyEvent.VK_SPACE,
	)

	val charMap by lazy {
		codeMap.map { it.value to it.key }.toMap()
	}
}