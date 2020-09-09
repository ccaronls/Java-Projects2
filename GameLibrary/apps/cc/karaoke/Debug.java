package cc.karaoke;

public class Debug {

    public static final boolean DEBUG = true;

    public static void println(String str) {
        if (DEBUG) {
            System.out.println(str);
        }
    }

    public static void print(String str) {
        if (DEBUG) {
            System.out.print(str);
        }
    }

    public static void printf(String format, Object... args) {
        if (DEBUG) {
            System.out.printf(format, args);
        }
    }
}
	    
	    
