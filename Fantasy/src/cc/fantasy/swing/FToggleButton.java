package cc.fantasy.swing;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FToggleButton extends FButton implements ActionListener {

	boolean on;
	
	FToggleButton(Command command, boolean on) {
		this(command.name(), command, on);
	}

	FToggleButton(String txt, Command command, boolean on) {
		super(txt, command);
		this.on = on;
		addActionListener(this);
	}
	
	@Override
	public Color getBackground() {
		if (on)
			return Color.CYAN;
		// TODO Auto-generated method stub
		return super.getBackground();
	}

	public void actionPerformed(ActionEvent arg0) {
		on = !on;
	}
	
	
	
}
