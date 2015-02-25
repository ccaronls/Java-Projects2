package marcos.game.stackmate.core;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class StackMatePlayer extends Reflector<StackMatePlayer> {

	static {
		addAllFields(StackMatePlayer.class);
	}
	
	StackMate.Chip color;

	public int chooseSourceStack(StackMate sm, int [] choices) {
		return choices[Utils.rand() % choices.length];
	}

	public int chooseTargetStack(StackMate sm, int [] choices) {
		return choices[Utils.rand() % choices.length];
	}
	
}
