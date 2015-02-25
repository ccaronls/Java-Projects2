package cc.lib.utils;

import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;

public class Profiler {

    /**
     * enable profiling
     */
    public static boolean ENABLED = false;
    
    /**
     * enable asserts on incorrect usage.  No exceptions are thrown unless 
     */
    public static boolean STRICT = true;
    
   
    private static class Node {
        public Node(String name, long startTime) {
            this.name = name;
            this.start = startTime;
        }
        final long start;
        final String name;
        public String toString() {
            return name ;
        }
    }

    static Stack<Node> stack = new Stack<Node>();    
    static Map<String, Long> times = new HashMap<String, Long>();
    
    /**
     * Push a name to profiler.  If strict then will throw a RuntimeException if the same id is pushed twice without a pop inbetween.
     * @param id
     */
    public static void push(String id) {
        if (ENABLED) {
            try {
                if (STRICT && stack.size() > 0 && stack.peek().equals(id))
                    throw new RuntimeException("Profiler::push called twice with the same id ");
                long startTime = System.currentTimeMillis();
                stack.push(new Node(id, startTime));
            } catch (Exception e) {
                if (STRICT)
                    throw new RuntimeException(e);
            }
        }
    }

    /**
     * Pop a name off the stack.  If strict mode then assert if improper usage.
     * @param id
     */
    public static void pop(String id) {
        try {
            if (ENABLED) {
                long endTime = System.currentTimeMillis();
                Node top = null;
                do {
                    top = stack.pop();
                    if (!top.name.equals(id)) {
                        throw new RuntimeException("Profiler::pop(" + id + ") called but top of stack is: " + top);
                    }
                    if (times.containsKey(top.name)) {
                        long time = times.get(top.name);
                        times.put(top.name,  time + (endTime - top.start));
                    } else {
                        times.put(top.name, endTime - top.start);
                    }
                } while (!top.name.equals(id));
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (STRICT)
                throw new RuntimeException(e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static void dumpTimes(PrintStream out) {
        int len = 20;
        for (String key : times.keySet())
            len = Math.max(len, key.length()+2);
        
        out.println(String.format("%-" + len + "s   milisecs", "ID"));
        Map.Entry<String, Long> [] entries = times.entrySet().toArray(
                new Map.Entry[times.entrySet().size()]); 
        Arrays.sort(entries, new Comparator<Map.Entry<String, Long>>() {

            @Override
            public int compare(Entry<String, Long> o1, Entry<String, Long> o2) {
                return (int)(o1.getValue().longValue() - o2.getValue().longValue());
            }
            
        });
        
        for (Map.Entry<String, Long> entry : entries) {
        //for (String key: times.keySet()) {
            //long time = times.get(key);
            out.println(String.format("%-" + len + "s   %d", entry.getKey(), entry.getValue()));
        }
    }
    
}
