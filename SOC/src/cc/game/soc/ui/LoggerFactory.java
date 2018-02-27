package cc.game.soc.ui;

/**
 * Created by chriscaron on 2/27/18.
 */

public abstract class LoggerFactory {

    public abstract Logger getLogger(String name);

    public static LoggerFactory factory = new LoggerFactory() {
        @Override
        public Logger getLogger(final String name) {
            return new Logger() {
                @Override
                public void error(String msg, Object... args) {
                    System.err.println("E[" + name + "]:" + String.format(msg, args));
                }

                @Override
                public void debug(String msg, Object... args) {
                    System.out.println("D[" + name + "]:" + String.format(msg, args));
                }

                @Override
                public void info(String msg, Object... args) {
                    System.err.println("I[" + name + "]:" + String.format(msg, args));
                }
            };
        }
    };

    public static Logger getLogger(Class<?> clazz) {
        return factory.getLogger(clazz.getSimpleName());
    }
}
