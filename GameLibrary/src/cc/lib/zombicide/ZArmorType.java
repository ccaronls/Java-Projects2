package cc.lib.zombicide;

import cc.lib.annotation.Keep;

@Keep
public enum ZArmorType implements ZEquipmentType<ZArmor> {
    LEATHER(false, ZEquipSlotType.BODY,5, ""),
    CHAINMAIL(false, ZEquipSlotType.BODY, 4, ""),
    PLATE(false, ZEquipSlotType.BODY, 3, ""),
    SHIELD(true, ZEquipSlotType.HAND, 4, ""),
    DWARVEN_SHIELD(true, ZEquipSlotType.HAND, 4, "TODO: Protects against abomination"),
    SHIELD_OF_AGES(true, ZEquipSlotType.HAND, 4, "TODO: Gain the shove skill"),
    ;

    ZArmorType(boolean isShield, ZEquipSlotType slotType, int dieRollToBlock, String specialAbilityDescription) {
        this.shield = isShield;
        this.slotType = slotType;
        this.dieRollToBlock = dieRollToBlock;
        this.specialAbilityDescription = specialAbilityDescription;
    }

    final int dieRollToBlock;
    final ZEquipSlotType slotType;
    final String specialAbilityDescription;
    final boolean shield;

    int getRating(ZZombieType type) {
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
    public ZActionType getActionType() {
        return ZActionType.NOTHING;
    }

    public ZSkill extraSkill() {
        switch (this) {
            case SHIELD_OF_AGES:
                return ZSkill.Shove;
        }
        return null;
    }


}
