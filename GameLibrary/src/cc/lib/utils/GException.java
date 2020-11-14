package cc.lib.utils;

public class GException extends RuntimeException {

    public GException() {
    }

    public GException(String message) {
        super(message);
    }

    public GException(String message, Throwable cause) {
        super(message, cause);
    }

    public GException(Throwable cause) {
        super(cause);
    }

}
