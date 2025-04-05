package cc.lib.game;

import cc.lib.math.Matrix3x3;
import cc.lib.math.MutableVector2D;

public class Transformer {
	
	public Transformer() {
		m_stack = new Matrix3x3[8];
		for (int i=0; i<m_stack.length; i++) {
			m_stack[i] = new Matrix3x3();
		}
		m_stack_size = 1;
		cur_mat = m_stack[0];
		s_mat = new Matrix3x3();
		t_mat = new Matrix3x3();
		makeIdentity();
	} 
	
	/**
	 * multiply a translation matrix to the transform
	 * @param tx
	 * @param ty
	 */
	public final void translate(float tx, float ty) {
		t_mat.setTranslate(tx, ty);
		cur_mat.multiply(t_mat, s_mat);
		cur_mat.copy(s_mat);
	}

	/**
	 * multiply a rotation matrix to the transform
	 * @param angle
	 */
	public final void rotate(float angle) {
		t_mat.setRotation(angle);
		cur_mat.multiply(t_mat, s_mat);
		cur_mat.copy(s_mat);
	}
	
	/**
	 * Scale all components of current transform by a scalar
	 * @param s
	 */
	public final void scale(float s) {
		scale(s, s);
	}

	/**
	 * multiply a scale matrix to the transform
	 * @param sx
	 * @param sy
	 */
	public final void scale(float sx, float sy) {
		t_mat.setScale(sx, sy);
		cur_mat.multiply(t_mat, s_mat);
		cur_mat.copy(s_mat);
	}
	
	/**
	 * set the top matrix to the identity matrix
	 */
	public final void makeIdentity() {
		cur_mat.identityEq();
	}
	
	/**
	 * 
	 * @param v
	 */
    public final void transformXY(MutableVector2D v) {
        transformXY(v.getX(), v.getY(), v);
    }

    /**
     * 
     * @param v
     */
    public final void transformXY(float [] v) {
        transformXY(v[0], v[1], v);
    }

    /**
     * 
     * @param x
     * @param y
     * @param v
     */
    public final void transformXY(float x, float y, MutableVector2D v) {
        v.assign(x, y);
        cur_mat.transform(v);
    }

    /**
     * 
     * @param x
     * @param y
     * @param v
     */
    public final void transformXY(float x, float y, float [] v) {
        s_vec.assign(x, y);
        cur_mat.transform(s_vec);
        v[0] = s_vec.getX();
        v[1] = s_vec.getY();
    }

	/**
	 * Push the matrix stack
	 */
	public final void pushMatrix() {
		if (m_stack_size < m_stack.length)
		{
			m_stack[m_stack_size].copy(m_stack[m_stack_size-1]);
			cur_mat = m_stack[m_stack_size];
			m_stack_size++;
		} else
			throw new RuntimeException("Fixed stack size of [" + m_stack.length + "] for pushMatrix()");
	}
	
	/**
	 * pop off the matrix stack
	 */
	public final void popMatrix() {
		if (m_stack_size > 1) {
		    m_stack_size -= 1;
			cur_mat = m_stack[m_stack_size-1];
		}
		else
			throw new RuntimeException("too many pops");
	}

	/**
	 * Return the top level matrix
	 * @return
	 */
	public final Matrix3x3 getCurrentTransform() {
		return new Matrix3x3(cur_mat);
	}
	
	////////////////////////////////////////////////////
	// PRIVATE STUFF ///////////////////////////////////
	////////////////////////////////////////////////////
	
	private Matrix3x3 []	m_stack;
	private Matrix3x3		cur_mat;
	private int			m_stack_size;
	private Matrix3x3		s_mat; // working matrix
	private Matrix3x3		t_mat; // working matrix
	private MutableVector2D s_vec = new MutableVector2D();

}
