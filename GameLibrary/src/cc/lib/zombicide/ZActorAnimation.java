package cc.lib.zombicide;

public abstract class ZActorAnimation extends ZAnimation {

    public final ZActor actor;
    ZActorAnimation next;

    public ZActorAnimation(ZActor actor, long duration) {
        super(duration);
        this.actor = actor;
    }

    public ZActorAnimation(ZActor actor, long durationMSecs, int repeats) {
        super(durationMSecs, repeats);
        this.actor = actor;
    }

    @Override
    protected void onDone() {
        if (next != null) {
            actor.animation = next;
            next.start();
        }
    }

    void add(ZActorAnimation anim) {
        if (next == null)
            next = anim;
        else
            next.add(anim);
    }

    protected boolean hidesActor() {
        return true;
    }
}
