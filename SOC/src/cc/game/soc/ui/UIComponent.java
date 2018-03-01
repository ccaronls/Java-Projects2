package cc.game.soc.ui;

import cc.lib.game.APGraphics;

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

    /**
     * Tell component to resize itself if neccessary
     *
     * @param width
     * @param height
     */
    void setMinSize(int width, int height);

    /**
     *
     * @param x
     * @param y
     * @param w
     * @param h
     */
    void setBounds(float x, float y, float w, float h);
}
