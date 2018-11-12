package cc.lib.utils;

import junit.framework.TestCase;

/**
 * Created by chriscaron on 3/13/18.
 */

public class LRUCacheTest extends TestCase {

    public void test() {
        LRUCache<String, Integer> cache = new LRUCache<>(10);

        for (int i=0; i<20; i++) {
            cache.put(String.valueOf(i), i);
        }

        assertEquals(10, cache.size());
        assertTrue(cache.getOldest().key.equals("10"));


    }

}
