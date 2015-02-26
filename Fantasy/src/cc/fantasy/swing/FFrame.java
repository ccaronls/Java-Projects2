package cc.fantasy.swing;

import java.awt.*;
import java.awt.event.*;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cc.fantasy.model.User;

public class FFrame extends JFrame implements WindowListener{

	
	private Container content = null;
	private JPanel footer = null;

    private boolean isPopup = false;
    private int stackIndex = -1;

    private static FFrame [] stack = new FFrame[32];
    static int numFrames = 0;
    
    private boolean finalized = false;
    
    static FFrame getTopFrame() {
        return stack[numFrames-1];
    }
    
	public FFrame() {
        if (numFrames>0)
            stack[numFrames-1].setEnabled(false);
        stackIndex = numFrames;
		stack[numFrames++] = this;
        this.addWindowListener(this);
		footer = new JPanel();
		footer.setLayout(new FlowLayout());
		super.add(footer, BorderLayout.SOUTH);
		content = new JPanel();
        content.setLayout(new BorderLayout());
        super.add(content);
	}
    
    void setPopup() {
        isPopup = true;
    }
	
	void addLogout() {
		JPanel header = new JPanel();
		header.setLayout(new FlowLayout());
		User user = Fantasy.instance.user;
		header.add(new JLabel("user: " + user.getUserName()));
		header.add(new FButton(Command.LOGOUT));
		super.add(header, BorderLayout.NORTH);
	}
	
	void addSideBar() {
		User user = Fantasy.instance.user;
		FGridPanel menu = new FGridPanel(1);
		super.add(menu, BorderLayout.WEST);
		if (user.hasAdminAccess()) {
			menu.add(new FLabel("ADMIN").setHeader2());
			menu.add(new FButton(Command.VIEW_FRANCHISES));
			menu.add(new FButton(Command.VIEW_LEAGUES));
			menu.add(new FButton(Command.VIEW_TEAMS));
			menu.add(new FButton(Command.VIEW_USERS));
		}
		if (user.hasLeagueAccess()) {
			menu.add(new FLabel("LEAGUE").setHeader2());
			menu.add(new FButton(Command.VIEW_USER_LEAGUES));
		}
		if (user.hasTeamAccess()) {
			menu.add(new FLabel("TEAM").setHeader2());
			menu.add(new FButton(Command.VIEW_USER_TEAMS));
		}
	}
	
	@Override
	public Component add(Component arg0, int arg1) {
        throw new RuntimeException("Not supported");
	}

	@Override
	public void add(Component arg0, Object arg1, int arg2) {
        throw new RuntimeException("Not supported");
	}

	@Override
	public void add(Component arg0, Object arg1) {
		if (content == null) {
			super.add(arg0, arg1);
		} else {
			content.add(arg0, arg1);
		}
	}

	@Override
	public Component add(Component arg0) {
		if (content == null) {
			return super.add(arg0);
		} else {
			return content.add(arg0);
		}
	}

	public void setLayout(LayoutManager arg0) {
		// TODO Auto-generated method stub
		if (content == null) {
			super.setLayout(arg0);
        } else {
			content.setLayout(arg0);
		}        
	}
    
    public void addHeader(String label) {
        if (content == null)
            add(new FLabel(label).setHeader1(), BorderLayout.NORTH);
        else
            content.add(new FLabel(label).setHeader1(), BorderLayout.NORTH);
    }
	
	public void addFooter(FButton button) {
        button.setFrame(this);
		footer.add(button);
	}

	public void close() {
		setVisible(false);
        for (int i=stackIndex; i<numFrames-1; i++) {
            stack[i] = stack[i+1];
            stack[i].stackIndex = i;
        }
        numFrames -= 1;
        if (numFrames>0) {
            stack[numFrames-1].setEnabled(true);
        }
	}
	
	void clearContent() {
		content.removeAll();
        footer.removeAll();
        footer.repaint();
	}
	
	public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        super.setFocusable(enabled);
        super.setVisible(true);
    }

    public void finalizeFrame() {
        if (!finalized) {
            finalized = true;
            Dimension size = getSize();
            pack();
            Dimension newSize = getSize();     
            int maxWidth = Fantasy.instance.config.getInt("window.w", 400);
            int maxHeight = Fantasy.instance.config.getInt("window.h", 400);
            if (newSize.width > maxWidth)
                newSize.width = maxWidth;
            else if (newSize.width < size.width)
                newSize.width = size.width;
            
            if (newSize.height > maxHeight)
                newSize.height = maxHeight;
            else if (newSize.height < size.height)
                newSize.height = size.height;
            setSize(newSize);
            setVisible(true);
        }
    }
    
    public void center(FFrame parent) {
    	Rectangle rect = null;
    	if (parent == null) {
        	Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    		rect = new Rectangle(0,0,d.width,d.height);
    	} else {
    		rect = parent.getBounds();
    	}
    	Rectangle r = getBounds();
    	int x0 = rect.x + rect.width/2 - r.width/2;
    	int y0 = rect.y + rect.height/2 - r.height/2;
    	setLocation(x0, y0);
    }
    
	public void windowActivated(WindowEvent arg0) {}
	public void windowClosed(WindowEvent arg0) {}
	public void windowClosing(WindowEvent arg0) { 
        if (!isPopup) {
            Fantasy.instance.exit(); 
        } else {
        	close();
        }
	}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}

	
    
}
