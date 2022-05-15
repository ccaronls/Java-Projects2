package cc.game.android.yahtzee;

import java.io.File;
import java.util.List;

import cc.game.yahtzee.core.Yahtzee;
import cc.game.yahtzee.core.YahtzeeRules;
import cc.game.yahtzee.core.YahtzeeSlot;

class YahtzeeRunner extends Yahtzee{

	private final File SAVE_FILE;
	private final File RULES_FILE;
	
	private final YahtzeeActivity activity;
	private final YahtzeeRules rules = new YahtzeeRules();
	
	YahtzeeRunner(YahtzeeActivity activity) {
		this.activity = activity;
		SAVE_FILE = new File(activity.getFilesDir(), "yahtzee.save");
		RULES_FILE = new File(activity.getFilesDir(), "yahtzeerules.sav");
		if (RULES_FILE.exists()) {
			try {
				rules.loadFromFile(RULES_FILE);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (SAVE_FILE.exists()) {
			try {
				loadFromFile(SAVE_FILE);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	protected boolean onChooseKeepers(boolean[] keeprs) {
		return activity.showChooseKeepers(keeprs);
	}

	@Override
	protected YahtzeeSlot onChooseSlotAssignment(List<YahtzeeSlot> choices) {
		return activity.showChooseSlot(choices);
	}
	
	@Override
	protected void onRollingDice() {
		activity.rollTheDice();
	}

	@Override
	protected void onBonusYahtzee(int bonusScore) {
		// TODO Auto-generated method stub
		super.onBonusYahtzee(bonusScore);
	}

	@Override
	protected void onGameOver() {
		activity.showGameOver();
	}

	@Override
	protected void onError(String msg) {
		// TODO Auto-generated method stub
		super.onError(msg);
	}

	enum RunState {
		STOPPED,
		STARTED,
		STOPPING
	}
	
	private RunState runState = RunState.STOPPED;
	
	private Runnable thread = new Runnable() {
		public void run() {
			try {
				while (runState == RunState.STARTED) {
					saveToFile(SAVE_FILE);
					runGame();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			runState = RunState.STOPPED;
		}
	};
	
	public void startThread() {
		synchronized (this) {
			if (runState == RunState.STOPPED) {
				runState = RunState.STARTED;
				new Thread(thread).start();
			}
		}
		
	}

	public void stopThread() {
		synchronized (this) {
			runState = RunState.STOPPING;
			notify();
		}
	}
	
	boolean isRunning() {
		return runState == RunState.STARTED;
	}

}
