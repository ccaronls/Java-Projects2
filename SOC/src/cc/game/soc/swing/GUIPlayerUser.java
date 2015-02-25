package cc.game.soc.swing;

import java.util.Arrays;
import java.util.List;

import cc.game.soc.core.*;

public class GUIPlayerUser extends GUIPlayer {
	
	public GUIPlayerUser() {
	}

	
	@Override
	public MoveType chooseMove(SOC soc, List<MoveType> moves) {
		return GUI.instance.getChooseMoveMenu(moves);
	}


	@Override
	public RouteChoiceType chooseRouteType(SOC soc) {
		return GUI.instance.getChooseRouteType();
	}

	@Override
	public Vertex chooseVertex(SOC soc, List<Integer> vertexIndices, VertexChoice mode) {
		Vertex v = null;
		switch (mode) {
			case CITY:
				v = GUI.instance.getChooseCityVertex(vertexIndices);
				if (v != null) {
					super.startCityAnimation(v);
				}
				break;
			case CITY_WALL:
				v = GUI.instance.getChooseCityWallVertex(vertexIndices);
				if (v != null) {
					super.startCityWallAnimation(v);
				}
				break;
			case KNIGHT_DESERTER:
				break;
			case KNIGHT_DISPLACED:
				break;
			case KNIGHT_MOVE_POSITION:
				break;
			case KNIGHT_TO_ACTIVATE:
				break;
			case KNIGHT_TO_MOVE:
				break;
			case KNIGHT_TO_PROMOTE:
				break;
			case NEW_KNIGHT:
				break;
			case OPPONENT_KNIGHT_TO_DISPLACE:
				break;
			case POLITICS_METROPOLIS:
				break;
			case SCIENCE_METROPOLIS:
				break;
			case TRADE_METROPOLIS:
				break;
			case SETTLEMENT:
				v = GUI.instance.getChooseSettlementVertex(vertexIndices);
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
			case ROAD: r = GUI.instance.getChooseRoadEdge(routeIndices);
				if (r != null) {
					super.startRoadAnimation(r, soc);
				}
				break;
			
			case ROUTE_DIPLOMAT:
				break;
			case SHIP: 
				r = GUI.instance.getChooseShipEdge(routeIndices);
				if (r != null) {
					super.startShipAnimation(r, soc);
				}
				break;
			
			case SHIP_TO_MOVE: 
				r = GUI.instance.getChooseShipEdge(routeIndices);
				if (r != null) {
					super.startShipAnimation(r, soc);
				}
				break;
		}
		return r;
	}


	@Override
	public Tile chooseTile(SOC soc, List<Integer> tileIndices, TileChoice mode) {
		switch (mode) {
			case INVENTOR:
				return GUI.instance.getChooseInventorTile(tileIndices);
			case MERCHANT:
				return GUI.instance.getChooseMerchantTile(tileIndices);			
			case ROBBER:
				return GUI.instance.getChooseRobberTile(tileIndices);
		}
		throw new RuntimeException("Unhandled case '" + mode + "'");
	}


	@Override
	public Trade chooseTradeOption(SOC soc, List<Trade> trades) {
		return GUI.instance.getChooseTradeMenu(trades);
	}


	@Override
	public Player choosePlayer(SOC soc, List<Player> players, PlayerChoice mode) {
		switch (mode) {
			case PLAYER_FOR_DESERTION:
			case PLAYER_TO_TAKE_CARD_FROM:
				return GUI.instance.getChoosePlayerToTakeCardFromMenu(players);
			
		}
		assert(false);
		return null;
	}


	@Override
	public Card chooseCard(SOC soc, List<Card> cards, CardChoice mode) {
		Card card = GUI.instance.getChooseCardMenu(cards);
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
		return GUI.instance.getChooseEnum(Arrays.asList(values));
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
