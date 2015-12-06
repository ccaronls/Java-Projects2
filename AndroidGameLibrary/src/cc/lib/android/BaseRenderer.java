package cc.lib.android;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import cc.lib.game.AColor;
import cc.lib.game.Justify;
import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;

public abstract class BaseRenderer implements GLSurfaceView.Renderer {

	//DisplayMetrics metrics = new DisplayMetrics();

	public BaseRenderer(GLSurfaceView parent) {
        this.parent = parent;
        //WindowManager mgr = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
        //mgr.getDefaultDisplay().getMetrics(metrics);
    }
    
    protected abstract void drawFrame(GL10Graphics g);
    
    protected abstract void init(GL10Graphics g);

    private final AColor BLK = new GLColor(0,0,0,0.5f);

    @Override
    public final void onDrawFrame(GL10 arg0) {
    	if (g == null)
    		return;
    	synchronized (this) {
            final long enterTime = getSystemClock();
            try {
                g.beginScene();
    
                try {
                    drawFrame(g);
                } catch (Exception e) {
                    e.printStackTrace();
                    setPaused(true);
                }
                
                
                if (drawFPS && fps > 0) {
                    g.setIdentity();
                    g.ortho();
                    String fpsStr = String.valueOf(fps);
                    int txtHeight = 32;//(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, metrics);
                    g.setTextHeight(txtHeight);
                    float wid = g.getTextWidth(fpsStr);
                    float x = g.getViewportWidth()-5;
                    float y = 5;
                    AColor c = g.getColor();
                    g.setColor(BLK);
                    g.drawFilledRectf(x-wid-2, y-2, wid+4, 4f + g.getTextHeight());
                    g.setColor(g.RED);
                    g.drawJustifiedString(x, y, Justify.RIGHT, Justify.TOP, fpsStr);
                    g.setColor(c);
                }
                
                g.endScene();
                long delta = getSystemClock() - enterTime;
                if (targetFPS > 0) {
                    int targetDelta = 1000 / targetFPS;
                    if (!paused) {
                        if (delta < targetDelta) {
                            synchronized (this) {
                            	long dt = targetDelta - delta;
                            	if (dt > 500)
                            		dt = 500;
                            	if (dt > 0)
                            		wait(dt);
                            }
                        }
                    	repaint();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                setPaused(true);
            }
            
            long exitTime = getSystemClock();
            
            frameCounter++;
            frameCounterTime += (exitTime - enterTime);
            
            if (frameCounterTime >= 1000) {
                fps = Math.round(frameCounter * 1000 / frameCounterTime);
                //Log.i("FPS", String.valueOf(fps));
                frameCounterTime = 0;
                frameCounter = 0;
            }        
    	}
    }

    @Override
    public final void onSurfaceChanged(GL10 gl, int w, int h) {
        try {
        	if (gl instanceof GL11) {
                g = new GL11Graphics((GL11)gl, parent.getContext());
        	} else {
                g = new GL10Graphics(gl, parent.getContext());
        	}
            g.initViewport(w, h);
            init(g);
        } catch (Exception e) {
            e.printStackTrace();
            setPaused(true);
        }
    }

    @Override
    public final void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glEnable(GL10.GL_ALPHA_TEST);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
        if (g != null)
            g.shutDown();
        g = null;
    }
    
    /**
     * Set the target fps.  If <= 0 then no refresh is done.
     * default is 30 FPS
     * @param targetFPS
     */
    public final void setTargetFPS(int targetFPS) {
    	if (targetFPS != this.targetFPS) {
            this.targetFPS = targetFPS;
            repaint();
    	}
    }
    
    public final int getTargetFPS() {
    	return this.targetFPS;
    }
    
    public final int getFPS() {
        return this.fps;
    }
    
    public final void setDrawFPS(boolean draw) {
        this.drawFPS = draw;
    }
    
    public final void setPaused(boolean paused) {
    	if (this.paused != paused) {
            this.paused = paused;
            if (!paused)
                repaint();
    	}
    }
    
    public final boolean isPaused() {
        return paused;
    }
    
    public final void repaint() {
        parent.requestRender();
    }
    
    public final long getSystemClock() {
        return SystemClock.uptimeMillis();
    }
    
    public final GLSurfaceView getParent() {
        return parent;
    }
    
    public final Activity getActivity() {
        return (Activity)parent.getContext();
    }
    
    public final void shutDown() {
    	synchronized (this) {
            setPaused(true);
            if (g != null) {
                g.shutDown();
                g = null;
            }
    	}
    }
    
    public final Context getContext() {
        return parent.getContext();
    }
    
    public final int getWidth() {
    	return g.getViewportWidth();
    }
    
    public final int getHeight() {
    	return g.getViewportHeight();
    }
    
    public final GL10Graphics getGraphics() {
    	return g;
    }
    
    private GL10Graphics g;
    private final GLSurfaceView parent;
    private boolean drawFPS = true;
    private int targetFPS = 30;
    private long frameCounterTime = 0;
    private int  frameCounter = 0;
    private int fps = 0;
    private boolean paused = false;
}
