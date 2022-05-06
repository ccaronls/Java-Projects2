package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;

/**
 * Created by Chris Caron on 8/30/21.
 */
public class EmptyAnimation extends ZActorAnimation {
    public EmptyAnimation(ZActor actor) {
        super(actor, 1);
    }

    @Override
    protected void drawPhase(AGraphics g, float position, int phase) {
        // do nothing
    }

    @Override
    public boolean hidesActor() {
        return false;
    }
}
