package cc.lib.zombicide;

import java.util.Arrays;
import java.util.List;

import cc.lib.annotation.Keep;
import cc.lib.game.Utils;

@Keep
public enum ZWeaponType implements ZEquipmentType<ZWeapon> {
    // DAGGER get extra die roll when 2 handed with another melee weapon

    // BLUE WEAPONS

    // MELEE
    DAGGER(ZEquipmentClass.DAGGER, ZColor.BLUE,false, true, false, true, Utils.toArray(new ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 4, 0, 0, 1, 4, 1), new ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_THROW, 0, 1, 1, 1, 3, 2)),  "Gain +1 die with another equipped melee weapon") {
        @Override
        public List<ZSkill> getSkillsWhenUsed() {
            return Utils.toList(ZSkill.Plus1_die_Melee_Weapon);
        }
    },
    AXE(ZEquipmentClass.AXE, ZColor.BLUE, false, true, false, true, Utils.toArray(new ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE,1, 0, 0, 1, 4, 1)), null),
    HAMMER(ZEquipmentClass.AXE, ZColor.BLUE, false, false, false, true, Utils.toArray(new ZWeaponStat(ZActionType.MELEE, ZAttackType.CRUSH,4, 0, 0, 1, 3, 2)), null),
    SHORT_SWORD(ZEquipmentClass.SWORD, ZColor.BLUE, false, true, false, true, Utils.toArray(new ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE,3, 0, 0, 1, 4, 1)), null),
    SWORD(ZEquipmentClass.SWORD, ZColor.BLUE, false, true, false, true, Utils.toArray(new ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE,3, 0, 0, 2, 4, 1)), null),
    GREAT_SWORD(ZEquipmentClass.SWORD, ZColor.BLUE, false, false, false, true, Utils.toArray(new ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE,4, 0, 0, 5, 5, 1)), null),
    // BOWS
    SHORT_BOW(ZEquipmentClass.BOW, ZColor.BLUE, false, false, false, false, Utils.toArray(new ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_ARROWS,0, 0, 1, 1, 3, 1)), null),
    LONG_BOW(ZEquipmentClass.BOW, ZColor.BLUE, false, false, false, false, Utils.toArray(new ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_ARROWS,0, 1, 3, 1, 3, 1)), null),
    // CROSSBOWS
    CROSSBOW(ZEquipmentClass.CROSSBOW, ZColor.BLUE, true, false, false, false, Utils.toArray(new ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_BOLTS,0, 1, 2, 2, 4,2)), null),
    REPEATING_CROSSBOW(ZEquipmentClass.CROSSBOW, ZColor.BLUE, true, true, false, false, Utils.toArray(new ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_BOLTS,0, 0, 1, 3, 5, 1)), null),
    HAND_CROSSBOW(ZEquipmentClass.CROSSBOW, ZColor.BLUE, true, true, false, false, Utils.toArray(new ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_BOLTS,0, 0, 3, 2, 3, 1)), "Auto reload at end of turn"),
    ORCISH_CROSSBOW(ZEquipmentClass.CROSSBOW, ZColor.BLUE, true, false, false, false, Utils.toArray(new ZWeaponStat(ZActionType.MELEE, ZAttackType.CRUSH,0, 0, 0, 2, 3, 2), new ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_BOLTS,0, 1, 2, 2, 3, 2)), null),
    HEAVY_CROSSBOW(ZEquipmentClass.CROSSBOW, ZColor.BLUE, true, false, false, false, Utils.toArray(new ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_BOLTS,0, 1, 2, 2, 4, 3)), null),

    // MAGIC
    DEATH_STRIKE(ZEquipmentClass.MAGIC, ZColor.BLUE, false,true, true, false, Utils.toArray(new ZWeaponStat(ZActionType.MAGIC, ZAttackType.MENTAL_STRIKE,0, 0, 1, 1, 4, 2)), null),
    // TODO: +1 damage on a die roll 6
    DISINTEGRATE(ZEquipmentClass.MAGIC, ZColor.BLUE, false, true, true, false, Utils.toArray(new ZWeaponStat(ZActionType.MAGIC, ZAttackType.DISINTEGRATION,0, 0, 1, 3, 5, 2)), null),
    EARTHQUAKE(ZEquipmentClass.MAGIC, ZColor.BLUE, false, true, true, false, Utils.toArray(new ZWeaponStat(ZActionType.MAGIC, ZAttackType.EARTHQUAKE,0, 0, 1, 3, 4, 1)), null),
    FIREBALL(ZEquipmentClass.MAGIC, ZColor.BLUE, false, true, true, false, Utils.toArray(new ZWeaponStat(ZActionType.MAGIC, ZAttackType.FIRE,0, 0, 1, 3, 4, 1)), null),
    MANA_BLAST(ZEquipmentClass.MAGIC, ZColor.BLUE, false, true, true, false, Utils.toArray(new ZWeaponStat(ZActionType.MAGIC, ZAttackType.MENTAL_STRIKE,0, 0, 2, 1, 4, 1)), null),
    INFERNO(ZEquipmentClass.MAGIC, ZColor.BLUE, false, false, true, false, Utils.toArray(new ZWeaponStat(ZActionType.MAGIC, ZAttackType.FIRE,0 ,0 ,1, 4, 4, 2)), null),
    LIGHTNING_BOLT(ZEquipmentClass.MAGIC, ZColor.BLUE, false,true, true, false, Utils.toArray(new ZWeaponStat(ZActionType.MAGIC, ZAttackType.ELECTROCUTION,0 ,0 ,3, 1, 4, 1)), null),

    // WOLFBERG

    BASTARD_SWORD(ZEquipmentClass.SWORD, ZColor.BLUE, false, false, false, true, Utils.toArray(new ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 4, 0, 0, 2, 4, 2)), null),

    // YELLOW
    DEFLECTING_DAGGER(ZEquipmentClass.DAGGER, ZColor.YELLOW, false, true, false, true, Utils.toArray(new ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 4, 0, 0, 1, 4, 1)), "Gain +1 die from another equipped melee weapon. Acts as a shield with 4+ armor roll") {
        @Override
        public List<ZSkill> getSkillsWhenUsed() {
            return Utils.toList(ZSkill.Plus1_die_Melee_Weapon);
        }

        @Override
        public boolean isShield() {
            return true;
        }

        @Override
        public int getDieRollToBlock(ZZombieType type) {
            if (type.ignoresArmor)
                return 0;
            return 4;
        }
    },
    FLAMING_GREAT_SWORD(ZEquipmentClass.SWORD, ZColor.YELLOW,false, false, false, true, Utils.toArray(new ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 5, 0, 0, 5, 5, 2)), "Can ignite Dragon Fire at range 0-1") {
        @Override
        public List<ZSkill> getSkillsWhileEquipped() {
            return Utils.toList(ZSkill.Ignite_Dragon_Fire);
        }
    },
    VAMPIRE_CROSSBOW(ZEquipmentClass.CROSSBOW, ZColor.YELLOW, true, false, false, false, Utils.toArray(new ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_BOLTS, 0, 1, 2, 2, 4, 3)), "Heal 1 wound each time you kill a zombie.") {
        @Override
        public List<ZSkill> getSkillsWhenUsed() {
            return Utils.toList(ZSkill.Hit_Heals);
        }
    },

    // ORANGE
    EARTHQUAKE_HAMMER(ZEquipmentClass.AXE, ZColor.ORANGE, false, false, false, true, Utils.toArray(new ZWeaponStat(ZActionType.MELEE, ZAttackType.EARTHQUAKE, 3, 0, 0, 3, 3, 2)), "Roll 6: +1 die and +1 damage") {
        @Override
        public List<ZSkill> getSkillsWhenUsed() {
            return Utils.toList(ZSkill.Roll_6_Plus1_Damage, ZSkill.Roll_6_plus1_die_Melee);
        }
    },
    AXE_OF_CARNAGE(ZEquipmentClass.AXE, ZColor.ORANGE, false, false, false, true, Utils.toArray(new ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 1, 0, 0, 4, 4, 2)), "Add an additional success with each melee action resolved.??") {
        @Override
        public List<ZSkill> getSkillsWhenUsed() {
            return Utils.toList(ZSkill.Two_For_One_Melee);
        }
    },
    DRAGON_FIRE_BLADE(ZEquipmentClass.SWORD, ZColor.ORANGE, false, false, false, true, Utils.toArray(new ZWeaponStat(ZActionType.MELEE, ZAttackType.BLADE, 3, 0, 0, 2, 3,2), new ZWeaponStat(ZActionType.THROW_ITEM, ZAttackType.DRAGON_FIRE, 0, 0, 1, 1, 1, 3)), "Throw (Discard) at range 0-1 to create a dragon fire.") {
        @Override
        public void onThrown(ZGame game, ZCharacter thrower, int targetZoneIdx) {
            game.onEquipmentThrown(thrower.getPlayerName(), ZIcon.DAGGER, targetZoneIdx);
            game.performDragonFire(thrower, targetZoneIdx);
        }
    },
    CHAOS_LONGBOW(ZEquipmentClass.BOW, ZColor.ORANGE, false, false, false, false, Utils.toArray(new ZWeaponStat(ZActionType.RANGED, ZAttackType.RANGED_ARROWS, 0, 0, 3, 4, 4, 2)), "4 or more hits on a ranged action causes dragon fire in the targeted zone.") {
        @Override
        public List<ZSkill> getSkillsWhenUsed() {
            return Utils.toList(ZSkill.Hit_4_Dragon_Fire);
        }
    },
    // RED

    ;

    ZWeaponType(ZEquipmentClass clazz, ZColor minColorToEquip, boolean needsReload, boolean canTwoHand, boolean attackIsNoisy, boolean openDoorsIsNoisy, ZWeaponStat [] weaponStats, String specialInfo) {
        this.weaponClass = clazz;
        this.minColorToEquip = minColorToEquip;
        this.needsReload = needsReload;
        this.canTwoHand = canTwoHand;
        this.attackIsNoisy = attackIsNoisy;
        this.openDoorsIsNoisy = openDoorsIsNoisy;
        this.weaponStats = weaponStats;
        this.specialInfo = specialInfo;
    }

    final ZEquipmentClass weaponClass;
    final ZColor minColorToEquip;
    final boolean needsReload;
    final boolean canTwoHand;
    final boolean attackIsNoisy;
    final boolean openDoorsIsNoisy;
    final ZWeaponStat [] weaponStats;
    final String specialInfo;

    public List<ZWeaponStat> getStats() {
        return Arrays.asList(weaponStats);
    }

    @Override
    public ZWeapon create() {
        return new ZWeapon(this);
    }

    public boolean isFire() {
        switch (this) {
            case INFERNO:
            case FIREBALL:
                return true;
        }
        return false;
    }

    @Override
    public boolean isActionType(ZActionType type) {
        return Utils.count(weaponStats, stat -> stat.actionType == type) > 0;
    }

    @Override
    public ZEquipmentClass getEquipmentClass() {
        return weaponClass;
    }
}
