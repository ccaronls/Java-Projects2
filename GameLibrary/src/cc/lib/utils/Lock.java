package cc.lib.utils;

import cc.lib.game.Utils;

/**
 * Counting lock. Allows for blocking a thread until some number of tasks are completed.
 *
 * Aquire increments the count.
 * Release decrements a count.
 * When count gets to zero the blocked thread is notified.
 */
public final class Lock {

    private int holders = 0;

    private final Object monitor = new Object();

    public synchronized void acquire() {
        holders++;
    }

    public Lock() {}

    public Lock(int holders) {
        this.holders = holders;
    }

    public void block() {
        if (holders > 0) {
            try {
                synchronized (monitor) {
                    monitor.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        holders = 0;
    }

    public void block(long waitTimeMillis) {
        if (holders > 0) {
            try {
                synchronized (monitor) {
                    monitor.wait(waitTimeMillis);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        holders = 0;
    }

    public synchronized void acquireAndBlock() {
        if (holders > 0)
            throw new GException("Dead Lock");
        holders++;
        block();
    }

    public synchronized void release() {
        if (holders > 0)
            holders --;
        if (holders == 0) {
            synchronized (monitor) {
                monitor.notify();
            }
        }
    }

    public synchronized void releaseAll() {
        holders = 0;
        synchronized (monitor) {
            monitor.notify();
        }
    }

    public void releaseDelayed(long ms) {
        new Thread(() -> {
            Utils.waitNoThrow(this, ms);
            release();
        }).start();
    }

    public int getHolders() {
        return holders;
    }

    public final void reset() {
        holders = 0;
    }
}
