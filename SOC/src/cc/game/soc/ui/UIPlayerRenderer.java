package cc.game.soc.ui;

import cc.game.soc.android.R;
import cc.game.soc.core.Card;
import cc.game.soc.core.CardStatus;
import cc.game.soc.core.CardType;
import cc.game.soc.core.CommodityType;
import cc.game.soc.core.DevelopmentArea;
import cc.game.soc.core.DevelopmentCardType;
import cc.game.soc.core.ResourceType;
import cc.game.soc.core.SOC;
import cc.game.soc.core.SpecialVictoryType;
import cc.game.soc.core.VertexType;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.Utils;

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
        component.redraw();
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
        str.append(getString(R.string.player_info_header_name_pts_cards_maxsafecards, player.getName(), player.getPoints(), player.getTotalCardsLeftInHand(), soc.getRules().getMaxSafeCardsForPlayer(player.getPlayerNum(), soc.getBoard())));
        str.append("\n");
        if (player.isInfoVisible()) {
            for (ResourceType t : ResourceType.values()) {
                int num = player.getCardCount(t);
                str.append(t.getName(UISOC.getInstance())).append(" X ").append(num).append("\n");
            }
            if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
                for (CommodityType t : CommodityType.values()) {
                    int num = player.getCardCount(t);
                    str.append(t.getName(UISOC.getInstance())).append(" X ").append(num).append("\n");
                }
                for (Card c : player.getCards(CardType.Progress)) {
                    str.append(c.getName(UISOC.getInstance())).append(" (").append(c.getCardStatus().getName(UISOC.getInstance())).append(")\n");
                }
            } else {
                for (Card c : player.getCards(CardType.Development)) {
                    // dont need to show these since it is covered by the army size
                    if (c.getCardStatus() == CardStatus.USED && c.getTypeOrdinal() == DevelopmentCardType.Soldier.ordinal())
                        continue;
                    str.append(c.getName(UISOC.getInstance())).append(" (").append(c.getCardStatus().getName(UISOC.getInstance())).append(")\n");
                }
            }
        } else {
            if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
                int num = player.getCardCount(CardType.Resource);
                num += player.getCardCount(CardType.Commodity);
                str.append(getString(R.string.player_info_materials_count, num)).append("\n");
                num = player.getCardCount(CardType.Progress);
                str.append(getString(R.string.player_info_progress_count, num)).append("\n");
            } else {
                int num = player.getUnusedCardCount();
                str.append(getString(R.string.player_info_cards_count, num)).append("\n");
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
            str.append(getString(R.string.player_info_army_size, size)).append("\n");
        }
        if (numKnights > 0) {
            str.append(getString(R.string.player_info_knights_count_strength, knightLevel, maxKnightLevel)).append("\n");
        }
        str.append(getString(R.string.player_info_road_length, player.getRoadLength())).append("\n");
        for (SpecialVictoryType sv : SpecialVictoryType.values()) {
            int num = player.getCardCount(sv);
            if (num > 0) {
                str.append(sv.getName(this));
                if (sv.points != 0)
                    str.append(Utils.getSignedString(num*sv.points));
                str.append("\n");
            }
        }

        for (DevelopmentArea d : DevelopmentArea.values()) {
            if (player.getCityDevelopment(d) > 0) {
                str.append(d.getName(this)).append(" ").append(d.getLevelName(player.getCityDevelopment(d), UISOC.getInstance())).append(" (").append(player.getCityDevelopment(d)).append(") ");
                if (soc.getMetropolisPlayer(d) == player.getPlayerNum()) {
                    str.append(" +").append(soc.getRules().getPointsPerMetropolis());
                }
                str.append("\n");
            }
        }
        if (player.getMerchantFleetTradable() != null) {
            str.append(getString(R.string.player_info_merchantfleet_tradable, player.getMerchantFleetTradable().getName(UISOC.getInstance()))).append("\n");
        }

        {
            int num = soc.getBoard().getNumDiscoveredIslands(player.getPlayerNum());
            if (num > 0) {
                str.append(getString(R.string.player_info_discoveredislands_count_pts, num, Utils.getSignedStringOrEmptyWhenZero(num * soc.getRules().getPointsIslandDiscovery()))).append("\n");
            }
        }

        if (numDiscoveredTiles > 0) {
            str.append(getString(R.string.player_info_discoveredtiles_count, numDiscoveredTiles)).append("\n");
        }

        float padding = RenderConstants.textMargin;

        g.setColor(player.getColor());
        GDimension dim = g.drawWrapString(padding, padding, component.getWidth(), str.toString());
        GDimension min = new GDimension(dim.width + padding*2, dim.height + padding*2);
        setMinDimension(min);

        if (isCurrentPlayer()) {
            g.drawRect(0,0, component.getWidth()-RenderConstants.thickLineThickness/2, Math.max(min.height, component.getHeight())-RenderConstants.thickLineThickness/2, RenderConstants.thickLineThickness);
        }
    }

    final boolean isCurrentPlayer() {
        return playerNum == UISOC.getInstance().getCurPlayerNum();
    }

}
