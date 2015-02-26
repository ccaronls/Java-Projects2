package cc.fantasy.struts.action;

import javax.servlet.http.HttpSession;

import cc.fantasy.model.User;

public class UserActionTest extends BaseMockStrutsTest {

    public void setUp() throws Exception {
        super.setUp();
    }
    
    public void testLogin() {
        
        log.debug("test login");
        setRequestPathInfo("/login");
        
        addRequestParameter("userName", "ccaron");
        addRequestParameter("passWord", "1234");
        
        actionPerform();
        
        verifyNoActionErrors();
        
        HttpSession session = request.getSession(true);
        User user = (User)session.getAttribute("user");
        
        assertNotNull(user);
        assertEquals(user.getUserName(), "ccaron");
        assertEquals(user.getPassWord(), "1234");
        
    }
    
}
