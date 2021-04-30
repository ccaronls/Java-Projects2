package cc.game.zombicide.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;

public class Splash extends Activity implements Runnable {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        handler.postDelayed(this, BuildConfig.DEBUG ? 2000 : 5000);
    }

    Handler handler = new Handler();

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(this);
    }

    @Override
    public void run() {
        Intent intent = new Intent(this, ZombicideActivity.class);
        startActivity(intent);
        finish();
    }
}
