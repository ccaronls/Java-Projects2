package cc.lib.net;

import java.io.IOException;
import java.io.InputStream;

import cc.lib.crypt.HuffmanEncoding;

/**
 * Created by chriscaron on 3/23/18.
 */

public class HuffmanByteCounterInputStream extends InputStream {

    private final InputStream in;
    private final HuffmanEncoding enc;

    public HuffmanByteCounterInputStream(InputStream in, HuffmanEncoding enc) {
        this.in = in;
        this.enc = enc;
    }


    @Override
    public int read() throws IOException {
        int i = in.read();
        enc.increment(i);
        return i;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int n = in.read(b);
        enc.increment(b, 0, n);
        return n;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = in.read(b, off, len);
        enc.increment(b, off, len);
        return n;
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }
}
