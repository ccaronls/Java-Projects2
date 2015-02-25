package cc.game.soc.nety;

import cc.game.soc.core.SOCPlayer;
import cc.game.soc.core.SOC;

public interface IClient {

    static interface ConnectionListener  {

        void onConnected();

        void onConnectionError(String string);
        
    }
    
    
	boolean isConnected();
	
	void disconnect();
	
	void initialize(SOCPlayer p, SOC soc);
	
	// create a connection and start a thread
	// to handle input
	void connect(String host) throws Exception;
	
	boolean canCancel();
}
