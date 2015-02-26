package cc.fantasy.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

public class FGridPanel extends JScrollPane implements LayoutManager {
    
    int [] hSize;
    int [] vSize = new int[1];
    
    int curRow=0;
    int curCol=0;
    
    int hSpacing = 5;
    int vSpacing = 0;
    
    ArrayList<Component> components = new ArrayList();
    ArrayList<Integer>   spans = new ArrayList();

    JPanel content;
    
    FGridPanel(int cols) {
        hSize = new int[cols];
        content = new JPanel();
        content.setLayout(this);
        getViewport().add(content);
        setWheelScrollingEnabled(true);
        this.getVerticalScrollBar().setUnitIncrement(30);
        this.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    }

	public void add(int numStr) {
        add(String.valueOf(numStr));
    }

	public void add(float numStr) {
        add(String.valueOf(numStr));
    }

    public Component add(Component comp) {
        return add(comp, 1);
    }
    
    public Component add(Component comp, int span) {
        content.add(comp);
        components.add(comp);
        if (curCol + span > hSize.length) {
            span = hSize.length - curCol;
        }
        if (span < 1)
            span = 1;
        spans.add(span);
        return comp;
    }

    public void setBounds(int x, int y, int w, int h) {
    	compute();
        super.setBounds(x, y, w, h);
        float width = 0;
        for (int i=0; i<hSize.length; i++)
            width += hSize[i];
        // keep the percentage of the widths the same
        for (int i=0; i<hSize.length; i++) {
        	float size = hSize[i];
        	float percent = size / width;
        	size = percent * w;
        	hSize[i] = Math.round(size);
        }        
    }
    
    void compute() {
        
        Iterator<Component> it = components.iterator(); 
        int index = 0;
        while (it.hasNext()) {
        
            int span = spans.get(index++); 
            Component comp = it.next();
            Dimension size = comp.getPreferredSize();
            int hSz = 0;
            for (int i=0; i<span; i++)
                hSz += hSize[curCol+i];
            if (size.width > hSz) {
                int growSz = (size.width - hSz)/span + 5;
                if (growSz == 0)
                    growSz = 1;
                for (int i=0; i<span; i++) {
                    hSize[curCol+i] += growSz;
                }
            }
            if (size.height > vSize[curRow])
                vSize[curRow] = size.height;
            if (vSize[curRow] < 30)
                vSize[curRow] = 30;                
            curCol += span;
            if (curCol >= hSize.length) {
                curCol = 0;
                curRow += 1;
                if (curRow >= vSize.length) {
                    int [] newVSize = new int[curRow+1];
                    System.arraycopy(vSize, 0, newVSize, 0, vSize.length);
                    vSize = newVSize;
                }
            }
        }
        
        vSize[vSize.length-1] += 20;
    }

    public void add(String label) {
        add(new FLabel(label));
    }
    
    public void addHeader(String text) {
        add(new FLabel(text).setHeader2());     
    }
    
    public void add(Date date) {
        add(date == null ? "???" : Fantasy.instance.getDateString(date));
    }
    
    /*
     *  @Depricated
     *  (non-Javadoc)
     * @see java.awt.Container#setLayout(java.awt.LayoutManager)
     */
    public void setLayout(LayoutManager l) {
        super.setLayout(l);
    }

    public void addLayoutComponent(String arg0, Component arg1) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Invalid use of layout");
    }

    public void layoutContainer(Container arg0) {
        compute();
        Iterator<Component> it = components.iterator();
        // now set the bounds of all the components
        int col = 0;
        int row = 0;
        int x = arg0.getX();
        int y = arg0.getY();
        
        it = components.iterator();
        int index = 0;
        while (it.hasNext()) {
            Component comp = it.next();
            int span = spans.get(index++);
            int width = 0;
            for (int i=0; i<span; i++)
                width += hSize[col+i];
            int height = vSize[row];
            comp.setBounds(x, y, width, height);
            x += hSize[col];
            col += span;
            if (col >= hSize.length) {
                col = 0;
                x = arg0.getX();
                y += vSize[row];
                row += 1;
            }
        }
        
    }

    public Dimension minimumLayoutSize(Container arg0) {
        return preferredLayoutSize(arg0);
    }

    public Dimension preferredLayoutSize(Container arg0) {
        compute();
        int width = 0;
        int height = 0;
        for (int i=0; i<hSize.length; i++)
            width += hSize[i];
        for (int i=0; i<vSize.length; i++)
            height += vSize[i];
        Dimension d = new Dimension(width, height);
        content.setPreferredSize(d);
        return d;
    }

    public void removeLayoutComponent(Component arg0) {
        super.remove(arg0);
        components.remove(arg0);
    }

    
}
