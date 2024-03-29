package cc.lib.utils;

import junit.framework.TestCase;

import java.io.File;

import cc.lib.game.Utils;

public class FileWatcherTest extends TestCase {

	
	
	@Override
	protected void setUp() throws Exception {
		// TODO Auto-generated method stub
		super.setUp();
	}

	public void testWatchDir() throws Exception {
		Utils.setDebugEnabled();
		File tmpFile = File.createTempFile("tmp", "txt", new File("/tmp"));
		tmpFile.deleteOnExit();
        FileWatcher watcher = new FileWatcher(tmpFile) {

            @Override
            protected void onFileDeleted(File file) {
                Utils.println("File deleted '" + file + "'");
            }

            @Override
            protected void onFileChanged(File file) {
                Utils.println("File changed '" + file + "'");
            }

            @Override
            protected void onFileAdded(File file) {
                Utils.println("File added '" + file + "'");
            }
        };
        Thread.sleep(60000);
        watcher.stop();
	}

	public void testWatchFile() throws Exception {
		Utils.setDebugEnabled();
		FileWatcher watcher = new FileWatcher(new File("/Users/chriscaron/Documents/workspace2/MyStuff/Java/GameLibrary/added")) {
			
			@Override
			protected void onFileDeleted(File file) {
				Utils.println("File deleted '" + file + "'");
			}
			
			@Override
			protected void onFileChanged(File file) {
				Utils.println("File changed '" + file + "'");
				
			}
			
			@Override
			protected void onFileAdded(File file) {
				Utils.println("File added '" + file + "'");
			}
		};
		Thread.sleep(60000);
		watcher.stop();
	}
	
}
