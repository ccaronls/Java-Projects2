package cc.game.android.rocketman;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

public class RocketManView extends GLSurfaceView implements SensorEventListener {

    final String TAG = "RocketManView";
    
    RocketManRenderer renderer;
    
    public RocketManView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            renderer = new RocketManRenderer(this);
            this.setRenderer(renderer);
            renderer.setTargetFPS(30);
            renderer.setDrawFPS(true);
        }
    }

    public RocketManView(Context context) {
        super(context);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
        
    }



    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.i(TAG, "onSensorChanged");
        renderer.azimuth_angle = event.values[0];
        renderer.pitch_angle = event.values[1];
        renderer.roll_angle = event.values[2];
    }
    
}
