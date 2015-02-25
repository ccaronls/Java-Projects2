package cc.game.soc.netx;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;

abstract class CommandQueue implements Runnable {
    private Queue<Command> queue = new LinkedList<Command>();
    private boolean running = true;
    Logger log = Logger.getLogger(getClass().getSimpleName());
    
    public void run() {

        //log.debug("thread starting");
        while (isRunning()) {
            try {
                Command cmd = null;
                synchronized (queue) {
                    if (queue.isEmpty()) {
                        //log.debug("waiting");
                        queue.wait();
                        //log.debug("waking up");
                    }
                    if (!queue.isEmpty())
                        cmd = queue.peek();
                }
                //log.debug("next cmd: " + cmd);
                
                if (cmd != null) {
                    process(cmd);
                    queue.remove();
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }           
        ///log.debug("thread exiting");
        onExit();
    }
    boolean isRunning() {
        return running || !queue.isEmpty();
    }
    synchronized void add(Command cmd) {
        //log.debug("add cmd: "+ cmd);
        if (running) {
            synchronized (queue) {
                queue.add(cmd);
                queue.notify();
            }
        }
    }
    void stop() {
        //log.debug("stop");
        running = false;
        synchronized (queue) {
            queue.notify();
        }
    }


    protected abstract void process(Command cmd) throws Exception;
    protected abstract void onExit();
}
