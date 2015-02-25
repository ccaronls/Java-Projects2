package cc.game.soc.nety;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import cc.game.soc.core.SOCCell;
import cc.game.soc.core.SOCEdge;
import cc.game.soc.core.GiveUpCardOption;
import cc.game.soc.core.MoveType;
import cc.game.soc.core.SOCPlayer;
import cc.game.soc.core.ResourceType;
import cc.game.soc.core.SOC;
import cc.game.soc.core.SOCTrade;
import cc.game.soc.core.SOCVertex;

/**
 * Class for accepting commands that require waiting for input from user.
 * These are generally the CMD_CHOOSE_* commands
 * 
 * @author Chris Caron
 *
 */
public class ClientInput implements Runnable {

    private final static Logger log = Logger.getLogger(ClientInput.class);

	private boolean running = false;
	
	private List<Command> queue = Collections.synchronizedList(new ArrayList<Command>());
	private SOCPlayer player;
	private Client client;
	private ClientConnection conn;
	private SOC soc;
	private Command curCommand;
	
	ClientInput(SOCPlayer player, Client client, ClientConnection conn, SOC soc) {
		this.player = player;
		this.client = client;
		this.conn = conn;
		this.soc = soc;
		new Thread(this).start();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		log.debug("Entering ClientInput Thread");
		running = true;
		while (running) {

			try {
				if (curCommand == null && queue.size() == 0) {
					synchronized (this) {
						wait();
					}
				}
				
				if (curCommand == null && queue.size() > 0) {
					curCommand = queue.get(0);
					queue.remove(0);
				}
				
				if (curCommand != null) {
					if (waitForInput(curCommand))
						curCommand = null;
				}
				
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		log.debug("Exiting ClientInput Thread");
	}

	void stop() {
		running = false;
		synchronized (this) {
			notify();
		}
	}
	
	void queueCommand(Command cmd) {
	    log.debug("queue cmd: " + cmd);
		queue.add(cmd);
		synchronized (this) {
			notify();
		}
	}
	
	// return true when the command has been handled
	private boolean waitForInput(Command cmd) throws Exception {
		log.debug("Waiting on input for command '" + cmd + "'");
		switch (cmd.getType()) {
		case CMD_ROLL_DICE:
			if (player.rollDice(soc)) {
				conn.setRequest(getRollDiceRequest());
				return true;
			}
			break;		
        case CMD_CHOOSE_COLOR:
            /*
        	client.setColorChoices(Protocol.decodeColors(cmd.getData()));
        	String str = client.chooseColor();
        	if (str != null) {
        		conn.setRequest(getChooseColorRequest(str));
        		return true;
        	}*/
        	break;
        case CMD_CHOOSE_MOVE:
        	MoveType move = player.chooseMove(soc, Protocol.decodeMoveList(cmd.getData()));
        	if (move != null) {
        		return processMove(move);
        	}
        	break;
        case CMD_CHOOSE_CARDS_FOR_GIVEUP:
        	int numCardsToGiveUp = Protocol.decodeGiveUpCardAmount(cmd.getData());
        	GiveUpCardOption [] cards = new GiveUpCardOption[numCardsToGiveUp];
        	while (numCardsToGiveUp > 0) {
        		GiveUpCardOption op = player.chooseCardToGiveUp(soc, SOC.computeGiveUpCardOptions(player), numCardsToGiveUp);
        		if (op == null) {
        			break;
        		}
        		cards[--numCardsToGiveUp] = op;
        	}
        	if (numCardsToGiveUp == 0) {
        		conn.setRequest(getGiveUpCardsRequest(cards));
        		return true;
        	}
        	break;
        case CMD_CHOOSE_ROBBER_CELL:
        	SOCCell cell = player.chooseRobberCell(soc, Protocol.decodeIntList(cmd.getData()));
        	if (cell != null) {
                soc.getBoard().setRobber(soc.getBoard().getCellIndex(cell));
                List<SOCPlayer> options = soc.computeTakeOpponentCardOptions(player, soc.getBoard());
                int playerNum = 0;
                if (options.size() > 0) {
                    SOCPlayer p = player.choosePlayerNumToTakeCardFrom(soc, options);
                    if (p != null)
                        playerNum = p.getPlayerNum();
                }
                conn.setRequest(getRobberRequest(cell, playerNum));
                return true;
        	} 
        	conn.setRequest(getCancelRequest());
        	return true;
            
        case CMD_CHOOSE_SETTLEMENT:
            SOCVertex v = player.chooseSettlementVertex(soc, Protocol.decodeIntList(cmd.getData()));
            if (v != null) {
                conn.setRequest(getSetSettlementRequest(v));
                return true;
            }
            break;
        
        case CMD_CHOOSE_ROAD:
            SOCEdge e = player.chooseRoadEdge(soc, Protocol.decodeIntList(cmd.getData()));
            if (e != null) {
                conn.setRequest(getSetRoadRequest(e));
                return true;
            }
            break;

        default:
        	assert(false); // unhandled
		}
		
		return false;
	}
	
	boolean processMove(MoveType type) {
		switch (type) {
		// Build a Road
		case BUILD_ROAD:
			SOCEdge e = player.chooseRoadEdge(soc, SOC.computeRoadOptions(player, soc.getBoard()));
			if (e != null) {
				conn.setRequest(getBuildRoadRequest(e));
				return true;
			}
			break;
		case BUILD_SETTLEMENT:
			SOCVertex v = player.chooseSettlementVertex(soc, SOC.computeSettlementOptions(player, soc.getBoard()));
			if (v != null) {
				conn.setRequest(getBuildSettlementRequest(v));
				return true;
			}
			break;
		case BUILD_CITY:
            v = player.chooseCityVertex(soc, SOC.computeCityOptions(player, soc.getBoard()));
            if (v != null) {
                conn.setRequest(getBuildCityRequest(v));
                return true;
            }
            break;
		case DRAW_DEVELOPMENT:
            conn.setRequest(getDrawDevelopmentRequest());
            return true;
		case MONOPOLY_CARD:
            ResourceType r = player.chooseResource(soc);
            if (r != null) {
                conn.setRequest(getMonopolyRequest(r));
                return true;
            }
            break;
		case YEAR_OF_PLENTY_CARD:
            r = player.chooseResource(soc);
            if (r != null) {
                ResourceType r2 = player.chooseResource(soc);
                if (r2 != null) {
                    conn.setRequest(getYearOfPlentryRequest(r, r2));
                    return true;
                }
            }
            break;
		case ROAD_BUILDING_CARD:
            e = player.chooseRoadEdge(soc, SOC.computeRoadOptions(player, soc.getBoard()));
            if (e != null) {
                soc.getBoard().setPlayerForEdge(e, player.getPlayerNum());
                SOCEdge e2 = player.chooseRoadEdge(soc, SOC.computeRoadOptions(player, soc.getBoard()));
                soc.getBoard().setPlayerForEdge(e, 0);
                if (e2 != null) {
                    conn.setRequest(getRoadBuildingRequest(e, e2));
                    return true;
                }
            }
            break;
		case VICTORY_CARD:
            conn.setRequest(getVictoryCardRequest());
            return true;
		case SOLDIER_CARD:
            SOCCell cell = player.chooseRobberCell(soc, SOC.computeRobberOptions(soc.getBoard()));
            if (cell != null) {
                soc.getBoard().setRobber(soc.getBoard().getCellIndex(cell));
                List<SOCPlayer> options = soc.computeTakeOpponentCardOptions(player, soc.getBoard());
                if (options.size() > 0) {
                	SOCPlayer p = player.choosePlayerNumToTakeCardFrom(soc, options);
                	if (player != null) {
                		conn.setRequest(getSoldierRequest(cell, p.getPlayerNum()));
                	}
                } else {
                    conn.setRequest(getSoldierRequest(cell, 0));
                }
                return true;
            }
            break;
		case CONTINUE:
            conn.setRequest(getContinueRequest());
            return true;
		case TRADE:
            SOCTrade t = player.chooseTradeOption(soc, SOC.computeTradeOptions(player, soc.getBoard()));
            if (t != null) {
            	r = player.chooseResource(soc);
            	if (r != null) {
            		conn.setRequest(getTradeRequest(t, r));
            		return true;
            	}
            }
            break;
		}
		
		return false;
	}

	private Request getSetRoadRequest(SOCEdge e) {
		return new Request(RequestType.REQ_SET_ROAD, conn.getClientName(), soc.getBoard().getEdgeIndex(e));
	}

    private Request getBuildRoadRequest(SOCEdge e) {
		return new Request(RequestType.REQ_BUILD_ROAD, conn.getClientName(), soc.getBoard().getEdgeIndex(e)); 
	}

    private Request getSetSettlementRequest(SOCVertex v) {
		return new Request(RequestType.REQ_SET_SETTLEMENT, conn.getClientName(), soc.getBoard().getVertexIndex(v));
	}

    private Request getBuildSettlementRequest(SOCVertex v) {
		return new Request(RequestType.REQ_BUILD_SETTLEMENT, conn.getClientName(), soc.getBoard().getVertexIndex(v));
	}

    private Request getBuildCityRequest(SOCVertex v) {
		return new Request(RequestType.REQ_BUILD_CITY, conn.getClientName(), soc.getBoard().getVertexIndex(v));
    }
    
    private Request getDrawDevelopmentRequest() {
        return new Request(RequestType.REQ_DRAW_DEVELOPMENT, conn.getClientName());
    }
    
    private Request getMonopolyRequest(ResourceType t) {
        return new Request(RequestType.REQ_MONOPOLY, conn.getClientName(), t.ordinal());
    }
    
    private Request getYearOfPlentryRequest(ResourceType r1, ResourceType r2) {
    	List<Integer> list = new ArrayList<Integer>();
    	list.add(r1.ordinal());
    	list.add(r2.ordinal());
        return new Request(RequestType.REQ_YEAR_OF_PLENTY, conn.getClientName(), Protocol.encodeIntList(list));
    }
    
    private Request getRoadBuildingRequest(SOCEdge e1, SOCEdge e2) {
    	int E1 = soc.getBoard().getEdgeIndex(e1);
    	int E2 = soc.getBoard().getEdgeIndex(e2);
    	List<Integer> list = new ArrayList<Integer>();
    	list.add(E1); list.add(E2);
        return new Request(RequestType.REQ_ROAD_BUILDING, conn.getClientName(), Protocol.encodeIntList(list));
    }
    
    private Request getVictoryCardRequest() {
        return new Request(RequestType.REQ_VICTORY, conn.getClientName());
    }
    
    private Request getSoldierRequest(SOCCell cell, int playerNum) {
    	List<Integer> list = new ArrayList<Integer>();
        int cellIndex = soc.getBoard().getCellIndex(cell);
    	list.add(cellIndex);
    	list.add(playerNum);
    	return new Request(RequestType.REQ_SOLDIER, conn.getClientName(), Protocol.encodeIntList(list));
    }
    
    private Request getRobberRequest(SOCCell cell, int playerNum) {
        List<Integer> list = new ArrayList<Integer>();
        list.add(soc.getBoard().getCellIndex(cell));
        list.add(playerNum);
        return new Request(RequestType.REQ_ROBBER, conn.getClientName(), Protocol.encodeIntList(list));
    }
    
    private Request getContinueRequest() {
        return new Request(RequestType.REQ_CONTINUE, conn.getClientName());
    }
    
    private Request getTradeRequest(SOCTrade t, ResourceType r) {
        return new Request(RequestType.REQ_TRADE, conn.getClientName(), Protocol.encodeTrade(t, r));
    }
    
    private Request getChooseColorRequest(String str) {
    	return new Request(RequestType.REQ_COLOR, conn.getClientName(), str);
    }
    
    private Request getRollDiceRequest() {
    	return new Request(RequestType.REQ_ROLL_DICE, conn.getClientName());
    }
    
    private Request getGiveUpCardsRequest(GiveUpCardOption [] options) {
    	return new Request(RequestType.REQ_GIVE_UP_CARD, conn.getClientName(), Protocol.encodeGiveUpCardInfo(options.length, Arrays.asList(options)));
    }
    
    private Request getCancelRequest() {
    	return new Request(RequestType.REQ_CANCEL, conn.getClientName());
    }
	
    boolean canCancel() {
    	if (curCommand == null)
    		return false;
    	
    	switch (curCommand.getType()) {
    	case CMD_CHOOSE_ROBBER_CELL:
    	case CMD_CHOOSE_SETTLEMENT:
    	case CMD_CHOOSE_ROAD:
    		return true;
    	}
    	
    	return false;
    }
}
