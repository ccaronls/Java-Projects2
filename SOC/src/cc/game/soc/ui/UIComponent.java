package cc.game.soc.ui;

import cc.lib.game.APGraphics;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;

/**
 * Created by chriscaron on 2/23/18.
 *
 * interface for a cross-platform component.
 */

public interface UIComponent {

    /**
     * the physical width of this component in renderable units
     * @return
     */
    int getWidth();

    /**
     * the physical height of this component in renderable units
     * @return
     */
    int getHeight();

    /**
     * trigger the component to redraw itself
     */
    void redraw();

    /**
     * Take an object to act as the render delegate
     * @param r
     */
    void setRenderer(UIRenderer r);

}