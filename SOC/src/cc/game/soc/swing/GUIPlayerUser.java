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
	public Vertex chooseVertex(SOC soc, Collection<Integer> vertexIndices, VertexChoice mode) {
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
			case PIRATE_FORTRESS:
				v = GUI.instance.chooseVertex(vertexIndices, getPlayerNum(), mode);
				break;
		}
		return v;
	}


	@Override
	public Route chooseRoute(SOC soc, Collection<Integer> routeIndices, RouteChoice mode) {
		Route r = GUI.instance.chooseRoute(routeIndices, mode);
		if (r != null) {
    		switch (mode) {
    			case ROAD: 
    				startRoadAnimation(r, soc);
    				break;
    			
    			case ROUTE_DIPLOMAT:
    				break;
    				
    			case UPGRADE_SHIP:
    				r.setType(RouteType.OPEN);
    				startUpgradeShipAnimation(r);
    				r.setType(RouteType.SHIP);
    				break;
    				
    			case SHIP: 
    				startShipAnimation(r, soc);
    				break;
    			
    			case SHIP_TO_MOVE: 
    				break;
    				
    		}
		}
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
