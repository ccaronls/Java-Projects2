package cc.android.checkerboard;

import java.util.ArrayList;
import java.util.List;

import cc.android.checkerboard.CheckerboardRenderer.JumpAnim;
import cc.android.checkerboard.CheckerboardRenderer.SlideAnim;
import cc.android.checkerboard.CheckerboardRenderer.StackAnim;
import cc.android.checkerboard.ICheckerboard.Move;
import cc.android.checkerboard.ICheckerboard.Piece;
import cc.lib.game.Utils;
import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

/**
 * This handles events form system like user input, pause, resume etc.
 * 
 * @author chriscaron
 *
 */
public class CheckerboardView extends GLSurfaceView implements View.OnTouchListener, GestureDetector.OnGestureListener {

	private final static String TAG = "CheckerboardView";

	GestureDetector gestures;
	CheckerboardRenderer cbRenderer;
	float touchX = -1, touchY = -1;
	float grabRank = -1, grabColumn = -1;
	long downTime = 0;

	private void initView() {
		if (!isInEditMode()) {
			setRenderer(cbRenderer = new CheckerboardRenderer(this));
			if (Utils.DEBUG_ENABLED)
                setDebugFlags(DEBUG_CHECK_GL_ERROR);// | DEBUG_LOG_GL_CALLS);
            setRenderMode(RENDERMODE_WHEN_DIRTY);
            //gestures = new GestureDetector(getContext(), this, getHandler());
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

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				downTime = System.currentTimeMillis();
				touchX = event.getX();
				touchY = event.getY();
				break;
			case MotionEvent.ACTION_UP:
				downTime = 0;
				touchX = touchY = -1;
				break;
			case MotionEvent.ACTION_MOVE: // should be called drag
				touchX = event.getX();
				touchY = event.getY();
				break;
		}
		return gestures.onTouchEvent(event);
	}
	
	/*
		if (blockingAnimations.size() > 0)
			return false;
		
		ICheckerboard checkers = getGame();
		if (checkers == null)
			return false;
		
		synchronized (nonBlockingAnimations) {
			for (ACBAnimation a: nonBlockingAnimations)
				a.stop();
			nonBlockingAnimations.clear();
		}
		
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				downTime = System.currentTimeMillis();
				touchX = event.getX();
				touchY = event.getY();
				touchRank = (int)(touchY * 8 / getGraphics().getViewportHeight());
				touchColumn = (int)(touchX * 8 / getGraphics().getViewportWidth());
				break;
			case MotionEvent.ACTION_MOVE: {
				touchX = event.getX();
				touchY = event.getY();
//				touchRank = (int)(touchY * 8 / getGraphics().getViewportHeight());
//				touchColumn = (int)(touchX * 8 / getGraphics().getViewportWidth());
				break;
			}
			case MotionEvent.ACTION_UP:
				if (System.currentTimeMillis() - downTime < 500) {
					onTap();
				}
				touchX = touchY = -1;
				break;
		}
		
		return true;
	}
	
	void onTap() {
		ICheckerboard checkers = getGame();
		if (checkers.isOnBoard(touchColumn, touchColumn)) {
			if (moves.size() == 0)
				moves = checkers.computeMoves(touchRank, touchColumn);
			else {
				for (Move m : moves) {
					int tr = m.endRank;
					int tc = m.endCol;
					if (tr == touchRank && tc == touchColumn) {
						switch (m.type) {
							case JUMP:
							case JUMP_CAPTURE:
								addAnimation(true, new JumpAnim(1000, 0, m));
								break;
							case SLIDE:
								addAnimation(true, new SlideAnim(500, 0, m));
								break;
							case STACK:
								addAnimation(true, new StackAnim(800, 0, m));
								break;
						}
						return;
					}
				}
				moves.clear();
			}
		}
	}*/

	private ICheckerboard getGame() {
		return CheckerboardActivity.game;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		/*
		Log.i(TAG, "onFling vX=" + velocityX + " vy=" + velocityY);
		
		try {
    		int sRank = Math.round(e1.getY() / cbRenderer.glCellHeight);
    		int sCol = Math.round(e1.getX() / cbRenderer.glCellWidth);
    		
    		Piece p = getGame().getPiece(sRank, sCol);
    		List<Move> moves = getGame().computeMoves(sRank, sCol);

    		int eRank = Math.round(e2.getY() / cbRenderer.glCellHeight);
    		int eCol = Math.round(e2.getX() / cbRenderer.glCellWidth);

    		for (Move m : moves) {
    			if (eRank == m.endRank && eCol == m.endCol) {
    				
    			}
    		}
    		
    		if (moves.size() > 0) {
        		int eRank = Math.round(e2.getY() / cbRenderer.glCellHeight);
        		int eCol = Math.round(e2.getX() / cbRenderer.glCellWidth);
        		
        		for ()
    		}
    		
    		
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		// TODO Auto-generated method stub
		return false;
	}

	
}
