package marcos.game.stackmate.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import cc.lib.crypt.*;

public class StackMateClient implements Runnable {

	ServerSocket server;
	HuffmanEncoding cypher;
	
	public StackMateClient() throws IOException {
		cypher = new HuffmanEncoding();
		cypher.loadEncoding(new File("huffman.counts"));
	}
	
	public void listen(int port) throws IOException {
		if (server != null)
			throw new IOException("Already listening");
		server = new ServerSocket(port);
		new Thread(this).start();
	}
	
	public void stop() {
		try {
			server.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		server = null;
	}
	
	public void run() {
		while (server != null) {
    		try {
    			Socket client = server.accept();
    			InputStream in = new EncryptionInputStream(client.getInputStream(), cypher);
    			OutputStream out = new EncryptionOutputStream(client.getOutputStream(), cypher);
    			
    		} catch (Exception e) {
    			
    		}
		} 
	}
	
}
