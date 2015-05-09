package cc.game.soc.core;

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
	public MoveType chooseMove(SOC soc, List<MoveType> moves) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vertex chooseVertex(SOC soc, List<Integer> vertexIndices,
			VertexChoice mode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Route chooseRoute(SOC soc, List<Integer> routeIndices,
			RouteChoice mode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RouteChoiceType chooseRouteType(SOC soc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Tile chooseTile(SOC soc, List<Integer> tileIndices, TileChoice mode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Trade chooseTradeOption(SOC soc, List<Trade> trades) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Player choosePlayer(SOC soc, List<Integer> playerOptions, PlayerChoice mode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Card chooseCard(SOC soc, List<Card> cards, CardChoice mode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends Enum<T>> T chooseEnum(SOC soc, EnumChoice mode, T ... values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean setDice(int[] die, int num) {
		// TODO Auto-generated method stub
		return false;
	}
	

}
