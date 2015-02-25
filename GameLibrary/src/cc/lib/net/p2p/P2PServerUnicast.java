package cc.lib.net.p2p;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class P2PServerUnicast implements IP2PServer {

    public static class Peer {

        public final int port;
        public final InetAddress address;
        
        public Peer(int port, InetAddress address) {
            super();
            this.port = port;
            this.address = address;
        }
        
    }
    
    private class ListenerThread implements Runnable {
        public void run() {
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(inBuffer, inBuffer.length);
                    socket.receive(packet);
                    String addressStr = packet.getAddress().getHostAddress();
                    if (!peers.containsKey(addressStr)) {
                        peers.put(addressStr, new Peer(packet.getPort(), packet.getAddress()));
                    }
                    listener.onPacket(inBuffer);
                } catch (Exception e) {
                    if (running) {
                        logDebug("Error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    private final Listener listener;
    private final byte [] inBuffer;
    private final DatagramSocket socket;
    private boolean running = false;
    private final Map<String, Peer> peers = new HashMap();
    private ListenerThread listenerThread;
    
    
    public P2PServerUnicast(int listenPort, InetAddress bindAddress, Listener listener, int maxDataSize) throws IOException {
        if (listener == null)
            throw new NullPointerException("Listener cannot be null");
        this.listener = listener;
        this.inBuffer = new byte[maxDataSize];
        this.socket = new DatagramSocket(listenPort, bindAddress);
    }
    
    @Override
    public void start() {
        if (listenerThread == null) {
            running = true;
            listenerThread = new ListenerThread();
            new Thread(listenerThread).start();
        }
    }
    
    @Override
    public void stop() {
        if (listenerThread != null) {
            running = false;
            synchronized (listenerThread) {
                listenerThread.notify();
            }
            listenerThread = null;
        }
    }

    @Override
    public void close() {
        stop();
        socket.close();
    }
    
    @Override
    public void broadcast(byte [] data) throws IOException {
        for (Peer p : peers.values()) {
            send(data, p.address, p.port);
        }
    }
    
    @Override
    public void send(byte [] data, InetAddress remoteAddress, int remotePort) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, remotePort);
        socket.send(packet);
    }
    
    public Collection<Peer> getPeers() {
        return peers.values();
    }
    
    public Peer getPeer(String ipAddress) {
        return peers.get(ipAddress);
    }

    protected void logDebug(String msg) {
        System.out.println("P2PServer: " + msg);
    }

}
