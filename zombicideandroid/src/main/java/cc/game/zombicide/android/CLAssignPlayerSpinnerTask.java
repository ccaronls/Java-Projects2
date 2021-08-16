package cc.game.zombicide.android;

import cc.lib.android.SpinnerTask;
import cc.lib.game.Utils;
import cc.lib.net.GameClient;
import cc.lib.net.GameCommand;
import cc.lib.net.GameCommandType;

class CLAssignPlayerSpinnerTask extends SpinnerTask<Assignee> implements GameCommandType.Listener {

    final GameClient client;
    final ZClientMgr mgr;

    public CLAssignPlayerSpinnerTask(ZombicideActivity context) {
        super(context);
        client = context.getClient();
        mgr = context.clientMgr;
        ZMPCommon.SVR_ASSIGN_PLAYER.addListener(this);
    }

    @Override
    protected void doIt(Assignee... args) throws Exception {
        GameCommand cmd = ((ZombicideActivity) getContext()).clientMgr.newAssignCharacter(args[0].name,
                args[0].checked);
        client.sendCommand(cmd);
        Utils.waitNoThrow(this, 5000);
    }

    @Override
    public void onCommand(GameCommand cmd) {
        synchronized (this) {
            notify();
        }
    }

    @Override
    protected void onCompleted() {
        ZMPCommon.SVR_ASSIGN_PLAYER.removeListener(this);
    }
}
