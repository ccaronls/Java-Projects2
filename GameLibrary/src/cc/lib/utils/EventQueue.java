package cc.lib.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

/**
 * Event Queue processor.
 *
 * Usage example:
 *
 * EventQueue q = new EventQueue();
 *
 * new Thread(q).start(); <-- all events executed inside this thread
 *
 * q.enqueue(1000, ()-> { ... });
 *
 *
 */
public class EventQueue implements Runnable {

    private final Logger log = LoggerFactory.getLogger(EventQueue.class);

    static class QueueItem implements Comparable<QueueItem> {
        final long runTimeMS;
        final Runnable runnable;
        final long id;

        public QueueItem(long runTimeMS, Runnable runnable, long id) {
            this.runTimeMS = runTimeMS;
            this.runnable = runnable;
            this.id = id;
        }

        @Override
        public int compareTo(QueueItem o) {
            return Long.compare(runTimeMS, o.runTimeMS);
        }
    }

    private boolean running = false;
    private final List<QueueItem> queue = new ArrayList<>();
    private long idAllocator = 1;

    public synchronized long enqueue(long delay, Runnable event) {
        synchronized (queue) {
            queue.add(new QueueItem(getCurrentTime() + delay, event, idAllocator));
            Collections.sort(queue);
        }
        notify();
        return idAllocator++;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        if (running)
            return;
        log.debug("Thread starting");
        running = true;
        while (running) {
            QueueItem i = null;
            synchronized (queue) {
                if (queue.size() > 0) {
                    i = queue.get(0);
                }
            }

            long delay = 0;
            if (i != null) {
                if (i.runTimeMS <= getCurrentTime()) {
                    synchronized (queue) {
                        queue.remove(0);
                    }
                    try {
                        i.runnable.run();
                    } catch (Exception e) {
                        onError(i.id, e);
                    }
                } else {
                    delay = i.runTimeMS - getCurrentTime();
                }
            } else {
                delay = 10000;
            }

            if (delay > 0) {
                log.debug("Delay for " + delay + " millis");
                synchronized (this) {
                    try {
                        wait(delay);
                        log.debug("wakeup");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        log.debug("Thread exiting");

    }

    protected void onError(long eventId, Exception e) {
        log.error("%d has error: %s", eventId, e.getMessage());
        e.printStackTrace();
    }

    public synchronized void stop() {
        running = false;
        notify();
    }

    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public int getQuesuSize() {
        return queue.size();
    }
}
