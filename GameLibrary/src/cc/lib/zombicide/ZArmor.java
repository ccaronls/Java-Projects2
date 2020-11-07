package cc.lib.zombicide;

public enum ZArmor implements ZEquipment {

    LEATHER(5),
    CHAIN(4),
    PLATE(3),
    SHIELD(3),
    DWARVEN_SHIELD(4), // protects against abomination TODO
    SHIELD_OF_AGES(4), // gain the shove skill TODO
    ;

    ZArmor(int dieRollToBlock) {
        this.dieRollToBlock = dieRollToBlock;
    }

    final int dieRollToBlock;

    @Override
    public ZEquipSlot getSlot() {
        switch (this) {
            case SHIELD:
                return ZEquipSlot.HAND;
        }
        return ZEquipSlot.BODY;
    }

    @Override
    public boolean canOpenDoor() {
        return false;
    }
}
