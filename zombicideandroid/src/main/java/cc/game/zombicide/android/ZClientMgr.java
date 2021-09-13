package cc.game.zombicide.android;

import android.content.DialogInterface;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.net.GameClient;
import cc.lib.net.GameCommand;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZUser;
import cc.lib.zombicide.p2p.ZGameMP;
import cc.lib.zombicide.p2p.ZUserMP;
import cc.lib.zombicide.ui.UIZombicide;

/**
 * Created by Chris Caron on 7/28/21.
 */
class ZClientMgr extends ZMPCommon implements GameClient.Listener, ZMPCommon.CL {

    static Logger log = LoggerFactory.getLogger(ZClientMgr.class);

    final ZUser user;
    final GameClient client;
    CharacterChooserDialog playerChooser = null;

    ZClientMgr(ZombicideActivity activity, UIZombicide game, GameClient client, ZUser user) {
        super(activity, game);
        this.client = client;
        this.user = user;
        client.addListener(this);
    }

    void shutdown() {
        client.removeListener(this);
    }

    @Override
    public void onCommand(GameCommand cmd) {
        parseSVRCommand(cmd);
    }

    @Override
    public void onLoadQuest(ZQuests quest) {
        game.loadQuest(quest);
        game.boardRenderer.redraw();
    }

    @Override
    public void onInit(int color, int maxCharacters, List<Assignee> playerAssignments) {
        user.setColor(color);
        game.clearCharacters();
        game.clearUsersCharacters();

        List<Assignee> assignees = new ArrayList<>();
        for (ZombicideActivity.CharLock c : activity.charLocks) {
            Assignee a = new Assignee(c);
            int idx = playerAssignments.indexOf(a);
            if (idx >= 0) {
                Assignee aa = playerAssignments.get(idx);
                a.copyFrom(aa);
                if (color == a.color)
                    a.isAssingedToMe = true;
            }
            assignees.add(a);
            if (a.checked) {
                game.addCharacter(a.name);
                if (a.isAssingedToMe) {
                    user.addCharacter(a.name);
                }
            }
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                playerChooser = new CharacterChooserDialog(activity, assignees, maxCharacters) {
                    @Override
                    protected void onAssigneeChecked(Assignee assignee, boolean checked) {
                        Log.d(TAG, "onAssigneeChecked: " + assignee);
                        GameCommand cmd = activity.clientMgr.newAssignCharacter(assignee.name,checked);
                        new CLSendCommandSpinnerTask(activity, ZMPCommon.SVR_ASSIGN_PLAYER) {
                            @Override
                            protected void onSuccess() {
                                assignee.checked = checked;
                                postNotifyUpdateAssignee(assignee);
                            }
                        }.execute(cmd);
                    }

                    @Override
                    protected void onStart() {
                        game.setClient(client);
                        client.sendCommand(newStartPressed());
                        activity.initGameMenu();
                    }
                };
                game.boardRenderer.redraw();
            }
        });
    }

    @Override
    public void onAssignPlayer(Assignee assignee) {
        Log.d("ZClientMgr", "onAssignPlayer: " + assignee);
        if (user.getColorId() == assignee.color)
            assignee.isAssingedToMe = true;
        if (assignee.checked) {
            game.addCharacter(assignee.name);
            if (assignee.isAssingedToMe)
                user.addCharacter(assignee.name);
        } else {
            game.removeCharacter(assignee.name);
            user.removeCharacter(assignee.name);
        }
        game.boardRenderer.redraw();
        if (playerChooser != null)
            playerChooser.postNotifyUpdateAssignee(assignee);
    }

    @Override
    public void onError(Exception e) {
        log.error(e);
        game.addPlayerComponentMessage("Error: " + e.getClass().getSimpleName() + ":" + e.getMessage());
    }

    @Override
    public UIZombicide getGameForUpdate() {
        return game;
    }

    @Override
    public void onGameUpdated(ZGame game) {
        this.game.boardRenderer.redraw();
        this.game.characterRenderer.redraw();
    }

    @Override
    public void onMessage(String msg) {
        game.addPlayerComponentMessage(msg);
    }

    @Override
    public void onDisconnected(String reason, boolean serverInitiated) {
        if (serverInitiated) {
            activity.game.setResult(null);
            if (playerChooser != null)
                playerChooser.dialog.dismiss();
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    activity.newDialogBuilder().setTitle("Disconnected from Server")
                            .setMessage("Do you want to try and Reconnect?")
                            .setNegativeButton(R.string.popup_button_no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    activity.p2pShutdown();
                                }
                            }).setPositiveButton(R.string.popup_button_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            client.reconnectAsync();
                        }
                    }).show();
                }
            });
        }
    }

    @Override
    public void onConnected() {
        client.register(ZUserMP.USER_ID, user);
        client.register(ZGameMP.GAME_ID, game);
        client.setDisplayName(activity.getDisplayName());
    }
}
