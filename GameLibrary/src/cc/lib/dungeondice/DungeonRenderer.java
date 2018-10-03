package cc.lib.dungeondice;

import cc.lib.game.APGraphics;
import cc.lib.ui.UIComponent;
import cc.lib.ui.UIRenderer;

public class DungeonRenderer extends UIRenderer {

    DDungeon dungeon;

    public DungeonRenderer(UIComponent component) {
        super(component);
    }

    @Override
    public void draw(APGraphics g, int px, int py) {
        dungeon.draw(g);
    }
}
