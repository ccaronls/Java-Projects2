package cc.game.soc.ui;

import java.util.Arrays;
import java.util.Collection;

import cc.game.soc.core.Card;
import cc.game.soc.core.Dice;
import cc.game.soc.core.MoveType;
import cc.game.soc.core.Player;
import cc.game.soc.core.Route;
import cc.game.soc.core.SOC;
import cc.game.soc.core.Tile;
import cc.game.soc.core.Trade;
import cc.game.soc.core.Vertex;

public final class UIPlayerUser extends UIPlayer {

    private final UIPlayerRenderer renederer;
	public UIPlayerUser(UIPlayerRenderer playerRenderer) {
	    this.renederer = playerRenderer;
	}

	@Override
	public MoveType chooseMove(SOC soc, Collection<MoveType> moves) {
		return ((UISOC)soc).chooseMoveMenu(moves);
	}


	@Override
	public RouteChoiceType chooseRouteType(SOC soc) {
		return ((UISOC)soc).chooseRouteType();
	}

	@Override
	public Vertex chooseVertex(SOC soc, Collection<Integer> vertexIndices, VertexChoice mode, Vertex knightToMove) {
		Vertex v = ((UISOC)soc).chooseVertex(vertexIndices, getPlayerNum(), mode);
		renederer.doVertexAnimation(UISOC.getInstance().getUIBoard(), mode, v, knightToMove);
		return v;
	}


	@Override
	public Route chooseRoute(SOC soc, Collection<Integer> routeIndices, RouteChoice mode) {
		Route r = ((UISOC)soc).chooseRoute(routeIndices, mode);
        doRouteAnimation(soc, mode, r);
		return r;
	}

	@Override
	public Tile chooseTile(SOC soc, Collection<Integer> tileIndices, TileChoice mode) {
		return ((UISOC)soc).chooseTile(tileIndices, mode);
	}


	@Override
	public Trade chooseTradeOption(SOC soc, Collection<Trade> trades) {
		return ((UISOC)soc).chooseTradeMenu(trades);
	}


	@Override
	public Player choosePlayer(SOC soc, Collection<Integer> players, PlayerChoice mode) {
		return ((UISOC)soc).choosePlayerMenu(players, mode);
	}


	@Override
	public Card chooseCard(SOC soc, Collection<Card> cards, CardChoice mode) {
		return ((UISOC)soc).chooseCardMenu(cards);
	}

	@Override
	public <T extends Enum<T>> T chooseEnum(SOC soc, EnumChoice mode, T [] values) {
		return ((UISOC)soc).chooseEnum(Arrays.asList(values));
	}

	@Override
	public boolean setDice(SOC soc, Dice [] die, int num) {
		return ((UISOC)soc).getSetDiceMenu(die, num);
	}

	@Override
    public boolean isInfoVisible() {
        return true;
    }

}
