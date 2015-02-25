package cc.game.soc.swing;

import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.apache.log4j.Logger;

import cc.game.soc.core.SOCBoard;
import cc.game.soc.netz.SOCClient;
import cc.game.soc.netz.SOCProtocol;

@SuppressWarnings("serial")
public class GUIClient { //extends GameClient implements SOCClient {

/*
    Logger log = Logger.getLogger("GUIClient");
    
    final GUI gui;

    public GUIClient(GUI gui, String userName, String host) {
        super(userName, SOCGameCommand.VERSION);
        this.gui = gui;
    }
    
    
    
    private class MyJButton extends JButton implements ActionListener {
        FormElem elem;
        ClientForm form;
        MyJButton(ClientForm form, FormElem elem) {
            super(elem.getText());
            this.form = form;
            this.elem = elem;
            addActionListener(this);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            form.doAction(elem);
        }
    }
    
    private class MyJComboBox extends JComboBox {
        FormElem elem;
        ClientForm form;
        MyJComboBox(ClientForm form, FormElem elem) {
            this.form = form;
            this.elem = elem;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            elem.setText(this.getSelectedItem().toString());
        }        
    }
    
    private class MyJToggleButton extends JToggleButton implements ActionListener {
        FormElem elem;
        ClientForm form;
        MyJToggleButton(ClientForm form, FormElem elem) {
            super(elem.getText(), elem.isEnabled());
            this.form = form;
            this.elem = elem;
            addActionListener(this);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            form.doAction(elem);
        }
        
    }
    
    private class MyJTextInput extends JTextField implements ActionListener {

        FormElem elem;
        ClientForm form;
        MyJTextInput(ClientForm form, FormElem elem) {
            this.elem = elem;
            this.form = form;
            addActionListener(this);
            this.setColumns(elem.getMaxChars());
            this.setText(elem.getText());
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            form.doAction(elem);
        }
        
    }
    
    private void buildSwingFormR(ClientForm form, final FormElem e, Container c) {
        switch (e.getType()) {
            case VLISTCONTAINER:
                JPanel panel = new JPanel();
                panel.setLayout(new GridLayout(0, 1));
                c.add(panel);
                for (FormElem child : e.getChildren()) {
                    buildSwingFormR(form, child, panel);
                }
                break;
            case HLISTCONTAINER:
                panel = new JPanel();
                panel.setLayout(new GridLayout(1, 0));
                c.add(panel);
                for (FormElem child : e.getChildren()) {
                    buildSwingFormR(form, child, panel);
                }
                break;
            case SUBMITBUTTON:
                c.add(new MyJButton(form, e));
                break;
            case CHOICEBUTTON:
                JComboBox combo = new MyJComboBox(form, e);
                c.add(combo);
                for (FormElem child : e.getChildren()) {
                    buildSwingFormR(form, child, combo);
                }
                break;
            case BUTTONOPTION:
                ((JComboBox)c).addItem(new Object() {
                    public String toString() { return e.getText(); }
                });
                break;
            case TOGGLEBUTTON:
                c.add(new MyJToggleButton(form, e));
                break;                    
            case TEXTINPUT:
                c.add(new MyJTextInput(form, e));
                break;
            case LABEL:
                c.add(new JLabel(e.getText()));
                break;
            
        }
        
    }

    @Override
    protected void onConnected() {
        gui.onConnected();
    }

    @Override
    protected void onDisconnected(String reason) {
        gui.onConnectionError(reason);
    }

    @Override
    public void onForm(ClientForm form) {
        // TODO Auto-generated method stub
        log.debug("onForm : " + form);
        JFrame frame = new JFrame();
        buildSwingFormR(form, form.getRootElem(), frame.getContentPane());
        gui.showPopup(frame);
    }

    @Override
    public void onNewBoard(SOCBoard board) {
    }

    @Override
    protected void onMessage(String message) {
    }

    @Override
    protected void onCommand(GameCommand cmd) {
        SOCGameCommand.clientProcess(cmd, this);
    }

*/     
}
