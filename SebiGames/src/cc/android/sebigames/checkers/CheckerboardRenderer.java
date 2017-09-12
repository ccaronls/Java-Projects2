package cc.android.sebigames.checkers;

import java.util.*;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import cc.android.sebigames.checkers.ICheckerboard.Move;
import cc.android.sebigames.checkers.ICheckerboard.Piece;
import cc.lib.android.BaseRenderer;
import cc.lib.android.GL10Graphics;
import cc.lib.game.AAnimation;
import cc.lib.game.AColor;
import cc.lib.game.AGraphics;
import cc.lib.game.IVector2D;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.math.Bezier;
import cc.lib.math.Vector2D;

class CheckerboardRenderer extends BaseRenderer implements View.OnTouchListener {
	
	public CheckerboardRenderer(GLSurfaceView parent) {
		super(parent);
	}

	final String TAG = "CheckerboardRenderer";

	float touchX = -1, touchY = -1;
	int touchRank, touchColumn;
	long downTime = 0;
	long frameCount = 0;

	LinkedList<AAnimation> animations = new LinkedList<AAnimation>();
	
	int DIR_BLACK = -1;
	int DIR_RED   = 1;
	
	int getDir(int playerNum) {
		switch (playerNum) {
			case 0:
    			return DIR_BLACK;
    		case 1:
    			return DIR_RED;
		}
		return 0;
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		
		if (animations.size() > 0)
			return false;
		
		ICheckerboard checkers = CheckerboardActivity.game;
		if (checkers == null)
			return false;
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
	
	IVector2D [] computeJumpPoints(Move move) {
		
		float sx = glCellWidth * move.startCol + glCellWidth/2;
		float sy = glCellWidth * move.startRank + glCellHeight/2;
		float ex = glCellHeight * move.endCol + glCellWidth/2;
		float ey = glCellHeight * move.endRank + glCellHeight/2;
		
		float midx1 = sx + ((ex-sx) / 3);
		float midx2 = sx + ((ex-sx) * 2 / 3);
		float midy1 = sy + ((ey-sy) / 3);
		float midy2 = sy + ((ey-sy) * 2 / 3);
		float dist = glCellHeight * getDir(move.playerNum);
		IVector2D [] v = {
				new Vector2D(sx, sy),
				new Vector2D(midx1, midy1+dist),
				new Vector2D(midx2, midy2+dist),
				new Vector2D(ex, ey),
		};
		return v;
	}
	
	class StackAnim extends AAnimation {
		final Move move;
		final int stacks;
		final int playerNum;
		
		public StackAnim(long duration, int maxRepeats, Move move, int playerNum) {
			super(duration, maxRepeats);
			this.move = move;
			this.playerNum = playerNum;
			stacks = CheckerboardActivity.game.getPiece(move.startRank, move.startCol).stacks;
		}

		@Override
		protected void onDone() {
			Piece p = CheckerboardActivity.game.getPiece(move.startRank, move.startCol);
			p.stacks = stacks;
			moves = CheckerboardActivity.game.executeMove(move);
		}

		@Override
		protected void onStarted() {
			//Piece p = CheckerboardActivity.game.getPiece(move.startRank, move.startCol);
			//p.stacks = 0;
		}

		@Override
		public void draw(AGraphics g, float position, float dt) {
			float sx = glCellWidth * move.startCol + glCellWidth/2;
			float sy = 0;//glCellHeight * move.startRank + glCellHeight/2;
			float ex = glCellWidth * move.startCol + glCellWidth/2;
			float ey = glCellHeight * move.startRank + glCellHeight/2;
			float scale = 1 + (1-position);
			int x = Math.round(sx + (ex-sx) * position);
			int y = Math.round(sy + (ey-sy) * position);
			Piece p = CheckerboardActivity.game.getPiece(move.startRank, move.startCol);
			g.pushMatrix();
			g.translate(x, y);
			g.scale(scale);
			drawChecker(g, 0, 0, glPieceRad, stacks, playerNum, getPieceColor(playerNum, g));
			g.popMatrix();
		}

	}

	
	class SlideAnim extends AAnimation {
		final Move move;
		final int stacks;
		final int playerNum;
		
		public SlideAnim(long duration, int maxRepeats, Move move, int playerNum) {
			super(duration, maxRepeats);
			this.move = move;
			this.playerNum = playerNum;
			stacks = CheckerboardActivity.game.getPiece(move.startRank, move.startCol).stacks;
		}

		@Override
		protected void onDone() {
			Piece p = CheckerboardActivity.game.getPiece(move.startRank, move.startCol);
			p.stacks = stacks;
			moves = CheckerboardActivity.game.executeMove(move);
		}

		@Override
		protected void onStarted() {
			Piece p = CheckerboardActivity.game.getPiece(move.startRank, move.startCol);
			p.stacks = 0;
		}

		@Override
		public void draw(AGraphics g, float position, float dt) {
			float sx = glCellWidth * move.startCol + glCellWidth/2;
			float sy = glCellHeight * move.startRank + glCellHeight/2;
			float ex = glCellWidth * move.endCol + glCellWidth/2;
			float ey = glCellHeight * move.endRank + glCellHeight/2;
			int x = Math.round(sx + (ex-sx) * position);
			int y = Math.round(sy + (ey-sy) * position);
			Piece p = CheckerboardActivity.game.getPiece(move.startRank, move.startCol);
			drawChecker(g, x, y, glPieceRad, stacks, playerNum, getPieceColor(playerNum, g));
		}

	}
	
	class JumpAnim extends AAnimation {

		final Bezier curve;
		final Move move;
		final int stacks;
		final int playerNum;
		
		public JumpAnim(long duration, int maxRepeats, Move move, int playerNum) {
			super(duration, maxRepeats);
			this.move = move;
			this.playerNum = playerNum;
			curve = new Bezier(computeJumpPoints(move));
			stacks = CheckerboardActivity.game.getPiece(move.startRank, move.startCol).stacks;
		}

		@Override
		protected void onDone() {
			Piece p = CheckerboardActivity.game.getPiece(move.startRank, move.startCol);
			p.stacks = stacks;
			moves = CheckerboardActivity.game.executeMove(move);
		}

		@Override
		protected void onStarted() {
			Piece p = CheckerboardActivity.game.getPiece(move.startRank, move.startCol);
			p.stacks = 0;
		}

		@Override
		public void draw(AGraphics g, float position, float dt) {
			Vector2D v = curve.getPointAt(position);
			Piece p = CheckerboardActivity.game.getPiece(move.startRank, move.startCol);
			drawChecker(g, v.Xi(), v.Yi(), glPieceRad, stacks, playerNum, getPieceColor(playerNum, g));
		}
		
	};
	
	void onTap() {
		ICheckerboard checkers = CheckerboardActivity.game;
		if (checkers.isOnBoard(touchColumn, touchColumn)) {
			if (moves.size() == 0)
				moves = checkers.computeMoves(touchRank, touchColumn);
			else {
				for (Move m : moves) {
					int tr = m.endRank;
					int tc = m.endCol;
					if (tr == touchRank && tc == touchColumn) {
						switch (m.type) {
							case JUMP:
							case JUMP_CAPTURE:
								addAnimation(new JumpAnim(1000, 0, m, checkers.getCurPlayerNum()));
								break;
							case SLIDE:
								addAnimation(new SlideAnim(500, 0, m, checkers.getCurPlayerNum()));
								break;
							case STACK:
								addAnimation(new StackAnim(800, 0, m, checkers.getCurPlayerNum()));
								break;
						}
						return;
					}
				}
				moves.clear();
			}
		}
	}
	
	private void addAnimation(AAnimation a) {
		synchronized (animations) {
			animations.add(a);
		}
		a.start();
	}
	
	int glWidth = 0;
	int glHeight = 0;
	int glCellWidth = 0;
	int glCellHeight = 0;
	int glPieceRad = 0;
	
	@Override
	protected void drawFrame(GL10Graphics g) {
		ICheckerboard checkers = CheckerboardActivity.game;
		
		final int RANKS = checkers.getRanks();
		final int COLUMNS = checkers.getColumns();
		
		g.pushMatrix();
		g.clearScreen(g.DARK_GRAY);

		glWidth = g.getViewportWidth();
		glHeight = g.getViewportHeight();
		glCellWidth = glWidth/COLUMNS;
		glCellHeight = glHeight/RANKS;
		glPieceRad = Math.min(glCellHeight/3, glCellHeight/3);
		for (int rank=0; rank<RANKS; rank++) {
			g.setColor(g.ORANGE);
			for (int col=rank%2; col<COLUMNS; col+=2) {
				g.drawFilledRect(col*glCellWidth, rank*glCellHeight, glCellWidth, glCellHeight);
			}
			for (int col=0; col<COLUMNS; col++) {

    			int rx = col*glCellWidth;
    			int ry = rank*glCellHeight;
				if (touchRank == rank && touchColumn == col) {
					float fadeSecs = 3;
					float alpha = (1.0f / (fadeSecs * 1000)) * (System.currentTimeMillis() - downTime);
    				if (alpha > 0) {
    					g.setColor(g.makeColor(0, 1, 0, alpha));
        				g.drawFilledRect(rx, ry, glCellWidth, glCellHeight);
    				}
				}
				
				Piece p = checkers.getPiece(rank, col);
				drawChecker(g, p, rank, col);
			}
		}
		
		if (animations.size() > 0) {
			synchronized (animations) {
    			Iterator<AAnimation> it = animations.iterator();
    			while (it.hasNext()) {
    				AAnimation a = it.next();
    				a.update(g);
    				if (a.isDone()) {
    					it.remove();
    				}
    			}
			}
		} else {
		
    		for (Move m : moves) {
    			int rank = m.endRank;
    			int col  = m.endCol;
    			int x = col * glCellWidth;
    			int y = rank * glCellHeight;
    			g.setLineWidth(3 + (frameCount / 10) % 5);
    //			float alpha = System.currentTimeMillis() - downTime;
    			g.setColor(g.GREEN);//.setAlpha(alpha));
    			g.drawRect(x,  y, glCellWidth, glCellHeight);
    		}
		}
		
		// Draw an indicator bar on side of the player whose turn it is.
		g.setColor(getPieceColor(checkers.getCurPlayerNum(), g));
		if (checkers.getCurPlayerNum() == 0) {
			g.drawFilledRect(0, glHeight-10, glWidth, 10);
		} else {
			g.drawFilledRect(0, 0, glWidth, 10);
		}
		
		if (animations == null)
		
		g.setColor(g.YELLOW);
		switch (checkers.getWinner()) {
			case 0: // BLACK
				//g.drawJustifiedString(w/2, h/2, Justify.CENTER, "BLACK WINS");
				break;
			case 1: // RED
				//g.drawJustifiedString(w/2, h/2, Justify.CENTER, "RED WINS");
				break;
		}
		
		g.popMatrix();
		frameCount ++;
	}
	
	AColor getPieceColor(int playerNum, AGraphics g) {
		switch (playerNum) {
			case 0:
				return g.BLUE;
			case 1:
				return g.RED;
			
		}
		return g.TRANSPARENT;
	}
	
	void drawChecker(AGraphics g, Piece p, int rank, int col) {
		int cx = col*glCellWidth + glCellWidth/2;
		int cy = rank*glCellHeight + glCellHeight/2;
		drawChecker(g, p, cx, cy, glPieceRad);
	}
	
	void drawChecker(AGraphics g, Piece p, int x, int y, int rad) {
		if (p.stacks == 0)
			return;
		for (int i=0; i<p.stacks; i++) {
			drawChecker(g, x, y, rad, p.stacks, p.playerNum, getPieceColor(p.playerNum, g));
			y -= rad/4;
		}
	}
	
	void drawChecker(AGraphics g, int x, int y, int rad, int stacks, int playerNum, AColor color) {
		if (stacks <= 0)
			return;
		AColor dark = color.darkened(0.5f);
		g.setColor(dark);
		final int step = rad/20 * getDir(playerNum);
		final int num = 5;
		for (int i=0; i<num; i++) {
			g.drawDisk(x/*+i*step*/, y+i*step, rad);
		}
		g.setColor(color);
		g.drawDisk(x/*+num*step*/, y+num*step, rad);
		
	}
	/*
	void drawChecker(AGraphics g, AColor colorTop, AColor color, int x, int y, int r) {
		g.setColor(color);
		final int step = r/20;
		final int num = 5;
		for (int i=0; i<num; i++) {
			g.drawDisk(x, y+i*step, r);
		}
		g.setColor(colorTop);
		g.drawDisk(x, y+num*step, r);
	}*/

	@Override
	protected void init(GL10Graphics g) {
		g.ortho();
		setDrawFPS(false);
		CheckerboardActivity.game.newGame();
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