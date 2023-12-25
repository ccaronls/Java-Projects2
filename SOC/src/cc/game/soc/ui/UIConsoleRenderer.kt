package cc.game.soc.ui

import cc.lib.game.AGraphics
import cc.lib.game.APGraphics
import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.ui.UIComponent
import cc.lib.utils.QueueRunner
import java.util.*

class UIConsoleRenderer(component: UIComponent) : UIRenderer(component) {
	internal class Line(val text: String, val color: GColor)

	private var startLine = 0
	private var maxVisibleLines = 1
	private var minVisibleLines = 0
	private val lines = LinkedList<Line>()
	fun scroll(numLines: Int) {
		startLine += numLines
		if (startLine > lines.size - maxVisibleLines) {
			startLine = lines.size - maxVisibleLines
		}
		if (startLine < 0) startLine = 0
		getComponent<UIComponent>().redraw()
	}

	fun scrollToTop() {
		startLine = 0
		getComponent<UIComponent>().redraw()
	}

	/*
	 *  (non-Javadoc)
	 * @see java.awt.Component#paint(java.awt.Graphics)
	 */
	override fun draw(g: APGraphics, pickX: Int, pickY: Int) {
		if (anim != null) {
			anim = if (anim!!.isDone) {
				null
			} else {
				anim!!.update(g)
				getComponent<UIComponent>().redraw()
				return
			}
		}
		drawPrivate(g)
	}

	private fun drawPrivate(g: AGraphics) {
		val txtHgt = g.textHeight.toInt()
		maxVisibleLines = getComponent<UIComponent>().getHeight() / txtHgt
		var y = 0f
		for (i in startLine until lines.size) {
			val l = lines[i]
			g.color = l.color
			g.textHeight = RenderConstants.textSizeSmall
			val dim = g.drawWrapString(0f, y, getComponent<UIComponent>().getWidth().toFloat(), l.text)
			y += dim.height
			if (y > getComponent<UIComponent>().getHeight()) {
				break
			}
		}
		if (minVisibleLines > 0) {
			minDimension = GDimension(getComponent<UIComponent>().getWidth().toFloat(), (Math.max(minVisibleLines, lines.size) * txtHgt).toFloat())
		}
	}

	fun setMinVisibleLines(min: Int) {
		minVisibleLines = min
	}

	private var anim: UIAnimation? = null
	private val queue: QueueRunner<Line> = object : QueueRunner<Line>() {
		override fun process(item: Line) {
			val text = item.text
			val color = item.color
			anim = if (startLine > 0) {

				// if user is scrolling, then show this line at top with a fade out.
				object : UIAnimation(500) {
					override fun draw(g: AGraphics, position: Float, dt: Float) {
						drawPrivate(g)
						g.color = GColor.BLACK.withAlpha(0.5f - position)
						val lines = g.generateWrappedLines(text, getComponent<UIComponent>().getWidth().toFloat())
						g.drawFilledRect(0f, 0f, getComponent<UIComponent>().getWidth().toFloat(), lines.size * g.textHeight)
						g.color = color.withAlpha(1.0f - position / 3)
						var y = 0f
						for (l in lines) {
							g.drawString(l, 0f, y)
							y += g.textHeight
						}
					}

					override fun onDone() {
						lines.addFirst(item)
						super.onDone()
					}
				}
			} else {
				// if user not scrolling, then show this line with the rest of lines tracing downward
				object : UIAnimation(500) {
					override fun draw(g: AGraphics, position: Float, dt: Float) {
						g.pushMatrix()
						g.color = color.withAlpha(position)
						val dim = g.drawWrapString(0f, 0f, getComponent<UIComponent>().getWidth().toFloat(), text)
						g.translate(0f, dim.height * position)
						drawPrivate(g)
						g.popMatrix()
					}

					override fun onDone() {
						lines.addFirst(item)
						super.onDone()
					}
				}
			}.start<UIAnimation>().also {
				getComponent<UIComponent>().redraw()
				it.block()
				if (startLine > 0) startLine++
				while (lines.size > 100) {
					lines.removeLast()
				}
				getComponent<UIComponent>().redraw()
			}
		}
	}

	@Synchronized
	fun addText(color: GColor, text: String) {
		queue.add(Line(text, color))
	}

	fun clear() {
		queue.clear()
		lines.clear()
		getComponent<UIComponent>().redraw()
	}
}