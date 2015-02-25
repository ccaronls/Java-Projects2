package cc.lib.net.p2p;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;

import junit.framework.TestCase;

public class P2PTest extends TestCase implements IP2PServer.Listener {

    IP2PServer server = null;
    
    public void testScan() {
        Utils.dumpNetworkInterfaces();
    }
    
    public void testBadIP() throws IOException {
        InetAddress wifi = Utils.findNetworkInterface("wifi");
        try {
            server = new P2PServerMulticast(31111, wifi, "127.0.0.1", this, 1024);
            fail("Expected exception for non-multicast address");
        } catch (IOException e) {
            // expected
        }
    }
    
    public void testUnicastServer() throws Exception {
        
        InetAddress wifi = Utils.findNetworkInterface("wireless");
        P2PServerUnicast server = new P2PServerUnicast(31111, wifi, this, 1024);
        server.start();
        this.server = server;
        
        try {

            for (int i=0; i<99999; i++) {
                
                if (server.getPeers().size() == 0) {
                    synchronized (this) {
                        wait();
                    }
                }
                
                Data d = new Data();
                d.magic = "SOME_MAGIC_STRING";
                d.n = i;
                d.time = System.nanoTime();
                sendPacket(d);
                // sync to 20 frames per second
                Thread.sleep(50);
            }
        } finally {
            server.close();
        }
        
    }
    
    public void xtestMulticastServer() throws Exception {
        
        InetAddress wifi = Utils.findNetworkInterface("wireless");
        server = new P2PServerMulticast(31111, wifi, "224.0.0.1", this, 1024);
        server.start();
        
        try {

            for (int i=0; i<99999; i++) {
                Data d = new Data();
                d.magic = "SOME_MAGIC_STRING";
                d.n = i;
                d.time = System.nanoTime();
                sendPacket(d);
                // sync to 20 frames per second
                Thread.sleep(50);
            }
        } finally {
            server.close();
        }
        
    }

    @Override
    public void onPacket(byte[] data) {
        
        synchronized (this) {
            notify();
        }
        
        System.out.println("Packet received");
        Data d = new Data();
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        
        try {
            d.magic = in.readUTF();
            d.time = in.readLong();
            d.n = in.readInt();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    class Data {
        String magic;
        int n;
        long time;
        
        public String toString() {
            return magic + " n=" + n + " time=" + time;
        }
    }
    
    void sendPacket(Data p) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(1024);
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeUTF(p.magic);
        out.writeLong(p.time);
        out.writeInt(p.n);
        server.broadcast(bytes.toByteArray());
        System.out.println("Sent packet: " + p);
        out.close();
    }
    
    
}
