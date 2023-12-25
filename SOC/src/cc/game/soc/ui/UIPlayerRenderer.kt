package cc.game.soc.ui

import cc.game.soc.core.*
import cc.lib.game.APGraphics
import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.game.Utils
import cc.lib.ui.UIComponent

/**
 *
 * @author Chris Caron
 *
 * Base player type to interact with GUI
 */
class UIPlayerRenderer(component: UIComponent) : UIRenderer(component) {
	var color = GColor.BLACK

	@JvmField
	var numCardAnimations = 0

	@JvmField
	var playerNum = 0
	fun setPlayer(playerNum: Int) {
		this.playerNum = playerNum
		getComponent<UIComponent>().redraw()
	}

	override fun draw(g: APGraphics, px: Int, py: Int) {
		if (playerNum < 1) return
		val soc = UISOC.instance
		val player = soc.getPlayerByPlayerNum(playerNum) as UIPlayer
		g.textHeight = RenderConstants.textSizeBig
		//g.setTextStyles(AGraphics.TextStyle.BOLD);
		val str = StringBuffer()
		str.append(String.format("%1\$s %2\$d Points\nCards %3\$d(%4\$d)", player.name, player.points, player.totalCardsLeftInHand, soc.rules.getMaxSafeCardsForPlayer(player.playerNum, soc.board)))
		str.append("\n")
		if (player.isInfoVisible) {
			for (t in ResourceType.values()) {
				val num = player.getCardCount(t)
				str.append(t.getNameId()).append(" X ").append(num).append("\n")
			}
			if (soc.rules.isEnableCitiesAndKnightsExpansion) {
				for (t in CommodityType.values()) {
					val num = player.getCardCount(t)
					str.append(t.getNameId()).append(" X ").append(num).append("\n")
				}
				for (c in player.getCards(CardType.Progress)) {
					str.append(c.name).append(" (").append(c.cardStatus.getNameId()).append(")\n")
				}
			} else {
				for (c in player.getCards(CardType.Development)) {
					// dont need to show these since it is covered by the army size
					when (c.cardStatus) {
						CardStatus.USABLE -> str.append(c.name).append("\n")
						CardStatus.USED -> {
							// ignore these since they are covered by the army size
							if (c.typeOrdinal == DevelopmentCardType.Soldier.ordinal || c.typeOrdinal == DevelopmentCardType.Warship.ordinal) continue
							str.append(c.name).append(" (").append(c.cardStatus.getNameId()).append(")\n")
						}
						CardStatus.UNUSABLE -> str.append(c.name).append(" (").append(c.cardStatus.getNameId()).append(")\n")
					}
				}
			}
		} else {
			if (soc.rules.isEnableCitiesAndKnightsExpansion) {
				var num = player.getCardCount(CardType.Resource)
				num += player.getCardCount(CardType.Commodity)
				str.append(String.format("Progress X %d", num)).append("\n")
				num = player.getCardCount(CardType.Progress)
				str.append(String.format("Progress X %d", num)).append("\n")
			} else {
				val num = player.unusedCardCount
				str.append(String.format("Cards X %d", num)).append("\n")
			}
		}

		//int numSettlements = soc.getBoard().getNumVertsOfType(player.getPlayerNum(), VertexType.SETTLEMENT);
		//int numCities      = soc.getBoard().getNumVertsOfType(player.getPlayerNum(), VertexType.CITY, VertexType.WALLED_CITY);
		//nt numMetros      = soc.getBoard().getNumVertsOfType(player.getPlayerNum(), VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE);
		val numKnights = soc.board.getNumKnightsForPlayer(player.playerNum)
		val knightLevel = soc.board.getKnightLevelForPlayer(player.playerNum, true, false)
		val maxKnightLevel = soc.board.getKnightLevelForPlayer(player.playerNum, true, true)
		val numDiscoveredTiles = player.numDiscoveredTerritories
		val size = player.getArmySize(soc.board)
		if (size > 0) {
			str.append(String.format("Army X %d", size)).append("\n")
		}
		if (numKnights > 0) {
			str.append(String.format("Knights %1\$d/%2\$d", knightLevel, maxKnightLevel)).append("\n")
		}
		str.append(String.format("Road Length %d", player.roadLength)).append("\n")
		for (sv in SpecialVictoryType.values()) {
			val num = player.getCardCount(sv)
			if (num > 0) {
				str.append(sv.getNameId())
				if (sv.points != 0) str.append(Utils.getSignedString(num * sv.points))
				str.append("\n")
			}
		}
		for (d in DevelopmentArea.values()) {
			if (player.getCityDevelopment(d) > 0) {
				str.append(d.getNameId()).append(" ").append(d.getLevelName(player.getCityDevelopment(d))).append(" (").append(player.getCityDevelopment(d)).append(") ")
				if (soc.getMetropolisPlayer(d) == player.playerNum) {
					str.append(" +").append(soc.rules.pointsPerMetropolis)
				}
				str.append("\n")
			}
		}
		player.merchantFleetTradable?.let {
			str.append(String.format("Merchant Fleet %s", it.name)).append("\n")
		}
		run {
			val num = soc.board.getNumDiscoveredIslands(player.playerNum)
			if (num > 0) {
				str.append(String.format("Discovered Islands X %$1d %$2s", num, Utils.getSignedStringOrEmptyWhenZero(num * soc.rules.pointsIslandDiscovery))).append("\n")
			}
		}
		if (numDiscoveredTiles > 0) {
			str.append(String.format("Discovered Tiles X %d", numDiscoveredTiles)).append("\n")
		}
		val padding = RenderConstants.textMargin
		g.color = player.color
		val dim = g.drawWrapString(padding, padding, getComponent<UIComponent>().getWidth().toFloat(), str.toString())
		val min = GDimension(dim.width + padding * 2, dim.height + padding * 2)
		minDimension = min
		if (isCurrentPlayer) {
			g.drawRect(0f, 0f, getComponent<UIComponent>().getWidth() - RenderConstants.thickLineThickness / 2, Math.max(min.height, getComponent<UIComponent>().getHeight().toFloat()) - RenderConstants.thickLineThickness / 2, RenderConstants.thickLineThickness)
		}
	}

	val isCurrentPlayer: Boolean
		get() = playerNum == UISOC.instance.curPlayerNum
}