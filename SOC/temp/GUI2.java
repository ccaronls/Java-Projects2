package cc.game.soc.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;

import cc.game.soc.ai.AIEvaluator;
import cc.game.soc.ai.AIFactor;
import cc.game.soc.core.BuildableType;
import cc.game.soc.core.DevelopmentCardType;
import cc.game.soc.core.GiveUpCardOption;
import cc.game.soc.core.MoveType;
import cc.game.soc.core.ResourceType;
import cc.game.soc.core.SOC;
import cc.game.soc.core.SOCBoard;
import cc.game.soc.core.SOCCell;
import cc.game.soc.core.SOCCellType;
import cc.game.soc.core.SOCEdge;
import cc.game.soc.core.SOCPlayer;
import cc.game.soc.core.SOCTrade;
import cc.game.soc.core.SOCVertex;
import cc.game.soc.swing.BoardComponent.PickMode;
import cc.game.soc.swing.BoardComponent.RenderFlag;
import cc.lib.game.Utils;

/**
 * Manager class to handle the menu stack and popups mostly
 * 
 * @author ccaron
 *
 */
public class GUI2 implements IGui, ComponentListener, WindowListener {

    public static void main(String [] args)  {
        JFrame frame = new JFrame();
        try {
            Utils.DEBUG_ENABLED = false;
            GUIProperties.getInstance().load("gui.properties");
            GUI gui = new GUI(frame);
            frame.addWindowListener(gui);
            int w = GUIProperties.getInstance().getIntProperty("gui.w", 640);
            int h = GUIProperties.getInstance().getIntProperty("gui.h", 480);
            frame.setSize(w, h);
            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
            int x,y;
            x = dim.width / 2 - frame.getWidth() / 2;
            y = dim.height / 2 - frame.getHeight() / 2;
            x = GUIProperties.getInstance().getIntProperty("gui.x", x);
            y = GUIProperties.getInstance().getIntProperty("gui.y", y);
            frame.setLocation(x, y);
            frame.setVisible(true);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    final Logger log = Logger.getLogger(GUI.class);
    private Stack<IController> menuStack = new Stack<IController>();
    protected Container frame;
    private JFrame popup;
    private SOCBoard board; 
    
    public GUI2(Container frame) throws IOException {
        this.frame = frame;
        menuStack.push(new NavController(this));
        String defaultBoardFileName = GUIProperties.getInstance().getProperty("gui.defaultBoardFilename", "soc_def_board.txt");
        if (!loadBoard(defaultBoardFileName)) {
            board.generateCells(8);
            menuStack.push(new BoardSetupController());
        }
    }

    public void closePopup() {
        if (popup != null) {
            popup.setVisible(false);
            popup = null;        
        }
        frame.setEnabled(true);
        frame.setVisible(true);
    }
    /*
    void pushMenuState(IController controller) {
        menuStack.push(controller);
        controller.onPush(this);
    }*/
    
    boolean saveBoard(String fileName) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(fileName));
            board.saveBoard(out);
        } catch (IOException e) {
            log.error(e.getMessage());
            return false;
        } finally {
            try {
                out.close();
            } catch (Exception e) {}
        }
        return true;
    }
    
    boolean loadBoard(String fileName) {
        BufferedReader in = null;
        
        try {
            in = new BufferedReader(new FileReader(fileName));
            board.loadBoard(in);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        } finally {
            try {
                in.close();
            } catch (Exception e) {}
        }
    
    }
    
    
    public void showPopup(JFrame pop) {
        if (popup != null)
            popup.setVisible(false);
        popup = pop;
        popup.setUndecorated(true);
        frame.setEnabled(false);
        popup.setMinimumSize(new Dimension(160,120));
        popup.pack();
        //Dimension dim = popup.getSize();
        /*
        if (dim.getWidth() < 100)
            dim.setSize(100, dim.getHeight());
        if (dim.getHeight() < 100)
            dim.setSize(dim.getWidth(), 100);
        popup.setSize(dim);
        */
        int x = frame.getX() + frame.getWidth()/2 - popup.getWidth()/2;
        int y = frame.getY() + frame.getHeight()/2 - popup.getHeight()/2;
        popup.setLocation(x, y);
        popup.setResizable(false);
        popup.setVisible(true);
    }
    
    ActionListener popupButtonListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            if (((PopupButton)e.getSource()).doAction()) {
                closePopup();
            }   
        }        
    };
    
    public void showPopup(String title, JComponent view, PopupButton [] button) {
        JFrame frame = new JFrame();
        frame.setTitle(title);

        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
        container.setLayout(new BorderLayout());
        container.add(new JLabel(title), BorderLayout.NORTH);
        container.add(view, BorderLayout.CENTER);
        Container buttons = new Container();
        container.add(buttons, BorderLayout.SOUTH);
        buttons.setLayout(new GridLayout(1, 0));
        for (int i=0; i<button.length; i++) {
            if (button[i] != null) {
                buttons.add(button[i]);
                button[i].addActionListener(popupButtonListener);
            } else {
                buttons.add(new JLabel());
            }
        }
        frame.setContentPane(container);
        showPopup(frame);
    }
    
    public void showPopup(String name, String msg, 
            PopupButton leftButton,
            PopupButton middleButton,
            PopupButton rightButton) {
        JFrame frame = new JFrame();
        frame.setTitle(name);
        JLabel label = new JWrapLabel(msg);

        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
        container.setLayout(new BorderLayout());
        container.add(new JLabel(name), BorderLayout.NORTH);
        container.add(label, BorderLayout.CENTER);
        Container buttons = new Container();
        container.add(buttons, BorderLayout.SOUTH);
        buttons.setLayout(new GridLayout(1, 0));
        if (leftButton != null) {
            buttons.add(leftButton);
            leftButton.addActionListener(popupButtonListener);
        }
        else
            buttons.add(new JLabel());
        if (middleButton!= null) {
            buttons.add(middleButton);
            middleButton.addActionListener(popupButtonListener);
        }
        else
            buttons.add(new JLabel());
        if (rightButton != null) {
            buttons.add(rightButton);
            rightButton.addActionListener(popupButtonListener);
        }
        else
            buttons.add(new JLabel());
        
        frame.setContentPane(container);
        showPopup(frame);
    }
    
    public void showPopup(String name, String msg, PopupButton leftButton, PopupButton rightButton) {
        showPopup(name, msg, leftButton, null, rightButton);
    }

    public void showPopup(String name, String msg, PopupButton middleButton) {
        showPopup(name, msg, null, middleButton, null);
    }
    
    public void showOkPopup(String name, JComponent view) {
        PopupButton button = new PopupButton("OK");
        showPopup(name, view, new PopupButton[] { null, button, null});
    }
    
    public void showOkPopup(String name, String msg) {
        PopupButton button = new PopupButton("OK");
        showPopup(name, msg, button);
    }

    public void componentHidden(ComponentEvent arg0) {}
    public void componentMoved(ComponentEvent arg0) {
        GUIProperties.getInstance().setProperty("gui.x", frame.getX());
        GUIProperties.getInstance().setProperty("gui.y", frame.getY());
        //log.debug("Moved too : " + frame.getX() + " x " + frame.getY()); 
    }
    public void componentResized(ComponentEvent arg0) {
        GUIProperties.getInstance().setProperty("gui.w", frame.getWidth());
        GUIProperties.getInstance().setProperty("gui.h", frame.getHeight());
        menuStack.peek().onResized(frame.getWidth(), frame.getHeight());
        frame.validate();
        //log.debug("Resized too : " + frame.getWidth() + " x " + frame.getHeight()); 
    }
    public void componentShown(ComponentEvent arg0) {}

    public void windowActivated(WindowEvent arg0) {}
    public void windowClosed(WindowEvent arg0) {
        GUIProperties.getInstance().saveIfDirty();
        System.exit(0); 
    }
    public void windowClosing(WindowEvent arg0) {}
    public void windowDeactivated(WindowEvent arg0) {}
    public void windowDeiconified(WindowEvent arg0) {}
    public void windowIconified(WindowEvent arg0) {}
    public void windowOpened(WindowEvent arg0) {}

    @Override
    public void logDebug(String msg) {
        log.debug(msg);
    }

    @Override
    public void logError(String msg) {
        log.error(msg);
    }

    @Override
    public void logInfo(String string) {
        log.info(string);
    }

}

