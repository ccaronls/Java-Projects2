package cc.game.soc.swing;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class Helper {

	static FileFilter getExtensionFilter(final String ext, final boolean acceptDirectories) {
		
		return new FileFilter() {

			public boolean accept(File file) {
			    if (file.isDirectory() && acceptDirectories)
			        return true;
				return file.getName().endsWith(ext);
			}

			public String getDescription() {
				return "SOC Board Files";
			}
			
		};		
	}
	
}
