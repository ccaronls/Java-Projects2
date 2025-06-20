package cc.lib.zombicide.ui

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.game.IDimension
import cc.lib.game.Justify
import cc.lib.ui.ButtonHandler
import cc.lib.ui.UIComponent
import cc.lib.ui.UIKeyCode
import cc.lib.ui.UIRenderer
import cc.lib.utils.Table
import cc.lib.utils.prettify
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZCharacter
import cc.lib.zombicide.ZEquipSlot
import cc.lib.zombicide.ZOgre
import cc.lib.zombicide.ZSiegeEngine
import cc.lib.zombicide.ZZombie
import java.util.Collections
import java.util.LinkedList

abstract class UIZCharacterRenderer(component: UIComponent) : UIRenderer(component) {
	interface IWrapped {
		fun drawWrapped(g: AGraphics, maxWidth: Float, dimmed: Boolean): GDimension
	}

	private var textColor = GColor.BLACK

	internal class StringLine(val color: GColor, val msg: String) : IWrapped {
		override fun drawWrapped(g: AGraphics, maxWidth: Float, dimmed: Boolean): GDimension {
			g.color = if (dimmed) color.withAlpha(.5f) else color
			return g.drawWrapString(0f, 0f, maxWidth, Justify.RIGHT, Justify.TOP, msg)
		}
	}

	var actorInfo: ZActor? = null
		set(value) {
			field = value
			redraw()
		}

	private val messages = Collections.synchronizedList(LinkedList<IWrapped>())
	private val game: UIZombicide
		get() = UIZombicide.instance

	fun setTextColor(textColor: GColor) {
		this.textColor = textColor
	}

	fun addMessage(msg: String) {
		addMessage(msg, textColor)
	}

	fun addMessage(msg: String, color: GColor) {
		messages.add(0, StringLine(color, msg))
		while (messages.size > 32) {
			messages.removeLast()
		}
		redraw()
	}

	@Synchronized
	fun addWrapped(line: IWrapped) {
		messages.add(0, line)
		while (messages.size > 32) {
			messages.removeLast()
		}
		redraw()
	}

	@Synchronized
	fun clearMessages() {
		messages.clear()
	}

	fun drawZombieInfo(g: AGraphics, zombie: ZZombie): IDimension = with(zombie) {
		val info = Table(getLabel()).setNoBorder()
		info.addRow("Damage Per hit", type.damagePerHit)
		info.addRow("Min Hits", type.minDamageToDestroy)
		info.addRow("Actions", "${zombie.actionsPerTurn}/${zombie.actionsLeftThisTurn}")
		info.addRow("Experience", type.expProvided)
		info.addRow("Ignores Armor", type.ignoresArmor)
		info.addRow("Ranged Priority", type.targetingPriority)
		(zombie as? ZOgre)?.let {
			info.addRow("Aggressive", it.aggressive)
		}
		val outer = Table().setNoBorder()
		outer.addRow(info, type.description)
		outer.draw(g)
	}

	fun drawCatapultInfo(g: AGraphics, catapult: ZSiegeEngine): IDimension = Table()
		.setNoBorder()
		.addColumn("CATAPULT", catapult.getInfo(game)).draw(g)

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
		val stats = getStatsTable(game.rules).setNoBorder().setPadding(0)
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


	override fun draw(g: AGraphics) {
		var info: IDimension? = null
		g.color = textColor
		clearButtons()
		when {
			actorInfo is ZZombie -> info = drawZombieInfo(g, actorInfo as ZZombie)
			actorInfo is ZCharacter -> info = drawCharacterInfo(g, actorInfo as ZCharacter)
			actorInfo is ZSiegeEngine -> info = drawCatapultInfo(g, actorInfo as ZSiegeEngine)
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
		for (msg in messages.toList()) {
			g.pushMatrix()
			g.translate(g.viewportWidth.toFloat(), y.toFloat())
			val d = msg.drawWrapped(g, maxWidth, dimmed)
			//GDimension d = g.drawWrapString(g.getViewportWidth(), y, maxWidth, Justify.RIGHT, Justify.TOP, msg);
			g.popMatrix()
			dimmed = true
			y += d.height.toInt()
		}
		info?.let {
			setMinDimension(width, it.height.coerceAtLeast(y.toFloat()))
		}
	}

	abstract fun scrollToTop()

	override fun onKeyTyped(code: UIKeyCode): Boolean {
		when (code) {
			UIKeyCode.BACK -> UIZombicide.instance.focusOnMainMenu()
			UIKeyCode.RIGHT -> UIZombicide.instance.focusOnBoard()
			else -> return false
		}
		scrollToTop()
		return true
	}

	override fun onFocusChanged(gained: Boolean) {
		if (!gained)
			scrollToTop()
	}
}