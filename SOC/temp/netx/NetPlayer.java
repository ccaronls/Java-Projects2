package cc.game.soc.netx;

import java.util.List;
import java.util.Random;

import cc.game.soc.core.SOCCell;
import cc.game.soc.core.SOCEdge;
import cc.game.soc.core.GiveUpCardOption;
import cc.game.soc.core.MoveType;
import cc.game.soc.core.SOCPlayer;
import cc.game.soc.core.SOCPlayerRandom;
import cc.game.soc.core.ResourceType;
import cc.game.soc.core.SOC;
import cc.game.soc.core.SOCTrade;
import cc.game.soc.core.SOCVertex;

class NetPlayer extends SOCPlayerRandom {

	ClientConnection connection;

	NetPlayer() {
	}
	
	void setConnection(ClientConnection connection) {
	    this.connection = connection;
	    connection.player = this;
	}
	
	private Object result;
	
	@SuppressWarnings("unchecked")
	<T> T waitForResult() {
		result = null;
		try {
			synchronized (this) {
				wait();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (T)result;
	}

	void setResult(Object result) {
		this.result = result;
		synchronized (this) {
			notify();
		}
	}

	private void send(Command cmd) {
	    try {
    	    for (int i=0; i<3; i++) {
    	        try {
    	            connection.send(cmd);
    	            return;
    	        } catch (Exception e) {
    	            e.printStackTrace();
    	        }
    	    }
       	    connection.leaveGame();
       	    connection = null;
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	@Override
	public SOCVertex chooseSettlementVertex(SOC soc, List<Integer> vertexIndices) {
	    if (connection == null)
	        return super.chooseSettlementVertex(soc, vertexIndices);
		send(Command.newSrvrChooseSettlementVertex(vertexIndices));
		return waitForResult();
	}

	@Override
	public SOCEdge chooseRoadEdge(SOC soc, List<Integer> edgeIndices) {
	    if (connection == null)
	        return super.chooseRoadEdge(soc, edgeIndices);
		send(Command.newSrvrChooseRoadEdge(edgeIndices));		
		return waitForResult();
	}

	@Override
	public SOCVertex chooseCityVertex(SOC soc, List<Integer> vertexIndices) {
	    if (connection == null)
	        return super.chooseCityVertex(soc, vertexIndices);
		send(Command.newSrvrChooseCityVertex(vertexIndices));
		return waitForResult();
	}

	@Override
	public SOCCell chooseRobberCell(SOC soc, List<Integer> cellIndices) {
	    if (connection == null)
	        return super.chooseRobberCell(soc, cellIndices);
		send(Command.newSrvrChooseRobberCell(cellIndices));
		return waitForResult();
	}

	@Override
	public MoveType chooseMove(SOC soc, List<MoveType> moves) {
	    if (connection == null)
	        return super.chooseMove(soc, moves);
		send(Command.newSrvrChooseMove(moves));
		return waitForResult();
	}

	@Override
	public boolean rollDice(SOC soc) {
	    if (connection == null)
	        return super.rollDice(soc);
		send(Command.newSrvrRollDice());
		return waitForResult();
	}

	@Override
	public SOCTrade chooseTradeOption(SOC soc, List<SOCTrade> trades) {
	    if (connection == null)
	        return super.chooseTradeOption(soc, trades);
		send(Command.newSrvrChooseTradeOption(trades));
		return waitForResult();
	}

	@Override
	public SOCPlayer choosePlayerNumToTakeCardFrom(SOC soc, List<SOCPlayer> playerOptions) {
	    if (connection == null)
	        return super.choosePlayerNumToTakeCardFrom(soc, playerOptions);
		send(Command.newSrvrChoosePlayerToTakeCardFrom(playerOptions));
		return waitForResult();
	}

	@Override
	public GiveUpCardOption chooseCardToGiveUp(SOC soc, List<GiveUpCardOption> options, int numCardsToSurrender) {
	    if (connection == null)
	        return super.chooseCardToGiveUp(soc, options, numCardsToSurrender);
		send(Command.newSrvrChooseCardToGiveUp(options));
		return waitForResult();
	}

	@Override
	public ResourceType chooseResource(SOC soc) {
	    if (connection == null)
	        return super.chooseResource(soc);
		send(Command.newSrvrChooseResource());
		return waitForResult();
	}

    public boolean isConnected() {
        return connection != null && connection.isConnected();
    }


	
}
