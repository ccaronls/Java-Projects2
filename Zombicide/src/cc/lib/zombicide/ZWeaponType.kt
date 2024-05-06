package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.utils.Table
import cc.lib.utils.appendedWith
import cc.lib.utils.prettify
import java.util.Arrays

@Keep
enum class ZWeaponType(
	val weaponClass: ZWeaponClass,
	override val equipmentClass: ZEquipmentClass,
	val minColorToEquip: ZColor,
	val needsReload: Boolean,
	val canTwoHand: Boolean,
	val attackIsNoisy: Boolean,
	val openDoorsIsNoisy: Boolean,
	val weaponStats: Array<ZWeaponStat>,
	val specialInfo: String?
) : ZEquipmentType {
	// DAGGER get extra die roll when 2 handed with another melee weapon
	// BLUE WEAPONS
	// MELEE
	DAGGER(
		ZWeaponClass.NORMAL, ZEquipmentClass.DAGGER, ZColor.BLUE, false, true, false, true, arrayOf(
			ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 4, 0, 0, 1, 4, 1),
			ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_THROW, 0, 0, 1, 1, 3, 2)
		),
		"Gain +1 die with another equipped melee weapon"
	) {
		override val skillsWhenUsed: List<ZSkill>
			get() = listOf(ZSkill.Plus1_die_Melee_Weapon)
	},
	HEAVY_DAGGER(
		ZWeaponClass.NORMAL, ZEquipmentClass.DAGGER, ZColor.BLUE, false, true, false, true, arrayOf(
			ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 4, 0, 0, 1, 4, 2)
		),
		"Gain +1 die with another equipped melee weapon"
	) {
		override val skillsWhenUsed: List<ZSkill>
			get() = listOf(ZSkill.Plus1_die_Melee_Weapon)
	},
	AXE(
		ZWeaponClass.NORMAL, ZEquipmentClass.AXE, ZColor.BLUE, false, true, false, true, arrayOf(
			ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 1, 0, 0, 1, 4, 1)
		), null
	),
	HAMMER(
		ZWeaponClass.NORMAL, ZEquipmentClass.AXE, ZColor.BLUE, false, false, false, true, arrayOf(
			ZWeaponStat(ZActionType.MELEE, ZAttackType.CRUSH, 4, 0, 0, 1, 3, 2)
		), null
	),
	SHORT_SWORD(
		ZWeaponClass.NORMAL, ZEquipmentClass.SWORD, ZColor.BLUE, false, true, false, true, arrayOf(
			ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 3, 0, 0, 1, 4, 1)
		), null
	),
	SWORD(
		ZWeaponClass.NORMAL, ZEquipmentClass.SWORD, ZColor.BLUE, false, true, false, true, arrayOf(
			ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 3, 0, 0, 2, 4, 1)
		), null
	),
	HEAVY_SWORD(
		ZWeaponClass.NORMAL, ZEquipmentClass.SWORD, ZColor.BLUE, false, true, false, true, arrayOf(
			ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 3, 0, 0, 2, 4, 2)
		), null
	),
	GREAT_SWORD(
		ZWeaponClass.NORMAL, ZEquipmentClass.SWORD, ZColor.BLUE, false, false, false, true, arrayOf(
			ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 4, 0, 0, 5, 5, 1)
		), null
	),  // BOWS
	SHORT_BOW(
		ZWeaponClass.NORMAL, ZEquipmentClass.BOW, ZColor.BLUE, false, false, false, false, arrayOf(
			ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_ARROWS, 0, 0, 1, 1, 3, 1)
		), null
	),
	LONG_BOW(
		ZWeaponClass.NORMAL, ZEquipmentClass.BOW, ZColor.BLUE, false, false, false, false, arrayOf(
			ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_ARROWS, 0, 1, 3, 1, 3, 1)
		), null
	),  // CROSSBOWS
	CROSSBOW(
		ZWeaponClass.NORMAL,
		ZEquipmentClass.CROSSBOW,
		ZColor.BLUE,
		true,
		false,
		false,
		false,
		arrayOf(
			ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_BOLTS, 0, 1, 2, 2, 4, 2)
		),
		null
	),
	REPEATING_CROSSBOW(
		ZWeaponClass.NORMAL,
		ZEquipmentClass.CROSSBOW,
		ZColor.BLUE,
		true,
		true,
		false,
		false,
		arrayOf(
			ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_BOLTS, 0, 0, 1, 3, 5, 1)
		),
		null
	),
	HAND_CROSSBOW(
		ZWeaponClass.NORMAL,
		ZEquipmentClass.CROSSBOW,
		ZColor.BLUE,
		true,
		true,
		false,
		false,
		arrayOf(
			ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_BOLTS, 0, 0, 3, 2, 3, 1)
		),
		"Auto reload at end of turn"
	),
	ORCISH_CROSSBOW(
		ZWeaponClass.VAULT,
		ZEquipmentClass.CROSSBOW,
		ZColor.BLUE,
		true,
		false,
		false,
		false,
		arrayOf(
			ZWeaponStat(ZActionType.MELEE, ZAttackType.CRUSH, 0, 0, 0, 2, 3, 2),
			ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_BOLTS, 0, 1, 2, 2, 3, 2)
		),
		null
	),
	ORCISH_BOW(
		ZWeaponClass.VAULT, ZEquipmentClass.BOW, ZColor.BLUE, false, false, false, false, arrayOf(
			ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 0, 0, 0, 2, 3, 1),
			ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_ARROWS, 0, 1, 3, 2, 3, 1)
		), null
	),

	HEAVY_CROSSBOW(
		ZWeaponClass.NORMAL,
		ZEquipmentClass.CROSSBOW,
		ZColor.BLUE,
		true,
		false,
		false,
		false,
		arrayOf(
			ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_BOLTS, 0, 1, 2, 2, 4, 3)
		),
		null
	),  // MAGIC
	DEATH_STRIKE(
		ZWeaponClass.NORMAL, ZEquipmentClass.MAGIC, ZColor.BLUE, false, true, true, false, arrayOf(
			ZWeaponStat(ZActionType.MAGIC, ZAttackType.MENTAL_STRIKE, 0, 0, 1, 1, 4, 2)
		), null
	),
	DISINTEGRATE(
		ZWeaponClass.NORMAL, ZEquipmentClass.MAGIC, ZColor.BLUE, false, true, true, false, arrayOf(
			ZWeaponStat(ZActionType.MAGIC, ZAttackType.DISINTEGRATION, 0, 0, 1, 3, 5, 2)
		), null
	) {
		override val skillsWhenUsed: List<ZSkill>
			get() = listOf(ZSkill.Roll_6_Plus1_Damage)
	},
	EARTHQUAKE(
		ZWeaponClass.NORMAL, ZEquipmentClass.MAGIC, ZColor.BLUE, false, true, true, false, arrayOf(
			ZWeaponStat(ZActionType.MAGIC, ZAttackType.EARTHQUAKE, 0, 0, 1, 3, 4, 1)
		), null
	),
	FIREBALL(
		ZWeaponClass.NORMAL, ZEquipmentClass.MAGIC, ZColor.BLUE, false, true, true, false, arrayOf(
			ZWeaponStat(ZActionType.MAGIC, ZAttackType.FIRE, 0, 0, 1, 3, 4, 1)
		), null
	),
	MANA_BLAST(
		ZWeaponClass.NORMAL, ZEquipmentClass.MAGIC, ZColor.BLUE, false, true, true, false, arrayOf(
			ZWeaponStat(ZActionType.MAGIC, ZAttackType.MENTAL_STRIKE, 0, 0, 2, 1, 4, 1)
		), null
	),
	INFERNO(
		ZWeaponClass.VAULT, ZEquipmentClass.MAGIC, ZColor.BLUE, false, false, true, false, arrayOf(
			ZWeaponStat(ZActionType.MAGIC, ZAttackType.FIRE, 0, 0, 1, 4, 4, 2)
		), null
	),
	LIGHTNING_BOLT(
		ZWeaponClass.NORMAL, ZEquipmentClass.MAGIC, ZColor.BLUE, false, true, true, false, arrayOf(
			ZWeaponStat(ZActionType.MAGIC, ZAttackType.ELECTROCUTION, 0, 0, 3, 1, 4, 1)
		), null
	),  // WOLFBERG
	BASTARD_SWORD(
		ZWeaponClass.NORMAL, ZEquipmentClass.SWORD, ZColor.BLUE, false, false, false, true, arrayOf(
			ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 4, 0, 0, 2, 4, 2)
		), null
	),  // YELLOW
	DEFLECTING_DAGGER(
		ZWeaponClass.VAULT,
		ZEquipmentClass.DAGGER,
		ZColor.YELLOW,
		false,
		true,
		false,
		true,
		arrayOf(
			ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 4, 0, 0, 1, 4, 1)
		),
		"Gain +1 die from another equipped melee weapon. Acts as a shield with 4+ armor roll"
	) {
		override val skillsWhenUsed: List<ZSkill>
			get() = listOf(ZSkill.Plus1_die_Melee_Weapon)
		override val isShield: Boolean
			get() = true

		override fun getDieRollToBlock(type: ZZombieType): Int {
			return if (type.ignoresArmor) 0 else 4
		}
	},
	FLAMING_GREAT_SWORD(
		ZWeaponClass.VAULT, ZEquipmentClass.SWORD, ZColor.YELLOW, false, false, false, true,
		arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 5, 0, 0, 5, 5, 2)),
		"Can ignite Dragon Fire at range 0-1"
	) {
		override val skillsWhileEquipped: List<ZSkill>
			get() = listOf(ZSkill.Ignite_Dragon_Fire)
	},
	VAMPIRE_CROSSBOW(
		ZWeaponClass.VAULT, ZEquipmentClass.CROSSBOW, ZColor.YELLOW, true, false, false, false,
		arrayOf(ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_BOLTS, 0, 1, 2, 2, 4, 3)),
		"Heal 1 wound each time you kill a zombie."
	) {
		override val skillsWhenUsed: List<ZSkill>
			get() = listOf(ZSkill.Hit_Heals)
	},  // ORANGE
	EARTHQUAKE_HAMMER(
		ZWeaponClass.VAULT, ZEquipmentClass.AXE, ZColor.ORANGE, false, false, false, true,
		arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.EARTHQUAKE, 3, 0, 0, 3, 3, 2)),
		"Roll 6: +1 die and +1 damage"
	) {
		override val skillsWhenUsed: List<ZSkill>
			get() = listOf(ZSkill.Roll_6_Plus1_Damage, ZSkill.Roll_6_plus1_die_Melee)
	},
	AXE_OF_CARNAGE(
		ZWeaponClass.VAULT, ZEquipmentClass.AXE, ZColor.ORANGE, false, false, false, true,
		arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 1, 0, 0, 4, 4, 2)),
		"Add an additional success with each melee action resolved."
	) {
		override val skillsWhenUsed: List<ZSkill>
			get() = listOf(ZSkill.Two_For_One_Melee)
	},
	DRAGON_FIRE_BLADE(
		ZWeaponClass.VAULT, ZEquipmentClass.SWORD, ZColor.ORANGE, false, false, false, true,
		arrayOf(
			ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 3, 0, 0, 2, 3, 2),
			ZWeaponStat(ZActionType.THROW_ITEM, ZAttackType.DRAGON_FIRE, 0, 0, 1, 1, 1, 3)
		),
		"Throw (Discard) at range 0-1 to create a dragon fire."
	) {
		override fun onThrown(game: ZGame, thrower: ZCharacter, targetZoneIdx: Int) {
			game.onEquipmentThrown(thrower.type, ZIcon.SWORD, targetZoneIdx)
			game.performDragonFire(thrower, targetZoneIdx)
		}
	},
	CHAOS_LONGBOW(
		ZWeaponClass.VAULT, ZEquipmentClass.BOW, ZColor.ORANGE, false, false, false, false,
		arrayOf(ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_ARROWS, 0, 0, 3, 4, 4, 2)),
		"4 or more hits on a ranged action causes dragon fire in the targeted zone."
	) {
		override val skillsWhenUsed: List<ZSkill>
			get() = listOf(ZSkill.Hit_4_Dragon_Fire)
	},
	MJOLNIR(
		ZWeaponClass.SPECIAL, ZEquipmentClass.AXE, ZColor.RED, false, false, true, true,
		arrayOf(
			ZWeaponStat(ZActionType.MELEE, ZAttackType.CRUSH, 1, 0, 0, 2, 4, 3),
			ZWeaponStat(ZActionType.MAGIC, ZAttackType.ELECTROCUTION, 0, 1, 4, 4, 5, 1),
			ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_THROW, 0, 1, 2, 4, 5, 2)
		),
		"Returns to the survivor after being thrown. Survivor gains Hand of God Skill."
	) {
		override val skillsWhileEquipped: List<ZSkill>
			get() = listOf(ZSkill.Hand_of_God)
	},
	SWORD_OF_LIFE(
		ZWeaponClass.VAULT, ZEquipmentClass.SWORD, ZColor.ORANGE, false, false, false, true,
		arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 4, 0, 0, 3, 4, 2)),
		"Rolling a six heals a point"
	) {
		override val skillsWhenUsed: List<ZSkill>
			get() = listOf(ZSkill.Roll_6_Hit_Heals)
	},
	WAR_CLEAVER(
		ZWeaponClass.VAULT, ZEquipmentClass.AXE, ZColor.ORANGE, false, false, false, true,
		arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.CRUSH, 1, 0, 0, 4, 4, 2)),
		"Roll 6 +1 Die Melee"
	) {
		override val skillsWhenUsed: List<ZSkill>
			get() = listOf(ZSkill.Roll_6_plus1_die_Melee)
	},
	TELEKINETIC_BLAST(
		ZWeaponClass.VAULT, ZEquipmentClass.MAGIC, ZColor.BLUE, false, true, true, true,
		arrayOf(ZWeaponStat(ZActionType.MAGIC, ZAttackType.MENTAL_STRIKE, 1, 0, 1, 1, 4, 1)), null
	),
	KINGS_MACE(
		ZWeaponClass.VAULT, ZEquipmentClass.AXE, ZColor.BLUE, false, false, false, false,
		arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.CRUSH, 0, 0, 0, 1, 4, 2)),
		"Get +1 Die for each survivor in vicinity"
	) {
		override val skillsWhenUsed: List<ZSkill>
			get() = listOf(ZSkill.Kings_Mace)
	},
	QUICKSILVER_SWORD(
		ZWeaponClass.VAULT, ZEquipmentClass.SWORD, ZColor.YELLOW, false, false, false, true,
		arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 4, 0, 0, 4, 4, 1)),
		"Gain the Lucky Skill"
	) {
		override val skillsWhileEquipped: List<ZSkill>
			get() = listOf(ZSkill.Lucky)
	},
	STORM_BOW(
		ZWeaponClass.VAULT, ZEquipmentClass.BOW, ZColor.YELLOW, false, false, false, false,
		arrayOf(ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_ARROWS, 0, 0, 3, 1, 3, 1)),
		"Roll 6 +1 Die"
	) {
		override val skillsWhenUsed: List<ZSkill>
			get() = listOf(ZSkill.Roll_6_plus1_die_Ranged)
	},
	CHAIN_LIGHTNING(
		ZWeaponClass.VAULT, ZEquipmentClass.MAGIC, ZColor.BLUE, false, true, true, false,
		arrayOf(ZWeaponStat(ZActionType.MAGIC, ZAttackType.ELECTROCUTION, 0, 0, 1, 3, 5, 1)),
		"Roll 6 +1 Die"
	) {
		override val skillsWhenUsed: List<ZSkill>
			get() = listOf(ZSkill.Roll_6_plus1_die_Magic)
	},
	ICE_BLAST(
		ZWeaponClass.VAULT, ZEquipmentClass.MAGIC, ZColor.BLUE, false, true, false, false,
		arrayOf(ZWeaponStat(ZActionType.MAGIC, ZAttackType.FREEZE, 0, 0, 2, 3, 4, 1)),
		"Roll 6 will freeze zombies in zone for 1 turn"
	) {
		override val skillsWhenUsed: List<ZSkill>
			get() = listOf(ZSkill.Roll_6_Freeze)
	},
	SCATTERSHOT(
		ZWeaponClass.SIEGE_ENGINE,
		ZEquipmentClass.THROWABLE,
		ZColor.BLUE,
		false,
		false,
		true,
		false,
		arrayOf(
			ZWeaponStat(
				ZActionType.CATAPULT_FIRE,
				ZAttackType.CRUSH,
				0,
				2,
				Int.MAX_VALUE,
				6,
				4,
				1
			)
		),
		null
	) {
		override val skillsWhenUsed: List<ZSkill>
			get() = listOf(ZSkill.Plus1_to_die_roll_observed)
	},
	GRAPESHOT(
		ZWeaponClass.SIEGE_ENGINE,
		ZEquipmentClass.THROWABLE,
		ZColor.BLUE,
		false,
		false,
		true,
		false,
		arrayOf(
			ZWeaponStat(
				ZActionType.CATAPULT_FIRE,
				ZAttackType.CRUSH,
				0,
				2,
				Int.MAX_VALUE,
				3,
				4,
				2
			)
		),
		null
	) {
		override val skillsWhenUsed: List<ZSkill>
			get() = listOf(ZSkill.Plus1_to_die_roll_observed)
	},
	BOULDER(
		ZWeaponClass.SIEGE_ENGINE,
		ZEquipmentClass.THROWABLE,
		ZColor.BLUE,
		false,
		false,
		true,
		false,
		arrayOf(
			ZWeaponStat(
				ZActionType.CATAPULT_FIRE,
				ZAttackType.CRUSH,
				0,
				2,
				Int.MAX_VALUE,
				1,
				4,
				3
			)
		),
		null
	) {
		override val skillsWhenUsed: List<ZSkill>
			get() = listOf(ZSkill.Plus1_to_die_roll_observed)
	},
	BOLT(
		ZWeaponClass.SIEGE_ENGINE,
		ZEquipmentClass.THROWABLE,
		ZColor.BLUE,
		false,
		false,
		false,
		false, // true?
		arrayOf(
			ZWeaponStat(
				ZActionType.BALLISTA_FIRE,
				ZAttackType.RANGED_BOLTS,
				0,
				1,
				Int.MAX_VALUE,
				3,
				4
			)
		),
		null
	) {
		override val skillsWhenUsed: List<ZSkill>
			get() = listOf(ZSkill.Plus1_to_die_roll_observed)
	},
	FAMILIAR_HOUND(
		ZWeaponClass.NORMAL, ZEquipmentClass.FAMILIAR, ZColor.BLUE, false, false, true, false,
		arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.FAMILIAR, 0, 0, 0, 3, 4, 1)),
		null
	) {
		override val skillsWhileEquipped: List<ZSkill>
			get() = listOf(ZSkill.Plus1_die_Melee)
	},
	FAMILIAR_FLYING_CAT(
		ZWeaponClass.NORMAL, ZEquipmentClass.FAMILIAR, ZColor.BLUE, false, false, true, false,
		arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.FAMILIAR, 0, 0, 0, 2, 3, 1)),
		null
	) {
		override val skillsWhileEquipped: List<ZSkill>
			get() = listOf(ZSkill.Plus1_max_Range, ZSkill.Birds_eye_view)
	},
	FAMILIAR_WOLF(
		ZWeaponClass.NORMAL, ZEquipmentClass.FAMILIAR, ZColor.BLUE, false, false, true, false,
		arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.FAMILIAR, 0, 0, 0, 1, 3, 2)),
		null
	) {
		override val skillsWhileEquipped: List<ZSkill>
			get() = listOf(ZSkill.Plus2_die_Melee)
	},

	;

	init {
		stats.map { it.actionType }.toSet().forEach { type ->
			if (stats.count { it.actionType == type } > 1)
				error("Multiple $type for $name is not allowed")
		}
	}

	val stats: List<ZWeaponStat>
		get() = listOf(*weaponStats)


	override fun getInfoTable(): Table? = getStatsTable(null)

	fun getStatsTable(isEmpty: Boolean?): Table {
		val cardLower = Table().setNoBorder()
		cardLower.addColumnNoHeader(
			Arrays.asList(
				"Attack Type",
				"Dual Wield",
				"Damage",
				"Hit %",
				"Range",
				"Doors",
				"Reloads"
			)
		)
		for (stats in stats) {
			var doorInfo: String = if (stats.dieRollToOpenDoor > 0) {
				String.format(
					"%s %d%%",
					if (openDoorsIsNoisy) "noisy" else "quiet",
					stats.dieRollToOpenDoorPercent
				)
			} else {
				"no"
			}
			cardLower.addColumnNoHeader(
				Arrays.asList(
					stats.attackType.name.prettify(),
					if (canTwoHand) "yes" else "no",
					String.format(
						"%d %s",
						stats.damagePerHit,
						if (attackIsNoisy) " loud" else " quiet"
					),
					String.format("%d%% x %d", stats.dieRollToHitPercent, stats.numDice),
					if (stats.minRange == stats.maxRange) stats.minRange.toString() else String.format(
						"%d-%d",
						stats.minRange,
						stats.maxRange
					),
					doorInfo,
					if (needsReload) (isEmpty?.let {
						String.format("yes (%s)", if (isEmpty) "empty" else "loaded")
					} ?: "yes") else "no"
				))
		}
		if (minColorToEquip.ordinal > 0) {
			cardLower.addRow(minColorToEquip.toString() + " Required")
		}
		val skills = skillsWhileEquipped.appendedWith(skillsWhenUsed)
		for (skill in skills) {
			cardLower.addRow(skill.getLabel())
		}
		return cardLower
	}

	override fun create(): ZWeapon {
		return ZWeapon(this)
	}

	val isFire: Boolean
		get() = when (this) {
			INFERNO, FIREBALL -> true
			else -> false
		}

    override fun isActionType(type: ZActionType): Boolean {
        return weaponStats.count { stat: ZWeaponStat -> stat.actionType === type } > 0
    }
}