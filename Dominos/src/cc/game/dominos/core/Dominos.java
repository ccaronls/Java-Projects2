package cc.game.dominos.core;

import java.util.*;

import cc.lib.game.AGraphics;
import cc.lib.math.Vector2D;
import cc.lib.utils.Reflector;

public class Dominos extends Reflector<Dominos> {

	Player [] players;	
	List<Piece> pool = new LinkedList<Piece>();
	Piece root;
	int minX, minY;
	int maxX, maxY;
	
	public void initPieces(int maxNum) {
		pool.clear();
		for (int i=1; i<=maxNum; i++) {
			for (int ii=i; ii<=maxNum; ii++) {
				pool.add(new Piece(i, ii));
			}
		}
	}
	
	public void initPlayers(Player ... players) {
		
	}
	
	public void newGame() {
		
	}

	public void runGame() {
		
	}
	
}
