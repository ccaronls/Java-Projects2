package cc.lib.android;

import android.util.Log;

import cc.lib.logger.Logger;

/**
 * Created by chriscaron on 3/11/18.
 */

public class AndroidLogger implements Logger {

    final String name;

    public AndroidLogger(String name) {
        this.name = name;
    }

    @Override
    public void debug(String msg, Object... args) {
        Log.d(name, String.format(msg, args));
    }

    @Override
    public void info(String msg, Object... args) {
        Log.i(name, String.format(msg, args));

    }

    @Override
    public void error(String msg, Object... args) {
        Log.e(name, String.format(msg, args));

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
