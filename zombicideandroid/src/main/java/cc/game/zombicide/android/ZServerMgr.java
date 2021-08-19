package cc.game.zombicide.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.lib.game.GColor;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.net.ClientConnection;
import cc.lib.net.GameCommand;
import cc.lib.net.GameServer;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZUser;
import cc.lib.zombicide.p2p.ZUserMP;
import cc.lib.zombicide.ui.UIZombicide;

/**
 * Created by Chris Caron on 7/28/21.
 */
class ZServerMgr extends ZMPCommon implements GameServer.Listener, ZMPCommon.SVR {

    static Logger log = LoggerFactory.getLogger(ZServerMgr.class);

    final GameServer server;

    int colorAssigner = 0;
    final int maxCharacters; // max chars each player can be assigned

    Map<ZPlayerName, Assignee> playerAssignments = new LinkedHashMap<>();
    Map<ClientConnection, ZUser> clientToUserMap = new ConcurrentHashMap<>();
    final CharacterChooserDialog playerChooser;

    ZServerMgr(ZombicideActivity activity, UIZombicide game, int maxCharacters, GameServer server) {
        super(activity, game);
        this.server = server;
        server.addListener(this);
        this.maxCharacters = maxCharacters;
        playerAssignments.clear();
        for (ZombicideActivity.CharLock c : activity.charLocks) {
            Assignee a = new Assignee(c);
            playerAssignments.put(c.player, a);
        }
        activity.user.setColor(nextColor());
        List<Assignee> assignments = new ArrayList<>(playerAssignments.values());
        Collections.sort(assignments);
        playerChooser = new CharacterChooserDialog(activity, assignments, maxCharacters) {
            @Override
            protected void onAssigneeChecked(Assignee assignee) {
                synchronized (playerAssignments) {
                    if (assignee.checked) {
                        assignee.userName = activity.getDisplayName();
                        assignee.color = activity.user.getColor();
                        assignee.isAssingedToMe = true;
                        game.addCharacter(assignee.name);
                        activity.user.addCharacter(assignee.name);
                    } else {
                        assignee.userName = null;
                        assignee.color = null;
                        assignee.isAssingedToMe = false;
                        activity.user.removeCharacter(assignee.name);
                        game.removeCharacter(assignee.name);
                    }
                    postNotifyUpdateAssignee(assignee);
                    server.broadcastCommand(newAssignPlayer(assignee));
                    game.boardRenderer.redraw();
                }
            }

            @Override
            protected void onStart() {
                game.setServer(server);
                activity.startGame();
            }
        };
    }

    GColor nextColor() {
        GColor color = ZUser.USER_COLORS[colorAssigner];
        colorAssigner = (colorAssigner + 1) % ZUser.USER_COLORS.length;
        return color;
    }

    @Override
    public void onConnected(ClientConnection conn) {
        // see if there is an existing user we can reuse
        for (ClientConnection c : clientToUserMap.keySet()) {
            if (!c.isConnected()) {
                ZUser user = clientToUserMap.get(c);
                if (user != null && user.getCharacters().size() > 0) {
                    // reuse
                    clientToUserMap.remove(c);
                    clientToUserMap.put(conn, user);
                    game.addUser(user);
                    conn.sendCommand(newInit(game.getQuest().getQuest(), user.getColor(), maxCharacters, new ArrayList<>(playerAssignments.values())));
                    game.characterRenderer.addMessage(conn.getDisplayName() + " has joined", user.getColor());
                    return;
                }
            }
        }
        ZUser user = new ZUserMP(conn);
        GColor color = nextColor();
        user.setColor(color);
        clientToUserMap.put(conn, user);
        conn.sendCommand(newInit(game.getQuest().getQuest(), color, maxCharacters, new ArrayList<>(playerAssignments.values())));
        game.characterRenderer.addMessage(conn.getDisplayName() + " has joined", color);
    }

    @Override
    public void onReconnection(ClientConnection conn) {
        ZUser user = clientToUserMap.get(conn);
        if (user != null) {
            game.addUser(user);
            conn.sendCommand(newInit(game.getQuest().getQuest(), user.getColor(), maxCharacters, new ArrayList<>(playerAssignments.values())));
            game.characterRenderer.addMessage(conn.getDisplayName() + " has rejoined", user.getColor());
        }
    }

    @Override
    public void onClientDisconnected(ClientConnection conn) {
        // TODO: Put up a dialog waiting for client to reconnect otherwise set their charaters to invisible and to stop moving
        ZUser user = clientToUserMap.get(conn);
        if (user != null) {
            for (ZPlayerName c : user.getCharacters()) {
                //game.removeCharacter(c);
                c.getCharacter().setInvisible(true);
            }
            game.removeUser(user);
            game.characterRenderer.addMessage(conn.getDisplayName() + " has disconnected", user.getColor());
            game.boardRenderer.redraw();
        }
    }

    @Override
    public void onCommand(ClientConnection conn, GameCommand cmd) {
        parseCLCommand(conn, cmd);
    }

    @Override
    public void onChooseCharacter(ClientConnection conn, ZPlayerName name, boolean checked) {
        synchronized (playerAssignments) {
            Assignee a = playerAssignments.get(name);
            ZUser user = clientToUserMap.get(conn);
            if (a != null && user != null) {
                a.checked = checked;
                a.isAssingedToMe = false;
                if (checked) {
                    a.userName = conn.getDisplayName();
                    a.color = user.getColor();
                    game.addCharacter(name);
                    user.addCharacter(name);
                } else {
                    a.color = null;
                    a.userName = null;
                    game.removeCharacter(name);
                    user.removeCharacter(name);
                }
                server.broadcastCommand(newAssignPlayer(a));
                playerChooser.postNotifyUpdateAssignee(a);
                game.boardRenderer.redraw();
            }
        }
    }

    @Override
    public void onStartPressed(ClientConnection conn) {
        ZUser user = clientToUserMap.get(conn);
        if (user != null) {
            game.addUser(user);
            conn.sendCommand(newUpdateGameCommand(game));
        }
    }

    @Override
    public void onError(Exception e) {
        log.error(e);
        game.addPlayerComponentMessage("Error:" + e.getClass().getSimpleName() + ":" + e.getMessage());
    }
}

