package cc.lib.zombicide.anims;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.utils.Pair;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZAnimation;

/**
 * Allow for running multiple animations with some delay inbetween each starting position
 *
 */
public class GroupAnimation extends ZActorAnimation {

    private final List<Pair<ZAnimation, Integer>> group = new ArrayList<>();

    public GroupAnimation(ZActor actor) {
        super(actor, 1);
    }

    /**
     * IMPORTANT!: Make sure to have all animations added before starting!
     * @param animation
     */
    public synchronized void addAnimation(int delay, ZAnimation animation) {
        Utils.assertTrue(!isStarted());
        group.add(new Pair(animation, delay));
    }

    @Override
    public boolean isDone() {
        for (Pair<ZAnimation, Integer> a : group) {
            if (!a.first.isDone())
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
        Iterator<Pair<ZAnimation,Integer>> it = group.iterator();
        while (it.hasNext()) {
            Pair<ZAnimation, Integer> p = it.next();
            if (p.first.isDone())
                it.remove();
            else if (!p.first.isStarted()) {
                //a.start();
                if (getCurrentTimeMSecs()-getStartTime() >= p.second) {
                    p.first.start();
                }
            }
            else {
                p.first.update(g);
            }
        }
        return group.isEmpty();
    }

    @Override
    protected boolean hidesActor() {
        return false;
    }
}
