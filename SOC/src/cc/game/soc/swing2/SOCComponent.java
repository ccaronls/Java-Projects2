package cc.game.soc.swing2;

import java.awt.Point;

import cc.game.soc.ui.UIComponent;
import cc.game.soc.ui.UIRenderer;
import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTGraphics;

/**
 * Created by chriscaron on 2/27/18.
 */

public class SOCComponent extends AWTComponent implements UIComponent {

    private UIRenderer delegate;
    protected float progress = 1;

    SOCComponent() {
        setBackground(GColor.DARK_GRAY);
    }

    @Override
    protected final void paint(AWTGraphics g, int mouseX, int mouseY) {
        //g.clearScreen(GColor.DARK_GRAY);
        if (delegate != null) {
            delegate.draw(g, mouseX, mouseY);
            setMinimumSize(delegate.getMinDimension());
        } else {
            System.err.println("Missing delegate");
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

    @Override
    protected void init(final AWTGraphics g) {
        final Object [][] assets = getImagesToLoad();
        if (assets.length > 0) {
            progress = 0;
            new Thread() {
                public void run() {
                    g.addSearchPath("images");

                    int [] ids = new int[assets.length];
                    float delta = 1.0f / ids.length;

                    for (int i=0; i<ids.length; i++) {
                        ids[i] = g.loadImage((String)assets[i][0], (GColor)assets[i][1]);
                        progress += delta;
                    }

                    onImagesLoaded(ids);
                    progress = 1;
                }
            }.start();
        }
    }

    protected Object [][] getImagesToLoad() {
        return new Object[0][];
    }

    protected void onImagesLoaded(int [] ids) {
        throw new RuntimeException("onImagesLoaded not handled");
    }

    @Override
    protected final float getInitProgress() {
        return progress;
    }

    @Override
    public Vector2D getViewportLocation() {
        Point pt = super.getLocationOnScreen();
        return new Vector2D(pt.x, pt.y);
    }
}
