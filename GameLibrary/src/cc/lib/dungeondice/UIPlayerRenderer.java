package cc.lib.dungeondice;

import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.ui.UIComponent;
import cc.lib.ui.UIRenderer;

public class UIPlayerRenderer extends UIRenderer {

    DPlayer player = null;
    GColor color = GColor.RED;
    static int keyAsset = -1;

    public UIPlayerRenderer(UIComponent component) {
        super(component);
    }

    @Override
    public void draw(APGraphics g, int px, int py) {
        if (player == null)
            return;

        if (UI.getInstance().getTurn() == player.playerNum) {
            g.setColor(color);
            g.setLineWidth(5);
            g.drawRect(0, 0, g.getViewportWidth(), g.getViewportHeight());
        }

        StringBuffer txt = new StringBuffer();
        txt.append(player.getName());
        txt.append("\n").append(String.format("%-5s %d", "STR", player.str));
        txt.append("\n").append(String.format("%-5s %d", "DEX", player.dex));
        txt.append("\n").append(String.format("%-5s %d", "ATT", player.attack));
        txt.append("\n").append(String.format("%-5s %d", "DEF", player.def));

        g.setColor(GColor.BLACK);
        g.drawString(txt.toString(), 10, 10);

        if (player.hasKey()) {
            if (keyAsset < 0) {
                keyAsset = g.loadImage("key.png", GColor.BLACK);
            }
            g.drawImage(keyAsset, g.getViewportWidth()-30, 5, 25, 25);
        }
    }
}
