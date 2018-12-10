package cc.lib.net;

import java.io.DataOutputStream;
import java.util.LinkedList;
import java.util.Queue;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

/**
 * Allows for queueing of commands so we are never waiting for the network to write
 * @author ccaron
 *
 */
class CommandQueueWriter {

    private Logger log = LoggerFactory.getLogger(CommandQueueWriter.class);
    
    private Queue<GameCommand> queue = new LinkedList<GameCommand>();
    private boolean running;
    private int timeout = 10000;
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
        synchronized (queue) {
            queue.notify();
        }
    }
    
    protected void log(String msg) {
        System.out.println("CommandQueueWriter [" + Thread.currentThread().getId() + "]: " + msg);
    }
    
    synchronized void start(final DataOutputStream out) {
        if (!running) {
            running = true;
            new Thread(new Runnable() {
                public void run() {
                    log.debug("thread starting");
                    int errors = 0;
                    while (running || !queue.isEmpty()) {
                        GameCommand cmd = null;
                        try {
                            if (queue.isEmpty()) {
                                synchronized (queue) {
                                    if (timeout > 0) {
                                        log.debug("Wait for '" + timeout + "' msecs");
                                        queue.wait(timeout);
                                    }
                                    else
                                        queue.wait();
                                }
                            }
                            //log.debug("Wake up");
                            
                            if (queue.isEmpty()) {
                                log.debug("Q empty");
                                if (running) {
                                    log.debug("Timeout");
                                    onTimeout();
                                }
                            } else {
                                
                                cmd = queue.peek();
                                log.debug("Writing command: " + cmd.getType());
                                cmd.write(out);
                                synchronized (queue) {
                                    queue.remove();
                                }
                            }
                            errors = 0;
                        } catch (Exception e) {
                            if (errors++ < 3) {
                                System.err.println("Problem sending command: " + cmd);
                                e.printStackTrace();
                            }
                            Utils.waitNoThrow(this, 500);
                        }
                    }
                    // signal waiting stop() call that we done.
                    synchronized (CommandQueueWriter.this) {
                        CommandQueueWriter.this.notify();
                    }
                    log.debug("thread exiting");
                }
            }).start();
        }
    }
    
    /**
     * Block until all commands sent.  No new commands will be accepted.
     */
    void stop() {
        log.debug("Stopping");
        try {
            // block for up to 5 seconds for the remaining commands to get sent
            if (queue.size() > 0) {
                log.debug("out Q has elements, notifying...");
                synchronized (queue) {
                    queue.notify();
                }
                log.debug("Wait for queue to flush");
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
            queue.notify();
        }
        log.debug("Stopped");
    }

    /**
     * Push a command to the outbound queue.
     * throw an error if the queue not running 
     * @param cmd
     * @throws Exception
     */
    void add(GameCommand cmd) {
//        log.debug("add command: " + cmd, 256, 32);
        if (!running)
            throw new RuntimeException("commandQueue is not running");
        synchronized (queue) {
            queue.add(cmd);
            queue.notify();
        }
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
    protected void onTimeout() {}
}
