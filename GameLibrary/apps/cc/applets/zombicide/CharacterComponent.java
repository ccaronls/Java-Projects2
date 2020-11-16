package cc.applets.zombicide;

import java.awt.Font;
import java.util.LinkedList;

import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTGraphics;
import cc.lib.zombicide.ZGame;

class CharacterComponent extends AWTComponent {

    CharacterComponent() {
        setPreferredSize(300, 200);
    }

    LinkedList<String> messages = new LinkedList<>();

    ZGame getGame() {
        return ZombicideApplet.instance.game;
    }

    void addMessage(String msg) {
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

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        g.clearScreen();
        if (getGame() == null)
            return;
        g.getGraphics().setFont(Font.decode(Font.MONOSPACED).deriveFont(Font.BOLD));
        if (ZombicideApplet.instance.boardComp.highlightedActor != null) {
            g.setColor(GColor.BLACK);
            ZombicideApplet.instance.boardComp.highlightedActor.drawInfo(g, getGame(), getWidth(), getHeight());
        } else if (getGame().getCurrentCharacter() != null) {
            String txt = getGame().getCurrentCharacter().getDebugString(getGame());
            g.setColor(GColor.BLACK);
            g.drawString(txt, 0, 0);
        }

        g.setColor(GColor.BLACK);
        int y = 0;
        for (String msg : messages) {
            g.drawJustifiedString(g.getViewportWidth(), y, Justify.RIGHT, Justify.TOP, msg);
            g.setColor(GColor.TRANSLUSCENT_BLACK);
            y += g.getTextHeight();
        }
    }

}
