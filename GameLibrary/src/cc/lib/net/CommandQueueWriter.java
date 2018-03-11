package cc.lib.net;

import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Allows for queueing of commands so we are never waiting for the network to write
 * @author ccaron
 *
 */
class CommandQueueWriter {

    static final boolean DBEUG_ENABLED = false;
    
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
    
    synchronized void start(final OutputStream out) {
        if (!running) {
            running = true;
            new Thread(new Runnable() {
                public void run() {
                    if (DBEUG_ENABLED) log("thread starting");
                    while (running || !queue.isEmpty()) {
                        GameCommand cmd = null;
                        try {
                            if (queue.isEmpty()) {
                                synchronized (queue) {
                                    if (timeout > 0) {
                                        if (DBEUG_ENABLED) log("Wait for '" + timeout + "' msecs");
                                        queue.wait(timeout);
                                    }
                                    else
                                        queue.wait();
                                }
                            }
                            if (DBEUG_ENABLED) log("Wake up");
                            
                            if (queue.isEmpty()) {
                                if (running) {
                                    if (DBEUG_ENABLED) log("Timeout");
                                    onTimeout();
                                }
                            } else {
                                
                                cmd = queue.peek();
                                if (DBEUG_ENABLED) log("Writing command: " + cmd);
                                cmd.write(out);
                                synchronized (queue) {
                                    queue.remove();
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Problem sending command: " + cmd);
                            e.printStackTrace();
                        }
                    }
                    // signal waiting stop() call that we done.
                    synchronized (CommandQueueWriter.this) {
                        CommandQueueWriter.this.notify();
                    }
                    if (DBEUG_ENABLED) log("thread exiting");
                }
            }).start();
        }
    }
    
    /**
     * Block until all commands sent.  No new commands will be accepted.
     */
    void stop() {
        if (DBEUG_ENABLED) log("Stopping");
        running = false;
        try {
            // block for up to 5 seconds for the remaining commands to get sent
            if (queue.size() > 0) {
                synchronized (queue) {
                    queue.notify();
                }
                synchronized (this) {
                    wait(5000);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        queue.clear();
        if (DBEUG_ENABLED) log("Stopped");
    }

    /**
     * Push a command to the outbound queue.
     * throw an error if the queue not running 
     * @param cmd
     * @throws Exception
     */
    void add(GameCommand cmd) {
        if (DBEUG_ENABLED) log("add command: " + cmd);
        if (!running)
            throw new RuntimeException("commandQueue is not running");
        synchronized (queue) {
            queue.add(cmd);
            queue.notify();
        }
    }
    
    /**
     * Called when the specified timeout has been reached without a command
     * being sent.  This could be used to support a keep-alive scheme.
     */
    protected void onTimeout() {}
}
