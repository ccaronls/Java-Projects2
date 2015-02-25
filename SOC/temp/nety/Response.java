package cc.game.soc.nety;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Response {
	
    private List<Command> commands = new ArrayList<Command>();
    private RequestType reqType;
    private ResponseStatus status;

    public String toString() {
    	StringBuffer buf = new StringBuffer();
    	buf.append(reqType.name()).append(",").append(status);
    	Iterator<Command> it = commands.iterator();
    	while (it.hasNext()) {
    		buf.append("\n").append(it.next().toString());
    	}
    	return buf.toString();
    }

    Response(RequestType reqType, ResponseStatus status) {
        this.reqType = reqType;
        this.status = status;
    }
    
	Response(DataInputStream in) throws IOException {
		reqType = RequestType.valueOf(in.readUTF());
		status = ResponseStatus.valueOf(in.readUTF());
		int numCommands = in.readInt();
		for (int i=0; i<numCommands; i++) {
			commands.add(new Command(in));
		}
	}
    
    void write(DataOutputStream out) throws IOException {
        out.writeUTF(reqType.name());
        out.writeUTF(status.name());
        out.writeInt(commands.size());
        Iterator<Command> it = commands.iterator();
        while (it.hasNext()) {
        	it.next().write(out);
        }
    }

    RequestType getReqType() {
        return reqType;
    }

    List<Command> getCommands() {
        return commands;
    }
    
    ResponseStatus getStatus() {
        return status;
    }
    
}
