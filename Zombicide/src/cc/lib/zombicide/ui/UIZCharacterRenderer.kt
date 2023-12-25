package cc.lib.zombicide.ui

import cc.lib.game.*
import cc.lib.ui.ButtonHandler
import cc.lib.ui.UIComponent
import cc.lib.ui.UIRenderer
import cc.lib.utils.Table
import cc.lib.utils.prettify
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZCharacter
import cc.lib.zombicide.ZEquipSlot
import cc.lib.zombicide.ZZombie
import java.util.*

class UIZCharacterRenderer(component: UIComponent) : UIRenderer(component) {
	interface IWrappable {
		fun drawWrapped(g: AGraphics, maxWidth: Float, dimmed: Boolean): GDimension
	}

	private var textColor = GColor.BLACK

	internal class StringLine(val color: GColor, val msg: String) : IWrappable {
		override fun drawWrapped(g: AGraphics, maxWidth: Float, dimmed: Boolean): GDimension {
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
	private val game: UIZombicide
		get() = UIZombicide.instance

	fun setTextColor(textColor: GColor) {
		this.textColor = textColor
	}

	fun addMessage(msg: String) {
		addMessage(msg, textColor)
	}

	fun addMessage(msg: String, color: GColor) {
		messages.addFirst(StringLine(color, msg))
		while (messages.size > 32) {
			messages.removeLast()
		}
		redraw()
	}

	fun addWrappable(line: IWrappable) {
		messages.addFirst(line)
		while (messages.size > 32) {
			messages.removeLast()
		}
		redraw()
	}

	fun clearMessages() {
		messages.clear()
	}

	fun drawZombieInfo(g: AGraphics, zombie: ZZombie): IDimension = with(zombie) {
		val info = Table(getLabel()).setNoBorder()
		info.addRow("Min Hits", type.minDamageToDestroy)
		info.addRow("Actions", type.actionsPerTurn)
		info.addRow("Experience", type.expProvided)
		info.addRow("Ignores Armor", type.ignoresArmor)
		info.addRow("Ranged Priority", type.rangedPriority)
		val outer = Table().setNoBorder()
		outer.addRow(info, type.description)
		outer.draw(g)
	}

	fun drawCharacterInfo(g: AGraphics, char: ZCharacter): IDimension = with(char) {
		clearButtons()
		val info = getEquippedTable(game).setNoBorder().setPadding(0)
		val slotInfo = getSlotInfo(ZEquipSlot.BACKPACK, game) ?: Table()
		addButton(slotInfo, object : ButtonHandler() {
			override fun onHoverEnter() {
				game.boardRenderer.setOverlay(char.getBackpackTable(game))
			}
		})
		info.addColumn(ZEquipSlot.BACKPACK.getLabel() + if (isBackpackFull) " (full)" else "", slotInfo)
		val stats = getStatsTable().setNoBorder().setPadding(0)
		info.addColumn("Stats", stats)
		if (getAvailableSkills().isNotEmpty()) {
			val skills = Table(this).setNoBorder().addColumnNoHeader(getAvailableSkills().map {
				it.prettify()
			})
			info.addColumn("Skills", skills)
			addButton(skills, object : ButtonHandler() {
				override fun onHoverEnter() {
					game.boardRenderer.setOverlay(char.getSkillsTable())
				}
			})
		}
		val main = Table(this).setNoBorder()
		if (isDead) {
			main.addRow(String.format("%s (%s) Killed in Action",
				type.getLabel(), type.characterClass))
		} else {
			main.addRow(String.format("%s (%s) Body:%s",
				type.getLabel(), type.characterClass, type.alternateBodySlot
			))
		}
		main.addRow(info)
		main.draw(g)
	}


	override fun draw(g: APGraphics, px: Int, py: Int) {
		var info: IDimension? = null
		g.color = textColor
		clearButtons()
		when {
			actorInfo is ZZombie -> info = drawZombieInfo(g, actorInfo as ZZombie)
			actorInfo is ZCharacter -> info = drawCharacterInfo(g, actorInfo as ZCharacter)
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