package cc.game.soc.ui;

import java.util.LinkedList;

import cc.lib.game.AAnimation;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.Utils;
import cc.lib.utils.QueueRunner;

@SuppressWarnings("serial")
public final class UIConsoleRenderer extends UIRenderer {

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
    private boolean maxVisibleLinesSet = false;

    public UIConsoleRenderer(UIComponent component) {
        super(component);
    }

    private final LinkedList<Line> lines = new LinkedList<>();

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
	    final int txtHgt = g.getTextHeight();
	    if (!maxVisibleLinesSet)
	        maxVisibleLines = component.getHeight() / txtHgt;
        float y = 0;
	    for (int i=startLine; i<lines.size(); i++) {
	        Line l = lines.get(i);
            g.setColor(l.color);
            g.setTextHeight(RenderConstants.textSizeSmall);
            GDimension dim = g.drawWrapString(0, y, component.getWidth(), l.text);
            y += dim.height;
            if (y > component.getHeight()) {
                break;
            }
        }
        if  (maxVisibleLinesSet) {
	        setMinDimension(new GDimension(component.getWidth(), Math.min(Math.max(1, lines.size()), maxVisibleLines)*txtHgt));
        }
	}

	public void setMaxVisibleLines(int max) {
	    this.maxVisibleLines = max;
	    maxVisibleLinesSet = true;
    }

	private AAnimation<APGraphics> anim = null;

    private QueueRunner<Line> queue = new QueueRunner<Line>() {
        @Override
        protected void process(final Line item) {
            final String text = item.text;
            final GColor color = item.color;
            if (startLine > 0) {

                // if user is scrolling, then show this line at top with a fade out.
                anim = new AAnimation<APGraphics>(500) {
                    @Override
                    protected void draw(APGraphics g, float position, float dt) {
                        drawPrivate(g);
                        g.setColor(GColor.BLACK.withAlpha(0.5f-position));
                        String [] lines = g.generateWrappedLines(text, component.getWidth());
                        g.drawFilledRect(0, 0, component.getWidth(), lines.length*g.getTextHeight());
                        g.setColor(color.withAlpha(1.0f-position/3));
                        float y = 0;
                        for (String l : lines) {
                            g.drawString(l, 0, y);
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
                        GDimension dim = g.drawWrapString(0, 0, component.getWidth(), text);
                        g.translate(0, dim.height*position);
                        drawPrivate(g);
                        g.popMatrix();
                    }
                }.start();
            }

            component.redraw();
            Utils.waitNoThrow(anim, anim.getDuration());
            lines.addFirst(item);
            if (startLine > 0)
                startLine++;
            while (lines.size() > 100) {
                lines.removeLast();
            }
            component.redraw();
        }
    };

	public synchronized final void addText(final GColor color, final String text) {
        queue.add(new Line(text, color));
    }

	public final void clear() {
        queue.clear();
	    lines.clear();
        component.redraw();
	}

}
