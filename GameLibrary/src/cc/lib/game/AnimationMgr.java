package cc.lib.game;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AnimationMgr<T> {

    public static interface Listener {
        void onDone();
    }

    final private Map<AAnimation<T>, Listener> anims = Collections.synchronizedMap(new HashMap<>());

    public void startAnim(AAnimation<T> a, Listener listener) {
        anims.put(a.start(), listener);
    }

    /**
     *
     *
     * @param g
     * @return true if animations in progress
     */
    public boolean updateAll(T g) {
        if (anims.size() == 0)
            return false;
        int num = anims.size();
        for (Map.Entry<AAnimation<T>, Listener> e : anims.entrySet()) {
            AAnimation<T> a = e.getKey();
            if (!a.isDone()) {
                a.update(g);
            } else {
                num--;
            }
        }
        if (num == 0) {
            onAllAnimationsCompleted();
            anims.clear();
        } else {
            return true;
        }
        return false;
    }

    protected void onAllAnimationsCompleted() {
        for (Map.Entry<AAnimation<T>, Listener> e : anims.entrySet()) {
            if (e.getValue() != null) {
                e.getValue().onDone();
            }
        }

    }
}
