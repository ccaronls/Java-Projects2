package cc.lib.dungeondice;

import cc.lib.game.APGraphics;
import cc.lib.ui.UIComponent;
import cc.lib.ui.UIRenderer;

public class UIBoardRenderer extends UIRenderer {

    UIBoardRenderer(UIComponent component) {
        super(component);
    }

    DBoard board = null;

    @Override
    public void draw(APGraphics g, int px, int py) {
        if (board != null) {
            board.drawCells(g, 1);
        }
    }
}