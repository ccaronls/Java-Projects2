package cc.lib.zombicide;

public enum ZArmorType implements ZEquipmentType<ZArmor> {
    LEATHER(ZEquipSlotType.BODY,5),
    CHAIN(ZEquipSlotType.BODY, 4),
    PLATE(ZEquipSlotType.BODY, 3),
    SHIELD(ZEquipSlotType.HAND, 3),
    DWARVEN_SHIELD(ZEquipSlotType.HAND, 4), // protects against abomination TODO
    SHIELD_OF_AGES(ZEquipSlotType.HAND, 4), // gain the shove skill TODO
    ;

    ZArmorType(ZEquipSlotType slotType, int dieRollToBlock) {
        this.slotType = slotType;
        this.dieRollToBlock = dieRollToBlock;
    }

    final int dieRollToBlock;
    final ZEquipSlotType slotType;

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
}
