package cc.lib.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.lib.net.GameClient;

/**
 * Created by chriscaron on 3/13/18.
 */

public class ListenerSet<T> {

    public abstract class Runner {
        public abstract void run(T l);
    }

    private final Set<T> listeners = new HashSet<>();

    public void addListener(T l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    public void removeListener(T l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    public void notify(Runner r) {
        if (listeners.size() > 0) {
            List<T> copy;
            synchronized (listeners) {
                copy = new ArrayList<>(listeners);
            }
            for (T l : copy) {
                try {
                    r.run(l);
                } catch (Exception e) {
                    e.printStackTrace();
                    removeListener(l);
                }
            }
        }
    }

}
