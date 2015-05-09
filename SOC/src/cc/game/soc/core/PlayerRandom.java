package cc.game.soc.core;

import java.util.Arrays;
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

    private <T> T pickRandom(List<T> options) {
    	return options.get(Utils.rand() % options.size());
    }
    
	@Override
	public MoveType chooseMove(SOC soc, List<MoveType> moves) {
		return pickRandom(moves);
	}

	@Override
	public Vertex chooseVertex(SOC soc, List<Integer> vertexIndices, VertexChoice mode) {
		return soc.getBoard().getVertex(pickRandom(vertexIndices));
	}

	@Override
	public Route chooseRoute(SOC soc, List<Integer> routeIndices, RouteChoice mode) {
		return soc.getBoard().getRoute(pickRandom(routeIndices));
	}

	@Override
	public RouteChoiceType chooseRouteType(SOC soc) {
		return pickRandom(Arrays.asList(RouteChoiceType.values()));
	}

	@Override
	public Tile chooseTile(SOC soc, List<Integer> tileIndices, TileChoice mode) {
		return soc.getBoard().getTile(pickRandom(tileIndices));
	}

	@Override
	public Trade chooseTradeOption(SOC soc, List<Trade> trades) {
		return pickRandom(trades);
	}

	@Override
	public Player choosePlayer(SOC soc, List<Integer> playerOptions, PlayerChoice mode) {
		return soc.getPlayerByPlayerNum(pickRandom(playerOptions));
	}

	@Override
	public Card chooseCard(SOC soc, List<Card> cards, CardChoice mode) {
		return pickRandom(cards);
	}

	@Override
	public <T extends Enum<T>> T chooseEnum(SOC soc, EnumChoice mode, T ... values) {
		return pickRandom(Arrays.asList(values));
	}

	@Override
	public boolean setDice(int[] die, int num) {
		for (int i=0; i<num; i++) {
			die[i] = 1 + Utils.rand() % 6;
		}
		return true;
	}
    
    
}
