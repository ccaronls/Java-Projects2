package cc.lib.reflector;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

public class RBufferedReader extends BufferedReader {

    private int markedLineNum = 0;
    private int lineNum = 0;
    private int depth = 0;
    private int[] markedDepthStack = new int[32];
    private int numMarkedDepths = 0;

    private String line = "";

    RBufferedReader(Reader arg0) {
        super(arg0);
    }

    public int getLineNum() {
        return lineNum;
    }

    @Override
    public String readLine() throws IOException {
        lineNum++;
        try {
            line = super.readLine();
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
            throw new IOException("Error on line: " + lineNum + " " + e.getMessage() + "\n   " + line, e);
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
                return null; // this mean we hit a '}'
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

    public void markDepth() {
        markedDepthStack[numMarkedDepths++] = depth;
    }

    public void restoreDepth(Exception original) throws IOException {
        if (original != null)
            throw new ParseException(lineNum, original);
        while (depth > markedDepthStack[numMarkedDepths - 1]) {
            String line = readLineOrEOF();
            if (line != null)
                throw new ParseException(lineNum, " Expected closing '}' but got:" + line);
        }
        numMarkedDepths--;
    }
}
