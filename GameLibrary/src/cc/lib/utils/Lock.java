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

    public synchronized void acquire() {
        holders++;
    }

    public Lock() {}

    public Lock(int holders) {
        this.holders = holders;
    }

    public synchronized void block() {
        try {
            synchronized (this) {
                wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
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
            notify();
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

    public synchronized void reset() {
        notify();
    }
}
