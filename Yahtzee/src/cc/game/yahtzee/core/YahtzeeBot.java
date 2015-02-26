package cc.game.yahtzee.core;

import java.util.List;

public class YahtzeeBot extends Yahtzee {

	@Override
	protected boolean onChooseKeepers(boolean[] keeprs) {

		// This is the hard one.  Need to consider many factors ....

		
		
		throw new RuntimeException("Not implemented");
	}

	@Override
	protected YahtzeeSlot onChooseSlotAssignment(List<YahtzeeSlot> choices) {
		int max = -1;
		YahtzeeSlot best = null;
		for (YahtzeeSlot slot : choices) {
			int score = slot.getScore(this);
			if (score > max) {
				max = score;
				best = slot;
			}
		}
		return best;
	}

}
