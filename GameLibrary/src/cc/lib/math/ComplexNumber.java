package cc.lib.math;

import cc.lib.utils.Reflector;

/**
 * Complex arithmetic.  Most functions are accounted for and gotten from web.
 * Some effort made to speed up computations, specifically 
 * 1> Having 3 pow versions (int, double, complex) (fast, med, slow)
 * 2> No calls to 'new' user provides value for result to support caching
 *
 * @author chriscaron
 *
 */
public final class ComplexNumber extends Reflector<ComplexNumber> {

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
     * 
     * @param rhs
     * @return
     */
    public ComplexNumber add(ComplexNumber rhs) {
        return new ComplexNumber(real + rhs.real, imag + rhs.imag);
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
     * 
     * @param rhs
     * @return
     */
    public ComplexNumber sub(ComplexNumber rhs) {
        return new ComplexNumber(real - rhs.real, imag - rhs.imag);
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
        return new ComplexNumber(-real, -imag);
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
        return new ComplexNumber(real * scalar, imag * scalar);
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
        return new ComplexNumber(real*rhs.real - imag*rhs.imag, imag*rhs.real + real*rhs.imag);
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
     * @return
     */
    public ComplexNumber divide(ComplexNumber denom) {
        double den=Math.pow(denom.mod(),2);
        return new ComplexNumber((real*denom.real+imag*denom.imag)/den,(imag*denom.real-real*denom.imag)/den);    
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
     * @return
     */
    public ComplexNumber conj() {
        return new ComplexNumber(-imag, real);
    }

    /**
     * Return sqrt(this)
     * @return
     */
    public ComplexNumber sqrt() {
        double r=Math.sqrt(mod());
        double theta=arg()/2;
        return new ComplexNumber(r*Math.cos(theta),r*Math.sin(theta));
    }
    
    /**
     * Return this ^ int
     * @param power
     * @return
     */
    public ComplexNumber powi(int power) {
    	ComplexNumber result = new ComplexNumber();
        result.set(1,0);
        for (int i=0; i<power; i++) {
            result.multiplyEq(this);
        }
        return result;
    }

    /**
     * Return this ^ double
     * @param n
     * @return
     */
    public ComplexNumber powd(double n) {
    	ComplexNumber result = new ComplexNumber();
        double real = n * Math.log(abs());
        double imag = n * arg();
        double scalar = Math.exp(real);
        return result.set(scalar * Math.cos(imag), scalar * Math.sin(imag));
    }

    /**
     * Return [e^power.real * cos(power.imag), e^power.real * sin(power.imag)]
     * @param power
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
     * @return
     */
    public ComplexNumber sine() {
        return new ComplexNumber(cosh(imag)*Math.sin(real),sinh(imag)*Math.cos(real));
    }

    /**
     * Return [fs(this.real) * cos.(this.imag), fc(this.real)*sin(this.imag)]
     *   where fs(x) = (e^x - e^-x)/2
     *   and   fc(x) = (e^x + e^-x)/2
     * @return
     */
    public ComplexNumber sineh() {
        return new ComplexNumber(sinh(real)*Math.cos(imag),cosh(real)*Math.sin(imag));
    }

    /**
     * Return -I * ln((this*I) + sqrt(1-this^2))
     * @return
     */
    public ComplexNumber asine() {
        //return -I * ((this*I) + (1 - (this * this)).Sqrt()).Log();
        ComplexNumber negI = new ComplexNumber(0, -1);
        ComplexNumber ttI = this.multiply(negI);
        ComplexNumber lhs = negI.multiply(ttI);
        ComplexNumber ttt = this.multiply(this);
        ComplexNumber omttt = new ComplexNumber(1, 0).sub(ttt);
        ComplexNumber  rhs = omttt.sqrt().ln();
        return lhs.add(rhs);
    }
    
    /**
     * return ln(this + sqrt((this^2)+1))
     * 
     * @return
     */
    public ComplexNumber asineh() {
        //return (this + ((this*this) + 1).Sqrt()).Log();
        ComplexNumber tttpo = this.multiply(this);
        tttpo.real += 1;
        ComplexNumber rhs = tttpo.sqrt().ln();
        return add(rhs);
    }
    
    public ComplexNumber cosine() {
        return new ComplexNumber(cosh(imag)*Math.cos(real),-sinh(imag)*Math.sin(real));
    }

    /**
     * 
     * @return
     */
    public ComplexNumber cosineh() {
        return new ComplexNumber(cosh(real)*Math.cos(imag),sinh(real)*Math.sin(imag));
    }
    
    /**
     * Return -I * ln(this+I * sqrt(1-(this^2))
     * @return
     */
    public ComplexNumber acosine() {
        // return -I * (this + I * (1 - (this*this)).Sqrt()).Log();
        ComplexNumber negI = new ComplexNumber(0, -1);
        ComplexNumber ttt = this.multiply(this);
        ComplexNumber omttt = new ComplexNumber(1, 0).subEq(ttt);
        ComplexNumber rhs = add(new ComplexNumber(0, 1).multiply(omttt.sqrt().ln()));
        return negI.multiply(rhs);
    }
    
    /**
     * Return 2 * ln(sqrt((this+1)/2) + sqrt(((this-1)/2))
     * @return
     */
    public ComplexNumber acosineh() {
        //return 2d * (((this+1d) / 2d).Sqrt() + ((this-1) / 2d).Sqrt()).Log();
        ComplexNumber tp1d2 = new ComplexNumber((real+1)/2, imag/2);
        ComplexNumber tm1d2 = new ComplexNumber((real-1)/2, imag/2);
        ComplexNumber lhs = tp1d2.sqrt().scaleEq(2);
        ComplexNumber rhs = tm1d2.sqrt().ln();
        return lhs.add(rhs);
    }
    
    /**
     * Return sin(this) / cosine(this)
     * @return
     */
    public ComplexNumber tangent() {
        ComplexNumber x = sine();
        ComplexNumber y = cosine();
        return x.divide(y);        
    }
    
    public ComplexNumber tangenth() {
        ComplexNumber x = sineh();
        ComplexNumber y = cosineh();
        return x.divide(y);
    }
    
    public ComplexNumber atangent() {
        //return -I/2 * ((I - this)/(I + this)).Log();
        ComplexNumber negI2 = new ComplexNumber(0, -0.5);
        ComplexNumber Imt = new ComplexNumber(0-real, 1-imag);
        ComplexNumber Ipt = new ComplexNumber(0+real, 1+imag);
        ComplexNumber rhs = Imt.divide(Ipt).ln();
        return negI2.multiply(rhs);
    }
    
    public ComplexNumber atangenth() {
        // return ((1+this) / (1-this)).Log() / 2d;
        ComplexNumber opt = new ComplexNumber(real + 1, imag);
        ComplexNumber omt = new ComplexNumber(1 - real, imag);
        return opt.divide(omt).ln().scaleEq(0.5);
    }
    
    public ComplexNumber ln() {
        return new ComplexNumber(Math.log(mod()),arg());
    }

    public ComplexNumber exp() {
        double ex = Math.exp(real);
        return new ComplexNumber(ex*Math.cos(imag),ex*Math.sin(imag));
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

    @Override
    protected boolean isImmutable() {
        return true;
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
    
}// end ComplexNumber class
