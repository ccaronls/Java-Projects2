package cc.lib.zombicide;

import cc.lib.game.Utils;
import cc.lib.ui.IButton;

public enum ZEquipSlot implements IButton {
    LEFT_HAND("LH"),
    BODY("Bo"),
    RIGHT_HAND("RH"),
    BACKPACK("BP");

    ZEquipSlot(String shorthand) {
        this.shorthand = shorthand;
    }

    final String shorthand;

    @Override
    public String getTooltipText() {
        return null;
    }

    @Override
    public String getLabel() {
        return Utils.getPrettyString(name());
    }
}
