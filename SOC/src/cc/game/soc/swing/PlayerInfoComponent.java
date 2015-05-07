package cc.game.soc.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.JComponent;

import cc.game.soc.core.*;
import cc.lib.game.Justify;
import cc.lib.swing.AWTUtils;

@SuppressWarnings("serial")
public abstract class PlayerInfoComponent extends JComponent {

	private GUIPlayer player;
    
    PlayerInfoComponent(GUIPlayer player) {
        super();
        this.player = player;
    }
    
    final void setPlayer(GUIPlayer player) {
        this.player = player;
        repaint();
    }
    
    final GUIPlayer getPlayer() {
    	return player;
    }
	
}
