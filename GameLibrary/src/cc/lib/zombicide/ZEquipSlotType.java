package cc.lib.zombicide;

public enum ZEquipSlotType {
    HAND, BODY, BACKPACK;

    public boolean canEquip() {
        return this != BACKPACK;
    }
}
