package cc.lib.crypt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

import cc.lib.utils.GException;

// 0 goes left
// 1 goes right

public class HuffmanEncoding implements Cypher {

    // this is the maximum number of bits to encode a single byte.
    // another way to think of it is it is the max depth of the tree.
    private final static int MAX_BIT_SIZE       = 32; 
    
    private final static int COUNTS_ARRAY_SIZE  = 256; // size of ASCii table
    private final static String ENCODING_ID     = "XsXsXsXsXsXs";
    
    /**
     * 
     * @author ccaron
     *
     */
    public static class HuffmanException extends Exception {
        public HuffmanException (String msg) {
            super(msg);
        }
    }
    
    /*
     * 
     */
    private class ByteCode implements Comparable<ByteCode> {
        
        private int c;
        private long occurances = 0;
        private ByteCode left, right, parent;
        
        ByteCode(int c) {
            this.c = c;
        }
        
        ByteCode (ByteCode left, ByteCode right) {
            this.left = left;
            this.right = right;
            left.setParent(this);
            right.setParent(this);
            occurances = left.getOccurances() + right.getOccurances();
        }
        
        int getByte() {
            return c;
        }
        
        /*
         *  (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            if (!isLeaf())
                return "|";
            if (c>=32 && c<=128)
                return new String("char '" + (char)c + "' count[" + occurances + "] code [" + getCodeStringR(this, "") + "]");
            return new String("char (" + c + ") count[" + occurances + "] code [" + getCodeStringR(this, "") + "]");
        }
        
        String getCodeStringR(ByteCode code, String soFar) {
            if (code.parent == null)
                return soFar;
            return getCodeStringR(code.getParent(), "" + code.getBit() + soFar);
        }
        
        int [] getCode() {
            int depth = 0;
            ByteCode code = this;
            int value = 0;
            while (code.parent != null) {
                value <<= 1;
                value |= code.getBit();
                code = code.parent;
                depth++;
            }
            return new int [] { depth, value }; 
        }
        
        int getBit() {
            if (parent != null) {
                if (parent.getLeft() == this)
                    return 0;
                return 1;
            }
            throw new RuntimeException("Cannot be called on root node");
        }
        
        void incrementOccurance() {
            if (occurances + 1 < 0)
                throw new GException("Overflow");
            occurances ++;
        }

        void setOccurance(int occurance) {
            occurances = occurance;
        }
        
        long getOccurances() {
            return occurances;
        }
        
        void setParent(ByteCode parent) {
            this.parent = parent;
        }
        
        public int compareTo(ByteCode o) {
            
            // we want decreasing order of probability
            if (getOccurances() < o.getOccurances())
                return 1;
            if (getOccurances() > o.getOccurances())
                return -1;
            return 0;
        }
        
        public boolean isLeaf() {
            return left == null && right == null;
        }
        
        public ByteCode getLeft() {
            return left;
        }
        
        public ByteCode getRight() {
            return right;
        }
        
        public ByteCode getParent() {
            return parent;
        }
        
        void dismantle() {
            parent = null;
            if (left != null)
                left.dismantle();
            left = null;
            if (right != null)
                right.dismantle();
            right = null;
        }
        
    }
    
    /*
     *
     */
    private final ByteCode[] counts = new ByteCode[COUNTS_ARRAY_SIZE];
    
    /*
     * 
     */
    private ByteCode root = null;

    /**
     *
     */
    public HuffmanEncoding() {
        clear();
    }

    public HuffmanEncoding(String key) throws HuffmanException {
        clear();
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("Empty key");
        int idx = 0;
        for (int i = 0; i < counts.length; i++) {
            counts[i].setOccurance((key.charAt(idx) + 1579) % 1093);
            idx = (idx + 1) % key.length();
        }
        generate();
    }

    public HuffmanEncoding(int[] counts) throws HuffmanException {
        clear();
        if (counts.length > this.counts.length)
            throw new AssertionError();
        for (int i = 0; i < counts.length; i++) {
            this.counts[i].setOccurance(counts[i]);
        }
        generate();
    }

    long [] getCounts() {
        long [] out = new long[COUNTS_ARRAY_SIZE];
        for (int i=0; i<this.counts.length; i++) {
            out[i] = counts[i].occurances;
        }
        return out;
    }

    /**
     * 
     *
     */
    public void clear() {
        for (int i=0; i<counts.length; i++) {
            counts[i] = new ByteCode(i);
        }
        root = null;
    }
    
    /**
     * 
     * @param input
     * @throws IOException
     */
    public void importCounts(InputStream input) throws IOException {
        byte [] buffer = new byte[COUNTS_ARRAY_SIZE];
        while (true) {
            int num = input.read(buffer);
            if (num < 0)
                break;
            for (int i=0; i<num; i++) {
                //int n = (buffer[i] + 256) % 256;
                int n = buffer[i];// < 0 ? buffer[i] + 128 : buffer[i];
                n = (n+256) % 256;
                //if (n < 0)
                //    n += 256;
                //n += 128;
                counts[n].incrementOccurance();
            }
        }
    }

    public final void importCounts(String s) {
        byte [] b = s.getBytes();
        for (int i=0; i<b.length; i++) {
            int n = b[i];
            n = (n+256) % 256;
            counts[n].incrementOccurance();
        }
    }

    public final void generateRandomCountsFromExisitngOccurances(long seed) {
        Random random = new Random(seed);
        for (int i=0; i<counts.length; i++) {
            if (counts[i].occurances > 0)
                counts[i].occurances = 1 + random.nextInt(1024);
        }
    }

    public void generateRandomCounts(long seed) {
        Random random = new Random(seed);
        for (int i=0; i<counts.length; i++) {
            counts[i].occurances = 1 + random.nextInt(1024);
        }
    }
    
    public void initStandardCounts() {
        String keys = " abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!@#$%^&*()`~-_=+[]{}\\|;:'\",<.>/?";
        importCounts(keys);
    }

    /**
     * 
     * @param file
     * @throws IOException
     */
    public void importCounts(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        importCounts(in);
        in.close();
    }
    
    /**
     * 
     * @param files
     * @throws IOException
     */
    public void importCounts(File [] files) throws IOException {
        for (int i=0; i<files.length; i++)
            importCounts(files[i]);
    }
    
    /**
     * 
     * @param out
     */
    public void debugDump(PrintStream out) {
        //for (int i=0; i<COUNTS_ARRAY_SIZE; i++) {
        //    if (counts[i].getOccurances() > 0)
        //        out.println("char [" + i + "] " + counts[i]);
        //}
        if (!isValid())
            throw new RuntimeException("Tree not generated");
        debugDumpTreeR(out, root, "");
    }
    
    /**
     * 
     * @return
     */
    public boolean isValid() {
        return root != null;
    }
    
    /**
     * 
     * @param out
     * @param root
     * @param indent
     */
    public static void debugDumpTreeR(PrintStream out, ByteCode root, String indent) {
        if (root.isLeaf())
            out.println(indent + root);
        else {
            debugDumpTreeR(out, root.getLeft(), indent+"   ");
            debugDumpTreeR(out, root.getRight(), indent+"   ");
        }
    }

    /**
     * To be called before generate. Makes sure all occurances are at least 1.
     */
    public void keepAllOccurances() {
        for (ByteCode b : counts) {
            if (b.occurances == 0) {
                b.incrementOccurance();
            }
        }
    }

    /**
     * generate the search tree
     * @throws HuffmanException
     */
    public void generate() throws HuffmanException {
        LinkedList<ByteCode> queue1 = new LinkedList<ByteCode>();

        if (counts[0].occurances == 0)
            counts[0].occurances = Integer.MAX_VALUE; // we want to make sure 0 == 0 in both directions
        
        // add all the elems
        for (int i=0; i<counts.length; i++) {
            if (counts[i].occurances > 0)
                queue1.addLast(counts[i]);
        }
        
        if (queue1.size() <= 0) {
            throw new HuffmanException("no occurances found");
        }
        
        // algorithm
        while (queue1.size() > 1) {
            Collections.sort(queue1);
            // pop off the 2 
            ByteCode c0 = queue1.removeLast();
            ByteCode c1 = queue1.removeLast();
            ByteCode newNode = new ByteCode(c0, c1);
            queue1.addLast(newNode);
        }
        
        root = queue1.getFirst();
        
        if (searchDepthR(root, 0) > MAX_BIT_SIZE) {
            root.dismantle();
            root = null;
            throw new HuffmanException("Unbalanced tree.  depth excedes " + MAX_BIT_SIZE);
        }
        
        fixTree();
    }
    
    /*
     * 
     */
    private void fixTree() {
        if (counts[0].getCode()[1] != 0) {
            // fix the tree so that char (0) is encoded as 0
            // now find the '0' encoding
            for (int i=0; i<counts.length; i++) {
                if (counts[i].getParent() != null && counts[i].getCode()[1] == 0) {
                    if (counts[i].getByte() != 0) {
                        counts[0].c = counts[i].c;
                        counts[i].c = 0;
                        // swap the elems in the array
                        ByteCode t = counts[0];
                        counts[0] = counts[i];
                        counts[i] = t;
                    }
                    break;
                }
            }
        }
    }
    
    /*
     * 
     */
    private static int searchDepthR(ByteCode code, int depth) {
        if (code.getLeft() == null)
            return depth;
        int depthLeft = searchDepthR(code.getLeft(), depth+1);
        int depthRight = searchDepthR(code.getRight(), depth+1);
        return Math.max(depthLeft, depthRight);
    }
    
    /**
     * 
     * @param file
     * @throws IOException
     */
    public void saveEncoding(File file) throws IOException {
        OutputStream out = new FileOutputStream(file);
        out.write(ENCODING_ID.getBytes());
        for (int i=0; i<COUNTS_ARRAY_SIZE; i++) {
            String str = ":" + String.valueOf(this.counts[i].occurances);
            out.write(str.getBytes());
        }
        out.close();
    }

    public void printEncodingAsCode(PrintStream out) {
        out.println(getEncodingAsCode());
    }

    public String getEncodingAsCode() {
        StringBuffer buf = new StringBuffer(256);
        buf.append("int [] counts = {");
        int n = COUNTS_ARRAY_SIZE;
        while (n > 0 && counts[--n].occurances == 0) {
        }
        if (n <= 0) {
            System.err.println("nothing counted");
        }
        for (int i = 0; i <= n; i++) {
            if (i > 0) {
                buf.append(",");
            }
            buf.append(counts[i].occurances);
        }
        buf.append("};");
        return buf.toString();
    }

    /**
     * 
     * @param file
     * @throws IOException
     */
    public void loadEncoding(File file) throws IOException {
        clear();
        InputStream in = new FileInputStream(file);
        if (in.available() < 256 || in.available() > 1024) {
            in.close();
            throw new IOException("Invalid encoding file");
        }
        byte [] buffer = new byte[in.available()];
        in.read(buffer);
        String str = new String(buffer);
        String [] parts = str.split(":");
        if (parts.length != COUNTS_ARRAY_SIZE+1) {
            in.close();
            throw new IOException("Invalid encoding file");
        }
        if (!parts[0].equals(ENCODING_ID)) {
            in.close();
            throw new IOException("Invalid encoding file");
        }
        try {
            for (int i=0; i<COUNTS_ARRAY_SIZE; i++) {
                counts[i].occurances = Integer.parseInt(parts[i+1]);
            }
            in.close();
        } catch (NumberFormatException e) {
            in.close();
            throw new IOException("Invalid encoding file");
        } 
        try {
            generate();
        } catch (HuffmanException e) {
            clear();
            throw new IOException(e.getMessage());
        }
    }
    
    /*
     *  (non-Javadoc)
     * @see Cypher#encrypt(int)
     */
    public int [] encrypt(int uncrypted) {
        //uncrypted += 128;
        if (uncrypted < 0)
            uncrypted += 256;
        //uncrypted = (uncrypted+256)%256;
        return counts[uncrypted].getCode();
    }
    
    /*
     *  (non-Javadoc)
     * @see Cypher#decrypt(int)
     */
    @Override
    public int [] decrypt(int encrypted) {
        // traverse the tree to get to the child
        return decryptR(encrypted, root, 0, 0);
    }

    /*
     *  (non-Javadoc)
     * @see Cypher#getMaxEncodedBitLength()
     */
    public int getMaxEncodedBitLength() {
        return MAX_BIT_SIZE;
    }
    
    /*
     * 
     */
    private int [] decryptR(int encrypted, ByteCode root, int bit, int code) {
        if (root.isLeaf()) {
            if (code != root.getCode()[1])
                throw new RuntimeException("Inconsistent");
            int c = root.getByte();
            //c -= 128;
            return new int [] { bit, c };
        }
        if ((encrypted & (1<<bit)) == 0) {
            return decryptR(encrypted, root.getLeft(), bit+1, code);
        }
        return decryptR(encrypted, root.getRight(), bit+1, code | (1<<bit));
        
    }

    public final void increment(int b) {
        counts[(b+256)%256].incrementOccurance();
    }

    public final void increment(byte [] bytes, int offset, int length) {
        for (int i=0; i<length; i++) {
            increment(bytes[i+offset]);
        }
    }
}
