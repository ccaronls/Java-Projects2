package cc.game.soc.swing;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JComponent;

import cc.game.soc.core.SOC;
import cc.lib.game.Justify;
import cc.lib.swing.AWTUtils;
import cc.lib.swing.ImageMgr;

public class BarbarianComponent extends JComponent {

	final int imageDefault;
	final int [] images;
	
	BarbarianComponent() {
		ImageMgr mgr = GUI.instance.images;
		imageDefault = mgr.loadImage("barbarians_tile.GIF");
		images = new int[8];
		for (int i=0; i<8; i++) {
			images[i] = mgr.loadImage("barbarians_tile" + i + ".GIF");
		}
	}
	
	@Override
	public void paint(Graphics g) {
		SOC soc = GUI.instance.getSOC();
		
		int numStepsAway = soc.getBarbarianDistance();
		int image = imageDefault;
		if (numStepsAway >= 0 && numStepsAway < images.length) {
			image = images[numStepsAway];
		}
		GUI.instance.images.drawImage(g, image, 0, 0, getWidth(), getHeight());
		
		String text = "Barbarians: " + SOC.computeBarbarianStrength(soc, soc.getBoard())
				    + "\nCatan: " + SOC.computeCatanStrength(soc, soc.getBoard());
		g.setColor(Color.BLACK);
		AWTUtils.drawJustifiedString(g, 10, 10, Justify.LEFT, Justify.TOP, text);
	}
}
