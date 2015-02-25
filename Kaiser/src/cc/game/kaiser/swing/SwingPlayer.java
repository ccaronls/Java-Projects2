package cc.game.kaiser.swing;

import cc.game.kaiser.ai.PlayerBot;

public class SwingPlayer extends PlayerBot {

    public SwingPlayer() {}
    
    public SwingPlayer(String name) {
        super(name);
    }

    public boolean isCardsShowing() {
        return false;
    }
    
    
}
