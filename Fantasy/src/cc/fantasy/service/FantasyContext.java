package cc.fantasy.service;

import java.io.InputStream;
import java.util.Properties;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class FantasyContext {

    private static ApplicationContext context;    
    private static Properties config; 
    
    // package access for unit tests
    static ApplicationContext getInstance() {
        
        if( context==null ) {
            context = new ClassPathXmlApplicationContext("fantasy-beans.xml");
            InputStream input = FantasyContext.class.getClassLoader().getResourceAsStream("fantasy.properties"); 
            config = new Properties();
            try {
                config.load(input);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load properties from 'fantasy.properties'", e);
            }
        }

        return context;
    }

    public static IFantasyService getService() {
        return (IFantasyService)getInstance().getBean(config.getProperty("fantasy.dao"));
    }
    
}
