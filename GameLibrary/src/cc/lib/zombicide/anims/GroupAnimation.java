package cc.lib.zombicide.anims;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZAnimation;

public class GroupAnimation extends ZActorAnimation {

    private final List<ZAnimation> group = new ArrayList<>();

    public GroupAnimation(ZActor actor) {
        super(actor, 1);
    }

    public synchronized void addAnimation(ZAnimation animation) {
        group.add(animation.start());
    }

    @Override
    public boolean isDone() {
        for (ZAnimation a : group) {
            if (!a.isDone())
                return false;
        }
        return true;
    }

    @Override
    protected void onStarted() {
        super.onStarted();
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        Utils.assertTrue(false); // should not get called
    }

    @Override
    public synchronized boolean update(AGraphics g) {
        Iterator<ZAnimation> it = group.iterator();
        while (it.hasNext()) {
            ZAnimation a = it.next();
            if (a.isDone())
                it.remove();
            else
                a.update(g);
        }
        return group.isEmpty();
    }
}
