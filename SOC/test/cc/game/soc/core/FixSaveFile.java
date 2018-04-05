package cc.game.soc.core;

import junit.framework.TestCase;

import java.io.File;

import cc.lib.utils.Reflector;

public class FixSaveFile extends TestCase {

	public void test() throws Exception {
		Reflector.THROW_ON_UNKNOWN = true;
		SOC soc = new SOC();
		assertTrue(soc.load("~/.soc/socsavegame.txt"));
		soc.serialize(System.out);
	}
	
	public void xtest() {
		Reflector.THROW_ON_UNKNOWN = true;
		SOC soc = new SOC();
		assertTrue(soc.load("socsavegame.txt"));
		Board b = soc.getBoard();
		for (int i=0; i<b.getNumAvailableVerts(); i++) {
		    Vertex v = b.getVertex(i);
			if (v.getPlayer()==0) {
				v.setOpen();
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

	public void testFixScenarios() throws Exception {
        File [] files = new File("assets/scenarios").listFiles();
        for (File f : files) {
            System.out.println("fixing: " + f);
            Scenario s = new Scenario();
            s.loadFromFile(f);
            s.saveToFile(f);
        }

    }
}
