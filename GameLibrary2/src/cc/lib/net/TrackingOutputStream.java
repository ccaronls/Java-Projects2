package cc.lib.net;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Chris Caron on 11/7/23.
 */
public class TrackingOutputStream extends OutputStream {

    private final OutputStream os;
    private final long[] counter;

    public TrackingOutputStream(OutputStream os, long[] counter) {
        this.os = os;
        this.counter = counter;
    }

    @Override
    public void write(int i) throws IOException {
        os.write(i);
        counter[0]++;
    }

    @Override
    public void write(@NotNull byte[] bytes) throws IOException {
        os.write(bytes);
        counter[0] += bytes.length;
    }

    @Override
    public void write(@NotNull byte[] bytes, int offset, int len) throws IOException {
        os.write(bytes, offset, len);
        counter[0] += len;
    }
}
