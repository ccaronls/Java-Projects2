package cc.game.soc.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.LinkedList;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class ScrollConsoleComponent extends JPanel
{

    class Entry {
        final Color color;
        final String text;
        Entry(Color color, String text) {
            this.color = color;
            this.text = text;
        }
    }
    LinkedList<Entry> lines = new LinkedList<Entry>();
    
    final int MAX_RECORD_LINES = 150;
    final int TEXT_HEIGHT = 16;
    final int VISIBLE_LINES = 8;
    final int MIN_HEIGHT = TEXT_HEIGHT * VISIBLE_LINES;
    
    private Color bkColor = null;
    
    ScrollConsoleComponent() {
        bkColor = GUIProperties.getInstance().getColorProperty("console.bkColor", getBackground());
        this.setOpaque(true);
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
    
    /**/
    public Dimension getTextSize() {
        Dimension d = super.getSize();
        d.height = Math.max(VISIBLE_LINES, lines.size()) * TEXT_HEIGHT;
        return d;
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
        int y = (startLine + 1) * TEXT_HEIGHT;
        for (int i=0; i < VISIBLE_LINES; i++) {
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
     */
    void addText(Color color, String text) {
        lines.addFirst(new Entry(color, text));
        if (lines.size() > MAX_RECORD_LINES)
            lines.removeLast();
        //setPreferredSize(getTextSize());
        revalidate();
        repaint();
    }   
    
    void clear() {
        lines.clear();
    }
}
