package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.game.GDimension
import cc.lib.game.IDimension
import cc.lib.ui.IButton

@Keep
enum class ZPlayerName(val characterClass: String, val startingEquipment: Array<ZEquipmentType>, val alternateBodySlot: ZEquipmentClass, blueSkillOptions: Array<ZSkill>, yellowSkillOptions: Array<ZSkill>, orangeSkillOptions: Array<ZSkill>, redSkillOptions: Array<ZSkill>) : IButton, IDimension {
	Ann("Angry Nun",
		arrayOf(ZWeaponType.DAGGER, ZWeaponType.HAND_CROSSBOW),
		ZEquipmentClass.DAGGER,
		arrayOf(ZSkill.Bloodlust),
		arrayOf(ZSkill.Plus1_Action),
		arrayOf(ZSkill.Plus1_free_Magic_Action, ZSkill.Plus1_free_Melee_Action),
		arrayOf(ZSkill.Plus1_free_Combat_Action, ZSkill.Plus1_to_dice_roll_Combat, ZSkill.Roll_6_plus1_die_Combat)),
	Baldric("Magician",
		arrayOf(ZWeaponType.MANA_BLAST),
		ZEquipmentClass.SWORD,
            arrayOf(ZSkill.Spellcaster),
            arrayOf(ZSkill.Plus1_Action),
            arrayOf(ZSkill.Plus1_free_Magic_Action, ZSkill.Spellbook),
            arrayOf(ZSkill.Plus1_die_Magic, ZSkill.Plus1_free_Combat_Action, ZSkill.Plus1_to_dice_roll_Melee)),
    Clovis("Warrior",
            arrayOf(ZWeaponType.SHORT_SWORD, ZWeaponType.CROSSBOW),
            ZEquipmentClass.SHIELD,
            arrayOf(ZSkill.Plus1_die_Melee, ZSkill.Marksman),
            arrayOf(ZSkill.Plus1_Action),
            arrayOf(ZSkill.Plus1_free_Melee_Action, ZSkill.Swordmaster),
            arrayOf(ZSkill.Plus1_free_Combat_Action, ZSkill.Plus1_to_dice_roll_Combat, ZSkill.Hit_and_run)),
    Samson("Dwarf",
            arrayOf(ZWeaponType.AXE, ZWeaponType.HAMMER),
            ZEquipmentClass.THROWABLE,
            arrayOf(ZSkill.Iron_hide),
            arrayOf(ZSkill.Plus1_Action),
            arrayOf(ZSkill.Plus1_die_Combat, ZSkill.Plus1_to_dice_roll_Melee),
            arrayOf(ZSkill.Plus1_Damage_Melee, ZSkill.Plus1_free_Combat_Action, ZSkill.Barbarian)),
    Nelly("Rogue",
            arrayOf(ZWeaponType.SHORT_SWORD, ZWeaponType.SHORT_BOW),
            ZEquipmentClass.BOW,
            arrayOf(ZSkill.Plus1_free_Move_Action),
            arrayOf(ZSkill.Plus1_Action),
            arrayOf(ZSkill.Bloodlust_Melee, ZSkill.Slippery),
            arrayOf(ZSkill.Plus1_free_Combat_Action, ZSkill.Plus1_to_dice_roll_Combat, ZSkill.Hit_and_run)),
    Silas("Elven Ranger",
            arrayOf(ZWeaponType.SHORT_SWORD, ZWeaponType.SHORT_BOW),
            ZEquipmentClass.SWORD,
            arrayOf(ZSkill.Plus1_to_dice_roll_Ranged),
            arrayOf(ZSkill.Plus1_Action),
            arrayOf(ZSkill.Plus1_free_Ranged_Action, ZSkill.Point_blank),
            arrayOf(ZSkill.Plus1_die_Ranged, ZSkill.Plus1_free_Combat_Action, ZSkill.Iron_rain)),
    Tucker("Monk",
	    arrayOf(ZWeaponType.CROSSBOW, ZWeaponType.BASTARD_SWORD),
	    ZEquipmentClass.AXE,
	    arrayOf(ZSkill.Shove),
	    arrayOf(ZSkill.Plus1_Action),
	    arrayOf(ZSkill.Lucky, ZSkill.Spellcaster),
	    arrayOf(ZSkill.Plus1_die_Combat, ZSkill.Plus1_free_Combat_Action, ZSkill.Free_reload)),
	Jain("Valkyrie",
		arrayOf(ZWeaponType.LONG_BOW),
		ZEquipmentClass.THROWABLE,
		arrayOf(ZSkill.Steady_hand),
		arrayOf(ZSkill.Plus1_Action),
		arrayOf(ZSkill.Plus1_die_Ranged, ZSkill.Plus1_max_Range),
		arrayOf(ZSkill.Plus1_Damage_Ranged, ZSkill.Point_blank, ZSkill.Sprint)),
	Benson("Elite Guard",
		arrayOf(ZWeaponType.SWORD),
		ZEquipmentClass.SWORD,
		arrayOf(ZSkill.Steel_hide),
            arrayOf(ZSkill.Plus1_Action),
            arrayOf(ZSkill.Plus1_free_Melee_Action, ZSkill.Born_leader),
            arrayOf(ZSkill.Plus1_Damage_Melee, ZSkill.Lucky, ZSkill.Shove)),  // Wulfsburg
    Theo("Ranger",
            arrayOf(ZWeaponType.DAGGER, ZWeaponType.SHORT_SWORD),  //, ZWeaponType.EARTHQUAKE_HAMMER),
            ZEquipmentClass.SWORD,
            arrayOf(ZSkill.Sprint),
            arrayOf(ZSkill.Plus1_Action),
            arrayOf(ZSkill.Plus1_die_Melee, ZSkill.Plus1_to_dice_roll_Ranged),
            arrayOf(ZSkill.Plus1_free_Combat_Action, ZSkill.Charge, ZSkill.Marksman)),
    Morrigan("Witcher",
            arrayOf(ZWeaponType.AXE),  //, ZWeaponType.CHAOS_LONGBOW),
            ZEquipmentClass.DAGGER,
            arrayOf(ZSkill.Reaper_Combat),
            arrayOf(ZSkill.Plus1_Action),
            arrayOf(ZSkill.Plus1_free_Melee_Action, ZSkill.Spellcaster),
            arrayOf(ZSkill.Plus1_Damage_Melee, ZSkill.Plus1_die_Magic, ZSkill.Bloodlust)),
    Karl("Monk",
            arrayOf(ZWeaponType.SWORD, ZWeaponType.MANA_BLAST),
            ZEquipmentClass.MAGIC,
            arrayOf(ZSkill.Plus1_die_Magic),
            arrayOf(ZSkill.Plus1_Action),
            arrayOf(ZSkill.Plus1_free_Combat_Action, ZSkill.Spellcaster),
            arrayOf(ZSkill.Plus1_free_Combat_Action, ZSkill.Plus1_max_Range, ZSkill.Marksman)),
    Ariane("Thief",
            arrayOf(ZWeaponType.CROSSBOW),
            ZEquipmentClass.CROSSBOW,
            arrayOf(ZSkill.Jump),
            arrayOf(ZSkill.Plus1_Action),
            arrayOf(ZSkill.Plus1_free_Move_Action, ZSkill.Plus1_free_Melee_Action),
            arrayOf(ZSkill.Plus1_die_Combat, ZSkill.Plus1_die_Ranged, ZSkill.Spellcaster));

    private val skillOptions: Array<Array<ZSkill>> = arrayOf(
            blueSkillOptions,
            yellowSkillOptions,
            orangeSkillOptions,
            redSkillOptions
    )

    var imageId = -1
    var cardImageId = -1
    var outlineImageId = -1
    var imageDim: GDimension = GDimension.EMPTY

    fun create(): ZCharacter = ZCharacter(this, skillOptions)

    override fun getTooltipText(): String {
        return characterClass
    }

    override fun getLabel(): String {
        return name
    }

    override fun getWidth(): Float {
        return imageDim.getWidth()
    }

    override fun getHeight(): Float {
        return imageDim.getHeight()
    }

    fun getSkillOptions(color: ZColor): Array<ZSkill> {
        return skillOptions[color.ordinal]
    }

}