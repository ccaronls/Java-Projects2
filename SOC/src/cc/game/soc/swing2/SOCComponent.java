package cc.game.soc.swing2;

import cc.game.soc.ui.UIComponent;
import cc.game.soc.ui.UIRenderer;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTGraphics;

/**
 * Created by chriscaron on 2/27/18.
 */

public class SOCComponent extends AWTComponent implements UIComponent {

    private UIRenderer delegate;

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        delegate.draw(g, mouseX, mouseY);
    }

    @Override
    public void redraw() {
        repaint();
    }

    @Override
    public void setRenderer(UIRenderer r) {
        this.delegate = r;
    }

    @Override
    protected void onClick() {
        delegate.doClick();
    }
}
