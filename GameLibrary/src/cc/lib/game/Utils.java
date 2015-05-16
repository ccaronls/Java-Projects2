package cc.lib.game;

import java.util.*;

import cc.lib.math.Vector2D;

public class Utils {

	// USER SETTABLE VARS
	
	/**
	 * Set too true to get debugging console
	 */
	public static boolean DEBUG_ENABLED = false;
	
	// CONSTANTS

	/**
	 * defien a small num
	 */
	public static float EPSILON      	= 0.00001f;

	/**
	 * convert from DEGREES to RADIANS
	 */
    public static final float DEG_TO_RAD 	= (float)(Math.PI / 180.0); // converts from degrees to radians
    
    /**
     * Convert from RADIANS to DEGREES
     */
    public static final float RAD_TO_DEG	= (float)(180.0 / Math.PI); // converts form radians to degress

    // general working matricies, created once
    private final static float [] 	m_matrix_2x2	= new float[4];
    private final static float [] 	r_matrix_2x2	= new float[4];
    private final static Random   	randGen = new Random(System.currentTimeMillis()); // random number generator
	
    // FUCNTIONS
    
    /**
     * 
     * @param c
     */
    public static void unhandledCase(Object c) {
        RuntimeException e = new RuntimeException("Unhandled case [" + c + "]");
        e.printStackTrace();
        throw e;
    }
    
    /**
     * 
     * @param expr
     */
    public static void assertTrue(boolean expr) {
        assertTrue(expr, "Expression is false");
    }

    /**
     * 
     * @param expr
     * @param msg
     * @param args
     */
    public static void assertTrue(boolean expr, String msg, Object ... args) {
        if (DEBUG_ENABLED && !expr) {
            throw new RuntimeException("ASSERT FAILED " + String.format(msg, args));
        }
    }

    /**
     * 
     * @param msg
     */
    public static void print(String msg) {
    	if (DEBUG_ENABLED)
    		System.out.print(msg);
    }
    
    public static <T> void printCollection(Collection<T> c) {
    	if (DEBUG_ENABLED) {
        	int index = 0;
        	for (T t : c) {
        		System.out.println(String.format("%3d:%s", index++, t));
        	}
    	}
    }
    
    /**
     * 
     * @param msg
     */
    public static void println(String msg) {
    	if (DEBUG_ENABLED)
    		System.out.println(msg);
    }

    /**
     * 
     *
     */
    public static void println() {
    	if (DEBUG_ENABLED)
    		System.out.println();
    }

    /**
     * 
     * @param msg
     * @param args
     */
    public static void print(String msg, Object ... args) {
        if (DEBUG_ENABLED) {
            System.out.print(String.format(msg, args));
        }
    }
    
    /**
     * 
     * @param msg
     * @param args
     */
    public static void println(String msg, Object ... args) {
        if (DEBUG_ENABLED) {
            System.out.println(String.format(msg, args));
        }
    }

    /**
     * 
     * @param mat
     * @param vx
     * @param vy
     * @param result_v
     */
    public static void  mult2x2MatrixVector(float [] mat, float vx, float vy, float [] result_v) {
        result_v[0] = mat[0]*vx + mat[1]*vy;
        result_v[1] = mat[2]*vx + mat[3]*vy;
    }

    /**
     * 
     * @param a
     * @param b
     * @param c
     * @param d
     * @param vx
     * @param vy
     * @param result_v
     */
    public static void  mult2x2MatrixVector(float a, float b, float c, float d, float vx, float vy, float [] result_v) {
        result_v[0] = a*vx + b*vy;
        result_v[1] = c*vx + d*vy;
    }

    /**
     * 
     * @param mat1
     * @param mat2
     * @param result
     */
    public static void  mult2x2Matricies(float [] mat1, float [] mat2, float [] result) {
        result[0] = mat1[0]*mat2[0]+mat1[1]*mat2[2];
        result[1] = mat1[0]*mat2[1]+mat1[1]*mat2[3];
        result[2] = mat1[2]*mat2[0]+mat1[3]*mat2[2];
        result[3] = mat1[2]*mat2[1]+mat1[3]*mat2[3];
    }

    /**
     * 
     * @param vector
     * @param degrees
     */
    public static void  rotateVector(float [] vector, float degrees) {
    	rotateVector(vector, vector, degrees);
    }       

    /**
     * 
     * @param vector
     * @param degrees
     */
    public static void  rotateVector(final float [] vector, float [] result, float degrees) {
    	degrees *= DEG_TO_RAD;
    	float cosd = (float)Math.cos(degrees);
    	float sind = (float)Math.sin(degrees);
    	
        float x = vector[0] * cosd - vector[1] * sind;
        float y = vector[0] * sind + vector[1] * cosd;
        result[0] = x;
        result[1] = y;
    }     
    
    /**
     * Return true if difference between to floats is less than EPSILON
     * @param a
     * @param b
     * @return
     */
    public static boolean isAlmostEqual(float a, float b) {
    	return Math.abs(a-b) < EPSILON;
    }

    /**
     * Return determinant of 2x2 matrix
     * 
     * @param mat
     * @return
     */
    public static float determinant2x2Matrix(float [] mat) {
        return (mat[0]*mat[3]-mat[1]*mat[2]);
    }

    /**
     * Invert a matrix
     * 
     * @param source
     * @param dest
     * @return
     */
    public static boolean invert2x2Matrix(float [] source, float [] dest) {    
        float det = (source[0]*source[3] - source[1]*source[2]);
        if (Math.abs(det) < EPSILON)
            return false;
        dest[0] =  source[3] / det;
        dest[1] = -source[1] / det;
        dest[2] = -source[2] / det;
        dest[3] =  source[0] / det;
        return true;
    }
    
    /**
     * 
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param px
     * @param py
     * @param radius
     * @return
     */
    public static boolean isCircleIntersectingLineSeg(int x0, int y0, int x1, int y1, int px, int py, int radius) {
    	float d2 = Utils.distSqPointSegment(px, py, x0, y0, x1, y1);
    	float r2 = radius * radius;
    	if (d2 < r2)
    		return true;
    	return false;
    }
    
    /**
     * Return the angle in degrees between 2 vectors
     * 
     * @param dx
     * @param dy
     * @param vx
     * @param vy
     * @return
     */
    public static float computeDegrees(float dx, float dy, float vx, float vy) {
        double magA = Math.sqrt(dx*dx + dy*dy);
        double magB = Math.sqrt(vx*vx + vy*vy);
        double AdotB = dx*vx + dy*vy;
        double acos = Math.acos(AdotB / (magA * magB));
        return (float)(acos * RAD_TO_DEG);
    }

    public static boolean isLineSegsIntersecting(
            int x0, int y0, int x1, int y1, // line segment 1
            int x2, int y2, int x3, int y3) // line segment 2
    {
        switch (getLineSegsIntersection(x0, y0, x1, y1, x2, y2, x3, y3)) {
            case 0: return false;
            default:
                return true;
        }
    }
    
    /**
     * 
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param x3
     * @param y3
     * @return 1 if segs are intersecting at a single point.  2 is segs are coincident.  0 if not intersecting
     */
    public static int getLineSegsIntersection(
                                int x0, int y0, int x1, int y1, // line segment 1
                                int x2, int y2, int x3, int y3) // line segment 2
    {
        
        m_matrix_2x2[0] = (x1 - x0);
        m_matrix_2x2[1] = (x2 - x3);
        m_matrix_2x2[2] = (y1 - y0);
        m_matrix_2x2[3] = (y2 - y3);

        if (!invert2x2Matrix(m_matrix_2x2, r_matrix_2x2)) {
            float cx0 = (x0+x1)/2; // center x of seg0
            float cy0 = (y0+y1)/2;
                        
            float cx1 = (x2+x3)/2; // center x of seg1
            float cy1 = (y2+y3)/2;
            
            float dx  = cx0 - cx1; // distance between centers
            float dy  = cy0 - cy1;
            
            // dot product of normal to delta and one of the line segs
            float dot = -dy*(x1-x0) + dx*(y1-y0);
            if (Math.abs(dot) > EPSILON)
                return 0; // if the delta is not near parallel, then this is not intersecting

            float d   = Math.abs((dx*dx) + (dy*dy)); // len^2 of delta
            
            float dx0 = (x1-cx0);
            float dy0 = (y1-cy0);
            
            float dx1 = (x3-cx1);
            float dy1 = (y3-cy1);
            
            // len^2 of 1/2 of the sum of the 2 segs
            float maxd = Math.abs((dx0*dx0) + (dy0*dy0) + (dx1*dx1) + (dy1*dy1));
            
            if (d <= maxd)
                return 2;
            
            return 0; // lines are parallel and not coincident
        }
        
        // is it neccessary to cache these?
        float vx = x2 - x0;
        float vy = y2 - y0;
        
        // tx,ty are the t value of L = p0 + v0*t for each line
        float t0 = r_matrix_2x2[0] * vx + r_matrix_2x2[1] * vy;
        float t1 = r_matrix_2x2[2] * vx + r_matrix_2x2[3] * vy;
        
        if (t0<0 || t0>1 || t1<0 || t1>1)
            return 0;
            
        return 1;
    }
    

    /**
     * 
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param x3
     * @param y3
     * @return 1 if segs are intersecting at a single point.  2 is segs are coincident.  0 if not intersecting
     */
    public static int isLineSegsIntersecting(
    							float x0, float y0, float x1, float y1, // line segment 1
    							float x2, float y2, float x3, float y3) // line segment 2
    {
        
        m_matrix_2x2[0] = (x1 - x0);
        m_matrix_2x2[1] = (x2 - x3);
        m_matrix_2x2[2] = (y1 - y0);
        m_matrix_2x2[3] = (y2 - y3);

        if (!invert2x2Matrix(m_matrix_2x2, r_matrix_2x2)) {
        	float cx0 = (x0+x1)/2; // center x of seg0
        	float cy0 = (y0+y1)/2;
        	        	
        	float cx1 = (x2+x3)/2; // center x of seg1
        	float cy1 = (y2+y3)/2;
        	
        	float dx  = cx0 - cx1; // distance between centers
        	float dy  = cy0 - cy1;
        	
        	// dot product of normal to delta and one of the line segs
        	float dot = dx*(y1-y0) - dy*(x1-x0);
        	if (Math.abs(dot) > EPSILON)
        	    return 0; // if the delta is not near parallel, then this is not intersecting

        	float d   = Math.abs((dx*dx) + (dy*dy)); // len^2 of delta
        	
        	float dx0 = (x1-cx0);
        	float dy0 = (y1-cy0);
        	
        	float dx1 = (x3-cx1);
        	float dy1 = (y3-cy1);
        	
        	// len^2 of 1/2 of the sum of the 2 segs
        	float maxd = Math.abs((dx0*dx0) + (dy0*dy0) + (dx1*dx1) + (dy1*dy1));
        	
        	if (d <= maxd)
        		return 2;
        	
            return 0; // lines are parallel and not coincident
        }
        
        // is it neccessary to cache these?
        float vx = x2 - x0;
        float vy = y2 - y0;
        
        // tx,ty are the t value of L = p0 + v0*t for each line
        float t0 = r_matrix_2x2[0] * vx + r_matrix_2x2[1] * vy;
        float t1 = r_matrix_2x2[2] * vx + r_matrix_2x2[3] * vy;
        
        if (t0<0 || t0>1 || t1<0 || t1>1)
            return 0;
            
        return 1;
    }

    /**
     * 
     * @param px
     * @param py
     * @param rx
     * @param ry
     * @param rw
     * @param rh
     * @return
     */
    public static boolean isPointInsideRect(float px, float py, float rx, float ry, float rw, float rh) {
    	if (px>rx && py>ry && px<rx+rw && py<ry+rh)
    		return true;
    	return false;
    }
    
    /**
     * 
     * @param px
     * @param py
     * @param xpts
     * @param ypts
     * @return
     */
    public static boolean isPointInsidePolygon(int px, int py, int [] xpts, int [] ypts, int numPts) {
        int orient = 0;
        
        for (int i=0; i<numPts; i++) {
            int dx = px - xpts[i];
            int dy = py - ypts[i];
            
            int ii = (i+1) % numPts;
            int dx2 = xpts[ii] - xpts[i];
            int dy2 = ypts[ii] - ypts[i];
            
            int nx = -dy2;
            int ny = dx2;
            
            int dot = nx*dx + ny*dy;
            
            if (dot < 0) {
                // return false if orientation changes
                if (orient > 0)
                    return false;
                // capture orientation if we dont have it yet
                else if (orient == 0)
                    orient = -1;
            } else {
                // return false if orientation changes
                if (orient < 0)
                    return false;
                // capture orientation if we dont have it yet
                else if (orient == 0)
                    orient = 1;
            }
            
        }
        return true;
    }

    /**
     * 
     * @param px
     * @param py
     * @param xpts
     * @param ypts
     * @return
     */
    public static boolean isPointInsidePolygonf(float px, float py, float [] xpts, float [] ypts, int numPts) {
        
        int orient = 0;
        
        for (int i=0; i<numPts; i++) {
            float dx = px - xpts[i];
            float dy = py - ypts[i];
            
            int ii = (i+1) % numPts;
            float dx2 = xpts[ii] - xpts[i];
            float dy2 = ypts[ii] - ypts[i];
            
            float nx = -dy2;
            float ny = dx2;
            
            float dot = nx*dx + ny*dy;
            
            if (Math.abs(dot) < EPSILON) {
                // ignore since this is 'on' the segment
            }
            else if (dot < 0) {
                // return false if orientation changes
                if (orient > 0)
                    return false;
                // capture orientation if we dont have it yet
                else if (orient == 0)
                    orient = -1;
            } else {
                // return false if orientation changes
                if (orient < 0)
                    return false;
                // capture orientation if we dont have it yet
                else if (orient == 0)
                    orient = 1;
            }
        }        
        
        return true;
    }
    
    /**
     * 
     * @param px
     * @param py
     * @param xpts
     * @param ypts
     * @return
     */
    public static boolean isPointInsidePolygon(float px, float py, IVector2D [] pts, int numPts) {
        
        int orient = 0;
        
        for (int i=0; i<numPts; i++) {
            float dx = px - pts[i].getX();
            float dy = py - pts[i].getY();
            
            int ii = (i+1) % numPts;
            float dx2 = pts[ii].getX() - pts[i].getX();
            float dy2 = pts[ii].getY() - pts[i].getY();
            
            float nx = -dy2;
            float ny = dx2;
            
            float dot = nx*dx + ny*dy;
            
            if (Math.abs(dot) < EPSILON) {
                // ignore since this is 'on' the segment
            }
            else if (dot < 0) {
                // return false if orientation changes
                if (orient > 0)
                    return false;
                // capture orientation if we dont have it yet
                else if (orient == 0)
                    orient = -1;
            } else {
                // return false if orientation changes
                if (orient < 0)
                    return false;
                // capture orientation if we dont have it yet
                else if (orient == 0)
                    orient = 1;
            }
        }        
        
        return true;
    }

    /**
     * 
     * @param px
     * @param py
     * @param cx
     * @param cy
     * @param radius
     * @return
     */
	public static boolean isPointInsideCircle(int px, int py, int cx, int cy, int radius) {
		int dx = px - cx;
		int dy = py - cy;
		int dist2 = dx * dx + dy * dy;
		return dist2 <= (radius * radius);
	}
	
	/**
	 * 
	 * @param px0
	 * @param py0
	 * @param r0
	 * @param px1
	 * @param py1
	 * @param r1
	 * @return
	 */
	public static boolean isCirclesOverlapping(int px0, int py0, int r0, int px1,int py1, int r1) {
		float d = Utils.distSqPointPoint(px0, py0, px1, py1);
		float d2 = r0+r1;
		d2 *= d2;
		return d < d2;
	}

	/**
	 * 
	 * @param degrees
	 * @return
	 */
	public static float sine(float degrees) {
		return (float)Math.sin(degrees*DEG_TO_RAD);
	}
	
	/**
	 * 
	 * @param degrees
	 * @return
	 */
	public static float cosine(float degrees) {
		return (float)Math.cos(degrees*DEG_TO_RAD);
	}

    /**
     * Primary version: takesd input 2 VALID RECTANGLES!
     * @param x0
     * @param y0
     * @param w0
     * @param h0
     * @param x1
     * @param y1
     * @param w1
     * @param h1
     * @return true when rectangles overlapp
     */
	public static boolean isBoxesOverlapping(float x0, float y0, float w0, float h0,
											 float x1, float y1, float w1, float h1) {
		float cx0 = x0 + w0/2;
		float cy0 = y0 + h0/2;
		float cx1 = x1 + w1/2;
		float cy1 = y1 + h1/2;
		
		float dx = Math.abs(cx0 - cx1);
		float dy = Math.abs(cy0 - cy1);
		
		float minx = w0/2 + w1/2;
		float miny = h0/2 + h1/2;
		
		if (dx < minx && dy < miny)
			return true;
		
		return false;
	}

    /**
     * return random value in range (min,max) inclusive
     * @param min
     * @param max
     * @return
     */
    public static int randRange(int min, int max) {
        return (rand()) % (max-min+1) + min;
    }

    /**
     * return true or false
     * @return
     */
    public static boolean flipCoin() {
        return randRange(0,1) == 1;
    }

    /**
     * return random float in range (0,scale] exclusive
     * @param scale
     * @return
     */
    public static float randFloat(float scale) {
        return (float)(randGen.nextDouble() * scale);
    }

    /**
     * return random float in range (-scale, scale) exclusive
     * @param scale
     * @return
     */
    public static float randFloatX(float scale) {
        return (float)(randGen.nextDouble() * (scale*2) - scale);
    }

    /**
     * return length of x,y with 8% error
     * @param x
     * @param y
     * @return
     */
    public static int fastLen(int x, int y) {
        x = Math.abs(x);
        y = Math.abs(y);
        int mn = (x > y ? y : x);
        int ret = (x+y-(mn/2)-(mn/4)+(mn/16));
        return ret;
    }
 
    /**
     * return approx len of x, y
     * @param x
     * @param y
     * @return
     */
    public static float fastLen(float x, float y) {
        x = Math.abs(x);
        y = Math.abs(y);
        float mn = (x > y ? y : x);
        float ret = (x+y-(mn/2)-(mn/4)+(mn/16));
        return ret;
    }

    /**
     * Return the anle of a vector
     * @param x
     * @param y
     * @return
     */
    public static int angle(float x, float y) {
        if (Math.abs(x) < EPSILON)
            return (y > 0 ? 90 : 270);
        int r = (int)Math.round(Math.atan(y/x) * RAD_TO_DEG);
        return (x < 0 ? 180 + r : r < 0 ? 360 + r : r);
    }
    
    /**
     * 
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @return
     */
    public static float distSqPointPoint(float x0, float y0, float x1, float y1) {
    	float dx = x0-x1;
    	float dy = y0-y1;
    	float d2 = dx*dx + dy*dy;
    	return d2;
    }
    
    /**
     * 
     * @param point_x
     * @param point_y
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @return
     */
    public static float distSqPointLine(float point_x, float point_y, float x0, float y0, float x1, float y1) {
        // get the normal (N) to the line
        float nx = -(y1 - y0);
        float ny = (x1 - x0);
        if (Math.abs(nx) < EPSILON && Math.abs(ny) < EPSILON) {
        	throw new RuntimeException("Degenerate Vector");
        	// TODO: treat this is a point?
        }
        // get the vector (L) from point to line
        float lx = point_x - x0;
        float ly = point_y - y0;
        
        // compute N dot N
        float ndotn = (nx*nx+ny*ny);
        // compute N dot L
        float ndotl = nx*lx+ny*ly;        
        // get magnitude squared of vector of L projected onto N
        float px = (nx * ndotl) / ndotn;
        float py = (ny * ndotl) / ndotn;
        return (px*px+py*py);
        
    }

    /**
     * Convenience mehtod
     * 
     * @param p_x
     * @param p_y
     * @param pts
     * @return
     */
    public static float distSqPointLine(float p_x, float p_y, float [] pts) {
    	return distSqPointLine(p_x, p_y, pts[0], pts[1], pts[2], pts[3]);
    }

    /**
     * Convenience method
     * 
     * @param p_x
     * @param p_y
     * @param pts
     * @return
     */
    public static float distSqPointLine(float p_x, float p_y, int [] pts) {
    	return distSqPointLine(p_x, p_y, pts[0], pts[1], pts[2], pts[3]);
    }
    
    /**
     * 
     * @param px
     * @param py
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @return
     */
    public static float distSqPointSegment(float px, float py, float x0, float y0, float x1, float y1) {
    	// compute vector rep of line
    	float lx = x1 - x0;
    	float ly = y1 - y0;
    	
    	// compute vector from p0 too p
    	float dx = px - x0;
    	float dy = py - y0;
    	
    	// dot product of d l
    	float dot = lx*dx + ly*dy;
    	
    	if (dot <= 0) {
    		return dx*dx + dy*dy;
    	}
    	
    	dx = px - x1;
    	dy = py - y1;
    	
    	dot = dx*lx + dy*ly;
    	
    	if (dot >= 0) {
    		return dx*dx + dy*dy;
    	}
    	
    	return Utils.distSqPointLine(px, py, x0, y0, x1, y1);
    }

	/**
	 * 
	 * @return
	 */
	public static int rand() {
		return Math.abs(randGen.nextInt());
	}
	
	/**
	 * 
	 * @param value
	 * @param min
	 * @param max
	 * @return
	 */
	public static float clamp(float value, float min, float max) {
		if (value < min)
			return min;
		if (value > max)
			return max;
		return value;
	}

	/**
	 * 
	 * @param value
	 * @param min
	 * @param max
	 * @return
	 */
	public static int clamp(int value, int min, int  max) {
		if (value < min)
			return min;
		if (value > max)
			return max;
		return value;
	}
	
	/**
	 * Return a value in the range: [0 - counts.length) where the weight of
	 * each values weight is in weights[]  
	 * @param counts
	 * @return
	 */
	public static int chooseRandomFromSet(int [] weights) {
		int i, total = 0;
		for (i=0; i<weights.length; i++)
			total += weights[i];
		if (total <= 0) {
			return 0;
		}      
		int r = rand() % total;
		for (i=0; i<weights.length; i++) {
			if (weights[i] <= r) {
				r -= weights[i];
			} else {
				break;
			}
		}
		assert(weights[i] > 0);
		return i;
	}
	
	/**
	 * 
	 * @param elems
	 * @return
	 */
	public static float sum(float [] elems) {
	    return sum(elems, 0, elems.length);
	}
	
	/**
     * 
     * @param elems
     * @return
     */
    public static float sum(float [] elems, int offset, int len) {
        float sum = 0;
        for (int i=offset; i<len; i++)
            sum += elems[i];
        return sum;
    }

	/**
	 * 
	 * @param elems
	 * @return
	 */
	public static int sum(int [] elems) {
	    return sum(elems, 0, elems.length);
	}

	   /**
     * 
     * @param elems
     * @return
     */
    public static int sum(int [] elems, int offset,int len) {
        int sum = 0;
        for (int i=offset; i<len; i++)
            sum += elems[i];
        return sum;
    }

	/**
	 * 
	 * @param elems
	 * @return
	 */
	public static float average(float [] elems) {
		return sum(elems) / elems.length;
	}
	
	/**
	 * 
	 * @param elems
	 * @param average
	 * @return
	 */
	public static float stdDev(float [] elems, float average) {
		float sum=0;
		for (int i=0; i<elems.length; i++) {
			float ds = elems[i]-average;
			sum += ds*ds;
		}
		float stdDev = (float)Math.sqrt(sum * 1.0/(elems.length-1));
		return stdDev;
	}

	public interface VertexList {
	    public void vertex(float x, float y);
	}
	
	/**
	 * Initialize x_pts and y_pts arrays with coordinate to the Beizer curve
	 * formed by 4 control points.
	 * 
	 * @param x_pts
	 * @param y_pts
	 * @param ctrl_x0
	 * @param ctrl_y0
	 * @param ctrl_x1
	 * @param ctrl_y1
	 * @param ctrl_x2
	 * @param ctrl_y2
	 * @param ctrl_x3
	 * @param ctrl_y3
	 */
	public static void computeBezierCurvePoints(int [] x_pts, int [] y_pts, float ctrl_x0, float ctrl_y0, float ctrl_x1, float ctrl_y1, float ctrl_x2, float ctrl_y2, float ctrl_x3, float ctrl_y3) {
		
		Utils.assertTrue(x_pts.length == y_pts.length);
		
		// construct the matrix for a beizer curve
		int steps = x_pts.length - 1;
		//Point [] points = new Point[steps+1];
	
		float step = 1.0f/steps;
		int pt = 0;
		for (float t=0; t<1.0f; t+=step) {
	        float fW = 1 - t; 
	        float fA = fW * fW * fW;
	        float fB = 3 * t * fW * fW; 
	        float fC = 3 * t * t * fW;
	        float fD = t * t * t;
	        float fX = fA * ctrl_x0 + fB * ctrl_x1 + fC * ctrl_x2 + fD * ctrl_x3; 
	        float fY = fA * ctrl_y0 + fB * ctrl_y1 + fC * ctrl_y2 + fD * ctrl_y3;
	        x_pts[pt] = Math.round(fX);
	        y_pts[pt] = Math.round(fY);
	        pt++;
		}
		
		x_pts[pt] = Math.round(ctrl_x3);
		y_pts[pt] = Math.round(ctrl_y3);
		
		pt ++;
		
	}
	
	public static void renderBSpline(VertexList g, int divisions, Vector2D ... controlPts) {
	    
	    if (controlPts.length < 4)
	        return;
	    
	    Vector2D P0 = new Vector2D(controlPts[0]);
        Vector2D P1 = new Vector2D(controlPts[1]);
        Vector2D P2 = new Vector2D(controlPts[2]);
        Vector2D P3 = new Vector2D(controlPts[3]);
	    
        float [] a = new float[5];
        float [] b = new float[5];
        
        int i=3;
        while (true) {

            a[0] = (-P0.X() + 3 * P1.X() - 3 * P2.X() + P3.X()) / 6.0f;
            a[1] = (3 * P0.X() - 6 * P1.X() + 3 * P2.X()) / 6.0f;
            a[2] = (-3 * P0.X() + 3 * P2.X()) / 6.0f;
            a[3] = (P0.X() + 4 * P1.X() + P2.X()) / 6.0f;
            b[0] = (-P0.Y() + 3 * P1.Y() - 3 * P2.Y() + P3.Y()) / 6.0f;
            b[1] = (3 * P0.Y() - 6 * P1.Y() + 3 * P2.Y()) / 6.0f;
            b[2] = (-3 * P0.Y() + 3 * P2.Y()) / 6.0f;
            b[3] = (P0.Y() + 4 * P1.Y() + P2.Y()) / 6.0f;

            g.vertex((float)a[3], (float)b[3]);
                ;
            for (int ii=1; ii<=divisions-1; ii++)
            { 
                double t = (double)ii/divisions;
                double x = (a[2] + t * (a[1] + t * a[0]))*t+a[3];
                double y = (b[2] + t * (b[1] + t * b[0]))*t+b[3];
                g.vertex((float)x,  (float)y);
            }

            if (i++>=controlPts.length)
                break;
            
            P0=P1; P1=P2; P2=P3; P3=new Vector2D(controlPts[i]);
        }
	}
	
	/**
	 * 
	 * @author Chris Caron
	 *
	 */
	public static interface Weighted {
		public int getWeight();
	};
	
	/**
	 * 
	 * @param values
	 * @return
	 */
	public static int max(Weighted [] values) {
		int m = Integer.MIN_VALUE;
		for (int i=0; i<values.length; i++)
			if (values[i].getWeight() > m)
				m = values[i].getWeight();
		return m;
		
	}

	/**
	 * 
	 * @author Chris Caron
	 *
	 */
	public interface Weigher {
		public int weightOf(Object o);
	};
	
	/**
	 * 
	 * @param values
	 * @param weigher
	 * @return
	 */
	public static int max(Object [] values, Weigher weigher) {
		int m = Integer.MIN_VALUE;
		for (int i=0; i<values.length; i++) {
			int weight = weigher.weightOf(values[i]);
			if (weight > m)
				m = weight;
		}
		return m;
		
	}

	/**
	 * 
	 * @param values
	 * @param a
	 * @param b
	 */
	public static void swapElems(int [] values, int a, int b) {
		int t = values[a];
		values[a] = values[b];
		values[b] = t;
	}
	
	public static <T> void swapElems(List<T> elems, int a, int b) {
		T t = elems.get(a);
		elems.set(a, elems.get(b));
		elems.set(b, t);
	}
	
	/**
	 * 
	 * @param values
	 * @param a
	 * @param b
	 */
	public static void swapElems(float [] values, int a, int b) {
		float t = values[a];
		values[a] = values[b];
		values[b] = t;
	}
	
	/**
	 * 
	 * @param values
	 * @param a
	 * @param b
	 */
	public static void swapElems(Object [] values, int a, int b) {
		Object t = values[a];
		values[a] = values[b];
		values[b] = t;
	}

	/**
	 * 
	 * @param dest
	 * @param source
	 */
	public static <T> void copyElems(T [] dest, T [] source) {
		int min = Math.min(source.length, dest.length);
		for (int i=0; i<min; i++) {
			dest[i] = source[i];
		}
	}

	/**
	 * 
	 * @param dest
	 * @param source
	 */
	public static void copyElems(int [] dest, int [] source) {
		int min = Math.min(source.length, dest.length);
		for (int i=0; i<min; i++) {
			dest[i] = source[i];
		}
	}

	/**
	 * 
	 * @param <T>
	 * @param dest
	 * @param value
	 * @param start
	 * @param end
	 */
	public static <T> void fillArray(T [] dest, T value, int start, int end) {
		for (int i=start; i<=end; i++)
			dest[i] = value;
	}

	/**
	 * 
	 * @param <T>
	 * @param dest
	 * @param value
	 */
	public static <T> void fillArray(T [] dest, T value) {
		fillArray(dest, value, 0, dest.length-1);
	}

	/**
	 * 
	 * @param <T>
	 * @param dest
	 * @param value
	 * @param start
	 * @param end
	 */
	public static void fillArray(int [] dest, int value, int start, int end) {
		for (int i=start; i<=end; i++)
			dest[i] = value;
	}

	/**
	 * 
	 * @param <T>
	 * @param dest
	 * @param value
	 */
	public static void fillArray(int [] dest, int value) {
		fillArray(dest, value, 0, dest.length-1);
	}

	/**
	 * 
	 * @param 
	 * @param dest
	 * @param value
	 * @param start
	 * @param end
	 */
	public static void fillArray(boolean [] dest, boolean value, int start, int end) {
		for (int i=start; i<=end; i++)
			dest[i] = value;
	}

	/**
	 * 
	 * @param <T>
	 * @param dest
	 * @param value
	 */
	public static void fillArray(boolean [] dest, boolean value) {
		fillArray(dest, value, 0, dest.length-1);
	}

	/**
	 * 
	 * @param elems
	 * @param start
	 * @param end
	 */
	public static void shuffle(Object [] elems, int start, int end) {
		for (int i=0; i<1000; i++) {
			int a = Utils.randRange(start, end-1);
			int b = Utils.randRange(start, end-1);
			Utils.swapElems(elems, a, b);
		}
	}
	
	public static <T> void shuffle(List<T> elems) {
		for (int i=0; i<1000; i++) {
			int a = Utils.rand() % elems.size();
			int b = Utils.rand() % elems.size();
			Utils.swapElems(elems, a, b);
		}
	}
	
	/**
	 * 
	 * @param elems
	 * @param len
	 */
	public static void shuffle(Object [] elems, int len) {
		shuffle(elems, 0, len);
	}
	
	/**
	 * 
	 * @param elems
	 */
	public static void shuffle(Object [] elems) {
		shuffle(elems, 0, elems.length);
	}

	/**
	 * 
	 * @param elems
	 * @param start
	 * @param end
	 */
	public static void shuffle(int [] elems, int start, int end) {
		for (int i=0; i<1000; i++) {
			int a = Utils.randRange(start, end-1);
			int b = Utils.randRange(start, end-1);
			Utils.swapElems(elems, a, b);
		}
	}
	
	/**
	 * 
	 * @param elems
	 * @param len
	 */
	public static void shuffle(int [] elems, int len) {
		shuffle(elems, 0, len-1);
	}
	
	/**
	 * 
	 * @param elems
	 */
	public static void shuffle(int [] elems) {
		shuffle(elems, 0, elems.length-1);
	}
	
	/**
	 * 
	 * @param elems
	 * @param start
	 * @param end
	 */
	public static void shuffle(float [] elems, int start, int end) {
		for (int i=0; i<1000; i++) {
			int a = Utils.randRange(start, end);
			int b = Utils.randRange(start, end);
			Utils.swapElems(elems, a, b);
		}
	}
	
	/**
	 * 
	 * @param elems
	 * @param len
	 */
	public static void shuffle(float [] elems, int len) {
		shuffle(elems, 0, len-1);
	}
	
	/**
	 * 
	 * @param elems
	 */
	public static void shuffle(float [] elems) {
		shuffle(elems, 0, elems.length-1);
	}

	/**
	 * Call type.newInstance() on each elem of the table
	 * @param table
	 * @param type
	 */
	public static <T> void initTable(T [] table, Class<T> type) {
		try {
			for (int i=0; i<table.length; i++) {
				table[i] = type.newInstance();
			}
		} catch (Exception e) { 
			throw new RuntimeException(e); 
		}
	}
	
	/**
	 * 
	 * @param c
	 */
	public static void unhandledCase(int c) {
		RuntimeException e = new RuntimeException("Unhandled case [" + c + "]");
		e.printStackTrace();
		throw e;
	}

	/**
	 * 
	 * @param seed
	 */
	public static void setRandomSeed(long seed) {
		randGen.setSeed(seed);
	}
	
	public static String toString(int [] array) {
		StringBuffer buf = new StringBuffer("[");
		int i = 0;
		for (; i<array.length-1; i++)
			buf.append(array[i]).append(", ");
		buf.append(array[i]).append("]");
		return buf.toString();
	}
	
	/**
     * 
     * @param color
     * @return
     */
    public static int rgbaToInt(int r, int g, int b, int a) {
        int d = (a << 24) | (r << 16) | (g << 8) | (b << 0);
        return d;
    }

    /**
     * 
     * @return
     */
    public static Random getRandom() {
        return randGen;
    }

    /**
     * 
     * @param n
     * @return
     */
    public static int nearestPowerOf2(int n) {
        return (int)(Math.pow( 2, Math.ceil( Math.log( n ) / Math.log( 2 ))));         
    }
    
    private static <T extends Number> T max2(T a, T b) {
        if (a.doubleValue() > b.doubleValue())
            return a;
        return b;
    }

    private static <T extends Number> T min2(T a, T b) {
        if (a.doubleValue() < b.doubleValue())
            return a;
        return b;
    }

    public static <T extends Number> T max(T t0, T t1, T ... args) {
        T max = max2(t0, t1);
        for (T t : args) {
            max = max2(max, t);
        }
        return max;
    }

    public static <T extends Number> T min(T t0, T t1, T ... args) {
        T max = min2(t0, t1);
        for (T t : args) {
            max = min2(max, t);
        }
        return max;
    }

	public static int[] copyOf(int [] arr) {
		if (arr == null)
			return null;
		int [] copy= new int[arr.length];
		System.arraycopy(arr, 0, copy, 0, arr.length);
		return copy;
	}

	public static boolean[] copyOf(boolean [] arr) {
		if (arr == null)
			return null;
		boolean [] copy= new boolean[arr.length];
		System.arraycopy(arr, 0, copy, 0, arr.length);
		return copy;
	}

	public static long[] copyOf(long [] arr) {
		if (arr == null)
			return null;
		long [] copy= new long[arr.length];
		System.arraycopy(arr, 0, copy, 0, arr.length);
		return copy;
	}

	public static double[] copyOf(double [] arr) {
		if (arr == null)
			return null;
		double[] copy= new double[arr.length];
		System.arraycopy(arr, 0, copy, 0, arr.length);
		return copy;
	}

	public static float[] copyOf(float [] arr) {
		if (arr == null)
			return null;
		float [] copy= new float[arr.length];
		System.arraycopy(arr, 0, copy, 0, arr.length);
		return copy;
	}
	
	public static <T> List<T> asList(T ... arr) {
		return new ArrayList<T>(Arrays.asList(arr));
	}
	
	public static <T> void setElems(T [] arr, T ... elems) {
		System.arraycopy(elems, 0, arr, 0, elems.length);
	}

	public static void setElems(byte [] arr, byte ... elems) {
		System.arraycopy(elems, 0, arr, 0, elems.length);
	}

	public static void setElems(int [] arr, int ... elems) {
		System.arraycopy(elems, 0, arr, 0, elems.length);
	}

	public static void setElems(float  [] arr, float ... elems) {
		System.arraycopy(elems, 0, arr, 0, elems.length);
	}

	public static void setElems(long [] arr, long ... elems) {
		System.arraycopy(elems, 0, arr, 0, elems.length);
	}

	public static void setElems(double [] arr, double ... elems) {
		System.arraycopy(elems, 0, arr, 0, elems.length);
	}
	
	public static void setDebugEnabled(boolean enable) {
		DEBUG_ENABLED = enable;
		if (enable) {
			randGen.setSeed(0);
		}
	}
	
	public static <T> T randItem(List<T> items) {
		return items.get(rand() % items.size());
	}
	
	/**
	 * Return a random entry from an array
	 * @param items
	 * @return
	 */
	public static <T> T randItem(T [] items) {
		return randItem(items, 0, items.length);
	}
	
	/**
	 * 
	 * @param items
	 * @param offset
	 * @param len
	 * @return
	 */
	public static <T> T randItem(T [] items, int offset, int len) {
		if (items == null || items.length == 0)
			return null;
		return items[offset + rand() % len];
	}

	/**
	 * Populate result array from an array of strings.
	 * 
	 * @param arr
	 * @param enumType
	 * @param result
	 * @return
	 */
	public static <T extends Enum<T>> T [] convertToEnumArray(String [] arr, Class<T> enumType, T [] result) {
        int num = Math.min(arr.length,  result.length);
		for (int i=0; i<num; i++) {
            arr[i] = arr[i].trim();
            if (arr[i].length() > 0)
                result[i] = Enum.valueOf(enumType, arr[i]);
        }
        return result;
    }

	/**
	 * Return string value of a number and its sign (+ / -)
	 * @param pts
	 * @return
	 */
	public static String getSignedString(int n) {
		if (n < 0)
			return String.valueOf(n);
		return "+" + n;
	}

	/**
	 * Return the next enum occurrence wrapping if necessary.
	 * 
	 * Example:
	 * 
	 * enum X {
	 *    A,B,C
	 * }
	 * 
	 * X result = incrementEnum(X.C, X.values());
	 * assert(result == X.A);
	 * 
	 * @param value
	 * @param values
	 * @return
	 */
    public static <T extends Enum<T>> T incrementEnum(T value, T [] values) {
		int ordinal = value.ordinal()+1;
		ordinal %= values.length;
		return values[ordinal];
	}
    
}
