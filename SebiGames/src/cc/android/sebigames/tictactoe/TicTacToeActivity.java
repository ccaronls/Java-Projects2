package cc.android.sebigames.tictactoe;

import cc.android.pacboy.R;
import cc.lib.game.Utils;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

public class TicTacToeActivity extends Activity implements OnClickListener, DialogInterface.OnCancelListener {

	int GLOW_REPEATS = 5;
	int GLOW_PERIOD = 1000;
	int GLOW_DURATION_MSECS = GLOW_REPEATS * GLOW_PERIOD;
	
	int numPlayers = 0;
	
	int [] COLORS = {
			0,
			Color.BLUE,
			Color.RED
	};
	
	TTTView [][] grid = new TTTView[3][3];
	
	int turn = 0;
	int difficulty = 0;
	
	void newGame() {
		try {
    		turn = 0;
    		for (TTTView [] tt : grid) {
    			for (TTTView t : tt) {
    				t.setColor(0);
    				t.setShape(0);
    				t.setAnimation(null);
    			}
    		}
    		new AlertDialog.Builder(this).setTitle("PLAYERS").setItems(new String[] { "One",  "Two" }, new DialogInterface.OnClickListener() {
    			
    			@Override
    			public void onClick(DialogInterface dialog, int which) {
    				numPlayers = which+1;
    				if (numPlayers == 1) {
    					new AlertDialog.Builder(TicTacToeActivity.this).setTitle("DIFFICULTY").setItems(new String [] { "Easy", "Hard" }, new DialogInterface.OnClickListener() {
    						
    						@Override
    						public void onClick(DialogInterface dialog, int which) {
    							// TODO Auto-generated method stub
    							turn = 1;
    							difficulty = which;
    						}
    					}).setOnCancelListener(TicTacToeActivity.this).show();
    				} else {
    					turn = 1;
    				}
    			}
    		}).setOnCancelListener(this).show();
		} catch (Exception e) {}
	}
	
	@Override
	public void onCancel(DialogInterface dialog) {
		finish();
	}
	
	private Animation glowAnim;
	private Animation turnAnim;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tictactoe_activity);
		(grid[0][0] = (TTTView)findViewById(R.id.buttonTopLeft)).setOnClickListener(this);
		(grid[1][0] = (TTTView)findViewById(R.id.buttonTopMiddle)).setOnClickListener(this);
		(grid[2][0] = (TTTView)findViewById(R.id.buttonTopRight)).setOnClickListener(this);

		(grid[0][1] = (TTTView)findViewById(R.id.buttonMiddleLeft)).setOnClickListener(this);
		(grid[1][1] = (TTTView)findViewById(R.id.buttonMiddle)).setOnClickListener(this);
		(grid[2][1] = (TTTView)findViewById(R.id.buttonMiddleRight)).setOnClickListener(this);

		(grid[0][2] = (TTTView)findViewById(R.id.buttonBottomLeft)).setOnClickListener(this);
		(grid[1][2] = (TTTView)findViewById(R.id.buttonBottomMiddle)).setOnClickListener(this);
		(grid[2][2] = (TTTView)findViewById(R.id.buttonBottomRight)).setOnClickListener(this);
		
		makeAnims();
		newGame();
	}
	
	private void makeAnims() {
		{
    		AlphaAnimation  a = new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible
    		a.setDuration(GLOW_PERIOD); // duration - half a second
    		a.setInterpolator(new LinearInterpolator()); // do not alter animation rate
    		a.setRepeatCount(GLOW_REPEATS); // Repeat animation infinitely
    		a.setRepeatMode(Animation.REVERSE);
    		glowAnim = a;
		}
		
		{
			
			ScaleAnimation a = new ScaleAnimation(0.5f, 1, 1, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
			a.setDuration(100);
			a.setRepeatCount(3);
			a.setRepeatMode(Animation.REVERSE);
			a.setInterpolator(new LinearInterpolator());

			turnAnim = a;
		}
		
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	int [][] weightsEasy = {
			{ 1,1,1 },
			{ 1,2,1 },
			{ 1,1,1 },
	};
	
	int [][] weightsHard = {
			{ 2,1,2 },
			{ 1,4,1 },
			{ 2,1,2 }
	};
	
	int [][][] diff = {
			weightsEasy, weightsHard
	};
	
	@Override
	public void onClick(View v) {
		if (turn == 0)
			return;
		
		final TTTView tv = (TTTView)v;
		if (tv.getShape() == 0) {
			tv.setShape(turn);
			tv.setColor(COLORS[turn]);
			final int saveTurn = turn;
			turn = 0;
			turnAnim.setAnimationListener(new Animation.AnimationListener() {
				
				@Override
				public void onAnimationStart(Animation animation) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onAnimationRepeat(Animation animation) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					turnAnim.setAnimationListener(null);
					tv.setAnimation(null);
					turn = saveTurn;
					checkWinner(turn);
					checkTie();
					checkRobot();
				}
			});
			tv.startAnimation(turnAnim);
		} 
		
	}

	int [][] winx = {
			// horz
			{ 0, 1, 2 },
			{ 0, 1, 2 },
			{ 0, 1, 2 },
			
			// vert
			{ 0, 0, 0 },
			{ 1, 1, 1 },
			{ 2, 2, 2 },
			
			// diag
			{ 0, 1, 2 },
			{ 2, 1, 0 },

	};
	int [][] winy = { 
			// horz
			{ 0, 0, 0 },
			{ 1, 1, 1 },
			{ 2, 2, 2 },
			
			// vert
			{ 0, 1, 2 },
			{ 0, 1, 2 },
			{ 0, 1, 2 },

			// diag
			{ 0, 1, 2 },
			{ 0, 1, 2 },
	};
	
	void checkWinner(int shape) {
		if (turn == 0)
			return;
		for (int i=0; i<winx.length; i++) {
			TTTView [] winner = new TTTView[3];
			for (int ii=0; ii<3; ii++) {
				if ((winner[ii] = grid[winx[i][ii]][winy[i][ii]]).getShape() != shape)
					break;
				if (ii == 2) {
					// we have a winner!
					for (int iii=0; iii<3; iii++) {
						winner[iii].startAnimation(glowAnim);
					}
					grid[0][0].postDelayed(new Runnable() {
						public void run() {
							newGame();
						}
					}, GLOW_DURATION_MSECS);
					turn = 0;
					return;
				}
			}
		}
	}
	
	void checkTie() {
		if (turn == 0)
			return;
		for (TTTView [] tt : grid) {
			for (TTTView t : tt) {
				if (t.getShape() == 0)
					return;
			}
		}
		turn = 0;
		final Dialog d = new AlertDialog.Builder(this).setMessage("Tie Game").show();
		grid[0][0].postDelayed(new Runnable() {
			public void run() {
				d.dismiss();
				newGame();
			}
		}, 5000);
	}
	
	void checkRobot() {
		if (turn == 0)
			return;
		if (numPlayers == 1) {
			// pick a random
			int [][] w = diff[difficulty];
			int [] weights = new int[9];
			for (int i=0; i<9; i++) {
				TTTView t = grid[i%3][i/3];
				if (t.getShape() != 0)
					continue;
				weights[i] = w[i%3][i/3];
			}
			int index = Utils.chooseRandomFromSet(weights);
			final TTTView t = grid[index%3][index/3];
			t.setShape(2);
			t.setColor(COLORS[2]);
			turn = 0;
			
			turnAnim.setAnimationListener(new Animation.AnimationListener() {
				
				@Override
				public void onAnimationStart(Animation animation) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onAnimationRepeat(Animation animation) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					animation.setAnimationListener(null);
					t.setAnimation(null);
					turn = 1;
					checkWinner(2);
					checkTie();
				}
			});
			t.startAnimation(turnAnim);

		} else {
			turn = turn == 1 ? 2 : 1;
		}
				
	}
}
