package cc.chat;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JTextArea;
import javax.swing.JTextField;

import cc.lib.swing.EZFrame;

public abstract class ChatWindow extends EZFrame implements ActionListener {

	private JTextArea area = new JTextArea(20,30);
	private JTextField field = new JTextField(30);
	
	public ChatWindow(boolean left) {
		add(area);
		area.setLineWrap(true);
		area.setEditable(false);
		area.setWrapStyleWord(true);
		add(field, BorderLayout.SOUTH);
		field.addActionListener(this);
		this.pack();
		this.centerToScreen();
	}
	
	public void windowActivated(WindowEvent ev) {
		field.grabFocus();
	}
	
	public void actionPerformed(ActionEvent ev) {
		appendText(ev.getActionCommand());
		field.setText("");
		field.grabFocus();
	}
	
	private void appendText(String text) {
		if (area.getLineCount() >= area.getRows()) {
			String oldText = area.getText();
			int endl = oldText.indexOf('\n');
			if (endl>=0) {
				String newText = oldText.substring(endl+1);
				area.setText(newText);
			} else {
				area.setText("");
			}
		}
		area.append(text + '\n');
	}	
}
