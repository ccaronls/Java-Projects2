package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;

public class DeathAnimation extends ZActorAnimation {

    public DeathAnimation(ZActor a) {
        super(a, 2000);
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        GRectangle rect = new GRectangle(actor.getRect());
        rect.y += rect.h*position;
        rect.h *= (1f-position);
        float dx = rect.w*position;
        rect.w += dx;
        rect.x -= dx/2;
        g.drawImage(actor.getImageId(), rect);
    }
}