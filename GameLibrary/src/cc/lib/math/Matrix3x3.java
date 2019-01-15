package cc.lib.math;

import cc.lib.game.IVector2D;
import cc.lib.utils.Reflector;


/**
 * A 3x3 matrix implementation
 */
public final class Matrix3x3 extends Reflector<Matrix3x3>  {
	
	static {
		addAllFields(Matrix3x3.class);
	}

	public final static Matrix3x3 newIdentity() {
	    return new Matrix3x3(1,0,0,0,1,0,0,0,1);
    }

	private double a11, a12, a13;
	private double a21, a22, a23;
	private double a31, a32, a33;

    /**
     * Builds a zero matrix
     */
	public Matrix3x3() {
		a11=0; a12=0; a13=0;
		a21=0; a22=0; a23=0;
		a31=0; a32=0; a33=0;
	}

	/**
	 * Assign the zero matrix to this matrix
	 * @return <code>this</code>
	 */
	public final Matrix3x3 zeroEq() {
		a11 = 0; a12 = 0; a13 = 0;
		a21 = 0; a22 = 0; a23 = 0;
		a31 = 0; a32 = 0; a33 = 0;
		return this;
	}

	/**
	 * Construct a 3x3 matrix using specified fields
	 * @param a11
	 * @param a12
	 * @param a13
	 * @param a21
	 * @param a22
	 * @param a23
	 * @param a31
	 * @param a32
	 * @param a33
	 */
	public Matrix3x3(double a11, double a12, double a13, double a21, double a22,
			double a23, double a31, double a32, double a33) {  
		this.a11 = a11;
		this.a12 = a12;
		this.a13 = a13;
		this.a21 = a21;
		this.a22 = a22;
		this.a23 = a23;
		this.a31 = a31;
		this.a32 = a32;
		this.a33 = a33;
	}


	/**
	 * Construct a new 3x3 matrix as a copy of the given matrix B
	 * @param B
	 * @throws NullPointerException
	 */
	public Matrix3x3( Matrix3x3 B) {
		assign(B);
	}

	/**
	 * assign the value of B to this Matrix
	 * @param B
	 */
	public final Matrix3x3 assign(Matrix3x3 B) {
		a11 = B.a11; a12 = B.a12; a13 = B.a13;
		a21 = B.a21; a22 = B.a22; a23 = B.a23;
		a31 = B.a31; a32 = B.a32; a33 = B.a33;
		return this;
	}

	/**
	 * Assign the scale matrix given by s, to this matrix
	 */
	public final Matrix3x3 scaleEq(final double s) {
		a11 = s; a12 = 0; a13 = 0;
		a21 = 0; a22 = s; a23 = 0;
		a31 = 0; a32 = 0; a33 = s;
		return this;
	}

	/**
	 * Assign the non-uniform scale matrix given by s1, s2 and s3, to this matrix
	 */
	public final Matrix3x3 scaleEq(double sx, double sy, double sz) {
		a11 = sx; a12 = 0.; a13 = 0.;
		a21 = 0.; a22 = sy; a23 = 0.;
		a31 = 0.; a32 = 0.; a33 = sz;
		return this;
	}


	/**
	 * Assign the identity matrix to this matrix
	 */
	public final Matrix3x3 identityEq() {
		a11 = 1; a12 = 0; a13 = 0;
		a21 = 0; a22 = 1; a23 = 0;
		a31 = 0; a32 = 0; a33 = 1;
		return this;
	}

	public final Matrix3x3 assign(
			double a11, double a12, double a13,
			double a21, double a22, double a23,
			double a31, double a32, double a33) {
		this.a11 = a11;  this.a12 = a12;  this.a13 = a13;
		this.a21 = a21;  this.a22 = a22;  this.a23 = a23;
		this.a31 = a31;  this.a32 = a32;  this.a33 = a33;
		return this;
	}

	/**
	 * Multiply this matrix by a scalar, return the resulting matrix
	 * @param s
	 * @return
	 */
	public final Matrix3x3 multiply( double s) {
		Matrix3x3 A = new Matrix3x3();
		A.a11 = a11*s; A.a12 = a12*s; A.a13 = a13*s;
		A.a21 = a21*s; A.a22 = a22*s; A.a23 = a23*s;
		A.a31 = a31*s; A.a32 = a32*s; A.a33 = a33*s;    
		return A;
	}

	/**
	 * Multiply this matrix by the matrix A and return the result
	 * @param A
	 * @return
	 */
	public final Matrix3x3 multiply(Matrix3x3 A) {
		return multiply(this,A,new Matrix3x3());
	}

	/**
	 * Multiply this matrix by the matrix A and return the result
	 * @param A
	 * @return
	 */
	public final Matrix3x3 multiplyEq(Matrix3x3 A) {
		return multiply(this,A,this);
	}

	//C = AxB 
	public static Matrix3x3 multiply( final Matrix3x3 A, final Matrix3x3 B, final Matrix3x3 C ) {
		//               B | b11 b12 b13
		//                 | b21 b22 b23
		//                 | b31 b32 b33
		//     -------------------------
		//  A  a11 a12 a13 | c11 c12 c13
		//     a21 a22 a23 | c21 c22 c23
		//     a31 a32 a33 | c31 c32 c33  C

		double t11 = A.a11*B.a11 + A.a12*B.a21 + A.a13*B.a31;
		double t12 = A.a11*B.a12 + A.a12*B.a22 + A.a13*B.a32;
		double t13 = A.a11*B.a13 + A.a12*B.a23 + A.a13*B.a33;

		double t21 = A.a21*B.a11 + A.a22*B.a21 + A.a23*B.a31;
		double t22 = A.a21*B.a12 + A.a22*B.a22 + A.a23*B.a32;
		double t23 = A.a21*B.a13 + A.a22*B.a23 + A.a23*B.a33;

		double t31 = A.a31*B.a11 + A.a32*B.a21 + A.a33*B.a31;
		double t32 = A.a31*B.a12 + A.a32*B.a22 + A.a33*B.a32;
		double t33 = A.a31*B.a13 + A.a32*B.a23 + A.a33*B.a33;

		//copy to C
		C.a11 = t11;
		C.a12 = t12;
		C.a13 = t13;

		C.a21 = t21;
		C.a22 = t22;
		C.a23 = t23;

		C.a31 = t31;
		C.a32 = t32;
		C.a33 = t33;

		return C;
	}

	//functional
	/**
	 * Multiply a vector by this matrix, return the resulting vector
	 */
	public final Vector2D multiply( final Vector2D v) {
		MutableVector2D r = new MutableVector2D();
		Matrix3x3.multiply(this, v, r);
		return r;
	}


	//A = A^T 
	public final Matrix3x3 transposeEq() {
		double t;
		t=a12; a12=a21; a21=t;
		t=a13; a13=a31; a31=t;
		t=a23; a23=a32; a32=t;
		return this;
	}

	/**
	 * Functional method. Transpose this matrix and return the result
	 * @return
	 */
	public final Matrix3x3 transpose() {
		return new Matrix3x3(this).transposeEq();
	}


	//C = A-B
	public static Matrix3x3 subtract( final Matrix3x3 A, final Matrix3x3 B, final Matrix3x3 C ) {
		C.a11 = A.a11-B.a11; C.a12 = A.a12-B.a12; C.a13 = A.a13-B.a13;
		C.a21 = A.a21-B.a21; C.a22 = A.a22-B.a22; C.a23 = A.a23-B.a23;
		C.a31 = A.a31-B.a31; C.a32 = A.a32-B.a32; C.a33 = A.a33-B.a33;
		return C;
	}
	/**
	 * Substract to this matrix the matrix B, return result in a new matrix instance
	 * @param B
	 * @return
	 */
	public final Matrix3x3 subtract( Matrix3x3 B ) {
		return subtract(this,B,new Matrix3x3());
	}
	/**
	 * Substract to this matrix the matrix B, return result in a new matrix instance
	 * @param B
	 * @return
	 */
	public final Matrix3x3 subtractEq( Matrix3x3 B ) {
		return subtract(this,B,this);
	}
	/**
	 * Add to this matrix the matrix B, return result in a new matrix instance
	 * @param B
	 * @return
	 */
	public final Matrix3x3 add( Matrix3x3 B ) {
		return add(this,B,new Matrix3x3());
	}
	/**
	 * Add to this matrix the matrix B, return result in a new matrix instance
	 * @param B
	 * @return
	 */
	public final Matrix3x3 addEq( Matrix3x3 B ) {
		return add(this,B,this);
	}

	//C = A+B
	public static Matrix3x3 add( final Matrix3x3 A, final Matrix3x3 B, final Matrix3x3 C ) {
		C.a11 = A.a11+B.a11; C.a12 = A.a12+B.a12; C.a13 = A.a13+B.a13;
		C.a21 = A.a21+B.a21; C.a22 = A.a22+B.a22; C.a23 = A.a23+B.a23;
		C.a31 = A.a31+B.a31; C.a32 = A.a32+B.a32; C.a33 = A.a33+B.a33;
		return C;
	}


	/**
	 * Multiply v by A, and place result in r, so r = Av
	 * @param A 3 by 3 matrix
	 * @param v Vector to be multiplied
	 * @param r Vector to hold result of multiplication
	 * @return Reference to the given Vector2D r instance
	 */
	public static MutableVector2D multiply( final Matrix3x3 A, final Vector2D v, final MutableVector2D r ) {
		//                   
		//               V | v1
		//                 | v2
		//                 | 1
		//     -----------------
		//  A  a11 a12 a13 | c1
		//     a21 a22 a23 | c2
		//     a31 a32 a33 | c3   

		double t1 = v.x*A.a11+v.y*A.a12+A.a13;
		double t2 = v.x*A.a21+v.y*A.a22+A.a23;
		double t3 = v.x*A.a31+v.y*A.a32+A.a33;

		r.set((float)(t1/t3), (float)(t2/t3));

		return r;
	}  

	/**
	 * Compute the determinant of Matrix A
	 * @return 
	 */
	public final double determinant() {
        return a31*a12*a23 - a31*a13*a22 - a21*a12*a33 + a21*a13*a32 + a11*a22*a33 - a11*a23*a32;
//		return a11*a22*a33 - a11*a23*a32 + a21*a32*a13 - a21*a12*a33 + a31*a12*a23 - a31*a22*a13;
	}

	/**
	 * Compute the inverse of the matrix A, place the result in C
	 */
	public static Matrix3x3 inverse( final Matrix3x3 A, final Matrix3x3 C ) {
		double d = A.determinant();//(A.a31*A.a12*A.a23-A.a31*A.a13*A.a22-A.a21*A.a12*A.a33+A.a21*A.a13*A.a32+A.a11*A.a22*A.a33-A.a11*A.a23*A.a32);
		double t11 =  (A.a22*A.a33-A.a23*A.a32)/d;
		double t12 = -(A.a12*A.a33-A.a13*A.a32)/d;
		double t13 =  (A.a12*A.a23-A.a13*A.a22)/d;
		double t21 = -(-A.a31*A.a23+A.a21*A.a33)/d;
		double t22 =  (-A.a31*A.a13+A.a11*A.a33)/d;
		double t23 = -(-A.a21*A.a13+A.a11*A.a23)/d;
		double t31 =  (-A.a31*A.a22+A.a21*A.a32)/d;
		double t32 = -(-A.a31*A.a12+A.a11*A.a32)/d;
		double t33 =  (-A.a21*A.a12+A.a11*A.a22)/d;

		C.a11 = t11; C.a12 = t12; C.a13 = t13;
		C.a21 = t21; C.a22 = t22; C.a23 = t23;
		C.a31 = t31; C.a32 = t32; C.a33 = t33;
		return C;
	}

	public final Matrix3x3 inverseEq() {
		return inverse(this,this);
	}

	public final Matrix3x3 inverse() {
		return inverse(this,new Matrix3x3());
	}

	@Override
	public final String toString() {
		return "["+a11+", " + a12 + ", " + a13 + "]\n"
				+ "["+a21+", " + a22 + ", " + a23 + "]\n"
				+ "["+a31+", " + a32 + ", " + a33 + "]" ;
	}

	/**
	 * Check matrix for NaN values 
	 */
	public final boolean isNaN() {
		return Double.isNaN(a11)||Double.isNaN(a12)||Double.isNaN(a13)
				|| Double.isNaN(a21)||Double.isNaN(a22)||Double.isNaN(a23)
				|| Double.isNaN(a31)||Double.isNaN(a32)||Double.isNaN(a33);
	}

	public final double[] toArray() {
		return new double[]{
				a11, a21, a31,
				a12, a22, a32,
				a13, a23, a33};
	}

    public final float[] toFloatArray() {
        return new float[]{
                (float)a11, (float)a21, (float)a31,
                (float)a12, (float)a22, (float)a32,
                (float)a13, (float)a23, (float)a33};
    }

    /**
	 * Return the Frobenius norm of this Matrix
	 * @return
	 */
	public final double fnorm() {
		return Math.sqrt(  a11*a11 + a12*a12 + a13*a13  + a21*a21 + a22*a22  + a23*a23  + a31*a31 + a32*a32 + a33*a33 ); 
	}
	
	public final Matrix3x3 setTranslate(IVector2D v) {
		return setTranslate(v.getX(), v.getY());
	}

	public final Matrix3x3 setTranslate(float tx, float ty) {
		return assign(1, 0, tx, 0, 1, ty, 0, 0, 1);
	}

	public final Matrix3x3 multiply(Matrix3x3 M, Matrix3x3 R) {
		return multiply(this, M, R);
	}

	public final Matrix3x3 copy(Matrix3x3 M) {
		return assign(M);
	}

	public final Matrix3x3 setRotation(float degrees) {
		double rads  = degrees * CMath.DEG_TO_RAD;
		double cos	= Math.cos(rads);
		double sin	= Math.sin(rads);
		return assign(cos, -sin, 0, sin, cos, 0, 0, 0, 1);
	}

	public final Matrix3x3 setScale(float sx, float sy) {
		return assign(sx, 0, 0, 0, sy, 0, 0, 0, 1);
	}

	public final Matrix3x3 setHorzSkew(float n) {
	    return assign(1-2*n, n, 0, 0, 1, 0, 0, 0, 1);
    }

	public final void transform(MutableVector2D v) {
		Vector2D V2 = multiply(v);
		v.set(V2);
	}

    public final Matrix3x3 translate(Vector2D v) {
		Matrix3x3 T = new Matrix3x3();
		T.setTranslate(v.X(), v.Y());
		return multiply(T);
	}

	public final Matrix3x3 rotateEq(float degrees) {
		Matrix3x3 T = new Matrix3x3();
		T.setRotation(degrees);
		assign(multiply(T));
		return this;
	}
}
