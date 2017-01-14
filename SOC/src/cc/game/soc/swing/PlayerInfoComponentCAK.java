package cc.game.soc.swing;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

import cc.game.soc.core.*;
import cc.lib.game.Utils;
import cc.lib.swing.AWTUtils;

public class PlayerInfoComponentCAK extends PlayerInfoComponent {

    public PlayerInfoComponentCAK(int playerNum) {
		super(playerNum);
	}

	@Override
    public void paint(Graphics g) {
		GUIPlayer player = getPlayer();
        if (player == null)
            return;
        
        SOC soc = GUI.instance.getSOC();
        Font bold = g.getFont().deriveFont(Font.BOLD);
        g.setFont(bold);
        
    	StringBuffer str = new StringBuffer();
    	str.append(player.getName()).append(" ").append(player.getPoints()).append(" Points Cards ")
    			.append(player.getTotalCardsLeftInHand()).append( "(").append(soc.getRules().getMaxSafeCardsForPlayer(player.getPlayerNum(), soc.getBoard()))
    			.append(")\n");
        if (player.isInfoVisible()) {
        	for (ResourceType t : ResourceType.values()) {
        		int num = player.getCardCount(t);
        		str.append(t.name()).append(" X ").append(num).append("\n");
        	}
        	if (GUI.instance.getRules().isEnableCitiesAndKnightsExpansion()) {
            	for (CommodityType t : CommodityType.values()) {
            		int num = player.getCardCount(t);
            		str.append(t.name()).append(" X ").append(num).append("\n");
            	}
            	for (Card c : player.getCards(CardType.Progress)) {
            		str.append(c.getName()).append("(").append(c.getCardStatus()).append(")\n");
            	}
        	} else {
        		for (Card c : player.getCards(CardType.Development)) {
        			str.append(c.getName()).append("(").append(c.getCardStatus()).append(")\n");
        		}
        	}
        } else {
        	if (GUI.instance.getRules().isEnableCitiesAndKnightsExpansion()) {
            	int num = player.getCardCount(CardType.Resource);
            	num += player.getCardCount(CardType.Commodity);
            	str.append("Materials X ").append(num).append("\n");
            	num = player.getCardCount(CardType.Progress);
            	str.append("Progress X ").append(num).append("\n");
        	} else {
        		int num = player.getUnusedCardCount();
        		str.append("Card X ").append(num).append("\n");
        	}
        }
        
        int numSettlements = soc.getBoard().getNumVertsOfType(player.getPlayerNum(), VertexType.SETTLEMENT);
        int numCities      = soc.getBoard().getNumVertsOfType(player.getPlayerNum(), VertexType.CITY, VertexType.WALLED_CITY);
        int numMetros      = soc.getBoard().getNumVertsOfType(player.getPlayerNum(), VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE);
        int numKnights     = soc.getBoard().getNumKnightsForPlayer(player.getPlayerNum());
        int knightLevel    = soc.getBoard().getKnightLevelForPlayer(player.getPlayerNum(), true, false);
        int maxKnightLevel = soc.getBoard().getKnightLevelForPlayer(player.getPlayerNum(), true, true);
        int numDiscoveredTiles = player.getNumDiscoveredTerritories();
/*
        str.append(" S X ").append(numSettlements).append(" +").append(numSettlements * soc.getRules().getPointsPerSettlement())
           .append(" C X ").append(numCities).append(" +").append(numCities * soc.getRules().getPointsPerCity())
           .append(" KL X ").append(knightLevel).append("/").append(maxKnightLevel);
        
        if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
        	str.append(" M X ").append(numMetros).append(" +").append(numMetros * soc.getRules().getPointsPerMetropolis());
        }
        str.append("\n");
  */      
		int size = player.getArmySize();
		if (size > 0) {
			str.append("Army X ").append(size).append("\n");
		}
		if (numKnights > 0) {
			str.append("Knights ").append(knightLevel).append("/").append(maxKnightLevel).append("\n");
		}
        str.append("Road Length ").append(player.getRoadLength());
        str.append("\n");
        for (SpecialVictoryType sv : SpecialVictoryType.values()) {
        	int num = player.getCardCount(sv);
        	if (num > 0) {
        		str.append(sv.name()).append(Utils.getSignedString(num*sv.points)).append("\n");
        	}
        }
        /*
        for (Card c : player.getCards(CardType.SpecialVictory)) {
        	int pts = (Integer)c.getData();//c.getSpecialVictoryType.values()[c.getTypeOrdinal()].points;
        	str.append(c.getName());
        	if (pts != 0) {
        		str.append(Utils.getSignedString(pts));
        	}
        	str.append("\n");
        }*/
        for (DevelopmentArea d : DevelopmentArea.values()) {
        	if (player.getCityDevelopment(d) > 0) {
        		str.append(d.name()).append(" ").append(d.levelName[player.getCityDevelopment(d)]).append(" (").append(player.getCityDevelopment(d)).append(") ");
        		if (soc.getMetropolisPlayer(d) == player.getPlayerNum()) {
        			str.append(" Metro +").append(soc.getRules().getPointsPerMetropolis());
        		}
        		str.append("\n");
        	}
        }
        if (player.getMerchantFleetTradable() != null) {
        	str.append("Merchant Fleet ").append(player.getMerchantFleetTradable().getName()).append("\n");
        }
        
        {
        	int num = soc.getBoard().getNumDiscoveredIslands(player.getPlayerNum());
        	if (num > 0) {
        		str.append("Discovered Islands X ").append(num).append(" +").append(num * soc.getRules().getPointsIslandDiscovery()).append("\n");
        	}
        }
        
        if (numDiscoveredTiles > 0) {
        	str.append("Discovered Tiles X " + numDiscoveredTiles).append("\n");
        }
        
        g.setColor(player.getColor());
        Rectangle r = AWTUtils.drawWrapString(g, 5, 5, getWidth(), str.toString());
        int h = r.y + r.height + 10;
        int w = getWidth() - 1;
        if (isCurrentPlayer()) {
        	AWTUtils.drawRect(g, 0, 0, getWidth(), getHeight(), 3, 0);
        }
        setPreferredSize(new Dimension(w, h));
    }
	
}
