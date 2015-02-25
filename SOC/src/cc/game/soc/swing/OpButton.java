package cc.game.soc.swing;

import javax.swing.JButton;

@SuppressWarnings("serial")
public class OpButton extends JButton {

	final Object extra;
	
	OpButton(MenuOp op, String text, Object extra) {
		super(text);
		this.setActionCommand(op.name());
		this.extra = extra;
	}
	
}
