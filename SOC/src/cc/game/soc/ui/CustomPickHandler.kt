package cc.game.soc.ui;

import cc.lib.game.APGraphics;

/**
 * Created by chriscaron on 2/28/18.
 */

public interface CustomPickHandler extends PickHandler {

    /**
     * Return number of custom pickable elements
     * @return
     */
    int getNumElements();

    /**
     * Pick a custom element
     *
     * Example:
     *   for (int i : getNumElements())
     *      g.setName(i)
     *      g.vertex(...)
     *
     *   return b.pickPoints(g, 10);
     *
     * @param b
     * @param g
     * @param x
     * @param y
     * @return
     */
    int pickElement(UIBoardRenderer b, APGraphics g, int x, int y);

}
