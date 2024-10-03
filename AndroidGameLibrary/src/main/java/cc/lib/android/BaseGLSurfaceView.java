package cc.lib.android;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

public abstract class BaseGLSurfaceView extends GLSurfaceView {

    protected abstract BaseRenderer newRenderer(); 
    
    private BaseRenderer renderer;
    //private boolean fullScreenMode = false;
    
    private void initView() {
        setRenderer(renderer = newRenderer());
        if (BuildConfig.DEBUG)
            setDebugFlags(DEBUG_CHECK_GL_ERROR);// | DEBUG_LOG_GL_CALLS);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        if (!(getContext() instanceof CCActivityBase))
            throw new RuntimeException("BaseGLSurfaceView have have BaseActivity as context");
    }
    
    public BaseGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public BaseGLSurfaceView(Context context) {
        super(context);
        initView();
    }

    @Override
    public final void surfaceDestroyed(SurfaceHolder holder) {
        if (renderer != null) {
            renderer.shutDown();
        }
        super.surfaceDestroyed(holder);
    }

    @Override
    protected final void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width =  MeasureSpec.getSize(widthMeasureSpec);
        final int height =  MeasureSpec.getSize(heightMeasureSpec);

        int dim = width > height ? height : width;
        int max = (width > height ? heightMeasureSpec : widthMeasureSpec);
        dim = roundUpToTile(dim, 1, max);
        setMeasuredDimension(dim, dim);
    }
    
    private int roundUpToTile(int dimension, int tileSize, int maxDimension) {
        return Math.min(((dimension + tileSize - 1) / tileSize) * tileSize, maxDimension);
    }    
    
    public final void pause() {
        if (renderer != null)
            renderer.setPaused(true);
    }
    
    public final void resume() {
        if (renderer != null)
            renderer.setPaused(false);
    }
    /*
    public void setFullScreenMode(boolean fullscreen) {
        this.fullScreenMode = fullscreen;
    }*/

    public CCActivityBase getActivity() {
        return (CCActivityBase)getContext();
    }
}
