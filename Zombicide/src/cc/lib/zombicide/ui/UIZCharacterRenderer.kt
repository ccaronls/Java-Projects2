package cc.lib.zombicide.ui

import cc.lib.game.*
import cc.lib.ui.UIComponent
import cc.lib.ui.UIRenderer
import cc.lib.utils.Table
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZGame
import java.util.*

class UIZCharacterRenderer(component: UIComponent) : UIRenderer(component) {
	interface IWrappable {
		fun drawWrapped(g: APGraphics, maxWidth: Float, dimmed: Boolean): GDimension
	}

	private var textColor = GColor.BLACK

	internal class StringLine(val color: GColor, val msg: String) : IWrappable {
		override fun drawWrapped(g: APGraphics, maxWidth: Float, dimmed: Boolean): GDimension {
			g.color = if (dimmed) color.withAlpha(.5f) else color
			return g.drawWrapString(0f, 0f, maxWidth, Justify.RIGHT, Justify.TOP, msg)
		}
	}

	var actorInfo: ZActor? = null
		set(value) {
			if (field != value) {
				field = value
				redraw()
			}
		}
	private val messages = LinkedList<IWrappable>()
	private val game: ZGame
		get() = UIZombicide.instance

	fun setTextColor(textColor: GColor) {
		this.textColor = textColor
	}

	@Synchronized
	fun addMessage(msg: String) {
		addMessage(msg, textColor)
	}

	@Synchronized
	fun addMessage(msg: String, color: GColor) {
		messages.addFirst(StringLine(color, msg))
		while (messages.size > 32) {
			messages.removeLast()
		}
		redraw()
	}

	@Synchronized
	fun addWrappable(line: IWrappable) {
		messages.addFirst(line)
		while (messages.size > 32) {
			messages.removeLast()
		}
		redraw()
	}

	@Synchronized
	fun clearMessages() {
		messages.clear()
	}

	@Synchronized
	override fun draw(g: APGraphics, px: Int, py: Int) {
		var info: IDimension? = null
		g.color = textColor
		when {
			actorInfo != null -> {
				info = actorInfo?.drawInfo(g, game, width, height)
			}
			game.questInitialized -> {
				val quest = game.quest
				val table = Table(object : Table.Model {
					override fun getMaxCharsPerLine(): Int {
						return 128
					}
				}).addColumn(quest.name, quest.quest.description.replace('\n', ' '))
				info = table.draw(g)
			}
		}
		g.color = textColor
		var y = 0
		val maxWidth: Float = g.viewportWidth - (info?.width ?: 0f)
		var dimmed = false
		for (msg in messages) {
			g.pushMatrix()
			g.translate(g.viewportWidth.toFloat(), y.toFloat())
			val d = msg.drawWrapped(g, maxWidth, dimmed)
			//GDimension d = g.drawWrapString(g.getViewportWidth(), y, maxWidth, Justify.RIGHT, Justify.TOP, msg);
			g.popMatrix()
			dimmed = true
			y += d.getHeight().toInt()
		}
		info?.let {
			setMinDimension(width, it.height.coerceAtLeast(y.toFloat()))
		}
	}

}