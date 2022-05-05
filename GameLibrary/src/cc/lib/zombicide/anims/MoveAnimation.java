package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;

public class MoveAnimation extends ZActorAnimation {

    final GRectangle start, end;
    GRectangle current = null;

    public MoveAnimation(ZActor actor, GRectangle start, GRectangle end, long speed) {
        super(actor, speed);
        this.start = start;
        this.end = end;
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {

        MutableVector2D dv0 = end.getTopLeft().sub(start.getTopLeft());
        MutableVector2D dv1 = end.getBottomRight().sub(start.getBottomRight());

        Vector2D topLeft = start.getTopLeft().add(dv0.scaledBy(position));
        Vector2D bottomRight = start.getBottomRight().add(dv1.scaledBy(position));

        current = new GRectangle(topLeft, bottomRight);
        //g.drawImage(actor.getImageId(), current);
        actor.draw(g);
    }

    @Override
    public GRectangle getRect() {
        return current;
    }
}
