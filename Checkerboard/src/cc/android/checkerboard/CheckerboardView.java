package cc.android.checkerboard;

import cc.lib.game.Utils;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

public class CheckerboardView extends GLSurfaceView {

	private CheckerboardRenderer cbRenderer;

	private void initView() {
		if (!isInEditMode()) {
			setRenderer(cbRenderer = new CheckerboardRenderer(this));
			if (Utils.DEBUG_ENABLED)
                setDebugFlags(DEBUG_CHECK_GL_ERROR);// | DEBUG_LOG_GL_CALLS);
            setRenderMode(RENDERMODE_WHEN_DIRTY);
            setOnTouchListener(cbRenderer);
		}
	}
	
	public CheckerboardView(Context context) {
		super(context);
		initView();
	}

	public CheckerboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}
	
	@Override
    public void surfaceDestroyed(SurfaceHolder holder) {
		if (cbRenderer != null) {
			cbRenderer.shutDown();
			cbRenderer = null;
		}
	}
	
	public void setPaused(boolean paused) {
		cbRenderer.setPaused(paused);
		Log.d("CBV", "W=" + getWidth() + " x H=" + getHeight());
    }

	public void initIntro() {
		// TODO Auto-generated method stub
		
	}

	
}
