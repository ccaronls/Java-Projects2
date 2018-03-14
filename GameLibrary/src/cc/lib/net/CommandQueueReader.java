package cc.lib.net;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

/**
 * The purpose of this class is to never block the Network read
 * 
 * @author ccaron
 *
 */
abstract class CommandQueueReader {

    // TODO: Replace this class with QueueRunner
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Queue<GameCommand> queue = new LinkedList<GameCommand>();
    private boolean running = false;
    
    public void stop() {
        running = false;
        synchronized (this) {
            notify();
        }
    }
    
    public void queue(GameCommand cmd) {
        log.debug("queded cmd: " + cmd);
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

                    log.debug("queue waking: size=%d", queue.size());
                    GameCommand cmd = null;
                    synchronized (queue) {
                        if (!queue.isEmpty()) {
                            cmd = queue.remove();
                        }
                    }
                    if (cmd != null) {
                        try {
                            process(cmd);
                        } catch (IOException e) {
                            e.printStackTrace();
                            running = false;
                        }
                    }

                    if (!running)
                        break;
                        
                }
                onStopped();
                running = false;
            }
        }).start();
    }
    
    protected abstract void process(GameCommand cmd) throws IOException;
    
    protected abstract void onStopped();
}
