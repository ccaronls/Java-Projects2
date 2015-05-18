package cc.game.soc.swing;

import java.io.File;

import cc.game.soc.core.SOC;
import cc.lib.utils.Reflector;
import junit.framework.TestCase;

public class ScenariosTest extends TestCase {

	public void testOne() throws Exception {
		SOC soc = new SOC();
		soc.loadFromFile(new File("scenarios/four_islands.txt"));
	}
	
	public void testLoad() throws Exception {
//		Reflector.ENABLE_THROW_ON_UNKNOWN = true;
		SOC soc = new SOC();
		File [] files = new File("scenarios").listFiles();
		for (File file : files) {
			System.out.println("Loading " + file);
			soc.loadFromFile(file);
			soc.saveToFile(file);
		}
	}
}
