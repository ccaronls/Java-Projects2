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
public class PlayerInfoComponent extends JComponent {

    private Color bkColor;
    
    private GUI gui;
    
    PlayerInfoComponent(GUI gui) {
        this.gui = gui;
        bkColor = GUI.instance.getProps().getColorProperty("playerinfo.bkcolor", Color.BLACK);
    }
    
	/*
	 *  (non-Javadoc)
	 * @see java.awt.Component#paint(java.awt.Graphics)
	 */
	public void paint(Graphics g) {
        Font bold = g.getFont().deriveFont(Font.BOLD);
        g.setFont(bold);
		g.setColor(bkColor);
		g.fillRect(0, 0, getWidth(), getHeight());
		SOC soc = gui.getSOC();
		if (soc != null) {
            int y = getY();
		    for (int i=1; i<=soc.getNumPlayers(); i++) {
		        GUIPlayer p = (GUIPlayer)soc.getPlayerByPlayerNum(i);
                String [] string = getInfo(p);
                g.setColor(p.getColor());
                AWTUtils.drawJustifiedString(g, getX()+5, y, Justify.LEFT, Justify.TOP, string[0]);
                y += AWTUtils.drawJustifiedString(g, getX()+getWidth()/2, y, Justify.LEFT, Justify.TOP, string[1]);
		    }
		    
		    // this way draws the current player first but looks wierd since things are shifting do much
		    /*
    		AIPlayerGUI player = gui.getCurGUIPlayer();    		
    		int y = getY();
    		for (int i=0; player != null && i<gui.getSOC().getNumPlayers(); i++) {
    			String txt = getInfo(player);
    			g.setColor(player.getColor());
    			y += Utils.drawJustifiedString(g, getX(), y, txt);
    			player = (AIPlayerGUI)player.getNext();
    		}*/
		}
	}
	
	
	private String [] getInfo(GUIPlayer player) {
		StringBuffer left = new StringBuffer();
		StringBuffer right = new StringBuffer();
		
		left.append("Player ").append(String.valueOf(player.getPlayerNum())).append("\n");
		right.append(player.getPoints()).append(" Points\n");
		if (player.isInfoVisible()) {
    		for (ResourceType type : ResourceType.values()) {
    			left.append(type).append("\n");
    			right.append(player.getCardCount(type)).append("\n");
    		}
    		for (DevelopmentCardType type: DevelopmentCardType.values()) {
    			int num = player.getUsableCardCount(type);
    			if (num > 0) {
    				left.append(type).append("\n");
    				right.append(num).append("\n");
    			}
    		}
		} else {
		    left.append("Cards").append("\n");
		    right.append(player.getTotalCardsLeftInHand()).append("\n");
		}
		
		left.append("Army").append("\n");
		right.append(player.getArmySize());
		if (player.getPlayerNum() == gui.getSOC().getLargestArmyPlayerNum())
			right.append("+").append(GUI.instance.getSOC().getPointsLargestArmy());
		right.append("\n");
		left.append("Road").append("\n");
		right.append(player.getRoadLength());
		if (player.getPlayerNum() == gui.getSOC().getLongestRoadPlayerNum())
	         right.append("+").append(GUI.instance.getSOC().getPointsLongestRoad());		
		right.append("\n");
		//buf.append("Points: ").append(player.getPoints());
		
		return new String [] { left.toString(), right.toString() };
	}

    @Override
    public Dimension getMinimumSize() {
        int num = gui.getSOC().getNumPlayers();
        int th = 16;
        int h = th*10 + (num-1) * th*5;
        return new Dimension(64, h);
    }
	
	
}
