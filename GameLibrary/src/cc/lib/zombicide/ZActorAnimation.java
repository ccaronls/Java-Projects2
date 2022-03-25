package cc.lib.zombicide;

import cc.lib.annotation.CallSuper;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;

public abstract class ZActorAnimation extends ZAnimation {

    public final ZActor actor;
    ZActorAnimation next;

    public ZActorAnimation(ZActor actor, long ... durations) {
        super(Utils.toLongArray(durations));
        this.actor = actor;
    }

    public ZActorAnimation(ZActor actor, long durationMSecs, int repeats) {
        super(durationMSecs, repeats);
        this.actor = actor;
    }

    @Override
    @CallSuper
    protected void onDone() {
        if (next != null && actor != null) {
            actor.animation = next;
            next.start();
        }
    }

    @Override
    public boolean isDone() {
        if (next == null || actor ==  null)
            return super.isDone();
        if (next != null)
            return next.isDone();
        return false;
    }

    void add(ZActorAnimation anim) {
        if (next == null)
            next = anim;
        else
            next.add(anim);
    }

    protected GRectangle getRect() {
        return null;
    }

    protected boolean hidesActor() {
        return true;
    }
}
