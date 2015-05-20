package cc.game.soc.swing;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

@SuppressWarnings("serial")
public abstract class PlayerInfoComponent extends JComponent {

	enum CardLoc {
	    CL_NONE,
	    CL_UPPER_LEFT,
	    CL_UPPER_RIGHT,
	    CL_MIDDLE_LEFT,
	    CL_MIDDLE_RIGHT,
	    CL_LOWER_RIGHT
	}

	private final int playerNum;
	private final CardLoc cardLoc;
	private List<Animation> cardsList = new ArrayList<Animation>();
	
    PlayerInfoComponent(int playerNum, CardLoc loc) {
        super();
        this.playerNum = playerNum;
        this.cardLoc = loc;
    }
    
    final GUIPlayer getPlayer() {
    	return GUI.instance.getGUIPlayer(playerNum);
    }
	
    final CardLoc getCardLoc() {
    	return cardLoc;
    }
    
    final List<Animation> getCardAnimations() {
    	return cardsList;
    }
    
    final boolean isCurrentPlayer() {
    	return getPlayer().getPlayerNum() == GUI.instance.getSOC().getCurPlayerNum();	
    }
}
