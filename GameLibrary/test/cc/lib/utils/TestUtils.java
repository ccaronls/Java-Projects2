package cc.lib.utils;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import cc.lib.game.Utils;
import cc.lib.math.CMath;

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

    public void testRepeating() {
        int c = lengthOfLongestSubstring("abcabbcbb");
        assertEquals(3, c);
    }

    public int lengthOfLongestSubstring(String s) {
        if (s.length() == 0)
            return 0;
        int max = 1;
        for (int len=2; len<s.length(); len++) {
            for (int start=0; start<s.length()-len; start++) {
                String test = s.substring(start, len);
                if (!hasRepeating(test)) {
                    max=len;
                    break;
                }
            }
        }

        return max;
    }

    boolean hasRepeating(String test) {
        int count[] = new int[256];
        for (int i=0; i<test.length(); i++) {
            if (count[(int)test.charAt(i)] > 0)
                return true;
            count[(int)test.charAt(i)] = 1;
        }
        return false;
    }

    public class ListNode {
      int val;
      ListNode next;
      ListNode(int x) { val = x; }
      ListNode(int x, ListNode next) {
          val=x; this.next = next;
      }
      public String toString() {
          return "" + val + " " + (next == null ? "" : next.toString());
      }
    }

    public void testSwapPairs() {
        ListNode l = new ListNode(1, new ListNode(2, new ListNode(3, new ListNode (4))));
        ListNode top = swapPairs(l);
        System.out.println(top);
    }

    public ListNode swapPairs(ListNode head) {
        if (head == null || head.next == null)
            return head;
        ListNode result = null;
        ListNode back = null;
        while (true) {
            if (head == null) {
                break;
            }
            ListNode newFront = swap(head);
            if (result == null) {
                result = newFront;
                back = newFront.next;
            } else {
                back.next = newFront;
                back = newFront.next;
            }
            if (newFront == head)
                break;
            head = newFront.next.next;
            //newFront.next.next = null;
        }
        return result;
    }

    ListNode swap(ListNode l) {
        if (l.next == null)
            return l;
        ListNode r = l.next;
        ListNode r2 = l.next.next;
        r.next = l;
        l.next = r2;
        return r;
    }

    public void testReverseGroup() {
        ListNode l = new ListNode(1, new ListNode(2, new ListNode(3, new ListNode (4))));
        ListNode top = reverseKGroup(l, 2);
        System.out.println(top);

    }

    public ListNode reverseKGroup(ListNode head, int k) {
        if (k < 2 || head == null)
            return head;
        ListNode front = head;
        ListNode back = null;
        while (head != null) {
            ListNode start = head;
            boolean hasEnough = true;
            for (int i=0; i<k; i++) {
                if (head == null) {
                    hasEnough = false;
                    break;
                }
                head = head.next;
            }
            ListNode reversed = hasEnough ? reverseR(start, k) : start;
            if (back == null) {
                front = back = reversed;
            } else {
                back.next = reversed;
            }
            while (back.next != null)
                back = back.next;
        }
        return front;
    }

    ListNode reverseR(ListNode front, int n) {
        if (n == 1) {
            front.next = null;
            return front;
        }
        ListNode next = front.next;
        front.next = null;
        ListNode node = reverseR(next, n-1);
        node.next = front;
        return node;
    }

    public void testTrap() {
        int sum = trap(0,1,0,2,1,0,1,3,2,1,2,1);
        assertEquals(6, sum);
    }

    public int trap(int ... height) {

        int start=0;
        for (; start<height.length; start++) {
            if (height[start] > 0)
                break;
        }
        int sum = 0;

        // search forward for a bar that is greater
        int h = height[start];
        while (start < height.length) {
            int s = 0;
            int nextMax = 0;
            for (int i = start + 1; i < height.length; i++) {
                if (height[i] >= h) {
                    // capture sum here
                    sum += s;
                    start = i;
                    nextMax = height[i];
                    break;
                }
                s += h-height[i];
                nextMax = Math.max(nextMax, height[i]);
            }
            // if we made it here then the height st start was too tall. Go again this time with t height of the next highest
            h = nextMax;
            while (h == 0 && start < height.length) {
                h = height[start++];
            }
        }
        return sum;
    }

    public void testJump() {
        int min = jump(2,3,1,1,4);
        assertEquals(3, min);
    }

    public int jump(int ... nums) {
        int [] min = { Integer.MAX_VALUE };
        jumpR(nums, 0, 0, min);
        return min[0];
    }

    void jumpR(int [] nums, int pos, int jumps, int [] min) {
        if (pos >= nums.length) {
            min[0] = Math.min(min[0], jumps);
            return;
        }
        int numj = nums[pos];
        for (int i=numj; i>0; i--) {
            jumpR(nums, pos+i, jumps+1, min);
        }
    }

    public void testSpiral() {
        int [][] matrix = {
                { 1,2,3 },
                { 4,5,6 },
                { 7,8,9 }
        };
        List<Integer> order = spiralOrder(matrix);
        System.out.println(order);
    }

    public List<Integer> spiralOrder(int[][] matrix) {
        int h=matrix.length;
        int w=matrix[0].length;
        List<Integer> result = new ArrayList<>();
        int x=0; int y=0;
        int xs=0; int ys=1;
        int dir=0; // right, down, left, up
        while (x <= w && y <= h) {
            result.add(matrix[y][x]);
            switch (dir) {
                case 0: // right
                    if (x==w-1) {
                        dir=1;
                        y++;
                    } else {
                        x++;
                    }
                    break;
                case 1:
                    if (y == h-1) {
                        dir=2;
                        x--;
                        h--;
                    } else {
                        y++;
                    }
                    break;
                case 2:
                    if (x==xs) {
                        dir=3;
                        y--;
                        xs++;
                    } else {
                        x--;
                    }
                    break;
                case 3:
                    if (y==ys) {
                        dir=0;
                        x++;
                        w--;
                        ys++;
                    } else {
                        y--;
                    }
            }
        }
        return result;
    }

    public void testThreeSums() {
        CMath.combinations(new Integer[]{-1, 2, 1, -4}, new Integer[3], new CMath.PermuteCallback<Integer>() {
            @Override
            public void onPermutation(Integer[] array) {
                System.out.println(Arrays.toString(array));
            }
        });
//        int closest = threeSumClosest(new int [] { -1,2,1,-4}, 1);
    }

    public int threeSumClosest(int[] nums, int target) {
        int closest = Integer.MAX_VALUE;
        for (int i=0; i<nums.length-1; i++) {
            for (int ii=i+1; ii<nums.length-1; ii++) {
                int sum = nums[i]+nums[ii]+nums[ii+1];
                System.out.println("sum: " + nums[i] + "," + nums[ii] + "," + nums[ii+1]);
                int diff = Math.abs(target - sum);
                if (diff < closest) {
                    closest = diff;
                }
            }
        }
        return closest;
    }

    public void testGetNthFromEnd() {
        ListNode n = new ListNode(1, new ListNode(2, new ListNode(3, new ListNode(4, new ListNode(5)))));
        ListNode [] result = { null };
        getNthFromEnd(n, result, new int[] { 2 });
    }

    public void getNthFromEnd(ListNode node, ListNode [] nth, int [] n) {
        if (node.next == null) {
            return;
        }

        getNthFromEnd(node.next, nth, n);
        if (--n[0] == 0) {
            nth[0] = node;
        }
    }

    public void testMerge2Lists() {
        ListNode l1 = new ListNode(1, new ListNode(2, new ListNode(4)));
        ListNode l2 = new ListNode(1, new ListNode(3, new ListNode(4)));
        ListNode n = mergeTwoLists(l1, l2);
    }

    public ListNode mergeTwoLists(ListNode l1, ListNode l2) {
        ListNode first = null, last = null;
        while (l1 != null || l2 != null) {
            ListNode toAdd = null;
            if (l1 == null) {
                toAdd = l2;
                l2 = l2.next;
            } else if (l2 == null) {
                toAdd = l1;
                l1 = l1.next;
            } else if (l1.val < l2.val) {
                toAdd = l1;
                l1 = l1.next;
            } else {
                toAdd = l2;
                l2 = l2.next;
            }
            toAdd.next = null;
            if (first == null) {
                first = last = toAdd;
            } else {
                last.next = toAdd;
                last = toAdd;
            }
        }
        return first;
    }

    public void testSearchIndex() {
        int index = searchIndexR(new int[] { 1,3,5,6}, 0, 0, 3);
    }

    int searchIndexR(int [] nums, int target, int low, int high) {
        if (low == high) {
            if (target == nums[low])
                return low;
            if (target < nums[low])
                return low;
            return high+1;
        }
        int mid = (low+high)/2;
        if (nums[mid] == target)
            return mid;
        if (target < nums[mid])
            return searchIndexR(nums, target, low, mid-1);
        return searchIndexR(nums, target, mid+1, high);
    }

    public void testFindFirstMissingPos() {
        int first = firstMissingPositive(new int[] { 3,4,-1, 1});
    }

    public int firstMissingPositive(int[] nums) {
        int min=Integer.MAX_VALUE, max=Integer.MIN_VALUE;
        int count = 0;

        // first pass to find min/max
        for (int i=0; i<nums.length; i++) {
            if (nums[i] < 0)
                continue;
            min = Math.min(nums[i], min);
            max = Math.max(nums[i], max);
            count ++;
        }

        if (count == 0)
            return 1;

        if (min > 1) {
            return min-1;
        }

        return max+1;
    }

    public void testMultiply() {
        String result = multiply("18", "13");
    }

    /**
     *    18  <-- num1
     *   *13  <-- num2
     *   ---
     *    54
     *   180
     *   ---
     *   234
     *
     *
     * @param num1
     * @param num2
     * @return
     */
    public String multiply(String num1, String num2) {
        int result = 0;
        int scale2 = 1;
        for (int i=num2.length()-1; i>=0; i--) {
            int n2 = (int)(num2.charAt(i)-'0');
            int scale1 = 1;
            int carry=0;
            for (int ii=num1.length()-1; ii>=0; ii--) {
                int n1 = (int)(num1.charAt(ii)-'0');
                int n = (n1*n2 + carry) * scale1;
                carry = n/10;
                n = n % 10;
                scale1 *= 10;
                result += n*scale2;
            }
            scale2 *= 10;
            result += scale2*carry;
        }
        return "" + result;
    }

    public void testRotateRight() {
        ListNode n = new ListNode(1, new ListNode(2, new ListNode(3, new ListNode(4, new ListNode(5)))));
        ListNode h = rotateRight(n, 2);
    }


    public ListNode rotateRight(ListNode head, int k) {
        if (head == null || head.next == null || k < 1)
            return head;

        for (int i=0; i<k; i++) {
            // find node previous to tail
            ListNode pTail = head;
            while (pTail.next.next != null) {
                pTail = pTail.next;
            }
            ListNode tail = pTail.next;
            pTail.next = null;
            tail.next = head;
            head = tail;
        }
        return head;
    }

    public void testIsNumber() {
        assertTrue(isNumber("0"));
        assertTrue(isNumber(" 0.1 "));
        assertTrue(isNumber(" 10.0 "));
        assertTrue(isNumber(" 100.10 "));
        assertFalse(isNumber("abc" ));
        assertFalse(isNumber("1 a"));
        assertTrue(isNumber("2e10"));
        assertTrue(isNumber(" -90e3   "));
        assertFalse(isNumber(" 1e"));
        assertFalse(isNumber("e3"));
        assertTrue(isNumber(" 6e-1"));
        assertFalse(isNumber(" 99e2.5 "));
        assertTrue(isNumber("53.5e93"));
        assertFalse(isNumber(" --6 "));
        assertFalse(isNumber("-+3"));
        assertFalse(isNumber("95a54e53"));
    }

    public boolean isNumber(String s) {
        List l;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\+|-)?([1-9][0-9]*|(0|[1-9][0-9]*)([.][0-9]+(e[0-9]+)?)?|[1-9][0-9]*e-?[1-9][0-9]*)");
        return p.matcher(s.trim()).matches();
    }

    public void testFullJustify() {
        String [] words = { "This", "is", "an", "example", "of", "text", "justification." };
        List<String> result = fullJustify(words, 16);
    }

    public List<String> fullJustify(String[] words, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String line = "";
        for (int i=0; i<words.length; i++) {
            if (line.length() == 0) {
                line = line+words[i];
            } else if (line.length() + 1 + words[i].length() <= maxWidth) {
                line = line + " " + words[i];
            } else {
                lines.add(format(line, maxWidth));
                line = words[i];
            }
        }
        // anything leftover just gets added
        if (line.isEmpty()) {
            // reformat previous line
            line = lines.remove(lines.size()-1);
            line = line.replace("[ ]+", " ");
            lines.add(line);
        } else {
            lines.add(line);
        }
        return lines;
    }

    String format(String line, int maxWidth) {
        String [] words = line.split("[ ]");
        if (words.length == 1)
            return line;
        int len = 0;
        for (String w : words) {
            len += w.length();
        }
        int space = (maxWidth-len) / (words.length-1);
        String p = "";
        for (int i=0; i<space; i++) {
            p += " ";
        }
        String [] spaces = new String[words.length-1];
        int used = len + space*spaces.length;
        for (int i=0; i<spaces.length; i++) {
            spaces[i] = p;
            if (used < maxWidth) {
                spaces[i] += " ";
                used++;
            }
        }
        line = words[0];
        for (int i=0; i<spaces.length; i++) {
            line += spaces[i];
            line += words[i+1];
        }
        return line;
    }

    public void testCombine() {
        List<List<Integer>> result = combine(4, 4);
    }

    public List<List<Integer>> combine(int n, int k) {
        List<List<Integer>> result = new ArrayList<>();
        Integer [] data = new Integer[k];
        comboR(data, 1, n, 0, result);
        return result;
    }

    void comboR(Integer [] data, int start, int end, int index, List<List<Integer>> result) {
        if (index == data.length) {
            result.add(new ArrayList<>(Arrays.asList(data)));
            return;
        }

        for (int i=start; i<=end; i++) {
            data[index] = i;
            comboR(data, i+1, end, index+1, result);
        }
    }

    public void testCombinations() {
        CMath.combinations(new Integer[]{1, 2, 3, 4}, new Integer[1], new CMath.PermuteCallback<Integer>() {
            @Override
            public void onPermutation(Integer [] array) {
                System.out.println(Arrays.toString(array));
            }
        });
    }

    public void testDecodings() {
        int num = numDecodings("1926");
    }
    public int numDecodings(String s) {
        char [] arr = s.toCharArray();
        int [] count = { 0 };
        searchR(arr, 0, count);
        return count[0];
    }

    void searchR(char [] arr, int start, int [] count) {
        if (start >= arr.length) {
            count[0] ++;
            return;
        }

        searchR(arr, start+1, count);
        if (start < arr.length-1 && (arr[start] == '1' || (arr[start] == '2' && arr[start+1] <= '6'))) {
            searchR(arr, start+2, count);
        }
    }

    public void testReverseList() {
        ListNode l = new ListNode(1, new ListNode(2, new ListNode(3, new ListNode(4))));
        ListNode r = reverse(l);
    }

    ListNode reverse(ListNode front) {
        if (front == null || front.next == null)
            return front;
        ListNode r= front;
        ListNode l = front.next;
        r.next = null;
        while (l != null) {
            ListNode t = l;
            l = l.next;
            t.next = r;
            r = t;
        }
        return r;
    }

    public void testReverseBetween() {
        ListNode l = new ListNode(1, new ListNode(2, new ListNode(3, new ListNode(4, new ListNode(5)))));

        ListNode h = reverseBetween(l, 3, 4);
    }

    public ListNode reverseBetween(ListNode head, int m, int n) {
        ListNode prev = null;
        ListNode cur = head;
        while (m > 1 && cur != null) {
            prev = cur;
            cur = cur.next;
            m--;
            n--;
        }

        if (cur == null)
            return head; // cant do it.

        ListNode r = cur;
        ListNode l = cur.next;
        r.next = null;
        while (l != null && n-- > 1) {
            ListNode t = l;
            l = l.next;
            t.next = r;
            r = t;
        }
        cur.next = l;

        if (prev == null) {
            return r;
        }

        List x;
        prev.next = r;
        return head;
    }

    public void testNumDistinct() {
        int num = numDistinct("raagg", "rag");
        System.out.println("num=" + num);
    }

    public int numDistinct(String s, String t) {
        int [] num = { 0 };
        countR(s, t, num, "");
        return num[0];
    }

    void countR(String s, String t, int [] num, String indent) {
        System.out.print("testing: " + indent + s);
        if (s.length()<=t.length()) {
            if (s.equals(t)) {
                num[0]++;
                System.out.print("  <---");
            }
            System.out.println();
            return;
        }
        System.out.println();

        if (s.length() < 2)
            return;

        String r = s.substring(1);
        countR(r, t, num, indent + "  ");
        for (int i=1; i<s.length()-1; i++) {
            r = s.substring(0, i) + s.substring(i+1);
            countR(r, t, num, indent + "  ");
        }
        r = s.substring(0, s.length()-1);
        countR(r, t, num, indent + "  ");
    }

    public class Node {
        public int val;
        public Node left;
        public Node right;
        public Node next;

        public Node() {}

        public Node(int _val) {
            val = _val;
        }

        public Node(int _val, Node _left, Node _right) {
            val = _val;
            left = _left;
            right = _right;
        }

        public String toString() {
            return "" + val;
        }
    };

    public void testConnect() {
        Node root = new Node(1, new Node(2, new Node(4), new Node(5)), new Node(3, new Node(6), new Node(7)));
        root = connect(root);
    }

    public Node connect(Node root) {
        if (root.left == null)
            return root;
        root.left.next = root.right;
        root.right.next = connect(root.right).left;
        connect(root.left);
        return root;
    }

    public void testLadders() {
        List<List<String>> ladders = findLadders("hit", "cog", Arrays.asList("hot","dot","dog","lot","log","cog"));
        System.out.println(ladders);
    }

    public List<List<String>> findLadders(String beginWord, String endWord, List<String> wordList) {
        int [] min = { Integer.MAX_VALUE };
        List<List<String>> result = new ArrayList<>();

        searchR(beginWord, endWord, wordList, new ArrayList<>(), result, min);
        // remove any ladders with length > min
        Iterator<List<String>> it = result.iterator();
        while (it.hasNext()) {
            if (it.next().size() > min[0]) {
                it.remove();
            }
        }
        return result;
    }

    void searchR(String startWord, String endWord, List<String> dict, List<String> ladder, List<List<String>> result, int [] min) {
        ladder.add(startWord);
        if (startWord.equalsIgnoreCase(endWord)) {
            min[0] = Math.min(min[0], ladder.size());
            result.add(ladder);
            return;
        }

        for (String word : dict) {
            if (isWordsCompatible(word, startWord)) {
                List<String> newDict = new ArrayList(dict);
                newDict.remove(word);
                searchR(word, endWord, newDict, new ArrayList<>(ladder), result, min);
            }
        }

    }

    boolean isWordsCompatible(String w0, String w1) {
        if (w0.length() != w1.length())
            return false;
        int numDiff = 0;
        for (int i=0; i<w0.length(); i++) {
            char a = w0.charAt(i);
            char b = w1.charAt(i);
            if (a != b) {
                numDiff ++;
            }
        }
        return numDiff==1;
    }

    public void testPascals2() {
        List<Integer> row = getRow(13);
        System.out.println(row);
    }

    public List<Integer> getRow(int rowIndex) {
        int n = rowIndex;
        List<Integer> row = new ArrayList<>();
        for (int k=0; k<=n; k++) {
            int x = (int)(fac(n) / (fac(k) * fac(n-k)));
            row.add(x);
        }
        return row;
    }

    long fac(int n) {
        if (n == 0)
            return 1;
        long r = n--;
        while (n>0) {
            r *= n--;
        }
        return r;
    }

    public static class TreeNode {
      int val;
      TreeNode left;
      TreeNode right;
      TreeNode() {}
      TreeNode(int val) { this.val = val; }
      TreeNode(int val, TreeNode left, TreeNode right) {
          this.val = val;
          this.left = left;
          this.right = right;
      }
   }

   public void testPreorderTraversal() {
        TreeNode root = new TreeNode(0, null, new TreeNode(1, new TreeNode(2), null));
        List<Integer> result = preorderTraversal(root);
        System.out.println(result);
   }

    public List<Integer> preorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        Stack<TreeNode> st = new Stack<>();
        while (root != null || st.size() > 0) {
            if (root == null)
                root = st.pop();
            else
                result.add(root.val);
            if (root.left != null && root.right != null) {
                st.push(root);
            }
            if (root.left != null) {
                root = root.left;
            } else if (root.right != null) {
                root = root.right;
            } else {
                root = null;
            }
        }
        return result;
    }

    public void testInsertSortList() {
        ListNode l = new ListNode(4, new ListNode(2, new ListNode(1, new ListNode(3))));
        l = insertionSortList(l);
    }

    public ListNode insertionSortList(ListNode head) {
        if (head == null || head.next == null)
            return head;
        ListNode cur = head;
        while (cur.next != null) {
            if (cur.next.val < cur.val) {
                ListNode t = cur.next;
                cur.next = cur.next.next;
                t.next = null;
                head = insert(head, t);
            } else {
                cur = cur.next;
            }
        }
        return head;
    }

    ListNode insert(ListNode head, ListNode item) {
        if (item.val < head.val) {
            item.next = head;
            return item;
        }
        ListNode prev = head;
        ListNode cur = head.next;
        while (cur.next != null && cur.val < item.val) {
            prev = cur;
            cur = cur.next;
        }
        prev.next = item;
        item.next = cur;
        return head;
    }

    public void testMaxPoints() {
        int [][] pts = {
                //{1,1}, {2,2}, {3,3}
                {0,0},{94911151,94911150},{94911152,94911151}
        };
        int max = maxPoints(pts);
        assertEquals(2, max);
    }

    public int maxPoints(int[][] points) {
        int max=Math.min(points.length, 2);
        for (int i=0; i<points.length-1; i++) {
            for (int ii=i+1; ii<points.length; ii++) {
                max = Math.max(max, findNumPtsForLine(points, i, ii));
            }
        }
        return max;
    }

    int findNumPtsForLine(int [][] pts, int start, int end) {
        int num=0;
        for (int i=0; i<pts.length; i++) {
            if (i==start || i==end) {
                num++;
                continue;
            }
            //if (0 == distSqPointLine(pts[i][0], pts[i][1], pts[start][0], pts[start][1], pts[end][0], pts[end][1])) {
            if (isOnLine(pts[i][0], pts[i][1], pts[start][0], pts[start][1], pts[end][0], pts[end][1])) {
                num++;
            }
        }
        return num;
    }

    boolean isOnLine(double point_x, double point_y, double x0, double y0, double x1, double y1) {
        double dx0 = point_x-x0;
        double dy0 = point_y-y0;
        double dx1 = x1-point_x;
        double dy1 = y1-point_y;
        double dx2 = x1-x0;
        double dy2 = y1-y0;

        double m0 = dx0/dy0;
        double m1 = dx1/dy1;
        double m2 = dx2/dy2;

        if (m0 == m2 || m1 == -m2)
            return true;

        return false;
    }

    double distSqPointLine(double point_x, double point_y, double x0, double y0, double x1, double y1) {
        // get the normal (N) to the line
        double nx = -(y1 - y0);
        double ny = (x1 - x0);
        if (Math.abs(nx) == 0 && Math.abs(ny) == 0) {
            double dx = point_x-x0;
            double dy = point_y-y0;
            return (dx*dx+dy*dy);
        }
        // normalize n
        double mag = Math.sqrt(nx*nx+ny*ny);
        nx /= mag;
        ny /= mag;

        // get the vector (L) from point to line
        double lx = point_x - x0;
        double ly = point_y - y0;

        // compute N dot N
        //double ndotn = (nx * nx + ny * ny);
        // compute N dot L
        double ndotl = nx * lx + ny * ly;
        // get magnitude squared of vector of L projected onto N
        double px = (nx * ndotl);// / ndotn;
        double py = (ny * ndotl);// / ndotn;
        double dist = Math.sqrt(px * px + py * py);
        return dist;

    }

    public void testReverseInt() {
        int rev = reverse(1534236469);
        assertEquals(0, rev);
    }

    public int reverse(int x) {
        boolean neg = x<0;
        x = Math.abs(x);
        int output = 0;
        while (x > 0) {
            int last = x%10;
            x /= 10;
            if (((long)output) * 10 > Integer.MAX_VALUE)
                return 0;
            output = output*10 + last;
        }
        if (neg)
            output = -output;
        return output;
    }

    public void testRemoveComments() {
        String program = "/*Test program */\n" +
        "int main()\n"+
        "{\n"+
        "    // variable declaration\n"+
        "    int a, b, c;\n"+
"/* This is a test\n"+
"   multiline\n"+
"   comment for\n"+
"   testing */\n"+
"            a = b + c;\n"+
"        }\n";


        String [] lines = {"a//*b//*c","blank","d/*/e*//f"};
        List<String> result = removeComments(lines);
        System.out.println(result);
    }

    public List<String> removeComments(String[] source) {
        List<String> result = new ArrayList<>();
        if (source.length == 0)
            return result;
        String block = null;
        String line = source[0];
        int index = 1;
        while (line.length() > 0 || index < source.length) {
            if (line.isEmpty())
                line = source[index++];
            if (block != null) {
                int endComment = line.indexOf("*/");
                if (endComment < 0) {
                    line = source[index++];
                    continue;
                } else {
                    line = block + line.substring(endComment + 2);
                    block = null;
                }
            } else {
                int beginComment = line.indexOf("/*");
                int lineComment = line.indexOf("//");

                if (beginComment < 0 && lineComment < 0) {
                    if (!line.isEmpty())
                        result.add(line);
                    line = "";
                    continue;
                }

                if (lineComment == 0) {
                    line = "";
                    continue;
                }

                if (beginComment == 0) {
                    block = "";
                    line = line.substring(2);
                    continue;
                }

                if (lineComment > 0 && (beginComment < 0 || lineComment < beginComment)) {
                    result.add(line.substring(0, lineComment));
                    line = "";
                    continue;
                } else {
                    block = line.substring(0, beginComment);
                    line = line.substring(beginComment+2);
                }
            }
        }
        return result;
    }

    public void testLexicalInts() {
        int max = 985;
        String [] nums = new String[max];
        for (int i=0; i<nums.length; i++) {
            nums[i] = String.valueOf(i + 1);
        }
        Arrays.sort(nums);
        for (int i=0; i<nums.length; i++) {
            System.out.println("" + nums[i] + " -> " + nextLex(Integer.parseInt(nums[i]), max));
        }
    }

    public void testFindKthNumber() throws Exception {
        int r = findKthNumber(100, 3);
        System.out.println("r=" + r);
/*
        int i=0;
        PrintWriter writer = new PrintWriter(new FileWriter("/tmp/z"));
        for (int ii=0; ii<1545581; ii++) {
            i = nextLex(i, 1692778);
            writer.println("" + i);
        }
        //int r = findKthNumber(1692778,1545580);
        //System.out.println("r=" + r);
        writer.flush();
        writer.close();*/
    }

    public int findKthNumber(int n, int k) {
        int r=0;
        for (int i=0; i<k; i++) {
            r = nextLex(r, n);
            //System.out.println("r="+r);
        }
        return r;
    }

    int nextLex(int i, int max) {
        if (i<=0)
            return 1;
        if (i*10 <= max) {
            return i*10;
        }
        if (i == max) {
            i/=10;
        }
        int lastDigit = i%10;
        while (lastDigit == 9) {
            i/=10;
            lastDigit = i%10;
        }
        lastDigit++;
        i = (i/10) * 10 + lastDigit;
        return i;
    }

    public void testIsAdditiveNumber() {
        //assertFalse(isAdditiveNumber("0235813"));
        //assertTrue(isAdditiveNumber("199111992"));
        assertTrue(isAdditiveNumber("121474836472147483648"));
    }

    public boolean isAdditiveNumber(String num) {
        for (int i=1; i<num.length()/2+1; i++) {
            try {
                long num1 = toInt(num.substring(0, i));
                try {
                    for (int ii = 1; ii < num.length() / 2+ 1; ii++) {
                        long num2 = toInt(num.substring(i, i + ii));
                        if (isAdditive(num1, num2, num.substring(i + ii)))
                            return true;
                    }
                } catch (NumberFormatException e) {
                    // ignore
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    long toInt(String s) {
        if (s.length() > 1 && s.startsWith("0"))
            throw new NumberFormatException();
        return Long.parseLong(s);
    }

    boolean isAdditive(long num0, long num1, String s) {
        long sum = num0+num1;
        String sumStr = String.valueOf(sum);
//        System.out.println(num0 + "+" + num1 + "=" + sum + "-> startsWith(" + s + ") " + s.startsWith(sumStr));
        if (s.startsWith(sumStr)) {
            if (s.length() == sumStr.length())
                return true;
            return isAdditive(num1, sum, s.substring(sumStr.length()));
        }
        return false;
    }

    public void testDivide() {
        int n = divide(-2147483648, -1);
    }

    public int divide(int dividend, int divisor) {
        boolean neg = dividend < 0 ^ divisor < 0;
        long _dividend = Math.abs((long)dividend);
        long _divisor = Math.abs((long)divisor);
        if (_divisor == 1)
            return (int)(neg ? -_dividend : _dividend);
        long n = 1;
        while (true) {
            long s = mult(divisor, n);
            if (s > _dividend) {
                n -= 1;
                break;
            }
            if (s == _dividend) {
                break;
            }
            n++;
        }
        return (int)(neg ? -n : n);
    }

    long mult(long x, long n) {
        long r = 0;
        for (int i=0; i<n; i++)
            r += x;
        return r;
    }
}

