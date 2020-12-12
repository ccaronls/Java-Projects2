package cc.android.game.robots;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import cc.lib.android.BaseRenderer;
import cc.lib.android.GL10Graphics;

public class RoboView extends GLSurfaceView { 

    private final static boolean LOGGING_ENABLED = true;
    
    private final static String TAG = "RoboView";
    
    private RoboRenderer roboRenderer;

    private Robotron robotron;

    public Robotron getRobotron() {
        return robotron;
    }
    
    private void initView() {
    	if (!isInEditMode())
    		robotron = new Robotron();
        setRenderer(roboRenderer = new RoboRenderer());
        if (BuildConfig.DEBUG)
            setDebugFlags(DEBUG_CHECK_GL_ERROR);// | DEBUG_LOG_GL_CALLS);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        //this.setOnTouchListener(this);
    }
    
    public RoboView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public RoboView(Context context) {
        super(context);
        initView();
    }
    
    public void doTouch(MotionEvent event, float x, float y) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_CANCEL:
                robotron.onTouch(Robotron.TouchEvent.TOUCH_CANCEL, x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                robotron.onTouch(Robotron.TouchEvent.TOUCH_DRAG, x, y);
                break;
            case MotionEvent.ACTION_UP:
                robotron.onTouch(Robotron.TouchEvent.TOUCH_UP, x, y);
                break;
            case MotionEvent.ACTION_DOWN:
                robotron.onTouch(Robotron.TouchEvent.TOUCH_DOWN, x, y);
                break;
        }
        
    }

    public void roboPause() {
        roboRenderer.setPaused(true);
    }
    
    public void roboResume() {
        Log.d(TAG, "Resume");
        roboRenderer.setPaused(false);
    }

    private long frameCounterTime = 0;
    private int  frameCounter = 0;
    
    private final int FPS = 30;
    
    public void reset() {
        robotron.processEvent(Robotron.RoboEvent.GAME_RESET);
    }
    
    
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (roboRenderer != null) {
            roboRenderer.shutDown();
        }
    }

/*    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width =  MeasureSpec.getSize(widthMeasureSpec);
        final int height =  MeasureSpec.getSize(heightMeasureSpec);

        int dim = width > height ? height : width;
        int max = (width > height ? heightMeasureSpec : widthMeasureSpec);
        dim = roundUpToTile(dim, 1, max);
        setMeasuredDimension(dim, dim);
    }
  */  
    private int roundUpToTile(int dimension, int tileSize, int maxDimension) {
        return Math.min(((dimension + tileSize - 1) / tileSize) * tileSize, maxDimension);
    }    

    private RoboActivity getActivity() {
        return (RoboActivity)getContext();
    }
    
    private class RoboRenderer extends BaseRenderer {

        
        public RoboRenderer() {
            super(RoboView.this);
        }

        @Override
        public void drawFrame(GL10Graphics g) {
            robotron.drawGame(g);
        }

        @Override
        public void init(final GL10Graphics g) {
            int w = g.getViewportWidth();
            int h = g.getViewportHeight();
            robotron.setDimension(w, h);
            /*
            SharedPreferences sp = getContext().getSharedPreferences(DebugPreferences.DEBUG_PREFERENCES_NAME, Context.MODE_PRIVATE);
            boolean debugBarrier = sp.getBoolean(DebugPreferences.DEBUG_PREF_BOOL_BARRIER, false);
            boolean debugGhost   = sp.getBoolean(DebugPreferences.DEBUG_PREF_BOOL_GHOST, false);
            boolean debugHulk    = sp.getBoolean(DebugPreferences.DEBUG_PREF_BOOL_HULK, false);
            boolean debugInvincible = sp.getBoolean(DebugPreferences.DEBUG_PREF_BOOL_INVINCIBLE, false);
            boolean debugShowEnemyInfo = sp.getBoolean(DebugPreferences.DEBUG_PREF_BOOL_SHOW_ENEMY_INFO, false);
            boolean debugShowMazeInfo = sp.getBoolean(DebugPreferences.DEBUG_PREF_BOOL_SHOW_MAZE_INFO, false);
            boolean debugShowPlayerInfo = sp.getBoolean(DebugPreferences.DEBUG_PREF_BOOL_SHOW_PLAYER_INFO, false);
            robotron.setDebugEnabled(Robotron.Debug.BARRIER, debugBarrier);
            robotron.setDebugEnabled(Robotron.Debug.GHOST, debugGhost);
            robotron.setDebugEnabled(Robotron.Debug.HULK, debugHulk);
            robotron.setDebugEnabled(Robotron.Debug.INVINCIBLE, debugInvincible);
            robotron.setDebugEnabled(Robotron.Debug.DRAW_ENEMY_INFO, debugShowEnemyInfo);
            robotron.setDebugEnabled(Robotron.Debug.DRAW_MAZE_INFO, debugShowMazeInfo);
            robotron.setDebugEnabled(Robotron.Debug.DRAW_PLAYER_INFO, debugShowPlayerInfo);
            */
            robotron.initGame(g);
            g.ortho(-w/2, w/2, -h/2, h/2);
            setTargetFPS(FPS);
            /*
            final RoboActivity activity = (RoboActivity)getActivity();
            activity.showSpinner();
            new Thread(new Runnable() {
                public void run() {
                    robotron.initGame(g);
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            activity.hideSpinner();
                        }
                    });
                }
            }).start();*/
        }
        
    }
    
    
}
