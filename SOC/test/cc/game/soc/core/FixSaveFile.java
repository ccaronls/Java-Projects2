package cc.game.soc.core;

import junit.framework.TestCase;

public class FixSaveFile extends TestCase {

	public void test() {
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
						c.setUsed(true);
						System.out.println("Fixing " + p.getName() + "'s card : " + c);
					}
				}
			}
		}
		
		
		soc.save("socsavegame.txt");
	}
	
}
