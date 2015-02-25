package cc.game.soc.nety;

import java.io.*;

public class Request {

    private RequestType type;
    private String clientName;
    private String version;
    private String data = "";

    public String toString() {
    	return type.name() + ":" + clientName + " [" + version + "] " + Helper.trimString(data);
    }

    Request(DataInputStream in) throws IOException {
        type = RequestType.valueOf(in.readUTF());
        clientName = in.readUTF();
        version = in.readUTF();
        data = in.readUTF();
    }
    
	Request(RequestType type, String clientName) {
		this(type, clientName, "");
	}

	Request(RequestType type, String clientName, int data) {
		this(type, clientName, String.valueOf(data));
	}

	Request(RequestType type, String clientName, String data) {
	    this.type = type;
	    this.clientName = clientName;
	    this.version = Protocol.VERSION;
	    this.data = data;
	}

	void write(DataOutputStream out) throws IOException {
		out.writeUTF(type.name());
		out.writeUTF(clientName);
		out.writeUTF(version);
		out.writeUTF(data);
	}
	
    RequestType getType() {
        return type;
    }
    
    String getClientName() {
        return clientName;
    }
    
    String getVersion() {
    	return version;
    }
    
    String getData() {
    	return data;
    }
}
