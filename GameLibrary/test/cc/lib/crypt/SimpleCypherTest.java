package cc.lib.crypt;

import java.io.*;

import cc.lib.utils.Profiler;

import junit.framework.TestCase;

public class SimpleCypherTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        Profiler.ENABLED = false;

        System.out.println("---------------------------------------\n"
                         + "Start Test: " + getName() + "\n"
                         + "---------------------------------------\n");
    }
    
    @Override
    protected void tearDown() throws Exception {

        System.out.println(
                "----------------------------------------\n"
              + "End Test: " + getName() + "\n");
        Profiler.dumpTimes(System.out);
        System.out.println(
                "----------------------------------------\n");
        
    }

    protected Cypher getCypher() throws Exception {
        return SimpleCypher.generateCypher(0);
    }

    protected ByteArrayOutputStream loadFile(String fileName) throws Exception {
        System.out.println("Loading file: " + fileName);
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        
        // read test file into String
        InputStream in = getClass().getClassLoader().getResourceAsStream(fileName);
        doRead(in, file);
        System.out.println("File length: " + file.size());
        return file;
    }
    
    public void testDataStream() throws Exception {
        
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(new EncryptionOutputStream(buffer, getCypher()));
        out.writeBoolean(true);
        out.writeBoolean(false);
        out.flush();
        out.writeFloat(1234567.0f);
        out.writeInt(01010101010);
        out.flush();
        out.writeLong(99999999999999999L);
        out.writeUTF("Hello");
        out.flush();
        out.close();
        //BitVector vec = new BitVector(buffer.toByteArray());
        System.out.println("Length of encrypted buffer: " + buffer.size());
        
        ByteArrayInputStream inBuffer = new ByteArrayInputStream(buffer.toByteArray());
        DataInputStream in = new DataInputStream(new EncryptionInputStream(inBuffer, getCypher()));
        assertEquals(true, in.readBoolean());
        assertEquals(false, in.readBoolean());
        assertEquals(5 + 2 + 8 + 4 + 4, in.available());
        assertEquals(1234567.0f, in.readFloat());
        assertEquals(5 + 2 + 8 + 4, in.available());
        assertEquals(01010101010, in.readInt());
        assertEquals(5 + 2 + 8, in.available());
        assertEquals(99999999999999999L, in.readLong());
        assertEquals(5 + 2, in.available());
        assertEquals("Hello", in.readUTF());
        in.close();
    }
    /*
    public void testAllFiles() throws Exception {
        String [] files = new File("resources").list();
        for (String f: files) {
            ByteArrayOutputStream file = loadFile("resources/" + f);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            OutputStream out = new EncryptionOutputStream(bytes, getCypher());
            out.write(file.toByteArray());
            out.close();
            
            byte [] byteArray = bytes.toByteArray();
            System.out.println("Encoded file length: " + byteArray.length);

            ByteArrayOutputStream decoded = new ByteArrayOutputStream();
            ByteArrayInputStream inBytes = new ByteArrayInputStream(byteArray);
            InputStream in = new EncryptionInputStream(inBytes, getCypher());
            doRead(in, decoded);
            assertEquals(file.size(), decoded.size());
            int len = Math.min(file.size(), decoded.size());
            byte [] arr1 = file.toByteArray();
            byte [] arr2 = decoded.toByteArray();
            for (int i=0; i<len; i++) {
                assertEquals("At position '" + i + "'", arr1[i], arr2[i]);
            }
        }
    }
    */
    private void doRead(InputStream in, OutputStream out) throws IOException, InterruptedException {
        byte [] buffer = new byte[1024];
        while (true) {
            int num = in.read(buffer);
            if (num < 0)
                break;
            out.write(buffer, 0, num);
        }
        in.close();
        out.flush();
        out.close();
        Thread.sleep(100);
    }
    
    void doTestTextFile(String file) throws Exception {
        processTextFile(file);
    }
    
    protected void processTextFile(String file) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStream out = new EncryptionOutputStream(bytes, getCypher());
        out.write(file.getBytes());
        out.close();
        
        byte [] byteArray = bytes.toByteArray();
        System.out.println("Encoded file length: " + byteArray.length);

        ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(byteArray);
        InputStream in = new EncryptionInputStream(inBytes, getCypher());
        doRead(in, decoded);
        assertEquals(file.length(), decoded.size());
        int len = Math.min(file.length(), decoded.size());
        byte [] arr1 = file.getBytes();
        byte [] arr2 = decoded.toByteArray();
        for (int i=0; i<len; i++) {
            assertEquals("At position '" + i + "'", arr1[i], arr2[i]);
        }
    }

    public void testBinaryFile() throws Exception {


        ByteArrayOutputStream file = loadFile("signed_forms.pdf");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStream out = new EncryptionOutputStream(bytes, getCypher());
        out.write(file.toByteArray());
        out.close();
        
        byte [] byteArray = bytes.toByteArray();
        System.out.println("Encoded file length: " + byteArray.length);
        
        ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(byteArray);
        InputStream in = new EncryptionInputStream(inBytes, getCypher());
        doRead(in, decoded);
        assertEquals(file.size(), decoded.size());
        int len = Math.min(file.size(), decoded.size());
        byte [] arr1 = file.toByteArray();
        byte [] arr2 = decoded.toByteArray();
        for (int i=0; i<len; i++) {
            assertEquals("At position '" + i + "'", arr1[i], arr2[i]);
        }
    }
    

}
