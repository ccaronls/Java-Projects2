package cc.lib.zombicide

import cc.lib.annotation.RuleMeta
import cc.lib.reflector.Reflector

/**
 * Created by Chris Caron on 1/16/24.
 */
class ZRules : Reflector<ZRules>() {

	companion object {
		init {
			addAllFields(ZRules::class.java)
		}
	}

	@RuleMeta("Ranged indoor limitation", "Black Plague")
	var rangedIndoorLimitation = false

	/**
	 * Enables ULTRA RED mode
	 */
	@RuleMeta("Enable Ultra Red", "Wolfzburg")
	var ultraRed = true

	/**
	 * Enables familiars expansion
	 */
	@RuleMeta(description = "Enable Familiars", variation = "Friends and Foes", order = 1)
	var familiars = false

	@RuleMeta(description = "Enable Toxic Orcs", variation = "Friends and Foes", order = 1)
	var toxicOrcs = false

	@RuleMeta("Enable Ballista", "No Rest for the Wicked")
	var ballista = false

	@RuleMeta("Enable Necromantic Dragon", "No Rest for the Wicked")
	var necromanticDragon = false

	@RuleMeta("Enable Spectral walkers", "No Rest for the Wicked")
	var spectralWalkers = false

	/**
	 * Add some Rat King Necromancer cards to the deck
	 */
	@RuleMeta("Enable Rat King Necromancer", "Rat King")
	var ratKing = false

	/**
	 * Add some swampTroll Abomination cards to the deck
	 */
	@RuleMeta("Enable Swamp Troll Abomination", "Swamp Troll")
	var swampTroll = false

	/**
	 * Enable rat swarm spawns
	 */
	@RuleMeta("Enable Rat Swarms", "No Rest for the Wicked")
	var ratSwarms = false

	/**
	 * Enable murder of crows spawns
	 */
	@RuleMeta("Enable Murder of Crowz", "No Rest for the Wicked")
	var murderOfCrowz = false

}