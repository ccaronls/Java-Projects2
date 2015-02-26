package marcos.games.hexes.core;

import cc.lib.utils.Reflector;

public class Edge extends Reflector<Edge> implements Comparable<Edge> {
	
	static {
		addAllFields(Edge.class);
	}
	
	final int from, to;
	private int n=0;
	final int [] p = new int[2]; // index to pieces adjacent to
	
	public Edge() { from=to=0; }
	
	Edge(int from, int to) {
		if (from == to)
			throw new RuntimeException("Illegal edge");
		if (from < to) {
			this.from = from;
			this.to = to;
		} else {
			this.to = from;
			this.from = to;
		}
	}
	
	void addPiece(int index) {
		p[n++] = index;
	}
	
	int getNum() {
		return n;
	}

	@Override
	public int compareTo(Edge e) {
		if (from == e.from)
			return to-e.to;
		return from - e.from;
	}

	@Override
	public boolean equals(Object obj) {
		Edge e = (Edge)obj;
		if (e == this)
			return true;
		return (from==e.from && to==e.to);
	}

	@Override
	public String toString() {
		return String.valueOf(from) + "->" + String.valueOf(to);
	}
	
	
	
}
