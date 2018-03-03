package cc.lib.swing;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import javax.swing.*;
import javax.swing.event.*;

import cc.lib.game.Utils;

public class EZFrame extends JFrame implements WindowListener, ComponentListener, MenuListener, MenuKeyListener {

	public static final long serialVersionUID = 20002;

	private File propertiesFile = null;

	public EZFrame() {
		super();
		addWindowListener(this);
		addComponentListener(this);
		//setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
	
	public EZFrame(String label) {
		super(label);
		addWindowListener(this);
        addComponentListener(this);
	}
	
	public EZFrame(String label, int width, int height) {
		super(label);
		this.setSize(width, height);
		addWindowListener(this);
        addComponentListener(this);
	}
	
	public void listScreens() {
	    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	    GraphicsDevice[] gs = ge.getScreenDevices();
	    for (int i=0; i<gs.length; i++) {
	        System.out.println(String.format("Screen %2d %10s %10b", i, gs[i].getIDstring(), gs[i].isFullScreenSupported()));
	    }

	}
	
	public void showFullscreenOnScreen( int screen )
	{
	    GraphicsEnvironment ge = GraphicsEnvironment
	        .getLocalGraphicsEnvironment();
	    GraphicsDevice[] gs = ge.getScreenDevices();
	    if( screen > -1 && screen < gs.length )
	    {
	        gs[screen].setFullScreenWindow( this );
	    }
	    else if( gs.length > 0 )
	    {
	        gs[0].setFullScreenWindow( this );
	    }
	    else
	    {
	        throw new RuntimeException( "No Screens Found" );
	    }
	}
	
	public void showOnScreen( int screen ) {
	    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	    GraphicsDevice[] gd = ge.getScreenDevices();
	    if( screen > -1 && screen < gd.length ) {
	        setLocation(gd[screen].getDefaultConfiguration().getBounds().x, getY());
	    } else if( gd.length > 0 ) {
	        setLocation(gd[0].getDefaultConfiguration().getBounds().x, getY());
	    } else {
	        throw new RuntimeException( "No Screens Found" );
	    }
	}
	
	protected void onWindowClosing() {}

	protected void saveToFile() {
	    if (propertiesFile != null) {
            Properties p = new Properties();
            p.setProperty("gui.x", String.valueOf(getX()));
            p.setProperty("gui.y", String.valueOf(getY()));
            p.setProperty("gui.w", String.valueOf(getWidth()));
            p.setProperty("gui.h", String.valueOf(getHeight()));
            try {
                OutputStream out = new FileOutputStream(propertiesFile);
                try {
                    p.store(out, "");
                } finally {
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean loadFromFile(File propertiesFile) {
	    this.propertiesFile = propertiesFile;
	    try {
            Properties p = new Properties();
            InputStream in = new FileInputStream(propertiesFile);
            try {
                p.load(in);
                int x = Integer.parseInt(p.getProperty("gui.x"));
                int y = Integer.parseInt(p.getProperty("gui.y"));
                int w = Integer.parseInt(p.getProperty("gui.w"));
                int h = Integer.parseInt(p.getProperty("gui.h"));
                setBounds(x, y, w, h);
                this.setVisible(true);
                return true;
            } finally {
                in.close();
            }
        } catch (FileNotFoundException e) {
            System.err.println("File Not Found: " + propertiesFile);
        } catch (Exception e) {
	        e.printStackTrace();
        }

        return false;
    }

    public void addMenuBarMenu(String menuName, String ... menuItems) {
        if (menuItemActionListener == null) {
            menuItemActionListener = new ActionListener() {
                @Override
                public final void actionPerformed(ActionEvent e) {
                    Utils.println("actionPerformed: " + e);
                    ActionEvent ev = (ActionEvent) e;
                    String cmd = ev.getActionCommand();
                    //JMenuItem source = (JMenuItem)ev.getSource();
                    onMenuItemSelected(selectedMenu.getText(), cmd);
                    //Utils.println("actionPerformed: cmd=" + cmd + " source=" + source);
                    //JMenuItem item = (JMenuItem)source;
                }
            };
        }
	    addMenuBarMenu(menuName, menuItemActionListener, menuItems);
    }

    private ActionListener menuItemActionListener = null;

    public void addMenuBarMenu(String menuName, ActionListener listener, String ... menuItems) {
	    JMenuBar bar = getJMenuBar();
	    if (bar == null) {
	        bar = new JMenuBar();
	        setJMenuBar(bar);
        }
        JMenu menu = new JMenu(menuName);
	    bar.add(menu);
	    menu.addMenuListener(this);
	    for (String item : menuItems) {
	        if (item == null)
	            menu.addSeparator();
	        else {
                JMenuItem i = menu.add(item);
                //i.addMenuKeyListener(this);
                i.addActionListener(listener);
            }
        }
    }

	public void windowOpened(WindowEvent ev) {}
	public void windowClosed(WindowEvent ev) {}
	public final void windowClosing(WindowEvent ev) { saveToFile(); onWindowClosing(); System.exit(0); }
	public void windowIconified(WindowEvent ev) {}
	public void windowDeiconified(WindowEvent ev) {}
	public void windowActivated(WindowEvent ev) {}
	public void windowDeactivated(WindowEvent ev) {}

	public void centerToScreen() {
		this.pack();
		finalizeFrame(getWidth(), getHeight(), 1,1,0);
	}
	
	public void centerToScreen(int width, int height) {
		this.setSize(width, height);
		finalizeFrame(getWidth(), getHeight(), 1,1,0);
	}

	public void finalizeToBounds(int x, int y, int w, int h) {
        pack();
        setBounds(x,y,w,h);
        setVisible(true);
    }

	public void finalizeToPosition(int x, int y) {
        pack();
        setBounds(x,y,getWidth(),getHeight());
        setVisible(true);
    }

    public void finalizeToBounds(Rectangle rect) {
        pack();
        setBounds(rect);
        setVisible(true);
    }
	
	/*
	 * 
	 * @param hJust 0==LEFT, 1==CENTER, 2==RIGHT
	 * @param vJust 0==TOP, 1==CENTER, 2==RIGHT
	 * @param padding border padding
	 */
	private void finalizeFrame(int w, int h, int hJust, int vJust, int padding) {
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		//int w = getWidth();
		//int h = getHeight();
		int x,y;
		switch (hJust) {
		case 0: x = padding; break;
		case 1: x = dim.width / 2 - w / 2; break;
		default: x = dim.width - w - padding; break;
		}
		switch (vJust) {
		case 0: y = padding; break;
		case 1 : y = dim.height / 2 - h / 2; break;
		default: y = dim.height - h - padding; break;
		}
		this.setBounds(x,y,w,h);
		this.setVisible(true);
	}
	
    @Override
    public void componentHidden(ComponentEvent arg0) {}

    @Override
    public void componentMoved(ComponentEvent arg0) {
        saveToFile();
    }

    @Override
    public void componentResized(ComponentEvent arg0) {
        saveToFile();
    }

    @Override
    public void componentShown(ComponentEvent arg0) {}

    private JMenu selectedMenu = null;

    @Override
    public final void menuSelected(MenuEvent e) {
	    Utils.println("menuSelected: " + e);
	    selectedMenu = (JMenu)e.getSource();
    }

    @Override
    public final void menuDeselected(MenuEvent e) {
        Utils.println("menuDeselected: " + e);
    }

    @Override
    public final void menuCanceled(MenuEvent e) {
        Utils.println("menuCancelled: " + e);
    }

    @Override
    public final void	menuKeyPressed(MenuKeyEvent e) {
        Utils.println("menuKeyPressed: " + e);
    }
    @Override
    public final void	menuKeyReleased(MenuKeyEvent e) {
        Utils.println("menuKeyReleased: " + e);
    }
    @Override
    public final void	menuKeyTyped(MenuKeyEvent e) {
        Utils.println("menuKeyTyped: " + e);
    }

    protected void onMenuItemSelected(String menu, String subMenu) {
        Utils.println("onMneuItemSelected: menu=" + menu + " item=" + subMenu);
    }

    public final void add(AWTComponent comp) {
        super.add(comp);
    }

    public final void repaint() {
        super.repaint();
    }

    public final void validate() {
        super.validate();
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
    }
}
