package cc.game.soc.base;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import cc.game.soc.model.User;
import cc.game.soc.model.UserAccess;
import cc.game.soc.service.SOCContext;
import cc.game.soc.service.SOCService;
import junit.framework.TestCase;

public class SOCTestBase extends TestCase implements WindowListener {

    protected SOCService service;

    protected Category log = Logger.getLogger(getClass().getSimpleName());
    
    @Override
    public void tearDown() {
        log.info("---------------------------------------------------------------------------");
        log.info("Test Done " + log.getName() + ":" +this.getName());
        
    }
    
    @Override
    public void setUp() throws Exception {
        super.setUp();

        log.info("Running test " + log.getName() + ":" +this.getName());
        log.info("---------------------------------------------------------------------------");
        
        service = SOCContext.getService();//new SOCServiceXml("testresult/db/xml");

        super.setUp();

        User user = service.getUser("admin");
        if (user == null) {
            user = new User();
            user.setUserName("admin");
            user.setPassWord("admin");
            service.createOrUpdateUser(user);
        }

        user = service.getUser("nopassword");
        if (user == null) {
            user = new User();
            user.setUserName("nopassword");     
            service.createOrUpdateUser(user);
        }

        user = service.getUser("inactive");
        if (user == null) {
            user = new User();
            user.setUserName("inactive");
            user.setAccess(UserAccess.DISABLED);
            service.createOrUpdateUser(user);
        }

        user = service.getUser("basic");
        if (user == null) {
            user = new User();
            user.setUserName("basic");
            user.setAccess(UserAccess.BASIC);
            service.createOrUpdateUser(user);
        }

        user = service.getUser("manager");
        if (user == null) {
            user = new User();
            user.setUserName("manager");
            user.setAccess(UserAccess.MANAGER);
            service.createOrUpdateUser(user);
        }

    }
    
    public void test() {}
    
    protected void safeClose(Socket x) {
        try {
            x.close();
        } catch (Exception e) {}
    }

    protected void safeClose(InputStream x) {
        try {
            x.close();
        } catch (Exception e) {}
    }

    protected void safeClose(OutputStream x) {
        try {
            x.close();
        } catch (Exception e) {}
    }

    protected void safeClose(ServerSocket x) {
        try {
            x.close();
        } catch (Exception e) {}
    }

    /*
    protected void display(Form f) throws Exception {
        JFrame frame = new JFrame();
        frame.setContentPane(f.clBuildContainer(this));
        //frame.pack();
        if (f.getType() == FormType.MENU)
            frame.pack();
        else
            frame.setSize(400,300);
        frame.setVisible(true);
        frame.addWindowListener(this);
        synchronized (this) {
            wait();
        }
    }

    @Override
    public void addCommand(String cmd) {
        System.out.println("AddCommand: " + cmd);
    }
    
    @Override
    public void submitForm(String action, Map<String, String> vars) {
        System.out.println("submitForm: " + action + " vars=" + vars);
    }
*/
    @Override
    public void windowActivated(WindowEvent arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void windowClosed(WindowEvent arg0) {
    }

    @Override
    public void windowClosing(WindowEvent arg0) {
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void windowDeactivated(WindowEvent arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void windowDeiconified(WindowEvent arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void windowIconified(WindowEvent arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void windowOpened(WindowEvent arg0) {
        // TODO Auto-generated method stub
        
    }
    
    

}
