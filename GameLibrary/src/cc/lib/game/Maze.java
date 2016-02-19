package cc.lib.game;

import java.util.*;

/**
 * Class to generate draw a basic maze of N x N squares.
 * Can customize how generation works by overriding directionHeuristic and nextIndex
 * 
 * @author chriscaron
 *
 */
public class Maze {
	
	public static boolean DEBUG = false;

	// wall flags organized in CCW order, this is important DO NOT
    // REORGANIZE!
	public enum Compass {
		NORTH(1, 0, -1),
		EAST(2, 1, 0),
		SOUTH(4, 0, 1),
		WEST(8, -1, 0);
		
		private Compass(int flag, int dx, int dy) {
			this.flag = flag;
			this.dx = dx;
			this.dy = dy;
		}
		
		final int flag;
		public final int dx, dy;
		
		public Compass opposite() {
			final int len = values().length;
			return values()[((ordinal() + len/2) % len)];
		}
	}
	
	private final int COMPASS_LEN = Compass.values().length;
	
	private final int UNVISITED = ((1<<COMPASS_LEN)-1); // all directions walled
    
	private int width, height;
	
	private int [][] maze;
	
	private int sx, sy;
	private int ex, ey;
	
	private List<Compass> solution = null;
	
	public Maze(int width, int height) {
		resize(width, height);
	}
	
	/**
	 * Resets the maze to a new dimension.  The maze will not be generated.
	 * @param newWidth
	 * @param newHeight
	 */
	public void resize(int newWidth, int newHeight) {
		if (newWidth < 2 || newHeight < 2)
			throw new RuntimeException("Illegal sized maze " + newWidth + "x" + newHeight);
		this.width = newWidth;
		this.height = newHeight;
		maze = new int[width][height];
	}
	
	/**
	 * Mark all cells as UNVISITED.  Called prior to generate automatically.
	 */
	public void clear() {
		this.solution = null;
		for (int i=0; i<width; i++) {
			for (int ii=0; ii<height; ii++) {
				maze[i][ii] = UNVISITED;
			}
		}
	}
	
	/**
	 * Generate a random path using recursive DFS search.  This form tend to generate mazes with a long path and not alot of branches.
	 */
	public final void generateDFS() {
		clear();
		generateR(Utils.rand() % width, Utils.rand() % height);
	}
	
	private final void generateR(int x, int y) {
		Compass [] dir = Compass.values();
		Utils.shuffle(dir);
		for (Compass c : dir) {
			int nx = x+c.dx;
			int ny = y+c.dy;
			if (nx < 0 || ny < 0 || nx >= width || ny >= height)
				continue;
			if (maze[nx][ny] == UNVISITED) {
				breakWall(x, y, c);
				breakWall(nx, ny, c.opposite());
				generateR(nx, ny);
			}
		}
	}

	public final void generateBFS() {
		clear();
		ArrayList<int[]> Q = new ArrayList<int[]>();
		ArrayList<Integer> min = new ArrayList<Integer>();
		Q.add(new int[] { Utils.rand() % width, Utils.rand() % height, 1 });
		
		while (!Q.isEmpty()) {
			min.clear();
			int m = Integer.MAX_VALUE;
			for (int i=0; i<Q.size(); i++) {
				int [] q = Q.get(i);
				if (q[2] > m) {
					continue;
				}
				if (q[2] < m) {
					m = q[2];
					min.clear();
				}
				min.add(i);
			}
			
			int index = min.get(Utils.rand() % min.size());
			int [] xy = Q.remove(index);
			int x = xy[0];
			int y = xy[1];
			int l = xy[2];
			Compass [] dir = Compass.values();
			Utils.shuffle(dir);
			int num = Utils.rand() % 3 + 1;
			for (Compass c : dir) {
				int nx = x + c.dx;
				int ny = y + c.dy;
				
				if (nx < 0 || ny < 0 || nx >= width || ny >= height)
					continue;

				if (maze[nx][ny] != UNVISITED)
					continue;
				
				breakWall(x,y,c);
				breakWall(nx,ny,c.opposite());
				
				Q.add(new int[] { nx,ny, l+1 });
				if (--num == 0)
					break;
			}
		}
	}
	
	/**
	 * Search all dead ends and set start/end to the ends that result in the longest path
	 */
	public void setStartEndToLongestPath() {
		List<int[]> ends = new ArrayList<int[]>();
		for (int i=0; i<width; i++) {
			for (int ii=0; ii<height; ii++) {
				if (isDeadEnd(i, ii)) {
					ends.add(new int[] { i, ii });
				}
			}
		}
		int d = 0;
		int s = 0;
		int e = 0;
		for (int i=0; i<ends.size()-1; i++) {
			for (int ii=i+1; ii<ends.size(); ii++) {
				if (i == ii)
					continue;
				int [] sxy = ends.get(i);
				int [] exy = ends.get(ii);
				List<Compass> path = findPath(sxy[0], sxy[1], exy[0], exy[1]);
				int len = path.size();
				if (len > d) {
					d = len;
					s = i;
					e = ii;
				}
			}
		}
		sx = ends.get(s)[0];
		sy = ends.get(s)[1];
		ex = ends.get(e)[0];
		ey = ends.get(e)[1];
		solution = findSolution();
	}
	
	/**
	 * Break the wall at xy and x+dx, y+dy where dx/dy is derived form dir.  dir = 0,1,2,3 for NORTH,EAST,SOUTH,WEST.
	 * 
	 * Default behavior is random
	 * @param x
	 * @param y
	 * @param dir
	 */
	public void breakWall(int x, int y, Compass dir) {
        maze[x][y] &= ~(dir.flag);
	}
	
	/**
	 * Return true iff there is a wall at x,y pointing in direction d
	 * @param x
	 * @param y
	 * @param dir
	 * @return
	 */
	public boolean isWall(int x, int y, Compass dir) {
		return 0 != (maze[x][y] & dir.flag);
	}
	
	/**
	 * Return true if the cell at x,y has 3 walls
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean isDeadEnd(int x, int y) {
		int mask = maze[x][y];
		for (int i=0; i<COMPASS_LEN; i++) {
			int m = (~(1<<i)) & UNVISITED;
			if (mask == m)
				return true;
		}
		return false;
	}
    
    public void draw(AGraphics g, float lineThickness) {
    	for (int i=0; i<width; i++) {
    		for (int ii=0; ii<height; ii++) {
        		g.begin();
    			int cell = maze[i][ii];
    			if (0 != (cell & Compass.NORTH.flag)) {
    				g.vertex(i, ii);
    				g.vertex(i+1, ii);
    			}
    			if (0 != (cell & Compass.SOUTH.flag)) {
    				g.vertex(i, ii+1);
    				g.vertex(i+1, ii+1);
    			}
    			if (0 != (cell & Compass.EAST.flag)) {
    				g.vertex(i+1,ii);
    				g.vertex(i+1, ii+1);
    			}
    			if (0 != (cell & Compass.WEST.flag)) {
    				g.vertex(i, ii);
    				g.vertex(i, ii+1);
    			}
        		g.drawLines(lineThickness);
        		if (DEBUG) {
        			if (isDeadEnd(i, ii)) {
        				g.drawCircle(0.5f+i, 0.5f+ii, 0.4f);
        			}
        		}
    		}
    	}
    	
    	if (DEBUG && solution != null) {
    		g.begin();
    		float x = 0.5f + sx;
    		float y = 0.5f + sy;
    		g.vertex(x,y);
    		for (Compass c : solution) {
    			x += c.dx;
    			y += c.dy;
    			g.vertex(x,y);
    		}
    		g.drawLineStrip();
    	}
    }

	public final int getWidth() {
		return width;
	}

	public final int getHeight() {
		return height;
	}

	public final int getStartX() {
		return sx;
	}
	public final int getStartY() {
		return sy;
	}
	public final int getEndX() {
		return ex;
	}
	public final int getEndY() {
		return ey;
	}
	
	public void setStart(int x, int y) {
		this.sx = Utils.clamp(x, 0, width-1);
		this.sy = Utils.clamp(y, 0, height-1);
	}
	
	public void setEnd(int x, int y) {
		this.ex = Utils.clamp(x, 0, width-1);
		this.ey = Utils.clamp(y, 0, height-1);
	}
	
	/**
	 * return true if there is a direct open path from p0 -> p1.  A recursive search is used.
	 * @param x0
	 * @param y0
	 * @param x1
	 * @param y1
	 * @return
	 */
	public boolean isOpen(int x0, int y0, int x1, int y1) {
		// simple case
		if (x0<0 || y0<0 || x0>=width || y0>=height || x1<0 || y1<0 || x1>=width || y1>=height)
			return false;

		return isOpenR(x0, y0, x1, y1);
	}
	
	private boolean isOpenR(int x0, int y0, int x1, int y1) {
		if (x0 == x1 && y0 == y1)
			return true;

		Utils.println("isOpenR x0=" + x0 + " y0=" + y0 + " x1=" + x1 + " y1=" + y1);

		if (x1 < x0) {
			if (0 == (maze[x0][y0] & Compass.WEST.flag)) {
				if (isOpenR(x0-1, y0, x1, y1))
					return true;
			}
		} 

		if (x1 > x0) {
			if (0 == (maze[x0][y0] & Compass.EAST.flag)) {
				if (isOpenR(x0+1, y0, x1, y1))
					return true;
			}
		} 

		if (y1 < y0) {
			if (0 == (maze[x0][y0] & Compass.NORTH.flag)) {
				if (isOpenR(x0, y0-1, x1, y1))
					return true;
			}
		} 

		if (y1 > y0) {
			if (0 == (maze[x0][y0] & Compass.SOUTH.flag)) {
				if (isOpenR(x0, y0+1, x1, y1))
					return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Return a list of DIRECTION flags.
	 * 
	 * @param sx
	 * @param sy
	 * @param ex
	 * @param ey
	 * @return
	 */
	public List<Compass> findPath(int sx, int sy, int ex, int ey) {
		int [][] visited = new int[width][height];
		LinkedList<Compass> path = new LinkedList<Compass>();
		findPathR(visited, path, sx, sy, ex, ey);
		return path;
	}
	
	/**
	 * 
	 * @return
	 */
	public List<Compass> findSolution() {
		return findPath(sx, sy, ex, ey);
	}

	// non-recursive that uses the heap...to prevent stack overflow
	private boolean findPathDFS(LinkedList<Compass> path, int x, int y, int ex, int ey) {
		Stack<int[]> S = new Stack<int[]>();
		
		S.add(new int[] { x,y,0 });
		while (!S.isEmpty()) {
			int [] q = S.peek();
			x = q[0];
			y = q[1];
			if (x == ex && y == ey)
				return true; // done
			Compass c = Compass.values()[q[2]];
			while (c.ordinal() < COMPASS_LEN) {
				if (!isWall(x,y,c)) {
					path.addLast(c);
					S.add(new int[] { x+c.dx, y+c.dy, c.ordinal() });
				}
			}
		}
		
		return false;
	}
	
	private boolean findPathR(int [][] visited, LinkedList<Compass> path, int x, int y, int ex, int ey) {
		if (x == ex && y == ey)
			return true;

		if (visited[x][y] != 0)
			return false;
		
		visited[x][y] = 1;

		for (Compass c : Compass.values()) {
			if (!isWall(x, y, c)) {
				path.addLast(c);
				if (findPathR(visited, path, x + c.dx, y + c.dy, ex, ey)) {
					return true;
				}
				path.removeLast();
			}
		}
		
		return false;
	}
}
