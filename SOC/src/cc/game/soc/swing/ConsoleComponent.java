package cc.game.soc.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JComponent;

import cc.lib.swing.AWTUtils;

@SuppressWarnings("serial")
public class ConsoleComponent extends JComponent {

	private String text = "";	
	
	private final static int MIN_HEIGHT = 16 * 8;
    
    private Color bkColor = null;
	
	ConsoleComponent(int width) {
        bkColor = GUI.instance.getProps().getColorProperty("console.bkColor", getBackground());
		this.setPreferredSize(new Dimension(width, MIN_HEIGHT));
	}
	
	/*
	 *  (non-Javadoc)
	 * @see java.awt.Component#getMinimumSize()
	 *
	public Dimension getMinimumSize() {
		Dimension d = super.getMinimumSize();
		d.height = MIN_HEIGHT;
		return d;
	}
	
	/*
	 *  (non-Javadoc)
	 * @see java.awt.Component#paint(java.awt.Graphics)
	 */
	public void paint(Graphics g) {
		final int fontHeight = g.getFontMetrics().getHeight();
		int x = getX();
		int y = 0 + fontHeight;
		g.setColor(bkColor);
		g.fillRect(x, 0, getWidth(), getHeight());
		final int maxY = y + getHeight() - fontHeight;
		final int maxWidth = getWidth(); 
		String t = text.trim();
		int totalUsed = 0;
		while (t.length() > 0 && y < maxY) {
			if (t.charAt(0) == '[') {
				int right = t.indexOf(']');
				String colorStr = t.substring(1, right);
                g.setColor(AWTUtils.stringToColor(colorStr));
				t = t.substring(right+1);
				//String [] comps = colorStr.split(",");
				//int R = Integer.parseInt(comps[0]);
				//int G = Integer.parseInt(comps[1]);
				//int B = Integer.parseInt(comps[2]);
				//g.setColor(new Color(R,G,B));
			}
			Rectangle r = AWTUtils.drawWrapString(g, x, y, maxWidth, t);
			int len = r.width;
			totalUsed += len;
			if (len >= t.length())
				break;
			t = t.substring(len).trim();
			y += fontHeight;
		}
		
		// anything left over gets chopped
		if (totalUsed > text.length()) {
			text = text.substring(0, totalUsed);
		}
	}
	
	void addText(Color color, String text) {
		String t = this.text;
		text = "[" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "]" + text;
		this.text = text + "\n" + t;
		repaint();
	}	
	
	void clear() {
		text = "";
	}
	
	
}
