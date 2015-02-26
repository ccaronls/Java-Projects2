package cc.chat;

import java.net.*;
import java.util.Vector;
import java.io.*;

public class Server implements Runnable {

	private int port;
	private boolean working = false;
	// shared object
	private Vector connections = new Vector();
	
	public Server(int port) {
		this.port = port;
	}	
	
	public void run() {
	
		working = true;
		try {			
		
			ServerSocket sock = new ServerSocket(port);
			sock.setSoTimeout(100);
			
			while (working) {
				
				try {
					Socket newConnection = sock.accept();
					addNewConnection(newConnection);
					
					
				} catch (SocketTimeoutException ev) {
					// ignore
				}
				
				updateClients();
				
				try {
					wait(); // allow other threads to work
				} catch (Exception e) {
					
				}
				
			}
		
		} catch (IOException e) {
			
		} catch (Exception e) {
			
		}
	}

	/*
	 * Called by thread only
	 */
	private void addNewConnection(Socket connection) {
		
	}

	/*
	 * Called by thread only
	 */
	private void updateClients() {
		synchronized (connections) {
			for (int i=0; i<this.connections.size(); i++) {
				ClientMonitor client = (ClientMonitor)connections.get(i);
				if (!client.isConnected()) {
					debug("Removing client " + client.getName());
					connections.remove(i);
					break; // dont continue
				}
			}
		}
	}
	
	/**
	 * Gracefully terminate all threads, may take a efw seconds
	 *
	 */
	public void shutDown() {
		
	}
	
	private void debug(String msg) {
		System.out.println(msg);
	}
}
