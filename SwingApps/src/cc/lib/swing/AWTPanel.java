package cc.lib.swing;

import java.awt.*;

import javax.swing.*;

public class AWTPanel extends JPanel {

    public AWTPanel(int rows, int cols, Component ... components) {
        this(new GridLayout(rows, cols), components);
    }

	public AWTPanel(LayoutManager lm, Component ... components) {
		super(lm);
		for (Component c : components) {
			add(c);
		}
	}

	public AWTPanel(Component ... components) {
		for (Component c : components) {
			add(c);
		}
	}
	
	public void addTop(Component comp) {
        add(comp, BorderLayout.NORTH);
    }
    public void addBottom(Component comp) {
        add(comp, BorderLayout.SOUTH);
    }
    public void addLeft(Component comp) {
        add(comp, BorderLayout.WEST);
    }
    public void addRight(Component comp) {
        add(comp, BorderLayout.EAST);
    }
}
