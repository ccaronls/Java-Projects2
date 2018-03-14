package cc.lib.utils;

import junit.framework.TestCase;

import cc.lib.game.Utils;

/**
 * Created by chriscaron on 3/13/18.
 */

public class TestUtils extends TestCase {

    public void testTruncate() {

        assertEquals("hello", Utils.truncate("hello", 64, 1));
        assertEquals("hello", Utils.truncate("hello\ngoodbye", 64, 1));
        assertEquals("hello...", Utils.truncate("hello this is you rmother speaking", 5, 1));
        assertEquals("hello\ngoodbye", Utils.truncate("hello\ngoodbye\nsolong\nfarewell", 64, 2));


    }

}
