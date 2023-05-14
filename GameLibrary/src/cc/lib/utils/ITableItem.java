package cc.lib.utils;

import cc.lib.game.AGraphics;
import cc.lib.game.IDimension;
import cc.lib.game.IMeasurable;

public interface ITableItem extends IMeasurable {
    IDimension draw(AGraphics g);

    int getBorderWidth();
}
