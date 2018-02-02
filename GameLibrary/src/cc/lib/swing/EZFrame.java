package cc.lib.swing;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import javax.swing.JFrame;

public class EZFrame extends JFrame implements WindowListener, ComponentListener {

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
        } catch (Exception e) {
	        e.printStackTrace();
        }

        return false;
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
    public void componentMoved(ComponentEvent arg0) {}

    @Override
    public void componentResized(ComponentEvent arg0) {}

    @Override
    public void componentShown(ComponentEvent arg0) {}
}
