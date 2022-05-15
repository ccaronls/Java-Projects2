package cc.game.android.yahtzee;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import cc.game.yahtzee.core.YahtzeeRules;
import cc.game.yahtzee.core.YahtzeeSlot;
import cc.game.yahtzee.core.YahtzeeState;
import cc.lib.game.Utils;

public class YahtzeeActivity extends Activity implements OnClickListener {

	private final int DICE_COUNT = 5;
	
	private final ImageView [] imageViewDie = new ImageView[DICE_COUNT];
	private final TextView [] textViewKeepDie = new TextView[DICE_COUNT];
	
	private TextView textViewRollsValue;
	private TextView textViewYahtzeesValue;
	private TextView textViewUpperPointsValue;
	private TextView textViewTotalPointsValue;
	private TextView textViewBonusPointsValue;
	private TextView textViewTopScoreValue;
	
	private ListView listViewYahtzeeSlots;
	private Button buttonRollDice;
	
	// should these really be in other files?
	private YahtzeeSlotAdapter slotAdapter;
	private YahtzeeRunner runner;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		runner = new YahtzeeRunner(this);
		setContentView(R.layout.yahtzeeactivity);
		imageViewDie[0] = (ImageView)findViewById(R.id.imageViewDie1);
		imageViewDie[1] = (ImageView)findViewById(R.id.imageViewDie2);
		imageViewDie[2] = (ImageView)findViewById(R.id.imageViewDie3);
		imageViewDie[3] = (ImageView)findViewById(R.id.imageViewDie4);
		imageViewDie[4] = (ImageView)findViewById(R.id.imageViewDie5);
		
		textViewKeepDie[0] = (TextView)findViewById(R.id.textViewKeepDie1);
		textViewKeepDie[1] = (TextView)findViewById(R.id.textViewKeepDie2);
		textViewKeepDie[2] = (TextView)findViewById(R.id.textViewKeepDie3);
		textViewKeepDie[3] = (TextView)findViewById(R.id.textViewKeepDie4);
		textViewKeepDie[4] = (TextView)findViewById(R.id.textViewKeepDie5);
		imageViewDie[0].setOnClickListener(this);
		imageViewDie[1].setOnClickListener(this);
		imageViewDie[2].setOnClickListener(this);
		imageViewDie[3].setOnClickListener(this);
		imageViewDie[4].setOnClickListener(this);
		
		textViewRollsValue = (TextView)findViewById(R.id.TextViewRollsValue);
		textViewYahtzeesValue = (TextView)findViewById(R.id.textViewYahtzeesValue);
		textViewUpperPointsValue = (TextView)findViewById(R.id.textViewUpperPointsValue);
		textViewBonusPointsValue = (TextView)findViewById(R.id.textViewBonusPointValue);
		textViewTotalPointsValue = (TextView)findViewById(R.id.textViewTotalPointsValue);
		textViewTopScoreValue = (TextView)findViewById(R.id.textViewTopScoreValue);

		listViewYahtzeeSlots = (ListView)findViewById(R.id.listViewYahtzeeSlots);
		slotAdapter = new YahtzeeSlotAdapter(this, runner);
		listViewYahtzeeSlots.setAdapter(slotAdapter);
		buttonRollDice = (Button)findViewById(R.id.buttonRollDice);
		buttonRollDice.setOnClickListener(this);
	}

	private final int [] diceImageIds = { 
			R.drawable.dice1, 
			R.drawable.dice2, 
			R.drawable.dice3, 
			R.drawable.dice4, 
			R.drawable.dice5, 
			R.drawable.dice6 
		};

	void setDieImage(ImageView view, int dieNum) {
		if (view != null && dieNum > 0 && dieNum <= diceImageIds.length) {
			view.setImageResource(diceImageIds[dieNum-1]);
        }
	}
	
	boolean [] keepers = new boolean[DICE_COUNT];
	YahtzeeSlot slotChoice = null;
	
	void refresh() {
		runOnUiThread(new Runnable() {
			public void run() {
				slotAdapter.notifyDataSetChanged();
				switch (runner.getState()) {
					case CHOOSE_SLOT:
						buttonRollDice.setText("Choose Slot");
						buttonRollDice.setEnabled(false);
						break;
					
					case GAME_OVER:
						buttonRollDice.setText("Play Again");
						buttonRollDice.setEnabled(true);
						break;
						
					default:
						buttonRollDice.setText("Roll Dice");
						buttonRollDice.setEnabled(true);
						break;
				}
				int [] dice = runner.getDiceRoll();
				for (int i=0; i<dice.length; i++) {
					setDieImage(imageViewDie[i], dice[i]);
					textViewKeepDie[i].setVisibility(keepers[i] ? View.VISIBLE : View.INVISIBLE);
				}
				textViewRollsValue.setText("" + runner.getRollCount() + " of " + runner.getRules().getNumRollsPerRound());
				textViewYahtzeesValue.setText("" + runner.getNumYahtzees());
				textViewUpperPointsValue.setText("" + runner.getUpperPoints());
				textViewBonusPointsValue.setText("" + runner.getBonusPoints());
				textViewTotalPointsValue.setText("" + runner.getTotalPoints());
				textViewTopScoreValue.setText("" + runner.getTopScore());
			}
		});
	}
	
	void showGameOver() {
		refresh();
        Utils.waitNoThrow(runner, -1);
	}
	
	// called from other runner thread
	YahtzeeSlot showChooseSlot(List<YahtzeeSlot> slots) {
		slotChoice = null;
		refresh();
        Utils.waitNoThrow(runner, -1);
		return slotChoice;
	}
	
	private void sleep() {
        Utils.waitNoThrow(runner, -1);
	}
	
	private void wake() {
		synchronized (runner) {
			runner.notify();
		}
	}
	
	private boolean rollEm = false;
	
	// called from other runner thread
	boolean showChooseKeepers(final boolean [] keepers) {
		rollEm = false;
		this.keepers = keepers;
		refresh();
		sleep();
		return rollEm;
	}
	
	void chooseSlot(YahtzeeSlot slot) {
		slotChoice = slot;
		slotAdapter.notifyDataSetChanged();
        Arrays.fill(keepers, false);
	}

	@Override
	protected void onResume() {
		super.onResume();
		runner.startThread();
	}

	@Override
	protected void onPause() {
		super.onPause();
		runner.stopThread();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("New Game");
		menu.add("About");
		menu.add("Rules");
		return super.onCreateOptionsMenu(menu);
		
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getTitle().equals("New Game")) {
			runner.reset();
			wake();
		} else if (item.getTitle().equals("Rules")) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			String [] items = { "Normal", "Alternate" };
			int checkedItem = runner.getRules().isEnableAlternateVersion() ? 1 : 0;
			builder.setTitle("Choose game type")
				.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						YahtzeeRules rules = new YahtzeeRules();
						rules.setEnableAlternateVersion(which == 0 ? false : true);
						runner.reset(rules);
						dialog.dismiss();
						wake();
					}
				}).setNegativeButton("Cancel", null)
				.show();
		}
		
		
		return super.onOptionsItemSelected(item);
	}

	private int numDiceRolling = 0;

	class DieRoller implements Runnable {
		
		int delay;
		ImageView view;
		
		DieRoller(ImageView view) {
			delay = Utils.rand() % 10 + 10;
			this.view = view;
			view.postDelayed(this, delay);
			numDiceRolling++;
		}
		public void run() {
			delay += Utils.rand() % 30 + 10;
			if (delay < 500) {
				buttonRollDice.setEnabled(false);
				view.setImageResource(diceImageIds[Utils.rand() % diceImageIds.length]);
				view.postDelayed(this, delay);
			} else {
				numDiceRolling--;
				if (numDiceRolling <= 0) {
					rollEm = true;
					buttonRollDice.setEnabled(false);
					wake();
				}
			}
		}
	}
	
	public void rollTheDice() {
		refresh();
		numDiceRolling = 0;
		boolean [] keepers = runner.getKeepers();
		for (int i=0; i<keepers.length; i++) {
			if (!keepers[i]) {
				new DieRoller(imageViewDie[i]);
			}
		}
		if (numDiceRolling <= 0) {
			wake();
		} else {
			sleep();
		}
	}
	
	@Override
	public void onClick(View v) {
		
		if (v.getTag() != null && (v.getTag() instanceof YahtzeeSlot)) {
			chooseSlot((YahtzeeSlot)v.getTag());
		} else if (v.getId() == R.id.buttonRollDice) {
			if (runner.getState() == YahtzeeState.GAME_OVER) {
				runner.reset();
			} else {
				rollEm = true;
			}
		} else {
			if (numDiceRolling <= 0) {
				for (int i=0; i<imageViewDie.length; i++) {
					if (v.getId() == imageViewDie[i].getId()) {
						keepers[i] = !keepers[i];
						break;
					}
				}
			}
		}
		wake();
	}

	
	
}
