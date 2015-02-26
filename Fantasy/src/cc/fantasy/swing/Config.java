package cc.fantasy.swing;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.log4j.Category;

public class Config {

    private Category log = Category.getInstance(getClass());

    private Properties properties;
    private File file;
    
    Config() {
    	properties = new Properties();
    }
    
    void load(File file) {
        this.file = file;
        if (!file.exists() || !file.canRead())
            return;
        InputStream input = null;
        try {
            input = new FileInputStream(file);
            properties = new Properties();
            properties.load(input);
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            try {
                input.close();
            } catch (Exception e) {}
        }
    }
    
    void save() {
        if (file != null) {
            OutputStream output = null;
            try {
                output = new FileOutputStream(file);
                properties.store(output, "Fantasy Sports Swing UI Config");
            } catch (Exception e) {
                log.error(e.getMessage());
            } finally {
                try {
                    output.close();
                } catch (Exception e) {}
            }
        }
    }

    int getInt(String key, int defVal) {
    	try {
    		return Integer.parseInt(properties.getProperty(key));
    	} catch (Exception e) {
    		properties.setProperty(key, String.valueOf(defVal));
    	}
    	return defVal;
    }

    void setInt(String key, int value) {
    	properties.put(key, String.valueOf(value));
    }
    
    float getFloat(String key, float defVal) {
    	try {
    		return Float.parseFloat(properties.getProperty(key));
    	} catch (Exception e) {
    		properties.setProperty(key, String.valueOf(defVal));
    	}
    	return defVal;
    }

    void setFloat(String key, float value) {
    	properties.put(key, String.valueOf(value));
    }
    
    boolean getBoolean(String key, boolean defVal) {
    	try {
    		return Boolean.parseBoolean(properties.getProperty(key));
    	} catch (Exception e) {
    		properties.setProperty(key, String.valueOf(defVal));
    	}
    	return defVal;
    }
    
    void setBoolean(String key, boolean value) {
    	properties.put(key, String.valueOf(value));
    }
    
    Color getColor(String key, Color defVal) {
    	try {
    		String [] color = properties.getProperty(key).split(",");
    		int r = Integer.parseInt(color[0]);
    		int g = Integer.parseInt(color[1]);
    		int b = Integer.parseInt(color[2]);
    		int a = Integer.parseInt(color[3]);
    		return new Color(r, g, b, a);
    	} catch (Exception e) {
    		properties.setProperty(key, "" + defVal.getRed() + "," + defVal.getGreen() + "," + defVal.getBlue() + "," + defVal.getAlpha());
    	}
    	return defVal;
    }
    
    String getString(String key, String defVal) {
    	if (properties.containsKey(key))
    		return properties.getProperty(key);
    	if (defVal != null)
    		properties.setProperty(key, defVal);
    	return defVal;
    }
    
    void setString(String key, String value) {
        if (value == null)
            properties.remove(key);
        else
            properties.put(key, value);
    }
}
