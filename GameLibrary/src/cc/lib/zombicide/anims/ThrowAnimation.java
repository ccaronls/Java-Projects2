package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GRectangle;
import cc.lib.game.IInterpolator;
import cc.lib.game.IVector2D;
import cc.lib.math.Bezier;
import cc.lib.math.Vector2D;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZIcon;

// TODO: Consider merging Fireball, Throw, Shoot animation types which all have similar features and special characteristics like: STATIC, SPIN, DIRECTIONAL, RANDOM
public class ThrowAnimation extends ZActorAnimation {

    final ZIcon icon;
    final IInterpolator<Vector2D> curve;
    GRectangle rect = null;
    final ZDir dir;

    public ThrowAnimation(ZActor actor, IVector2D target, ZIcon icon) {
        this(actor, target, icon, .5f, 1000);
    }

    public ThrowAnimation(ZActor actor, IVector2D target, ZIcon icon, float arc, long duration) {
        super(actor, duration);
        this.icon = icon;
        Vector2D start = actor.getRect().getCenter();
        Vector2D end = new Vector2D(target);
        dir = ZDir.getFromVector(end.sub(start));
        curve = Bezier.build(start, end, arc);
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        int idx = Math.round(position * (icon.imageIds.length-1));
        int id = icon.imageIds[idx];

        AImage img = g.getImage(id);
        if (rect == null)
            rect = actor.getRect().scaledBy(.5f).fit(img);
        rect.setCenter(curve.getAtPosition(position));
        g.drawImage(id, rect);
    }

    @Override
    protected boolean hidesActor() {
        return false;
    }

    public ZDir getDir() {
        return dir;
    }
}