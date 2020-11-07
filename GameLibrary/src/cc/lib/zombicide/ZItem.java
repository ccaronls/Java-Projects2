package cc.lib.zombicide;

public enum ZItem implements ZEquipment {
    TORCH,
    DRAGON_BILE,
    WATER,
    SALTED_MEAT,
    APPLES,
    PLENTY_OF_ARROWS, // user can reroll ranged arrow attacks if they want
    PLENTY_OF_BOLTS,
    ;


    @Override
    public ZEquipSlot getSlot() {
        switch (this) {
            case WATER:
            case SALTED_MEAT:
            case APPLES:
                return ZEquipSlot.BACKPACK;
        }
        return ZEquipSlot.HAND;
    }

    @Override
    public boolean canOpenDoor() {
        return false;
    }
}
