package cc.game.dominos.android;

import android.content.DialogInterface;
import android.util.Log;

import cc.game.dominos.core.Dominos;
import cc.game.dominos.core.MPConstants;
import cc.game.dominos.core.Player;
import cc.lib.net.ClientConnection;
import cc.lib.net.GameCommand;

/**
 * Created by chriscaron on 3/14/18.
 *
 * This is a player that represents a connection to a remote MPPlayerUser when connected.
 * Just a normal AI player otherwise.
 */
public class MPPlayerRemote { /*extends Player implements ClientConnection.Listener {

    public final String TAG = getClass().getSimpleName();

    Dominos dominos;
    DominosActivity activity;

    public MPPlayerRemote() {}

    MPPlayerRemote(int playerNum, Dominos dominos, DominosActivity activity) {
        super(playerNum);
        this.dominos = dominos;
        this.activity = activity;
    }

    @Override
    public void onCommand(ClientConnection c, GameCommand cmd) {
        if (cmd.getType() == MPConstants.CL_TO_SVR_FORFEIT) {
            c.getServer().broadcastMessage("Player " + c.getName() + " has forfeited the game.");
            activity.startNewGame();
        } else {
            Log.w(TAG, "Unhandled cmd: " + cmd);
        }
    }

    @Override
    public void onDisconnected(final ClientConnection c, final String reason) {
        Log.w(TAG, "Client disconnected: " + reason);
        if (dominos.isGameRunning()) {
            dominos.stopGameThread();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.newDialogBuilder().setTitle("Notice")
                            .setMessage("Client " + c.getName() + " has disconnected because " + reason + ". You can choose to continue without or wait for them to reconnect.")
                            .setNegativeButton("Continue without", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    c.disconnect("Dropped");
                                    dominos.startGameThread();
                                }
                            }).show();
                }
            });
        }
    }

    @Override
    public void onConnected(final ClientConnection c) {
        // reconnect
        if (c.getServer().getNumConnectedClients() == dominos.getNumPlayers()-1) {
            c.sendCommand(new GameCommand(MPConstants.SVR_TO_CL_INIT_GAME).setArg("numPlayers", dominos.getNumPlayers())
                    .setArg("playerNum", getPlayerNum())
                    .setArg("dominos", dominos.toString()));

            activity.currentDialog.dismiss();
            dominos.startGameThread();
        }
    }*/
}
