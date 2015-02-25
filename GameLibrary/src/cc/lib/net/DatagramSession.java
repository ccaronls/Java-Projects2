package cc.lib.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Manager class for optimizing a connectionless network (Datagram)
 * 
 * Goals:
 * - optimize network for speed by sending uniform size packets
 * - manage dropped packets or packets that come out of order
 * - provide interface for handling lost/partial packets
 * - collect statistics
 * - ease of use.  
 * 
 * Example:
 * 
 * class MySession extends DatagramSession {
 *    MySession() {
 *      super("127.32.90.2", 1234");
 *    }
 *    
 *    // one of these called such that 'id' starts at 1 and increments
 *    // by one 
 *    public void onPacketRead(long id, byte [] data, int size) {}
 *    public void onPacketDropped(long id) {}
 *    public void onPacketPartial(long id, byte [] data, int size) {}
 *    
 *    void myWrite() {
 *        write(smallData); // pads data as neccessary to send uniform packets
 *        write(mediaData); // pads or splits data into multiple packets
 *        write(largeData); // splits data into many packets to be reassembled on the other side
 *    }
 * }
 * 
 * 
 * 
 * 
 * 
 * @author ccaron
 *
 */
abstract public class DatagramSession {

    /**
     * Called when our reader thread has assembled the next packet in the series
     * 
     * @param count
     * @param data
     * @param size
     */
    protected abstract void onPacketRead(long userData, long count, byte [] data, int size);
    
    /**
     * Called when a packet with the given id never arrived
     * @param count
     */
    protected void onPacketDropped(long count) {}
    
    /**
     * Called when only a partial packet has been assembled
     * 
     * @param count
     * @param data
     * @param size
     */
    protected void onPartialPacket(long userData, long count, byte [] data, int size) {}

    /**
     * Optional to get debug info
     * 
     * @param msg
     */
    protected void printDebug(String msg) {}
    
    public InetAddress getAddress() {
        return mAddress;
    }
    
    public void setRemote(InetAddress address, int writePort) {
        if (address != null && (mAddress == null || !mAddress.equals(address)))
            this.mIdAllocator = 1;

        printDebug("Set remote address '" + address + ":" + writePort);
        this.mAddress = address;
        this.mWritePort = writePort;
    }
    
    public long getWritePort() {
        return this.mWritePort;
    }
    
    private long mIdAllocator = -1; // only applied to writes
    protected DatagramSocket mSocket;
    private InetAddress mAddress;
    private int mWritePort;
    
    public static boolean DEBUG_ENABLED = true;
    private Reader mReader = null;
    private Processor mProcessor = null;

    private int mOptimalPacketSize;
    private final int mHeaderSize = computeHeaderSize();
    private int mMaxDataSize;
    private int mMaxRangePackets;

    private int computeHeaderSize() {
        int headerSize = 64;
        try {
            DataOutputStream out = new DataOutputStream(new ByteArrayOutputStream());
            writeHeader(out, 0, 0, 0, 0, 0, 0);
            headerSize = out.size();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        logDebug("Header size = " + headerSize);
        return headerSize;
    }
    
    /**
     * Override to log a message.  Default writes to stdout.
     * @param msg
     */
    protected void logDebug(String msg) {
        System.out.println(msg);
    }
    
    // unique identifier for our packet type
    private final static long MAGIC_NUMBER = 98432098472L;
    
    /**
     * Create a session  
     * @param listenPort port to listen on
     * @param packetSize size of packets
     * @param historySize size of history buffer (for managing late packets)
     * @throws IOException if socket cannot be bound to listenPort
     */
    public DatagramSession(int listenPort, int packetSize, int historySize) throws IOException {
        this(new DatagramSocket(listenPort), packetSize, historySize);
    }
    
    /**
     * Create a session
     * @param socket send/recv socket
     * @param packetSize size of packets to send / recv
     * @param historySize size of history buffer for managing late packets
     * @throws IOException
     */
    public DatagramSession(DatagramSocket socket, int packetSize, int historySize) {
        this.mSocket = socket;
        if (packetSize < mHeaderSize * 2) {
            packetSize = mHeaderSize * 2;
        }
        mOptimalPacketSize = packetSize;
        mMaxDataSize = mOptimalPacketSize - mHeaderSize;
        mMaxRangePackets = historySize;
    }
    
    // TODO
    
    int mStatSentBytes;
    int mStatRcvdBytes;
    int mStatSentPackets;
    int mStatRcvdPackets;
    int mStatRcvdDropped;
    int mStatSentDataPackets;
    int mStatRcvdDataPackets;
    long mStatSentDataTotalSizeBytes = 0;
    int mStatSentDataAveSizeBytes;

    Field [] fields;

    public void dumpDebug(PrintStream out) {
        if (fields == null)
            fields = DatagramSession.class.getDeclaredFields();
        for (int i=0; i<fields.length; i++) {
            try {
                
                if (fields[i].getName().startsWith("mStat")) {
                    out.println(String.format("%-30s = %d", fields[i].getName(), fields[i].get(this)));
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Start the reader/writer threads
     */
    public void start() {
        if (mReader == null) {
            mReader = new Reader();
            new Thread(mReader).start();
        }
        if (mProcessor == null) {
            mProcessor = new Processor();
            new Thread(mProcessor).start();
        }
    }

    /**
     * Stop all threads and close the socket
     */
    public void stop() {
        printDebug("Stop Called");
        if (mSocket != null) {
            mSocket.close();
            mSocket = null;
        }
        if (mReader != null) {
            mReader.stop();
            mReader = null;
        }
        if (mProcessor != null) {
            mProcessor.stop();
            mProcessor = null;
        }
    }
    
    /**
     * Flush all buffers
     */
    public void flush() {
        if (mProcessor != null)
            mProcessor.flush();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(mOptimalPacketSize);
        for (int i=0; i<mMaxRangePackets; i++) {
            try {
                DataOutputStream out = new DataOutputStream(buffer);
                out.writeLong(mIdAllocator);
                //while (buffer.size() < mOptimalPacketSize) {
                //    out.write(0);
               // }
                DatagramPacket packet = new DatagramPacket(buffer.toByteArray(), mOptimalPacketSize, mAddress, mWritePort);
                mSocket.send(packet);
                mIdAllocator++;
                //Thread.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
            buffer.reset();
        }
    }
    
    boolean simulateDroppedPackets = false;
    boolean simulateOutOfOrderPackets = false;

    public synchronized long write(byte [] message, int priority) throws IOException {
        return write(0, message, priority);
    }
    
    
    public synchronized long write(long userData, byte [] message, int priority) throws IOException {
        return write(userData, message, message.length, priority);
    }
    
    public synchronized long writeEmpty() throws IOException {
        final long id = mIdAllocator;
        byte [] buffer = new byte[mOptimalPacketSize];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, mAddress, mWritePort);
        mSocket.send(packet);
        mIdAllocator++;
        return id;
    }
    
    private static void writeHeader(DataOutputStream out, long id, int priority, int size, int packetIndex, int num, long userData) throws IOException {
        out.writeLong(MAGIC_NUMBER);
        out.writeLong(id);
        out.writeInt(priority);
        out.writeInt(size);
        out.writeShort(packetIndex);
        out.writeShort(num);
        out.writeLong(userData);
    }

    private ByteArrayOutputStream mBuffer = new ByteArrayOutputStream();

    private final Object mAckMonitor = new Object();
    
    /**
     * 
     * @param userData
     * @param message
     * @param size
     * @param priority
     * @return
     * @throws IOException
     */
    public synchronized long write(long userData, byte [] message, int size, int priority) throws IOException {

        if (size <= 0)
            return writeEmpty();
        
        final long id = mIdAllocator;
        mBuffer.reset();

        DataOutputStream out = new DataOutputStream(mBuffer);

        final int num = (size-1) / mMaxDataSize + 1;
        int messageByteIndex = 0;
        int packetIndex = 0;
        
        while (mSocket != null & packetIndex < num) {
            
            if (DEBUG_ENABLED) printDebug("WRITER: writeHeader id(" + id + ") size (" + size + ") index (" + packetIndex + ") num (" + num + ")");
            //    private static void writeHeader(DataOutputStream out, long id, int priority, int size, int packetIndex, int num, long userData) throws IOException {
            writeHeader(out, id, priority, size, packetIndex, num, userData);
            
            if (mBuffer.size() > mHeaderSize)
                throw new RuntimeException("INTERNAL: Header size too small");
            
            // pad out the header to the headersize
            while (mBuffer.size() < mHeaderSize) {
                out.write(0);
                //this.mStatSentEmptyBytes++;
            }

            
            try {
                int bytesToWrite = Math.min(size-messageByteIndex, mMaxDataSize);

                // DEBUG CODE
                if (simulateDroppedPackets) {
                    if (id > 1 && Math.random() > 0.5) {
                        // simulate a dropped packet
                        packetIndex++;
                        messageByteIndex += bytesToWrite;
                        mBuffer.reset();
                        if (DEBUG_ENABLED) printDebug("WRITER: *SIMDROP* id " + id + ", index '" + packetIndex + "'");
                        continue;
                    }
                }
                
                mBuffer.write(message, messageByteIndex, bytesToWrite);
                while (mBuffer.size() < mOptimalPacketSize) {
                    mBuffer.write(0);
                    //this.mStatSentEmptyBytes++;
                }
                mBuffer.flush();
                if (mBuffer.size() != mOptimalPacketSize)
                    throw new RuntimeException("INTERNAL: Buffer not assembled properly");
                
                // DEBUG CODE
                if (simulateOutOfOrderPackets) {
                    final int indexToSend = packetIndex;
                    final byte [] bytes = mBuffer.toByteArray();
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                int sleeptime = (int)(Math.random() * 100);
                                Thread.sleep(sleeptime);
                                if (DEBUG_ENABLED) printDebug("WRITER: *SIMDELAY* id '" + id + "' index '" + indexToSend + "'");
                                DatagramPacket packet = new DatagramPacket(bytes, mOptimalPacketSize, mAddress, mWritePort);
                                mSocket.send(packet);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } else {
                    byte [] byteArray = mBuffer.toByteArray();
                    DatagramPacket packet = new DatagramPacket(byteArray, mOptimalPacketSize, mAddress, mWritePort);

                    mSocket.send(packet);

                    mAckId = 0;
                    while (priority-- > 0) {
                        synchronized (mAckMonitor) {
                            mAckMonitor.wait(1000);
                        }
                        if (mAckId != id) {
                            logDebug("Ack not recieved, resending id '" + id + "'");
                            packet = new DatagramPacket(byteArray, mOptimalPacketSize, mAddress, mWritePort);
                            mSocket.send(packet);
                        } else {
                            break;
                        }
                    }
                }
                
                // success!, increment
                packetIndex ++;
                messageByteIndex += bytesToWrite;
                
                this.mStatSentBytes += mOptimalPacketSize;
                this.mStatSentPackets += 1;
                
                //Thread.yield();
            } catch (Exception e) {
                // failed to send the packet, ignore and just try again
                e.printStackTrace();
            }
            
        }       
        
        mIdAllocator++;
        
        out.close();
        
        this.mStatSentDataPackets += 1;
        this.mStatSentDataTotalSizeBytes += size;
        this.mStatSentDataAveSizeBytes = (int)(mStatSentDataTotalSizeBytes / (long)mStatSentDataPackets);  
        
        return id;
    }
    
    private class Packet {
        
        private byte [][] partials; // individual incoming packets including headers
        private int numInserted; // totalNumber of inserts
        final int totalSize;
        final long userData;
        
        Packet(int num, int totalSize, long userData) {
            partials = new byte[num][];
            this.totalSize = totalSize;
            this.userData = userData;
        }
        
        void insert(byte [] data, int index) {
            partials[index] = data;
            numInserted++;
        }
        
        void assemble(byte [] data) {
            int index = 0;
            for (int i=0; i<partials.length; i++) {
                if (partials[i] != null)
                    System.arraycopy(partials[i], mHeaderSize, data, index, mMaxDataSize);
                index += mMaxDataSize;
            }
        }
        
        boolean isComplete() {
            return numInserted == partials.length;
        }
    }

    private final static long MAGIC_ACK = 7472346572065276L;
    
    private long mAckId = 0;
    
    void sendAck(long id) throws Exception {
        logDebug("Sending ack for id '" + id + "'");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(out);
        ds.writeLong(MAGIC_ACK);
        ds.writeLong(id);
        while (ds.size() < mOptimalPacketSize)
            ds.writeChar(0);

        byte [] data = out.toByteArray();
        DatagramPacket packet = new DatagramPacket(data, data.length, this.mAddress, this.mWritePort);
        mSocket.send(packet);
    }
    
    void parseAck(long id) {
        logDebug("recieved ack for id '" + id + "'");
        mAckId = id;
        synchronized (mAckMonitor) {
            mAckMonitor.notify();
        }
    }
    
    private class Processor implements Runnable {
        // process packets in a seperate thread

        Packet [] packets = new Packet[mMaxRangePackets];
        long minTrackingId = 1;
        byte [] readBuffer = new byte[4096];

        long highestId = 0;
        boolean running;
        
        Queue<DatagramPacket> dataQueue = new LinkedList<DatagramPacket>();

        public void run() {
            printDebug("Processor thread starting");
            running = true;
            while (running) {
                try {
                    DatagramPacket packet = null;
                    synchronized (dataQueue) {
                        if (dataQueue.size() == 0)
                            dataQueue.wait();
                        if (dataQueue.size() == 0)
                            continue;
                        packet = dataQueue.remove();
                    }
                    process(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            printDebug("processor thread exiting");            
        }
        
        void process(DatagramPacket packet) throws Exception {
            
            // parse the header
            byte [] data = packet.getData();
            ByteArrayInputStream is = new ByteArrayInputStream(data);
            DataInputStream input = new DataInputStream(is);

            do { // sumptin to break out uv
                final long NUMBER = input.readLong();
                if (NUMBER == MAGIC_ACK) {
                    long id = input.readLong();
                    parseAck(id);
                    break;
                }
                
                if (NUMBER != MAGIC_NUMBER)
                    break; // ignore invalid packets            
                
                if (packet.getAddress() == null) {
                    if (DEBUG_ENABLED) printDebug("Packet with null address!");
                } else if (mAddress == null) {
                    setRemote(packet.getAddress(), packet.getPort());
                } else if (!mAddress.equals(packet.getAddress())) {
                    if (DEBUG_ENABLED) printDebug("Ignoring packet from '" + packet.getAddress() + "' since we are connected to  '" + mAddress  +"'");
                    break;
                }
    
                mStatRcvdBytes += data.length;
                mStatRcvdPackets += 1;
                
                final long id = input.readLong();
                final int priority = input.readInt();
                final int size = input.readInt();
                final int index = input.readShort();
                final int num = input.readShort();
                final long userData = input.readLong();
                
                if (DEBUG_ENABLED) printDebug("READER: Read header id(" + id + ") priority (" + priority + ") size (" + size + ") index (" + index + ") num (" + num + "}");
    
                if (priority > 0) {
                    sendAck(id);
                }
                
                int bytesToSkip = mMaxDataSize - is.available();
                is.skip(bytesToSkip);
                
                // priority 0 packets accumulate
                // priority 1 packets get sent emmediatly and any remaining packets flushed
                // priority 2 packets get sent emmediatly and any existing packets are discarded
                if (index == 0) {
                    switch (priority) {
                        case 0: break;
                        case 1: 
                            while (minTrackingId < id) {
                                doCallback();
                            }
                            highestId = id;
                            break;
                        case 2:
                            Arrays.fill(packets, null);
                            minTrackingId = id;
                            highestId = id;
                            break;
                    }
                }
                
                if (id < minTrackingId) {
                    // ignore
                    if (DEBUG_ENABLED) printDebug("READER: Ignoring packet '" + id + "' since the min is '" + minTrackingId + "'");
                    break;
                }
    
                highestId = Math.max(highestId, id);
                
                int insertIndex = (int)(id - minTrackingId);
                
                while (insertIndex >= mMaxRangePackets) {
                    if (DEBUG_ENABLED) printDebug("READER: Flushing packet '" + minTrackingId + "'");
                    doCallback();
                    insertIndex -= 1;
                }
                
                if (size > 0) {
                    if (DEBUG_ENABLED) printDebug("READER: Inserting id '" + id + "' index '" + insertIndex + "' with min '" + minTrackingId + "'");
                    if (packets[insertIndex] == null)
                        packets[insertIndex] = new Packet(num, size, userData);
                    packets[insertIndex].insert(data, index);
                }
                
                // send off any complete packets that are at the front right away
                while (packets[0] != null && packets[0].isComplete()) {
                    if (DEBUG_ENABLED) printDebug("READER: Sending complete packet");
                    doCallback();
                }                   
            } while (false);
            
            input.close();
        }
        
        void stop() {
            running = false;
            synchronized (dataQueue) {
                dataQueue.notify();
            }
        }
        
        synchronized void doCallback() {
            if (DEBUG_ENABLED) printDebug("READER: doCallback min'" + minTrackingId + "'");
            try {
                Packet p = packets[0];
                if (p == null || p.numInserted == 0) {
                    mStatRcvdDropped += 1;
                    onPacketDropped(minTrackingId);
                }
                else if (p.isComplete()) {
                    allocate(p.totalSize);
                    p.assemble(readBuffer);
                    mStatRcvdDataPackets ++;
                    onPacketRead(p.userData, minTrackingId, readBuffer, p.totalSize);
                } else {
                    allocate(p.totalSize);
                    p.assemble(readBuffer);
                    onPartialPacket(p.userData, minTrackingId, readBuffer, p.totalSize);
                }
            } catch (Exception e) {
                if (DEBUG_ENABLED) printDebug("READER: ######" + e.getMessage());
                //e.printStackTrace();
            }
            remove();
            minTrackingId ++;
        }
        
        void allocate(int minReadSize) {
            if (readBuffer.length < minReadSize)
                readBuffer = new byte[minReadSize * 2];
        }
        
        void remove() {
            for (int i=0; i<packets.length-1; i++) {
                packets[i] = packets[i+1];
            }
            packets[packets.length-1] = null;
        }
        
        void flush() {
            while (minTrackingId <= highestId) {
                if (DEBUG_ENABLED) printDebug("READER: flush '" + minTrackingId + "'");
                doCallback();
            }                       
        }
        
        synchronized void queue(DatagramPacket packet) {
            synchronized (dataQueue) {
                dataQueue.add(packet);
                dataQueue.notify();
            }
        }
    }
    
    /*
     * Packets can arrive out of order or not at all, so, as we collect
     * and reassemble incoming packets we need some rule to decide which
     * of our callbacks to execute.
     *
     * Assumptions:
     * We are tracking at most N packets at a time.  
     * We will execute at least one callback for each sequential id starting at 0
     */
    private class Reader implements Runnable {

        void stop() {
            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
        }
        
        public void run() {
            
            printDebug("READER: thread starting");
            
            while (mSocket != null) {
                try {
                    byte [] buffer = new byte[mOptimalPacketSize];
                    DatagramPacket packet = new DatagramPacket(buffer, mOptimalPacketSize);
                    mSocket.receive(packet);
                    mProcessor.queue(packet);
                } catch (SocketException e) {
                    if (e.getMessage().equalsIgnoreCase("Socket closed")) {
                        break; // end the connection
                    }
                    else
                        e.printStackTrace();
                } catch (IOException e) {
                    if (DEBUG_ENABLED) printDebug("Failed to parse header...");
                    mAddress = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            printDebug("READER: thread exiting");
            
        }
    }
}
