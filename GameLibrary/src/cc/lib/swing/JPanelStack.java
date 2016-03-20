package cc.lib.swing;

import java.awt.BorderLayout;
import java.awt.LayoutManager;
import java.util.Stack;

import javax.swing.JPanel;

public class JPanelStack extends JPanel {

	private final Stack<JPanel> stack = new Stack<JPanel>();
	
	public JPanelStack() {
		super(new BorderLayout());
	}
	
	public JPanel push() {
		if (stack.size() > 0) {
			remove(stack.peek());
		}
		JPanel panel = new JPanel(new BorderLayout());
		stack.add(panel);
		add(panel);
		invalidate();
		return panel;
	}
	
	public JPanel top() {
		return stack.peek();
	}
	
	public void pop() {
		if (stack.size() > 0) {
    		remove(stack.pop());
    		if (stack.size() > 0) {
    			add(stack.peek());
    		}
    		invalidate();
		}
	}

	public void removeAll() {
		pop();
		stack.clear();
	}
	
}
