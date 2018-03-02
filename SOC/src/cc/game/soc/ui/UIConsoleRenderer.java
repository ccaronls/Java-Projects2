package cc.game.soc.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;

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
    private float padding = 5;

    private final UIComponent component;

    public UIConsoleRenderer(UIComponent component) {
        this.component = component;
        component.setRenderer(this);
    }

    public void setStyles(float padding) {
        this.padding = padding;
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
        if (anim != null) {
            if (anim.isDone()) {
                anim = null;
            } else {
                anim.update(g);
                component.redraw();
                return;
            }
        }
        drawPrivate(g);
    }

    private void drawPrivate(APGraphics g) {
        g.clearScreen(bkColor);
	    final int txtHgt = g.getTextHeight();
	    float y = padding;
	    maxVisibleLines = component.getHeight() / txtHgt;
	    for (int i=startLine; i<lines.size(); i++) {
	        Line l = lines.get(i);
            g.setColor(l.color);
            GDimension dim = g.drawWrapString(padding, y, component.getWidth()-padding*2, l.text);
            y += dim.height;
            if (y > component.getHeight()) {
                break;
            }
        }
	}

	private AAnimation<APGraphics> anim = null;

	public final void addText(final GColor color, final String text) {
	    if (startLine > 0) {
	        // if user is scrolling, then show this line at top with a fade out.
	        anim = new AAnimation<APGraphics>(1000) {
                @Override
                protected void draw(APGraphics g, float position, float dt) {
                    drawPrivate(g);
                    g.setColor(bkColor);//GColor.BLACK.withAlpha(0.5f-position));
                    String [] lines = g.generateWrappedLines(text, component.getWidth()-2*padding);
                    g.drawFilledRectf(0, 0, component.getWidth(), lines.length*g.getTextHeight()+2*padding);
                    g.setColor(color.withAlpha(1.0f-position/3));
                    float y = padding;
                    for (String l : lines) {
                        g.drawString(l, padding, y);
                        y += g.getTextHeight();
                    }
                }
            }.start();
        } else {
	        // if user not scrolling, then show this line with the rest of lines tracing downward
            anim = new AAnimation<APGraphics>(500) {
                @Override
                protected void draw(APGraphics g, float position, float dt) {
                    g.pushMatrix();
                    g.setColor(color.withAlpha(position));
                    GDimension dim = g.drawWrapString(padding, padding, component.getWidth()-2*padding, text);
                    g.translate(0, dim.height*position);
                    drawPrivate(g);
                    g.popMatrix();
                }
            }.start();
        }

        lines.addFirst(new Line(text, color));
	    if (startLine > 0)
	        startLine++;
	    component.redraw();
	}
	
	public final void clear() {
	    lines.clear();
        component.redraw();
	}

    @Override
    public void doClick() {

    }

    @Override
    public void startDrag(float x, float y) {

    }

    @Override
    public void endDrag() {

    }

}
