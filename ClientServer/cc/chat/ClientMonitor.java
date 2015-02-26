package cc.chat;

import java.net.*;
import java.io.*;

/**
 * Instances of these are managed by Server
 * @author Chris Caron
 *
 */
public class ClientMonitor implements Runnable {

	private static int numNames=0;
	
	private Socket connection;
	private InputStream fromClient;
	private OutputStream toClient;
	private boolean connected;
	private boolean authenticated;
	private boolean threadDone;
	// initially our name is not known until 
	private String name = "UNKNOWN_"+numNames++;
	
	public ClientMonitor(Socket connection) {
		this.connection = connection;
	}
	
	public void run() {
		threadDone = false;
		
		try {
			
			this.fromClient = connection.getInputStream();
			this.toClient = connection.getOutputStream();
			connected = true;
		
			while (connected) {
				
			}
		} catch (IOException e) {
			
		} catch (Exception e) {
			
		}
		
		threadDone = true;
	}
	
	public void pushPacket(Packet packet) {
		
	}
	
	public boolean isConnected() {
		return !threadDone;
	}
	
	public String getName() {
		return name;
	}
	
}
