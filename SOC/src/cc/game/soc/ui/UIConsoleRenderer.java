package cc.game.soc.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import cc.lib.game.APGraphics;
import cc.lib.game.GColor;

@SuppressWarnings("serial")
public abstract class UIConsoleRenderer implements UIRenderer {

    static class Line {
        final String text;
        final GColor color;

        public Line(String text, GColor color) {
            this.text = text;
            this.color = color;
        }
    }

    final UIComponent component;

    public UIConsoleRenderer(UIComponent component) {
        this.component = component;
    }

    private final LinkedList<Line> lines = new LinkedList<>();

	/*
	 *  (non-Javadoc)
	 * @see java.awt.Component#paint(java.awt.Graphics)
	 */
	public void draw(APGraphics g, int pickX, int pickY) {

	    final float padding = 5;
	    final int txtHgt = g.getTextHeight();
	    float y = 0;
        Iterator<Line> it = lines.iterator();
	    while (it.hasNext()) {
	        Line l = it.next();
	        g.setColor(l.color);
	        y += g.drawWrapString(padding, y, component.getWidth()-padding*2, l.text);
	        if (y > component.getHeight()) {
	            break;
            }
        }

        while (it.hasNext()) {
	        it.remove();
        }

	}
	
	public final void addText(GColor color, String text) {
	    lines.addFirst(new Line(text, color));
		component.redraw();
	}	
	
	public final void clear() {
	    lines.clear();
        component.redraw();
	}
	
	
}
