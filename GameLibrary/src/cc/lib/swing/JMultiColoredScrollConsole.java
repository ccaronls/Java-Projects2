package cc.lib.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.LinkedList;

import javax.swing.JPanel;

/**
 * This class allows for a console swing component where each console line
 * can be highlighted in its own color by prepending the line with a color code ala bash.
 * 
 * 
 * @author ccaron
 *
 */
@SuppressWarnings("serial")
public class JMultiColoredScrollConsole extends JPanel
{

    private class Entry {
        final Color color;
        final String text;
        Entry(Color color, String text) {
            this.color = color;
            this.text = text;
        }
    }
    private LinkedList<Entry> lines = new LinkedList<Entry>();
    
    private int maxRecordLines = 150;
    private int textHeight = 16;
    private int visibleLines = 8;
    //private int MIN_HEIGHT = textHeight * visibleLines;
    
    private Color bkColor = null;
    
    public JMultiColoredScrollConsole() {
        bkColor = this.getBackground();
    }
    
    public JMultiColoredScrollConsole(Color bkColor) {
        this.bkColor = bkColor;
        this.setOpaque(true);
    }

    public Dimension getTextSize() {
        Dimension d = super.getSize();
        d.height = Math.max(visibleLines, lines.size()) * textHeight;
        return d;
    }

    public int getMaxRecordLines() {
        return maxRecordLines;
    }

    public void setMaxRecordLines(int maxRecordLines) {
        this.maxRecordLines = maxRecordLines;
    }

    public int getTextHeight() {
        return textHeight;
    }

    public void setTextHeight(int textHeight) {
        this.textHeight = textHeight;
    }

    public int getVisibleLines() {
        return visibleLines;
    }

    public void setVisibleLines(int visibleLines) {
        this.visibleLines = visibleLines;
    }

    @Override
    public Dimension getPreferredSize() {
        return getTextSize();
    }


    @Override
    public synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (lines.size() == 0)
            return;
        final int fontHeight = g.getFontMetrics().getHeight();
        Font bold = g.getFont().deriveFont(Font.BOLD);
        g.setFont(bold);
        int x = getX();
        g.setColor(bkColor);
        g.fillRect(x, 0, getWidth(), getHeight());
        Rectangle rect = this.getVisibleRect();
        int startLine = rect.y  / fontHeight;
        int y = (startLine + 1) * textHeight;
        for (int i=0; i < visibleLines; i++) {
            if (i + startLine >= lines.size())
                break;
            Entry line = lines.get(startLine + i);
            g.setColor(line.color);
            g.drawString(String.valueOf(startLine+i) + ":" + line.text, x, y);
            y += fontHeight;
        }
    }

    /**
     * 
     * @param color
     * @param text
     */
    public synchronized void addText(Color color, String text) {
        lines.addFirst(new Entry(color, text));
        while (lines.size() > maxRecordLines)
            lines.removeLast();
        //setPreferredSize(getTextSize());
        revalidate();
        repaint();
    }   
    
    /**
     * Clear the console of all the lines of text
     */
    public void clear() {
        lines.clear();
    }
}
