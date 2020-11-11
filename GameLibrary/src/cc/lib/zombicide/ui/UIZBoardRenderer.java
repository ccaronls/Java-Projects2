package cc.lib.zombicide.ui;

import cc.lib.game.APGraphics;
import cc.lib.ui.UIComponent;
import cc.lib.ui.UIRenderer;
import cc.lib.zombicide.ZBoard;

public class UIZBoardRenderer extends UIRenderer {

    public ZBoard board;

    public UIZBoardRenderer(UIComponent component) {
        super(component);
    }

    @Override
    public void draw(APGraphics g, int px, int py) {
        board.drawDebug(g, px, py);
    }
}
