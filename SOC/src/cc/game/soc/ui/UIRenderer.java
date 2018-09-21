package cc.game.soc.ui;

import cc.game.soc.core.StringResource;
import cc.lib.ui.UIComponent;

/**
 * Created by chriscaron on 2/27/18.
 */

public abstract class UIRenderer extends cc.lib.ui.UIRenderer implements StringResource {

    UIRenderer(UIComponent component) {
        this(component, true);
    }

    UIRenderer(UIComponent component, boolean attach) {
        super(component, attach);
    }

    @Override
    public String getString(int resourceId, Object... args) {
        if (UISOC.getInstance() != null) {
            return UISOC.getInstance().getString(resourceId, args);
        }
        return "NO STRINGS";
    }
}
