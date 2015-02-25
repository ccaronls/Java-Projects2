package cc.game.soc.swing;

@SuppressWarnings("serial")
public class PopupButton extends OpButton {
	
	public PopupButton(String text) {
		super(MenuOp.POPUPBUTTON, text, null);
	}

	public boolean doAction() { return true; }
}
