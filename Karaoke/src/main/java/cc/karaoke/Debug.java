package cc.karaoke;

import java.io.PrintWriter;

public class Debug {

    public static final boolean DEBUG = false;
    public static final boolean WRITE_LYRICS = true;
    public static PrintWriter LYRICS_WRITER = null;
    public static PrintWriter SCRIPT_WRITER = null;

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

    /*--*/

    public static void lprintln(String str) {
        if (WRITE_LYRICS) {
            LYRICS_WRITER.println(str);
        }
    }

    public static void lprint(String str) {
        if (WRITE_LYRICS) {
            LYRICS_WRITER.print(str);
        }
    }

    public static void lprintf(String format, Object... args) {
        if (WRITE_LYRICS) {
            LYRICS_WRITER.printf(format, args);
        }
    }

    /* - */

    public static void sprintln(String str) {
        if (WRITE_LYRICS) {
            SCRIPT_WRITER.println(str);
        }
    }

    public static void sprint(String str) {
        if (WRITE_LYRICS) {
            SCRIPT_WRITER.print(str);
        }
    }

    public static void sprintf(String format, Object... args) {
        if (WRITE_LYRICS) {
            SCRIPT_WRITER.printf(format, args);
        }
    }


}
	    
	    
