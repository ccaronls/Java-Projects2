package cc.lib.yahtzee;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class YahtzeeTest extends TestCase {

	YahtzeeRules rules = new YahtzeeRules();
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testSmallStraight() {
		
		int [] dice = { 4, 6, 5, 4, 3 };
		
		int score = YahtzeeSlot.SMALL_STRAIGHT.getScore(rules, dice);
		assertTrue(score > 0);
		score = YahtzeeSlot.LARGE_STRAIGHT.getScore(rules, dice);
		assertTrue(score == 0);
	}
	
	boolean gotBonus = false;
	
	public void testYahtzeeBonus() {
		Yahtzee y = new Yahtzee() {
			
			@Override
			protected YahtzeeSlot onChooseSlotAssignment(List<? extends YahtzeeSlot> choices) {
				if (choices.contains(YahtzeeSlot.YAHZEE))
					return YahtzeeSlot.YAHZEE;
				return choices.get(0);
			}
			
			@Override
			protected boolean onChooseKeepers(boolean[] keepers) {
				Arrays.fill(keepers,  true);
				return true;
			}

			@Override
			public void rollDice(int [] dice) {
				Arrays.fill(dice,  6); // always a yahtzee
			}

			@Override
			protected void onBonusYahtzee(int bonusScore) {
				System.out.println("Bonus Yahtzee!");
				gotBonus = true;
			}
			
			
		};
		
		for (int i=0; i<10; i++) {
			y.runGame();
		}
		
		assertTrue(gotBonus);
	}
	
	void scramble(int [] arr) {
		for (int i=0; i<100; i++) {
			int a = rand.nextInt(arr.length);
			int b = rand.nextInt(arr.length);
			int t = arr[a];
			arr[a] = arr[b];
			arr[b] = t;
		}
	}
	
	Random rand = new Random();
}
