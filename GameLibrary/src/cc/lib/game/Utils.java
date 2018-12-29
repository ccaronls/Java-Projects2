package cc.lib.game;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.lib.math.CMath;
import cc.lib.math.Vector2D;
import cc.lib.utils.FileUtils;

public class Utils {

    // USER SETTABLE VARS

    /**
     * Set too true to get debugging console
     */
    public static boolean DEBUG_ENABLED = false;

    // CONSTANTS

    // general working matricies, created once
    private final static float[] m_matrix_2x2 = new float[4];
    private final static float[] r_matrix_2x2 = new float[4];
    private final static Random randGen = new Random(System.currentTimeMillis()); // random number generator

    // FUCNTIONS

    /**
     * @param c
     */
    public static void unhandledCase(Object c) {
        RuntimeException e = new RuntimeException("Unhandled case [" + c + "]");
        e.printStackTrace();
        throw e;
    }

    /**
     * @param expr
     */
    public static void assertTrue(boolean expr) {
        assertTrue(expr, "Expression is false");
    }

    /**
     * @param expr
     */
    public static void assertFalse(boolean expr) {
        assertTrue(!expr, "Expression is true");
    }

    public static void assertContains(Object o, Collection c) {
        if (DEBUG_ENABLED && !c.contains(o)) {
            throw new AssertionError("Object '" + o + "' is not contained by: " + c);
        }
    }

    /**
     * @param expr
     * @param msg
     * @param args
     */
    public static void assertTrue(boolean expr, String msg, Object... args) {
        if (DEBUG_ENABLED && !expr) {
            throw new AssertionError("ASSERT FAILED " + String.format(msg, args));
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

        int orient = 0;

        for (int i = 0; i < numPts; i++) {
            float dx = px - pts[i].getX();
            float dy = py - pts[i].getY();

            int ii = (i + 1) % numPts;
            float dx2 = pts[ii].getX() - pts[i].getX();
            float dy2 = pts[ii].getY() - pts[i].getY();

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
        return (float) (randGen.nextDouble() * scale);
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
            throw new RuntimeException("Degenerate Vector");
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
    public static int chooseRandomFromSet(int[] weights) {
        int i, total = 0;
        for (i = 0; i < weights.length; i++)
            total += weights[i];
        if (total <= 0) {
            return 0;
        }
        int r = rand() % total;
        for (i = 0; i < weights.length; i++) {
            if (weights[i] <= r) {
                r -= weights[i];
            } else {
                break;
            }
        }
        assert (weights[i] > 0);
        return i;
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

    /**
     * 1. Strip extension if any
     * 2. Replace [_]+ (underscores) with a space
     * 3. Make whole strings lowercase with first letter capitalized
     *
     * @param str
     * @return
     */
    public static String getPrettyString(String str) {
        if (str == null)
            return null;
        Matcher us = Pattern.compile("[a-zA-Z0-9]+").matcher(FileUtils.stripExtension(str.trim()));
        StringBuffer result = new StringBuffer();
        while (us.find()) {
            String s = us.group().toLowerCase();
            if (result.length() > 0)
                result.append(" ");
            result.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1));
        }
        return result.toString();
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
    public static <T> void copyElems(T[] dest, T[] source) {
        int min = Math.min(source.length, dest.length);
        for (int i = 0; i < min; i++) {
            dest[i] = source[i];
        }
    }

    /**
     * @param dest
     * @param source
     */
    public static void copyElems(int[] dest, int[] source) {
        int min = Math.min(source.length, dest.length);
        for (int i = 0; i < min; i++) {
            dest[i] = source[i];
        }
    }

    /**
     * @param <T>
     * @param dest
     * @param value
     * @param start
     * @param end
     */
    public static <T> void fillArray(T[] dest, T value, int start, int end) {
        for (int i = start; i <= end; i++)
            dest[i] = value;
    }

    /**
     * @param <T>
     * @param dest
     * @param value
     */
    public static <T> void fillArray(T[] dest, T value) {
        fillArray(dest, value, 0, dest.length - 1);
    }

    /**
     * @param dest
     * @param value
     * @param start
     * @param end
     */
    public static void fillArray(int[] dest, int value, int start, int end) {
        for (int i = start; i <= end; i++)
            dest[i] = value;
    }

    /**
     * @param dest
     * @param value
     */
    public static void fillArray(int[] dest, int value) {
        fillArray(dest, value, 0, dest.length - 1);
    }

    /**
     * @param
     * @param dest
     * @param value
     * @param start
     * @param end
     */
    public static void fillArray(boolean[] dest, boolean value, int start, int end) {
        for (int i = start; i <= end; i++)
            dest[i] = value;
    }

    /**
     * @param dest
     * @param value
     */
    public static void fillArray(boolean[] dest, boolean value) {
        fillArray(dest, value, 0, dest.length - 1);
    }

    /**
     * @param elems
     * @param start
     * @param end
     */
    public static void shuffle(Object[] elems, int start, int end) {
        for (int i = 0; i < 1000; i++) {
            int a = Utils.randRange(start, end - 1);
            int b = Utils.randRange(start, end - 1);
            Utils.swapElems(elems, a, b);
        }
    }

    public static <T> void shuffle(List<T> elems) {
        for (int i = 0; i < 1000; i++) {
            int a = Utils.rand() % elems.size();
            int b = Utils.rand() % elems.size();
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
            int a = Utils.randRange(start, end - 1);
            int b = Utils.randRange(start, end - 1);
            Utils.swapElems(elems, a, b);
        }
    }

    /**
     * @param elems
     * @param len
     */
    public static void shuffle(int[] elems, int len) {
        shuffle(elems, 0, len - 1);
    }

    /**
     * @param elems
     */
    public static void shuffle(int[] elems) {
        shuffle(elems, 0, elems.length - 1);
    }

    /**
     * @param elems
     * @param start
     * @param end
     */
    public static void shuffle(float[] elems, int start, int end) {
        for (int i = 0; i < 1000; i++) {
            int a = Utils.randRange(start, end);
            int b = Utils.randRange(start, end);
            Utils.swapElems(elems, a, b);
        }
    }

    /**
     * @param elems
     * @param len
     */
    public static void shuffle(float[] elems, int len) {
        shuffle(elems, 0, len - 1);
    }

    /**
     * @param elems
     */
    public static void shuffle(float[] elems) {
        shuffle(elems, 0, elems.length - 1);
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
            throw new RuntimeException(e);
        }
    }

    /**
     * @param c
     */
    public static void unhandledCase(int c) {
        RuntimeException e = new RuntimeException("Unhandled case [" + c + "]");
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
        assert(args.length>1);
        T max = max2(args[0], args[1]);
        for (int i=2; i<args.length; i++) {
            max = max2(max, args[i]);
        }
        return max;
    }

    @SafeVarargs
    public static <T extends Number> T min(T... args) {
        assert(args.length>1);
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

    public static <T> T randItem(List<T> items) {
        return items.get(rand() % items.size());
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
     * Populate result array from an array of strings.
     *
     * @param arr
     * @param enumType
     * @param result
     * @return
     */
    public static <T extends Enum<T>> T[] convertToEnumArray(String[] arr, Class<T> enumType, T[] result) {
        int num = Math.min(arr.length, result.length);
        for (int i = 0; i < num; i++) {
            arr[i] = arr[i].trim();
            if (arr[i].length() > 0)
                result[i] = Enum.valueOf(enumType, arr[i]);
        }
        return result;
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
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i].name();
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
        assert (target.length >= length && primary.length >= length);
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
        assert (target.length >= primary.length);
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
    public static <T extends Comparable<T>> int linearSearch(T[] arr, T key) {
        for (int i = 0; i < arr.length; i++) {
            int x = arr[i].compareTo(key);
            if (x == 0)
                return i;
        }
        return -1;
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
        System.err.println("isEmpty does not know about class " + o.getClass());
        return false;
    }

    /**
     *
     * @param c
     * @param length
     * @return
     */
    public static Object getRepeatingChars(char c, int length) {
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
        synchronized (lock) {
            try {
                if (msecs < 0)
                    lock.wait();
                else
                    lock.wait(msecs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * @param s
     * @return
     */
    public static String trimQuotes(String s) {
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
                        throw new AssertionError(e);
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
                throw new AssertionError(e);
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
            throw new AssertionError("Failed to process strings: " + e);
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
                    throw new AssertionError("Failed to find quoted string on line:\n" + line);
                String name = stripEnds(m.group());
                m = xmlContent.matcher(line);
                if (!m.find())
                    throw new AssertionError("Failed to find xml content on line:\n" + line);
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
            return String.format(msg, args);
        } catch (MissingFormatArgumentException e) {
            e.printStackTrace();
            return msg + " ERR: " + e.getMessage();
        } catch (Exception e) {
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

    public static <T> List<T> toList(T ... array) {
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
}
