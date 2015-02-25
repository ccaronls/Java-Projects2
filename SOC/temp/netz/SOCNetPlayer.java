package cc.game.soc.netz;

import java.util.List;

import cc.game.soc.ai.AIPlayer;
import cc.game.soc.core.*;
import cc.lib.net.ClientConnection;
import cc.lib.net.GameCommand;

class SOCNetPlayer extends AIPlayer {

    ClientConnection connection;
    Object response;
    GameCommand lastCommand = null;
    
    @SuppressWarnings("unchecked")
    private <T> T waitForResponse() {
        response = null;
        try {
            synchronized (this) {
                wait(3000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (T)response;
    }
    
    @Override
    public SOCVertex chooseSettlementVertex(SOC soc, List<Integer> vertexIndices) {
        if (connection == null || !connection.isConnected())
            return super.chooseSettlementVertex(soc, vertexIndices);
        
        lastCommand = SOCProtocol.svrChooseSettlement(soc, vertexIndices);
        connection.sendCommand(lastCommand);
        return waitForResponse();
    }

    @Override
    public SOCEdge chooseRoadEdge(SOC soc, List<Integer> edgeIndices) {
        if (connection == null || !connection.isConnected())
            return super.chooseRoadEdge(soc, edgeIndices);
        lastCommand = SOCProtocol.svrChooseRoadEdge(soc, edgeIndices);
        connection.sendCommand(lastCommand);
        return waitForResponse();
    }

    @Override
    public SOCVertex chooseCityVertex(SOC soc, List<Integer> vertexIndices) {
        if (connection == null || !connection.isConnected())
            return super.chooseCityVertex(soc, vertexIndices);
        lastCommand = SOCProtocol.svrChooseCityVertex(soc, vertexIndices);
        connection.sendCommand(lastCommand);
        return waitForResponse();
    }

    @Override
    public SOCCell chooseRobberCell(SOC soc, List<Integer> cellIndices) {
        if (connection == null || !connection.isConnected())
            return super.chooseRobberCell(soc, cellIndices);
        lastCommand = SOCProtocol.svrChooseRobberCell(soc, cellIndices);
        connection.sendCommand(lastCommand);
        return waitForResponse();
    }

    @Override
    public MoveType chooseMove(SOC soc, List<MoveType> moves) {
        if (connection == null || !connection.isConnected())
            return super.chooseMove(soc, moves);
        lastCommand = SOCProtocol.svrChooseMove(soc, moves);
        connection.sendCommand(lastCommand);
        return waitForResponse();
    }

    @Override
    public boolean rollDice(SOC soc) {
        if (connection == null || !connection.isConnected())
            return super.rollDice(soc);
        lastCommand = SOCProtocol.svrRollDice();
        connection.sendCommand(lastCommand);
        return waitForResponse();
    }

    @Override
    public SOCTrade chooseTradeOption(SOC soc, List<SOCTrade> trades) {
        if (connection == null || !connection.isConnected())
            return super.chooseTradeOption(soc, trades);
        lastCommand = SOCProtocol.svrChooseTradeOption(soc, trades);
        connection.sendCommand(lastCommand);
        return waitForResponse();
    }

    @Override
    public SOCPlayer choosePlayerNumToTakeCardFrom(SOC soc, List<SOCPlayer> playerOptions) {
        if (connection == null || !connection.isConnected())
            return super.choosePlayerNumToTakeCardFrom(soc, playerOptions);
        lastCommand = SOCProtocol.svrChoosePlayerNumToTakeCardFrom(soc, playerOptions);
        connection.sendCommand(lastCommand);
        return waitForResponse();
    }

    @Override
    public GiveUpCardOption chooseCardToGiveUp(SOC soc, List<GiveUpCardOption> options, int numCardsToSurrender) {
        if (connection == null || !connection.isConnected())
            return super.chooseCardToGiveUp(soc, options, numCardsToSurrender);
        lastCommand = SOCProtocol.svrChooseCardToGiveUp(soc, options, numCardsToSurrender);
        connection.sendCommand(lastCommand);
        return waitForResponse();
    }

    @Override
    public ResourceType chooseResource(SOC soc) {
        if (connection == null || !connection.isConnected())
            return super.chooseResource(soc);
        lastCommand = SOCProtocol.svrChooseResource(soc);
        connection.sendCommand(lastCommand);
        return waitForResponse();
    }

    public void resendLastCommand() {
        connection.sendCommand(lastCommand);
    }

    
}
