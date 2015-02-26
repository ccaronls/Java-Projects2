package cc.fantasy.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.SwingConstants;


public class FLabel extends JLabel {

	final static Color H1_BK_COLOR;// = Config.getColor("header1.bkcolor", new Color(200, 100, 200, 255));
	final static Color H1_TXT_COLOR;// = Color.ORANGE;
	final static Color H2_BK_COLOR;// = Config.getColor("header1.bkcolor", new Color(200, 100, 200, 255));
	final static Color H2_TXT_COLOR;// = Color.ORANGE;
	private static Font HEADER1_FONT = null;
	private static Font HEADER2_FONT = null;
	
	static {
		H1_BK_COLOR = Fantasy.instance.config.getColor("h1.bkcolor", Color.BLUE);
		H1_TXT_COLOR = Fantasy.instance.config.getColor("h1.txtcolor", Color.ORANGE);
		H2_BK_COLOR = Fantasy.instance.config.getColor("h1.bkcolor", Color.BLUE);
		H2_TXT_COLOR = Fantasy.instance.config.getColor("h1.txtcolor", Color.ORANGE);
	}
	
	private boolean header = false;
    private Font font = super.getFont();

    static Font getHeader1Font(Font font) {
    	if (HEADER1_FONT == null) {
    		HEADER1_FONT = font.deriveFont(AffineTransform.getScaleInstance(1.3, 1.8));
    	}
    	return HEADER1_FONT;
    }

    static Font getHeader2Font(Font font) {
    	if (HEADER2_FONT == null) {
    		HEADER2_FONT = font.deriveFont(AffineTransform.getScaleInstance(1.2, 1.5));
    	}
    	return HEADER2_FONT;
    }

	// primary cons, all other cons should pass through this(txt)
	FLabel(String txt) {
		super(txt);
	}

	FLabel(Date date) {
		this(Fantasy.instance.getDateString(date));
	}
	
	FLabel(int num) {
		this(String.valueOf(num));
	}
	
	FLabel() {
		this("");
	}
	
	FLabel setHeader1() {
		header = true;
        font = getHeader1Font(font);
        super.setBackground(H1_BK_COLOR);
        super.setForeground(H1_TXT_COLOR);
        super.setHorizontalAlignment(SwingConstants.CENTER);
        setOpaque(true);
		return this;
	}
	
	FLabel setHeader2() {
		header = true;
        font = getHeader2Font(font);
        super.setBackground(H2_BK_COLOR);
        super.setForeground(H2_TXT_COLOR);
        setOpaque(true);
        super.setHorizontalAlignment(SwingConstants.CENTER);
		return this;
	}

	@Override
	public Dimension getPreferredSize() {
        Dimension dim = super.getPreferredSize();
		if (header) {
			dim.width += 10;
			dim.height += 10;
		}
		return dim;
	}
    
    public Font getFont() {
        return font;
    }
    
	public void paint(Graphics g) {
        Rectangle r = g.getClipBounds();
        g.setColor(getBackground());
        g.fillRect(r.x, r.y, r.width, r.height);
        super.paint(g);
    }

	public void mouseClicked(MouseEvent arg0) {
		System.out.println(getText());
	}

	public void mouseEntered(MouseEvent arg0) {}
	public void mouseExited(MouseEvent arg0) {}
	public void mousePressed(MouseEvent arg0) {}
	public void mouseReleased(MouseEvent arg0) {}
}
