package cc.lib.utils;

import java.io.File;
import java.io.IOException;
import java.util.*;

import cc.lib.game.Utils;

/**
 * Watches a file or directory for changes.
 * 
 * @author chriscaron
 *
 */
public abstract class FileWatcher {

	private final File file;
	private boolean running = false;
	private final HashMap<String, Long> tree = new HashMap<String, Long>();
	
	public FileWatcher(File file) throws IOException {
		this.file = file;
		if (!file.exists())
			throw new IOException("File '" + file + "' doe not exist");
		if (file.isDirectory()) {
			File [] files = file.listFiles();
			for (File f : files) {
				tree.put(f.getAbsolutePath(), f.lastModified());
			}
		} else {
			tree.put(file.getAbsolutePath(), file.lastModified());
		}
		startWatching();
	}

	private void startWatching() {
		new Thread() {
			public void run() {
				try {
					running = true;
    				while (running) {
    					if (!file.exists()) {
    						onFileDeleted(file);
    						break;
    					} if (file.isDirectory()) {
    						File [] files = file.listFiles();
    						HashSet<String> deletedFiles = new HashSet<String>();
    						deletedFiles.addAll(tree.keySet());
    						for (File f : files) {
    							if (tree.containsKey(f.getAbsolutePath())) {
    								deletedFiles.remove(f.getAbsolutePath());
    								long lastModified = tree.get(f.getAbsolutePath());
    								if (lastModified < f.lastModified()) {
    									onFileChanged(f);
    									tree.put(f.getAbsolutePath(), f.lastModified());
    								}
    							} else {
    								//new file
    								onFileAdded(f);
    								tree.put(f.getAbsolutePath(), f.lastModified());
    							}
    						}
    						for (String f : deletedFiles) {
    							tree.remove(f);
    							onFileDeleted(new File(f));
    						}
    					} else {
    						if (!tree.containsKey(file.getAbsolutePath())) {
    							onFileDeleted(file);
    							break;
    						}
    						long lastModified = tree.get(file.getAbsolutePath());
    						if (lastModified < file.lastModified()) {
    							onFileChanged(file);
    							tree.put(file.getAbsolutePath(), lastModified);
    						}
    					}
    					synchronized (FileWatcher.this) {
    						FileWatcher.this.wait(500);
    					}
    				}
				} catch (Exception e) {
					e.printStackTrace();
				}
				running = false;
				Utils.println("Watcher stopped");
			}
		}.start();
	}
	
	public void stop() {
		running = false;
		synchronized (this) {
			notify();
		}
	}
	
	public final boolean isWatching() {
		return running;
	}
	
	protected abstract void onFileChanged(File file);
	
	protected abstract void onFileAdded(File file);
	
	protected abstract void onFileDeleted(File file);
}
