package cc.lib.net.p2p;

import java.io.IOException;
import java.net.InetAddress;

public interface IP2PClient {

    public static interface Listener {
        void onPacket(byte [] data);
    }

    void start() throws IOException;

    void stop();

    void close();

    void send(byte[] data) throws IOException;

}
