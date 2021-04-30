package cc.lib.zombicide;

import java.util.Arrays;

import cc.lib.annotation.Keep;
import cc.lib.game.GDimension;
import cc.lib.ui.IButton;

@Keep
public enum ZPlayerName implements IButton {
    Ann("Angry Nun",
            toArray(ZWeaponType.DAGGER),
            toArray(ZWeaponType.DAGGER),
            toArray(ZSkill.Bloodlust),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Plus1_free_Magic_Action, ZSkill.Plus1_free_Melee_Action),
            toArray(ZSkill.Plus1_free_Combat_Action, ZSkill.Plus1_to_dice_roll_Combat, ZSkill.Roll_6_plus1_die_Combat)),
    Baldric("Magician",
            toArray(ZWeaponType.MANA_BLAST, ZWeaponType.FIREBALL), //, ZWeaponType.LIGHTNING_BOLT, ZWeaponType.INFERNO, ZWeaponType.FIREBALL, ZWeaponType.EARTHQUAKE), //, (ZEquipmentType)ZSpellType.SPEED),
            toArray(ZWeaponType.SWORD),
            toArray(ZSkill.Spellcaster),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Plus1_free_Magic_Action, ZSkill.Spellbook),
            toArray(ZSkill.Plus1_die_Magic, ZSkill.Plus1_free_Combat_Action, ZSkill.Plus1_to_dice_roll_Melee)),
    Clovis("Warrior",
            toArray(ZWeaponType.SHORT_SWORD), //, ZItemType.TORCH, ZItemType.DRAGON_BILE),
            toArray(ZArmorType.SHIELD),
            toArray(ZSkill.Plus1_die_Melee),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Plus1_free_Melee_Action, ZSkill.Swordmaster),
            toArray(ZSkill.Plus1_free_Combat_Action, ZSkill.Plus1_to_dice_roll_Combat, ZSkill.Hit_and_run)),
    Samson("Dwarf",
            toArray(ZWeaponType.AXE, ZItemType.TORCH), //, (ZEquipmentType)ZItemType.DRAGON_BILE, (ZEquipmentType)ZItemType.TORCH),
            toArray(ZArmorType.SHIELD),
            toArray(ZSkill.Iron_hide),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Plus1_die_Combat, ZSkill.Plus1_to_dice_roll_Melee),
            toArray(ZSkill.Plus1_Damage_Melee, ZSkill.Plus1_free_Combat_Action, ZSkill.Barbarian)),
    Nelly("Rogue",
            toArray(ZWeaponType.SHORT_SWORD),
            toArray(ZWeaponType.DAGGER),
            toArray(ZSkill.Plus1_free_Move_Action),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Bloodlust_Melee, ZSkill.Slippery),
            toArray(ZSkill.Plus1_free_Combat_Action, ZSkill.Plus1_to_dice_roll_Combat, ZSkill.Hit_and_run)),
    Silas("Elven Ranger",
            toArray(ZWeaponType.SHORT_BOW),
            toArray(ZWeaponType.SHORT_SWORD),
            toArray(ZSkill.Plus1_to_dice_roll_Ranged),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Plus1_free_Ranged_Action, ZSkill.Point_blank),
            toArray(ZSkill.Plus1_die_Ranged, ZSkill.Plus1_free_Combat_Action, ZSkill.Iron_rain)),
    Tucker("Monk",
            toArray(ZWeaponType.CROSSBOW),
            toArray(ZWeaponType.AXE),
            toArray(ZSkill.Shove),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Lucky, ZSkill.Spellcaster),
            toArray(ZSkill.Plus1_die_Combat, ZSkill.Plus1_free_Combat_Action, ZSkill.Free_reload)),
    Jain("Valkerie",
            toArray(ZWeaponType.LONG_BOW, ZItemType.PLENTY_OF_ARROWS),
            toArray(ZItemType.TORCH),
            toArray(ZSkill.Steady_hand),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Plus1_die_Ranged, ZSkill.Plus1_max_Range),
            toArray(ZSkill.Plus1_Damage_Ranged, ZSkill.Point_blank, ZSkill.Sprint)),
    Benson("Elite Guard",
            toArray(ZWeaponType.SWORD),
            toArray(ZWeaponType.SWORD),
            toArray(ZSkill.Steel_hide),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Plus1_free_Melee_Action, ZSkill.Born_leader),
            toArray(ZSkill.Plus1_Damage_Melee, ZSkill.Lucky, ZSkill.Shove)),
    /*
    Leander("Elite Guard",
            toArray(ZWeaponType.GREAT_SWORD, ZWeaponType.ORCISH_CROSSBOW, (ZEquipmentType)ZArmorType.PLATE),
            toArray(ZWeaponType.GREAT_SWORD),
            toArray(ZSkill.Steel_hide),
            toArray(ZSkill.Plus1_Damage_Melee),
            toArray(ZSkill.Plus1_to_dice_roll_Melee, ZSkill.Plus1_to_dice_roll_Ranged),
            toArray(ZSkill.Plus1_die_Melee, ZSkill.Plus1_Damage_Ranged, ZSkill.Bloodlust)),

    /*
    Tyrion("Elitist",
            toArray(ZWeaponType.DAGGER),
            toArray(ZWeaponType.AXE),
            toArray(ZSkill.Born_leader, ZSkill.Low_profile),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Plus1_free_Ranged_Action, ZSkill.Plus1_die_Combat),
            toArray(ZSkill.Plus1_die_Ranged, ZSkill.Plus1_free_Combat_Action, ZSkill.Marksman)),
    Damiel("Assasin",
            toArray(ZWeaponType.DEATH_STRIKE),
            toArray(ZItemType.DRAGON_BILE),
            toArray(), //ZSkill.TwoDragonBilesIsBeterThanOne),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Search_plus1_card, ZSkill.Scavenger),
            toArray(ZSkill.Low_profile, ZSkill.Spellbook, ZSkill.Free_reload)),
    Annice("Healer",
            toArray(ZSpellType.HEALING),
            toArray(ZSpellType.HEALING),
            toArray(),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Lifesaver, ZSkill.Spellcaster),
            toArray(ZSkill.Born_leader, ZSkill.Regeneration, ZSkill.Plus1_free_Magic_Action)),
    Kabral("Barbarian",
            toArray(ZWeaponType.SWORD),
            toArray(ZWeaponType.DAGGER),
            toArray(ZSkill.Shove),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Barbarian, ZSkill.Bloodlust_Melee),
            toArray(ZSkill.Plus1_die_Melee, ZSkill.Plus1_free_Melee_Action, ZSkill.Plus1_to_dice_roll_Melee)),
    /*
    Mortimer("Knight",
            toArray(ZWeapon.GREAT_SWORD),
            toArray(),
            )*/
    ;

    ZPlayerName(String characterClass, ZEquipmentType [] startingEquipment, ZEquipmentType [] alternateBodySlots, ZSkill [] blueSkillOptions, ZSkill[] yellowSkillOptions, ZSkill[] orangeSkillOptions, ZSkill[] redSkillOptions) {
        this.characterClass = characterClass;
        this.startingEquipment = startingEquipment;
        this.alternateBodySlots = alternateBodySlots;
        this.skillOptions = new ZSkill[][] {
                blueSkillOptions,
                yellowSkillOptions,
                orangeSkillOptions,
                redSkillOptions
        };
    }

    public final String characterClass;
    public final ZEquipmentType [] startingEquipment;
    public final ZEquipmentType [] alternateBodySlots;
    private final ZSkill [][] skillOptions;

    public int imageId = -1;
    public int cardImageId = -1;
    public GDimension imageDim;

    public ZCharacter create() {
        ZCharacter c = new ZCharacter();
        c.name = this;
        character = c;
        for (ZEquipmentType e : startingEquipment)
            c.equip(e.create());
        c.allSkills.addAll(Arrays.asList(getSkillOptions(ZSkillLevel.BLUE)));
        return c;
    }

    ZCharacter character;

    public ZSkill [] getSkillOptions(ZSkillLevel level) {
        return skillOptions[level.ordinal()];
    }

    @Override
    public String getTooltipText() {
        return characterClass;
    }

    @Override
    public String getLabel() {
        return name();
    }

    static ZEquipmentType [] toArray(ZEquipmentType ... e) {
        return e;
    }

    static ZSkill [] toArray(ZSkill ... e) {
        return e;
    }

}
