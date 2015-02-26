package cc.fantasy.struts.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import cc.fantasy.exception.ErrorCode;
import cc.fantasy.exception.FantasyException;
import cc.fantasy.model.User;
import cc.fantasy.model.type.UserAccess;
import cc.fantasy.struts.form.*;
import cc.fantasy.struts.util.Common;

public class UserAction extends BaseAction {

    public ActionForward homeAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        User user = getUser(request);
        if (user == null)
        	return mapping.findForward(FAILED);
    	
    	try {
            UserAccess access = user.getAccess();
            switch (access)
            {
            case ADMIN:
                return mapping.findForward("admin");
            case LEAGUE:
                return mapping.findForward("league");
            case TEAM:
                return mapping.findForward("team");
            }
        } catch (Exception e) {            
            log.error(e);
            setErrorMessage(request, e);           
        }
        return mapping.findForward(FAILED);
    }

    
    public ActionForward redirectAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {

        UserForm userForm = (UserForm)form;
        
        int numUsers = getService().getUserCount();        
        userForm.setNumUsers(numUsers);
        if (numUsers == 0)
            return mapping.findForward("newuser");
        return mapping.findForward("login");        
    }
    
    
    public ActionForward loginAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        
        UserForm userForm = (UserForm)form;
        
        try {
            User user = getService().getUser(userForm.getUserName(), userForm.getPassWord());
            if (!user.isActive())
            	throw new FantasyException(ErrorCode.USER_ACCOUNT_NOT_ACTIVE, user.getUserName());
            
            request.getSession(false).setAttribute("user", user);
            
        } catch (FantasyException e) {
            log.error(e.getMessage());
            setErrorMessage(request, e);           
            return mapping.findForward(FAILED);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            setErrorMessage(request, e);           
            return mapping.findForward(FAILED);
        }
        
        return mapping.findForward(SUCCESS);
    }
    
    public ActionForward logoutAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        
        HttpSession session = request.getSession(false);
        session.removeAttribute("user");      
        session.invalidate();
        // clear the user form
        UserForm userForm = (UserForm)form;
        userForm.setFirstName(null);
        userForm.setLastName(null);
        userForm.setUserName(null);
        userForm.setPassWord(null);
        userForm.setPassWordConfirm(null);
        userForm.setEmail(null);
        
        return mapping.findForward(SUCCESS);
    }
    
    public ActionForward saveUserAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
                
        UserForm userForm = (UserForm)form;
        
        try {
            
            if (!userForm.getPassWord().equals(userForm.getPassWordConfirm()))
                throw new Exception("Password and Password confirm do not match");
            
            User user = null;
            if (userForm.getUserId() < 0)
            	user = new User();
            else
            	user = getService().getUser(userForm.getUserId());
            
            user.setUserName(userForm.getUserName());
            user.setPassWord(userForm.getPassWord());
            user.setFirstName(userForm.getFirstName());
            user.setLastName(userForm.getLastName());
            user.setEmail(userForm.getEmail());
            user.setAccess(UserAccess.valueOf(userForm.getAccess()));
            user.setActive(userForm.getActive());
            
            log.debug("Creating new user : " + user);
            
            getService().addOrUpdateUser(user);
            
            userForm.setNumUsers(getService().getUserCount());
            
            if ( request.getSession().getAttribute("user") == null)
            	request.getSession().setAttribute("user", user);
            
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e);
            setErrorMessage(request, e);           
            
            return mapping.findForward(FAILED);
            
        }
        
        return mapping.findForward(SUCCESS);
    }

    public ActionForward searchUsersAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        UserForm userForm = (UserForm)form;

        userForm.setUsers(getService().getUsers(null));
        
        return mapping.findForward(SUCCESS);
    }

    public ActionForward setUserActiveAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
    	int userId = Integer.parseInt(request.getParameter("userId"));
    	boolean active = Common.parseBoolean(request.getParameter("activate"));
    	
    	User user = getService().getUser(userId);
    	user.setActive(active);
    	getService().addOrUpdateUser(user);
    	
        return mapping.findForward(SUCCESS);
    }

    public ActionForward editUserAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
    	
        UserForm userForm = (UserForm)form;
        
        String forward = null;

        String userIdStr = request.getParameter("userId");
    	if(userIdStr != null) {
    		User user = getService().getUser(Integer.parseInt(userIdStr));
    		userForm.setFirstName(user.getFirstName());
    		userForm.setLastName(user.getLastName());
    		userForm.setAccess(user.getAccess().name());
    		userForm.setActive(user.isActive());
    		userForm.setEmail(user.getEmail());
    		userForm.setPassWord(user.getPassWord());
    		userForm.setPassWordConfirm(user.getPassWord());
    		userForm.setUserName(user.getUserName());
    		userForm.setUserId(user.getId());
    		forward = "edit";
    	} else {
    		userForm.setFirstName(null);
    		userForm.setLastName(null);
    		userForm.setEmail(null);
    		userForm.setPassWord(null);
    		userForm.setPassWordConfirm(null);
    		userForm.setUserName(null);
    		userForm.setAccess(null);
    		userForm.setActive(true);
    		userForm.setUserId(-1);
    		forward = "new";
    	}
    	
        return mapping.findForward(forward);
    }

    public ActionForward newUserAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        UserForm userForm = (UserForm)form;
        int numUsers = getService().getUserCount();        
        userForm.setNumUsers(numUsers);
        return mapping.findForward(SUCCESS);
    }

    /*
    public ActionForward searchTeamsAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return mapping.findForward(SUCCESS);
    }
    public ActionForward searchTeamsAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return mapping.findForward(SUCCESS);
    }
    public ActionForward searchTeamsAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return mapping.findForward(SUCCESS);
    }
    public ActionForward searchTeamsAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return mapping.findForward(SUCCESS);
    }
    public ActionForward searchTeamsAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return mapping.findForward(SUCCESS);
    }
    public ActionForward searchTeamsAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return mapping.findForward(SUCCESS);
    }
    */

}
