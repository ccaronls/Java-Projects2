package cc.game.soc.core;

/**
 * Created by chriscaron on 3/27/18.
 */

public interface StringResource {
    default String getString(String format, Object ... args) {
        return String.format(format, args);
    }
}
