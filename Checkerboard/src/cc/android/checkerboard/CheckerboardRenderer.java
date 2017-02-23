package cc.android.checkerboard;

import java.util.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import cc.android.checkerboard.ICheckerboard.Piece;
import cc.android.checkerboard.ICheckerboard.*;
import cc.lib.android.BaseRenderer;
import cc.lib.android.GL10Graphics;
import cc.lib.game.AAnimation;
import cc.lib.game.AColor;
import cc.lib.game.AGraphics;
import cc.lib.game.IVector2D;
import cc.lib.game.Justify;
import cc.lib.math.Bezier;
import cc.lib.math.CMath;
import cc.lib.math.Vector2D;

class CheckerboardRenderer extends BaseRenderer implements View.OnTouchListener {
	
	public CheckerboardRenderer(GLSurfaceView parent) {
		super(parent);
	}

	ICheckerboard getGame() {
		return CheckerboardActivity.game;
	}

	final String TAG = "CheckerboardRenderer";

	/*
	float touchX = -1, touchY = -1;
	float grabRank = -1, grabColumn = -1;
	int touchRank, touchColumn;
	long downTime = 0;*/
	long frameCount = 0;
	final List<Move> moves = new ArrayList<Move>();
	
	int glWidth = 0;
	int glHeight = 0;
	int glCellWidth = 0;
	int glCellHeight = 0;
	int glPieceRad = 0;


	final int DIR_BLACK = -1;
	final int DIR_RED   = 1;
	
	int getDir(int playerNum) {
		switch (playerNum) {
			case 0:
    			return DIR_BLACK;
    		case 1:
    			return DIR_RED;
		}
		return 0;
	}

	// these are animations initiated by 
	LinkedList<ACBAnimation> blockingAnimations = new LinkedList<ACBAnimation>();

	private void addBlockingAnimation(ACBAnimation a) {
		synchronized (blockingAnimations) {
			blockingAnimations.add(a);
		}
		a.start();
	}

	private <T extends AAnimation>void renderAnimations(AGraphics g, Collection<T> animations) {
		if (animations.size() > 0) {
			synchronized (animations) {
    			Iterator<T> it = animations.iterator();
    			while (it.hasNext()) {
    				AAnimation a = it.next();
    				if (a.update(g)) {
    					it.remove();
    				}
    			}
			}
		} 
	}
	
	int grabRank = -1, grabColumn = -1;
	int touchX=0, touchY=0;
	int touchRank = -1, touchColumn = -1;
	boolean touching = false;
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		
		if (blockingAnimations.size() > 0)
			return false;

		touchX = (int)event.getX();
		touchY = (int)event.getY();
		touchRank = Math.round(event.getY() / glCellHeight);
		touchColumn = Math.round(event.getX() / glCellWidth);
		
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN: {
				if (getGame().isOnBoard(touchRank, touchColumn) && moves.size() == 0) {
					moves.addAll(getGame().computeMoves(touchRank, touchColumn));
					if (moves.size() > 0) {
						touching = true;
						grabRank = touchRank;
						grabColumn = touchColumn;
						Piece p = getGame().getPiece(grabRank, grabColumn);
						p.data = new CheckerRenderer() {
							
							@Override
							public void render(AGraphics g, int rank, int col, Piece p) {
								if (!touching) {
									p.data = null;
									return;
								}
								for (Move m : moves) {
									if (m.endRank == touchRank && m.endCol == touchColumn) {
										drawChecker(g, p, touchRank, touchColumn);
										return;
									}
								}
								
								drawChecker(g, p, touchX, touchY, glPieceRad, getPieceColor(p.playerNum, g));
							}
						};
					} else {
						touching = false;
					}
				}
				break;
			}
			case MotionEvent.ACTION_MOVE: {
				break;
			}
			case MotionEvent.ACTION_UP: {
				if (touching) {
					for (Move m : moves) {
						if (m.endRank == touchRank && m.endCol == touchColumn) {
							switch (m.type) {
								case JUMP:
								case JUMP_CAPTURE:
									addBlockingAnimation(new JumpAnim(800, 0, m));
									break;
								case SLIDE:
									addBlockingAnimation(new SlideAnim(400, 0, m));
									break;
								case STACK:
									addBlockingAnimation(new StackAnim(900, 0, m));
									break;
							}
						}
					}
				}
				touching = false;
				break;
			}
		}
		
		return true;
	};

	
	@Override
	protected void drawFrame(GL10Graphics g) {
		ICheckerboard checkers = getGame();
		
		final int w = g.getViewportWidth();
		final int h = g.getViewportHeight();
		
		final int RANKS = checkers.getRanks();
		final int COLUMNS = checkers.getColumns();
		
		g.pushMatrix();
		g.clearScreen(g.DARK_GRAY);

		glWidth = g.getViewportWidth();
		glHeight = g.getViewportHeight();
		glCellWidth = glWidth/COLUMNS;
		glCellHeight = glHeight/RANKS;
		glPieceRad = Math.min(glCellHeight/3, glCellHeight/3);
		// render the checkerboard
		for (int rank=0; rank<RANKS; rank++) {
			g.setColor(g.ORANGE);
			for (int col=rank%2; col<COLUMNS; col+=2) {
				g.drawFilledRect(col*glCellWidth, rank*glCellHeight, glCellWidth, glCellHeight);
			}
		}
		for (int rank=0; rank<RANKS; rank++) {
			for (int col=0; col<COLUMNS; col++) {
    			final int cx = col*glCellWidth;
    			final int cy = rank*glCellHeight;
				Piece p = checkers.getPiece(rank, col);
				
				if (p.data != null && p.data instanceof CheckerRenderer) {
					((CheckerRenderer)p.data).render(g, rank, col, p);
				} else if (p.stacks > 0) {
					drawChecker(g, p, rank, col);
				} else if (moves.size() > 0) {
					for (Move m : moves) {
						if (m.endCol == col && m.endRank == rank) {
							g.setColor(g.GREEN);
							g.setLineWidth(2 + frameCount % 10);
							g.drawRect(cx, cy, glCellWidth, glCellHeight);
						}
					}
				}
			}
		}

		renderAnimations(g, blockingAnimations);
		
		// Draw an indicator bar on side of the player whose turn it is.
		g.setColor(getPieceColor(checkers.getCurPlayerNum(), g));
		if (checkers.getCurPlayerNum() == 0) {
			g.drawFilledRect(0, glHeight-10, glWidth, 10);
		} else {
			g.drawFilledRect(0, 0, glWidth, 10);
		}
		
		g.setColor(g.YELLOW);
		switch (checkers.getWinner()) {
			case 0: // BLACK
				g.drawJustifiedString(w/2, h/2, Justify.CENTER, "BLACK WINS");
				break;
			case 1: // RED
				g.drawJustifiedString(w/2, h/2, Justify.CENTER, "RED WINS");
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
		drawChecker(g, p, rank, col, getPieceColor(p.playerNum, g));
	}
	
	void drawChecker(AGraphics g, Piece p, int rank, int col, AColor color) {
		int cx = col*glCellWidth + glCellWidth/2;
		int cy = rank*glCellHeight + glCellHeight/2;
		drawChecker(g, p, cx, cy, glPieceRad, color);
	}
	
	void drawChecker(AGraphics g, Piece p, int x, int y, int rad, AColor color) {
		if (p.stacks == 0)
			return;
		for (int i=0; i<p.stacks; i++) {
			drawChecker(g, x, y, rad, p.playerNum, color);
			y += rad/4 * getDir(p.playerNum);
		}
	}

	void drawChecker(AGraphics g, int playerNum, int rank, int col, AColor color, int stackIndex) {
		int cx = col*glCellWidth + glCellWidth/2;
		int cy = rank*glCellHeight + glCellHeight/2;
		cy += stackIndex * (glPieceRad/4 * getDir(playerNum));
		drawChecker(g, cx, cy, glPieceRad, playerNum, color);
	}
	
	void drawChecker(AGraphics g, int x, int y, int rad, int playerNum, AColor color) {
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
		getGame().newGame();
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
	
	interface CheckerRenderer {
		void render(AGraphics g, int rank, int col, Piece p);
	}
	
	abstract class ACBAnimation extends AAnimation implements CheckerRenderer {

		final Move move;
		final Piece p;
		final int rank, column;
		private final boolean executeMoveWhenDone;
		
		public ACBAnimation(long duration, int maxRepeats, Move move, boolean executeMoveWhenDone) {
			super(duration, maxRepeats);
			this.executeMoveWhenDone = executeMoveWhenDone;
			this.move = move;
			this.rank = move.startRank;
			this.column = move.startCol;
			this.p = getGame().getPiece(rank, column);
		}
		
		@Override
		protected final void onDone() {
			if (executeMoveWhenDone) {
				moves.clear();
				moves.addAll(getGame().executeMove(move));
			}
			p.data = null;
		}

		@Override
		protected final void onStarted() {
			p.data = this;
		}

		@Override
		public final void render(AGraphics g, int rank, int col, Piece p) {
			// we will call update from renderAnimations method
		}
		
	}
	
	class GlowAnim extends ACBAnimation {

		public GlowAnim(long duration, int maxRepeats, Move move) {
			super(duration, maxRepeats, move, false);
		}

		@Override
		public void draw(AGraphics g, float position, float dt) {

			AColor glowColor = getPieceColor(p.playerNum, g);
			glowColor.setAlpha(0.5f + (float)(Math.signum(position * CMath.M_PI * 2) * 0.25));
			drawChecker(g, p, rank, column, glowColor);
		}

	}

	
	class PulseAnim extends ACBAnimation {

		public PulseAnim(long duration, int maxRepeats, Move move) {
			super(duration, maxRepeats, move, false);
		}

		@Override
		public void draw(AGraphics g, float position, float dt) {
			
			float pulseScaleX = 1.0f + (float)(Math.sin(position * CMath.M_PI * 2) * 0.1);
			float pulseScaleY = 1.0f + (float)(Math.cos(position * CMath.M_PI * 2) * 0.1);
			
			float sx = glCellWidth * column + glCellWidth/2;
			float sy = glCellHeight * rank + glCellHeight/2;
			g.pushMatrix();
			g.translate(sx, sy);
			g.scale(pulseScaleX, pulseScaleY);
			drawChecker(g, 0, 0, glPieceRad, p.playerNum, getPieceColor(p.playerNum, g));
			g.popMatrix();
		}

	}
	
	
	class StackAnim extends ACBAnimation {
		
		public StackAnim(long duration, int maxRepeats, Move move) {
			super(duration, maxRepeats, move, true);
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
			g.pushMatrix();
			g.translate(x, y);
			g.scale(scale);
			drawChecker(g, 0, 0, glPieceRad, p.playerNum, getPieceColor(p.playerNum, g));
			g.popMatrix();
		}

	}

	
	class SlideAnim extends ACBAnimation {
		
		public SlideAnim(long duration, int maxRepeats, Move move) {
			super(duration, maxRepeats, move, true);
		}

		@Override
		public void draw(AGraphics g, float position, float dt) {
			float sx = glCellWidth * move.startCol + glCellWidth/2;
			float sy = glCellHeight * move.startRank + glCellHeight/2;
			float ex = glCellWidth * move.endCol + glCellWidth/2;
			float ey = glCellHeight * move.endRank + glCellHeight/2;
			int x = Math.round(sx + (ex-sx) * position);
			int y = Math.round(sy + (ey-sy) * position);
			drawChecker(g, x, y, glPieceRad, p.playerNum, getPieceColor(p.playerNum, g));
		}

	}
	
	class JumpAnim extends ACBAnimation {

		final Bezier curve;
		
		public JumpAnim(long duration, int maxRepeats, Move move) {
			super(duration, maxRepeats, move, true);
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
			curve = new Bezier(v);
		}

		@Override
		public void draw(AGraphics g, float position, float dt) {
			Vector2D v = curve.getPointAt(position);
			drawChecker(g, v.Xi(), v.Yi(), glPieceRad, p.playerNum, getPieceColor(p.playerNum, g));
		}
		
	}
	
}