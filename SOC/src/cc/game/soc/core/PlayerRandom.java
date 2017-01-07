package cc.game.soc.core;

import java.util.Collection;

import cc.lib.game.Utils;

/**
 * An implementation of SOCPlayer that returns random results.
 * 
 * @author ccaron
 *
 */
public class PlayerRandom extends Player {

    public PlayerRandom() {
    }

    private Object pickRandom(Collection<?> options) {
    	return options.toArray(new Object[options.size()])[Utils.rand() % options.size()];
    }
    
	@Override
	public MoveType chooseMove(SOC soc, Collection<MoveType> moves) {
		return (MoveType)pickRandom(moves);
	}

	@Override
	public Vertex chooseVertex(SOC soc, Collection<Integer> vertexIndices, VertexChoice mode, Vertex knightToMove) {
		return soc.getBoard().getVertex((Integer)pickRandom(vertexIndices));
	}

	@Override
	public Route chooseRoute(SOC soc, Collection<Integer> routeIndices, RouteChoice mode) {
		return soc.getBoard().getRoute((Integer)pickRandom(routeIndices));
	}

	@Override
	public RouteChoiceType chooseRouteType(SOC soc) {
		return RouteChoiceType.values()[Utils.rand() % RouteChoiceType.values().length];
	}

	@Override
	public Tile chooseTile(SOC soc, Collection<Integer> tileIndices, TileChoice mode) {
		return soc.getBoard().getTile((Integer)pickRandom(tileIndices));
	}

	@Override
	public Trade chooseTradeOption(SOC soc, Collection<Trade> trades) {
		return (Trade)pickRandom(trades);
	}

	@Override
	public Player choosePlayer(SOC soc, Collection<Integer> playerOptions, PlayerChoice mode) {
		return soc.getPlayerByPlayerNum((Integer)pickRandom(playerOptions));
	}

	@Override
	public Card chooseCard(SOC soc, Collection<Card> cards, CardChoice mode) {
		return (Card)pickRandom(cards);
	}

	@Override
	public <T extends Enum<T>> T chooseEnum(SOC soc, EnumChoice mode, T [] values) {
		return values[Utils.rand() % values.length];
	}

	@Override
	public boolean setDice(Dice [] die, int num) {
		for (int i=0; i<num; i++) {
			die[i].roll();
		}
		return true;
	}
    
    
}
