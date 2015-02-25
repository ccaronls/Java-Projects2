package cc.game.kaiser.net;

import java.io.InputStream;

import cc.game.kaiser.ai.PlayerBot;
import cc.game.kaiser.core.Bid;
import cc.game.kaiser.core.Card;
import cc.game.kaiser.core.Kaiser;
import cc.game.kaiser.core.Player;
import cc.lib.net.GameClient;
import cc.lib.net.GameCommand;

public abstract class KaiserClient extends GameClient implements KaiserCommand.Listener {

    Kaiser kaiser = new Kaiser();
    final Player player;
    
    public KaiserClient(Player player, String version) {
        super(player.getName(), version);
        this.player = player;
    }

    @Override
    public void onSetPlayer(int num, String name) {
        if (!name.equals(player.getName()))
            kaiser.setPlayer(num, new PlayerBot(name));
        else
            kaiser.setPlayer(num, player);
    }

    @Override
    public void onPlayTrick(Card[] options) {
        Card card = player.playTrick(kaiser, options);
        if (card != null)
            this.send(KaiserCommand.clientPlayTrick(card));
    }

    @Override
    public void onMakeBid(Bid[] bids) {
        Bid bid = player.makeBid(kaiser, bids);
        if (bid != null)
            this.send(KaiserCommand.clientMakeBid(bid));
    }

    @Override
    public void onDealtCard(Card card) {
        player.onDealtCard(kaiser, card);
    }

    @Override
    public void onGameUpdate(InputStream in) {
        try {
            kaiser.deserialize(in);
            drawGame(kaiser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected abstract void drawGame(Kaiser kaiser);

    /**
     * This method should not be overriden furthur than this class
     */
    @Override
    protected void onCommand(GameCommand cmd) {
        try {
            KaiserCommand.clientDecode(this, cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
}
