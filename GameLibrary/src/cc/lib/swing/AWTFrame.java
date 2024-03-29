package cc.lib.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileFilter;

import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

public class AWTFrame extends JFrame implements WindowListener, ComponentListener, MenuListener, MenuKeyListener {

	public static final long serialVersionUID = 20002;

	public final Logger log = LoggerFactory.getLogger(getClass());

	private File propertiesFile = null;
	private Properties properties = null;

	public AWTFrame() {
		super();
		addWindowListener(this);
		addComponentListener(this);
		//setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
	
	public AWTFrame(String label) {
		super(label);
		addWindowListener(this);
        addComponentListener(this);
	}
	
	public AWTFrame(String label, int width, int height) {
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
        this.parent = parent;
    }

    private JFrame parent;

    public void closePopup() {
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

	protected synchronized void saveToFile() {
	    if (propertiesFile != null) {
            Properties p = getProperties();
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
        if (propertiesFile != null) {
            try (OutputStream out = new FileOutputStream(propertiesFile)) {
                props.store(out, "");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        properties = props;
    }

    public synchronized void setProperty(String name, Object value) {
	    Properties p = getProperties();
	    if (value == null)
	        p.remove(name);
	    else
	        p.setProperty(name, value.toString());
	    setProperties(p);
    }

    public void setPropertiesFile(File file) {
        this.propertiesFile = file;
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
                properties = p;
            } finally {
                in.close();
            }
        } catch (FileNotFoundException e) {
            System.err.println("File Not Found: " + propertiesFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean loadFromFile(File propertiesFile) {
	    setPropertiesFile(propertiesFile);
	    return restoreFromProperties();
    }

    public boolean restoreFromProperties() {
	    Properties p = getProperties();
        try {
            int x = Integer.parseInt(p.getProperty("gui.x"));
            int y = Integer.parseInt(p.getProperty("gui.y"));
            int w = Integer.parseInt(p.getProperty("gui.w"));
            int h = Integer.parseInt(p.getProperty("gui.h"));
            setBounds(x, y, w, h);
            this.setVisible(true);
            return true;
        } catch (NumberFormatException e) {
        } catch (NullPointerException e) {
            // ignore
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

    public void fullscreenMode() {
        //pack();
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
        pack();
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
        log.warn("Unhandled onMenuItemSelected: menu=" + menu + " item=" + subMenu);
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
    public synchronized Properties getProperties() {
        if (properties != null) {
            return properties;
        }
        Properties p = new Properties();
        if (propertiesFile != null && propertiesFile.isFile()) {
            try (InputStream in = new FileInputStream(propertiesFile)) {
                p.load(in);
            } catch (Exception e) {
                e.printStackTrace();
                // oh well!
            }
        }
        properties = p;
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
        String value = getProperties().getProperty(name);
        try {
            if (value != null)
                return Integer.parseInt(value);
        } catch (Exception e) {
            log.error("Cannot convert value '" + value + "' to integer");
        }
        return defaultValue;
    }

    /**
     *
     * @param name
     * @param defaultValue
     * @return
     */
    public final double getDoubleProperty(String name, double defaultValue) {
        String value = getProperties().getProperty(name);
        try {
            if (value != null)
                return Double.parseDouble(value);
        } catch (Exception e) {
            log.error("Cannot convert value '" + value + "' to double");
        }
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

    public final <T extends Enum<T>> List<T> getEnumListProperty(String propertyName, Class enumClass, List<T> defaultList) {
        String value = getStringProperty(propertyName, null);
        if (value == null)
            return defaultList;
        List<T> list = new ArrayList<>();
        String [] parts = value.split("[,]+");
        for (String s : parts) {
            s = s.trim();
            if (s.isEmpty())
                continue;
            try {
                list.add((T) Enum.valueOf(enumClass, s));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public <T extends Enum<T>> void setEnumListProperty(String property, Collection<T> items) {
        setProperty(property, Utils.trimEnclosure(items.toString()));
    }

    static FileFilter getExtensionFilter(final String ext, final String description, final boolean acceptDirectories) {

        return new FileFilter() {

            public boolean accept(File file) {
                if (file.isDirectory() && acceptDirectories)
                    return true;
                return file.getName().endsWith(ext);
            }

            public String getDescription() {
                return description;
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
    public File showFileOpenChooser(String title, String extension, String description) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(getWorkingDir());
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (extension != null) {
            if (!extension.startsWith("."))
                extension = "." + extension;
            chooser.setFileFilter(getExtensionFilter(extension, description, true));
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
    public File showFileSaveChooser(String title, String extension, String description, File selectedFile) {
        final JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(selectedFile);
        chooser.setCurrentDirectory(getWorkingDir());
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (extension != null) {
            if (!extension.startsWith("."))
                extension = "." + extension;
            chooser.setFileFilter(getExtensionFilter(extension, description, true));
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
        if (message.length() > 32) {
            message = Utils.wrapTextWithNewlines(message, 64);
        }
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

    public void showMessageDialogWithHTMLContent(String titleStr, String html) {
        final AWTFrame dialog = new AWTFrame();
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());
        JTextPane txt = new JTextPane();
        txt.setEditable(false);
        txt.setContentType("text/html");
        txt.setText(html);
        content.add(txt, BorderLayout.CENTER);
        JLabel title = new AWTLabel(titleStr, 1, 16, true);
        content.add(title, BorderLayout.NORTH);
        JButton close = new AWTButton("Close") {
            @Override
            protected void onAction() {
                dialog.closePopup();
            }
        };
        content.add(close, BorderLayout.SOUTH);
        dialog.add(content, BorderLayout.CENTER);
        dialog.showAsPopup(this);
//        dialog.pack();
  //      dialog.setVisible(true);
    }

    /**
     * Show drop down menu with options
     *
     * @param title
     * @param message
     * @param items
     * @return index of the chosen item or -1 if cancelled
     */
    public int showItemChooserDialog(String title, String message, String selectedItem, String ... items) {
        return JOptionPane.showOptionDialog(null, message, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, items, selectedItem == null ? items[0] : selectedItem);
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

        void cancelled();
    }

    /**
     * Show chooser with list of items
     *
     * @param title
     * @param items
     * @return
     */
    public void showListChooserDialog(final OnListItemChoosen itemListener, String title, final String ... items) {
        final AWTFrame popup = new AWTFrame();
        final ActionListener listener = (ActionEvent e) -> {
            int index = Utils.linearSearch(items, e.getActionCommand());
            if (index >= 0)
                itemListener.itemChoose(index);
            else
                itemListener.cancelled();
            popup.closePopup();
        };
        AWTPanel frame = new AWTPanel(new BorderLayout());
        AWTPanel list = new AWTPanel(0, 1);
        for (String item : items) {
            list.add(new AWTButton(item, listener));
        }
        if (title != null) {
            frame.add(new AWTLabel(title, 1, 20, true), BorderLayout.NORTH);
        }
        frame.add(list, BorderLayout.CENTER);
        frame.add(new AWTButton("CANCEL", listener), BorderLayout.SOUTH);
        popup.setContentPane(frame);
        popup.showAsPopup(this);
    }
}
