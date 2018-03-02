package cc.game.soc.ui;

import cc.game.soc.core.Card;
import cc.game.soc.core.CardType;
import cc.game.soc.core.CommodityType;
import cc.game.soc.core.DevelopmentArea;
import cc.game.soc.core.ResourceType;
import cc.game.soc.core.SOC;
import cc.game.soc.core.SpecialVictoryType;
import cc.game.soc.core.VertexType;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Utils;

/**
 * 
 * @author Chris Caron
 *
 * Base player type to interact with GUI
 */
public final class UIPlayerRenderer implements UIRenderer {

	private GColor color = GColor.BLACK;
	
	public void setColor(GColor color) {
		this.color = color;
	}
	
	public GColor getColor() {
		return color;
	}
	
    private boolean animationEnabled = true;
	
	private long getAnimTime() {
		return 1500;//return GUI.instance.getProps().getIntProperty("anim.ms", 1500);
	}

	private UIPlayer player;
    private final UIComponent component;

    public UIPlayerRenderer(UIComponent component) {
        this.component = component;
        this.component.setRenderer(this);
    }

    public void setPlayer(UIPlayer player) {
        this.player = player;
    }

    @Override
    public void draw(APGraphics g, int px, int py) {
        if (player == null)
            return;

        SOC soc = UISOC.getInstance();
        g.setTextStyles(AGraphics.TextStyle.BOLD);

        StringBuffer str = new StringBuffer();
        str.append(player.getName()).append(" ").append(player.getPoints()).append(" Points Cards ")
                .append(player.getTotalCardsLeftInHand()).append( "(").append(soc.getRules().getMaxSafeCardsForPlayer(player.getPlayerNum(), soc.getBoard()))
                .append(")\n");
        if (player.isInfoVisible()) {
            for (ResourceType t : ResourceType.values()) {
                int num = player.getCardCount(t);
                str.append(t.name()).append(" X ").append(num).append("\n");
            }
            if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
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
            if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
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
                str.append(sv.name());
                if (sv.points != 0)
                    str.append(Utils.getSignedString(num*sv.points));
                str.append("\n");
            }
        }

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
        float height = g.drawWrapString(5, 5, component.getWidth(), str.toString());



        //Rectangle r = AWTUtils.drawWrapString(g, 5, 5, getWidth(), str.toString());
        //int h = r.y + r.height + 10;
        //int w = getWidth() - 1;
        if (isCurrentPlayer()) {
            g.drawRect(0, 0, component.getWidth(), component.getHeight(), 3);
        }
        //setPreferredSize(new Dimension(w, h));
        component.setMinSize(component.getWidth(), Math.round(15 + height));
    }

    final boolean isCurrentPlayer() {
        return player.getPlayerNum() == UISOC.getInstance().getCurPlayerNum();
    }

    @Override
    public void doClick() {

    }
}
