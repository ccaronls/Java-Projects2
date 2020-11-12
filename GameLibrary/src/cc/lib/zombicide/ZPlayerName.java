package cc.lib.zombicide;

import java.util.Arrays;

import cc.lib.game.Utils;

public enum ZPlayerName {
    Ann("Angry Nun",
            Utils.toArray(ZWeapon.DAGGER),
            Utils.toArray(ZWeapon.DAGGER),
            Utils.toArray(ZSkill.Bloodlust),
            Utils.toArray(ZSkill.Plus1_Action),
            Utils.toArray(ZSkill.Plus1_free_Magic_Action, ZSkill.Plus1_free_Melee_Action),
            Utils.toArray(ZSkill.Plus1_free_Combat_Action, ZSkill.Plus1_to_dice_roll_Combat, ZSkill.Roll_6_plus1_die_Combat)),
    Baldric("Magician",
            Utils.toArray(),
            Utils.toArray(ZWeapon.SHORT_SWORD),
            Utils.toArray(ZSkill.Spellcaster),
            Utils.toArray(ZSkill.Plus1_Action),
            Utils.toArray(ZSkill.Plus1_free_Magic_Action, ZSkill.Spellbook),
            Utils.toArray(ZSkill.Plus1_die_Magic, ZSkill.Plus1_free_Combat_Action, ZSkill.Plus1_to_dice_roll_Melee)),
    Clovis("Warrior",
            Utils.toArray(),
            Utils.toArray(ZArmor.SHIELD),
            Utils.toArray(ZSkill.Plus1_die_Melee),
            Utils.toArray(ZSkill.Plus1_Action),
            Utils.toArray(ZSkill.Plus1_free_Melee_Action, ZSkill.Swordmaster),
            Utils.toArray(ZSkill.Plus1_free_Combat_Action, ZSkill.Plus1_to_dice_roll_Combat, ZSkill.Hit_and_run)),
    Samson("Dwarf",
            Utils.toArray(),
            Utils.toArray(ZArmor.SHIELD),
            Utils.toArray(ZSkill.Iron_hide),
            Utils.toArray(ZSkill.Plus1_Action),
            Utils.toArray(ZSkill.Plus1_die_Combat, ZSkill.Plus1_to_dice_roll_Melee),
            Utils.toArray(ZSkill.Plus1_Damage_Melee, ZSkill.Plus1_free_Combat_Action, ZSkill.Barbarian)),
    Nelly("Rogue",
            Utils.toArray(),
            Utils.toArray(ZWeapon.DAGGER),
            Utils.toArray(ZSkill.Plus1_free_Move_Action),
            Utils.toArray(ZSkill.Plus1_Action),
            Utils.toArray(ZSkill.Bloodlust_Melee, ZSkill.Slippery),
            Utils.toArray(ZSkill.Plus1_free_Combat_Action, ZSkill.Plus1_to_dice_roll_Combat, ZSkill.Hit_and_run)),
    Silas("Elven Ranger",
            Utils.toArray(),
            Utils.toArray(ZWeapon.SHORT_SWORD),
            Utils.toArray(ZSkill.Plus1_to_dice_roll_Ranged),
            Utils.toArray(ZSkill.Plus1_Action),
            Utils.toArray(ZSkill.Plus1_free_Ranged_Action, ZSkill.Point_blank),
            Utils.toArray(ZSkill.Plus1_die_Ranged, ZSkill.Plus1_free_Combat_Action, ZSkill.Iron_rain)),
    Tucker("Monk",
            Utils.toArray(),
            Utils.toArray(ZWeapon.AXE),
            Utils.toArray(ZSkill.Shove),
            Utils.toArray(ZSkill.Plus1_Action),
            Utils.toArray(ZSkill.Lucky, ZSkill.Spellcaster),
            Utils.toArray(ZSkill.Plus1_die_Combat, ZSkill.Plus1_free_Combat_Action, ZSkill.Free_reload)),
    Tyrion("Elitist",
            Utils.toArray(),
            Utils.toArray(ZWeapon.AXE),
            Utils.toArray(ZSkill.Born_leader, ZSkill.Low_profile),
            Utils.toArray(ZSkill.Plus1_Action),
            Utils.toArray(ZSkill.Plus1_free_Ranged_Action, ZSkill.Plus1_die_Combat),
            Utils.toArray(ZSkill.Plus1_die_Ranged, ZSkill.Plus1_free_Combat_Action, ZSkill.Marksman)),
    Damiel("Assasin",
            Utils.toArray(),
            Utils.toArray(ZItem.DRAGON_BILE),
            Utils.toArray(), //ZSkill.TwoDragonBilesIsBeterThanOne),
            Utils.toArray(ZSkill.Plus1_Action),
            Utils.toArray(ZSkill.Search_plus1_card, ZSkill.Scavenger),
            Utils.toArray(ZSkill.Low_profile, ZSkill.Spellbook, ZSkill.Free_reload)),
    Annice("Healer",
            Utils.toArray(ZEnchantment.HEALING),
            Utils.toArray(ZEnchantment.HEALING),
            Utils.toArray(),
            Utils.toArray(ZSkill.Plus1_Action),
            Utils.toArray(ZSkill.Lifesaver, ZSkill.Spellcaster),
            Utils.toArray(ZSkill.Born_leader, ZSkill.Regeneration, ZSkill.Plus1_free_Magic_Action)),
    Kabral("Barbarian",
            Utils.toArray(),
            Utils.toArray(ZWeapon.DAGGER),
            Utils.toArray(ZSkill.Shove),
            Utils.toArray(ZSkill.Plus1_Action),
            Utils.toArray(ZSkill.Barbarian, ZSkill.Bloodlust_Melee),
            Utils.toArray(ZSkill.Plus1_die_Melee, ZSkill.Plus1_free_Melee_Action, ZSkill.Plus1_to_dice_roll_Melee)),
    /*
    Mortimer("Knight",
            Utils.toArray(ZWeapon.GREAT_SWORD),
            Utils.toArray(),
            )*/
    ;

    ZPlayerName(String characterClass, ZEquipment [] startingEquipment, ZEquipment [] alternateBodySlots, ZSkill [] blueSkillOptions, ZSkill[] yellowSkillOptions, ZSkill[] orangeSkillOptions, ZSkill[] redSkillOptions) {
        this.characterClass = characterClass;
        this.startingEquipment = startingEquipment;
        this.alternateBodySlots = alternateBodySlots;
        this.blueSkillOptions = blueSkillOptions;
        this.yellowSkillOptions = yellowSkillOptions;
        this.orangeSkillOptions = orangeSkillOptions;
        this.redSkillOptions = redSkillOptions;
    }

    public final String characterClass;
    public final ZEquipment [] startingEquipment;
    public final ZEquipment [] alternateBodySlots;
    public final ZSkill [] blueSkillOptions;
    public final ZSkill [] yellowSkillOptions;
    public final ZSkill [] orangeSkillOptions;
    public final ZSkill [] redSkillOptions;
    public final int startingActionsPerTurn = 3;

    public ZCharacter create() {
        ZCharacter c = new ZCharacter();
        c.name = this;
        for (ZEquipment e : startingEquipment)
            c.equip(e);
        c.allSkills.addAll(Arrays.asList(blueSkillOptions));
        return c;
    }

    public int imageId = -1;
}
