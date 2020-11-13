package cc.lib.zombicide;

public enum ZArmor implements ZEquipment {

    LEATHER(ZEquipSlotType.BODY,5),
    CHAIN(ZEquipSlotType.BODY, 4),
    PLATE(ZEquipSlotType.BODY, 3),
    SHIELD(ZEquipSlotType.HAND, 3),
    DWARVEN_SHIELD(ZEquipSlotType.HAND, 4), // protects against abomination TODO
    SHIELD_OF_AGES(ZEquipSlotType.HAND, 4), // gain the shove skill TODO
    ;

    ZArmor(ZEquipSlotType slotType, int dieRollToBlock) {
        this.slotType = slotType;
        this.dieRollToBlock = dieRollToBlock;
    }

    final int dieRollToBlock;
    final ZEquipSlotType slotType;

    @Override
    public ZEquipSlotType getSlotType() {
        return slotType;
    }

    @Override
    public boolean canEquip() {
        return true;
    }

    @Override
    public boolean isArmor() {
        return true;
    }

    int getRating(ZZombieType type) {
        switch (this) {
            default:
                if (type == ZZombieType.Abomination)
                    return 0;
            case DWARVEN_SHIELD:
        }
        return 6-dieRollToBlock;
    }
}
