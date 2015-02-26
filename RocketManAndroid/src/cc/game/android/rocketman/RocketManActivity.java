package cc.game.android.rocketman;

import cc.lib.android.SpringSlider;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;
import android.widget.TextView;

public class RocketManActivity extends Activity {

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private RocketManView rocketManView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rocket_man);
        final SpringSlider thrust = (SpringSlider)findViewById(R.id.springSliderThrust);
        rocketManView = (RocketManView)findViewById(R.id.rocketManView1);
        thrust.setOnSliderChangedListener(rocketManView.renderer);
        findViewById(R.id.buttonCenter).setOnClickListener(rocketManView.renderer);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    }

    
    
    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }



    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        mSensorManager.unregisterListener(rocketManView, mSensor);
        rocketManView.renderer.setPaused(true);
    }



    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(rocketManView, mSensor, SensorManager.SENSOR_DELAY_GAME);
        rocketManView.renderer.setPaused(false);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.rocket_man, menu);
        return true;
    }
}
