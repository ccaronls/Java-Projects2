package cc.game.rocketman.swing;

import java.awt.Color;
import java.io.File;

import cc.game.rocketman.core.RocketManLevel;
import cc.game.rocketman.core.RocketManLevel.CellType;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTKeyboardAnimationApplet;

public class LevelBuilder extends AWTKeyboardAnimationApplet {

	public static void main(String[] args) {
        Utils.setDebugEnabled();
        final LevelBuilder app = new LevelBuilder();
        AWTFrame frame = new AWTFrame("Animation Maker") {
        	
        	@Override
			protected void onWindowClosing() {
        		try {
        			app.level.saveToFile(new File("level.txt"));
        		} catch (Exception e) {
        			e.printStackTrace();
        		}
			}

        };
        frame.add(app);
        frame.centerToScreen(640+6, 480+6);
        app.init();
        app.start();
        app.focusGained(null);
    }
	
	
	RocketManLevel level = new RocketManLevel();
	
	@Override
	protected void doInitialization() {
		try {
			level.loadFromFile(new File("level.txt"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void drawFrame(AGraphics g) {
		final int cw = level.getCellWidth();
		final int ch = level.getCellHeight(); 
		
		final int width = cw * level.getNumCellColumns();
		final int height = ch * level.getNumCellRows();
		
		g.ortho(0, width+1, height+1, 0);
		g.setIdentity();
		
		g.clearScreen(GColor.BLACK);
		g.setColor(GColor.WHITE);
		g.setLineWidth(1);
		
		for (int i=0; i<=level.getNumCellColumns(); i++) {
			int x = i*cw;
			g.drawLine(x, height, x, 0);
		}
		
		for (int i=0; i<=level.getNumCellRows(); i++) {
			int y = i*ch;
			g.drawLine(0, y, width, y);
		}

		for (int i=0; i<level.getNumCellColumns(); i++) {
			for (int ii=0; ii<level.getNumCellRows(); ii++) {
				int x = i*cw;
				int y = ii*ch;
				switch (level.getCellType(i, ii)) {
				case BLOCK:
					g.setColor(GColor.DARK_GRAY);
					g.drawFilledRect(x, y, cw, ch);
					break;
				case NONE:
					break;
				}
			}
		}		
		
		Vector2D v = g.screenToViewport(getMouseX(), getMouseY());
		int col = (int)Math.floor(v.X() / 10);
		int row = (int)Math.floor(v.Y() / 10);
		g.setColor(GColor.RED);
		g.drawRect(col*10, row*10, 10, 10);
		
		if (getKeyboard('b')) {
			level.setCell(col, row, CellType.BLOCK);
		}
	}

	@Override
	protected void onDimensionsChanged(AGraphics g, int width, int height) {
		// TODO Auto-generated method stub
		
	}

}
