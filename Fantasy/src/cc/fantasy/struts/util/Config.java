package cc.fantasy.struts.util;

import java.util.Properties;

public class Config {

    private static Config instance = null;
    
    private Properties properties;
    
    static {
        instance = new Config();
    }
    
    public static Config getInstance() {
        return instance;
    }
    
    private Config() {
        properties = new Properties();
    }
    
    public Properties getProperties() {
        return properties;
    }
    
    
}
