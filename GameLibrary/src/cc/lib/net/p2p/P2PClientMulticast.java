package cc.lib.net.p2p;

import java.net.*;
import java.io.*;
import java.util.*;

public class P2PClientMulticast implements IP2PClient {

    final MulticastSocket socket;
    final InetAddress multicastAddress;
    final int port;
    final byte [] inData;
    ListenerThread thread;
    public boolean running; 
    final Listener listener;
    
    public P2PClientMulticast(int port,String multicastAddress, Listener listener, int maxDataSize) throws IOException {
        if (listener == null)
            throw new NullPointerException("listener cannot be null");
        this.listener = listener;
        inData = new byte[maxDataSize];
        this.port = port;
        this.multicastAddress = InetAddress.getByName(multicastAddress);
        if (!this.multicastAddress.isMulticastAddress())
            throw new IOException("address " + multicastAddress + " is not multicast");
        socket = new MulticastSocket(port);
        socket.setTimeToLive(100);
        socket.joinGroup(this.multicastAddress);
    }
    
    @Override
    public void start() throws IOException {
        if (thread == null) {
            thread = new ListenerThread();
            running = true;
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
        try {
            socket.leaveGroup(this.multicastAddress);
        } catch (Exception e) {
            logDebug("Problem leaving group " + this.multicastAddress + " " + e.getMessage());
        }
        socket.close();
    }
    
    @Override
    public void send(byte [] data) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length, this.multicastAddress, port);
        socket.send(packet);
    }
    
    class ListenerThread implements Runnable {
        public void run() {
            logDebug("client thread starting");
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(inData, inData.length);
                    socket.receive(packet);
                    //logDebug("Packet received");
                    listener.onPacket(inData);
                } catch (Exception e) {
                    if (running) {
                        e.printStackTrace();
                        logDebug("ERROR: " + e.getMessage());
                    }
                }
            }
            logDebug("client thread stopped");
        }
    }
    
    protected void logDebug(String msg) {
        System.out.println("P2PClient: " + msg);
    }
}
