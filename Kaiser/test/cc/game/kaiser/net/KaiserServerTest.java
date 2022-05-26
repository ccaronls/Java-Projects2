package cc.game.kaiser.net;

import cc.game.kaiser.ai.PlayerBot;
import cc.game.kaiser.core.Kaiser;
import cc.lib.game.Utils;
import cc.lib.net.ClientForm;
import junit.framework.TestCase;

public class KaiserServerTest extends TestCase {

    int PORT = 32323;
    String VERSION = "KaiserServerTest";
    
    KaiserServer server = null;
    int numConnected = 0;
    
    public void test() throws Exception {
        // start the server listening
        server = new KaiserServer();
        server.start(PORT, VERSION);
        
        String [] names = { "A", "B", "C", "D", "E" };
        for (int i=0; i<names.length; i++) {
            pause(Utils.rand() % 1000 + 100);
            //ClientThread c0 = new ClientThread(names[i]);
            KaiserClient c0 = new KaiserClient(new PlayerBot(names[i]), VERSION) {

                @Override
                protected void drawGame(Kaiser kaiser) {
                    
                }

                @Override
                protected void onMessage(String message) {
                    System.out.println("MSG [" + getName() + "] -> " + message);
                }

                @Override
                protected void onDisconnected(String message) {
                    System.out.println("onDisconnected: " + getName());
                }

                @Override
                protected void onConnected() {
                    System.out.println("onConnected: " + this.getName());
                }

                @Override
                protected void onForm(ClientForm form) {
                    // TODO Auto-generated method stub
                    
                }
                
            };
            c0.connect("localhost", PORT);
        }        
        
        synchronized (this) {
            while (numConnected > 0)
                wait();
        }
        server.stop();
    }
    
    /*
    class ClientThread extends GameClient implements KaiserCommand.Listener {

        public ClientThread(String userName) {
            super(userName, "localhost", PORT, VERSION);
        }

        @Override
        public void onSetPlayer(int num, String name) {
            this.logDebug("onSetPlayer " + num + " -> " + name);
        }

        @Override
        public void onPlayTrick(Card [] cards) {
            this.logDebug("onPlayTrick: " + Arrays.toString(cards));
            pause(Utils.rand() % 1000 + 100);
            this.send(KaiserCommand.clientPlayTrick(cards[Utils.rand() % cards.length]));
        }

        @Override
        public void onMakeBid(Bid[] bids) {
            this.logDebug("onMakeBid: " + Arrays.toString(bids));
            pause(Utils.rand() % 1000 + 100);
            //this.send(KaiserCommand.clientMakeBid(bids[Utils.rand() % bids.length]));
            this.send(KaiserCommand.clientMakeBid(NO_BID));
        }

        @Override
        public void onDealtCard(Card card) {
            this.logDebug("onDealtCard: " + card);
        }

        @Override
        public void onGameUpdate(InputStream in) {
            this.logDebug("onGameUpdate, reading ...");
            Kaiser k = new Kaiser() {
                @Override
                public Player instantiatePlayer(String clazz, String name) {
                    return new PlayerBot(name);
                }
            };
            try {
                k.loadGame(in);
                logDebug("Team A:" + k.getTeam(0).toString());
                logDebug("Team B:" + k.getTeam(1).toString());
            } catch (Exception e) {
                e.printStackTrace();
                failExit(e.getMessage());
            }
        }

        @Override
        protected void onMessage(String message) {
            logDebug("onMessage: " + message);
        }

        @Override
        protected void onDisconnected(String message) {
            numConnected -- ;
            logDebug("onDisconnected: " + message + "  num=" + numConnected);
            synchronized (KaiserServerTest.this) {
                KaiserServerTest.this.notify();
            }
        }

        @Override
        protected void onConnected() {
            numConnected++;
            logDebug("onConnected: num=" + numConnected);
            
        }

        @Override
        protected void onCommand(GameCommand cmd) {
            try {
                logDebug("onCommand: " + cmd);
                pause(Utils.rand() % 1000 + 100);
                KaiserCommand.clientDecode(this, cmd);
            } catch (Exception e) {
                e.printStackTrace();
                failExit(e.getMessage());
            }
        }
        
        protected void logDebug(String msg) {
            super.logDebug("------> " + getName() + ":" + msg);
        }
    }*/
    
    void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void failExit(String msg) {
        fail(msg);
        System.exit(1);
    }
}
