package cc.game.soc.swing2;

import cc.game.soc.ui.UIComponent;
import cc.game.soc.ui.UIRenderer;
import cc.lib.game.Utils;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTGraphics;

/**
 * Created by chriscaron on 2/27/18.
 */

public class SOCComponent extends AWTComponent implements UIComponent {

    private UIRenderer delegate;

    @Override
    protected final void paint(AWTGraphics g, int mouseX, int mouseY) {
        if (delegate != null)
            delegate.draw(g, mouseX, mouseY);
        else {
            Utils.print("Missing delegate");
        }
    }

    @Override
    public final void redraw() {
        repaint();
    }

    @Override
    public final void setRenderer(UIRenderer r) {
        this.delegate = r;
    }

    @Override
    protected final void onClick() {
        delegate.doClick();
    }

    @Override
    protected final void startDrag(int x, int y) {
        delegate.startDrag(x, y);
    }

    @Override
    protected final void stopDrag() {
        delegate.endDrag();
    }
}
