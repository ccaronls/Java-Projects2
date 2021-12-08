package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GRectangle;
import cc.lib.game.IInterpolator;
import cc.lib.game.IVector2D;
import cc.lib.math.Vector2D;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZIcon;

public class ShootAnimation extends ZActorAnimation {

    final ZIcon icon;
    final ZDir dir;
    GRectangle rect;
    final IInterpolator<Vector2D> path;
    Vector2D pos;

    public ShootAnimation(ZActor actor, long duration, IVector2D center, ZIcon icon) {
        super(actor, duration);
        this.icon = icon;
        Vector2D start = new Vector2D(actor);
        Vector2D end   = new Vector2D(center);
        Vector2D dv = end.sub(start);
        dir = ZDir.getFromVector(dv);
        path = Vector2D.getLinearInterpolator(start, end);
        setDuration(Math.round(dv.mag() * duration));
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        int id = ZIcon.ARROW.imageIds[dir.ordinal()];
        AImage img = g.getImage(id);
        pos = path.getAtPosition(position);
        if (rect == null) {
            rect = actor.getRect().scaledBy(.5f).fit(img);
        }
        g.drawImage(id, getRect());
    }

    @Override
    protected boolean hidesActor() {
        return false;
    }

    public ZDir getDir() {
        return dir;
    }

    public GRectangle getRect() {
        return rect.withCenter(pos);
    }
}