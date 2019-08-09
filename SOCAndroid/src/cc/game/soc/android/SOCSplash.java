package cc.game.soc.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SOCSplash extends Activity {

    long startTime = System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startTime = System.currentTimeMillis();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        int splashTimeMSecs = 5000;

        int timeShown = (int)(System.currentTimeMillis()-startTime);

        final int timeLeft = splashTimeMSecs - timeShown;
        final Intent i = new Intent(this, SOCActivity.class);
        if (timeLeft <= 0) {
            startActivity(i);
            finish();
        }
        else
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        synchronized (this) {
                            wait(timeLeft);
                        }
                        runOnUiThread(() -> {
                                startActivity(i);
                                finish();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();

                    }
                }
            }).start();

    }
}
