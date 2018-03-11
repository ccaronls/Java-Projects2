package cc.lib.net;

import java.util.LinkedList;
import java.util.Queue;

/**
 * The purpose of this class is to never block the Network read
 * 
 * @author ccaron
 *
 */
abstract class CommandQueueReader {

    // TODO: Replace this class with QueueRunner

    private Queue<GameCommand> queue = new LinkedList<GameCommand>();
    private boolean running = false;
    
    public void stop() {
        running = false;
        synchronized (this) {
            notify();
        }
    }
    
    public void queue(GameCommand cmd) {
        if (running) {
            synchronized (queue) {
                this.queue.add(cmd);
                queue.notify();
            }
        } else {
            throw new RuntimeException("Queuing a command when stopped");
        }
    }
    
    public synchronized void start() {
        if (running)
            return;
        running = true;
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        synchronized (queue) {
                            if (queue.isEmpty() && running)
                                queue.wait(2000);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    GameCommand cmd = null;
                    synchronized (queue) {
                        if (!queue.isEmpty()) {
                            cmd = queue.remove();
                        }
                    }
                    if (cmd != null)
                        process(cmd);
                    else if (!running)
                        break;
                        
                }
                onStopped();
            }
        }).start();
    }
    
    protected abstract void process(GameCommand cmd);
    
    protected abstract void onStopped();
}
