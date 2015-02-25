package cc.game.kaiser.net;

import cc.game.kaiser.ai.PlayerBot;

/**
 * Example KaiserServer creates 3 bots and waits for a remote player to join
 * 
 * @author ccaron
 *
 */
public class KaiserServerExample {

    public static final String VERSION = "KaiserServerExample";
    public static final int PORT = 32323; 
    
    public static void main(String [] args) throws Exception {
        
        KaiserServer server = new KaiserServer();
        server.start(PORT, VERSION);
        server.setPlayer(0, new PlayerBot("Bot1"));
        server.setPlayer(1, new PlayerBot("Bot2"));
        server.setPlayer(2, new PlayerBot("Bot3"));
        
        while (!server.isGameOver()) {
            synchronized (server) {
                server.wait(5000);
            }
        }
    
        server.stop();
    
    }
    
}
