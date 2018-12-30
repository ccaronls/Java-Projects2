package cc.game.soc.ui;

import java.util.Collection;
import java.util.List;

import cc.game.soc.core.BotNode;
import cc.game.soc.core.Card;
import cc.game.soc.core.Dice;
import cc.game.soc.core.MoveType;
import cc.game.soc.core.PlayerBot;
import cc.game.soc.core.SOC;
import cc.game.soc.core.Trade;
import cc.lib.annotation.Keep;
import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.net.ClientConnection;
import cc.lib.net.GameCommand;

/**
 * 
 * @author Chris Caron
 *
 * Base player type to interact with GUI
 */
public class UIPlayer extends PlayerBot implements ClientConnection.Listener {

	static {
		addField(UIPlayer.class, "color");
	}

	@Omit
	ClientConnection connection = null; // this is set when game is in server mode and this object represents a remote player

    void connect(ClientConnection conn) {
        if (connection != null && connection.isConnected())
            throw new AssertionError("Connection already assigned");
        connection = conn;
        connection.addListener(this);
    }

	private GColor color = GColor.BLACK;

    public UIPlayer() {}

    public UIPlayer(GColor color) {
        this.color = color;
    }

	public void setColor(GColor color) {
		this.color = color;
	}
	
	public GColor getColor() {
		return color;
	}

    public String getName() {
        if (connection != null && connection.isConnected()) {
            return connection.getName();
        }
        return "Player " + getPlayerNum();
    }

    public boolean isInfoVisible() {
        return isNeutralPlayer() || UISOC.getInstance().isAITuningEnabled();
    }

	@Override
    @Keep
	public Integer chooseVertex(SOC soc, Collection<Integer> vertexIndices, VertexChoice mode, Integer knightToMove) {
		Integer vIndex = null;
		if (connection != null && connection.isConnected()) {
		    vIndex = connection.executeDerivedOnRemote(NetCommon.USER_ID, true, vertexIndices, mode, knightToMove);
        } else {
		    vIndex = super.chooseVertex(soc, vertexIndices, mode, knightToMove);
        }
		return vIndex;
	}

    @Override
    @Keep
    public MoveType chooseMove(SOC soc, Collection<MoveType> moves) {
	    MoveType mv = null;
	    if (connection != null && connection.isConnected()) {
	        mv = connection.executeDerivedOnRemote(NetCommon.USER_ID, true, moves);
        } else {
            mv = super.chooseMove(soc, moves);
        }
        return mv;
    }

    @Override
    @Keep
    public RouteChoiceType chooseRouteType(SOC soc) {
	    RouteChoiceType rt = null;
        if (connection != null && connection.isConnected()) {
            rt = connection.executeDerivedOnRemote(NetCommon.USER_ID, true);
        } else {
            rt = super.chooseRouteType(soc);
        }
        return rt;
    }

    @Override
    @Keep
    public Integer chooseTile(SOC soc, Collection<Integer> tileIndices, TileChoice mode) {
	    Integer tIndex = null;
        if (connection != null && connection.isConnected()) {
            tIndex = connection.executeDerivedOnRemote(NetCommon.USER_ID, true, tileIndices, mode);
        } else {
            tIndex = super.chooseTile(soc, tileIndices, mode);
        }
        return tIndex;
    }

    @Override
    @Keep
    public Trade chooseTradeOption(SOC soc, Collection<Trade> trades) {
	    Trade trade = null;
        if (connection != null && connection.isConnected()) {
            trade = connection.executeDerivedOnRemote(NetCommon.USER_ID, true, trades);
        } else {
            trade = super.chooseTradeOption(soc, trades);
        }
        return trade;
    }

    @Override
    @Keep
    public Integer choosePlayer(SOC soc, Collection<Integer> playerOptions, PlayerChoice mode) {
	    Integer player = null;
        if (connection != null && connection.isConnected()) {
            player = connection.executeDerivedOnRemote(NetCommon.USER_ID, true, playerOptions, mode);
        } else {
            player = super.choosePlayer(soc, playerOptions, mode);
        }
        return player;
    }

    @Override
    @Keep
    public Card chooseCard(SOC soc, Collection<Card> cards, CardChoice mode) {
	    Card card = null;
        if (connection != null && connection.isConnected()) {
            card = connection.executeDerivedOnRemote(NetCommon.USER_ID, true, cards, mode);
        } else {
            card = super.chooseCard(soc, cards, mode);
        }
        return card;
    }

    @Override
    @Keep
    public <T extends Enum<T>> T chooseEnum(SOC soc, EnumChoice mode, T[] values) {
	    T e = null;
        if (connection != null && connection.isConnected()) {
            e = connection.executeDerivedOnRemote(NetCommon.USER_ID, true, mode, values);
        } else {
            e = super.chooseEnum(soc, mode, values);
        }
        return e;
    }

    @Override
    @Keep
    public boolean setDice(SOC soc, List<Dice> die, int num) {
	    if (connection != null && connection.isConnected()) {
	        Boolean result = connection.executeDerivedOnRemote(NetCommon.USER_ID, true, die, num);
	        if (result != null && result) {
	            return true;
            }
            return false;
        }
        return super.setDice(soc, die, num);
    }

	@Override
    @Keep
	public Integer chooseRoute(SOC soc, Collection<Integer> routeIndices, RouteChoice mode, Integer shipToMove) {
	    Integer route = null;
	    if (connection != null && connection.isConnected()) {
	        route = connection.executeDerivedOnRemote(NetCommon.USER_ID, true, routeIndices, mode, shipToMove);
        } else {
            route = super.chooseRoute(soc, routeIndices, mode, shipToMove);
        }
		return route;
	}
	
	@Override
	protected void onBoardChanged() {
        if (UISOC.getInstance() == null)
            return;
        final UIBoardRenderer bc = UISOC.getInstance().getUIBoard();
		bc.getComponent().redraw();
        Utils.waitNoThrow(bc, 100);
//        synchronized (this) {
//            notify(); // notify anyone waiting on this (see spinner)
//        }
	}

	@Override
	protected BotNode onOptimalPath(BotNode optimal, List<BotNode> leafs) {
        if (UISOC.getInstance() == null) {
            return super.onOptimalPath(optimal, leafs);
        }
		return UISOC.getInstance().chooseOptimalPath(optimal, leafs);
	}

    @Override
    public void onCommand(ClientConnection c, GameCommand cmd) {
    }

    @Override
    public void onDisconnected(ClientConnection c, String reason) {

    }

    @Override
    public void onConnected(ClientConnection c) {
    }

    @Override
    public void onCancelled(ClientConnection c, String id) {
    }
}
