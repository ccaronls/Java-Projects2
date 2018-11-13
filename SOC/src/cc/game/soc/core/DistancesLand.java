package cc.game.soc.core;

import java.util.ArrayList;
import java.util.List;

public class DistancesLand implements IDistances {

	public DistancesLand() {
		this(null, null);
	}
	
	private final byte [][] dist;
	private final byte [][] next;
	
	public DistancesLand(byte [][] dist, byte [][] next) {
		this.dist = dist;
		this.next = next;
	}
	
	public List<Integer> getShortestPath(int fromVertex, int toVertex) {
		List<Integer> path = new ArrayList<Integer>();
		if (dist[fromVertex][toVertex] != DISTANCE_INFINITY)
		{
			path.add(fromVertex);
			while (fromVertex != toVertex) {
				fromVertex = next[fromVertex][toVertex];
				path.add(fromVertex);
			}
		}		
		return path;
	}
	
	public int getDist(int from, int to) {
		return dist[from][to];
	}

}
