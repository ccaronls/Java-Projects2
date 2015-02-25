package cc.lib.swing;

import java.awt.event.ActionListener;

import javax.swing.JButton;

public class EZButton extends JButton {

	public EZButton(String label, ActionListener listener) {
		super(label);
		this.addActionListener(listener);
	}
	
}
