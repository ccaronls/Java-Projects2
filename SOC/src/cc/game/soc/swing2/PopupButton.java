package cc.game.soc.swing2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import cc.lib.swing.EZButton;

/**
 * Created by chriscaron on 2/28/18.
 */

public class PopupButton extends EZButton implements ActionListener {

    PopupButton(String txt) {
        super(txt, null);
        this.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        doAction();
    }

    boolean doAction() {
        return true;
    }
}
