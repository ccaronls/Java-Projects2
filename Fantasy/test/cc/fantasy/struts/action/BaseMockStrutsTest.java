package cc.fantasy.struts.action;

import java.io.File;

import org.apache.log4j.Category;

import servletunit.struts.MockStrutsTestCase;

public class BaseMockStrutsTest extends MockStrutsTestCase {
    
    Category log = Category.getInstance(getClass());
    
    public void setUp() throws Exception {

        super.setUp();

        File cwd = new File(".");
        
        log.debug("Current working Dir is : " + cwd.getAbsolutePath());
                
        // Needed for eclipse
        //System.setProperty("javax.xml.parsers.SAXParserFactory", "org.apache.xerces.jaxp.SAXParserFactoryImpl");
        
        //log.debug("Setting config file");
        this.setContextDirectory(new File("webapp"));
        setServletConfigFile("WEB-INF/web.xml");
        
        //log.debug("Setting struts config file");
        setConfigFile("WEB-INF/struts-config.xml");
        
        //log.debug(getActionServlet());
    }
    
}
