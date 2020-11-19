package cc.lib.zombicide;

import cc.lib.game.Utils;
import cc.lib.ui.IButton;

public interface ZEquipmentType<T extends ZEquipment> extends IButton {

    T create();

    String name();

    @Override
    default String getLabel() {
        return Utils.toPrettyString(name());
    }

    @Override
    default String getTooltipText() {
        return Utils.toPrettyString(name());
    }


}
