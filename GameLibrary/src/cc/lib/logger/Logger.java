package cc.lib.logger;

/**
 * Created by chriscaron on 2/27/18.
 */

public interface Logger {
    void debug(String msg, Object... args);
    void info(String msg, Object... args);
    void error(String msg, Object... args);
    void error(Throwable e);
    void warn(String msg, Object... args);
    void verbose(String msg, Object ... args);
}