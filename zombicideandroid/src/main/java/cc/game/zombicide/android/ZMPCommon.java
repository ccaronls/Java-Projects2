package cc.game.zombicide.android;

import android.util.Log;

import java.util.List;

import cc.lib.net.ClientConnection;
import cc.lib.net.GameCommand;
import cc.lib.net.GameCommandType;
import cc.lib.utils.Reflector;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ui.UIZombicide;

/**
 * Created by Chris Caron on 7/28/21.
 */
class ZMPCommon {

    final ZombicideActivity activity;
    final UIZombicide game;

    final static int CONNECT_PORT = 31314;
    final static String VERSION   = BuildConfig.VERSION_NAME;

    // commands that originate from server are marked SVR
    public static GameCommandType SVR_INIT = new GameCommandType("SVR_INIT");
    public static GameCommandType SVR_LOAD_QUEST = new GameCommandType("SVR_LOAD_QUEST");
    public static GameCommandType SVR_ASSIGN_PLAYER = new GameCommandType("SVR_ASSIGN_PLAYER");
    public static GameCommandType SVR_UPDATE_GAME = new GameCommandType("SVR_UPDATE_GAME");

    // commands that originate from client are marked CL
    private static GameCommandType CL_CHOOSE_CHARACTER = new GameCommandType("CL_CHOOSE_CHARACTER");
    private static GameCommandType CL_BUTTON_PRESSED = new GameCommandType("CL_BUTTON_PRESSED");

    ZMPCommon(ZombicideActivity activity, UIZombicide game) {
        this.activity = activity;
        this.game = game;
    }

    interface CL {

        void onLoadQuest(ZQuests quest);

        void onInit(int color, int maxCharacters, List<Assignee> playerAssignments);

        void onAssignPlayer(Assignee assignee);

        void onError(Exception e);

        ZGame getGameForUpdate();

        void onGameUpdated(ZGame game);

        default GameCommand newAssignCharacter(ZPlayerName name, boolean checked) {
            return new GameCommand(CL_CHOOSE_CHARACTER).setArg("name", name.name()).setArg("checked", checked);
        }

        default GameCommand newStartPressed() {
            return new GameCommand(CL_BUTTON_PRESSED).setArg("button", "START");
        }

        default GameCommand newUndoPressed() {
            return new GameCommand(CL_BUTTON_PRESSED).setArg("button", "UNDO");
        }

        default void parseSVRCommand(GameCommand cmd) {
            try {
                if (cmd.getType() == SVR_INIT) {
                    int color = cmd.getInt("color");
                    List<Assignee> list = Reflector.deserializeFromString(cmd.getString("assignments"));
                    int maxCharacters = cmd.getInt("maxCharacters");
                    onInit(color, maxCharacters, list);
                } else if (cmd.getType() == SVR_LOAD_QUEST) {
                    ZQuests quest = ZQuests.valueOf(cmd.getString("quest"));
                    onLoadQuest(quest);
                } else if (cmd.getType() == SVR_ASSIGN_PLAYER) {
                    Assignee a = cmd.parseReflector("assignee", new Assignee());
                    onAssignPlayer(a);
                } else if (cmd.getType() == SVR_UPDATE_GAME) {
                    ZGame game = getGameForUpdate();
                    cmd.parseReflector("board", game.getBoard());
                    cmd.parseReflector("quest", game.getQuest());
                    onGameUpdated(game);
                } else {
                    throw new Exception("Unhandled cmd: " + cmd);
                }
            } catch (Exception e) {
                e.printStackTrace();
                onError(e);
            }
        }


    }

    interface SVR {
        void onChooseCharacter(ClientConnection conn, ZPlayerName name, boolean checked);

        void onStartPressed(ClientConnection conn);

        void onUndoPressed(ClientConnection conn);

        void onError(Exception e);

        default GameCommand newInit(int clientColor, int maxCharacters, List<Assignee> playerAssignments) {
            try {
                return new GameCommand(SVR_INIT)
                        .setArg("color", clientColor)
                        .setArg("maxCharacters", maxCharacters)
                        .setArg("assignments", Reflector.serializeObject(playerAssignments));
            } catch (Exception e) {
                onError(e);
                return null;
            }
        }

        default GameCommand newLoadQuest(ZQuests quest) {
            return new GameCommand(SVR_LOAD_QUEST).setArg("quest", quest);
        }


        default GameCommand newAssignPlayer(Assignee assignee) {
            return new GameCommand(SVR_ASSIGN_PLAYER).setArg("assignee", assignee);
        }

        default GameCommand newUpdateGameCommand(ZGame game) {
            //ZPlayerName currentChar = game.getCurrentCharacter();
            return new GameCommand(SVR_UPDATE_GAME)
                    .setArg("board", game.getBoard())
                    .setArg("quest", game.getQuest())
                    ;
        }

        default void parseCLCommand(ClientConnection conn, GameCommand cmd) {
            try {
                if (cmd.getType() == CL_CHOOSE_CHARACTER) {
                    onChooseCharacter(conn, ZPlayerName.valueOf(cmd.getString("name")), cmd.getBoolean("checked", false));
                } else if (cmd.getType() == CL_BUTTON_PRESSED) {
                    switch (cmd.getString("button")) {
                        case "START":
                            onStartPressed(conn);
                            break;
                        case "UNDO":
                            onUndoPressed(conn);
                            break;
                    }
                } else {
                    //throw new Exception("Unhandled cmd: " + cmd);
                    Log.w("ZMPCommon", "parseCLCommand:Unhandled command: " + cmd);
                }
            } catch (Exception e) {
                e.printStackTrace();
                onError(e);
            }
        }

    }



}
