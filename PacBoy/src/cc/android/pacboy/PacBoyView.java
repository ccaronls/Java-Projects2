package cc.android.pacboy;

import cc.lib.game.Utils;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

public class PacBoyView extends GLSurfaceView {

	private PacBoyRenderer pb;

	private void initView() {
		if (!isInEditMode()) {
			setRenderer(pb = new PacBoyRenderer(this));
			if (Utils.DEBUG_ENABLED)
                setDebugFlags(DEBUG_CHECK_GL_ERROR);// | DEBUG_LOG_GL_CALLS);
            setRenderMode(RENDERMODE_WHEN_DIRTY);
            setOnTouchListener(pb);
		}
	}
	
	public PacBoyView(Context context) {
		super(context);
		initView();
	}

	public PacBoyView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}
	
	@Override
    public void surfaceDestroyed(SurfaceHolder holder) {
		if (pb != null) {
			pb.shutDown();
			pb = null;
		}
	}
	
	public void setPaused(boolean paused) {
		pb.setPaused(paused);
    }

	public void initMaze(int width, int height, int difficulty) {
		pb.newMaze(width, height, difficulty);
	}
	
	public void initIntro() {
		pb.setupIntro();
	}

	public int getScore() {
		return pb.getScore();
	}
	
	@Override
	public boolean performClick() {
		return super.performClick();
	}
	
	public int getDifficulty() {
		return pb.getDifficulty();
	}
	
}
