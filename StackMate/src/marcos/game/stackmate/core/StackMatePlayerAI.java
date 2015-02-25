package marcos.game.stackmate.core;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import marcos.game.stackmate.core.StackMate.Chip;

public class StackMatePlayerAI extends StackMatePlayer {

	public static boolean ENABLE_LOGGING = false;
	
	private class Node implements Comparable<Node> {
		final LinkedList<Node> children = new LinkedList<Node>();
		Node parent;
		final int source, target;
		float score = 0;
		String extra;
		
		Node() {
			parent = null;
			source = target = -1;
			extra = "ROOT";
		}
		
		Node(Node parent, int source, int dest) {
			this.parent = parent;
			this.source = source;
			this.target = dest;
			this.extra = "";
		}

		@Override
		public int compareTo(Node o) {
			if (score < o.score)
				return -1;
			else if (o.score < score)
				return 1;
			return 0;
		}
		
		public String toString() {
			if (source < 0)
				return extra;
			return "" + source + "->" + target + " " + score + " " + extra + (this == best ? "  **BEST**" : "") + (this == highest ? "  **HIGH**" : "");
		}
	}
	
	Node best = null;
	Node highest = null;
	int [] choices = null;
	
	@Override
	public final int chooseSourceStack(StackMate sm, int[] choices) {
		// search all the moves for the best option
		StackMate copy = new StackMate();
		copy.copyFrom(sm);
		Node root = new Node();
		this.choices = new int[copy.getNumStacks()];
		System.arraycopy(choices, 0, this.choices, 0, choices.length);
		buildTree(root, copy, choices.length, color == Chip.RED, 0);
		best = null;
		findWinner(root);
		String bestMoves = "";
		while (best.parent != root) {
			bestMoves = best.source + "->" + best.target + " " + bestMoves;
			best = best.parent;
		}
		bestMoves = best.source + "->" + best.target + " " + bestMoves;
		if (ENABLE_LOGGING)
			dumpTree(root, 0);
		if (ENABLE_LOGGING)
			System.out.println("Best moves=" + bestMoves);
		return best.source;
	}
	
	void dumpTree(Node root, int depth) {
		if (root == null)
			return;
		for (int i=0; i<depth; i++)
			System.out.print("  ");
		System.out.println(root);
		for (Node c : root.children)
			dumpTree(c, depth+1);
	}
	
	void findWinner(Node root) {
		if (highest == null || root.score > highest.score)
			highest = root;
		if (root.children.size() == 0) {
			if (best == null || root.score > best.score) {
				best = root;
			}
		}
		for (Node n : root.children) {
			findWinner(n);
		}
	}
	
	private void buildTree(Node root, StackMate sm, int numChoices, boolean isRed, int depth) {
		
		if (depth > 5)
			return;
		
		if (numChoices == 0) {
			Chip winner = sm.determineWinner(isRed);
			//root.score *= 100;
			root.extra = winner + " WINS";
			return;
		}
		
		for (int i=0; i<numChoices; i++) {
			int source = choices[i];
			for (int target=0; target<sm.getNumStacks(); target++) {
				if (source == target)
					continue;
				Node node = new Node(root, source, target);
				sm.moveChip(source, target);
				node.score = computeScore(sm, isRed ? Chip.RED : Chip.BLACK);
				root.children.add(node);
				sm.moveChip(target, source);
			}
		}
		
		Collections.sort(root.children);
		Collections.reverse(root.children);
		while (root.children.size() > 3) {
			root.children.removeLast();
		}
		for (Node n : root.children) {
			if (sm.redTurn != isRed)
				n.score *= -1;
			//n.score /= (depth+1);
			sm.moveChip(n.source, n.target);
			buildTree(n, sm, sm.computeStackCoices(!isRed, choices), !isRed, depth+1);
			sm.moveChip(n.target, n.source);
		}
	}
	
	/**
	 * Give a value to the board for this player.
	 * @param sm
	 * @param color
	 * @return
	 */
	public float computeScore(StackMate sm, Chip color) {
		float score = 0;
		if (sm.countMoves(color == Chip.RED) == 0) {
			return -Float.MAX_VALUE;
		}
		for (int i=0;i<sm.getNumStacks(); i++) {
			Chip [] chips = sm.getStack(i);
			float value = 0;
			float maxValue = chips.length+1;
			for (int ii=chips.length-1; ii>=0; ii--) {
				if (chips[ii] == color) {
					if (value == 0)
						value = maxValue;
					score += value;
					value--;
				} else {
					value = 0;
				}
				maxValue--;
			}
		}
		return score + ((float)Math.random()) - 0.5f;
	}//*/

	@Override
	public int chooseTargetStack(StackMate sm, int[] choices) {
		return best.target;
	}

	
}
