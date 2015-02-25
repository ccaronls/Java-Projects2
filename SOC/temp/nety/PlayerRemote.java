package cc.game.soc.nety;

import java.awt.Color;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import cc.game.soc.core.*;

public class PlayerRemote extends PlayerNet implements Runnable {

    private final static Logger log = Logger.getLogger(PlayerRemote.class);

    private Socket conn;

    private DataInputStream in;

    private DataOutputStream out;

    private SOCPlayer playerWhenDisconnected;

    private boolean connected = false;
    
    private boolean confirmed = false;

    private boolean processMove = false;
    
    // list of command to send to the client on the next ping
    private List<Command> outCommands = new ArrayList<Command>();
    
    // list of moves returned by the client
    private List<PlayerMove> inMoves = new ArrayList<PlayerMove>();
    
    PlayerRemote(SOCPlayer playerWhenDisconnected, SOCServer server) throws IOException {
    	super(server);
        // set the timout to something reasonable so that if
        // we stop hearing from the player, then we revert to
        // the behavior of the default player.
        this.playerWhenDisconnected = playerWhenDisconnected;
    }
    
    /**
     * 
     * @param conn
     * @param in
     * @param out
     */
    void setConnection(Socket conn, DataInputStream in, DataOutputStream out) {
        this.conn = conn;
        this.in = in;
        this.out = out;
        connected = true;
        new Thread(this).start();
    }

    /*
     *  (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        // TODO Auto-generated method stub
    	log.debug("Entering PlayerRemote thread");
        while (connected) {
            try {
                Request req = new Request(in);
                Response rsp = processRequest(req);
                rsp.write(out);
                if (req.getType() != RequestType.REQ_PING) {
                	synchronized (this) {
                		notify();
                	}
                }
            } catch (Exception e) {
            	e.printStackTrace();
            	System.err.flush();
                connected = false;
            }
        }
        log.debug("Disconnecting ...");
        try {
            in.close();
            out.flush();
            out.close();
            conn.close();
        } catch (Exception e) {
        	e.printStackTrace();
        	System.err.flush();
        }
        log.debug("DISCONNECTED");
    	log.debug("Exiting PlayerRemote thread");
        synchronized (this) {
            notify();
        }
        getServer().playerDisconnected(getPlayerNum());
    }

    /*
     * 
     */
    private Response processRequest(Request req) throws ProtocolException {
    	if (req.getType() != RequestType.REQ_PING)
    	    log.debug("Processing request: " + req);
    	processMove = false;
    	switch (req.getType()) {
    	case REQ_DISCONNECT:
    		connected = false;
            break;
    	case REQ_PING:
    		break;
    	case REQ_COLOR:
			Color color = getServer().useColor(req.getData());
    		if (color != null) {
                confirmed = true;
                getServer().newConfirmation(getPlayerNum());
    			getServer().broadcastCommand(new Command(CommandType.CMD_SET_COLOR, Protocol.encodeColor(color)), getPlayerNum());
    		} else {
    		    log.debug("Color '" + req.getData() + "' is not available");
                addCommand(new Command(CommandType.CMD_CHOOSE_COLOR, Protocol.encodeColors(getServer().getAvailableColors())));
            }
    		break;
  		// notify requests
    	case REQ_SET_ROAD:
    		processMove = true;
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_PLACE_ROAD, Protocol.decodeInt(req.getData())));
    		break;    		
    	case REQ_BUILD_ROAD:
    		processMove = true;
            inMoves.add(new PlayerMove(PlayerMoveType.MT_MOVE, MoveType.BUILD_ROAD.ordinal()));
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_PLACE_ROAD, Protocol.decodeInt(req.getData())));
    		break;    		
    	case REQ_SET_SETTLEMENT:
    		processMove = true;
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_PLACE_SETTLEMENT, Protocol.decodeInt(req.getData())));
    		break;
    	case REQ_BUILD_SETTLEMENT:
    		processMove = true;
            inMoves.add(new PlayerMove(PlayerMoveType.MT_MOVE, MoveType.BUILD_SETTLEMENT.ordinal()));
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_PLACE_SETTLEMENT, Protocol.decodeInt(req.getData())));
    		break;
    	case REQ_BUILD_CITY:
    		processMove = true;
            inMoves.add(new PlayerMove(PlayerMoveType.MT_MOVE, MoveType.BUILD_CITY.ordinal()));
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_PLACE_CITY, Protocol.decodeInt(req.getData())));
    		break;
    	case REQ_DRAW_DEVELOPMENT:
    		processMove = true;
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_MOVE, MoveType.DRAW_DEVELOPMENT.ordinal()));
    		break;
    	case REQ_MONOPOLY:
    		processMove = true;
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_MOVE, MoveType.MONOPOLY_CARD.ordinal()));
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_CHOOSE_RESOURCE, Protocol.decodeInt(req.getData())));
    		break;
    	case REQ_YEAR_OF_PLENTY:
    		processMove = true;
    		List<Integer> list = Protocol.decodeIntList(req.getData());
    		if (list.size() != 2)
    			throw new ProtocolException("Invalid YOP list '" + req.getData() + "', expected 2 entries but found " + list.size());
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_MOVE, MoveType.YEAR_OF_PLENTY_CARD.ordinal()));
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_CHOOSE_RESOURCE, list.get(0)));
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_CHOOSE_RESOURCE, list.get(1)));
    		break;
    	case REQ_ROAD_BUILDING:
    		processMove = true;
    		list = Protocol.decodeIntList(req.getData());
    		if (list.size() != 2)
    			throw new ProtocolException("Invalid RoadBuilding list '" + req.getData() + "', expected 2 entries but found " + list.size());
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_MOVE, MoveType.ROAD_BUILDING_CARD.ordinal()));
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_PLACE_ROAD, list.get(0)));
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_PLACE_ROAD, list.get(1)));
    		break;
    	case REQ_VICTORY:
    		this.useDevelopmentCard(DevelopmentCardType.Victory);
    		break;
    	case REQ_SOLDIER:
    		processMove = true;
    		list = Protocol.decodeIntList(req.getData());
    		if (list.size() != 2)
    			throw new ProtocolException("Invalid RoadBuilding list '" + req.getData() + "', expected 2 entries but found " + list.size());
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_MOVE, MoveType.SOLDIER_CARD.ordinal()));
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_PLACE_ROBBER, list.get(0)));
            int playerNum = list.get(1);
            if (playerNum > 0) {
                inMoves.add(new PlayerMove(PlayerMoveType.MT_TAKE_CARD_FROM_PLAYER, playerNum));
            }
    		break;
        case REQ_ROBBER:
            processMove = true;
            list = Protocol.decodeIntList(req.getData());
            inMoves.add(new PlayerMove(PlayerMoveType.MT_PLACE_ROBBER, list.get(0)));
            playerNum = list.get(1);
            if (playerNum > 0) {
                inMoves.add(new PlayerMove(PlayerMoveType.MT_TAKE_CARD_FROM_PLAYER, playerNum));
            }
            break;    		
    	case REQ_CONTINUE:
    		processMove = true;
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_MOVE, MoveType.CONTINUE.ordinal()));
    		break;
    	case REQ_TRADE:
    		processMove = true;
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_MOVE, MoveType.TRADE.ordinal()));
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_TRADE, Protocol.decodeTrade(req.getData())));
    		inMoves.add(new PlayerMove(PlayerMoveType.MT_CHOOSE_RESOURCE, Protocol.decodeTradeResource(req.getData()).ordinal()));
    		break;
    	case REQ_ROLL_DICE:
    		processMove = true;
    		break;
    	case REQ_GIVE_UP_CARD:
    		List<GiveUpCardOption> options = Protocol.decodeGiveUpCardList(req.getData());
    		Iterator<GiveUpCardOption> it = options.iterator();
    		while (it.hasNext()) {
    			inMoves.add(new PlayerMove(PlayerMoveType.MT_GIVEUP_CARD, it.next().ordinal()));
    		}
    		break;
    	case REQ_CANCEL:
    		getServer().cancel();
    		break;

    	default:
    		outCommands.clear();
    		return new Response(req.getType(), ResponseStatus.RSP_ERR_INVALID_REQUEST);    			
    	}
        Response rsp = new Response(req.getType(), ResponseStatus.RSP_OK);
    	rsp.getCommands().addAll(outCommands);
    	outCommands.clear();
    	return rsp;
    }

    /**
     * 
     * @param c
     */
    void addCommand(Command c) {
        if (c.getPlayerNum() == 0)
            c.setPlayerNum(getPlayerNum());
        log.debug("Adding outbound command : " + c);
        synchronized (outCommands) {
            outCommands.add(c);
        }
    }
    
    /**
     * 
     * @return
     */
    boolean waitForRequest() {
        try {
            synchronized (this) {
                wait();
            }
        } catch (Exception e) {}
        return processMove;
    }
    
    /**
     * 
     * @param expected
     * @return
     */
    PlayerMove getMove(PlayerMoveType expected) {
        log.debug("getMove '" + expected + "', actual=" + inMoves);
        PlayerMove move = inMoves.get(0);
        inMoves.remove(0);
        assert(move.getType() == expected);
        return move;
    }
    
    /**
     * 
     * @return
     */
    boolean isConnected() {
        return connected;
    }
    
    /**
     * Return tre when this player is connected and choosen color is confirmed. 
     * @return
     */
    boolean isConfirmed() {
        return confirmed;
    }
    
    @Override
    public GiveUpCardOption chooseCardToGiveUp(SOC soc, List<GiveUpCardOption> options, int numCardsToGiveUp) {
        if (!connected)
            return playerWhenDisconnected.chooseCardToGiveUp(soc, options, numCardsToGiveUp);
        if (inMoves.size() == 0) {
            int numToGiveUp = this.getTotalCardsLeftInHand() / 2;
            addCommand(new Command(CommandType.CMD_CHOOSE_CARDS_FOR_GIVEUP, Protocol.encodeGiveUpCardInfo(numToGiveUp, options)));
            if (!waitForRequest())
            	return null;
        }
        return GiveUpCardOption.values()[getMove(PlayerMoveType.MT_GIVEUP_CARD).getIndex()];
    }

    @Override
    public SOCVertex chooseCityVertex(SOC soc, List<Integer> vertexIndices) {
        if (!connected)
            return playerWhenDisconnected.chooseCityVertex(soc, vertexIndices);
        int index = getMove(PlayerMoveType.MT_PLACE_CITY).getIndex();
        getServer().broadcastCommand(new Command(CommandType.CMD_SET_CITY, index), getPlayerNum());
        return soc.getBoard().getVertex(index);
    }

    @Override
    public MoveType chooseMove(SOC soc, List<MoveType> moves) {
        if (!connected)
            return playerWhenDisconnected.chooseMove(soc, moves);
        if (inMoves.size() == 0) {
            addCommand(new Command(CommandType.CMD_CHOOSE_MOVE, Protocol.encodeMoveList(moves)));
            if (!waitForRequest())
            	return null;
        }
        return MoveType.values()[getMove(PlayerMoveType.MT_MOVE).getIndex()];
    }

    @Override
    public SOCPlayer choosePlayerNumToTakeCardFrom(SOC soc, List<SOCPlayer> playerOptions) {
        if (!connected)
            return playerWhenDisconnected.choosePlayerNumToTakeCardFrom(soc, playerOptions);
        return soc.getPlayerByPlayerNum(getMove(PlayerMoveType.MT_TAKE_CARD_FROM_PLAYER).getIndex());
    }
    @Override
    public ResourceType chooseResource(SOC soc) {
        if (!connected)
            return playerWhenDisconnected.chooseResource(soc);
        // TODO Auto-generated method stub
        return ResourceType.values()[getMove(PlayerMoveType.MT_CHOOSE_RESOURCE).getIndex()];
    }

    @Override
    public SOCEdge chooseRoadEdge(SOC soc, List<Integer> edgeIndices) {
        if (!connected)
            return playerWhenDisconnected.chooseRoadEdge(soc, edgeIndices);
        // TODO Auto-generated method stub
        if (inMoves.size() == 0) {
            addCommand(new Command(CommandType.CMD_CHOOSE_ROAD, Protocol.encodeIntList(edgeIndices)));
            if (!waitForRequest())
                return null;
        }
        int index = getMove(PlayerMoveType.MT_PLACE_ROAD).getIndex();
        getServer().broadcastCommand(new Command(CommandType.CMD_SET_ROAD, index), getPlayerNum());
        return soc.getBoard().getEdge(index);
    }

    @Override
    public SOCCell chooseRobberCell(SOC soc, List<Integer> cellIndices) {
        if (!connected)
            return playerWhenDisconnected.chooseRobberCell(soc, cellIndices);
        // TODO Auto-generated method stub
        if (inMoves.size() == 0) {
        	addCommand(new Command(CommandType.CMD_CHOOSE_ROBBER_CELL, Protocol.encodeIntList(cellIndices)));
            if (!waitForRequest())
            	return null;
        }
        int cellIndex = getMove(PlayerMoveType.MT_PLACE_ROBBER).getIndex();
        if (cellIndex < 0) {
            soc.cancel();
            return null;
        }
        getServer().broadcastCommand(new Command(CommandType.CMD_SET_ROBBER, cellIndex), getPlayerNum());
        return soc.getBoard().getCell(cellIndex);
    }

    @Override
    public SOCVertex chooseSettlementVertex(SOC soc, List<Integer> vertexIndices) {
        if (!connected)
            return playerWhenDisconnected.chooseSettlementVertex(soc, vertexIndices);
        if (inMoves.size() == 0) {
            addCommand(new Command(CommandType.CMD_CHOOSE_SETTLEMENT, Protocol.encodeIntList(vertexIndices)));
            if (!waitForRequest())
            	return null;
        }
        // TODO Auto-generated method stub
        int index = getMove(PlayerMoveType.MT_PLACE_SETTLEMENT).getIndex();
        getServer().broadcastCommand(new Command(CommandType.CMD_SET_SETTLEMENT, index), getPlayerNum());
        return soc.getBoard().getVertex(index);
    }

    @Override
    public SOCTrade chooseTradeOption(SOC soc, List<SOCTrade> trades) {
        if (!connected)
            return playerWhenDisconnected.chooseTradeOption(soc, trades);
        // TODO Auto-generated method stub
        return getMove(PlayerMoveType.MT_TRADE).getTrade();
    }

    @Override
    public boolean rollDice(SOC soc) {
        if (!connected)
            return playerWhenDisconnected.rollDice(soc);
        addCommand(new Command(CommandType.CMD_ROLL_DICE));
        waitForRequest();
        return true;
    }

    @Override
    public void addDevelopmentCard(DevelopmentCardType type, int flag) {
        // TODO Auto-generated method stub
        super.addDevelopmentCard(type, flag);
        playerWhenDisconnected.addDevelopmentCard(type, flag);
        //addCommand(new Command(CommandType.CMD_DEVEL_CARD, Protocol.encodeDevelCard(type, flag, true)));
    }

    @Override
    public void incrementResource(ResourceType type, int amount) {
        // TODO Auto-generated method stub
        super.incrementResource(type, amount);
        playerWhenDisconnected.incrementResource(type, amount);
        //addCommand(new Command(CommandType.CMD_SET_RESOURCE_COUNT, Protocol.encodeResourceCount(type, this.getResourceCount(type))));
    }

    @Override
    public void removeDevelopmentCard(DevelopmentCardType type, int flag) {
        // TODO Auto-generated method stub
        super.removeDevelopmentCard(type, flag);
        playerWhenDisconnected.removeDevelopmentCard(type, flag);
        //addCommand(new Command(CommandType.CMD_DEVEL_CARD, Protocol.encodeDevelCard(type, flag, false)));
    }

    @Override
    public void setDevelopmentCardsUsable() {
        // TODO Auto-generated method stub
        super.setDevelopmentCardsUsable();
        playerWhenDisconnected.setDevelopmentCardsUsable();
        addCommand(new Command(CommandType.CMD_SET_DEVEL_CARDS_USABLE));        
    }

    @Override
    public void setRoadLength(int len) {
        // TODO Auto-generated method stub
        super.setRoadLength(len);
        playerWhenDisconnected.setRoadLength(len);
    }
    
    @Override
    public void setPoints(int points) {
        super.setPoints(points);
        playerWhenDisconnected.setPoints(points);
    }
}
