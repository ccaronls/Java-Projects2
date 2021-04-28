package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GRectangle;
import cc.lib.math.Vector2D;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZIcon;
import cc.lib.zombicide.ZZone;

public class ShootAnimation extends ZActorAnimation {

    final ZIcon icon;
    final ZDir dir;
    final GRectangle rect;
    final Vector2D start, path;

    public ShootAnimation(ZActor actor, ZBoard board, long duration, int targetZone, ZIcon icon) {
        super(actor, duration);
        this.icon = icon;
        ZZone end   = board.getZone(targetZone);
        rect = actor.getRect(board).scaledBy(.5f);
        start = rect.getCenter();
        Vector2D dv = end.getCenter().sub(start);
        dir = ZDir.getFromVector(dv);
        path = end.getCenter().addEq(Vector2D.newRandom(.3f)).sub(start);
        setDuration(Math.round(path.mag() * duration));
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        int id = ZIcon.ARROW.imageIds[dir.ordinal()];
        AImage img = g.getImage(id);
        Vector2D pos = start.add(path.scaledBy(position));
        g.drawImage(id, rect.fit(img).setCenter(pos));
    }

    @Override
    protected boolean hidesActor() {
        return false;
    }
}