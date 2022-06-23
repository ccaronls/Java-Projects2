package cc.applets.soc;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import cc.lib.swing.AWTButton;

/**
 * Created by chriscaron on 2/28/18.
 */

public class PopupButton extends AWTButton implements ActionListener {

    PopupButton(String txt) {
        super(txt, null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        doAction();
    }

    boolean doAction() {
        return true;
    }
}
