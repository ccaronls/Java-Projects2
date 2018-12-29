package cc.lib.android;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;

/**
 * Created by chriscaron on 3/11/18.
 */

public class AndroidLogger implements Logger {

    final String name;
    public static int maxLen = 1024;
    public static int maxLines = 20;
    private static PrintStream out = null;

    private synchronized void writeFile(String level, String msg) {
        if (out != null) {
            try {
                out.print(name);
                out.print(":");
                out.print(level);
                out.print(" - ");
                out.println(msg);
                out.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public AndroidLogger(String name) {
        this.name = name;
    }

    /**
     * Allows sending log output to file
     * @param file
     * @throws IOException
     */
    public static void setLogFile(File file) {
        if (out == null) {
            try {
                out = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void debug(String msg, Object... args) {
        String str = Utils.formatNoThrow(msg, args);
        Log.d(name, Utils.truncate(str, maxLen, maxLines, Utils.EllipsisStyle.INFO));
        writeFile("D", str);
    }

    @Override
    public void info(String msg, Object... args) {
        String str = Utils.formatNoThrow(msg, args);
        Log.i(name, Utils.truncate(str, maxLen, maxLines, Utils.EllipsisStyle.INFO));
        writeFile("I", str);
    }

    @Override
    public void error(String msg, Object... args) {
        String str = Utils.formatNoThrow(msg, args);
        Log.e(name, Utils.truncate(str, maxLen, maxLines, Utils.EllipsisStyle.INFO));
        writeFile("E", str);
    }

    @Override
    public void error(Throwable e) {
        String str = e.getClass().getSimpleName()+":"+e.getMessage();
        Log.e(name, str);
        Log.e(name, e.toString());
        writeFile("E", str);
        if (out != null)
            e.printStackTrace(out);
    }

    @Override
    public void warn(String msg, Object... args) {
        String str = String.format(msg, args);
        Log.w(name, str);
        writeFile("W", str);
    }
}
