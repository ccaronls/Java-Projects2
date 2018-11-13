package cc.game.soc.core;

import java.util.List;

public interface IDistances {

	public final static byte DISTANCE_INFINITY = Byte.MAX_VALUE;
	
	List<Integer> getShortestPath(int fromVertex, int toVertex);
	
	int getDist(int from, int to);

}
