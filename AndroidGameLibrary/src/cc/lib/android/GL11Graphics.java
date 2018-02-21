package cc.lib.android;

import javax.microedition.khronos.opengles.*;

import cc.lib.math.Matrix3x3;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import android.content.Context;
import android.opengl.GLU;

public final class GL11Graphics extends GL10Graphics {

	public GL11Graphics(GL11 gl, Context context) {
		super(gl, context);
	}

	public final GL11 getGl11() {
		return (GL11)getGl10();
	}
	
	@Override
	public final Vector2D screenToViewport(int screenX, int screenY) {
		float [] model = new float[16];
		getGl11().glGetFloatv(GL11.GL_MODELVIEW_MATRIX, model, 0);
		float [] project = new float[16];
		getGl11().glGetFloatv(GL11.GL_PROJECTION_MATRIX, project, 0);
		int [] view = new int[4];
		getGl11().glGetIntegerv(GL11.GL_VIEWPORT, view, 0);
		float [] obj = new float[3];
		GLU.gluUnProject(screenX, screenY, 0, model, 0, project, 0, view, 0, obj, 0);
		return new Vector2D(obj[0], obj[1]);
	}

	private float [] t_modelViewMatrix = new float[16];
	private float [] t_vertex = new float[4];
	
	private final float [] getTVertex(float x, float y, float z, float w) {
		t_vertex[0] = x;
		t_vertex[1] = y;
		t_vertex[2] = z;
		t_vertex[3] = w;
		return t_vertex;
	}
	
	@Override
	public void vertex(float x, float y) {
		super.vertex(x, y);
		getGl11().glGetFloatv(GL11.GL_MODELVIEW_MATRIX, t_modelViewMatrix, 0);
		DroidUtils.multiply(t_modelViewMatrix, getTVertex(x, y, 0, 0));
		Vector2D V = new Vector2D(t_vertex[0]/t_vertex[3], t_vertex[1]/t_vertex[3]);
		minRect.minEq(V);
		maxRect.maxEq(V);
	}
	
	/*
	public void project(float x, float y, float z, float[] v) {
		float [] model = new float[16];
		getGl11().glGetFloatv(GL11.GL_MODELVIEW_MATRIX, model, 0);
		float [] project = new float[16];
		getGl11().glGetFloatv(GL11.GL_PROJECTION_MATRIX, project, 0);
		int [] view = new int[4];
		getGl11().glGetIntegerv(GL11.GL_VIEWPORT, view, 0);
		GLU.gluProject(x, y, 0, model, 0, project, 0, view, 0, v, 0);
	}*/
	
	

	private final MutableVector2D minRect = new MutableVector2D(-Float.MAX_VALUE, -Float.MAX_VALUE);  
	private final MutableVector2D maxRect = new MutableVector2D(Float.MAX_VALUE, Float.MAX_VALUE);  
	
	@Override
	public void clearMinMax() {
		minRect.set(-Float.MAX_VALUE, -Float.MAX_VALUE);
		maxRect.set(Float.MAX_VALUE, Float.MAX_VALUE);
	}

	@Override
	public Vector2D getMinBoundingRect() {
		return minRect;
	}

	@Override
	public Vector2D getMaxBoundingRect() {
		// TODO Auto-generated method stub
		return maxRect;
	}

    @Override
    public void getTransform(Matrix3x3 result) {
	    float [] array = new float[16];
        getGl11().glGetFloatv(GL11.GL_MODELVIEW_MATRIX, array, 0);
        result.assign(array[0], array[4], array[8],
                array[1], array[5], array[9],
                array[2], array[6], array[1]);
    }

}
