package cc.lib.math;

import java.util.Arrays;

/**
 * Complex arithmetic.  Most functions are accounted for and gotten from web.
 * Some effort made to speed up computations, specifically 
 * 1> Having 3 pow versions (int, double, complex) (fast, med, slow)
 * 2> No calls to 'new' user provides value for result to support caching
 *
 * @author chriscaron
 *
 */
public final class ComplexNumber {

    private double real, imag;
    
    private enum CachedValue {
    	ACOSINE {
			@Override
			ComplexNumber execute(ComplexNumber v) {
				return v.acosine_priv();
			}
		},
    	ACOSINEH {
			@Override
			ComplexNumber execute(ComplexNumber v) {
				return v.acosineh_prov();
			}
		},
    	ASINE {
			@Override
			ComplexNumber execute(ComplexNumber v) {
				return v.asine_priv();
			}
		},
    	ASINEH {
			@Override
			ComplexNumber execute(ComplexNumber v) {
				return v.asineh_priv();
			}
		},
    	ATAN {
			@Override
			ComplexNumber execute(ComplexNumber v) {
				return v.atangent_priv();
			}
		},
    	ATANH {
			@Override
			ComplexNumber execute(ComplexNumber v) {
				return v.atangenth_priv();
			}
		},
    	COSINE {
			@Override
			ComplexNumber execute(ComplexNumber v) {
				return new ComplexNumber(v.cosine_priv());
			}
		},
    	COSINEH {
			@Override
			ComplexNumber execute(ComplexNumber v) {
				return v.cosineh_priv();
			}
		},
    	EXP {
			@Override
			ComplexNumber execute(ComplexNumber v) {
				return v.exp_priv();
			}
		},
    	LN {
			@Override
			ComplexNumber execute(ComplexNumber v) {
				return v.ln_priv();
			}
		},
    	SINE {
			@Override
			ComplexNumber execute(ComplexNumber v) {
				return v.sine_priv();
			}
		},
    	SINEH {
			@Override
			ComplexNumber execute(ComplexNumber v) {
				return v.sineh_priv();
			}
		},
    	SQRT {
			@Override
			ComplexNumber execute(ComplexNumber v) {
				return v.sqrt_priv();
			}
		},
    	TAN {
			@Override
			ComplexNumber execute(ComplexNumber v) {
				return v.tangent_priv();
			}
		},
    	TANH {
			@Override
			ComplexNumber execute(ComplexNumber v) {
				return v.tangenth();
			}
		};
    	
    	abstract ComplexNumber execute(ComplexNumber v);
    }
    
    private enum DCachedValue {
    	MOD {
			@Override
			double execute(ComplexNumber v) {
				return v.mod_priv();
			}
		},
    	ARG {
			@Override
			double execute(ComplexNumber v) {
				return v.arg_priv();
			}
		};
    	
    	abstract double execute(ComplexNumber v);
    }
    
    private Double [] dcache = null;
    private ComplexNumber [] icache = null;

    public static long cacheHits = 0;
    public static long cacheMiss = 0;
    
    public static void resetCacheStats() {
    	cacheHits = cacheMiss = 0;
    }
    
    public static void printCacheStats() {
    	System.out.println("Cache Hits = " + cacheHits);
    	System.out.println("Cache Misses = " + cacheMiss);
    }
    
    private double getDCache(DCachedValue cv) {
    	int index = cv.ordinal();
    	if (dcache == null || dcache[index] == null) {
    		if (dcache == null) {
    			dcache = new Double[CachedValue.values().length];
    		}
    		cacheMiss++;
    		dcache[index] = cv.execute(this);
    	} else {
    		cacheHits ++;
    	}
    	return dcache[index];
    }
    
    private ComplexNumber getICache(CachedValue cv) {
    	int index = cv.ordinal();
    	if (icache == null || icache[index] == null) {
    		if (icache == null) {
    			icache = new ComplexNumber[CachedValue.values().length];
    		}
    		cacheMiss++;
    		icache[index] = cv.execute(this);
    	} else {
    		cacheHits ++;
    	}
    	return icache[cv.ordinal()];
    }
    
    private void clearCache() {
    	if (icache != null)
    		Arrays.fill(icache,  null);
    	if (dcache != null)
    		Arrays.fill(dcache, null);
    }
    
    public ComplexNumber() {
        set(0,0);
    }
    
    public ComplexNumber(double a, double b) {
        set(a,b);
    }
    
    public ComplexNumber(double a) {
        set(a,0);
    }
    
    public ComplexNumber(ComplexNumber toCopy) {
        copy(toCopy);
    }
    
    /**
     * 
     * @param rhs
     * @return
     */
    public ComplexNumber add(ComplexNumber rhs) {
        return getTemp().set(real + rhs.real, imag + rhs.imag);
    }

    
    /**
     * Modifier, return this + rhs
     * @param rhs
     * @return
     */
    public ComplexNumber addEq(ComplexNumber rhs) {
    	clearCache();
    	return set(real + rhs.real, imag + rhs.imag);
    }

    /**
     * 
     * @param rhs
     * @return
     */
    public ComplexNumber sub(ComplexNumber rhs) {
        return getTemp().set(real - rhs.real, imag - rhs.imag);
    }

    /**
     * Modifier, return this - rhs
     * @param rhs
     * @return
     */
    public ComplexNumber subEq(ComplexNumber rhs) {
        return set(real - rhs.real, imag - rhs.imag);
    }

    /**
     * Return negative this
     * @param result
     * @return
     */
    public ComplexNumber negate(ComplexNumber result) {
        return result.set(-real, -imag);
    }

    public ComplexNumber negate() {
        return getTemp().set(-real, -imag);
    }

    /**
     * Modifier, return -this
     * @return
     */
    public ComplexNumber negateEq() {
        return set(-real, -imag);
    }

    /**
     * 
     * @param scalar
     * @return
     */
    public ComplexNumber scale(double scalar) {
        return getTemp().set(real * scalar, imag * scalar);
    }

    /**
     * Modifier, return this scaled by a double
     * @param scalar
     * @return
     */
    public ComplexNumber scaleEq(double scalar) {
        return set(real *= scalar, imag *= scalar);
    }

    /**
     * 
     * @param rhs
     * @return
     */
    public ComplexNumber multiply(ComplexNumber rhs) {
        return getTemp().set(real*rhs.real - imag*rhs.imag, imag*rhs.real + real*rhs.imag);
    }

    /**
     * 
     * @param rhs
     * @return
     */
    public ComplexNumber multiplyEq(ComplexNumber rhs) {
        return set(real*rhs.real - imag*rhs.imag, imag*rhs.real + real*rhs.imag);
    }
    
    /**
     * Return this / denom
     * @param denom
     * @param result
     * @return
     */
    public ComplexNumber divide(ComplexNumber denom) {
        double den=Math.pow(denom.mod(),2);
        return getTemp().set((real*denom.real+imag*denom.imag)/den,(imag*denom.real-real*denom.imag)/den);    
    }

    /**
     * Synonym for mod
     * @return
     */
    public double abs() {
        return mod();
    }

    /**
     * Synonym for mod
     * @return
     */
    public double mag() {
        return mod();
    }
    
    /**
     * Return sqrt(this.real^2 + this.imag^2)
     * @return
     */
    public double mod() {
    	return getDCache(DCachedValue.MOD);
    }
    
    private double mod_priv() {
        if (real != 0 || imag!= 0)
            return Math.sqrt(real*real + imag*imag);
        return 0;
    }

    /**
     * Return atan2(this.imag, this.real)
     * @return
     */
    public double arg() {
    	return getDCache(DCachedValue.ARG);
    }
    
    private double arg_priv() {
        return Math.atan2(imag, real);
    }
    
    /**
     * Return [-this.imag, this.real]
     * @param result
     * @return
     */
    public ComplexNumber conj() {
        return getTemp().set(-imag, real);
    }

    /**
     * Return sqrt(this)
     * @param result
     * @return
     */
    public ComplexNumber sqrt() {
    	return getICache(CachedValue.SQRT);
    }
    
    private ComplexNumber sqrt_priv() {
        double r=Math.sqrt(mod());
        double theta=arg()/2;
        return new ComplexNumber(r*Math.cos(theta),r*Math.sin(theta));
    }
    
    /**
     * Return this ^ int
     * @param power
     * @param result
     * @return
     */
    public ComplexNumber powi(int power) {
    	ComplexNumber result = getTemp();
        result.set(1,0);
        for (int i=0; i<power; i++) {
            multiply(result);
        }
        return result;
    }

    /**
     * Return this ^ double
     * @param n
     * @param result
     * @return
     */
    public ComplexNumber powd(double n) {
    	ComplexNumber result = getTemp();
        double real = n * Math.log(abs());
        double imag = n * arg();
        double scalar = Math.exp(real);
        return result.set(scalar * Math.cos(imag), scalar * Math.sin(imag));
    }

    /**
     * Return [e^power.real * cos(power.imag), e^power.real * sin(power.imag)]
     * @param power
     * @param result
     * @return
     */
    public ComplexNumber powc(ComplexNumber power) {
        return power.multiply(ln()).exp();
    }

    // Real cosh function (used to compute complex trig functions)
    private double cosh(double theta) {
        return (Math.exp(theta)+Math.exp(-theta))/2;
    }
    
    // Real sinh function (used to compute complex trig functions)
    private double sinh(double theta) {
        return (Math.exp(theta)-Math.exp(-theta))/2;
    }
    
    /**
     * Return [fc(this.imag) * sin(this.real), fs(this.imag)*cos(this.real)]
     *   where fs(x) = (e^x - e^-x)/2
     *   and   fc(x) = (e^x + e^-x)/2
     * @param result
     * @return
     */
    public ComplexNumber sine() {
    	return getICache(CachedValue.SINE);
    }
    
    private ComplexNumber sine_priv() { 
        return new ComplexNumber(cosh(imag)*Math.sin(real),sinh(imag)*Math.cos(real));
    }

    /**
     * Return [fs(this.real) * cos.(this.imag), fc(this.real)*sin(this.imag)]
     *   where fs(x) = (e^x - e^-x)/2
     *   and   fc(x) = (e^x + e^-x)/2
     * @param result
     * @return
     */
    public ComplexNumber sineh() {
    	return getICache(CachedValue.SINEH);
    }
    
    private ComplexNumber sineh_priv() {
        return new ComplexNumber(sinh(real)*Math.cos(imag),cosh(real)*Math.sin(imag));
    }

    /**
     * Return -I * ln((this*I) + sqrt(1-this^2))
     * @param result
     * @return
     */
    public ComplexNumber asine() {
    	return getICache(CachedValue.ASINE);
    }
    
    private ComplexNumber asine_priv() {
        //return -I * ((this*I) + (1 - (this * this)).Sqrt()).Log();
        ComplexNumber negI = getTemp().set(0, -1);
        ComplexNumber ttI = this.multiply(negI);
        ComplexNumber lhs = negI.multiply(ttI);
        ComplexNumber ttt = this.multiply(this);
        ComplexNumber omttt = getTemp().set(1, 0).sub(ttt);
        ComplexNumber  rhs = omttt.sqrt().ln();
        return new ComplexNumber(lhs.add(rhs));
    }
    
    /**
     * return ln(this + sqrt((this^2)+1))
     * 
     * @param result
     * @return
     */
    public ComplexNumber asineh() {
    	return getICache(CachedValue.ASINEH);
    }
    
    private ComplexNumber asineh_priv() {
        //return (this + ((this*this) + 1).Sqrt()).Log();
        ComplexNumber tttpo = this.multiply(this);
        tttpo.real += 1;
        ComplexNumber rhs = tttpo.sqrt().ln();
        return new ComplexNumber(add(rhs));
    }
    
    public ComplexNumber cosine() {
    	return getICache(CachedValue.COSINE);
    }
    
    private ComplexNumber cosine_priv() {
        return new ComplexNumber(cosh(imag)*Math.cos(real),-sinh(imag)*Math.sin(real));
    }

    /**
     * 
     * @param result
     * @return
     */
    public ComplexNumber cosineh() {
    	return getICache(CachedValue.COSINEH);
    }
    
    private ComplexNumber cosineh_priv() {
        return new ComplexNumber(cosh(real)*Math.cos(imag),sinh(real)*Math.sin(imag));
    }
    
    /**
     * Return -I * ln(this+I * sqrt(1-(this^2))
     * @param result
     * @return
     */
    public ComplexNumber acosine() {
    	return getICache(CachedValue.ACOSINE);
    }
    
    private ComplexNumber acosine_priv() {
        // return -I * (this + I * (1 - (this*this)).Sqrt()).Log();
        ComplexNumber negI = getTemp().set(0, -1);
        ComplexNumber ttt = this.multiply(this);
        ComplexNumber omttt = getTemp().set(1, 0).subEq(ttt);
        ComplexNumber rhs = add(getTemp().set(0, 1).multiply(omttt.sqrt().ln()));
        return new ComplexNumber(negI.multiply(rhs));
    }
    
    /**
     * Return 2 * ln(sqrt((this+1)/2) + sqrt(((this-1)/2))
     * @param result
     * @return
     */
    public ComplexNumber acosineh() {
    	return getICache(CachedValue.ACOSINEH);
    }
    
    private ComplexNumber acosineh_prov() {
        //return 2d * (((this+1d) / 2d).Sqrt() + ((this-1) / 2d).Sqrt()).Log();
        ComplexNumber tp1d2 = getTemp().set((real+1)/2, imag/2);
        ComplexNumber tm1d2 = getTemp().set((real-1)/2, imag/2);
        ComplexNumber lhs = tp1d2.sqrt().scaleEq(2);
        ComplexNumber rhs = tm1d2.sqrt().ln();
        return new ComplexNumber(lhs.add(rhs));
    }
    
    /**
     * Return sin(this) / cosine(this)
     * @param result
     * @return
     */
    public ComplexNumber tangent() {
    	return getICache(CachedValue.TAN);
    }
    
    private ComplexNumber tangent_priv() {
        ComplexNumber x = sine();
        ComplexNumber y = cosine();
        return new ComplexNumber(x.divide(y));        
    }
    
    public ComplexNumber tangenth() {
    	return getICache(CachedValue.TANH);
    }
    
    private ComplexNumber tangenth_priv() {
        ComplexNumber x = sineh();
        ComplexNumber y = cosineh();
        return new ComplexNumber(x.divide(y));
    }
    
    public ComplexNumber atangent() {
    	return getICache(CachedValue.ATAN);
    }
    
    private ComplexNumber atangent_priv() {
        //return -I/2 * ((I - this)/(I + this)).Log();
        ComplexNumber negI2 = getTemp().set(0, -0.5);
        ComplexNumber Imt = getTemp().set(0-real, 1-imag);
        ComplexNumber Ipt = getTemp().set(0+real, 1+imag);
        ComplexNumber rhs = Imt.divide(Ipt).ln();
        return new ComplexNumber(negI2.multiply(rhs));
    }
    
    public ComplexNumber atangenth() {
    	return getICache(CachedValue.ATANH);
    }
    
    private ComplexNumber atangenth_priv() {
        // return ((1+this) / (1-this)).Log() / 2d;
        ComplexNumber opt = getTemp().set(real + 1, imag);
        ComplexNumber omt = getTemp().set(1 - real, imag);
        return new ComplexNumber(opt.divide(omt).ln().scaleEq(0.5));
    }
    
    public ComplexNumber ln() {
    	return getICache(CachedValue.LN);
    }
    
    private ComplexNumber ln_priv() {
        return new ComplexNumber(Math.log(mod()),arg());
    }

    public ComplexNumber exp() {
    	return getICache(CachedValue.EXP);
    }
    
    private ComplexNumber exp_priv() {
        double ex = Math.exp(real);
        return new ComplexNumber(ex*Math.cos(imag),ex*Math.sin(imag));
    }

    public ComplexNumber set(double a, double b) {
    	clearCache();
        this.real = a;
        this.imag = b;
        return this;
    }

    public ComplexNumber copy(ComplexNumber rhs) {
    	clearCache();
        return set(rhs.real, rhs.imag);
    }

    public double getReal() {
        return real;
    }
    
    public double getImag() {
        return imag;
    }
    
    public boolean equals(Object o) {
        ComplexNumber c = (ComplexNumber)o;
        return c.real == real && c.imag == imag;
    }
    
    public boolean isNaN()
    {
        return Double.isNaN(real) || Double.isNaN(imag);
    }

    public boolean isInfinity()
    {
        return Double.isInfinite(real) || Double.isInfinite(imag);
    }
    
    public boolean isReal() {
        return Math.abs(imag) <= 0.0000001;
    }

    // TODO: Move to Utils
    public static String formatDouble(double d) {
    	 String opt1 = String.valueOf(d);
    	 String opt2 = String.format("%.6f", d);
    	 while (opt2.length() > 2 && opt2.charAt(opt2.length()-1) == '0' && opt2.charAt(opt2.length()-2) != '.') {
    		 opt2 = opt2.substring(0, opt2.length()-1);
    	 }
    	 
    	 if (opt1.length() < opt2.length())
    		 return opt1;
    	 
    	 return opt2;
    }
    
    public String toString() {
        if (isNaN())
            return "NaN";
        if (isInfinity())
            return "INF";
        if (isReal())
            return formatDouble(real);
        return "[" + formatDouble(real) + "," + formatDouble(imag) + "]";
    }

    static ComplexNumber [] pool = new ComplexNumber[256];    
    static int current = 0;
    
    static {
        for (int i=0; i<pool.length; i++) {
            pool[i] = new ComplexNumber();
        }
    }
    
    static ComplexNumber getTemp() {
        ComplexNumber n = pool[current].set(0,0);
        current = (current+1) % pool.length;
        return n;
    }
    
}// end ComplexNumber class
