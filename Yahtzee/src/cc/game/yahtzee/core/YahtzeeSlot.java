package cc.game.yahtzee.core;

public enum YahtzeeSlot {

	ONES("Ones", true),
	TWOS("Twos", true),
	THREES("Threes", true),
	FOURS("Fours", true),
	FIVES("Fives", true),
	SIXES("Sixes", true),
	
	ONE_PAIR("One Pair", false),
	TWO_PAIR("Two Pair", false),
	THREE_OF_A_KIND("Three of a Kind", false),
	FOUR_OF_A_KIND("Four of a Kind", false),
	SMALL_STRAIGHT("Small Straight", false),
	LARGE_STRAIGHT("Large Straight", false),
	FULL_HOUSE("Full House", false),
	YAHZEE("Yahtzee", false),
	CHANCE("Chance", false);

	final boolean isUpper;
	final String niceName;
	
	private YahtzeeSlot(String cleanName, boolean isUpper) {
		this.niceName = cleanName;
		this.isUpper = isUpper;
	}

	public int getScore(Yahtzee yahtzee) {
		return getScore(yahtzee.getRules(), yahtzee.getDiceRoll());
	}
	
	public String getNiceName() {
		return niceName;
	}
	
	public int getScore(YahtzeeRules rules, int ... roll) {
		int score = 0;
		switch (this) {
		case ONES:
		case TWOS:
		case THREES:
		case FOURS:
		case FIVES:
		case SIXES:
			for (int i : roll) {
				if (i == ordinal()+1) {
					score += ordinal()+1;
				}
			}
			break;
			
		case ONE_PAIR: {
			int [] counts = occurances(rules, roll);
			for (int i=rules.getNumDiceSides(); i>0; i--) {
				if (counts[i] >= 2) {
					score = 2*i;
					break;
				}
			}
			break;
		}
		case TWO_PAIR:{
			int high = 0;
			int low = 0;
			int [] counts = occurances(rules, roll);
			for (int i=rules.getNumDiceSides(); i>0; i--) {
				if (counts[i] >= 2) {
					if (high == 0) {
						high = 2*i;
					} else {
						low = 2*i;
						break;
					}
				}
			}
			if (high > 0 && low > 0) {
				score = high + low;
			}
			break;
		}
		case THREE_OF_A_KIND: {
			int [] counts = occurances(rules, roll);
			for (int i=rules.getNumDiceSides(); i>0; i--) {
				if (counts[i] >= 3) {
					score = sum(roll);
					break;
				}
			}
			break;
		}
		case FOUR_OF_A_KIND:{
			int [] counts = occurances(rules, roll);
			for (int i=rules.getNumDiceSides(); i>0; i--) {
				if (counts[i] >= 4) {
					score = sum(roll);
					break;
				}
			}
			break;
		}
		case SMALL_STRAIGHT: {
			if (getStraightLength(rules, roll) >= 4)
				score = 30;
			break;
		}
		case LARGE_STRAIGHT:{
			if (getStraightLength(rules, roll) >= 5)
				score = 40;
			break;
		}
		case FULL_HOUSE: {
			int [] counts = occurances(rules, roll);
			boolean have2 = false;
			boolean have3 = false;
			for (int i=0; i<counts.length; i++) {
				if (counts[i] == 2)
					have2 = true;
				if (counts[i] == 3)
					have3 = true;
			}
			if (have2 && have3)
				score = 25;
			break;
		}
		case YAHZEE:{
			int [] counts = occurances(rules, roll);
			for (int i=1; i<counts.length; i++) {
				if (counts[i] >= 5) {
					score = 50;
					break;
				}
			}
			break;
		}
			
		case CHANCE:
			score = sum(roll);
			break;
		}
		return score;
	}
	
	static int getStraightLength(YahtzeeRules rules, int [] dice) {
		int [] counts = occurances(rules, dice);
		int max = 0;
		int len = 0;
		for (int i=1; i<counts.length; i++) {
			if (counts[i] > 0)
				len++;
			else
				len=0;
			max = Math.max(max, len);
		}
		return max;
	}
	
	static int [] occurances(YahtzeeRules rules, int ... dice) {
		int [] count = new int[rules.getNumDiceSides()+1];
		for (int i : dice) {
			count[i]++;
		}
		return count;
	}
	
	static int sum(int ... dice) {
		int sum= 0;
		for (int i : dice) {
			sum += i;
		}
		return sum;
	}
}
