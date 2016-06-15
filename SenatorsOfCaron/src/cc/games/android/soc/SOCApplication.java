package cc.games.android.soc;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;

import android.app.AlertDialog;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class SOCApplication extends Application implements ConnectionCallbacks, OnConnectionFailedListener {

	private GoogleApiClient mGAPI;
	private SharedPreferences prefs;
	
	@Override
	public void onCreate() {
		super.onCreate();
		application = this;
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getBoolean(PREF_AUTO_GOOGLE_SIGNIN, false))
			signInGooglePlay();
	}

	@Override
	public void onTerminate() {
		// TODO Auto-generated method stub
		super.onTerminate();
	}

	@Override
	public void onLowMemory() {
		// TODO Auto-generated method stub
		super.onLowMemory();
	}

	private static SOCApplication application;
	
	public static SOCApplication getApp() {
		return application;
	}
	
	public void signInGooglePlay() {
		mGAPI = new GoogleApiClient.Builder(this)
    		.addConnectionCallbacks(this)
    		.addOnConnectionFailedListener(this)
    		.addApi(Games.API)
    		.addScope(Games.SCOPE_GAMES)
    		.build();
	}
	
	public void signOutGooglePlay() {
		mGAPI.disconnect();
		mGAPI = null;
		prefs.edit().putBoolean(PREF_AUTO_GOOGLE_SIGNIN, false).apply();
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// TODO Auto-generated method stub
		Toast.makeText(this, R.string.toast_connection_to_google_play_failed, Toast.LENGTH_LONG).show();
		Log.i(TAG, "Connection to Google Play Failed: " + result);
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		prefs.edit().putBoolean(PREF_AUTO_GOOGLE_SIGNIN, true).apply();
		Toast.makeText(this, R.string.toast_connected_to_google_play, Toast.LENGTH_LONG).show();
	}

	@Override
	public void onConnectionSuspended(int cause) {
		// TODO Auto-generated method stub
		Toast.makeText(this, R.string.toast_connection_to_google_play_suspended, Toast.LENGTH_LONG).show();
		Log.i(TAG, "Connection to Google Play Suspended: " + cause);
	}
	
	public 
	
	private final static String TAG = SOCApplication.class.getSimpleName();
	
	private final static String PREF_AUTO_GOOGLE_SIGNIN = "AUTO_GOOGLE_SIGNIN";
}
