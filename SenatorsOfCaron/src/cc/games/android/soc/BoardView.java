package cc.games.android.soc;

import cc.lib.game.Utils;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

public class BoardView extends GLSurfaceView {

	BoardRenderer br;
	
	private void initView(Context context, AttributeSet attrs) {
		if (!isInEditMode()) {
			setRenderer(br = new BoardRenderer(this));
			if (Utils.DEBUG_ENABLED)
                setDebugFlags(DEBUG_CHECK_GL_ERROR);// | DEBUG_LOG_GL_CALLS);
            setRenderMode(RENDERMODE_WHEN_DIRTY);
		}
	}
	
	public BoardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context, attrs);
	}

	public BoardView(Context context) {
		super(context);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		super.surfaceCreated(holder);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (br != null) {
			br.shutDown();
			br = null;
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// TODO Auto-generated method stub
		super.surfaceChanged(holder, format, w, h);
	}

	
	
}
