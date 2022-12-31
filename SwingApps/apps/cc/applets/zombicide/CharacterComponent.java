package cc.applets.zombicide;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.Scrollable;

import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTGraphics;
import cc.lib.ui.UIComponent;
import cc.lib.ui.UIRenderer;
import cc.lib.zombicide.ui.UIZCharacterRenderer;

class CharacterComponent extends AWTComponent implements Scrollable, UIComponent {

    CharacterComponent() {
        setPreferredSize(300, 200);
        setAutoscrolls(true);
    }

    @Override
    protected void init(AWTGraphics g) {
        //setMouseEnabled(true);
        int minHeight = (int)(g.getTextHeight() * 30);
        setPreferredSize(minHeight*2, minHeight);
        setMinimumSize(minHeight*2, minHeight);
        g.setTextHeight(14);
    }

    UIZCharacterRenderer renderer;

    @Override
    public void setRenderer(UIRenderer renderer) {
        this.renderer = (UIZCharacterRenderer)renderer;
    }

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        if (renderer != null)
            renderer.draw(g, mouseX, mouseY);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(getWidth(), Math.max(getHeight(), 200));
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 10;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 10;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
