package cc.android.pacboy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.annotation.SuppressLint;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import cc.lib.android.BaseRenderer;
import cc.lib.android.GL10Graphics;
import cc.lib.game.IVector2D;
import cc.lib.game.Maze;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;

class PacBoyRenderer extends BaseRenderer implements View.OnTouchListener {

	final String TAG = "PacBoy";
	
	private Maze maze;
	private LinkedList<IVector2D> path = new LinkedList<IVector2D>();
	
	private float tx, ty; // touch point
	private float sx, sy; // scale
	private float ix, iy; // starting cell
	private float ex, ey; // ending cell
	
	private boolean solved = false;
	
	public PacBoyRenderer(GLSurfaceView parent) {
		super(parent);
	}

	private long downTime = 0;
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		
		if (maze == null || getGraphics() == null)
			return false;
		
		tx = event.getX();
		ty = event.getY();
		
		tx /= sx;
		ty /= sy;
		
		float x = tx;
		float y = ty;
		
		switch (event.getAction()) {
			case MotionEvent.ACTION_UP: {
				long t = SystemClock.uptimeMillis();
				if (t - downTime < 500) {
					//newMaze();
					//v.performClick();
				}
				downTime = 0;
				break;
			}
			case MotionEvent.ACTION_DOWN:
				if (downTime == 0)
					downTime = SystemClock.uptimeMillis();
			case MotionEvent.ACTION_MOVE: {
				if (!solved && x > 0 && y > 0 && x < maze.getWidth() && y < maze.getHeight()) {
					float d = 0;
    				if (path.size() == 0) {
    					//if ((d = Utils.distSqPointPoint(x, y, ix, iy)) < 0.5) {
    					if (maze.getStartX() == (int)x && maze.getStartY() == (int)y) {
    						addPath(x,y);
    						repaint();
    					}
    					Log.d("Dist", "d=" + d);
    				} else {
    					//if ((d=Utils.distSqPointPoint(ex, ey, x, y)) < 1) {
    					if (maze.getEndX() == (int)x && maze.getEndY() == (int)y) {
        					addPath(x,y);
    						solved = true;
    						Log.d(TAG, "Solved");
    						setTargetFPS(5);
    					} else {
        					IVector2D top = path.get(path.size()-1);
        					if ((d=Utils.distSqPointPoint(top.getX(), top.getY(), x, y)) > 0.3f && d < 1) {
        						int x0 = (int)top.getX();
        						int y0 = (int)top.getY();
        						int x1 = (int)x;
        						int y1 = (int)y;
        						
        						if (maze.isOpen(x0,y0,x1,y1)) {
            						addPath(x,y);
            						repaint();
        						}
        					}
    					}
    				}
				}
			}
		}
		
		//repaint();
		
		return true;
	}
	
	private void addPath(float x, float y) {
		synchronized (path) {
			path.addLast(new Vector2D(x, y));
		}
	}

	@Override
	protected void drawFrame(GL10Graphics g) {
		if (maze == null)
			return;
		g.clearScreen(g.BLACK);
		g.setColor(g.CYAN);
		g.pushMatrix();
		g.scale(sx, sy);
		maze.render(g);
		g.setColor(g.GREEN);
		g.drawDisk(ix, iy, 0.3f);
		g.setColor(g.RED);
		g.drawDisk(ex, ey, 0.3f);
		if (solved)
			g.setColor(g.ORANGE);
		else
			g.setColor(g.YELLOW);
		g.begin();
		int index = 0;
		synchronized (path) {
    		for (IVector2D v: path) {
    			g.vertex(v);
    			if (index++%64 == 0) {
    				g.drawPoints(solved ? 10 : 8);
    				g.begin();
    			}
    		}
		}
		g.drawPoints(solved ? 10 : 8);
		g.begin();
		g.setColor(g.RED);
		g.drawCircle(tx, ty, 1);
		g.popMatrix();
		if (solved) {
			if (path.size() == 0) {
				newMaze();
			} else {
				synchronized (path) {
					path.removeFirst();
					if (path.size() % 3 == 0) {
						setTargetFPS(getTargetFPS()+1);
					}
				}
			}
		} 
	}

	@Override
	protected void init(GL10Graphics g) {
		g.ortho();
		setDrawFPS(false);
		if (maze != null) {
			sx = getGraphics().getViewportWidth()/maze.getWidth();
			sy = getGraphics().getViewportHeight()/maze.getHeight();
		}
	}

	void newMaze(int width, int height, boolean hard) {
		maze = new Maze(width, height);
		newMaze();
	}
	
	void newMaze() {
		Log.d(TAG, "newMaze");
		maze.generate(false);
		path.clear();
		solved = false;
		ix = 0.5f + maze.getStartX();
		iy = 0.5f + maze.getStartY();
		ex = 0.5f + maze.getEndX();
		ey = 0.5f + maze.getEndY();
		setTargetFPS(0);
	}
}