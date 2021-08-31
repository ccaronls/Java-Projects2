package cc.lib.zombicide;

import cc.lib.annotation.Keep;
import cc.lib.game.Utils;
import cc.lib.ui.IButton;

@Keep
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
        return Utils.toPrettyString(name());
    }

    public static ZEquipSlot [] wearableValues() {
        return Utils.toArray(LEFT_HAND, BODY, RIGHT_HAND);
    }
}
