package cc.game.soc.netz;

import java.util.Arrays;
import java.util.List;

import cc.game.soc.ai.PlayerTemp;
import cc.game.soc.core.GiveUpCardOption;
import cc.game.soc.core.MoveType;
import cc.game.soc.core.SOC;
import cc.game.soc.core.SOCBoard;
import cc.game.soc.core.SOCCell;
import cc.game.soc.core.SOCEdge;
import cc.game.soc.core.SOCPlayer;
import cc.game.soc.core.SOCTrade;
import cc.game.soc.core.SOCVertex;
import cc.lib.net.ClientForm;
import cc.lib.net.GameClient;
import cc.lib.net.GameCommand;

public class SOCClient extends GameClient {

    final SOCPlayer player;
    SOC soc = new SOC() {

        @Override
        protected SOCPlayer instantiatePlayer(String className) throws Exception {
            return new PlayerTemp();
        }

        
    };
    
    public SOC getSOC() {
        return this.soc;
    }
    
    public SOCClient(String userName, SOCPlayer player) {
        super(userName, SOCProtocol.VERSION);
        this.player = player;
    }

    void onChooseSettlement(List<Integer> vertexIndices) {
        SOCVertex vertex = player.chooseSettlementVertex(soc, vertexIndices);
        if (vertex != null)
            this.send(SOCProtocol.clSetSettlement(soc.getBoard().getVertexIndex(vertex)));
    }

    void onChooseRoad(List<Integer> edgeIndices) {
        SOCEdge edge = player.chooseRoadEdge(soc, edgeIndices);
        if (edge != null)
            this.send(SOCProtocol.clSetRoad(soc.getBoard().getEdgeIndex(edge)));
    }

    void onChooseCity(List<Integer> vertexIndices) {
        SOCVertex vertex = player.chooseCityVertex(soc, vertexIndices);
        if (vertex != null)
            this.send(SOCProtocol.clSetCity(soc.getBoard().getVertexIndex(vertex)));
    }

    void onChooseRobberCell(List<Integer> cellIndices) {
        SOCCell cell = player.chooseRobberCell(soc, cellIndices);
        if (cell != null)
            this.send(SOCProtocol.clSetRobberCell(soc.getBoard().getCellIndex(cell)));
    }

    void onRollDice() {
        if (player.rollDice(soc))
            this.send(SOCProtocol.clRollDice());
    }

    void onChooseTrade(List<SOCTrade> tradesList) {
        SOCTrade trade = player.chooseTradeOption(soc, tradesList);
        if (trade != null)
            send(SOCProtocol.clSetTrade(trade));
    }

    void onChoosePlayerToTakeCardFrom(List<Integer>players) {
    }

    void onChooseMove(List<MoveType> moves) {
    }

    void onChooseCardToGiveUp(List<GiveUpCardOption> options, int numCards) {
    }

    void onChooseResource() {
    }

    @Override
    protected void onMessage(String message) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void onDisconnected(String message) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void onConnected() {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void onCommand(GameCommand cmd) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void onForm(ClientForm form) {
        // TODO Auto-generated method stub
        
    }

    public void onBoardUpdated(SOCBoard board) {
        // TODO Auto-generated method stub
        
    }

    public void onGameUpdated(SOC soc2) {
        // TODO Auto-generated method stub
        
    }

    
}
