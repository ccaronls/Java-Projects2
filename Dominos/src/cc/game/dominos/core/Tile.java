package cc.game.dominos.core;

import cc.lib.utils.Reflector;

public final class Tile extends Reflector<Tile> {

	static {
		addAllFields(Tile.class);
	}
	
	public final int pip1, pip2;

	int openPips;

	public Tile() {
	    this(0,0);
    }

	public Tile(int pip1, int pip2) {
		this.pip1 = pip1;
		this.pip2 = pip2;
		openPips = pip1;
	}

	public int getClosedPips() {
	    if (pip1 == openPips)
	        return pip2;
	    return pip1;
    }

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		Tile p = (Tile)o;
		return pip1 == p.pip1 && pip2 == p.pip2;
	}

	public final boolean isDouble() {
		return pip1 == pip2;
	}
	
}
