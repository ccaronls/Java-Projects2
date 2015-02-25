package cc.lib.game;


/**
 * Class for managing a 3x3 matrix with implied value for
 * the final column (0,0,1) making this a 2x3 matrix really.
 * 
 * Good for simple 2D graphics.
 * 
 * @author Chris Caron
 *
 */
public final class Matrix {
	//private float [] M = new float [6];
	private float a,b,c,d,x,y;
	
	public Matrix() {}
	
	public Matrix(Matrix m) {
	    copy(m);
    }

    private void set(float a, float b, float c, float d, float x, float y) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.x = x;
		this.y = y;
	}
	
	/**
	 * zero this matrix
	 */
	public void zero() {
		set(0,0,0,0,0,0);
	}
	
	
		
	/**
	 * make this into the identity matrix
	 */
	public void identity() {
		set(1,0,0,1,0,0);
	}
	
	/**
	 * copy all components of m into this
	 * @param m
	 */
	public void copy(Matrix M) {
		a = M.a;
		b = M.b;
		c = M.c;
		d = M.d;
		x = M.x;
		y = M.y;
	}
		
	/**
	 * set this to the scale matrix
	 * @param x
	 * @param y
	 */
	public void setScale(float sx, float sy) {
		set(sx, 0, 0, sy, 0, 0);
	}
		
	/**
	 * set this to the translation matrix
	 * @param x
	 * @param y
	 */
	public void setTranslate(float x, float y) {
		set(1,0,0,1,x,y);
	}
		
	/**
	 * set this to the rotation matrix
	 * @param angle
	 */
	public void setRotation(float degrees) {
		zero();
		float rads  = degrees * Utils.DEG_TO_RAD;
		float cos	= (float)Math.cos(rads);
		float sin	= (float)Math.sin(rads);
		set(cos, sin, -sin, cos, 0, 0);
	}
		
	/**
	 * set result to this * m
	 * @param m
	 * @param result
	 */
	public void multiply(Matrix m, Matrix result) {
		result.set(
			a*m.a + b*m.c,
			a*m.b + b*m.d,
			c*m.a + d*m.c,
			c*m.b + d*m.d,
			x*m.a + y*m.c + m.x,
			x*m.b + y*m.d + m.y
		);		
	}

	
	
	/**
	 * 
	 * @param vector
	 * @param result
	 */
	public void multiply(final float [] vector, float [] result) {
		result[0] = a * vector[0] + c * vector[1] + x;
		result[1] = b * vector[0] + d * vector[1] + y;
	}

    /**
     * 
     * @param vector
     * @param result
     */
    public void multiply(final Vector2D v, MutableVector2D out) {
        out.set(a*v.getX() + c*v.getY() + x, b*v.getX() + d*v.getY() + y);
    }
	
    private float [] t_result = {0,0}; 

    /**
	 * 
	 * @param vector
	 */
	public void transform(float [] vector) {
		multiply(vector, t_result);
		vector[0] = t_result[0];
		vector[1] = t_result[1];
	}

 	/**
	 * 
	 * @param t
	 * @param result
	 */
	public void transform(ITransformable t, float [] result) {
		float [] v = { t.getX(), t.getY() };
		multiply(v, result);
	}
	
	/**
	 * 
	 * @param tx
	 * @param ty
	 */
	public void translate(float tx, float ty) {
		x += tx;
		y += ty;
	}

	   /**
     * 
     * @param tx
     * @param ty
     */
    public void translate(Vector2D v) {
        x += v.getX();
        y += v.getY();
    }

	/**
	 * 
	 * @param scalar
	 */
	public void scale(float scalar) {
		a*=scalar;
		b*=scalar;
		c*=scalar;
		d*=scalar;
		x*=scalar;
		y*=scalar;
	}
	
	public void setOrtho(float left, float right, float top, float bottom) {
		set(2.0f / (right - left), 0, 
	        0, 2.0f / (top - bottom), 
	        (right + left) / (right - left), (top + bottom) / (top - bottom));
	}
}
