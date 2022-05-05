package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;

public class EarthquakeAnimation extends ZActorAnimation {
    final ZActor target;

    public EarthquakeAnimation(ZActor actor) {
        super(actor, 2000);
        target = actor;
    }

    public EarthquakeAnimation(ZActor target, ZActor owner, long dur) {
        super(owner, dur);
        this.target = target;
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        g.pushMatrix();
        g.translate(Utils.randFloatX((1f-position)/8), 0);
        g.drawImage(target.getImageId(), target.getRect());
        g.popMatrix();
    }

    @Override
    public boolean hidesActor() {
        return false;
    }
}