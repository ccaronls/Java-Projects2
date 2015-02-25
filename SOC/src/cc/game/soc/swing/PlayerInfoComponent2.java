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
public class PlayerInfoComponent2 extends JComponent {

    GUIPlayer player;
    
    PlayerInfoComponent2(GUIPlayer player) {
        super();
        this.player = player;
    }
    
    void setPlayer(GUIPlayer player) {
        this.player = player;
        repaint();
    }
    
    private void drawCard(Graphics g, Color color, int x, int y, int w, int h, String txt) {
        g.setColor(color);
        y -= h-2;
        h -= 1;
        w -= 1;
        x += 1;
        g.fillRect(x, y, w, h);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, w, h);
        if (txt != null && txt.length() > 0) {
        	String [] lines = AWTUtils.generateWrappedLines(g, txt, w);
        	y -= 2;
        	for (String s : lines) {
        		AWTUtils.drawJustifiedString(g, x+w/2+1, y-2, Justify.CENTER, Justify.TOP, s);
        		y += AWTUtils.getFontHeight(g);
        	}
        }
    }
    
    @Override
    public void paint(Graphics g) {
        if (player == null)
            return;
        Font bold = g.getFont().deriveFont(Font.BOLD);
        g.setFont(bold);
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        
        if (player.getPlayerNum() == GUI.instance.getSOC().getCurPlayerNum()) {
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, getWidth()-1, getHeight()-1);
        }
        
        int y = 0;
        final int cw = AWTUtils.getStringWidth(g, "+W");
        final int ch = cw*4/3;
        int th = g.getFontMetrics().getHeight();
        if (th < ch) {
        	th = ch;
        }
        final int sx = 5;
        
        g.setColor(player.getColor());
        y += th;
        g.drawString("Player " + player.getPlayerNum() + " " + player.getPoints() + " Points", sx, y);
        
    	Color bk1 = Color.LIGHT_GRAY;
    	Color bk2 = Color.gray;
    	Color bk = bk1;
        if (player.isInfoVisible()) {
            for (ResourceType r : ResourceType.values()) {
            	g.setColor(bk);
            	g.fillRect(sx, y+3, getWidth(), ch);
            	bk = bk == bk1 ? bk2 : bk1;
                int cnt = player.getCardCount(r);
                y += th;
                g.setColor(player.getColor());
                g.drawString(r.name(), sx, y);
                int x = getWidth()-5;
                for (int i=0; i<cnt; i++) {
                    drawCard(g, player.getColor(), x-cw, y, cw, ch, "");
                    x -= cw;
                }
            }
            for (DevelopmentCardType d: DevelopmentCardType.values()) {
                int used      = player.getUsedCardCount(d);
                int usable    = player.getUsableCardCount(d);
                int notusable = player.getCardCount(d) - used - usable;

                if (notusable == 0 && usable == 0)
                	continue;
                
            	g.setColor(bk);
            	g.fillRect(sx, y+3, getWidth(), ch);
            	bk = bk == bk1 ? bk2 : bk1;
                y += th;

                g.setColor(player.getColor());
                g.drawString(d.name(), sx, y);
                
                int x = getWidth()-5;
                
                Color c = AWTUtils.setAlpha(player.getColor(), 100);
                for (int i=0; i<notusable; i++) {
                	drawCard(g, c, x-cw, y, cw, ch, "");
                    x -= cw;
                }

                for (int i=0; i<usable; i++) {
                	drawCard(g, player.getColor(), x-cw, y, cw, ch, "");
                    x -= cw;
                }

                /*
                int used      = player.getNumDevelopment(d, DevelopmentCard.USED);
                int notusable = player.getNumDevelopment(d, DevelopmentCard.NOT_USABLE);
                int usable    = player.getNumDevelopment(d, DevelopmentCard.USABLE);
                if (notusable > 0 || usable > 0 || used > 0) {
                	g.setColor(bk);
                	g.fillRect(sx, y+3, getWidth(), ch);
                	bk = bk == bk1 ? bk2 : bk1;
                    y += th;
                    g.setColor(player.getColor());
                    if (d == DevelopmentCardType.Soldier && this.player.getPlayerNum() == GUI.instance.getSOC().getLargestArmyPlayerNum()) {
                        g.drawString(d.name()+ " +" + GUI.instance.getSOC().getRules().getPointsLargestArmy(), sx, y);
                    } else {
                        g.drawString(d.name(), sx, y);
                    }
                    int x = getWidth()-5;
                    for (int i=0; i<used; i++) {
                        String s = "";
                        switch (d) {
                            case Monopoly:
                            case YearOfPlenty:
                            case RoadBuilding:
                                continue;
                            case Victory:
                                s = "+1";                                    
                                break;
                            case Soldier:
                                s = "A";
                                break;
                        }
                        drawCard(g, player.getColor(), x-cw, y, cw, ch, s);
                        x -= cw;
                    }
                    for (int i=0; i<notusable; i++) {
                        drawCard(g, AWTUtils.setAlpha(player.getColor(), 100), x-cw, y, cw, ch, "");
                        x -= cw;
                    }
                    for (int i=0; i<usable; i++) {
                        drawCard(g, player.getColor(), x-cw, y, cw, ch, "");
                        x -= cw;
                    }
                    
                }
                */
            }
        } else {
            {
                y += ch;
                g.setColor(player.getColor());
                g.drawString("Cards", sx, y);
                int num = player.getTotalCardsLeftInHand();
                int x = getWidth()-5;
                for (int i=0; i<num; i++) {
                    drawCard(g, player.getColor(), x-cw, y, cw, ch, "");
                    x -= cw;
                    if (x < getWidth()/3) {
                        y += ch;
                        x = getWidth() - 5;
                    }
                }
            }
            for (DevelopmentCardType d: DevelopmentCardType.values()) {
            	int used      = player.getUsedCardCount(d);
                int usable    = player.getUsableCardCount(d);
                int notusable = player.getCardCount(d) - used - usable;
                
                if (used > 0 || notusable > 0 || usable > 0) {
                    y += ch;
                    g.setColor(player.getColor());
                    if (d == DevelopmentCardType.Soldier && this.player.getPlayerNum() == GUI.instance.getSOC().getLargestArmyPlayerNum()) {
                        g.drawString(d.name()+ " +" + GUI.instance.getSOC().getPointsLargestArmy(), sx, y);
                    } else {
                        g.drawString(d.name(), sx, y);
                    }
                    int x = getWidth()-5;
                    for (int i=0; i<used; i++) {
                        String s = "";
                        switch (d) {
                            case Monopoly:
                            case YearOfPlenty:
                            case RoadBuilding:
                                break;
                            case Victory:
                                s = "+1";
                                break;
                            case Soldier:
                                s = "A";
                                break;
                        }
                        drawCard(g, player.getColor(), x-cw, y, cw, ch, s);
                        x -= cw;
                    }
                }
            }
        }
    	g.setColor(bk);
    	g.fillRect(sx, y+3, getWidth(), ch);
    	bk = bk == bk1 ? bk2 : bk1;
        y += th;
        g.setColor(player.getColor());
        String s = "Route Length " + player.getRoadLength();
        g.drawString(s, sx, y);
        if (GUI.instance.getSOC().getLongestRoadPlayerNum() == player.getPlayerNum()) {
            drawCard(g, player.getColor(), getWidth()-5-cw, y, cw, ch, "+"+ GUI.instance.getSOC().getPointsLongestRoad());
        }
        if (player.getArmySize() > 0) {
        	g.setColor(bk);
        	g.fillRect(sx, y+3, getWidth(), ch);
        	bk = bk == bk1 ? bk2 : bk1;
            y += th;
            g.setColor(player.getColor());
            g.drawString("Army X " + player.getArmySize(), sx, y);
            if (GUI.instance.getSOC().getLargestArmyPlayerNum() == player.getPlayerNum()) {
            	AWTUtils.drawJustifiedString(g, getWidth(), y, Justify.RIGHT, Justify.BOTTOM, "+" +GUI.instance.getSOC().getPointsLargestArmy());
            }
        }
    }


    @Override
    public Dimension getPreferredSize() {
        // TODO Auto-generated method stub
        return super.getPreferredSize();
    }
    
    

    
}
