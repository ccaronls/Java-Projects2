package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;

public class SpawnAnimation extends ZActorAnimation {

    final GRectangle rect;

    public SpawnAnimation(ZActor actor) {
        super(actor, 1000);
         rect = new GRectangle(actor.getRect());
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        GRectangle dest = new GRectangle(rect);
        dest.y += (dest.h) * (1f - position);
        dest.h *= position;
        g.drawImage(actor.getImageId(), dest);
    }

}
