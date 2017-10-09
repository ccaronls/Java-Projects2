package cc.lib.android;

import android.util.Log;

import java.io.IOException;
import java.io.Writer;

/**
 * Created by chriscaron on 10/9/17.
 */

public class AndroidLogWriter extends Writer {

    final String tag;

    public AndroidLogWriter(String tag) {
        this.tag = tag;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        String line = new String(cbuf, off, len);
        if (line.length() > 0 && line.endsWith("\n")) {
            Log.d(tag, line);
        }
    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws IOException {

    }
}
