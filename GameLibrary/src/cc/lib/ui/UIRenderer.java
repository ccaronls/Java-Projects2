package cc.lib.ui;

import cc.lib.game.APGraphics;
import cc.lib.game.GDimension;
import cc.lib.game.IDimension;

/**
 * Created by chriscaron on 2/27/18.
 *
 * Provides a generalized interface for application ui elements that render
 */
public abstract class UIRenderer implements IDimension {

    private final UIComponent component;

    protected UIRenderer(UIComponent component) {
        this(component, true);
    }

    protected UIRenderer(UIComponent component, boolean attach) {
        this.component = component;
        if (component == null)
            throw new NullPointerException("Component cannot be null");
        if (attach)
            component.setRenderer(this);
    }

    public <T extends UIComponent> T getComponent() {
        return (T)component;
    }

    private GDimension min = new GDimension(32, 32);

    public abstract void draw(APGraphics g, int px, int py);

    public void onClick() {}

    public void onDragStart(float x, float y) {}

    public void onDragMove(float x, float y) {}

    public void onDragEnd() {}

    public final GDimension getMinDimension() {
        return min;
    }

    public void setMinDimension(GDimension dim) {
        this.min = dim;
    }

    public void setMinDimension(float w, float h) {
        this.min = new GDimension(w, h);
    }

    public float getWidth() {
        return component.getWidth();
    }

    public float getHeight() {
        return component.getHeight();
    }

    public void redraw() {
        component.redraw();
    }
}
