package cc.lib.swing;

import java.awt.*;

import javax.swing.*;

public class EZPanel extends JPanel {

	public EZPanel(LayoutManager lm, Component ... components) {
		super(lm);
		for (Component c : components) {
			add(c);
		}
	}

	public EZPanel(Component ... components) {
		for (Component c : components) {
			add(c);
		}
	}
	
	
}
