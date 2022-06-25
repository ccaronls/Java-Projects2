package cc.game.soc.ui;

import cc.game.soc.core.Card;
import cc.game.soc.core.CardType;
import cc.game.soc.core.CommodityType;
import cc.game.soc.core.DevelopmentArea;
import cc.game.soc.core.DevelopmentCardType;
import cc.game.soc.core.ResourceType;
import cc.game.soc.core.SOC;
import cc.game.soc.core.SpecialVictoryType;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.Utils;
import cc.lib.ui.UIComponent;

/**
 * 
 * @author Chris Caron
 *
 * Base player type to interact with GUI
 */
public final class UIPlayerRenderer extends UIRenderer {

	private GColor color = GColor.BLACK;
	
	public void setColor(GColor color) {
		this.color = color;
	}
	
	public GColor getColor() {
		return color;
	}

	int numCardAnimations = 0;
	int playerNum=0;

    public UIPlayerRenderer(UIComponent component) {
        super(component);
    }

    public void setPlayer(int playerNum) {
        this.playerNum = playerNum;
        getComponent().redraw();
    }

    @Override
    public void draw(APGraphics g, int px, int py) {
        if (playerNum < 1)
            return;

        SOC soc = UISOC.getInstance();
        if (soc == null)
            return;
        UIPlayer player = (UIPlayer)soc.getPlayerByPlayerNum(playerNum);
        if (player == null)
            return;

        g.setTextHeight(RenderConstants.textSizeBig);
        //g.setTextStyles(AGraphics.TextStyle.BOLD);

        StringBuffer str = new StringBuffer();
        str.append(String.format("%1$s %2$d Points\nCards %3$d(%4$d)", player.getName(), player.getPoints(), player.getTotalCardsLeftInHand(), soc.getRules().getMaxSafeCardsForPlayer(player.getPlayerNum(), soc.getBoard())));
        str.append("\n");
        if (player.isInfoVisible()) {
            for (ResourceType t : ResourceType.values()) {
                int num = player.getCardCount(t);
                str.append(t.getName()).append(" X ").append(num).append("\n");
            }
            if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
                for (CommodityType t : CommodityType.values()) {
                    int num = player.getCardCount(t);
                    str.append(t.getName()).append(" X ").append(num).append("\n");
                }
                for (Card c : player.getCards(CardType.Progress)) {
                    str.append(c.getName()).append(" (").append(c.getCardStatus().getName()).append(")\n");
                }
            } else {
                for (Card c : player.getCards(CardType.Development)) {
                    // dont need to show these since it is covered by the army size
                    switch (c.getCardStatus()) {
                        case USABLE:
                            str.append(c.getName()).append("\n");
                            break;
                        case USED:
                            // ignore these since they are covered by the army size
                            if (c.getTypeOrdinal() == DevelopmentCardType.Soldier.ordinal() || c.getTypeOrdinal() == DevelopmentCardType.Warship.ordinal())
                                continue;
                        case UNUSABLE:
                            str.append(c.getName()).append(" (").append(c.getCardStatus().getName()).append(")\n");
                            break;
                    }
                }
            }
        } else {
            if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
                int num = player.getCardCount(CardType.Resource);
                num += player.getCardCount(CardType.Commodity);
                str.append(String.format("Progress X %d", num)).append("\n");
                num = player.getCardCount(CardType.Progress);
                str.append(String.format("Progress X %d", num)).append("\n");
            } else {
                int num = player.getUnusedCardCount();
                str.append(String.format("Cards X %d", num)).append("\n");
            }
        }

        //int numSettlements = soc.getBoard().getNumVertsOfType(player.getPlayerNum(), VertexType.SETTLEMENT);
        //int numCities      = soc.getBoard().getNumVertsOfType(player.getPlayerNum(), VertexType.CITY, VertexType.WALLED_CITY);
        //nt numMetros      = soc.getBoard().getNumVertsOfType(player.getPlayerNum(), VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE);
        int numKnights     = soc.getBoard().getNumKnightsForPlayer(player.getPlayerNum());
        int knightLevel    = soc.getBoard().getKnightLevelForPlayer(player.getPlayerNum(), true, false);
        int maxKnightLevel = soc.getBoard().getKnightLevelForPlayer(player.getPlayerNum(), true, true);
        int numDiscoveredTiles = player.getNumDiscoveredTerritories();

        int size = player.getArmySize(soc.getBoard());
        if (size > 0) {
            str.append(String.format("Army X %d", size)).append("\n");
        }
        if (numKnights > 0) {
            str.append(String.format("Knights %1$d/%2$d", knightLevel, maxKnightLevel)).append("\n");
        }
        str.append(String.format("Road Length %d", player.getRoadLength())).append("\n");
        for (SpecialVictoryType sv : SpecialVictoryType.values()) {
            int num = player.getCardCount(sv);
            if (num > 0) {
                str.append(sv.getName());
                if (sv.points != 0)
                    str.append(Utils.getSignedString(num*sv.points));
                str.append("\n");
            }
        }

        for (DevelopmentArea d : DevelopmentArea.values()) {
            if (player.getCityDevelopment(d) > 0) {
                str.append(d.getName()).append(" ").append(d.getLevelName(player.getCityDevelopment(d))).append(" (").append(player.getCityDevelopment(d)).append(") ");
                if (soc.getMetropolisPlayer(d) == player.getPlayerNum()) {
                    str.append(" +").append(soc.getRules().getPointsPerMetropolis());
                }
                str.append("\n");
            }
        }
        if (player.getMerchantFleetTradable() != null) {
            str.append(String.format("Merchant Fleet %s", player.getMerchantFleetTradable().getName())).append("\n");
        }

        {
            int num = soc.getBoard().getNumDiscoveredIslands(player.getPlayerNum());
            if (num > 0) {
                str.append(String.format("Discovered Islands X %$1d %$2s", num, Utils.getSignedStringOrEmptyWhenZero(num * soc.getRules().getPointsIslandDiscovery()))).append("\n");
            }
        }

        if (numDiscoveredTiles > 0) {
            str.append(String.format("Discovered Tiles X %d", numDiscoveredTiles)).append("\n");
        }

        float padding = RenderConstants.textMargin;

        g.setColor(player.getColor());
        GDimension dim = g.drawWrapString(padding, padding, getComponent().getWidth(), str.toString());
        GDimension min = new GDimension(dim.width + padding*2, dim.height + padding*2);
        setMinDimension(min);

        if (isCurrentPlayer()) {
            g.drawRect(0,0, getComponent().getWidth()-RenderConstants.thickLineThickness/2, Math.max(min.height, getComponent().getHeight())-RenderConstants.thickLineThickness/2, RenderConstants.thickLineThickness);
        }
    }

    final boolean isCurrentPlayer() {
        return playerNum == UISOC.getInstance().getCurPlayerNum();
    }

}
