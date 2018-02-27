package cc.game.soc.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import cc.lib.game.GColor;

@SuppressWarnings("serial")
public class UIProperties extends Properties {

//    private final static Logger log = Logger.getLogger(UIProperties.class);

    private final static Logger log = LoggerFactory.getLogger(UIProperties.class);

    public UIProperties() {}
    
    private String fileName = null;
    
    public GColor getColorProperty(String key, GColor defaultValue) {
        try {
            return GColor.fromString(getProperty(key, defaultValue.toString()));
        } catch (Exception e) {
        	e.printStackTrace();
            log.error(e.getMessage());
        }
        return defaultValue;
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
