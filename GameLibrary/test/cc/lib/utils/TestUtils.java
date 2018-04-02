package cc.lib.utils;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Stack;

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

    /**
     * Return true if the parentheses "()", brackets "[]", and braces "{}" in expr are balanced.
     *
     * Examples of balanced:
     *
     * "", "1+2[]", "(1+2)"
     *
     * Examples of not balanced:
     *
     * "(1+2 * [3 - 4)]", "}1-2{"
     */
    boolean isBalanced(String expr) {
        // your code here

        Stack<Character> stack = new Stack<>();

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            switch (c) {
                case '(':
                case '{':
                case '[':
                    stack.push(c);
                    break;
                case ']':
                    if (stack.isEmpty() || stack.pop()!='[')
                        return false;
                    break;
                case '}':
                    if (stack.isEmpty() || stack.pop()!='{')
                        return false;
                    break;
                case ')':
                    if (stack.isEmpty() || stack.pop()!='(')
                        return false;
                    break;
            }

        }
        return stack.isEmpty();
    }

    public void testBalanced() {
            assertTrue(isBalanced(""));
            assertTrue(isBalanced("1+2[]"));
            assertTrue(isBalanced("(1+2)"));

            assertFalse(isBalanced("(1+2 * [3 - 4)]"));
            assertFalse(isBalanced( "}1-2{"));
    }


    boolean checkAnagrams(String string1, String string2){

        string1 = string1.replaceAll("[ ]", "");
        string2 = string2.replaceAll("[ ]", "");

        if (string1.length() != string2.length())
            return false;

        byte [] b1 = string1.getBytes();
        byte [] b2 = string2.getBytes();

        Arrays.sort(b1);
        Arrays.sort(b2);

        for (int i=0; i<b1.length; i++) {
            if (b1[i] != b2[i])
                return false;
        }

        return true;
    }

    public void testAnagrams() {
        assertTrue(checkAnagrams("astronomer", "moon starer"));
        assertFalse(checkAnagrams("something", "another thing"));


    }

}
