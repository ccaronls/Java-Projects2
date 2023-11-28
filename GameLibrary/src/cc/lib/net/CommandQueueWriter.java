package cc.lib.net;

import java.io.DataOutputStream;
import java.util.LinkedList;
import java.util.Queue;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.GException;
import cc.lib.utils.Lock;

/**
 * Allows for queueing of commands so we are never waiting for the network to write
 * @author ccaron
 *
 */
class CommandQueueWriter implements Runnable {

    private final Logger log;

    private Queue<GameCommand> queue = new LinkedList<>();
    private boolean running;
    private int timeout = 10000;
    private DataOutputStream out;
    private Lock lock;

    CommandQueueWriter(String logPrefix) {
        log = LoggerFactory.getLogger(logPrefix, CommandQueueWriter.class);
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
        lock.release();
    }

    synchronized void start(final DataOutputStream out) {
        if (!running) {
            this.out = out;
            running = true;
            lock = new Lock();
            new Thread(this).start();
        }
    }

    public void run() {
        log.debug("thread starting");
        int errors = 0;
        while (errors < 5 && (running || !queue.isEmpty())) {
            GameCommand cmd = null;
            try {
                if (queue.isEmpty()) {
                    lock.acquire(1);
                    lock.block(timeout, () -> onTimeout());
                }

                if (!queue.isEmpty()) {
                    synchronized (queue) {
                        cmd = queue.peek();
                    }
                    log.debug("Writing command: " + cmd.getType());
                    cmd.write(out);
                    out.flush();
                    synchronized (queue) {
                        queue.remove();
                    }
                    errors = 0;
                }
            } catch (Exception e) {
                errors++;
                log.error("ERROR: " + errors + " Problem sending command: " + cmd);
                e.printStackTrace();
                Utils.waitNoThrow(this, 500);
            }
        }
        // signal waiting stop() call that we done.
        synchronized (this) {
            notify();
        }
        log.debug("thread exiting");
    }
    
    /**
     * Block until all commands sent.  No new commands will be accepted.
     */
    void stop() {
        log.debug("Stopping");
        try {
            // block for up to 5 seconds for the remaining commands to get sent
            if (queue.size() > 0) {
                log.debug("Wait for queue to flush");
                lock.release();
                synchronized (this) {
                    wait(5000);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        running = false;
        synchronized (queue) {
            queue.clear();
        }
        if (lock != null)
            lock.releaseAll();
        log.debug("Stopped");
    }

    /**
     * Push a command to the outbound queue.
     * throw an error if the queue not running 
     * @param cmd
     * @throws Exception
     */
    void add(GameCommand cmd) {
        log.debug("add command: " + cmd);
        if (!running)
            throw new GException("commandQueue is not running");
        synchronized (queue) {
            queue.add(cmd);
        }
        lock.release();
    }

    /**
     * Clear out any pending things
     */
    void clear() {
        synchronized (queue) {
            queue.clear();
        }
    }

    /**
     * Called when the specified timeout has been reached without a command
     * being sent.  This could be used to support a keep-alive scheme.
     */
    protected void onTimeout() {
    }
}
