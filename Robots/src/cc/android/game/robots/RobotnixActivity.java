package cc.android.game.robots;

import android.os.Bundle;
import android.view.View;

import cc.lib.android.CCActivityBase;

/**
 * Created by chriscaron on 10/19/17.
 */

public class RobotnixActivity extends CCActivityBase implements View.OnClickListener {

    RobotnixView robo;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.robotnix_activity);
        robo = (RobotnixView)findViewById(R.id.vRobotnix);
        findViewById(R.id.bClear).setOnClickListener(this);
        findViewById(R.id.bUp).setOnClickListener(this);
        findViewById(R.id.bDown).setOnClickListener(this);
        findViewById(R.id.bLeft).setOnClickListener(this);
        findViewById(R.id.bRight).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onPoll() {
        super.onPoll();
    }

    @Override
    public void onClick(final View v) {
        final float [] deltaPerSec = new float[2];
        float step = 30;
        switch (v.getId()) {
            case R.id.bClear:
                robo.arm.clear();
                robo.tappedIndex=robo.draggingIndex=-1;
                robo.invalidate();
                return;
            case R.id.bUp:
                deltaPerSec[1] = -step;
                break;
            case R.id.bDown:
                deltaPerSec[1] = step;
                break;
            case R.id.bLeft:
                deltaPerSec[0] = -step;
                break;
            case R.id.bRight:
                deltaPerSec[0] = step;
                break;
            default:
                return;
        }

        if (robo.tappedIndex >= 0) {
            robo.arm.moveHinge(robo.tappedIndex, deltaPerSec[0], deltaPerSec[1]);
            robo.invalidate();
        }
    }
}
