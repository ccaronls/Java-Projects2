package cc.lib.zombicide;

import cc.lib.game.AGraphics;

public interface ZTiles {

    int [] loadTiles(AGraphics g, String [] names, int [] orientations);

    int getImage(Object obj);
}
