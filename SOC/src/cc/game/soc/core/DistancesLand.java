package cc.game.soc.core;

import java.util.ArrayList;
import java.util.List;

public class DistancesLand implements IDistances {

	public DistancesLand() {
		this(new int[0][0], new int[0][0]);
	}
	
	private final int [][] dist;
	private final int [][] next;
	
	public DistancesLand(int [][] dist, int [][] next) {
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
