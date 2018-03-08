package cc.lib.crypt;

import java.io.*;

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
        encoding.importCounts(new File("dominos.save"));
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
    
}
