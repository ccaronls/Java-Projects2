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
			return values()[((ordinal() + values().length/2) % values().length)];
		}
	}
	
	private final int COMPASS_LEN = Compass.values().length;
    
	private int width, height;
	
	private int [][] maze;
	
	private int sx, sy;
	private int ex, ey;
	
	private List<Compass> solution = null;
	
	public Maze(int width, int height) {
		resize(width, height);
	}
	
	public void resize(int newWidth, int newHeight) {
		if (newWidth < 2 || newHeight < 2)
			throw new RuntimeException("Illegal sized maze " + newWidth + "x" + newHeight);
		this.width = newWidth;
		this.height = newHeight;
		maze = new int[width][height];
	}
	
	public final void generate() {
		clear();
		//generate(0, Utils.rand() % height, width-1, Utils.rand() % height);
		
		generateR(Utils.rand() % width, Utils.rand() % height);
		List<int[]> ends = new ArrayList<int[]>();
		for (int i=0; i<width; i++) {
			for (int ii=0; ii<height; ii++) {
				if (isEndingCell(i, ii)) {
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
	
	public void clear() {
		this.solution = null;
		for (int i=0; i<width; i++) {
			for (int ii=0; ii<height; ii++) {
				maze[i][ii] = UNVISITED;
			}
		}
	}
	
	private final int UNVISITED = ((1<<COMPASS_LEN)-1);
	
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

	public final void generate(int sx, int sy, int ex, int ey) {
		this.sx = sx;
		this.sy = sy;
		this.ex = ex;
		this.ey = ey;

		clear();
		ArrayList<int[]> Q = new ArrayList<int[]>();
		Q.add(new int[] { Utils.rand() % width, Utils.rand() % height });
		Compass [] dir = Arrays.copyOf(Compass.values(), COMPASS_LEN);
		while (!Q.isEmpty()) {
			int index = nextIndex(Q.size());
			int [] xy = Q.remove(index);
			int x = xy[0];
			int y = xy[1];
			System.arraycopy(Compass.values(), 0, dir, 0, COMPASS_LEN);
			directionHeuristic(dir, x, y, ex, ey);
			for (Compass c : dir) {
				int nx = x + c.dx;
				int ny = y + c.dy;
				
				if (nx < 0 || ny < 0 || nx >= width || ny >= height)
					continue;

				if (maze[nx][ny] != UNVISITED)
					continue;
				
				breakWall(x,y,c);
				breakWall(nx,ny,Compass.values()[(c.ordinal()+COMPASS_LEN/2)%COMPASS_LEN]);
				
				Q.add(new int[] { nx,ny });
			}
		}
	}
	
	/**
	 * This affects the maze generation DFS search.  Always choosing 0 results in a BFS type maze (short paths).
	 * Always choosing size-1 results in a DFS type maze (longer paths)
	 * Choosing randomly results in a more mixed type maze.
	 * 
	 * Default behavior id random
	 * @param size
	 * @return
	 */
	protected int nextIndex(int size) {
		return Utils.rand() % size;
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
	
	public boolean isWall(int x, int y, Compass dir) {
		return 0 != (maze[x][y] & dir.flag);
	}
	
	public boolean isEndingCell(int x, int y) {
		int mask = maze[x][y];
		for (int i=0; i<COMPASS_LEN; i++) {
			int m = (~(1<<i)) & UNVISITED;
			if (mask == m)
				return true;
		}
		return false;
	}
	
	/**
	 * This affects how the maze is generated.  The array must have the values 0,1,2,3 provided in any order.
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return
	 */
    protected void directionHeuristic(Compass [] d, int x1, int y1, int x2, int y2) {
    	Utils.shuffle(d);
    }
    
    /*
        // resulting list
        int[] d = new int[4];
        
        // Use the transform array (normally used to render) to keep our weights
        // (trying to save mem)
        for (int i = 0; i < 4; i++) {
            d[i] = i;
            transform[i] = 0.5f;
        }
        
        if (y1 < y2) // tend to move north
            transform[0] += Utils.randFloat(0.4f) - 0.1f;
        else if (y1 > y2) // tend to move south
            transform[2] += Utils.randFloat(0.4f) - 0.1f;
        
        if (x1 < x2) // tend to move west
            transform[3] += Utils.randFloat(0.4f) - 0.1f;
        else if (x1 > x2) // tend to move east
            transform[1] += Utils.randFloat(0.4f) - 0.1f;
        
        // Now bubble sort the list (descending) using our weights to determine order.
        // Elems that have the same weight will be determined by a coin flip
        
        // temporaries
        float t_f;
        int t_i;
        
        // bubblesort
        for (int i = 0; i < 3; i++) {
            for (int j = i; j < 4; j++) {
                if (transform[i] < transform[j] || (transform[i] == transform[j] && Utils.flipCoin())) {
                    // swap elems in BOTH arrays
                    t_f = transform[i];
                    transform[i] = transform[j];
                    transform[j] = t_f;
                    
                    t_i = d[i];
                    d[i] = d[j];
                    d[j] = t_i;
                }
            }
        }
        
        return d;
    }*/
    
    
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
        		//if (isEndingCell(i, ii)) {
        		//	g.drawCircle(0.5f+i, 0.5f+ii, 0.4f);
        		//}
    		}
    	}
    	/*
    	if (solution != null) {
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
    	}*/
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
	
/*		
		
		if (x0 >= 0 && y0 >= 0 && x1 >=0 && y1 >= 0
				&& x0 < width && y0 < height && x1 < width && y1 < height) {
			int dx = x1-x0;
			int dy = y1-y0;
			
			//System.out.println("dx=" + dx + " dy=" + dy);

			if (dx == 0 && dy == 0)
				return true;
			
			int mask = 0;

			if (dy == -1) {
				mask |= NORTH;
			} else if (dy == 1){
				mask |= SOUTH;
			}

			if (dx == -1) {
				mask |= WEST;
			} else if (dx == 1){
				mask |= EAST;
			}
			
			return mask > 0 && 0 == (maze[x0][y0] & mask);
		}
		
		return false;
	}*/

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
