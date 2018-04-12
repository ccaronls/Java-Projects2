package cc.game.soc.core;

import java.util.Collection;

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer chooseVertex(SOC soc, Collection<Integer> vertexIndices, VertexChoice mode, Integer knightToMove) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer chooseRoute(SOC soc, Collection<Integer> routeIndices, RouteChoice mode, Integer shipToMove) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RouteChoiceType chooseRouteType(SOC soc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer chooseTile(SOC soc, Collection<Integer> tileIndices, TileChoice mode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Trade chooseTradeOption(SOC soc, Collection<Trade> trades) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer choosePlayer(SOC soc, Collection<Integer> playerOptions, PlayerChoice mode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Card chooseCard(SOC soc, Collection<Card> cards, CardChoice mode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends Enum<T>> T chooseEnum(SOC soc, EnumChoice mode, T [] values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean setDice(SOC soc, Dice [] die, int num) {
		// TODO Auto-generated method stub
		return false;
	}
	

}
