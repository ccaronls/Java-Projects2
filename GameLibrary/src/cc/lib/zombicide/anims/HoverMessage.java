package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.IVector2D;
import cc.lib.game.Justify;
import cc.lib.math.Vector2D;
import cc.lib.zombicide.ZAnimation;
import cc.lib.zombicide.ZBoard;

public class HoverMessage extends ZAnimation {

    private final String msg;
    private final IVector2D center;
    private final float zoom;

    public HoverMessage(ZBoard board, String msg, IVector2D center) {
        super(3000);
        this.msg = msg;
        this.center = center;
        zoom = board.getZoom()+1;
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        Vector2D v = new Vector2D(center);
        v = v.sub(0, position/zoom);
        float t = g.getTextHeight();
        g.setTextHeight(20);
        g.setColor(GColor.YELLOW.withAlpha(1f-position));
        g.drawJustifiedString(v, Justify.LEFT, Justify.BOTTOM, msg);
        g.setTextHeight(t);
    }
}