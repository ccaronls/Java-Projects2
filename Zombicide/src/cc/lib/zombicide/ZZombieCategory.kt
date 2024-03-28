package cc.lib.zombicide

import cc.lib.annotation.Keep

@Keep
enum class ZZombieCategory(val label: String, vararg _decks: ZDeckType) {
	ALL("All Zombies", *ZDeckType.values()),
	STANDARD("Standard Zombie Invasion", ZDeckType.BLACK_PLAGUE, ZDeckType.WOLFSBURF),
	NECROMANCER("Necromancer!", ZDeckType.BLACK_PLAGUE, ZDeckType.WOLFSBURF),
	EXTRA_ACTIVATION("Extra Activation!", ZDeckType.BLACK_PLAGUE, ZDeckType.WOLFSBURF),
	DOUBLE_SPAWN("Double Spawn!", ZDeckType.BLACK_PLAGUE, ZDeckType.WOLFSBURF),
	WOLFSBURG("Zombie Wolfz Invasion", ZDeckType.WOLFSBURF),
	GREEN_HOARD("Orc Invasion", ZDeckType.GREEN_HOARD),
	GREEN_HOARD_ASSEMBLE(
		"Assemble the Hoard",
		ZDeckType.GREEN_HOARD
	), // place an extra zombie in the hoard zone
	GREEN_HOARD_ENTER(
		"Enter the Hoard!",
		ZDeckType.GREEN_HOARD
	), // deploy all the assembled zombies in target zone (not for Ahhhh)
	NECRO_DRAGON(
		"Necromantic Dragon!",
		ZDeckType.BLACK_PLAGUE,
		ZDeckType.WOLFSBURF,
		ZDeckType.GREEN_HOARD
	) {
		override fun isEnabled(rules: ZRules): Boolean = rules.necromanticDragon
	},
	SPECTRAL(
		"Spectral Walker Invasion",
		ZDeckType.BLACK_PLAGUE,
		ZDeckType.WOLFSBURF,
		ZDeckType.GREEN_HOARD
	) {
		override fun isEnabled(rules: ZRules): Boolean = rules.spectralWalkers
	},
	RATKING(
		"Enter the Rat King!",
		ZDeckType.BLACK_PLAGUE,
		ZDeckType.WOLFSBURF,
		ZDeckType.GREEN_HOARD
	) {
		override fun isEnabled(rules: ZRules): Boolean = rules.ratKing
	},
	SWAMPTROLL("The Swamp Troll emerges", ZDeckType.GREEN_HOARD) {
		override fun isEnabled(rules: ZRules): Boolean = rules.swampTroll
	},
	RAT_SWARMS("Ratz Swarm", ZDeckType.BLACK_PLAGUE, ZDeckType.WOLFSBURF, ZDeckType.GREEN_HOARD) {
		override fun isEnabled(rules: ZRules): Boolean = rules.ratSwarms
	},
	MURDER_CROWS("Crows!", ZDeckType.BLACK_PLAGUE, ZDeckType.WOLFSBURF, ZDeckType.GREEN_HOARD) {
		override fun isEnabled(rules: ZRules): Boolean = rules.murderOfCrowz
	},
	TOXIC_ORCS("Toxic Orcs", ZDeckType.GREEN_HOARD) {
		override fun isEnabled(rules: ZRules): Boolean = rules.toxicOrcs
	}
	;

	val decks: Array<ZDeckType>

	init {
		this.decks = arrayOf(*_decks)
	}

	open fun isEnabled(rules: ZRules): Boolean = true

}