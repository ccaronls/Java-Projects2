package cc.lib.crypt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * This class demonstrates a simple encryptor
 * This simply maps characters
 *
 * @author ccaron
 */
public class SimpleCypher implements Cypher {

    // table provides: table[unencoded] -> encoded
    private int[] encodeLookup = new int[256];

    // table provides: table[encoded] -> unencoded
    private int[] decodeLookup = new int[256];

    /*
     * Dont allow uninitialized instances
     */
    private SimpleCypher() {
    }

    /**
     * @param file
     */
    public static SimpleCypher loadCypher(File file) throws IOException {
        SimpleCypher crypt = new SimpleCypher();
        crypt.load(file);
        return crypt;
    }

    /**
     * Return a randomly generated encryption
     *
     * @param seed
     * @return
     */
    public static SimpleCypher generateCypher(long seed) {
        SimpleCypher crypt = new SimpleCypher();
        crypt.generate(seed);
        return crypt;
    }

    /**
     * Generate a random encoding
     *
     * @param seed
     */
    public void generate(long seed) {
        for (int i = 0; i < encodeLookup.length; i++) {
            encodeLookup[i] = i;
            decodeLookup[encodeLookup[i]] = i;
        }
        if (seed != 0) {
            // shuffle
            Random r = new Random();
            r.setSeed(seed);
            for (int i = 0; i < 10000; i++) {
                int a = Math.abs(r.nextInt()) % 256;
                int b = Math.abs(r.nextInt()) % 256;
                int t = encodeLookup[a];
                encodeLookup[a] = encodeLookup[b];
                encodeLookup[b] = t;
                decodeLookup[encodeLookup[a]] = a;
                decodeLookup[encodeLookup[b]] = b;
            }
        }
    }

    /**
     * Save the encryption encoding too a file
     *
     * @param file
     * @throws IOException
     */
    public void save(File file) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        for (int i = 0; i < 256; i++) {
            out.write(String.valueOf(encodeLookup[i]) + '\n');
        }
        out.close();
    }

    /**
     * @param file
     * @throws IOException
     */
    public void load(File file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        try {
            for (int i = 0; i < 256; i++) {
                String line = in.readLine();
                int code = -1;
                try {
                    code = Integer.parseInt(line);
                } catch (NumberFormatException e) {
                    throw new IOException("Not a number [" + line + "]");
                }
                if (code < 0 || code >= 256)
                    throw new IOException("Invalid value [" + code + "] for code");
                encodeLookup[i] = code;
                decodeLookup[code] = i;
            }
        } finally {
            in.close();
        }
    }

    /*
     *  (non-Javadoc)
     * @see cc.lib.crypt.Cypher#encrypt(int)
     */
    public int[] encrypt(int mbyte) {
        mbyte = (mbyte & 0xff) % 256;
        return new int[]{8, encodeLookup[mbyte]};
    }

    /*
     *  (non-Javadoc)
     * @see cc.lib.crypt.Cypher#decrypt(int)
     */
    public int[] decrypt(int mbyte) {
        mbyte = (mbyte & 0xff) % 256;
        return new int[]{8, decodeLookup[mbyte]};
    }

    /*
     *  (non-Javadoc)
     * @see cc.lib.crypt.Cypher#getMaxEncodedBitLength()
     */
    public int getMaxEncodedBitLength() {
        return 8;
    }
}
