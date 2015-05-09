package cc.game.soc.swing;

import javax.swing.JComponent;

@SuppressWarnings("serial")
public abstract class PlayerInfoComponent extends JComponent {

	private final int playerNum;
	
    PlayerInfoComponent(int playerNum) {
        super();
        this.playerNum = playerNum;
    }
    
    final GUIPlayer getPlayer() {
    	return GUI.instance.getGUIPlayer(playerNum);
    }
	
}
