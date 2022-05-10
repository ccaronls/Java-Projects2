package cc.game.zombicide.android;

import cc.lib.android.SpinnerTask;
import cc.lib.game.Utils;
import cc.lib.net.GameClient;
import cc.lib.net.GameCommand;
import cc.lib.net.GameCommandType;

class CLSendCommandSpinnerTask extends SpinnerTask<GameCommand> implements GameCommandType.Listener {

    final GameClient client;
    final ZClientMgr mgr;
    final GameCommandType responseType;

    public CLSendCommandSpinnerTask(ZombicideActivity context, GameCommandType responseType) {
        super(context);
        client = context.getClient();
        mgr = context.clientMgr;
        this.responseType = responseType;
        responseType.addListener(this);
    }

    @Override
    protected void doIt(GameCommand... args) throws Exception {
        client.sendCommand(args[0]);
        Utils.waitNoThrow(this, 20000);
    }

    @Override
    public void onCommand(GameCommand cmd) {
        synchronized (this) {
            notify();
        }
    }

    @Override
    protected void onCompleted() {
        responseType.removeListener(this);
    }
}
