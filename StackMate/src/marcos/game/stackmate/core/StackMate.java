package marcos.game.stackmate.core;

import java.util.*;

import cc.lib.crypt.BitVector;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class StackMate extends Reflector<StackMate> {

	public static boolean ENABLE_DEBUG = false;
	
	static {
		addAllFields(StackMate.class);
	}
	
	public enum Chip {
		RED, BLACK
	}
	
	boolean redTurn = true;
	BitVector [] stacks;
	
	StackMatePlayer black, red;
	int sourceStack = -1;
	
	public StackMate() {
		newGame();
	}
	
	public final void newGame() {
		stacks = new BitVector[6];
		stacks[0] = createStack(Chip.RED, Chip.RED, Chip.BLACK, Chip.BLACK);
		stacks[1] = createStack(Chip.RED, Chip.RED, Chip.BLACK, Chip.BLACK);
		stacks[2] = createStack(Chip.RED, Chip.RED, Chip.BLACK, Chip.BLACK);
		stacks[3] = createStack(Chip.BLACK, Chip.BLACK, Chip.RED, Chip.RED);
		stacks[4] = createStack(Chip.BLACK, Chip.BLACK, Chip.RED, Chip.RED);
		stacks[5] = createStack(Chip.BLACK, Chip.BLACK, Chip.RED, Chip.RED);
		redTurn = true;
		sourceStack = -1;
	}
	
	public final void newGame(Properties props) throws Exception {
		String numStacksStr = props.getProperty("numStacks", "6").trim();
		try {
    		int numStacks = Integer.parseInt(numStacksStr);
    		if (numStacks < 3 || numStacks > 20)
    			throw new Exception("Cannot create game with "+ numStacks + " stacks");
    		stacks = new BitVector[numStacks];
    		for (int i=0; i<numStacks; i++) {
    			String chipStr = props.getProperty("stack" + i);
    			if (chipStr == null)
    				throw new Exception("Missing property 'stack" + i + "'");
    			String [] parts = chipStr.split(" ");
    			Chip [] chips = Utils.convertToEnumArray(parts, Chip.class, new Chip[parts.length]);
    			stacks[i] = createStack(chips);
    		}
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Not a number '" + numStacksStr + "'");
		}
		sourceStack = -1;
		redTurn = true;
	}

	private BitVector createStack(Chip ... chips) {
		BitVector bv = new BitVector();
		for (Chip c : chips) {
			bv.pushBack(c == Chip.RED ? true : false);
		}
		return bv;
	}
	
	public final void runGame() {
		if(isGameOver())
			return;
		StackMatePlayer p = redTurn ? red : black;
		if (sourceStack < 0) {
    		int [] choices = new int[stacks.length];
    		int num = computeStackCoices(redTurn, choices);
    		if (num == 0) {
    			throw new AssertionError();
    		}
    		sourceStack = p.chooseSourceStack(this, Arrays.copyOfRange(choices, 0, num));
		}
		if (sourceStack >= 0) {
			int [] target = new int[stacks.length-1];
			int index = 0;
			for (int i=0; i<stacks.length; i++) {
				if (i != sourceStack)
					target[index++] = i;
			}
			int targetStack = p.chooseTargetStack(this, target);
			if (targetStack == sourceStack) {
				cancel();
			} else if (targetStack >= 0) {
				onChipMoved(sourceStack, targetStack, redTurn ? Chip.RED : Chip.BLACK);
				moveChip(sourceStack, targetStack);
				redTurn = !redTurn;
				sourceStack = -1;
			}
		}
		if (isGameOver()) {
			onGameOver(determineWinner());
		}
	}
	
	Chip determineWinner() {
		return determineWinner(redTurn);
	}
	
	Chip determineWinner(boolean redTurn) {
		int numRed = 0;
		for (int i=0; i<stacks.length; i++) {
			if (stacks[i].getLast())
				numRed++;
		}
		if (numRed == stacks.length)
			return Chip.RED;
		if (redTurn)
			return Chip.BLACK;
		return Chip.RED;
	}
	
	public final void cancel() {
		sourceStack = -1;
	}
	
	public void onChipMoved(int source, int target, Chip color) {
		if (ENABLE_DEBUG)
			System.out.println("" + getCurentPlayer().color + " Move " + color + " chip from " + source + " to " + target);
	}
	
	public void onGameOver(Chip winnerColor) {
		if (ENABLE_DEBUG)
			System.out.println("Game Over.  " + winnerColor + " Player Wins");
	}
	
	int computeStackCoices(boolean red, int [] holder) {
		int num=0;
		for (int i=0; i<stacks.length; i++) {
			if (stacks[i].getLast() == red && stacks[i].getLen() > 1)
				holder[num++] = i;
		}
		return num;
	}

	public final boolean isGameOver() {
		int numMoves=0;
		for (int i=0; i<stacks.length; i++) {
			if (stacks[i].getLast() == redTurn && stacks[i].getLen() > 1)
				numMoves++;
		}
		return numMoves == 0;
	}

	final int countMoves(boolean isRed) {
		int numMoves=0;
		for (int i=0; i<stacks.length; i++) {
			if (stacks[i].getLast() == isRed && stacks[i].getLen() > 1)
				numMoves++;
		}
		return numMoves;
	}

	/*
	 * Package access for AI to manipulate as it see fits
	 */
	void moveChip(int source, int target) {
		if (stacks[source].getLen() <= 1)
			throw new AssertionError();
		stacks[target].pushBack(stacks[source].popBack());
	}

	int countChipsOnTop(Chip chip) {
		int count = 0;
		for (BitVector bv : stacks) {
			if (bv.getLast()) {
				if (chip == Chip.RED)
					count++;
			} else {
				if (chip == Chip.BLACK)
					count++;
			}
			
		}
		return count;
	}
	
	public final void initPlayers(StackMatePlayer black, StackMatePlayer red) {
		this.black = black;
		this.red = red;
		black.color = Chip.BLACK;
		red.color = Chip.RED;
	}
	
	// return a stack such that the bottom piece is the first element in the array
	public final Chip [] getStack(int index) {
		Chip [] arr = new Chip[stacks[index].getLen()];
		for (int i=0; i<stacks[index].getLen(); i++)
			arr[i] = stacks[index].get(i) ? Chip.RED : Chip.BLACK;
		return arr;
	}
	
	public final int getStackHeight(int index) {
		return stacks[index].getLen();
	}
	
	// return the top piece (owner color) of a stack
	public final Chip getStackTop(int index) {
		return stacks[index].getLast() ? Chip.RED : Chip.BLACK;
	}
	
	public final int getNumStacks() {
		return stacks.length;
	}
	
	public final StackMatePlayer getCurentPlayer() {
		return redTurn ? red : black;
	}
	
	public final int getSourceStack() {
		return sourceStack;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("redTurn = " + redTurn);
		for (int i=0; i<stacks.length; i++) {
			buf.append("\nStack "+ i + "=" + stacks[i]);
		}
		return buf.toString();
	}
}
