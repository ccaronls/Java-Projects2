package cc.game.soc.nety;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Command {

    private CommandType type = null;
    private String data = "";
    private int playerNum = 0;
    
    Command(DataInputStream in) throws IOException {
        type = CommandType.valueOf(in.readUTF());
        playerNum = in.readInt();
        data = in.readUTF();
    }
    
    void write(DataOutputStream out) throws IOException {
        out.writeUTF(type.toString());
        out.writeInt(playerNum);
        out.writeUTF(data);
    }
    
    Command(CommandType type) {
        this(type, "");
    }

    Command(CommandType type, int data) {
        this(type, String.valueOf(data));
    }


    Command(CommandType type, String data) {
        this.type = type;
        this.data = data;
    }

    CommandType getType() {
		return type;
	}
    
    void setPlayerNum(int num) {
    	this.playerNum = num;
    }
	
	long getTimeout() {
		return 60000;
	}
    
    int getPlayerNum() {
        return playerNum;
    }
    
    String getData() {
    	return data;
    }
    
    public String toString() {
    	return type + " player(" + playerNum + ") " + Helper.trimString(data);
    }
    
    
}
