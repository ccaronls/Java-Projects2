package cc.lib.swing;

import java.awt.event.*;

import javax.swing.*;

public class EZButton extends JButton implements ActionListener {

	public EZButton(String label, ActionListener listener) {
		super(label);
		this.addActionListener(listener);
	}

	public EZButton(String label) {
	    super(label);
        this.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
	    onAction();
    }

    protected void onAction() {

    }
}
