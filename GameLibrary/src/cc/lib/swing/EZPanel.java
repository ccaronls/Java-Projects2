package cc.lib.swing;

import java.awt.*;

import javax.swing.*;

public class EZPanel extends JPanel {

    public EZPanel(int rows, int cols, Component ... components) {
        this(new GridLayout(rows, cols), components);
    }

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
