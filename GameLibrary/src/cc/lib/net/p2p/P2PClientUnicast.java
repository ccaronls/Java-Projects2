package cc.lib.net.p2p;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * This version of the client must initiate a connection with a server
 * 
 * @author ccaron
 *
 */
public class P2PClientUnicast implements IP2PClient {

    private final DatagramSocket socket;
    private final int listenPort;
    private final byte [] inData;
    private ListenerThread thread;
    private boolean running; 
    private final Listener listener;
    private InetAddress remoteAddress;
    private int remotePort;
    
    public P2PClientUnicast(int port, InetAddress bindAddress, Listener listener, int maxDataSize) throws IOException {
        if (listener == null)
            throw new NullPointerException("listener cannot be null");
        this.listener = listener;
        inData = new byte[maxDataSize];
        this.listenPort = port;
        socket = new DatagramSocket(port, bindAddress);
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
        socket.close();
    }
    
    @Override
    public void send(byte [] data) throws IOException {
        if (remoteAddress != null) {
            DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, remotePort);
            socket.send(packet);
        }
    }

    public void setRemote(String address, int port) throws IOException {
        this.remoteAddress = InetAddress.getByName(address);
        this.remotePort = port;
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
