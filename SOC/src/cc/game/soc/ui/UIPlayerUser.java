package cc.game.soc.ui;

import java.util.Arrays;
import java.util.Collection;

import cc.game.soc.core.Card;
import cc.game.soc.core.Dice;
import cc.game.soc.core.MoveType;
import cc.game.soc.core.Player;
import cc.game.soc.core.SOC;
import cc.game.soc.core.Trade;
import cc.lib.annotation.Keep;
import cc.lib.net.GameClient;
import cc.lib.net.GameCommand;

/**
 * A UI Plauer User is a player that required user feedback for choice callabcks. On any individual
 * device "there can only be one" user.
 *
 * In a MP game, if the device is connected as a client, then the GameClient is connected to a game
 * server where this user is represented as a UIPlayer with an active clientConnection.
 */
public final class UIPlayerUser extends UIPlayer implements GameClient.Listener {

    public final GameClient client = new GameClient(getName(), NetCommon.VERSION, NetCommon.getCypher());

	public UIPlayerUser() {
        client.addListener(this);
    }

	@Override
	public MoveType chooseMove(SOC soc, Collection<MoveType> moves) {
		return ((UISOC)soc).chooseMoveMenu(moves);
	}
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
    @Keep
    private MoveType chooseMove(Collection<MoveType> moves) {
        return chooseMove(UISOC.getInstance(), moves);
    }

	@Override
	public RouteChoiceType chooseRouteType(SOC soc) {
		return ((UISOC)soc).chooseRouteType();
	}
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
    @Keep
    private RouteChoiceType chooseRouteType() {
	    return chooseRouteType(UISOC.getInstance());
    }

	@Override
	public Integer chooseVertex(SOC soc, Collection<Integer> vertexIndices, VertexChoice mode, Integer knightToMove) {
		return ((UISOC)soc).chooseVertex(vertexIndices, mode, knightToMove);
	}
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
    @Keep
    private Integer chooseVertex(Collection<Integer> vertexIndices, VertexChoice mode, Integer knightToMove) {
	    return chooseVertex(UISOC.getInstance(), vertexIndices, mode, knightToMove);
    }

	@Override
	public Integer chooseRoute(SOC soc, Collection<Integer> routeIndices, RouteChoice mode, Integer shipToMove) {
		return ((UISOC)soc).chooseRoute(routeIndices, mode, shipToMove == null ? null : soc.getBoard().getRoute(shipToMove));
	}
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
    @Keep
    private  Integer chooseRoute(Collection<Integer> routeIndices, RouteChoice mode, Integer shipToMove) {
	    return chooseRoute(UISOC.getInstance(), routeIndices, mode, shipToMove);
    }

    @Override
	public Integer chooseTile(SOC soc, Collection<Integer> tileIndices, TileChoice mode) {
		return ((UISOC)soc).chooseTile(tileIndices, mode);
	}
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
    @Keep
    private  Integer chooseTile(Collection<Integer> tileIndices, TileChoice mode) {
	    return chooseTile(UISOC.getInstance(), tileIndices, mode);
    }

	@Override
	public Trade chooseTradeOption(SOC soc, Collection<Trade> trades) {
		return ((UISOC)soc).chooseTradeMenu(trades);
	}
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
    @Keep
    private  Trade chooseTradeOption(Collection<Trade> trades) {
        return chooseTradeOption(UISOC.getInstance(), trades);
    }

    @Override
	public Integer choosePlayer(SOC soc, Collection<Integer> players, PlayerChoice mode) {
		return ((UISOC)soc).choosePlayerMenu(players, mode);
	}
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
    @Keep
    private  Integer choosePlayer(Collection<Integer> players, PlayerChoice mode) {
	    return choosePlayer(UISOC.getInstance(), players, mode);
    }

	@Override
	public Card chooseCard(SOC soc, Collection<Card> cards, CardChoice mode) {
		return ((UISOC)soc).chooseCardMenu(cards);
    }
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
    @Keep
    private Card chooseCard(Collection<Card> cards, CardChoice mode) {
        return chooseCard(UISOC.getInstance(), cards, mode);
    }

    @Override
	public <T extends Enum<T>> T chooseEnum(SOC soc, EnumChoice mode, T [] values) {
		return ((UISOC)soc).chooseEnum(Arrays.asList(values));
    }
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
    @Keep
    private <T extends Enum<T>> T chooseEnum(EnumChoice mode, T [] values) {
        return chooseEnum(UISOC.getInstance(), mode, values);
    }

    @Override
	public boolean setDice(SOC soc, Dice [] die, int num) {
		return ((UISOC)soc).getSetDiceMenu(die, num);
	}
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
    @Keep
	private Dice [] setDice(Dice [] die, int num) {
	    setDice(UISOC.getInstance(), die, num);
	    return die;
    }

	@Override
    public boolean isInfoVisible() {
        return true;
    }

    @Override
    public void onCommand(GameCommand cmd) {
        try {

            if (cmd.getType().equals(NetCommon.SVR_TO_CL_INIT)) {
                UISOC soc = UISOC.getInstance();
                soc.deserialize(cmd.getArg("soc"));
                int playerNum = cmd.getInt("playerNum");
                Player p = soc.getPlayerByPlayerNum(playerNum);
                //KEEP_INSTANCES = true;
                copyFrom(p);
                soc.setPlayer(this, playerNum);
                soc.redraw();
            } else if (cmd.getType().equals(NetCommon.SVR_TO_CL_UPDATE)) {
                UISOC.getInstance().mergeDiff(cmd.getArg("soc"));
                UISOC.getInstance().redraw();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onMessage(String msg) {
        UISOC.getInstance().printinfo(0, msg);
        UISOC.getInstance().showOkPopup("MESSAGE", msg);
    }

    @Override
    public void onDisconnected(String reason) {
        UISOC.getInstance().printinfo(getPlayerNum(), "Disconnected from " + client.getServerName() + ": " + reason);
    }

    @Override
    public void onConnected() {
        UISOC.getInstance().printinfo(getPlayerNum(), "Connected to " + client.getServerName());
    }
}
