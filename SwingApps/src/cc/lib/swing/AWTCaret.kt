package cc.lib.swing

import java.awt.Graphics
import java.awt.Rectangle
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultCaret

class AWTCaret @JvmOverloads constructor(private val mark: String = "<", blinkRate: Int = 500) : DefaultCaret() {
	init {
		if (blinkRate > 0) setBlinkRate(blinkRate)
	}

	@Synchronized
	override fun damage(r: Rectangle) {
		if (r == null) {
			return
		}
		val comp = component
		val fm = comp.getFontMetrics(comp.font)
		val textWidth = fm.stringWidth(">")
		val textHeight = fm.height
		x = r.x
		y = r.y
		width = textWidth
		height = textHeight
		repaint() // calls getComponent().repaint(x, y, width, height)
	}

	override fun paint(g: Graphics) {
		val comp = component ?: return
		val dot = dot
		var r: Rectangle? = null
		r = try {
			comp.modelToView(dot)
		} catch (e: BadLocationException) {
			return
		}
		if (r == null) {
			return
		}
		if (x != r.x || y != r.y) {
			repaint() // erase previous location of caret
			damage(r)
		}
		if (isVisible) {
			val fm = comp.getFontMetrics(comp.font)
			val textWidth = fm.stringWidth(">")
			val textHeight = fm.height
			g.color = comp.caretColor
			g.drawString(mark, x, y + fm.ascent)
		}
	}
}