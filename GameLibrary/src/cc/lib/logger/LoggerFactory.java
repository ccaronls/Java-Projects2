package cc.lib.logger;

import java.util.Date;

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
                    System.err.println(new Date() + " E[" + name + "]:" + String.format(msg, args));
                }

                @Override
                public void debug(String msg, Object... args) {
                    System.out.println(new Date() + " D[" + name + "]:" + String.format(msg, args));
                }

                @Override
                public void info(String msg, Object... args) {
                    System.out.println(new Date() + " I[" + name + "]:" + String.format(msg, args));
                }

                @Override
                public void error(Exception e) {
                    error("%s:%s\n%s", e.getClass().getSimpleName(), e.getMessage());
                    for (StackTraceElement s : e.getStackTrace()) {
                        error(s.toString());
                    }
                }

                @Override
                public void warn(String msg, Object... args) {
                    System.err.println(new Date() + " W[" + name + "]:" + String.format(msg, args));
                }
            };
        }
    };


    public static Logger getLogger(String prefix, Class<?> clazz) {
        return factory.getLogger(prefix + ":" + clazz.getSimpleName());
    }

    public static Logger getLogger(Class<?> clazz) {
        return factory.getLogger(clazz.getSimpleName());
    }
}
