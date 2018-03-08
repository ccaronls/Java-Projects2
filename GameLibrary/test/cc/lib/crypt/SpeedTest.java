package cc.lib.crypt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import junit.framework.TestCase;

public class SpeedTest extends TestCase {

    public void test() throws Exception {
        
        File bigFile = new File("resources/eswitch_logs_05132012.zip");
        File encFile = new File("/tmp/cyphertest.bin");
        
        HuffmanEncoding cypher = new HuffmanEncoding();
        cypher.importCounts(bigFile);
        cypher.generate();
        
        System.out.println("Speedtest on " + bigFile + " SIZE: " + bigFile.length());
        
        {
            
            long startTime = System.currentTimeMillis();
            FileInputStream in = new FileInputStream(bigFile);
            byte [] buf = new byte[1024];
            while (true) {
                if (in.read(buf) < 0)
                    break;
            }
            in.close();
            long endTime = System.currentTimeMillis();
            long time = endTime - startTime;
            
            System.out.println("Time to read unencrypted file " + bigFile + " : " + time);
        }

        {
            long startTime = System.currentTimeMillis();
            FileInputStream in = new FileInputStream(bigFile);
            EncryptionOutputStream out = new EncryptionOutputStream(new FileOutputStream(encFile), cypher);
            byte [] buf = new byte[1024];
            long byteRead = 0;
            while (true) {
                int n = in.read(buf);
                if (n < 0)
                    break;
                byteRead += n;
                out.write(buf, 0, n);
            }
            in.close();
            out.close();
            long endTime = System.currentTimeMillis();
            long time = endTime - startTime;
            
            System.out.println("Time to encrypted " + bigFile + " : " + time);
            System.out.println("Read " + byteRead + " bytes");
            System.out.println("File size of encrypted file: " + new File(encFile.getAbsolutePath()).length());
        }

        {
            long startTime = System.currentTimeMillis();
            EncryptionInputStream in = new EncryptionInputStream(new FileInputStream(encFile), cypher);
            byte [] buf = new byte[1024];
            while (true) {
                if (in.read(buf) < 0)
                    break;
            }
            in.close();
            long endTime = System.currentTimeMillis();
            long time = endTime - startTime;
            
            System.out.println("Time to read encrypted file: " + time);
        }
        
        
        
        
        
    }
    
}
