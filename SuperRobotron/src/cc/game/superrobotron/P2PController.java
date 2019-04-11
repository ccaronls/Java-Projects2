package cc.game.superrobotron;

import java.net.*;
import java.io.*;

public class P2PController {

    private static long MAGIC_NUMBER = 320417203L;
    
    private class Peer {
        InetAddress address;
    }

    private DatagramSocket socket;
    
    P2PController(int port) throws IOException {
        
        InetAddress local = null;
        for (InetAddress addr : InetAddress.getAllByName(null)) {
            if (addr.isLoopbackAddress())
                continue;
            if (!addr.isMulticastAddress())
                continue;
            local = addr;
            break;
        }

        if (local == null)
            throw new IOException("Cannot find multicast socket");
        
        logDebug("Binding to port " + port + " and address " + local);
        socket = new DatagramSocket(port, local);
        socket.setBroadcast(true);
    }
    
    protected void logDebug(String msg) {
        System.out.println("DEBUG" + getClass().getSimpleName() + " " + msg);
    }

    private class BroadcastThread implements Runnable {
        byte [] message;
        boolean running = false;
        int port;
        InetAddress addr;
        int periodMS;
        
        BroadcastThread(byte [] message, int port, InetAddress addr, int periodMS) {
            this.message = message;
            if (message == null)
                throw new NullPointerException("message cannot be null");
            this.port = port;
            this.addr = addr;
            this.periodMS = periodMS;
        }
        
        @Override
        public void run() {
            running = true;
            logDebug("Broadcast thread starting");
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(message, message.length, addr, port);
                    socket.send(packet);

                    synchronized (this) {
                        wait(periodMS);
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            logDebug("Broadcast thread exiting");
        }
        
        void stop() {
            running = false;
            synchronized (this) {
                notify();
            }
        }
    }
    
    private BroadcastThread bThread = null;
    
    public void startBroadcastThread(String message, int port, int frequencyMS) throws IOException {
        if (bThread == null) {
            bThread = new BroadcastThread(message.getBytes(), port, findMulticastAddress(), frequencyMS);
            new Thread(bThread).start();
        }
    }
    
    public void stopBroadcastThread() {
        if (bThread != null) {
            bThread.stop();
            bThread = null;
        }
    }
    
    
    
    public void startListenThread(int port) {
        
    }

    public InetAddress findMulticastAddress() throws IOException {
        for (InetAddress addr : InetAddress.getAllByName(null)) {
            if (addr.isLoopbackAddress())
                continue;
            if (!addr.isMulticastAddress())
                continue;
            logDebug("Found multicast address '" + addr + "'");
            return addr;
        }
        throw new IOException("Cannot find multicast address");
    }
    
}
