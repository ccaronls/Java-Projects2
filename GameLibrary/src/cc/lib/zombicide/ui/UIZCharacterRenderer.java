package cc.lib.zombicide.ui;

import java.util.LinkedList;

import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.Justify;
import cc.lib.ui.UIComponent;
import cc.lib.ui.UIRenderer;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZGame;

public class UIZCharacterRenderer extends UIRenderer {

    public UIZCharacterRenderer(UIComponent component) {
        super(component);
    }

    private ZActor actorInfo = null;

    private LinkedList<String> messages = new LinkedList<>();

    private ZGame getGame() {
        return UIZombicide.getInstance();
    }

    synchronized void addMessage(String msg) {
        messages.addFirst(msg);
        while (messages.size() > 32) {
            messages.removeLast();
        }
        redraw();
    }

    synchronized void clearMessages() {
        messages.clear();
    }

    @Override
    public synchronized void draw(APGraphics g, int px, int py) {
        if (getGame() == null)
            return;
        GDimension info = null;
        g.setColor(GColor.BLACK);
        if (UIZombicide.getInstance().getHighlightedActor() != null) {
            actorInfo = UIZombicide.getInstance().getHighlightedActor();
            info = UIZombicide.getInstance().getHighlightedActor().drawInfo(g, getGame(), getWidth(), getHeight());
        } else if (getGame().getCurrentCharacter() != null) {
            info = getGame().getCurrentCharacter().drawInfo(g, getGame(), getWidth(), getHeight());
        } else if (actorInfo != null) {
            info = actorInfo.drawInfo(g, getGame(), getWidth(), getHeight());
        }

        g.setColor(GColor.BLACK);
        int y = 0;
        float maxWidth = g.getViewportWidth() - (info == null ? 0 : info.width);
        for (String msg : messages) {
            GDimension d = g.drawWrapString(g.getViewportWidth(), y, maxWidth, Justify.RIGHT, Justify.TOP, msg);
            g.setColor(GColor.TRANSLUSCENT_BLACK);
            y += d.getHeight();
        }
        if (info != null) {
            setMinDimension(getWidth(), (int) Math.max(info.height, y));
        }
    }

}
