package cc.android.checkerboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

import cc.lib.game.Utils;

/**
 * This handles events form system like user input, pause, resume etc.
 * 
 * @author chriscaron
 *
 */
public class CheckerboardView extends GLSurfaceView {

	private final static String TAG = "CheckerboardView";

	CheckerboardRenderer cbRenderer;

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
    public void onPause() {
        super.onPause();
        if (cbRenderer != null)
            cbRenderer.setPaused(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (cbRenderer != null)
            cbRenderer.setPaused(false);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        super.surfaceChanged(holder, format, w, h);
    }
}
