package cc.game.soc.ui;

import cc.lib.game.APGraphics;
import cc.lib.game.GDimension;

/**
 * Created by chriscaron on 2/27/18.
 */

public abstract class UIRenderer {

    final UIComponent component;

    UIRenderer(UIComponent component) {
        this(component, true);
    }

    UIRenderer(UIComponent component, boolean attach) {
        this.component = component;
        if (attach)
            component.setRenderer(this);
    }

    public <T extends UIComponent> T getComponent() {
        return (T)component;
    }

    private GDimension min = new GDimension(32, 32);

    public abstract void draw(APGraphics g, int px, int py);

    public void doClick() {}

    public void startDrag(float x, float y) {}

    public void endDrag() {}

    public final GDimension getMinDimension() {
        return min;
    }

    public void setMinDimension(GDimension dim) {
        this.min = dim;
    }
}
