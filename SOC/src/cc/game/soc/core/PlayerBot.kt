package cc.game.soc.core

import cc.game.soc.core.DevelopmentArea
import cc.game.soc.core.DistancesLandWater
import cc.game.soc.core.ProgressCardType
import cc.game.soc.core.TileType
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.math.CMath
import cc.lib.utils.FileUtils
import cc.lib.utils.GException
import java.io.File
import java.io.IOException
import java.io.PrintStream
import java.lang.AssertionError
import java.util.*
import kotlin.math.absoluteValue

open class PlayerBot : Player() {
	private var movesPath: BotNode? = null

	//	private LinkedList<BotNode> leafNodes = new LinkedList<BotNode>();
	//	private Set<Integer> usedRoadRoutes = new HashSet<Integer>();
	//	private Set<Integer> usedShipRoutes = new HashSet<Integer>();
	private val usedMoves: MutableSet<MoveType> = HashSet()
	private var numLeafs = 0

	class Statistics {
		var numOccurances = 0
		var accumulatedValue = 0.0
	}

	override fun reset() {
		super.reset()
		movesPath = null
	}

	private fun createNewTree(desc: String): BotNode {
		val node: BotNode = BotNodeRoot(desc)

		//	leafNodes.clear();
//		usedRoadRoutes.clear();
//		usedShipRoutes.clear();
		usedMoves.clear()
		numLeafs = 0
		// populate with ALL evaluations that will propogate throughout tree
		return node
	}

	private fun <T> detatchMove(): T? {
		val front = movesPath
		movesPath = movesPath!!.next
		return front!!.data as T?
	}

	@Throws(IOException::class)
	private fun dumpAllPathsAsCSV(leafs: List<BotNode>, outFile: File) {
		val out = PrintStream(outFile)
		try {
			Collections.sort(leafs)
			out.print("MOVES")
			val properties = isolateChangingLeafProperties(leafs)
			val maxValues = HashMap<String, Double?>()
			for (n in leafs) {
				for (key in n.properties.keys) {
					val v = n.properties[key]
					if (!maxValues.containsKey(key)) {
						maxValues[key] = v
					} else {
						maxValues[key] = Math.max(v!!, maxValues[key]!!)
					}
				}
			}
			for (leaf in leafs) {
				val moves = LinkedList<BotNode>()
				var n: BotNode? = leaf
				while (n != null) {
					moves.addFirst(n)
					n = n.parent
				}
				run {
					for (n in moves) {
						out.print(String.format(",%s(%4s)", n.description, n.getValue()))
						//out.print(String.format("%-" + maxWidth + "s(%4f)", n.getDescription(), n.getValue()));
					}
					if (leaf.isOptimal) {
						out.print(",** OPTIMAL **")
					}
					out.println()
				}
				var total = 0.0
				for (key in properties) {
					if (leaf.properties.containsKey(key)) {
						total += leaf.properties[key]!!
					} else {
						leaf.properties[key] = 0.0
					}
				}
				for (key in properties) {
					out.print(key)
					var `val`: Double? = null
					for (n in moves) {
						`val` = n.properties[key]
						out.print(String.format(",%s", `val` ?: "0"))
					}
					val max = maxValues[key]!!
					val maxPercent = (100 * `val`!! / max).toInt()
					val maxTotal = (100 * `val` / total).toInt()
					out.print(String.format(",%d%% of max - %d%% of total", maxPercent, maxTotal))
					out.println()
				}
			}
		} finally {
			out.close()
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
	private fun stripUnchangingProperties(nodes: List<BotNode>) {
		val values = HashMap<String, Double>()
		for (n in nodes) {
			values.putAll(n.properties)
		}
		val changing = HashSet<String>()
		for (n in nodes) {
			for (k in values.keys) { //n.properties.keySet()) {
				if (n.properties.containsKey(k)) {
					val value = n.properties[k]!!
					if (value != values[k]) {
						changing.add(k)
					}
				} else {
					n.properties[k] = 0.0
					changing.add(k)
				}
			}
		}
		for (n in nodes) {
			for (k in changing) {
				if (!n.properties.containsKey(k)) {
					n.properties[k] = 0.0
				}
				n.properties.keys.retainAll(changing)
			}
		}
	}

	private fun isolateChangingLeafProperties(leafs: List<BotNode>): Collection<String> {
		val keepers: MutableSet<String> = HashSet()
		val values = HashMap<String, Double>()
		for (n in leafs) {
			values.putAll(n.properties)
		}
		for (n in leafs) {
			for (key in n.properties.keys) {
				if (keepers.contains(key)) continue
				if (values[key] != n.properties[key]) {
					keepers.add(key)
					break
				}
			}
		}
		return keepers
	}

	private fun isolateChangingProperties(leaf: BotNode): List<String> {
		val props: MutableList<String> = ArrayList()
		for (key in leaf.properties.keys) {
			var `val`: Any? = leaf.properties[key]
			var p = leaf.parent
			while (p != null) {
				val val2: Any? = p.properties[key]
				if (val2 == null || val2 != `val`) {
					props.add(key)
					break
				}
				`val` = val2
				p = p.parent
			}
		}
		return props
	}

	/**
	 * Use this method as an option to return a different node for optimal.  Must be one of the nodes in the leafs.
	 * @param optimal
	 * @param leafs
	 * @return
	 */
	protected open fun onOptimalPath(optimal: BotNode?, leafs: List<BotNode>): BotNode? {
		return optimal
	}

	private fun buildOptimalPath(root: BotNode) {
		val leafs = ArrayList<BotNode>()
		var child = buildOptimalPathR(root, leafs)
		stripUnchangingProperties(leafs)
		leafs.sort()
		child = onOptimalPath(child, leafs)
		while (child == null) {
			for (l in leafs) {
				l.resetCache()
			}
			leafs.sort()
			child = onOptimalPath(null, leafs)
		}
		assert(leafs.contains(child))
		child.isOptimal = true
		if (DEBUG_ENABLED) {
			for (property in child.keys) {
				addStat(property, child.getValue(property)!!)
			}
		}
		if (false && DEBUG_ENABLED) {
//			root.printTree(System.out);
			try {
				//PrintStream out = new PrintStream(new File("/tmp/playerAI" + getPlayerNum()));
				//dumpAllPaths(leafs, out);
				//out.close();
				val fileName = System.getenv("HOME") + "/.soc/playerAI" + playerNum + ".csv"
				FileUtils.backupFile(fileName, 10)
				dumpAllPathsAsCSV(leafs, File(fileName))
			} catch (e: IOException) {
				e.printStackTrace()
			}
		}
		while (child!!.parent != null && child.parent!!.data != null) {
			child.parent!!.next = child
			child = child.parent
		}
		movesPath = child
		val buf = StringBuffer()
		buf.append(name).append(" Path: ")
		var n = movesPath
		while (n != null) {
			buf.append(n.description)
			if (n.next != null) buf.append("==>>")
			n = n.next
		}
		log.debug(buf.toString())

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
	private fun buildOptimalPathR(root: BotNode, leafs: MutableList<BotNode>): BotNode? {
		return if (root.isLeaf) {
			leafs.add(root)
			root
		} else {
			var best: BotNode? = null
			for (child in root.children) {
				val node = buildOptimalPathR(child, leafs)
				if (best == null) best = node else if (node!!.getValue() > best.getValue()) {
					best = node
				}
			}
			best
		}
	}

	private fun checkDiscoveredTerritories(p: Player, r: Route, b: Board): Tile? {
		var v = b.getVertex(r.from)
		for (i in 0..1) {
			for (t in b.getVertexTiles(v)) {
				if (t.type === TileType.UNDISCOVERED) {
					t.type = TileType.NONE
					p.incrementDiscoveredTerritory(1)
					return t
				}
			}
			v = b.getVertex(r.to)
		}
		return null
	}

	private fun addRouteBuildingMovesR(soc: SOC, p: Player, b: Board, root: BotNode, allowRoads: Boolean, allowShips: Boolean, depth: Int, recurseMoves: Boolean) {
		if (depth <= 0) {
			if (recurseMoves) buildChooseMoveTreeR(soc, p, b, root, SOC.computeMoves(p, b, soc)) else {
				doEvaluateAll(root, soc, p, b)
			}
			return
		}
		val roads: List<Int> = if (allowRoads) SOC.computeRoadRouteIndices(p.playerNum, b) else listOf()
		val ships: List<Int> = if (allowShips) SOC.computeShipRouteIndices(soc, p.playerNum, b) else listOf()
		val numRoads = roads.size
		val numShips = ships.size

//		roads.removeAll(usedRoadRoutes);
//		ships.removeAll(usedShipRoutes);

//		usedRoadRoutes.addAll(roads);
//		usedShipRoutes.addAll(ships);
		if (numRoads > 0) {
			var n = root
			if (numShips > 0) n = root.attach(BotNodeEnum(RouteChoiceType.ROAD_CHOICE))
			for (roadIndex in roads) {
				val r = b.getRoute(roadIndex)
				assert(r.type === RouteType.OPEN)
				assert(r.isAdjacentToLand)
				b.setPlayerForRoute(r, p.playerNum, RouteType.ROAD)
				r.isLocked = true
				val t = checkDiscoveredTerritories(p, r, b)
				var min = 0
				var overtaken = false
				if (t != null && soc.rules.minMostDiscoveredTerritories.also { min = it } > 0) {
					if (p.numDiscoveredTerritories >= min) {
						// see if we have overtaken another player
						overtaken = true
						for (other in soc.players) {
							if (other.playerNum != p.playerNum) {
								if (other.numDiscoveredTerritories >= p.numDiscoveredTerritories) {
									overtaken = false
								}
							}
						}
					}
					if (overtaken) {
						p.addCard(SpecialVictoryType.Explorer)
					}
				}
				addRouteBuildingMovesR(soc, p, b, n.attach(BotNodeRoute(r, roadIndex)), allowRoads, allowShips, depth - 1, recurseMoves)
				if (t != null) {
					p.incrementDiscoveredTerritory(-1)
					t.type = TileType.UNDISCOVERED
				}
				if (overtaken) {
					p.removeCard(SpecialVictoryType.Explorer)
				}
				r.isLocked = false
				b.setRouteOpen(r)
			}
		}
		if (numShips > 0) {
			var n = root
			if (numRoads > 0) n = root.attach(BotNodeEnum(RouteChoiceType.SHIP_CHOICE))
			for (shipIndex in ships) {
				val r = b.getRoute(shipIndex)
				assert(r.type === RouteType.OPEN)
				assert(r.isAdjacentToWater)
				b.setPlayerForRoute(r, p.playerNum, RouteType.SHIP)
				r.isLocked = true
				val t = checkDiscoveredTerritories(p, r, b)
				addRouteBuildingMovesR(soc, p, b, n.attach(BotNodeRoute(r, shipIndex)), allowRoads, allowShips, depth - 1, recurseMoves)
				if (t != null) {
					p.incrementDiscoveredTerritory(-1)
					t.type = TileType.UNDISCOVERED
				}
				r.isLocked = false
				b.setRouteOpen(r)
			}
		}

//		usedRoadRoutes.removeAll(roads);
//		usedShipRoutes.removeAll(ships);
	}

	private fun processMetropolis(p: Player, soc: SOC, _b: Board, root: BotNode, area: DevelopmentArea, craneAdjust: Int) {
		var devel = p.getCityDevelopment(area)
		devel++
		val numCards = devel - craneAdjust
		p.removeCards(area.commodity, numCards)
		p.setCityDevelopment(area, devel)
		val b = _b.deepCopy()
		if (SOC.checkMetropolis(soc, b, devel, p.playerNum, area)) {
			for (vIndex in SOC.computeMetropolisVertexIndices(p.playerNum, b)) {
				val v = b.getVertex(vIndex)
				val save = v.deepCopy()
				v.setType(area.vertexType)
				val n = root.attach(BotNodeVertex(v, vIndex))
				val oldNum = soc.getMetropolisPlayer(area)
				soc.setMetropolisPlayer(area, p.playerNum)
				buildChooseMoveTreeR(soc, p, b, n, SOC.computeMoves(p, b, soc))
				v.copyFrom(save)
				soc.setMetropolisPlayer(area, oldNum)
			}
		} else {
			buildChooseMoveTreeR(soc, p, b, root, SOC.computeMoves(p, b, soc))
		}
		p.setCityDevelopment(area, --devel)
		p.addCards(area.commodity, numCards)
	}

	private fun buildChooseMoveTreeR(soc: SOC, p: Player, b: Board, _root: BotNode, moves: Collection<MoveType>) {
		for (move in moves) {
			if (move.aiUseOnce) {
				if (usedMoves.contains(move)) continue
				usedMoves.add(move)
			}
			if (numLeafs > 500) {
				doEvaluateAll(_root, soc, p, b)
				break
			}
			val root = _root.attach(BotNodeEnum(move))
			when (move) {
				MoveType.CONTINUE ->                    //doEvaluate(root, soc, p, b, 1);
					// nothing to evaluate since we inherit all values from our parent
					doEvaluateAll(root, soc, p, b)
				MoveType.TRADE -> {
					val trades = SOC.computeTrades(p, b)
					for (trade in trades) {
						val n = root.attach(BotNodeTrade(trade))
						p.incrementResource(trade.getType(), -trade.amount)
						for (r in ResourceType.values()) {
							if (r === trade.getType()) // as an optimization we skip trades for the same type
								continue  //
							p.addCard(r)
							val node = n.attach(BotNodeCard(r))
							//evaluatePlayer(node, soc, p, b);
							buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc))
							p.removeCard(r)
						}
						if (soc.rules.isEnableCitiesAndKnightsExpansion) {
							for (c in CommodityType.values()) {
								if (c === trade.getType()) continue
								p.addCard(c)
								buildChooseMoveTreeR(soc, p, b, n.attach(BotNodeCard(c)), SOC.computeMoves(p, b, soc))
								p.removeCard(c)
							}
						}
						p.incrementResource(trade.getType(), trade.amount)
					}
				}
				MoveType.BUILD_SETTLEMENT -> {
					val verts = SOC.computeSettlementVertexIndices(soc, p.playerNum, b)
					p.adjustResourcesForBuildable(BuildableType.Settlement, -1)
					for (vIndex in verts) {
						val v = b.getVertex(vIndex)
						v.setPlayerAndType(p.playerNum, VertexType.SETTLEMENT)
						onBoardChanged()
						val node = root.attach(BotNodeVertex(v, vIndex))
						//evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc))
						v.setOpen()
					}
					p.adjustResourcesForBuildable(BuildableType.Settlement, 1)
				}
				MoveType.BUILD_CITY -> {
					val verts = SOC.computeCityVertxIndices(p.playerNum, b)
					p.adjustResourcesForBuildable(BuildableType.City, -1)
					for (vIndex in verts) {
						val v = b.getVertex(vIndex)
						v.setPlayerAndType(p.playerNum, VertexType.CITY)
						onBoardChanged()
						val node = root.attach(BotNodeVertex(v, vIndex))
						//evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc))
						v.setPlayerAndType(p.playerNum, VertexType.SETTLEMENT)
					}
					p.adjustResourcesForBuildable(BuildableType.City, 1)
				}
				MoveType.BUILD_CITY_WALL -> {
					val verts = SOC.computeCityWallVertexIndices(p.playerNum, b)
					p.adjustResourcesForBuildable(BuildableType.CityWall, -1)
					for (vIndex in verts) {
						val v = b.getVertex(vIndex)
						v.setPlayerAndType(p.playerNum, VertexType.WALLED_CITY)
						onBoardChanged()
						val node = root.attach(BotNodeVertex(v, vIndex))
						//evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc))
						v.setPlayerAndType(p.playerNum, VertexType.CITY)
					}
					p.adjustResourcesForBuildable(BuildableType.CityWall, 1)
				}
				MoveType.REPAIR_ROAD -> {
					p.adjustResourcesForBuildable(BuildableType.Road, -1)
					p.removeCard(SpecialVictoryType.DamagedRoad)
					//evaluatePlayer(root, soc, p, b);
					buildChooseMoveTreeR(soc, p, b, root, SOC.computeMoves(p, b, soc))
					p.addCard(SpecialVictoryType.DamagedRoad)
					p.adjustResourcesForBuildable(BuildableType.Road, 1)
				}
				MoveType.BUILD_ROAD -> {
					p.adjustResourcesForBuildable(BuildableType.Road, -1)
					addRouteBuildingMovesR(soc, p, b, root, true, false, 1, true)
					p.adjustResourcesForBuildable(BuildableType.Road, 1)
				}
				MoveType.ROAD_BUILDING_CARD -> {
					val copy = p.deepCopy()
					if (!soc.rules.isEnableCitiesAndKnightsExpansion) {
						copy.removeCard(DevelopmentCardType.RoadBuilding)
						copy.setCardsUsable(CardType.Development, false)
					}
					addRouteBuildingMovesR(soc, copy, b, root, true, soc.rules.isEnableSeafarersExpansion, 2, true)
				}
				MoveType.BISHOP_CARD, MoveType.SOLDIER_CARD -> {
					val saveRobber = b.robberTileIndex
					val savePirate = b.pirateTileIndex
					val opts = SOC.computeRobberTileIndices(soc, b)
					for (tIndex in opts) {
						val t = b.getTile(tIndex)
						if (t.isWater) b.setPirate(tIndex) else b.setRobber(tIndex)
						onBoardChanged()
						val node = root.attach(BotNodeTile(t, tIndex))
						doEvaluateAll(node, soc, p, b)
						b.setRobber(saveRobber)
						b.setPirate(savePirate)
					}
				}
				MoveType.YEAR_OF_PLENTY_CARD -> {

					// we want to avoid duplicates (wood/brick == brick/wwod) so ....
					val copy = p.deepCopy()
					copy.removeCard(DevelopmentCardType.YearOfPlenty)
					copy.setCardsUsable(CardType.Development, false)
					var i = 0
					while (i < ResourceType.values().size) {
						val r = ResourceType.values()[i]
						copy.incrementResource(r, 1)
						val n = root.attach(BotNodeCard(r))
						var ii = i
						while (ii < ResourceType.values().size) {
							val r2 = ResourceType.values()[ii]
							copy.incrementResource(r2, 1)
							val node = n.attach(BotNodeCard(r2))
							//evaluatePlayer(node, soc, p, b);
							buildChooseMoveTreeR(soc, copy, b, node, SOC.computeMoves(copy, b, soc))
							copy.incrementResource(r2, -1)
							ii++
						}
						copy.incrementResource(r, -1)
						i++
					}
				}
				MoveType.BUILD_SHIP -> {
					p.adjustResourcesForBuildable(BuildableType.Ship, -1)
					addRouteBuildingMovesR(soc, p, b, root, false, true, 1, true)
					p.adjustResourcesForBuildable(BuildableType.Ship, 1)
				}
				MoveType.BUILD_WARSHIP -> {
					p.adjustResourcesForBuildable(BuildableType.Warship, -1)
					for (rIndex in b.getRoutesIndicesOfType(playerNum, RouteType.SHIP)) {
						val r = b.getRoute(rIndex)
						r.type = RouteType.WARSHIP
						var movePirate = false
						val node = root.attach(BotNodeRoute(r, rIndex))
						if (!soc.isPirateAttacksEnabled) {
							for (t in b.getRouteTiles(r)) {
								if (b.getPirateTile() === t) {
									for (tIndex in SOC.computePirateTileIndices(soc, b)) {
										b.setPirate(tIndex)
										doEvaluateAll(node.attach(BotNodeTile(b.getTile(tIndex), tIndex)), soc, p, b)
									}
									movePirate = true
									b.setPirateTile(t)
									break
								}
							}
						}
						if (!movePirate) {
							buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc))
						}
						r.type = RouteType.SHIP
					}
					p.adjustResourcesForBuildable(BuildableType.Warship, 1)
				}
				MoveType.ATTACK_SHIP -> {
					for (rIndex in SOC.computeAttackableShips(soc, playerNum, b)) {
						val r = b.getRoute(rIndex)
						val defenderNum = r.player
						val dieToWin = SOC.computeDiceToWinAttackShip(b, playerNum, defenderNum)
						val node = root.attach(BotNodeRoute(r, rIndex))
						node.chance = .7f + 1f / dieToWin
						val savedPlayer = r.player
						b.setPlayerForRoute(r, playerNum, r.type)
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc))
						b.setPlayerForRoute(r, savedPlayer, r.type)
					}
				}
				MoveType.DRAW_DEVELOPMENT -> {
					root.chance = 0.8f + 0.4f * (Utils.rand() % 101)
					p.adjustResourcesForBuildable(BuildableType.Development, -1)
					val temp = Card(DevelopmentCardType.Soldier, CardStatus.UNUSABLE)
					p.addCard(temp)
					doEvaluateAll(root, soc, p, b)
					p.removeCard(temp)
					p.adjustResourcesForBuildable(BuildableType.Development, 1)
				}
				MoveType.HIRE_KNIGHT -> {
					p.adjustResourcesForBuildable(BuildableType.Knight, -1)
					val verts = SOC.computeNewKnightVertexIndices(p.playerNum, b)
					for (vIndex in verts) {
						val v = b.getVertex(vIndex)
						v.setPlayerAndType(p.playerNum, VertexType.BASIC_KNIGHT_INACTIVE)
						val node = root.attach(BotNodeVertex(v, vIndex))
						onBoardChanged()
						var robberTile = -1
						if (isKnightNextToRobber(v, b).also { robberTile = it } >= 0) {
							b.setRobber(-1)
							doEvaluateAll(node, soc, p, b)
							//buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
							b.setRobber(robberTile)
						} else {
							doEvaluateAll(node, soc, p, b)
						}
						v.setOpen()
					}
					p.adjustResourcesForBuildable(BuildableType.Knight, 1)
				}
				MoveType.ACTIVATE_KNIGHT -> {
					p.adjustResourcesForBuildable(BuildableType.ActivateKnight, -1)
					val verts = SOC.computeActivateKnightVertexIndices(p.playerNum, b)
					for (vIndex in verts) {
						val v = b.getVertex(vIndex)
						v.activateKnight()
						onBoardChanged()
						buildChooseMoveTreeR(soc, p, b, root.attach(BotNodeVertex(v, vIndex)), SOC.computeMoves(p, b, soc))
						v.deactivateKnight()
					}
					p.adjustResourcesForBuildable(BuildableType.ActivateKnight, 1)
				}
				MoveType.IMPROVE_CITY_POLITICS -> {
					processMetropolis(p, soc, b, root, DevelopmentArea.Politics, 0)
				}
				MoveType.IMPROVE_CITY_SCIENCE -> {
					processMetropolis(p, soc, b, root, DevelopmentArea.Science, 0)
				}
				MoveType.IMPROVE_CITY_TRADE -> {
					processMetropolis(p, soc, b, root, DevelopmentArea.Trade, 0)
				}
				MoveType.MOVE_KNIGHT -> {
					val knights = SOC.computeMovableKnightVertexIndices(soc, p.playerNum, b)
					for (knightIndex in knights) {
						val knight = b.getVertex(knightIndex)
						val knightCopy = knight.deepCopy()
						val knightChoice = root.attach(BotNodeVertex(knight, knightIndex))
						val knightMoves = SOC.computeKnightMoveVertexIndices(soc, knightIndex, b)
						knight.setOpen()
						for (moveIndex in knightMoves) {
							val knightMove = b.getVertex(moveIndex)
							val knightMoveCopy = knightMove.deepCopy()
							knightMove.setPlayerAndType(p.playerNum, knightCopy!!.type)
							val knightMoveChoice = knightChoice.attach(BotNodeVertex(knightMove, moveIndex))
							onBoardChanged()
							var robberTile = -1
							if (isKnightNextToRobber(knightMove, b).also { robberTile = it } >= 0) {
								b.setRobber(-1)
								doEvaluateAll(knightMoveChoice, soc, p, b)
								//buildChooseMoveTreeR(soc, p, b, knightMoveChoice, SOC.computeMoves(p, b, soc));
								b.setRobber(robberTile)
							} else {
								doEvaluateAll(knightMoveChoice, soc, p, b)
							}
							knightMove.copyFrom(knightMoveCopy)
						}
						knight.copyFrom(knightCopy)
					}
				}
				MoveType.MOVE_SHIP -> {
					val shipVerts = SOC.computeMovableShips(soc, p, b)
					for (shipIndex in shipVerts) {
						val shipToMove = b.getRoute(shipIndex)
						val shipType = shipToMove.type
						b.setRouteOpen(shipToMove)
						shipToMove.isLocked = true
						val shipChoice = root.attach(BotNodeRoute(shipToMove, shipIndex))
						val newPositions = SOC.computeShipRouteIndices(soc, p.playerNum, b)
						for (moveIndex in newPositions) {
							if (moveIndex == shipIndex) continue
							val newPos = b.getRoute(moveIndex)
							b.setPlayerForRoute(newPos, p.playerNum, shipType)
							newPos.isLocked = true
							val node: BotNode = BotNodeRoute(newPos, moveIndex)
							onBoardChanged()
							//evaluateEdges(node, soc, p, b);
							buildChooseMoveTreeR(soc, p, b, shipChoice.attach(node), SOC.computeMoves(p, b, soc))
							b.setRouteOpen(newPos)
							newPos.isLocked = false
						}
						b.setPlayerForRoute(shipToMove, p.playerNum, shipType)
						shipToMove.isLocked = false
					}
				}
				MoveType.PROMOTE_KNIGHT -> {
					val promotable = SOC.computePromoteKnightVertexIndices(p, b)
					p.adjustResourcesForBuildable(BuildableType.PromoteKnight, -1)
					//					doEvaluateAll(root, soc, p, b);
					for (knightIndex in promotable) {
						val knight = b.getVertex(knightIndex)
						knight.promoteKnight()
						val node = root.attach(BotNodeVertex(knight, knightIndex))
						onBoardChanged()
						//evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc))
						knight.demoteKnight()
					}
					p.adjustResourcesForBuildable(BuildableType.PromoteKnight, 1)
				}
				MoveType.DEAL_EVENT_CARD, MoveType.ROLL_DICE, MoveType.ROLL_DICE_NEUTRAL_PLAYER -> root.properties.clear()
				MoveType.ALCHEMIST_CARD -> {

					// cycle throught the 2nd die first, since this affects progress card distribution
					var i = 1
					while (i <= 6) {
						var ii = i
						while (ii <= 6) {
							val dice = root.attach(BotNodeDice(ii, i))
							evaluateDice(dice, ii, i, soc, p, b)
							root.attach(dice)
							ii++
						}
						i++
					}
				}
				MoveType.CRANE_CARD -> {
					p.removeCard(ProgressCardType.Crane)
					for (area in SOC.computeCraneCardImprovements(p)) {
						processMetropolis(p, soc, b, root.attach(BotNodeEnum(area)), area, 1)
						/*
						int level = p.getCityDevelopment(area);
						p.incrementResource(area.commodity, -level);
						p.setCityDevelopment(area, level+1);
						BotNode node = root.attach(new BotNodeEnum(area));
						//evaluatePlayer(node, soc, p, b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
						p.setCityDevelopment(area, level);
						p.incrementResource(area.commodity, level);
						*/
					}
				}
				MoveType.DESERTER_CARD -> {
					p.removeCard(ProgressCardType.Deserter)
					val knightOptions = SOC.computeNewKnightVertexIndices(playerNum, b)
					if (knightOptions.size == 0) {
						root.clear()
					} else for (pIndex in SOC.computeDeserterPlayers(soc, b, p)) {
						val node = root.attach(BotNodePlayer(soc.getPlayerByPlayerNum(pIndex)))
						val oIndex = b.getKnightsForPlayer(pIndex)[0]
						val oVertex = b.getVertex(oIndex)
						val oType = oVertex.type
						oVertex.setOpen()
						for (kIndex in knightOptions) {
							val v = b.getVertex(kIndex)
							assert(v.type === VertexType.OPEN)
							v.setPlayerAndType(p.playerNum, oType)
							onBoardChanged()
							val b2 = node.attach(BotNodeVertex(v, kIndex))
							doEvaluateAll(b2, soc, p, b)
							// dont recurse since this step is a bit random
							v.setOpen()
						}
						oVertex.setPlayerAndType(pIndex, oType)
					}
				}
				MoveType.DIPLOMAT_CARD -> {
					p.removeCard(ProgressCardType.Diplomat)
					val allOpenRoutes = SOC.computeDiplomatOpenRouteIndices(soc, b)
					for (rIndex1 in allOpenRoutes) {
						val r = b.getRoute(rIndex1)
						val savePlayer = r.player
						b.setRouteOpen(r)
						val n = root.attach(BotNodeRoute(r, rIndex1))
						if (savePlayer == p.playerNum) {
							val newPos = SOC.computeRoadRouteIndices(p.playerNum, b).toMutableList()
							newPos.remove(rIndex1 as Any)
							for (rIndex2 in newPos) {
								val r2 = b.getRoute(rIndex2)
								b.setPlayerForRoute(r2, p.playerNum, RouteType.ROAD)
								val node = n.attach(BotNodeRoute(r2, rIndex2))
								onBoardChanged()
								//evaluateEdges(node, soc, p, b);
								buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc))
								b.setRouteOpen(r2)
							}
						} else {
							onBoardChanged()
							//evaluateEdges(n, soc, p, b);
							buildChooseMoveTreeR(soc, p, b, n, SOC.computeMoves(p, b, soc))
						}
						b.setPlayerForRoute(r, savePlayer, RouteType.ROAD)
					}
				}
				MoveType.ENGINEER_CARD -> {
					p.removeCard(ProgressCardType.Engineer)
					val cities = SOC.computeCityWallVertexIndices(p.playerNum, b)
					for (cIndex in cities) {
						val city = b.getVertex(cIndex)
						city.setPlayerAndType(p.playerNum, VertexType.WALLED_CITY)
						onBoardChanged()
						val node = root.attach(BotNodeVertex(city, cIndex))
						//evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc))
						city.setPlayerAndType(p.playerNum, VertexType.CITY)
					}
				}
				MoveType.HARBOR_CARD -> {
					p.removeCard(ProgressCardType.Harbor)
					val harborPlayers = SOC.computeHarborTradePlayers(p, soc)
					for (pNum in harborPlayers) {
						val p2 = soc.getPlayerByPlayerNum(pNum)
						val n = root.attach(BotNodePlayer(p2))
						evaluateOpponent(n, soc, p, b)
					}
				}
				MoveType.INTRIGUE_CARD -> {
					p.removeCard(ProgressCardType.Intrigue)
					val intrigueKnights = SOC.computeIntrigueKnightsVertexIndices(p.playerNum, b)
					for (vIndex in intrigueKnights) {
						val v = b.getVertex(vIndex)
						val savePlayer = v.player
						val saveType = v.type
						v.setOpen()
						// since we dont know where the opponent will position the displaced knight, we evaluate here
						val node = root.attach(BotNodeVertex(v, vIndex))
						onBoardChanged()
						doEvaluateAll(node, soc, p, b)
						v.setPlayerAndType(savePlayer, saveType)
					}
				}
				MoveType.INVENTOR_CARD -> {
					p.removeCard(ProgressCardType.Inventor)
					val tiles = SOC.computeInventorTileIndices(b, soc)
					var i = 0
					while (i < tiles.size - 1) {
						var ii = i + 1
						while (ii < tiles.size) {
							val i0 = tiles[i]
							val i1 = tiles[ii]
							val t0 = b.getTile(i0)
							val t1 = b.getTile(i1)
							val die0 = t0.dieNum
							val die1 = t1.dieNum
							if (die0 == die1) {
								ii++
								continue  // ignore these
							}
							t0.dieNum = die1
							t1.dieNum = die0
							val node = root.attach(BotNodeTile(t0, i0)).attach(BotNodeTile(t1, i1))
							onBoardChanged()
							doEvaluateAll(node, soc, p, b)
							t0.dieNum = die0
							t1.dieNum = die1
							ii++
						}
						i++
					}
				}
				MoveType.IRRIGATION_CARD -> {
					val numGained = SOC.computeNumStructuresAdjacentToTileType(p.playerNum, b, TileType.FIELDS)
					p.removeCard(ProgressCardType.Irrigation)
					//doEvaluateAll(root, soc, p, b);
					if (numGained > 0) {
						p.incrementResource(ResourceType.Wheat, numGained)
						//evaluatePlayer(root, soc, p, b);
						buildChooseMoveTreeR(soc, p, b, root, SOC.computeMoves(p, b, soc))
						p.incrementResource(ResourceType.Wheat, -numGained)
					}
				}
				MoveType.MASTER_MERCHANT_CARD -> {
					p.removeCard(ProgressCardType.MasterMerchant)
					var playerToChoose = -1
					var largestHand = 0
					for (playerNum in SOC.computeMasterMerchantPlayers(soc, p)) {
						val num = soc.getPlayerByPlayerNum(playerNum).unusedCardCount
						if (num > largestHand) {
							largestHand = num
							playerToChoose = playerNum
						}
					}
					if (playerToChoose >= 0) {
						val node = root.attach(BotNodePlayer(soc.getPlayerByPlayerNum(playerToChoose)))
						val num = Math.min(2, largestHand)
						val cards: Array<ICardType<*>> = Array(num) {
							Utils.randItem(ResourceType.values())
						}
						cards.forEach {
							p.addCard(it)
						}
						doEvaluateAll(node, soc, p, b)
						cards.forEach {
							p.removeCard(it)
						}
					}
				}
				MoveType.MEDICINE_CARD -> {
					p.removeCard(ProgressCardType.Medicine)
					if (p.getCardCount(ResourceType.Ore) >= 2 && p.getCardCount(ResourceType.Wheat) >= 1) {
						p.incrementResource(ResourceType.Ore, -2)
						p.incrementResource(ResourceType.Wheat, -1)
						val settlements = b.getSettlementsForPlayer(p.playerNum)
						if (settlements.size > 0) {
							for (vIndex in settlements) {
								val v = b.getVertex(vIndex)
								v.setPlayerAndType(p.playerNum, VertexType.CITY)
								onBoardChanged()
								val node = root.attach(BotNodeVertex(v, vIndex))
								//evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
								buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc))
								v.setPlayerAndType(p.playerNum, VertexType.SETTLEMENT)
							}
						}
						p.incrementResource(ResourceType.Ore, 2)
						p.incrementResource(ResourceType.Wheat, 1)
					}
				}
				MoveType.MERCHANT_CARD -> {
					p.removeCard(ProgressCardType.Merchant)
					val tiles = SOC.computeMerchantTileIndices(soc, p.playerNum, b)
					val saveMerchantTile = b.merchantTileIndex
					val saveMerchantPlayer = b.merchantPlayer
					for (tIndex in tiles) {
						val t = b.getTile(tIndex)
						b.setMerchant(tIndex, p.playerNum)
						val node = root.attach(BotNodeTile(t, tIndex))
						//evaluateVertices(node, soc.getRules(), p.getPlayerNum(), b);
						//evaluateTiles(node, soc, p, b);
						buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc))
						b.setMerchant(saveMerchantTile, saveMerchantPlayer)
					}
				}
				MoveType.MERCHANT_FLEET_CARD -> {
					val opts = SOC.computeMerchantFleetCards(p)
					if (opts.size > 0) {
						val copy = p.deepCopy()
						copy.getCard(ProgressCardType.MerchantFleet)!!.setUsed()
						for (c in opts) {
							copy.merchantFleetTradable = c
							buildChooseMoveTreeR(soc, copy, b, root.attach(BotNodeCard(c)), SOC.computeMoves(copy, b, soc))
						}
					}
				}
				MoveType.MINING_CARD -> {
					p.removeCard(ProgressCardType.Mining)
					val numGained = SOC.computeNumStructuresAdjacentToTileType(p.playerNum, b, TileType.MOUNTAINS)
					if (numGained > 0) {
						p.incrementResource(ResourceType.Ore, numGained)
						buildChooseMoveTreeR(soc, p, b, root, SOC.computeMoves(p, b, soc))
						p.incrementResource(ResourceType.Ore, -numGained)
						//						p.addCard(ProgressCardType.Mining);
					}
				}
				MoveType.MONOPOLY_CARD -> {
					val copy = p.deepCopy()
					copy.setCardsUsable(CardType.Development, false)
					copy.removeCard(DevelopmentCardType.Monopoly)
					for (t in ResourceType.values()) {
						val n = root.attach(BotNodeEnum(t))
						copy.incrementResource(t, soc.numPlayers - 1)
						n.chance = computeChanceForResource(soc, b, copy)
						doEvaluateAll(n, soc, copy, b)
						// for random things we need to add some extra randomness
						//n.addValue("randomness", Utils.randFloatX(1));
						copy.incrementResource(t, -(soc.numPlayers - 1))
					}
				}
				MoveType.RESOURCE_MONOPOLY_CARD -> {
					p.removeCard(ProgressCardType.ResourceMonopoly)
					//					evaluatePlayer(root, soc, p, b);
					val num = 2 * (soc.numPlayers - 1)
					for (t in ResourceType.values()) {
						val n = root.attach(BotNodeEnum(t))
						p.incrementResource(t, num)
						doEvaluateAll(n, soc, p, b)
						p.incrementResource(t, -num)
					}
				}
				MoveType.SABOTEUR_CARD -> {
					p.removeCard(ProgressCardType.Saboteur)
					var totalCardsToSabotage = 0.0
					val sabotagePlayers = SOC.computeSaboteurPlayers(soc, playerNum)
					for (pNum in sabotagePlayers) {
						val player = soc.getPlayerByPlayerNum(pNum)
						totalCardsToSabotage += (1 + player.unusedCardCount) / 2
					}
					doEvaluateAll(root, soc, p, b)
					root.addValue("sabotagedCards", if (totalCardsToSabotage > 0) totalCardsToSabotage else -100.0)
				}
				MoveType.SMITH_CARD -> {
					p.removeCard(ProgressCardType.Smith)
					val promotableKnights = SOC.computePromoteKnightVertexIndices(p, b)
					if (promotableKnights.size > 0) {
						if (promotableKnights.size == 1) {
							// promotion is automatic
							val kIndex = promotableKnights[0]
							val v = b.getVertex(kIndex)
							v.promoteKnight()
							val n = root.attach(BotNodeVertex(v, kIndex))
							buildChooseMoveTreeR(soc, p, b, n, SOC.computeMoves(p, b, soc))
							v.demoteKnight()
						} else if (promotableKnights.size == 2) {
							// promotion is automatic
							val k0 = promotableKnights[0]
							val k1 = promotableKnights[1]
							val v0 = b.getVertex(k0)
							val v1 = b.getVertex(k1)
							v0.promoteKnight()
							v1.promoteKnight()
							val n = root.attach(BotNodeVertex(v0, k0)).attach(BotNodeVertex(v1, k1))
							buildChooseMoveTreeR(soc, p, b, n, SOC.computeMoves(p, b, soc))
							v0.demoteKnight()
							v1.demoteKnight()
						} else {
							// compute permutations
							var i = 0
							while (i < promotableKnights.size - 1) {
								var ii = i + 1
								while (ii < promotableKnights.size) {
									val k0 = promotableKnights[i]
									val k1 = promotableKnights[ii]
									val v0 = b.getVertex(k0)
									val v1 = b.getVertex(k1)
									v0.promoteKnight()
									v1.promoteKnight()
									buildChooseMoveTreeR(soc, p, b, root.attach(BotNodeVertex(v0, k0)).attach(BotNodeVertex(v1, k1)), SOC.computeMoves(p, b, soc))
									v1.demoteKnight()
									v0.demoteKnight()
									ii++
								}
								i++
							}
						}
					}
				}
				MoveType.SPY_CARD -> {
					p.removeCard(ProgressCardType.Spy)
					doEvaluateAll(root, soc, p, b)
					val players = SOC.computeSpyOpponents(soc, p.playerNum)
					for (num in players) {
						val player = soc.getPlayerByPlayerNum(num)
						val n = root.attach(BotNodePlayer(player))
						//Card removed = player.removeRandomUnusedCard(CardType.Progress);
						evaluateOpponent(n, soc, player, b)
						//player.addCard(removed);
					}
				}
				MoveType.TRADE_MONOPOLY_CARD -> {
					p.removeCard(ProgressCardType.TradeMonopoly)
					for (t in CommodityType.values()) {
						val node = root.attach(BotNodeEnum(t))
						node.chance = computeChanceForCommodity(soc, this)
						p.addCards(t, 2)
						doEvaluateAll(node, soc, p, b) // dont recurse since the outcome is random
						//buildChooseMoveTreeR(soc, p, b, node, SOC.computeMoves(p, b, soc));
						p.removeCards(t, 2)
					}
				}
				MoveType.WARLORD_CARD -> {
					p.removeCard(ProgressCardType.Warlord)
					val verts = b.getVertIndicesOfType(p.playerNum, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE)
					if (verts.size > 0) {
						for (vIndex in verts) {
							b.getVertex(vIndex).activateKnight()
						}
						//evaluateVertices(root, soc.getRules(), p.getPlayerNum(), b);
						buildChooseMoveTreeR(soc, p, b, root, SOC.computeMoves(p, b, soc))
						for (vIndex in verts) {
							b.getVertex(vIndex).deactivateKnight()
						}
					}
				}
				MoveType.WEDDING_CARD -> {
					p.removeCard(ProgressCardType.Wedding)
					doEvaluateAll(root, soc, p, b)
					val weddingPlayers = SOC.computeWeddingOpponents(soc, p)
					if (weddingPlayers.size == 0) root.clear() else {
						root.addValue("wedding", 0.1 * weddingPlayers.size)
					}
				}
				MoveType.ATTACK_PIRATE_FORTRESS -> {
					for (vIndex in SOC.computeAttackablePirateFortresses(b, p)) {
						val v = b.getVertex(vIndex)
						assert(v.type === VertexType.PIRATE_FORTRESS)
						assert(v.player == 0)
						val playerHealth = b.getRoutesOfType(playerNum, RouteType.WARSHIP).size.toFloat()
						val node = root.attach(BotNodeVertex(v, vIndex))
						node.chance = 1.0f + playerHealth / 6
						val health = v.pirateHealth
						if (health <= 1) v.setPlayerAndType(playerNum, VertexType.SETTLEMENT) else v.pirateHealth = health - 1
						doEvaluateAll(node, soc, p, b)
						v.setPirateFortress()
						v.pirateHealth = health
					}
				}
				MoveType.KNIGHT_ATTACK_ROAD -> {
					for (rIndex in SOC.computeAttackableRoads(soc, playerNum, b)) {
						val info = SOC.computeAttackRoad(rIndex, soc, b, playerNum)
						val route = b.getRoute(rIndex)
						val node = root.attach(BotNodeRoute(route, rIndex))
						node.chance = .5f + (info.knightStrength - info.minScore).toFloat() / 6
						val savedType = route.type
						route.type = info.destroyedType?:RouteType.OPEN
						if (info.destroyedType === RouteType.OPEN) b.clearRouteLenCache()
						doEvaluateAll(node, soc, p, b)
						route.type = savedType
						if (info.destroyedType === RouteType.OPEN) b.clearRouteLenCache()
						for (k in info.attackingKnights) {
							b.getVertex(k).activateKnight()
						}
					}
				}
				MoveType.KNIGHT_ATTACK_STRUCTURE -> {
					for (vIndex in SOC.computeAttackableStructures(soc, playerNum, b)) {
						val info = SOC.computeStructureAttack(vIndex, soc, b, playerNum)
						val v = b.getVertex(vIndex)
						val node = root.attach(BotNodeVertex(v, vIndex))
						node.chance = .5f + (info.knightStrength - info.minScore).toFloat() / 6
						val copy = v.deepCopy()
						v.setType(info.destroyedType!!)
						doEvaluateAll(node, soc, p, b)
						v.copyFrom(copy)
						for (k in info.attackingKnights) {
							b.getVertex(k).activateKnight()
						}
					}
				}
				MoveType.WARSHIP_CARD -> {
					for (rIndex in b.getRoutesIndicesOfType(playerNum, RouteType.SHIP)) {
						val r = b.getRoute(rIndex)
						r.type = RouteType.WARSHIP
						doEvaluateAll(root.attach(BotNodeRoute(r, rIndex)), soc, p, b)
						r.type = RouteType.SHIP
					}
				}
			}


			// TODO: Why did I put this here?
			// break; // break out of for loop
		} // end for
	}

	private fun isKnightNextToRobber(v: Vertex, b: Board): Int {
		for (i in 0 until v.numTiles) {
			if (v.getTile(i) == b.robberTileIndex) return v.getTile(i)
		}
		return -1
	}

	private fun computeChanceForCommodity(soc: SOC, player: PlayerBot): Float {

		// scale the chance but likelyhood of getting the commodity
		// - players with more cards are more likely to have a commodity
		// - players with a higher city level in a certain area will have a higher chance of having that particular commodity
		val likelyhood = FloatArray(SOC.NUM_COMMODITY_TYPES)
		for (p in soc.players) {
			if (p.playerNum != player.playerNum) {
				for (c in CommodityType.values()) {
					likelyhood[c.ordinal] += 0.1f * p.totalCardsLeftInHand * (1 + p.getCityDevelopment(c.area))
				}
			}
		}
		return Utils.sum(likelyhood)
	}

	private fun computeChanceForResource(soc: SOC, b: Board, player: Player): Float {

		// scale the chance but likelyhood of getting the resource
		// - players with more cards are more likely to have a commodity
		// - players who have structures on vertices next to high prob tiles have a better chance of having those resources (?)
		val likelyhood = FloatArray(SOC.NUM_RESOURCE_TYPES)
		for (p in soc.players) {
			if (p.playerNum != player.playerNum) {
				for (r in ResourceType.values()) {
					likelyhood[r.ordinal] += 0.1f * p.totalCardsLeftInHand
				}
				for (vIndex in b.getStructuresForPlayer(p.playerNum)) {
					val v = b.getVertex(vIndex)
					for (t in b.getVertexTiles(v)) {
						if (t.isDistributionTile && t.resource != null) {
							likelyhood[t.resource!!.ordinal] *= (getDiePossibility(t.dieNum, soc.rules) * if (v.isCity) soc.rules.numResourcesForCity else soc.rules.numResourcesForSettlement).toFloat()
						}
					}
				}
			}
		}
		return Utils.sum(likelyhood)
	}

	override fun chooseMove(soc: SOC, moves: Collection<MoveType>): MoveType? {
		if (movesPath != null) {
			return detatchMove<MoveType>()
		}
		assert(moves.size > 0)
		if (moves.size == 1) return moves.iterator().next()
		val root = createNewTree("Choose Move")
		//		evaluateEdges(root, soc, this, b);
//		evaluatePlayer(root, soc, this, b);
//		evaluateTiles(root, soc, this, b);
//		evaluateVertices(root, soc, this, b);
//		evaluateSeafarers(root, soc,this, b);
//		evalu
		//Player t = new PlayerTemp(this);
//		setCardsUsable(CardType.Development, false); // prevent generating development card moves on subsequent calls
		val copy = SOC()
		copy.copyFrom(soc)
		buildChooseMoveTreeR(copy, copy.getPlayerByPlayerNum(playerNum), copy.board, root, moves)
		try {
			val diff = copy.board.diff(soc.board)
			if (!diff.isEmpty()) {
				log.info("Board changes: $diff")
			}

			/*for (Player p : copy.getPlayers()) {
                diff = p.diff(soc.getPlayerByPlayerNum(p.getPlayerNum()));
                if (!diff.isEmpty()) {
                    log.info(p.getName() + " changes: " + diff);
                }
            }*/
		} catch (e: Exception) {
			e.printStackTrace()
		}

//		copyFrom(t);
		/*
		if (DEBUG_ENABLED) {
			try {
				PrintStream out = new PrintStream(new File("/tmp/movestree"));
				root.printTree(out);
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}*/buildOptimalPath(root)
		return detatchMove<MoveType>()
	}

	override fun chooseVertex(soc: SOC, vertexIndices: Collection<Int>, mode: VertexChoice, knightIndexToMove: Int?): Int? {
		if (movesPath != null) {
			return detatchMove<Int>()
		}
		when (mode) {
			VertexChoice.POLITICS_METROPOLIS, VertexChoice.SCIENCE_METROPOLIS, VertexChoice.TRADE_METROPOLIS -> return Utils.randItem(ArrayList(vertexIndices)) // special case where it does not matter which vertex we choose
			else                                                                                             -> {
			}
		}
		val b = soc.board
		val p: Player = this
		val root = createNewTree("Choose Vertex")
		for (vIndex in vertexIndices) {
			val v = b.getVertex(vIndex)
			val save = v.deepCopy()
			var save2: Vertex? = null
			var knightToMove: Vertex? = null
			if (knightIndexToMove != null) {
				knightToMove = b.getVertex(knightIndexToMove)
				save2 = knightToMove.deepCopy()
			}
			val islandNum = b.getIslandAdjacentToVertex(v)
			var discovered = false
			if (islandNum > 0) discovered = b.isIslandDiscovered(p.playerNum, islandNum)
			when (mode) {
				VertexChoice.CITY -> v.setPlayerAndType(p.playerNum, VertexType.CITY)
				VertexChoice.CITY_WALL -> v.setPlayerAndType(p.playerNum, VertexType.WALLED_CITY)
				VertexChoice.KNIGHT_DESERTER -> v.setOpen()
				VertexChoice.KNIGHT_DISPLACED -> {
					assert(knightToMove != null)
					assert(knightToMove!!.isKnight)
					v.setPlayerAndType(p.playerNum, knightToMove.type)
					knightToMove.setOpen()
				}
				VertexChoice.KNIGHT_MOVE_POSITION, VertexChoice.KNIGHT_TO_ACTIVATE, VertexChoice.KNIGHT_TO_MOVE, VertexChoice.KNIGHT_TO_PROMOTE, VertexChoice.NEW_KNIGHT, VertexChoice.OPPONENT_KNIGHT_TO_DISPLACE, VertexChoice.POLITICS_METROPOLIS, VertexChoice.SCIENCE_METROPOLIS, VertexChoice.TRADE_METROPOLIS, VertexChoice.PIRATE_FORTRESS, VertexChoice.OPPONENT_STRUCTURE_TO_ATTACK -> assert(false)
				VertexChoice.SETTLEMENT -> {
					v.setPlayerAndType(p.playerNum, VertexType.SETTLEMENT)
					if (islandNum > 0 && !discovered) {
						b.setIslandDiscovered(p.playerNum, islandNum, true)
					}
				}
			}
			onBoardChanged()
			val node = root.attach(BotNodeVertex(v, vIndex))
			doEvaluateAll(node, soc, p, b)
			v.copyFrom(save)
			if (save2 != null) {
				knightToMove!!.copyFrom(save2)
			}
			if (islandNum > 0 && !discovered) {
				b.setIslandDiscovered(p.playerNum, islandNum, false)
			}
		}
		buildOptimalPath(root)
		return detatchMove<Int>()
	}

	override fun chooseRoute(soc: SOC, routeIndices: Collection<Int>, mode: RouteChoice, shipToMove: Int?): Int? {
		if (movesPath != null) {
			return detatchMove<Int>()
		}
		val root = createNewTree("Choose Route")
		val b = soc.board
		val p: Player = this
		for (rIndex in routeIndices) {
			val r = b.getRoute(rIndex)
			val save = r.deepCopy()
			when (mode) {
				RouteChoice.ROAD -> b.setPlayerForRoute(r, p.playerNum, RouteType.ROAD)
				RouteChoice.SHIP -> b.setPlayerForRoute(r, p.playerNum, if (shipToMove == null) RouteType.SHIP else b.getRoute(shipToMove).type)
				RouteChoice.SHIP_TO_MOVE, RouteChoice.ROUTE_DIPLOMAT, RouteChoice.UPGRADE_SHIP, RouteChoice.OPPONENT_ROAD_TO_ATTACK, RouteChoice.OPPONENT_SHIP_TO_ATTACK -> throw GException("unhandled case $mode")
			}
			val t = checkDiscoveredTerritories(p, r, b)
			onBoardChanged()
			val node = root.attach(BotNodeRoute(r, rIndex))
			doEvaluateAll(node, soc, p, b)
			r.copyFrom(save)
			if (t != null) {
				p.incrementDiscoveredTerritory(-1)
				t.type = TileType.UNDISCOVERED
			}
		}
		buildOptimalPath(root)
		return detatchMove<Int>()
	}

	override fun chooseRouteType(soc: SOC): RouteChoiceType? {
		if (movesPath != null) {
			return detatchMove<RouteChoiceType>()
		}
		val root = createNewTree("Choose Route Type")
		val p: Player = PlayerTemp(this)
		val b = soc.board
		addRouteBuildingMovesR(soc, p, b, root, true, true, 1, false)
		buildOptimalPath(root)
		return detatchMove<RouteChoiceType>()
	}

	override fun chooseTile(soc: SOC, tileIndices: Collection<Int>, mode: TileChoice): Int? {
		if (movesPath != null) {
			return detatchMove<Int>()
		}
		val p: Player = this
		val b = soc.board
		val root = createNewTree("Choose Tile")
		when (mode) {
			TileChoice.INVENTOR, TileChoice.MERCHANT -> assert(false)
			TileChoice.PIRATE, TileChoice.ROBBER -> {
				val pirateSave = b.pirateTileIndex
				val robberSave = b.robberTileIndex
				for (tIndex in tileIndices) {
					val t = b.getTile(tIndex)
					if (t.isWater) {
						b.setPirate(tIndex)
					} else {
						b.setRobber(tIndex)
					}
					val node = root.attach(BotNodeTile(t, tIndex))
					doEvaluateAll(node, soc, p, b)
					b.setPirate(pirateSave)
					b.setRobber(robberSave)
				}
			}
		}
		buildOptimalPath(root)
		return detatchMove<Int>()
	}

	override fun chooseTradeOption(soc: SOC, trades: Collection<Trade>): Trade? {
		if (movesPath != null) {
			return detatchMove<Trade>()
		}
		assert(false)
		return null
	}

	override fun choosePlayer(soc: SOC, playerOptions: Collection<Int>, mode: PlayerChoice): Int? {
		if (movesPath != null) {
			return detatchMove<Int>()
		}
		if (playerOptions.size == 1) return playerOptions.iterator().next()
		val p: Player = this
		val b = soc.board
		val root = createNewTree("Choose Player:$mode")
		for (num in playerOptions) {
			val opponent = soc.getPlayerByPlayerNum(num)
			val node = root.attach(BotNodePlayer(opponent))
			when (mode) {
				PlayerChoice.PLAYER_TO_TAKE_CARD_FROM -> {
				}
				PlayerChoice.PLAYER_TO_SPY_ON -> {
				}
				PlayerChoice.PLAYER_FOR_DESERTION -> {
				}
				PlayerChoice.PLAYER_TO_GIFT_CARD ->                     // choose a lower value opponent
					node.chance = (-1).toFloat()
				PlayerChoice.PLAYER_TO_FORCE_HARBOR_TRADE -> {
				}
			}
			evaluateOpponent(node, soc, opponent, b)
		}
		buildOptimalPath(root)
		return detatchMove<Int>()
	}

	override fun chooseCard(soc: SOC, cards: Collection<Card>, mode: CardChoice): Card? {
		if (movesPath != null && movesPath!!.data is Card) {
			return detatchMove<Card>()
		}
		val root = createNewTree("Choose Card:$mode")
		val mft = merchantFleetTradable
		for (c in cards) {
			val node = root.attach(BotNodeCard(c))
			when (mode) {
				CardChoice.GIVEUP_CARD, CardChoice.EXCHANGE_CARD -> {
					removeCard(c)
					evaluateCards(node, soc, this, soc.board)
					addCard(c)
				}
				CardChoice.FLEET_TRADE -> {
					// eval best 2:1 trades
					merchantFleetTradable = c
					val trades = SOC.computeTrades(this, soc.board)
					for (t in trades) {
						val nn = node.attach(BotNodeTrade(t))
						buildChooseMoveTreeR(soc, this, soc.board, nn, SOC.computeMoves(this, soc.board, soc))
					}
				}
				CardChoice.OPPONENT_CARD, CardChoice.RESOURCE_OR_COMMODITY -> {
					// evaluate buildability
					addCard(c)
					evaluateCards(node, soc, this, soc.board)
					removeCard(c)
				}
				else                                                       -> assert(false)
			}
		}
		buildOptimalPath(root)
		merchantFleetTradable = mft
		return detatchMove<Card>()
	}

	override fun <T : Enum<T>> chooseEnum(soc: SOC, mode: EnumChoice, values: Array<T>): T? {
		if (movesPath != null) {
			return detatchMove()
		}
		when (mode) {
			EnumChoice.DRAW_PROGRESS_CARD -> {
				val chance = IntArray(values.size)
				for (a in DevelopmentArea.values()) {
					chance[a.ordinal] = 1 + (DevelopmentArea.MAX_CITY_IMPROVEMENT - getCityDevelopment(a))
				}
				// choose the card we are least likely to get
				return values[Utils.chooseRandomFromSet(*chance)]
			}
		}
		throw AssertionError("Dont know how to handle this")
	}

	override fun setDice(soc: SOC, die: List<Dice>, num: Int): Boolean {
		val dice = detatchMove<Array<Int>>()!!
		for (i in 0 until num) {
			die[i].setNum(dice[i], true)
		}
		return true
	}

	companion object {
		val log = LoggerFactory.getLogger(PlayerBot::class.java)
		@JvmField
        var DEBUG_ENABLED = false
		val stats: MutableMap<String, Statistics> = HashMap()
		private fun addStat(property: String, value: Double) {
			var stat = stats[property]
			if (stat == null) {
				stat = Statistics()
				stats[property] = stat
			}
			stat.numOccurances++
			stat.accumulatedValue += value
		}

		@JvmStatic
        fun dumpStats() {
			log.info("STAT: %-25s %5s %11s %11s", "KEY", "CNT", "TOTAL", "AVE")
			for ((key, value) in stats) {
				val ave = value.accumulatedValue / value.numOccurances
				log.info("STAT: %-25s %5d %11.3f %11.3f", key, value.numOccurances, value.accumulatedValue, ave)
			}
		}

		@JvmStatic
        fun clearStats() {
			stats.clear()
		}

		val diePossibility_usegetter // TODO: The die distribution is different (equal) for EventCards
			: DoubleArray

		fun getDiePossibility(dieNum: Int, rules: Rules): Double {
			return if (rules.isEnableEventCards) 0.1 else diePossibility_usegetter[dieNum]
		}

		private fun doEvaluateVertices(node: BotNode, soc: SOC, p: Player, b: Board) {

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
			val playerNum = p.playerNum
			val rules = soc.rules
			var pirateHealth = 0
			val tilesAdjacent = HashSet<Int>()
			val tilesProtected = HashSet<Int>()
			val resourceProb = DoubleArray(SOC.NUM_RESOURCE_TYPES)
			val resourcePorts = IntArray(SOC.NUM_RESOURCE_TYPES)
			var numMultiPorts = 0f
			b.getMerchantTile()?.takeIf { b.merchantPlayer == playerNum }?.let {
				resourcePorts[it.resource!!.ordinal]++
			}
			var knightValue = 0f
			var structureValue = 0f
			for (vIndex in 0 until b.numAvailableVerts) {
				val v = b.getVertex(vIndex)
				var scale = 0f
				if (v.player != playerNum) continue
				if (b.isVertexAdjacentToPirateRoute(vIndex)) {
					scale *= -1f
				}
				pirateHealth += v.pirateHealth
				when (v.type) {
					VertexType.OPEN -> {
					}
					VertexType.SETTLEMENT -> structureValue += 1f
					VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE -> structureValue += 5f
					VertexType.WALLED_CITY -> structureValue += 3f
					VertexType.CITY -> structureValue += 2f
					VertexType.MIGHTY_KNIGHT_ACTIVE, VertexType.BASIC_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_ACTIVE -> {
						for (tIndex in b.getTileIndicesAdjacentToVertex(v)) {
							tilesProtected.add(tIndex)
						}
						knightValue += (v.type!!.knightLevel * 2).toFloat()
					}
					VertexType.MIGHTY_KNIGHT_INACTIVE, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_INACTIVE -> {
						for (tIndex in b.getTileIndicesAdjacentToVertex(v)) {
							tilesProtected.add(tIndex)
						}
						knightValue += v.type!!.knightLevel.toFloat()
					}
					VertexType.OPEN_SETTLEMENT, VertexType.PIRATE_FORTRESS -> pirateHealth += v.pirateHealth
				}
				if (v.isStructure) {
					for (cell in b.getTilesAdjacentToVertex(v)) {
						if (cell === b.getRobberTile()) continue
						when (cell.type) {
							TileType.GOLD -> {
								var i = 0
								while (i < resourceProb.size) {
									resourceProb[i] += getDiePossibility(cell.dieNum, rules) * scale / 5
									i++
								}
							}
							TileType.FIELDS, TileType.FOREST, TileType.HILLS, TileType.MOUNTAINS, TileType.PASTURE -> resourceProb[cell.resource!!.ordinal] += getDiePossibility(cell.dieNum, rules) * scale
							TileType.PORT_BRICK, TileType.PORT_ORE, TileType.PORT_SHEEP, TileType.PORT_WHEAT, TileType.PORT_WOOD -> resourcePorts[cell.resource!!.ordinal]++
							TileType.PORT_MULTI -> numMultiPorts++
							TileType.DESERT -> {
							}
							TileType.NONE -> {
							}
							TileType.RANDOM_PORT -> {
							}
							TileType.RANDOM_PORT_OR_WATER -> {
							}
							TileType.RANDOM_RESOURCE -> {
							}
							TileType.RANDOM_RESOURCE_OR_DESERT -> {
							}
							TileType.UNDISCOVERED -> {
							}
							TileType.WATER -> {
							}
						}
					}
					for (tIndex in b.getTileIndicesAdjacentToVertex(v)) {
						tilesAdjacent.add(tIndex)
					}
				}
			}
			val resourceProbValue = 4 * CMath.sum(resourceProb)
			//resourceProbValue *= 2;
			var portValue = 0.0
			for (i in 0 until SOC.NUM_RESOURCE_TYPES) {
				if (resourcePorts[i] > 0) portValue += resourceProb[i] * 2 else if (numMultiPorts > 0) portValue += resourceProb[i]
			}
			tilesAdjacent.retainAll(tilesProtected)
			var covered = 0f
			for (tIndex in tilesAdjacent) {
				// these are the uncovered tiles
				covered += getDiePossibility(b.getTile(tIndex).dieNum, rules).toFloat()
			}
			val ave = CMath.sum(resourceProb) / resourceProb.size
			val stdDev = 0.1 * Math.abs(CMath.stdDev(resourceProb, ave))
			val resourceDistribution = ave - stdDev
			node.addValue("Resource distribution", resourceDistribution)
			node.addValue("Port value", portValue)
			node.addValue("Resource Prob", resourceProbValue)
			node.addValue("Structures", structureValue.toDouble())
			node.addValue("tiles protected", covered.toDouble())
			node.addValue("knights", (0.1f * knightValue).toDouble())
			if (pirateHealth > 0) {
				node.addValue("pirateHealth", (10f / pirateHealth).toDouble())
			}
			if (soc.rules.isEnableCitiesAndKnightsExpansion) {
				var barbResist = 0f
				val barStrength = SOC.computeBarbarianStrength(soc, b)
				// see if we would win or lose a barbarian attack and scale by the distance away the brs are
				var sum = 0
				var max = 0
				var min = 1000
				val knightLevel = IntArray(soc.numPlayers + 1)
				for (i in 1..soc.numPlayers) {
					knightLevel[i] = b.getKnightLevelForPlayer(i, true, false)
					val s = knightLevel[i]
					if (s > max) {
						max = s
					}
					if (s < min) {
						min = s
					}
					sum += s
				}
				if (sum < barStrength) {
					if (min == knightLevel[p.playerNum]) {
						// this is bad. This means we will get creamed if barbrians attack
						barbResist = -1f
					}
				} else if (sum > barStrength) {
					if (max == knightLevel[p.playerNum]) {
						// this is good this means we will get pts
						barbResist = 1f
					}
				}
				node.addValue("barbResist", (barbResist / Math.max(soc.barbarianDistance, 1)).toDouble()) // scale with distance barbs are form attack
			}
		}

		private fun doEvaluateDifferentials(node: BotNode, soc: SOC, p: Player, b: Board) {
			var myRoutesValue = 0f
			var theirRoutesValue = 0f
			for (r in b.getRoutesOfType(p.playerNum, RouteType.ROAD, RouteType.SHIP, RouteType.WARSHIP)) {
				when (r.type) {
					RouteType.ROAD -> if (r.player == p.playerNum) myRoutesValue += 1f else theirRoutesValue += 1f
					RouteType.SHIP -> if (r.player == p.playerNum) myRoutesValue += 2f else theirRoutesValue += 2f
					RouteType.WARSHIP -> if (r.player == p.playerNum) myRoutesValue += 2f else theirRoutesValue += 2f
					else              -> assert(false)
				}
			}
			var myStructuresValue = 0f
			var theirStructuresValue = 0f
			for (v in b.getVertsOfType(p.playerNum, VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.SETTLEMENT, VertexType.WALLED_CITY, VertexType.CITY)) {
				when (v.type) {
					VertexType.CITY -> if (v.player == p.playerNum) myStructuresValue += 2f else theirStructuresValue += 2f
					VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE -> if (v.player == p.playerNum) myStructuresValue += 4f else theirStructuresValue += 4f
					VertexType.SETTLEMENT -> if (v.player == p.playerNum) myStructuresValue += 1f else theirStructuresValue += 1f
					VertexType.WALLED_CITY -> if (v.player == p.playerNum) myStructuresValue += 3f else theirStructuresValue += 3f
					else                                                                                       -> assert(false)
				}
			}
			if (theirRoutesValue > 0) node.addValue("routesDifferential", (myRoutesValue / theirRoutesValue).toDouble())
			if (theirStructuresValue > 0) node.addValue("structuresDifferential", (myStructuresValue / theirStructuresValue).toDouble())
		}

		private fun doEvaluateEdges(node: BotNode, soc: SOC, p: Player, b: Board) {
/*
		float longestRoadValue = 0;
		float len = b.computeMaxRouteLengthForPlayer(p.getPlayerNum(), soc.getRules().isEnableRoadBlock());

		Player cur = soc.getLongestRoadPlayer();
		if (cur != null && (cur.getPlayerNum() == p.getPlayerNum() || cur.getRoadLength() < len)) {
			longestRoadValue = 1;
		}
		node.addValue("longestRoad", longestRoadValue);
		if (len > 0)
			node.addValue("roadLength", 3f * Math.log(len)); // have value taper off at the length grows.  Factor of 3 chosen b/c: 3ln(5)==5
*/

			// Route length is a factor in that we should strive for longest road, so unless we have length shorter than the current longest or
			// we are not at the minimum.
			var minRoadLen = soc.rules.minLongestLoadLen
			val curLongest = soc.longestRoadPlayer
			if (curLongest != null && curLongest.playerNum != p.playerNum) {
				minRoadLen = curLongest.roadLength + 1
			}

			// road length is inverse of how far we are form the min
			if (p.roadLength >= minRoadLen) {
				node.addValue("roadLength", 1.0)
			} else {
				node.addValue("roadLength", (1f / (minRoadLen - p.roadLength)).toDouble())
			}
			var routeTileResourceProb = 0.0
			var routeExpabability = 0.0
			val tiles: MutableSet<Int> = HashSet()
			val routes: MutableSet<Int> = HashSet()
			var routeValue = 0
			for (r in b.getRoutesForPlayer(p.playerNum)) {
				val v0 = b.getVertex(r.from)
				val v1 = b.getVertex(r.to)
				when (r.type) {
					RouteType.OPEN -> {
					}
					RouteType.ROAD -> routeValue++
					RouteType.DAMAGED_ROAD -> routeValue -= 2
					RouteType.SHIP -> routeValue += 1
					RouteType.WARSHIP -> routeValue += 2
				}
				if (!v0.type!!.isStructure && !v1.type!!.isStructure) {
					tiles.addAll(v0.tiles)
					tiles.addAll(v1.tiles)
				}
				routes.addAll(b.getVertexRouteIndices(r.from))
				routes.addAll(b.getVertexRouteIndices(r.to))
			}
			for (tIndex in tiles) {
				val t = b.getTile(tIndex)
				if (t.isDistributionTile) {
					routeTileResourceProb += getDiePossibility(t.dieNum, soc.rules)
				}
			}
			for (rIndex in routes) {
				val r = b.getRoute(rIndex)
				if (r.player == 0 && !r.isLocked && !r.isClosed) {
					routeExpabability += 1.0
				}
			}
			node.addValue("routeTileResourceProb", routeTileResourceProb)
			node.addValue("routeExpandability", 0.1 * routeExpabability)
			//node.addValue("routeValue", 1f * routeValue);
		}

		private fun doEvaluateDistances(node: BotNode, soc: SOC, p: Player, b: Board) {
			val d = b.computeDistances(soc.rules, p.playerNum)

			// visit the vertices and assign a value to each.
			val vertexValue = DoubleArray(b.numAvailableVerts)
			val structures: MutableList<Int> = ArrayList()
			for (vIndex in 0 until b.numAvailableVerts) {
				val v = b.getVertex(vIndex)
				if (v.player == p.playerNum) {
					if (v.isStructure) structures.add(vIndex)
					continue
				}

				// things to consider:
				// vertexType
				// tiles adjacent (undiscovered, resource distribution)
				// road blocking? (This might already be covered)
				// attacking (if enabled, choose structures to attack)


				// Ultimately we want to be as close as possible to the most important things and reasonably close
				val tiles: Iterable<Tile> = b.getVertexTiles(v)
				if (v.canPlaceStructure() && v.player == 0 && v.type === VertexType.OPEN) {
					var open = true
					for (i in 0 until v.numAdjacentVerts) {
						val v2Index = v.adjacentVerts[i]
						val v2 = b.getVertex(v2Index)
						if (v2.isStructure) {
							open = false
							break
						}
					}
					if (open) {
						for (t in tiles) {
							if (t.isDistributionTile) vertexValue[vIndex] += getDiePossibility(t.dieNum, soc.rules)
						}
					}
				}
				for (t in tiles) {
					when (t.type) {
						TileType.WATER, TileType.DESERT -> {
						}
						TileType.FIELDS, TileType.FOREST -> {
						}
						TileType.GOLD -> vertexValue[vIndex] = vertexValue[vIndex] + 1
						TileType.HILLS -> {
						}
						TileType.MOUNTAINS -> {
						}
						TileType.NONE -> {
						}
						TileType.PASTURE -> {
						}
						TileType.PORT_MULTI -> vertexValue[vIndex] = vertexValue[vIndex] + 0.2f
						TileType.PORT_BRICK, TileType.PORT_ORE, TileType.PORT_SHEEP, TileType.PORT_WHEAT, TileType.PORT_WOOD -> vertexValue[vIndex] = vertexValue[vIndex] +0.5f
						TileType.RANDOM_PORT -> {
						}
						TileType.RANDOM_PORT_OR_WATER -> {
						}
						TileType.RANDOM_RESOURCE -> {
						}
						TileType.RANDOM_RESOURCE_OR_DESERT -> {
						}
						TileType.UNDISCOVERED -> vertexValue[vIndex] = vertexValue[vIndex] + 1.5f
					}
				}
				when (v.type) {
					VertexType.BASIC_KNIGHT_ACTIVE -> {
					}
					VertexType.BASIC_KNIGHT_INACTIVE -> {
					}
					VertexType.CITY, VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE, VertexType.MIGHTY_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE, VertexType.SETTLEMENT, VertexType.WALLED_CITY -> {
					}
					VertexType.OPEN_SETTLEMENT, VertexType.OPEN -> {
					}
					VertexType.PIRATE_FORTRESS -> vertexValue[vIndex] = 2.0 // heavy weight this so we try to get to it?
					VertexType.STRONG_KNIGHT_ACTIVE -> {
					}
					VertexType.STRONG_KNIGHT_INACTIVE -> {
					}
					else                                                                                                                                                                                                                           -> {
					}
				}
			}
			var distValue = 0.0
			val num = 0
			for (i in vertexValue.indices) {
				if (vertexValue[i].absoluteValue < 0.0000001) continue
				var minDist = 100
				for (vIndex in structures) {
					val dist = d.getDist(vIndex, i)
					minDist = Math.min(minDist, dist)
				}
				assert(minDist >= 0)
				vertexValue[i] = vertexValue[i] / 1 + minDist
			}
			if (num > 0) {
				distValue /= num.toDouble()
			}
			node.addValue("distValue", distValue)
		}

		private fun doEvaluateSeafarers(node: BotNode, soc: SOC, p: Player, b: Board) {
			val d = b.computeDistances(soc.rules, p.playerNum)
			val pirateFortresses: MutableList<Int> = ArrayList()
			val undiscoveredVerts: MutableSet<Int> = HashSet()
			val playerStructures: MutableList<Int> = ArrayList()
			val islandShorelines: MutableSet<Int> = HashSet()
			var numUndiscoverdIslands = 0
			for (i in 0 until b.numIslands) {
				if (!b.isIslandDiscovered(p.playerNum, i + 1)) {
					numUndiscoverdIslands++
				}
			}
			for (vIndex in 0 until b.numAvailableVerts) {
				val v = b.getVertex(vIndex)
				if (v.type === VertexType.PIRATE_FORTRESS) {
					pirateFortresses.add(vIndex)
				}
				for (t in b.getVertexTiles(vIndex)) {
					if (t.type === TileType.UNDISCOVERED) {
						undiscoveredVerts.add(vIndex)
					}
					if (numUndiscoverdIslands > 0 && t.islandNum > 0 && !b.isIslandDiscovered(p.playerNum, t.islandNum)) {
						for (vv in t.getAdjVerts()) {
							val vert = b.getVertex(vv)
							if (vert.isAdjacentToWater) {
								islandShorelines.add(vv)
							}
						}
					}
				}
				if (v.player == p.playerNum && v.isStructure) {
					playerStructures.add(vIndex)
				}
			}
			var minDistToFortress: Int = IDistances.DISTANCE_INFINITY.toInt()
			var minDistToUndiscovered: Int = IDistances.DISTANCE_INFINITY.toInt()
			var minDistToIsland: Int = IDistances.DISTANCE_INFINITY.toInt()
			var aveDistToIsland = 0f
			for (sIndex in playerStructures) {
				for (pIndex in pirateFortresses) {
					minDistToFortress = Math.min(minDistToFortress, d.getDist(sIndex, pIndex))
				}
				for (uIndex in undiscoveredVerts) {
					minDistToUndiscovered = Math.min(minDistToUndiscovered, d.getDist(sIndex, uIndex))
				}
				if (numUndiscoverdIslands == 0) continue
				for (iIndex in islandShorelines) {
					val dist = d.getDist(sIndex, iIndex)
					minDistToIsland = Math.min(minDistToIsland, dist)
					aveDistToIsland += dist.toFloat()
				}
				aveDistToIsland /= numUndiscoverdIslands.toFloat()
			}
			node.addValue("minDistToFortress", (1.0f / (minDistToFortress + 1)).toDouble())
			node.addValue("minDistToUndiscov", (Math.max(1, soc.rules.minMostDiscoveredTerritories) * 1.0f / (1 + minDistToUndiscovered)).toDouble())
			node.addValue("minDistToIsland", (soc.rules.pointsIslandDiscovery * 1.0f / (1 + minDistToIsland)).toDouble())
			node.addValue("aveDistToIsland", (soc.rules.pointsIslandDiscovery * 1.0f / (1 + aveDistToIsland)).toDouble())
			node.addValue("warShipValue", (0.1f * b.getRoutesOfType(p.playerNum, RouteType.WARSHIP).size).toDouble())
		}

		private fun doEvaluateTiles(node: BotNode, soc: SOC, p: Player, b: Board) {

			// Robber
			b.getRobberTile()?.let { t ->
				var robberValue = 0f
				for (v in b.getTileVertices(t)) {
					when (v.type) {
						VertexType.BASIC_KNIGHT_ACTIVE, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_INACTIVE -> {
						}
						VertexType.CITY, VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE, VertexType.WALLED_CITY -> if (v.player == p.playerNum) {
							robberValue -= (soc.rules.numResourcesForCity * 2 * getDiePossibility(t.dieNum, soc.rules)).toFloat()
						} else {
							robberValue += (soc.rules.numResourcesForCity * getDiePossibility(t.dieNum, soc.rules)).toFloat()
						}
						VertexType.SETTLEMENT -> if (v.player == p.playerNum) {
							robberValue -= (soc.rules.numResourcesForSettlement * 2 * getDiePossibility(t.dieNum, soc.rules)).toFloat()
						} else {
							robberValue += (soc.rules.numResourcesForSettlement * getDiePossibility(t.dieNum, soc.rules)).toFloat()
						}
						VertexType.OPEN, VertexType.OPEN_SETTLEMENT, VertexType.PIRATE_FORTRESS -> {
						}
					}
				}
				node.addValue("robberTile", robberValue.toDouble())
			}

			// Pirate
			b.getRobberTile()?.let { t ->
				var scale = 1f
				when (t.type) {
					TileType.DESERT -> {
					}
					TileType.FIELDS, TileType.FOREST, TileType.HILLS, TileType.MOUNTAINS, TileType.PASTURE -> {
					}
					TileType.PORT_MULTI, TileType.PORT_BRICK, TileType.PORT_ORE, TileType.PORT_SHEEP, TileType.PORT_WHEAT, TileType.PORT_WOOD -> scale = 2f
					TileType.GOLD -> {
					}
					TileType.NONE -> {
					}
					TileType.RANDOM_PORT -> {
					}
					TileType.RANDOM_PORT_OR_WATER -> {
					}
					TileType.RANDOM_RESOURCE -> {
					}
					TileType.RANDOM_RESOURCE_OR_DESERT -> {
					}
					TileType.UNDISCOVERED -> {
					}
					TileType.WATER -> {
					}
					else                                                                                                                      -> {
					}
				}
				var pirateValue = 0f
				for (r in b.getTileRoutes(t)) {
					when (r.type) {
						RouteType.ROAD, RouteType.DAMAGED_ROAD, RouteType.OPEN -> {
						}
						RouteType.SHIP, RouteType.WARSHIP -> if (r.player == p.playerNum) {
							pirateValue -= 3f
						} else {
							pirateValue += 0.5f
						}
					}
				}
				node.addValue("pirateTile", (scale * pirateValue).toDouble())
			}

			// Merchant
			// Inventor
			// should do this when evaluating vertices
		}

		private fun doEvaluatePlayer(node: BotNode, soc: SOC, p: Player, b: Board) {

			// Special Victory Cards
			var value = 0f
			for (c in SpecialVictoryType.values()) {
				value += (c.getData() * p.getCardCount(c)).toFloat()
			}
			node.addValue("specialVictoryCards", value.toDouble())

			// number of progress cards (less is better)
			/*
		value = 0;
		int count = p.getCardCount(CardType.Progress);
		int max = soc.getRules().getMaxProgressCards();
		if (count < max)
			value = 0.1f * count;
		node.addValue("progressCardCount", value);
		*/
			// total cards near the max but not over
			// we want the highest value to be half of the max
			//node.addValue("cardsInHand", CMath.normalDistribution(p.getTotalCardsLeftInHand(), soc.getRules().getMaxSafeCards()));
			var cardsValue = 0f
			val maxCards = soc.rules.getMaxSafeCardsForPlayer(p.playerNum, b)
			val numCards = p.totalCardsLeftInHand
			for (c in p.getCards()) {
				when (c.cardType) {
					CardType.Resource -> cardsValue++
					CardType.Commodity -> cardsValue += 2f
					CardType.Development -> cardsValue += 3f
					CardType.Progress -> cardsValue += 3f
					CardType.SpecialVictory -> cardsValue += 4f
					CardType.Event, CardType.BarbarianAttackDevelopment -> assert(false)
				}
			}

			//if (numCards > maxCards) {
			//    cardsValue -= 2*(numCards-maxCards); // this causes bot to trade too quickly
			//}
			node.addValue("maxCards", 0.1 * maxCards)
			//node.addValue("cardsValue", 0.1 * cardsValue);

			// city development
			var sum = 0f
			for (a in DevelopmentArea.values()) sum += p.getCityDevelopment(a).toFloat()
			node.addValue("cityDevelopment", sum.toDouble())
			/*
		float buildableValue = 0;
		for (BuildableType t : BuildableType.values()) {
			if (!p.canBuild(t) && t.isAvailable(soc)) {
				buildableValue += (t.ordinal()+1);
			}
		}
		node.addValue("buildability", buildableValue);
	*/
			// this seems stoopid
			if (soc.rules.isEnableCitiesAndKnightsExpansion) {
				var developmentValue = 0f
				for (a in DevelopmentArea.values()) {
					val num = p.getCardCount(a.commodity)
					val cur = p.getCityDevelopment(a)
					if (cur < DevelopmentArea.MAX_CITY_IMPROVEMENT && num > cur) {
						developmentValue += (cur + 1).toFloat()
					}
				}
				node.addValue("cityDevelValue", developmentValue.toDouble())
			}
			node.addValue("points", SOC.computePointsForPlayer(p, b, soc).toDouble())
			node.addValue("discoveredTiles", (0.1f * p.numDiscoveredTerritories).toDouble())
			node.addValue("armySize", (0.1f * p.getArmySize(b)).toDouble())
			b.getPirateTile()?.let {
				node.addValue("pirateDefence", (0.1f * b.getNumRoutesOfType(p.playerNum, RouteType.WARSHIP)).toDouble())
			}
		}

		private fun evaluateDice(node: BotNode, die1: Int, die2: Int, soc: SOC, p: Player, b: Board) {
			// which die combine gives us the best results?

			// if CAK enabled, then die2 should be a value that gives us the best opportunity for a progress card
			if (soc.rules.isEnableCitiesAndKnightsExpansion) {
				if (p.getCardCount(CardType.Progress) < soc.rules.maxProgressCards) {
					var best: DevelopmentArea? = null
					var bestCount = 0
					for (a in DevelopmentArea.values()) {
						val devel = p.getCityDevelopment(a)
						if (best == null || devel > bestCount) {
							best = a
							bestCount = devel
						}
					}
					node.addValue("progress dice", bestCount.toDouble())
				}
			}
			var dieValue = 0f
			for (t in b.getTiles()) {
				if (t.dieNum == die1 + die2) {
					for (v in b.getTileVertices(t)) {
						if (v.isStructure) {
							val value = if (v.isCity) soc.rules.numResourcesForCity.toFloat() else soc.rules.numResourcesForSettlement.toFloat()
							if (v.player == p.playerNum) {
								dieValue += value
							} else if (v.player > 0) {
								dieValue -= value // discount if this die helps an opponent
							}
						}
					}
				}
			}
			node.addValue("dieValue", dieValue.toDouble())
		}

		private fun evaluateCards(node: BotNode, soc: SOC, p: Player, board: Board) {
			var buildableValue = 0f
			for (b in BuildableType.values()) {
				if (p.canBuild(b)) buildableValue += b.ordinal.toFloat()
			}
			var develValue = 0f
			for (a in DevelopmentArea.values()) {
				if (p.getCityDevelopment(a) < p.getCardCount(a.commodity)) {
					develValue += (p.getCityDevelopment(a) + 1).toFloat()
				}
			}
			node.addValue("cards buildability", (0.02f * buildableValue).toDouble())
			node.addValue("cards city development", (02f * develValue).toDouble())
			var cardsValue = 0f
			for (c in p.getCards()) {
				cardsValue += c.cardType.ordinal.toFloat()
			}
			node.addValue("cardsValue", (0.02f * cardsValue).toDouble())
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
		private fun evaluateOpponent(node: BotNode, soc: SOC, opponent: Player, b: Board) {
			val numProgress = opponent.getCardCount(CardType.Progress)
			val numPoints = opponent.points
			val numCards = opponent.totalCardsLeftInHand
			val knightStrength = b.getKnightLevelForPlayer(opponent.playerNum, true, true)
			node.addValue("opponent progress", numProgress.toDouble())
			node.addValue("opponent points", numPoints.toDouble())
			node.addValue("opponent cards", numCards.toDouble())
			node.addValue("opponent knights", knightStrength.toDouble())
		}

		init {
			// rate the possible number on the board
			// there are 21 possible rolls with 2, 6 sided die
			val p1 = (1.0f / 21.0f).toDouble()
			val p2 = (2.0f / 21.0f).toDouble()
			val p3 = (3.0f / 21.0f).toDouble()
			val p4 = (4.0f / 21.0f).toDouble()
			val p5 = (5.0f / 21.0f).toDouble()
			diePossibility_usegetter = doubleArrayOf(0.0, 0.0,
				p1, p2, p3, p4, p5, 0.0, p5, p4, p3, p2, p1
			)
		}
	}

	private fun doEvaluateAll(node: BotNode, soc: SOC, p: Player, b: Board) {
		doEvaluateEdges(node, soc, p, b)
		doEvaluatePlayer(node, soc, p, b)
		doEvaluateSeafarers(node, soc, p, b)
		//doEvaluateDistances(node, soc, p, b);
		doEvaluateTiles(node, soc, p, b)
		doEvaluateVertices(node, soc, p, b)
		//doEvaluateDifferentials(node, soc, p, b);
		//node.addValue("randomness", Utils.randFloatX(1));
		numLeafs++
	}

	/**
	 * Called whenever the board is updated to trigger a redraw if desired
	 */
	protected open fun onBoardChanged() {}
}