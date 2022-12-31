package cc.lib.zombicide

import cc.lib.annotation.Keep

@Keep
enum class ZWeaponType(override val equipmentClass: ZEquipmentClass, val minColorToEquip: ZColor, val needsReload: Boolean, val canTwoHand: Boolean, val attackIsNoisy: Boolean, val openDoorsIsNoisy: Boolean, val weaponStats: Array<ZWeaponStat>, val specialInfo: String?) : ZEquipmentType {
    // DAGGER get extra die roll when 2 handed with another melee weapon
    // BLUE WEAPONS
    // MELEE
    DAGGER(ZEquipmentClass.DAGGER, ZColor.BLUE, false, true, false, true, arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 4, 0, 0, 1, 4, 1), ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_THROW, 0, 0, 1, 1, 3, 2)), "Gain +1 die with another equipped melee weapon") {
        override val skillsWhenUsed: List<ZSkill>
            get() = listOf(ZSkill.Plus1_die_Melee_Weapon)
    },
    AXE(ZEquipmentClass.AXE, ZColor.BLUE, false, true, false, true, arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 1, 0, 0, 1, 4, 1)), null),
    HAMMER(ZEquipmentClass.AXE, ZColor.BLUE, false, false, false, true, arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.CRUSH, 4, 0, 0, 1, 3, 2)), null),
    SHORT_SWORD(ZEquipmentClass.SWORD, ZColor.BLUE, false, true, false, true, arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 3, 0, 0, 1, 4, 1)), null),
    SWORD(ZEquipmentClass.SWORD, ZColor.BLUE, false, true, false, true, arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 3, 0, 0, 2, 4, 1)), null),
    GREAT_SWORD(ZEquipmentClass.SWORD, ZColor.BLUE, false, false, false, true, arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 4, 0, 0, 5, 5, 1)), null),  // BOWS
    SHORT_BOW(ZEquipmentClass.BOW, ZColor.BLUE, false, false, false, false, arrayOf(ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_ARROWS, 0, 0, 1, 1, 3, 1)), null),
    LONG_BOW(ZEquipmentClass.BOW, ZColor.BLUE, false, false, false, false, arrayOf(ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_ARROWS, 0, 1, 3, 1, 3, 1)), null),  // CROSSBOWS
    CROSSBOW(ZEquipmentClass.CROSSBOW, ZColor.BLUE, true, false, false, false, arrayOf(ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_BOLTS, 0, 1, 2, 2, 4, 2)), null),
    REPEATING_CROSSBOW(ZEquipmentClass.CROSSBOW, ZColor.BLUE, true, true, false, false, arrayOf(ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_BOLTS, 0, 0, 1, 3, 5, 1)), null),
    HAND_CROSSBOW(ZEquipmentClass.CROSSBOW, ZColor.BLUE, true, true, false, false, arrayOf(ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_BOLTS, 0, 0, 3, 2, 3, 1)), "Auto reload at end of turn"),
    ORCISH_CROSSBOW(ZEquipmentClass.CROSSBOW, ZColor.BLUE, true, false, false, false, arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.CRUSH, 0, 0, 0, 2, 3, 2), ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_BOLTS, 0, 1, 2, 2, 3, 2)), null),
    HEAVY_CROSSBOW(ZEquipmentClass.CROSSBOW, ZColor.BLUE, true, false, false, false, arrayOf(ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_BOLTS, 0, 1, 2, 2, 4, 3)), null),  // MAGIC
    DEATH_STRIKE(ZEquipmentClass.MAGIC, ZColor.BLUE, false, true, true, false, arrayOf(ZWeaponStat(ZActionType.MAGIC, ZAttackType.MENTAL_STRIKE, 0, 0, 1, 1, 4, 2)), null),  // TODO: +1 damage on a die roll 6
    DISINTEGRATE(ZEquipmentClass.MAGIC, ZColor.BLUE, false, true, true, false, arrayOf(ZWeaponStat(ZActionType.MAGIC, ZAttackType.DISINTEGRATION, 0, 0, 1, 3, 5, 2)), null),
    EARTHQUAKE(ZEquipmentClass.MAGIC, ZColor.BLUE, false, true, true, false, arrayOf(ZWeaponStat(ZActionType.MAGIC, ZAttackType.EARTHQUAKE, 0, 0, 1, 3, 4, 1)), null),
    FIREBALL(ZEquipmentClass.MAGIC, ZColor.BLUE, false, true, true, false, arrayOf(ZWeaponStat(ZActionType.MAGIC, ZAttackType.FIRE, 0, 0, 1, 3, 4, 1)), null),
    MANA_BLAST(ZEquipmentClass.MAGIC, ZColor.BLUE, false, true, true, false, arrayOf(ZWeaponStat(ZActionType.MAGIC, ZAttackType.MENTAL_STRIKE, 0, 0, 2, 1, 4, 1)), null),
    INFERNO(ZEquipmentClass.MAGIC, ZColor.BLUE, false, false, true, false, arrayOf(ZWeaponStat(ZActionType.MAGIC, ZAttackType.FIRE, 0, 0, 1, 4, 4, 2)), null),
    LIGHTNING_BOLT(ZEquipmentClass.MAGIC, ZColor.BLUE, false, true, true, false, arrayOf(ZWeaponStat(ZActionType.MAGIC, ZAttackType.ELECTROCUTION, 0, 0, 3, 1, 4, 1)), null),  // WOLFBERG
    BASTARD_SWORD(ZEquipmentClass.SWORD, ZColor.BLUE, false, false, false, true, arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 4, 0, 0, 2, 4, 2)), null),  // YELLOW
    DEFLECTING_DAGGER(ZEquipmentClass.DAGGER, ZColor.YELLOW, false, true, false, true, arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 4, 0, 0, 1, 4, 1)), "Gain +1 die from another equipped melee weapon. Acts as a shield with 4+ armor roll") {
        override val skillsWhenUsed: List<ZSkill>
            get() = listOf(ZSkill.Plus1_die_Melee_Weapon)
        override val isShield: Boolean
            get() = true
        override fun getDieRollToBlock(type: ZZombieType): Int {
            return if (type.ignoresArmor) 0 else 4
        }
    },
    FLAMING_GREAT_SWORD(ZEquipmentClass.SWORD, ZColor.YELLOW, false, false, false, true, arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 5, 0, 0, 5, 5, 2)), "Can ignite Dragon Fire at range 0-1") {
        override val skillsWhileEquipped: List<ZSkill>
            get() = listOf(ZSkill.Ignite_Dragon_Fire)
    },
    VAMPIRE_CROSSBOW(ZEquipmentClass.CROSSBOW, ZColor.YELLOW, true, false, false, false, arrayOf(ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_BOLTS, 0, 1, 2, 2, 4, 3)), "Heal 1 wound each time you kill a zombie.") {
        override val skillsWhenUsed: List<ZSkill>
            get() = listOf(ZSkill.Hit_Heals)
    },  // ORANGE
    EARTHQUAKE_HAMMER(ZEquipmentClass.AXE, ZColor.ORANGE, false, false, false, true, arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.EARTHQUAKE, 3, 0, 0, 3, 3, 2)), "Roll 6: +1 die and +1 damage") {
        override val skillsWhenUsed: List<ZSkill>
            get() = listOf(ZSkill.Roll_6_Plus1_Damage, ZSkill.Roll_6_plus1_die_Melee)
    },
    AXE_OF_CARNAGE(ZEquipmentClass.AXE, ZColor.ORANGE, false, false, false, true, arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 1, 0, 0, 4, 4, 2)), "Add an additional success with each melee action resolved.") {
        override val skillsWhenUsed: List<ZSkill>
            get() = listOf(ZSkill.Two_For_One_Melee)
    },
    DRAGON_FIRE_BLADE(ZEquipmentClass.SWORD, ZColor.ORANGE, false, false, false, true, arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 3, 0, 0, 2, 3, 2), ZWeaponStat(ZActionType.THROW_ITEM, ZAttackType.DRAGON_FIRE, 0, 0, 1, 1, 1, 3)), "Throw (Discard) at range 0-1 to create a dragon fire.") {
        override fun onThrown(game: ZGame, thrower: ZCharacter, targetZoneIdx: Int) {
            game.onEquipmentThrown(thrower.type, ZIcon.SWORD, targetZoneIdx)
            game.performDragonFire(thrower, targetZoneIdx)
        }
    },
    CHAOS_LONGBOW(ZEquipmentClass.BOW, ZColor.ORANGE, false, false, false, false, arrayOf(ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_ARROWS, 0, 0, 3, 4, 4, 2)), "4 or more hits on a ranged action causes dragon fire in the targeted zone.") {
        override val skillsWhenUsed: List<ZSkill>
            get() = listOf(ZSkill.Hit_4_Dragon_Fire)
    },
    MJOLNIR(ZEquipmentClass.AXE, ZColor.RED, false, false, true, true,
	    arrayOf(ZWeaponStat(ZActionType.MELEE, ZAttackType.CRUSH, 1, 0, 0, 2, 4, 3),
		        ZWeaponStat(ZActionType.MAGIC, ZAttackType.ELECTROCUTION, 0, 1, 4, 4, 5, 1),
	            ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_THROW, 0, 1, 2, 4, 5, 2)), "Returns to the survivor after being thrown. Survivor gains Hand of God Skill.") {
	    override val skillsWhileEquipped : List<ZSkill>
		    get() = listOf(ZSkill.Hand_of_God)
    }

    ;

    val stats: List<ZWeaponStat>
        get() = listOf(*weaponStats)

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