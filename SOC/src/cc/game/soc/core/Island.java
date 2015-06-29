package cc.game.soc.core;

import java.util.ArrayList;
import java.util.List;

import cc.lib.utils.Reflector;

public class Island extends Reflector<Island> {

	static {
		addAllFields(Island.class);
	}
	
	public Island() {
		this(0);
	}
	
	Island(int num) {
		this.num = num;
	}
	
	int num; // starts at 1
	final List<Integer> tiles = new ArrayList<Integer>();
	final List<Integer> borderRoute = new ArrayList<Integer>();
	final boolean [] discovered = new boolean[16];
	
	public int getNum() {
		return num;
	}
	
	public Iterable<Integer> getTiles() {
		return tiles;
	}
	
	public Iterable<Integer> getShoreline() {
		return borderRoute;
	}
	
	public boolean isDiscoveredBy(int playerNum) {
		if (playerNum > 0 && playerNum < discovered.length) {
			return discovered[playerNum];
		}
		return false;
	}
}
