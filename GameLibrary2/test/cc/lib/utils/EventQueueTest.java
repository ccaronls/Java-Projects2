package cc.lib.utils;

import junit.framework.TestCase;

import org.junit.Assert;

import cc.lib.logger.LoggerFactory;

public class EventQueueTest extends TestCase {

    public void test() throws Exception {

        EventQueue q = new EventQueue();
        LoggerFactory.logLevel = LoggerFactory.LogLevel.SILENT;
        long startTime = q.getCurrentTime();

        new Thread(q).start();

        q.enqueue(2000, () -> System.out.println((q.getCurrentTime() - startTime) + " E"));
        q.enqueue(1500, () -> System.out.println((q.getCurrentTime() - startTime) + " D"));
        q.enqueue(1000, () -> System.out.println((q.getCurrentTime() - startTime) + " C"));
        q.enqueue(500, () -> System.out.println((q.getCurrentTime() - startTime) + " B"));
        q.enqueue(100, () -> System.out.println((q.getCurrentTime() - startTime) + " A"));

        Thread.sleep(3000);

        q.stop();

        Assert.assertEquals(0, q.getQuesuSize());
    }


}
