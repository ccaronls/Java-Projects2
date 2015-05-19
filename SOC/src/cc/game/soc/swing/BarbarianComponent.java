package cc.game.soc.swing;

import java.awt.Graphics;

import javax.swing.JComponent;

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
		int numStepsAway = GUI.instance.getSOC().getBarbarianDistance();
		int image = imageDefault;
		if (numStepsAway >= 0 && numStepsAway < images.length) {
			image = images[numStepsAway];
		}
		GUI.instance.images.drawImage(g, image, 0, 0, getWidth(), getHeight());
	}
}
