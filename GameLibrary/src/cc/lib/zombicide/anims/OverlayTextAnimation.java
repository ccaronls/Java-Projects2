package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.zombicide.ZAnimation;

public class OverlayTextAnimation extends ZAnimation {
    final String text;

    public OverlayTextAnimation(String text) {
        super(2000);
        this.text = text;
    }
    @Override
    protected void draw(AGraphics g, float position, float dt) {
        int cx = g.getViewportWidth()/2;
        int cy = g.getViewportHeight()/2;

        float minHeight = 32;
        float maxHeight = 48;

        GColor color = GColor.GREEN.withAlpha(1f-position);
        float curHeight = g.getTextHeight();
        g.setTextHeight(minHeight + (maxHeight-minHeight)*position);
        g.drawJustifiedString(cx, cy, Justify.CENTER, Justify.CENTER, text);
        g.setTextHeight(curHeight);
    }
}
