package cc.game.soc.swing;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

@SuppressWarnings("serial")
public abstract class PlayerInfoComponent extends JComponent {

	private final int playerNum;
	private List<Animation> cardsList = new ArrayList<Animation>();
	
    PlayerInfoComponent(int playerNum) {
        super();
        this.playerNum = playerNum;
    }
    
    final GUIPlayer getPlayer() {
    	return GUI.instance.getGUIPlayer(playerNum);
    }
	
    final List<Animation> getCardAnimations() {
    	return cardsList;
    }
    
    final boolean isCurrentPlayer() {
    	return getPlayer().getPlayerNum() == GUI.instance.getSOC().getCurPlayerNum();	
    }
}
