package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.zombicide.ZAnimation;

public class OverlayTextAnimation extends ZAnimation {
    final String text;
    final int dyType;

    public OverlayTextAnimation(String text, int type) {
        super(3000);
        this.text = text;
        this.dyType = type % 3; // this make the animation stagger in case there are multiple
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        float cx = g.getViewportWidth()/2;
        float cy0 = g.getViewportHeight()/2;

        float cy1 = 0;
        switch (dyType) {
            default:
                cy1 = g.getViewportHeight()/3;
                break;
            case 1:
                cy1 = g.getViewportHeight()/2;
                break;
            case 2:
                cy1 = g.getViewportHeight()*2/3;
                break;
        }

        float minHeight = 32;
        float maxHeight = 48;

        GColor color = GColor.GREEN;
        if (position > .5f) {
            float alpha = 1f - 2f * (position - .5f);
            color = color.withAlpha(alpha);
        }

        g.setColor(color);
        float curHeight = g.getTextHeight();
        g.setTextHeight(minHeight + (maxHeight-minHeight)*position);
        float cy = cy0 + (cy1-cy0) * position;
        g.drawJustifiedString(cx, cy, Justify.CENTER, Justify.BOTTOM, text);
        g.setTextHeight(curHeight);
    }
}
