package cc.lib.reflector;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

import cc.lib.game.Utils;

public class RPrintWriter extends PrintWriter {

    final boolean numbered;
    static String[] indents;
    int lineNum = 0;
    boolean indented = false;

    boolean writeNulls = true;

    static {
        indents = new String[32];
        String indent = "";
        for (int i = 0; i < indents.length; i++) {
            indents[i] = indent;
            indent += "   ";
        }
    }

    public RPrintWriter(Writer out, boolean numbered, boolean writeNulls) {
        super(out, true);
        this.numbered = numbered;
        this.writeNulls = writeNulls;
    }

    public RPrintWriter(Writer out) {
        this(out, false, true);
    }

    public RPrintWriter(OutputStream out, boolean numbered, boolean writeNulls) {
        super(out, true);
        this.numbered = numbered;
        this.writeNulls = writeNulls;
    }

    public RPrintWriter(OutputStream out) {
        this(out, false, true);
    }

    private int currentIndent = 0;

    void push() {
        println(" {");
        if (currentIndent < indents.length - 1)
            currentIndent++;
    }

    void pop() {
        Utils.assertTrue(currentIndent > 0);
        if (currentIndent > 0)
            currentIndent--;
        println("}");
    }

    @Override
    public void write(String s) {
        if (!indented) {
            if (numbered)
                super.write(String.format("%-5d:", lineNum++));
            super.write(indents[currentIndent]);
            indented = true;
        }
        super.write(s);
    }

    @Override
    public void println() {
        super.println();
        indented = false;
    }

    RPrintWriter p(Object o) {
        write(String.valueOf(o));
        return this;
    }

    public void writeNull(String name) {
        if (writeNulls) {
            p(name).println("=null");
        }
    }
}
