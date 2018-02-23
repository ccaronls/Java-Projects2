package cc.game.android.rocketman;

import android.opengl.GLSurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import cc.lib.android.BaseRenderer;
import cc.lib.android.GL10Graphics;
import cc.lib.android.SpringSlider;
import cc.lib.android.SpringSlider.OnSliderChangedListener;
import cc.lib.game.GColor;

public class RocketManRenderer extends BaseRenderer implements OnClickListener, OnSliderChangedListener {

    final String TAG = "RocketManRenderer";
    
    float center_pitch_angle = 0;
    float center_roll_angle = 0;
    
    float azimuth_angle = 0;
    float pitch_angle = 0;
    float roll_angle = 0;

    public RocketManRenderer(GLSurfaceView parent) {
        super(parent);
    }

    @Override
    protected void drawFrame(GL10Graphics g) {
        
        //Log.i(TAG, "viewport width/height = " + g.getViewportWidth() + "/" + g.getViewportHeight());
        
        final float roll = center_roll_angle - roll_angle;
        final float pitch = center_pitch_angle - pitch_angle;

        //Log.i(TAG, "pitch/roll = " + pitch + "/" + roll);

        g.setIdentity();
        g.ortho(-100, 100, 100, -100);

        g.clearScreen(GColor.GRAY);
        g.setColor(GColor.BLUE.withAlpha(0.3f));
        g.begin();
        g.vertex(-10, 0);
        g.vertex(10, 0);
        g.vertex(-10, roll);
        g.vertex(10, roll);
        g.drawTriangleStrip();
        
        g.vertex(pitch, 10);
        g.vertex(pitch, -10);
        g.vertex(0, 10);
        g.vertex(0, -10);
        g.drawTriangleStrip();
        
    }

    @Override
    protected void init(GL10Graphics g) {
        g.setTextHeight(48);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonCenter:
                // center the phone so that pitch/roll are zero at current orientation
                center_roll_angle = roll_angle;
                center_pitch_angle = pitch_angle;
                break;
        }
    }

    @Override
    public void sliderMoved(SpringSlider slider, float position) {
        // Called when user moves the 'Thrust' slider, as well as when slider is springing back to 0
        
    }

}
