package cc.game.soc.nety;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

class ClientConnection implements Runnable {

    private final static Logger log = Logger.getLogger(ClientConnection.class);

	private Socket connection;
	private DataInputStream in;
	private DataOutputStream out;
	private boolean connected;
	private String hostName;
	private String clientName = "newclient";
	private Request req;
	private int pingFreq = 1000; // set by the server
	private List<Command> inCommands = new ArrayList<Command>();
	
	static final int PORT = 44444;
    //static final int PING_FREQ = 1000;
	
	public ClientConnection(String host) throws IOException {
		hostName = host;
		connect(host);
	}
	
	void cancelConnect() {
	    try {
	        connected = false;
	        connection.close();
	    } catch (Exception e) {}
	}
	
	private void connect(final String host) throws IOException {
		connection = new Socket(host, PORT);
		out = new DataOutputStream(connection.getOutputStream());
		in = new DataInputStream(connection.getInputStream());
		connected = true;
		req = new Request(RequestType.REQ_CONNECT, getClientName());
		new Thread(this).start();
	}
	
	public void run()
	{
		log.debug("Entering ClientConnection thread");
		while (connected) {
			try {
				synchronized (this) {
					Request req = getRequest();
					//log.debug("Sending Request " + req);
	                req.write(out);
					Response rsp = new Response(in);
					processResponse(rsp, req.getType());
					if (req.getType() == RequestType.REQ_DISCONNECT)
						connected = false;
					else
						wait(pingFreq);
				}
			} catch (Exception e) {
				e.printStackTrace();
				log.error("Connection failed : " + e.getMessage());
				connected = false;
                synchronized (inCommands) {
                    inCommands.notify();
                }
			}
		}
		log.debug("Disconnecting ...");
		
		try {
			in.close();
			out.flush();
			out.close();				
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		in = null;
		out = null;
		connection = null;
		log.debug("CLOSED, Exiting ClientConnection thread");
	}
	
	boolean isConnected() {
		return connected;
	}
	
	void reconnect() throws IOException {
		if (!connected) {
			log.debug("Attepting reconnect ...");
			connect(hostName);
		}
	}
	
	void disconnect() {
		setRequest(new Request(RequestType.REQ_DISCONNECT, getClientName()));
	}
	
	private Request getRequest() {
		if (this.req == null)
			return new Request(RequestType.REQ_PING, getClientName());
		Request r = req;
		req = null;
		return r;
	}
	
	void setRequest(Request req) {
		this.req = req;
		synchronized (this) {
			notify();
		}
	}
	
	void setClientName(String name) {
		this.clientName = name;
	}
	
	String getClientName() {
		return clientName;
	}
	
	void setPingFreq(int pingFreqMS) {
		this.pingFreq = pingFreqMS;
	}
	
	void processResponse(Response rsp, RequestType type) throws IOException {
		if (rsp.getReqType() != RequestType.REQ_PING || rsp.getStatus() != ResponseStatus.RSP_OK)
		    log.debug("Processing response: " + rsp);
        if (rsp.getReqType() != type)
            throw new ProtocolException("Broken Protocol");
        switch (rsp.getStatus()) {
        case RSP_OK:
            synchronized (inCommands) {
                inCommands.addAll(rsp.getCommands());
                if (inCommands.size() > 0) {
                    inCommands.notify();
                }
            }
            break;
        default:
            throw new IOException("Error Response : " + rsp.getStatus());
        }
	}

	Command getNextCommand() {
		Command cmd = null;
    	synchronized (inCommands) {
    		if (inCommands.size() == 0) {
    			try {
            		inCommands.wait();
                } catch (Exception e) {
	                // what to do here?
	            	e.printStackTrace();
	            }
        	}
            if (inCommands.size() == 0)
                return null;
        	cmd = inCommands.get(0);
        	inCommands.remove(0);
    	}
        return cmd;
	}
	
}
