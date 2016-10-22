package cc.android.checkerboard;

import java.util.*;

import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import cc.lib.android.BaseRenderer;
import cc.lib.android.GL10Graphics;
import cc.lib.game.AColor;
import cc.lib.game.AGraphics;
import cc.lib.game.IVector2D;
import cc.lib.game.Maze;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;

class CheckerboardRenderer extends BaseRenderer implements View.OnTouchListener {

	
	public CheckerboardRenderer(GLSurfaceView parent) {
		super(parent);
	}

	final String TAG = "CheckerboardRenderer";

	Checkers checkers = new Checkers();
	
	float touchX = -1, touchY = -1;
	int grabRank = 0, grabCol = 0;
	boolean grabbed = false;
	
	void tryGrab() {
		if (!grabbed) {
			int cx = Math.round(touchX * 8 / getGraphics().getViewportWidth());
			int cy = Math.round(touchY * 8 / getGraphics().getViewportHeight());
			if (cx >= 0 && cy >= 0 && cx < 8 && cy < 8) {
				int color = checkers.board[cy][cx];
				if (color < 0 && checkers.getTurnColor() < 0) {
					grabRank = cy;
					grabCol = cx;
					grabbed = true;
				} else if (color > 0 && checkers.getTurnColor() > 0) {
					grabRank = cy;
					grabCol = cx;
					grabbed = true;
				}
			}
		}
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				touchX = event.getX();
				touchY = event.getY();
				break;
			case MotionEvent.ACTION_UP:
				touchX = touchY = -1;
				grabbed = false;
				break;
		}
		return true;
	}

	@Override
	protected void drawFrame(GL10Graphics g) {
		g.pushMatrix();
		g.clearScreen(g.DARK_GRAY);

		int w = g.getViewportWidth();
		int h = g.getViewportHeight();
		int w8 = w/8;
		int h8 = h/8;
		final int rad = Math.min(w8/3, h8/3);
		for (int rank=0; rank<8; rank++) {
			g.setColor(g.ORANGE);
			for (int col=rank%2; col<8; col+=2) {
				g.drawFilledRect(col*w8, rank*h8, w8, h8);
			}
			for (int col=0; col<8; col++) {
				
				if (grabbed && grabRank == rank && grabCol == col) {
					continue;
				}
				
    			int rx = col*w8;
    			int ry = rank*h8;
    			if (Utils.isPointInsideRect(touchX, touchY, rx, ry, w8, h8)) {
    				g.setColor(g.GREEN);
    				g.drawFilledRect(rx, rx, w8, h8);
    			}

				int c = checkers.board[rank][col];
				if (c<0) {
					// black
					drawChecker(g, g.BLUE, g.BLUE.darkened(0.5f), col*w8+w8/2, rank*h8+h8/2, rad);
				}
				if (c<-1) {
					// black king
					drawChecker(g, g.BLUE, g.BLUE.darkened(0.5f), col*w8+w8/2+20, rank*h8+h8/2+20, rad);
				}
				if(c > 0) {
					// red
					drawChecker(g, g.RED, g.RED.darkened(0.5f), col*w8+w8/2, rank*h8+h8/2, rad);
				}
				if (c > 1) {
					// red king
					drawChecker(g, g.RED, g.RED.darkened(0.5f), col*w8+w8/2+20, rank*h8+h8/2+20, rad);
				}
			}
		}
		
		if (grabbed) {
			int color = checkers.board[grabRank][grabCol];
			drawChecker(g, color, (int)touchX, (int)touchY, rad);
		}
		
		g.popMatrix();
	}
	
	void drawChecker(AGraphics g, int c, int x, int y, int r) {
		if (c<0) {
			// black
			drawChecker(g, g.BLUE, g.BLUE.darkened(0.5f), x, y, r);
		}
		if (c<-1) {
			// black king
			drawChecker(g, g.BLUE, g.BLUE.darkened(0.5f), x, y, r);
		}
		if(c > 0) {
			// red
			drawChecker(g, g.RED, g.RED.darkened(0.5f), x, y, r);
		}
		if (c > 1) {
			// red king
			drawChecker(g, g.RED, g.RED.darkened(0.5f), x, y, r);
		}		
	}
	
	void drawChecker(AGraphics g, AColor colorTop, AColor color, int x, int y, int r) {
		g.setColor(color);
		final int step = r/20;
		final int num = 5;
		for (int i=0; i<num; i++) {
			g.drawDisk(x+i*step, y+i*step, r);
		}
		g.setColor(colorTop);
		g.drawDisk(x+num*step, y+num*step, r);
	}

	@Override
	protected void init(GL10Graphics g) {
		g.ortho();
		setDrawFPS(false);
		checkers.setup();
	}
	
	
}