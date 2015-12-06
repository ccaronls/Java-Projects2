package cc.lib.utils;

import java.io.File;

import cc.lib.game.Utils;
import junit.framework.TestCase;

public class TestFileUtils extends TestCase {

	@Override
	public void setUp() {
		Utils.DEBUG_ENABLED = true;
	}
	
	public void testBackupRestoreWithExtension() throws Exception {
		
		File test = new File("testresults/bktest.txt");
		test.getParentFile().mkdir();
		FileUtils.stringToFile("0", test);
		
		for (int i=0; i<10; i++) {
			assertTrue(FileUtils.backupFile("testresults/bktest.txt", 10));
			FileUtils.stringToFile(String.valueOf(i), test);
		}

		for (int i=9; i>=0; i--) {
			String s = FileUtils.fileToString(test);
			assertEquals(Integer.parseInt(s), i);
			assertTrue(FileUtils.restoreFile("testresults/bktest.txt"));
		}
		
		assertTrue(test.delete());
	}

	public void testBackupRestoreNoExtension() throws Exception {
		
		File test = new File("testresults/bktest");
		FileUtils.stringToFile("0", test);
		
		for (int i=0; i<10; i++) {
			assertTrue(FileUtils.backupFile("testresults/bktest", 10));
			FileUtils.stringToFile(String.valueOf(i), test);
		}

		for (int i=9; i>=0; i--) {
			String s = FileUtils.fileToString(test);
			assertEquals(Integer.parseInt(s), i);
			assertTrue(FileUtils.restoreFile("testresults/bktest"));
		}
		
		assertTrue(test.delete());
	}
	
}
