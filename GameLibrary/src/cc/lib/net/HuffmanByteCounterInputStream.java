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
}
