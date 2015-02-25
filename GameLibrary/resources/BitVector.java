package cc.lib.crypt;

import java.util.Arrays;

import cc.lib.utils.Profiler;

/**
 * Class for managing a set of bits
 * 
 * Ordering:
 * 
 * example:
 * assume array = new byte[3];
 * then, BitVector(array) becomes:
 * 
 * byte[2] byte[1] byte[0]
 * ^                     ^ 
 * bit 24                bit 0
 * 
 * @author ccaron
 *
 */
@SuppressWarnings("unused")
public class BitVector {

    private int [] buffer;
    private int len = 0;
    
    /*
     * 
     */
    private void grow(int newSize) {
        int ns = (int)(newSize/32 + 1);//newSize = newSize/32 + 1;
        if (ns < 32)
            ns = 32;
        int [] newBuffer = new int[ns];
        for (int i=0; i<buffer.length; i++)
            newBuffer[i] = buffer[i];
        buffer = newBuffer;
        
    }

    /*
     * 
     */
    private int getMaxBits() {
        return buffer.length * 32;
    }

    /**
     * 
     */
    public BitVector() {
        buffer = new int[32];
    }
    
    /**
     * 
     * @param v
     */
    public BitVector(BitVector v) {
        buffer = new int[v.buffer.length];
        System.arraycopy(v.buffer, 0, buffer, 0, v.buffer.length);
        len = v.len;
    }
    
    /**
     * 
     * @param bytes
     */
    public BitVector(byte [] bytes) {
        this(bytes, bytes.length);
    }
    
    /**
     * 
     * @param bytes
     * @param end
     */
    public BitVector(byte [] bytes, int end) {
        this(bytes, 0, end);
    }

    /**
     * 
     * @param bytes
     * @param start
     * @param end
     */
    public BitVector(byte [] bytes, int start, int end) {
        this((end-start) * 8);
        int num = end-start;
        for (int i=start; i<num; i++) {
            for (int b=0; b<8; b++) {
                boolean value = (bytes[i] & (1<<b)) != 0;
                set(len++, value);
            }
        }        
    }

    /**
     * 
     * @param words
     */
    public BitVector(int [] words) {
        this(words, words.length);
    }

    /**
     * 
     * @param words
     * @param num
     */
    public BitVector(int [] words, int num) {
        this(words, 0, num);
    }
    
    /**
     * 
     * @param words
     * @param start
     * @param end
     */
    public BitVector(int [] words, int start, int end) {
        this((end-start) * 32);
        int num = end-start;
        for (int i=0; i<num; i++) {
            for (int b=0; b<32; b++) {
                boolean value = (words[i+start] & (1<<b)) != 0;
                set(len++, value);
            }
        }            
    }
    
    /**
     * 
     * @param numBits
     */
    public BitVector(int numBytes) {
        buffer = new int [numBytes];
        len = 0;
    }
    
    /**
     * Set the length of this vector too 0
     */
    public void clear() {
        len = 0;
        Arrays.fill(buffer, 0);
    }
    
    /**
     * get the bit at index
     * @param index
     * @return
     */
    public boolean get(int index) {
        if (index >= len)
            throw new IndexOutOfBoundsException(String.valueOf(index));
        return (buffer[(int)(index>>5)] & (1<<(index&0x1f))) != 0; // optimized
    }
    
    /**
     * 
     * @param index
     * @param value
     */
    public void set(int index, boolean value) {
        if (index < 0 || index >= this.getMaxBits())
            throw new IndexOutOfBoundsException(String.valueOf(index));
        if (!value)
            buffer[(int)(index>>5)] &= ~(1<<(index&0x1f));
        else
            buffer[(int)(index>>5)] |= (1<<(index&0x1f));
    }
    
    /**
     * 
     * @return
     */
    public int getLen() {
        return len;
    }
    
    /**
     * 
     * @param value
     */
    public void pushBack(boolean value) {
        if (len == getMaxBits()) {
            // grow
            grow(len * 2);
        }
        set(len, value);
        len++;
    }
    
    /**
     * 
     * @param bits
     * @param numBits
     */
    public void pushBack(int bits, int numBits) {
        if (numBits > 32)
            throw new IllegalArgumentException("Invalid value for numBits '" + numBits + "' <= 32");
        for (int i=0; i<numBits; i++) {
            boolean x = (bits & (1<<i)) == 0 ? false : true;
            pushBack(x);
        }
    }
    
    /**
     * 
     * @return
     */
    public byte toByte() {
        return toByte(0);
    }
    
    /**
     * 
     * @param startIndex
     * @return
     */
    public byte toByte(int startIndex) {
        return toByte(startIndex, startIndex+8);
    }

    /**
     * 
     * @param startIndex
     * @return
     */
    public byte toByte(int startIndex, int endIndex) {
        if (endIndex < startIndex)
            throw new IllegalArgumentException("endIndex [" + endIndex + "] is < startIndex [" + startIndex + "]");
        endIndex -= startIndex;
        endIndex = Math.min(8, endIndex);
        endIndex = Math.min(len - startIndex, endIndex);
        byte result = 0;
        for (int i=0; i<endIndex; i++) {
            //result <<= 1;
            if (get(startIndex + i))
                result |= (0x1 << i);
        }
        return result;
    }
    
    /**
     * 
     * @return
     */
    public int toInt() {
        return toInt(0);
    }

    /**
     * 
     * @param startIndex
     * @return
     */
    public int toInt(int startIndex) {
        if (startIndex < 0 || startIndex > len)
            throw new IllegalArgumentException("Invalid value for startIndex (0<=" + startIndex + "<" + len + ")");
        int result = 0;
        for (int i=0; (i+startIndex)<len && i<32; i++) {
            //result <<= 1;
            if (get(startIndex + i))
                result |= (0x1 << i);
        }
        return result;
    }

    /*
     *  (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<len; i++) {
            buf.append(get(i) ? "1" : "0");
        }
        return buf.toString();
    }
    
    /**
     * 
     * @param value
     */
    public void pushFront(boolean value) {
        if (len >= getMaxBits()) {
            grow(len*2);
        }
        for (int i=len; i>0; i--)
            set(i, get(i-1));
        set(0, value);
    }
    
    /**
     * Shift bits such that the total number of bit decreases.  Bits are popped from the front.
     * 
     * WARNING: CPU Intensive!! O((len-numBits)/32).
     * This function can be slow when len is large and shift length is small.  
     * 
     * @param numBits
     */
    public synchronized void shiftRight(int numBits) {
        if (numBits < 0 || numBits > len)
            throw new RuntimeException("Invalid value for rightShift [" + numBits + "]");
        if (numBits == 0)
            return;
        if (numBits >= len) {
            Arrays.fill(buffer, 0);
            len = 0;
            return;
        }
        
        int s0 = 0;
        int s1 = numBits / 32;
        final int n  = len / 32;
        final int nb = numBits & 0x1f; // optimized %32
        final int upperMask = ((1<<(32-nb))-1);
        
        for (int i=s1; i<n; i++) {
            buffer[s0] = (buffer[s1] >> nb) & upperMask;
            buffer[s0] |= ((buffer[s1+1] & upperMask)  << (32-nb)); 
            s0++; s1++;
        }
        buffer[s0] = buffer[s0] >> nb;
        len -= numBits;
        
        
    }
    
    /**
     * 
     * @param num
     */
    public void slow_shiftRight(int numBits) {
        if (numBits < 0)
            throw new RuntimeException("Invalid value for rightShift [" + numBits + "]");
        if (numBits == 0)
            return;
        if (numBits > len)
            numBits = len;
        int newLen = len - numBits;
        for (int i=0; i<newLen; i++)
            set(i, get(i+numBits));
        len = newLen;
    }
    
    /**
     * Convenience.  Shift left for negative numBits and right for positive.
     * fill value only relevant when numBits < 0
     * @param numBits
     * @param fillValue
     */
    public void shift(int numBits, boolean fillValue) {
        if (numBits < 0)
            shiftLeft(-numBits, fillValue);
        else if (numBits > 0)
            shiftRight(numBits);
    }
    
    
    
    /**
     * 
     * @param num
     * @param padValue
     */
    public void shiftLeft(int num, boolean padValue) {
        if (num == 0)
            return;
        if (num + this.len > this.getMaxBits())
            grow((len+num)*2);
        for (int i=len+num-1; i>=num; i--)
            this.set(i, get(i-num));
        for (int i=0; i<num; i++)
            set(i, padValue);
        len += num;
    }

    /**
     * Return a BitVector that is the INTERSECTION of 2 inputs.
     * The result will be have len that is the MIN of the inputs' lengths.
     * @param rhs
     * @return
     */
    public BitVector and(BitVector rhs) {
        int minLen = Math.min(getLen(), rhs.getLen());
        BitVector result= new BitVector(minLen);
        for (int i=0; i<minLen; i++) {
            boolean value = get(i) && rhs.get(i);
            result.set(i, value);
        }
        return result;
    }
    
    /**
     * Return BitVector that is the UNION of 2 inputs.
     * the result will have length that s the MAX of the inputs' lengths.
     * @param rhs
     * @return
     */
    public BitVector or(BitVector rhs) {
        int minLen = Math.min(getLen(), rhs.getLen());
        BitVector result= new BitVector(minLen);
        int i = 0;
        for (i=0; i<minLen; i++) {
            boolean value = get(i) || rhs.get(i);
            result.set(i, value);
        }
        while (i<getLen()) {
            result.set(i, get(i));
            i++;
        }
        
        while (i<rhs.getLen()) {
            result.set(i, rhs.get(i));
            i++;
        }
        
        return result;
    }
    
    /**
     * 
     * @param rhs
     * @return
     */
    public BitVector xor(BitVector rhs) {
        int minLen = Math.min(getLen(), rhs.getLen());
        BitVector result= new BitVector(minLen);
        int i = 0;
        for (i=0; i<minLen; i++) {
            boolean value = (get(i) || rhs.get(i)) && !(get(i) && rhs.get(i));
            result.set(i, value);
        }
        while (i<getLen()) {
            result.set(i, get(i));
            i++;
        }
        
        while (i<rhs.getLen()) {
            result.set(i, rhs.get(i));
            i++;
        }
        
        return result;
    }
    
    /**
     * Append bits from another too this
     * @param toAppend
     */
    public void append(BitVector toAppend) {
        int minLen = getLen() + toAppend.getLen();
        if (minLen > getMaxBits()) {
            grow(minLen);
        }
        for (int i=len, j=0; i<minLen && j<toAppend.getLen(); i++, j++) {
            set(i, toAppend.get(j));
        }
        this.len = minLen;
    }
    
    /**
     * 
     * @param bits
     */
    public void append(byte bits) {
        for (int i=0; i<8; i++) {
            boolean value = (bits & (1<<i)) != 0;
            this.pushBack(value);
        }
    }
    
    /**
     * 
     * @param bits
     * @param numBits
     */
    public void append(int bits, int numBits) {
        for (int i=0; i<32 && i<numBits; i++) {
            boolean value = (bits & (1<<i)) != 0;
            pushBack(value);
        }
    }
    
    /*
     *  (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        BitVector v = (BitVector)o;
        if (v.getLen() != getLen())
            return false;
        for (int i=0; i<getLen(); i++) {
            if (get(i) != v.get(i))
                return false;
        }
        return true;
    }

    public void getBits(byte[] chunk, int start, int end) {
        int s = 0;
        for (int i=start; i<end && s < getLen(); i++) {
            chunk[i] = toByte(s, s+8);
            s += 8;
        }
    }
    
    public void getBits(byte[] chunk) {
        getBits(chunk, 0, chunk.length);
    }
}
