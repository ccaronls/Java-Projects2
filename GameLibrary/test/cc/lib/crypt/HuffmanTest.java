package cc.lib.crypt;

import java.io.*;

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
            encoding.generate(true);
            //encoding.debugDump(System.out);
            cypher = encoding;
        }
        return cypher;
    }

    @Override
    public void testTextFile() throws Exception {
        HuffmanEncoding encoding = new HuffmanEncoding();
        encoding.importCounts(new File("resources/cyphertest.txt"));
        encoding.generate(true);
        cypher = encoding;
        super.testTextFile();
    }

    @Override
    public void testBinaryFile() throws Exception {
        HuffmanEncoding encoding = new HuffmanEncoding();
        encoding.importCounts(new File("resources/streamvideo.3gp"));
        encoding.generate(true);
        cypher = encoding;
        super.testBinaryFile();
    }

    @Override
    public void testDataStream() throws Exception {
        HuffmanEncoding encoding = new HuffmanEncoding();
        //encoding.importCounts(new File("resources/streamvideo.3gp"));
        encoding.generateRandomCounts(0);
        encoding.generate(true);
        //encoding.debugDump(System.out);
        cypher = encoding;
        super.testDataStream();
    }

    public void testSOCFile() throws Exception {
    	String txt = loadFile("resources/socsavegame.txt").toString();
        HuffmanEncoding encoding = new HuffmanEncoding();
        encoding.importCounts(new File("resources/socsavegame.txt"));
        encoding.generate(true);
        cypher = encoding;
    	processTextFile(txt);
    }
    
    
}
