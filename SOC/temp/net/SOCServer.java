package cc.game.soc.net;

import java.io.IOException;
import java.util.Map;

import cc.game.soc.core.SOC;
import cc.lib.net.ClientConnection;
import cc.lib.net.GameCommand;
import cc.lib.net.GameServer;

public class SOCServer implements GameServer.Listener {

    final static String VERSION = "SOC1.0";
    final static int LISTEN_PORT = 44144;
    final static int CLIENT_READ_TIMEOUT = 10000;
    
    SOC soc;
    GameServer server;
    
    public SOCServer() {
    }
    
    public void start() throws IOException {
        server = new GameServer(this, LISTEN_PORT, CLIENT_READ_TIMEOUT, VERSION);
    }
    
    @Override
    public void onConnected(ClientConnection conn) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void onReconnection(ClientConnection conn) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void onClientDisconnected(ClientConnection conn) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void onClientCommand(ClientConnection conn, GameCommand command) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void onFormSubmited(ClientConnection conn, int id, Map<String, String> params) {
        // TODO Auto-generated method stub
        
    }
    
}
