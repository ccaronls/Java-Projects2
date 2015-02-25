package cc.game.kaiser.net;

import java.io.InputStream;

import cc.game.kaiser.core.Bid;
import cc.game.kaiser.core.Card;
import cc.game.kaiser.core.Kaiser;
import cc.lib.net.GameCommand;
import junit.framework.TestCase;

public class KaiserCommandTest extends TestCase implements KaiserCommand.Listener {

    public void testClientToServer() throws Exception {
        KaiserServer server = new KaiserServer();
        //for (Bid b: Bid.ALL_BIDS) {
        //    GameCommand cmd = KaiserCommand.clientMakeBid(b);
        //    assertTrue(KaiserCommand.serverDecode(null, server, cmd));
       // }
        
        for (Card c: server.getAllCards()) {
            GameCommand cmd = KaiserCommand.clientPlayTrick(c);
            assertTrue(KaiserCommand.serverDecode(null, server, cmd));
        }
    }
    
    public void testServerToclient() throws Exception {
        KaiserServer server = new KaiserServer();
        for (Card c: server.getAllCards()) {
            GameCommand cmd = KaiserCommand.getDealtCardCommand(c);
            assertTrue(KaiserCommand.clientDecode(this, cmd));
        }
        
        {
            //GameCommand cmd = KaiserCommand.getMakeBidCommand(Bid.ALL_BIDS);
            //assertTrue(KaiserCommand.clientDecode(this, cmd));
        }
        {
            GameCommand cmd = KaiserCommand.getMakeBidCommand(new Bid [] { Bid.NO_BID });
            assertTrue(KaiserCommand.clientDecode(this, cmd));
        }
        {
            GameCommand cmd = KaiserCommand.getPlayTrickCommand(server.getAllCards());
            assertTrue(KaiserCommand.clientDecode(this, cmd));
        }
        for (int i=0; i<4; i++){
            GameCommand cmd = KaiserCommand.getSetPlayerCommand("A", i);
            assertTrue(KaiserCommand.clientDecode(this, cmd));
        }
        {
            GameCommand cmd = KaiserCommand.getUpdateGameCommand(server);
            assertTrue(KaiserCommand.clientDecode(this, cmd));
        }
    }

    @Override
    public void onSetPlayer(int num, String name) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onPlayTrick(Card[] options) {
        
    }

    @Override
    public void onMakeBid(Bid[] bids) {
        
    }

    @Override
    public void onDealtCard(Card card) {
        
    }

    @Override
    public void onGameUpdate(InputStream in) {
        Kaiser game = new Kaiser();
        try {
            game.deserialize(in);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
    }
    
    
    
}
