package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZBoard;

public class SpawnAnimation extends ZActorAnimation {

    final GRectangle rect;

    public SpawnAnimation(ZActor actor, ZBoard board) {
        super(actor, 1000);
        rect = new GRectangle(actor.getRect(board));
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        GRectangle dest = new GRectangle(rect);
        dest.y += (dest.h) * (1f - position);
        dest.h *= position;
        g.drawImage(actor.getImageId(), dest);
    }

}
