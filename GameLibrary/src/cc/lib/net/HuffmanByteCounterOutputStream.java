package cc.lib.net;

import java.io.IOException;
import java.io.OutputStream;

import cc.lib.crypt.HuffmanEncoding;

/**
 * Created by chriscaron on 3/23/18.
 */

public class HuffmanByteCounterOutputStream extends OutputStream {

    private final OutputStream out;
    private final HuffmanEncoding enc;

    public HuffmanByteCounterOutputStream(OutputStream out, HuffmanEncoding enc) {
        this.out = out;
        this.enc = enc;
    }

    @Override
    public void write(int b) throws IOException {
        enc.increment(b);
        out.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        enc.increment(b, 0, b.length);
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        enc.increment(b, off, len);
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
