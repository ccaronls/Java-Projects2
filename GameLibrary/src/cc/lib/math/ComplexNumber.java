package cc.lib.math;

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
     * Return this + rhs
     * @param rhs
     * @param result
     * @return
     */
    public ComplexNumber add(ComplexNumber rhs, ComplexNumber result) {
        return result.set(real + rhs.real, imag + rhs.imag);
    }

    /**
     * Modifier, return this + rhs
     * @param rhs
     * @return
     */
    public ComplexNumber addEq(ComplexNumber rhs) {
        return set(real + rhs.real, imag + rhs.imag);
    }

    /**
     * Return this - rhs
     * @param rhs
     * @param result
     * @return
     */
    public ComplexNumber sub(ComplexNumber rhs, ComplexNumber result) {
        return result.set(real - rhs.real, imag - rhs.imag);
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

    /**
     * Modifier, return -this
     * @return
     */
    public ComplexNumber negateEq() {
        return set(-real, -imag);
    }

    /**
     * Return this scale by a double
     * @param scalar
     * @param result
     * @return
     */
    public ComplexNumber scale(double scalar, ComplexNumber result) {
        return result.set(real * scalar, imag * scalar);
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
     * Return [this.real*rhs.read - this.imag*rhs.imag, this.imag*rhs.real + this.real*rhs.imag] 
     * @param rhs
     * @param result
     * @return
     */
    public ComplexNumber multiply(ComplexNumber rhs, ComplexNumber result) {
        return result.set(real*rhs.real - imag*rhs.imag, imag*rhs.real + real*rhs.imag);
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
    public ComplexNumber divide(ComplexNumber denom, ComplexNumber result) {
        double den=Math.pow(denom.mod(),2);
        return result.set((real*denom.real+imag*denom.imag)/den,(imag*denom.real-real*denom.imag)/den);    
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
        if (real != 0 || imag!= 0)
            return Math.sqrt(real*real + imag*imag);
        return 0;
    }

    /**
     * Return atan2(this.imag, this.real)
     * @return
     */
    public double arg() {
        return Math.atan2(imag, real);
    }
    
    /**
     * Return [-this.imag, this.real]
     * @param result
     * @return
     */
    public ComplexNumber conj(ComplexNumber result) {
        return result.set(-imag, real);
    }

    /**
     * Return sqrt(this)
     * @param result
     * @return
     */
    public ComplexNumber sqrt(ComplexNumber result) {
        double r=Math.sqrt(mod());
        double theta=arg()/2;
        return result.set(r*Math.cos(theta),r*Math.sin(theta));
    }
    
    /**
     * Return this ^ int
     * @param power
     * @param result
     * @return
     */
    public ComplexNumber powi(int power, ComplexNumber result) {
        result.set(1,0);
        for (int i=0; i<power; i++) {
            multiply(result, result);
        }
        return result;
    }

    /**
     * Return this ^ double
     * @param n
     * @param result
     * @return
     */
    public ComplexNumber powd(double n, ComplexNumber result) {
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
    public ComplexNumber powc(ComplexNumber power, ComplexNumber result) {
        return power.multiply(ln(getTemp()), getTemp()).exp(getTemp());
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
    public ComplexNumber sine(ComplexNumber result) {
        return result.set(cosh(imag)*Math.sin(real),sinh(imag)*Math.cos(real));
    }

    /**
     * Return [fs(this.real) * cos.(this.imag), fc(this.real)*sin(this.imag)]
     *   where fs(x) = (e^x - e^-x)/2
     *   and   fc(x) = (e^x + e^-x)/2
     * @param result
     * @return
     */
    public ComplexNumber sineh(ComplexNumber result) {
        return result.set(sinh(real)*Math.cos(imag),cosh(real)*Math.sin(imag));
    }

    /**
     * Return -I * ln((this*I) + sqrt(1-this^2))
     * @param result
     * @return
     */
    public ComplexNumber asine(ComplexNumber result) {
        //return -I * ((this*I) + (1 - (this * this)).Sqrt()).Log();
        ComplexNumber negI = getTemp().set(0, -1);
        ComplexNumber ttI = this.multiply(negI, getTemp());
        ComplexNumber lhs = negI.multiply(ttI, getTemp());
        ComplexNumber ttt = this.multiply(this, getTemp());
        ComplexNumber omttt = getTemp().set(1, 0).sub(ttt, getTemp());
        ComplexNumber  rhs = omttt.sqrt(getTemp()).ln(getTemp());
        return lhs.add(rhs, result);
    }
    
    /**
     * return ln(this + sqrt((this^2)+1))
     * 
     * @param result
     * @return
     */
    public ComplexNumber asineh(ComplexNumber result) {
        //return (this + ((this*this) + 1).Sqrt()).Log();
        ComplexNumber tttpo = this.multiply(this, getTemp());
        tttpo.real += 1;
        ComplexNumber rhs = tttpo.sqrt(getTemp()).ln(getTemp());
        return add(rhs, result);
    }
    
    public ComplexNumber cosine(ComplexNumber result) {
        return result.set(cosh(imag)*Math.cos(real),-sinh(imag)*Math.sin(real));
    }

    /**
     * 
     * @param result
     * @return
     */
    public ComplexNumber cosineh(ComplexNumber result) {
        return result.set(cosh(real)*Math.cos(imag),sinh(real)*Math.sin(imag));
    }
    
    /**
     * Return -I * ln(this+I * sqrt(1-(this^2))
     * @param result
     * @return
     */
    public ComplexNumber acosine(ComplexNumber result) {
        // return -I * (this + I * (1 - (this*this)).Sqrt()).Log();
        ComplexNumber negI = getTemp().set(0, -1);
        ComplexNumber ttt = this.multiply(this, getTemp());
        ComplexNumber omttt = getTemp().set(1, 0).subEq(ttt);
        ComplexNumber rhs = add(getTemp().set(0, 1).multiply(omttt.sqrt(getTemp()).ln(getTemp()), getTemp()), getTemp());
        return negI.multiply(rhs, result);
    }
    
    /**
     * Return 2 * ln(sqrt((this+1)/2) + sqrt(((this-1)/2))
     * @param result
     * @return
     */
    public ComplexNumber acosineh(ComplexNumber result) {
        //return 2d * (((this+1d) / 2d).Sqrt() + ((this-1) / 2d).Sqrt()).Log();
        ComplexNumber tp1d2 = getTemp().set((real+1)/2, imag/2);
        ComplexNumber tm1d2 = getTemp().set((real-1)/2, imag/2);
        ComplexNumber lhs = tp1d2.sqrt(getTemp()).scaleEq(2);
        ComplexNumber rhs = tm1d2.sqrt(getTemp()).ln(getTemp());
        return lhs.add(rhs, result);
    }
    
    /**
     * Return sin(this) / cosine(this)
     * @param result
     * @return
     */
    public ComplexNumber tangent(ComplexNumber result) {
        ComplexNumber x = sine(getTemp());
        ComplexNumber y = cosine(getTemp());
        return x.divide(y, result);        
    }
    
    public ComplexNumber tangenth(ComplexNumber result) {
        ComplexNumber x = sineh(getTemp());
        ComplexNumber y = cosineh(getTemp());
        return x.divide(y, result);
    }
    
    public ComplexNumber atangent(ComplexNumber result) {
        //return -I/2 * ((I - this)/(I + this)).Log();
        ComplexNumber negI2 = getTemp().set(0, -0.5);
        ComplexNumber Imt = getTemp().set(0-real, 1-imag);
        ComplexNumber Ipt = getTemp().set(0+real, 1+imag);
        ComplexNumber rhs = Imt.divide(Ipt, getTemp()).ln(getTemp());
        return negI2.multiply(rhs, result);
    }
    
    public ComplexNumber atangenth(ComplexNumber result) {
        // return ((1+this) / (1-this)).Log() / 2d;
        ComplexNumber opt = getTemp().set(real + 1, imag);
        ComplexNumber omt = getTemp().set(1 - real, imag);
        return opt.divide(omt, getTemp()).ln(result).scaleEq(0.5);
    }
    
    public ComplexNumber ln(ComplexNumber result) {
        return result.set(Math.log(mod()),arg());
    }

    public ComplexNumber exp(ComplexNumber result) {
        double ex = Math.exp(real);
        return result.set(ex*Math.cos(imag),ex*Math.sin(imag));
    }

    public ComplexNumber set(double a, double b) {
        this.real = a;
        this.imag = b;
        return this;
    }

    public ComplexNumber copy(ComplexNumber rhs) {
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
    
    private String formatDouble(double d) {
        if (Math.abs(d) < 0.00000001)
            return String.valueOf(d);
        return String.format("%.5f", d);
    }
    
    public String toString() {
        if (isNaN())
            return "NaN";
        if (isInfinity())
            return "INF";
        if (isReal())
            return formatDouble(real);
        //return "[" + real + (imag >= 0 ? "+" : "") + imag + "i]";
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
