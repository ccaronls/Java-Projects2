package cc.lib.net;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * The purpose of this class is to never block the Network read
 * 
 * @author ccaron
 *
 */
abstract class CommandQueueReader {

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
    /*
    synchronized void start() {
        if (!running) {
            running = true;
            new Thread(new Runnable() {
                public void run() {
                    while (running) {
                        try {

                            GameCommand cmd = GameCommand.parse(in);
                            synchronized (queue) {
                               queue.add(cmd);
                               queue.notify();
                            }
                            
                        } catch (Exception e) {
                            onError(e);
                        }
                    }
                }
            }).start();
        }
    }
    
    void stop() {
        running = false;
        synchronized (queue) {
            queue.notify();
        }
    }
    
    
    public GameCommand readCommand(int timeout) throws Exception {
        if (queue.isEmpty()) {
            synchronized (queue) {
                queue.wait(timeout);
            }
        }
        
        if (queue.isEmpty())
            throw new Exception("Connection Lost");
        
        synchronized (queue) {
            return queue.remove();
        }
    }
    
    protected void onError(Exception e) {
        e.printStackTrace();
    }
    
    protected void log(String msg) {
        System.out.println(msg);
    }
    */
}
