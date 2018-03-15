package cc.game.soc.ui;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;

import cc.game.soc.core.Card;
import cc.game.soc.core.Dice;
import cc.game.soc.core.MoveType;
import cc.game.soc.core.SOC;
import cc.game.soc.core.Trade;
import cc.lib.net.GameClient;
import cc.lib.net.GameCommand;

public final class UIPlayerUser extends UIPlayer implements GameClient.Listener {

    private final GameClient client = new GameClient(getName(), NetCommon.VERSION, NetCommon.cypher);

	public UIPlayerUser() {
	    client.addListener(this);
    }

    public void connect(InetAddress address) throws IOException {
        client.connect(address, NetCommon.PORT);
    }

    public void disconnect() {
	    client.disconnect("user disconnected");
    }

	@Override
	public MoveType chooseMove(SOC soc, Collection<MoveType> moves) {
		return ((UISOC)soc).chooseMoveMenu(moves);
	}
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
    private MoveType chooseMove(Collection<MoveType> moves) {
        return chooseMove(UISOC.getInstance(), moves);
    }

	@Override
	public RouteChoiceType chooseRouteType(SOC soc) {
		return ((UISOC)soc).chooseRouteType();
	}
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
    private RouteChoiceType chooseRouteType() {
	    return chooseRouteType(UISOC.getInstance());
    }

	@Override
	public Integer chooseVertex(SOC soc, Collection<Integer> vertexIndices, VertexChoice mode, Integer knightToMove) {
		Integer v = ((UISOC)soc).chooseVertex(vertexIndices, getPlayerNum(), mode);
		if (v != null) {
            doVertexAnimation(soc, mode, soc.getBoard().getVertex(v), knightToMove);
        }
		return v;
	}
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
    private Integer chooseVertex(Collection<Integer> vertexIndices, VertexChoice mode, Integer knightToMove) {
	    return chooseVertex(UISOC.getInstance(), vertexIndices, mode, knightToMove);
    }

	@Override
	public Integer chooseRoute(SOC soc, Collection<Integer> routeIndices, RouteChoice mode) {
		Integer r = ((UISOC)soc).chooseRoute(routeIndices, mode);
		if (r != null) {
            doRouteAnimation(soc, mode, soc.getBoard().getRoute(r));
        }
		return r;
	}
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
    private  Integer chooseRoute(Collection<Integer> routeIndices, RouteChoice mode) {
	    return chooseRoute(UISOC.getInstance(), routeIndices, mode);
    }

    @Override
	public Integer chooseTile(SOC soc, Collection<Integer> tileIndices, TileChoice mode) {
		return ((UISOC)soc).chooseTile(tileIndices, mode);
	}
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
    private  Integer chooseTile(Collection<Integer> tileIndices, TileChoice mode) {
	    return chooseTile(UISOC.getInstance(), tileIndices, mode);
    }

	@Override
	public Trade chooseTradeOption(SOC soc, Collection<Trade> trades) {
		return ((UISOC)soc).chooseTradeMenu(trades);
	}
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
    private  Trade chooseTradeOption(Collection<Trade> trades) {
        return chooseTradeOption(UISOC.getInstance(), trades);
    }

    @Override
	public Integer choosePlayer(SOC soc, Collection<Integer> players, PlayerChoice mode) {
		return ((UISOC)soc).choosePlayerMenu(players, mode);
	}
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
    private  Integer choosePlayer(Collection<Integer> players, PlayerChoice mode) {
	    return choosePlayer(UISOC.getInstance(), players, mode);
    }

	@Override
	public Card chooseCard(SOC soc, Collection<Card> cards, CardChoice mode) {
		return ((UISOC)soc).chooseCardMenu(cards);
    }
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
    private Card chooseCard(Collection<Card> cards, CardChoice mode) {
        return chooseCard(UISOC.getInstance(), cards, mode);
    }

    @Override
	public <T extends Enum<T>> T chooseEnum(SOC soc, EnumChoice mode, T [] values) {
		return ((UISOC)soc).chooseEnum(Arrays.asList(values));
    }
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
    private <T extends Enum<T>> T chooseEnum(EnumChoice mode, T [] values) {
        return chooseEnum(UISOC.getInstance(), mode, values);
    }

    @Override
	public boolean setDice(SOC soc, Dice [] die, int num) {
		return ((UISOC)soc).getSetDiceMenu(die, num);
	}
    // private versions of overloaded methods are called by remote server so as not to need to include the large SOC object
	private Dice [] setDice(Dice [] die, int num) {
	    setDice(UISOC.getInstance(), die, num);
	    return die;
    }

	@Override
    public boolean isInfoVisible() {
        return true;
    }

    /*
    @Override
    public void onConnected(ClientConnection conn) {
        // we are the server and have a new client connection.
        // match them with a player in the game...
        UISOC soc = UISOC.getInstance();
        for (Player p : soc.getPlayers()) {
            if (p == this)
                continue;
            if (p instanceof UIPlayer) {
                UIPlayer uip = (UIPlayer)p;
                if (uip.connection == null) {
                    uip.connect(conn);
                    return;
                }
            }
        }
        // made it here so it means we were not able to assign, so add a new player!
        UIPlayer player = new UIPlayer();
        player.connect(conn);
        soc.addPlayer(player);
    }*/

    @Override
    public void onCommand(GameCommand cmd) {

    }

    @Override
    public void onMessage(String msg) {

    }

    @Override
    public void onDisconnected(String reason) {

    }

    @Override
    public void onConnected() {

    }
}
