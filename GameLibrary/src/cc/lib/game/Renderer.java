package cc.lib.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cc.lib.math.Matrix3x3;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.utils.GException;

/**
 * Provides mechanism to manipulate vectors in a OpenGL type way
 * 
 * @author chriscaron
 *
 */
public class Renderer {

    public static boolean USE_PROJECTION_MATRIX = true;

    final int MATRIX_STACK_SIZE = 16;

	public Renderer(Renderable window) {
		this.window = window;		
		pts = new MutableVector2D[MAX_VERTS];
		names = new int[MAX_VERTS];
		for (int i=0; i<MAX_VERTS; i++) {
			pts[i] = new MutableVector2D();
		}
		m_stack = new Matrix3x3[MATRIX_STACK_SIZE];
		for (int i=0; i<m_stack.length; i++) {
			m_stack[i] = new Matrix3x3();
		}
		m_stack_size = 1;
		cur_mat = m_stack[0];
		m_stack_proj = new Matrix3x3[MATRIX_STACK_SIZE];
		for (int i=0; i<m_stack_proj.length; i++) {
		    m_stack_proj[i] = new Matrix3x3();
        }
		proj_mat = m_stack_proj[0];
		s_mat = new Matrix3x3();
		t_mat = new Matrix3x3();
		makeIdentity();
	}

    /**
     *
     * @param m
     */
	public final void multiply(Matrix3x3 m) {
	    cur_mat.multiply(m, s_mat);
	    cur_mat.copy(s_mat);
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
     *
     * @param v
     */
	public final void translate(IVector2D v) {
	    translate(v.getX(), v.getY());
    }

	/**
	 * multiply a rotation matrix to the transform
	 * @param degrees
	 */
	public final void rotate(float degrees) {
		t_mat.setRotation(degrees);
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
	 * Add a transformed vertex to the list
	 * @param x
	 * @param y
	 */
	public final void addVertex(float x, float y) {
		transformXY(x, y, s_vec);
		pts[num_pts].set(s_vec.X(), s_vec.Y());
		names[num_pts++] = cur_name;
	}

	public final List<IVector2D> getVerticesForName(int name) {
	    List<IVector2D> pts = new ArrayList<>();
	    for (int i=0; i<num_pts; i++) {
	        if (names[i] == name) {
	            pts.add(this.pts[i]);
            }
        }
        return pts;
    }
    
	/**
	 * 
	 * @param v
	 */
    public final void transformXY(MutableVector2D v) {
        transformXY(v.X(), v.Y(), v);
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
    	v.set(x,y);
        cur_mat.transform(v);
		min.minEq(v);
		max.maxEq(v);
        float W = window.getViewportWidth();
        float H = window.getViewportHeight();
		if (USE_PROJECTION_MATRIX) {
            proj_mat.transform(v);
            v.addEq(1, 1).scaleEq(W / 2, H / 2);
        } else {
            v.setX((v.X() - left) * (W / (right - left)));
            v.setY(H - ((v.Y() - bottom) * H / (top - bottom)));

        }

    }
    
    /**
     * Convert screen coods to viewport coords using projection only
     * @param screenX
     * @param screenY
     * @return
     */
	public MutableVector2D untransform(float screenX, float screenY) {
        float W = this.window.getViewportWidth();
        float H = this.window.getViewportHeight();

        if (USE_PROJECTION_MATRIX) {
            float Xd = (2f * screenX) / W - 1;
            float Yd = (2f * screenY) / H - 1;
            MutableVector2D v = new MutableVector2D(Xd, Yd);
            proj_mat.inverse().transform(v);
            cur_mat.inverse().transform(v);
            return v;
        }

        float x = left + screenX / (W / (right-left));
        float y = bottom + (H - screenY)/(H / (top-bottom));

        MutableVector2D v = new MutableVector2D(x, y);
        cur_mat.inverse().transform(v);

		return v;
	}

	/**
	 * 
	 * @param v
	 * @return
	 */
	public final MutableVector2D transformXY(IVector2D v) {
		transformXY(v.getX(), v.getY(), s_vec);
    	return new MutableVector2D(s_vec);
	}

	/**
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
    public final MutableVector2D transformXY(float x, float y) {
		transformXY(x, y, s_vec);
    	return new MutableVector2D(s_vec);
    }
    /**
     *
     * @param x
     * @param y
     * @param v
     */
    public final void transformXY(float x, float y, float[] v) {
        transformXY(x, y, s_vec);
        v[0] = s_vec.X();
        v[1] = s_vec.Y();
    }

    public final void setOrtho(IRectangle rect) {
        setOrtho(rect.X(), rect.X() + rect.getWidth(), rect.Y(), rect.Y() + rect.getHeight());
    }

    /**
     * @param left
     * @param right
     * @param top
     * @param bottom
     */
    public final void setOrtho(float left, float right, float top, float bottom) {
        float xo = 2 / (right - left);
        float yo = 2/(bottom - top);
        float tx = -(right + left) / (right -left);
        float ty = -(top + bottom) / (bottom-top);
        proj_mat.assign(xo, 0, tx,
                0, yo, ty,
                0, 0, 1);

        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }
        
	/**
	 * 
	 * @param t
	 */
	public final void addVertex(IVector2D t) {
		addVertex(t.getX(), t.getY());
	}

	/**
	 * 
	 * @param list a list of Transformable objects
	 */
	public final void addVertices(Iterable<IVector2D> list) {
		for (IVector2D v : list) {
			addVertex(v);
		}
	}
	
	/**
	 * 
	 * @param list
	 */
	public final void addVertices(Collection<IVector2D> list) {
		for (IVector2D v : list) {
			addVertex(v);
		}
		
	}

	/**
	 * 
	 * @param list
	 */
	public final void addVertices(IVector2D ... list) {
		for (IVector2D v : list) {
			addVertex(v);
		}
	}
	
	/**
	 * Push the matrix stack
	 */
	public final void pushMatrix() {
		if (m_stack_size < m_stack.length)
		{
			m_stack[m_stack_size].copy(m_stack[m_stack_size-1]);
			m_stack_proj[m_stack_size].copy(m_stack_proj[m_stack_size-1]);
			cur_mat = m_stack[m_stack_size];
			proj_mat = m_stack_proj[m_stack_size];
			m_stack_size++;
		} else
			throw new GException("Fixed stack size of [" + m_stack.length + "] for pushMatrix()");
	}

	/**
	 * pop off the matrix stack
	 */
	public final void popMatrix() {
		if (m_stack_size > 1) {
		    m_stack_size -= 1;
            cur_mat = m_stack[m_stack_size-1];
            proj_mat = m_stack_proj[m_stack_size-1];
		}
		else
			throw new GException("too many pops");
	}

    /**
	 * 
	 * @param name
	 */
	public final void setName(int name) {
		this.cur_name = name;
	}
	
	/**
	 * Return the top level matrix
	 * @return
	 */
	public final Matrix3x3 getCurrentTransform() {
		return new Matrix3x3(cur_mat);
	}
	
	/**
	 * 
	 * @return
	 */
	public final int getNumVerts() {
	    return num_pts;
	}
	
	/**
	 * 
	 * @param index
	 * @return
	 */
	public final float getX(int index) {
		return pts[index].X();
	}
	
	/**
	 * 
	 * @param index
	 * @return
	 */
	public final float getY(int index) {
		return pts[index].Y();
	}
	
	/**
	 * 
	 * @param index
	 * @return
	 */
	public final Vector2D getVertex(int index) {
		return pts[index];
	}
	
	/**
	 * 
	 * @param index
	 * @return
	 */
	public final int getName(int index) {
		return names[index];
	}
	
	/**
	 * 
	 */
    public final void clearVerts() {
        num_pts = 0;
    }

    /**
     * 
     * @param r
     */
    public final void setWindow(Renderable r) {
        this.window = r;
    }
    
    /**
     * Return the transformed vector that is minimum bounding rect point
     * @return
     */
    public final Vector2D getMin() {
    	return min;
    }
    
    /**
     * Return the transformed vector that is the maximum bounding rect point
     * @return
     */
    public final Vector2D getMax() {
    	return max;
    }

    /**
     * 
     */
    public final void clearBoundingRect() {
    	min.set(Float.MAX_VALUE, Float.MAX_VALUE);
    	max.set(-Float.MAX_VALUE, -Float.MAX_VALUE);
    }

    public int getStackSize() {
        return m_stack_size;
    }

	////////////////////////////////////////////////////
	// PRIVATE STUFF ///////////////////////////////////
	////////////////////////////////////////////////////
	
    private Renderable  window;
    private MutableVector2D s_vec = new MutableVector2D();
    private MutableVector2D min = new MutableVector2D();
    private MutableVector2D max = new MutableVector2D();
    private MutableVector2D [] pts;
    private int             [] names;
    private int			num_pts = 0; 
	private int			cur_name = 0;
	private Matrix3x3 []	m_stack;
	private Matrix3x3		cur_mat;
    private int			    m_stack_size;
	private Matrix3x3 []    m_stack_proj;
	private Matrix3x3       proj_mat;
	private Exception []    m_stack_trace;
	private Matrix3x3		s_mat; // working matrix
	private Matrix3x3		t_mat; // working matrix
	private float       left, right, top, bottom;
	
	private final int MAX_VERTS = 1024;
}
