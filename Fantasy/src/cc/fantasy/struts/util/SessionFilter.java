package cc.fantasy.struts.util;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cc.fantasy.model.User;

public class SessionFilter implements Filter {

    private Log log = LogFactory.getLog(getClass());
    
    private FilterConfig config;

    public void destroy() {
        // TODO Auto-generated method stub
        
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        
        log.debug("Entering doFilter");
        
        HttpSession session = ((HttpServletRequest)request).getSession(false);

        boolean doChain = true;
        
        if((session != null) && (session.getAttribute("user") != null)) {
        	User user = (User)session.getAttribute("user");
        	if (!user.isActive())
        		doChain = false;
        }else{
            
            String URI = ((HttpServletRequest)request).getRequestURI();

            log.debug("Request URI " + URI);

            if((URI.endsWith(config.getInitParameter("LOGIN_URI"))) 
                    || (URI.endsWith(config.getInitParameter("FORGET_PASSWORD_URI")))
                    || (URI.endsWith(config.getInitParameter("NEW_USER_URI")))
                    || (URI.endsWith(config.getInitParameter("REDIRECT_URI")))
                    || (URI.endsWith(config.getInitParameter("GO_NEW_USER_URI")))        
                    )
            {     
            	doChain = true;
            }else{
            	doChain = false;
            }            
        }
        
        if (doChain)
            chain.doFilter(request, response);
        else {
            ((HttpServletResponse)response).setHeader("LOGIN", "LOGIN_ACTION");
            log.debug("User not present or not active in session.So, Redirecting to login page");
            String redirectUri = config.getInitParameter("REDIRECT_URI");
            request.getRequestDispatcher(redirectUri).forward(request, response);
        }
        
    }

    public void init(FilterConfig config) throws ServletException {
        this.config = config;
    }

    
}
