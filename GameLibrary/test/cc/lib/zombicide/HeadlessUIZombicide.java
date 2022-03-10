package cc.lib.zombicide;

import cc.lib.game.AGraphics;
import cc.lib.math.Vector2D;
import cc.lib.ui.UIComponent;
import cc.lib.ui.UIRenderer;
import cc.lib.zombicide.ui.UIZBoardRenderer;
import cc.lib.zombicide.ui.UIZCharacterRenderer;
import cc.lib.zombicide.ui.UIZComponent;
import cc.lib.zombicide.ui.UIZombicide;

/**
 * Created by Chris Caron on 3/7/22.
 */
class HeadlessUIZombicide extends UIZombicide {
    public HeadlessUIZombicide() {
        super(new UIZCharacterRenderer(new UIComponent() {
            @Override
            public int getWidth() {
                return 1000;
            }

            @Override
            public int getHeight() {
                return 200;
            }

            @Override
            public void redraw() {

            }

            @Override
            public void setRenderer(UIRenderer r) {

            }

            @Override
            public Vector2D getViewportLocation() {
                return Vector2D.ZERO;
            }
        }), new UIZBoardRenderer(new UIZComponent() {
            @Override
            public void loadTiles(AGraphics g, ZTile[] tiles) {

            }

            @Override
            public int getWidth() {
                return 1000;
            }

            @Override
            public int getHeight() {
                return 800;
            }

            @Override
            public void redraw() {

            }

            @Override
            public void setRenderer(UIRenderer r) {

            }

            @Override
            public Vector2D getViewportLocation() {
                return Vector2D.ZERO;
            }
        }));
    }


}
