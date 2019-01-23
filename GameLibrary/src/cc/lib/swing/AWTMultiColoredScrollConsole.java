package cc.lib.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.LinkedList;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * This class allows for a console swing component where each console line
 * can be highlighted in its own color by prepending the line with a color code ala bash.
 * 
 * 
 * @author ccaron
 *
 */
@SuppressWarnings("serial")
public class AWTMultiColoredScrollConsole extends JList<AWTMultiColoredScrollConsole.Entry> implements ListCellRenderer<AWTMultiColoredScrollConsole.Entry>
{

    public static class Entry {
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
    private Color backgroundColor;
    
    private ListDataListener listener;

    public AWTMultiColoredScrollConsole() {
        this(null);
    }
    
    public AWTMultiColoredScrollConsole(Color bkColor) {
    	this.backgroundColor = bkColor;
        this.setOpaque(true);
        super.setVisibleRowCount(visibleLines);
        super.setCellRenderer(this);
        super.setModel(model);
    }

    private ListModel<Entry> model = new ListModel<Entry>() {
        
        @Override
    	public Entry getElementAt(int index) {
        	synchronized (lines) {
        		return lines.get(index);
        	}
    	}
    
    	@Override
    	public void addListDataListener(ListDataListener l) {
    		assert(listener == null);
    		listener = l;
    	}
    
    	@Override
    	public void removeListDataListener(ListDataListener l) {
    		assert(listener == l);
    		listener = null;
    	}

		@Override
		public int getSize() {
			synchronized (lines) {
				return lines.size();
			}
		}
    };
    
	public synchronized void addText(Color color, String text) {
		int width = getBounds().width;
		int scrollBarSize = ((Integer)UIManager.get("ScrollBar.width")).intValue();
		setFixedCellWidth(width);
//		System.out.println("width = " + width);
		String [] l = AWTUtils.generateWrappedLines(getGraphics(), text, width - scrollBarSize - 5);
		for (int i=l.length-1; i>=0; i--)
			lines.addFirst(new Entry(color, l[i]));
        while (lines.size() > maxRecordLines) {
            lines.removeLast();
            if (listener != null) {
            	listener.contentsChanged(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, lines.size()-1, lines.size()-1));
            }
        }
        if (listener != null) {
        	listener.contentsChanged(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, 0, 0));
        }
        super.revalidate();
    }   
    
    /**
     * Clear the console of all the lines of text
     */
    public void clear() {
        lines.clear();
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
	public Component getListCellRendererComponent(JList<? extends Entry> list,
			Entry value, int index, boolean isSelected, boolean cellHasFocus) {
		JLabel label = new JLabel();
		label.setForeground(value.color);
		label.setText(value.text);
		return label;
	}

	@Override
    public synchronized void paintComponent(Graphics g) {
		if (backgroundColor != null) {
			Rectangle rect = getBounds();
			g.setColor(backgroundColor);
			g.fillRect(rect.x, rect.y, rect.width, rect.height);
		}
        super.paintComponent(g);
    
	}
      
	
}