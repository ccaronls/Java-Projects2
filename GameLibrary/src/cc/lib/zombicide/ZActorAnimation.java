package cc.lib.zombicide;

public abstract class ZActorAnimation extends ZAnimation {

    public final ZActor actor;
    ZActorAnimation next;

    public ZActorAnimation(ZActor actor, long duration) {
        super(duration);
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


}
