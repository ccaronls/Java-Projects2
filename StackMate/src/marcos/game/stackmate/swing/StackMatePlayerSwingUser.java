package marcos.game.stackmate.swing;

import marcos.game.stackmate.core.StackMate;
import marcos.game.stackmate.core.StackMatePlayer;

public class StackMatePlayerSwingUser extends StackMatePlayer {

	@Override
	public int chooseSourceStack(StackMate sm, int[] choices) {
		return StackMateApplet.instance.chooseStack(choices);
	}

	@Override
	public int chooseTargetStack(StackMate sm, int[] choices) {
		return StackMateApplet.instance.chooseStack(choices);
	}

	
	
}
