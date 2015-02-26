package cc.fantasy.exception;

public class FantasyException extends RuntimeException {

	private ErrorCode code;
	
	public FantasyException(ErrorCode code) {
		this.code = code;
	}
	
	public FantasyException(ErrorCode code, Exception cause) {
		super(cause);
		this.code = code;
	}
	
    public FantasyException(ErrorCode code, String msg) {
        super(msg);
        this.code = code;
    }
    
    public FantasyException(ErrorCode code, String msg, Exception cause) {
        super(msg, cause);
        this.code = code;
    }
    
    public ErrorCode getErrorCode() {
    	return code;
    }
    
    public String getMessage() {
    	return code.name() + ":" + super.getMessage();
    }
    
    public String getParameter() {
        return super.getMessage();
    }
}
