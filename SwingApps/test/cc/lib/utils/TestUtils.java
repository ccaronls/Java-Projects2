package cc.lib.utils;

import junit.framework.TestCase;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        System.out.println(quoteMe(Utils.toPrettyString(";asihfva.kjvnakwhv")));
        System.out.println(quoteMe(Utils.toPrettyString("12324 hgjt $90")));
        System.out.println(quoteMe(Utils.toPrettyString("THIS_IS_A_TYPICAL_EXAMPLE")));
        System.out.println(quoteMe(Utils.toPrettyString("the quick br0wn fox jumped over the lazy brown dog")));
        System.out.println(quoteMe(Utils.toPrettyString("PLAYER1")));
        System.out.println(quoteMe(Utils.toPrettyString("00 001HELLO100 This is 10101010 test 0001")));
    }

    public void testWrapString() {

        String [] str = {
                "\nhello\n\ngoodbye\n", "","a", Utils.getRepeatingChars('a', 100), "the quick brown fox jumped over the lazy brown dog"
        };

        for (String s : str) {
            String wrapped = Utils.wrapTextWithNewlines(s, 10);
            System.out.println(s + "->\n'" + wrapped + "'");
        }



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

    public void testTable() {

        String [] txt = Utils.wrapText("ThisIsThePartThatIsGettingTestedToMakeSureThereIsAHyphen The quick brown fox jumped over the lazy brown dog and then tried to eat a sandwich before going over to his friends house for some tea", 20);

        // Try a table with 7 entries
        Table t = new Table();
        t.addRow(new Table().addRow(new Table().addColumnNoHeader(txt), new Table().addColumnNoHeader(txt), new Table().addColumnNoHeader(txt)));
        t.addRow(new Table().addRow(new Table().addColumnNoHeader(txt), new Table().addColumnNoHeader(txt)));

        //t.addRow(new Table().addColumnNoHeader(txt), new Table().addColumnNoHeader(txt));

        System.out.println(t.toString(10));

    }

    public void testMap() {

        List<String> someEnums = Utils.map(SomeEnum.values(), (in)-> in.name());
        System.out.println(someEnums);
        SomeEnum [] enums = Utils.map(someEnums, (in)->SomeEnum.valueOf(in)).toArray(new SomeEnum[0]);
        Assert.assertArrayEquals(enums, SomeEnum.values());
    }

}
