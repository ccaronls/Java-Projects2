package cc.android.pacboy;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import cc.lib.android.CCActivityBase;

public class HomeActivity extends CCActivityBase implements OnClickListener, GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener {

	//private GoogleApiClient mGAPI;
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.home_activity);
		findViewById(R.id.buttonSmallEasy).setOnClickListener(this);
		findViewById(R.id.buttonSmallHard).setOnClickListener(this);
		findViewById(R.id.buttonSmallVeryHard).setOnClickListener(this);
		findViewById(R.id.buttonMedEasy).setOnClickListener(this);
		findViewById(R.id.buttonMedHard).setOnClickListener(this);
		findViewById(R.id.buttonMedVeryHard).setOnClickListener(this);
		findViewById(R.id.buttonLargeEasy).setOnClickListener(this);
		findViewById(R.id.buttonLargeHard).setOnClickListener(this);
		findViewById(R.id.buttonLargeVeryHard).setOnClickListener(this);
		findViewById(R.id.buttonHugeEasy).setOnClickListener(this);
		findViewById(R.id.buttonHugeHard).setOnClickListener(this);
		findViewById(R.id.buttonHugeVeryHard).setOnClickListener(this);
		findViewById(R.id.buttonViewLeaderboards).setOnClickListener(this);
		/*
		mGAPI = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Games.API)
				.addScope(Games.SCOPE_GAMES)
				.build();*/
		findViewById(R.id.buttonViewLeaderboards).setEnabled(false);
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
		Intent i = new Intent(this, PacBoyActivity.class);
		int code = RC_GAME_RESULT;
		switch (v.getId()) {
			case R.id.buttonSmallEasy:
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 6);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 4);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 0);
				break;
			case R.id.buttonSmallHard:
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 6);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 4);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 10);
				break;
			case R.id.buttonSmallVeryHard:
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 6);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 4);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 20);
				break;
			case R.id.buttonMedEasy:
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 10);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 6);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 0);
				break;
			case R.id.buttonMedHard:
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 10);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 6);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 10);
				break;
			case R.id.buttonMedVeryHard:
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 10);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 6);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 20);
				break;
			case R.id.buttonLargeEasy:
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 16);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 10);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 0);
				break;
			case R.id.buttonLargeHard:
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 16);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 10);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 10);
				break;
			case R.id.buttonLargeVeryHard:
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 16);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 10);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 20);
				break;
			case R.id.buttonHugeEasy:
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 24);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 16);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 0);
				break;
			case R.id.buttonHugeHard:
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 24);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 16);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 10);
				break;
			case R.id.buttonHugeVeryHard:
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 24);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 16);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 20);
				break;
			case R.id.buttonViewLeaderboards:
				//i = Games.Leaderboards.getLeaderboardIntent(mGAPI, LEADERBOARD_ID);
//					.getAllLeaderboardsIntent(mGAPI);
				code = RC_GET_ALL_LEADERBOARDS;
				break;
		}
		startActivityForResult(i, code);
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
    public void onDisconnected() {}

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