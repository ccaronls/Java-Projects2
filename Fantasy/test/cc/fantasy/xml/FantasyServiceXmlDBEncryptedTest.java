package cc.fantasy.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.apache.log4j.Category;

import cc.lib.crypt.Cypher;
import cc.lib.crypt.EncryptionInputStream;
import cc.lib.crypt.EncryptionOutputStream;

public class FantasyServiceXmlDBEncryptedTest extends TestCase {

    Category log = Category.getInstance(getClass());
    
    public void test() throws Exception {
        //encryptTest("testresources/users.xml");
        encryptTest("db/xml/users.xml");
        encryptTest("db/xml/franchise.xml");
    }
    
    public void encryptTest(String file) throws Exception {
        //HuffmanEncoding cypher = new HuffmanEncoding();
        Cypher cypher = FantasyServiceXmlDBEncrypted.getCypher();
        String testXml = fileToString(new File(file));
        //cypher.enableDebugLogging();
        
        long startTime = System.currentTimeMillis();
        
        log.info("Encrypting " + testXml.length() + " bytes");
        // encrypt the xml
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EncryptionOutputStream encryptor = new EncryptionOutputStream(out, cypher);
        encryptor.write(testXml.getBytes());
        encryptor.close();
        float timeTaken = 0.001f * (System.currentTimeMillis() - startTime);
        log.info("Encrypted " + testXml.length() + " in " + timeTaken + " Seconds");
        
        
        byte [] encrypted = out.toByteArray();
        
        startTime = System.currentTimeMillis();
        log.info("Decrypting " + encrypted.length + " bytes ...");
        // now decrypt the xml
        ByteArrayInputStream in = new ByteArrayInputStream(encrypted);
        EncryptionInputStream decryptor = new EncryptionInputStream(in, cypher, true);
        int encAvailable = decryptor.available();
        assertEquals(testXml.length(), encAvailable);
        byte [] decrypted = new byte[encAvailable];
        decryptor.read(decrypted);
        timeTaken = 0.001f * (System.currentTimeMillis() - startTime);
        log.info("Decrypted " + encrypted.length + " bytes in " + timeTaken + " seconds");
        
        String xmlDecrypted = new String(decrypted);
        
        //log.info(xmlDecrypted);
        
        assertEquals(testXml, xmlDecrypted);
    }
    
    private String fileToString(File file) throws IOException {
        InputStream input = new FileInputStream(file);
        StringBuffer buf = new StringBuffer();
        byte [] bytes = new byte[1024];
        while (true) {
            int read = input.read(bytes);
            if (read < 0)
                break;
            buf.append(new String(bytes, 0, read));
        }
        input.close();
        return buf.toString();
    }
}
