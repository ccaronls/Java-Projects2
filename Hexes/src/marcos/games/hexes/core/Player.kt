package marcos.games.hexes.core;

import java.util.Arrays;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.reflector.Reflector;

public class Player extends Reflector<Player> {

	static {
		addAllFields(Player.class);
	}
	
	private final int [] pieces = new int[Shape.values().length];
	int score = 0;
	
	public final int getShapeCount(Shape shape) {
		return pieces[shape.ordinal()];
	}
	
	public final int getScore() {
		return score;
	}
	
	void decrementPiece(Shape shape) {
		pieces[shape.ordinal()] -= 1;
	}
	
	final void init() {
		Utils.println("player.init");
		pieces[Shape.NONE.ordinal()] = 0;
		pieces[Shape.DIAMOND.ordinal()] = 6;
		pieces[Shape.TRIANGLE.ordinal()] = 6;
		pieces[Shape.HEXAGON.ordinal()] = 6;
		score = 0;
	}
	
	public final int countPieces() {
		return Utils.sum(pieces);
	}
	
	/**
	 * Return an int from the options list
	 * @param hexes
	 * @param choices
	 * @return
	 */
	public int choosePiece(Hexes hexes, List<Integer> choices) {
		Utils.println("choosePiece pieces=" + Arrays.toString(pieces));
		int choice = choices.get(Utils.rand() % choices.size());
		Utils.println("choosePiece pieces=" + Arrays.toString(pieces));
		return choice;
	}

	public Shape chooseShape(Hexes hexes, Shape [] choices) {
		return choices[Utils.rand() % choices.length];
	}
	
}
