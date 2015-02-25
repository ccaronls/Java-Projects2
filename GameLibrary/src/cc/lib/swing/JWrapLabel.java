package cc.lib.swing;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.JLabel;

import cc.lib.game.Justify;

@SuppressWarnings("serial")
public class JWrapLabel extends JLabel {

    String [] lines;
    int maxLineChars = 25;
    
    public JWrapLabel() {
        super();
        lines = new String[0];
    }

    public JWrapLabel(String arg0) {
        super(arg0);
        setText(arg0);
    }

    @Override
    public void paint(Graphics g) {
        if (lines == null) {
            lines = AWTUtils.generateWrappedLines(g, getText(), getWidth());
            validate();
        }
        int fontHeight = g.getFontMetrics().getHeight();
        Font f = g.getFont().deriveFont(Font.BOLD);
        g.setFont(f);
        int x = 0;//getX();
        int y = 0;//getY();
        for (int i=0; i<lines.length; i++) {
            AWTUtils.drawJustifiedString(g, x, y, Justify.LEFT, Justify.TOP, lines[i]);
            y += fontHeight;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (lines == null)
            return super.getPreferredSize();
        Dimension min = super.getPreferredSize();
        min.setSize(min.getWidth(), Math.max(maxLineChars * lines.length, min.getHeight()));
        return min;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        lines = null;
    }

    @Override
    public void setText(String arg0) {
        super.setText(arg0);
        lines = null;
    }
    
    
    
}
