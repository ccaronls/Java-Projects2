package cc.lib.zombicide;

import cc.lib.annotation.Keep;

@Keep
public enum ZEquipSlotType {
    HAND, BODY, BACKPACK;

    public boolean canEquip() {
        return this != BACKPACK;
    }
}
