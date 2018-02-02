package cc.game.dominos.swing;

import java.util.ArrayList;
import java.util.List;

import cc.game.dominos.core.*;
import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.EZFrame;
import cc.lib.swing.KeyboardAnimationApplet;

public class DominosApplet extends KeyboardAnimationApplet {

    public static void main(String [] args) {
        AGraphics.DEBUG_ENABLED = true;
        Utils.DEBUG_ENABLED = true;
        EZFrame frame = new EZFrame("Dominos");
        KeyboardAnimationApplet app = new DominosApplet();
        frame.add(app);
        frame.centerToScreen(800, 600);
        app.init();
        app.start();
        app.setTargetFPS(30);
    }

    DominosBoard b = new DominosBoard();

	@Override
	protected void doInitialization() {
		// TODO Auto-generated method stub
		b.placeRootPiece(new Piece(6, 6));

		Piece p = new Piece(6, 5);
		b.doMove(b.findMovesForPiece(p).get(0));
        p = new Piece(6, 4);
        b.doMove(b.findMovesForPiece(p).get(0));
        p = new Piece(6, 3);
        b.doMove(b.findMovesForPiece(p).get(0));
        p = new Piece(6, 2);
        b.doMove(b.findMovesForPiece(p).get(0));
        //pcs.add(new Piece(6, 1));
//*/
	}

	@Override
	protected void drawFrame(AGraphics g) {
		// TODO Auto-generated method stub
        g.ortho();
        g.setIdentity();
        g.clearScreen(g.GREEN);
		b.draw(g);
	}

	@Override
	protected void onDimensionsChanged(AGraphics g, int width, int height) {
		// TODO Auto-generated method stub
		
	}

}
