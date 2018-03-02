package cc.game.soc.ui;

import cc.lib.game.APGraphics;

/**
 * Created by chriscaron on 2/28/18.
 */

public interface PickHandler {

    PickMode getPickMode();

    /**
     * Called when mouse pressed on a pickable element
     * @param pickedValue
     */
    void onPick(UIBoardRenderer b, int pickedValue);

    /**
     * Called when rendering an index that passes the isPickableIndex test
     *
     * @param b
     * @param g
     * @param index
     */
    void onDrawPickable(UIBoardRenderer b, APGraphics g, int index);

    /**
     * Called after tiles, edges and verts are rendered for pick handler to render it own stuff
     *
     * @param b
     * @param g
     */
    void onDrawOverlay(UIBoardRenderer b, APGraphics g);

    /**
     * Render a highlighted index
     *
     * @param b
     * @param g
     * @param highlightedIndex
     */
    void onHighlighted(UIBoardRenderer b, APGraphics g, int highlightedIndex);

    /**
     * Return whether the index is pickable
     * @param index
     * @return
     */
    boolean isPickableIndex(UIBoardRenderer b, int index);
}