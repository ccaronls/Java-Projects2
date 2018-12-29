package cc.game.soc.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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

    private <T> T pickRandom(Collection<T> options) {
        Iterator<T> it = options.iterator();
        int num = Utils.rand() % options.size();
        for (int i=0; i<num-1; i++)
            it.next();
        return it.next();
    }
    
	@Override
	public MoveType chooseMove(SOC soc, Collection<MoveType> moves) {
		return pickRandom(moves);
	}

	@Override
	public Integer chooseVertex(SOC soc, Collection<Integer> vertexIndices, VertexChoice mode, Integer knightToMove) {
		return pickRandom(vertexIndices);
	}

	@Override
	public Integer chooseRoute(SOC soc, Collection<Integer> routeIndices, RouteChoice mode, Integer shipToMove) {
		return pickRandom(routeIndices);
	}

	@Override
	public RouteChoiceType chooseRouteType(SOC soc) {
		return RouteChoiceType.values()[Utils.rand() % RouteChoiceType.values().length];
	}

	@Override
	public Integer chooseTile(SOC soc, Collection<Integer> tileIndices, TileChoice mode) {
		return pickRandom(tileIndices);
	}

	@Override
	public Trade chooseTradeOption(SOC soc, Collection<Trade> trades) {
		return pickRandom(trades);
	}

	@Override
	public Integer choosePlayer(SOC soc, Collection<Integer> playerOptions, PlayerChoice mode) {
		return pickRandom(playerOptions);
	}

	@Override
	public Card chooseCard(SOC soc, Collection<Card> cards, CardChoice mode) {
		return pickRandom(cards);
	}

	@Override
	public <T extends Enum<T>> T chooseEnum(SOC soc, EnumChoice mode, T [] values) {
		return values[Utils.rand() % values.length];
	}

	@Override
	public boolean setDice(SOC soc, List<Dice> die, int num) {
		for (int i=0; i<num; i++) {
			die.get(i).roll();
		}
		return true;
	}
    
    
}
