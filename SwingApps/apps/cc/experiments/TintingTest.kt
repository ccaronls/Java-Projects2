package cc.experiments

import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.swing.AWTComponent
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTGraphics
import java.awt.AlphaComposite
import java.awt.BorderLayout
import javax.swing.JSlider
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class TintingTest internal constructor() : AWTComponent(), ChangeListener {
	override fun stateChanged(e: ChangeEvent) {
		repaint()
	}

	var slider: JSlider
	override var initProgress = 0f
	override fun init(g: AWTGraphics) {
		image = g.loadImage("zchar_benson.gif")
		initProgress = 0.5f
		repaint()
		outline = g.loadImage("zchar_benson_outline.gif")
		initProgress = 1f
		repaint()
	}

	var image = 0
	var outline = 0
	var modes = intArrayOf(
		AlphaComposite.CLEAR,
		AlphaComposite.SRC,
		AlphaComposite.SRC_OVER,
		AlphaComposite.DST_OVER,
		AlphaComposite.SRC_IN,
		AlphaComposite.DST_IN,
		AlphaComposite.SRC_OUT,
		AlphaComposite.DST_OUT,
		AlphaComposite.DST,
		AlphaComposite.SRC_ATOP,
		AlphaComposite.DST_ATOP,
		AlphaComposite.XOR
	)
	var names = arrayOf(
		"CLEAR",
		"SRC",
		"SRC_OVER",
		"DST_OVER",
		"SRC_IN",
		"DST_IN",
		"SRC_OUT",
		"DST_OUT",
		"DST",
		"SRC_ATOP",
		"DST_ATOP",
		"XOR"
	)

	init {
		val frame: AWTFrame = object : AWTFrame("Tinting Test") {
			override fun onWindowClosing() {
				try {
					//app.figures.saveToFile(app.figuresFile);
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
		}
		slider = JSlider()
		slider.addChangeListener(this)
		slider.minimum = 0
		slider.maximum = 100
		slider.value = 50
		frame.add(this)
		frame.add(slider, BorderLayout.SOUTH)
		frame.centerToScreen(600, 600)
	}

	override fun paint(g: AWTGraphics) {
		val rect = GRectangle(0f, 0f, width.toFloat(), height.toFloat())
		val imagesrc = g.getImage(image)
		val outlineSrc = g.getImage(outline)
		rect.scale(.25f)
		rect.left = 0f
		rect.top = 0f
		val srcRect = rect.fit(imagesrc!!)
		var mode = 0
		g.pushMatrix()
		for (i in 0..2) {
			g.pushMatrix()
			for (ii in 0..3) {
				g.removeFilter()

				//g.removeFilter();
				//g.setTransparencyFilter(.5f);
				//g.drawImage(image, srcRect);
				//
				//g.setColor(GColor.RED.withAlpha(0));
				g.setTintFilter(GColor.WHITE, GColor.RED)
				//g.setComposite(BlendComposite.Multiply);
				g.drawImage(outline, srcRect)
				g.removeFilter()
				//g.drawFilledRect(srcRect);
				g.removeFilter()
				g.color = GColor.BLACK
				g.drawJustifiedString(rect.width / 2, rect.height + 5, Justify.CENTER, Justify.TOP, names[mode])
				g.translate((width / 4).toFloat(), 0f)
				mode++
			}
			g.popMatrix()
			g.translate(0f, (height / 3).toFloat())
		}
		g.popMatrix()
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			TintingTest()
		}
	}
}
