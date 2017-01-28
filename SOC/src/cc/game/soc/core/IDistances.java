package cc.game.soc.core;

import java.util.List;

public interface IDistances {

	public final static int DISTANCE_INFINITY = 100;
	
	List<Integer> getShortestPath(int fromVertex, int toVertex);
	
	int getDist(int from, int to);
}
