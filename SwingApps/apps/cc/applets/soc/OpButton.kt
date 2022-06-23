package cc.applets.soc;

import javax.swing.JButton;

import cc.game.soc.ui.MenuItem;

@SuppressWarnings("serial")
public class OpButton extends JButton {

	public final Object extra;
	public final MenuItem item;
	
	OpButton(MenuItem op, String text, Object extra) {
		super(text);
		this.item = op;
		this.extra = extra == null ? this : extra;
		this.setToolTipText(op.helpText);
	}
	
	public Object getExtra() {
		return extra;
	}
	
}
