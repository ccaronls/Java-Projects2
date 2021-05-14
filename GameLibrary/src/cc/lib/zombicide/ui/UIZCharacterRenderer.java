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

    interface IWrappable {

        GDimension drawWrapped(APGraphics g, float maxWidth);
    }

    static class StringLine implements IWrappable {

        final String msg;
        StringLine(String msg) {
            this.msg = msg;
        }

        @Override
        public GDimension drawWrapped(APGraphics g, float maxWidth) {
            return g.drawWrapString(0, 0, maxWidth, Justify.RIGHT, Justify.TOP, msg);
        }
    }

    public UIZCharacterRenderer(UIComponent component) {
        super(component);
    }

    private ZActor actorInfo = null;

    private LinkedList<IWrappable> messages = new LinkedList<>();

    private ZGame getGame() {
        return UIZombicide.getInstance();
    }

    synchronized void addMessage(String msg) {
        messages.addFirst(new StringLine(msg));
        while (messages.size() > 32) {
            messages.removeLast();
        }
        redraw();
    }

    synchronized void addWrappable(IWrappable line) {
        messages.addFirst(line);
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
        if (getGame() == null || UIZombicide.getInstance().boardRenderer == null)
            return;
        GDimension info = null;
        g.setColor(GColor.BLACK);
        if (UIZombicide.getInstance().boardRenderer.getHighlightedActor() != null) {
            actorInfo = UIZombicide.getInstance().boardRenderer.getHighlightedActor();
            info = UIZombicide.getInstance().boardRenderer.getHighlightedActor().drawInfo(g, getGame(), getWidth(), getHeight());
        } else if (getGame().getCurrentCharacter() != null) {
            info = getGame().getCurrentCharacter().drawInfo(g, getGame(), getWidth(), getHeight());
        } else if (actorInfo != null) {
            info = actorInfo.drawInfo(g, getGame(), getWidth(), getHeight());
        }

        g.setColor(GColor.BLACK);
        int y = 0;
        float maxWidth = g.getViewportWidth() - (info == null ? 0 : info.width);
        for (IWrappable msg : messages) {
            g.pushMatrix();
            g.translate(g.getViewportWidth(), y);
            GDimension d = msg.drawWrapped(g, maxWidth);
            //GDimension d = g.drawWrapString(g.getViewportWidth(), y, maxWidth, Justify.RIGHT, Justify.TOP, msg);
            g.popMatrix();
            g.setColor(GColor.TRANSLUSCENT_BLACK);
            y += d.getHeight();
        }
        if (info != null) {
            setMinDimension(getWidth(), (int) Math.max(info.height, y));
        }
    }

}
