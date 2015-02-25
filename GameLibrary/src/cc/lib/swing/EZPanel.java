package cc.lib.swing;

import java.awt.LayoutManager;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class EZPanel extends JPanel {

	public EZPanel(LayoutManager lm, JComponent ... components) {
		super(lm);
		for (JComponent c : components) {
			add(c);
		}
	}

	public EZPanel(JComponent ... components) {
		for (JComponent c : components) {
			add(c);
		}
	}
	
	
}
