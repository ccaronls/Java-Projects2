package cc.lib.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import cc.lib.game.Utils;
import cc.lib.utils.GException;

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
                    System.out.println("I[" + name + "]:" + String.format(msg, args));
                }

                @Override
                public void error(Throwable e) {
                    error("%s:%s", e.getClass().getSimpleName(), e.getMessage());
                    for (StackTraceElement s : e.getStackTrace()) {
                        error(s.toString());
                    }
                }

                @Override
                public void warn(String msg, Object... args) {
                    System.err.println("W[" + name + "]:" + String.format(msg, args));
                }
            };
        }
    };

    private static String getName(Class<?> clazz) {
        if (!Utils.isEmpty(clazz.getSimpleName()))
            return clazz.getSimpleName();
        else return getName(clazz.getSuperclass());
    }

    public static Logger getLogger(String prefix, Class<?> clazz) {
        return factory.getLogger(prefix + ":" + getName(clazz));
    }

    public static Logger getLogger(Class<?> clazz) {
        return factory.getLogger(getName(clazz));
    }

    public static void setFileLogger(final File outFile) {
        setFileLogger(outFile, true);
    }

    public static void setFileLogger(final File outFile, final boolean includeConsole) {
        factory = new LoggerFactory() {

            PrintWriter out = null;

            void write(String txt, PrintStream o2) {
                if (includeConsole)
                    o2.println(txt);
                try {
                    if (out == null) {
                        if (!outFile.exists()) {
                            try {
                                if (!outFile.createNewFile())
                                    throw new GException("Cannot create file '" + outFile + "'");
                            } catch (IOException e) {
                                throw new GException(e);
                            }
                        }
                        out = new PrintWriter(new FileWriter(outFile));
                    }

                    out.println(txt);
                    out.flush();
                    if (outFile.length() > 5*1024*1024) {
                        File newFile = new File(outFile.getParent(), outFile.getName()+".0");
                        newFile.delete();
                        if (!outFile.renameTo(newFile))
                            throw new GException("Failed to rename " + outFile + " to " + newFile);
                        if (!outFile.createNewFile())
                            throw new GException("Failed to create file: " + outFile);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public Logger getLogger(final String name) {
                return new Logger() {
                    @Override
                    public void error(String msg, Object... args) {
                        write("E[" + name + "]:" + String.format(msg, args), System.err);
                    }

                    @Override
                    public void debug(String msg, Object... args) {
                        write("D[" + name + "]:" + String.format(msg, args), System.out);
                    }

                    @Override
                    public void info(String msg, Object... args) {
                        write("I[" + name + "]:" + String.format(msg, args), System.out);
                    }

                    @Override
                    public void error(Throwable e) {
                        error("%s:%s\n%s", e.getClass().getSimpleName(), e.getMessage());
                        for (StackTraceElement s : e.getStackTrace()) {
                            error(s.toString());
                        }
                    }

                    @Override
                    public void warn(String msg, Object... args) {
                        write("W[" + name + "]:" + String.format(msg, args), System.err);
                    }
                };
            }
        };
    }
}
