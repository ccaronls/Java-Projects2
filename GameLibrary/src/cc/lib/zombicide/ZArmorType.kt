package cc.lib.zombicide;

import java.util.List;

import cc.lib.annotation.Keep;
import cc.lib.game.Utils;

@Keep
public enum ZArmorType implements ZEquipmentType {
    LEATHER(ZEquipmentClass.ARMOR, ZEquipSlotType.BODY,5, ""),
    CHAINMAIL(ZEquipmentClass.ARMOR, ZEquipSlotType.BODY, 4, ""),
    PLATE(ZEquipmentClass.ARMOR, ZEquipSlotType.BODY, 3, ""),
    SHIELD(ZEquipmentClass.SHIELD, ZEquipSlotType.HAND, 4, ""),
    DWARVEN_SHIELD(ZEquipmentClass.SHIELD, ZEquipSlotType.HAND, 4, "TODO: Protects against abomination"),
    SHIELD_OF_AGES(ZEquipmentClass.SHIELD, ZEquipSlotType.HAND, 4, "TODO: Gain the shove skill") {
        @Override
        public List<ZSkill> getSkillsWhileEquipped() {
            return Utils.toList(ZSkill.Shove);
        }
    },
    ;

    ZArmorType(ZEquipmentClass equipClass, ZEquipSlotType slotType, int dieRollToBlock, String specialAbilityDescription) {
        this.equipClass = equipClass;
        this.slotType = slotType;
        this.dieRollToBlock = dieRollToBlock;
        this.specialAbilityDescription = specialAbilityDescription;
    }

    final int dieRollToBlock;
    final ZEquipSlotType slotType;
    final String specialAbilityDescription;
    final ZEquipmentClass equipClass;

    public int getDieRollToBlock(ZZombieType type) {
        switch (this) {
            default:
                if (type.ignoresArmor)
                    return 0;
            case DWARVEN_SHIELD:
        }
        return dieRollToBlock;
    }

    @Override
    public ZArmor create() {
        return new ZArmor(this);
    }

    @Override
    public boolean isActionType(ZActionType type) {
        return false;
    }

    @Override
    public ZEquipmentClass getEquipmentClass() {
        return equipClass;
    }

    @Override
    public boolean isShield() {
        return equipClass == ZEquipmentClass.SHIELD;
    }
}
