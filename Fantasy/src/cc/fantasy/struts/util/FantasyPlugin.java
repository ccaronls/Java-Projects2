package cc.fantasy.struts.util;

import java.io.FileInputStream;

import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.PlugIn;
import org.apache.struts.config.ModuleConfig;

import cc.fantasy.service.FantasyContext;

public class FantasyPlugin implements PlugIn {

    private Log log = LogFactory.getLog(getClass());

    public void destroy() {
        // TODO Auto-generated method stub
        
    }

    public void init(ActionServlet actionServlet, ModuleConfig moduleConfig) throws ServletException {
        try {
            
            String fantasyProps = actionServlet.getInitParameter("fantasy-init-file");
            String propfile = actionServlet.getServletContext().getRealPath(fantasyProps);
            
            Config.getInstance().getProperties().load(new FileInputStream(propfile));
            log.info("Loaded fantasy properties from [" + propfile + "]");
            
            FantasyContext.getService();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    
}
