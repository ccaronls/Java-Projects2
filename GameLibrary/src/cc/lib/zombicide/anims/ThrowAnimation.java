package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GRectangle;
import cc.lib.math.Bezier;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZIcon;

public class ThrowAnimation extends ZActorAnimation {

    final int zone;
    final ZIcon icon;
    final Bezier curve;

    public ThrowAnimation(ZActor actor, ZBoard board, int targetZone, ZIcon icon) {
        super(actor, 1000);
        this.zone = targetZone;
        this.icon = icon;
        curve = Bezier.build(actor.getRect(board).getCenter(), board.getZone(zone).getCenter(), .5f);
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        int idx = Math.round(position * (icon.imageIds.length-1));
        int id = icon.imageIds[idx];

        AImage img = g.getImage(id);
        GRectangle rect = actor.getRect().scaledBy(.5f).fit(img);
        rect.setCenter(curve.getPointAt(position));
        g.drawImage(id, rect);
    }

    @Override
    protected boolean hidesActor() {
        return false;
    }
}