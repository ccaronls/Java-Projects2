package cc.game.soc.core;

public class SOCException extends RuntimeException {
    public SOCException() {
    }

    public SOCException(String message) {
        super(message);
    }

    public SOCException(String message, Throwable cause) {
        super(message, cause);
    }
}
