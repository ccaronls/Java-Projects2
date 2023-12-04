package cc.lib.reflector;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

public class RBufferedReader extends BufferedReader {

    private int markedLineNum = 0;
    int lineNum = 0;
    int depth = 0;

    RBufferedReader(Reader arg0) {
        super(arg0);
    }

    @Override
    public String readLine() throws IOException {
        lineNum++;
        try {
            String line = super.readLine();
            if (line == null) {
                if (depth > 0)
                    throw new EOFException();
                return null;
            }
            line = line.trim();
            if (line.endsWith("{")) {
                depth++;
                return line.substring(0, line.length() - 1).trim();
            }
            if (line.endsWith("}")) {
                depth--;
                return null;
            }
            return line;
        } catch (IOException e) {
            throw new IOException("Error on line: " + lineNum + " " + e.getMessage(), e);
        }
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        super.mark(readAheadLimit);
        markedLineNum = lineNum;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        lineNum = markedLineNum;
    }

    public String peekLine() throws IOException {
        try {
            mark(1024);
            return super.readLine();
        } finally {
            reset();
        }
    }

    public String readLineOrEOF() throws IOException {
        while (true) {
            String line = readLine();
            if (line == null)
                return null;
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#"))
                continue;
            return line;
        }
    }

    public String readLineAndClosedParen() throws IOException {
        String value = readLineOrEOF();
        String line = readLineOrEOF();
        if (line != null)
            throw new IOException("Expected closing paren } but found: " + line);
        return value;
    }
}
