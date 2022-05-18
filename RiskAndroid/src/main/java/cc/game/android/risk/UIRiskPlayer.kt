package cc.game.android.risk

import cc.lib.game.Utils
import cc.lib.risk.Action
import cc.lib.risk.Army
import cc.lib.risk.RiskGame
import cc.lib.risk.RiskPlayer

/**
 * Created by Chris Caron on 9/14/21.
 */
class UIRiskPlayer(army: Army=Army.BLUE) : RiskPlayer(army) {

	override fun pickTerritoryToClaim(game: RiskGame, options: List<Int>): Int? {
		return RiskActivity.instance.pickTerritory(options, "$army Pick a territory to claim")
	}

	override fun pickTerritoryForArmy(game: RiskGame, options: List<Int>, remainingArmiesToPlace: Int, startArmiesToPlace: Int): Int? {
		return RiskActivity.instance.pickTerritory(options, String.format("%s Pick territory to place the %d%s of %d armies", army, remainingArmiesToPlace + 1, Utils.getSuffix(remainingArmiesToPlace + 1), startArmiesToPlace))
	}

	override fun pickTerritoryForNeutralArmy(game: RiskGame, options: List<Int>): Int? {
		return RiskActivity.instance.pickTerritory(options, "$army Pick territory to place a Neutral Army")
	}

	override fun pickTerritoryToAttackFrom(game: RiskGame, options: List<Int>): Int? {
		return RiskActivity.instance.pickTerritory(options, "$army Pick territory from which to stage an attack")
	}

	override fun pickTerritoryToAttack(game: RiskGame, territoryAttackingFrom: Int, options: List<Int>): Int? {
		return RiskActivity.instance.pickTerritory(options, "$army Pick Territory to Attack")
	}

	override fun pickTerritoryToMoveFrom(game: RiskGame, options: List<Int>): Int? {
		return RiskActivity.instance.pickTerritory(options, "$army Pick territory from which to move a armys")
	}

	override fun pickTerritoryToMoveTo(game: RiskGame, territoryMovingFrom: Int, options: List<Int>): Int? {
		return RiskActivity.instance.pickTerritory(options, "$army Pick Territory to Move an Army to")
	}

	override fun pickAction(game: RiskGame, actions: List<Action>, msg: String): Action? {
		return RiskActivity.instance.pickAction(actions, msg)
	}
}