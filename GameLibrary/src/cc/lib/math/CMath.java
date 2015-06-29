package cc.lib.math;

import java.util.Collection;

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
	
	public static int factorial(int n) {
		int r = 1;
		for (int i=1; i<=n; i++) {
			r *= i;
		}
		return r;
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
}
