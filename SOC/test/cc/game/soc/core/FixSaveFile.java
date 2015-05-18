package cc.game.soc.core;

import java.io.PrintWriter;

import cc.lib.utils.Reflector;
import junit.framework.TestCase;

public class FixSaveFile extends TestCase {

	public void test() throws Exception {
		Reflector.ENABLE_THROW_ON_UNKNOWN = true;
		SOC soc = new SOC();
		assertTrue(soc.load("socsavegame.txt"));
		soc.serialize(new PrintWriter(System.out));
	}
	
	public void xtest() {
		Reflector.ENABLE_THROW_ON_UNKNOWN = true;
		SOC soc = new SOC();
		assertTrue(soc.load("socsavegame.txt"));
		Board b = soc.getBoard();
		for (Vertex v : b.getVerticies()) {
			if (v.getPlayer()==0) {
				v.removePlayer();
				System.out.println("fixing board vertex : " + v);
			}
		}
		
		for (Player p : soc.getPlayers()) {
			for (Card c : p.getCards()) {
				if (c.getCardType() == CardType.SpecialVictory) {
					if (!c.isUsed()) {
						c.setUsed();
						System.out.println("Fixing " + p.getName() + "'s card : " + c);
					}
				}
			}
		}
		
		
		soc.save("socsavegame_FIXED.txt");
		soc.load("socsavegame_FIXED.txt");
	}
	
}
