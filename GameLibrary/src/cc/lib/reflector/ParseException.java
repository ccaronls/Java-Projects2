package cc.lib.reflector;

import java.io.IOException;

public class ParseException extends IOException {
    final int lineNum;

    ParseException(int lineNum, String msg) {
        super(msg);
        this.lineNum = lineNum;
    }

    ParseException(int lineNum, Exception e) {
        super(e);
        this.lineNum = lineNum;
    }

    @Override
    public String getMessage() {
        return "Line (" + lineNum + ") " + super.getMessage();
    }
}