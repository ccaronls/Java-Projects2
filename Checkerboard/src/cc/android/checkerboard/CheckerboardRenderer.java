package cc.android.checkerboard;

import java.util.*;

import org.junit.runner.Computer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import cc.android.checkerboard.Checkers.*;
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
	int touchRank, touchColumn;
	long downTime = 0;
	long frameCount = 0;
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				downTime = System.currentTimeMillis();
			case MotionEvent.ACTION_MOVE:
				touchX = event.getX();
				touchY = event.getY();
				touchRank = (int)(touchY * 8 / getGraphics().getViewportHeight());
				touchColumn = (int)(touchX * 8 / getGraphics().getViewportWidth());
				break;
			case MotionEvent.ACTION_UP:
				if (System.currentTimeMillis() - downTime < 3000) {
					onTap();
				}
				touchX = touchY = -1;
				break;
		}
		return true;
	}

	List<Move> moves = new ArrayList<>();
	
	void onTap() {
		if (checkers.isOnBoard(touchColumn, touchColumn)) {
			if (moves.size() == 0)
				moves = checkers.computeMovesForSquare(touchRank, touchColumn);
			else {
				for (Move m : moves) {
					int tr = m.startRank + m.dRank;
					int tc = m.startCol  + m.dCol;
					if (tr == touchRank && tc == touchColumn) {
						moves = checkers.executeMove(m);
						return;
					}
				}
				moves.clear();
			}
		}
	}
	
	@Override
	protected void drawFrame(GL10Graphics g) {
		
		switch (checkers.getWinner()) {
			case BLACK:
				checkers.setup();
				new AlertDialog.Builder(getContext()).setMessage("BLACK os the winner!").setNegativeButton("Ok", null).show();
				break;
			case NONE:
				break;
			case RED:
				checkers.setup();
				new AlertDialog.Builder(getContext()).setMessage("BLACK os the winner!").setNegativeButton("Ok", null).show();
				break;
		}
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

    			int rx = col*w8;
    			int ry = rank*h8;
				if (touchRank == rank && touchColumn == col) {
					float fadeSecs = 3;
					float alpha = (1.0f / (fadeSecs * 1000)) * (System.currentTimeMillis() - downTime);
    				if (alpha > 0) {
    					g.setColor(g.makeColor(0, 1, 0, alpha));
        				g.drawFilledRect(rx, ry, w8, h8);
    				}
				}
				
				Piece p = checkers.getBoard(rank, col);
				int x = col*w8 + w8/2;
				int y = rank*h8 + h8/2;
				drawChecker(g, p, x, y, rad);
			}
		}
		
		for (Move m : moves) {
			int rank = m.startRank + m.dRank;
			int col  = m.startCol  + m.dCol;
			int x = col * w8;
			int y = rank * h8;
			g.setLineWidth(3 + (frameCount / 10) % 5);
//			float alpha = System.currentTimeMillis() - downTime;
			g.setColor(g.GREEN);//.setAlpha(alpha));
			g.drawRect(x,  y, w8, h8);
		}

		
		g.setColor(getPieceColor(checkers.getTurnColor(), g));
		if (checkers.getTurnColor() == PieceColor.BLACK) {
			g.drawFilledRect(0, h-10, w, 10);
		} else {
			g.drawFilledRect(0, 0, w, 10);
		}
		
		g.popMatrix();
		frameCount ++;
	}
	
	AColor getPieceColor(PieceColor p, AGraphics g) {
		switch (p) {
			case BLACK:
				return g.BLUE;
			case RED:
				return g.RED;
			
		}
		return g.TRANSPARENT;
	}
	
	void drawChecker(AGraphics g, Piece p, int x, int y, int rad) {
		if (p == Piece.EMPTY)
			return;
		AColor high = getPieceColor(p.color, g);
		AColor dark = high.darkened(0.5f);
		if (p.isKing) {
			drawChecker(g, high, dark, x, y, rad);
			y += rad/4;
		}
		drawChecker(g, high, dark, x, y, rad);
	}
	
	void drawChecker(AGraphics g, AColor colorTop, AColor color, int x, int y, int r) {
		g.setColor(color);
		final int step = r/20;
		final int num = 5;
		for (int i=0; i<num; i++) {
			g.drawDisk(x/*+i*step*/, y+i*step, r);
		}
		g.setColor(colorTop);
		g.drawDisk(x/*+num*step*/, y+num*step, r);
	}

	@Override
	protected void init(GL10Graphics g) {
		g.ortho();//0, g.getViewportWidth(), g.getViewportHeight(), 0);
		setDrawFPS(false);
		checkers.setup();
	}
	
	public int getRotation(){
		final int rotation = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getOrientation();
		switch (rotation) {
			case Surface.ROTATION_0:
				return 0;
			case Surface.ROTATION_90:
				return 90;
			case Surface.ROTATION_180:
				return 180;
			default:
				return 270;
		}
	}
	
}