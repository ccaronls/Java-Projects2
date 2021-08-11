package cc.lib.utils;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Iterator;

public class WeakHashSetTest extends TestCase {

    public void testAdd() {

        WeakHashSet<String> w = new WeakHashSet();

        assertTrue(w.add("Hello"));
        assertEquals(1, w.size());
        assertTrue(w.contains("Hello"));

        assertFalse(w.add("Hello"));
        assertEquals(1, w.size());

        assertTrue(w.remove("Hello"));
        assertEquals(0, w.size());

        assertTrue(w.addAll(Arrays.asList("Hello", "Goodbye", "So Long")));
        assertEquals(3, w.size());

        assertTrue(w.retainAll(Arrays.asList("Hello")));
        assertEquals(1, w.size());

        assertFalse(w.addAll(Arrays.asList("Hello")));
        assertEquals(1, w.size());

        assertFalse(w.removeAll(Arrays.asList("Goodbye", "Whatever")));
        assertEquals(1, w.size());

        String [] arr = w.toArray(new String[1]);
        assertTrue(Arrays.equals(arr, new String[] { "Hello"} ));


        assertTrue(w.removeAll(Arrays.asList("Hello")));
        assertEquals(0, w.size());

        assertTrue(w.addAll(Arrays.asList("Hello", "Goodbye", "So Long")));
        assertEquals(3, w.size());

        w.clear();
        assertEquals(0, w.size());

        assertTrue(w.addAll(Arrays.asList("Hello", "Goodbye", "So Long")));
        assertEquals(3, w.size());
    }


    public void testGC() {
        WeakHashSet w = new WeakHashSet();
        Object o = new Object();
        w.add(o);
        o = null;

        for (int i=0; i<10 && w.size() > 0; i++) {
            System.gc();
        }

        assertEquals(0, w.size());
    }


    public void testIterator() {

        WeakHashSet w = new WeakHashSet();
        Object o = new Object();
        w.add(o);
        //o = null;

        int count=0;
        Iterator it = w.iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
            count++;
        }

        assertEquals(0, w.size());
        assertEquals(1, count);
    }

    public void testToArray() {

        WeakHashSet<String> w = new WeakHashSet();
        String [] s = {
                "Hello", "Goodbye", "So long"
        };

        assertTrue(w.addAll(Arrays.asList(s)));
        assertEquals(3, w.size());

        Object [] arr = w.toArray();
        assertEquals(3, arr.length);
        for (Object o : arr) {
            assertTrue(w.contains(o));
        }

        s = null;
        for (int i=0; i<10 && w.size() > 2; i++) {
            System.gc();
        }
        Object [] arr2 = w.toArray();
        assertEquals(2, arr2.length);
    }

}
