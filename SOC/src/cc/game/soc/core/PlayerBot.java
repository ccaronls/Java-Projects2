package cc.game.soc.core;

import java.io.PrintStream;
import java.util.*;

import cc.lib.game.Utils;
import cc.lib.math.CMath;

public class PlayerBot extends Player {

	private BotNode movesPath = null; 
//	private LinkedList<BotNode> leafNodes = new LinkedList<BotNode>();
	private Set<Integer> usedRoutes = new HashSet<Integer>();
	
	private BotNode createNewTree() {
		BotNode node = new BotNodeRoot();
		
	//	leafNodes.clear();
		usedRoutes.clear();
		// populate with ALL evaluations that will propogate throughout tree
		
		return node;
	}
	
	@SuppressWarnings("unchecked")
	private <T> T detatchMove() {
		BotNode front = movesPath;
		movesPath = movesPath.next;
		return (T)front.getData();
	}
	
	private void findChangingProperties(BotNode leaf, HashSet<String> changes) {
		HashMap<String,List<Double>> all = new HashMap<String, List<Double>>();
		while (leaf != null) {
    		for (Map.Entry<String, Double> e : leaf.properties.entrySet()) {
    			if (!all.containsKey(e.getKey())) {
    				all.put(e.getKey(), new ArrayList<Double>());
    			}
    			all.get(e.getKey()).add(e.getValue());
    		}
    		leaf = leaf.parent;
		}
		for (Map.Entry<String, List<Double>> e : all.entrySet()) {
			if (e.getValue().size()>1) {
				changes.add(e.getKey());
			}
		}
	}
	
	private void dumpAllPaths(List<BotNode> leafs, PrintStream out) {
		// print out all move paths such that the optimal is printed last
		Collections.sort(leafs);
		Collections.reverse(leafs);

		for (BotNode leaf : leafs) {
			HashSet<String> changes = new HashSet<String>();
			findChangingProperties(leaf, changes);
			int maxWidth = 20;
			for (String s : changes) {
				maxWidth = Math.max(maxWidth, s.length());
			}
			LinkedList<BotNode> moves = new LinkedList<BotNode>();
			for (BotNode n = leaf; n!=null; n=n.parent) {
				moves.addFirst(n);
			}
			
			{
    			for (BotNode n : moves) {
    				out.print(String.format("%-" + maxWidth + "s(%4f)", n.getDescription(), n.getValue()));
    			}
    			out.println();
			}
			
			for (String key : changes) {
				for (BotNode n : moves) {
					out.print(String.format("  %-" +maxWidth+ "s %4f", key, n.properties.get(key)));
				}
    			out.println();
			}
		}
	}
	
	private void buildOptimalPath(BotNode root) {
		ArrayList<BotNode> leafs = new ArrayList<BotNode>();
		BotNode child = buildOptimalPathR(root, leafs);
		child.setOptimal(true);
		if(Utils.DEBUG_ENABLED) {
//			root.printTree(System.out);
			dumpAllPaths(leafs, System.out);
		}
		while (child.parent != null && child.parent.getData() != null) {
			child.parent.next = child;
			child = child.parent;
		}
		movesPath = child;
		
		
		
		/*
		if (Utils.DEBUG_ENABLED) {
			StringBuffer buf = new StringBuffer();
			buf.append("OPTIMAL PATH=");
			for (BotNode c = movesPath; c!=null; c=c.next) {
				buf.append("{").append(c).append("}");
			}
			System.out.println(buf.toString());
		}*/
		// summarize the data the evaluation
		// we want to know what variable(s) contribute most to the descision
		// track ave,stddev,min,max
		//summarizeEvaluation();
		
	}
	/*
	
	private final static class Summary implements Comparable<Summary>{
		String key;
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		double ave = 0;
		double sum = 0;
		double stdDev = 0;
		double winner = 0;
		List<Double> values = new ArrayList<Double>();
		
		@Override
		public int compareTo(Summary o) {
			if (o.winner < winner) {
				return -1;
			} else if (o.winner > winner) {
				return 1;
			}
			
			return 0;
		}
		
		
	}

	public void debugDump(PrintStream out) {
		ArrayList<BotNode> leafs = new ArrayList<BotNode>();
		findLeafsR(root, leafs);
	}
	private void summarizeEvaluation() {
		Map<String,Summary> map = new HashMap<String, PlayerBot.Summary>();
		Map<String, Integer> maxContributors = new HashMap<String, Integer>();
		int strlen = 0;
		for (BotNode node : evaluatedNodes) {
			double maxMagValue = -Double.MAX_VALUE;
			String maxMagContributor = null;
			for (String key : node.properties.keySet()) {
				strlen = Math.max(key.length(), strlen);
				Summary summary = null;
				if (!map.containsKey(key)) {
					summary = new Summary();
					summary.key = key;
					map.put(key, summary);
				} else {
					summary = map.get(key);
				}
				double value = node.properties.get(key);
				summary.values.add(value);
				summary.max = Math.max(summary.max, value);
				summary.min = Math.min(summary.min, value);
				summary.sum += value;
				if (node.isOptimal()) {
					summary.winner = value;
				}
				if (Math.abs(value) > maxMagValue) {
					maxMagValue = Math.abs(value);
					maxMagContributor = key;
				}
			}

			if (!maxContributors.containsKey(maxMagContributor)) {
				maxContributors.put(maxMagContributor, 1);
			} else {
				int mag = maxContributors.get(maxMagContributor);
				mag ++;
				maxContributors.put(maxMagContributor, mag);
			}
		}

		double maxWinner = -Double.MAX_VALUE;
		String maxContributor = "dunno";
		List<Summary> mapValues = new ArrayList<PlayerBot.Summary>();
		mapValues.addAll(map.values());
		Collections.sort(mapValues);
		for (Summary s : mapValues) {
			s.ave = s.sum / s.values.size();
			s.stdDev = CMath.stdDev(s.values, s.ave);
			if (s.winner > maxWinner) {
				maxWinner = s.winner;
				maxContributor = s.key;
			}
		}
		
		StringBuffer buf = new StringBuffer();
		buf.append("**** SUMMARY *************************************************************\n");
		buf.append(String.format("%-" + strlen + "s %7s %7s %7s %7s %7s\n", "KEY", "MIN", "MAX", "AVE", "STDDEV", "WINNER"));
		for (Summary s : map.values()) {
			if (s.stdDev > 0) // dont need to consider these values since they were always the same 
				buf.append(String.format("%-" + strlen + "s %7.4f %7.4f %7.4f %7.4f %7.4f\n", s.key, s.min, s.max, s.ave, s.stdDev, s.winner));
		}
		buf.append(maxContributors.toString().replaceAll(",", "\n"));
		buf.append("\n**** END SUMMARY *********************************************************");
		System.out.println(buf.toString());
	}*/
	
	private BotNode buildOptimalPathR(BotNode root, List<BotNode> leafs) {
		if (root.isLeaf()) {
			leafs.add(root);
			return root;
		} else {
			BotNode best = null;
			for (BotNode child : root.children) {
				BotNode node = buildOptimalPathR(child, leafs);
				if (best == null)
					best = node;
				else if (node.getValue() > best.getValue()) {
					best = node;
				} 
			}
			return best;
		}
	}
/*	
	private void doEvaluate(BotNode node, SOC soc, Player p, Board b) {
		properties = node.properties;
		evaluateBoard(soc, p.getPlayerNum(), b);
		evaluateCards(soc, p);
		addValue("Random", 0.0001 * (Utils.getRandom().nextDouble()-0.5)); // apply just a slight amount of randomness to avoid duplicate values among nodes
		evaluatedNodes.add(node);
	}*/
	
	private void buildRoutesTree(SOC soc, Player p, Board b, BotNode root) {
		List<Integer> roads1 = SOC.computeRoadRouteIndices(p.getPlayerNum(), b);
		List<Integer> ships1 = Collections.emptyList();
		if (soc.getRules().isEnableSeafarersExpansion()) {
			ships1 = SOC.computeShipRouteIndices(p.getPlayerNum(), b);
		}
		
		usedRoutes.addAll(roads1);
		usedRoutes.addAll(ships1);
		
		if (ships1.size() > 0 && roads1.size() > 0) {
			{
    			BotNode roadOptions = root.attach(new BotNodeEnum(RouteChoiceType.ROAD_CHOICE));
    			for (int roadIndex1 : roads1) {
    				b.setPlayerForEdge(roadIndex1, p.getPlayerNum());
    				Route r = b.getRoute(roadIndex1);
    				//r.setLocked(true);
    				BotNode n = roadOptions.attach(new BotNodeRoute(r, roadIndex1));
    				
    				List<Integer> roads2 = SOC.computeRoadRouteIndices(p.getPlayerNum(), b);
    				roads2.removeAll(usedRoutes);
    				usedRoutes.addAll(roads2);
    				
    				for (int roadIndex2 : roads2) {
    					Route r2 = b.getRoute(roadIndex2);
    					b.setPlayerForEdge(roadIndex2, p.getPlayerNum());
    					//r2.setLocked(true);
    					buildChooseMoveTreeR(soc, p, b, n.attach(new BotNodeRoute(r2, roadIndex2)), SOC.computeMoves(p, b, soc));
    					b.setPlayerForEdge(roadIndex2, 0);
    					//r2.setLocked(false);
    				}
    				b.setPlayerForEdge(roadIndex1, 0);
    				//r.setLocked(false);
    			}
			}
			{
    			BotNode shipOptions = root.attach(new BotNodeEnum(RouteChoiceType.SHIP_CHOICE));
    			for (int shipIndex1 : ships1) {
    				b.setPlayerForEdge(shipIndex1, p.getPlayerNum());
    				Route ship = b.getRoute(shipIndex1);
    				ship.setShip(true);
    				//ship.setLocked(true);
    				BotNode n = shipOptions.attach(new BotNodeRoute(ship, shipIndex1));
    				
    				List<Integer> ships2 = SOC.computeShipRouteIndices(p.getPlayerNum(), b);
    				ships2.removeAll(usedRoutes);
    				usedRoutes.addAll(ships2);
    				
    				for (int shipIndex2 : ships2) {
    					Route ship2 = b.getRoute(shipIndex2);
    					b.setPlayerForEdge(shipIndex2, p.getPlayerNum());
    					ship2.setShip(true);
    					//ship2.setLocked(true);
    					buildChooseMoveTreeR(soc, p, b, n.attach(new BotNodeRoute(ship2, shipIndex2)), SOC.computeMoves(p, b, soc));
    					b.setPlayerForEdge(shipIndex2, 0);
    					ship2.setShip(false);
    					//ship2.setLocked(false);
    				}
    				b.setPlayerForEdge(shipIndex1, 0);
    				ship.setShip(false);
    			}
			}
		} else if (ships1.size() > 0) {
			for (int shipIndex1 : ships1) {
				b.setPlayerForEdge(shipIndex1, p.getPlayerNum());
				Route ship = b.getRoute(shipIndex1);
				ship.setShip(true);
				//ship.setLocked(true);
				BotNode n = root.attach(new BotNodeRoute(ship, shipIndex1));

				List<Integer> ships2 = SOC.computeShipRouteIndices(p.getPlayerNum(), b);
				ships2.removeAll(usedRoutes);
				usedRoutes.addAll(ships2);
				
				for (int shipIndex2 : ships2) {
					Route ship2 = b.getRoute(shipIndex2);
					b.setPlayerForEdge(shipIndex2, p.getPlayerNum());
					ship2.setShip(true);
					//ship2.setLocked(true);
					buildChooseMoveTreeR(soc, p, b, n.attach(new BotNodeRoute(ship2, shipIndex2)), SOC.computeMoves(p, b, soc));
					b.setPlayerForEdge(shipIndex2, 0);
					ship2.setShip(false);
					//ship2.setLocked(false);
				}
				b.setPlayerForEdge(shipIndex1, 0);
				ship.setShip(false);
				//ship.setLocked(false);
			}
		} else if (roads1.size() > 0) {
			for (int roadIndex1 : roads1) {
				b.setPlayerForEdge(roadIndex1, p.getPlayerNum());
				Route r = b.getRoute(roadIndex1);
				//r.setLocked(true);
				BotNode n = root.attach(new BotNodeRoute(r, roadIndex1));

				List<Integer> roads2 = SOC.computeRoadRouteIndices(p.getPlayerNum(), b);
				roads2.removeAll(usedRoutes);
				usedRoutes.addAll(roads2);
				
				for (int roadIndex2 : roads2) {
					Route r2 = b.getRoute(roadIndex2);
					//r2.setLocked(true);
					b.setPlayerForEdge(roadIndex2, p.getPlayerNum());
					buildChooseMoveTreeR(soc, p, b, n.attach(new BotNodeRoute(r2, roadIndex2)), SOC.computeMoves(p, b, soc));
					b.setPlayerForEdge(roadIndex2, 0);
					//r2.setLocked(false);
				}
				b.setPlayerForEdge(roadIndex1, 0);
				//r.setLocked(false);
			}
		}		
	}
	
	private void buildChooseMoveTreeR(SOC soc, Player p, Board b, BotNode _root, List<MoveType> moves) {
		//Collections.sort(moves);
		for (MoveType move : moves) {
			BotNode root = _root.attach(new BotNodeEnum(move));
			switch (move) {
				case CONTINUE:
					//doEvaluate(root, soc, p, b);
					// nothing to evaluate since we inherit all values from our parent
					break;
				case TRADE: {
					List<Trade> trades = SOC.computeTrades(p, b);
					for (Trade trade : trades) {
						BotNode n = root.attach(new BotNodeTrade(trade));
						p.incrementResource(trade.getType(), -trade.getAmount());
						for (ResourceType r : ResourceType.values()) {
							p.addCard(r);
							BotNode node = n.attach(new BotNodeCard(r));
							evaluatePlayer(node, soc, p, b);
							buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
							p.removeCard(r);
						}
						if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
							for (CommodityType c : CommodityType.values()) {
								p.addCard(c);
								buildChooseMoveTreeR(soc, p, b, n.attach(new BotNodeCard(c)), SOC.computeMoves(p, b, soc));
								p.removeCard(c);
							}
						}
						p.incrementResource(trade.getType(), trade.getAmount());
					}
					break;
				}
				case BUILD_SETTLEMENT: {
					List<Integer> verts = SOC.computeSettlementVertexIndices(soc, p.getPlayerNum(), b);
					p.adjustResourcesForBuildable(BuildableType.Settlement, -1);
					for (int vIndex : verts) {
						Vertex v = b.getVertex(vIndex);
						v.setType(VertexType.SETTLEMENT);
						v.setPlayer(p.getPlayerNum());
						BotNode node = root.attach(new BotNodeVertex(v, vIndex));
						evaluateVertices(node, soc, p, b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
						v.setPlayer(0);
						v.setType(VertexType.OPEN);
					}
					p.adjustResourcesForBuildable(BuildableType.Settlement, 1);
					break;
				}
				case BUILD_CITY: {
					List<Integer> verts = SOC.computeCityVertxIndices(p.getPlayerNum(), b);
					p.adjustResourcesForBuildable(BuildableType.City, -1);
					for (int vIndex : verts) {
						Vertex v = b.getVertex(vIndex);
						v.setType(VertexType.CITY);
						BotNode node = root.attach(new BotNodeVertex(v, vIndex));
						evaluateVertices(node, soc, p, b);
						buildChooseMoveTreeR(soc, p, b, root.attach(node), SOC.computeMoves(p, b, soc));
						v.setType(VertexType.SETTLEMENT);
					}
					p.adjustResourcesForBuildable(BuildableType.City, 1);
					break;
				}
				case BUILD_CITY_WALL:{
					List<Integer> verts = SOC.computeCityVertxIndices(p.getPlayerNum(), b);
					p.adjustResourcesForBuildable(BuildableType.CityWall, -1);
					for (int vIndex : verts) {
						Vertex v = b.getVertex(vIndex);
						v.setType(VertexType.WALLED_CITY);
						BotNode node = root.attach(new BotNodeVertex(v, vIndex));
						evaluateVertices(node, soc, p, b);
						buildChooseMoveTreeR(soc, p, b, root.attach(node), SOC.computeMoves(p, b, soc));
						v.setType(VertexType.CITY);
					}
					p.adjustResourcesForBuildable(BuildableType.CityWall, 1);
					break;
				}
				case BUILD_ROAD: {
					List<Integer> roads = SOC.computeRoadRouteIndices(p.getPlayerNum(), b);
					roads.removeAll(usedRoutes);
					usedRoutes.addAll(roads);

					p.adjustResourcesForBuildable(BuildableType.Road, -1);
					for (int rIndex : roads) {
						Route r = b.getRoute(rIndex);
						//r.setLocked(true);
						b.setPlayerForRoute(r, p.getPlayerNum());
						BotNode node = root.attach(new BotNodeRoute(r, rIndex));
						evaluateEdges(node, soc, p, b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
						//r.setLocked(false);
						b.setPlayerForRoute(r, 0);
					}
					p.adjustResourcesForBuildable(BuildableType.Road, 1);
					break;
				}
				case ROAD_BUILDING_CARD: {
					buildRoutesTree(soc, p, b, root);
					break;
				}
				case BISHOP_CARD:
				case SOLDIER_CARD: {
					int saveRobber = b.getRobberTile();
					int savePirate = b.getPirateTile();
					for (int tIndex : SOC.computeRobberTileIndices(soc, b)) {
						Tile t = b.getTile(tIndex);
						if (t.isWater())
							b.setPirate(tIndex);
						else
							b.setRobber(tIndex);
						BotNode node = root.attach(new BotNodeTile(t, tIndex));
						evaluateTiles(node, soc, p, b);
						b.setRobber(saveRobber);
						b.setPirate(savePirate);
					}
					break;
				}
				case YEAR_OF_PLENTY_CARD: {
					for (ResourceType r : ResourceType.values()) {
						p.incrementResource(r, 1);
						BotNode n = root.attach(new BotNodeCard(r));
						for (ResourceType r2 : ResourceType.values()) {
							p.incrementResource(r2, 1);
							BotNode node = n.attach(new BotNodeCard(r2));
							evaluatePlayer(node, soc, p, b);
							buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
							p.incrementResource(r2, -1);
						}
						p.incrementResource(r, -1);
					}
					break;
				}
				case BUILD_SHIP:{
					List<Integer> ships = SOC.computeShipRouteIndices(p.getPlayerNum(), b);
					ships.removeAll(usedRoutes);
					usedRoutes.addAll(ships);
					
					p.adjustResourcesForBuildable(BuildableType.Ship, -1);
					for (int rIndex : ships) {
						Route r = b.getRoute(rIndex);
						b.setPlayerForRoute(r, p.getPlayerNum());
						r.setShip(true);
						//r.setLocked(true);
						BotNode node = root.attach(new BotNodeRoute(r, rIndex));
						evaluateEdges(node, soc, p, b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
						b.setPlayerForRoute(r, 0);
						r.setShip(false);
						//r.setLocked(false);
					}
					p.adjustResourcesForBuildable(BuildableType.Ship, 1);
					break;
				}
				case DRAW_DEVELOPMENT: {
					p.adjustResourcesForBuildable(BuildableType.Development, -1);
					Card temp = new Card(DevelopmentCardType.Soldier, CardStatus.UNUSABLE);
					p.addCard(temp);
					evaluatePlayer(root, soc, p, b);
					p.removeCard(temp);
					p.adjustResourcesForBuildable(BuildableType.Development, 1);
					break;
				}
				case HIRE_KNIGHT: {
					p.adjustResourcesForBuildable(BuildableType.Knight, -1);
					List<Integer> verts = SOC.computeNewKnightVertexIndices(p.getPlayerNum(), b);
					for (int vIndex : verts) {
						Vertex v = b.getVertex(vIndex);
						v.setPlayer(p.getPlayerNum());
						v.setType(VertexType.BASIC_KNIGHT_INACTIVE);
						BotNode node = root.attach(new BotNodeVertex(v, vIndex));
						evaluateVertices(node, soc, p, b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
						v.setPlayer(0);
						v.setType(VertexType.OPEN);
					}
					p.adjustResourcesForBuildable(BuildableType.Knight, 1);
					break;
				}
				case ACTIVATE_KNIGHT: {
					p.adjustResourcesForBuildable(BuildableType.ActivateKnight, -1);
					List<Integer> verts = SOC.computeActivateKnightVertexIndices(p.getPlayerNum(), b);
					for (int vIndex : verts) {
						Vertex v = b.getVertex(vIndex);
						v.activateKnight();
						v.setType(VertexType.BASIC_KNIGHT_ACTIVE);
						buildChooseMoveTreeR(soc, p, b, root.attach(new BotNodeVertex(v, vIndex)), SOC.computeMoves(p, b, soc));
						v.deactivateKnight();
					}
					p.adjustResourcesForBuildable(BuildableType.ActivateKnight, 1);
					break;
				}
				case IMPROVE_CITY_POLITICS: {
					DevelopmentArea area = DevelopmentArea.Politics;
					int commodity = p.getCityDevelopment(area);
					p.setCityDevelopment(area, commodity+1);
					p.incrementResource(area.commodity, -(commodity+1));
					evaluatePlayer(root, soc, p, b);
					buildChooseMoveTreeR(soc, p, b, root, SOC.computeMoves(p, b, soc));
					p.setCityDevelopment(area, commodity);
					p.incrementResource(area.commodity, commodity+1);
					break;
				}
				case IMPROVE_CITY_SCIENCE: {
					DevelopmentArea area = DevelopmentArea.Science;
					int commodity = p.getCityDevelopment(area);
					p.setCityDevelopment(area, commodity+1);
					p.incrementResource(area.commodity, -(commodity+1));
					evaluatePlayer(root, soc, p, b);
					buildChooseMoveTreeR(soc, p, b, root, SOC.computeMoves(p, b, soc));
					p.setCityDevelopment(area, commodity);
					p.incrementResource(area.commodity, commodity+1);
					break;
				}
				case IMPROVE_CITY_TRADE: {
					DevelopmentArea area = DevelopmentArea.Trade;
					int commodity = p.getCityDevelopment(area);
					p.setCityDevelopment(area, commodity+1);
					p.incrementResource(area.commodity, -(commodity+1));
					evaluatePlayer(root, soc, p, b);
					buildChooseMoveTreeR(soc, p, b, root, SOC.computeMoves(p, b, soc));
					p.setCityDevelopment(area, commodity);
					p.incrementResource(area.commodity, commodity+1);
					break;
				}
				case MOVE_KNIGHT: {
					List<Integer> knights = SOC.computeMovableKnightVertexIndices(p.getPlayerNum(), b);//b.getVertsOfType(p.getPlayerNum(), VertexType.BASIC_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE);
					for (int knightIndex : knights) {
						Vertex knight = b.getVertex(knightIndex);
						BotNode knightChoice = root.attach(new BotNodeVertex(knight, knightIndex)); 
						List<Integer> knightMoves = SOC.computeKnightMoveVertexIndices(knightIndex, b);
						VertexType saveType = knight.getType();
						knight.setType(VertexType.OPEN);
						for (int moveIndex : knightMoves) {
							Vertex knightMove = b.getVertex(moveIndex);
							VertexType saveType2 = knightMove.getType();
							BotNode knightMoveChoice = knightChoice.attach(new BotNodeVertex(knightMove, moveIndex));
							evaluateVertices(knightMoveChoice, soc, p, b);
							if (saveType2 == VertexType.OPEN) {
								buildChooseMoveTreeR(soc, p, b, knightMoveChoice, SOC.computeMoves(p, b, soc));
							}
							knightMove.setType(saveType2);
						}
						knight.setType(saveType);
					}
					break;
				}
				case MOVE_SHIP: {
					List<Integer> shipVerts = SOC.computeOpenRouteIndices(p.getPlayerNum(), b, false, true);
					for (int shipIndex : shipVerts) {
						Route shipToMove = b.getRoute(shipIndex);
						b.setPlayerForRoute(shipToMove, 0);
						shipToMove.setShip(false);
						shipToMove.setLocked(true);
						BotNode shipChoice = root.attach(new BotNodeRoute(shipToMove, shipIndex));
						List<Integer> newPositions = SOC.computeShipRouteIndices(p.getPlayerNum(), b);
						for (int moveIndex : newPositions) {
							if (moveIndex == shipIndex)
								continue;
							Route newPos = b.getRoute(moveIndex);
							b.setPlayerForRoute(newPos, p.getPlayerNum());
							newPos.setShip(true);
							newPos.setLocked(true);
							BotNode node = new BotNodeRoute(newPos, moveIndex);
							evaluateEdges(node, soc, p, b);
							buildChooseMoveTreeR(soc, p, b, shipChoice.attach(node), SOC.computeMoves(p, b, soc));
							b.setPlayerForRoute(newPos, 0);
							newPos.setShip(false);
							newPos.setLocked(false);
						}
						b.setPlayerForRoute(shipToMove, p.getPlayerNum());
						shipToMove.setShip(true);
						shipToMove.setLocked(false);
					}
					break;
				}
				case PROMOTE_KNIGHT: {
					List<Integer> promotable = SOC.computePromoteKnightVertexIndices(p, b);
					p.adjustResourcesForBuildable(BuildableType.PromoteKnight, -1);
					evaluatePlayer(root, soc, p, b);
					for (int knightIndex : promotable) {
						Vertex knight = b.getVertex(knightIndex);
						knight.promoteKnight();
						BotNode node = new BotNodeVertex(knight, knightIndex);
						evaluateVertices(node, soc, p, b);
						buildChooseMoveTreeR(soc, p, b, root.attach(node), SOC.computeMoves(p, b, soc));
						knight.demoteKnight();
					}
					p.adjustResourcesForBuildable(BuildableType.PromoteKnight, 1);
					break;
				}
				case ROLL_DICE:
					root.properties.clear();
					break;
				case ALCHEMIST_CARD: {
					// cycle throught the 2nd die first, since this affects progress card distribution
					for (int i=1; i<=6; i++) {
						for (int ii=i; ii<=6; ii++) {
							BotNode dice = root.attach(new BotNodeDice(ii, i));
							evaluateDice(dice, ii, i, soc, p, b);
							root.attach(dice);
						}
					}
					break;
				}
				case CRANE_CARD: {
					p.removeCard(ProgressCardType.Crane);
					for (DevelopmentArea area : DevelopmentArea.values()) {
						int count = p.getCardCount(area.commodity);
						if (count == p.getCityDevelopment(area)) {
							p.incrementResource(area.commodity, -count);
							p.setCityDevelopment(area, count+1);
							BotNode node = root.attach(new BotNodeEnum(area));
							evaluatePlayer(node, soc, p, b);
							buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
							p.setCityDevelopment(area, count);
							p.incrementResource(area.commodity, count);
						}
					}
					p.addCard(ProgressCardType.Crane);
					break;
				}
				case DESERTER_CARD: {
					p.removeCard(ProgressCardType.Deserter);
					List<Integer> knights = b.getKnightsForPlayer(0);//getVertsOfType(0, VertexType.BASIC_KNIGHT_ACTIVE, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE);
					for (int knightIndex : knights) {
						Vertex knight = b.getVertex(knightIndex);
						if (knight.getPlayer() != p.getPlayerNum()) {
							int savePlayer = knight.getPlayer();
							knight.setPlayer(p.getPlayerNum());
							BotNode node = root.attach(new BotNodeVertex(knight, knightIndex));
							evaluateVertices(node, soc, p, b);
							buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
							knight.setPlayer(savePlayer);
						}
					}
//					p.addCard(ProgressCardType.Deserter);
					break;
				}
				case DIPLOMAT_CARD: {
					p.removeCard(ProgressCardType.Diplomat);
					List<Integer> allOpenRoutes = SOC.computeDiplomatOpenRouteIndices(soc, b);
					for (int rIndex1 : allOpenRoutes) {
						Route r = b.getRoute(rIndex1);
						int savePlayer = r.getPlayer();
						b.setPlayerForRoute(r, 0);
						BotNode n = root.attach(new BotNodeRoute(r, rIndex1));
						if (savePlayer == p.getPlayerNum()) {
							List<Integer> newPos = SOC.computeRoadRouteIndices(p.getPlayerNum(), b);
							for (int rIndex2 : newPos) {
								Route r2 = b.getRoute(rIndex2);
								b.setPlayerForRoute(r2, p.getPlayerNum());
								BotNode node = n.attach(new BotNodeRoute(r2, rIndex2));
								evaluateEdges(node, soc, p, b);
								buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
								b.setPlayerForRoute(r2, 0);
							}
						} else {
							evaluateEdges(n, soc, p, b);
							buildChooseMoveTreeR(soc, p, b, n, SOC.computeMoves(p, b, soc));
						}
						b.setPlayerForRoute(r, savePlayer);
					}
//					p.addCard(ProgressCardType.Diplomat);
					break;
				}
				case ENGINEER_CARD: {
					p.removeCard(ProgressCardType.Engineer);
					List<Integer> cities = b.getCitiesForPlayer(p.getPlayerNum());
					for (int cIndex : cities) {
						Vertex city = b.getVertex(cIndex);
						city.setType(VertexType.WALLED_CITY);
						buildChooseMoveTreeR(soc, p, b, root.attach(new BotNodeVertex(city, cIndex)), SOC.computeMoves(p, b, soc));
						city.setType(VertexType.CITY);
					}
					p.addCard(ProgressCardType.Engineer);
					break;
				}
				case HARBOR_CARD: {
					p.removeCard(ProgressCardType.Harbor);
					List<Card> resourceCards = p.getCards(CardType.Resource);
					List<Integer> harborPlayers = SOC.computeHarborTradePlayers(p, soc);
					for (Card c : resourceCards) {
						p.removeCard(c);
						BotNode n = root.attach(new BotNodeCard(c));
						for (int num: harborPlayers) {
							Player p2 = soc.getPlayerByPlayerNum(num);
							CommodityType comm = Utils.randItem(CommodityType.values());
							p2.removeCard(comm);
							p.addCard(comm);
							BotNode node = new BotNodeCard(comm);
							evaluateOpponents(node, soc, p, b);
							evaluatePlayer(node, soc, p, b);
							p.removeCard(comm);
							p2.addCard(comm);
						}
						p.addCard(c);
					}	
					p.addCard(ProgressCardType.Harbor);
					break;
				}
				case INTRIGUE_CARD: {
					p.removeCard(ProgressCardType.Intrigue);
					List<Integer> intrigueKnights = SOC.computeIntrigueKnightsVertexIndices(p.getPlayerNum(), b);
					for (int vIndex : intrigueKnights) {
						Vertex v = b.getVertex(vIndex);
						int savePlayer = v.getPlayer();
						v.setPlayer(0);
						// since we dont know where the opponent will position the displaced knight, we evaluate here
						BotNode node = root.attach(new BotNodeVertex(v, vIndex));
						evaluateVertices(node, soc, p, b);
						v.setPlayer(savePlayer);
					}
					p.addCard(ProgressCardType.Intrigue);
					break;
				}
				case INVENTOR_CARD: {
					p.removeCard(ProgressCardType.Inventor);
					List<Integer> tiles = SOC.computeInventorTileIndices(b);
					for (int i=0; i<tiles.size()-1; i++) {
						for (int ii=i+1; ii<tiles.size(); ii++) {
							Tile t0 = b.getTile(i);
							Tile t1 = b.getTile(ii);
							int die0 = t0.getDieNum();
							int die1 = t1.getDieNum();
							if (die0 == die1)
								continue; // ignore these 
							t0.setDieNum(die1);
							t1.setDieNum(die0);
							BotNode node = root.attach(new BotNodeTile(t0, i)).attach(new BotNodeTile(t1, ii));
							evaluateTiles(node, soc, p, b);
							t0.setDieNum(die0);
							t1.setDieNum(die1);
						}
					}
					p.addCard(ProgressCardType.Inventor);
					break;
				}
				case IRRIGATION_CARD: {
					int numGained = SOC.computeNumStructuresAdjacentToTileType(p.getPlayerNum(), b, TileType.FIELDS);
					p.removeCard(ProgressCardType.Irrigation);
					evaluatePlayer(root, soc, p, b);
					if (numGained > 0) {
						p.incrementResource(ResourceType.Wheat, numGained);
						evaluatePlayer(root, soc, p, b);
						buildChooseMoveTreeR(soc, p, b, root, SOC.computeMoves(p, b, soc));
						p.incrementResource(ResourceType.Wheat, -numGained);
					}
					p.addCard(ProgressCardType.Irrigation);
					break;
				}
				case MASTER_MERCHANT_CARD: {
					p.removeCard(ProgressCardType.MasterMerchant);
					int playerToChoose = -1;
					int largestHand = 0;
					for (int playerNum : SOC.computeMasterMerchantPlayers(soc, p)) {
						int num = soc.getPlayerByPlayerNum(playerNum).getUnusedCardCount();
						if (num > largestHand) {
							largestHand = num;
							playerToChoose = playerNum;
						}
					}
					if (playerToChoose >= 0) {
						
						buildChooseMoveTreeR(soc, p, b, root.attach(new BotNodePlayer(soc.getPlayerByPlayerNum(playerToChoose))), moves);
					}
					p.addCard(ProgressCardType.MasterMerchant);
					break;
				}
				case MEDICINE_CARD: {
					if (p.getCardCount(ResourceType.Ore) >= 2 && p.getCardCount(ResourceType.Wheat) >= 1) {
						p.incrementResource(ResourceType.Ore, -2);
						p.incrementResource(ResourceType.Wheat, -1);
						List<Integer> settlements = b.getSettlementsForPlayer(p.getPlayerNum());
						if (settlements.size() > 0) {
							p.removeCard(ProgressCardType.Medicine);
							for (int vIndex : settlements) {
    							Vertex v = b.getVertex(vIndex);
    							v.setType(VertexType.CITY);
    							buildChooseMoveTreeR(soc, p, b, root.attach(new BotNodeVertex(v, vIndex)), SOC.computeMoves(p, b, soc));
    							v.setType(VertexType.SETTLEMENT);
    						}
//							p.addCard(ProgressCardType.Medicine);
						}
						p.incrementResource(ResourceType.Ore, 2);
						p.incrementResource(ResourceType.Wheat, 1);
					}
					break;
				}
				case MERCHANT_CARD: {
					p.removeCard(ProgressCardType.Merchant);
					List<Integer> tiles = SOC.computeMerchantTileIndices(soc, p.getPlayerNum(), b);
					int saveMerchantTile   = b.getMerchantTile();
					int saveMerchantPlayer = b.getMerchantPlayer();
					for (int tIndex : tiles) {
						Tile t = b.getTile(tIndex);
						b.setMerchant(tIndex, p.getPlayerNum());
						buildChooseMoveTreeR(soc, p, b, root.attach(new BotNodeTile(t, tIndex)), SOC.computeMoves(p, b, soc));
					}
					b.setMerchant(saveMerchantTile, saveMerchantPlayer);
//					p.addCard(ProgressCardType.Merchant);
					break;
				}
				case MERCHANT_FLEET_CARD: {
					Card merchantFleetCard = p.getCard(ProgressCardType.MerchantFleet);
					merchantFleetCard.setUsed();
					for (ResourceType r : ResourceType.values()) {
						if (p.getCardCount(r) >= 2) {
							Card tradable = new Card(r, CardStatus.USABLE);
							p.setMerchantFleetTradable(tradable);
							buildChooseMoveTreeR(soc, p, b, root.attach(new BotNodeCard(tradable)), SOC.computeMoves(p, b, soc));
						}
					}
					p.setMerchantFleetTradable(null);
//					merchantFleetCard.setUsed(false);
					break;
				}
				case MINING_CARD: {
					int numGained = SOC.computeNumStructuresAdjacentToTileType(p.getPlayerNum(), b, TileType.MOUNTAINS);
					if (numGained > 0) {
						p.removeCard(ProgressCardType.Mining);
						p.incrementResource(ResourceType.Ore, numGained);
						buildChooseMoveTreeR(soc, p, b, root, SOC.computeMoves(p, b, soc));
						p.incrementResource(ResourceType.Ore, -numGained);
//						p.addCard(ProgressCardType.Mining);
					}

					break;
				}
				case MONOPOLY_CARD:
					p.removeCard(DevelopmentCardType.Monopoly);
					evaluatePlayer(root, soc, p, b);
					for (ResourceType t : ResourceType.values()) {
						BotNode n = root.attach(new BotNodeEnum(t));
						p.incrementResource(t, soc.getNumPlayers()-1);
						evaluatePlayer(n, soc, p, b);
						p.incrementResource(t, -(soc.getNumPlayers()-1));
					}
					p.addCard(DevelopmentCardType.Monopoly);
					break;
				case RESOURCE_MONOPOLY_CARD: {
					p.removeCard(ProgressCardType.ResourceMonopoly);
					evaluatePlayer(root, soc, p, b);
					int num = 2*(soc.getNumPlayers()-1);
					for (ResourceType t : ResourceType.values()) {
						BotNode n = root.attach(new BotNodeEnum(t));
						p.incrementResource(t, num);
						evaluatePlayer(n, soc, p, b);
						p.incrementResource(t, -num);
					}
					p.addCard(ProgressCardType.ResourceMonopoly);
					break;
				}
				case SABOTEUR_CARD: {
					int totalCardsToSabotage = 0;
					for (Player player : soc.getPlayers()) {
						if (p == player)
							continue;
						if (p.getPoints() < player.getPoints()) {
							totalCardsToSabotage += (1+p.getUnusedCardCount()) / 2;
						}
					}
					if (totalCardsToSabotage > 0) {
						p.removeCard(ProgressCardType.Saboteur);
						evaluatePlayer(root, soc, p, b);
						//doEvaluate(root, soc, p, b);
						//evaluateSabotage(p, totalCardsToSabotage);
						p.addCard(ProgressCardType.Saboteur);
					}
					break;
				}
				case SMITH_CARD: {
					List<Integer> promotableKnights = SOC.computePromoteKnightVertexIndices(p, b);
					if (promotableKnights.size() > 0) {
						p.removeCard(ProgressCardType.Smith);
    					if (promotableKnights.size() == 1) {
    						// promotion is automatic
    						Vertex v = b.getVertex(promotableKnights.get(0));
    						v.promoteKnight();
    						buildChooseMoveTreeR(soc, p, b, root, SOC.computeMoves(p, b, soc));
    						v.demoteKnight();
    					} else if (promotableKnights.size() == 2) {
    						// promotion is automatic
    						Vertex v0 = b.getVertex(promotableKnights.get(0));
    						Vertex v1 = b.getVertex(promotableKnights.get(1));
    						v0.promoteKnight();
    						v1.promoteKnight();
    						buildChooseMoveTreeR(soc, p, b, root, SOC.computeMoves(p, b, soc));
    						v0.demoteKnight();
    						v1.demoteKnight();
    					} else {
    						// compute permutations
    						for (int i=0; i<promotableKnights.size()-1; i++) {
        						for (int ii=i+1; ii<promotableKnights.size(); ii++) {
        							Vertex v0 = b.getVertex(i);
        							Vertex v1 = b.getVertex(ii);
        							v0.promoteKnight();
        							v1.promoteKnight();
        							buildChooseMoveTreeR(soc, p, b, root.attach(new BotNodeVertex(v0, i)).attach(new BotNodeVertex(v1, ii)), SOC.computeMoves(p, b, soc));
        							v1.demoteKnight();
        							v0.demoteKnight();
        						}
        					}
    					}
					}
					break;
				}
				case SPY_CARD: {
					p.removeCard(ProgressCardType.Spy);
					evaluatePlayer(root, soc, p, b);
					List<Integer> players = SOC.computeSpyOpponents(soc, p.getPlayerNum());
					for (int num: players) {
						Player player = soc.getPlayerByPlayerNum(num);
						BotNode n = root.attach(new BotNodePlayer(player));
						Card removed = player.removeRandomUnusedCard(CardType.Progress);
						evaluateOpponents(n, soc, player, b);
						player.addCard(removed);
					}
					p.addCard(ProgressCardType.Spy);
					break;
				}
				case TRADE_MONOPOLY_CARD: {
					p.removeCard(ProgressCardType.TradeMonopoly);
					evaluatePlayer(root, soc, p, b);
					for (Player player : soc.getPlayers()) {
						if (player.getPlayerNum() == p.getPlayerNum())
							continue;
						List<Card> cards = player.getUnusedBuildingCards();
						Card removed = Utils.randItem(cards);
						player.removeCard(removed);
						BotNode node = root.attach(new BotNodePlayer(player));
						evaluateOpponents(node, soc, player, b);
						player.addCard(removed);
					}
					p.addCard(ProgressCardType.TradeMonopoly);
					break;
				}
				case WARLORD_CARD: {
					p.removeCard(ProgressCardType.Warlord);
					List<Integer> verts = b.getVertsOfType(p.getPlayerNum(), VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE);
					if (verts.size() > 0) {
						for (int vIndex : verts) {
							b.getVertex(vIndex).activateKnight();
						}
						evaluateVertices(root, soc, p, b);
						buildChooseMoveTreeR(soc, p, b, root, SOC.computeMoves(p, b, soc));
						for (int vIndex : verts) {
							b.getVertex(vIndex).deactivateKnight();
						}
					}
					p.addCard(ProgressCardType.Warlord);
					break;
				}
				case WEDDING_CARD: {
					p.removeCard(ProgressCardType.Wedding);
					evaluatePlayer(root, soc, p, b);
					for (int num : SOC.computeWeddingOpponents(soc, p)) {
						Player player = soc.getPlayerByPlayerNum(num);
						List<Card> cards = SOC.computeGiftCards(soc, player);
						if (cards.size() > 0) {
							Card c = Utils.randItem(cards);
							p.addCard(c);
							player.removeCard(c);
							BotNode node = root.attach(new BotNodeCard(c));
							evaluatePlayer(node, soc, player, b);
							evaluateOpponents(node, soc, player, b);
							player.addCard(c);
							p.removeCard(c);
						}
					}
					p.addCard(ProgressCardType.Wedding);
					break;
				}
			}
		}		
	}
	
	@Override
	public MoveType chooseMove(SOC soc, List<MoveType> moves) {
		if (movesPath != null) {
			return detatchMove();
		}
		assert(moves.size() > 0);
		if (moves.size() == 1)
			return moves.get(0);
		
		BotNode root = createNewTree();
		Player p = new PlayerTemp(this);
		p.setCardsUsable(CardType.Development, false); // prevent generating development card moves on subsequent calls
		buildChooseMoveTreeR(soc, p, soc.getBoard(), root, moves);
		buildOptimalPath(root);
		return detatchMove();
	}

	@Override
	public Vertex chooseVertex(SOC soc, List<Integer> vertexIndices, VertexChoice mode) {
		if (movesPath != null) {
			return detatchMove();
		}
		Board b = soc.getBoard();
		Player p = this;
		BotNode root = createNewTree();
		
		for (int vIndex : vertexIndices) {
			Vertex v = b.getVertex(vIndex);
			Vertex save = v.deepCopy();
			int islandNum = b.getIslandAdjacentToVertex(v);
			boolean discovered = false;
			if (islandNum > 0)
				discovered = b.isIslandDiscovered(p.getPlayerNum(), islandNum);
			switch (mode) {
				case CITY:
					v.setPlayer(p.getPlayerNum());
					v.setType(VertexType.CITY);
					break;
				case CITY_WALL:
					v.setPlayer(p.getPlayerNum());
					v.setType(VertexType.WALLED_CITY);
					break;
				case KNIGHT_DESERTER:
					
					break;
				case KNIGHT_DISPLACED:
					break;
				case KNIGHT_MOVE_POSITION:
					break;
				case KNIGHT_TO_ACTIVATE:
					break;
				case KNIGHT_TO_MOVE:
					break;
				case KNIGHT_TO_PROMOTE:
					break;
				case NEW_KNIGHT:
					break;
				case OPPONENT_KNIGHT_TO_DISPLACE:
					break;
				case POLITICS_METROPOLIS:
					v.setType(VertexType.METROPOLIS_POLITICS);
					break;
				case SCIENCE_METROPOLIS:
					v.setPlayer(p.getPlayerNum());
					v.setType(VertexType.METROPOLIS_SCIENCE);
					break;
				case SETTLEMENT: {
					v.setPlayer(p.getPlayerNum());
					v.setType(VertexType.SETTLEMENT);
					if (islandNum > 0 && !discovered) {
						b.setIslandDiscovered(p.getPlayerNum(), islandNum, true);
					}
					break;
				}
				case TRADE_METROPOLIS:
					v.setPlayer(p.getPlayerNum());
					v.setType(VertexType.METROPOLIS_TRADE);
					break;
			}
			BotNode node = root.attach(new BotNodeVertex(v, vIndex));
			evaluateVertices(node, soc, p, b);
			v.copyFrom(save);
			if (islandNum > 0 && !discovered) {
				b.setIslandDiscovered(p.getPlayerNum(), islandNum, false);
			}
		}
		buildOptimalPath(root);
		return detatchMove();
	}

	@Override
	public Route chooseRoute(SOC soc, List<Integer> routeIndices, RouteChoice mode) {
		if (movesPath != null) {
			return detatchMove();
		}

		BotNode root = createNewTree();
		Board b = soc.getBoard();
		Player p = this;
		
		for (int rIndex : routeIndices) {
			Route r = b.getRoute(rIndex);
			Route save = r.deepCopy();
			switch (mode) {
				case ROAD:
					b.setPlayerForRoute(r, p.getPlayerNum());
					break;
				case ROUTE_DIPLOMAT:
					break;
				case SHIP:
					b.setPlayerForRoute(r, p.getPlayerNum());
					r.setShip(true);
					break;
				case SHIP_TO_MOVE:
					break;
			}
			b.setPlayerForRoute(r, save.getPlayer());
			r.copyFrom(save);
			BotNode node = root.attach(new BotNodeRoute(r, rIndex));
			evaluateEdges(node, soc, p, b);
		}
		buildOptimalPath(root);
		return detatchMove();
	}

	@Override
	public RouteChoiceType chooseRouteType(SOC soc) {
		if (movesPath != null) {
			return detatchMove();
		}
		
		BotNode root = createNewTree();
		Player p = new PlayerTemp(this);
		Board b = soc.getBoard();
		
		List<Integer> roads = SOC.computeRoadRouteIndices(p.getPlayerNum(), b);
		List<Integer> ships = SOC.computeShipRouteIndices(p.getPlayerNum(), b);
		
		BotNode n = root.attach(new BotNodeEnum(RouteChoiceType.ROAD_CHOICE));
		for (int rIndex : roads) {
			Route r = b.getRoute(rIndex);
			b.setPlayerForEdge(rIndex, p.getPlayerNum());
			BotNode node = n.attach(new BotNodeRoute(r, rIndex));
			evaluateEdges(node, soc, p, b);
			b.setPlayerForEdge(rIndex, 0);
		}

		n = root.attach(new BotNodeEnum(RouteChoiceType.SHIP_CHOICE));
		for (int rIndex : ships) {
			Route r = b.getRoute(rIndex);
			b.setPlayerForEdge(rIndex, p.getPlayerNum());
			r.setShip(true);
			BotNode node = n.attach(new BotNodeRoute(r, rIndex));
			evaluateEdges(node, soc, p, b);
			b.setPlayerForEdge(rIndex, 0);
			r.setShip(false);
		}

		buildOptimalPath(root);
		return detatchMove();
	}

	@Override
	public Tile chooseTile(SOC soc, List<Integer> tileIndices, TileChoice mode) {
		if (movesPath != null) {
			return detatchMove();
		}

		Player p = this;
		Board b = soc.getBoard();
		
		BotNode root = createNewTree();
		int pirateSave = b.getPirateTile();
		int robberSave = b.getRobberTile();
		for (int tIndex : tileIndices) {
			Tile t = b.getTile(tIndex);
			if (t.isWater()) {
				b.setPirate(tIndex);
			} else {
				b.setRobber(tIndex);
			}
			BotNode node = root.attach(new BotNodeTile(t, tIndex));
			evaluateTiles(node, soc, p, b);
			b.setPirate(pirateSave);
			b.setRobber(robberSave);
		}
		buildOptimalPath(root);
		return detatchMove();
	}

	@Override
	public Trade chooseTradeOption(SOC soc, List<Trade> trades) {
		if (movesPath != null) {
			return detatchMove();
		}
		// TODO Auto-generated method stub
		assert(false);

		return null;
	}

	@Override
	public Player choosePlayer(SOC soc, List<Integer> playerOptions, PlayerChoice mode) {
		if (movesPath != null) {
			return detatchMove();
		}
		
		if (playerOptions.size() == 1)
			return soc.getPlayerByPlayerNum(playerOptions.get(0));
		
		Player p = this;
		Board b = soc.getBoard();
		
		BotNode root = createNewTree();
		
		for (int num : playerOptions) {
			Player opponent = soc.getPlayerByPlayerNum(num);
			Card card = opponent.removeRandomUnusedCard();
			p.addCard(card);
			BotNode node = root.attach(new BotNodePlayer(opponent));
			evaluateOpponents(node, soc, opponent, b);
			p.removeCard(card);
			opponent.addCard(card);
		}
		
		buildOptimalPath(root);
		return detatchMove();
	}

	@Override
	public Card chooseCard(SOC soc, List<Card> cards, CardChoice mode) {
		if (movesPath != null) {
			return detatchMove();
		}
		
		switch (mode) {
			case EXCHANGE_CARD:
				break;
			case FLEET_TRADE:
				break;
			case GIVEUP_CARD: {
				BotNode root = createNewTree();
				for (Card c : cards) {
					removeCard(c);
					BotNode node = root.attach(new BotNodeCard(c));
					evaluatePlayer(node, soc, this, soc.getBoard());
					addCard(c);
				}
				buildOptimalPath(root);
				return detatchMove();
			}
			case OPPONENT_CARD:
				break;
			case PROGRESS_CARD:
				break;
			case RESOURCE_OR_COMMODITY:
				break;
		}
		
		// TODO Auto-generated method stub
		assert(false);
		return null;
	}

	@Override
	public <T extends Enum<T>> T chooseEnum(SOC soc, EnumChoice mode, T ... values) {
		if (movesPath != null) {
			return detatchMove();
		}
		// TODO Auto-generated method stub
		assert(false);
		return null;
	}

	@Override
	public boolean setDice(int[] die, int num) {
		Integer [] dice = detatchMove();
		for (int i=0; i<num; i++) {
			die[i] = dice[i];
		}
		return true;
	}
	
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

	protected void evaluateStructure(BotNode node, SOC soc, Vertex v, Player p, Board b) {
		double [] resourceProb = new double[SOC.NUM_RESOURCE_TYPES];
		final int [] resourcePorts = new int[SOC.NUM_RESOURCE_TYPES];
		int numMultiPorts = 0;
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
					resourceProb[cell.getResource().ordinal()] += diePossibility[cell.getDieNum()] * scale;
					break;
				case PORT_BRICK:
				case PORT_ORE:
				case PORT_SHEEP:
				case PORT_WHEAT:
				case PORT_WOOD:
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
		double resourceProbValue = 0.5 * CMath.sum(resourceProb);
		//resourceProbValue *= 2;
		node.addValue("Resource Prob", resourceProbValue);
		double resourcePortProb = 0;
		for (int i=0; i<SOC.NUM_RESOURCE_TYPES; i++) {
			if (resourcePorts[i] > 0)
				resourcePortProb += resourceProb[i];
		}
		
		double ave = CMath.sum(resourceProb) / resourceProb.length;
		double stdDev = 0.1 * Math.abs(CMath.stdDev(resourceProb, ave));
		double resourceDistribution = ave - stdDev;
		node.addValue("Resource distribution", resourceDistribution);
		node.addValue("Resource Port Prob", resourcePortProb);
	}
    
    
	/**
	 * evaluate the board's vertices WRT player p.  Call node.addValue with string properties 
	 * @param node
	 * @param soc
	 * @param p
	 * @param b
	 */
	protected void evaluateVertices(BotNode node, SOC soc, Player p, Board b) {
		
		int knightValue = 0;
		
		for (Vertex v : b.getVerticies()) {
			if (v.getPlayer() != p.getPlayerNum())
				continue;
			switch (v.getType()) {
				case OPEN:
					break;
				case SETTLEMENT:
				case CITY:
				case WALLED_CITY:
				case METROPOLIS_POLITICS:
				case METROPOLIS_SCIENCE:
				case METROPOLIS_TRADE:
					evaluateStructure(node, soc, v, p, b);
					break;
				case MIGHTY_KNIGHT_ACTIVE:
				case BASIC_KNIGHT_ACTIVE:
				case STRONG_KNIGHT_ACTIVE:
					knightValue += v.getType().getKnightLevel() * 2;
					break;
				case MIGHTY_KNIGHT_INACTIVE:
				case BASIC_KNIGHT_INACTIVE:
				case STRONG_KNIGHT_INACTIVE:
					knightValue += v.getType().getKnightLevel();
					break;
			}
		}
		
		node.addValue("knights", knightValue);
	}
	
	protected void evaluateEdges(BotNode node, SOC soc, Player p, Board b) {
		// longest road
		{ // sumtin to break out uv
			int longestRoadValue = -1;
    		int len = b.computeMaxRouteLengthForPlayer(p.getPlayerNum(), soc.getRules().isEnableRoadBlock());
    		if (len >= soc.getRules().getMinLongestLoadLen()) {
    			Player cur = soc.getLongestRoadPlayer();
        		if (cur == null || 
        			cur.getPlayerNum() == p.getPlayerNum() || 
        			cur.getRoadLength() < len) {
        			longestRoadValue = 1;
        		}
    		}
    		node.addValue("longestRoad", longestRoadValue);
		}
		
		// evaluate 
	}
	
	protected void evaluateTiles(BotNode node, SOC soc, Player p, Board b) {
		
	}
	
	protected void evaluatePlayer(BotNode node, SOC soc, Player p, Board b) {
		
	}
	
	protected void evaluateDice(BotNode node, int die1, int die2, SOC soc, Player p, Board b) {
		
	}
	
	protected void evaluateOpponents(BotNode node, SOC soc, Player p, Board b) {
		
	}
	
	
	
	/*
	protected void evaluateDice(int die0, int die1, SOC soc, Board b) {
		final int dieNum = die0 + die1;
		
		// value of the dice depends
		float dieValue = 0;
		for (int tIndex=0; tIndex<b.getNumTiles(); tIndex++) {
			Tile tile = b.getTile(tIndex);
			if (tile.getDieNum() != dieNum)
				continue;

			if (b.getRobberTile() == tIndex)
				dieValue -= 2;
			
			for (Vertex v : b.getTileVertices(tile)) {
				if (v.isStructure() && v.getPlayer() != 0) {
					if (v.getPlayer() == getPlayerNum()) {
						dieValue += v.isCity() ? 2 : 1;
					} else {
						dieValue -= v.isCity() ? 1.5f : 0.7f; 
					}
				}
			}
		}
		addValue("dice", dieValue);
		
		// we want die1 to be a value that best gives change to produce a progress card without
		// giving too much chance to the other players
		// NOTE: A six will never produce a progress card.  
		//    This may be desirable if we have the max number of progress card.
		int numProgress = getCardCount(CardType.Progress);
		float progressDie = 0;
		if (numProgress >= soc.getRules().getMaxProgressCards()) {
			if (die1 == 6)
				progressDie += 2;
			else
				progressDie -= die1;
		} else {
			int max = 0;
			for (DevelopmentArea a : DevelopmentArea.values()) {
				max = Math.max(getCityDevelopment(a), max);
			}
			if (die1 == max)
				progressDie += 3;
			else
				progressDie -= die1;
		}
		
		addValue("progressDie", progressDie);
	}

	/**
	 * default behavior returns a value that is higher when we have less than the average card held by others
	 * @param soc
	 * @param p
	 * @param b
	 * @return
	 *
	protected void evaluateMonopolyCard(SOC soc, Player p, Board b) {
		float totalCards = 0;
		for (Player player : soc.getPlayers()) {
			if (player.getPlayerNum() != p.getPlayerNum()) {
				totalCards += player.getTotalResourceCount(); 
			}
		}
		float aveNum = totalCards / (soc.getNumPlayers()-1);
		addValue("monopoly", aveNum - p.getTotalResourceCount()); // if we have less cards than average, then this is good, else bad
	}
	
	/**
	 * 
	 * @param p
	 * @param totalCardsDiscarded
	 * @return
	 *
	protected void evaluateSabotage(Player p, int totalCardsDiscarded) {
		addValue("sabotage", 0.1f * totalCardsDiscarded);
	}
	
	/**
	 * Here we want a value that increases as the number of progress card choices increases
	 * Default simply returns the size of the spied players unused cards / 10
	 * @param spy
	 * @param spied
	 * @return
	 *
	protected void evaluateSpy(Player spy, Player spied) {
		addValue("spy",0.1f * spied.getUnusedCardCount());
	}
	
	/**
	 * default behavior will return a value that is higher when the total cards in our hand after gifting is less than the max
	 * cards allowed before robber discard.  Otherwise if the result will put us over the limit, then this value degrades rapidly.  
	 * 
	 * Simply put: 
	 *   return totalAfterGift < max ? numGiftCards : max - numGiftCards;
	 * 
	 * @param soc
	 * @param receiver
	 * @param numGiftCards
	 * @return
	 *
	protected void evaluateWedding(SOC soc, Player receiver, int numGiftCards) {
		int cardsInHand = receiver.getTotalCardsLeftInHand() + numGiftCards;
		int maxCards = soc.getMinHandCardsForRobberDiscard(receiver.getPlayerNum());
		if (cardsInHand < maxCards) {
			addValue("wedding", numGiftCards);
		} else {
			addValue("wedding", -(maxCards - numGiftCards));
		}
	}
	
	/**
     * Get the probability of a die roll from 2 6 sided dice.
     * @param num
     * @return
     *
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

	protected void evaluateBoard(SOC soc, int playerNum, Board b) {
	
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
				if (i == playerNum) {
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
		}		
		
		// 2. TODO: Seems like this is covered by points already
		if (false) {
			double cityValue = soc.getRules().getPointsPerCity() * b.getNumCitiesForPlayer(playerNum);
			double settlementValue = soc.getRules().getPointsPerSettlement() * b.getNumSettlementsForPlayer(playerNum);
			addValue("City Value", cityValue);
			addValue("Settlement Value", settlementValue);
		}
		
		final int [] resourcePorts = new int[SOC.NUM_RESOURCE_TYPES];
		int numMultiPorts = 0;
		
		// 3. In general, we want the probability of any resource to be high.  But also, we want the probability
		//    of the resources of any 2:1 ports we own to be especially high
		{
			double [] resourceProb = new double[SOC.NUM_RESOURCE_TYPES];
			
			for (Vertex v : b.getVerticies()) {
				if (v.getPlayer() == playerNum) {
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
    							resourceProb[cell.getResource().ordinal()] += diePossibility[cell.getDieNum()] * scale;
    							break;
							case PORT_BRICK:
	    					case PORT_ORE:
	    					case PORT_SHEEP:
	    					case PORT_WHEAT:
	    					case PORT_WOOD:
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
			double resourcePortProb = 0;
			for (int i=0; i<SOC.NUM_RESOURCE_TYPES; i++) {
				if (resourcePorts[i] > 0)
					resourcePortProb += resourceProb[i];
			}
			
			double ave = CMath.sum(resourceProb) / resourceProb.length;
			double stdDev = 0.1 * Math.abs(CMath.stdDev(resourceProb, ave));
			double resourceDistribution = ave - stdDev;
			addValue("Resource distribution", resourceDistribution);
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
			addValue("Resource Port", resourcePortValue);
			double multiPortValue = 0;
			if (numMultiPorts == 1)
				multiPortValue = 0.1;
			else if (numMultiPorts > 1) 
				multiPortValue = -(numMultiPorts-1)/10;
			addValue("Multi Port", multiPortValue);
		}		
		
		// 5. Value the robber.  Robber value is placed next to players with lots of cards in their hand
		{
			double robberValue = 0;
			if (b.getRobberTile() >= 0) {
				Tile cell = b.getTile(b.getRobberTile());
				if (cell.isDistributionTile()) {
					for (int v: cell.getAdjVerts()) {
						Vertex vert = b.getVertex(v);
						if (vert.getPlayer() == playerNum) {
							robberValue += -5;
						} else if (vert.getPlayer() > 0) {
							robberValue += soc.getPlayerByPlayerNum(vert.getPlayer()).getTotalCardsLeftInHand();
						}
					}
				}
			}
			addValue("Robber", robberValue);
		}
		
		// 6. Tally the potential settlements
		if (false && b.getNumStructuresForPlayer(playerNum) >= 2) {
			double potentialSettlementValue = 0;
			double potentialPortValue = 0;
			List<Integer> options= SOC.computeSettlementVertexIndices(soc, playerNum, b);
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
    					case PORT_BRICK:
    					case PORT_ORE:
    					case PORT_SHEEP:
    					case PORT_WHEAT:
    					case PORT_WOOD:
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
			addValue("Potential Port", potentialPortValue);
		}
		
		// 7
		if (false) {
			double potentialRoadsValue = 0.1 * SOC.computeRoadRouteIndices(playerNum, b).size();
			addValue("Potential roads", potentialRoadsValue);
		}
		
		// 8
		{
		}
	}
	
	protected void evaluateCards(SOC soc, Player p) {
		
		// rate the PLAYER's cards
		
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
		}	
		// 3. Points
		{
			double pointsValue = SOC.computePointsFromCards(p, soc);
			addValue("Points", pointsValue);
		}
		
		// 4. Developement cards
		{
			double newDevelCards = 0.1 * p.getUnusableCardCount(CardType.Development);
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
			addValue("Buildables", buildableValue);
		}
	}

	private static class DistanceInfo {
		HashSet<Integer> verts = new HashSet<Integer>();
		int minDist = Integer.MAX_VALUE;
		int islandNum; // 0 means a territory
	}
	
	public void evaluateSeafarers(Player p, Board b, SOC soc) {
		
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
						double numMovable = SOC.computeOpenRouteIndices(r.getPlayer(), b, false, true).size();
						pirateValue += soc.getPlayerByPlayerNum(r.getPlayer()).getTotalCardsLeftInHand() - 0.1 * numMovable;
					}
				}
			}
			addValue("Pirate", pirateValue);
		}
		// 3.
		{
			double num = SOC.computeShipRouteIndices(p.getPlayerNum(), b).size();
			double potentialShipValue = 0.02 * Math.log(1+num);
			addValue("Potential Ships", potentialShipValue);
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
    					}*
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
			}
		}
	}
	
	public void evaluateBoard(SOC soc, Board b, int playerNum) {
		
		// Compute distances between all verts
		int [][] dist = new int[b.getNumVerts()][b.getNumVerts()];
		for (int i=0; i<dist.length; i++) {
			Arrays.fill(dist[i], 10000);
			dist[i][i] = 0; // distance to itself is always 0
		}
		
		for (Route r : b.getRoutes()) {
			int weight = 0;
			if (r.getPlayer() == 0) {
				// roads only we set the dist to 3
				// shoreline = 2
				// ship only 1
				// this is to promote the use of ships when possible to bridge gaps
				if (r.isShoreline())
					weight = 2;
				else if (r.isAdjacentToLand())
					weight = 3;
				else
					weight = 1;
			} else if (r.getPlayer() == playerNum) {

			} else {
				// edge is not accessable
				continue;
			}
			dist[r.getFrom()][r.getTo()] = dist[r.getTo()][r.getFrom()] = weight;
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
		
		float [] vertexValues = new float[b.getNumVerts()];
		List<Integer> myVerts = new ArrayList<Integer>();

		int [] resourceFavor = new int [SOC.NUM_RESOURCE_TYPES];
		boolean [] portResource = new boolean[SOC.NUM_RESOURCE_TYPES];
		boolean multiPort = false;
		
		// now iterate over the verts and attach a value based on the vertex type
		for (int vIndex = 0; vIndex < b.getNumVerts(); vIndex++) {
			Vertex v = b.getVertex(vIndex);
			if (v.getPlayer() == playerNum) {
				for (Tile t : b.getVertexTiles(vIndex)) {
					switch (t.getType()) {
						case FIELDS:
						case FOREST:
						case HILLS:
						case MOUNTAINS:
						case PASTURE:
							resourceFavor[t.getResource().ordinal()] += diePossibility[t.getDieNum()];
							myVerts.add(vIndex);
							break;
						case PORT_BRICK:
						case PORT_ORE:
						case PORT_SHEEP:
						case PORT_WHEAT:
						case PORT_WOOD:
							portResource[t.getResource().ordinal()] = true;
							myVerts.add(vIndex);
							break;
						case PORT_MULTI:
							multiPort = true;
							myVerts.add(vIndex);
							break;
						case GOLD:
							myVerts.add(vIndex);
							break;
					}
				}
				continue;
			}
			if (v.getPlayer() != 0)
				continue; // dont consider verts that are occupied
			float vertexValue = 0;
			int islandNum = 0;
			for (Tile t : b.getVertexTiles(vIndex)) {
				if (t.getIslandNum() > 0)
					islandNum = t.getIslandNum();
				switch (t.getType()) {
					case NONE:
					case WATER:
					case DESERT:
					case RANDOM_PORT:
					case RANDOM_PORT_OR_WATER:
					case RANDOM_RESOURCE:
					case RANDOM_RESOURCE_OR_DESERT:
						// no value
						break;
					case FIELDS:
					case FOREST:
					case HILLS:
					case MOUNTAINS:
					case PASTURE:
						vertexValue += diePossibility[t.getDieNum()];
						break;
					case GOLD:
						vertexValue += diePossibility[t.getDieNum()] * SOC.NUM_RESOURCE_TYPES;
						break;
					case UNDISCOVERED:
						vertexValue += 2;
						break;
					case PORT_BRICK:
					case PORT_ORE:
					case PORT_SHEEP:
					case PORT_WHEAT:
					case PORT_WOOD:
						// these have value only if we do not have the trade advantage
						if (!portResource[t.getResource().ordinal()]) {
							vertexValue += 2;
						}
						break;
					case PORT_MULTI:
						if (!multiPort)
							vertexValue += 1;
						break;
				}
				// check for islands
				if (islandNum > 0 && !b.isIslandDiscovered(playerNum, islandNum)) {
					vertexValue += 2;
				}
				
				vertexValues[vIndex] += vertexValue;
			}
			
		}

		// Now, visit each of myVerts and use the distance against the valued Verts to produce a total value...
		float value = 0;
		for (int i : myVerts) {
			for (int ii = 0; ii<vertexValues.length; ii++) {
				if (i != ii) {
					value += vertexValues[ii] / (1 + dist[i][ii]); // as distance increases, the value decreases
				}
			}
		}

		addValue("board", value);
	}
	/*
	public double evaluateCAK(Player p, Board b, SOC soc) {
		// evaluate knights strength vs. other players
		float knights = 0;
		for (int vIndex : b.getVertsOfType(p.getPlayerNum(), VertexType.BASIC_KNIGHT_ACTIVE, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE)) {
			Vertex v = b.getVertex(vIndex);
			knights += v.getType().getKnightLevel() + (v.getType().isKnightActive() ? 0.5f : 0f);
		}
		
		// evaluate city walls
		float cityWalls = b.getVertsOfType(p.getPlayerNum(), VertexType.WALLED_CITY).size();
		
		// evaluate progress cards (we dont want ANY because we want AI to use them)
		float progress = p.getCardCount(CardType.Progress,  CardStatus.USABLE) * -1;
		
		
		// evaluate city development areas
		float cityDevel = 0;
		for (DevelopmentArea a : DevelopmentArea.values()) {
			cityDevel += p.getCityDevelopment(a);
		}
		
		// evaluate metropolis
		float metroValue = b.getVertsOfType(p.getPlayerNum(), VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE).size() * 10;
		
		
		return knights + cityWalls + progress + cityDevel + metroValue;
	}*/
}
