package cc.lib.monopoly

import cc.lib.reflector.Reflector

class Rules : Reflector<Rules?>() {
	companion object {
		init {
			addAllFields(Rules::class.java)
		}
	}

    var startMoney = 1000 // initial starting momey
    var valueToWin = 5000 // If no other players go bankrupt then first player to this value is the winner
    var jailBumpEnabled = false // if true then when a player goes to jail they bump an existing jailed player to freedomn (Sebi Rule)
    var taxScale = 1f // iuse this to scale the 'meanness' of the tax squares.
    var jailMultiplier = false // cost to get out of jail goes up each time you goto jail.
	var maxTurnsInJail = 3 // max attempts to roll double to get out of jail
	var doublesRule = true // rolling doubles gives extra turn. 3 doubles goes to jail
}