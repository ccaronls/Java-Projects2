package cc.game.dominos.android;

import android.content.DialogInterface;
import android.util.Log;

import java.util.List;

import cc.game.dominos.core.Dominos;
import cc.game.dominos.core.Move;
import cc.game.dominos.core.Player;
import cc.lib.annotation.Keep;
import cc.lib.net.ClientConnection;
import cc.lib.net.GameCommand;

import static cc.game.dominos.android.DominosActivity.CL_TO_SVR_FORFEIT;
import static cc.game.dominos.android.DominosActivity.SVR_TO_CL_INIT_GAME;
import static cc.game.dominos.android.DominosActivity.USER_ID;

/**
 * Created by chriscaron on 3/14/18.
 *
 * This is a player that represents a connection to a remote MPPlayerUser when connected.
 * Just a normal AI player otherwise.
 */
public class MPPlayerRemote extends Player implements ClientConnection.Listener {

    public final String TAG = getClass().getSimpleName();

    ClientConnection connection = null;
    Dominos dominos;
    DominosActivity activity;

    public MPPlayerRemote() {}

    @Override
    public String getName() {
        if (connection != null && connection.isConnected()) {
            return connection.getName();
        }
        return super.getName();
    }

    MPPlayerRemote(int playerNum, Dominos dominos, DominosActivity activity) {
        super(playerNum);
        this.dominos = dominos;
        this.activity = activity;
    }

    void setConnection(ClientConnection conn) {
        this.connection = conn;
        this.connection.addListener(this);
    }

    @Override
    @Keep
    public Move chooseMove(Dominos game, List<Move> moves) {
        if (connection != null && connection.isConnected())
            return connection.executeOnRemote(USER_ID, dominos, moves);
        return super.chooseMove(game, moves);
    }

    @Override
    public void onCommand(ClientConnection c, GameCommand cmd) {
        if (cmd.getType() == CL_TO_SVR_FORFEIT) {
            connection.getServer().broadcastMessage("Player " + connection.getName() + " has forfeited the game.");
            activity.startNewGame();
        } else {
            Log.w(TAG, "Unhandled cmd: " + cmd);
        }
    }

    @Override
    public void onDisconnected(ClientConnection c, final String reason) {
        Log.w(TAG, "Client disconnected: " + reason);
        if (dominos.isGameRunning()) {
            dominos.stopGameThread();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.newDialogBuilder().setTitle("Notice")
                            .setMessage("Client " + connection.getName() + " has disconnected because " + reason + ". You can choose to continue without or wait for them to reconnect.")
                            .setNegativeButton("Continue without", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    connection.disconnect("Dropped");
                                    dominos.startGameThread();
                                }
                            }).show();
                }
            });
        }
    }

    @Override
    public void onConnected(ClientConnection c) {
        // reconnect
        if (connection.getServer().getNumConnectedClients() == dominos.getNumPlayers()-1) {
            connection.sendCommand(new GameCommand(SVR_TO_CL_INIT_GAME).setArg("numPlayers", dominos.getNumPlayers())
                    .setArg("playerNum", getPlayerNum())
                    .setArg("dominos", dominos.toString()));

            activity.currentDialog.dismiss();
            dominos.startGameThread();
        }
    }
}
