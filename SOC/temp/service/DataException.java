package cc.game.soc.service;

@SuppressWarnings("serial")
public class DataException extends RuntimeException {

	DataException(String msg) {
		super(msg);
	}

	DataException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
