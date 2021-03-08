package cc.lib.zombicide.ui;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.ui.IButton;
import cc.lib.utils.Table;

public interface UIZButton extends IButton {

    default void onClick() {
        UIZombicide.getInstance().setResult(this);
    }

    GRectangle getRect();

    default Table getInfo(AGraphics g, int width, int height) { return null; }

    /**
     * Higher numbers have priority when buttons overlapp
     *
     * @return
     */
    default int getZOrder() { return 0; }
}
