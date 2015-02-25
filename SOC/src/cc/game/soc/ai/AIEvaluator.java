package cc.game.soc.ai;

import java.io.PrintWriter;
import java.util.*;

import cc.game.soc.core.*;
import cc.lib.game.Utils;
import cc.lib.math.CMath;

public class AIEvaluator implements IEvaluator {

	/**
     * Get the probability of a die roll from 2 6 sided dice.
     * @param num
     * @return
     */
    static  {
        // rate the possible number on the board
        // there are 21 possible rolls with 2, 6 sided die
        double p1 = 1.0f/21.0f;
        double p2 = 2.0f/21.0f;
        double p3 = 3.0f/21.0f;
        double p4 = 4.0f/21.0f;
        double p5 = 5.0f/21.0f;

        diePossibility = new double[] {
                0, 0, p1, p2, p3, p4, p5, 0, p5, p4, p3, p2, p1
        };

    }
    
    final static double [] diePossibility;

    private final StringBuffer buf = new StringBuffer();
    
    private void addValue(String name, double value) {
    	if (value != 0) {
    		if (buf.length() > 0)
    			buf.append("\n");
    		buf.append(String.format("%-20s %f", name, value));
    	}
    }
    
	@Override
	synchronized public float evaluate(Player p, Board b, SOC soc, PrintWriter debugOutput) {
		buf.setLength(0);
		double value = 0;
		
		// rate the BOARD

		// things to contribute to a good board:
		// 1  long roads compared with others
		// 2  lots of structures
		// 3  structures on high probability vertices
		// 4  structures next to unique ports
		// 5  robber on cells that we are not adjacent too but is adjacent to opponents cells
		// 6  lots of potential settlements with high potential resource probability
		// 7  Rate potential roads to prevent AI from building along edges too much
		// 8  roads should run along cells that are resources or ports
		
		// 1. We want to rate the value of our road length on a logarithmic scale of how much longer ours is compared to the next closest
		//    This is because we want the difference to become less of a contribution as the delta increases
		{
			double longRoadValue = 0;
			int mine = 0;
			int theirs = 0;
			for (int i=1; i<=soc.getNumPlayers(); i++) {
				int len = b.computeMaxRouteLengthForPlayer(i, soc.getRules().isEnableRoadBlock());
				if (i == p.getPlayerNum()) {
					mine = len;
				} else if (len > theirs) {
					theirs = len;
				}
			}
			int delta = mine-theirs;
			if (delta <= 0) {
				longRoadValue = delta;
			} else {
				longRoadValue = Math.log(1+delta);
			}
			
			addValue("Long Road Value", longRoadValue);
			value += longRoadValue;
		}		
		
		// 2. TODO: Seems like this is covered by points already
		if (false) {
			double cityValue = soc.getRules().getPointsPerCity() * b.getNumCitiesForPlayer(p.getPlayerNum());
			double settlementValue = soc.getRules().getPointsPerSettlement() * b.getNumSettlementsForPlayer(p.getPlayerNum());
			value += cityValue;
			addValue("City Value", cityValue);
			value += settlementValue;
			addValue("Settlement Value", settlementValue);
		}
		
		final int [] resourcePorts = new int[SOC.NUM_RESOURCE_TYPES];
		int numMultiPorts = 0;
		
		// 3. In general, we want the probability of any resource to be high.  But also, we want the probability
		//    of the resources of any 2:1 ports we own to be especially high
		{
			double [] resourceProb = new double[SOC.NUM_RESOURCE_TYPES];
			
			for (Vertex v : b.getVerticies()) {
				if (v.getPlayer() == p.getPlayerNum()) {
					double scale = v.isCity() ? soc.getRules().getNumResourcesForCity() : soc.getRules().getNumResourcesForSettlement();
					for (Tile cell : b.getTilesAdjacentToVertex(v)) {
						switch (cell.getType()) {
							case GOLD:
								for (int i=0; i<resourceProb.length; i++)
									resourceProb[i] += diePossibility[cell.getDieNum()] * scale;
								break;
							case FIELDS:
							case FOREST:
							case HILLS:
							case MOUNTAINS:
							case PASTURE:
    						case RESOURCE:
    							resourceProb[cell.getResource().ordinal()] += diePossibility[cell.getDieNum()] * scale;
    							break;
    						case PORT_RESOURCE:
    							resourcePorts[cell.getResource().ordinal()]++;
    							break;
    						case PORT_MULTI:
    							numMultiPorts++;
    							break;
							case DESERT:
								break;
							case NONE:
								break;
							case RANDOM_PORT:
								break;
							case RANDOM_PORT_OR_WATER:
								break;
							case RANDOM_RESOURCE:
								break;
							case RANDOM_RESOURCE_OR_DESERT:
								break;
							case UNDISCOVERED:
								break;
							case WATER:
								break;
						}
					}
				}
			}
			
			double resourceProbValue = 0.5 * CMath.sum(resourceProb);
			//resourceProbValue *= 2;
			addValue("Resource Prob", resourceProbValue);
			value += resourceProbValue;
			double resourcePortProb = 0;
			for (int i=0; i<SOC.NUM_RESOURCE_TYPES; i++) {
				if (resourcePorts[i] > 0)
					resourcePortProb += resourceProb[i];
			}
			
			double ave = CMath.sum(resourceProb) / resourceProb.length;
			double stdDev = 0.1 * Math.abs(CMath.stdDev(resourceProb, ave));
			double resourceDistribution = ave - stdDev;
			addValue("Resource distribution stddev=" + stdDev + " ave=" + ave, resourceDistribution);
			value += resourceDistribution;
			
			//resourcePortProb *= 2;
			value += resourcePortProb;
			addValue("Resource Port Prob", resourcePortProb);
		}
		
		// 4. We DONT want to place structures on ports we already have
		{
			double resourcePortValue = 0;
			for (int port : resourcePorts) {
				//value -= (port-1);
				if (port == 1)
					resourcePortValue ++;
				else if (port > 1)
					resourcePortValue -= (port-1);
			}
			resourcePortValue /= 10;
			value += resourcePortValue;
			addValue("Resource Port", resourcePortValue);
			double multiPortValue = 0;
			if (numMultiPorts == 1)
				multiPortValue = 0.1;
			else if (numMultiPorts > 1) 
				multiPortValue = -(numMultiPorts-1)/10;
			addValue("Multi Port", multiPortValue);
			value += multiPortValue;
		}		
		
		// 5. Value the robber.  Robber value is placed next to players with lots of cards in their hand
		{
			double robberValue = 0;
			if (b.getRobberTile() >= 0) {
				Tile cell = b.getTile(b.getRobberTile());
				if (cell.isDistributionTile()) {
					for (int v: cell.getAdjVerts()) {
						Vertex vert = b.getVertex(v);
						if (vert.getPlayer() == p.getPlayerNum()) {
							robberValue += -5;
						} else if (vert.getPlayer() > 0) {
							robberValue += soc.getPlayerByPlayerNum(vert.getPlayer()).getTotalCardsLeftInHand();
						}
					}
				}
			}
			value += robberValue;
			addValue("Robber", robberValue);
		}
		
		// 6. Tally the potential settlements
		if (false && b.getNumStructuresForPlayer(p.getPlayerNum()) >= 2) {
			double potentialSettlementValue = 0;
			double potentialPortValue = 0;
			List<Integer> options= SOC.computeSettlementOptions(soc, p.getPlayerNum(), b);
			//value += options.size();
			
			double [] prob = new double[SOC.NUM_RESOURCE_TYPES];
			for (int index : options) {
				Vertex v = b.getVertex(index);
				for (Tile cell : b.getTilesAdjacentToVertex(v)) {
					switch (cell.getType()) {
						case GOLD:
							for (int i=0; i<prob.length; i++)
								prob[i] += diePossibility[cell.getDieNum()];
    						break;
						case FIELDS:
						case FOREST:
						case HILLS:
						case MOUNTAINS:
						case PASTURE:
    					case RESOURCE:
    						prob[cell.getResource().ordinal()] += diePossibility[cell.getDieNum()];
    						break;
    						
    					case PORT_MULTI:
    						if (numMultiPorts == 0) {
    							potentialPortValue += 1;
    						} else {
    							potentialPortValue -= numMultiPorts;
    						}
    						numMultiPorts++;
    						break;
    					case PORT_RESOURCE:
    						if (resourcePorts[cell.getResource().ordinal()] == 0) {
    							potentialPortValue += 2;
    						} else {
    							potentialPortValue -= resourcePorts[cell.getResource().ordinal()];
    						}
    						resourcePorts[cell.getResource().ordinal()]++;
    						break;
						case DESERT:
							break;
						case NONE:
							break;
						case RANDOM_PORT:
							break;
						case RANDOM_PORT_OR_WATER:
							break;
						case RANDOM_RESOURCE:
							break;
						case RANDOM_RESOURCE_OR_DESERT:
							break;
						case UNDISCOVERED:
							break;
						case WATER:
							break;
					}
				}
			}
			
			potentialSettlementValue = CMath.sum(prob);
			addValue("Potential Settlement", potentialSettlementValue);
			value += potentialSettlementValue;
			addValue("Potential Port", potentialPortValue);
		}
		
		// 7
		if (false) {
			double potentialRoadsValue = SOC.computeRoadOptions(p.getPlayerNum(), b).size();
			value += potentialRoadsValue;
			addValue("Potential roads", potentialRoadsValue);
		}
		
		// 8
		{
		}
		
		// rate the PLAYER
		
		// 1. We dont want too many cards in our hand
		// 2. Similar to longest road in that we want the delta from other army size to grow logarithmically
		// 3. Points...obviously!
		// 4. Add value to new development cards
		// 5. Add value to the ability to build 
		
		// 1.
		{
			double cardCountValue = 0;
			int cardsLeft = p.getTotalCardsLeftInHand();
			int maxCards = soc.getRules().getMinHandSizeForGiveup();
			if (cardsLeft <= maxCards) {
				cardCountValue = (double)cardsLeft / maxCards;
			} else {
				cardCountValue = (double)(maxCards/2) / cardsLeft;
			}
			addValue("Card Count", cardCountValue);
			value += cardCountValue;
		}
		
		// 2.
		{
			int mine = 0;
			int theirs = 0;
			for (Player pp : soc.getPlayers()) {
				int size = pp.getArmySize();
				if (pp == p) {
					mine = size;
				} else if (size > theirs) {
					theirs = size;
				}
			}
			int delta = mine-theirs;
			double armySizeValue = 0;
			if (delta <= 0) {
				armySizeValue = delta;
			} else {
				armySizeValue = Math.log(delta);
			}
			addValue("Army Size", armySizeValue);
			value += armySizeValue;
		}	
		// 3. Points
		{
			double pointsValue = SOC.computePointsForPlayer(p, b, soc);
			addValue("Points", pointsValue);
			value += pointsValue;
		}
		
		// 4. Developement cards
		{
			double newDevelCards = 0;
			for (DevelopmentCardType t : DevelopmentCardType.values()) {
				newDevelCards += p.getCardCount(CardType.Development);
			}
			value += 0.1 * newDevelCards;
			addValue("New Devel Cards", newDevelCards);
		}
		
		// 5. Buildables
		{
			double buildableValue = 0;
			for (BuildableType t : BuildableType.values()) {
				if (p.canBuild(t)) {
					buildableValue += 0.05 * (1+t.ordinal());
				}
			}
			value += buildableValue;
			addValue("Buildables", buildableValue);
		}
		
		// Rate the seafarers expansion items
		if (soc.getRules().isEnableSeafarersExpansion()) {
			value += evaluateSeafarers(p, b, soc, debugOutput);
		}

		value += 0.0001 * (Utils.getRandom().nextDouble()-0.5); // apply just a slight amount of randomness to avoid duplicate values among nodes
		debugOutput.print(buf.toString());

		return (float)value;
	}

	private static class DistanceInfo {
		HashSet<Integer> verts = new HashSet<Integer>();
		int minDist = Integer.MAX_VALUE;
		int islandNum; // 0 means a territory
	}
	
	public double evaluateSeafarers(Player p, Board b, SOC soc, PrintWriter debugOutput) {
		
		double value = 0;
		
		// 1. Evaluate Ship placement.  Ships that reveal a undiscovered tile are good.  Also ships that discover a new island are VERY good.
		// 2. Evaluate the placement of the pirate
		// 3. Potential ships
		// 4. Evaluate distance(s) to undiscovered territories and islands, shorter is better 
		// 5. Ratio of ships to roads should be 50/50 ish

		// 1.
		{
			double shipsValue = 0;
			boolean [] islands = new boolean[b.getNumIslands()+1];
			for (Route e : b.getShipRoutesForPlayer(p.getPlayerNum())) {
				for (Tile t : b.getTilesAdjacentToVertex(e.getFrom())) {
					if (t.getType() == TileType.UNDISCOVERED) {
						shipsValue += 2;
					} else if (t.getIslandNum() > 0 && !islands[t.getIslandNum()] && !b.isIslandDiscovered(p.getPlayerNum(), t.getIslandNum())) {
						shipsValue += 1;
						islands[t.getIslandNum()] = true;
					}
				}
				for (Tile t : b.getTilesAdjacentToVertex(e.getTo())) {
					if (t.getType() == TileType.UNDISCOVERED) {
						shipsValue += 2;
					} else if (t.getIslandNum() > 0 && !islands[t.getIslandNum()] && !b.isIslandDiscovered(p.getPlayerNum(), t.getIslandNum())) {
						shipsValue += 1;
						islands[t.getIslandNum()] = true;
					}
				}
			}
			addValue("Ships", shipsValue);
			value += shipsValue;
		}
		
		// 2.
		{
			double pirateValue = 0;
			if (b.getPirateTile() >= 0) {
				Tile cell = b.getTile(b.getPirateTile());
				for (Route r : b.getTileRoutes(cell)) {
					if (!r.isShip())
						continue;
					if (r.getPlayer() == p.getPlayerNum())
						pirateValue -= 5;
					else if (r.getPlayer() > 0) {
						double numMovable = SOC.computeMovableShipOptions(r.getPlayer(), b).size();
						pirateValue += soc.getPlayerByPlayerNum(r.getPlayer()).getTotalCardsLeftInHand() - 0.1 * numMovable;
					}
				}
			}
			value += pirateValue;
			addValue("Pirate", pirateValue);
		}
		// 3.
		{
			double num = SOC.computeShipOptions(p.getPlayerNum(), b).size();
			double potentialShipValue = 0.02 * Math.log(1+num);
			addValue("Potential Ships", potentialShipValue);
			value += potentialShipValue;
		}
		
		// 4.
		{
			double distanceValue = 0;
			// we want the number of moves to reach an undiscovered island/territory from any or our current positions
			// we dont need the actual path, just the distance (# of moves)

			List<DistanceInfo> distances = new ArrayList<DistanceInfo>();

			for (int i=1; i<=b.getNumIslands(); i++) {
				if (!b.isIslandDiscovered(p.getPlayerNum(), i)) {
					DistanceInfo info = new DistanceInfo();
					distances.add(info);
					info.islandNum = i;
					for (int tIndex : b.getIsland(i).getTiles()) {
						info.verts.addAll(b.getTile(tIndex).getAdjVerts());
					}
				}
			}
			
			// find the verts we can touch
			for (Tile t : b.getTiles()) {
				boolean addIt = false;
				if (t.getType() == TileType.UNDISCOVERED) {
					DistanceInfo info = new DistanceInfo();
					distances.add(info);
					info.verts.addAll(t.getAdjVerts());
				}
			}
//			debugOutput.println("discoverableV=" + discoverableV);
			
			if (distances.size() > 0) {
    			// Floyd-Marshall Algorithm.  Find distances without path knowledge O(|V|^3)
				int [][] dist = new int[b.getNumVerts()][b.getNumVerts()];
				for (int i=0; i<dist.length; i++) {
					Arrays.fill(dist[i], 10000);
					dist[i][i] = 0; // distance to itself is always 0
				}
				
    			// compute the edges.  Only unowned edges are considered 
    			for (Route r : b.getRoutes()) {
    				if (r.getPlayer() == 0) {
    					if (r.isAdjacentToLand() || !r.isAttacked()) {
            				dist[r.getFrom()][r.getTo()] = dist[r.getTo()][r.getFrom()] = 1;
    					} 
    					/*
    					else if (!r.isAttacked()) {
    						// if an endpoint of a vertex canPlaceStructure, but doesnt have a structure, then add +1 to weight (an extra move to build the structure)
    						// this is to promote building on a shoreline to reduce distances.
    						Vertex v0 = b.getVertex(r.getFrom());
    						Vertex v1 = b.getVertex(r.getTo());
    						int weight = 1;
    						if (v0.getCanPlaceStructure() && v0.getPlayer() != p.getPlayerNum()) { 
    							weight++;
    						}
    						if (v1.getCanPlaceStructure() && v1.getPlayer() != p.getPlayerNum()) {
    							weight++;
    						}  
            				dist[r.getFrom()][r.getTo()] = dist[r.getTo()][r.getFrom()] = weight;
    					}*/
    				}
    			}
    			
    			for (int k=0; k<dist.length; k++) {
    				for (int i=0; i<dist.length; i++) {
    					for (int j=0; j<dist.length; j++) {
    						int d = dist[i][k] + dist[k][j];
    						if (dist[i][j] > d) {
    							dist[i][j] = d;
    						}
    					}
    				}
    			}
    			
    			// We have our lookup table, so now figure out the distances from our verts to the discoverables
    			// these are all the verts of the available routes
    			
    			// may need to cache these values into a lookup somehow
    			List<Integer> sourceV = new ArrayList<Integer>();
    			for (int vIndex=0; vIndex<b.getNumVerts(); vIndex++) {
    				Vertex v = b.getVertex(vIndex);
    				boolean open = false;
    				boolean owned = v.getPlayer() == p.getPlayerNum();
    				for (int i=0; i<v.getNumAdjacent(); i++) {
    					Route r = b.getRoute(vIndex, v.getAdjacent()[i]);
    					if (r.getPlayer() == 0) {
    						open = true;
    					} else if (r.getPlayer() == p.getPlayerNum()) {
    						owned = true;
    					}
    				}
    				if (open && owned) {
    					sourceV.add(vIndex);
    					for (DistanceInfo d : distances) {
    						for (int dvIndex : d.verts) {
    							int dst = dist[vIndex][dvIndex];
    							if (dst < d.minDist) {
    								d.minDist = dst;
    							}
    						}
    					}
    				}
    			}
    			//debugOutput.println("sourceV=" + sourceV);
    			double sum = 0;
    			for (DistanceInfo d : distances) {
//    				debugOutput.println("Min Distance to " + d.islandNum + "=" + d.minDist);
    				sum += d.minDist;
    			}
    			if (sum > 0) {
    				distanceValue = 2 * distances.size() / sum;
    			}
			}
			addValue("Undisc aveMinDist", distanceValue);
			value += distanceValue;
		}
		
		{
			double routeRatio = 0;
			double numShips = 0;
			double numRoads = 0;
			for (Route r : b.getRoutes()) {
				if (r.getPlayer() == p.getPlayerNum()) {
					if (r.isShip())
						numShips++;
					else
						numRoads++;
				}
			}
			
			if (numRoads > 0 || numShips > 0) {
    			routeRatio = ((numShips + numRoads) - Math.abs(numShips - numRoads)) / (numShips + numRoads);
    			addValue("Route ratio", routeRatio);
    			value += routeRatio;
			}
		}
		
		return value;
	}
	
	public double evaluateCAK(Player p, Board b, SOC soc, PrintWriter debugOutput) {
		// evaluate knights strength vs. other players
		
		// evaluate city walls
		
		// evaluate progress cards
		
		// evaluate city development areas
		
		// evaluate metropolis
		return 0;
	}
	
	@Override
	public void onOptimalPath(SOC soc, AINode optimal) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBeginNewDescisionTree() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDescisionTreeComplete() {
		// TODO Auto-generated method stub
		
	}

	
}
