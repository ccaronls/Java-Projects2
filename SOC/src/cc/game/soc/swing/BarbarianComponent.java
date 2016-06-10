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
		
		int barbStr = SOC.computeBarbarianStrength(soc, soc.getBoard());
		int catanStr = SOC.computeCatanStrength(soc, soc.getBoard());
		
		String text = "Barbarians: " + barbStr
				    + "\nCatan: " + catanStr;
		if (catanStr >= barbStr)
			g.setColor(Color.GREEN);
		else
			g.setColor(Color.RED);
		
		AWTUtils.drawWrapJustifiedStringOnBackground(g, 3, 3, getWidth(), 3, Justify.LEFT, Justify.TOP, text, AWTUtils.TRANSLUSCENT_BLACK);
	}
}
