package cc.lib.zombicide;

public enum ZArmorType implements ZEquipmentType<ZArmor> {
    LEATHER(ZEquipSlotType.BODY,5, ""),
    CHAIN(ZEquipSlotType.BODY, 4, ""),
    PLATE(ZEquipSlotType.BODY, 3, ""),
    SHIELD(ZEquipSlotType.HAND, 3, ""),
    DWARVEN_SHIELD(ZEquipSlotType.HAND, 4, "TODO: Protects against abomination"),
    SHIELD_OF_AGES(ZEquipSlotType.HAND, 4, "TODO: Gain the shove skill"),
    ;

    ZArmorType(ZEquipSlotType slotType, int dieRollToBlock, String specialAbilityDescription) {
        this.slotType = slotType;
        this.dieRollToBlock = dieRollToBlock;
        this.specialAbilityDescription = specialAbilityDescription;
    }

    final int dieRollToBlock;
    final ZEquipSlotType slotType;
    final String specialAbilityDescription;

    int getRating(ZZombieType type) {
        switch (this) {
            default:
                if (type == ZZombieType.Abomination)
                    return 0;
            case DWARVEN_SHIELD:
        }
        return 6-dieRollToBlock;
    }

    @Override
    public ZArmor create() {
        return new ZArmor(this);
    }

    public ZSkill extraSkill() {
        switch (this) {
            case SHIELD_OF_AGES:
                return ZSkill.Shove;
        }
        return null;
    }
}
