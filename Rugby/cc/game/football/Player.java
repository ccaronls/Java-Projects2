package cc.game.football;

import java.io.*;

public class Player implements Serializable {

	
	// members
	float px, py; // coordinate position of player relative to the position of the ball at the start of play
	float dx, dy; // player motion delta, set by player control or AI
	
	int height, weight; // player individual stats used to affect AI and randomness
	
	int number; // the player's number as displayed on their helmets
	
	Position position; // one of offense or defensive positions enumerated above

	Strategy strategy;
	
	public Player(float sx, float sy, Position p) {
		px = sx;
		py = sy;
		position = p;
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeFloat(px);
		out.writeFloat(py);
		out.writeInt(height);
		out.writeInt(weight);
		out.writeInt(number);
		out.writeInt(position.ordinal());
		out.writeInt(strategy.ordinal());
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		px = in.readFloat();
		py = in.readFloat();
		height = in.readInt();
		weight = in.readInt();
		number = in.readInt();
		position = Position.values()[in.readInt()];
		strategy = Strategy.values()[in.readInt()];
	}
	
	private void readObjectNoData() throws ObjectStreamException {
		// init the object
	}

}
