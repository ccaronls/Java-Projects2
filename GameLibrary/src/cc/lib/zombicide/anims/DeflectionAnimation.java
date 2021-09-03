package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GRectangle;
import cc.lib.game.IInterpolator;
import cc.lib.math.Bezier;
import cc.lib.math.Vector2D;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZDir;

/**
 * Created by Chris Caron on 8/31/21.
 */
public class DeflectionAnimation extends ZActorAnimation {

    final IInterpolator<Vector2D> arc;
    final int imageId;
    final GRectangle rect;

    public DeflectionAnimation(ZActor actor, int imageId, GRectangle rect, ZDir dir) {
        super(actor, 500);
        this.imageId = imageId;
        this.rect = rect;
        Vector2D start = actor.getRect().getCenter();
        Vector2D end = actor.getRect().getCenterBottom().add(.5f * dir.dx, 0);
        arc = Bezier.build(start,end,.5f);
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        AImage img = g.getImage(imageId);
        Vector2D pos = arc.getAtPosition(position);
        g.drawImage(imageId, rect.fit(img).setCenter(pos));
    }

    @Override
    protected boolean hidesActor() {
        return false;
    }
}
