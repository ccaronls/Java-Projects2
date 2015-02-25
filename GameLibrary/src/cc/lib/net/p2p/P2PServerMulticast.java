package cc.lib.net.p2p;

import java.net.*;
import java.io.*;
import java.util.*;

public class P2PServerMulticast implements IP2PServer {

    /**
     * Create a multicast server
     * 
     * @param listenPort can be any open port
     * @param bindAddress use Utils.findNetworkInterface for the best interface to bind too
     * @param multicastAddress must be range 224.0.0.1 to 239.255.255.255
     * @param listener must be non null implementation to handle incoming packets
     * @param maxPacketSize max packet size to receive
     * @throws IOException
     */
    public P2PServerMulticast(int listenPort, InetAddress bindAddress, String multicastAddress, Listener listener, int maxPacketSize) throws IOException {
        if (listener == null)
            throw new NullPointerException("listener cannot be null");
        data = new byte[maxPacketSize];
        this.port = listenPort;
        this.listener = listener;
        this.multicastAddress = InetAddress.getByName(multicastAddress);
        if (!this.multicastAddress.isMulticastAddress())
            throw new IOException("Address " + multicastAddress + " is not multicast.  Range is 224.0.0.1 to 239.255.255.255");
        socket = new DatagramSocket(listenPort, bindAddress);
        socket.setBroadcast(true);
    }
    
    final private int port;
    final private byte [] data;
    final private Listener listener;
    private ListenerThread thread;
    final private DatagramSocket socket;
    final private InetAddress multicastAddress;
    private boolean running;
    
    @Override
    public void start() {
        if (thread == null) {
            running = true;
            thread = new ListenerThread();
            new Thread(thread).start();
        }
    }
    
    @Override
    public void stop() {
        if (thread != null) {
            running = false;
            synchronized (thread) {
                thread.notify();
            }
            thread = null;
        }
    }
    
    @Override
    public void close() {
        stop();
        socket.close();
    }
    
    @Override
    public void broadcast(byte [] data) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length, this.multicastAddress, this.port);
        socket.send(packet);
    }
    
    /**
     * Override to customize logging.  Default prints to stdout
     * @param msg
     */
    protected void logDebug(String msg) {
        System.out.println("P2PServer: " + msg);
    }
    
    private class ListenerThread implements Runnable {
        public void run() {
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(data, data.length);
                    socket.receive(packet);
                    listener.onPacket(data);
                } catch (Exception e) {
                    if (running) {
                        logDebug("Error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void send(byte[] data, InetAddress remoteAddress, int remotePort) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, remotePort);
        socket.send(packet);
    }
}
