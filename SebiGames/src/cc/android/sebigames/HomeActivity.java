package cc.android.sebigames;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.ScoreSubmissionData;
import com.google.android.gms.games.leaderboard.Leaderboards.SubmitScoreResult;
import com.google.android.gms.games.leaderboard.ScoreSubmissionData.Result;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import cc.android.pacboy.R;
import cc.android.sebigames.tictactoe.TicTacToeActivity;
import cc.lib.android.CCActivityBase;

public class HomeActivity extends CCActivityBase implements OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

	//private GoogleApiClient mGAPI;
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.home_activity);
		findViewById(R.id.buttonPacboy).setOnClickListener(this);
		findViewById(R.id.buttonPicmatch).setOnClickListener(this);
		findViewById(R.id.buttonSnakes).setOnClickListener(this);
		findViewById(R.id.buttonCalculator).setOnClickListener(this);
		findViewById(R.id.buttonTicTacToe).setOnClickListener(this);
		/*
		mGAPI = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Games.API)
				.addScope(Games.SCOPE_GAMES)
				.build();*/
		//findViewById(R.id.buttonViewLeaderboards).setEnabled(false);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		//mGAPI.connect();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		//mGAPI.disconnect();
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
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.buttonCalculator:
			case R.id.buttonPacboy:
			case R.id.buttonPicmatch:
			case R.id.buttonSnakes:
				break;
			case R.id.buttonTicTacToe:
				startActivity(new Intent(this, TicTacToeActivity.class));
				break;
		}
	}

	private final int MSG_SIGNIN_FAILED = 33;
	private final int MSG_SIGNIN_SUCCESS = 34;
	private final int MSG_SCORE_SUBMIT_RESULT = 35;
	
	private final Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			Activity a = HomeActivity.this;
			switch (msg.what) {
				case MSG_SIGNIN_FAILED:
					Toast.makeText(a, "Failed to Sign in to Google Play", Toast.LENGTH_SHORT).show();
					findViewById(R.id.buttonViewLeaderboards).setEnabled(false);
					break;
				case MSG_SIGNIN_SUCCESS:
					Toast.makeText(a, "Signed in to Google Play", Toast.LENGTH_SHORT).show();
					findViewById(R.id.buttonViewLeaderboards).setEnabled(true);
					break;
				case MSG_SCORE_SUBMIT_RESULT:
					Toast.makeText(a,  msg.obj.toString(), Toast.LENGTH_LONG).show();
					break;
				default:
					super.handleMessage(msg);
			}
		}
		
	};

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		try {
			result.startResolutionForResult(this, RC_RESOLUTION_RESULT);
		} catch (Exception e) {
			e.printStackTrace();
			handler.sendEmptyMessage(MSG_SIGNIN_FAILED);
		}
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		handler.sendEmptyMessage(MSG_SIGNIN_SUCCESS);
	}

	@Override
	public void onConnectionSuspended(int cause) {
		//mGAPI.reconnect();
	}
	
	final int RC_GET_ALL_LEADERBOARDS = 10030;
	final int RC_GAME_RESULT = 10031;
	final int RC_RESOLUTION_RESULT = 10032;
	
	final String LEADERBOARD_ID = "CgkIq6K3rt4dEAIQBg";
	
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		
		switch (requestCode) {
			case RC_RESOLUTION_RESULT: {
				if (resultCode == RESULT_OK) {
					//mGAPI.connect();
				}
				break;
			}
			
			case RC_GET_ALL_LEADERBOARDS: {
				break;
			}
			
			case RC_GAME_RESULT: {
				/*
				if (mGAPI != null && mGAPI.isConnected()) {
					PendingResult<SubmitScoreResult> result = Games.Leaderboards.submitScoreImmediate(mGAPI, LEADERBOARD_ID, intent.getIntExtra("score", 0));
					result.setResultCallback(new ResultCallback<SubmitScoreResult>() {
						
						@Override
						public void onResult(SubmitScoreResult result) {
							Status status = result.getStatus();
							if (status.isSuccess()) {
								do {
    								ScoreSubmissionData.Result score = result.getScoreData().getScoreResult(LeaderboardVariant.TIME_SPAN_ALL_TIME);
    								if (score != null && score.newBest) {
    									handler.sendMessage(handler.obtainMessage(MSG_SCORE_SUBMIT_RESULT, "New All Time Best!"));
    									break;
    								} 
    								score = result.getScoreData().getScoreResult(LeaderboardVariant.TIME_SPAN_WEEKLY);
    								if (score != null && score.newBest) {
    									handler.sendMessage(handler.obtainMessage(MSG_SCORE_SUBMIT_RESULT, "New Weekly Best!"));
    									break;
    								} 
    								score = result.getScoreData().getScoreResult(LeaderboardVariant.TIME_SPAN_DAILY);
    								if (score != null && score.newBest) {
    									handler.sendMessage(handler.obtainMessage(MSG_SCORE_SUBMIT_RESULT, "New Daily Best!"));
    									break;
    								} 
								} while (false);
							} else if (status.getStatusCode() == GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED) {
								mGAPI.reconnect();
							} 
						}
					});
				}*/
			}
		}
		
		/*
	    if (requestCode == RC_SIGN_IN) {
	        mSignInClicked = false;
	        mResolvingConnectionFailure = false;
	        if (resultCode == RESULT_OK) {
	            mGoogleApiClient.connect();
	        } else {
	            // Bring up an error dialog to alert the user that sign-in
	            // failed. The R.string.signin_failure should reference an error
	            // string in your strings.xml file that tells the user they
	            // could not be signed in, such as "Unable to sign in."
	            BaseGameUtils.showActivityResultError(this,
	                requestCode, resultCode, R.string.signin_failure);
	        }
	    }*/
	}

}