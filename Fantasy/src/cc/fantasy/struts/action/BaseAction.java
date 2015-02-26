package cc.fantasy.struts.action;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.actions.MappingDispatchAction;

import com.thoughtworks.xstream.XStream;

import cc.fantasy.exception.FantasyException;
import cc.fantasy.model.User;
import cc.fantasy.service.FantasyContext;
import cc.fantasy.service.IFantasyService;
import cc.fantasy.service.Search;
import cc.fantasy.struts.util.Common;

public class BaseAction extends MappingDispatchAction {

    Log log = LogFactory.getLog(getClass());
    
    protected final static String SUCCESS = "success";
    protected final static String FAILED  = "failed";
    
    protected IFantasyService getService() {
        return FantasyContext.getService();
    }
    
    protected User getUser(HttpServletRequest request) {
        return (User)request.getSession(false).getAttribute("user");
    }
    
    protected void setErrorMessage(HttpServletRequest request, Exception e) {
        ActionMessages messages = new ActionMessages();
        String msg = e.getMessage();
        if (e instanceof NullPointerException) {
            msg = "NullPointerException at :" + e.getStackTrace()[0];
        } else if (e instanceof IndexOutOfBoundsException) {
            msg = "IndexOutOfBoundsException index [" + e.getMessage() + "] at :" + e.getStackTrace()[0];
        } else if (e instanceof FantasyException) {
            FantasyException f = (FantasyException)e;
            switch (f.getErrorCode()) {
            case USER_ACCOUNT_NOT_ACTIVE:
                msg = getResources(request).getMessage("errors.user.notactive", f.getParameter());
                break;
            case PASSWORD_MISMATCH:
                msg = getResources(request).getMessage("errors.password.mismatch");
                break;
            case EMPTY_FIELD:
                msg = getResources(request).getMessage("errors.emptyfield", f.getParameter());
                break;
            //case INVALID_DIRECTORY:
            //case CANNOT_CREATE_DIRECTORY:
            //case USER_ID_NOT_FOUND:
            case USERNAME_FIELD_MIN_CHARS_NOT_MET:
                msg = getResources(request).getMessage("errors.minlength", "username", f.getParameter());
                break;
            case PASSWORD_FIELD_MIN_CHARS_NOT_MET:
                msg = getResources(request).getMessage("errors.minlength", "password", f.getParameter());
                break;
            case USERNAME_ALREADY_IN_USE:
                msg = getResources(request).getMessage("errors.username.taken", f.getParameter());
                break;
            case PASSWORD_NOT_VALID_FOR_USER:
                msg = getResources(request).getMessage("errors.password.invalid", f.getParameter());
                break;
            case USERNAME_NOT_FOUND:
                msg = getResources(request).getMessage("errors.username.invalid", f.getParameter());
                break;
            //case FRANCHISE_NOT_FOUND:             
            //case FRANCHISE_ID_NOT_FOUND:
            //case LEAGUE_ID_NOT_FOUND:
            //case TEAM_NOT_FOUND:
                
            default: 
                msg = e.getMessage(); 
                break;

            }
        }
        messages.add("error",new ActionMessage("errors.detail",msg));
        saveMessages(request, messages);
    }
    
    protected void dumpRequest(HttpServletRequest request) {
        
        StringBuffer sb = new StringBuffer("HTTP request:\n");
        sb.append("  URL: " + request.getRequestURL() + "\n");
        sb.append("  Servlet Path: " + request.getServletPath() + "\n");
        sb.append("  Path Info: " + request.getPathInfo() + "\n");
        sb.append("  Content length: " + request.getContentLength() + "\n");
        sb.append("  Content type: " +  request.getContentType() + "\n");
        sb.append("  Server name: " +  request.getServerName() + "\n");
        sb.append("  Server port: " +  request.getServerPort() + "\n");
        sb.append("  Remote address: " +  request.getRemoteAddr() + "\n");

        log.debug(sb);
        
        Enumeration e = request.getAttributeNames();
        while (e.hasMoreElements()) {
            String key = (String)e.nextElement();
            Object value = request.getAttribute(key);
            log.debug("Attrib: key='" + key + "' = '" + value + "'");
        }
        
        e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String key = (String)e.nextElement();
            Object value = request.getParameter(key);
            log.debug("Param: key='" + key + "' = '" + value + "'");
        }
        
    }
    
    protected void dumpObjectMembers(Object obj) {
        Field [] fields = obj.getClass().getDeclaredFields();
        for (int i=0; i<fields.length; i++) {
            try {
                fields[i].setAccessible(true);
                Object o = fields[i].get(obj);
                if (o instanceof List) {
                    log.debug(fields[i].getName() + "=" + (o == null ? "null" : ((List)o).size()));
                } else {
                    log.debug(fields[i].getName() + "=" + o);
                }
            } catch (Exception e) {
                
            }
        }
    }

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		
        if (log.isDebugEnabled()) {
            dumpRequest(request);
            dumpObjectMembers(form);
        }
        
		if (this.isCancelled(request)) {
			ActionForward forward = mapping.findForward("cancel");
			if (forward != null)
				return forward;
		}
		return super.execute(mapping, form, request, response);
	}
    
    protected Search getSearch(HttpServletRequest request, String key) {
        Search search = (Search)request.getSession(false).getAttribute(key);
        if (search == null) {
            search = new Search();
            request.getSession(false).setAttribute(key, search);
        }
        String offsetStr = request.getParameter("offset");
        //String maxStr = request.getParameter("max");
        //String searchKey = request.getParameter("searchKey");
        String descendingStr = request.getParameter("desc");
        
        if (offsetStr != null)
            search.setOffset(Integer.parseInt(offsetStr));
        if (descendingStr != null)
            search.setDescending(Boolean.parseBoolean(descendingStr));
        
        return search;
    }
   
    protected String readInput(HttpServletRequest request) throws IOException {
        InputStream input = request.getInputStream();
        StringBuffer buf = new StringBuffer();
        byte [] buffer = new byte[1024];
        while (true) {
            int num = input.read(buffer);
            if (num < 0)
                break;
            buf.append(new String(buffer, 0, num));
        }
        Common.safeClose(input);
        return buf.toString();
    }
    
    protected Object readXml(HttpServletRequest request, XStream stream) throws IOException {
        if (log.isDebugEnabled()) {
            String xml = readInput(request);
            log.debug("Incoming XML:\n" + xml);
            return stream.fromXML(xml);
        }
        InputStream input = request.getInputStream();
        try {
            return stream.fromXML(request.getInputStream());
        } finally {
            Common.safeClose(input);
        }
    }
}
