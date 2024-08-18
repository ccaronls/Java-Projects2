package cc.lib.swing

import cc.lib.game.APGraphics
import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.game.IVector2D
import cc.lib.game.Renderable
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.math.Vector2D
import cc.lib.ui.UIComponent
import cc.lib.ui.UIKeyCode
import cc.lib.utils.GException
import java.awt.Color
import java.awt.Dimension
import java.awt.Event
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import javax.swing.JComponent

/**
 * Created by chriscaron on 2/21/18.
 */
abstract class AWTComponent : UIComponent, JComponent, Renderable, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
	private val log = LoggerFactory.getLogger(javaClass)
	private lateinit var G: AWTGraphics
	var mouseX = -1
		private set
	var mouseY = -1
		private set
	var isFocused = false
		private set
	private var padding = 5
	private var scrollAmount = -1
	private var scrollStartY = 0

	constructor() {}
	constructor(width: Int, height: Int) {
		setPreferredSize(width, height)
	}

	fun setMouseEnabled(enabled: Boolean) {
		if (enabled) {
			addMouseListener(this)
			addMouseMotionListener(this)
			addMouseWheelListener(this)
			isFocusable = true
			addKeyListener(this)
		} else {
			removeMouseListener(this)
			removeMouseMotionListener(this)
			removeMouseWheelListener(this)
			isFocusable = false
			removeKeyListener(this)
			mouseY = -1
			mouseX = mouseY
		}
	}

	fun setGesturesEnabled() {
		log.warn("Gestures not supported")
	}

	override fun paint(g: Graphics) {
		try {
			if (width > 0 && height > 0) {
				val c = g.color
				g.color = getBackground() //AWTUtils.toColor(GColor.TRANSPARENT));
				g.fillRect(0, 0, super.getWidth(), super.getHeight())
				g.color = c
				//g.setClip(padding,padding, getWidth()+1, getHeight()+1);
				if (!::G.isInitialized) {
					G = if (g is Graphics2D) AWTGraphics2(g, this) else AWTGraphics(g, this)
					init(G)
					repaint()
				} else {
					val progress = initProgress
					G.graphics = g
					G.initViewport(width, height)
					G.ortho()
					if (progress >= 1) {
						val matStack = G.matrixStackSize
						if (scrollAmount < 0) scrollAmount = G.textHeight.toInt()
						if (scrollStartY != 0) {
							G.pushMatrix()
							G.translate(0f, scrollStartY.toFloat())
							paint(G, mouseX, mouseY)
							G.popMatrix()
						} else {
							try {
								paint(G, mouseX, mouseY)
							} catch (e: Exception) {
								log.error("Error: %s", e)
								e.printStackTrace()
								throw GException(e)
							}
						}
						if (G.matrixStackSize != matStack) {
							throw GException("Matrix stack not zero")
						}
					} else {
						val f = g.font
						G.clearScreen(GColor.CYAN)
						G.color = GColor.WHITE
						G.textHeight = (height / 10).toFloat()
						val x = (width / 2).toFloat()
						var y = (height / 3).toFloat()
						val txt = "INITIALIZING"
						val tw = G.getTextWidth(txt)
						val th = G.textHeight
						while (tw > width && G.textHeight > 8) {
							G.textHeight = G.textHeight - 2
						}
						G.drawJustifiedString(x - tw / 2, y, txt)
						y += th
						G.drawFilledRect(x - tw / 2, y, tw * progress, th)
						G.font = f
						repaint()
					}
				}
				//g.setClip(0, 0, super.getWidth(), super.getHeight());
				if (isFocused) {
					//                System.out.println("AWT " + toString() + " has focus!");
					g.color = Color.BLUE
					g.drawRect(0, 0, super.getWidth() - 2, super.getHeight() - 2)
				}
			} else {
				repaint()
			}
		} catch (e: Throwable) {
			e.printStackTrace()
			throw GException(e)
		}
	}

	protected abstract fun paint(g: AWTGraphics, mouseX: Int, mouseY: Int)

	/**
	 * Called on first call from paint
	 * @param g
	 */
	protected open fun init(g: AWTGraphics) {}

	/**
	 * Return value between 0-1 that is the progress of init flow
	 * @return
	 */
	protected open val initProgress: Float
		protected get() = 1f

	override fun mouseClicked(e: MouseEvent) {
		//Utils.println("mouseClicked");
		onClick()
	}

	override fun mousePressed(e: MouseEvent) {
		Utils.println("mousePressed")
		grabFocus()
		mouseX = e.x - padding
		mouseY = e.y - padding
		repaint()
		onMousePressed(mouseX, mouseY)
	}

	protected fun onMousePressed(mouseX: Int, mouseY: Int) {}

	@Synchronized
	override fun mouseReleased(e: MouseEvent) {
		Utils.println("mouseReleased")
		if (dragging) {
			onDragStopped()
			dragging = false
		}
		mouseX = e.x - padding
		mouseY = e.y - padding
		repaint()
	}

	@Synchronized
	override fun mouseEntered(e: MouseEvent) {
//        grabFocus();
		isFocused = true
		repaint()
		onFocusGained()
	}

	protected open fun onFocusGained() {}
	protected open fun onFocusLost() {}

	@Synchronized
	override fun mouseExited(e: MouseEvent) {
		isFocused = false
		mouseY = -1
		mouseX = mouseY
		repaint()
		onFocusLost()
	}

	var dragging = false

	@Synchronized
	override fun mouseWheelMoved(e: MouseWheelEvent) {
		onMouseWheel(e.wheelRotation)
	}

	protected open fun onMouseWheel(rotation: Int) {
		val d = minimumSize
		val maxScroll = height - d.height
		if (maxScroll < 0) {
			scrollStartY = (scrollStartY - rotation * scrollAmount).coerceIn(maxScroll, 0)
			repaint()
		}
	}

	protected open fun onZoom(scale: Float) {} // Future work

	@Synchronized
	override fun lostFocus(ev: Event, obj: Any): Boolean {
		repaint()
		return super.lostFocus(ev, obj)
	}

	@Synchronized
	override fun mouseDragged(e: MouseEvent) {
		//Utils.println("mouseDragged")
		val x = e.x - padding
		val y = e.y - padding
		val dx = x - mouseX
		val dy = y - mouseY
		mouseX = x
		mouseY = y
		if (!dragging) {
			onDragStarted(mouseX, mouseY)
			dragging = true
		} else {
			onDrag(mouseX, mouseY, dx, dy)
		}
		//Utils.println("mouseDragged");
		repaint()
	}

	override fun mouseMoved(e: MouseEvent) {
		//log.info("mouse %d,%d", e.getX(), e.getY());
		//Utils.println("mouseMoved");
		mouseX = e.x - padding
		mouseY = e.y - padding
		onMouseMoved(mouseX, mouseY)
		repaint()
	}

	protected open fun onMouseMoved(mouseX: Int, mouseY: Int) {}

	override fun keyTyped(evt: KeyEvent) {
	}

	override fun keyPressed(evt: KeyEvent) {
		when (evt.keyCode) {
			KeyEvent.VK_RIGHT -> onKeyEvent(true, UIKeyCode.RIGHT)
			KeyEvent.VK_LEFT -> onKeyEvent(true, UIKeyCode.LEFT)
			KeyEvent.VK_DOWN -> onKeyEvent(true, UIKeyCode.DOWN)
			KeyEvent.VK_UP -> onKeyEvent(true, UIKeyCode.UP)
			KeyEvent.VK_ENTER -> onKeyEvent(true, UIKeyCode.CENTER)
			KeyEvent.VK_ESCAPE,
			KeyEvent.VK_DELETE -> onKeyEvent(true, UIKeyCode.BACK)
		}
	}

	override fun keyReleased(evt: KeyEvent) {
		when (evt.keyCode) {
			KeyEvent.VK_RIGHT -> onKeyEvent(false, UIKeyCode.RIGHT)
			KeyEvent.VK_LEFT -> onKeyEvent(false, UIKeyCode.LEFT)
			KeyEvent.VK_DOWN -> onKeyEvent(false, UIKeyCode.DOWN)
			KeyEvent.VK_UP -> onKeyEvent(false, UIKeyCode.UP)
			KeyEvent.VK_ENTER -> onKeyEvent(false, UIKeyCode.CENTER)
			KeyEvent.VK_ESCAPE,
			KeyEvent.VK_DELETE -> onKeyEvent(false, UIKeyCode.BACK)
		}
	}

	protected open fun onDragStarted(x: Int, y: Int) {}
	protected open fun onDragStopped() {}
	protected open fun onDrag(x: Int, y: Int, dx: Int, dy: Int) {}
	protected open fun onClick() {}

	/**
	 * Return true is consumed
	 */
	protected open fun onKeyEvent(down: Boolean, code: UIKeyCode): Boolean {
		return false
	}

	override fun getX(): Int {
		return super.getX() + padding
	}

	override fun getY(): Int {
		return super.getY() + padding
	}

	override fun getWidth(): Int {
		return super.getWidth() - padding * 2
	}

	override fun getHeight(): Int {
		return super.getHeight() - padding * 2
	}

	override fun getViewportWidth(): Int {
		return width
	}

	override fun getViewportHeight(): Int {
		return height
	}

	fun setMinimumSize(w: Int, h: Int) {
//        log.debug("set min size: %d x %d", w, h);
		super.setMinimumSize(Dimension(w, h))
	}

	fun setMinimumSize(dim: GDimension) {
		setMinimumSize(Math.round(dim.width), Math.round(dim.height))
	}

	fun setPreferredSize(w: Int, h: Int) {
		//log.debug("set pref size: %d x %d", w, h);
		super.setPreferredSize(Dimension(w, h))
	}

	fun setMaximumSize(w: Int, h: Int) {
		log.debug("set max size: %d x %d", w, h)
		super.setMaximumSize(Dimension(w, h))
	}

	val aPGraphics: APGraphics
		get() = G

	fun setPadding(padding: Int) {
		this.padding = padding
	}

	var background: GColor?
		get() = with(super.getBackground()) {
			GColor(red, green, blue, alpha)
		}
		set(color) {
			super.setBackground(AWTUtils.toColor(color))
		}

	/**
	 * Get the position in viewport coords
	 * @return
	 */
	protected val mousePos: IVector2D
		protected get() = getMousePos(mouseX, mouseY)

	protected fun getMousePos(mx: Int, my: Int): IVector2D {
		return aPGraphics.screenToViewport(mx, my)
	}

	override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
		val rect = bounds
		super.setBounds(x, y, width, height)
		if (::G.isInitialized && (rect.width != width || rect.height != height)) {
			log.info("Dimension changed to %d x %d", width, height)
			onDimensionChanged(G, width, height)
		}
	}

	protected open fun onDimensionChanged(g: AWTGraphics, width: Int, height: Int) {
		log.info("Dimension changed to %d x %d", width, height)
	}

	override fun redraw() {
		repaint()
	}

	override fun getViewportLocation(): Vector2D {
		val pt = super.getLocationOnScreen()
		return Vector2D(pt.x.toFloat(), pt.y.toFloat())
	}
}