package cc.lib.zombicide;

public enum ZSpell implements ZEquipment {
    HEALING
    ;


    @Override
    public ZEquipSlot getSlot() {
        return ZEquipSlot.HAND;
    }
}
