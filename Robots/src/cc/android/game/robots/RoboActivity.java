package cc.android.game.robots;


import cc.lib.android.DPadView;
import cc.lib.game.Utils;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;


/**
 * Port of Java Applet
 * 
 * @author ccaron
 *
 */
public class RoboActivity extends Activity implements View.OnTouchListener {
    
    // ---------------------------------------------------------//
    // ANDROID
    // ---------------------------------------------------------//
    
    RoboView roboView;
    DPadView dpadLeft, dpadRight;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_BEHIND);
        this.setContentView(R.layout.roboview);
        roboView = (RoboView)this.findViewById(R.id.roboView1);
        dpadLeft = (DPadView)this.findViewById(R.id.dPadLeft);
        dpadRight = (DPadView)this.findViewById(R.id.dPadRight);
        // since we are using multi-touch we need to handle in just the single containing view
        this.findViewById(R.id.roboLayout).setOnTouchListener(this);
        //dpadLeft.setOnDpadListener(robotron);
        //dpadRight.setOnDpadListener(robotron);
        
    }

    public final static int MSG_SHOW_SPINNER = 0;
    public final static int MSG_HIDE_SPINNER = 1;
    
    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case MSG_SHOW_SPINNER:
                        showDialog(DIALOG_INDETERMINANT_SPINNER); break;
                    case MSG_HIDE_SPINNER:
                        dismissDialog(DIALOG_INDETERMINANT_SPINNER); break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    };

    public void showSpinner() {
        handler.sendEmptyMessage(MSG_SHOW_SPINNER);
    }
    
    public void hideSpinner() {
        handler.sendEmptyMessage(MSG_HIDE_SPINNER);
    }
    
    private final static int DIALOG_INDETERMINANT_SPINNER = 0;
    
    @Override
    protected Dialog onCreateDialog(int id) {
        try {
            if (id == DIALOG_INDETERMINANT_SPINNER) {
                Dialog dialog = new Dialog(this);
                dialog.setCancelable(false);
                dialog.setTitle("Loading ...");
                dialog.setContentView(R.layout.spinnerdialog);
                return dialog;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            roboView.roboPause();
            this.dismissDialog(DIALOG_INDETERMINANT_SPINNER);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        roboView.roboResume();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = (event.getAction() & MotionEvent.ACTION_MASK);
        //int pIndex = (event.getActionIndex() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        //Log.d("DPadView", "viewId=" + view.getId() + ", aIndex=" + event.getActionIndex() + ", pointerId=" + event.getPointerId(0));
        
        //Log.d("DPadView", "View=" + view.getId() + ", action=" + action + ", pCount=" + event.getPointerCount() + ", pIndex=" + pIndex);
        //Log.d("DPadView", "" + event.get
        
        Log.d("onTouch", "action=" + action + ", numPointers=" + event.getPointerCount());
        
        // find center x,y of the pointers
        
        // each pointer is a touch bu a unique finger
        
        for (int i=0; i<event.getPointerCount(); i++) {
            //Log.d("Pointer " + i, "id=" + event.getPointerId(i) + ", x=" + event.getX(i) + ", y=" + event.getY(i) );
            float x = event.getX(i);
            float y = event.getY(i);

            if (Utils.isPointInsideRect(x, y, dpadLeft.getLeft(), dpadRight.getTop(), dpadLeft.getWidth(), dpadLeft.getHeight())) {
                dpadLeft.doTouch(event, x - dpadLeft.getLeft() , y-dpadLeft.getTop());
                roboView.getRobotron().setMovement(dpadLeft.getDx(), dpadLeft.getDy());
            } else if (Utils.isPointInsideRect(x, y, dpadRight.getLeft(), dpadRight.getTop(), dpadRight.getWidth(), dpadRight.getHeight())) {
                dpadRight.doTouch(event, x - dpadRight.getLeft() , y-dpadRight.getTop());
                //if (event.getAction() == MotionEvent.)
                boolean firing = event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE; 
                roboView.getRobotron().setFiring(firing, dpadRight.getDx(), dpadRight.getDy());
            } else if (Utils.isPointInsideRect(x, y, roboView.getLeft(), roboView.getTop(), roboView.getWidth(), roboView.getHeight())) {
                roboView.doTouch(event, x - roboView.getLeft(), y - roboView.getTop());
            }
        }
        
        return true;
    }        

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("RoboCraze ported to Android by Chris Caron April 2012");
        menu.addSubMenu("RESET");
        menu.addSubMenu("OPTIONS");
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        roboView.roboPause();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        if (item.getTitle().equals("RESET")) {
            roboView.reset();
            roboView.roboResume();
        } else if (item.getTitle().equals("OPTIONS")) {
            //this.startActivity(new Intent(this, DebugPreferences.class));
        } else {
            roboView.roboResume();
        }
        // by default close the options menu
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        roboView.roboResume();
        // TODO Auto-generated method stub
        super.onOptionsMenuClosed(menu);
    }
    
    
    
	
} // end class SuperRobotron
