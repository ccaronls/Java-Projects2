package cc.lib.android;

import android.util.Log;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;

/**
 * Created by chriscaron on 3/11/18.
 */

public class AndroidLogger implements Logger {

    final String name;
    final int maxLen;
    final int maxLines;

    public AndroidLogger(String name, int maxLen, int maxLines) {
        this.name = name;
        this.maxLen = maxLen;
        this.maxLines = maxLines;
    }

    public AndroidLogger(String name) {
        this(name, 1024, 32);
    }

    @Override
    public void debug(String msg, Object... args) {
        Log.d(name, Utils.truncate(String.format(msg, args), maxLen, maxLines));
    }

    @Override
    public void info(String msg, Object... args) {
        Log.i(name, Utils.truncate(String.format(msg, args), maxLen, maxLines));

    }

    @Override
    public void error(String msg, Object... args) {
        Log.e(name, Utils.truncate(String.format(msg, args), maxLen, maxLines));

    }

    @Override
    public void error(Exception e) {
        Log.e(name, e.getClass().getSimpleName()+":"+e.getMessage(), e);
    }

    @Override
    public void warn(String msg, Object... args) {
        Log.d(name, String.format(msg, args));

    }
}
