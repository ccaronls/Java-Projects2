package cc.lib.zombicide;

import cc.lib.game.AGraphics;

public interface ZTiles<T extends AGraphics> {

    int [] loadTiles(T g, String [] names, int [] orientations);
}
