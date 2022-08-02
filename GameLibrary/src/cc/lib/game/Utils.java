package cc.lib.game;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingFormatArgumentException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.lib.math.CMath;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.utils.FileUtils;
import cc.lib.utils.GException;
import cc.lib.utils.LRUCache;
import cc.lib.utils.Pair;

public class Utils {

    // USER SETTABLE VARS

    /**
     * Set too true to get debugging console
     */
    private static boolean DEBUG_ENABLED = false;

    // CONSTANTS

    // general working matricies, created once
    private final static float[] m_matrix_2x2 = new float[4];
    private final static float[] r_matrix_2x2 = new float[4];
    private static Random randGen = new Random(System.currentTimeMillis());

    public static void setDebugEnabled() {
        DEBUG_ENABLED = true;
        randGen = new Random(0);
    }

    public static boolean isDebugEnabled() {
        return DEBUG_ENABLED;
    }

    // FUCNTIONS

    /**
     * @param c
     */
    public static void unhandledCase(Object c) {
        GException e = new GException("Unhandled case [" + c + "]");
        e.printStackTrace();
        throw e;
    }

    /**
     * @param expr
     */
    public static void assertTrue(boolean expr) {
        assertTrue(expr, null);
    }

    /**
     * @param expr
     */
    public static void assertFalse(boolean expr) {
        assertTrue(!expr, "Expression is true");
    }

    public static void assertContains(Object o, Collection c) {
        if (DEBUG_ENABLED && !c.contains(o)) {
            throw new cc.lib.utils.GException("Object '" + o + "' is not contained by: " + c);
        }
    }

    /**
     * @param expr
     * @param msg
     * @param args
     */
    public static void assertTrue(boolean expr, String msg, Object... args) {
        if (!expr) {
            if (msg==null)
                msg = "Expresion Failed";
            if (DEBUG_ENABLED)
                throw new GException("ASSERT FAILED " + String.format(msg, args));
            else
                System.err.println(formatNoThrow(msg, args));
        }
    }

    /**
     * @param msg
     */
    public static void print(String msg) {
        if (DEBUG_ENABLED)
            System.out.print(msg);
    }

    /**
     *
     * @param c
     * @param <T>
     */
    public static <T> void printCollection(Collection<T> c) {
        if (DEBUG_ENABLED) {
            int index = 0;
            for (T t : c) {
                System.out.println(String.format("%3d:%s", index++, t));
            }
        }
    }

    /**
     * Use Logger instead
     * @param msg
     */
    @Deprecated
    public static void println(String msg) {
        if (DEBUG_ENABLED)
            System.out.println(msg);
    }

    /**
     * Use Logger instead
     *
     */
    @Deprecated
    public static void println() {
        if (DEBUG_ENABLED)
            System.out.println();
    }

    /**
     * Use logger instead
     * @param msg
     * @param args
     */
    @Deprecated
    public static void print(String msg, Object... args) {
        if (DEBUG_ENABLED) {
            System.out.print(String.format(msg, args));
        }
    }

    /**
     * Use logger instead
     * @param msg
     * @param args
     */
    @Deprecated
    public static void println(String msg, Object... args) {
        if (DEBUG_ENABLED) {
            System.out.println(String.format(msg, args));
        }
    }

    /**
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
     *
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param x3
     * @param y3
     * @return
     */
    public static boolean isLineSegsIntersecting(
            int x0, int y0, int x1, int y1, // line segment 1
            int x2, int y2, int x3, int y3) // line segment 2
    {
        switch (getLineSegsIntersection(x0, y0, x1, y1, x2, y2, x3, y3)) {
            case 0:
                return false;
            default:
                return true;
        }
    }

    /**
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

        if (!CMath.invert2x2Matrix(m_matrix_2x2, r_matrix_2x2)) {
            float cx0 = (x0 + x1) / 2; // center x of seg0
            float cy0 = (y0 + y1) / 2;

            float cx1 = (x2 + x3) / 2; // center x of seg1
            float cy1 = (y2 + y3) / 2;

            float dx = cx0 - cx1; // distance between centers
            float dy = cy0 - cy1;

            // dot product of normal to delta and one of the line segs
            float dot = -dy * (x1 - x0) + dx * (y1 - y0);
            if (Math.abs(dot) > CMath.EPSILON)
                return 0; // if the delta is not near parallel, then this is not intersecting

            float d = Math.abs((dx * dx) + (dy * dy)); // len^2 of delta

            float dx0 = (x1 - cx0);
            float dy0 = (y1 - cy0);

            float dx1 = (x3 - cx1);
            float dy1 = (y3 - cy1);

            // len^2 of 1/2 of the sum of the 2 segs
            float maxd = Math.abs((dx0 * dx0) + (dy0 * dy0) + (dx1 * dx1) + (dy1 * dy1));

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

        if (t0 < 0 || t0 > 1 || t1 < 0 || t1 > 1)
            return 0;

        return 1;
    }

    /**
     *
     * @param p0
     * @param p1
     * @param p2
     * @param p3
     * @return
     */
    public static int isLineSegsIntersecting(IVector2D p0, IVector2D p1, IVector2D p2, IVector2D p3) {
        return isLineSegsIntersecting(p0.getX(), p0.getY(),
                p1.getX(), p1.getY(),
                p2.getX(), p2.getY(),
                p3.getX(), p3.getY());
    }

    /**
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

        if (!CMath.invert2x2Matrix(m_matrix_2x2, r_matrix_2x2)) {
            float cx0 = (x0 + x1) / 2; // center x of seg0
            float cy0 = (y0 + y1) / 2;

            float cx1 = (x2 + x3) / 2; // center x of seg1
            float cy1 = (y2 + y3) / 2;

            float dx = cx0 - cx1; // distance between centers
            float dy = cy0 - cy1;

            // dot product of normal to delta and one of the line segs
            float dot = dx * (y1 - y0) - dy * (x1 - x0);
            if (Math.abs(dot) > CMath.EPSILON)
                return 0; // if the delta is not near parallel, then this is not intersecting

            float d = Math.abs((dx * dx) + (dy * dy)); // len^2 of delta

            float dx0 = (x1 - cx0);
            float dy0 = (y1 - cy0);

            float dx1 = (x3 - cx1);
            float dy1 = (y3 - cy1);

            // len^2 of 1/2 of the sum of the 2 segs
            float maxd = Math.abs((dx0 * dx0) + (dy0 * dy0) + (dx1 * dx1) + (dy1 * dy1));

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

        if (t0 < 0 || t0 > 1 || t1 < 0 || t1 > 1)
            return 0;

        return 1;
    }

    /**
     * @param px
     * @param py
     * @param rx
     * @param ry
     * @param rw
     * @param rh
     * @return
     */
    public static boolean isPointInsideRect(float px, float py, float rx, float ry, float rw, float rh) {
        if (px > rx && py > ry && px < rx + rw && py < ry + rh)
            return true;
        return false;
    }

    /**
     * @param px
     * @param py
     * @param xpts
     * @param ypts
     * @return
     */
    public static boolean isPointInsidePolygon(int px, int py, int[] xpts, int[] ypts, int numPts) {
        int orient = 0;

        for (int i = 0; i < numPts; i++) {
            int dx = px - xpts[i];
            int dy = py - ypts[i];

            int ii = (i + 1) % numPts;
            int dx2 = xpts[ii] - xpts[i];
            int dy2 = ypts[ii] - ypts[i];

            int nx = -dy2;
            int ny = dx2;

            int dot = nx * dx + ny * dy;

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
     * @param px
     * @param py
     * @param xpts
     * @param ypts
     * @return
     */
    public static boolean isPointInsidePolygonf(float px, float py, float[] xpts, float[] ypts, int numPts) {

        int orient = 0;

        for (int i = 0; i < numPts; i++) {
            float dx = px - xpts[i];
            float dy = py - ypts[i];

            int ii = (i + 1) % numPts;
            float dx2 = xpts[ii] - xpts[i];
            float dy2 = ypts[ii] - ypts[i];

            float nx = -dy2;
            float ny = dx2;

            float dot = nx * dx + ny * dy;

            if (Math.abs(dot) < CMath.EPSILON) {
                // ignore since this is 'on' the segment
            } else if (dot < 0) {
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

    public static boolean isPointInsidePolygon(float px, float py, IVector2D... pts) {
        return isPointInsidePolygon(px, py, pts, pts.length);
    }

    /**
     * @param px
     * @param py
     * @param pts
     * @return
     */
    public static boolean isPointInsidePolygon(float px, float py, IVector2D[] pts, int numPts) {
        if (numPts < 3)
            return false;
        List<IVector2D> l = new ArrayList<>();
        for (int i=0; i<numPts; i++) {
            l.add(pts[i]);
        }
        return isPointInsidePolygon(new Vector2D(px, py), l);
    }

    /**
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
     * @param px0
     * @param py0
     * @param r0
     * @param px1
     * @param py1
     * @param r1
     * @return
     */
    public static boolean isCirclesOverlapping(int px0, int py0, int r0, int px1, int py1, int r1) {
        float d = Utils.distSqPointPoint(px0, py0, px1, py1);
        float d2 = r0 + r1;
        d2 *= d2;
        return d < d2;
    }

    /**
     * Primary version: takesd input 2 VALID RECTANGLES!
     *
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
        float cx0 = x0 + w0 / 2;
        float cy0 = y0 + h0 / 2;
        float cx1 = x1 + w1 / 2;
        float cy1 = y1 + h1 / 2;

        float dx = Math.abs(cx0 - cx1);
        float dy = Math.abs(cy0 - cy1);

        float minx = w0 / 2 + w1 / 2;
        float miny = h0 / 2 + h1 / 2;

        if (dx < minx && dy < miny)
            return true;

        return false;
    }

    /**
     * return random value in range (min,max) inclusive
     *
     * @param min
     * @param max
     * @return
     */
    public static int randRange(int min, int max) {
        return (rand()) % (max - min + 1) + min;
    }

    /**
     * return true or false
     *
     * @return
     */
    public static boolean flipCoin() {
        return randRange(0, 1) == 1;
    }

    /**
     * return random float in range (0,scale] exclusive
     *
     * @param scale
     * @return
     */
    public static float randFloat(float scale) {
        if (scale == 0)
            return 0;
        return (float) (randGen.nextDouble() * scale);
    }

    /**
     *
     * @param min
     * @param max
     * @return
     */
    public static float randRangeFloat(float min, float max) {
        return min + randFloat(max-min);
    }

    /**
     * return random float in range (-scale, scale) exclusive
     *
     * @param scale
     * @return
     */
    public static float randFloatX(float scale) {
        return (float) (randGen.nextDouble() * (scale * 2) - scale);
    }

    /**
     * return length of x,y with 8% error
     *
     * @param x
     * @param y
     * @return
     */
    public static int fastLen(int x, int y) {
        x = Math.abs(x);
        y = Math.abs(y);
        int mn = (x > y ? y : x);
        int ret = (x + y - (mn / 2) - (mn / 4) + (mn / 16));
        return ret;
    }

    /**
     * return approx len of x, y
     *
     * @param x
     * @param y
     * @return
     */
    public static float fastLen(float x, float y) {
        x = Math.abs(x);
        y = Math.abs(y);
        float mn = (x > y ? y : x);
        float ret = (x + y - (mn / 2) - (mn / 4) + (mn / 16));
        return ret;
    }

    /**
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @return
     */
    public static float distSqPointPoint(float x0, float y0, float x1, float y1) {
        float dx = x0 - x1;
        float dy = y0 - y1;
        float d2 = dx * dx + dy * dy;
        return d2;
    }

    public static float distSqPointPoint(IVector2D v0, IVector2D v1) {
        return distSqPointPoint(v0.getX(), v0.getY(), v1.getX(), v1.getY());
    }

    /**
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
        if (Math.abs(nx) < CMath.EPSILON && Math.abs(ny) < CMath.EPSILON) {
            throw new GException("Degenerate Vector");
            // TODO: treat this is a point?
        }
        // get the vector (L) from point to line
        float lx = point_x - x0;
        float ly = point_y - y0;

        // compute N dot N
        float ndotn = (nx * nx + ny * ny);
        // compute N dot L
        float ndotl = nx * lx + ny * ly;
        // get magnitude squared of vector of L projected onto N
        float px = (nx * ndotl) / ndotn;
        float py = (ny * ndotl) / ndotn;
        return (px * px + py * py);

    }

    /**
     * Convenience mehtod
     *
     * @param p_x
     * @param p_y
     * @param pts
     * @return
     */
    public static float distSqPointLine(float p_x, float p_y, float... pts) {
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
    public static float distSqPointLine(float p_x, float p_y, int[] pts) {
        return distSqPointLine(p_x, p_y, pts[0], pts[1], pts[2], pts[3]);
    }

    /**
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

        // compute vector from p0 too unicodePattern
        float dx = px - x0;
        float dy = py - y0;

        // dot product of d l
        float dot = lx * dx + ly * dy;

        if (dot <= 0) {
            return dx * dx + dy * dy;
        }

        dx = px - x1;
        dy = py - y1;

        dot = dx * lx + dy * ly;

        if (dot >= 0) {
            return dx * dx + dy * dy;
        }

        return Utils.distSqPointLine(px, py, x0, y0, x1, y1);
    }

    /**
     * @return
     */
    public static int rand() {
        return Math.abs(randGen.nextInt());
    }

    /**
     * Clamp a value between min and mac inclusive
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
     * Clamp a value between min and mac inclusive
     *
     * @param value
     * @param min
     * @param max
     * @return
     */
    public static int clamp(int value, int min, int max) {
        if (value < min)
            return min;
        if (value > max)
            return max;
        return value;
    }

    /**
     * Return a value in the range: [0 - counts.length) where the weight of
     * each values weight is in weights[]
     *
     * @param weights
     * @return
     */
    public static int chooseRandomFromSet(int ... weights) {
        int i, total = 0;
        for (i = 0; i < weights.length; i++)
            total += weights[i];
        if (total <= 0) {
            return -1;
        }
        int r = rand() % total;
        for (i = 0; i < weights.length; i++) {
            if (weights[i] <= r) {
                r -= weights[i];
            } else {
                break;
            }
        }
        Utils.assertTrue (weights[i] > 0);
        return i;
    }

    /**
     *
     * @param items
     * @param weights
     * @param <T>
     * @return
     */
    public static <T> T chooseRandomWeightedItem(List<T> items, int [] weights) {
        int index = chooseRandomFromSet(weights);
        return items.get(index);
    }

    /**
     * @param elems
     * @return
     */
    public static float sum(float[] elems) {
        return sum(elems, 0, elems.length);
    }

    /**
     * @param elems
     * @return
     */
    public static float sum(float[] elems, int offset, int len) {
        float sum = 0;
        for (int i = offset; i < len; i++)
            sum += elems[i];
        return sum;
    }

    /**
     * @param elems
     * @return
     */
    public static int sum(int[] elems) {
        return sum(elems, 0, elems.length);
    }

    /**
     * @param elems
     * @return
     */
    public static long sum(long[] elems) {
        return sum(elems, 0, elems.length);
    }

    /**
     * @param elems
     * @return
     */
    public static long sum(Long[] elems) {
        return sum(elems, 0, elems.length);
    }

    /**
     * @param elems
     * @return
     */
    public static int sum(int[] elems, int offset, int len) {
        int sum = 0;
        for (int i = offset; i < len; i++)
            sum += elems[i];
        return sum;
    }

    /**
     * @param elems
     * @return
     */
    public static long sum(long[] elems, int offset, int len) {
        long sum = 0;
        for (int i = offset; i < len; i++)
            sum += elems[i];
        return sum;
    }

    /**
     * @param elems
     * @return
     */
    public static long sum(Long[] elems, int offset, int len) {
        long sum = 0;
        for (int i = offset; i < len; i++)
            sum += elems[i];
        return sum;
    }

    /**
     * @param elems
     * @return
     */
    public static float average(float[] elems) {
        return sum(elems) / elems.length;
    }

    /**
     * @param elems
     * @param average
     * @return
     */
    public static float stdDev(float[] elems, float average) {
        float sum = 0;
        for (int i = 0; i < elems.length; i++) {
            float ds = elems[i] - average;
            sum += ds * ds;
        }
        float stdDev = (float) Math.sqrt(sum * 1.0 / (elems.length - 1));
        return stdDev;
    }

    /**
     * Perform element wise comparison for equality
     * @param m0
     * @param m1
     * @return
     */
    public static boolean isEquals(Map m0, Map m1) {
        if (m0 == null && m1 == null)
            return true;
        if (m0 == null || m1 == null)
            return false;
        if (m0.size() != m1.size())
            return false;
        for (Object k : m0.keySet()) {
            if (!m0.get(k).equals(m1.get(k)))
                return false;
        }
        return true;
    }

    /**
     * Perform elemwise comparison for equality
     * length and elem positions are required for equality
     *
     * @param c0
     * @param c1
     * @return
     */
    public static boolean isEquals(Collection c0, Collection c1) {
        if (c0 == null && c1 == null)
            return true;
        if (c0 == null || c1 == null)
            return false;
        if (c0.size() != c1.size())
            return false;
        Iterator i0 = c0.iterator();
        Iterator i1 = c1.iterator();
        while (i0.hasNext()) {
            if (!i0.next().equals(i1.next()))
                return false;
        }
        return true;
    }

    public static String genRandomString(int length) {
        StringBuffer buf = new StringBuffer(length);
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUV1234567890";
        for (int i=0; i<length; i++) {
            buf.append(chars.charAt(Utils.rand() % chars.length()));
        }
        return buf.toString();
    }

    private static Pattern PRETTY_STRING_PATTERN = null;

    private static LRUCache<String, String> PRETTY_CACHE = new LRUCache<>(256);

    /**
     * 1. Strip extension if any
     * 2. Replace [_]+ (underscores) with a space
     * 3. Make whole strings lowercase with first letter capitalized
     *
     * @param obj
     * @return
     */
    public static String toPrettyString(final Object obj) {
        if (obj == null)
            return "null";
        String str = obj.toString();
        String cached = PRETTY_CACHE.get(str);
        if (cached != null)
            return cached;
        String pretty = FileUtils.stripExtension(str.replaceAll("[_]+", " ").trim());
        if (PRETTY_STRING_PATTERN == null)
            PRETTY_STRING_PATTERN = Pattern.compile("([A-Za-z][a-zA-Z]+)|([IiAa])");
        Matcher us = PRETTY_STRING_PATTERN.matcher(pretty);
        StringBuffer result = new StringBuffer();
        int begin = 0;
        while (us.find()) {
            String s = us.group().toLowerCase();
            if (result.length() > 0 && result.charAt(result.length()-1) != ' ' && pretty.charAt(begin) != ' ' )
                result.append(" ");
            result.append(pretty.substring(begin, us.start()));
            begin = us.end();
            if (result.length() > 0 && result.charAt(result.length()-1) != ' ')
                result.append(" ");
            result.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1));
        }
        if (begin >= 0 && begin < pretty.length()) {
            if (result.length() > 0 && result.charAt(result.length()-1) != ' ' && pretty.charAt(begin) != ' ')
                result.append(" ");
            result.append(pretty.substring(begin));
        }
        cached = result.toString();
        PRETTY_CACHE.put(str, cached);
        return cached;
    }

    public static String formatTime(long l) {

        if (l < 1000) {
            return String.format("%d millis", l);
        }

        long secs = l/1000;
        long mins = secs / 60;
        secs -= mins*60;

        long hrs = mins / 60;
        mins -= hrs*60;

        if (hrs == 0) {
            return String.format("%d:%02d", mins, secs);
        }

        return String.format("%d:%02d:%02d", hrs, mins, secs);
    }

    public enum EllipsisStyle {
        NONE,
        MIDDLE,
        END,
        INFO
    };

    public static String truncate(Object o, final int maxChars, int maxLines) {
        return truncate(o, maxChars, maxLines, EllipsisStyle.NONE);
    }

    public static String truncate(Object o, final int maxChars, int maxLines, EllipsisStyle eStyle) {
        if (o == null)
            return null;
        int charsLeft = maxChars;
        String s = o.toString();
        final int len = s.length();
        String result = "";
        for (int i=0; i<maxLines; i++) {
            if (s.length() == 0)
                break;
            if (result.length()>0)
                result += "\n";
            int endl = s.indexOf('\n');
            if (endl > charsLeft) {
                result += s.substring(0, charsLeft);
                break;
            } else if (endl >= 0) {
                result += s.substring(0, endl);
                s = s.substring(endl+1);
                charsLeft -= endl;
            } else if (s.length() > charsLeft) {
                result += s.substring(0, charsLeft);
                break;
            } else {
                result += s;
                s = "";
                break;
            }
        }

        if (s.length() > 0) {
            switch (eStyle) {

                case NONE:
                    break;
                case MIDDLE:
                    if (maxLines == 1 && result.length()>3) {
                        result = result.substring(0, result.length()/2-1) + "..." + result.substring(result.length()/2+1);
                    }
                    break;
                case END:
                    result += "...";
                    break;
                case INFO:
                    result += "\n  truncated to " + maxLines + " lines " + " (" + len + ") bytes";
                    break;
            }
        }

        return result;
    }

    public interface VertexList {
        void vertex(float x, float y);
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
    public static void computeBezierCurvePoints(int[] x_pts, int[] y_pts, float ctrl_x0, float ctrl_y0, float ctrl_x1, float ctrl_y1, float ctrl_x2, float ctrl_y2, float ctrl_x3, float ctrl_y3) {

        Utils.assertTrue(x_pts.length == y_pts.length);

        // construct the matrix for a beizer curve
        int steps = x_pts.length - 1;
        //Point [] points = new Point[steps+1];

        float step = 1.0f / steps;
        int pt = 0;
        for (float t = 0; t < 1.0f; t += step) {
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

        pt++;
    }

    /**
     * @param out
     * @param start
     * @param num
     * @param ctrl0
     * @param ctrl1
     * @param ctrl2
     * @param ctrl3
     */
    public static void computeBezierCurvePoints(Vector2D[] out, int start, int num, IVector2D ctrl0, IVector2D ctrl1, IVector2D ctrl2, IVector2D ctrl3) {
        computeBezierCurvePoints(out, 0, out.length, ctrl0, ctrl1, ctrl2, ctrl3);
    }

    /**
     * @param out
     * @param ctrl0
     * @param ctrl1
     * @param ctrl2
     * @param ctrl3
     */
    public static void computeBezierCurvePoints(Vector2D[] out, IVector2D ctrl0, IVector2D ctrl1, IVector2D ctrl2, IVector2D ctrl3) {
        computeBezierCurvePoints(out, out.length, ctrl0, ctrl1, ctrl2, ctrl3);
    }

    /**
     * @param out
     * @param num
     * @param ctrl0
     * @param ctrl1
     * @param ctrl2
     * @param ctrl3
     */
    public static void computeBezierCurvePoints(Vector2D[] out, int num, IVector2D ctrl0, IVector2D ctrl1, IVector2D ctrl2, IVector2D ctrl3) {
        int steps = out.length - 1;
        float step = 1.0f / steps;
        int pt = 0;
        for (float t = 0; t < 1.0f; t += step) {
            float fW = 1 - t;
            float fA = fW * fW * fW;
            float fB = 3 * t * fW * fW;
            float fC = 3 * t * t * fW;
            float fD = t * t * t;
            float fX = fA * ctrl0.getX() + fB * ctrl1.getX() + fC * ctrl2.getX() + fD * ctrl3.getX();
            float fY = fA * ctrl0.getY() + fB * ctrl1.getY() + fC * ctrl2.getY() + fD * ctrl3.getY();
            out[pt] = new Vector2D(fX, fY);
            pt++;
        }
        out[pt] = new Vector2D(ctrl3);
    }

    /**
     * @param g
     * @param divisions
     * @param controlPts
     */
    public static void renderBSpline(VertexList g, int divisions, Vector2D... controlPts) {

        if (controlPts.length < 4)
            return;

        Vector2D P0 = new Vector2D(controlPts[0]);
        Vector2D P1 = new Vector2D(controlPts[1]);
        Vector2D P2 = new Vector2D(controlPts[2]);
        Vector2D P3 = new Vector2D(controlPts[3]);

        float[] a = new float[5];
        float[] b = new float[5];

        int i = 3;
        while (true) {

            a[0] = (-P0.X() + 3 * P1.X() - 3 * P2.X() + P3.X()) / 6.0f;
            a[1] = (3 * P0.X() - 6 * P1.X() + 3 * P2.X()) / 6.0f;
            a[2] = (-3 * P0.X() + 3 * P2.X()) / 6.0f;
            a[3] = (P0.X() + 4 * P1.X() + P2.X()) / 6.0f;
            b[0] = (-P0.Y() + 3 * P1.Y() - 3 * P2.Y() + P3.Y()) / 6.0f;
            b[1] = (3 * P0.Y() - 6 * P1.Y() + 3 * P2.Y()) / 6.0f;
            b[2] = (-3 * P0.Y() + 3 * P2.Y()) / 6.0f;
            b[3] = (P0.Y() + 4 * P1.Y() + P2.Y()) / 6.0f;

            g.vertex((float) a[3], (float) b[3]);
            ;
            for (int ii = 1; ii <= divisions - 1; ii++) {
                double t = (double) ii / divisions;
                double x = (a[2] + t * (a[1] + t * a[0])) * t + a[3];
                double y = (b[2] + t * (b[1] + t * b[0])) * t + b[3];
                g.vertex((float) x, (float) y);
            }

            if (i++ >= controlPts.length)
                break;

            P0 = P1;
            P1 = P2;
            P2 = P3;
            P3 = new Vector2D(controlPts[i]);
        }
    }

    /**
     * @author Chris Caron
     */
    public interface Weighted {
        int getWeight();
    }

    ;

    /**
     * @param values
     * @return
     */
    public static int max(Weighted[] values) {
        int m = Integer.MIN_VALUE;
        for (int i = 0; i < values.length; i++)
            if (values[i].getWeight() > m)
                m = values[i].getWeight();
        return m;

    }

    /**
     * @author Chris Caron
     */
    public interface Weigher {
        int weightOf(Object o);
    }

    ;

    /**
     * @param values
     * @param weigher
     * @return
     */
    public static int max(Object[] values, Weigher weigher) {
        int m = Integer.MIN_VALUE;
        for (int i = 0; i < values.length; i++) {
            int weight = weigher.weightOf(values[i]);
            if (weight > m)
                m = weight;
        }
        return m;

    }

    /**
     * @param values
     * @param a
     * @param b
     */
    public static void swapElems(int[] values, int a, int b) {
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
     * @param values
     * @param a
     * @param b
     */
    public static void swapElems(float[] values, int a, int b) {
        float t = values[a];
        values[a] = values[b];
        values[b] = t;
    }

    /**
     * @param values
     * @param a
     * @param b
     */
    public static void swapElems(Object[] values, int a, int b) {
        Object t = values[a];
        values[a] = values[b];
        values[b] = t;
    }

    /**
     * @param dest
     * @param source
     */
    public static void copyElems(int[] dest, int ... source) {
        int min = Math.min(source.length, dest.length);
        for (int i = 0; i < min; i++) {
            dest[i] = source[i];
        }
    }

    /**
     * @param dest
     * @param source
     */
    public static void copyElems(float[] dest, float ... source) {
        int min = Math.min(source.length, dest.length);
        for (int i = 0; i < min; i++) {
            dest[i] = source[i];
        }
    }

    /**
     * @param dest
     * @param source
     */
    public static <T> void copyElems(T[] dest, T ... source) {
        int min = Math.min(source.length, dest.length);
        for (int i = 0; i < min; i++) {
            dest[i] = source[i];
        }
    }

    /**
     *
     * @param array
     * @param value
     */
    public static void fill(int [][] array, int value) {
        for (int i=0; i<array.length; i++) {
            for (int ii=0; ii<array[i].length; ii++)
                array[i][ii] = value;
        }
    }

    /**
     * @param elems
     * @param start
     * @param end
     */
    public static void shuffle(Object[] elems, int start, int end) {
        if (elems == null || elems.length < 2)
            return;
        int n = elems.length * 10;
        for (int i = 0; i < n; i++) {
            int a = i%elems.length;//Utils.randRange(start, end - 1);
            int b = Utils.rand() % elems.length;
            while (b == a) {
                b = Utils.rand() % elems.length;
            }

            Utils.swapElems(elems, a, b);
        }
    }

    public static <T> void shuffle(List<T> elems) {
        if (elems == null || elems.size() < 2)
            return;
        int n = elems.size() * 10;
        for (int i = 0; i < n; i++) {
            int a = i%elems.size();//Utils.rand() % elems.size();
            int b = Utils.rand() % elems.size();
            while (b == a) {
                b = Utils.rand() % elems.size();
            }
            Utils.swapElems(elems, a, b);
        }
    }

    /**
     * @param elems
     * @param len
     */
    public static void shuffle(Object[] elems, int len) {
        shuffle(elems, 0, len);
    }

    /**
     * @param elems
     */
    public static void shuffle(Object[] elems) {
        shuffle(elems, 0, elems.length);
    }

    /**
     * @param elems
     * @param start
     * @param end
     */
    public static void shuffle(int[] elems, int start, int end) {
        for (int i = 0; i < 1000; i++) {
            int a = i%elems.length;//Utils.randRange(start, end - 1);
            int b = Utils.randRange(start, end-1);
            Utils.swapElems(elems, a, b);
        }
    }

    /**
     * @param elems
     * @param len
     */
    public static void shuffle(int[] elems, int len) {
        shuffle(elems, 0, len);
    }

    /**
     * @param elems
     */
    public static void shuffle(int[] elems) {
        shuffle(elems, 0, elems.length);
    }

    /**
     * @param elems
     * @param start
     * @param end
     */
    public static void shuffle(float[] elems, int start, int end) {
        for (int i = 0; i < 1000; i++) {
            int a = i%elems.length;//Utils.randRange(start, end);
            int b = Utils.randRange(start, end-1);
            Utils.swapElems(elems, a, b);
        }
    }

    /**
     * @param elems
     * @param len
     */
    public static void shuffle(float[] elems, int len) {
        shuffle(elems, 0, len);
    }

    /**
     * @param elems
     */
    public static void shuffle(float[] elems) {
        shuffle(elems, 0, elems.length);
    }

    /**
     * Call type.newInstance() on each elem of the table
     *
     * @param table
     * @param type
     */
    public static <T> void initTable(T[] table, Class<T> type) {
        try {
            for (int i = 0; i < table.length; i++) {
                table[i] = type.newInstance();
            }
        } catch (Exception e) {
            throw new GException(e);
        }
    }

    /**
     * @param c
     */
    public static void unhandledCase(int c) {
        GException e = new GException("Unhandled case [" + c + "]");
        e.printStackTrace();
        throw e;
    }

    /**
     * @param seed
     */
    public static void setRandomSeed(long seed) {
        randGen.setSeed(seed);
    }

    public static String toString(int[] array, int startIndex, int len) {
        StringBuffer buf = new StringBuffer("[");
        int i = startIndex;
        for (; i < len - 1; i++)
            buf.append(array[i]).append(", ");
        if (len > 0)
            buf.append(array[i]).append("]");
        return buf.toString();
    }

    /**
     * @param array
     * @return
     */
    public static String toString(int[][] array) {
        StringBuffer buf = new StringBuffer("{");
        int i = 0;
        for (; i < array.length - 1; i++)
            buf.append(toString(array[i], 0, array[i].length)).append(", ");
        buf.append(toString(array[i], 0, array[i].length)).append("}");
        return buf.toString();
    }

    /**
     * Return a stacktrace string nicer than what is returned by Arrays.toString
     * @param st
     * @return
     */
    public static String toString(StackTraceElement [] st) {
        StringBuffer b = new StringBuffer();
        for (StackTraceElement e : st) {
            b.append(e.toString()).append("\n");
        }
        return b.toString();
    }

    /**
     * @param r
     * @param g
     * @param b
     * @param a
     * @return
     */
    public static int rgbaToInt(int r, int g, int b, int a) {
        int d = (a << 24) | (r << 16) | (g << 8) | (b << 0);
        return d;
    }

    /**
     * @return
     */
    public static Random getRandom() {
        return randGen;
    }

    /**
     * @param n
     * @return
     */
    public static int nearestPowerOf2(int n) {
        return (int) (Math.pow(2, Math.ceil(Math.log(n) / Math.log(2))));
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

    @SafeVarargs
    public static <T extends Number> T max(T... args) {
        Utils.assertTrue(args.length>1);
        T max = max2(args[0], args[1]);
        for (int i=2; i<args.length; i++) {
            max = max2(max, args[i]);
        }
        return max;
    }

    @SafeVarargs
    public static <T extends Number> T min(T... args) {
        Utils.assertTrue(args.length>1);
        T min = min2(args[0], args[1]);
        for (int i=2; i<args.length; i++) {
            min = min2(min, args[i]);
        }
        return min;
    }

    public static Integer [] copyOf(int[] arr, Integer [] copy) {
        for (int i =0; i<copy.length; i++) {
            copy[i] = arr[i];
        }
        return copy;
    }

    public static int[] copyOf(int[] arr) {
        if (arr == null)
            return null;
        int[] copy = new int[arr.length];
        System.arraycopy(arr, 0, copy, 0, arr.length);
        return copy;
    }


    public static boolean[] copyOf(boolean[] arr) {
        if (arr == null)
            return null;
        boolean[] copy = new boolean[arr.length];
        System.arraycopy(arr, 0, copy, 0, arr.length);
        return copy;
    }

    public static long[] copyOf(long[] arr) {
        if (arr == null)
            return null;
        long[] copy = new long[arr.length];
        System.arraycopy(arr, 0, copy, 0, arr.length);
        return copy;
    }

    public static double[] copyOf(double[] arr) {
        if (arr == null)
            return null;
        double[] copy = new double[arr.length];
        System.arraycopy(arr, 0, copy, 0, arr.length);
        return copy;
    }

    public static float[] copyOf(float[] arr) {
        if (arr == null)
            return null;
        float[] copy = new float[arr.length];
        System.arraycopy(arr, 0, copy, 0, arr.length);
        return copy;
    }

    public static <T> T[] copyOf(T [] arr) {
        if (arr == null)
            return null;
        Class<? extends T[]> newType = (Class<? extends T[]>) arr.getClass();
        T[] copy = ((Object)newType == (Object)Object[].class)
                ? (T[]) new Object[arr.length]
                : (T[]) Array.newInstance(newType.getComponentType(), arr.length);
        System.arraycopy(arr, 0, copy, 0, arr.length);
        return copy;
    }

    public static <T> List<T> asList(T[] arr, int start, int num) {
        List<T> list = new ArrayList<T>();
        for (int i = 0; i < num; i++) {
            list.add(arr[start + i]);
        }
        return list;
    }

    public static <T> List<T> asList(T... items) {
        List<T> list = new ArrayList<T>();
        for (T i : items) {
            list.add(i);
        }
        return list;
    }

    public static List<Integer> asList(int[] arr, int start, int num) {
        List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < num; i++) {
            list.add(arr[start + i]);
        }
        return list;
    }

    @SafeVarargs
    public static <T> void setElems(T[] arr, T... elems) {
        System.arraycopy(elems, 0, arr, 0, Math.min(arr.length, elems.length));
    }

    public static void setElems(boolean[] arr, boolean... elems) {
        System.arraycopy(elems, 0, arr, 0, Math.min(arr.length, elems.length));
    }

    public static void setElems(byte[] arr, byte... elems) {
        System.arraycopy(elems, 0, arr, 0, Math.min(arr.length, elems.length));
    }

    public static void setElems(int[] arr, int... elems) {
        System.arraycopy(elems, 0, arr, 0, Math.min(arr.length, elems.length));
    }

    public static void setElems(float[] arr, float... elems) {
        System.arraycopy(elems, 0, arr, 0, Math.min(arr.length, elems.length));
    }

    public static void setElems(long[] arr, long... elems) {
        System.arraycopy(elems, 0, arr, 0, Math.min(arr.length, elems.length));
    }

    public static void setElems(double[] arr, double... elems) {
        System.arraycopy(elems, 0, arr, 0, Math.min(arr.length, elems.length));
    }

    public static void setDebugEnabled(boolean enable) {
        DEBUG_ENABLED = enable;
    }

    /**
     *
     * @param items
     * @param <T>
     * @return
     */
    public static <T> T randItem(Collection<T> items) {
        int which = rand() % items.size();
        if (items instanceof List)
            return ((List<T>)items).get(which);
        Iterator<T> it = items.iterator();
        T result = it.next();
        for (int i=0; i<which; i++) {
            result = it.next();
        }
        return result;
    }

    /**
     * Return a random entry from an array
     *
     * @param items
     * @return
     */
    public static <T> T randItem(T[] items) {
        return randItem(items, 0, items.length);
    }

    /**
     * @param items
     * @param offset
     * @param len
     * @return
     */
    public static <T> T randItem(T[] items, int offset, int len) {
        if (items == null || items.length == 0)
            return null;
        return items[offset + rand() % len];
    }

    /**
     *
     * @param items
     * @param offset
     * @param len
     * @return
     */
    public static int randItem(int[] items, int offset, int len) {
        if (items == null || items.length == 0)
            return 0;
        return items[offset + rand() % len];
    }

    /**
     *
     * @param items
     * @return
     */
    public static int randItem(int[] items) {
        return randItem(items, 0, items.length);
    }

    /**
     * Return string value of a number and its sign (+ / -)
     *
     * @param n
     * @return
     */
    public static String getSignedString(int n) {
        if (n < 0)
            return String.valueOf(n);
        return "+" + n;
    }

    /**
     * Return string value of a number and its sign (+ / -)
     *
     * @param n
     * @return
     */
    public static String getSignedStringOrEmptyWhenZero(int n) {
        if (n == 0)
            return "";
        if (n < 0)
            return String.valueOf(n);
        return "+" + n;
    }

    /**
     * Return the next enum occurrence wrapping if necessary.
     * <unicodePattern>
     * Example:
     * <unicodePattern>
     * enum X {
     * A,B,C
     * }
     * <unicodePattern>
     * X result = incrementEnum(X.C, X.values());
     * assert(result == X.A);
     *
     * @param value
     * @param values
     * @return
     */
    public static <T extends Comparable<T>> T incrementValue(T value, T ... values) {
        int index = linearSearch(values, value);
        index = (index+1) % values.length;
        return values[index];
    }

    /**
     *
     * @param value
     * @param values
     * @param <T>
     * @return
     */
    public static <T extends Comparable<T>> T decrementValue(T value, T ... values) {
        int index = linearSearch(values, value);
        index = (index-1+values.length) % values.length;
        return values[index];
    }

    /**
     * Convert enum array to string array
     *
     * @param values
     * @return
     */
    public static <T extends Enum<T>> String[] toStringArray(T[] values) {
        return toStringArray(values, false);
    }

    public static <T> String [] toStringArray(Collection<T> values, Mapper<T, String> mapper) {
        String [] result = new String[values.size()];
        return map(values, result, mapper);
    }

    public static <T extends Enum<T>> String[] toStringArray(T[] values, boolean pretty) {
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = pretty ? toPrettyString(values[i].name()) : values[i].name();
        }
        return result;
    }

    public static String [] toStringArray(Collection items) {
        return toStringArray(items, false);
    }

    public static String [] toStringArray(Collection items, boolean pretty) {
        String [] result = new String[items.size()];
        int index=0;
        for (Object o : items) {
            result[index++] = o == null ? "null" : (pretty ? toPrettyString(o.toString()) : o.toString());
        }
        return result;
    }

    public static <T> String [] toStringArray(T [] items, boolean pretty) {
        String [] result = new String[items.length];
        int index=0;
        for (Object o : items) {
            result[index++] = o == null ? "null" : (pretty ? toPrettyString(o.toString()) : o.toString());
        }
        return result;
    }

    /**
     *
     * @param ints
     * @return
     */
    public static int [] toIntArray(Collection<Integer> ints) {
        int [] result = new int[ints.size()];
        int index = 0;
        for (Integer i : ints) {
            result[index++] = i;
        }
        return result;
    }

    /**
     *
     * @param floats
     * @return
     */
    public static float [] toFloatArray(Collection<Float> floats) {
        float [] result = new float[floats.size()];
        int index = 0;
        for (Float i : floats) {
            result[index++] = i;
        }
        return result;
    }

    /**
     * Sort a secondary array based on a primary array
     *
     * @param primary
     * @param target
     * @param comp
     * @param <T>
     * @param <S>
     */
    public static <T, S> void bubbleSort(T[] primary, S[] target, Comparator<T> comp) {
        bubbleSort(primary, target, Math.min(primary.length, target.length), comp);

    }

    /**
     *
     * @param primary
     * @param target
     * @param length
     * @param comp
     * @param <T>
     * @param <S>
     */
    public static <T, S> void bubbleSort(T[] primary, S[] target, int length, Comparator<T> comp) {
        Utils.assertTrue (target.length >= length && primary.length >= length);
        boolean swapped = false;
        do {
            swapped = false;
            for (int ii = 1; ii < length; ii++) {
                int c = comp.compare(primary[ii - 1], primary[ii]);
                if (c < 0) {
                    swapElems(primary, ii - 1, ii);
                    swapElems(target, ii - 1, ii);
                    swapped = true;
                }
            }
            length--;
        } while (swapped);
    }

    /**
     *
     * @param primary
     * @param target
     * @param <T>
     * @param <S>
     */
    public static <T extends Comparable<T>, S> void bubbleSort(T[] primary, S[] target) {
        bubbleSort(primary, target, Math.min(primary.length, target.length), false);
    }

    /**
     *
     * @param primary
     * @param target
     * @param length
     * @param <T>
     * @param <S>
     */
    public static <T extends Comparable<T>, S> void bubbleSort(T[] primary, S[] target, int length) {
        bubbleSort(primary, target, length, false);
    }

    /**
     *
     * @param primary
     * @param target
     * @param descending
     * @param <T>
     * @param <S>
     */
    public static <T extends Comparable<T>, S> void bubbleSort(T[] primary, S[] target, boolean descending) {
        bubbleSort(primary, target, Math.min(primary.length, target.length), descending);
    }

    /**
     *
     * @param primary
     * @param target
     * @param length
     * @param descending
     * @param <T>
     * @param <S>
     */
    public static <T extends Comparable<T>, S> void bubbleSort(T[] primary, S[] target, int length, boolean descending) {
        Utils.assertTrue (target.length >= primary.length);
        if (primary.length < 2)
            return; // already sorted
        boolean swapped;
        int n = primary.length;
        do {
            swapped = false;
            for (int ii = 1; ii < n; ii++) {
                int c = primary[ii - 1].compareTo(primary[ii]);
                if (descending) {
                    c = -c;
                }
                if (c > 0) {
                    swapElems(primary, ii - 1, ii);
                    swapElems(target, ii - 1, ii);
                    swapped = true;
                }
            }
            n--;
        } while (swapped);
    }

    /**
     *
     * @param arr
     * @param key
     * @param <T>
     * @return
     */
    public static <T> int linearSearch(T[] arr, T key) {
        return linearSearch(arr, key, arr.length);
    }

    /**
     *
     * @param arr
     * @param key
     * @param len
     * @param <T>
     * @return
     */
    public static <T> int linearSearch(T[] arr, T key, int len) {
        for (int i = 0; i < len; i++) {
            if (isEquals(arr[i], key))
                return i;
        }
        return -1;
    }

    public static boolean isEquals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    /**
     * Shift all array elements to the left and return item shifted off the front (zeroth element)
     * @param arr
     * @param <T>
     * @return
     */
    public static <T> T popFirst(T [] arr) {
        T top = arr[0];
        for (int i=0; i<arr.length-1; i++) {
            arr[i] = arr[i+1];
        }
        return top;
    }

    /**
     * Shift all array elements to the left and return item shifted off the front (zeroth element)
     * @param arr
     * @return
     */
    public static int popFirst(int [] arr) {
        int top = arr[0];
        for (int i=0; i<arr.length-1; i++) {
            arr[i] = arr[i+1];
        }
        return top;
    }

    /**
     * Shift all array elements to the right and return the item shift aff the back (nth-1 element)
     * The zeroth element will be item.
     *
     * @param arr
     * @param <T>
     * @return
     */
    public static <T> T pushFirst(T item, T [] arr) {
        T last = arr[arr.length-1];
        for (int i=arr.length-1; i>0; i--) {
            arr[i] = arr[i-1];
        }
        arr[0] = item;
        return last;
    }

    /**
     *
     * @param item
     * @param arr
     * @return
     */
    public static char pushFirst(char item, char [] arr) {
        char last = arr[arr.length-1];
        for (int i=arr.length-1; i>0; i--) {
            arr[i] = arr[i-1];
        }
        arr[0] = item;
        return last;
    }

    /**
     *
     * @param item
     * @param arr
     * @return
     */
    public static byte pushFirst(byte item, byte [] arr) {
        byte last = arr[arr.length-1];
        for (int i=arr.length-1; i>0; i--) {
            arr[i] = arr[i-1];
        }
        arr[0] = item;
        return last;
    }

    /**
     *
     * @param item
     * @param arr
     * @return
     */
    public static int pushFirst(int item, int [] arr) {
        int last = arr[arr.length-1];
        for (int i=arr.length-1; i>0; i--) {
            arr[i] = arr[i-1];
        }
        arr[0] = item;
        return last;
    }

    /**
     *
     * @param item
     * @param arr
     * @return
     */
    public static long pushFirst(long item, long [] arr) {
        long last = arr[arr.length-1];
        for (int i=arr.length-1; i>0; i--) {
            arr[i] = arr[i-1];
        }
        arr[0] = item;
        return last;
    }

    /**
     *
     * @param item
     * @param arr
     * @return
     */
    public static float pushFirst(float item, float [] arr) {
        float last = arr[arr.length-1];
        for (int i=arr.length-1; i>0; i--) {
            arr[i] = arr[i-1];
        }
        arr[0] = item;
        return last;
    }

    /**
     *
     * @param item
     * @param arr
     * @return
     */
    public static double pushFirst(double item, double [] arr) {
        double last = arr[arr.length-1];
        for (int i=arr.length-1; i>0; i--) {
            arr[i] = arr[i-1];
        }
        arr[0] = item;
        return last;
    }

    /**
     * Return var args list as an array
     *
     * @param arr
     * @return
     */
    @SafeVarargs
    public static <T> T[] toArray(T... arr) {
        return arr;
    }

    /**
     *
     * @param arr
     * @return
     */
    public static int [] toIntArray(int... arr) {
        return arr;
    }

    /**
     *
     * @param arr
     * @return
     */
    public static float [] toFloatArray(float... arr) {
        return arr;
    }

    /**
     *
     * @param arr
     * @return
     */
    public static long [] toLongArray(long... arr) {
        return arr;
    }

    /**
     *
     * @param arr
     * @return
     */
    public static double [] toDoubleArray(double... arr) {
        return arr;
    }

    /**
     * Returns true if object is null or 'empty'.
     * If o is a string, then empty means at least 1 non whitespace char.
     * If o is a collection then empty means sie() == 0
     * If o is an array then empty means length == 0
     * otherwise not empty
     *
     * @param o
     * @return
     */
    public static boolean isEmpty(Object o) {
        if (o == null)
            return true;
        if (o instanceof String)
            return ((String) o).trim().length() == 0;
        if (o instanceof Collection)
            return ((Collection) o).size() == 0;
        if (o.getClass().isArray())
            return Array.getLength(o) == 0;
        if (DEBUG_ENABLED)
            throw new IllegalArgumentException("isEmpty not compatible of object of type: " + o.getClass());
        System.err.println("isEmpty does not know about class " + o.getClass());
        return false;
    }

    /**
     *
     * @param c
     * @param length
     * @return
     */
    public static String getRepeatingChars(char c, int length) {
        String s = "";
        for (int i = 0; i < length; i++)
            s += c;
        return s;
    }

    /**
     *
     * @param argb
     * @return
     */
    public static int[] getARGB(int argb) {
        return new int[]{
                (argb >>> 24) & 0xff,
                (argb >> 16) & 0xff,
                (argb >> 8) & 0xff,
                (argb >> 0) & 0xff
        };
    }

    /**
     *
     * @param argb0
     * @param argb1
     * @param factor
     * @return
     */
    public static int interpolateColor(int argb0, int argb1, float factor) {

        int[] argb0A = getARGB(argb0);
        int[] argb1A = getARGB(argb1);

        int c = 0;

        for (int i = 0; i < 4; i++) {
            float comp = argb0A[i] * factor + argb1A[i] * (1.0f - factor);
            c |= Math.round(comp) << (8 * (3 - i));
        }

        return c;
    }

    /**
     *
     * @param pt
     * @param v0
     * @param v1
     * @return
     */
    public static boolean isPointInsideRect(IVector2D pt, IVector2D v0, IVector2D v1) {
        float px = pt.getX();
        float py = pt.getY();
        float x = Math.min(v0.getX(), v1.getX());
        float y = Math.min(v0.getY(), v1.getY());
        float w = Math.abs(v0.getX() - v1.getX());
        float h = Math.abs(v0.getY() - v1.getY());
        return Utils.isPointInsideRect(px, py, x, y, w, h);
    }

    /**
     *
     * @param lock
     * @param msecs
     */
    public static void waitNoThrow(Object lock, long msecs) {
        try {
            synchronized (lock) {
                if (msecs < 0)
                    lock.wait();
                else
                    lock.wait(msecs);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param msecs
     */
    public static void waitNoThrow(long msecs) {
        waitNoThrow(new Object(), msecs);
    }

    /**
     *
     * @param s
     * @return
     */
    public static String trimEnclosure(String s) {
        if (s == null) {
            return null;
        }
        if (s != null) {
            if (s.startsWith("\"") && s.endsWith("\"")) {
                return s.substring(1, s.length() - 1);
            }
            if (s.startsWith("'") && s.endsWith("'")) {
                return s.substring(1, s.length() - 1);
            }
            if (s.startsWith("[") && s.endsWith("]")) {
                return s.substring(1, s.length() - 1);
            }
            if (s.startsWith("{") && s.endsWith("}")) {
                return s.substring(1, s.length() - 1);
            }
            if (s.startsWith("(") && s.endsWith(")")) {
                return s.substring(1, s.length() - 1);
            }
            if (s.startsWith("<") && s.endsWith(">")) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }

    private final static Pattern unicodePattern = Pattern.compile("\\\\\\d\\d\\d"); // \123 hopefully

    /**
     * Take a string munged by our network libraries and turn it back into
     * a proper string (hopefully).
     * Fixes:
     * - Unicode characters converted into backslash-decimal-bytes
     * eg \123\234
     * - Backslash-doubling
     * - Backslash added before "
     * - Backslash added before (
     */
    public static String reUnicodify(String poorlyEscaped) {
        String ret = poorlyEscaped;
        if (ret == null)
            return null;
        Matcher m = unicodePattern.matcher(poorlyEscaped);
        StringBuilder sb = new StringBuilder();
        byte[] bytes = new byte[256];
        int byteIndex = 0;
        int lastEnd = 0;
        while (m.find()) {
            int start = m.start();
            if (start > lastEnd + 1) {
                if (byteIndex != 0) {
                    try {
                        sb.append(new String(bytes, 0, byteIndex, "UTF-8"));
                    } catch (java.io.UnsupportedEncodingException e) {
                        sb.append("?");
                        throw new cc.lib.utils.GException(e);
                    }

                    byteIndex = 0;
                }

                sb.append(poorlyEscaped.substring(lastEnd, start));
            }

            bytes[byteIndex++] = (byte) (Integer.parseInt(m.group().substring(1)));

            // Guard against very unlikely overflow
            if (byteIndex >= bytes.length) {
                byte[] realloc = new byte[bytes.length * 2];
                System.arraycopy(bytes, 0, realloc, 0, bytes.length);
                bytes = realloc;
            }

            lastEnd = m.end();
        }
        if (byteIndex != 0) {
            try {
                sb.append(new String(bytes, 0, byteIndex, "UTF-8"));

            } catch (java.io.UnsupportedEncodingException e) {
                sb.append("?");
                throw new cc.lib.utils.GException(e);
            }
            byteIndex = 0;
        }
        sb.append(poorlyEscaped.substring(lastEnd));
        // At this point, we've fixed all the emoji etc
        ret = sb.toString();
        // BUT WAIT THERE'S MORE!!
        // It's also doubling all backslashes, and putting backslashes
        // in front of double-quote " and open-paren.  So undo those.
        // First the backslash-double-quote.
        ret = ret.replaceAll("\\\\\"", "\"");
        // Next do paren.  \( to (
        ret = ret.replaceAll("\\(", "(");
        // Then the double-backslash
        // This looks like 8-to-4 but it's actually 2-to-1
        ret = ret.replaceAll("\\\\\\\\", "\\\\");
        return ret;
    }

    /**
     *
     * @param n
     * @return
     */
    public static int signOf(int n) {
        if (n < 0)
            return -1;
        return 1;
    }

    /**
     *
     * @param ip
     * @return
     */
    public static String getIpAddressString(int ip) {
        // http://stackoverflow.com/questions/1957637/java-convert-int-to-inetaddress
        String ipStr =
                String.format("%d.%d.%d.%d",
                        (ip & 0xff),
                        (ip >> 8 & 0xff),
                        (ip >> 16 & 0xff),
                        (ip >> 24 & 0xff));
        return ipStr;
    }

    /**
     *
     * @param stringResource
     * @param resources
     * @return
     */
    public static Map<Integer, String> buildStringsTable(Class<?> stringResource, String ... resources) {
        Map<Integer, String> table = new HashMap<>();
        try {
            for (String resource : resources) {
                processStringResource(stringResource, table, resource);
            }
        } catch (Exception e) {
            throw new cc.lib.utils.GException("Failed to process strings: " + e);
        }
        return table;
    }

    /**
     *
     * @param s
     * @return
     */
    public static String stripEnds(String s) {
        return s.substring(1, s.length()-1);
    }

    /**
     *
     * @param s
     * @param ending
     * @return
     */
    public static String chopEnd(String s, String ending) {
        if (s.endsWith(ending)) {
            return s.substring(0, s.length()-ending.length());
        }
        return s;
    }

    /**
     *
     * @param stringResource
     * @param stringTable
     * @param resource
     * @throws Exception
     */
    private static void processStringResource(Class<?> stringResource, Map<Integer, String> stringTable, String resource) throws Exception {
        Pattern quoted = Pattern.compile("\"[^\"]+\"");
        Pattern xmlContent = Pattern.compile(">[^<]*<");
        InputStream is = Utils.class.getClassLoader().getResourceAsStream(resource);
        if (is == null)
            is = new FileInputStream(new File(resource));
        int numParsed = 0;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            while (true) {
                String line = in.readLine();
                if (line == null)
                    break;

                line = line.trim();
                if (!line.startsWith("<string name="))
                    continue;

                Matcher m = quoted.matcher(line);
                if (!m.find())
                    throw new cc.lib.utils.GException("Failed to find quoted string on line:\n" + line);
                String name = stripEnds(m.group());
                m = xmlContent.matcher(line);
                if (!m.find())
                    throw new cc.lib.utils.GException("Failed to find xml content on line:\n" + line);
                String content = stripEnds(m.group());
                content = content.replace("\\n", "\n");
                Field f = stringResource.getField(name);
                int id = f.getInt(null);
                stringTable.put(id, content);
                System.out.print(".");
                if (++numParsed % 50 == 0) {
                    System.out.println();
                }
            }

        } finally {
            is.close();
        }
        System.out.println("Parsed " + numParsed + " strings");
    }

    /**
     *
     * @param msg
     * @param args
     * @return
     */
    public static String formatNoThrow(String msg, Object ... args) {
        try {
            if (args == null || args.length == 0)
                return msg;
            return String.format(msg, args);
        } catch (MissingFormatArgumentException e) {
            return msg + " ERR: " + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return e.getClass().getSimpleName() + ":" + e.getMessage();
        }
    }

    /**
     * Return true if n is between min and max
     * @param n
     * @param min
     * @param max
     * @param <T>
     * @return
     */
    public static <T extends Number> boolean isBetween(T n, T min, T max) {
        return n.doubleValue() >= min.doubleValue() && n.doubleValue() <= max.doubleValue();
    }

    /**
     * Remove adjacent duplicates from the list.
     * @param list
     */
    public static void unique(List list) {
        if (list.size() < 2)
            return;
        for (int i=0; i<list.size()-1; ) {
            if (list.get(i).equals(list.get(i+1))) {
                list.remove(i+1);
            } else {
                i++;
            }
        }
    }

    /**
     *
     * @param array
     * @param <T>
     * @return
     */
    public static <T> List<T> toList(T ... array) {
        return toList(0, array.length, array);
    }

    /**
     *
     * @param array
     * @return
     */
    public static List<Integer> toIntList(Integer ... array) {
        return toList(0, array.length, array);
    }

    /**
     * Return subset of an array as a list
     * @param num
     * @param array
     * @param <T>
     * @return
     */
    public static <T> List<T> toList(int start, int num, T ... array) {
        List<T> l = new ArrayList<>();
        for (int i=start; i<Math.min(start+num, array.length); i++) {
            l.add(array[i]);
        }
        return l;
    }

    /**
     * Same as wrapText except it appends newlines as well
     * @param txt
     * @param maxChars
     * @return
     */
    public static String wrapTextWithNewlines(String txt, int maxChars) {
        String [] lines = wrapText(txt, maxChars);
        if (lines == null || lines.length==0)
            return "";
        if (lines.length == 1)
            return lines[0];
        StringBuffer out = new StringBuffer();
        for (int i=0; i<lines.length; i++) {
            out.append(lines[i]);
            if (i < lines.length-1)
                out.append("\n");
        }
        return out.toString();
    }

    /**
     *
     * @param txt
     * @param maxChars
     * @return
     */
    public static String [] wrapText(String txt, int maxChars) {
        assertTrue(txt != null && maxChars > 1);
        if (txt == null || maxChars < 1)
            return null;
        List<String> lines = new ArrayList<>();
        int newline = txt.indexOf('\n');
        String str = newline>=0? txt.substring(0,newline) : txt;
        while (true) {
            while (str.length() > maxChars) {
                int spc = str.indexOf(' ');
                if (spc < 0 || spc > maxChars-1) {
                    lines.add(str.substring(0, maxChars-1)+"-");
                    str = str.substring(maxChars-1);
                } else {
                    while (true) {
                        int nxt = str.indexOf(' ', spc + 1);
                        if (nxt < 0 || nxt > maxChars) {
                            break;
                        }
                        spc = nxt;
                    }
                    lines.add(str.substring(0, spc));
                    str = str.substring(spc + 1);
                }
            }
            lines.add(str);
            if (newline >= 0) {
                int end = txt.indexOf('\n', newline+1);
                if (end > newline) {
                    str = txt.substring(newline+1, end);
                    newline = end;
                } else {
                    str = txt.substring(newline+1);
                    newline = -1;
                }
            } else {
                break;
            }
        }

        return lines.toArray(new String[lines.size()]);

    }

    /**
     *
     * @param values
     * @return
     */
    public static int getMaxStringLength(Object ... values) {
        int max = 0;
        for (Object e : values) {
            if (e == null)
                continue;
            max = Math.max(e.toString().length(), max);
        }
        return max;
    }

    /**
     *
     * @param <T>
     */
    public interface Filter<T> {
        boolean keep(T object);
    }

    /**
     * Modifies the input list to remove all elements not meeting filter criteria
     *
     * @param collection
     * @param filter
     * @param <O>
     * @return
     */
    public static <O> List<O> filter(Iterable<O> collection, Filter<O> filter) {
        List<O> result = new ArrayList<>();
        for (O o : collection) {
            if (filter.keep(o))
                result.add(o);
        }
        return result;
    }

    /**
     *
     * @param collection
     * @param filter
     * @param <O>
     */
    public static <O> void filterInPlace(Iterable<O> collection, Filter<O> filter) {
        for (Iterator<O> i = collection.iterator(); i.hasNext(); ) {
            if (!filter.keep(i.next()))
                i.remove();
        }
    }

    /**
     * Returns number of items in collection after filtering
     * @param collection
     * @param filter
     * @param <O>
     * @return
     */
    public static <O> int count(Iterable<O> collection, Filter<O> filter) {
        int count = 0;
        for (O o : collection) {
            if (filter.keep(o))
                count++;
        }
        return count;
    }

    /**
     * Return true if any element meets filter criteria, false otherwise
     *
     * @param collection
     * @param filter
     * @param <O>
     * @return
     */
    public static <O> boolean any(Iterable<O> collection, Filter<O> filter) {
        for (O o : collection) {
            if (filter.keep(o))
                return true;
        }
        return false;
    }

    /**
     * Return true if all elements meet filter criteria, false otherwise
     * @param collection
     * @param filter
     * @param <O>
     * @return
     */
    public static <O> boolean all(Iterable<O> collection, Filter<O> filter) {
        for (O o : collection) {
            if (!filter.keep(o))
                return false;
        }
        return true;
    }

    /**
     *
     * @param list
     * @param <O>
     * @return
     */
    public static <O> O getFirstOrNull(List<O> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     *
     * @param filter
     * @param items
     * @param <T>
     * @return
     */
    public static <T> List<T> filter(T [] items, Filter<T> filter) {
        List list = new ArrayList();
        for (T item : items) {
            if (item != null && filter.keep(item))
                list.add(item);
        }
        return list;
    }

    /**
     *
     * @param items
     * @param filter
     * @param <T>
     * @return
     */
    public static <T> int count(T [] items, Filter<T> filter) {
        int count = 0;
        for (T item : items) {
            if (item != null && filter.keep(item))
                count++;
        }
        return count;
    }

    public static int hashCode(Object ... a) {
        if (a == null)
            return 0;

        int result = 1;

        for (Object element : a)
            result = 31 * result + (element == null ? 0 : element.hashCode());

        return result;
    }

    /**
     *
     * @param array
     * @param result
     * @param <T>
     */
    public final static <T> void rotate(T [] array, T [] result) {
        int num = result.length;
        for (int i=0; i<num; i++) {
            result[i] = array[i];
        }
        for (int i = 0; i< array.length-num; i++) {
            array[i] = array[i+num];
        }
        for (int i = 0; i< num; i++) {
            array[i+ array.length-num] = result[i];
        }
    }

    public interface Callback<T> {
        void onDone(T result);
    }

    public interface Mapper<IN,OUT> {
        OUT map(IN in);
    }

    /**
     * Return a list of new types based on the input list using a mapping lambda
     *
     * @param inList
     * @param mapper
     * @param <IN>
     * @param <OUT>
     * @return
     */
    public static <IN,OUT> List<OUT> map(Iterable<IN> inList, Mapper<IN,OUT> mapper) {
        List<OUT> outList = new ArrayList<>();
        for (IN in : inList) {
            outList.add(mapper.map(in));
        }
        return outList;
    }

    /**
     * Return a list of new types based on the input array using a mapping lambda
     *
     * @param inArr
     * @param mapper
     * @param <IN>
     * @param <OUT>
     * @return
     */
    public static <IN,OUT> List<OUT> map(IN [] inArr, Mapper<IN,OUT> mapper) {
        List<OUT> outList = new ArrayList<>();
        for (IN in : inArr) {
            outList.add(mapper.map(in));
        }
        return outList;
    }

    /**
     * Fill an array using input array and mapping lambda
     *
     * @param inArr
     * @param outArr
     * @param mapper
     * @param <IN>
     * @param <OUT>
     * @return
     */
    public static <IN,OUT> OUT [] map(IN [] inArr, OUT [] outArr, Mapper<IN,OUT> mapper) {
        int n = Math.min(inArr.length, outArr.length);
        for (int idx=0; idx<n; idx++) {
            outArr[idx] = mapper.map(inArr[idx]);
        }
        return outArr;
    }

    /**
     *
     * @param in
     * @param outArr
     * @param mapper
     * @param <IN>
     * @param <OUT>
     * @return
     */
    public static <IN,OUT> OUT [] map(Collection<IN> in, OUT [] outArr, Mapper<IN,OUT> mapper) {
        int idx=0;
        for (IN i : in) {
            outArr[idx++] = mapper.map(i);
        }
        return outArr;
    }

    /**
     * Convert a map to a list of pairs
     *
     * @param map
     * @param <FIRST>
     * @param <SECOND>
     * @return
     */
    public static <FIRST,SECOND> List<Pair<FIRST,SECOND>> toList(Map<FIRST,SECOND> map) {
        List<Pair<FIRST,SECOND>> list = new ArrayList<>();
        for (Map.Entry e : map.entrySet()) {
            list.add(new Pair(e.getKey(), e.getValue()));
        }
        return list;
    }

    /**
     * Merge multiple lists into a single list
     * @param lists
     * @param <T>
     * @return
     */
    public static <T> List<T> mergeLists(List<T> ... lists) {
        List<T> merged = new ArrayList<>();
        for (List l : lists) {
            merged.addAll(l);
        }
        return merged;
    }

    /**
     *
     * @param iterable
     * @param mapper
     * @param <T>
     * @param <C>
     * @return
     */
    public static <T,C> List<T> mergeLists(Iterable<C> iterable, Mapper<C, List<T>> mapper) {
        List<T> merged = new ArrayList<>();
        for (C c : iterable) {
            merged.addAll(mapper.map(c));
        }
        return merged;
    }

    /**
     * Sum the mapped values from a collection
     *
     * @param in
     * @param mapper
     * @param <T>
     * @return
     */
    public static <T> long sumLong(Iterable<T> in, Mapper<T, Long> mapper) {
        long total = 0;
        for (T t : in) {
            total += mapper.map(t);
        }
        return total;
    }

    public static <T> int sumInt(Iterable<T> in, Mapper<T, Integer> mapper) {
        int total = 0;
        for (T t : in) {
            total += mapper.map(t);
        }
        return total;
    }

    public static <T> float sumFloat(Iterable<T> in, Mapper<T, Float> mapper) {
        float total = 0;
        for (T t : in) {
            total += mapper.map(t);
        }
        return total;
    }

    /**
     * Return the min value from the collection or Long.MAX_VALUE
     *
     * @param in
     * @param mapper
     * @param <T>
     * @return
     */
    public static <T> long min(Collection<T> in, Mapper<T, Long> mapper) {
        long min = Long.MAX_VALUE;
        for (T t : in) {
            min = Math.min(min, mapper.map(t));
        }
        return min;
    }

    /**
     * Return the max value from the collection or Long.MIN_VALUE
     *
     * @param in
     * @param mapper
     * @param <T>
     * @return
     */
    public static <T> long max(Collection<T> in, Mapper<T, Long> mapper) {
        long max = Long.MIN_VALUE;
        for (T t : in) {
            max = Math.max(max, mapper.map(t));
        }
        return max;
    }

    /**
     *
     * @param list
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K,V> Map<K,List<V>> toMap(List<Pair<K,V>> list) {
        Map<K,List<V>> map = new HashMap<>();
        for (Pair<K,V> p : list) {
            List<V> l = map.get(p.first);
            if (l == null) {
                l = new ArrayList<>();
                map.put(p.first, l);
            }
            l.add(p.second);
        }
        return map;
    }

    /**
     * Return an interator that will return numbers [start-end] inclusive incremented using provided step value
     *
     * @param start
     * @param end
     * @return
     */
    public static Iterable<Integer> getRangeIterator(int start, int end, int step) {
        return () -> new RangeIter(start, end, step);
    }

    /**
     * Convenience method with default step of 1
     *
     * @param start
     * @param end
     * @return
     */
    public static Iterable<Integer> getRangeIterator(int start, int end) {
        return () -> new RangeIter(start, end, 1);
    }

    public static class RangeIter implements Iterator<Integer> {
        final int start, end, step;
        int current;

        public RangeIter(int end) {
            this(0, end, 1);
        }

        public RangeIter(int start, int end) {
            this(start, end, 1);
        }

        public RangeIter(int start, int end, int step) {
            this.start = start;
            this.end = end;
            this.step = step;
            current = start-step;
        }

        @Override
        public boolean hasNext() {
            return current+step <= end;
        }

        @Override
        public Integer next() {
            current += step;
            return current;
        }
    }

    /**
     * Reverese elements in an array inplace
     *
     * @param input
     */
    public static void reverse(int[] input) {
        int last = input.length - 1;
        int middle = input.length / 2;
        for (int i = 0; i < middle; i++) {
            int temp = input[i];
            input[i] = input[last - i];
            input[last - i] = temp;
        }
    }

    /**
     * Reverese elements in an array inplace
     *
     * @param input
     */
    public static void reverse(float[] input) {
        int last = input.length - 1;
        int middle = input.length / 2;
        for (int i = 0; i < middle; i++) {
            float temp = input[i];
            input[i] = input[last - i];
            input[last - i] = temp;
        }
    }

    /**
     * Reverese elements in an array inplace
     *
     * @param input
     */
    public static void reverse(double[] input) {
        int last = input.length - 1;
        int middle = input.length / 2;
        for (int i = 0; i < middle; i++) {
            double temp = input[i];
            input[i] = input[last - i];
            input[last - i] = temp;
        }
    }

    /**
     * Reverese elements in an array inplace
     *
     * @param input
     */
    public static void reverse(long[] input) {
        int last = input.length - 1;
        int middle = input.length / 2;
        for (int i = 0; i < middle; i++) {
            long temp = input[i];
            input[i] = input[last - i];
            input[last - i] = temp;
        }
    }

    /**
     * Reverese elements in an array inplace
     *
     * @param input
     */
    public static <T> void reverse(T [] input) {
        int last = input.length - 1;
        int middle = input.length / 2;
        for (int i = 0; i < middle; i++) {
            T temp = input[i];
            input[i] = input[last - i];
            input[last - i] = temp;
        }
    }

    /**
     * Linear search the highest value item in a list based on mapping
     * @param c
     * @param valueMapper
     * @param <T>
     * @return
     */
    public static <T> T search(Iterable<T> c, Mapper<T, Number> valueMapper) {
        double bestValue = Double.MIN_VALUE;
        T best = null;
        for (T i : c) {
            double value = valueMapper.map(i).doubleValue();
            if (best == null || value > bestValue) {
                bestValue = value;
                best = i;
            }
        }
        return best;
    }

    /**
     * Linear search the highest value item in a list based on mapping
     * @param c
     * @param valueMapper
     * @param <T>
     * @return
     */
    public static <T> int searchIndex(Iterable<T> c, Mapper<T, Number> valueMapper) {
        double bestValue = Double.MIN_VALUE;
        int bestIndex = -1;
        int index = 0;
        for (T i : c) {
            double value = valueMapper.map(i).doubleValue();
            if (bestIndex < 0 || value > bestValue) {
                bestValue = value;
                bestIndex = index;
            }
            index++;
        }
        return bestIndex;
    }

    /**
     *
     * @param c
     * @param filter
     * @param <T>
     * @return
     */
    public static <T> T findFirstOrNull(Collection<T> c, Filter<T> filter) {
        for (T t : c) {
            if (filter.keep(t))
                return t;
        }
        return null;
    }

    /**
     * Get the proper suffix (st, nd, rd, th) for a number
     *
     * ie. 1st, 2nd, 3rd, 4th, etc.
     *
     * @param number
     * @return
     */
    public static String getSuffix(int number) {
        int j = number % 10;;
        int k = number % 100;
        if (j == 1 && k != 11) {
            return "st";
        }
        if (j == 2 && k != 12) {
            return "nd";
        }
        if (j == 3 && k != 13) {
            return "rd";
        }
        return "th";
    }

    /**
     * Return a list of vertices that represents the outermost polygon of the input
     *
     * For input size < 3 result is empty list.
     * For input size == 3 result is the input list
     *
     * Requires that input vertices contains no duplicates.
     * Output list will be populated with same instances from input upon result
     *
     * CPU: O(n^2)
     * MEM: O(2n)
     *
     * @param input
     * @return
     */
    public static <V extends IVector2D> List<V> computeGiftWrapVertices(Collection<V> input) {
        if (input.size() < 3)
            return Collections.emptyList();
        if (input.size() == 3)
            return input instanceof List ? (List)input : new ArrayList<>(input);

        MutableVector2D cntr = new MutableVector2D();
        for (IVector2D v : input) {
            cntr.addEq(v);
        }

        cntr.scaleEq(1f / input.size());

        List<Pair<MutableVector2D, V>> working = Utils.map(input, v -> new Pair(new MutableVector2D(v).subEq(cntr), v));
        int primary = Utils.searchIndex(working, p -> p.first.magSquared());

        // compute the bounding polygon using giftwrap algorithm

        // start at the primary (longest) vertex from the center since this must be on the bounding rect
        List<MutableVector2D> newV = new ArrayList<>(working.size()/2);

        newV.add(working.get(primary).first);

        int start = primary;//(primary+1) % numVerts;
        final int numVerts = working.size();

        MutableVector2D dv = new MutableVector2D();
        MutableVector2D vv = new MutableVector2D();
        try {
            do {
                working.get(start).first.scale(-1, dv);
                float best = 0;
                int next = -1;
                for (int i=(start+1)%numVerts; i!=start; i = (i+1)%numVerts) {
                    working.get(i).first.sub(working.get(start).first, vv);
                    float angle = vv.angleBetweenSigned(dv);
                    if (angle > best) {
                        best = angle;
                        next = i;
                    }
                }
                Utils.assertTrue(next >= 0);
                if (next != primary) {
                    newV.add(working.get(next).first);
                }
                start = next;
            } while (start != primary && newV.size() < numVerts);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        Map<MutableVector2D, List<V>> map = Utils.toMap(working);
        return Utils.map(newV, v -> map.get(v).get(0));
    }

    /**
     * Return whether pos is contained by the convex polygon represented by the list.
     * List must be convex and ordered. A Result from computeGiftwrapVertices is compatible.
     *
     * @param pos
     * @param polygon
     * @return
     */
    public static <V extends IVector2D> boolean isPointInsidePolygon(IVector2D pos, List<V> polygon) {
        if (polygon.size() < 3)
            return false;

        MutableVector2D side = Vector2D.sub(polygon.get(0), polygon.get(polygon.size()-1)).normEq();
        MutableVector2D dv = Vector2D.sub(pos, polygon.get(polygon.size()-1));

        int sign = CMath.signOf(side.dot(dv));
        for (int i=1; i<polygon.size(); i++) {
            side.set(polygon.get(i)).subEq(polygon.get(i-1)).normEq();
            dv.set(pos).subEq(polygon.get(i-1));
            if (sign != CMath.signOf(side.dot(dv))) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @param in
     * @param delimiter
     * @param mapper
     * @param <T>
     * @return
     */
    public static <T> String toString(Iterable<T> in, String delimiter, Mapper<T, String> mapper) {
        StringBuffer buf = new StringBuffer();
        for (T i : in) {
            if (buf.length() > 0)
                buf.append(delimiter);
            buf.append(mapper.map(i));
        }
        return buf.toString();
    }

    /**
     *
     * @param in
     * @param <T>
     * @return
     */
    public static <T> T requireNotNull(T in) {
        if (in == null)
            throw new NullPointerException();
        return in;
    }

    /**
     *
     * @param in
     * @param otherwise
     * @param <T>
     * @return
     */
    public static <T> T requireNotNull(T in, T otherwise) {
        return in == null ? otherwise : in;
    }

    /**
     *
     * @param map
     * @param object
     * @param <T>
     */
    public static <T> void incrementCountingMap(Map<T, Integer> map, T object, int amt) {
        if (map.containsKey(object)) {
            int value = map.get(object)+amt;
            map.put(object, value);
        } else {
            map.put(object, amt);
        }
    }

    public static String trimSpaces(String in) {
        if (in == null)
            return null;
        char [] value = in.toCharArray();
        int len = value.length;

        int st = 0;
        char[] val = value;    /* avoid getfield opcode */

        while ((st < len) && (val[st] == ' ')) {
            st++;
        }
        while ((st < len) && (val[len - 1] == ' ')) {
            len--;
        }
        return ((st > 0) || (len < value.length)) ? in.substring(st, len) : in;
    }
}