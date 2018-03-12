package cc.lib.android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

/**
 * Base class has support for inApp billing, polling and various helper methods
 *  
 *  
 * @author ccaron
 *
 */
public class CCActivityBase extends Activity {

    static {
        LoggerFactory.factory = new LoggerFactory() {
            @Override
            public Logger getLogger(String name) {
                return new AndroidLogger(name);
            }
        };
    }

    public final Logger log = new AndroidLogger(getClass().getSimpleName());

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		stopPolling();
	}
	
	// ************ HELPERS ****************
	
	public final SharedPreferences getPrefs() {
		return PreferenceManager.getDefaultSharedPreferences(this);
	}

	public final boolean isPortrait() {
		return getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
	}
	
	public final void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            View focused = getContent().findFocus();
            if (focused != null) {
                imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
            }
        }
    }
	
	public View getContent() {
		return getWindow().getDecorView().findViewById(android.R.id.content);
	}
	
	// ************ POLLING ****************
	
	private Timer pollingTimer = null;
	
	/**
	 * Start polling.  onPoll will be called on UI thread at regular intervals until the
	 * activity is paused or user calls stopPolling.
	 * 
	 * @param intervalSeconds
	 */
	protected final void startPolling(int intervalSeconds) {
		if (pollingTimer == null) {
			pollingTimer = new Timer();
			pollingTimer.schedule(new TimerTask() {
				
				@Override
				public void run() {
					runOnUiThread(pollRunnable);
				}
			}, intervalSeconds * 1000, intervalSeconds * 1000);
		}
	}
	
	/**
	 * This is visible because there are cases when we want to stop polling even when not paused.
	 */
	protected final void stopPolling() {
		if (pollingTimer != null) {
			pollingTimer.cancel();
			pollingTimer = null;
		}
	}
	
	private Runnable pollRunnable = new Runnable() {
		public void run() {
			onPoll();
		}
	};
	
	/**
	 * Override this method to handle your polling needs.  Base method just logs to LogCat a warning.
	 */
	protected void onPoll() {
		log.warn("onPoll not handled");
	}

	/**
	 * Convenience to get the users currently configured locale
	 * @return
	 */
	public final Locale getLocale() {
		return getResources().getConfiguration().locale;
	}
}
