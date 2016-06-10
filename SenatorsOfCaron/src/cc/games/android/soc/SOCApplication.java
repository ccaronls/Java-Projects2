package cc.games.android.soc;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.games.Games;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class SOCApplication extends Application implements ConnectionCallbacks, OnConnectionFailedListener {

	private GoogleApiClient mGAPI;
	private SharedPreferences prefs;
	
	@Override
	public void onCreate() {
		super.onCreate();
		application = this;
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getBoolean(AUTO_GOOGLE_SIGNIN, false))
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
		prefs.edit().putBoolean(AUTO_GOOGLE_SIGNIN, false).apply();
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		prefs.edit().putBoolean(AUTO_GOOGLE_SIGNIN, true).apply();
	}

	@Override
	public void onConnectionSuspended(int cause) {
		// TODO Auto-generated method stub
		
	}
	
	private static final String AUTO_GOOGLE_SIGNIN = "AUTO_GOOGLE_SIGNIN";
}
