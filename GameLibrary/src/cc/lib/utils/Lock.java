package cc.lib.utils;

/**
 * Counting lock. Allows for blocking a thread until some number of tasks are completed.
 * <p>
 * Aquire increments the count.
 * Release decrements a count.
 * When count gets to zero the blocked thread is notified.
 */
public class Lock {

    private int holders = 0;
    // allows us to differentiate when a 'block' has released due to notify of timeout
    private boolean notified = false;
    private boolean blocking = false;

    private final Object monitor = new Object();

    public synchronized void acquire() {
        holders++;
    }

    public synchronized void acquire(int num) {
        if (holders != 0)
            throw new IllegalArgumentException("Holders [" + holders + "] != 0");
        holders = num;
    }

    public Lock() {
    }

    public Lock(int holders) {
        this.holders = holders;
    }

    public void block() {
        block(0, null);
    }

    public void block(long waitTimeMillis) {
        block(waitTimeMillis, null);
    }

    public void block(long waitTimeMillis, Runnable onTimeout) {
        if (onTimeout != null && waitTimeMillis <= 0)
            throw new IllegalArgumentException("Cannot have timeout on infinite wait");
        if (blocking)
            throw new IllegalArgumentException("Already blocking");
        notified = false;
        if (holders > 0) {
            blocking = true;
            try {
                synchronized (monitor) {
                    monitor.wait(waitTimeMillis);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            blocking = false;
            holders = 0;
            if (!notified && onTimeout != null) {
                onTimeout.run();
            }
        }
    }

    public void acquireAndBlock() {
        acquireAndBlock(0, null);
    }

    public void acquireAndBlock(long timeout) {
        acquireAndBlock(timeout, null);
    }

    public void acquireAndBlock(long timeout, Runnable onTimeout) {
        synchronized (this) {
            if (holders > 0)
                throw new GException("Dead Lock");
            holders++;
        }
        block(timeout, onTimeout);
    }

    public synchronized void release() {
        if (holders > 0)
            holders--;
        if (holders == 0) {
            notified = true;
            synchronized (monitor) {
                monitor.notify();
            }
        }
    }

    public synchronized void releaseAll() {
        holders = 0;
        notified = true;
        synchronized (monitor) {
            monitor.notify();
        }
    }

    /*
    public void releaseDelayed(long ms) {
        new Thread(() -> {
            Utils.waitNoThrow(this, ms);
            release();
        }).start();
    }*/

    public synchronized final void reset() {
        if (blocking)
            throw new IllegalArgumentException("Cannot reset while blocking");
        holders = 0;
        notified = false;
    }

    public int getHolders() {
        return holders;
    }

    public boolean isNotified() {
        return notified;
    }

    public boolean isBlocking() {
        return blocking;
    }
}
