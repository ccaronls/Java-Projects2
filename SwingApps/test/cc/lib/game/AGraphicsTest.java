package cc.lib.game;

import junit.framework.TestCase;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.lib.utils.FileUtils;

/**
 * Created by chriscaron on 2/14/18.
 */

public class AGraphicsTest extends TestCase {

    public void testAnnotations() {
        Pattern pa = AGraphics.ANNOTATION_PATTERN;

        String color1 = "[0,0,0]";
        String color2 = "[0,0,0,0]";
        String color3 = "[20,20,20]";
        String color4 = "[100,100,100,100]";
        String color5 = "[1,2,3,4]";

        String [] all = {
                color1, color2, color3, color4, color5
        };

        for (String test : all) {
            Matcher m = pa.matcher(test);
            assertTrue("Failed for string: " + test, m.find());
        }

        String test = "hello " + color1 + " this " + color2 + " is a test + " + color3;

        Matcher m = pa.matcher(test);
        assertTrue("Failed for string: " + test, m.find());

    }

}
