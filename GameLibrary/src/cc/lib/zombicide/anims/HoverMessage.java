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
    private Justify hJust = null;

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
        Vector2D tv = g.transform(center);
        float width = g.getTextWidth(msg)/2;
        Justify vJust = Justify.CENTER;
        if (hJust == null) {
            if (tv.X() + width > g.getViewportWidth()) {
                hJust = Justify.RIGHT;
            } else if (tv.X() - width < 0) {
                hJust = Justify.LEFT;
            } else {
                hJust = Justify.CENTER;
            }
        }
        g.drawJustifiedString(v, hJust, Justify.CENTER, msg);
//        g.setTextHeight(t);
    }
}