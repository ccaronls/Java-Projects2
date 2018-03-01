package cc.game.soc.ui;

import cc.lib.game.APGraphics;

/**
 * Created by chriscaron on 2/27/18.
 */

public interface UIRenderer {
    void draw(APGraphics g, int px, int py);

    void doClick();
}
