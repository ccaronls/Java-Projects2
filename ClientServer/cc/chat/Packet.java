package cc.chat;

import java.net.*;
import java.io.*;

public class Packet {

	private long arriveTime;
	private int code;
	private long sendTime;
	private byte [] data;
	
	public Packet(int bufSize) {
		data = new byte[bufSize];
	}
	
	public void send(OutputStream output) throws IOException {
		output.write(code);
		output.write(data);
	}
	
	public void read(InputStream input) throws IOException {
	}
	
	public boolean canRead(InputStream input) throws IOException {
		return input.available() > data.length;
	}
	
}
