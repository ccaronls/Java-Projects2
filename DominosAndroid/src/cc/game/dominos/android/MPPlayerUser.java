package cc.game.dominos.android;

import android.content.DialogInterface;
import android.widget.Toast;

import java.util.List;

import cc.game.dominos.core.Dominos;
import cc.game.dominos.core.Move;
import cc.game.dominos.core.Player;
import cc.game.dominos.core.PlayerUser;
import cc.lib.net.GameClient;
import cc.lib.net.GameCommand;
import cc.lib.utils.Reflector;

import static cc.game.dominos.android.DominosActivity.*;

/**
 * Created by chriscaron on 3/14/18.
 *
 * This is a user that can be connected as a client to server or just a SP normal user
 */
public class MPPlayerUser extends PlayerUser implements GameClient.Listener {

    private GameClient client;
    private DominosActivity activity;
    private Dominos dominos;

    public void connect(GameClient client, DominosActivity activity, Dominos game) {
        this.client = client;
        this.activity = activity;
        this.dominos = game;
        client.register(USER_ID, this);
        client.register(DOMINOS_ID, dominos);
        client.addListener(this);
    }

    public Move chooseMove(List<Move> options) {
        // TODO: we should be able to apply the move our self to prevent the visual glitch that happens
        //    while waiting for the server to process our move
        return super.chooseMove(dominos, options);
    }

    @Override
    public void onCommand(GameCommand cmd) {
        if (cmd.getType() == SVR_TO_CL_INIT_GAME) {
            int numPlayers = cmd.getInt("numPlayers");
            if (numPlayers < 2 || numPlayers > 4)
                throw new AssertionError("invalid numPlayers: " + numPlayers);
            int playerNum = cmd.getInt("playerNum");
            if (playerNum < 0 || playerNum >= numPlayers)
                throw new AssertionError("invalid playerNum: " + playerNum);
            Player[] players = new Player[numPlayers];
            int idx = 0;
            for (; idx < playerNum; idx++)
                players[idx] = new Player();
            players[idx++] = this;
            for (; idx < numPlayers; idx++)
                players[idx] = new Player();
            dominos.setPlayers(players);
            Reflector.KEEP_INSTANCES = true;
            try {
                String str = cmd.getArg("dominos");
//                    FileUtils.stringToFile(str, new File(Environment.getExternalStorageDirectory(), "dominos.in"));
                dominos.deserialize(str);
                dominos.redraw();
            } catch (Exception e) {
                client.sendError(e);
                client.disconnect("Error: " + e.getMessage());
            }
            Reflector.KEEP_INSTANCES = false;
            activity.currentDialog.dismiss();
        } else if (cmd.getType() == SVR_TO_CL_INIT_ROUND) {
            Reflector.KEEP_INSTANCES = true;
            try {
                String str = cmd.getArg("dominos");
                dominos.deserialize(str);
                dominos.redraw();
                activity.currentDialog.dismiss();
            } catch (Exception e) {
                client.sendError(e);
                client.disconnect("Error: " + e.getMessage());
            }
            Reflector.KEEP_INSTANCES = false;
        } else {
            client.sendError("Dont know how to handle cmd: '" + cmd + "'");
            client.disconnect("Error: dont understand command: " + cmd.getType());
        }
    }

    @Override
    public void onMessage(final String message) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDisconnected(String reason) {
        client.removeListener(this);
        client.unregister(USER_ID);
        client.unregister(DOMINOS_ID);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.newDialogBuilder().setTitle("Disconnected")
                        .setMessage("You have been disconnected from the server.")
                        .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                activity.showNewGameDialog();
                            }
                        }).setCancelable(false).show();
            }
        });
        activity.killGame();
    }

    @Override
    public void onConnected() {
        synchronized (this) {
            notify();
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, "Connected", Toast.LENGTH_LONG).show();
            }
        });
    }

}
