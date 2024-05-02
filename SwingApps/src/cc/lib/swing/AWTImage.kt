package cc.lib.swing

import cc.lib.game.AGraphics
import cc.lib.game.AImage
import java.awt.Component
import java.awt.Graphics
import java.awt.Image
import java.awt.Toolkit
import java.awt.image.FilteredImageSource
import java.awt.image.ImageFilter
import java.awt.image.ImageProducer
import java.awt.image.PixelGrabber
import javax.swing.Icon
import kotlin.math.roundToInt

class AWTImage constructor(val image: Image, val comp: Component) : AImage(), Icon {
	private val _pixels: IntArray by lazy {
		val w = image.getWidth(comp)
		val h = image.getHeight(comp)
		IntArray(w * h).also {
			val pg = PixelGrabber(image, 0, 0, w, h, pixels, 0, w)
			try {
				if (!pg.grabPixels()) {
					throw RuntimeException("Failed to grabPixels")
				}
			} catch (e: RuntimeException) {
				throw e
			} catch (e: Exception) {
				throw RuntimeException("Failed to grabPixels", e)
			}
		}
	}

	override fun getWidth(): Float {
		return image.getWidth(comp).toFloat()
	}

	override fun getHeight(): Float {
		return image.getHeight(comp).toFloat()
	}

	override fun getPixels() = _pixels

	override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
		g.drawImage(image, x, y, null)
	}

	override fun getIconWidth(): Int {
		return image.getWidth(null)
	}

	override fun getIconHeight(): Int {
		return image.getHeight(null)
	}

	fun transform(filter: ImageFilter): AWTImage {
		val p: ImageProducer = FilteredImageSource(image.source, filter)
		return AWTImage(Toolkit.getDefaultToolkit().createImage(p), comp)
	}

	override fun draw(g: AGraphics, x: Float, y: Float) {
		with(g as AWTGraphics2) {
			g.graphics.drawImage(image, x.roundToInt(), y.roundToInt(), null)
		}
	}
}