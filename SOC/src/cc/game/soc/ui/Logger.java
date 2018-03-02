package cc.game.soc.ui;

/**
 * Created by chriscaron on 2/27/18.
 */

public interface Logger {
    void error(String msg, Object ... args);
    void debug(String msg, Object ... args);
    void info(String msg, Object ... args);
}