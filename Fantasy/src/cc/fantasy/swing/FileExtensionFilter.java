package cc.fantasy.swing;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class FileExtensionFilter extends FileFilter {

	String ext;
	
	FileExtensionFilter(String ext) {
		this.ext = ext.toLowerCase();
	}
	
	@Override
	public String getDescription() {
		return "XLS Tab Delimited Spread Sheets";
	}

	public boolean accept(File file) {
		return file.isDirectory() || file.getName().toLowerCase().endsWith(ext);
	}

	
}
