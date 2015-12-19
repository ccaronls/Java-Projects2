package cc.game.soc.core;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.ObjectInputStream.GetField;
import java.util.*;

import cc.lib.game.Utils;
import cc.lib.math.CMath;
import cc.lib.utils.FileUtils;
import cc.lib.utils.Reflector;

public class PlayerBot extends Player {

	public static boolean DEBUG_ENABLED = false;
	
	private BotNode movesPath = null; 
//	private LinkedList<BotNode> leafNodes = new LinkedList<BotNode>();
	private Set<Integer> usedRoadRoutes = new HashSet<Integer>();
	private Set<Integer> usedShipRoutes = new HashSet<Integer>();
	private Set<MoveType> usedMoves = new HashSet<MoveType>();

	private BotNode createNewTree() {
		BotNode node = new BotNodeRoot();
		
	//	leafNodes.clear();
		usedRoadRoutes.clear();
		usedShipRoutes.clear();
		usedMoves.clear();
		// populate with ALL evaluations that will propogate throughout tree
		
		return node;
	}
	
	@SuppressWarnings("unchecked")
	private <T> T detatchMove() {
		BotNode front = movesPath;
		movesPath = movesPath.next;
		return (T)front.getData();
	}
	/*
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
	}*/
	
	/*

FORMAT:

moves:    ROOT       BUILD_ROAD E(n)   TRADE BrickX2  DRAW Wood BUILD
prop0     prop0val
prop1
prop2
...




	 */
	
	private void dumpAllPathsAsCSV(List<BotNode> leafs, File outFile) throws IOException {
		PrintStream out = new PrintStream(outFile);
		
		try {
    		Collections.sort(leafs);
    		Collections.reverse(leafs);
    		out.print("MOVES");
    		
    		for (BotNode leaf : leafs) {
    			LinkedList<BotNode> moves = new LinkedList<BotNode>();
    			for (BotNode n = leaf; n!=null; n=n.parent) {
    				moves.addFirst(n);
    			}
    			
    			{
        			for (BotNode n : moves) {
        				out.print(String.format(",%s(%4f)", n.getDescription(), n.getValue()));
        				//out.print(String.format("%-" + maxWidth + "s(%4f)", n.getDescription(), n.getValue()));
        			}
        			if (leaf.isOptimal()) {
        				out.print("** OPTIMAL **");
        			}
        			out.println();
    			}
    			
    			List<String> properties = isolateChangingProperties(leaf);
    			for (String key :  properties) {
    				out.print(key);
    				for (BotNode n : moves) {
    					Object val = n.properties.get(key);
    					out.print(String.format(",%s", val == null ? "null" : val));
    				}
        			out.println();
    			}
    		}
		} finally {
			out.close();
		}
	}
	
	
	private void dumpAllPaths(List<BotNode> leafs, PrintStream out) {
		// print out all move paths such that the optimal is printed last
		Collections.sort(leafs);
		//Collections.reverse(leafs);

		for (BotNode leaf : leafs) {
			//HashSet<String> changes = new HashSet<String>();
			//findChangingProperties(leaf, changes);
			int maxWidth = 20;
			for (String s : leaf.properties.keySet()) {
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
    			if (leaf.isOptimal()) {
    				out.print("OPTIMAL");
    			}
    			out.println();
			}
			
			List<String> properties = isolateChangingProperties(leaf);
			for (String key :  leaf.properties.keySet()) {
				for (BotNode n : moves) {
					Object val = n.properties.get(key);
					if (val != null) 
						out.print(String.format("  %-" +maxWidth+ "s %4f", key, val));
					else {
						out.print(String.format("  %" + (maxWidth+5) + "s", ""));
					}
				}
    			out.println();
			}
		}
	}
	
	private List<String> isolateChangingProperties(BotNode leaf) {
		List<String> props = new ArrayList<String>();
		for (String key : leaf.properties.keySet()) {
			Object val = leaf.properties.get(key);
			BotNode p = leaf.parent;
			while (p != null) {
				Object val2 = p.properties.get(key);
				if (val2 == null || !val2.equals(val)) {
					props.add(key);
					break;
				}
				p = p.parent;
			}
		}
		return props;
	}
	
	private void buildOptimalPath(BotNode root) {
		ArrayList<BotNode> leafs = new ArrayList<BotNode>();
		BotNode child = buildOptimalPathR(root, leafs);
		child.setOptimal(true);
		if(DEBUG_ENABLED) {
//			root.printTree(System.out);
			try {
				//PrintStream out = new PrintStream(new File("/tmp/playerAI" + getPlayerNum()));
				//dumpAllPaths(leafs, out);
				//out.close();
				String fileName = System.getenv("HOME") + "/Documents/playerAI" + getPlayerNum() + ".csv";
				FileUtils.backupFile(fileName, 10);
				dumpAllPathsAsCSV(leafs, new File(fileName));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
			
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
		
		usedRoadRoutes.addAll(roads1);
		usedShipRoutes.addAll(ships1);
		
		if (ships1.size() > 0 && roads1.size() > 0) {
			{
    			BotNode roadOptions = root.attach(new BotNodeEnum(RouteChoiceType.ROAD_CHOICE));
    			for (int roadIndex1 : roads1) {
    				Route r = b.getRoute(roadIndex1);
    				b.setPlayerForRoute(r, p.getPlayerNum(), RouteType.ROAD);
    				//r.setLocked(true);
    				BotNode n = roadOptions.attach(new BotNodeRoute(r, roadIndex1));
    				
    				List<Integer> roads2 = SOC.computeRoadRouteIndices(p.getPlayerNum(), b);
    				roads2.removeAll(usedRoadRoutes);
    				usedRoadRoutes.addAll(roads2);
    				
    				for (int roadIndex2 : roads2) {
    					Route r2 = b.getRoute(roadIndex2);
    					b.setPlayerForRoute(r2, p.getPlayerNum(), RouteType.ROAD);
    					//r2.setLocked(true);
    					BotNode n2 = n.attach(new BotNodeRoute(r2, roadIndex2));
    					evaluateEdges(n2, soc, p, b);
    					buildChooseMoveTreeR(soc, p, b, n2, SOC.computeMoves(p, b, soc));
    					b.setRouteOpen(r2);
    					//r2.setLocked(false);
    				}
    				b.setRouteOpen(r);
    				//r.setLocked(false);
    			}
			}
			{
    			BotNode shipOptions = root.attach(new BotNodeEnum(RouteChoiceType.SHIP_CHOICE));
    			for (int shipIndex1 : ships1) {
    				Route ship = b.getRoute(shipIndex1);
    				b.setPlayerForRoute(ship, p.getPlayerNum(), RouteType.SHIP);
    				//ship.setLocked(true);
    				BotNode n = shipOptions.attach(new BotNodeRoute(ship, shipIndex1));
    				
    				List<Integer> ships2 = SOC.computeShipRouteIndices(p.getPlayerNum(), b);
    				ships2.removeAll(usedShipRoutes);
    				usedShipRoutes.addAll(ships2);
    				
    				for (int shipIndex2 : ships2) {
    					Route ship2 = b.getRoute(shipIndex2);
    					b.setPlayerForRoute(ship2, p.getPlayerNum(), RouteType.SHIP);
    					//ship2.setLocked(true);
    					BotNode n2 = n.attach(new BotNodeRoute(ship2, shipIndex2));
    					evaluateEdges(n2, soc, p, b);
    					buildChooseMoveTreeR(soc, p, b, n2, SOC.computeMoves(p, b, soc));
    					b.setRouteOpen(ship2);
    					//ship2.setLocked(false);
    				}
    				b.setRouteOpen(ship);
    			}
			}
		} else if (ships1.size() > 0) {
			for (int shipIndex1 : ships1) {
				Route ship = b.getRoute(shipIndex1);
				b.setPlayerForRoute(ship, p.getPlayerNum(), RouteType.SHIP);
				//ship.setLocked(true);
				BotNode n = root.attach(new BotNodeRoute(ship, shipIndex1));

				List<Integer> ships2 = SOC.computeShipRouteIndices(p.getPlayerNum(), b);
				ships2.removeAll(usedShipRoutes);
				usedShipRoutes.addAll(ships2);
				
				for (int shipIndex2 : ships2) {
					Route ship2 = b.getRoute(shipIndex2);
					b.setPlayerForRoute(ship2, p.getPlayerNum(), RouteType.SHIP);
					//ship2.setLocked(true);
					BotNode n2 = n.attach(new BotNodeRoute(ship2, shipIndex2));
					evaluateEdges(n2, soc, p, b);
					buildChooseMoveTreeR(soc, p, b, n2, SOC.computeMoves(p, b, soc));
					b.setRouteOpen(ship2);
					//ship2.setLocked(false);
				}
				b.setRouteOpen(ship);
				//ship.setLocked(false);
			}
		} else if (roads1.size() > 0) {
			for (int roadIndex1 : roads1) {
				Route r = b.getRoute(roadIndex1);
				b.setPlayerForRoute(r, p.getPlayerNum(), RouteType.ROAD);
				//r.setLocked(true);
				BotNode n = root.attach(new BotNodeRoute(r, roadIndex1));

				List<Integer> roads2 = SOC.computeRoadRouteIndices(p.getPlayerNum(), b);
				roads2.removeAll(usedRoadRoutes);
				usedRoadRoutes.addAll(roads2);
				
				for (int roadIndex2 : roads2) {
					Route r2 = b.getRoute(roadIndex2);
					//r2.setLocked(true);
					b.setPlayerForRoute(r2, p.getPlayerNum(), RouteType.ROAD);
					BotNode n2 = n.attach(new BotNodeRoute(r2, roadIndex2));
					evaluateEdges(n2, soc, p, b);
					buildChooseMoveTreeR(soc, p, b, n2, SOC.computeMoves(p, b, soc));
					b.setRouteOpen(r2);
					//r2.setLocked(false);
				}
				b.setRouteOpen(r);
				//r.setLocked(false);
			}
		}		
	}
	
	private void buildChooseMoveTreeR(SOC soc, Player p, Board b, BotNode _root, Collection<MoveType> _moves) {
		MoveType [] moves = _moves.toArray(new MoveType[_moves.size()]);
		Arrays.sort(moves);
		for (MoveType move : moves) {
			if (move.aiUseOnce) {
				if (usedMoves.contains(move))
					continue;
				usedMoves.add(move);
			}
			BotNode root = _root.attach(new BotNodeEnum(move));
			switch (move) {
				case CONTINUE:
					//doEvaluate(root, soc, p, b, 1);
					// nothing to evaluate since we inherit all values from our parent
					break;
				case TRADE: {
					List<Trade> trades = SOC.computeTrades(p, b);
					for (Trade trade : trades) {
						BotNode n = root.attach(new BotNodeTrade(trade));
						p.incrementResource(trade.getType(), -trade.getAmount());
						for (ResourceType r : ResourceType.values()) {
							if (r == trade.getType()) // as an optimization we skip trades for the same type
								continue; // 
							p.addCard(r);
							BotNode node = n.attach(new BotNodeCard(r));
							evaluatePlayer(node, soc, p, b);
							buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
							p.removeCard(r);
						}
						if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
							for (CommodityType c : CommodityType.values()) {
								if (c == trade.getType())
									continue;
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
						onBoardChanged();
						BotNode node = root.attach(new BotNodeVertex(v, vIndex));
						evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
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
						onBoardChanged();
						BotNode node = root.attach(new BotNodeVertex(v, vIndex));
						evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
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
						onBoardChanged();
						BotNode node = root.attach(new BotNodeVertex(v, vIndex));
						evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
						v.setType(VertexType.CITY);
					}
					p.adjustResourcesForBuildable(BuildableType.CityWall, 1);
					break;
				}
				case REPAIR_ROAD: {
					p.adjustResourcesForBuildable(BuildableType.Road, -1);
					p.removeCard(SpecialVictoryType.DamagedRoad);
					evaluatePlayer(root, soc, p, b);
					buildChooseMoveTreeR(soc, p, b, root, SOC.computeMoves(p, b, soc));
					p.addCard(SpecialVictoryType.DamagedRoad);
					p.adjustResourcesForBuildable(BuildableType.Road, 1);
					break;
				}
				case BUILD_ROAD: {
					List<Integer> roads = SOC.computeRoadRouteIndices(p.getPlayerNum(), b);
					roads.removeAll(usedRoadRoutes);
					usedRoadRoutes.addAll(roads);

					p.adjustResourcesForBuildable(BuildableType.Road, -1);
					for (int rIndex : roads) {
						Route r = b.getRoute(rIndex);
						//r.setLocked(true);
						b.setPlayerForRoute(r, p.getPlayerNum(), RouteType.ROAD);
						onBoardChanged();
						BotNode node = root.attach(new BotNodeRoute(r, rIndex));
						evaluateEdges(node, soc, p, b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
						//r.setLocked(false);
						b.setRouteOpen(r);
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
					int saveRobber = b.getRobberTileIndex();
					int savePirate = b.getPirateTileIndex();
					for (int tIndex : SOC.computeRobberTileIndices(soc, b)) {
						Tile t = b.getTile(tIndex);
						if (t.isWater())
							b.setPirate(tIndex);
						else
							b.setRobber(tIndex);
						onBoardChanged();
						BotNode node = root.attach(new BotNodeTile(t, tIndex));
						evaluateTiles(node, soc, p, b);
					}
					b.setRobber(saveRobber);
					b.setPirate(savePirate);
					break;
				}
				case YEAR_OF_PLENTY_CARD: {
					// we want to avoid duplicates (wood/brick == brick/wwod) so ....
					for (int i=0; i<ResourceType.values().length; i++) {
						ResourceType r = ResourceType.values()[i];
						p.incrementResource(r, 1);
						BotNode n = root.attach(new BotNodeCard(r));
						for (int ii=i; ii<ResourceType.values().length; ii++) {
							ResourceType r2 = ResourceType.values()[ii];
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
					ships.removeAll(usedShipRoutes);
					usedShipRoutes.addAll(ships);
					
					p.adjustResourcesForBuildable(BuildableType.Ship, -1);
					for (int rIndex : ships) {
						Route r = b.getRoute(rIndex);
						b.setPlayerForRoute(r, p.getPlayerNum(), RouteType.SHIP);
						//r.setLocked(true);
						BotNode node = root.attach(new BotNodeRoute(r, rIndex));
						onBoardChanged();
						evaluateEdges(node, soc, p, b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
						b.setRouteOpen(r);
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
					root.addValue("randomness", Utils.randFloatX(1));
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
						onBoardChanged();
						evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
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
						onBoardChanged();
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
					List<Integer> knights = SOC.computeMovableKnightVertexIndices(soc, p.getPlayerNum(), b);//b.getVertsOfType(p.getPlayerNum(), VertexType.BASIC_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE);
					for (int knightIndex : knights) {
						Vertex knight = b.getVertex(knightIndex);
						BotNode knightChoice = root.attach(new BotNodeVertex(knight, knightIndex)); 
						List<Integer> knightMoves = SOC.computeKnightMoveVertexIndices(soc, knightIndex, b);
						VertexType saveType = knight.getType();
						knight.setType(VertexType.OPEN);
						for (int moveIndex : knightMoves) {
							Vertex knightMove = b.getVertex(moveIndex);
							VertexType saveType2 = knightMove.getType();
							BotNode knightMoveChoice = knightChoice.attach(new BotNodeVertex(knightMove, moveIndex));
							onBoardChanged();
							evaluateVertices(knightMoveChoice, soc.getRules(), p.getPlayerNum(), b);
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
						b.setRouteOpen(shipToMove);
						shipToMove.setLocked(true);
						BotNode shipChoice = root.attach(new BotNodeRoute(shipToMove, shipIndex));
						List<Integer> newPositions = SOC.computeShipRouteIndices(p.getPlayerNum(), b);
						for (int moveIndex : newPositions) {
							if (moveIndex == shipIndex)
								continue;
							Route newPos = b.getRoute(moveIndex);
							b.setPlayerForRoute(newPos, p.getPlayerNum(), RouteType.SHIP);
							newPos.setLocked(true);
							BotNode node = new BotNodeRoute(newPos, moveIndex);
							onBoardChanged();
							evaluateEdges(node, soc, p, b);
							buildChooseMoveTreeR(soc, p, b, shipChoice.attach(node), SOC.computeMoves(p, b, soc));
							b.setRouteOpen(newPos);
							newPos.setLocked(false);
						}
						b.setPlayerForRoute(shipToMove, p.getPlayerNum(), RouteType.SHIP);
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
						BotNode node = root.attach(new BotNodeVertex(knight, knightIndex));
						onBoardChanged();
						evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
						knight.demoteKnight();
					}
					p.adjustResourcesForBuildable(BuildableType.PromoteKnight, 1);
					break;
				}
				case DEAL_EVENT_CARD:
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
					for (DevelopmentArea area : SOC.computeCraneCardImprovements(p)) {
						int count = p.getCardCount(area.commodity);
						p.incrementResource(area.commodity, -count);
						p.setCityDevelopment(area, count+1);
						BotNode node = root.attach(new BotNodeEnum(area));
						evaluatePlayer(node, soc, p, b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
						p.setCityDevelopment(area, count);
						p.incrementResource(area.commodity, count);
					}
					//p.addCard(ProgressCardType.Crane);
					break;
				}
				case DESERTER_CARD: {
					p.removeCard(ProgressCardType.Deserter);
					List<Integer> knightOptions = SOC.computeNewKnightVertexIndices(getPlayerNum(), b);
					for (int pIndex : SOC.computeDeserterPlayers(soc, b, p)) {
						BotNode node = root.attach(new BotNodePlayer(soc.getPlayerByPlayerNum(pIndex)));
						int oIndex = b.getKnightsForPlayer(pIndex).get(0);
						Vertex oVertex = b.getVertex(oIndex);
						VertexType oType = oVertex.getType();
						oVertex.setType(VertexType.OPEN);
						for (int kIndex : knightOptions) {
							Vertex v = b.getVertex(kIndex);
							v.setType(oType);
							onBoardChanged();
							BotNode b2 = node.attach(new BotNodeVertex(v, kIndex));
							evaluateVertices(b2, soc.getRules(), p.getPlayerNum(), b);
							// dont recurse since this step is a bit random
							v.setType(VertexType.OPEN);
						}
						oVertex.setType(oType);
					}
					
					/*
					List<Integer> knights = SOC.computeKnightsForDesertion(soc, b, p);
					for (int knightIndex : knights) {
						Vertex knight = b.getVertex(knightIndex);
						assert(knight.getPlayer() != p.getPlayerNum());
						int savePlayer = knight.getPlayer();
						knight.setPlayer(p.getPlayerNum());
						BotNode node = root.attach(new BotNodeVertex(knight, knightIndex));
						onBoardChanged();
						evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
						knight.setPlayer(savePlayer);
					}
					*/
//					p.addCard(ProgressCardType.Deserter);
					break;
				}
				case DIPLOMAT_CARD: {
					p.removeCard(ProgressCardType.Diplomat);
					List<Integer> allOpenRoutes = SOC.computeDiplomatOpenRouteIndices(soc, b);
					for (int rIndex1 : allOpenRoutes) {
						Route r = b.getRoute(rIndex1);
						int savePlayer = r.getPlayer();
						b.setRouteOpen(r);
						BotNode n = root.attach(new BotNodeRoute(r, rIndex1));
						if (savePlayer == p.getPlayerNum()) {
							List<Integer> newPos = SOC.computeRoadRouteIndices(p.getPlayerNum(), b);
							for (int rIndex2 : newPos) {
								Route r2 = b.getRoute(rIndex2);
								b.setPlayerForRoute(r2, p.getPlayerNum(), RouteType.ROAD);
								BotNode node = n.attach(new BotNodeRoute(r2, rIndex2));
								onBoardChanged();
								evaluateEdges(node, soc, p, b);
								buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
								b.setRouteOpen(r2);
							}
						} else {
							onBoardChanged();
							evaluateEdges(n, soc, p, b);
							buildChooseMoveTreeR(soc, p, b, n, SOC.computeMoves(p, b, soc));
						}
						b.setPlayerForRoute(r, savePlayer, RouteType.ROAD);
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
						onBoardChanged();
						buildChooseMoveTreeR(soc, p, b, root.attach(new BotNodeVertex(city, cIndex)), SOC.computeMoves(p, b, soc));
						city.setType(VertexType.CITY);
					}
					//p.addCard(ProgressCardType.Engineer);
					break;
				}
				case HARBOR_CARD: {
					p.removeCard(ProgressCardType.Harbor);
					List<Card> resourceCards = p.getUniqueCards(CardType.Resource);
					for (int pNum : SOC.computeHarborTradePlayers(p, soc)) {
						Player p2 = soc.getPlayerByPlayerNum(pNum);
						BotNode n = root.attach(new BotNodePlayer(p2));
						for (Card c : resourceCards) {
							p.removeCard(c);
							Card comm = p2.removeRandomUnusedCard(CardType.Commodity);
							p.addCard(comm);
							BotNode node = n.attach(new BotNodeCard(c));
							evaluateOpponent(node, soc, p, b);
							evaluatePlayer(node, soc, p, b);
							p.removeCard(comm);
							p2.addCard(comm);
							p.addCard(c);
						}
					}
					//p.addCard(ProgressCardType.Harbor);
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
						onBoardChanged();
						evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
						v.setPlayer(savePlayer);
					}
					//p.addCard(ProgressCardType.Intrigue);
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
							onBoardChanged();
							evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
							t0.setDieNum(die0);
							t1.setDieNum(die1);
						}
					}
					//p.addCard(ProgressCardType.Inventor);
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
					//p.addCard(ProgressCardType.Irrigation);
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
						BotNode node = root.attach(new BotNodePlayer(soc.getPlayerByPlayerNum(playerToChoose)));
						int num = Math.min(2, largestHand);
						ICardType<?> [] cards = new ICardType[num];
						for (int i=0; i<num; i++) {
							cards[i] = Utils.randItem(ResourceType.values());
							p.addCard(cards[i]);
						}
						evaluatePlayer(node, soc, p, b);
//						evaluateOpponent(node, soc, p, b);
						for (int i=0; i<num; i++) {
							p.removeCard(cards[i]);
						}
					}
					//p.addCard(ProgressCardType.MasterMerchant);
					break;
				}
				case MEDICINE_CARD: {
					p.removeCard(ProgressCardType.Medicine);
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
					int saveMerchantTile   = b.getMerchantTileIndex();
					int saveMerchantPlayer = b.getMerchantPlayer();
					for (int tIndex : tiles) {
						Tile t = b.getTile(tIndex);
						b.setMerchant(tIndex, p.getPlayerNum());
						BotNode node = root.attach(new BotNodeTile(t, tIndex));
						evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
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
					p.removeCard(ProgressCardType.Mining);
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
					for (ResourceType t : ResourceType.values()) {
						BotNode n = root.attach(new BotNodeEnum(t));
						p.incrementResource(t, soc.getNumPlayers()-1);
						evaluatePlayer(n, soc, p, b);
						// for random things we need to add some extra randomness
						n.addValue("randomness", Utils.randFloatX(1));
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
					//p.addCard(ProgressCardType.ResourceMonopoly);
					break;
				}
				case SABOTEUR_CARD: {
					p.removeCard(ProgressCardType.Saboteur);
					int totalCardsToSabotage = 0;
					for (Player player : soc.getPlayers()) {
						if (p == player)
							continue;
						if (p.getPoints() < player.getPoints()) {
							totalCardsToSabotage += (1+p.getUnusedCardCount()) / 2;
						}
					}
					
					root.addValue("sabotagedCards", 0.1 * totalCardsToSabotage);
					break;
				}
				case SMITH_CARD: {
					p.removeCard(ProgressCardType.Smith);
					List<Integer> promotableKnights = SOC.computePromoteKnightVertexIndices(p, b);
					if (promotableKnights.size() > 0) {
						p.removeCard(ProgressCardType.Smith);
    					if (promotableKnights.size() == 1) {
    						// promotion is automatic
    						int kIndex = promotableKnights.get(0);
    						Vertex v = b.getVertex(kIndex);
    						v.promoteKnight();
    						BotNode n = root.attach(new BotNodeVertex(v, kIndex));
    						buildChooseMoveTreeR(soc, p, b, n, SOC.computeMoves(p, b, soc));
    						v.demoteKnight();
    					} else if (promotableKnights.size() == 2) {
    						// promotion is automatic
    						int k0 = promotableKnights.get(0);
    						int k1 = promotableKnights.get(1);
    						Vertex v0 = b.getVertex(k0);
    						Vertex v1 = b.getVertex(k1);
    						v0.promoteKnight();
    						v1.promoteKnight();
    						BotNode n = root.attach(new BotNodeVertex(v0, k0)).attach(new BotNodeVertex(v1, k1));
    						buildChooseMoveTreeR(soc, p, b, n, SOC.computeMoves(p, b, soc));
    						v0.demoteKnight();
    						v1.demoteKnight();
    					} else {
    						// compute permutations
    						for (int i=0; i<promotableKnights.size()-1; i++) {
        						for (int ii=i+1; ii<promotableKnights.size(); ii++) {
        							Vertex v0 = b.getVertex(promotableKnights.get(i));
        							Vertex v1 = b.getVertex(promotableKnights.get(ii));
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
						evaluateOpponent(n, soc, player, b);
						player.addCard(removed);
					}
					//p.addCard(ProgressCardType.Spy);
					break;
				}
				case TRADE_MONOPOLY_CARD: {
					p.removeCard(ProgressCardType.TradeMonopoly);
					for (CommodityType t : CommodityType.values()) {
						BotNode node = root.attach(new BotNodeEnum(t));
						p.addCards(t, 2);
						evaluatePlayer(node, soc, p, b);
						//buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
						p.removeCards(t, 2);
					}
					//p.addCard(ProgressCardType.TradeMonopoly);
					break;
				}
				case WARLORD_CARD: {
					p.removeCard(ProgressCardType.Warlord);
					List<Integer> verts = b.getVertsOfType(p.getPlayerNum(), VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE);
					if (verts.size() > 0) {
						for (int vIndex : verts) {
							b.getVertex(vIndex).activateKnight();
						}
						evaluateVertices(root, soc.getRules(), p.getPlayerNum(), b);
						buildChooseMoveTreeR(soc, p, b, root, SOC.computeMoves(p, b, soc));
						for (int vIndex : verts) {
							b.getVertex(vIndex).deactivateKnight();
						}
					}
					//p.addCard(ProgressCardType.Warlord);
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
							//BotNode node = root.attach(new BotNodeCard(c));
							evaluatePlayer(root, soc, player, b);
							evaluateOpponent(root, soc, player, b);
							player.addCard(c);
							p.removeCard(c);
						}
					}
					//p.addCard(ProgressCardType.Wedding);
					break;
				}
				case ATTACK_PIRATE_FORTRESS: {
					for (int vIndex : SOC.computeAttackablePirateFortresses(b, p)) {
						Vertex v = b.getVertex(vIndex);
						int score = b.getRoutesOfType(getPlayerNum(), RouteType.WARSHIP).size();
						float chance = 1f * score / 6f;
						v.setPlayer(getPlayerNum());
						BotNode node = root.attach(new BotNodeVertex(v, vIndex));
						node.chance = chance;
						evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
						v.setPlayer(0);
					}
					break;
				}
				case BUILD_WARSHIP:
					for (int rIndex : b.getRoutesIndicesOfType(getPlayerNum(), RouteType.SHIP)) {
						Route r = b.getRoute(rIndex);
						r.setType(RouteType.WARSHIP);
						evaluateEdges(root.attach(new BotNodeRoute(r, rIndex))	, soc, p, b);
						r.setType(RouteType.SHIP);
					}
					break;
				case KNIGHT_ATTACK_ROAD: {
					for (int rIndex : SOC.computeAttackableRoads(soc, getPlayerNum(), b)) {
						Board copy = b.deepCopy();
						Route route = copy.getRoute(rIndex);
						int score = SOC.computeAttackerScoreAgainstRoad(route, getPlayerNum(), copy);
						float chance = 1.0f*score / soc.getRules().getKnightScoreToDestroyRoad();
						BotNode node = root.attach(new BotNodeRoute(route, rIndex));
						node.chance = chance;
						copy.setRouteOpen(route);
						evaluateEdges(node, soc, p, b);
						evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
					}
					break;
				}
				case KNIGHT_ATTACK_STRUCTURE: {
					for (int vIndex : SOC.computeAttackableStructures(soc, getPlayerNum(), b)) {
						Board copy = b.deepCopy();
						Vertex v = copy.getVertex(vIndex);
						int score = SOC.computeAttackerScoreAgainstStructure(v, getPlayerNum(), copy);
						VertexType [] result = new VertexType[1];
						float chance = 1.0f*score / SOC.getKnightScoreToAttackStructure(v, result, soc.getRules());
						BotNode node = root.attach(new BotNodeVertex(v, vIndex));
						node.chance = chance;
						v.setType(result[0]);
						evaluateEdges(node, soc, p, b);
						evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
					}
					break;
				}
				case WARSHIP_CARD: {
					for (int rIndex : b.getRoutesIndicesOfType(getPlayerNum(), RouteType.SHIP)) {
						Route r = b.getRoute(rIndex);
						r.setType(RouteType.WARSHIP);
						evaluateEdges(root.attach(new BotNodeRoute(r, rIndex)), soc, p, b);
						r.setType(RouteType.SHIP);
					}
					break;
				}
			} // end switch
			
			
			// TODO: Why did I put this here?
			// break; // break out of for loop
		} // end for
	}
	
	@Override
	public MoveType chooseMove(SOC soc, Collection<MoveType> moves) {
		if (movesPath != null) {
			return detatchMove();
		}
		assert(moves.size() > 0);
		if (moves.size() == 1)
			return moves.iterator().next();
		
		BotNode root = createNewTree();
		evaluateEdges(root, soc, this, soc.getBoard());
		evaluatePlayer(root, soc, this, soc.getBoard());
		evaluateTiles(root, soc, this, soc.getBoard());
		evaluateVertices(root, soc.getRules(), getPlayerNum(), soc.getBoard());
		Player p = new PlayerTemp(this);
		p.setCardsUsable(CardType.Development, false); // prevent generating development card moves on subsequent calls
		buildChooseMoveTreeR(soc, p, soc.getBoard(), root, moves);
		/*
		if (DEBUG_ENABLED) {
			try {
				PrintStream out = new PrintStream(new File("/tmp/movestree"));
				root.printTree(out);
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}*/
		buildOptimalPath(root);
		return detatchMove();
	}

	@Override
	public Vertex chooseVertex(SOC soc, Collection<Integer> vertexIndices, VertexChoice mode) {
		
		switch (mode) {
			case POLITICS_METROPOLIS:
			case SCIENCE_METROPOLIS:
			case TRADE_METROPOLIS:
				return soc.getBoard().getVertex(Utils.randItem(new ArrayList<Integer>(vertexIndices))); // special case where it does not matter which vertex we choose
			default:
		}
		
		
		
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
				case SCIENCE_METROPOLIS:
				case TRADE_METROPOLIS:
					assert(false);
					break;
				case SETTLEMENT: {
					v.setPlayer(p.getPlayerNum());
					v.setType(VertexType.SETTLEMENT);
					if (islandNum > 0 && !discovered) {
						b.setIslandDiscovered(p.getPlayerNum(), islandNum, true);
					}
					break;
				}
				case PIRATE_FORTRESS:
					break;
				case OPPONENT_STRUCTURE_TO_ATTACK:
					break;
			}
			onBoardChanged();
			BotNode node = root.attach(new BotNodeVertex(v, vIndex));
			evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
			v.copyFrom(save);
			if (islandNum > 0 && !discovered) {
				b.setIslandDiscovered(p.getPlayerNum(), islandNum, false);
			}
		}
		buildOptimalPath(root);
		return detatchMove();
	}

	@Override
	public Route chooseRoute(SOC soc, Collection<Integer> routeIndices, RouteChoice mode) {
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
					b.setPlayerForRoute(r, p.getPlayerNum(), RouteType.ROAD);
					break;
				case SHIP:
					b.setPlayerForRoute(r, p.getPlayerNum(), RouteType.SHIP);
					break;
				case SHIP_TO_MOVE:
				case ROUTE_DIPLOMAT:
				case UPGRADE_SHIP:
				case OPPONENT_ROAD_TO_ATTACK:
					throw new AssertionError("unhandled case " + mode);
			}
			onBoardChanged();
			BotNode node = root.attach(new BotNodeRoute(r, rIndex));
			evaluateEdges(node, soc, p, b);
			r.copyFrom(save);
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
			b.setPlayerForRoute(r, p.getPlayerNum(), RouteType.ROAD);
			BotNode node = n.attach(new BotNodeRoute(r, rIndex));
			evaluateEdges(node, soc, p, b);
			b.setRouteOpen(r);
		}

		n = root.attach(new BotNodeEnum(RouteChoiceType.SHIP_CHOICE));
		for (int rIndex : ships) {
			Route r = b.getRoute(rIndex);
			b.setPlayerForRoute(r, p.getPlayerNum(), RouteType.SHIP);
			BotNode node = n.attach(new BotNodeRoute(r, rIndex));
			evaluateEdges(node, soc, p, b);
			b.setRouteOpen(r);
		}

		buildOptimalPath(root);
		return detatchMove();
	}

	@Override
	public Tile chooseTile(SOC soc, Collection<Integer> tileIndices, TileChoice mode) {
		if (movesPath != null) {
			return detatchMove();
		}

		Player p = this;
		Board b = soc.getBoard();
		
		BotNode root = createNewTree();
		switch (mode) {
			case INVENTOR:
			case MERCHANT:
				assert(false);
				break;
			case PIRATE:
			case ROBBER: {
				int pirateSave = b.getPirateTileIndex();
				int robberSave = b.getRobberTileIndex();
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
				
				break;
			}
			
		}
		buildOptimalPath(root);
		return detatchMove();
	}

	@Override
	public Trade chooseTradeOption(SOC soc, Collection<Trade> trades) {
		if (movesPath != null) {
			return detatchMove();
		}
		// TODO Auto-generated method stub
		assert(false);

		return null;
	}

	@Override
	public Player choosePlayer(SOC soc, Collection<Integer> playerOptions, PlayerChoice mode) {
		if (movesPath != null) {
			return detatchMove();
		}
		
		if (playerOptions.size() == 1)
			return soc.getPlayerByPlayerNum(playerOptions.iterator().next());
		
		Player p = this;
		Board b = soc.getBoard();
		
		BotNode root = createNewTree();
		
		for (int num : playerOptions) {
			Player opponent = soc.getPlayerByPlayerNum(num);
			Card card = opponent.removeRandomUnusedCard();
			p.addCard(card);
			BotNode node = root.attach(new BotNodePlayer(opponent));
			evaluateOpponent(node, soc, opponent, b);
			p.removeCard(card);
			opponent.addCard(card);
		}
		
		buildOptimalPath(root);
		return detatchMove();
	}

	@Override
	public Card chooseCard(SOC soc, Collection<Card> cards, CardChoice mode) {
		if (movesPath != null) {
			return detatchMove();
		}

		BotNode root = createNewTree();
		for (Card c : cards) {
			addCard(c);
			BotNode node = root.attach(new BotNodeCard(c));
			evaluatePlayer(node, soc, this, soc.getBoard());
			removeCard(c);
		}
		buildOptimalPath(root);
		return detatchMove();
/*
		switch (mode) {
			case EXCHANGE_CARD:
				break;
			case FLEET_TRADE:
				break;
			case RESOURCE_OR_COMMODITY: {
				BotNode root = createNewTree();
				for (Card c : cards) {
					addCard(c);
					BotNode node = root.attach(new BotNodeCard(c));
					evaluatePlayer(node, soc, this, soc.getBoard());
					removeCard(c);
				}
				buildOptimalPath(root);
				return detatchMove();
			}
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
		}
		
		// TODO Auto-generated method stub
		assert(false);
		return null;*/
	}
	
	@Override
	public <T extends Enum<T>> T chooseEnum(SOC soc, EnumChoice mode, T [] values) {
		if (movesPath != null) {
			return detatchMove();
		}
		// TODO Auto-generated method stub
		assert(false);
		return null;
	}

	@Override
	public boolean setDice(Dice [] die, int num) {
		Integer [] dice = detatchMove();
		for (int i=0; i<num; i++) {
			die[i].setNum(dice[i]);
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

        diePossibility_usegetter = new double[] {
                0, 0, p1, p2, p3, p4, p5, 0, p5, p4, p3, p2, p1
        };

    }
    
    final static double [] diePossibility_usegetter; // TODO: The die distribution is different (equal) for EventCards
    
    public static double getDiePossibility(int dieNum, Rules rules) {
    	if (rules.isEnableEventCards())
    		return 0.1;
    	return diePossibility_usegetter[dieNum];
    }
	
	public static void evaluateVertices(BotNode node, Rules rules, int playerNum, Board b) {
		
		// things to consider:
		// score from structures:
		//   settlement +1
		//   city +2
		//   city with wall +3
		//   metro +5
		//
		// Resource Distribution:
		//   We want to maximize trade options
		//   We want to maximize the probability we will obtain any one of the resources or commodities
		//
		// score from knights:
		//   SUM(knightLevel(i) * isActive(i) ? 2 : 1)
		//
		// protected tiles
		//   Minimize(Intersection(Tiles adjacent, Tiles knights adjacent))
		//
		// Is the robber or pirate adjacent to one of our structures? -5
		// Is one of our knights chasing away the robber? +5
		//
		// Value from ports:
		//   
		HashSet<Integer> tilesAdjacent = new HashSet<Integer>();
		HashSet<Integer> tilesProtected  = new HashSet<Integer>();
		
		double [] resourceProb = new double[SOC.NUM_RESOURCE_TYPES];
		final int [] resourcePorts = new int[SOC.NUM_RESOURCE_TYPES];
		float numMultiPorts = 0;
		
		if (b.getMerchantTile() != null && b.getMerchantPlayer() == playerNum) {
			Tile t = b.getMerchantTile();
			resourcePorts[t.getResource().ordinal()] ++;
		}
		
		float knightValue = 0;
		float structureValue = 0;
		
		for (Vertex v : b.getVerticies()) {
			float scale = 0;
			if (v.getPlayer() != playerNum)
				continue;
			switch (v.getType()) {
				case OPEN:
					break;
				case SETTLEMENT:
					structureValue += 1;
					scale = rules.getNumResourcesForSettlement();
					break;
				case METROPOLIS_POLITICS:
				case METROPOLIS_SCIENCE:
				case METROPOLIS_TRADE:
					structureValue += 2;
				case WALLED_CITY:
					structureValue += 1; 
				case CITY:
					scale = rules.getNumResourcesForCity();
					structureValue += 2;
					break;
				case MIGHTY_KNIGHT_ACTIVE:
				case BASIC_KNIGHT_ACTIVE:
				case STRONG_KNIGHT_ACTIVE:
					for (int tIndex : b.getTileIndicesAdjacentToVertex(v)) {
						tilesProtected.add(tIndex);
					}
					knightValue += v.getType().getKnightLevel() * 2;
					break;
				case MIGHTY_KNIGHT_INACTIVE:
				case BASIC_KNIGHT_INACTIVE:
				case STRONG_KNIGHT_INACTIVE:
					for (int tIndex : b.getTileIndicesAdjacentToVertex(v)) {
						tilesProtected.add(tIndex);
					}
					knightValue += v.getType().getKnightLevel();
					break;
				case PIRATE_FORTRESS:
					break;
			}
			if (v.isStructure()) {
				for (Tile cell : b.getTilesAdjacentToVertex(v)) {
					if (cell == b.getRobberTile())
						continue;
					switch (cell.getType()) {
						case GOLD:
							for (int i=0; i<resourceProb.length; i++)
								resourceProb[i] += getDiePossibility(cell.getDieNum(), rules) * scale;
							break;
						case FIELDS:
						case FOREST:
						case HILLS:
						case MOUNTAINS:
						case PASTURE:
							resourceProb[cell.getResource().ordinal()] += getDiePossibility(cell.getDieNum(), rules) * scale;
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
				for (int tIndex : b.getTileIndicesAdjacentToVertex(v)) {
					tilesAdjacent.add(tIndex);
				}
			}
		}

		double resourceProbValue = 0.5 * CMath.sum(resourceProb);
		//resourceProbValue *= 2;
		double resourcePortProb = 0;
		for (int i=0; i<SOC.NUM_RESOURCE_TYPES; i++) {
			if (resourcePorts[i] > 0)
				resourcePortProb += resourceProb[i];
		}

		tilesAdjacent.retainAll(tilesProtected);
		float covered = 0;
		for (int tIndex : tilesAdjacent) {
			// these are the uncovered tiles
			covered += getDiePossibility(b.getTile(tIndex).getDieNum(), rules);
		}
		double ave = CMath.sum(resourceProb) / resourceProb.length;
		double stdDev = 0.1 * Math.abs(CMath.stdDev(resourceProb, ave));
		double resourceDistribution = ave - stdDev;
		
		float multiPortValue = 0.1f * (numMultiPorts > 0 ? 1.0f / numMultiPorts : 0);
		
		if (rules.isEnableSeafarersExpansion()) {
			Distances d = evaluateWaterwayDistances(b, playerNum);
			node.addValue("shortestDistanceToAPirateFortress", 1 / (1+d.shortesDistanceToAPirateFortress));
			node.addValue("shortestDistanceToUndiscoveredTile", 1 / (1+d.shortestDistanceToUndiscoveredTile));
			if (b.getNumIslands() > 0) {
    			float minDistToUndiscoveredIsland = 1000;
    			for (int i=0; i<b.getNumIslands(); i++) {
    				if (d.shortestDistanceToUndiscoveredIslands[i] < minDistToUndiscoveredIsland) {
    					minDistToUndiscoveredIsland = d.shortestDistanceToUndiscoveredIslands[i];
    				}
    			}
    			node.addValue("shortestDistanceToUndiscoveredIslands", 1 / (1+minDistToUndiscoveredIsland));
			}
		}
		
		node.addValue("Resource distribution", resourceDistribution);
		node.addValue("Resource Port Prob", resourcePortProb);
		node.addValue("Multi Ports", multiPortValue);
		node.addValue("Resource Prob", resourceProbValue);
		node.addValue("Structures", structureValue);
		node.addValue("tiles protected", covered);
		node.addValue("knights", knightValue);
	}

	public static void evaluateEdges(BotNode node, SOC soc, Player p, final Board b) {

		if (soc.getRules().isEnableSeafarersExpansion()) {
			Distances d = evaluateWaterwayDistances(b, p.getPlayerNum());
			node.addValue("shortestDistanceToAPirateFortress", 1 / (1+d.shortesDistanceToAPirateFortress));
			node.addValue("shortestDistanceToUndiscoveredTile", 1 / (1+d.shortestDistanceToUndiscoveredTile));
			if (b.getNumIslands() > 0) {
    			float minDistToUndiscoveredIsland = 1000;
    			for (int i=0; i<b.getNumIslands(); i++) {
    				if (d.shortestDistanceToUndiscoveredIslands[i] < minDistToUndiscoveredIsland) {
    					minDistToUndiscoveredIsland = d.shortestDistanceToUndiscoveredIslands[i];
    				}
    			}
    			node.addValue("shortestDistanceToUndiscoveredIslands", 1 / (1+minDistToUndiscoveredIsland));
			}
		}
		
		
		float value = 0;
		// evaluate 
		for (Route r : b.getRoutesForPlayer(p.getPlayerNum())) {
			for (Tile t : b.getRouteTiles(r)) {
				if (t.getResource() != null)
					value += getDiePossibility(t.getDieNum(),  soc.getRules());
			}
		}
		
		node.addValue("routeValue", value);
		
		int longestRoadValue = -1;
		int len = b.computeMaxRouteLengthForPlayer(p.getPlayerNum(), soc.getRules().isEnableRoadBlock());
		
		Player cur = soc.getLongestRoadPlayer();
		if (cur != null) {
			if (cur.getPlayerNum() == p.getPlayerNum() || cur.getRoadLength() < len) {
				longestRoadValue = 1;
			}
		} else {
			longestRoadValue = len;
		}
		node.addValue("longestRoad", longestRoadValue);
		
		// evaluate potential settlements
		int numPotential = 0;
		double [] resourceProb = new double[SOC.NUM_RESOURCE_TYPES];
		for (int vIndex=0; vIndex<b.getNumVerts(); vIndex++) {
			if (b.isVertexAdjacentToPlayerRoad(vIndex, p.getPlayerNum()) && b.isVertexAvailbleForSettlement(vIndex)) {
				numPotential ++;
				Vertex v = b.getVertex(vIndex);
				for (Tile cell : b.getTilesAdjacentToVertex(v)) {
					switch (cell.getType()) {
						case GOLD:
							for (int i=0; i<resourceProb.length; i++)
								resourceProb[i] += getDiePossibility(cell.getDieNum(), soc.getRules());
							break;
						case FIELDS:
						case FOREST:
						case HILLS:
						case MOUNTAINS:
						case PASTURE:
							resourceProb[cell.getResource().ordinal()] += getDiePossibility(cell.getDieNum(), soc.getRules());
							break;
						case PORT_BRICK:
						case PORT_ORE:
						case PORT_SHEEP:
						case PORT_WHEAT:
						case PORT_WOOD:
							break;
						case PORT_MULTI:
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
		
		double potentialSettlementResourceProb = 0.5f * CMath.sum(resourceProb);
		
		node.addValue("potentialSettlements", 0.1f * numPotential);
		node.addValue("potentialSettlementResourceProb", potentialSettlementResourceProb);
	}
	
	public static class Distances extends Reflector<Distances> {
		
		static {
			addAllFields(Distances.class);
		}
		
		public Distances() {
			this(0,null,0);
		}
		
		public final float shortesDistanceToAPirateFortress;
		public final float [] shortestDistanceToUndiscoveredIslands;
		public final float shortestDistanceToUndiscoveredTile;
		public Distances(float shortesDistanceToAPirateFortress,
				float[] shortestDistanceToUndiscoveredIslands,
				float shortestDistanceToUndiscoveredTile) {
			super();
			this.shortesDistanceToAPirateFortress = shortesDistanceToAPirateFortress;
			this.shortestDistanceToUndiscoveredIslands = shortestDistanceToUndiscoveredIslands;
			this.shortestDistanceToUndiscoveredTile = shortestDistanceToUndiscoveredTile;
		}
		
		
	}
	
	public static Distances evaluateWaterwayDistances(final Board b, final int playerNum) {
		final int [][] matrix = new int[b.getNumVerts()][b.getNumVerts()];
		
		for (int i=0; i<matrix.length; i++) {
			for (int ii=0; ii<matrix.length; ii++) {
				matrix[i][ii] = 1000;
			}
			matrix[i][i] = 0;
		}
		
		for (Route r: b.getRoutes()) {
			if (r.isAdjacentToWater()) {
    			if (r.getPlayer() == playerNum && r.isVessel()) {
    				matrix[r.getFrom()][r.getTo()] = 0;
    				matrix[r.getTo()][r.getFrom()] = 0;
    			} else if (r.getPlayer() == 0 && !r.isAttacked() && !r.isClosed()) {
					matrix[r.getFrom()][r.getTo()] = 1;
					matrix[r.getTo()][r.getFrom()] = 1;
    			}
			}
		}
		
		// All-Pairs shortest paths [Floyd-Marshall O(|V|^3)] algorithm.  This is a good choice for dense graphs like ours
		// where every vertex has 2 or 3 edges.  The memory usage of a Dijkstra's would make it less
		// desirable.  Also, this method gives us all pairs, not just 
		for (int k=0; k<matrix.length; k++) {
			for (int i=0; i<matrix.length; i++) {
				for (int j=0; j<matrix.length; j++) {
					int sum = matrix[i][k] + matrix[k][j];
					matrix[i][j] = Math.min(matrix[i][j],  sum);
				}
			}
		}
		/*
		Utils.print("    |");
		for (int ii=0; ii<matrix.length; ii++) {
			Utils.print("%-5d", ii);
		}
		Utils.println();
		for (int i=0; i<matrix.length; i++) {
			Utils.print("%-4d|", i);
			for (int ii=0; ii<matrix.length; ii++) {
				Utils.print("%-4s|", matrix[i][ii] < 1000 ? String.valueOf(matrix[i][ii]) : "INF");
			}
			Utils.println();
		}*/

		float shortestDistanceToAPirateFortress = 1000;
		float [] shortestDistanceToUnDiscoveredIsland = new float[b.getNumIslands()+1]; 
		float shortestDistanceToUndiscoveredTile = 1000;

		Iterable<Integer> pirateFortresses = b.getVertsOfType(0, VertexType.PIRATE_FORTRESS); 
		Iterable<Integer> undiscoveredTiles = b.getTilesOfType(TileType.UNDISCOVERED);
		
		// in order to consider islands
		// we only consider routes the originate from ports
		for (int vIndex : b.getStructuresForPlayer(playerNum)) {
			Vertex v = b.getVertex(vIndex);
			if (!v.isAdjacentToWater())
				continue;

			// check pirate fortress.  We want the smallest possible distance to the nearest fortress
			for (int v2 : pirateFortresses) {
				shortestDistanceToAPirateFortress = Math.min(shortestDistanceToAPirateFortress, matrix[vIndex][v2]);
			}
		
    		// we want to consider the distance from any of our structures to any undiscovered tile, pirate fortress
    		for (Island i : b.getIslands()) {
    			shortestDistanceToUnDiscoveredIsland[i.getNum()] = 1000;
    			if (!i.isDiscoveredBy(playerNum)) {
					for (int rIndex : i.borderRoute) {
						Route r = b.getRoute(rIndex);
						shortestDistanceToUnDiscoveredIsland[i.getNum()] = Math.min(shortestDistanceToUnDiscoveredIsland[i.getNum()], matrix[vIndex][r.getFrom()]);
						shortestDistanceToUnDiscoveredIsland[i.getNum()] = Math.min(shortestDistanceToUnDiscoveredIsland[i.getNum()], matrix[vIndex][r.getTo()]);
					}
    			} else {
    				shortestDistanceToUnDiscoveredIsland[i.getNum()] = 0;
    			}
    		}
		
    		for (int tIndex : undiscoveredTiles) {
    			Tile t = b.getTile(tIndex);
    			for (int tv:  t.getAdjVerts()) {
    				shortestDistanceToUndiscoveredTile = Math.min(shortestDistanceToUndiscoveredTile, matrix[tv][vIndex]);
    			}
			}
		}
		
		return new Distances(shortestDistanceToAPirateFortress, shortestDistanceToUnDiscoveredIsland, shortestDistanceToUndiscoveredTile);
	}
	
	public static void evaluateTiles(BotNode node, SOC soc, Player p, Board b) {
		
		// Robber
		if (b.getRobberTileIndex() >= 0) {
			float robberValue = 0;
			Tile t = b.getRobberTile();
			for (Vertex v : b.getTileVertices(t)) {
				switch (v.getType()) {
					case BASIC_KNIGHT_ACTIVE:
					case BASIC_KNIGHT_INACTIVE:
					case MIGHTY_KNIGHT_ACTIVE:
					case MIGHTY_KNIGHT_INACTIVE:
					case STRONG_KNIGHT_ACTIVE:
					case STRONG_KNIGHT_INACTIVE:
						break;
					case CITY:
					case METROPOLIS_POLITICS:
					case METROPOLIS_SCIENCE:
					case METROPOLIS_TRADE:
					case WALLED_CITY:
						if (v.getPlayer() == p.getPlayerNum()) {
							robberValue -= soc.getRules().getNumResourcesForCity() * 2 * getDiePossibility(t.getDieNum(), soc.getRules());
						} else {
							robberValue += soc.getRules().getNumResourcesForCity() * getDiePossibility(t.getDieNum(), soc.getRules());
						}
						break;
					case SETTLEMENT:
						if (v.getPlayer() == p.getPlayerNum()) {
							robberValue -= soc.getRules().getNumResourcesForSettlement() * 2 * getDiePossibility(t.getDieNum(), soc.getRules());
						} else {
							robberValue += soc.getRules().getNumResourcesForSettlement() * getDiePossibility(t.getDieNum(), soc.getRules());
						}
						break;
					case OPEN:
						break;
					case PIRATE_FORTRESS:
						break;
				}
			}
			node.addValue("robberTile", robberValue);
		}
		
		// Pirate
		if (b.getPirateTile() != null) {
			Tile t = b.getPirateTile();
			float pirateValue = 0;
			for (Route r : b.getTileRoutes(t)) {
				switch (r.getType()) {
					case ROAD:
					case DAMAGED_ROAD:
					case OPEN:
						break;
					case SHIP:
					case WARSHIP:
						if (r.getPlayer() == p.getPlayerNum()) {
							pirateValue -= 2;
						} else {
							pirateValue += 1;
						}
						break;
				}
			}
			node.addValue("pirateTile", pirateValue);
		}
		
		// Merchant
		// Inventor
		// should do this when evaluating vertices
		
		
	}
	
	public static void evaluatePlayer(BotNode node, SOC soc, Player p, Board b) {
		
		// Special Victory Cards
		float value = 0;
		for (SpecialVictoryType c : SpecialVictoryType.values()) {
			value += c.getData() * p.getCardCount(c);
		}
		node.addValue("SpecialVictoryCards", value);
		
		// number of progress cards (less is better)
		value = p.getCardCount(CardType.Progress);
		if (value > 0) {
			node.addValue("ProgressCardCount", 1.0f / value);
		}
		
		// total cards near the max but not over
		// we want the highest value to be half of the max
		//node.addValue("cardsInHand", CMath.normalDistribution(p.getTotalCardsLeftInHand(), soc.getRules().getMaxSafeCards()));
		
		node.addValue("cardsInHand", p.getTotalCardsLeftInHand() > soc.getRules().getMaxSafeCards() ? 0 : 1);
		
		// city development
		float sum = 0;
		for (DevelopmentArea a : DevelopmentArea.values())
			sum += p.getCityDevelopment(a);
		node.addValue("cityDevelopment", sum);

		float buildableValue = 0;
		for (BuildableType t : BuildableType.values()) {
			if (!p.canBuild(t) && t.isAvailable(soc)) {
				buildableValue += (t.ordinal()+1);
			}
		}
		node.addValue("buildability", 0.02f * buildableValue);
		if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
			float developmentValue = 0;
			for (DevelopmentArea a : DevelopmentArea.values()) {
				int num = p.getCardCount(a.commodity);
				int cur = p.getCityDevelopment(a);
				if (cur < DevelopmentArea.MAX_CITY_IMPROVEMENT && num>cur) {
					developmentValue += cur+1;
				}
			}
			node.addValue("cityDevelValue", 0.2f * 	developmentValue);
		}
	}
	
	public static void evaluateDice(BotNode node, int die1, int die2, SOC soc, Player p, Board b) {
		// which die combe gives us the best results?
		
		// if CAK enabled, then die2 should be a value that gives us the best opportunity for a progress card
		if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
			if (p.getCardCount(CardType.Progress) < soc.getRules().getMaxProgressCards()) {
				DevelopmentArea best = null;
				int bestCount = 0;
				for (DevelopmentArea a : DevelopmentArea.values()) {
					int devel = p.getCityDevelopment(a);
					if (best == null || devel > bestCount) {
						best = a;
						bestCount = devel;
					}
				}
				node.addValue("progress dice", bestCount);
			}
		}
		
		float dieValue = 0;
		for (Tile t : b.getTiles()) {
			if (t.getDieNum() == die1+die2) {
    			for (Vertex v : b.getTileVertices(t)) {
    				if (v.isStructure()) {
    					float value = v.isCity() ? soc.getRules().getNumResourcesForCity() : soc.getRules().getNumResourcesForSettlement();
    					if (v.getPlayer() == p.getPlayerNum()) {
    						dieValue += value;
    					} else if (v.getPlayer() > 0) {
    						dieValue -= value; // discount if this die helps an opponent
    					}
    				}
    			}
			}
		}
		
		node.addValue("dieValue", dieValue);
	}
	
	/**
	 * Give a rating for the opponent keeping in mind that their in hand cards are not visible, except that we can tell how many
	 * progress cards they have vs. other cards.
	 * 
	 * @param node
	 * @param soc
	 * @param opponent
	 * @param b
	 */
	public static void evaluateOpponent(BotNode node, SOC soc, Player opponent, Board b) {
		int numProgress = opponent.getCardCount(CardType.Progress);
		int numPoints   = opponent.getPoints();
		int numCards    = opponent.getTotalCardsLeftInHand();
		
		node.addValue("opponent progress", numProgress);
		node.addValue("opponent points", numPoints);
		node.addValue("opponent cards", numCards);
	}

	/**
	 * Called whenever the board is updated to trigger a redraw if desired
	 */
	protected void onBoardChanged() {}
}
