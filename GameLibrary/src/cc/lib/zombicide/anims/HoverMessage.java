package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.IVector2D;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.zombicide.ZAnimation;
import cc.lib.zombicide.ui.UIZBoardRenderer;

public class HoverMessage extends ZAnimation {

    private final String msg;
    private final IVector2D center;
    private final Vector2D dv;

    public HoverMessage(UIZBoardRenderer board, String msg, IVector2D center) {
        super(3000);
        this.msg = msg;
        this.center = center;
        dv = board.getZoomedRect().getCenter().subEq(center)
                .rotateEq(Utils.randFloatX(30))
                .normalizedEq().scaleEq(.5f);
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        Vector2D v = new Vector2D(center).add(dv.scaledBy(position));
//        float t = g.getTextHeight();
//        g.setTextHeight(20);
        g.setColor(GColor.YELLOW.withAlpha(1f-position));
        g.drawJustifiedString(v, Justify.LEFT, Justify.BOTTOM, msg);
//        g.setTextHeight(t);
    }
}