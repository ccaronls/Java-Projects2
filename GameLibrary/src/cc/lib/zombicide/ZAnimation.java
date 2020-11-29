package cc.lib.zombicide;

import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;

public abstract class ZAnimation extends AAnimation<AGraphics> {

    final ZActor actor;
    ZAnimation next;

    public ZAnimation(ZActor actor, long duration) {
        super(duration);
        this.actor = actor;
    }

    @Override
    protected void onDone() {
        actor.animation = next;
        if (next != null) {
            next.start();
        }
    }

    void add(ZAnimation anim) {
        if (next == null)
            next = anim;
        else
            next.add(anim);
    }
}
