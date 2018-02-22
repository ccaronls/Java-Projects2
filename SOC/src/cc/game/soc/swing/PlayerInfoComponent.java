package cc.game.soc.swing;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import cc.lib.game.AAnimation;

@SuppressWarnings("serial")
public abstract class PlayerInfoComponent extends JComponent {

	private final int playerNum;
	private List<AAnimation<Graphics>> cardsList = new ArrayList<>();
	
    PlayerInfoComponent(int playerNum) {
        super();
        this.playerNum = playerNum;
    }
    
    final GUIPlayer getPlayer() {
    	return GUI.instance.getGUIPlayer(playerNum);
    }
	
    final List<AAnimation<Graphics>> getCardAnimations() {
    	return cardsList;
    }
    
    final boolean isCurrentPlayer() {
    	return getPlayer().getPlayerNum() == GUI.instance.getSOC().getCurPlayerNum();	
    }
}
