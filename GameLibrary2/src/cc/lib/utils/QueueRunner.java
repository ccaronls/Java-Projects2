package cc.lib.utils;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by chriscaron on 3/4/18.
 * <p>
 * FIFO Command processing Q. Items are processed in separate thread.
 */

public abstract class QueueRunner<T> implements Runnable {

    private Queue<T> queue = new LinkedList<>();
    private boolean running = false;

    public void add(T item) {
        synchronized (queue) {
            queue.add(item);
        }

        if (running) {
            synchronized (this) {
                notify();
            }
        } else {
            new Thread(this).start();
        }
    }

    public final void run() {
        running = true;
        try {
            while (true) {
                if (queue.isEmpty()) {
                    synchronized (this) {
                        wait(5000);
                    }
                }

                if (queue.isEmpty())
                    break;

                T item = null;
                synchronized (queue) {
                    item = queue.remove();
                }

                process(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        running = false;
    }

    public final void clear() {
        synchronized (queue) {
            queue.clear();
            synchronized (this) {
                notify();
            }
        }
    }

    protected abstract void process(T item);

}
