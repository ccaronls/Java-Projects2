package cc.game.soc.nety;

import java.util.List;

import cc.game.soc.core.SOCCell;
import cc.game.soc.core.SOCEdge;
import cc.game.soc.core.GiveUpCardOption;
import cc.game.soc.core.MoveType;
import cc.game.soc.core.SOCPlayer;
import cc.game.soc.core.ResourceType;
import cc.game.soc.core.SOC;
import cc.game.soc.core.SOCTrade;
import cc.game.soc.core.SOCVertex;
import cc.lib.game.Utils;

public class PlayerNetAI extends PlayerNet {

    PlayerNetAI(SOCServer server) {
        super(server);
    }
    
    /* (non-Javadoc)
     * @see cc.game.soc.core.Player#chooseCardToGiveUp(cc.game.soc.core.SOC, java.util.List)
     */
    @Override
    public GiveUpCardOption chooseCardToGiveUp(SOC sar, List<GiveUpCardOption> options, int numCardsToGiveUp) {
        // TODO Auto-generated method stub
        return options.get(Utils.rand() % options.size());
    }

    /* (non-Javadoc)
     * @see cc.game.soc.core.Player#chooseCityVertex(cc.game.soc.core.SOC, java.util.List)
     */
    @Override
    public SOCVertex chooseCityVertex(SOC sar, List<Integer> options) {
        // TODO Auto-generated method stub
        int index = options.get(Utils.rand() % options.size());
        getServer().broadcastCommand(new Command(CommandType.CMD_SET_CITY, index), getPlayerNum());
        return sar.getBoard().getVertex(index);
    }

    /* (non-Javadoc)
     * @see cc.game.soc.core.Player#chooseMove(cc.game.soc.core.SOC, java.util.List)
     */
    @Override
    public MoveType chooseMove(SOC sar, List<MoveType> options) {
        // TODO Auto-generated method stub
        return options.get(Utils.rand() % options.size());
    }

    /* (non-Javadoc)
     * @see cc.game.soc.core.Player#choosePlayerNumToTakeCardFrom(cc.game.soc.core.SOC, java.util.List)
     */
    @Override
    public SOCPlayer choosePlayerNumToTakeCardFrom(SOC sar, List<SOCPlayer> options) {
        // TODO Auto-generated method stub
        int index = Utils.rand() % options.size();
        return options.get(index);
    }

    /* (non-Javadoc)
     * @see cc.game.soc.core.Player#chooseResource(cc.game.soc.core.SOC)
     */
    @Override
    public ResourceType chooseResource(SOC sar) {
        // TODO Auto-generated method stub
        return ResourceType.values()[Utils.rand() % ResourceType.values().length];
    }

    /* (non-Javadoc)
     * @see cc.game.soc.core.Player#chooseRoadEdge(cc.game.soc.core.SOC, java.util.List)
     */
    @Override
    public SOCEdge chooseRoadEdge(SOC sar, List<Integer> options) {
        // TODO Auto-generated method stub
        int index = options.get(Utils.rand() % options.size());
        getServer().broadcastCommand(new Command(CommandType.CMD_SET_ROAD, index), getPlayerNum());
        return sar.getBoard().getEdge(index);
    }

    /* (non-Javadoc)
     * @see cc.game.soc.core.Player#chooseRobberCell(cc.game.soc.core.SOC, java.util.List)
     */
    @Override
    public SOCCell chooseRobberCell(SOC sar, List<Integer> options) {
        // TODO Auto-generated method stub
        int cellIndex = options.get(Utils.rand() % options.size());
        getServer().broadcastCommand(new Command(CommandType.CMD_SET_ROBBER, cellIndex), getPlayerNum());
        return sar.getBoard().getCell(cellIndex);
    }

    /* (non-Javadoc)
     * @see cc.game.soc.core.Player#chooseSettlementVertex(cc.game.soc.core.SOC, java.util.List)
     */
    @Override
    public SOCVertex chooseSettlementVertex(SOC sar, List<Integer> options) {
        // TODO Auto-generated method stub
        int index = options.get(Utils.rand() % options.size());
        getServer().broadcastCommand(new Command(CommandType.CMD_SET_SETTLEMENT, index), getPlayerNum());
        return sar.getBoard().getVertex(index);
    }

    /* (non-Javadoc)
     * @see cc.game.soc.core.Player#chooseTradeOption(cc.game.soc.core.SOC, java.util.List)
     */
    @Override
    public SOCTrade chooseTradeOption(SOC sar, List<SOCTrade> options) {
        // TODO Auto-generated method stub
        return options.get(Utils.rand() % options.size());
    }

    /* (non-Javadoc)
     * @see cc.game.soc.core.Player#rollDice()
     */
    @Override
    public boolean rollDice(SOC soc) {
        // TODO Auto-generated method stub
        return true;
    }
}
