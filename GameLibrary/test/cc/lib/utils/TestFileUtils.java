package cc.lib.utils;

import java.io.File;

import junit.framework.TestCase;

public class TestFileUtils extends TestCase {

	public void testBackupRestore() throws Exception {
		
		File test = new File("bktest");
		
		for (int i=0; i<10; i++) {
			FileUtils.backupFile("bktest", 10);
			FileUtils.stringToFile(String.valueOf(i), test);
		}

		for (int i=9; i>=0; i--) {
			String s = FileUtils.fileToString(test);
			assertEquals(Integer.parseInt(s), i);
			FileUtils.restoreFile("bktest");
		}
		
		assertTrue(test.delete());
	}
	
}
