package cc.game.soc.swing;

import cc.game.soc.ui.UIBoard;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTGraphics;

/**
 * Created by chriscaron on 2/22/18.
 */

public abstract class UIBoardComponent extends AWTComponent {

    UIBoardComponent() {
        super(true);
    }

    private final UIBoard board = new UIBoard() {
        @Override
        protected GColor getPlayerColor(int playerNum) {
            GColor c = UIBoardComponent.this.getPlayerColor(playerNum);
            return new GColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
        }

        @Override
        protected void initAssets(AGraphics g) {
            desertImage  = g.loadImage("desert.GIF", GColor.WHITE);
            woodImage    = g.loadImage("wood.GIF",   GColor.WHITE);
            wheatImage   = g.loadImage("wheat.GIF",  GColor.WHITE);
            oreImage     = g.loadImage("ore.GIF",    GColor.WHITE);
            brickImage   = g.loadImage("brick.GIF",  GColor.WHITE);
            sheepImage   = g.loadImage("sheep.GIF",  GColor.WHITE);
            waterImage   = g.loadImage("water.GIF",  GColor.WHITE);
            robberImage  = g.loadImage("robber.GIF", GColor.WHITE);
            pirateImage	 = g.loadImage("pirate.GIF");
            goldImage    = g.loadImage("gold.GIF");

            mountainshexImage 	= g.loadImage("mountainshex.GIF");
            hillshexImage 		= g.loadImage("hillshex.GIF");
            pastureshexImage 	= g.loadImage("pastureshex.GIF");
            fieldshexImage 		= g.loadImage("fieldshex.GIF");
            foresthexImage 		= g.loadImage("foresthex.GIF");

            undiscoveredImage = g.loadImage("undiscoveredtile.GIF");

            cardFrameImage = g.loadImage("cardFrame.GIF", GColor.WHITE);
            knightImages[0] = g.loadImage("knight_basic_inactive.GIF");
            knightImages[1] = g.loadImage("knight_basic_active.GIF");
            knightImages[2] = g.loadImage("knight_strong_inactive.GIF");
            knightImages[3] = g.loadImage("knight_strong_active.GIF");
            knightImages[4] = g.loadImage("knight_mighty_inactive.GIF");
            knightImages[5] = g.loadImage("knight_mighty_active.GIF");
        }

        @Override
        protected void repaint() {
            UIBoardComponent.this.repaint();
        }

        @Override
        public int getViewportWidth() {
            return UIBoardComponent.this.getViewportWidth();
        }

        @Override
        public int getViewportHeight() {
            return UIBoardComponent.this.getViewportHeight();
        }
    };

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        board.draw(g, mouseX, mouseY);
    }

    protected abstract GUIProperties getProperties();

    protected abstract GColor getPlayerColor(int playerNum);

    UIBoard getBoard() {
        return board;
    }

    @Override
    protected void onClick() {
        board.onClick();
    }
}
