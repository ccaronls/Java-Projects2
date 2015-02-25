package cc.game.soc.netz;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import cc.game.soc.core.GiveUpCardOption;
import cc.game.soc.core.MoveType;
import cc.game.soc.core.SOC;
import cc.game.soc.core.SOCBoard;
import cc.game.soc.core.SOCPlayer;
import cc.game.soc.core.SOCTrade;
import cc.game.soc.core.SOCVertex;
import cc.lib.net.*;

public class SOCProtocol extends GameCommand {

    public static int SOC_PORT = 11111;
    public static String VERSION = "SOC.1.0";

    private static final GameCommandType SVR_CHOOSE_SETTLEMENT          = new GameCommandType("SVR_CHOOSE_SETTLEMENT");
    private static final GameCommandType SVR_CHOOSE_ROAD                = new GameCommandType("SVR_CHOOSE_ROAD");
    private static final GameCommandType SVR_CHOOSE_CITY                = new GameCommandType("SVR_CHOOSE_CITY");
    private static final GameCommandType SVR_CHOOSE_ROBBER_CELL         = new GameCommandType("SVR_CHOOSE_ROBBER_CELL");
    private static final GameCommandType SVR_ROLL_DICE                  = new GameCommandType("SVR_ROLL_DICE");
    private static final GameCommandType SVR_CHOOSE_TRADE               = new GameCommandType("SVR_CHOOSE_TRADE");
    private static final GameCommandType SVR_CHOOSE_PLAYER_TO_TAKE_CARD = new GameCommandType("SVR_CHOOSE_PLAYER_TO_TAKE_CARD");
    private static final GameCommandType SVR_CHOOSE_MOVE                = new GameCommandType("SVR_CHOOSE_MOVE");
    private static final GameCommandType SVR_CHOOSE_CARD_TO_GIVEUP      = new GameCommandType("SVR_CHOOSE_CARD_TO_GIVEUP");
    private static final GameCommandType SVR_CHOOSE_RESOURCE            = new GameCommandType("SVR_CHOOSE_RESOURCE");
    private static final GameCommandType SVR_UPDATE_BOARD               = new GameCommandType("SVR_UPDATE_BOARD");
    private static final GameCommandType SVR_UPDATE_GAME                = new GameCommandType("SVR_UPDATE_GAME");
    
    private static final GameCommandType CL_SET_SETTLEMENT              = new GameCommandType("CL_SET_SETTLEMENT");
    private static final GameCommandType CL_SET_ROAD                    = new GameCommandType("CL_SET_ROAD");
    private static final GameCommandType CL_SET_CITY                    = new GameCommandType("CL_SET_CITY");
    private static final GameCommandType CL_SET_ROBBER_CELL             = new GameCommandType("CL_SET_ROBBER_CELL");
    private static final GameCommandType CL_ROLL_DICE                   = new GameCommandType("CL_ROLL_DICE");
    private static final GameCommandType CL_SET_TRADE                   = new GameCommandType("CL_SET_TRADE");
    private static final GameCommandType CL_SET_PLAYER_TO_TAKE_CARD     = new GameCommandType("CL_SET_PLAYER_TO_TAKE_CARD");
    private static final GameCommandType CL_SET_MOVE                    = new GameCommandType("CL_SET_MOVE");
    private static final GameCommandType CL_SET_CARD_TO_GIVEUP          = new GameCommandType("CL_SET_CARD_TO_GIVEUP");
    private static final GameCommandType CL_SET_RESOURCE                = new GameCommandType("CL_SET_RESOURCE");
    
    /**
     * Process commands originating from the server
     * 
     * @param cmd
     * @param client
     */
    public static void clientProcess(GameCommand cmd, SOCClient client) throws ProtocolException {
        if (cmd.getType() == SVR_CHOOSE_SETTLEMENT) {
            client.onChooseSettlement(cmd.getIntList("verts"));
        } else if (cmd.getType() == SVR_CHOOSE_ROAD) {
            client.onChooseRoad(cmd.getIntList("edges"));
        } else if (cmd.getType() == SVR_CHOOSE_CITY) {
            client.onChooseCity(cmd.getIntList("verts"));
        } else if (cmd.getType() == SVR_CHOOSE_ROBBER_CELL) {
            client.onChooseRobberCell(cmd.getIntList("cells"));
        } else if (cmd.getType() == SVR_ROLL_DICE) {
            client.onRollDice();
        } else if (cmd.getType() == SVR_CHOOSE_TRADE) {
            String s = cmd.getArg("trades");
            String [] ss = s.split("[, ]+");
            List<SOCTrade> tradesList = new ArrayList();//SOCTrade [] trades = new SOCTrade[ss.length];
            for (int i=0; i<ss.length; i++) {
                ss[i] = ss[i].trim();
                if (ss[i].length() > 0)
                    tradesList.add(new SOCTrade(ss[i]));
            }
            client.onChooseTrade(tradesList);
        } else if (cmd.getType() == SVR_CHOOSE_PLAYER_TO_TAKE_CARD) {
            client.onChoosePlayerToTakeCardFrom(cmd.getIntList("players"));
        } else if (cmd.getType() == SVR_CHOOSE_MOVE) {
            List<MoveType> moves = cmd.getEnumList("moves", MoveType.class);
            client.onChooseMove(moves);
        } else if (cmd.getType() == SVR_CHOOSE_CARD_TO_GIVEUP) {
            int numCards = Integer.parseInt(cmd.getArg("numCardsToSurrender"));
            List<GiveUpCardOption> options = cmd.getEnumList("cards", GiveUpCardOption.class);
            client.onChooseCardToGiveUp(options, numCards);
        } else if (cmd.getType() == SVR_CHOOSE_RESOURCE) {
            client.onChooseResource();
        } else if (cmd.getType() == SVR_UPDATE_BOARD) {
            SOCBoard  board = client.getSOC().getBoard();
            StringReader sr = new StringReader(cmd.getArg("board"));
            try {
                BufferedReader reader = new BufferedReader(sr);
                //board.loadBoard(reader);
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            client.onBoardUpdated(board);
        } else if (cmd.getType() == SVR_UPDATE_GAME) {
            SOC soc = client.getSOC();
            soc.setDice(Integer.parseInt(cmd.getArg("die1")), Integer.parseInt(cmd.getArg("die2")));
            soc.setLargestArmyPlayer(Integer.parseInt(cmd.getArg("largestArmyPlayerNum")));
            soc.setLongestRoadPlayer(Integer.parseInt(cmd.getArg("longestRoadPlayerNum")));
            client.onGameUpdated(soc);
        } else {
            client.logError("Unhandled command: " + cmd);
        }
    }
    
    /** ATT SALES: 877 722 3755 -> "sales" -> "agent"
     * process commands originating from the client
     * 
     * @param conn
     * @param command
     * @param listener
     */
    static void serverProcess(ClientConnection conn, GameCommand command, SOCGameServer server) {
    }
    
    
    private SOCProtocol(GameCommandType commandType) {
        super(commandType);
        super.setVersion(VERSION);
    }
    
    static GameCommand svrChooseSettlement(SOC soc, List<Integer> vertexIndices) {
        return new SOCProtocol(SVR_CHOOSE_SETTLEMENT).setIntList("verts", vertexIndices);
    }
    
    static GameCommand svrChooseRoadEdge(SOC soc, List<Integer> edgeIndices) {
        return new SOCProtocol(SVR_CHOOSE_ROAD).setIntList("edges", edgeIndices);
    }

    static GameCommand svrChooseCityVertex(SOC soc, List<Integer> vertexIndices) {
        return new SOCProtocol(SVR_CHOOSE_CITY).setIntList("verts", vertexIndices);
    }

    static GameCommand svrChooseRobberCell(SOC soc, List<Integer> cellIndices) {
        return new SOCProtocol(SVR_CHOOSE_ROBBER_CELL).setIntList("cells", cellIndices);
    }

    static GameCommand svrRollDice() {
        return new SOCProtocol(SVR_ROLL_DICE);
    }

    static GameCommand svrChooseTradeOption(SOC soc, List<SOCTrade> trades) {
        return new SOCProtocol(SVR_CHOOSE_TRADE).setArg("trades", trades);
    }

    static GameCommand svrChoosePlayerNumToTakeCardFrom(SOC soc, List<SOCPlayer> playerOptions) {
        Integer [] playerNums = new Integer[playerOptions.size()];
        int index=0;
        for (SOCPlayer p:playerOptions)
            playerNums[index++] = p.getPlayerNum();
        return new SOCProtocol(SVR_CHOOSE_PLAYER_TO_TAKE_CARD).setIntList("players", playerNums);
    }

    static GameCommand svrChooseMove(SOC soc, List<MoveType> moves) {
        return new SOCProtocol(SVR_CHOOSE_MOVE).setEnumList("moves", moves);
    }

    static GameCommand svrChooseCardToGiveUp(SOC soc, List<GiveUpCardOption> options, int numCardsToSurrender) {
        return new SOCProtocol(SVR_CHOOSE_CARD_TO_GIVEUP).setEnumList("cards", options).setArg("numCardsToSurrender", numCardsToSurrender);
    }

    static GameCommand svrChooseResource(SOC soc) {
        return new SOCProtocol(SVR_CHOOSE_RESOURCE);
    }

    static GameCommand svrUpdateBoard(SOCBoard board) {
        StringWriter sw = new StringWriter();
        BufferedWriter writer = new BufferedWriter(sw);
        try {
            //board.saveBoard(writer);
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new SOCProtocol(SVR_UPDATE_BOARD).setArg("board", sw.getBuffer().toString());
    }
    
    static GameCommand svrUpdateGame(SOC soc) {
        StringWriter sw = new StringWriter();
        BufferedWriter writer = new BufferedWriter(sw);
        try {
            //soc.write(writer);
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new SOCProtocol(SVR_UPDATE_GAME).setArg("soc", sw.getBuffer().toString());
    }

    public static GameCommand clSetSettlement(int index) {
        return new SOCProtocol(CL_SET_SETTLEMENT).setArg("index", index);
    }

    public static GameCommand clSetRoad(int edgeIndex) {
        return new SOCProtocol(CL_SET_ROAD).setArg("index", edgeIndex);
    }

    public static GameCommand clSetCity(int index) {
        return new SOCProtocol(CL_SET_CITY).setArg("index", index);
    }

    public static GameCommand clSetRobberCell(int cellIndex) {
        return new SOCProtocol(CL_SET_ROBBER_CELL).setArg("index", cellIndex);
    }

    public static GameCommand clRollDice() {
        return new SOCProtocol(CL_ROLL_DICE);
    }

    public static GameCommand clSetTrade(SOCTrade trade) {
        return new SOCProtocol(CL_SET_TRADE).setArg("tradde", trade);
    }
}
