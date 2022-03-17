package cc.lib.zombicide;

import cc.lib.annotation.Keep;
import cc.lib.game.GDimension;
import cc.lib.game.IDimension;
import cc.lib.ui.IButton;

@Keep
public enum ZPlayerName implements IButton, IDimension {
    Ann("Angry Nun",
            toArray(ZWeaponType.DAGGER, ZWeaponType.HAND_CROSSBOW),
            ZEquipmentClass.DAGGER,
            toArray(ZSkill.Bloodlust),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Plus1_free_Magic_Action, ZSkill.Plus1_free_Melee_Action),
            toArray(ZSkill.Plus1_free_Combat_Action, ZSkill.Plus1_to_dice_roll_Combat, ZSkill.Roll_6_plus1_die_Combat)),
    Baldric("Magician",
            toArray(ZWeaponType.MANA_BLAST, ZWeaponType.LIGHTNING_BOLT, ZItemType.TORCH),
            ZEquipmentClass.SWORD,
            toArray(ZSkill.Spellcaster),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Plus1_free_Magic_Action, ZSkill.Spellbook),
            toArray(ZSkill.Plus1_die_Magic, ZSkill.Plus1_free_Combat_Action, ZSkill.Plus1_to_dice_roll_Melee)),
    Clovis("Warrior",
            toArray(ZWeaponType.SHORT_SWORD, ZWeaponType.CROSSBOW),
            ZEquipmentClass.SHIELD,
            toArray(ZSkill.Plus1_die_Melee, ZSkill.Marksman),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Plus1_free_Melee_Action, ZSkill.Swordmaster),
            toArray(ZSkill.Plus1_free_Combat_Action, ZSkill.Plus1_to_dice_roll_Combat, ZSkill.Hit_and_run)),
    Samson("Dwarf",
            toArray(ZWeaponType.AXE, ZArmorType.SHIELD, ZItemType.BARRICADE),
            ZEquipmentClass.SHIELD,
            toArray(ZSkill.Iron_hide),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Plus1_die_Combat, ZSkill.Plus1_to_dice_roll_Melee),
            toArray(ZSkill.Plus1_Damage_Melee, ZSkill.Plus1_free_Combat_Action, ZSkill.Barbarian)),
    Nelly("Rogue",
            toArray(ZWeaponType.SHORT_SWORD, ZWeaponType.SHORT_BOW),
            ZEquipmentClass.BOW,
            toArray(ZSkill.Plus1_free_Move_Action),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Bloodlust_Melee, ZSkill.Slippery),
            toArray(ZSkill.Plus1_free_Combat_Action, ZSkill.Plus1_to_dice_roll_Combat, ZSkill.Hit_and_run)),
    Silas("Elven Ranger",
            toArray(ZWeaponType.SHORT_BOW),
            ZEquipmentClass.SWORD,
            toArray(ZSkill.Plus1_to_dice_roll_Ranged),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Plus1_free_Ranged_Action, ZSkill.Point_blank),
            toArray(ZSkill.Plus1_die_Ranged, ZSkill.Plus1_free_Combat_Action, ZSkill.Iron_rain)),
    Tucker("Monk",
            toArray(ZWeaponType.CROSSBOW, ZWeaponType.CROSSBOW),
            ZEquipmentClass.AXE,
            toArray(ZSkill.Shove),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Lucky, ZSkill.Spellcaster),
            toArray(ZSkill.Plus1_die_Combat, ZSkill.Plus1_free_Combat_Action, ZSkill.Free_reload)),
    Jain("Valkerie",
            toArray(ZWeaponType.LONG_BOW, ZItemType.PLENTY_OF_ARROWS),
            ZEquipmentClass.THROWABLE,
            toArray(ZSkill.Steady_hand),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Plus1_die_Ranged, ZSkill.Plus1_max_Range),
            toArray(ZSkill.Plus1_Damage_Ranged, ZSkill.Point_blank, ZSkill.Sprint)),
    Benson("Elite Guard",
            toArray(ZWeaponType.SWORD),
            ZEquipmentClass.SWORD,
            toArray(ZSkill.Steel_hide),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Plus1_free_Melee_Action, ZSkill.Born_leader),
            toArray(ZSkill.Plus1_Damage_Melee, ZSkill.Lucky, ZSkill.Shove)),
    // Wulfsburg
    Theo("Ranger",
            toArray(ZWeaponType.DAGGER),//, ZWeaponType.EARTHQUAKE_HAMMER),
            ZEquipmentClass.SWORD,
            toArray(ZSkill.Sprint),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Plus1_die_Melee, ZSkill.Plus1_to_dice_roll_Ranged),
            toArray(ZSkill.Plus1_free_Combat_Action, ZSkill.Charge, ZSkill.Marksman)),
    Morrigan("Witcher",
            toArray(ZWeaponType.AXE),//, ZWeaponType.CHAOS_LONGBOW),
            ZEquipmentClass.DAGGER,
            toArray(ZSkill.Reaper_Combat),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Plus1_free_Melee_Action, ZSkill.Spellcaster),
            toArray(ZSkill.Plus1_Damage_Melee, ZSkill.Plus1_die_Magic, ZSkill.Bloodlust)),
    Karl("Monk",
            toArray(ZWeaponType.SWORD, ZWeaponType.FIREBALL),
            ZEquipmentClass.MAGIC,
            toArray(ZSkill.Plus1_die_Magic),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Plus1_free_Combat_Action, ZSkill.Spellcaster),
            toArray(ZSkill.Plus1_free_Combat_Action, ZSkill.Plus1_max_Range, ZSkill.Marksman)),
    Ariane("Thief",
            toArray(ZWeaponType.CROSSBOW),
            ZEquipmentClass.CROSSBOW,
            toArray(ZSkill.Jump),
            toArray(ZSkill.Plus1_Action),
            toArray(ZSkill.Plus1_free_Move_Action, ZSkill.Plus1_free_Melee_Action),
            toArray(ZSkill.Plus1_die_Combat, ZSkill.Plus1_die_Ranged, ZSkill.Spellcaster))
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

    ZPlayerName(String characterClass, ZEquipmentType [] startingEquipment, ZEquipmentClass alternateBodySlot, ZSkill [] blueSkillOptions, ZSkill[] yellowSkillOptions, ZSkill[] orangeSkillOptions, ZSkill[] redSkillOptions) {
        this.characterClass = characterClass;
        this.startingEquipment = startingEquipment;
        this.alternateBodySlot = alternateBodySlot;
        this.skillOptions = new ZSkill[][] {
                blueSkillOptions,
                yellowSkillOptions,
                orangeSkillOptions,
                redSkillOptions
        };
    }

    public final String characterClass;
    public final ZEquipmentType [] startingEquipment;
    public final ZEquipmentClass alternateBodySlot;
    private final ZSkill [][] skillOptions;

    public int imageId = -1;
    public int cardImageId = -1;
    public int outlineImageId = -1;
    public GDimension imageDim;

    public ZCharacter create() {
        ZCharacter c = new ZCharacter(this);
        character = c;
        for (ZEquipmentType e : startingEquipment) {
            c.tryEquip(e.create());
            if (!ZGame.DEBUG)
                break;
        }
        c.initAllSkills(skillOptions);
        return c;
    }

    ZCharacter character;

    public ZCharacter getCharacter() {
        return character;
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

    @Override
    public float getWidth() {
        return imageDim.getWidth();
    }

    @Override
    public float getHeight() {
        return imageDim.getHeight();
    }

    public ZSkill [] getSkillOptions(ZColor color) {
        return skillOptions[color.ordinal()];
    }
}
