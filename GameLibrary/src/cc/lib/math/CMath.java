package cc.lib.math;

import java.util.Collection;

import cc.lib.game.IVector2D;
import cc.lib.game.Utils;

public class CMath {

	public static final double M_E        = 2.71828182845904523536f;
	public static final double M_LOG2E    = 1.44269504088896340736f;
	public static final double M_LOG10E   = 0.434294481903251827651f;
	public static final double M_LN2      = 0.693147180559945309417f;
	public static final double M_LN10     = 2.30258509299404568402f;
	public static final double M_PI       = 3.14159265358979323846f;
	public static final double M_PI_2     = 1.57079632679489661923f;
	public static final double M_PI_4     = 0.785398163397448309616f;
	public static final double M_1_PI     = 0.318309886183790671538f;
	public static final double M_2_PI     = 0.636619772367581343076f;
	public static final double M_2_SQRTPI = 1.12837916709551257390f;
	public static final double M_SQRT2    = 1.41421356237309504880f;
	public static final double M_SQRT1_2  = 0.707106781186547524401f;
	public static final double M_SQRT_2_PI = 2.506628274631000502415765284811f;
	
	/**
	 * defien a small num
	 */
	public static float EPSILON      	= 0.00001f;
    public static final float DEG_TO_RAD 	= (float)(Math.PI / 180.0); // converts from degrees to radians
    public static final float RAD_TO_DEG	= (float)(180.0 / Math.PI); // converts form radians to degress

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
     * @param n
     * @return
     */
	public static int factorial(int n) {
		int r = 1;
		for (int i=1; i<=n; i++) {
			r *= i;
		}
		return r;
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
    
	/**
	 *                     n!
	 * N choose R means --------
	 *                  (n-r)!r!
	 * | n |
	 * | r |
	 * @param n
	 * @param r
	 * @return
	 */
	public static int n_choose_r(int n, int r) {
		return factorial(n) / (factorial(n-r) * factorial(r));
	}
	
	/**
	 * 
	 * @param items
	 * @param num
	 * @return
	 */
	public static int sum(final int [] items, int num)
    {
        assert(num>0);
        int sum=0;
        for (int i=0; i<num; i++)
            sum += items[i];
        return sum;
    }
	
	/**
	 * 
	 * @param items
	 * @return
	 */
	public static int sum(final int [] items) {
		return sum(items, items.length);
	}

	/**
	 * 
	 * @param items
	 * @param num
	 * @return
	 */
	public static float sum(final float [] items, int num)
    {
        assert(num>0);
        int sum=0;
        for (int i=0; i<num; i++)
            sum += items[i];
        return sum;
    }

	/**
	 * 
	 * @param items
	 * @return
	 */
	public static float sum(final float [] items) {
		return sum(items, items.length);
	}

	/**
	 * 
	 * @param items
	 * @param num
	 * @return
	 */
	public static double sum(final double [] items, int num)
    {
        assert(num>0);
        double sum=0;
        for (int i=0; i<num; i++)
            sum += items[i];
        return sum;
    }
	
	/**
	 * 
	 * @param items
	 * @return
	 */
	public static double sum(final double [] items) {
		return sum(items, items.length);
	}

	/**
	 * 
	 * @param items
	 * @param num
	 * @return
	 */
    public static double stdDev(final double [] items, int num)
    {
        assert(num>1);
        double ave=sum(items, num) * 1.0f/num;
        return stdDev(items, num, ave);
    }

    /**
     * 
     * @param items
     * @param num
     * @param ave
     * @return
     */
    public static double stdDev(final double [] items, int num, double ave)
    {
        assert(num>1);
        double sum=0;
        for (int i=0; i<num; i++) {
            double ds = items[i]-ave;
            sum += ds*ds;
        }
        double stdDev = Math.sqrt(sum * 1.0f/(num-1));
        return stdDev;
    }

    /**
     * 
     * @param items
     * @param ave
     * @return
     */
    public static double stdDev(final double [] items, double ave) {
    	return stdDev(items, items.length, ave);
	}
    /**
     * 
     * @param values
     * @return
     */
    public static double sum(Collection<Double> values)
    {
        double sum=0;
        for (double d : values)
            sum += d;
        return sum;
    }

    /**
     * 
     * @param values
     * @return
     */
    public static double stdDev(Collection<Double> values)
    {
        assert(values.size() > 1);
        double ave=sum(values) * 1.0f/values.size();
        return stdDev(values, ave);
    }

    /**
     * 
     * @param values
     * @param ave
     * @return
     */
    public static double stdDev(Collection<Double> values, double ave)
    {
        assert(values.size() > 0);
        double sum=0;
        for (double d: values) {
            double ds = d-ave;
            sum += ds*ds;
        }
        double stdDev = Math.sqrt(sum * 1.0f/(values.size()-1));
        return stdDev;
    }

    /**
     * 'Bell' Curve
     * 
     * f(x) = e^(-0.5x^2)
     *        -----------
     *         sqrt(2*PI)
     *         
     * @param x
     * @param mean pivot value.  Curve is at its highest point when x == mean
     * @return value between (0-1] of x where nd(mean, mean) == 1
     */
    public static double normalDistribution(double x, double mean) {
    	return Math.pow(M_E, -0.5 * Math.pow(x - mean, 2)) / M_SQRT_2_PI;
    }
    
    /**
     * Return -1 if n < 0, 1 otherwise
     * @param n
     * @return
     */
    public static int signOf(float n) {
    	return n < 0 ? -1 : 1;
    }
    
    /**
     * 
     * @param pt
     * @param l0
     * @param l1
     * @return
     */
    public static float distSqPointLine(IVector2D pt, IVector2D l0, IVector2D l1) {
    	return Utils.distSqPointLine(pt.getX(), pt.getY(), l0.getX(), l0.getY(), l1.getX(), l1.getY());
    }

    public static int getNthPrime(int n) {
        int [] primes = { 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163, 167, 173, 179, 181, 191, 193, 197, 199 };
        //                   1  2  2   4   2   4   2   2   4   2   6   4   2   4   6   6   2   6   4   2   6   4   6   8    2    2    4    2    4    4    4    6    2   10    2    6    6    4    6    6    2   10    2    4    2
        //                      1  0   2  -2   2  -2   0   2  -2   4  -2  -2   2   2   0  -4   4  -2  -2   4  -2   2   2   -6    0    2   -2    2    0    0    2   -4    8   -8    4    0   -2    2    0   -4    8   -8    2   -2
        if (n >= primes.length)
            System.err.println("NOT ENOUGH PRIMES!!!! value " + n + " is larger than number of known primes");
        return primes[Utils.clamp(n, 0, primes.length-1)];
    }

    public interface PermuteCallback<T> {
        void onPermutation(T [] array);
    }

    public static <T> void permute(T [] array, PermuteCallback<T> callback) {
        permuteR(array, 0, callback);
    }

    private static <T> void permuteR(T [] array, int index, PermuteCallback<T> callback) {
        int r = array.length-1;
        if (r == index) {
            callback.onPermutation(array);
            return;
        }

        for (int i=index; i<=r; i++) {
            Utils.swapElems(array, i, index);
            permuteR(array, index+1, callback);
            Utils.swapElems(array, i, index);
        }

    }

    public static <T> void combinations(T [] array, T [] data, PermuteCallback<T> callback) {
        //combinationsR(array, data, 0, 0, callback);
        combinationUtil(array, data, 0, 0, callback);
    }

    private static <T> void combinationUtil(T arr[], T data[], int start, int index, PermuteCallback<T> callback)
    {

        // Current combination is ready
        // to be printed, print it
        if (index == data.length)
        {
            callback.onPermutation(data);
            return;
        }

        // replace index with all possible
        // elements. The condition "end-i+1 >= r-index"
        // makes sure that including one element
        // at index will make a combination with
        // remaining elements at remaining positions
        for (int i = start; i < arr.length /* && end - i + 1 >= data.length - index*/; i++)
        {
            data[index] = arr[i];
            combinationUtil(arr, data, i+1, index+1, callback);
        }
    }

    private static <T> void combinationsR(T [] array, Object [] data, int start, int index, PermuteCallback<T> callback) {
        if (index == data.length) {
            callback.onPermutation((T[])data);
            return;
        }

        for (int i=start; i<array.length; i++) {
            data[index]=array[i];
            combinationsR(array, data, i+1, index+1, callback);
        }
    }

}
