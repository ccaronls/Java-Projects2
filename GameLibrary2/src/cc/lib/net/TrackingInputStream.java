package cc.lib.net;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Chris Caron on 11/7/23.
 */
public class TrackingInputStream extends InputStream {

    private final InputStream is;
    private final long[] counter;

    public TrackingInputStream(InputStream is, long[] counter) {
        this.is = is;
        this.counter = counter;
    }

    @Override
    public int read() throws IOException {
        int r = is.read();
        counter[0]++;
        return r;
    }

    @Override
    public int read(@NotNull byte[] bytes) throws IOException {
        int r = is.read(bytes);
        counter[0] += r;
        return r;
    }

    @Override
    public int read(@NotNull byte[] bytes, int i, int i1) throws IOException {
        int r = is.read(bytes);
        counter[0] += r;
        return r;
    }
}
