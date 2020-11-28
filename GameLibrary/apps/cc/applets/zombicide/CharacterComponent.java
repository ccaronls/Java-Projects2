package cc.applets.zombicide;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.LinkedList;

import javax.swing.Scrollable;

import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.Justify;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTGraphics;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZGame;

class CharacterComponent extends AWTComponent implements Scrollable {

    CharacterComponent() {
        setPreferredSize(300, 200);
        setAutoscrolls(true);
    }

    LinkedList<String> messages = new LinkedList<>();

    ZGame getGame() {
        return ZombicideApplet.instance.game;
    }

    synchronized void addMessage(String msg) {
        messages.addFirst(msg);
        while (messages.size() > 32) {
            messages.removeLast();
        }
        repaint();
    }


    @Override
    protected void init(AWTGraphics g) {
        setMouseEnabled(true);
        int minHeight = g.getTextHeight() * 12;
        setPreferredSize(minHeight*2, minHeight);
        setMinimumSize(minHeight*2, minHeight);
    }

    Font courier = null;
    ZActor actorInfo = null;

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        g.clearScreen();
        if (getGame() == null)
            return;
        if (courier == null) {
            // this did not work
            //((AWTGraphics2)g).getGraphics2D().setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            courier = new Font("Monospaced", Font.PLAIN, g.getTextHeight());
        }
        g.getGraphics().setFont(courier);//Font.decode(Font.MONOSPACED));//.deriveFont(Font.BOLD));
        GDimension info = null;
        if (ZombicideApplet.instance.boardComp.highlightedActor != null) {
            actorInfo = ZombicideApplet.instance.boardComp.highlightedActor;
            g.setColor(GColor.BLACK);
            info = ZombicideApplet.instance.boardComp.highlightedActor.drawInfo(g, getGame(), getWidth(), getHeight());
        } else if (getGame().getCurrentCharacter() != null) {
            info = getGame().getCurrentCharacter().getInfoTable(getGame()).draw(g);
        } else if (actorInfo != null) {
            g.setColor(GColor.BLACK);
            info = actorInfo.drawInfo(g, getGame(), getWidth(), getHeight());
        }

        g.setColor(GColor.BLACK);
        int y = 0;
        float maxWidth = g.getViewportWidth() - (info == null ? 0 : info.width);
        synchronized (messages) {
            for (String msg : messages) {
                GDimension d = g.drawWrapString(g.getViewportWidth(), y, maxWidth, Justify.RIGHT, Justify.TOP, msg);
                g.setColor(GColor.TRANSLUSCENT_BLACK);
                y += d.getHeight();
            }
        }
        if (info != null)
            maxY = (int)Math.max(info.height, y);
    }

    int maxY = 200;

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(getWidth(), Math.max(getHeight(), maxY));
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 10;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 10;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
