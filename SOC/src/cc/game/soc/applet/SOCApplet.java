package cc.game.soc.applet;

import javax.swing.JApplet;

import cc.game.soc.swing.GUI;
import cc.game.soc.swing.GUIProperties;

@SuppressWarnings("serial")
public class SOCApplet extends JApplet {

	public SOCApplet() {
		try {
			new GUI(this, new GUIProperties());
			start();
		} catch (Exception e) {
			System.err.println("Applet failed to load");
			e.printStackTrace();
		}
	}
	
}
