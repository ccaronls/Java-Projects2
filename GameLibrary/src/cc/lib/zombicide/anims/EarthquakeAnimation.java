package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.math.Vector2D;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;

public class EarthquakeAnimation extends ZActorAnimation {
    public EarthquakeAnimation(ZActor actor) {
        super(actor, 2000);
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        g.pushMatrix();
        g.translate(Vector2D.newRandom(position/8));
        g.drawImage(actor.getImageId(), actor.getRect());
        g.popMatrix();
    }

    @Override
    protected boolean hidesActor() {
        return false;
    }
}