package cc.game.soc.swing;

import java.util.*;

import cc.game.soc.core.*;

public class GUIPlayerUser extends GUIPlayer {
	
	public GUIPlayerUser() {
	}

	
	@Override
	public MoveType chooseMove(SOC soc, Collection<MoveType> moves) {
		return GUI.instance.chooseMoveMenu(moves);
	}


	@Override
	public RouteChoiceType chooseRouteType(SOC soc) {
		return GUI.instance.chooseRouteType();
	}

	@Override
	public Vertex chooseVertex(SOC soc, Collection<Integer> vertexIndices, VertexChoice mode, Vertex knightToMove) {
		Vertex v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), mode);
		doVertexAnimation(soc, mode, v, knightToMove);
		return v;
	}


	@Override
	public Route chooseRoute(SOC soc, Collection<Integer> routeIndices, RouteChoice mode) {
		Route r = GUI.instance.chooseRoute(routeIndices, mode);
		doRouteAnimation(soc, mode, r);
		return r;
	}

	@Override
	public Tile chooseTile(SOC soc, Collection<Integer> tileIndices, TileChoice mode) {
		return GUI.instance.chooseTile(tileIndices, mode);
	}


	@Override
	public Trade chooseTradeOption(SOC soc, Collection<Trade> trades) {
		return GUI.instance.chooseTradeMenu(trades);
	}


	@Override
	public Player choosePlayer(SOC soc, Collection<Integer> players, PlayerChoice mode) {
		return GUI.instance.choosePlayerMenu(players, mode);
	}


	@Override
	public Card chooseCard(SOC soc, Collection<Card> cards, CardChoice mode) {
		return GUI.instance.chooseCardMenu(cards);
	}

	@Override
	public <T extends Enum<T>> T chooseEnum(SOC soc, EnumChoice mode, T [] values) {
		return GUI.instance.chooseEnum(Arrays.asList(values));
	}

	@Override
	public boolean setDice(Dice [] die, int num) {
		return GUI.instance.getSetDiceMenu(die, num);
	}

	@Override
    public boolean isInfoVisible() {
        return true;
    }

}
