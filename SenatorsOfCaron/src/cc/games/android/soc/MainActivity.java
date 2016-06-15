package cc.games.android.soc;

import java.util.ArrayList;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer.InitiateMatchResult;

import cc.lib.android.CCActivityBase;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements ConnectionCallbacks, OnConnectionFailedListener, ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>, RoomUpdateListener {

	public final String TAG = getClass().getSimpleName();
	
	private GoogleApiClient mGAPI;
	private final int ACTIVITY_RESULT_CODE_SELECT_PLAYERS = 5674;
	
	private final static String PREF_AUTO_GOOGLE_SIGNIN = "AUTO_GOOGLE_SIGNIN";

	@Override
	protected void onCreate(Bundle bundle) {
		// TODO Auto-generated method stub
		super.onCreate(bundle);
		if (getPrefs().getBoolean(PREF_AUTO_GOOGLE_SIGNIN, false))
			signInGooglePlay();
		setContentView(R.layout.main);
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction t = fm.beginTransaction();
		t.replace(R.id.fragment, new HomeScreen());
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
        RoomConfig.Builder config = RoomConfig.builder(this);

	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
    public void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);

        if (request == ACTIVITY_RESULT_CODE_SELECT_PLAYERS) {
            if (response != Activity.RESULT_OK) {
                // user canceled
                return;
            }

            // Get the invitee list.
            final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
            
            Log.d(TAG, "invitee list: " + invitees);

            // Get auto-match criteria.
            Bundle autoMatchCriteria = null;
            int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
            int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
            if (minAutoMatchPlayers > 0) {
                autoMatchCriteria = RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers, maxAutoMatchPlayers, 0);
            } else {
                autoMatchCriteria = null;
            }

            TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
                    .addInvitedPlayers(invitees)
                    .setAutoMatchCriteria(autoMatchCriteria)
                    .build();

            // Create and start the match.
            Games.TurnBasedMultiplayer
                .createMatch(mGAPI, tbmc)
                .setResultCallback(this);
        }
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
		getPrefs().edit().putBoolean(PREF_AUTO_GOOGLE_SIGNIN, false).apply();
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// TODO Auto-generated method stub
		Toast.makeText(this, R.string.toast_connection_to_google_play_failed, Toast.LENGTH_LONG).show();
		Log.i(TAG, "Connection to Google Play Failed: " + result);
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		getPrefs().edit().putBoolean(PREF_AUTO_GOOGLE_SIGNIN, true).apply();
		Toast.makeText(this, R.string.toast_connected_to_google_play, Toast.LENGTH_LONG).show();
	}

	@Override
	public void onConnectionSuspended(int cause) {
		// TODO Auto-generated method stub
		Toast.makeText(this, R.string.toast_connection_to_google_play_suspended, Toast.LENGTH_LONG).show();
		Log.i(TAG, "Connection to Google Play Suspended: " + cause);
	}
	
	@Override
    public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
        // Check if the status code is not success.
        Status status = result.getStatus();
        if (!status.isSuccess()) {
            showError(status.getStatusCode());
            return;
        }

        TurnBasedMatch match = result.getMatch();

        // If this player is not the first player in this match, continue.
        if (match.getData() != null) {
            showTurnUI(match);
            return;
        }

        // Otherwise, this is the first player. Initialize the game state.
        initGame(match);

        // Let the player take the first turn
        showTurnUI(match);
    }

	private void showError(int statusCode) {
		// TODO Auto-generated method stub
		
	}

	private void initGame(TurnBasedMatch match) {
		// TODO Auto-generated method stub
		
	}

	private void showTurnUI(TurnBasedMatch match) {
		// TODO Auto-generated method stub
		
	}
	
	private void startMatch() {
		Intent intent = Games.TurnBasedMultiplayer.getSelectOpponentsIntent(mGAPI, 1, 7, true);
		    startActivityForResult(intent, ACTIVITY_RESULT_CODE_SELECT_PLAYERS);
	}
	
	public final SharedPreferences getPrefs() {
		return PreferenceManager.getDefaultSharedPreferences(this);
	}

	public final boolean isPortrait() {
		return getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
	}
	
	public final void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            View focused = getContentView().findFocus();
            if (focused != null) {
                imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
            }
        }
    }
	
	public final View getContentView() {
		return getWindow().getDecorView().findViewById(android.R.id.content);
	}

	@Override
	public void onJoinedRoom(int statusCode, Room room) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLeftRoom(int statusCode, String roomId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRoomConnected(int statusCode, Room room) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRoomCreated(int statusCode, Room room) {
		// TODO Auto-generated method stub
		
	}
}
