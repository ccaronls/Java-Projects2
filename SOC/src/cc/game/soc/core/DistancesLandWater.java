package cc.game.soc.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DistancesLandWater implements IDistances {
	
	public DistancesLandWater() {
		this(new int[0][0], new int[0][0], new int[0][0], new int[0][0], null);
	}
	
	private final int [][] distLand;
	private final int [][] nextLand;
	private final int [][] distAqua;
	private final int [][] nextAqua;
	private final Collection<Integer> launchVerts;
	
	public DistancesLandWater(
			int [][] distLand, int [][] nextLand, 
			int [][] distAqua, int [][] nextAqua,
			Collection<Integer> launchVerts) {
		super();
		this.distAqua = distAqua;
		this.nextAqua = nextAqua;
		this.distLand = distLand;
		this.nextLand = nextLand;
		this.launchVerts = launchVerts;
	}
	
	private static int nearestShorelineIndex(int from, int [][] dist, Collection<Integer> launchVerts) {
		int d = DISTANCE_INFINITY;
		int index = -1;
		for (int vIndex : launchVerts) {
			if (dist[from][vIndex] < d) {
				d = dist[from][vIndex];
				index = vIndex;
			}
		}
		launchVerts.remove((Object)index);
		return index;
	}
	
	public List<Integer> getShortestPath(int fromVertex, int toVertex) {
		List<Integer> path = new ArrayList<Integer>();
		getShortestPathR(fromVertex, toVertex, path);
		return path;
	}
	
	private void getShortestPathR(int fromVertex, int toVertex, List<Integer> path) {

		// the possible cases:
		// land->land
		// water->water
		// land->shore->water
		// water->shore->land
		// land->shore->water->shore->land
		
		int index;
		List<Integer> copyVerts = new ArrayList<Integer>(launchVerts);
		if (distLand[fromVertex][toVertex] != DISTANCE_INFINITY) {
			// land->land
			getShortestPath(fromVertex, toVertex, path, distLand, nextLand);
		} else if (distAqua[fromVertex][toVertex] != DISTANCE_INFINITY) {
			// water->water
			getShortestPath(fromVertex, toVertex, path, distAqua, nextAqua);
		} else if ((index=nearestShorelineIndex(fromVertex, distLand, copyVerts)) >= 0) {
			//land->shore->?
			if (distLand[fromVertex][index] != DISTANCE_INFINITY) {
    			getShortestPath(fromVertex, index, path, distLand, nextLand);
    			getShortestPathR(index, toVertex, path);
			}
		} else if ((index=nearestShorelineIndex(fromVertex, distAqua, copyVerts)) >= 0) {
			//water->shore->?
			if (distAqua[fromVertex][index] != DISTANCE_INFINITY) {
    			getShortestPath(fromVertex, index, path, distAqua, nextAqua);
    			getShortestPathR(index, toVertex, path);
			}
		}
	}
	
	private void getShortestPath(int fromVertex, int toVertex, List<Integer> path, int [][] dist, int [][] next) {
		if (dist[fromVertex][toVertex] != DISTANCE_INFINITY)
		{
			path.add(fromVertex);
			while (fromVertex != toVertex) {
				fromVertex = next[fromVertex][toVertex];
				path.add(fromVertex);
			}
		}
	}
	
	public int getDist(int from, int to) {
		return getDistR(from, to, new ArrayList<Integer>(launchVerts));
	}
	
	private int getDistR(int from, int to, List<Integer> copyVerts) {
		int index;
		if (distLand[from][to] != DISTANCE_INFINITY) {
			return distLand[from][to];
		} else if (distAqua[from][to] != DISTANCE_INFINITY) {
			return distAqua[from][to];
		} else if ((index=nearestShorelineIndex(from, distLand, copyVerts)) >= 0) {
			return Math.min(DISTANCE_INFINITY, distLand[from][index] + getDistR(index, to, copyVerts));
		} else if ((index=nearestShorelineIndex(from, distAqua, copyVerts)) >= 0) {
			return Math.min(DISTANCE_INFINITY, distAqua[from][index] + getDistR(index, to, copyVerts));
		} 
		return DISTANCE_INFINITY;
	}
}