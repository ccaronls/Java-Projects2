package cc.game.soc.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import cc.lib.game.APGraphics;
import cc.lib.game.GColor;

@SuppressWarnings("serial")
public final class UIConsoleRenderer implements UIRenderer {

    static class Line {
        final String text;
        final GColor color;

        public Line(String text, GColor color) {
            this.text = text;
            this.color = color;
        }
    }

    private int startLine = 0;
    private int maxVisibleLines = 1;

    private final UIComponent component;

    public UIConsoleRenderer(UIComponent component) {
        this.component = component;
        component.setRenderer(this);
    }

    private final LinkedList<Line> lines = new LinkedList<>();
    private GColor bkColor = GColor.TRANSPARENT;

    public void initStyles(GColor bkColor) {
        this.bkColor = bkColor;
    }

    public void scroll(int numLines) {
        startLine += numLines;
        if (startLine > lines.size()-maxVisibleLines) {
            startLine = lines.size()-maxVisibleLines;
        }
        if (startLine < 0)
            startLine = 0;
        component.redraw();
    }

    public void scrollToTop() {
        startLine = 0;
        component.redraw();
    }

	/*
	 *  (non-Javadoc)
	 * @see java.awt.Component#paint(java.awt.Graphics)
	 */
	public void draw(APGraphics g, int pickX, int pickY) {
        g.clearScreen(bkColor);
	    final float padding = 5;
	    final int txtHgt = g.getTextHeight();
	    float y = 0;
	    maxVisibleLines = component.getHeight() / txtHgt;
	    for (int i=startLine; i<lines.size(); i++) {
	        Line l = lines.get(i);
            g.setColor(l.color);
            y += g.drawWrapString(padding, y, component.getWidth()-padding*2, l.text);
            if (y > component.getHeight()) {
                break;
            }
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

    @Override
    public void doClick() {

    }

}
