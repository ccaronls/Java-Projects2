package cc.lib.crypt;

import java.io.*;

import cc.lib.utils.Profiler;

/**
 * Class to read encrypted input to normal output
 *  
 * @author ccaron
 *
 */
public class EncryptionInputStream extends InputStream {

    private final DataInputStream input;
    private final Cypher cypher;
    private final BitVector bits = new BitVector(256);
    
    /**
     * 
     * @param input
     * @param cypher
     */
    public EncryptionInputStream(InputStream input, Cypher cypher) {
        this.input = new DataInputStream(input);
        this.cypher = cypher;
    }
    
    private int readChunk() throws IOException {
        if (Profiler.ENABLED) Profiler.push("EncryptionOutputStream::readChunk");
        try {
            int chunkSize = 0;
            int numBits = 0;
            final int EXTRA_BYTES = 8; 
            if (input.available() < 0) {
                System.err.println("Conditional met but no code!");
            }
            try {
                // WARNING!! If we change this then we need to change EXTRA_BYTES
                chunkSize = input.readInt(); 
                numBits = input.readInt();
            } catch (EOFException e) {
                return -1;
            }
            //System.out.println("Reading chunk " + chunkSize + " with " + numBits + "bits");
            byte [] chunk = new byte[chunkSize];
            int numRead = input.read(chunk);
            // BLOCKING FUNCTION!
            while (numRead < chunkSize) {
                int n = input.read(chunk, numRead, chunk.length-numRead);
                if (n < 0)
                    throw new EOFException("Expected chunk size '" + chunkSize + "' but only got '" + numRead + "' bytes");
                numRead += n;
            }
            for (int i=0; i<chunk.length && numBits > 0; i++) {
                int num = Math.min(8, numBits);
                bits.pushBack(chunk[i], num);
                numBits -= num;
            }
            return chunkSize + EXTRA_BYTES;
        } finally {
            if (Profiler.ENABLED) Profiler.pop("EncryptionOutputStream::writeChunk");
        }
    }
    
    public synchronized int read() throws IOException {
        if (Profiler.ENABLED) Profiler.push("EncryptionInputStream.read()");
        try {
            if (bits.getLen() <= 0) {
                if (readChunk() < 0)
                    return -1;
            }
            int value = bits.toInt();
            int [] r = cypher.decrypt(value);
            //BitVector decrypted = new BitVector();
            //decrypted.pushBack(value, r[0]);
            //System.out.println("read '"+  (byte)r[1] + "'  <--- " + decrypted);
            //Profiler.push("shift right" + r[0]);
            bits.shiftRight(r[0]);
            //Profiler.pop("shift right" + r[0]);

            value = r[1];
            if (value < 0) 
                return -1;
            return value;
        } finally {
            if (Profiler.ENABLED) Profiler.pop("EncryptionInputStream.read()");            
        }
    }
    
    /*
     *  (non-Javadoc)
     * @see java.io.InputStream#available()
     */
    public synchronized int available() throws IOException {
        int avail = input.available();
        while (avail > 0) {
            int chunkSize = readChunk();
            if (chunkSize < 0)
                break;
            avail -= chunkSize;
        }
        
        avail = 0;
        int start = 0;
        int bitsLeft = bits.getLen();
        while (true) {
            int value = bits.toInt(start);
            int [] r = cypher.decrypt(value);
            if (r[0] > bitsLeft)
                break;
            bitsLeft -= r[0];
            start += r[0];
            avail += 1;
        }
        return avail;
    }
    
    /*
     *  (non-Javadoc)
     * @see java.io.InputStream#markSupported()
     */
    public boolean markSupported() {
        return false;
    }
    
    /*
     *  (non-Javadoc)
     * @see java.io.InputStream#mark(int)
     */
    public void mark(int readLimit) {
        input.mark(readLimit);
    }
    
    /*
     *  (non-Javadoc)
     * @see java.io.InputStream#reset()
     */
    public void reset() throws IOException {
        input.reset();
        bits.clear();
    }
    
    /*
     *  (non-Javadoc)
     * @see java.io.InputStream#skip(long)
     */
    public long skip(long n) throws IOException {
        //return input.skip(n);
        throw new IOException("Search through an encrypted file is not supported");
    }
    
    /*
     *  (non-Javadoc)
     * @see java.io.Closeable#close()
     */
    public void close() throws IOException {
        input.close();
    }


}
