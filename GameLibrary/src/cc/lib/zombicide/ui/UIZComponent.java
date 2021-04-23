package cc.lib.zombicide.ui;

import cc.lib.game.AGraphics;
import cc.lib.ui.UIComponent;
import cc.lib.zombicide.ZTile;

public interface UIZComponent<T extends AGraphics> extends UIComponent {

    void loadTiles(T g, ZTile[] tiles);

}
