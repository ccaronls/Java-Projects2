package cc.lib.swing;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Properties;

import javax.swing.*;
import javax.swing.event.*;

import javax.swing.filechooser.*;

import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

public class EZFrame extends JFrame implements WindowListener, ComponentListener, MenuListener, MenuKeyListener {

	public static final long serialVersionUID = 20002;

	public final Logger log = LoggerFactory.getLogger(getClass());

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

	public void showAsPopup(JFrame parent) {
        setUndecorated(true);
        parent.setEnabled(false);
        setMinimumSize(new Dimension(160,120));
        pack();
        int x = parent.getX() + parent.getWidth()/2 - getWidth()/2;
        int y = parent.getY() + parent.getHeight()/2 - getHeight()/2;
        setLocation(x, y);
        setResizable(false);
        setVisible(true);
        setAlwaysOnTop(true);
    }

    public void closePopup(JFrame parent) {
        synchronized (this) {
            this.notify();
        }
        setVisible(false);
        parent.setEnabled(true);
        parent.setVisible(true);
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

	protected void onWindowResized(int w, int h) {}

	protected void saveToFile() {
	    if (propertiesFile != null) {
            Properties p = new Properties();
            try (InputStream in = new FileInputStream(propertiesFile)) {
                p.load(in);
            } catch (Exception e) {
                e.printStackTrace();
                // oh well!
            }
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

    public synchronized void setProperties(Properties props) {
        try {
            OutputStream out = new FileOutputStream(propertiesFile);
            try {
                props.store(out, "");
            } finally {
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void setProperty(String name, String value) {
	    Properties p = getProperties();
	    p.setProperty(name, value);
	    setProperties(p);
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
                    log.debug("actionPerformed: " + e);
                    ActionEvent ev = (ActionEvent) e;
                    String cmd = ev.getActionCommand();
                    //JMenuItem source = (JMenuItem)ev.getSource();
                    try {
                        onMenuItemSelected(selectedMenu.getText(), cmd);
                    } catch (Exception ee) {
                        ee.printStackTrace();
                    }
                    //log.debug("actionPerformed: cmd=" + cmd + " source=" + source);
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
    public final void componentMoved(ComponentEvent arg0) {
        saveToFile();
    }

    @Override
    public final void componentResized(ComponentEvent arg0) {
        saveToFile();
        onWindowResized(getWidth(), getHeight());
    }

    @Override
    public void componentShown(ComponentEvent arg0) {}

    private JMenu selectedMenu = null;

    @Override
    public final void menuSelected(MenuEvent e) {
	    log.debug("menuSelected: " + e);
	    selectedMenu = (JMenu)e.getSource();
    }

    @Override
    public final void menuDeselected(MenuEvent e) {
        log.debug("menuDeselected: " + e);
    }

    @Override
    public final void menuCanceled(MenuEvent e) {
        log.debug("menuCancelled: " + e);
    }

    @Override
    public final void	menuKeyPressed(MenuKeyEvent e) {
        log.debug("menuKeyPressed: " + e);
    }
    @Override
    public final void	menuKeyReleased(MenuKeyEvent e) {
        log.debug("menuKeyReleased: " + e);
    }
    @Override
    public final void	menuKeyTyped(MenuKeyEvent e) {
        log.debug("menuKeyTyped: " + e);
    }

    protected void onMenuItemSelected(String menu, String subMenu) {
        log.warn("Unhandled onMneuItemSelected: menu=" + menu + " item=" + subMenu);
    }

    public final void add(AWTComponent comp) {
        super.add(comp);
    }

    public final void repaint() {
        super.repaint();
    }

    public final void validate() {
        clearContainersBackgrounds(getContentPane());
        super.validate();
    }

    public final void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        super.setFocusable(enabled);
        super.setFocusableWindowState(enabled);
        if (enabled) {
            setVisible(true);
        }
    }

    public final void setVisible(boolean visible) {
        super.setVisible(visible);
    }

    public void setBackground(GColor c) {
        Container container = getContentPane();
        container.setBackground(AWTUtils.toColor(c));
        clearContainersBackgrounds(container);
    }

    private void clearContainersBackgrounds(Container c) {
        for (Component comp : c.getComponents()) {
            comp.setBackground(null);
            if (comp instanceof Container) {
                clearContainersBackgrounds((Container)comp);
            }
        }
    }

    /**
     * Load properties from file
     *
     * @return
     */
    public Properties getProperties() {
        Properties p = new Properties();
        if (propertiesFile != null) {
            try (InputStream in = new FileInputStream(propertiesFile)) {
                p.load(in);
            } catch (Exception e) {
                e.printStackTrace();
                // oh well!
            }
        }
        return p;
    }

    /**
     *
     * @param name
     * @param defaultValue
     * @return
     */
    public final boolean getBooleanProperty(String name, boolean defaultValue) {
        try {
            String prop = getProperties().getProperty(name);
            if (prop == null)
                return defaultValue;
            return Boolean.parseBoolean(prop);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     *
     * @param name
     * @param defaultValue
     * @return
     */
    public final int getIntProperty(String name, int defaultValue) {
        try {
            return Integer.parseInt(getProperties().getProperty(name));
        } catch (Exception e) {}
        return defaultValue;
    }

    /**
     *
     * @param name
     * @param defaultValue
     * @return
     */
    public final double getDoubleProperty(String name, double defaultValue) {
        try {
            return Double.parseDouble(getProperties().getProperty(name));
        } catch (Exception e) {}
        return defaultValue;
    }

    /**
     *
     * @param name
     * @param defaultValue
     * @return
     */
    public final String getStringProperty(String name, String defaultValue) {
        String value = getProperties().getProperty(name);
        if (value != null)
            return value;
        return defaultValue;
    }

    static FileFilter getExtensionFilter(final String ext, final boolean acceptDirectories) {

        return new FileFilter() {

            public boolean accept(File file) {
                if (file.isDirectory() && acceptDirectories)
                    return true;
                return file.getName().endsWith(ext);
            }

            public String getDescription() {
                return "SOC Board Files";
            }

        };
    }

    /**
     *
     * @return
     */
    public File getWorkingDir() {
        Properties p = getProperties();
        String dir = p.getProperty("workingDir");
        if (dir != null)
            return new File(dir);
        return new File(".");
    }

    /**
     *
     * @param dir
     */
    public void setWorkingDir(File dir) {
        Properties p = getProperties();
        p.setProperty("workingDir", dir.getAbsolutePath());
        setProperties(p);
    }

    /**
     *
     * @param title
     * @param extension
     * @return
     */
    public File showFileOpenChooser(String title, String extension) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(getWorkingDir());
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (extension != null) {
            if (!extension.startsWith("."))
                extension = "." + extension;
            chooser.setFileFilter(getExtensionFilter(extension, true));
        }
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            setWorkingDir(file.getParentFile());
            String fileName = file.getAbsolutePath();
            if (extension != null && !fileName.endsWith(extension))
                fileName += extension;
            return new File(fileName);
        }
        return null;
    }

    /**
     *
     * @param title
     * @param extension
     * @param selectedFile
     * @return
     */
    public File showFileSaveChooser(String title, String extension, File selectedFile) {
        final JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(selectedFile);
        chooser.setCurrentDirectory(getWorkingDir());
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (extension != null) {
            if (!extension.startsWith("."))
                extension = "." + extension;
            chooser.setFileFilter(getExtensionFilter(extension, true));
        }
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            setWorkingDir(file.getParentFile());
            String fileName = file.getName();
            if (extension != null && !fileName.endsWith(extension))
                fileName += extension;
            return new File(file.getParent(), fileName);
        }
        return null;
    }

    public enum MessageIconType {
        PLAIN(JOptionPane.PLAIN_MESSAGE),
        QUESTION(JOptionPane.QUESTION_MESSAGE),
        INFO(JOptionPane.INFORMATION_MESSAGE),
        WARNING(JOptionPane.WARNING_MESSAGE),
        ERROR(JOptionPane.ERROR_MESSAGE);

        MessageIconType(int type) {
            this.type=type;
        }

        final int type;
    };

    /**
     *
     * @param title
     * @param message
     * @param icon
     */
    public void showMessageDialog(String title, String message, MessageIconType icon) {
        JOptionPane.showMessageDialog(this, message, title, icon.type);
    }

    /**
     *
     * @param title
     * @param message
     */
    public void showMessageDialog(String title, String message) {
        showMessageDialog(title, message, MessageIconType.PLAIN);
    }

    /**
     * Show drop down menu with options
     *
     * @param title
     * @param message
     * @param items
     * @return index of the chosen item or -1 if cancelled
     */
    public int showItemChooserDialog(String title, String message, String ... items) {
        return JOptionPane.showOptionDialog(null, message, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, items, items[0]);
    }

    /**
     * Show a dialog with yes/no buttons. Return true if yes was pressed.
     *
     * @param title
     * @param message
     * @return
     */
    public boolean showYesNoDialog(String title, String message) {
        int n = JOptionPane.showConfirmDialog(
                this,
                message,
                title,
                JOptionPane.YES_NO_OPTION);
        return n == JOptionPane.YES_OPTION;
    }

    public interface OnListItemChoosen {
        void itemChoose(int index);
    }

    /**
     * Show chooser with list of items
     *
     * @param title
     * @param items
     * @return
     */
    public void showListChooserDialog(final OnListItemChoosen itemListener, String title, final String ... items) {
        final EZFrame popup = new EZFrame();
        final ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int index = Utils.linearSearch(items, e.getActionCommand());
                if (index >= 0)
                    itemListener.itemChoose(index);
                popup.setVisible(false);
            }
        };
        EZPanel frame = new EZPanel(new BorderLayout());
        EZPanel list = new EZPanel(0, 1);
        for (String item : items) {
            list.add(new EZButton(item, listener));
        }
        if (title != null) {
            frame.add(new EZLabel(title, 1, 20, true), BorderLayout.NORTH);
        }
        frame.add(list, BorderLayout.CENTER);
        frame.add(new EZButton("CANCEL", listener), BorderLayout.SOUTH);
        popup.setContentPane(frame);
        popup.showAsPopup(this);
    }
}
