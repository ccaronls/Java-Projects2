package cc.lib.utils;

import junit.framework.TestCase;

import cc.lib.game.Utils;

/**
 * Created by chriscaron on 3/13/18.
 */

public class LRUCacheTest extends TestCase {

    public void test() {
        LRUCache<String, Integer> cache = new LRUCache<>(5);

        for (int i=0; i<20; i++) {
            cache.put(String.valueOf(i), i);
        }

        assertEquals(5, cache.size());
        assertTrue(cache.getOldest().key.equals("15"));
        assertTrue(cache.getNewest().key.equals("19"));

        //System.out.println("entry set=" + cache.entrySet());

        assertTrue(cache.containsKey("19"));
        assertTrue(cache.containsValue(19));

        int [] x = { 15, 16, 17, 18, 19 };
        Utils.shuffle(x);

        for (int i:x) {
            assertNotNull(cache.get(String.valueOf(i)));
            assertTrue(cache.getNewest().key.equals(String.valueOf(i)));
        }

        System.out.println("entry set=" + cache.entrySet());

    }

}
