package cc.lib.utils;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import cc.lib.game.Utils;

/**
 * Created by chriscaron on 3/13/18.
 */

public class TestUtils extends TestCase {

    public void testTruncate() {

        assertEquals("hello", Utils.truncate("hello", 64, 1));
        assertEquals("hello", Utils.truncate("hello\ngoodbye", 64, 1));
        assertEquals("hello...", Utils.truncate("hello this is you rmother speaking", 5, 1, Utils.EllipsisStyle.END));
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

    class X {
        String s;
    }

    X x = new X() {

    };

    public void testClassnames() {
        printClassnames(x.getClass());
        System.out.println();
        printClassnames(x.getClass().getSuperclass());
    }

    private void printClassnames(Class c) {
        System.out.println(c.toString());
        System.out.println(c.toGenericString());
        System.out.println(c.getName());
        System.out.println(c.getSimpleName());
        System.out.println(c.getTypeName());
    }

    public void testUnique() {
        Integer [] nums = {
                0,1,1,2,2,2,3,3,3,3,3,3,4,4,4,4,5,6,7,8,9
        };
        List elems = new ArrayList(Arrays.asList(nums));

        Utils.unique(elems);

        assertEquals(10, elems.size());
        System.out.println("Elems: " + elems);
    }

    String quoteMe(String s) {
        return "\"" + s + "\"";
    }

    public void testPrettyString() {
        System.out.println(quoteMe(Utils.getPrettyString(";asihfva.kjvnakwhv")));
        System.out.println(quoteMe(Utils.getPrettyString("12324 hgjt $90")));
        System.out.println(quoteMe(Utils.getPrettyString("THIS_IS_A_TYPICAL_EXAMPLE")));
        System.out.println(quoteMe(Utils.getPrettyString("the quick br0wn fox jumped over the lazy brown dog")));
        System.out.println(quoteMe(Utils.getPrettyString("PLAYER1")));
        System.out.println(quoteMe(Utils.getPrettyString("00 001HELLO100 This is 10101010 test 0001")));
    }

}
