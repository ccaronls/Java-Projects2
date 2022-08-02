package cc.game.soc.core

import cc.game.soc.core.Player.VertexChoice

enum class DevelopmentArea(val stringId: String, val move: MoveType, val commodity: CommodityType, val vertexType: VertexType, val choice: VertexChoice, vararg _levelNameId: String) : ILocalized {
	Science("Science", MoveType.IMPROVE_CITY_SCIENCE, CommodityType.Paper, VertexType.METROPOLIS_SCIENCE, VertexChoice.SCIENCE_METROPOLIS,
		"", "Abbey", "Library", "Aqueduct", "Theatre", "University"),
	Trade("Trade", MoveType.IMPROVE_CITY_TRADE, CommodityType.Cloth, VertexType.METROPOLIS_TRADE, VertexChoice.TRADE_METROPOLIS,
		"", "Market", "Trading House", "Merchant Guild", "Bank", "Bazaar"),
	Politics("Politics", MoveType.IMPROVE_CITY_POLITICS, CommodityType.Coin, VertexType.METROPOLIS_POLITICS, VertexChoice.POLITICS_METROPOLIS,
		"", "Town Hall", "Church", "Cathedral", "Castle");

	val levelNameId = arrayOf(*_levelNameId)

	override fun getNameId(): String {
		return stringId
	}

	fun getLevelName(level: Int): String {
		return levelNameId[level]
	}

	companion object {
		/*
	 * Special abilities
	 * Science, when achieve aqueduct, then on a die roll, if the player receives no resources, then they get to pick one
	 * Trade, when achieve merchant guild then get 2:1 trading on resources and commodities
	 * Politics, when achieve fortress then get to promote knights to level 3
	 */
		const val MAX_CITY_IMPROVEMENT = 5
		const val MIN_METROPOLIS_IMPROVEMENT = 4
		const val CITY_IMPROVEMENT_FOR_SPECIAL_ABILITY = 3
	}
}