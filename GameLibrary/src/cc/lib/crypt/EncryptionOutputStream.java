package cc.lib.crypt;

import java.io.*;

import cc.lib.utils.Profiler;

/**
 * Write normal input too encrypted input
 * @author ccaron
 *
 */
public class EncryptionOutputStream extends OutputStream {

    // Problem:  Encryption can result in unaligned data (not divisible by 8) 
    // Solution: Send the data in fixed size chunks that are preceded by the
    //           number of bits in the chunk.  The chunk size should be set based
    //           on the situation.  For large files a large chunk size (1024*16)
    //           and for socketed connections a small chunk size like 1024.
    
    private final DataOutputStream out;
    private final Cypher cypher;
    private final BitVector data;
    private final byte [] chunk;
    private final int chunkSizeBits;
    
    public final static int DEFAULT_CHUNK_SIZE_BYTES = 1024;
    
    public EncryptionOutputStream(OutputStream out, Cypher cypher) {
        this(out, cypher, DEFAULT_CHUNK_SIZE_BYTES);
    }

    /**
     * Create a output stream that will encrypt the output.
     * 
     * @param out
     * @param cypher
     * @param chunkSizeBytes each chunk results in an additional 8 bytes of overhead as well as those bits nor used in the chunk.
     *                       tuning of this parameter for File I/O (high) or network (I/O) may be required depending on the circumstance.
     */
    public EncryptionOutputStream(OutputStream out, Cypher cypher, int chunkSizeBytes) {
        this.out = new DataOutputStream(out);
        this.cypher = cypher;
        this.chunkSizeBits = chunkSizeBytes * 8;
        data = new BitVector(chunkSizeBits);//cypher.getMaxEncodedBitLength());
        chunk = new byte[chunkSizeBytes];
    }

    @Override
    synchronized public void flush() throws IOException {
        while (data.getLen() > 0) {
            writeChunk();
        }
        out.flush();
    }

    private void writeChunk() throws IOException {
        try {
            if (Profiler.ENABLED) Profiler.push("EncryptionOutputStream::writeChunk");
            int numBits = Math.min(data.getLen(), chunkSizeBits);
            data.getBits(chunk);
            out.writeInt(chunk.length); // write the size of the chunk
            out.writeInt(numBits);
            out.write(chunk);
            data.shiftRight(numBits);
            //System.out.println("writeChunk numBits=" + numBits + ", chunkLen=" + chunk.length + ", data.len=" + data.getLen());
        } finally {
            if (Profiler.ENABLED) Profiler.pop("EncryptionOutputStream::writeChunk");
        }
    }

    @Override
    synchronized public void write(int b) throws IOException {
        if (Profiler.ENABLED) Profiler.push("EncryptionOutputStream.write()");
        try {
            int [] e = cypher.encrypt(b);
            //BitVector encrypted = new BitVector();
            //encrypted.pushBack(e[1], e[0]);
            //System.out.println("write: '" + (byte)n + "'    -> " + encrypted);
            synchronized (data) { 
                data.pushBack(e[1], e[0]);
            }
            
            if (data.getLen() >= chunkSizeBits - cypher.getMaxEncodedBitLength())
                writeChunk();
        } finally {
            if (Profiler.ENABLED) Profiler.pop("EncryptionOutputStream.write()");
        }
    }

    @Override
    public void close() throws IOException {
        flush();
        out.close();
        data.clear();
        super.close();
    }
    
}
