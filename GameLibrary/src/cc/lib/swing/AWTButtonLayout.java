package cc.lib.swing;

import java.awt.*;

/**
 * Created by chriscaron on 4/18/18.
 */

public class AWTButtonLayout implements LayoutManager2 {

    @Override
    public synchronized void invalidateLayout(Container target) {
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {

    }

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
    }

    public AWTButtonLayout() {}

    public AWTButtonLayout(Container target) {
        layoutContainer(target);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        Dimension size = new Dimension();
        int nComponents = target.getComponentCount();
        if (heights.length != nComponents) {
            heights = new int[nComponents];
        }
        for (int i=0; i<nComponents; i++) {
            Component c = target.getComponent(i);
            Dimension d = c.getPreferredSize();
            size.width = Math.max(size.width, d.width);
            size.height += d.height;
            heights[i] = d.height;
        }
        Insets insets = target.getInsets();
        size.width = (int) Math.min((long) size.width + (long) insets.left + (long) insets.right, Integer.MAX_VALUE);
        size.height = (int) Math.min((long) size.height + (long) insets.top + (long) insets.bottom, Integer.MAX_VALUE);
        return size;
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension size = new Dimension();
        int nComponents = target.getComponentCount();
        if (heights.length != nComponents) {
            heights = new int[nComponents];
        }
        for (int i=0; i<nComponents; i++) {
            Component c = target.getComponent(i);
            Dimension d = c.getMinimumSize();
            size.width = Math.max(size.width, d.width);
            size.height += d.height;
            heights[i] = d.height;
        }
        Insets insets = target.getInsets();
        size.width = (int) Math.min((long) size.width + (long) insets.left + (long) insets.right, Integer.MAX_VALUE);
        size.height = (int) Math.min((long) size.height + (long) insets.top + (long) insets.bottom, Integer.MAX_VALUE);
        return size;
    }


    @Override
    public Dimension maximumLayoutSize(Container target) {
        Dimension size = new Dimension();
        int nComponents = target.getComponentCount();
        if (heights.length != nComponents) {
            heights = new int[nComponents];
        }
        for (int i=0; i<nComponents; i++) {
            Component c = target.getComponent(i);
            Dimension d = c.getMaximumSize();
            size.width = Math.max(size.width, d.width);
            size.height += d.height;
            heights[i] = d.height;
        }
        Insets insets = target.getInsets();
        size.width = (int) Math.min((long) size.width + (long) insets.left + (long) insets.right, Integer.MAX_VALUE);
        size.height = (int) Math.min((long) size.height + (long) insets.top + (long) insets.bottom, Integer.MAX_VALUE);
        return size;
    }

    @Override
    public synchronized float getLayoutAlignmentX(Container target) {
        return 0;
    }

    @Override
    public synchronized float getLayoutAlignmentY(Container target) {
        return 0;
    }

    @Override
    public void layoutContainer(Container target) {
        int nChildren = target.getComponentCount();

        Dimension alloc = target.getSize();
        Insets in = target.getInsets();
        alloc.width -= in.left + in.right;
        alloc.height -= in.top + in.bottom;

        // flush changes to the container
        int x = in.left;
        int y = in.top;
        for (int i = 0; i < nChildren; i++) {
            Component c = target.getComponent(i);
            int h = heights[i];
            c.setBounds(x, y, alloc.width, h);
            y += h;
        }

    }

    int [] heights = new int[0];
}