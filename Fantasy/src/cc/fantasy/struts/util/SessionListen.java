package cc.fantasy.struts.util;

import java.util.Enumeration;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SessionListen implements HttpSessionListener {

    private Log log = LogFactory.getLog(getClass());

    public void sessionCreated(HttpSessionEvent event) {
        log.debug("sessionCreated");
        Enumeration sessionAttrs    =   event.getSession().getAttributeNames();
         
        while(sessionAttrs.hasMoreElements()){
           log.debug(" Session Element [ " + sessionAttrs.nextElement().toString() + " ] ");
        }
        log.debug(String.valueOf(event.getSession().getCreationTime()));
    }

    public void sessionDestroyed(HttpSessionEvent event) {
        log.info("sessionDestroyed");
    }

    
    
}
