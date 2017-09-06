package cc.android.sebigames.pacboy;

import java.util.*;

import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import cc.lib.android.BaseRenderer;
import cc.lib.android.GL10Graphics;
import cc.lib.game.AColor;
import cc.lib.game.IVector2D;
import cc.lib.game.Maze;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;

class PacBoyRenderer extends BaseRenderer implements View.OnTouchListener {

	final String TAG = "PacBoy";
	
	public static final int DIFFICULTY_NO_CHASE = 9;
	public static final int DIFFICULTY_INCREASE_MAZE_SIZE_MOD = 5;	
	public static final int DIFFICULTY_INCREASE_PACBOY_MAX_SPEED_MOD = 3;	

	private final int STATE_READY = 0; 		// waiting for use to touch near the start
	private final int STATE_PLAYING = 1;	// user laying their path
	private final int STATE_CHASING = 10;	// pacboy shasing the user
	private final int STATE_EATEN = 11;		// pacboy has caught the user
	private final int STATE_SOLVED = 12; // user has solved the maze
	private final int STATE_GAME_OVER = 13; // user has no more lives
	private final int STATE_INTRO = 100;
	
	private Maze maze;
	private LinkedList<IVector2D> path = new LinkedList<IVector2D>();
	
	private float tx, ty; // touch point
	private float scalex, scaley; // scale
	private float ix, iy; // starting cell
	private float ex, ey; // ending cell
	
	private final PacBoy pb = new PacBoy();
	private int difficulty = 0;
	
	private int lives = 0;
	private int score = 0;
	private int state = STATE_READY;
	private int frame = 0;
	private int startChasePts = 10;
	
	private List<Maze.Compass> solution = null;
	
	public PacBoyRenderer(GLSurfaceView parent) {
		super(parent);
	}
	
	private int findClosestPathIndex(float x, float y, boolean useTooClose) {
		IVector2D closest = null;
		int closestIndex = -1;
		float minD = 0;
		float d = 0;
		boolean tooClose = false;
		boolean tooFar = true;
		for (int i=path.size()-1; i>=0; i--) {
			IVector2D vv = path.get(i);
			d=Utils.distSqPointPoint(vv.getX(), vv.getY(), x, y);
			if (useTooClose && d<= 0.3f) {
				tooClose = true;
				break;
			}
			if (d<=1) {
				tooFar = false;
				if (closest == null || d < minD) {
					closest = vv;
					closestIndex = i;
					minD = d;
				} 
			}
		}
		
		if (closest != null && !tooClose && !tooFar) {
			int x0 = (int)closest.getX();
			int y0 = (int)closest.getY();
			int x1 = (int)x;
			int y1 = (int)y;
			
			if (maze.isOpen(x0,y0,x1,y1)) {
				return closestIndex;
			}
		}
		return -1;
	}
	
	@Override
	public synchronized boolean onTouch(View v, MotionEvent event) {
		
		if (maze == null || getGraphics() == null)
			return false;
		
		tx = event.getX();
		ty = event.getY();
		
		tx /= scalex;
		ty /= scaley;
		
		float x = tx;
		float y = ty;
		
		if (x>0 && y>0 && x<maze.getWidth() && y<maze.getHeight()) {
    		switch (event.getAction()) {
    			case MotionEvent.ACTION_DOWN:
    				v.performClick();
					synchronized (path) {
						if (path.size() > 0) {
        					// find the point we are closest too and remove all in front
        					int index = findClosestPathIndex(x, y, false);
        					if (index > 0) {
            					while (path.size() > 0 && index < path.size())
            						path.removeLast();
        					}
        					repaint();
						}
    				}
    				break;
    			case MotionEvent.ACTION_MOVE: {
    				if (state < STATE_EATEN) {
    					float d = 0;
        				if (path.size() == 0) {
        					//if ((d = Utils.distSqPointPoint(x, y, ix, iy)) < 0.5) {
        					if (maze.getStartX() == (int)x && maze.getStartY() == (int)y) {
        						addPath(x,y);
        						setState(STATE_PLAYING);
        						repaint();
        					}
        					Log.d("Dist", "d=" + d);
        				} else {
        					/*
        					//IVector2D top = path.get(path.size()-1);
        					IVector2D closest = null;
        					float minD = 0;
        					boolean tooClose = false;
        					boolean tooFar = true;
        					for (int i=path.size()-1; i>=0; i--) {
        						IVector2D vv = path.get(i);
        						d=Utils.distSqPointPoint(vv.getX(), vv.getY(), x, y);
        						if (d<= 0.3f) {
        							tooClose = true;
        							break;
        						}
        						if (d<=1) {
        							tooFar = false;
        							if (closest == null || d < minD) {
        								closest = vv;
        								minD = d;
        							} 
        						}
        					}
        					
        					if (closest != null && !tooClose && !tooFar) {
        						int x0 = (int)closest.getX();
        						int y0 = (int)closest.getY();
        						int x1 = (int)x;
        						int y1 = (int)y;
        						
        						if (maze.isOpen(x0,y0,x1,y1)) {
            						addPath(x,y);
            						//if ((d=Utils.distSqPointPoint(ex, ey, x, y)) < 1) {
                					if (maze.getEndX() == (int)x && maze.getEndY() == (int)y) {
                						setState(STATE_SOLVED);
                					}
                					repaint();
                					break;
        						}
    
        					}*/
        					int index = findClosestPathIndex(x, y, true);
        					if (index >= 0) {
        						addPath(x,y);
        						//if ((d=Utils.distSqPointPoint(ex, ey, x, y)) < 1) {
            					if (maze.getEndX() == (int)x && maze.getEndY() == (int)y) {
            						if (difficulty <= DIFFICULTY_NO_CHASE) {
            							loadNextLevel();
            						} else {
            							setState(STATE_SOLVED);
            						}
            					}
            					repaint();
        					}
        				}
    				}
    				break;
    			}
    		}
		}	

		return true;
	}
	
	private void addPath(float x, float y) {
		synchronized (path) {
			path.addLast(new Vector2D(x, y));
		}
		if (state == STATE_PLAYING && difficulty > DIFFICULTY_NO_CHASE && path.size() < 10) {
			setState(STATE_CHASING);
		}
	}

	@Override
	protected void drawFrame(GL10Graphics g) {
		if (maze != null) {
    		scalex = g.getViewportWidth()/maze.getWidth();
    		scaley = g.getViewportHeight()/maze.getHeight();
		}
		
		g.pushMatrix();
		g.scale(scalex, scaley);
		switch (state) {
			case STATE_READY:
				setTargetFPS(30);
				drawReady(g); 
				break;
			case STATE_PLAYING:
				setTargetFPS(0);
				drawPlaying(g); 
				break;
			case STATE_CHASING:
				setTargetFPS(20);
				drawChasing(g);
				break;
			case STATE_EATEN:
				setTargetFPS(20);
				drawEaten(g);
				break;
			case STATE_SOLVED:
				drawSolved(g);
				break;
			case STATE_GAME_OVER:
				drawGameOver(g);
				break;
			case STATE_INTRO:
				setTargetFPS(20);
				drawIntro(g);
				break;
		}
		g.popMatrix();
		frame++;
		if (difficulty > DIFFICULTY_NO_CHASE) {
    		g.setColor(g.RED);
    		float x = 15;
    		float y = g.getViewportHeight()- 15;
    		
    		for (int i=0; i<lives; i++) {
    			g.drawDisk(x, y, 10, 16);
    			x += 30;
    		}
		}
	}
	
	void addToScore(int pts) {
		score += pts;
	}

	void drawMaze(GL10Graphics g) {
		g.clearScreen(g.YELLOW);
		g.setColor(g.BLUE);
		maze.draw(g, 5);
	}
	
	private void drawDots(GL10Graphics g, float size) {
		g.begin();
		int index = 0;
		synchronized (path) {
    		for (IVector2D v: path) {
    			g.vertex(v);
    			if (index++%64 == 0) {
    				g.drawPoints(size);
    				g.begin();
    			}
    		}
		}
		g.drawPoints(size);
		g.end();
	}
	
	private void drawPath(GL10Graphics g, List<Maze.Compass> path) {
		int sx = maze.getStartX();
		int sy = maze.getStartY();
		g.begin();
		g.setColor(g.MAGENTA);
		g.vertex(sx, sy);
		for (Maze.Compass c : path) {
			sx += c.dx;
			sy += c.dy;
			g.vertex(sx, sy);
		}
		g.drawLineStrip(5);
	}
	
	float [] pulse = {0, 1, 2, 3, 2, 1, 0};
	
	void drawReady(GL10Graphics g) {
		drawMaze(g);
		g.setColor(g.GREEN);
		g.drawDisk(ix, iy, 0.3f + 0.01f * pulse[frame%pulse.length], 32);
		g.setColor(g.RED);
		g.drawDisk(ex, ey, 0.3f, 32);
	}
	
	void drawPlaying(GL10Graphics g) {
		drawMaze(g);
		g.setColor(g.GREEN);
		g.drawDisk(ix, iy, 0.3f);
		g.setColor(g.RED);
		g.drawDisk(ex, ey, 0.3f + 0.01f * pulse[frame%pulse.length], 32);
		drawDots(g, 8);
		//drawPath(g, solution);
	}
	
	private void drawChasing(GL10Graphics g) {
		drawPlaying(g);
		g.setColor(g.BLACK);
		pb.draw(g);
		if (path.size() > 0) {
			synchronized (path) {
				if (pb.moveTo(path.getFirst())) {
					path.removeFirst();
				}
			}
		} else {
			setState(STATE_EATEN);
		}
	}
	
	void setState(int newState) {
		if (state == newState)
			return;
		
		state = newState;
		switch (newState) {
			case STATE_READY:
				path.clear();
				break;
			case STATE_PLAYING:
				break;
			case STATE_CHASING:
				pb.pos.set(maze.getStartX(), maze.getStartY());
				pb.reset();
				break;
			case STATE_EATEN:
				frame = 0;
				break;
			case STATE_SOLVED:
				Log.d(TAG, "Solved");
				setTargetFPS(5);
				break;
			case STATE_GAME_OVER:
				state = STATE_GAME_OVER;
				frame = 0;
				setTargetFPS(20);
				break;
		}
	}
	
	private void drawEaten(GL10Graphics g) {
		
		AColor [] colors = { g.BLUE, g.YELLOW };

		int i0 = (frame/5) % 2;
		int i1 = (i0+1) % 2;
		
		g.clearScreen(colors[i0]);
		g.setColor(colors[i1]);
		float scale = 0.01f * frame;
		//g.translate(-scale/2, -scale/2);
		maze.draw(g, 5);
		pb.radius = 0.5f + scale;
		pb.degrees += scale * 20;
		pb.draw(g);
		if (frame > 100) {
			if (--lives <= 0) {
				setState(STATE_GAME_OVER);
			} else {
				setState(STATE_READY);
			}
		}
	}
	
	private void drawFace(GL10Graphics g, float x, float y, float r, boolean tongue) {
		g.setColor(g.BLACK);
		g.drawDisk(x, y, r, 32);
		g.setColor(g.YELLOW);
		g.begin();
		g.vertex(x-r/3, y-r/3);
		g.vertex(x+r/3, y-r/3);
		g.drawPoints(8);
		g.end();
		if (tongue) {
			// draw a tongue
			g.setColor(g.RED);
			g.drawFilledRectf(x-r/6, y+r/4, r/3, r/3);
			g.drawDisk(x, y+r/4+r/3, r/6);
		} else {
    		g.begin();
    		g.vertex(x-r/3, y);
    		g.vertex(x+r/3, y+r/3);
    		g.drawLines(4);
    		g.end();
		}
	}
	
	private void drawCleanup(GL10Graphics g)
	{
		drawPlaying(g);
		g.setColor(g.BLACK);
		pb.draw(g);
		if (path.size() > 0) {
			synchronized (path) {
				if (pb.moveTo(path.getFirst())) {
					path.removeFirst();
				}
			}
		} else {
			loadNextLevel();
		}
	}
	
	private void loadNextLevel() {
		if (0 == (difficulty % DIFFICULTY_INCREASE_MAZE_SIZE_MOD)) {
			int width = maze.getWidth() + 2;
			int height = maze.getHeight() + 1;
			maze.resize(width, height);
		} 
		
		if (DIFFICULTY_NO_CHASE > DIFFICULTY_NO_CHASE && difficulty % DIFFICULTY_INCREASE_PACBOY_MAX_SPEED_MOD == 0) {
			pb.maxSpeed += 0.1f;
		}
		newMaze();
	}
	
	private void drawSolved(GL10Graphics g) {
		drawPlaying(g);
		g.setColor(g.RED);
		g.drawDisk(ex, ey, 0.3f, 32);
		g.setColor(g.ORANGE);
		drawDots(g, 10);
		float x = pb.pos.getX();
		float y = pb.pos.getY();
		float r = pb.radius;
		g.setColor(g.BLACK);
		g.drawDisk(x, y, r, 32);
		g.setColor(g.YELLOW);
		g.begin();
		g.vertex(x-r/3, y-r/3);
		g.vertex(x+r/3, y-r/3);
		g.drawPoints(8);
		g.end();
		drawFace(g, x, y, r, path.size() <= 10);
		if (path.size() == 0) {
			loadNextLevel();
		} else {
			synchronized (path) {
				score += path.size();
				path.removeFirst();
				if (path.size() % 3 == 0) {
					setTargetFPS(getTargetFPS()+1);
				}
			}
		}
	}
	
	void drawGameOver(GL10Graphics g) {
		drawPlaying(g);
		float r = maze.getHeight()/4;
		float x = maze.getWidth()/2;
		float y = maze.getHeight()+r*2;
		float step = maze.getHeight() * 0.02f;
		y -= step * frame;
		if (y < maze.getHeight()/2) {
			y = maze.getHeight()/2;
			drawFace(g, x, y, r, true);
			setTargetFPS(0);
		} else {
			drawFace(g, x, y, r, false);
		}
	}
	
	void drawIntro(GL10Graphics g) {
		float wid = g.getViewportWidth();
		float hgt = g.getViewportHeight();
		if (frame == 0) {
			pb.radius = hgt/6; 
			pb.pos.set(-pb.radius, hgt/2);
		}
		int secondsToCenter = 3;
		int framesToCenter = secondsToCenter * getTargetFPS();
		float speed = (wid/2+pb.radius) / framesToCenter;
		g.clearScreen(g.YELLOW);
		g.setColor(g.BLACK);
		if (frame >= framesToCenter && frame < framesToCenter + getTargetFPS()*3) {
			if (frame < framesToCenter + getTargetFPS()) {
				drawFace(g, pb.pos.getX(), pb.pos.getY(), pb.radius, false);
			} else if (frame < framesToCenter + getTargetFPS()*2) {
				drawFace(g, pb.pos.getX(), pb.pos.getY(), pb.radius, true);
			} else {
				drawFace(g, pb.pos.getX(), pb.pos.getY(), pb.radius, false);
			}
		} else {
			pb.pos.addEq(speed, 0);
			pb.draw(g);
		}
	}

	@Override
	protected void init(GL10Graphics g) {
		g.ortho();
		setDrawFPS(false);
		if (maze != null) {
			scalex = getGraphics().getViewportWidth()/maze.getWidth();
			scaley = getGraphics().getViewportHeight()/maze.getHeight();
		}
	}

	void newMaze(int width, int height, int difficulty) {
		frame = 0;
		this.difficulty = difficulty;
		this.lives = 3;
		this.score = 0;
		if (difficulty == 0) {
			startChasePts = 20;
		}
		Log.d(TAG, "difficulty = " + difficulty);
		maze = new Maze(width, height);
		newMaze();
	}
	
	void newMaze() {
		Log.d(TAG, "newMaze");
		if (1 == (difficulty % 2)) {
			maze.generateDFS();
		} else {
			maze.generateBFS();
		}
		if (difficulty > 5) {
			maze.setStartEndToLongestPath();
		} else {
			maze.setStart(0, Utils.rand() % maze.getHeight());
			maze.setEnd(maze.getWidth()-1, Utils.rand() % maze.getHeight());
		}
		path.clear();
		state = STATE_READY;
		ix = 0.5f + maze.getStartX();
		iy = 0.5f + maze.getStartY();
		ex = 0.5f + maze.getEndX();
		ey = 0.5f + maze.getEndY();
		if (startChasePts > 5)
			startChasePts --;
		difficulty++;
		pb.reset();
		//solution = maze.findSolution();
	}
	
	void setupIntro() {
		state = STATE_INTRO;
		pb.reset();
		scalex = scaley = 1;
		frame = 0;
	}

	int getScore() {
		return score;
	}
	
	int getDifficulty() {
		return this.difficulty;
	}
}