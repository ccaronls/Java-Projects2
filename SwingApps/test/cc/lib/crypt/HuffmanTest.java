package cc.lib.crypt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Random;

import cc.lib.utils.FileUtils;

public class HuffmanTest extends SimpleCypherTest {

    Cypher cypher = null;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        cypher = null;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        cypher = null;
    }

    @Override
    protected Cypher getCypher() throws Exception {
        if (cypher == null) {
            HuffmanEncoding encoding = new HuffmanEncoding();
            //encoding.importCounts(new File("resources/cyphertest.txt"));
            encoding.generateRandomCounts(System.currentTimeMillis());
            encoding.generate();
            //encoding.debugDump(System.out);
            cypher = encoding;
        }
        return cypher;
    }

    public void testRandomNumbers() throws Exception {
        String kb = "1234567890-=`~!@#$%^&*()_+qwertyuiop[]\\asdfghjkl;'zxcvbnm,.//QWERTYUIOP{}|ASDFGHJKL:\"ZXCVBNM<>?\n\r\t";
        HuffmanEncoding enc = new HuffmanEncoding();
        enc.importCounts(kb);
        enc.generateRandomCounts(3287923);
        //enc.generateRandomCountsFromExisitngOccurances(871270);
        enc.generate();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EncryptionOutputStream eout = new EncryptionOutputStream(out, enc);
        DataOutputStream dout = new DataOutputStream(eout);

        Random r = new Random();
        r.setSeed(0);

        for (int i=0; i<1000; i++) {
            int ii = r.nextInt();
            dout.writeInt(ii);
        }

        dout.flush();

        DataInputStream din = new DataInputStream(new EncryptionInputStream(new ByteArrayInputStream(out.toByteArray()), enc));

        r.setSeed(0);
        for (int i=0; i<1000; i++) {
            if (i % 100 == 0)
                System.out.print(".");
            int ii = r.nextInt();
            assertEquals(din.readInt(), ii);
        }

        // longs

        out.reset();
        r.setSeed(340);
        for (int i=0; i<1000; i++) {
            long ii = r.nextLong();
            dout.writeLong(ii);
        }

        dout.flush();
        din = new DataInputStream(new EncryptionInputStream(new ByteArrayInputStream(out.toByteArray()), enc));

        r.setSeed(340);
        for (int i=0; i<1000; i++) {
            if (i % 100 == 0)
                System.out.print(".");
            long ii = r.nextLong();
            assertEquals(din.readLong(), ii);
        }

        // floats

        out.reset();
        r.setSeed(94058);
        for (int i=0; i<1000; i++) {
            float ii = r.nextFloat();
            dout.writeFloat(ii);
        }

        dout.flush();
        din = new DataInputStream(new EncryptionInputStream(new ByteArrayInputStream(out.toByteArray()), enc));

        r.setSeed(94058);
        for (int i=0; i<1000; i++) {
            if (i % 100 == 0)
                System.out.print(".");
            float ii = r.nextFloat();
            assertEquals(din.readFloat(), ii);
        }

        // doubles

        out.reset();
        r.setSeed(23498);
        for (int i=0; i<1000; i++) {
            double ii = r.nextDouble();
            dout.writeDouble(ii);
        }

        dout.flush();
        din = new DataInputStream(new EncryptionInputStream(new ByteArrayInputStream(out.toByteArray()), enc));

        r.setSeed(23498);
        for (int i=0; i<1000; i++) {
            if (i % 100 == 0)
                System.out.print(".");
            double ii = r.nextDouble();
            assertEquals(din.readDouble(), ii);
        }

    }
/*
    @Override
    public void testTextFile() throws Exception {
        HuffmanEncoding encoding = new HuffmanEncoding();
        encoding.importCounts(getClass().getClassLoader().getResourceAsStream("cyphertest.txt"));
        encoding.generate(true);
        cypher = encoding;
        super.testTextFile();
    }
*/
    @Override
    public void testBinaryFile() throws Exception {
        HuffmanEncoding encoding = new HuffmanEncoding();
        encoding.importCounts(getClass().getClassLoader().getResourceAsStream("signed_forms.pdf"));
        encoding.generate();
        cypher = encoding;
        super.testBinaryFile();
    }

    @Override
    public void testDataStream() throws Exception {
        HuffmanEncoding encoding = new HuffmanEncoding();
        //encoding.importCounts(new File("resources/streamvideo.3gp"));
        encoding.generateRandomCounts(0);
        encoding.generate();
        //encoding.debugDump(System.out);
        cypher = encoding;
        super.testDataStream();
    }

    public void testSOCFile() throws Exception {
    	String txt = loadFile("socsavegame.txt").toString();
        HuffmanEncoding encoding = new HuffmanEncoding();
        encoding.importCounts(getClass().getClassLoader().getResourceAsStream("socsavegame.txt"));
        encoding.generate();
        cypher = encoding;
    	processTextFile(txt);
    }

    public void testDominosFile() throws Exception {
        String txt = loadFile("dominos.save").toString();
        HuffmanEncoding encoding = new HuffmanEncoding();
        encoding.importCounts(getClass().getClassLoader().getResourceAsStream("dominos.save"));
        //encoding.keepAllOccurances();
        encoding.generate();
        cypher = encoding;
        processTextFile(txt);
        encoding.saveEncoding(new File("/tmp/encoding"));
        System.out.println(FileUtils.fileToString(new File("/tmp/encoding")));
        encoding.printEncodingAsCode(System.out);
    }

    public void testDominosFromPredeterminedCounts() throws Exception {
        int [] counts = {3884,1,1,1,1,1,1,1,1,1,476,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,42,430,1,152,130,160,83,48,58,26,35,41,198,1,1,1,285,1,1,1,5,1,1,35,8,1,1,1,1,1,1,17,29,1,1,50,1,1,1,46,1,37,1,1,1,1,1,1,1,1,1,1,240,63,320,62,366,3,51,40,313,11,6,175,191,154,247,319,1,120,123,179,36,13,1,37,36,1,102,1,102,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1};
        cypher = new HuffmanEncoding(counts);
        processTextFile(loadFile("dominos.save2").toString());
    }

    public void testGenRandomCountsForKB() throws Exception {
        String kb = "1234567890-=`~!@#$%^&*()_+qwertyuiop[]\\asdfghjkl;'zxcvbnm,.//QWERTYUIOP{}|ASDFGHJKL:\"ZXCVBNM<>?\n\r\t";
        HuffmanEncoding enc = new HuffmanEncoding();
        enc.importCounts(kb);
        enc.generateRandomCountsFromExisitngOccurances(System.currentTimeMillis());
        enc.generate();
        long [] c0 = enc.getCounts();
        enc.generate();
        long [] c1 = enc.getCounts();
        assertTrue(Arrays.equals(c0, c1));
        enc.printEncodingAsCode(System.out);

    }

    public void testJPG() throws Exception  {

        File tmp1 = File.createTempFile("tmp", "jpg", new File("/tmp/"));
        FileUtils.copy(getClass().getClassLoader().getResourceAsStream("librarybookszoom.jpg"), tmp1);

        System.out.println("File size of tmp1: " + tmp1.length());

        HuffmanEncoding enc = new HuffmanEncoding();
        enc.importCounts(getClass().getClassLoader().getResourceAsStream("librarybookszoom.jpg"));
        enc.generate();
        byte [] buf = new byte[1024];

        ByteArrayOutputStream encrypted = new ByteArrayOutputStream();
        long bytesRead = 0;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("librarybookszoom.jpg") ;
             OutputStream out = new EncryptionOutputStream(encrypted, enc)) {
            bytesRead = FileUtils.copy(in, out);
        }

        System.out.println("Bytes Read: " + bytesRead);
        System.out.println("Compressed Size: " + encrypted.size());

        File tmp2 = File.createTempFile("tmp", "jpg", new File("/tmp/"));
        try (InputStream in = new EncryptionInputStream(new ByteArrayInputStream(encrypted.toByteArray()), enc) ;
            OutputStream out = new FileOutputStream(tmp2)) {
            FileUtils.copy(in, out);
        }

        System.out.println("File size of tmp2: " + tmp2.length());


    }
}
