 package cc.applets.typing

 import cc.lib.game.AGraphics
 import cc.lib.game.GAnimation
 import cc.lib.game.GColor
 import cc.lib.game.GRectangle
 import cc.lib.game.Justify
 import cc.lib.math.Vector2D
 import cc.lib.reflector.Reflector
 import cc.lib.swing.AWTFrame
 import cc.lib.swing.AWTGraphics
 import cc.lib.swing.AWTKeyboardAnimationApplet
 import cc.lib.utils.KFileUtils.getOrCreateSettingsDirectory
 import cc.lib.utils.interpolate
 import cc.lib.utils.launchIn
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

	enum class DragMode {
		DRAG_NONE,
		DRAG_TOP,
		DRAG_BOTTOM,
		DRAG_LEFT,
		DRAG_RIGHT,
		DRAG_ALL,
		DRAG_CORNER
	}

	enum class Mode {
		LOADING,
		BUILD,
		READY,
		WAIT_FOR_KEYBOARD_READY,
		LEARN,
		ANIMATE
	}

	val rectMinDim = 10f

	val keysFile = File("SwingApps/resources/keys.txt")
	override fun doInitialization() {

		try {
			keys = Reflector.deserializeFromFile(keysFile)

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
	var dragMode = DragMode.DRAG_NONE
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
		animation = object : GAnimation(200, 1, true) {
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

	override fun onLevelUp() {
		animation = object : GAnimation(1000) {
			override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.pushMatrix()
				g.setIdentity()
				g.color = GColor.GREEN
				val h = g.setTextHeight(30f, false)
				g.drawJustifiedString(viewportWidth / 2, viewportHeight / 2, Justify.CENTER, Justify.CENTER, "L E V E L   U P")
				g.setTextHeight(h, true)
				g.popMatrix()
			}
		}.start()
	}

	override fun onLevelDown() {
		animation = object : GAnimation(1000) {
			override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.pushMatrix()
				g.setIdentity()
				g.color = GColor.RED
				val h = g.setTextHeight(30f, false)
				g.drawJustifiedString(viewportWidth / 2, viewportHeight / 2, Justify.CENTER, Justify.CENTER, "L E V E L   D O W N")
				g.setTextHeight(h, true)
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

	fun drawRectDrag(g: AGraphics) {
		g.pushMatrix()
		when (dragMode) {
			DragMode.DRAG_TOP -> {
				g.translate(0f, rectMinDim / 2)
				g.drawLine(rect.topLeft, rect.topRight)
			}

			DragMode.DRAG_RIGHT -> {
				g.translate(-rectMinDim / 2, 0f)
				g.drawLine(rect.topRight, rect.bottomRight)
			}

			DragMode.DRAG_BOTTOM -> {
				g.translate(0f, -rectMinDim / 2)
				g.drawLine(rect.bottomLeft, rect.bottomRight)
			}

			DragMode.DRAG_LEFT -> {
				g.translate(rectMinDim / 2, 0f)
				g.drawLine(rect.topLeft, rect.bottomLeft)
			}

			DragMode.DRAG_CORNER -> {
				g.translate(-rectMinDim / 2, -rectMinDim / 2)
				g.drawLine(rect.bottomRight, rect.bottomRight.add(0, rectMinDim / 2))
				g.drawLine(rect.bottomRight, rect.bottomRight.add(rectMinDim / 2, 0f))
			}

			else -> Unit
		}
		g.popMatrix()
	}

	override fun drawFrame(g: AWTGraphics) {
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
				rect.scaleEq(progress, 1f)
				rect.drawFilled(g)
				repaint()
			}

			Mode.BUILD -> {
				drawKeyboard(g)

				codeMap[keyCode]?.let { (char, shiftChar) ->
					g.color = GColor.BLACK
					g.drawJustifiedString(10f, viewportHeight - 10, Justify.LEFT, Justify.BOTTOM, char.toString())
				}
				for (key in keys) {
					g.color = GColor.YELLOW
					g.drawRect(key.rect)
					g.drawJustifiedString(key.rect.center, Justify.CENTER, Justify.CENTER, key.getDisplayKey(isShiftDown).toString())
				}

				g.color = GColor.GREEN
				g.drawRect(rect)
				drawRectDrag(g)
			}

			Mode.WAIT_FOR_KEYBOARD_READY,
			Mode.READY -> {
				drawFeedback(g)
				drawKeyboardKeys(g)
				val txt =
					"Thanks for trying Learn to Type!\nPlace fingers on the F and J keys and press them simultaneously to begin."
				g.drawWrapString(viewportWidth / 2, viewportHeight * 3 / 4, viewportWidth * 2 / 3, Justify.CENTER, Justify.CENTER, txt)
			}

			Mode.ANIMATE,
			Mode.LEARN -> {
				drawFeedback(g)
				drawKeyboardKeys(g)
				g.setTextHeight(56f, false)
				val prompt = engine.getPrompt().map { (c, color) -> color.color.toARGBString() + c }.joinToString()
				g.color = GColor.BLUE
				val y = viewportHeight * 3 / 4
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
		g.setTextHeight(20f, true)
		g.setTextStyles(AGraphics.TextStyle.BOLD)
		g.drawJustifiedString(10, viewportHeight - 10, Justify.LEFT, Justify.BOTTOM, engine.getFeedback().toDisplayString())
	}

	fun drawKeyboardKeys(g: AGraphics) {
		g.setTextHeight(20f, false)
		//        g.setTextStyles(AGraphics.TextStyle.BOLD);
		for (key in keys) {
			val weight = engine.getKeyWeight(key.key)
			g.color = interpolate(weight.toFloat(), GColor.RED, GColor.WHITE, GColor.GREEN) { f, a, b ->
				a.interpolateTo(b, f)
			}
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

	val rect = GRectangle()
	override fun mouseMoved(ev: MouseEvent) {
		if (mode == Mode.BUILD) {
			val v = Vector2D(ev.x, ev.y)
			keys.firstOrNull { v in it.rect }?.let {
				val r = it.rect
				if (v.y - r.top < r.height / 3) {
					// top third
					dragMode = DragMode.DRAG_TOP
				} else if (r.bottom - v.y < r.height / 3) {
					// bottom third
					dragMode = DragMode.DRAG_BOTTOM
				} else if (v.x - r.left < r.width / 3) {
					// left third
					dragMode = DragMode.DRAG_LEFT
				} else if (r.right - v.x < r.width / 3) {
					// right third
					dragMode = DragMode.DRAG_RIGHT
				} else {
					dragMode = DragMode.DRAG_ALL
				}
				rect.set(r)
			} ?: run {
				rect.zero()
				dragMode = DragMode.DRAG_NONE
			}
		}
	}

	override fun mouseDragged(ev: MouseEvent) {
		if (mode == Mode.BUILD) {
			val v = Vector2D(ev.x, ev.y)
			when (dragMode) {
				DragMode.DRAG_TOP -> rect.stretchTop(v.y)
				DragMode.DRAG_BOTTOM -> rect.stretchBottom(v.y)
				DragMode.DRAG_LEFT -> rect.stretchLeft(v.x)
				DragMode.DRAG_RIGHT -> rect.stretchRight(v.x)
				DragMode.DRAG_ALL -> rect.setCenter(v)
				DragMode.DRAG_CORNER -> rect.stretchRight(v.x).stretchBottom(v.y)
				DragMode.DRAG_NONE -> Unit
			}
		}
	}

	override fun onMousePressed(ev: MouseEvent) {
		if (mode == Mode.BUILD) {
			val v = Vector2D(ev.x.toFloat(), ev.y.toFloat())
			if (keyCode != 0) {
				rect.setDimension(rectMinDim, rectMinDim).setTopLeftPosition(v)
				keys.firstOrNull { it.code == keyCode }?.let {
					it.rect = rect
					dragMode = DragMode.DRAG_CORNER
				} ?: run {
					codeMap[keyCode]?.let { (char, shiftChar) ->
						keys.add(KeyboardButton(keyCode, char, shiftChar, rect))
						dragMode = DragMode.DRAG_CORNER
					}
				}
			} else if (dragMode != DragMode.DRAG_NONE) {
				keys.firstOrNull { it.rect == rect }?.let {
					keyCode = it.code
					it.rect = rect
				}
			}
		}
	}

	@Synchronized
	override fun mouseReleased(evt: MouseEvent) {
		if (mode == Mode.BUILD) {
			if (dragMode != DragMode.DRAG_NONE && keyCode != 0) {
				keys.firstOrNull { it.code == keyCode }?.let {
					it.rect = rect.deepCopy()
				}
				try {
					Reflector.serializeToFile(keys, keysFile)
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
			dragMode = DragMode.DRAG_NONE
			keyCode = 0
		}
	}

	override fun onKeyPressed(evt: KeyEvent) {
		when (mode) {
			Mode.BUILD -> {
				if (evt.keyCode == KeyEvent.VK_ESCAPE) {
					if (dragMode != DragMode.DRAG_NONE)
						keyCode = 0
					else
						mode = Mode.READY
				} else if (evt.keyCode in arrayOf(KeyEvent.VK_DELETE, KeyEvent.VK_BACK_SPACE)) {
					keys.firstOrNull { rect == it.rect }?.let {
						keys.remove(it)
					}
				} else {
					codeMap[evt.keyCode]?.let { (char, shiftChar) ->
						keyCode = evt.keyCode
					}
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
					save()
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
				keyCode = 0
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

	var keys = mutableListOf<KeyboardButton>()

	override fun onDimensionsChanged(g: AGraphics, width: Int, height: Int) {}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			//Utils.DEBUG_ENABLED = true;
			//Golf.DEBUG_ENABLED = true;
			//PlayerBot.DEBUG_ENABLED = true;
			//mode = 0;
			val app = LearnToType()
			val frame = AWTFrame("Learn to Type")
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

	val codeMap: Map<Int, Pair<Char, Char?>> = mapOf(
		KeyEvent.VK_BACK_QUOTE to Pair('`', '~'),
		KeyEvent.VK_1 to Pair('1', '!'),
		KeyEvent.VK_2 to Pair('2', '@'),
		KeyEvent.VK_3 to Pair('3', '#'),
		KeyEvent.VK_4 to Pair('4', '$'),
		KeyEvent.VK_5 to Pair('5', '%'),
		KeyEvent.VK_6 to Pair('6', '^'),
		KeyEvent.VK_7 to Pair('7', '&'),
		KeyEvent.VK_8 to Pair('8', '*'),
		KeyEvent.VK_9 to Pair('9', '('),
		KeyEvent.VK_0 to Pair('0', ')'),
		KeyEvent.VK_MINUS to Pair('-', '_'),
		KeyEvent.VK_EQUALS to Pair('=', '+'),

		KeyEvent.VK_TAB to Pair('\t', null),
		KeyEvent.VK_Q to Pair('q', 'Q'),
		KeyEvent.VK_W to Pair('w', 'W'),
		KeyEvent.VK_E to Pair('e', 'E'),
		KeyEvent.VK_R to Pair('r', 'R'),
		KeyEvent.VK_T to Pair('t', 'T'),
		KeyEvent.VK_Y to Pair('y', 'Y'),
		KeyEvent.VK_U to Pair('u', 'U'),
		KeyEvent.VK_I to Pair('i', 'I'),
		KeyEvent.VK_O to Pair('o', 'O'),
		KeyEvent.VK_P to Pair('p', 'P'),
		KeyEvent.VK_OPEN_BRACKET to Pair('[', '{'),
		KeyEvent.VK_CLOSE_BRACKET to Pair(']', '}'),

		KeyEvent.VK_A to Pair('a', 'A'),
		KeyEvent.VK_S to Pair('s', 'S'),
		KeyEvent.VK_D to Pair('d', 'D'),
		KeyEvent.VK_F to Pair('f', 'F'),
		KeyEvent.VK_G to Pair('g', 'G'),
		KeyEvent.VK_H to Pair('h', 'H'),
		KeyEvent.VK_J to Pair('j', 'J'),
		KeyEvent.VK_K to Pair('k', 'K'),
		KeyEvent.VK_L to Pair('l', 'L'),
		KeyEvent.VK_SEMICOLON to Pair(';', ':'),
		KeyEvent.VK_QUOTE to Pair('\'', '\"'),

		KeyEvent.VK_Z to Pair('z', 'Z'),
		KeyEvent.VK_X to Pair('x', 'X'),
		KeyEvent.VK_C to Pair('c', 'C'),
		KeyEvent.VK_V to Pair('v', 'V'),
		KeyEvent.VK_B to Pair('b', 'B'),
		KeyEvent.VK_N to Pair('n', 'N'),
		KeyEvent.VK_M to Pair('m', 'M'),

		KeyEvent.VK_COMMA to Pair(',', '<'),
		KeyEvent.VK_PERIOD to Pair('.', '>'),
		KeyEvent.VK_SPACE to Pair(' ', null),
	)

	val charMap by lazy {
		codeMap.map { it.value.first to it.key }.toMap()
	}

}