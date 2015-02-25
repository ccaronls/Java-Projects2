package cc.game.soc.swing;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.JLabel;

import cc.lib.game.Utils;

@SuppressWarnings("serial")
public class JWrapLabel extends JLabel {

    private String [] lines;
    private int maxLineChars = 25;
    
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
            lines = Utils.generateWrappedLines(g, getText(), getWidth());
            validate();
        }
        int fontHeight = g.getFontMetrics().getHeight();
        g.setFont(g.getFont().deriveFont(Font.BOLD));
        int x = 0, y = 0;
        for (int i=0; i<lines.length; i++) {
            Utils.drawJustifiedString(g, x, y, lines[i]);
            y += fontHeight;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (lines == null)
            return super.getPreferredSize();
        Dimension min = super.getPreferredSize();
        min.setSize(min.getWidth(), Math.max(maxVisibleLines * lines.length, min.getHeight()));
        return min;
    }
    
    public void setMaxVisibleLines(int maxLines) {
        this.maxVisibleLines = maxLines;
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
