package cc.game.soc.swing;

import java.util.Arrays;
import java.util.List;

import cc.game.soc.core.*;
import cc.game.soc.swing.BoardComponent.PickMode;

public class GUIPlayerUser extends GUIPlayer {
	
	public GUIPlayerUser() {
	}

	
	@Override
	public MoveType chooseMove(SOC soc, List<MoveType> moves) {
		return GUI.instance.chooseMoveMenu(moves);
	}


	@Override
	public RouteChoiceType chooseRouteType(SOC soc) {
		return GUI.instance.chooseRouteType();
	}

	@Override
	public Vertex chooseVertex(SOC soc, List<Integer> vertexIndices, VertexChoice mode) {
		Vertex v = null;
		switch (mode) {
			case CITY:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), mode);
				if (v != null) {
					super.startCityAnimation(v);
				}
				break;
			case CITY_WALL:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), mode);
				if (v != null) {
					super.startCityWallAnimation(v);
				}
				break;
			case KNIGHT_TO_ACTIVATE:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), mode);
				break;
			case KNIGHT_TO_PROMOTE:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), mode);
				break;
			case OPPONENT_KNIGHT_TO_DISPLACE:
			case KNIGHT_TO_MOVE:
			case KNIGHT_DESERTER:
			case KNIGHT_DISPLACED:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), mode);
				break;
			case NEW_KNIGHT:
			case KNIGHT_MOVE_POSITION:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), mode);
				if (v != null) {
					super.startKnightAnimation(v);
				}
				break;
			case POLITICS_METROPOLIS:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), mode);
				if (v != null) {
					//super.startKnightAnimation(v);
				}
				break;
			case SCIENCE_METROPOLIS:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), mode);
				if (v != null) {
					//super.startKnightAnimation(v);
				}
				break;
			case TRADE_METROPOLIS:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), mode);
				if (v != null) {
					//super.startKnightAnimation(v);
				}
				break;
			case SETTLEMENT:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), mode);
				if (v != null) {
					super.startSettlementAnimation(v);
				}
				break;
		}
		return v;
	}


	@Override
	public Route chooseRoute(SOC soc, List<Integer> routeIndices, RouteChoice mode) {
		Route r = null;
		switch (mode) {
			case ROAD: r = GUI.instance.chooseRoute(routeIndices, mode);
				if (r != null) {
					super.startRoadAnimation(r, soc);
				}
				break;
			
			case ROUTE_DIPLOMAT:
				r = GUI.instance.chooseRoute(routeIndices, mode);
				break;
			case SHIP: 
				r = GUI.instance.chooseRoute(routeIndices, mode);
				if (r != null) {
					super.startShipAnimation(r, soc);
				}
				break;
			
			case SHIP_TO_MOVE: 
				r = GUI.instance.chooseRoute(routeIndices, mode);
				if (r != null) {
					super.startShipAnimation(r, soc);
				}
				break;
		}
		return r;
	}


	@Override
	public Tile chooseTile(SOC soc, List<Integer> tileIndices, TileChoice mode) {
		return GUI.instance.chooseTile(tileIndices, mode);
	}


	@Override
	public Trade chooseTradeOption(SOC soc, List<Trade> trades) {
		return GUI.instance.chooseTradeMenu(trades);
	}


	@Override
	public Player choosePlayer(SOC soc, List<Integer> players, PlayerChoice mode) {
		switch (mode) {
			case PLAYER_FOR_DESERTION:
				return GUI.instance.choosePlayerKnightForDesertion(players);
			case PLAYER_TO_SPY_ON:
				return GUI.instance.choosePlayerToSpyOn(players);
			case PLAYER_TO_TAKE_CARD_FROM:
				return GUI.instance.choosePlayerToTakeCardFromMenu(players);
			case PLAYER_TO_GIFT_CARD:
				return GUI.instance.choosePlayerToGiftCardToMenu(players);
		}
		assert(false);
		return null;
	}


	@Override
	public Card chooseCard(SOC soc, List<Card> cards, CardChoice mode) {
		Card card = GUI.instance.chooseCardMenu(cards);
		switch (mode) {
			case RESOURCE_OR_COMMODITY:
				break;
			case EXCHANGE_CARD:
				break;
			case FLEET_TRADE:
				break;
			case GIVEUP_CARD:
				break;
			case OPPONENT_CARD:
				break;
			case PROGRESS_CARD:
				break;
			
		}
		return card;
	}

	@Override
	public <T extends Enum<T>> T chooseEnum(SOC soc, EnumChoice mode, T... values) {
		switch (mode) {
			case DRAW_DEVELOPMENT_CARD:
				break;
			case DRAW_PROGRESS_CARD:
				break;
			case IMPROVE_DEVELOPMENT_AREA:
				break;
			case MONOPOLY:
				break;
		}
		return GUI.instance.chooseEnum(Arrays.asList(values));
	}

	@Override
	public boolean setDice(int[] die, int num) {
		return GUI.instance.getSetDiceMenu(die, num);
	}

	@Override
    public boolean isInfoVisible() {
        return true;
    }

}
