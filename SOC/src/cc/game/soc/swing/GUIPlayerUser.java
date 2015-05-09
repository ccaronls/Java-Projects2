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
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), PickMode.PM_CITY);//getChooseCityVertex(vertexIndices);
				if (v != null) {
					super.startCityAnimation(v);
				}
				break;
			case CITY_WALL:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), PickMode.PM_WALLED_CITY);//getChooseCityWallVertex(vertexIndices);
				if (v != null) {
					super.startCityWallAnimation(v);
				}
				break;
			case KNIGHT_TO_ACTIVATE:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), PickMode.PM_ACTIVATE_KNIGHT);
				break;
			case KNIGHT_TO_PROMOTE:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), PickMode.PM_PROMOTE_KNIGHT);
				break;
			case OPPONENT_KNIGHT_TO_DISPLACE:
			case KNIGHT_TO_MOVE:
			case KNIGHT_DESERTER:
			case KNIGHT_DISPLACED:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), PickMode.PM_KNIGHT);
				break;
			case NEW_KNIGHT:
			case KNIGHT_MOVE_POSITION:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), PickMode.PM_KNIGHT);
				if (v != null) {
					super.startKnightAnimation(v);
				}
				break;
			case POLITICS_METROPOLIS:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), PickMode.PM_METROPOLIS_POLITICS);
				if (v != null) {
					//super.startKnightAnimation(v);
				}
				break;
			case SCIENCE_METROPOLIS:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), PickMode.PM_METROPOLIS_SCIENCE);
				if (v != null) {
					//super.startKnightAnimation(v);
				}
				break;
			case TRADE_METROPOLIS:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), PickMode.PM_METROPOLIS_TRADE);
				if (v != null) {
					//super.startKnightAnimation(v);
				}
				break;
			case SETTLEMENT:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), PickMode.PM_SETTLEMENT);
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
				r = GUI.instance.getChooseRoadEdge(routeIndices);
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
			case PIRATE:
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
	public Player choosePlayer(SOC soc, List<Integer> players, PlayerChoice mode) {
		switch (mode) {
			case PLAYER_FOR_DESERTION:
				return GUI.instance.getChoosePlayerKnightForDesertion(players);
			case PLAYER_TO_SPY_ON:
				return GUI.instance.getChoosePlayerToSpyOn(players);
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
