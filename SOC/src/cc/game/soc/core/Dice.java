package cc.game.soc.core;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public final class Dice extends Reflector<Dice> {

	static {
		addAllFields(Dice.class);
	}
	
	private int num = 0;
	private DiceType type = DiceType.None;
	
	public Dice() {}

	public Dice(DiceType type) {
		this(0, type);
	}

	public Dice(int num, DiceType type) {
		this.num = num;
		this.type = type;
	}

	public final int getNum() {
		return num;
	}
	
	public final void setNum(int num) {
		this.num = num;
	}
	
	public final DiceType getType() {
		return type;
	}
	
	final void setType(DiceType type) {
		this.type = type;
	}
	
	void roll() {
		num = Utils.rand() % 6 + 1;
	}
}
