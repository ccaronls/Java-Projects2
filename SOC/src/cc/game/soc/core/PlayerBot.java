package cc.game.soc.core;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
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

	private BotNode createNewTree(String desc) {
		BotNode node = new BotNodeRoot(desc);
		
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
	
	private void dumpAllPathsAsCSV(List<BotNode> leafs, File outFile) throws IOException {
		PrintStream out = new PrintStream(outFile);
		
		try {
    		Collections.sort(leafs);
    		out.print("MOVES");
    		
    		Collection<String> properties = isolateChangingLeafProperties(leafs);
    		
    		HashMap<String,Double> maxValues = new HashMap<>();
    		for (BotNode n : leafs) {
    			for (String key : n.properties.keySet()) {
    				Double v = n.properties.get(key);
    				if (!maxValues.containsKey(key)) {
    					maxValues.put(key, v);
    				} else {
    					maxValues.put(key, Math.max(v, maxValues.get(key)));
    				}
    			}
    		}
    		
    		for (BotNode leaf : leafs) {
    			LinkedList<BotNode> moves = new LinkedList<BotNode>();
    			for (BotNode n = leaf; n!=null; n=n.parent) {
    				moves.addFirst(n);
    			}
    			
    			{
        			for (BotNode n : moves) {
        				out.print(String.format(",%s(%4s)", n.getDescription(), n.getValue()));
        				//out.print(String.format("%-" + maxWidth + "s(%4f)", n.getDescription(), n.getValue()));
        			}
        			if (leaf.isOptimal()) {
        				out.print(",** OPTIMAL **");
        			}
        			out.println();
    			}

    			double total = 0;
    			for (String key : properties) {
    				if (leaf.properties.containsKey(key)) {
    					total += leaf.properties.get(key);
    				} else {
    					leaf.properties.put(key, 0.0);
    				}
    			}
    			
    			for (String key :  properties) {
    				out.print(key);
    				Double val = null;
    				for (BotNode n : moves) {
    					val = n.properties.get(key);
    					out.print(String.format(",%s", val == null ? "0" : val));
    				}

    				double max = maxValues.get(key);
					int maxPercent = (int)(100 * val / max);
					int maxTotal = (int)(100 * val / total);
					out.print(String.format(",%d%% of max - %d%% of total", maxPercent, maxTotal));
    				
        			out.println();
    			}
    		}
		} finally {
			out.close();
		}
	}
	
	/*
	private void dumpAllPaths(List<BotNode> leafs, PrintStream out) {
		// print out all move paths such that the optimal is printed last
		Collections.sort(leafs);

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
	}*/
	
	private void stripUnchangingProperties(List<BotNode> nodes) {
		HashMap<String, Double> values = new HashMap<>();
		for (BotNode n : nodes) {
			values.putAll(n.properties);
		}
		HashSet<String> changing = new HashSet<>();
		for (BotNode n : nodes) {
			for (String k : values.keySet()) { //n.properties.keySet()) {
				if (n.properties.containsKey(k)) {
    				double value = n.properties.get(k);
    				if (value != values.get(k)) {
    					changing.add(k);
    				}
				} else {
					n.properties.put(k, 0.0);
					changing.add(k);
				}
			}
		}
		for (BotNode n : nodes) {
			for (String k : changing) {
				if (!n.properties.containsKey(k)) {
					n.properties.put(k, 0.0);
				}
				n.properties.keySet().retainAll(changing);
			}
		}
	}
	
	private Collection<String> isolateChangingLeafProperties(List<BotNode> leafs) {
		Set<String> keepers = new HashSet<String>();
		HashMap<String, Double> values = new HashMap<>();
		for (BotNode n : leafs) {
			values.putAll(n.properties);
		}
		for (BotNode n : leafs) {
			for (String key: n.properties.keySet()) {
				if (keepers.contains(key))
					continue;
				if (!values.get(key).equals(n.properties.get(key))) {
					keepers.add(key);
					break;
				}
			}
		}
		return keepers;
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
				val = val2;
				p = p.parent;
			}
		}
		return props;
	}

	/**
	 * Use this method as an option to return a different node for optimal.  Must be one of the nodes in the leafs.
	 * @param optimal
	 * @param leafs
	 * @return
	 */
	protected BotNode onOptimalPath(BotNode optimal, List<BotNode> leafs) { return optimal; }
	
	private void buildOptimalPath(BotNode root) {
		ArrayList<BotNode> leafs = new ArrayList<BotNode>();
		BotNode child = buildOptimalPathR(root, leafs);
		stripUnchangingProperties(leafs);
		Collections.sort(leafs);
		child = onOptimalPath(child, leafs);
		while (child == null) {
			for (BotNode l : leafs) {
				l.resetCache();
			}
			Collections.sort(leafs);
			child = onOptimalPath(null, leafs);
		}
		
		assert(leafs.contains(child));
		child.setOptimal(true);
		
		if(false && DEBUG_ENABLED) {
//			root.printTree(System.out);
			try {
				//PrintStream out = new PrintStream(new File("/tmp/playerAI" + getPlayerNum()));
				//dumpAllPaths(leafs, out);
				//out.close();
				String fileName = System.getenv("HOME") + "/.soc/playerAI" + getPlayerNum() + ".csv";
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

		Utils.print("Path: ");
		for (BotNode n = movesPath; n!=null; n=n.next) {
			Utils.print(n.getDescription());
			if (n.next != null)
				Utils.print("==>>");
		}
		Utils.println();
		
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
	
	private void addRouteBuildingMovesR(SOC soc, Player p, Board b, BotNode root, boolean allowRoads, boolean allowShips, int depth, boolean recurseMoves) {
		
		if (depth <= 0) {
			if (recurseMoves)
				buildChooseMoveTreeR(soc, p, b, root, SOC.computeMoves(p, b, soc));
			else {
				
				doEvaluateAll(root, soc, p, b);
			}
				
			return;
		}
		
		@SuppressWarnings("unchecked")
		List<Integer> roads = allowRoads ? SOC.computeRoadRouteIndices(p.getPlayerNum(), b) : Collections.EMPTY_LIST;
		@SuppressWarnings("unchecked")
		List<Integer> ships = allowShips ? SOC.computeShipRouteIndices(soc, p.getPlayerNum(), b) : Collections.EMPTY_LIST;
		
		int numRoads = roads.size();
		int numShips = ships.size();
		
		//roads.removeAll(usedRoadRoutes);
		//ships.removeAll(usedShipRoutes);
		
		//usedRoadRoutes.addAll(roads);
		//usedShipRoutes.addAll(ships);
		
		if (numRoads > 0) {
			BotNode n = root;
			if (numShips > 0)
				n = root.attach(new BotNodeEnum(RouteChoiceType.ROAD_CHOICE));
			for (int roadIndex : roads) {
				Route r = b.getRoute(roadIndex);
				b.setPlayerForRoute(r, p.getPlayerNum(), RouteType.ROAD);
				r.setLocked(true);
				addRouteBuildingMovesR(soc, p, b, n.attach(new BotNodeRoute(r, roadIndex)), allowRoads, allowShips, depth-1, recurseMoves);
				r.setLocked(false);
				b.setRouteOpen(r);
			}
		}
		
		if (numShips > 0) {
			BotNode n = root;
			if (numRoads > 0)
				n = root.attach(new BotNodeEnum(RouteChoiceType.SHIP_CHOICE));
			for (int shipIndex : ships) {
				Route r = b.getRoute(shipIndex);
				b.setPlayerForRoute(r, p.getPlayerNum(), RouteType.SHIP);
				r.setLocked(true);
				addRouteBuildingMovesR(soc, p, b, n.attach(new BotNodeRoute(r, shipIndex)), allowRoads, allowShips, depth-1, recurseMoves);
				r.setLocked(false);
				b.setRouteOpen(r);
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
					doEvaluateAll(root, soc, p, b);
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
							//evaluatePlayer(node, soc, p, b);
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
						v.setPlayerAndType(p.getPlayerNum(), VertexType.SETTLEMENT);
						onBoardChanged();
						BotNode node = root.attach(new BotNodeVertex(v, vIndex));
						//evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
						v.setOpen();
					}
					p.adjustResourcesForBuildable(BuildableType.Settlement, 1);
					break;
				}
				case BUILD_CITY: {
					List<Integer> verts = SOC.computeCityVertxIndices(p.getPlayerNum(), b);
					p.adjustResourcesForBuildable(BuildableType.City, -1);
					for (int vIndex : verts) {
						Vertex v = b.getVertex(vIndex);
						v.setPlayerAndType(p.getPlayerNum(), VertexType.CITY);
						onBoardChanged();
						BotNode node = root.attach(new BotNodeVertex(v, vIndex));
						//evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
						v.setPlayerAndType(p.getPlayerNum(), VertexType.SETTLEMENT);
					}
					p.adjustResourcesForBuildable(BuildableType.City, 1);
					break;
				}
				case BUILD_CITY_WALL:{
					List<Integer> verts = SOC.computeCityWallVertexIndices(p.getPlayerNum(), b);
					p.adjustResourcesForBuildable(BuildableType.CityWall, -1);
					for (int vIndex : verts) {
						Vertex v = b.getVertex(vIndex);
						v.setPlayerAndType(p.getPlayerNum(), VertexType.WALLED_CITY);
						onBoardChanged();
						BotNode node = root.attach(new BotNodeVertex(v, vIndex));
						//evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
						v.setPlayerAndType(p.getPlayerNum(), VertexType.CITY);
					}
					p.adjustResourcesForBuildable(BuildableType.CityWall, 1);
					break;
				}
				case REPAIR_ROAD: {
					p.adjustResourcesForBuildable(BuildableType.Road, -1);
					p.removeCard(SpecialVictoryType.DamagedRoad);
					//evaluatePlayer(root, soc, p, b);
					buildChooseMoveTreeR(soc, p, b, root, SOC.computeMoves(p, b, soc));
					p.addCard(SpecialVictoryType.DamagedRoad);
					p.adjustResourcesForBuildable(BuildableType.Road, 1);
					break;
				}
				case BUILD_ROAD: {
					p.adjustResourcesForBuildable(BuildableType.Road, -1);
					addRouteBuildingMovesR(soc, p, b, root, true, false, 1, true);
					p.adjustResourcesForBuildable(BuildableType.Road, 1);
					break;
				}
				case ROAD_BUILDING_CARD: {
					addRouteBuildingMovesR(soc, p, b, root, true, soc.getRules().isEnableSeafarersExpansion(), 2, true);
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
						doEvaluateAll(node, soc, p, b);
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
							//evaluatePlayer(node, soc, p, b);
							buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
							p.incrementResource(r2, -1);
						}
						p.incrementResource(r, -1);
					}
					break;
				}
				case BUILD_SHIP:{
					p.adjustResourcesForBuildable(BuildableType.Ship, -1);
					addRouteBuildingMovesR(soc, p, b, root, false, true, 1, true);
					p.adjustResourcesForBuildable(BuildableType.Ship, 1);
					break;
				}
				case DRAW_DEVELOPMENT: {
					p.adjustResourcesForBuildable(BuildableType.Development, -1);
					Card temp = new Card(DevelopmentCardType.Soldier, CardStatus.UNUSABLE);
					p.addCard(temp);
					doEvaluateAll(root, soc, p, b);
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
						v.setPlayerAndType(p.getPlayerNum(), VertexType.BASIC_KNIGHT_INACTIVE);
						BotNode node = root.attach(new BotNodeVertex(v, vIndex));
						onBoardChanged();
						doEvaluateAll(node, soc, p, b);
						//buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
						v.setOpen();
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
					//evaluatePlayer(root, soc, p, b);
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
					//evaluatePlayer(root, soc, p, b);
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
					//evaluatePlayer(root, soc, p, b);
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
						int savePlayer = knight.getPlayer();
						knight.setOpen();
						for (int moveIndex : knightMoves) {
							Vertex knightMove = b.getVertex(moveIndex);
							VertexType saveType2 = knightMove.getType();
							BotNode knightMoveChoice = knightChoice.attach(new BotNodeVertex(knightMove, moveIndex));
							onBoardChanged();
							doEvaluateAll(knightMoveChoice, soc, p, b);
							if (saveType2 == VertexType.OPEN) {
								//buildChooseMoveTreeR(soc, p, b, knightMoveChoice, SOC.computeMoves(p, b, soc));
								knightMove.setOpen();
							} else {
								knightMove.setPlayerAndType(p.getPlayerNum(), saveType2);
							}
						}
						knight.setPlayerAndType(savePlayer, saveType);
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
						List<Integer> newPositions = SOC.computeShipRouteIndices(soc, p.getPlayerNum(), b);
						for (int moveIndex : newPositions) {
							if (moveIndex == shipIndex)
								continue;
							Route newPos = b.getRoute(moveIndex);
							b.setPlayerForRoute(newPos, p.getPlayerNum(), RouteType.SHIP);
							newPos.setLocked(true);
							BotNode node = new BotNodeRoute(newPos, moveIndex);
							onBoardChanged();
							//evaluateEdges(node, soc, p, b);
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
					doEvaluateAll(root, soc, p, b);
					for (int knightIndex : promotable) {
						Vertex knight = b.getVertex(knightIndex);
						knight.promoteKnight();
						BotNode node = root.attach(new BotNodeVertex(knight, knightIndex));
						onBoardChanged();
						//evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
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
						//evaluatePlayer(node, soc, p, b);
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
					if (knightOptions.size() == 0) {
						root.clear();
					} else for (int pIndex : SOC.computeDeserterPlayers(soc, b, p)) {
						BotNode node = root.attach(new BotNodePlayer(soc.getPlayerByPlayerNum(pIndex)));
						int oIndex = b.getKnightsForPlayer(pIndex).get(0);
						Vertex oVertex = b.getVertex(oIndex);
						VertexType oType = oVertex.getType();
						oVertex.setOpen();
						for (int kIndex : knightOptions) {
							Vertex v = b.getVertex(kIndex);
							assert(v.getType() == VertexType.OPEN);
							v.setPlayerAndType(p.getPlayerNum(), oType);
							onBoardChanged();
							BotNode b2 = node.attach(new BotNodeVertex(v, kIndex));
							doEvaluateAll(b2, soc, p, b);
							// dont recurse since this step is a bit random
							v.setOpen();
						}
						oVertex.setPlayerAndType(pIndex, oType);
					}
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
								//evaluateEdges(node, soc, p, b);
								buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
								b.setRouteOpen(r2);
							}
						} else {
							onBoardChanged();
							//evaluateEdges(n, soc, p, b);
							buildChooseMoveTreeR(soc, p, b, n, SOC.computeMoves(p, b, soc));
						}
						b.setPlayerForRoute(r, savePlayer, RouteType.ROAD);
					}
//					p.addCard(ProgressCardType.Diplomat);
					break;
				}
				case ENGINEER_CARD: {
					p.removeCard(ProgressCardType.Engineer);
					List<Integer> cities = SOC.computeCityWallVertexIndices(p.getPlayerNum(), b);
					for (int cIndex : cities) {
						Vertex city = b.getVertex(cIndex);
						city.setPlayerAndType(p.getPlayerNum(), VertexType.WALLED_CITY);
						onBoardChanged();
						BotNode node = root.attach(new BotNodeVertex(city, cIndex));
						//evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
						city.setPlayerAndType(p.getPlayerNum(), VertexType.CITY);
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
							doEvaluateAll(node, soc, p, b);
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
						VertexType saveType = v.getType();
						v.setOpen();
						// since we dont know where the opponent will position the displaced knight, we evaluate here
						BotNode node = root.attach(new BotNodeVertex(v, vIndex));
						onBoardChanged();
						doEvaluateAll(node, soc, p, b);
						v.setPlayerAndType(savePlayer, saveType);
					}
					//p.addCard(ProgressCardType.Intrigue);
					break;
				}
				case INVENTOR_CARD: {
					p.removeCard(ProgressCardType.Inventor);
					List<Integer> tiles = SOC.computeInventorTileIndices(b, soc);
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
							doEvaluateAll(node, soc, p, b);
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
					doEvaluateAll(root, soc, p, b);
					if (numGained > 0) {
						p.incrementResource(ResourceType.Wheat, numGained);
						//evaluatePlayer(root, soc, p, b);
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
						doEvaluateAll(node, soc, p, b);
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
							for (int vIndex : settlements) {
    							Vertex v = b.getVertex(vIndex);
    							v.setPlayerAndType(p.getPlayerNum(), VertexType.CITY);
    							onBoardChanged();
    							BotNode node = root.attach(new BotNodeVertex(v, vIndex));
    							//evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
    							buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
    							v.setPlayerAndType(p.getPlayerNum(), VertexType.SETTLEMENT);
    						}
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
						//evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
						//evaluateTiles(node, soc, p, b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
						b.setMerchant(saveMerchantTile, saveMerchantPlayer);
					}
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
						doEvaluateAll(n, soc, p, b);
						// for random things we need to add some extra randomness
						n.addValue("randomness", Utils.randFloatX(1));
						p.incrementResource(t, -(soc.getNumPlayers()-1));
					}
					p.addCard(DevelopmentCardType.Monopoly);
					break;
				case RESOURCE_MONOPOLY_CARD: {
					p.removeCard(ProgressCardType.ResourceMonopoly);
//					evaluatePlayer(root, soc, p, b);
					int num = 2*(soc.getNumPlayers()-1);
					for (ResourceType t : ResourceType.values()) {
						BotNode n = root.attach(new BotNodeEnum(t));
						p.incrementResource(t, num);
						doEvaluateAll(n, soc, p, b);
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
					doEvaluateAll(root, soc, p, b);
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
						doEvaluateAll(node, soc, p, b);
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
						//evaluateVertices(root, soc.getRules(), p.getPlayerNum(), b);
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
					doEvaluateAll(root, soc, p, b);
					List<Integer> weddingPlayers = SOC.computeWeddingOpponents(soc, p);
					if ( weddingPlayers.size() == 0)
						root.clear();
					else {
						root.addValue("wedding", 0.1 * weddingPlayers.size());
					}
					//p.addCard(ProgressCardType.Wedding);
					break;
				}
				case ATTACK_PIRATE_FORTRESS: {
					for (int vIndex : SOC.computeAttackablePirateFortresses(b, p)) {
						Vertex v = b.getVertex(vIndex);
						int score = b.getRoutesOfType(getPlayerNum(), RouteType.WARSHIP).size();
						float chance = 1f * score / 6f;
						assert(v.getType() == VertexType.PIRATE_FORTRESS);
						assert(v.getPlayer() == 0);
						v.setPlayerAndType(getPlayerNum(), VertexType.SETTLEMENT);
						BotNode node = root.attach(new BotNodeVertex(v, vIndex));
						node.chance = chance;
						doEvaluateAll(node, soc, p, b);
						v.setPirateFortress();
					}
					break;
				}
				case BUILD_WARSHIP:
					for (int rIndex : b.getRoutesIndicesOfType(getPlayerNum(), RouteType.SHIP)) {
						Route r = b.getRoute(rIndex);
						r.setType(RouteType.WARSHIP);
						doEvaluateAll(root.attach(new BotNodeRoute(r, rIndex))	, soc, p, b);
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
						doEvaluateAll(node, soc, p, b);
					}
					break;
				}
				case KNIGHT_ATTACK_STRUCTURE: {
					for (int vIndex : SOC.computeAttackableStructures(soc, getPlayerNum(), b)) {
						Board copy = b.deepCopy();
						Vertex v = copy.getVertex(vIndex);
						assert(v.getPlayer() > 0);
						assert(v.getPlayer() != getPlayerNum());
						int score = SOC.computeAttackerScoreAgainstStructure(v, getPlayerNum(), copy);
						VertexType [] result = new VertexType[1];
						float chance = 1.0f*score / SOC.getKnightScoreToAttackStructure(v, result, soc.getRules());
						BotNode node = root.attach(new BotNodeVertex(v, vIndex));
						node.chance = chance;
						v.setPlayerAndType(v.getPlayer(), result[0]);
						doEvaluateAll(node, soc, p, b);
					}
					break;
				}
				case WARSHIP_CARD: {
					for (int rIndex : b.getRoutesIndicesOfType(getPlayerNum(), RouteType.SHIP)) {
						Route r = b.getRoute(rIndex);
						r.setType(RouteType.WARSHIP);
						doEvaluateAll(root.attach(new BotNodeRoute(r, rIndex)), soc, p, b);
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
		
		BotNode root = createNewTree("Choose Move");
//		evaluateEdges(root, soc, this, soc.getBoard());
//		evaluatePlayer(root, soc, this, soc.getBoard());
//		evaluateTiles(root, soc, this, soc.getBoard());
//		evaluateVertices(root, soc, this, soc.getBoard());
//		evaluateSeafarers(root, soc,this, soc.getBoard());
//		evalu
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
		BotNode root = createNewTree("Choose Vertex");
		
		for (int vIndex : vertexIndices) {
			Vertex v = b.getVertex(vIndex);
			Vertex save = v.deepCopy();
			int islandNum = b.getIslandAdjacentToVertex(v);
			boolean discovered = false;
			if (islandNum > 0)
				discovered = b.isIslandDiscovered(p.getPlayerNum(), islandNum);
			switch (mode) {
				case CITY:
					v.setPlayerAndType(p.getPlayerNum(), VertexType.CITY);
					break;
				case CITY_WALL:
					v.setPlayerAndType(p.getPlayerNum(), VertexType.WALLED_CITY);
					break;
				case KNIGHT_DESERTER:
					v.setOpen();
					break;
				case KNIGHT_DISPLACED:
				case KNIGHT_MOVE_POSITION:
				case KNIGHT_TO_ACTIVATE:
				case KNIGHT_TO_MOVE:
				case KNIGHT_TO_PROMOTE:
				case NEW_KNIGHT:
				case OPPONENT_KNIGHT_TO_DISPLACE:
				case POLITICS_METROPOLIS:
				case SCIENCE_METROPOLIS:
				case TRADE_METROPOLIS:
				case PIRATE_FORTRESS:
				case OPPONENT_STRUCTURE_TO_ATTACK:
					assert(false);
					break;
				case SETTLEMENT: {
					v.setPlayerAndType(p.getPlayerNum(), VertexType.SETTLEMENT);
					if (islandNum > 0 && !discovered) {
						b.setIslandDiscovered(p.getPlayerNum(), islandNum, true);
					}
					break;
				}
			}
			onBoardChanged();
			BotNode node = root.attach(new BotNodeVertex(v, vIndex));
			doEvaluateAll(node, soc, p, b);
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

		BotNode root = createNewTree("Choose Route");
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
			doEvaluateAll(node, soc, p, b);
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
		
		BotNode root = createNewTree("Choose Route Type");
		Player p = new PlayerTemp(this);
		Board b = soc.getBoard();
		
		addRouteBuildingMovesR(soc, p, b, root, true, true, 1, false);

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
		
		BotNode root = createNewTree("Choose Tile");
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
					doEvaluateAll(node, soc, p, b);
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
		
		BotNode root = createNewTree("Choose Player");
		
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

		BotNode root = createNewTree("Choose Card");
		for (Card c : cards) {
			addCard(c);
			BotNode node = root.attach(new BotNodeCard(c));
			doEvaluateAll(node, soc, this, soc.getBoard());
			removeCard(c);
		}
		buildOptimalPath(root);
		return detatchMove();
	}
	
	@Override
	public <T extends Enum<T>> T chooseEnum(SOC soc, EnumChoice mode, T [] values) {
		if (movesPath != null) {
			return detatchMove();
		}
		
		switch (mode) {
			case CRANE_CARD_DEVELOPEMENT:
				break;
			case DRAW_DEVELOPMENT_CARD:
			case DRAW_PROGRESS_CARD:
				return Utils.randItem(values);

			case MONOPOLY:
				break;
		}
		throw new AssertionError("Dont know how to handle this");
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
	
    private static void doEvaluateAll(BotNode node, SOC soc, Player p, Board b) {
    	doEvaluateEdges(node, soc, p, b);
    	doEvaluatePlayer(node, soc, p, b);
    	doEvaluateSeafarers(node, soc, p, b);
    	doEvaluateTiles(node, soc, p, b);
    	doEvaluateVertices(node, soc, p, b);
    }
    
	private static void doEvaluateVertices(BotNode node, SOC soc, Player p, Board b) {
		
		////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
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
		////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		int playerNum = p.getPlayerNum();
		Rules rules = soc.getRules();
		
		HashSet<Integer> tilesAdjacent = new HashSet<Integer>();
		HashSet<Integer> tilesProtected  = new HashSet<Integer>();
		
		double [] resourceProb = new double[SOC.NUM_RESOURCE_TYPES];
		final int [] resourcePorts = new int[SOC.NUM_RESOURCE_TYPES];
		double [] resourceTradeFactor = new double[SOC.NUM_RESOURCE_TYPES];
		
		for (int i=0; i<resourceTradeFactor.length; i++) {
			resourceTradeFactor[i] = 4; // default is 4:1 trade
		}
		
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
							resourceTradeFactor[cell.getResource().ordinal()] = 2;
							break;
						case PORT_MULTI:
							numMultiPorts++;
							for (int i=0; i<resourceTradeFactor.length; i++)
								resourceTradeFactor[i] = Math.min(resourceTradeFactor[i], 3);
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

		for (int i=0; i<resourceProb.length; i++) {
			resourceProb[i] /= resourceTradeFactor[i]; // scale a resource differently when it has a trade option
		}
		
		double resourceProbValue = 4 * CMath.sum(resourceProb);
		//resourceProbValue *= 2;
		
		double portValue = 0;
		for (int i=0; i<SOC.NUM_RESOURCE_TYPES; i++) {
			if (resourcePorts[i] > 0)
				portValue += 0.02;
			else if (numMultiPorts > 0)
				portValue += 0.01;
		}		
		
		/*
		double resourcePortProb = 0;
		for (int i=0; i<SOC.NUM_RESOURCE_TYPES; i++) {
			if (resourcePorts[i] > 0)
				resourcePortProb += resourceProb[i];
		}*/

		tilesAdjacent.retainAll(tilesProtected);
		float covered = 0;
		for (int tIndex : tilesAdjacent) {
			// these are the uncovered tiles
			covered += getDiePossibility(b.getTile(tIndex).getDieNum(), rules);
		}
		double ave = CMath.sum(resourceProb) / resourceProb.length;
		double stdDev = 0.1 * Math.abs(CMath.stdDev(resourceProb, ave));
		double resourceDistribution = ave - stdDev;
		
		node.addValue("Resource distribution", resourceDistribution);
		node.addValue("Port value", portValue);
		node.addValue("Resource Prob", resourceProbValue);
		node.addValue("Structures", structureValue);
		node.addValue("tiles protected", covered);
		node.addValue("knights", knightValue);
	}

	private static void doEvaluateEdges(BotNode node, SOC soc, Player p, final Board b) {

		float value = 0;
		// evaluate 
		for (Route r : b.getRoutesForPlayer(p.getPlayerNum())) {
			for (Tile t : b.getRouteTiles(r)) {
				if (t.getResource() != null)
					value += getDiePossibility(t.getDieNum(), soc.getRules());
				else if (t.getType() == TileType.GOLD) {
					value += 2 * getDiePossibility(t.getDieNum(), soc.getRules());
				}
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
		boolean [] usedPorts = new boolean[SOC.NUM_RESOURCE_TYPES];
		boolean usedMulti = false;
		double [] resourceProb = new double[SOC.NUM_RESOURCE_TYPES];
		for (int vIndex=0; vIndex<b.getNumVerts(); vIndex++) {
			if (b.isVertexAdjacentToPlayerRoute(vIndex, p.getPlayerNum()) && b.isVertexAvailbleForSettlement(vIndex)) {
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
							usedPorts[cell.getResource().ordinal()] = true;
							break;
						case PORT_MULTI:
							usedMulti = true;
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
		
		for (int i=0; i<SOC.NUM_RESOURCE_TYPES; i++) {
			if (usedPorts[i])
				resourceProb[i] *= 1.25;
			else if (usedMulti)
				resourceProb[i] *= 1.1;
		}
		
		double potentialSettlementResourceProb = CMath.sum(resourceProb);
		
		node.addValue("potentialSettlements", numPotential);
		node.addValue("potentialResourceProb", potentialSettlementResourceProb);
	}
	
	private final static int DISTANCE_INFINITY = 100;
	
	public static class Distances extends Reflector<Distances> {
		
		static {
			omitField(Distances.class, "dist");
			omitField(Distances.class, "next");
			addAllFields(Distances.class);
		}
		
		public Distances() {
			this(0,null,0, new int[0][0], new int[0][0]);
		}
		
		public final float shortesDistanceToAPirateFortress;
		public final float [] shortestDistanceToUndiscoveredIslands;
		public final float shortestDistanceToUndiscoveredTile;
		private final int [][] dist;
		private final int [][] next;
		
		public Distances(float shortesDistanceToAPirateFortress,
				float[] shortestDistanceToUndiscoveredIslands,
				float shortestDistanceToUndiscoveredTile,
				int [][] dist, int [][] next) {
			super();
			this.shortesDistanceToAPirateFortress = shortesDistanceToAPirateFortress;
			this.shortestDistanceToUndiscoveredIslands = shortestDistanceToUndiscoveredIslands;
			this.shortestDistanceToUndiscoveredTile = shortestDistanceToUndiscoveredTile;
			this.dist = dist;
			this.next = next;
		}
		
		@Override
		public String toString() {
			return "shortestDistanceToPirateFortress=" + shortesDistanceToAPirateFortress
				 + "\nshortestDistanceToUndiscoveredIslands=" + Arrays.toString(shortestDistanceToUndiscoveredIslands)
				 + "\nshortestDistanceToUndiscoveredTile=" + shortestDistanceToUndiscoveredTile;
		}
		
		public List<Integer> getShortestPath(int fromVertex, int toVertex) {
			List<Integer> path = new ArrayList<Integer>();
			if (dist[fromVertex][toVertex] == DISTANCE_INFINITY)
				return path;
			
			path.add(fromVertex);
			while (fromVertex != toVertex) {
				fromVertex = next[fromVertex][toVertex];
				path.add(fromVertex);
			}
			
			return path;
		}
		
		public int getDist(int from, int to) {
			return dist[from][to];
		}
	}
	
	private static void doEvaluateSeafarers(BotNode node, SOC soc, Player p, Board b) {
		if (soc.getRules().isEnableSeafarersExpansion()) {
			Distances d = computeWaterwayDistances(soc.getRules(), b, p.getPlayerNum());
			node.addValue("minDistPirate", 1f / (1.0+d.shortesDistanceToAPirateFortress));
			node.addValue("minDistNewTile", 1f / (1.0+d.shortestDistanceToUndiscoveredTile));
			if (b.getNumIslands() > 0) {
    			float minDistToUndiscoveredIsland = DISTANCE_INFINITY;
    			float aveDist = 0;
    			int numIslands = 0;
    			for (int i=1; i<=b.getNumIslands(); i++) {
    				if (d.shortestDistanceToUndiscoveredIslands[i] < minDistToUndiscoveredIsland) {
    					minDistToUndiscoveredIsland = d.shortestDistanceToUndiscoveredIslands[i];
    				}
    				if (!b.isIslandDiscovered(p.getPlayerNum(), i)) {
    					aveDist += d.shortestDistanceToUndiscoveredIslands[i];
    					numIslands++;
    				}
    			}
    			node.addValue("minDistToNewIslands", 1f / (1.0+minDistToUndiscoveredIsland));
    			if (numIslands > 0) {
    				aveDist /= numIslands;
    				node.addValue("aveDistToNewIslands", 1f / (1.0+aveDist));
    			}
			}
		}		
	}
	
	public static Distances computeWaterwayDistances(final Rules rules, final Board b, final int playerNum) {
		
		final int numV = b.getNumVerts();
		final int [][] dist = new int[numV][numV];
		final int [][] next = new int[numV][numV];
		
		for (int i=0; i<dist.length; i++) {
			for (int ii=0; ii<dist.length; ii++) {
				dist[i][ii] = DISTANCE_INFINITY;
				next[i][ii] = ii;
			}
			dist[i][i] = 0;
		}
		
		// Assign distances to edges
		for (Route r: b.getRoutes()) {
			if (r.getPlayer() == playerNum) {
				dist[r.getFrom()][r.getTo()] = dist[r.getTo()][r.getFrom()] = 0;
				continue;
			}
			
			if (r.isAttacked() || r.isClosed() || r.getPlayer() != 0) {
				continue;
			}
			
			if (!r.isAdjacentToWater()) {
				// any land is 1 unit
				dist[r.getFrom()][r.getTo()] = dist[r.getTo()][r.getFrom()] = 1;
			} else {
				// in the water.  If one of verts is a land vert, then can only transition on
				// a port (if rules apply) or settlement
				// only if settlement (or port when rules apply) can build a ship
				Vertex v0 = b.getVertex(r.getFrom());
				Vertex v1 = b.getVertex(r.getTo());
				
				// water only routes are just 1 unit
				if (!v0.isAdjacentToLand() && !v1.isAdjacentToLand()) {
					dist[r.getFrom()][r.getTo()] = dist[r.getTo()][r.getFrom()] = 1;
					continue;
				}
				
				if (rules.isEnableBuildShipsFromPort()) {
					boolean done = false;
					for (Tile t : b.getVertexTiles(v0)) {
						if (t.isPort()) {
							dist[r.getFrom()][r.getTo()] = dist[r.getTo()][r.getFrom()] = 1;
							done = true;
							break;
						}
					}
					
					if (done)
						continue;
					for (Tile t : b.getVertexTiles(v1)) {
						if (t.isPort()) {
							dist[r.getFrom()][r.getTo()] = dist[r.getTo()][r.getFrom()] = 1;
							done = true;
							break;
						}
					}
					if (done)
						continue;
				} else {
					// water routes extending from land must be adjacent to player's ship, settlement or potential settlement
					if (v0.getPlayer() != playerNum && 
							v1.getPlayer() != playerNum &&
							!b.isVertexAdjacentToPlayerShip(r.getFrom(), playerNum) &&
							!b.isVertexAdjacentToPlayerShip(r.getTo(), playerNum) &&
							!b.isVertexAvailbleForSettlement(r.getFrom()) &&
							!b.isVertexAvailbleForSettlement(r.getTo()))
						continue; // these stay at infinity
				}
				
				dist[r.getFrom()][r.getTo()] = dist[r.getTo()][r.getFrom()] = 1;

				/*
				if (v0.isAdjacentToLand() && v0.isStructure() && v0.getPlayer() == playerNum) {
					dist[r.getFrom()][r.getTo()] = dist[r.getTo()][r.getFrom()] = 1;
					continue;
				}

				if (v1.isAdjacentToLand() && v1.isStructure() && v1.getPlayer() == playerNum) {
					dist[r.getFrom()][r.getTo()] = dist[r.getTo()][r.getFrom()] = 1;
					continue;
				}
				
				boolean done = false;
				for (Route r2 : b.getVertexRoutes(r.getFrom())) {
					if (r2 != r) {
						if (r2.isVessel()) {
							dist[r.getFrom()][r.getTo()] = dist[r.getTo()][r.getFrom()] = 1;
							done = true;
							break;
						}
					}
				}
				
				if (!done) {
					for (Route r2 : b.getVertexRoutes(r.getTo())) {
						if (r2 != r) {
							if (r2.isVessel()) {
								dist[r.getFrom()][r.getTo()] = dist[r.getTo()][r.getFrom()] = 1;
								done = true;
								break;
							}
						}
					}
				}
				
				// 2 moves includes the move to build a structure (more?)
				//   should we consider whether the player can build a settlement?
				if (!done)
					dist[r.getFrom()][r.getTo()] = dist[r.getTo()][r.getFrom()] = 2;
					*/
			} 
		}
		
		// All-Pairs shortest paths [Floyd-Marshall O(|V|^3)] algorithm.  This is a good choice for dense graphs like ours
		// where every vertex has 2 or 3 edges.  The memory usage and complexity of a Dijkstra's make it less desirable.  
		for (int k=0; k<dist.length; k++) {
			for (int i=0; i<dist.length; i++) {
				for (int j=0; j<dist.length; j++) {
					int sum = dist[i][k] + dist[k][j];
					if (sum < dist[i][j]) {
						dist[i][j] = sum;
						next[i][j] = next[i][k];
					}
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
				Utils.print("%-4s|", matrix[i][ii] < DISTANCE_INFINITY ? String.valueOf(matrix[i][ii]) : "INF");
			}
			Utils.println();
		}*/

		float shortestDistanceToAPirateFortress = DISTANCE_INFINITY;
		float [] shortestDistanceToUnDiscoveredIsland = new float[b.getNumIslands()+1]; 
		float shortestDistanceToUndiscoveredTile = DISTANCE_INFINITY;

		for (int i=1; i<shortestDistanceToUnDiscoveredIsland.length; i++)
			shortestDistanceToUnDiscoveredIsland[i] = DISTANCE_INFINITY;
		
		Iterable<Integer> pirateFortresses = b.getVertsOfType(0, VertexType.PIRATE_FORTRESS); 
		Iterable<Integer> undiscoveredTiles = b.getTilesOfType(TileType.UNDISCOVERED);
		
		// get the minimum distances to things 
		for (int vIndex : b.getStructuresForPlayer(playerNum)) {

			// check pirate fortress.  We want the smallest possible distance to the nearest fortress
			for (int v2 : pirateFortresses) {
				shortestDistanceToAPirateFortress = Math.min(shortestDistanceToAPirateFortress, dist[vIndex][v2]);
			}
		
    		// we want to consider the distance from any of our structures to any undiscovered tile, pirate fortress
    		for (Island i : b.getIslands()) {
    			if (!i.isDiscoveredBy(playerNum)) {
					for (int rIndex : i.borderRoute) {
						Route r = b.getRoute(rIndex);
						shortestDistanceToUnDiscoveredIsland[i.getNum()] = Math.min(shortestDistanceToUnDiscoveredIsland[i.getNum()], dist[vIndex][r.getFrom()]);
						shortestDistanceToUnDiscoveredIsland[i.getNum()] = Math.min(shortestDistanceToUnDiscoveredIsland[i.getNum()], dist[vIndex][r.getTo()]);
					}
    			} else {
    				shortestDistanceToUnDiscoveredIsland[i.getNum()] = 0;
    			}
    		}
		
    		for (int tIndex : undiscoveredTiles) {
    			Tile t = b.getTile(tIndex);
    			for (int tv:  t.getAdjVerts()) {
    				shortestDistanceToUndiscoveredTile = Math.min(shortestDistanceToUndiscoveredTile, dist[tv][vIndex]);
    			}
			}
		}
		
		return new Distances(shortestDistanceToAPirateFortress, shortestDistanceToUnDiscoveredIsland, shortestDistanceToUndiscoveredTile, dist, next);
	}
	
	private static void doEvaluateTiles(BotNode node, SOC soc, Player p, Board b) {
		
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
			float scale = 1;
			switch (t.getType()) {
				case DESERT:
					break;
				case FIELDS:
				case FOREST:
				case HILLS:
				case MOUNTAINS:
				case PASTURE:
					break;
				case PORT_MULTI:
					scale = 1.2f;
					break;
				case PORT_BRICK:
				case PORT_ORE:
				case PORT_SHEEP:
				case PORT_WHEAT:
				case PORT_WOOD:
					scale = 1.3f;
					break;
				case GOLD:
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
				default:
					break;
				
			}
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
			node.addValue("pirateTile", scale * pirateValue);
		}
		
		// Merchant
		// Inventor
		// should do this when evaluating vertices
		
		
	}
	
	private static void doEvaluatePlayer(BotNode node, SOC soc, Player p, Board b) {
		
		// Special Victory Cards
		float value = 0;
		for (SpecialVictoryType c : SpecialVictoryType.values()) {
			value += c.getData() * p.getCardCount(c);
		}
		node.addValue("specialVictoryCards", value);
		
		// number of progress cards (less is better)
		value = p.getCardCount(CardType.Progress);
		if (value > 0) {
			node.addValue("progressCardCount", 1.0 / value);
		}
		
		// total cards near the max but not over
		// we want the highest value to be half of the max
		//node.addValue("cardsInHand", CMath.normalDistribution(p.getTotalCardsLeftInHand(), soc.getRules().getMaxSafeCards()));
		
		int cardsInHand = 0;
		final int maxCards = soc.getRules().getMaxSafeCardsForPlayer(p.getPlayerNum(), b);
		if (p.getTotalCardsLeftInHand() <= maxCards) {
			cardsInHand = maxCards;
		} else {
			cardsInHand = maxCards - 2 * (p.getTotalCardsLeftInHand() - maxCards);
		}
		
//		int cardsInHand = p.getTotalCardsLeftInHand() > soc.getRules().getMaxSafeCards() ? 0 : 1;
		node.addValue("cardsInHand", 0.1 * cardsInHand);
		
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
		node.addValue("buildability", buildableValue);
		if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
			float developmentValue = 0;
			for (DevelopmentArea a : DevelopmentArea.values()) {
				int num = p.getCardCount(a.commodity);
				int cur = p.getCityDevelopment(a);
				if (cur < DevelopmentArea.MAX_CITY_IMPROVEMENT && num>cur) {
					developmentValue += cur+1;
				}
			}
			node.addValue("cityDevelValue", developmentValue);
		}
	}
	
	private static void evaluateDice(BotNode node, int die1, int die2, SOC soc, Player p, Board b) {
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
	private static void evaluateOpponent(BotNode node, SOC soc, Player opponent, Board b) {
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
