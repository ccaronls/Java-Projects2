package cc.fantasy.struts.exception;

public class SessionTimeoutException extends Exception {

    public SessionTimeoutException() {
        super("Session timed out");
    }
    
}
