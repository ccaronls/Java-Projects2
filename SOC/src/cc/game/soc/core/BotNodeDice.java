package cc.game.soc.core;

public class BotNodeDice extends BotNode {

	private Integer [] dice = new Integer[2];
	
	public BotNodeDice() {}

	BotNodeDice(int die0, int die1) {
		dice[0] = die0;
		dice[1] = die1;
	}
	
	@Override
	public Object getData() {
		return dice;
	}

	@Override
	public String getDescription() {
		return "Dice [" + dice[0] + " " + dice[1] + "]";
	}
	
	
}
