package cc.game.soc.core;

import java.util.Collection;
import java.util.List;

public class PlayerTemp extends Player {

    public PlayerTemp() {}
    
    PlayerTemp(int playerNum) {
        setPlayerNum(playerNum);
    }
    
	public PlayerTemp(Player p) {
		copyFrom(p);
	}

	@Override
	public MoveType chooseMove(SOC soc, Collection<MoveType> moves) {
		throw new AssertionError("This should never get called");
	}

	@Override
	public Integer chooseVertex(SOC soc, Collection<Integer> vertexIndices, VertexChoice mode, Integer knightToMove) {
        throw new AssertionError("This should never get called");
	}

	@Override
	public Integer chooseRoute(SOC soc, Collection<Integer> routeIndices, RouteChoice mode, Integer shipToMove) {
        throw new AssertionError("This should never get called");
	}

	@Override
	public RouteChoiceType chooseRouteType(SOC soc) {
        throw new AssertionError("This should never get called");
	}

	@Override
	public Integer chooseTile(SOC soc, Collection<Integer> tileIndices, TileChoice mode) {
        throw new AssertionError("This should never get called");
	}

	@Override
	public Trade chooseTradeOption(SOC soc, Collection<Trade> trades) {
        throw new AssertionError("This should never get called");
	}

	@Override
	public Integer choosePlayer(SOC soc, Collection<Integer> playerOptions, PlayerChoice mode) {
        throw new AssertionError("This should never get called");
	}

	@Override
	public Card chooseCard(SOC soc, Collection<Card> cards, CardChoice mode) {
        throw new AssertionError("This should never get called");
	}

	@Override
	public <T extends Enum<T>> T chooseEnum(SOC soc, EnumChoice mode, T [] values) {
        throw new AssertionError("This should never get called");
	}

	@Override
	public boolean setDice(SOC soc, List<Dice> die, int num) {
        throw new AssertionError("This should never get called");
	}

}
