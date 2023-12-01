package cc.lib.ui;

import org.jetbrains.annotations.NotNull;

import cc.lib.game.APGraphics;
import cc.lib.game.GDimension;
import cc.lib.game.IDimension;
import cc.lib.game.Renderable;

/**
 * Created by chriscaron on 2/27/18.
 * <p>
 * Provides a generalized interface for application ui elements that render
 */
public abstract class UIRenderer implements IDimension, Renderable {

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

    @NotNull
    public <T extends UIComponent> T getComponent() {
        return (T) component;
    }

    private GDimension min = new GDimension(32, 32);

    public abstract void draw(APGraphics g, int px, int py);

    public void onTouch(int x, int y) {
    }

    public void onTouchUp(int x, int y) {
    }

    public void onClick() {
    }

    public void onDragStart(int x, int y) {
    }

    public void onDragMove(int x, int y) {
    }

    public void onDragEnd() {
    }

    public void onSizeChanged(int w, int h) {
    }

    public void onZoom(float scale) {
    }

    public void onFocusChanged(boolean gained) {
    }

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

    @Override
    public int getViewportWidth() {
        return component.getWidth();
    }

    @Override
    public int getViewportHeight() {
        return component.getHeight();
    }

    public float getViewportAspect() {
        return (float) getViewportWidth() / getViewportHeight();
    }
}
