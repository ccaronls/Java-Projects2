package cc.lib.zombicide;

import cc.lib.annotation.Keep;
import cc.lib.game.Utils;
import cc.lib.ui.IButton;

/**
 * Created by Chris Caron on 8/28/21.
 */
@Keep
public enum ZEquipmentClass implements IButton {
    THROWABLE,
    CONSUMABLE,
    DAGGER,
    SWORD,
    AXE,
    BOW,
    CROSSBOW,
    MAGIC,
    ENCHANTMENT,
    ARMOR,
    SHIELD;


    @Override
    public String getTooltipText() {
        return null;
    }

    @Override
    public String getLabel() {
        return Utils.toPrettyString(name());
    }
}
