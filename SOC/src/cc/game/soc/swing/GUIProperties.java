package cc.game.soc.swing;

import java.awt.Color;
import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

import cc.lib.swing.AWTUtils;

@SuppressWarnings("serial")
public class GUIProperties extends Properties {

    private final static Logger log = Logger.getLogger(GUIProperties.class);

    public GUIProperties() {}
    
    private String fileName = null;
    
    public Color getColorProperty(String key, Color defaultValue) {
        try {
            return AWTUtils.stringToColor(getProperty(key, AWTUtils.colorToString(defaultValue)));
        } catch (Exception e) {
        	e.printStackTrace();
            log.error(e.getMessage());
        }
        return defaultValue;
    }

    /**
     * 
     * @param key
     * @param mustExist when true, return null when not exist.  Type determined by isFileIfExists
     * @param isFileIfExists indicates when file OR directory.  
     * @param createIfNeccessary create the file O directory as determined by isFileIfExists
     * @return
     *
    public File getFileProperty(String key, boolean mustExist, boolean isFileIfExists, boolean createIfNeccessary) {
        String name = getProperty(key);
        if (name == null)
            return null;
        File file = new File(name);
        if (mustExist && !file.exists())
            return null;
        if (file.exists()) {
            if (isFileIfExists && !file.isFile())
                return null;
            else if (!isFileIfExists && !file.isDirectory())
                return null;
        } 
        if (createIfNeccessary && !file.exists()) {
            if (file.isDirectory() && !file.mkdir())
                return null;
            else
                try {
                    if (file.isFile() && !file.createNewFile())
                        return null;
                } catch (IOException e) {
                    return null;
                }
        }
        return file;
        
    }
    
    /**
     * 
     */
    public void setProperty(String key, File file) {
        String fileName = file.getPath();
        fileName = fileName.replace(File.separatorChar, '/');
        setProperty(key, fileName);
    }
    

    public int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(getProperty(key, String.valueOf(defaultValue)));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return defaultValue;
    }
    
    
    @Override
    public synchronized Object setProperty(String key, String value) {
    	Object r = super.setProperty(key, value);
    	save();
        return r;
    }

    public void setProperty(String key, int value) {
        setProperty(key, String.valueOf(value));
    }
    
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(getProperty(key, String.valueOf(defaultValue)));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return defaultValue;
    }
    
	public void setProperty(String key, boolean selected) {
		setProperty(key, selected ? "true" : "false");
	}
    
    public float getFloatProperty(String key, float defaultValue) {
        try {
            return Float.parseFloat(getProperty(key, String.valueOf(defaultValue)));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return defaultValue;
    }
    
    @Override
    public String getProperty(String key, String defaultValue) {
        if (!containsKey(key)) {
            if (defaultValue == null)
                return null;
            put(key, defaultValue);
            save();
        }
        return getProperty(key);
    }
    
    public List<String> getListProperty(String key) {
        if (!containsKey(key)) {
            return Collections.emptyList();
        }
        String [] items = getProperty(key).split(",");
        ArrayList<String> list = new ArrayList<String>();
        for (int i=0; i<items.length; i++) {
            list.add(items[i].trim());
        }
        return list;
    }
    
    public void addListItem(String key, String value) {
        if (!containsKey(key)) {
            setProperty(key, value); 
        } else {
            String cur = getProperty(key);
            cur += ",";
            cur += value;
            setProperty(key, cur);
        }
    }

    void load(String fileName) throws IOException {
        this.fileName = fileName;
        log.debug("Loading properties '" + fileName + "'");
        InputStream in = null;
        try {
            in = new FileInputStream(fileName);
            this.load(in);
        } catch (IOException e) {
            log.error(e.getMessage());
        } finally {
            try {
                in.close();
            } catch (Exception e) {}
        }
    }
    
    void save() {
        log.debug("Saving properties '" + fileName + "'");
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(fileName));
            out.println("# Properties file for SOC GUI");
            out.println("# " + new Date());
            String [] keys = keySet().toArray(new String[size()]);
            Arrays.sort(keys);
            for (int i=0; i<keys.length; i++) {
                out.println(keys[i] + "=" + getProperty(keys[i]));
            }
            
            //this.store(out, "Properties file for SOC GUI");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (Exception e) {}
        }
    }

    public void addAll(Properties props) {
    	super.putAll(props);
        save();
    }


    
}
