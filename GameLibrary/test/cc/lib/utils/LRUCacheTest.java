package cc.lib.utils;

import junit.framework.TestCase;

import java.util.Random;

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
        assertTrue(cache.getOldest().equals("15"));
        assertTrue(cache.getNewest().equals("19"));

        //System.out.println("entry set=" + cache.entrySet());

        assertTrue(cache.containsKey("19"));
        assertTrue(cache.containsValue(19));

        int [] x = { 15, 16, 17, 18, 19 };
        Utils.shuffle(x);

        for (int i:x) {
            assertNotNull(cache.get(String.valueOf(i)));
            assertTrue(cache.getNewest().equals(String.valueOf(i)));
        }

        System.out.println("entry set=" + cache.entrySet());
        System.out.println("values=" + cache.values());

        cache = new LRUCache<>(100);

        Random r = new Random(0);
        for (int i=0; i<10000; i++) {
            String key = String.valueOf(r.nextInt(10000));
            cache.put(key, 0);
            assertTrue(cache.size() <= 100);
        }

        // remove everything
        while (cache.size() > 0) {
            Integer v = null;
            if (Utils.flipCoin()) {
                v = cache.remove(cache.getNewest());
            } else {
                v = cache.remove(cache.getOldest());
            }
            assertNotNull(v);
        }
    }

}
