package cc.game.soc.nety;

import java.io.IOException;

@SuppressWarnings("serial")
public class ProtocolException extends IOException {

    ProtocolException(String msg) {
        super(msg);
    }
    
    ProtocolException(String msg, Throwable cause) {
        super(msg + " caused by: " + cause.getClass() + " " + cause.getMessage());
    }
    
}
