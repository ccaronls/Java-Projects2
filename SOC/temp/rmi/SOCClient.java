package cc.game.soc.rmi;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class SOCClient {
    
    private SOCInterface server;
    private Registry registry;

    public void connect(String address, int port) {
        try{
            // get the registry 
            registry=LocateRegistry.getRegistry(address,port);
            // look up the remote object
            server=(SOCInterface)(registry.lookup(SOCServer.MYNAME));
            // call the remote method
        }
        catch(RemoteException e){
            e.printStackTrace();
        }
        catch(NotBoundException e){
            e.printStackTrace();
        }        
    }
    
    
}
