package cc.lib.utils;

import junit.framework.TestCase;

/**
 * Test shows that static blocks in base classes are called once before those in the super class when new 'Super' is instantiated.
 * @author ccaron
 *
 */
public class TestStaticInheritence extends TestCase {

    static class Base {
        static {
            System.out.println("static block in Base called");
        }
    };
    
    static class Super extends Base {
        static {
            System.out.println("static block in super called");
        }
    };
    
    public void x_test() {
        new Super();
        new Super(); // static blocks not called again
    }
    
    public void testByteClass() {
    	System.out.println(byte.class);
    	System.out.println(byte.class.getCanonicalName());
    	System.out.println(byte.class.getName());
    	System.out.println(byte[].class);
    	System.out.println(byte[].class.getCanonicalName());
    	System.out.println(byte[].class.getName());
    	System.out.println(boolean[].class);
    	System.out.println(boolean[].class.getCanonicalName());
    	System.out.println(boolean[].class.getName());

    }
    
}
