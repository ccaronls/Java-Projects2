package cc.lib.zombicide.ui;

import java.util.LinkedList;

import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.Justify;
import cc.lib.ui.UIComponent;
import cc.lib.ui.UIRenderer;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZQuest;

public class UIZCharacterRenderer extends UIRenderer {

    interface IWrappable {
        GDimension drawWrapped(APGraphics g, float maxWidth, boolean dimmed);
    }

    private GColor textColor = GColor.BLACK;

    static class StringLine implements IWrappable {

        final String msg;
        final GColor color;

        StringLine(GColor color, String msg) {
            this.msg = msg;
            this.color = color;
        }

        @Override
        public GDimension drawWrapped(APGraphics g, float maxWidth, boolean dimmed) {
            g.setColor(dimmed ? color.withAlpha(.5f) : color);
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

    public void setTextColor(GColor textColor) {
        this.textColor = textColor;
    }

    public synchronized void addMessage(String msg) {
        addMessage(msg, textColor);
    }

    public synchronized void addMessage(String msg, GColor color) {
        messages.addFirst(new StringLine(color, msg));
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
        g.setColor(textColor);
        if (UIZombicide.getInstance().boardRenderer.getHighlightedActor() != null) {
            actorInfo = UIZombicide.getInstance().boardRenderer.getHighlightedActor();
            info = UIZombicide.getInstance().boardRenderer.getHighlightedActor().drawInfo(g, getGame(), getWidth(), getHeight());
        } else if (getGame().getCurrentCharacter() != null) {
            info = getGame().getCurrentCharacter().getCharacter().drawInfo(g, getGame(), getWidth(), getHeight());
        } else if (actorInfo != null) {
            info = actorInfo.drawInfo(g, getGame(), getWidth(), getHeight());
        } else if (getGame().getQuest() != null) {
            ZQuest quest = getGame().getQuest();
            Table table = new Table(new Table.Model() {
                @Override
                public int getMaxCharsPerLine() {
                    return 128;
                }
            }).addColumn(quest.getName(), quest.getQuest().getDescription().replace('\n', ' '));
            info = table.draw(g);
        }

        g.setColor(textColor);
        int y = 0;
        float maxWidth = g.getViewportWidth() - (info == null ? 0 : info.width);
        boolean dimmed = false;
        for (IWrappable msg : messages) {
            g.pushMatrix();
            g.translate(g.getViewportWidth(), y);
            GDimension d = msg.drawWrapped(g, maxWidth, dimmed);
            //GDimension d = g.drawWrapString(g.getViewportWidth(), y, maxWidth, Justify.RIGHT, Justify.TOP, msg);
            g.popMatrix();
            dimmed = true;
            y += d.getHeight();
        }
        if (info != null) {
            setMinDimension(getWidth(), (int) Math.max(info.height, y));
        }
    }

}
