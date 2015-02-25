package cc.game.soc.swing;

import java.awt.Color;
import java.util.List;
import java.util.Properties;

import javax.swing.JFrame;

import cc.game.soc.core.GiveUpCardOption;
import cc.game.soc.core.MoveType;
import cc.game.soc.core.ResourceType;
import cc.game.soc.core.SOC;
import cc.game.soc.core.SOCCell;
import cc.game.soc.core.SOCEdge;
import cc.game.soc.core.SOCPlayer;
import cc.game.soc.core.SOCTrade;
import cc.game.soc.core.SOCVertex;

public interface IGui {

    void spinDice();

    <T> T waitForReturnValue(T b);

    BoardComponent getBoardComponent();

    SOC getSOC();

    Color getPlayerColor(int playerNum);

    GiveUpCardOption getChooseGiveUpCardMenu(List<GiveUpCardOption> options);

    SOCVertex getChooseCityVertex(List<Integer> vertexIndices);

    MoveType getChooseMoveMenu(List<MoveType> moves);

    SOCPlayer getChoosePlayerToTakeCardFromMenu(List<SOCPlayer> playerOptions);

    ResourceType getChooseResourceTypeMenu();

    SOCEdge getChooseRoadEdge(List<Integer> edgeIndices);

    SOCCell getChooseRobberCell(List<Integer> cellIndices);

    SOCVertex getChooseSettlementVertex(List<Integer> vertexIndices);

    SOCTrade getChooseTradeMenu(List<SOCTrade> trades);

    boolean getRollDiceMenu();

    void onConnected();

    void onConnectionError(String reason);

    void showPopup(JFrame frame);

    void closePopup();

    GUIPlayer getGUIPlayer(int playerNum);

    ScrollConsoleComponent getConsole();

    void logInfo(String string);

    DiceComponent getDiceComponent();

    void logDebug(String msg);

    void logError(String msg);

    void quitToMainMenu();

    void showPopup(String string, String string2, PopupButton button);

    Properties getAiProperties();

}
