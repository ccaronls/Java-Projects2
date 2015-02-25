package cc.game.soc.rmi;

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class SOCServer extends UnicastRemoteObject implements SOCInterface {

    public final static String MYNAME = "SOCServer";
    
    String myAddress;
    int myPort;
    Registry registry;

    protected SOCServer(int port) throws RemoteException {
        try {
            // get the address of this host.
            myAddress = (InetAddress.getLocalHost()).toString();
        } catch (Exception e) {
            throw new RemoteException("can't get inet address.");
        }
        myPort = port; // this port(registry fs port)
        System.out.println("this address=" + myAddress + ",port=" + myPort);
        try {
            // create the registry and bind the name and object.
            registry = LocateRegistry.createRegistry(myPort);
            registry.rebind(MYNAME, this);
        } catch (RemoteException e) {
            throw e;
        }
    }

}
