package cc.applets.soc

import cc.lib.game.GColor
import cc.lib.math.Vector2D
import cc.lib.swing.AWTComponent
import cc.lib.swing.AWTGraphics
import cc.lib.ui.UIComponent
import cc.lib.ui.UIRenderer

/**
 * Created by chriscaron on 2/27/18.
 */
open class SOCComponent internal constructor() : AWTComponent(), UIComponent {
	private lateinit var delegate: UIRenderer
	@JvmField
    protected var progress = 1f
	override fun paint(g: AWTGraphics, mouseX: Int, mouseY: Int) {
		//g.clearScreen(GColor.DARK_GRAY);
		delegate.draw(g, mouseX, mouseY)
		setMinimumSize(delegate.minDimension)
	}

	override fun redraw() {
		repaint()
	}

	override fun setRenderer(r: UIRenderer) {
		delegate = r
	}

	override fun onClick() {
		delegate.onClick()
	}

	override fun onDragStarted(x: Int, y: Int) {
		delegate.onDragStart(x, y)
	}

	override fun onDragStopped() {
		delegate.onDragEnd()
	}

	override fun init(g: AWTGraphics) {
		val assets = imagesToLoad
		if (assets.isNotEmpty()) {
			progress = 0f
			kotlin.runCatching {
				g.addSearchPath("images")
				val ids = IntArray(assets.size)
				val delta = 1.0f / ids.size
				for (i in ids.indices) {
					ids[i] = g.loadImage(assets[i][0] as String, assets[i][1] as GColor?)
					progress += delta
					redraw()
				}
				onImagesLoaded(ids)
				progress = 1f
			}
		}
	}

	protected open val imagesToLoad: Array<Array<Any?>>
		protected get() = Array(0) { Array(0) {} }

	protected open fun onImagesLoaded(ids: IntArray) {
		throw RuntimeException("onImagesLoaded not handled")
	}

	override val initProgress: Float = progress

	override fun getViewportLocation(): Vector2D {
		val pt = super.getLocationOnScreen()
		return Vector2D(pt.x.toFloat(), pt.y.toFloat())
	}
}