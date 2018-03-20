package cc.game.soc.core;

import junit.framework.TestCase;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import cc.lib.crypt.EncryptionOutputStream;
import cc.lib.crypt.HuffmanEncoding;
import cc.lib.utils.FileUtils;

/**
 * Created by chriscaron on 3/17/18.
 */

public class GenEncryptedAssetsForAndroid extends TestCase {

    public void testGenerate() throws Exception {
        // first read in all the boards and scenarios into a huffman
        HuffmanEncoding cypher = new HuffmanEncoding();

        File scenariosDir = new File("scenarios");
        assertTrue(scenariosDir.isDirectory());

        File boardsDir = new File("boards");
        assertTrue(boardsDir.isDirectory());

        long inSizeBytes = 0;

        for (File f : scenariosDir.listFiles()) {
            cypher.importCounts(f);
            inSizeBytes += f.length();
        }

        for (File f : boardsDir.listFiles()) {
            cypher.importCounts(f);
            inSizeBytes += f.length();
        }

        cypher.printEncodingAsCode(System.out);

        cypher.generate();

        // now encrypt all the files into SOCAndroid assets folder
        File assets = new File("../SOCAndroid/assets/");
        assertTrue(assets.isDirectory());

        File dest = new File(assets, "boards");
        if (!dest.exists()) {
            assertTrue(dest.mkdir());
        } else {
            for (File f : dest.listFiles()) {
                assertTrue(f.delete());
            }
        }

        long outSizeBytes = 0;

        for (File b : boardsDir.listFiles()) {
            File out = new File(dest, FileUtils.stripExtension(b.getName()) + ".encrypt");
            FileUtils.copyFile(b, new EncryptionOutputStream(new BufferedOutputStream(new FileOutputStream(out)), cypher));
            outSizeBytes += out.length();
        }

        dest = new File(assets, "scenarios");
        if (!dest.exists()) {
            assertTrue(dest.mkdir());
        } else {
            for (File f : dest.listFiles()) {
                assertTrue(f.delete());
            }
        }

        for (File b : scenariosDir.listFiles()) {
            File out = new File(dest, FileUtils.stripExtension(b.getName()) + ".encrypt");
            FileUtils.copyFile(b, new EncryptionOutputStream(new BufferedOutputStream(new FileOutputStream(out)), cypher));
            outSizeBytes += out.length();
        }

        System.out.println("In Size Bytes = " + inSizeBytes);
        System.out.println("OutSize Bytes = " + outSizeBytes);
        int sizeChange = 100-(int)(100*outSizeBytes/inSizeBytes);
        System.out.println("Size Change   = " + sizeChange + "%");

        // confirm these all load

    }

}
