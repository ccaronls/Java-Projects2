package cc.lib.utils;

import junit.framework.TestCase;

public class TestProfiler extends TestCase {

    public void test() throws Exception {
        
        Profiler.ENABLED = true;
        Profiler.push("A");
        Thread.sleep(300);
        Profiler.push("B");
        Thread.sleep(300);
        Profiler.push("C");
        Thread.sleep(300);
        Profiler.pop("C");
        Profiler.pop("B");
        Profiler.pop("A");
        Profiler.dumpTimes(System.out);
        
    }
    
    
}
