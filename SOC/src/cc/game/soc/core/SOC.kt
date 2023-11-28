package cc.game.soc.core

import cc.game.soc.core.DiceEvent.Companion.fromDieNum
import cc.game.soc.core.Player.*
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.math.CMath
import cc.lib.utils.Reflector
import cc.lib.utils.appendDelimited
import cc.lib.utils.takeIfNotEmpty
import java.io.File
import java.util.*

/**
 * SOC Core business logic
 *
 * Add player instances then call runGame() until isGameOver()
 * Make sure to initialize the board otherwise a compltly random one will get generated
 *
 * @author Chris Caron
 */
open class SOC() : Reflector<SOC>(), StringResource {
	interface UndoAction {
		fun undo()
	}

	companion object {
		protected val log = LoggerFactory.getLogger(SOC::class.java)
		var MAX_PLAYERS = 6
		val NUM_RESOURCE_TYPES = ResourceType.values().size
		val NUM_DEVELOPMENT_CARD_TYPES = DevelopmentCardType.values().size
		val NUM_DEVELOPMENT_AREA_TYPES = DevelopmentArea.values().size // DONT REMOVE. I know this is redundant, but it prevents a null ptr in DevelopementArea.commodity
		val NUM_COMMODITY_TYPES = CommodityType.values().size
		val NUM_DEVELOPMENT_AREAS = DevelopmentArea.values().size

		/**
		 * Compute the point the player should have based on the board and relevant SOC values.
		 * The player's point field is not changed.
		 *
		 * @param player
		 * @return
		 */
		fun computePointsForPlayer(player: Player, board: Board, soc: SOC): Int {
			var numPts = player.specialVictoryPoints
			// count cities and settlements
			val islands = BooleanArray(board.numIslands + 1)
			for (i in 0 until board.numAvailableVerts) {
				val vertex = board.getVertex(i)
				if (vertex.player == player.playerNum) {
					numPts += vertex.getPointsValue(soc.rules)
					for (t in board.getTilesAdjacentToVertex(vertex)) {
						if (t.islandNum > 0) {
							islands[t.islandNum] = true
						}
					}
				}
			}
			for (b in islands) {
				if (b) {
					numPts += soc.rules.pointsIslandDiscovery
				}
			}
			if (board.getNumVertsOfType(0, VertexType.PIRATE_FORTRESS) == 0 ||
				player.getCardCount(SpecialVictoryType.CapturePirateFortress) > 0) {
				val victoryPts = player.getUsableCardCount(DevelopmentCardType.Victory)
				if (numPts + victoryPts >= soc.rules.pointsForWinGame) {
					numPts += victoryPts
				}
			}
			return numPts
		}

		fun createCards(values: Array<ICardType<*>>, status: CardStatus): List<Card> {
			val cards: MutableList<Card> = ArrayList()
			for (c in values) {
				cards.add(Card(c, status))
			}
			return cards
		}

		fun computeHarborTradePlayers(trader: Player, soc: SOC): List<Int> {
			val players: MutableList<Int> = ArrayList()
			if (trader.getCardCount(CardType.Resource) == 0) return players
			for (pNum in players) {
				val p = soc.getPlayerByPlayerNum(pNum)
				val numCommodity = p.getCardCount(CardType.Commodity)
				if (numCommodity > 0) {
					players.add(p.playerNum)
				}
			}
			return players
		}

		/**
		 * Find all knights that are NOT owned by playerNum but ARE on a route of playerNum
		 *
		 * @param playerNum
		 * @param b
		 * @return
		 */
		fun computeIntrigueKnightsVertexIndices(playerNum: Int, b: Board): List<Int> {
			val verts: MutableList<Int> = ArrayList()
			for (vIndex in 0 until b.numAvailableVerts) {
				val v = b.getVertex(vIndex)
				if (v.isKnight && v.player != playerNum && b.isVertexAdjacentToPlayerRoute(vIndex, playerNum)) {
					verts.add(vIndex)
				}
			}
			return verts
		}

		fun computeInventorTileIndices(b: Board, soc: SOC): List<Int> {
			var values: IntArray? = null
			values = if (soc.rules.isUnlimitedInventorTiles) {
				intArrayOf(2, 3, 4, 5, 6, 8, 9, 10, 11, 12)
			} else {
				intArrayOf(3, 4, 5, 9, 10, 11)
			}
			val tiles: MutableList<Int> = ArrayList()
			for (tIndex in 0 until b.numTiles) {
				val t = b.getTile(tIndex)
				when (t.type) {
					TileType.FIELDS, TileType.FOREST, TileType.GOLD, TileType.HILLS, TileType.MOUNTAINS, TileType.PASTURE -> if (Arrays.binarySearch(values, t.dieNum) >= 0) {
						tiles.add(tIndex)
					}
					else -> {
					}
				}
			}
			return tiles
		}

		fun computeDiceToWinAttackShip(b: Board, attackerNum: Int, defenderNum: Int): Int {
			// if roll is 1,2,3 then attacker lost and loses their ship
			// if roll is 4,5,6 then attacker won and the attacked ship becomes property of attacker
			// the midpoint is shifted by the difference in the number of cities of each player
			val attackingCities = b.getCitiesForPlayer(attackerNum).size
			val defenderCities = b.getCitiesForPlayer(defenderNum).size
			return 4 - (attackingCities - defenderCities)
		}

		/**
		 * Returns an attack info structure.  The attackingKnights will be all knights involved in the attack. They will have been
		 * active prior to this call and inactive after leaving.  Caller will need to re-activate knight s if they wish to undo.
		 *
		 * @param vIndex
		 * @param soc
		 * @param board
		 * @param attackerPlayerNum
		 * @return
		 */
		fun computeStructureAttack(vIndex: Int, soc: SOC, board: Board, attackerPlayerNum: Int): AttackInfo<VertexType> {
			val v = board.getVertex(vIndex)
			val attackingKnights = mutableListOf<Int>()
			var knightStrength = 0
			var minScore = 0
			var destroyedType = VertexType.OPEN
			for (i in 0 until v.numAdjacentVerts) {
				val vIndex2 = v.adjacentVerts[i]
				val v2 = board.getVertex(vIndex2)
				run loop@ {
					if (v2.isActiveKnight && v2.player == attackerPlayerNum) {
						if (!soc.rules.isEnableKnightExtendedMoves) {
							board.getRoute(vIndex, vIndex2)?.let { r ->
								if (r.type !== RouteType.ROAD || r.player != attackerPlayerNum)
									return@loop
							}
						}
						attackingKnights.add(vIndex2)
						knightStrength += v2.type.knightLevel
						v2.deactivateKnight()
					}
				}
			}
			when (v.type) {
				VertexType.CITY -> {
					minScore = soc.rules.knightScoreToDestroyCity
					destroyedType = VertexType.SETTLEMENT
				}
				VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE -> {
					minScore = soc.rules.knightScoreToDestroyMetropolis
					destroyedType = VertexType.CITY
				}
				VertexType.SETTLEMENT -> {
					minScore = soc.rules.knightScoreToDestroySettlement
					destroyedType = VertexType.OPEN
				}
				VertexType.WALLED_CITY -> {
					minScore = soc.rules.knightScoreToDestroyWalledCity
					destroyedType = VertexType.CITY
				}
				else -> assert(false) // unhandled case
			}
			return AttackInfo(attackingKnights, knightStrength, minScore, destroyedType)
		}

		fun computeAttackRoad(routeIndex: Int, soc: SOC, b: Board, attackerPlayerNum: Int): AttackInfo<RouteType> {
			val r = b.getRoute(routeIndex)
			val v0 = b.getVertex(r.from)
			val v1 = b.getVertex(r.to)
			var knightStrength = 0
			val attackingKnights = mutableListOf<Int>()
			if (v0.isActiveKnight && v0.player == attackerPlayerNum) {
				knightStrength += v0.type.knightLevel
				v0.deactivateKnight()
				attackingKnights.add(r.from)
			}
			if (v1.isActiveKnight && v1.player == attackerPlayerNum) {
				knightStrength += v1.type.knightLevel
				v1.deactivateKnight()
				attackingKnights.add(r.to)
			}
			return AttackInfo(attackingKnights, knightStrength, soc.rules.knightScoreToDestroyRoad,
				when (r.type) {
					RouteType.ROAD -> RouteType.DAMAGED_ROAD
					else -> RouteType.OPEN
				}
			)
		}

		/**
		 * @param soc
		 * @param playerNum
		 * @param b
		 * @return
		 */
		fun computeAttackableRoads(soc: SOC, playerNum: Int, b: Board): List<Int> {
			if (soc.rules.knightScoreToDestroyRoad <= 0) return emptyList()
			val attackableroads: MutableList<Int> = ArrayList()
			for (rIndex in b.getRoutesIndicesOfType(0, RouteType.ROAD, RouteType.DAMAGED_ROAD)) {
				val r = b.getRoute(rIndex)
				if (r.player == playerNum) continue
				var v = b.getVertex(r.from)
				if (v.isActiveKnight && v.player == playerNum) {
					attackableroads.add(rIndex)
					continue
				}
				v = b.getVertex(r.to)
				if (v.isActiveKnight && v.player == playerNum) {
					attackableroads.add(rIndex)
				}
			}
			return attackableroads
		}

		/**
		 * @param soc
		 * @param playerNum
		 * @param b
		 * @return
		 */
		fun computeAttackableShips(soc: SOC, playerNum: Int, b: Board): List<Int> {
			val ships = HashSet<Int>()
			for (r in b.getRoutesOfType(playerNum, RouteType.WARSHIP)) {
				for (rIndex in b.getRouteIndicesAdjacentToVertex(r.from)) {
					val rr = b.getRoute(rIndex)
					if (rr.player != playerNum && rr.player > 0 && rr.type === RouteType.SHIP) {
						// attackable ship
						ships.add(rIndex)
					}
				}
			}
			return ArrayList(ships)
		}

		/**
		 * Attackable structures are those adjacent to a knight and on a road owned by the knight's player.
		 *
		 * @param soc
		 * @param playerNum
		 * @param b
		 * @return
		 */
		fun computeAttackableStructures(soc: SOC, playerNum: Int, b: Board): List<Int> {
			val verts = HashSet<Int>()
			for (vIndex in b.getVertIndicesOfType(playerNum, VertexType.BASIC_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE)) {
				val v = b.getVertex(vIndex)
				run loop@{
					for (i in 0 until v.numAdjacentVerts) {
						val vIndex2 = v.adjacentVerts[i]
						val v2 = b.getVertex(vIndex2)
						if (!v2.isStructure || v2.player == playerNum) 
							return@loop
						if (!soc.rules.isEnableKnightExtendedMoves) {
							b.getRoute(vIndex, vIndex2)?.let { r ->
								if (r.type !== RouteType.ROAD || r.player != playerNum) 
									return@loop
							}
						}
						when (v2.type) {
							VertexType.CITY -> if (soc.rules.knightScoreToDestroyCity > 0) verts.add(vIndex2)
							VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE -> if (soc.rules.knightScoreToDestroyMetropolis > 0) verts.add(vIndex2)
							VertexType.SETTLEMENT -> if (soc.rules.knightScoreToDestroySettlement > 0) verts.add(vIndex2)
							VertexType.WALLED_CITY -> if (soc.rules.knightScoreToDestroyWalledCity > 0) verts.add(vIndex2)
							else -> {
							}
						}
					}
				}
			}
			return ArrayList(verts)
		}

		fun computeSpyOpponents(soc: SOC, playerNum: Int): List<Int> {
			val players: MutableList<Int> = ArrayList()
			for (p in soc.players) {
				if (p.playerNum != playerNum) {
					if (p.getUnusedCardCount(CardType.Progress) > 0) {
						players.add(p.playerNum)
					}
				}
			}
			return players
		}

		fun computeNumStructuresAdjacentToTileType(playerNum: Int, b: Board, type: TileType): Int {
			var num = 0
			for (t in b.getTiles()) {
				if (t.type !== type) continue
				for (v in b.getTileVertices(t)) {
					if (v.isStructure && v.player == playerNum) {
						num++
					}
				}
			}
			return num
		}

		fun computeMasterMerchantPlayers(soc: SOC, player: Player): List<Int> {
			val players: MutableList<Int> = ArrayList()
			for (p in soc.players) {
				if (p.playerNum == player.playerNum) continue
				if (p.points > player.points) {
					if (p.getCardCount(CardType.Commodity) > 0 || p.getCardCount(CardType.Resource) > 0) players.add(p.playerNum)
				}
			}
			return players
		}

		/**
		 * compute the player num who should have the longest road, or 0 if none exists.
		 * soc is not changed.
		 *
		 * @param soc
		 * @return
		 */
		fun computeLongestRoadPlayer(soc: SOC): Player? {
			var maxRoadLen = soc.rules.minLongestLoadLen - 1
			val curLRP = soc.longestRoadPlayer
			if (curLRP != null) maxRoadLen = Math.max(maxRoadLen, curLRP.roadLength)
			var maxRoadLenPlayer = curLRP
			for (cur in soc.players) {
				val len = cur.roadLength
				if (len > maxRoadLen) {
					maxRoadLen = len
					maxRoadLenPlayer = cur
				}
			}
			if (maxRoadLenPlayer == null) return null
			return if (maxRoadLenPlayer.roadLength >= soc.rules.minLongestLoadLen) maxRoadLenPlayer else null
		}

		/**
		 * compute the player who should have the largest army.
		 * soc is not changed.
		 *
		 * @param soc
		 * @return
		 */
		fun computeLargestArmyPlayer(soc: SOC, b: Board): Player? {
			var maxArmySize = soc.rules.minLargestArmySize - 1
			val curLAP = soc.largestArmyPlayer
			if (curLAP != null) maxArmySize = Math.max(maxArmySize, curLAP.getArmySize(b))
			var maxArmyPlayer = curLAP
			for (cur in soc.players) {
				if (cur.getArmySize(b) > maxArmySize) {
					maxArmySize = cur.getArmySize(b)
					maxArmyPlayer = cur
				}
			}
			if (maxArmyPlayer == null) return null
			return if (maxArmyPlayer.getArmySize(b) >= soc.rules.minLargestArmySize) maxArmyPlayer else null
		}

		/**
		 * Compute the player who SHOULD have the harbor master points.  This is the single player with most harbor points >= 3.
		 *
		 * @param soc
		 * @return
		 */
		fun computeHarborMaster(soc: SOC): Player? {
			var minHarborPts = 2
			val curHM = soc.harborMaster
			if (curHM != null) minHarborPts = curHM.harborPoints
			var maxHM = curHM
			for (cur in soc.players) {
				if (cur.harborPoints > minHarborPts) {
					minHarborPts = cur.harborPoints
					maxHM = cur
				}
			}
			if (maxHM == null) return null
			return if (maxHM.harborPoints >= 3) maxHM else null
		}

		fun computeExporer(soc: SOC): Player? {
			var minExplorerPts = soc.rules.minMostDiscoveredTerritories - 1
			val curE = soc.explorer
			if (curE != null) {
				minExplorerPts = curE.numDiscoveredTerritories
			}
			var maxE = curE
			for (cur in soc.players) {
				if (cur.numDiscoveredTerritories > minExplorerPts) {
					minExplorerPts = cur.numDiscoveredTerritories
					maxE = cur
				}
			}
			return maxE
		}

		/**
		 * Return a list of vertices available for a settlement given a player and board instance.
		 *
		 * @param soc
		 * @param playerNum
		 * @param b
		 * @return
		 */
		fun computeSettlementVertexIndices(soc: SOC, playerNum: Int, b: Board): List<Int> {

			// un-owned settlements must be chosen first (pirate islands)
			val vertices = b.getVertIndicesOfType(0, VertexType.OPEN_SETTLEMENT).toMutableList()
			if (vertices.size > 0) return vertices

			// build an array of vertices legal for the current player
			// to place a settlement.
			for (i in 0 until b.numAvailableVerts) {
				val v = b.getVertex(i)
				if (v.type !== VertexType.OPEN) continue
				if (!v.canPlaceStructure()) continue
				var isOnIsland = false
				var canAdd = true
				for (cell in b.getVertexTiles(i)) {
					if (cell.islandNum > 0) {
						isOnIsland = true
					} else if (cell.type === TileType.UNDISCOVERED) {
						canAdd = false
						break
					}
				}
				if (!canAdd) continue
				var isOnRoute = false
				for (ii in 0 until v.numAdjacentVerts) {
					val iv = b.findAdjacentVertex(i, ii)
					if (iv >= 0) {
						val v2 = b.getVertex(iv)
						if (v2.isStructure) {
							canAdd = false
							break
						}
						val ie = b.getRouteIndex(i, iv)
						if (ie >= 0) {
							val e = b.getRoute(ie)
							if (e.player == playerNum) {
								if (e.type === RouteType.DAMAGED_ROAD) {
									canAdd = false
									break
								}
								isOnRoute = true
							}
						}
					}
				}
				if (!canAdd) continue
				if (b.getNumStructuresForPlayer(playerNum) < soc.rules.numStartSettlements) {
					if (soc.rules.isEnableIslandSettlementsOnSetup || !isOnIsland) vertices.add(i)
				} else {
					if (isOnRoute) vertices.add(i)
				}
			}
			return vertices
		}

		/**
		 * Return a list of edges available for a road given a player and board instance.
		 *
		 * @param playerNum
		 * @param b
		 * @return
		 */
		fun computeRoadRouteIndices(playerNum: Int, b: Board): List<Int> {
			//if (Profiler.ENABLED) Profiler.push("SOC::computeRoadOptions");
			return try {
				val edges: MutableList<Int> = ArrayList()
				for (i in 0 until b.numRoutes) {
					if (b.isRouteAvailableForRoad(i, playerNum)) edges.add(i)
				}
				edges
			} finally {
				//if (Profiler.ENABLED) Profiler.pop("SOC::computeRoadOptions");
			}
		}

		fun computeShipRouteIndices(soc: SOC, playerNum: Int, b: Board): List<Int> {
			//if (Profiler.ENABLED) Profiler.push("SOC::computeRoadOptions");
			return try {
				val edges: MutableList<Int> = ArrayList()
				for (i in 0 until b.numRoutes) {
					if (b.isRouteAvailableForShip(soc.rules, i, playerNum)) edges.add(i)
				}
				edges
			} finally {
				//if (Profiler.ENABLED) Profiler.pop("SOC::computeRoadOptions");
			}
		}

		/**
		 * return a list of vertices a new level 1 knight can be placed (basically any vertex that is on the road of a player that is empty)
		 *
		 * @param playerNum
		 * @param b
		 * @return
		 */
		fun computeNewKnightVertexIndices(playerNum: Int, b: Board): List<Int> {
			val verts = HashSet<Int>()
			// any open vertex on a players road will suffice
			for (r in b.getRoutes()) {
				if (r.player != playerNum) continue
				if (b.getVertex(r.from).type === VertexType.OPEN) verts.add(r.from)
				if (b.getVertex(r.to).type === VertexType.OPEN) verts.add(r.to)
			}
			return ArrayList(verts)
		}

		/**
		 * Return list of vertices where a knight can be promoted (this assumes the player has passed the canBuild(Knight) test)
		 *
		 * @param p
		 * @param b
		 * @return
		 */
		fun computePromoteKnightVertexIndices(p: Player, b: Board): List<Int> {
			val verts: MutableList<Int> = ArrayList()
			for (vIndex in 0 until b.numAvailableVerts) {
				val v = b.getVertex(vIndex)
				if (v.player == p.playerNum) {
					when (v.type) {
						VertexType.BASIC_KNIGHT_ACTIVE, VertexType.BASIC_KNIGHT_INACTIVE -> verts.add(vIndex)
						VertexType.STRONG_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_INACTIVE -> if (p.hasFortress()) {
							verts.add(vIndex)
						}
						else -> {
						}
					}
				}
			}
			return verts
		}

		fun computeDiplomatOpenRouteIndices(soc: SOC, b: Board): List<Int> {
			val allOpenRoutes: MutableList<Int> = ArrayList()
			for (i in 1..soc.numPlayers) {
				allOpenRoutes.addAll(computeOpenRouteIndices(i, b, true, false))
			}
			return allOpenRoutes
		}

		fun computeMovableShips(soc: SOC, p: Player, b: Board): List<Int> {
			// check for movable ships
			val movableShips: MutableList<Int> = ArrayList()
			val ships: Collection<Int> = computeOpenRouteIndices(p.playerNum, b, false, true)
			for (ship in ships) {
				val shipToMove = b.getRoute(ship)
				val shipType = shipToMove.type
				b.setRouteOpen(shipToMove)
				shipToMove.isLocked = true
				val openRoutes: Collection<Int> = computeShipRouteIndices(soc, p.playerNum, b)
				if (openRoutes.size > 0) {
					movableShips.add(ship)
				}
				b.setPlayerForRoute(shipToMove, p.playerNum, shipType)
				b.getRoute(ship).isLocked = false
			}
			return movableShips
		}

		fun computeOpenRouteIndices(playerNum: Int, b: Board, checkRoads: Boolean, checkShips: Boolean): List<Int> {
			val options: MutableSet<Int> = HashSet()
			for (eIndex in b.getRouteIndicesForPlayer(playerNum)) {
				val e = b.getRoute(eIndex)
				// check the obvious
				if (e.isClosed) continue
				if (e.isLocked) continue
				if (e.type.isVessel) {
					if (!checkShips) continue
					if (e.isAttacked) continue
				} else {
					if (!checkRoads) continue
				}

				// if either vertex is the players settlement, then not movable
				val v0 = b.getVertex(e.from)
				val v1 = b.getVertex(e.to)
				if (v0.type.isStructure && v0.player == playerNum) continue
				if (v1.type.isStructure && v1.player == playerNum) continue
				// if there is a route on EACH end, then not open
				var numConnected = 0
				for (ee in b.getVertexRoutes(e.from)) {
					if (ee !== e && (!checkShips || ee.isVessel) && ee.player == playerNum) {
						numConnected++
						break
					}
				}
				for (ee in b.getVertexRoutes(e.to)) {
					if (ee !== e && (!checkShips || ee.isVessel) && ee.player == playerNum) {
						numConnected++
						break
					}
				}
				if (numConnected < 2) {
					options.add(eIndex)
				}
			}
			return ArrayList(options)
		}

		/**
		 * Return list of verts a knight can move to including those where they can displace another player knight.
		 * These are all open verts that lie on the same route as the knight.  If expanded knight moves enabled
		 * then this includes the vertices that are one unit away, on land, and not on a route.
		 *
		 * @param knightVertex
		 * @param b
		 * @return
		 */
		fun computeKnightMoveVertexIndices(soc: SOC, knightVertex: Int, b: Board): List<Int> {
			val verts: MutableList<Int> = ArrayList()
			val knight = b.getVertex(knightVertex)
			assert(knight != null)
			assert(knight.type.knightLevel > 0)
			val visitedVerts = BooleanArray(b.numAvailableVerts)
			visitedVerts[knightVertex] = true
			findReachableVertsR(b, knight, knightVertex, verts, visitedVerts)
			if (soc.rules.isEnableKnightExtendedMoves) {
				val v = b.getVertex(knightVertex)
				for (i in 0 until v.numAdjacentVerts) {
					val vIndex = v.adjacentVerts[i]
					val v2 = b.getVertex(vIndex)
					if (v2.type === VertexType.OPEN && v2.isAdjacentToLand) verts.add(vIndex)
				}
			}
			return verts
		}

		fun computeActivateKnightVertexIndices(playerNum: Int, b: Board): List<Int> {
			return b.getVertIndicesOfType(playerNum, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE)
		}

		fun computeDisplacedKnightVertexIndices(soc: SOC, displacedKnightVertex: Int, b: Board): List<Int> {
			return computeKnightMoveVertexIndices(soc, displacedKnightVertex, b)
		}

		fun computeMovableKnightVertexIndices(soc: SOC, playerNum: Int, b: Board): List<Int> {
			val knights = b.getVertIndicesOfType(playerNum, VertexType.BASIC_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE)
			val movableKnights: MutableList<Int> = ArrayList()
			for (kIndex in knights) {
				if (computeKnightMoveVertexIndices(soc, kIndex, b).size > 0) {
					movableKnights.add(kIndex)
				}
			}
			return movableKnights
		}

		private fun findReachableVertsR(b: Board, knight: Vertex, startVertex: Int, verts: MutableList<Int>, visitedVerts: BooleanArray) {
			val start = b.getVertex(startVertex)
			for (i in 0 until start.numAdjacentVerts) {
				val vIndex = start.adjacentVerts[i]
				if (visitedVerts[vIndex]) continue
				visitedVerts[vIndex] = true
				val rIndex = b.getRouteIndex(startVertex, vIndex)
				val r = b.getRoute(rIndex)
				if (r.player != knight.player) continue
				val v = b.getVertex(vIndex)
				if (v.type === VertexType.OPEN) verts.add(vIndex) else if (v.isKnight && v.player != knight.player && knight.type.isKnightActive) {
					val kl = v.type.knightLevel
					if (kl > 0 && kl < knight.type.knightLevel) {
						verts.add(vIndex) // TODO: we can move to this vertex but not pass it?
					}
					continue
				}
				findReachableVertsR(b, knight, vIndex, verts, visitedVerts)
			}
		}

		/**
		 * Return a list of vertices available for a city given a player and board intance.
		 *
		 * @param playerNum
		 * @param b
		 * @return
		 */
		fun computeCityVertxIndices(playerNum: Int, b: Board): List<Int> {
			return b.getVertIndicesOfType(playerNum, VertexType.SETTLEMENT)
		}

		/**
		 * Return a list of vertices available for a city given a player and board instance.
		 *
		 * @param playerNum
		 * @param b
		 * @return
		 */
		fun computeCityWallVertexIndices(playerNum: Int, b: Board): List<Int> {
			return b.getVertIndicesOfType(playerNum, VertexType.CITY)
		}

		/**
		 * Return a list of vertices available for a metropolis given a player and board instance
		 *
		 * @param playerNum
		 * @param b
		 * @return
		 */
		fun computeMetropolisVertexIndices(playerNum: Int, b: Board): List<Int> {
			return b.getVertIndicesOfType(playerNum, VertexType.CITY, VertexType.WALLED_CITY)
		}

		/**
		 * Return a list of MoveTypes available given a player and board instance.
		 *
		 * @param p
		 * @param b
		 * @return
		 */
		fun computeMoves(p: Player, b: Board, soc: SOC): List<MoveType> {
			val types = LinkedHashSet<MoveType>()
			types.add(MoveType.CONTINUE)
			if (p.canBuild(BuildableType.City) && b.getNumSettlementsForPlayer(p.playerNum) > 0) types.add(MoveType.BUILD_CITY)
			if (!soc.rules.isEnableCitiesAndKnightsExpansion) {
				if (p.canBuild(BuildableType.Development)) types.add(MoveType.DRAW_DEVELOPMENT)
				for (t in DevelopmentCardType.values()) {
					if (t.moveType != null && p.getUsableCardCount(t) > 0) {
						types.add(t.moveType)
					}
				}
			}
			if (canPlayerTrade(p, b)) types.add(MoveType.TRADE)
			if (p.canBuild(BuildableType.Settlement)) {
				for (i in 0 until b.numAvailableVerts) {
					if (b.isVertexAvailbleForSettlement(i) && b.isVertexAdjacentToPlayerRoute(i, p.playerNum)) {
						types.add(MoveType.BUILD_SETTLEMENT)
						break
					}
				}
			}
			if (p.canBuild(BuildableType.Road)) {
				if (p.getCardCount(SpecialVictoryType.DamagedRoad) > 0) {
					types.add(MoveType.REPAIR_ROAD)
				} else {
					for (i in 0 until b.numRoutes) {
						if (b.isRouteAvailableForRoad(i, p.playerNum)) {
							types.add(MoveType.BUILD_ROAD)
							break
						}
					}
				}
			}
			if (soc.rules.isEnableSeafarersExpansion) {
				if (p.canBuild(BuildableType.Ship)) {
					for (i in 0 until b.numRoutes) {
						if (b.isRouteAvailableForShip(soc.rules, i, p.playerNum)) {
							types.add(MoveType.BUILD_SHIP)
							break
						}
					}
				}
				if (soc.rules.isEnableWarShipBuildable) {
					if (p.canBuild(BuildableType.Warship) && b.getRoutesOfType(p.playerNum, RouteType.SHIP).isNotEmpty()) {
						types.add(MoveType.BUILD_WARSHIP)
					}
					if (computeAttackableShips(soc, p.playerNum, b).isNotEmpty()) {
						types.add(MoveType.ATTACK_SHIP)
					}
				}

				// check for movable ships
				if (computeMovableShips(soc, p, b).isNotEmpty()) {
					types.add(MoveType.MOVE_SHIP)
				}
				if (b.getRoutesOfType(p.playerNum, RouteType.WARSHIP).size > 1) {
					for (vIndex in b.getVertIndicesOfType(0, VertexType.PIRATE_FORTRESS)) {
						for (r in b.getVertexRoutes(vIndex)) {
							if (r.type.isVessel && r.player == p.playerNum) {
								types.add(MoveType.ATTACK_PIRATE_FORTRESS)
								break
							}
						}
					}
				}
			}
			if (soc.rules.isEnableCitiesAndKnightsExpansion) {
				for (t in ProgressCardType.values()) {
					if (t.moveType != null && p.getUsableCardCount(t) > 0) {
						types.add(t.moveType)
					}
				}
				if (p.canBuild(BuildableType.CityWall)) {
					if (b.getNumVertsOfType(p.playerNum, VertexType.CITY) > 0) {
						types.add(MoveType.BUILD_CITY_WALL)
					}
				}
				if (p.canBuild(BuildableType.Knight)) {
					if (b.getOpenKnightVertsForPlayer(p.playerNum).size > 0) {
						types.add(MoveType.HIRE_KNIGHT)
					}
				}
				if (p.canBuild(BuildableType.PromoteKnight)) {
					if (computePromoteKnightVertexIndices(p, b).size > 0) types.add(MoveType.PROMOTE_KNIGHT)
				}
				if (p.canBuild(BuildableType.ActivateKnight)) {
					if (computeActivateKnightVertexIndices(p.playerNum, b).size > 0) types.add(MoveType.ACTIVATE_KNIGHT)
				}
				for (vIndex in b.getVertIndicesOfType(p.playerNum, VertexType.BASIC_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE)) {
					if (computeKnightMoveVertexIndices(soc, vIndex, b).size > 0) {
						types.add(MoveType.MOVE_KNIGHT)
						break
					}
				}
				val numCities = b.getNumVertsOfType(p.playerNum, VertexType.CITY, VertexType.WALLED_CITY)
				val numMetros = b.getNumVertsOfType(p.playerNum, VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE)

				// metropolis:
				//   Player must have at least 1 city / metropolis to make any improvements
				//   There are only 3 metropolis in play at any one time.
				//   Must have a non-metropolis city to increase beyond level 3 in any area
				//   First player to reach level 4 in an area, gets to convert one of their cities to a metropolis
				//   When a player reaches level 5 they can take the metropolis from another player with level 4
				//   Once a metropolis is at level 5 it cannot be taken away
				//   Can a player attain level 5 if someone else has a level 5 metro in that area? (no for now)
				if (numCities > 0 || numMetros > 0) {
					for (area in DevelopmentArea.values()) {
						val devel = p.getCityDevelopment(area)
						assert(devel <= DevelopmentArea.MAX_CITY_IMPROVEMENT)
						if (devel >= DevelopmentArea.MAX_CITY_IMPROVEMENT || p.getCardCount(requireNotNull(area.commodity)) <= devel) {
							continue
						}
						if (devel < DevelopmentArea.MIN_METROPOLIS_IMPROVEMENT - 1) {
							types.add(area.move)
							continue
						}
						if (soc.getMetropolisPlayer(area) != p.playerNum && numCities <= 0) {
							continue
						}
						if (devel <= DevelopmentArea.MIN_METROPOLIS_IMPROVEMENT) {
							types.add(area.move)
							continue
						}
						if (soc.getMetropolisPlayer(area) > 0) {
							val o = soc.getPlayerByPlayerNum(soc.getMetropolisPlayer(area))
							if (o.getCityDevelopment(area) >= DevelopmentArea.MAX_CITY_IMPROVEMENT) continue  // cant advance to max if someone else already has (TODO: confirm this rule or make config)
						}
						types.add(area.move)
					}
				}
				if (computeAttackableRoads(soc, p.playerNum, b).size > 0) {
					types.add(MoveType.KNIGHT_ATTACK_ROAD)
				}
				if (computeAttackableStructures(soc, p.playerNum, b).size > 0) {
					types.add(MoveType.KNIGHT_ATTACK_STRUCTURE)
				}
			}
			val moves = ArrayList(types)
			moves.sortWith { o1, o2 -> o1.priority - o2.priority }
			return moves
		}

		fun computePirateTileIndices(soc: SOC, b: Board): List<Int> {
			if (soc.isPirateAttacksEnabled) return emptyList()
			val cellIndices: MutableList<Int> = ArrayList()
			for (i in 0 until b.numTiles) {
				val cell = b.getTile(i)
				if (cell.isWater) {
					if (soc.rules.isEnableWarShipBuildable && b.pirateRouteStartTile < 0) {
						if (b.getTileRoutesOfType(cell, RouteType.WARSHIP).isEmpty())
							cellIndices.add(i)
					} else {
						cellIndices.add(i)
					}
				}
			}
			return cellIndices
		}

		/**
		 * Return a list of cells available for a robber given a board instance.
		 *
		 * @param b
		 * @return
		 */
		fun computeRobberTileIndices(soc: SOC, b: Board): List<Int> {
			val cellIndices: MutableList<Int> = ArrayList()
			if (!soc.rules.isEnableRobber) return cellIndices
			//		boolean desertIncluded = false;
			for (i in 0 until b.numTiles) {
				val cell = b.getTile(i)
				if (!cell.isLand) continue
				// only test tiles that has opposing players on them
				var addTile = true
				for (vIndex in 0 until cell.numAdj) {
					val v = b.getVertex(cell.getAdjVert(vIndex))
					if (v.player > 0 && v.player != soc.curPlayerNum) {
						if (v.isKnight) {
							addTile = false
							break
						}
						if (soc.getPlayerByPlayerNum(v.player).specialVictoryPoints < soc.rules.minVictoryPointsForRobber) {
							addTile = false
							break
						}
					}
				}
				if (addTile) {
					cellIndices.add(i)
				}
			}
			return cellIndices
		}

		/**
		 * Return a list of cells available for a merchant given a board instance.
		 *
		 * @param b
		 * @return
		 */
		fun computeMerchantTileIndices(soc: SOC, playerNum: Int, b: Board): List<Int> {
			val cellIndices: MutableList<Int> = ArrayList()
			for (i in 0 until b.numTiles) {
				if (b.robberTileIndex == i) continue
				val cell = b.getTile(i)
				if (cell.isDistributionTile && cell.resource != null) {
					for (v in b.getTileVertices(cell)) {
						if (v.isStructure && v.player == playerNum) {
							cellIndices.add(i)
							break
						}
					}
				}
			}
			return cellIndices
		}

		/**
		 * Return list of players who can be sabotaged by playerNum
		 *
		 * @param soc
		 * @param playerNum
		 * @return
		 */
		fun computeSaboteurPlayers(soc: SOC, playerNum: Int): List<Int> {
			val players: MutableList<Int> = ArrayList()
			val pts = soc.getPlayerByPlayerNum(playerNum).points
			for (player in soc.players) {
				if (player.playerNum == playerNum) continue
				if (player.points >= pts && player.unusedCardCount > 1) players.add(player.playerNum)
			}
			return players
		}

		/**
		 * Compute all the trade options given a player and board instance.
		 *
		 * @param p
		 * @param b
		 * @return
		 */
		fun computeTrades(p: Player, b: Board): List<Trade> {
			val trades: MutableList<Trade> = ArrayList()
			computeTrades(p, b, trades, 100)
			return trades
		}

		fun computeCatanStrength(soc: SOC, b: Board): Int {
			var str = 0
			for (i in 1..soc.numPlayers) {
				str += b.getKnightLevelForPlayer(i, true, false)
			}
			return str
		}

		fun computeBarbarianStrength(soc: SOC, b: Board): Int {
			var pts = 0
			if (soc.rules.barbarianPointsPerSettlement > 0) pts += b.getNumVertsOfType(0, VertexType.SETTLEMENT) * soc.rules.barbarianPointsPerSettlement
			pts += b.getNumVertsOfType(0, VertexType.CITY, VertexType.WALLED_CITY) * soc.rules.barbarianPointsPerCity
			pts += b.getNumVertsOfType(0, VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE) * soc.rules.barbarianPointsPerMetro
			return pts
		}

		private fun computeTrades(p: Player, b: Board, trades: MutableList<Trade>, maxOptions: Int) {

			//if (Profiler.ENABLED) Profiler.push("SOC::computeTradeOptions");
			try {
				var i: Int
				val resourcesFound = BooleanArray(NUM_RESOURCE_TYPES)
				val commoditiesFound = BooleanArray(NUM_COMMODITY_TYPES)

				// Check for Merchant Fleet
				p.merchantFleetTradable?.let { card ->
					if (p.getCardCount(card) >= 2) {
						trades.add(Trade(card, 2))
						when (card.cardType) {
							CardType.Commodity -> commoditiesFound[card.typeOrdinal] = true
							CardType.Resource -> resourcesFound[card.typeOrdinal] = true
							else -> throw SOCException("Unexpected case")
						}
					}
				}
				if (trades.size >= maxOptions) return

				// see if we have a 2:1 trade option

				// check for Trading House ability

				// we can trade 2:1 commodities if we have level 3 or greater trade improvement (Trading House)
				if (p.hasTradingHouse()) {
					for (type in CommodityType.values()) {
						if (!commoditiesFound[type.ordinal]) {
							if (p.getCardCount(type) >= 2) {
								trades.add(Trade(type, 2))
								commoditiesFound[type.ordinal] = true
							}
						}
					}
				}

				// check tiles for ports, merchant
				i = 0
				while (i < b.numTiles) {
					if (b.pirateTileIndex == i) {
						i++
						continue
					}
					if (trades.size >= maxOptions) return
					val tile = b.getTile(i)
					if (tile.resource == null) {
						i++
						continue
					}
					if (b.merchantTileIndex == i && b.merchantPlayer == p.playerNum) {
						if (p.getCardCount(requireNotNull(tile.resource)) < 2) {
							i++
							continue
						}
					} else {
						if (!tile.isPort || null == tile.resource) {
							i++
							continue
						}
						if (!b.isPlayerAdjacentToTile(p.playerNum, i)) {
							i++
							continue
						}
						if (p.getCardCount(requireNotNull(tile.resource)) < 2) {
							i++
							continue
						}
					}
					if (resourcesFound[requireNotNull(tile.resource).ordinal]) {
						i++
						continue
					}
					trades.add(Trade(requireNotNull(tile.resource), 2))
					resourcesFound[requireNotNull(tile.resource).ordinal] = true
					i++
				}

				// we have a 3:1 trade option when we are adjacent to a PORT_MULTI
				i = 0
				while (i < b.numTiles) {
					if (trades.size >= maxOptions) return
					val cell = b.getTile(i)
					if (cell.type !== TileType.PORT_MULTI) {
						i++
						continue
					}
					if (!b.isPlayerAdjacentToTile(p.playerNum, i)) {
						i++
						continue
					}

					// for (int r=0; r<Helper.NUM_RESOURCE_TYPES; r++) {
					for (r in ResourceType.values()) {
						if (!resourcesFound[r.ordinal] && p.getCardCount(r) >= 3) {
							trades.add(Trade(r, 3))
							resourcesFound[r.ordinal] = true
						}
					}
					for (c in CommodityType.values()) {
						if (!commoditiesFound[c.ordinal] && p.getCardCount(c) >= 3) {
							trades.add(Trade(c, 3))
							commoditiesFound[c.ordinal] = true
						}
					}
					i++
				}

				// look for 4:1 trades
				for (r in ResourceType.values()) {
					if (trades.size >= maxOptions) return
					if (!resourcesFound[r.ordinal] && p.getCardCount(r) >= 4) {
						trades.add(Trade(r, 4))
					}
				}
				for (c in CommodityType.values()) {
					if (trades.size >= maxOptions) return
					if (!commoditiesFound[c.ordinal] && p.getCardCount(c) >= 4) {
						trades.add(Trade(c, 4))
					}
				}
			} finally {
				//if (Profiler.ENABLED) Profiler.pop("SOC::computeTradeOptions");
			}
		}

		/**
		 * @param players
		 * @param p
		 * @param b
		 * @return
		 */
		fun computeTakeOpponentCardPlayers(players: Array<Player>, p: Player, b: Board, pirate: Boolean): List<Int> {
			val choices: MutableList<Int> = ArrayList()
			val playerNums = BooleanArray(players.size)
			if (pirate) {
				for (eIndex in b.getTileRouteIndices(b.getTile(b.pirateTileIndex))) {
					val e = b.getRoute(eIndex)
					if (e.player == 0) continue
					if (e.player == p.playerNum) continue
					if (players[e.player-1].totalCardsLeftInHand <= 0) continue
					playerNums[e.player-1] = true
				}
			} else {
				val cell = b.getTile(b.robberTileIndex)
				for (vIndex in cell.getAdjVerts()) {
					val v = b.getVertex(vIndex)
					if (v.player == 0) continue
					if (v.player == p.playerNum) continue
					if (players[v.player-1].totalCardsLeftInHand <= 0) continue
					playerNums[v.player-1] = true
				}
			}
			for (i in 1 until playerNums.size) if (playerNums[i]) choices.add(players[i].playerNum)
			return choices
		}

		/**
		 * Return list of vertices for warlord card
		 *
		 * @param b
		 * @param playerNum
		 * @return
		 */
		fun computeWarlordVertices(b: Board, playerNum: Int): List<Int> {
			return b.getVertIndicesOfType(playerNum, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE)
		}

		fun computeMerchantFleetCards(p: Player): List<Card> {
			val tradableCards: MutableList<Card> = ArrayList()
			for (t in ResourceType.values()) {
				val cards = p.getUsableCards(t)
				if (cards.size >= 2) {
					tradableCards.add(Card(t))
				}
			}
			for (t in CommodityType.values()) {
				val cards = p.getUsableCards(t)
				if (cards.size >= 2) {
					tradableCards.add(Card(t))
				}
			}
			return tradableCards
		}

		/**
		 * Return list of players who are not p who have more points than p and who have at least 1 Commodity or Resource card in their hand.
		 *
		 * @param soc
		 * @param p
		 * @return
		 */
		fun computeWeddingOpponents(soc: SOC, p: Player): List<Int> {
			val players: MutableList<Int> = ArrayList()
			for (player in soc.players) {
				if (player.playerNum == p.playerNum) continue
				if (player.points <= p.points) continue
				if (player.getUnusedCardCount(CardType.Commodity) > 0 || player.getUnusedCardCount(CardType.Resource) > 0) players.add(player.playerNum)
			}
			return players
		}

		/**
		 * Compute the vertices that the player is adjacent to that are pirate fortresses.
		 *
		 * @param b
		 * @param p
		 * @return
		 */
		fun computeAttackablePirateFortresses(b: Board, p: Player): List<Int> {
			val verts = HashSet<Int>()
			for (vIndex in b.getVertIndicesOfType(0, VertexType.PIRATE_FORTRESS)) {
				val v = b.getVertex(vIndex)
				run loop@ {
					for (v2 in v.adjacentVerts) {
						b.getRoute(vIndex, v2)?.let { r ->
							if (r.type.isVessel && r.player == p.playerNum) {
								verts.add(vIndex)
								return@loop
							}
						}
					}
				}
			}
			return ArrayList(verts)
		}

		/**
		 * Return a list of all the opponent players of playerNum
		 *
		 * @param soc
		 * @param playerNum
		 * @return
		 */
		fun computeOpponents(soc: SOC, playerNum: Int): List<Int> {
			val p: MutableList<Int> = ArrayList()
			for (i in 1..soc.numPlayers) {
				if (i != playerNum) {
					p.add(i)
				}
			}
			return p
		}

		/**
		 * Return a list of all opponents with at leat 1 card in their hand
		 *
		 * @param soc
		 * @param playerNum
		 * @return
		 */
		fun computeOpponentsWithCardsInHand(soc: SOC, playerNum: Int): List<Int> {
			val p: MutableList<Int> = ArrayList()
			for (i in 1..soc.numPlayers) {
				if (i != playerNum) {
					if (soc.getPlayerByPlayerNum(i).totalCardsLeftInHand > 0) p.add(i)
				}
			}
			return p
		}

		/**
		 * Get the winningest player with most victory points
		 *
		 * @param soc
		 * @return
		 */
		fun computePlayerWithMostVictoryPoints(soc: SOC): Player? {
			var winning: Player? = null
			var most = 0
			for (p in soc.players) {
				val num = p.specialVictoryPoints
				if (num > most) {
					most = num
					winning = p
				}
			}
			return winning
		}

		/**
		 * Return a list of players suitable for the deserter card
		 *
		 * @param soc
		 * @param b
		 * @param p
		 * @return
		 */
		fun computeDeserterPlayers(soc: SOC, b: Board, p: Player): List<Int> {
			val players: MutableList<Int> = ArrayList()
			for (i in 1..soc.numPlayers) {
				if (i == p.playerNum) continue
				if (0 < b.getNumKnightsForPlayer(i)) {
					players.add(i)
				}
			}
			return players
		}

		fun computeCraneCardImprovements(p: Player): List<DevelopmentArea> {
			val options = ArrayList<DevelopmentArea>()
			for (area in DevelopmentArea.values()) {
				val devel = p.getCityDevelopment(area)
				val numCommodity = p.getCardCount(requireNotNull(area.commodity))
				if (numCommodity >= devel) {
					options.add(area)
				}
			}
			return options
		}

		/**
		 * @param soc
		 * @param p
		 * @return
		 */
		fun computeGiftCards(soc: SOC, p: Player): List<Card> =
			p.getCards(CardType.Commodity).toMutableList().also {
				it.addAll(p.getCards(CardType.Resource))
			}

		/**
		 * @param p
		 * @param b
		 * @return
		 */
		fun canPlayerTrade(p: Player, b: Board): Boolean {
			val options: MutableList<Trade> = ArrayList()
			computeTrades(p, b, options, 1)
			return options.size > 0
		}

		fun checkMetropolis(soc: SOC, b: Board, devel: Int, playerNum: Int, area: DevelopmentArea): Boolean {
			if (devel >= DevelopmentArea.MIN_METROPOLIS_IMPROVEMENT) {
				//List<Integer> metropolis = board.getVertsOfType(0, area.vertexType);
				val metroPlayer = soc.getMetropolisPlayer(area)
				if (metroPlayer != playerNum && b.getNumVertsOfType(playerNum, VertexType.CITY, VertexType.WALLED_CITY) > 0) { // if we dont already own this metropolis
					if (metroPlayer == 0) { // if it is unowned
						return true
					} else {
						//assert(metropolis.size() == 1);
						val verts = b.getVertIndicesOfType(metroPlayer, area.vertexType)
						assert(verts.size == 1)
						val v = b.getVertex(verts[0])
						assert(v.player == metroPlayer)
						if (v.player != soc.curPlayerNum) {
							val other = soc.getPlayerByPlayerNum(metroPlayer)
							val otherDevel = other.getCityDevelopment(area)
							assert(otherDevel >= DevelopmentArea.MIN_METROPOLIS_IMPROVEMENT)
							if (otherDevel < devel) {
								soc.printinfo(soc.getString("%1\$s loses Metropolis %2\$s to %3\$s", other.name, area.getNameId(), soc.curPlayer.name))
								v.setPlayerAndType(other.playerNum, VertexType.CITY)
								soc.onMetropolisStolen(v.player, playerNum, area)
								return true
							}
						}
					}
				}
			}
			return false
		}

		private fun getDiceOfType(type: DiceType, dice: Collection<Dice>): Dice {
			for (d in dice) {
				if (d.type === type) return d
			}
			throw SOCException("No dice of type '$type' in: $dice")
		}

		init {
			addField(StackItem::class.java, "state")
			addAllFields(SOC::class.java)
		}
	}

	class StackItem @JvmOverloads constructor(
		val state: State = State.INIT_PLAYER_TURN, val undoAction: UndoAction? = null, val stateOptions: Collection<Int>? = null, val xtraOptions: Collection<*>? = null, val data: Any? = null) : Reflector<StackItem>() {
		override fun toString(): String {
			return state.name + if (data == null) "" else "($data)"
		}
	}

	private val mPlayers = ArrayList<Player>()
	private var mCurrentPlayer = 0

	/**
	 * Get number of attached players
	 *
	 * @return
	 */
	val numPlayers
		get() = mPlayers.size

	private val mDice = LinkedList<Int>() // compute the next 100 die rolls to support rewind with consistent die rolls
	private val mDiceConfigStack = Stack<Array<DiceType>>()
	private val mStateStack: Stack<StackItem> = Stack<StackItem>()
	private val mDevelopmentCards: MutableList<Card> = ArrayList()
	private lateinit var mProgressCards: Array<MutableList<Card>>
	private val mEventCards: MutableList<EventCard> = ArrayList()
	var board: Board = Board()
		private set

	open fun setBoard(board: Board) {
		this.board = board
	}

	var rules: Rules = Rules()
	
	var barbarianDistance = -1 // CAK
		private set
	private val mMetropolisPlayer = IntArray(NUM_DEVELOPMENT_AREA_TYPES)
	private var mBarbarianAttackCount = 0

	/**
	 * @return
	 */
	var isGameOver = false
		private set

	val state: State
		get() = mStateStack.peek().state

	val stateOptions: Collection<Int>?
		get() = mStateStack.peek().stateOptions

	init {
		board.generateDefaultBoard()
	}

	private fun <T> getStateData(): T {
		return mStateStack.peek().data as T
	}

	private fun <T> getStateExtraOptions(): Collection<T>? {
		return mStateStack.peek().xtraOptions as Collection<T>?
	}

	private fun getSpecialVictoryPlayer(card: SpecialVictoryType): Player? {
		var player: Player? = null
		for (p in players) {
			val num = p.getCardCount(card)
			assert(num == 0 || num == 1)
			if (num > 0) {
				assert(player == null)
				player = p
			}
		}
		return player
	}
	/**
	 * Get the playernum with the longest road
	 *
	 * @return
	 */
	/**
	 * Set the current longest road player
	 *
	 * @param player
	 */
	val longestRoadPlayer: Player?
		get() = getSpecialVictoryPlayer(SpecialVictoryType.LongestRoad)
		
	fun setLongestRoadPlayer(player: Player?) {
		player?.let { givePlayerSpecialVictoryCard(it, SpecialVictoryType.LongestRoad) }
	}

	/**
	 * Get the playernum with the largest ary
	 *
	 * @return
	 */
	/**
	 * Set the current largest army player
	 *
	 * @param player
	 */
	val largestArmyPlayer: Player?
		get() = getSpecialVictoryPlayer(SpecialVictoryType.LargestArmy)
		
	fun setLargestArmyPlayer(player: Player) {
		givePlayerSpecialVictoryCard(player, SpecialVictoryType.LargestArmy)
	}

	/**
	 * @return
	 */
	val harborMaster: Player?
		get() = getSpecialVictoryPlayer(SpecialVictoryType.HarborMaster)
	/**
	 * @return
	 */
	/**
	 * @param player
	 */
	val explorer: Player?
		get() = getSpecialVictoryPlayer(SpecialVictoryType.Explorer)
		
	fun setExplorer(player: Player) {
		givePlayerSpecialVictoryCard(player, SpecialVictoryType.Explorer)
	}

	/**
	 * Get the current player number.  If game not in progress then return 0.
	 *
	 * @return
	 */
	val curPlayerNum: Int
		get() = mCurrentPlayer+1

	/**
	 * @param playerNum
	 */
	fun setCurrentPlayer(playerNum: Int) {
		for (i in 0 until numPlayers) {
			if (mPlayers[i].playerNum == playerNum) {
				mCurrentPlayer = i
				break
			}
		}
	}

	private fun getDie(index: Int): Dice {
		return dice[index]
	}

	/**
	 * Get the dice.
	 *
	 * @return
	 */
	val dice: List<Dice>
		get() {
			initDice()
			val types = diceConfig
			val die: MutableList<Dice> = ArrayList()
			for (i in types.indices) {
				die.add(Dice(mDice[i], types[i]))
			}
			return die
		}
	private val diceConfig: Array<DiceType>
		private get() = mDiceConfigStack.peek()

	private fun pushDiceConfig(vararg types: DiceType) {
		mDiceConfigStack.add(arrayOf(*types))
	}

	private fun popDiceConfig() {
		mDiceConfigStack.pop()
	}

	/**
	 * Get sum of both 6 sided die
	 *
	 * @return
	 */
	val productionNum: Int
		get() = if (rules.isEnableEventCards)
					topEventCard.production
				else {
					val dice = dice
					var num = 0
					for (d in dice) {
						when (d.type) {
							DiceType.None,
							DiceType.Event -> Unit
							DiceType.RedYellow,
							DiceType.WhiteBlack,
							DiceType.YellowRed -> num += d.num
						}
					}
					num
				}

	/**
	 * @return
	 */
	val curPlayer: Player
		get() = mPlayers[mCurrentPlayer]

	/**
	 * Return player num who has existing metropolis or 0 if not owned
	 *
	 * @param area
	 * @return
	 */
	fun getMetropolisPlayer(area: DevelopmentArea): Int {
		return mMetropolisPlayer[area.ordinal]
	}

	fun setMetropolisPlayer(area: DevelopmentArea, playerNum: Int) {
		mMetropolisPlayer[area.ordinal] = playerNum
	}

	/**
	 * @return
	 */
	val robberCell: Tile
		get() = board.getTile(board.robberTileIndex)

	constructor(other: SOC) : this() {
		other.mPlayers.forEach {
			addPlayer(it.deepCopy())
		}
		mDice.addAll(other.mDice)
		mDiceConfigStack.addAll(other.mDiceConfigStack)
		mStateStack.addAll(other.mStateStack)
		mDevelopmentCards.addAll(other.mDevelopmentCards)
		mEventCards.addAll(other.mEventCards)
		barbarianDistance = other.barbarianDistance
		Utils.copyElems(mMetropolisPlayer, *other.mMetropolisPlayer)
		mBarbarianAttackCount = other.mBarbarianAttackCount
		board = other.board.shallowCopy()
		rules = other.rules
	}
	

	/**
	 * Resets game but keeps the players
	 */
	private fun reset() {
		for (i in 0 until numPlayers) {
			if (mPlayers[i] != null) mPlayers[i]!!.reset()
		}
		mStateStack.clear()
		mEventCards.clear()
		mDice.clear()
		mDiceConfigStack.clear()
		board.reset()
		Arrays.fill(mMetropolisPlayer, 0)
		mBarbarianAttackCount = 0
		isGameOver = false
	}

	/**
	 * Resets and removes all the players
	 */
	fun clear() {
		reset()
		mPlayers.forEach { p->
			p.reset()
		}
		mPlayers.clear()
		mCurrentPlayer = -1
	}

	private fun initDeck() {
		mDevelopmentCards.clear()
		if (rules.isEnableCitiesAndKnightsExpansion) {
			mProgressCards = Array(NUM_DEVELOPMENT_AREA_TYPES) {
				ArrayList()
			}
			for (p in ProgressCardType.values()) {
				if (!p.isEnabled(rules)) continue
				for (i in 0 until p.deckOccurances) {
					mProgressCards[p.getData().ordinal].add(Card(p, CardStatus.USABLE))
				}
			}
			for (i in mProgressCards.indices) Utils.shuffle(mProgressCards[i])
		} else {
			for (d in DevelopmentCardType.values()) {
				when (d) {
					DevelopmentCardType.Monopoly, DevelopmentCardType.RoadBuilding, DevelopmentCardType.YearOfPlenty, DevelopmentCardType.Victory -> {
						var i = 0
						while (i < d.deckOccurances) {
							mDevelopmentCards.add(Card(d, CardStatus.USABLE))
							i++
						}
					}
					DevelopmentCardType.Soldier -> {
						if (rules.isEnableRobber) {
							var i = 0
							while (i < d.deckOccurances) {
								mDevelopmentCards.add(Card(d, CardStatus.USABLE))
								i++
							}
						}
					}
					DevelopmentCardType.Warship -> {
						if (isPirateAttacksEnabled || rules.isEnableWarShipBuildable) {
							var i = 0
							while (i < d.deckOccurances) {
								mDevelopmentCards.add(Card(d, CardStatus.USABLE))
								i++
							}
						}
					}
					else -> throw SOCException("Unhandled case $d")
				}
			}
			Utils.shuffle(mDevelopmentCards)
		}
	}

	private fun initEventCards() {
		mEventCards.clear()
		for (e in EventCardType.values()) {
			for (p in e.production) {
				mEventCards.add(EventCard(e, p))
			}
		}
		Utils.shuffle(mEventCards)
	}

	/**
	 * @return
	 */
	val neutralPlayer: Player? = players.firstOrNull { it.isNeutralPlayer }

	/**
	 * @param playerNum range is [1-numPlayers] inclusive
	 * @return null if player num out of range, the player with num otherwise
	 */
	fun getPlayerByPlayerNum(playerNum: Int): Player {
		assert(playerNum > 0)
		return mPlayers[playerNum - 1]!!
	}

	fun setPlayer(p: Player, playerNum: Int) {
		assert(playerNum in 1..MAX_PLAYERS)
		mPlayers[playerNum - 1] = p
		p.playerNum = playerNum
	}

	// package access for unit tests
	fun pushStateFront(state: State) {
		pushStateFront(state, null, null, null)
	}

	fun pushStateFront(state: State, data: Any?) {
		pushStateFront(state, data, null, null)
	}

	private fun pushStateFront(state: State, data: Any?, options: Collection<Int>?, action: UndoAction? = null) {
		//log.debug("Push state: " + state);
		//mStateStack.add(new StackItem(state, action, options, null, data));
		pushStateFront(state, data, options, null, action)
	}

	// states are managed in a FIFO stack
	private fun pushStateFront(state: State, data: Any?, options: Collection<Int>?, xtraOptions: Collection<*>?, action: UndoAction?) {
		log.debug("Push state: $state")
		mStateStack.add(StackItem(state, action, options, xtraOptions, data))
	}

	/**
	 * Override to enable/disable some logging.  Base version always returns true.
	 *
	 * @return
	 */
	val isDebugEnabled: Boolean
		get() = true

	/**
	 * @param player
	 */
	fun addPlayer(player: Player) {
		if (numPlayers == MAX_PLAYERS) throw SOCException("Too many players")
		if (numPlayers == rules.maxPlayers) throw SOCException("Max players already added.")
		mPlayers.add(player)
		if (player.playerNum == 0) player.playerNum = numPlayers
		log.debug("AddPlayer num = " + player.playerNum + " " + player.javaClass.simpleName)
	}

	private fun incrementCurPlayer(num: Int) {
		val nextPlayer = (mCurrentPlayer + numPlayers + num) % numPlayers
		log.debug("Increment player [" + num + "] positions, was " + curPlayer.playerNum + ", now " + mPlayers[nextPlayer].playerNum)
		mCurrentPlayer = nextPlayer
	}

	private fun dumpStateStack() {
		if (mStateStack.size == 0) {
			log.warn("State Stack Empty")
		} else {
			val buf = StringBuffer()
			for (s in mStateStack) {
				buf.append(s.state).append(", ")
			}
			log.debug(buf.toString())
		}
	}

	/*
     *
     */
	private fun popState() {
		log.debug("Popping state $state")
		assert(mStateStack.size > 0)
		mStateStack.pop()
		//		log.debug("Setting state to " + (getState()));
	}

	// package access for unit tests
	//void setDice(List<Dice> dice) {
	//    this.di
	private fun initDice() {
		while (mDice.size < 100) {
			mDice.addLast(Utils.rand() % 6 + 1)
		}
	}

	fun clearDiceStack() {
		mDice.clear()
	}

	private fun nextDice(): List<Dice> {
		if (mDice.size > 0) mDice.removeFirst()
		initDice()
		return dice
	}

	fun nextDie(): Int {
		initDice()
		return mDice.removeFirst()
	}

	/**
	 *
	 */
	fun rollDice() {
		val dice = nextDice()
		onDiceRolledPrivate(dice)
	}

	var topEventCard = EventCard()

	private val nextEventCard: EventCard
		get() = if (mEventCards.isEmpty()) {
			initEventCards()
			mEventCards.first()
		} else {
			mEventCards.removeFirst()
		}.also {
			topEventCard = it
		}

	private fun processEventCard(next: EventCard) {
		when (next.type) {
			EventCardType.CalmSea -> {

				// player with most harbors get to pick a resource card
				val harborCount = IntArray(MAX_PLAYERS + 1)
				var most = 0
				for (p in players) {
					var num = 0
					val used = BooleanArray(board.numTiles)
					for (vIndex in board.getStructuresForPlayer(p.playerNum)) {
						val v = board.getVertex(vIndex)
						var i = 0
						while (i < v.numTiles) {
							val tIndex = v.getTile(i)
							if (used[tIndex]) {
								i++
								continue
							}
							used[tIndex] = true
							val t = board.getTile(tIndex)
							when (t.type) {
								TileType.PORT_BRICK, TileType.PORT_MULTI, TileType.PORT_ORE, TileType.PORT_SHEEP, TileType.PORT_WHEAT, TileType.PORT_WOOD -> num++
								else                                                                                                                      -> {
								}
							}
							i++
						}
					}
					harborCount[p.playerNum] = num
					printinfo(getString("%1\$s has %2\$d harbors", p.name, num))
					if (num > most) {
						most = num
					}
				}
				if (most > 0) {
					var i = 1
					while (i < numPlayers + 1) {
						if (harborCount[i] == most) {
							printinfo(getString("%s has most harbors and gets to pick a resource card", getPlayerByPlayerNum(i).name))
							pushStateFront(State.SET_PLAYER, curPlayerNum)
							pushStateFront(State.DRAW_RESOURCE_NOCANCEL)
							pushStateFront(State.SET_PLAYER, i)
						}
						i++
					}
				}
			}
			EventCardType.Conflict -> {
				var LAP = largestArmyPlayer
				if (LAP == null) {
					val armySize = IntArray(numPlayers + 1)
					var maxSize = 0
					for (p in players) {
						var size = 0
						size = if (rules.isEnableCitiesAndKnightsExpansion) {
							board.getKnightLevelForPlayer(p.playerNum, true, false)
						} else {
							p.getArmySize(board)
						}
						armySize[p.playerNum] = size
						maxSize = Math.max(size, maxSize)
					}
					var i = 1
					while (i < armySize.size) {
						if (armySize[i] == maxSize) {
							if (LAP == null) LAP = getPlayerByPlayerNum(i) else {
								// there is a tie, s no event
								LAP = null
								break
							}
						}
						i++
					}
				}
				if (LAP != null) {
					printinfo(getString("%s Has largest army and gets to take a resource card from another", LAP.name))
					pushStateFront(State.SET_PLAYER, curPlayerNum)
					pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM, null, computeOpponents(this, LAP.playerNum), null)
					pushStateFront(State.SET_PLAYER, LAP.playerNum)
				} else {
					printinfo(getString("No single player with largest army so event cancelled"))
				}
			}
			EventCardType.Earthquake -> {
				for (p in players) {
					if (p.getCardCount(SpecialVictoryType.DamagedRoad) > 0) continue
					val routes = board.getRoutesOfType(p.playerNum, RouteType.ROAD, RouteType.DAMAGED_ROAD)
					if (routes.size > 0) {
						val r = Utils.randItem(routes)
						p.addCard(SpecialVictoryType.DamagedRoad)
						r.type = RouteType.DAMAGED_ROAD
					}
				}
			}
			EventCardType.Epidemic -> {
			}
			EventCardType.GoodNeighbor -> {

				// each player gives player to their left a resource or commodity
				pushStateFront(State.SET_PLAYER, curPlayerNum)
				var i = 1
				while (i <= numPlayers) {
					var left = i + 1
					if (left > numPlayers) left = 1
					pushStateFront(State.CHOOSE_GIFT_CARD, getPlayerByPlayerNum(left))
					pushStateFront(State.SET_PLAYER, i)
					i++
				}
			}
			EventCardType.NeighborlyAssistance -> {

				// winning player gives up a resource to another
				val p = computePlayerWithMostVictoryPoints(this)
				if (p != null) {
					val cards = p.giftableCards
					if (cards.size > 0) {
						printinfo(getString("%s must give a resource card to another player of their choice", p.name))
						pushStateFront(State.SET_PLAYER, curPlayerNum)
						pushStateFront(State.CHOOSE_OPPONENT_FOR_GIFT_CARD, null, computeOpponents(this, p.playerNum), cards, null)
						pushStateFront(State.SET_PLAYER, p.playerNum)
					}
				}
			}
			EventCardType.NoEvent -> {
			}
			EventCardType.PlentifulYear -> {

				// each player draws a resource from pile
				var i = 0
				while (i < numPlayers) {
					pushStateFront(State.NEXT_PLAYER)
					pushStateFront(State.DRAW_RESOURCE_NOCANCEL)
					i++
				}
			}
			EventCardType.RobberAttack -> {
			}
			EventCardType.RobberFlees -> {
				val tiles = board.getTilesOfType(TileType.DESERT)
				if (tiles.size > 0) {
					board.setRobber(tiles[0])
				} else {
					board.setRobber(-1)
				}
			}
			EventCardType.Tournament -> {
				// TODO: verify non-cak rules
				val numKnights = IntArray(numPlayers + 1)
				var most = 0
				var i = 1
				while (i <= numPlayers) {
					var num = 0
					num = if (rules.isEnableCitiesAndKnightsExpansion) board.getNumKnightsForPlayer(i) else getPlayerByPlayerNum(i).getArmySize(board)
					numKnights[i] = num
					if (num > most) {
						most = num
					}
					i++
				}
				if (most > 0) {
					pushStateFront(State.SET_PLAYER, curPlayerNum)
					var i = 1
					while (i < numKnights.size) {
						if (numKnights[i] == most) {
							pushStateFront(State.DRAW_RESOURCE_NOCANCEL)
							pushStateFront(State.SET_PLAYER, i)
						}
						i++
					}
				}
			}
			EventCardType.TradeAdvantage -> {
				var LRP = longestRoadPlayer
				if (LRP == null) {
					val roadLength = IntArray(numPlayers + 1)
					var maxSize = 0
					for (p in players) {
						roadLength[p.playerNum] = p.roadLength
						val len = roadLength[p.playerNum]
						maxSize = Math.max(len, maxSize)
					}
					var i = 1
					while (i < roadLength.size) {
						if (roadLength[i] == maxSize) {
							if (LRP == null) LRP = getPlayerByPlayerNum(i) else {
								// there is a tie, s no event
								printinfo(getString("No event when 2 or more players have the same size army"))
								LRP = null
								break
							}
						}
						i++
					}
				}
				if (LRP != null) {
					printinfo(getString("%s has longest road and gets to take a card from another", LRP.name))
					pushStateFront(State.SET_PLAYER, curPlayerNum)
					pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM, null, computeOpponents(this, LRP.playerNum), null)
					pushStateFront(State.SET_PLAYER, LRP.playerNum)
				} else {
					printinfo(getString("No single player with the longest road so event cancelled"))
				}
			}
		}
	}

	/**
	 * Called for Every resource bundle a player recieves.
	 * Called once for each player, for each resource.
	 * default method does nothing.
	 *
	 * @param playerNum
	 * @param type
	 * @param amount
	 */
	protected open fun onDistributeResources(playerNum: Int, type: ResourceType, amount: Int) {}

	/**
	 * @param playerNum
	 * @param type
	 * @param amount
	 */
	protected open fun onDistributeCommodity(playerNum: Int, type: CommodityType, amount: Int) {}
	private fun distributeResources(diceRoll: Int) {
		// collect info to be displayed at the end
		val resourceInfo = Array(NUM_RESOURCE_TYPES) {
			IntArray(numPlayers + 1)
		}
		val commodityInfo = Array(NUM_COMMODITY_TYPES) {
			IntArray(numPlayers + 1)
		}
		if (diceRoll > 0) printinfo(getString("Distributing resources for num %d", diceRoll))
		var epidemic = rules.isEnableEventCards && topEventCard.type == EventCardType.Epidemic
		val playerDidReceiveResources = BooleanArray(numPlayers + 1)

		// visit all the cells with dice as their num
		for (i in 0 until board.numTiles) {
			val cell = board.getTile(i)
			if (!cell.isDistributionTile) continue
			assert(cell.dieNum != 0)
			if (board.robberTileIndex == i) continue  // apply the robber
			if (diceRoll > 0 && cell.dieNum != diceRoll) continue

			// visit each of the adjacent verts to this cell and
			// add to any player at the vertex, some resource of
			// type cell.resource
			for (vIndex in cell.getAdjVerts()) {
				val vertex = board.getVertex(vIndex)
				if (vertex.player > 0 && vertex.isStructure) {
					val p = getPlayerByPlayerNum(vertex.player)
					if (cell.type === TileType.GOLD) {
						// set to original player
						printinfo(getString("%s has struck Gold!", p.name))
						pushStateFront(State.SET_PLAYER, curPlayerNum)
						if (rules.isEnableCitiesAndKnightsExpansion) {
							pushStateFront(State.DRAW_RESOURCE_OR_COMMODITY_NOCANCEL)
						} else {
							pushStateFront(State.DRAW_RESOURCE_NOCANCEL)
						}
						if (vertex.isCity) {
							pushStateFront(State.DRAW_RESOURCE_NOCANCEL)
						}
						playerDidReceiveResources[p.playerNum] = true
						// set to player that needs choose a resource
						pushStateFront(State.SET_PLAYER, vertex.player)
					} else if (rules.isEnableCitiesAndKnightsExpansion) {
						val numPerCity = rules.numResourcesForCity
						val numPerSet = rules.numResourcesForSettlement
						if (vertex.isCity) {
							if (epidemic) {
								resourceInfo[requireNotNull(cell.resource).ordinal][vertex.player] += numPerCity / 2
								p.incrementResource(requireNotNull(cell.resource), numPerCity / 2)
							} else {
								if (cell.commodity == null) {
									resourceInfo[requireNotNull(cell.resource).ordinal][vertex.player] += numPerCity
									p.incrementResource(requireNotNull(cell.resource), numPerCity)
								} else {
									val numComm = numPerCity / 2
									val numRes = numPerCity - numComm
									resourceInfo[requireNotNull(cell.resource).ordinal][vertex.player] += numRes
									p.incrementResource(requireNotNull(cell.resource), numRes)
									commodityInfo[requireNotNull(cell.commodity).ordinal][vertex.player] += numComm
									p.incrementResource(requireNotNull(cell.commodity), numComm)
								}
							}
						} else if (vertex.isStructure) {
							resourceInfo[requireNotNull(cell.resource).ordinal][vertex.player] += numPerSet
							p.incrementResource(requireNotNull(cell.resource), numPerSet)
						}
					} else {
						var num = rules.numResourcesForSettlement
						if (!epidemic && vertex.isCity) num = rules.numResourcesForCity
						resourceInfo[requireNotNull(cell.resource).ordinal][vertex.player] += num
						p.incrementResource(requireNotNull(cell.resource), num)
					}
				}
			}
		}
		for (p in players) {
			var msg = ""
			for (r in ResourceType.values()) {
				val amount = resourceInfo[r.ordinal][p.playerNum]
				if (amount > 0) {
					msg += (if (msg.isEmpty()) "" else ", ") + amount + " X " + r.getNameId()
					onDistributeResources(p.playerNum, r, amount)
				}
			}
			for (c in CommodityType.values()) {
				val amount = commodityInfo[c.ordinal][p.playerNum]
				if (amount > 0) {
					msg += msg.appendDelimited(", ", amount.toString() + " X " + c.getNameId())
					onDistributeCommodity(p.playerNum, c, amount)
				}
			}
			if (msg.isNotEmpty()) {
				printinfo(getString("%1\$s gets %2\$s", p.name, msg))
			} else if (!playerDidReceiveResources[p.playerNum] && p.hasAqueduct()) {
				printinfo(getString("%s applying Aqueduct ability", p.name))
				onAqueduct(p.playerNum)
				pushStateFront(State.SET_PLAYER, curPlayerNum)
				pushStateFront(State.DRAW_RESOURCE_NOCANCEL)
				pushStateFront(State.SET_PLAYER, p.playerNum)
			}
		}
	}

	/**
	 * CValled when a player gets to apply their aqueduct special ability.
	 *
	 * @param playerNum
	 */
	protected open fun onAqueduct(playerNum: Int) {}

	/**
	 * Print a game info.  Base version writes to stdout.
	 * All messages originate from @see runRame
	 *
	 * @param playerNum
	 * @param txt
	 */
	open fun printinfo(playerNum: Int, txt: String) {
		if (!Utils.isEmpty(txt)) {
			if (playerNum > 0) {
				with (getPlayerByPlayerNum(playerNum)) {
					log.info("%s: %s", name, txt)
				}
			}
			log.info(txt)
		}
	}

	private fun printinfo(txt: String) {
		printinfo(curPlayerNum, txt)
	}

	/**
	 * Called when a players point change (for better or worse).  default method does nothing.
	 *
	 * @param playerNum
	 * @param changeAmount
	 */
	protected open fun onPlayerPointsChanged(playerNum: Int, changeAmount: Int) {}
	private fun updatePlayerPoints() {
		for (i in 0 until numPlayers) {
			val p = mPlayers[i]!!
			val newPoints = computePointsForPlayer(p, board, this)
			if (newPoints != p.points) {
				onPlayerPointsChanged(p.playerNum, newPoints - p.points)
				p.points = newPoints
			}
		}
	}

	private fun onDiceRolledPrivate(dice: List<Dice>) {
		var rolled: String = ""
		for (d in dice) {
			when (d.type) {
				DiceType.Event -> {
					rolled = rolled.appendDelimited(", ", fromDieNum(d.num))
				}
				DiceType.RedYellow, DiceType.WhiteBlack, DiceType.YellowRed -> {
					rolled = rolled.appendDelimited(", ", d.num)
				}
				else -> Unit
			}
		}
		printinfo(getString("Rolled %s", rolled))
		onDiceRolled(dice)
	}

	/**
	 * Called immediately after a die roll.  Base method does nothing.
	 *
	 * @param dice
	 */
	protected open fun onDiceRolled(dice: List<Dice>) {}

	/**
	 * Called immediately after event card dealt.  Base method does nothing.
	 *
	 * @param card
	 */
	protected open fun onEventCardDealt(card: EventCard) {}

	/**
	 * Called when a player picks a development card from the deck.
	 * default method does nothing.
	 *
	 * @param playerNum
	 * @param card
	 */
	protected open fun onCardPicked(playerNum: Int, card: Card) {}
	private fun pickDevelopmentCardFromDeck() {
		// add up the total chance
		if (mDevelopmentCards.size <= 0) {
			initDeck()
		}
		val picked = mDevelopmentCards.removeAt(0)
		picked.setUnusable()
		curPlayer.addCard(picked)
		printinfo(getString("%1\$s picked a %2\$s card", curPlayer.name, picked))
		onCardPicked(curPlayerNum, picked)
	}

	/**
	 * Called when a player takes a card from another due to soldier.  default method does nothing.
	 *
	 * @param takerNum
	 * @param giverNum
	 * @param card
	 */
	protected open fun onTakeOpponentCard(takerNum: Int, giverNum: Int, card: Card) {}
	protected fun newNeutralPlayer(): Player {
		throw SOCException("Not implemented")
	}

	private fun takeOpponentCard(taker: Player, giver: Player) {
		assert(giver !== taker)
		giver.removeRandomUnusedCard()?.let { taken ->
			taker.addCard(taken)
			printinfo(getString("%1\$s taking a %2\$s card from Player %3\$s", taker.name, taken.name, giver.name))
			onTakeOpponentCard(taker.playerNum, giver.playerNum, taken)
		}
	}

	fun initGame() {
		// setup
		if (numPlayers < rules.minPlayers) throw SOCException("Too few players " + numPlayers + " is too few of " + rules.minPlayers)
		if (rules.isCatanForTwo) {
			if (neutralPlayer == null) {
				addPlayer(newNeutralPlayer())
			}
		}
		clearDiceStack()
		board.reset()
		board.assignRandom()
		reset()
		if (rules.isEnableCitiesAndKnightsExpansion) {
			if (rules.isEnableEventCards) {
				pushDiceConfig(DiceType.RedYellow, DiceType.Event)
			} else {
				pushDiceConfig(DiceType.YellowRed, DiceType.RedYellow, DiceType.Event)
			}
			barbarianDistance = rules.barbarianStepsToAttack
		} else {
			if (!rules.isEnableEventCards) pushDiceConfig(DiceType.WhiteBlack, DiceType.WhiteBlack) else pushDiceConfig()
			barbarianDistance = -1
		}
		initDice()
		initDeck()
		if (isPirateAttacksEnabled) {
			board.setPirate(board.pirateRouteStartTile)
		}
		for (vIndex in board.getVertIndicesOfType(0, VertexType.PIRATE_FORTRESS)) {
			board.getVertex(vIndex).pirateHealth = rules.pirateFortressHealth
		}
		mCurrentPlayer = Utils.randRange(0, numPlayers - 1)
		pushStateFront(State.START_ROUND)
		pushStateFront(State.DEAL_CARDS)

		// first player picks last
		pushStateFront(State.POSITION_ROAD_OR_SHIP_NOCANCEL)
		if (rules.isEnableCitiesAndKnightsExpansion) pushStateFront(State.POSITION_CITY_NOCANCEL) else pushStateFront(State.POSITION_SETTLEMENT_NOCANCEL)

		// player picks in reverse order
		for (i in numPlayers - 1 downTo 1) {
			if (rules.isCatanForTwo) {
				pushStateFront(State.POSITION_NEUTRAL_ROAD_NOCANCEL)
				pushStateFront(State.POSITION_NEUTRAL_SETTLEMENT_NOCANCEL)
			}
			pushStateFront(State.PREV_PLAYER)
			pushStateFront(State.POSITION_ROAD_OR_SHIP_NOCANCEL)
			if (rules.isEnableCitiesAndKnightsExpansion) pushStateFront(State.POSITION_CITY_NOCANCEL) else pushStateFront(State.POSITION_SETTLEMENT_NOCANCEL)
		}
		pushStateFront(State.CLEAR_FORCED_SETTLEMENTS)

		// the last player picks again
		pushStateFront(State.POSITION_ROAD_OR_SHIP_NOCANCEL)
		pushStateFront(State.POSITION_SETTLEMENT_NOCANCEL)

		// players pick in order
		for (i in 0 until numPlayers - 1) {
			if (rules.isCatanForTwo) {
				pushStateFront(State.POSITION_NEUTRAL_ROAD_NOCANCEL)
				pushStateFront(State.POSITION_NEUTRAL_SETTLEMENT_NOCANCEL)
			}
			pushStateFront(State.NEXT_PLAYER)
			pushStateFront(State.POSITION_ROAD_OR_SHIP_NOCANCEL)
			pushStateFront(State.POSITION_SETTLEMENT_NOCANCEL)
		}
	}

	private fun checkGameOver(): Boolean {
		if (isGameOver) return true
		mPlayers.forEach { player ->
			val pts = player.points
			if (board.getNumVertsOfType(0, VertexType.PIRATE_FORTRESS) == 0 || player.getCardCount(SpecialVictoryType.CapturePirateFortress) > 0) {
				if (player.points >= rules.pointsForWinGame) {
					onGameOver(player.playerNum)
					isGameOver = true
					return true
				}
			}
		}
		return false
	}

	/**
	 * Return true when it is valid to call run()
	 *
	 * @return
	 */
	fun canRun(): Boolean {
		return mStateStack.size > 0
	}

	private fun runGameCheck(): Boolean {
		if (board == null) {
			throw SOCException("No board, cannot run game")
		}
		if (!board.isReady) {
			throw SOCException("Board not initialized, cannot run game")
		}
		if (numPlayers < 2) {
			throw SOCException("Not enought players, cannot run game")
		}
		var i: Int

		// test that the players are numbered correctly
		i = 1
		while (i <= numPlayers) {
			if (getPlayerByPlayerNum(i) == null) throw SOCException("Cannot find player '" + i + "' of '" + numPlayers + "' cannot run game")
			i++
		}
		if (mStateStack.isEmpty()) initGame()
		assert(!mStateStack.isEmpty())
		updatePlayerPoints()
		return if (checkGameOver()) {
			false
		} else true
	}

	/**
	 * A game processing step.  Typically this method is called from a unique thread.
	 *
	 * @return true if run is valid, false otherwise
	 */
	open fun runGame() {

		//Vertex knightVertex = null;
		if (!runGameCheck()) return

		val state = this.state

		//dumpStateStack();
		var msg = "Processing state : $state"
		if (getStateData<Any?>() != null) {
			msg += " data=" + getStateData<Any>()
		}
		stateOptions?.let { stateOptions ->
			msg += " options=${stateOptions.joinToString(",")}"
		}
		if (getStateExtraOptions<Any>() != null) {
			msg += " xtraOpts=" + getStateExtraOptions<Any>()
		}
		log.debug(msg)
		try {
			when (state) {
				State.DEAL_CARDS -> {
					popState()
					distributeResources(0)
				}
				State.PROCESS_DICE -> {
					popState()
					processDice()
				}
				State.PROCESS_PIRATE_ATTACK -> {
					popState()
					processPirateAttack()
				}
				State.CHOOSE_PIRATE_FORTRESS_TO_ATTACK -> {
					printinfo(getString("%s choose fortress to attack", curPlayer.name))
					curPlayer.chooseVertex(this, requireNotNull(stateOptions), VertexChoice.PIRATE_FORTRESS, null)?.let { v ->
						Utils.assertContains(v, requireNotNull(stateOptions))
						onVertexChosen(curPlayerNum, VertexChoice.PIRATE_FORTRESS, v, null)
						popState()
						if (rules.isAttackPirateFortressEndsTurn) popState()
						processPlayerAttacksPirateFortress(curPlayer, v)
					}
				}
				State.POSITION_NEUTRAL_SETTLEMENT_NOCANCEL,
				State.POSITION_SETTLEMENT_CANCEL,
				State.POSITION_SETTLEMENT_NOCANCEL -> {
					val options = stateOptions ?: when (state) {
						State.POSITION_NEUTRAL_SETTLEMENT_NOCANCEL -> {
							printinfo(getString("%s place settlement", curPlayer.name))
							computeSettlementVertexIndices(this, neutralPlayer!!.playerNum, board)
						}
						State.POSITION_SETTLEMENT_CANCEL,
						State.POSITION_SETTLEMENT_NOCANCEL -> {
							printinfo(getString("%s place settlement", curPlayer.name))
							computeSettlementVertexIndices(this, curPlayerNum, board)
						}
						else -> throw AssertionError("Unhandled case")
					}
					assert(!Utils.isEmpty(options))
					curPlayer.chooseVertex(this, options, VertexChoice.SETTLEMENT, null)?.let { vIndex ->
						Utils.assertContains(vIndex, options)
						onVertexChosen(curPlayerNum, VertexChoice.SETTLEMENT, vIndex, null)
						popState()
						val v = board.getVertex(vIndex)
						printinfo(getString("%1\$s placed a settlement on vertex %2\$s", curPlayer.name, vIndex))
						assert(v.player == 0)
						v.setPlayerAndType(curPlayerNum, VertexType.SETTLEMENT)
						updatePlayerRoadsBlocked(vIndex)
						v.adjacentVerts.map {
							board.getRouteIndex(vIndex, it).takeIf { it>=0 }?.let {
								board.getRoute(it).player
							}
						}.filterNotNull().filter {
							it > 0
						}.distinct().forEach {
							with (getPlayerByPlayerNum(it)) {
								val len = board.computeMaxRouteLengthForPlayer(it, rules.isEnableRoadBlock)
								roadLength = len
							}
						}
						updateLongestRoutePlayer()
						checkForDiscoveredIsland(vIndex)
						checkUpdateHarborMaster(v, curPlayer)
					}
				}
				State.POSITION_ROAD_OR_SHIP_CANCEL,
				State.POSITION_ROAD_OR_SHIP_NOCANCEL -> {
					printinfo(getString("%s position road or ship", curPlayer.name))
					if (rules.isEnableSeafarersExpansion) {

						// this state reserved for choosing between roads or ships to place
						val shipOptions = computeShipRouteIndices(this, curPlayerNum, board)
						val roadOptions = computeRoadRouteIndices(curPlayerNum, board)
						if (shipOptions.isNotEmpty() && roadOptions.isNotEmpty()) {
							curPlayer.chooseRouteType(this)?.let { type ->
								val saveState = state
								popState()
								// allow player to back out to this menu to switch their choice if they want
								when (type) {
									RouteChoiceType.ROAD_CHOICE -> pushStateFront(State.POSITION_ROAD_CANCEL, null, roadOptions, object : UndoAction {
										override fun undo() {
											pushStateFront(saveState)
										}
									})
									RouteChoiceType.SHIP_CHOICE -> pushStateFront(State.POSITION_SHIP_CANCEL, null, shipOptions, object : UndoAction {
										override fun undo() {
											pushStateFront(saveState)
										}
									})
								}
							}
						} else if (shipOptions.isNotEmpty()) {
							popState()
							if (canCancel()) {
								pushStateFront(State.POSITION_SHIP_CANCEL)
							} else {
								pushStateFront(State.POSITION_SHIP_NOCANCEL)
							}
						} else if (roadOptions.isNotEmpty()) {
							popState()
							if (canCancel()) {
								pushStateFront(State.POSITION_ROAD_CANCEL)
							} else {
								pushStateFront(State.POSITION_ROAD_NOCANCEL)
							}
						} else {
							popState() // no road or ship choices?  hmmmmmmmm
						}
					} else {
						popState()
						pushStateFront(State.POSITION_ROAD_NOCANCEL)
					}
				}
				State.POSITION_NEUTRAL_ROAD_NOCANCEL,
				State.POSITION_ROAD_NOCANCEL,
				State.POSITION_ROAD_CANCEL -> {
					val options = stateOptions ?: when (state) {
						State.POSITION_NEUTRAL_ROAD_NOCANCEL -> {
							printinfo(getString("%s place road", curPlayer.name))
							computeRoadRouteIndices(curPlayerNum, board)
						}
						State.POSITION_ROAD_NOCANCEL,
						State.POSITION_ROAD_CANCEL -> {
							printinfo(getString("%s place road", curPlayer.name))
							computeRoadRouteIndices(curPlayerNum, board)
						}
						else -> throw AssertionError("Unhandled Case")
					}
					assert(!Utils.isEmpty(options))
					curPlayer.chooseRoute(this, options, RouteChoice.ROAD, null)?.let { edgeIndex ->
						Utils.assertContains(edgeIndex, options)
						onRouteChosen(curPlayerNum, RouteChoice.ROAD, edgeIndex, null)
						val edge = board.getRoute(edgeIndex)
						assert(edge.type === RouteType.OPEN)
						assert(edge.player == 0)
						assert(edge.isAdjacentToLand)
						printinfo(getString("%1\$s placing a road on edge %2\$s", curPlayer.name, edgeIndex))
						board.setPlayerForRoute(edge, curPlayerNum, RouteType.ROAD)
						popState()
						processRouteChange(curPlayer, edge)
					}
				}
				State.POSITION_SHIP_NOCANCEL, State.POSITION_SHIP_AND_LOCK_CANCEL, State.POSITION_SHIP_CANCEL -> {
					val shipToMove = getStateData<Int>()
					printinfo(getString("%s place ship", curPlayer.name))
					val options = stateOptions ?: computeShipRouteIndices(this, curPlayerNum, board)
					val shipType = if (shipToMove == null) RouteType.SHIP else board.getRoute(shipToMove).type
					assert(shipType.isVessel)
					assert(!Utils.isEmpty(options))
					curPlayer.chooseRoute(this, options, RouteChoice.SHIP, shipToMove)?.let { edgeIndex ->
						Utils.assertContains(edgeIndex, options)
						onRouteChosen(curPlayerNum, RouteChoice.SHIP, edgeIndex, shipToMove)
						popState()
						val edge = board.getRoute(edgeIndex)
						assert(edge.player == 0)
						assert(edge.isAdjacentToWater)
						edge.isLocked = true
						printinfo(getString("%1\$s placing a ship on edge %2\$s", curPlayer.name, edgeIndex))
						board.setPlayerForRoute(edge, curPlayerNum, shipType)
						if (shipToMove != null) {
							board.setRouteOpen(board.getRoute(shipToMove))
						}
						processRouteChange(curPlayer, edge)
						if (state === State.POSITION_SHIP_AND_LOCK_CANCEL) {
							for (toLock in board.getRoutesOfType(curPlayerNum, RouteType.SHIP, RouteType.WARSHIP)) {
								toLock.isLocked = true
							}
						}
					}
				}
				State.UPGRADE_SHIP_CANCEL -> {
					printinfo(getString("%s upgrade one of your ships", curPlayer.name))
					val options = stateOptions?: board.getRoutesIndicesOfType(curPlayerNum, RouteType.SHIP)
					assert(!Utils.isEmpty(options))
					curPlayer.chooseRoute(this, options, RouteChoice.UPGRADE_SHIP, null)?.let { edgeIndex ->
						Utils.assertContains(edgeIndex, options)
						onRouteChosen(curPlayerNum, RouteChoice.UPGRADE_SHIP, edgeIndex, null)
						val edge = board.getRoute(edgeIndex)
						assert(edge.player == curPlayerNum)
						edge.type = RouteType.WARSHIP
						popState()
						for (t in board.getRouteTiles(edge)) {
							if (t === board.getPirateTile()) {
								val opts = computePirateTileIndices(this, board)
								if (opts.isNotEmpty())
									pushStateFront(State.POSITION_PIRATE_NOCANCEL, null, opts)
							}
						}
						updateLargestArmyPlayer()
					}
				}
				State.CHOOSE_SHIP_TO_MOVE -> {
					printinfo(getString("%s choose ship to move", curPlayer.name))
					val options = stateOptions?: computeOpenRouteIndices(curPlayerNum, board, false, true)
					assert(!Utils.isEmpty(options))
					curPlayer.chooseRoute(this, options, RouteChoice.SHIP_TO_MOVE, null)?.let { shipIndex ->
						Utils.assertContains(shipIndex, options)
						onRouteChosen(curPlayerNum, RouteChoice.SHIP_TO_MOVE, shipIndex, null)
						val ship = board.getRoute(shipIndex)
						assert(ship.type.isVessel)
						assert(ship.player == curPlayerNum)
						popState()
						val saveType = ship.type
						ship.type = RouteType.OPEN
						val moveShipOptions = computeShipRouteIndices(this, curPlayerNum, board).toMutableList()
						moveShipOptions.remove(shipIndex as Any?)
						ship.type = saveType
						pushStateFront(State.POSITION_SHIP_AND_LOCK_CANCEL, shipIndex, moveShipOptions, object : UndoAction {
							override fun undo() {
								ship.type = saveType
							}
						})
					}
				}
				State.POSITION_CITY_CANCEL,
				State.POSITION_CITY_NOCANCEL -> {
					val options = stateOptions?: when (state) {
						State.POSITION_CITY_NOCANCEL -> computeSettlementVertexIndices(this, curPlayerNum, board)
						else -> computeCityVertxIndices(curPlayerNum, board)
					}
					printinfo(getString("%s place city", curPlayer.name))
					assert(!Utils.isEmpty(options))
					curPlayer.chooseVertex(this, options, VertexChoice.CITY, null)?.let { vIndex ->
						Utils.assertContains(vIndex, options)
						onVertexChosen(curPlayerNum, VertexChoice.CITY, vIndex, null)
						popState()
						val v = board.getVertex(vIndex)
						v.setPlayerAndType(curPlayerNum, VertexType.CITY)
						printinfo(getString("%1\$s placing a city at vertex %2\$s", curPlayer.name, vIndex))
						checkUpdateHarborMaster(v, curPlayer)

//						checkForOpeningStructure(vIndex);
					}
				}
				State.CHOOSE_CITY_FOR_WALL -> {
					printinfo(getString("%s choose city to protect with wall", curPlayer.name))
					val options = stateOptions ?: computeCityWallVertexIndices(curPlayerNum, board)
					assert(!Utils.isEmpty(options))
					curPlayer.chooseVertex(this, options, VertexChoice.CITY_WALL, null)?.let { vIndex ->
						Utils.assertContains(vIndex, options)
						onVertexChosen(curPlayerNum, VertexChoice.CITY_WALL, vIndex, null)
						popState()
						val v = board.getVertex(vIndex)
						printinfo(getString("%1\$s placing a city at vertex %2\$s", curPlayer.name, vIndex))
						v.setPlayerAndType(curPlayerNum, VertexType.WALLED_CITY)
					}
				}
				State.CHOOSE_METROPOLIS -> {
					printinfo(getString("%s choose city to upgrade to Metropolis", curPlayer.name))
					val options = stateOptions ?: computeMetropolisVertexIndices(curPlayerNum, board)
					assert(!Utils.isEmpty(options))
					val area = requireNotNull(getStateData<DevelopmentArea>())
					curPlayer.chooseVertex(this, options, area.choice, null)?.let { vIndex ->
						Utils.assertContains(vIndex, options)
						onVertexChosen(curPlayerNum, area.choice, vIndex, null)
						popState()
						val v = board.getVertex(vIndex)
						setMetropolisPlayer(area, curPlayerNum)
						printinfo(getString("%1\$s is building a %2\$s Metropolis", curPlayer.name, area))
						v.setPlayerAndType(curPlayerNum, area.vertexType)
					}
				}
				State.NEXT_PLAYER -> {
					incrementCurPlayer(1)
					popState()
				}
				State.PREV_PLAYER -> {
					incrementCurPlayer(-1)
					popState()
				}
				State.START_ROUND -> {
					// transition state
					printinfo(getString("Begin round"))
					assert(mStateStack.size == 1) // this should always be the start
					onShouldSaveGame()
					val moves: List<MoveType> = if (rules.isEnableEventCards) {
						Arrays.asList(MoveType.DEAL_EVENT_CARD)
					} else {
						if (curPlayer.getCardCount(ProgressCardType.Alchemist) > 0) {
							listOf(MoveType.ALCHEMIST_CARD, MoveType.ROLL_DICE)
						} else {
							listOf(MoveType.ROLL_DICE)
						}
					}
					pushStateFront(State.PLAYER_TURN_NOCANCEL, moves)
				}
				State.PROCESS_NEUTRAL_PLAYER -> {
					var moves: List<MoveType> = if (rules.isEnableEventCards) {
						Arrays.asList(MoveType.DEAL_EVENT_CARD)
					} else {
						Arrays.asList(MoveType.ROLL_DICE)
					}
					pushStateFront(State.PLAYER_TURN_NOCANCEL, moves)
				}
				State.INIT_PLAYER_TURN -> {
					// transition state
					// update any unusable cards in that players hand to be usable
					curPlayer.merchantFleetTradable?.let { t ->
						curPlayer.merchantFleetTradable = null
						putCardBackInDeck(curPlayer.removeCard(ProgressCardType.MerchantFleet)!!)
					}
					curPlayer.setCardsUsable(CardType.Development, true)

					// lock all player routes that are not open ended.
					// If we dont do this the AI will find too many move ship choices.
					for (r in board.getRoutesForPlayer(curPlayerNum)) {
						r.isLocked = !board.isRouteOpenEnded(r)
					}
					popState()
					pushStateFront(State.PLAYER_TURN_NOCANCEL)
				}
				State.PLAYER_TURN_NOCANCEL -> {
					// wait state
					printinfo(getString("%s choose move", curPlayer.name))
					var moves = getStateData<List<MoveType>>()
					if (Utils.isEmpty(moves)) {
						moves = computeMoves(curPlayer, board, this)
						log.debug("computeMoves: %s", moves)
					}
					assert(!Utils.isEmpty(moves))
					val move = curPlayer.chooseMove(this, moves)
					if (move != null) {
						Utils.assertContains(move, moves)
						processMove(move)
					}
				}
				State.SHOW_TRADE_OPTIONS -> {
					// wait state
					printinfo(getString("%s select trade option", curPlayer.name))
					val trades = getStateData() ?: computeTrades(curPlayer, board)
					assert(!Utils.isEmpty(trades))
					curPlayer.chooseTradeOption(this, trades)?.let { trade ->
						Utils.assertContains(trade, trades)
						printinfo(getString("%1\$s trades %2\$s X %3\$s", curPlayer.name, trade.getType(), trade.amount))
						curPlayer.incrementResource(trade.getType(), -trade.amount)
						onCardsTraded(curPlayerNum, trade)
						popState()
						val action: UndoAction = object : UndoAction {
							override fun undo() {
								curPlayer.incrementResource(trade.getType(), trade.amount)
							}
						}
						if (rules.isEnableCitiesAndKnightsExpansion) {
							pushStateFront(State.DRAW_RESOURCE_OR_COMMODITY_CANCEL, null, null, action)
						} else {
							pushStateFront(State.DRAW_RESOURCE_CANCEL, null, null, action)
						}
					}
				}
				State.POSITION_ROBBER_OR_PIRATE_CANCEL,
				State.POSITION_ROBBER_OR_PIRATE_NOCANCEL,
				State.POSITION_ROBBER_CANCEL,
				State.POSITION_ROBBER_NOCANCEL -> {
					val options = stateOptions ?: when (state) {
						State.POSITION_ROBBER_OR_PIRATE_CANCEL,
						State.POSITION_ROBBER_OR_PIRATE_NOCANCEL -> {
							printinfo(getString("%s place robber or pirate", curPlayer.name))
							computeRobberTileIndices(this, board).toMutableList().also {
								it.addAll(computePirateTileIndices(this, board))
							}
						}
						State.POSITION_ROBBER_CANCEL,
						State.POSITION_ROBBER_NOCANCEL           -> {
							printinfo(getString("%s place robber", curPlayer.name))
							computeRobberTileIndices(this, board)
						}
						else                                     -> throw AssertionError("Unhandled case")
					}
					if (options.isEmpty()) {
						board.setRobber(-1)
						board.setPirate(-1)
						popState()
					} else {
						curPlayer.chooseTile(this, options, TileChoice.ROBBER)?.let { cellIndex ->
							Utils.assertContains(cellIndex, options)
							popState()
							val cell = board.getTile(cellIndex)
							if (cell.isWater) {
								printinfo(getString("%1\$s placing robber on cell %2\$s", curPlayer.name, cellIndex))
								board.setPirate(cellIndex)
								pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM, null, computeRobberTakeOpponentCardOptions(curPlayer, board, true), null)
							} else {
								printinfo(getString("%1\$s placing robber on cell %2\$s", curPlayer.name, cellIndex))
								board.setRobber(cellIndex)
								pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM, null, computeRobberTakeOpponentCardOptions(curPlayer, board, false), null)
							}
						}
					}
				}
				State.POSITION_PIRATE_CANCEL, State.POSITION_PIRATE_NOCANCEL -> {
					// wait state
					printinfo(getString("%s place pirate", curPlayer.name))
					val options = stateOptions?:computePirateTileIndices(this, board)
					assert(!Utils.isEmpty(options))
					val cellIndex = curPlayer.chooseTile(this, options, TileChoice.PIRATE)
					if (cellIndex != null) {
						Utils.assertContains(cellIndex, options)
						popState()
						val cell = board.getTile(cellIndex)
						if (cell.isWater) {
							printinfo(getString("%1\$s placing pirate on cell %2\$s", curPlayer.name, cellIndex))
							board.setPirate(cellIndex)
							pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM, null, computeRobberTakeOpponentCardOptions(curPlayer, board, true), null)
						} else {
							printinfo(getString("%1\$s placing robber on cell %2\$s", curPlayer.name, cellIndex))
							board.setRobber(cellIndex)
							pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM, null, computeRobberTakeOpponentCardOptions(curPlayer, board, false), null)
						}
					}
				}
				State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM -> {
					printinfo(getString("%s choose opponent to take card from", curPlayer.name))
					val options = requireNotNull(stateOptions)
					if (options.isEmpty()) {
						popState()
					} else {
						val playerNum = curPlayer.choosePlayer(this, options, PlayerChoice.PLAYER_TO_TAKE_CARD_FROM)
						if (playerNum != null) {
							Utils.assertContains(playerNum, options)
							val player = getPlayerByPlayerNum(playerNum)
							assert(player !== curPlayer)
							assert(player.playerNum > 0)
							takeOpponentCard(curPlayer, player)
							popState()
						}
					}
				}
				State.CHOOSE_OPPONENT_FOR_GIFT_CARD -> {
					printinfo(getString("%s choose opponent for gift", curPlayer.name))
					val options = requireNotNull(stateOptions)
					curPlayer.choosePlayer(this, options, PlayerChoice.PLAYER_TO_GIFT_CARD)?.let { playerNum ->
						Utils.assertContains(playerNum, stateOptions)
						val player = getPlayerByPlayerNum(playerNum)
						val cards = getStateExtraOptions<Card>()!!
						assert(!Utils.isEmpty(cards))
						popState()
						pushStateFront(State.CHOOSE_GIFT_CARD, player, null, cards, null)
					}
				}
				State.CHOOSE_RESOURCE_MONOPOLY -> {
					// wait state
					printinfo(getString("%s choose resource to monopolize", curPlayer.name))
					curPlayer.chooseEnum(this, EnumChoice.RESOURCE_MONOPOLY, ResourceType.values())?.let { type ->
						processMonopoly(type)
						popState()
					}
				}
				State.SETUP_GIVEUP_CARDS -> {
					popState()
					pushStateFront(State.NEXT_PLAYER)
					pushStateFront(State.SET_PLAYER, curPlayerNum)
					players.forEach { cur ->
						val numCards = cur.totalCardsLeftInHand
						if (numCards > rules.getMaxSafeCardsForPlayer(cur.playerNum, board)) {
							val numCardsToSurrender = numCards / 2
							printinfo(getString("%1\$s must give up %2\$d of %3\$d cards", cur.name, numCardsToSurrender, numCards))
							var i = 0
							while (i < numCardsToSurrender) {
								// (numCardsToSurrender > 0)
								pushStateFront(State.GIVE_UP_CARD, numCardsToSurrender)
								i++
							}
							pushStateFront(State.SET_PLAYER, cur.playerNum)
						}
					}
				}
				State.GIVE_UP_CARD -> {
					// wait state
					val numToGiveUp = getStateData<Int>()
					val cards = getStateExtraOptions()?:curPlayer.unusedCards
					assert(!Utils.isEmpty(cards))
					printinfo(getString("%1\$s Give up one of %2\$d cards", curPlayer.name, numToGiveUp))
					curPlayer.chooseCard(this, cards, CardChoice.GIVEUP_CARD)?.let { card ->
						Utils.assertContains(card, cards)
						curPlayer.removeCard(card)
						popState()
					}
				}
				State.DRAW_RESOURCE_OR_COMMODITY_NOCANCEL,
				State.DRAW_RESOURCE_OR_COMMODITY_CANCEL,
				State.DRAW_RESOURCE_NOCANCEL,
				State.DRAW_RESOURCE_CANCEL-> {
					val cards = ResourceType.values().map { Card(it, CardStatus.USABLE) }.toMutableList()
					when (state) {
						State.DRAW_RESOURCE_OR_COMMODITY_NOCANCEL,
						State.DRAW_RESOURCE_OR_COMMODITY_CANCEL -> {
							cards.addAll(CommodityType.values().map { Card(it, CardStatus.USABLE) })
							printinfo(getString("%s draw a resource or commodity", curPlayer.name))
						}
						else -> printinfo(getString("%s draw a resource", curPlayer.name))
					}
					curPlayer.chooseCard(this, cards, Player.CardChoice.RESOURCE_OR_COMMODITY)?.let { card ->
						Utils.assertContains(card, cards)
						printinfo(getString("%1\$s draws a %2\$s card", curPlayer.name, card.name))
						onCardPicked(curPlayerNum, card)
						curPlayer.addCard(card)
						popState()
					}
				}
				State.SET_PLAYER -> {
					val playerNum = getStateData<Int>()
					log.debug("Setting player to $playerNum")
					setCurrentPlayer(playerNum)
					popState()
				}
				State.SET_VERTEX_TYPE -> {
					val data = getStateData<Array<Any>>()
					val vIndex = data[0] as Int
					val type = data[1] as VertexType
					board.getVertex(vIndex).setPlayerAndType(curPlayerNum, type)
					popState()
				}
				State.CHOOSE_KNIGHT_TO_ACTIVATE -> {
					printinfo(getString("%s choose knight to activate", curPlayer.name))
					val options = stateOptions?:board.getVertIndicesOfType(curPlayerNum, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE)
					curPlayer.chooseVertex(this, options, VertexChoice.KNIGHT_TO_ACTIVATE, null)?.let { vIndex ->
						Utils.assertContains(vIndex, options)
						onVertexChosen(curPlayerNum, VertexChoice.KNIGHT_TO_ACTIVATE, vIndex, null)
						val v = board.getVertex(vIndex)
						curPlayer.adjustResourcesForBuildable(BuildableType.ActivateKnight, -1)
						v.activateKnight()
						popState()
					}
				}
				State.CHOOSE_KNIGHT_TO_PROMOTE -> {
					printinfo(getString("%s choose knight to promote", curPlayer.name))
					val options = stateOptions?:computePromoteKnightVertexIndices(curPlayer, board)
					curPlayer.chooseVertex(this, options, VertexChoice.KNIGHT_TO_PROMOTE, null)?.let { vIndex ->
						Utils.assertContains(vIndex, options)
						onVertexChosen(curPlayerNum, VertexChoice.KNIGHT_TO_PROMOTE, vIndex, null)
						val v = board.getVertex(vIndex)
						assert(v.isKnight)
						onPlayerKnightPromoted(curPlayerNum, vIndex)
						v.setPlayerAndType(curPlayerNum, v.type.promotedType())
						popState()
					}
				}
				State.CHOOSE_KNIGHT_TO_MOVE -> {
					printinfo(getString("%s choose knight to move", curPlayer.name))
					val options = requireNotNull(stateOptions)
					assert(!Utils.isEmpty(options))
					curPlayer.chooseVertex(this, options, VertexChoice.KNIGHT_TO_MOVE, null)?.let { vIndex ->
						Utils.assertContains(vIndex, options)
						onVertexChosen(curPlayerNum, VertexChoice.KNIGHT_TO_MOVE, vIndex, null)
						popState()
						pushStateFront(State.POSITION_KNIGHT_CANCEL, vIndex, computeKnightMoveVertexIndices(this, vIndex, board))
					}
				}
				State.POSITION_DISPLACED_KNIGHT -> {
					printinfo(getString("%s position your displaced knight", curPlayer.name))
					val knight = getStateData<Vertex>()
					val displacedKnight = knight.type
					val knightIndex = board.getVertexIndex(knight)
					assert(displacedKnight.isKnight)
					val options = requireNotNull(stateOptions)
					assert(!Utils.isEmpty(options))
					curPlayer.chooseVertex(this, options, VertexChoice.KNIGHT_DISPLACED, knightIndex)?.let { vIndex ->
						Utils.assertContains(vIndex, options)
						onVertexChosen(curPlayerNum, VertexChoice.KNIGHT_DISPLACED, vIndex, knightIndex)
						val v = board.getVertex(vIndex)
						knight.setOpen()
						v.setPlayerAndType(curPlayerNum, displacedKnight)
						popState()
					}
				}
				State.POSITION_NEW_KNIGHT_CANCEL, State.POSITION_KNIGHT_NOCANCEL, State.POSITION_KNIGHT_CANCEL -> {
					printinfo(getString("%s position knight", curPlayer.name))
					val options = requireNotNull(stateOptions)
					assert(!Utils.isEmpty(options))
					val sourceKnight = getStateData<Int?>()
					var knight = sourceKnight?.let {
						board.getVertex(sourceKnight).type
					}?:VertexType.BASIC_KNIGHT_INACTIVE
					assert(knight.isKnight)
					val choice = if (state === State.POSITION_NEW_KNIGHT_CANCEL) VertexChoice.NEW_KNIGHT else VertexChoice.KNIGHT_MOVE_POSITION
					curPlayer.chooseVertex(this, options, choice, sourceKnight)?.let { vIndex ->
						Utils.assertContains(vIndex, options)
						onVertexChosen(curPlayerNum, choice, vIndex, sourceKnight)
						if (sourceKnight != null) {
							board.getVertex(sourceKnight).setOpen()
						}
						val v = board.getVertex(vIndex)
						popState()

						// see if we are chasing away the robber/pirate
						var i = 0
						while (i < v.numTiles) {
							val tIndex = v.getTile(i)
							if (tIndex == board.robberTileIndex) {
								printinfo(getString("%s has chased away the robber!", curPlayer.name))
								pushStateFront(State.POSITION_ROBBER_NOCANCEL)
							} else if (tIndex == board.pirateTileIndex) {
								printinfo(getString("%s has chased away the pirate!", curPlayer.name))
								pushStateFront(State.POSITION_PIRATE_NOCANCEL)
							}
							i++
						}
						if (v.player != 0) {
							assert(v.isKnight)
							assert(v.type.knightLevel < knight.knightLevel)
							val options = computeDisplacedKnightVertexIndices(this, vIndex, board)
							if (options.isNotEmpty()) {
								pushStateFront(State.SET_VERTEX_TYPE, arrayOf(vIndex, knight))
								pushStateFront(State.SET_PLAYER, curPlayerNum)
								pushStateFront(State.POSITION_DISPLACED_KNIGHT, v, options)
								pushStateFront(State.SET_PLAYER, v.player)
								return@let // exit out early. SET_VERTEX_TYPE will assign after displaced knight is moved.
							}
						} else {
							assert(v.type === VertexType.OPEN)
						}
						v.setPlayerAndType(curPlayerNum, knight)
						updatePlayerRoadsBlocked(vIndex)
					}
				}
				State.CHOOSE_PROGRESS_CARD_TYPE -> {
					printinfo(getString("%s draw a progress card", curPlayer.name))
					val area = curPlayer.chooseEnum(this, EnumChoice.DRAW_PROGRESS_CARD, DevelopmentArea.values())
					if (area != null && mProgressCards[area.ordinal].size > 0) {
						val dealt = mProgressCards[area.ordinal].removeAt(0)
						curPlayer.addCard(dealt)
						onCardPicked(curPlayerNum, dealt)
						popState()
					}
				}
				State.CHOOSE_CITY_IMPROVEMENT -> {
					printinfo(getString("%s choose development area", curPlayer.name))
					val ops = getStateExtraOptions<DevelopmentArea>()!!
					assert(!Utils.isEmpty(ops))
					val areas = ops.toTypedArray()
					val area = curPlayer.chooseEnum(this, EnumChoice.CRANE_CARD_DEVELOPEMENT, areas)
					if (area != null) {
						Utils.assertContains(area, Arrays.asList(*areas))
						popState()
						processCityImprovement(curPlayer, area, 1)
					}
				}
				State.CHOOSE_KNIGHT_TO_DESERT -> {
					printinfo(getString("%s choose a knight for desertion", curPlayer.name))
					val options = requireNotNull(stateOptions)
					assert(!Utils.isEmpty(options))
					curPlayer.chooseVertex(this, options, VertexChoice.KNIGHT_DESERTER, null)?.let { vIndex ->
						Utils.assertContains(vIndex, options)
						onVertexChosen(curPlayerNum, VertexChoice.KNIGHT_DESERTER, vIndex, null)
						val newPlayerNum = getStateData<Int>()
						popState()
						computeNewKnightVertexIndices(newPlayerNum, board).takeIf { it.isNotEmpty() }?.let { options ->
							pushStateFront(State.POSITION_KNIGHT_NOCANCEL, vIndex, options)
						}
						pushStateFront(State.SET_PLAYER, newPlayerNum)
					}
				}
				State.CHOOSE_PLAYER_FOR_DESERTION -> {
					printinfo(getString("%s choose player for desertion", curPlayer.name))
					val options = requireNotNull(stateOptions)
					assert(!Utils.isEmpty(options))
					val playerNum = curPlayer.choosePlayer(this, options, PlayerChoice.PLAYER_FOR_DESERTION)
					if (playerNum != null) {
						Utils.assertContains(playerNum, options)
						popState()
						val knights = board.getKnightsForPlayer(playerNum)
						assert(knights.isNotEmpty())
						putCardBackInDeck(curPlayer.removeCard(ProgressCardType.Deserter)!!)
						pushStateFront(State.CHOOSE_KNIGHT_TO_DESERT, curPlayerNum, knights)
						pushStateFront(State.SET_PLAYER, playerNum)
					}
				}
				State.CHOOSE_DIPLOMAT_ROUTE -> {
					printinfo(getString("%s choose diplomat route", curPlayer.name))
					val options = requireNotNull(stateOptions)
					assert(!Utils.isEmpty(options))
					curPlayer.chooseRoute(this, options, RouteChoice.ROUTE_DIPLOMAT, null)?.let { rIndex ->
						Utils.assertContains(rIndex, options)
						onRouteChosen(curPlayerNum, RouteChoice.ROUTE_DIPLOMAT, rIndex, null)
						val r = board.getRoute(rIndex)
						popState()
						curPlayer.removeCard(ProgressCardType.Diplomat)?.let { card ->
							putCardBackInDeck(card)
							if (r.player == curPlayerNum) {
								board.setRouteOpen(r)
								val options = computeRoadRouteIndices(curPlayerNum, board).toMutableList().also {
									it.remove(board.getRouteIndex(r) as Any) // remove the edge we just removed
								}
								pushStateFront(State.POSITION_ROAD_CANCEL, null, options, object : UndoAction {
									override fun undo() {
										board.setPlayerForRoute(r, curPlayerNum, RouteType.ROAD)
										processRouteChange(curPlayer, r)
										removeCardFromDeck(card)
										curPlayer.addCard(card)
									}
								})
							} else {
								val playerNum = r.player
								board.setRouteOpen(r)
								processRouteChange(getPlayerByPlayerNum(playerNum), r)
							}
						}
					}
				}
				State.CHOOSE_HARBOR_PLAYER -> {
					val options = requireNotNull(stateOptions).toMutableList()
					if (Utils.isEmpty(options) || curPlayer.getCardCount(CardType.Resource) == 0) {
						popState()
					} else {
						printinfo(getString("%s choose player for harbor trade", curPlayer.name))
						val playerNum = curPlayer.choosePlayer(this, options, PlayerChoice.PLAYER_TO_FORCE_HARBOR_TRADE)
						if (playerNum != null) {
							Utils.assertContains(playerNum, options)
							val p = getPlayerByPlayerNum(playerNum)
							getStateData<Card?>()?.let { card ->
								curPlayer.removeCard(card)
								putCardBackInDeck(card)
							}
							options.remove(playerNum)
							popState()
							//						pushStateFront(State.CHOOSE_HARBOR_PLAYER, null, options);
							pushStateFront(State.CHOOSE_HARBOR_RESOURCE, p, options)
						}
					}
				}
				State.CHOOSE_HARBOR_RESOURCE -> {
					printinfo(getString("%s choose a harbor resource", curPlayer.name))
					val resourceCards: List<Card> = curPlayer.getCards(CardType.Resource)
					curPlayer.chooseCard(this, resourceCards, Player.CardChoice.EXCHANGE_CARD)?.let { card ->
						Utils.assertContains(card, resourceCards)
						val exchanging = getStateData<Player>()
						popState()
						printinfo(getString("%1\$s gives a %2\$s to %3\$s", curPlayer.name, card.name, exchanging.name))
						val exchanged = curPlayer.removeCard(card)!!
						onCardLost(curPlayerNum, exchanged)
						exchanging.addCard(exchanged)
						onCardPicked(exchanging.playerNum, exchanged)
						pushStateFront(State.SET_PLAYER, curPlayerNum)
						pushStateFront(State.EXCHANGE_CARD, curPlayer, null, exchanging.getCards(CardType.Commodity), null)
						pushStateFront(State.SET_PLAYER, exchanging.playerNum)
					}
				}
				State.EXCHANGE_CARD -> {
					printinfo(getString("%s choose card for exchange", curPlayer.name))
					val cards = getStateExtraOptions<Card>()!!
					assert(!Utils.isEmpty(cards))
					curPlayer.chooseCard(this, cards, CardChoice.EXCHANGE_CARD)?.let { card ->
						Utils.assertContains(card, cards)
						curPlayer.removeCard(card)
						onCardLost(curPlayerNum, card)
						val exchanging = getStateData<Player>()
						exchanging.addCard(card)
						onCardPicked(exchanging.playerNum, card)
						popState()
					}
				}
				State.CHOOSE_OPPONENT_KNIGHT_TO_DISPLACE -> {
					printinfo(getString("%s choose opponent knight to displace", curPlayer.name))
					val options = requireNotNull(stateOptions)
					assert(!Utils.isEmpty(options))
					curPlayer.chooseVertex(this, options, VertexChoice.OPPONENT_KNIGHT_TO_DISPLACE, null)?.let { vIndex ->
						Utils.assertContains(vIndex, options)
						onVertexChosen(curPlayerNum, VertexChoice.OPPONENT_KNIGHT_TO_DISPLACE, vIndex, null)
						val v = board.getVertex(vIndex)
						with (computeDisplacedKnightVertexIndices(this, vIndex, board).toMutableList()) {
							remove(vIndex as Any?)
							putCardBackInDeck(curPlayer.removeCard(ProgressCardType.Intrigue)!!)
							popState()
							if (isNotEmpty()) {
								pushStateFront(State.SET_PLAYER, curPlayerNum)
								pushStateFront(State.POSITION_DISPLACED_KNIGHT, v, this)
								pushStateFront(State.SET_PLAYER, v.player)
							} else {
								v.setOpen()
							}
						}
					}
				}
				State.CHOOSE_TILE_INVENTOR -> {
					printinfo(getString("%s choose tile", curPlayer.name))
					val options = requireNotNull(stateOptions).toMutableList()
					assert(!Utils.isEmpty(options))
					curPlayer.chooseTile(this, options, TileChoice.INVENTOR)?.let { tileIndex ->
						Utils.assertContains(tileIndex, options)
						getStateData<Int?>()?.also { firstTileIndex ->
							// swap em
							popState()
							val secondTile = board.getTile(tileIndex)
							val firstTile = board.getTile(firstTileIndex)
							val t = firstTile.dieNum
							firstTile.dieNum = secondTile.dieNum
							secondTile.dieNum = t
							onTilesInvented(curPlayerNum, firstTileIndex, tileIndex)
							putCardBackInDeck(curPlayer.removeCard(ProgressCardType.Inventor)!!)
						}?:run {
							popState()
							options.remove(tileIndex)
							pushStateFront(State.CHOOSE_TILE_INVENTOR, tileIndex, options)
						}
					}
				}
				State.CHOOSE_PLAYER_MASTER_MERCHANT -> {
					printinfo(getString("%s choose player to take a card from", curPlayer.name))
					val options = requireNotNull(stateOptions)
					assert(!Utils.isEmpty(options))
					curPlayer.choosePlayer(this, options, PlayerChoice.PLAYER_TO_TAKE_CARD_FROM)?.let { playerNum ->
						Utils.assertContains(playerNum, options)
						val p = getPlayerByPlayerNum(playerNum)
						putCardBackInDeck(curPlayer.removeCard(ProgressCardType.MasterMerchant)!!)
						val cards = p.getCards(CardType.Commodity).toMutableList()
						cards.addAll(p.getCards(CardType.Resource))
						popState()
						var i = 0
						while (i < Math.min(2, cards.size)) {
							pushStateFront(State.TAKE_CARD_FROM_OPPONENT, p)
							i++
						}
					}
				}
				State.TAKE_CARD_FROM_OPPONENT -> {
					printinfo(getString("%s draw a card from opponents hand", curPlayer.name))
					val p = getStateData<Player>()
					var cards = getStateExtraOptions<Card>()?:listOf()
					if (Utils.isEmpty(cards)) {
						cards = p.getCards(CardType.Commodity).toMutableList().also {
							it.addAll(p.getCards(CardType.Resource))
						}
					}
					curPlayer.chooseCard(this, cards, Player.CardChoice.OPPONENT_CARD)?.let { c->
						Utils.assertContains(c, cards)
						p.removeCard(c)
						curPlayer.addCard(c)
						onTakeOpponentCard(curPlayerNum, p.playerNum, c)
						popState()
					}
				}
				State.POSITION_MERCHANT -> {
					printinfo(getString("%s position the merchant", curPlayer.name))
					val options = computeMerchantTileIndices(this, curPlayerNum, board)
					assert(!Utils.isEmpty(options))
					curPlayer.chooseTile(this, options, TileChoice.MERCHANT)?.let { tIndex ->
						Utils.assertContains(tIndex, options)
						if (board.merchantPlayer > 0) {
							val p = getPlayerByPlayerNum(board.merchantPlayer)
							val c = p.removeCard(SpecialVictoryType.Merchant)!!
							onCardLost(p.playerNum, c)
						}
						putCardBackInDeck(curPlayer.removeCard(ProgressCardType.Merchant)!!)
						board.setMerchant(tIndex, curPlayerNum)
						curPlayer.addCard(SpecialVictoryType.Merchant)
						onSpecialVictoryCard(curPlayerNum, SpecialVictoryType.Merchant)
						popState()
					}
				}
				State.CHOOSE_RESOURCE_FLEET -> {
					printinfo(getString("%s choose card type for trade", curPlayer.name))
					val cards = getStateExtraOptions<Card>()!!
					assert(!Utils.isEmpty(cards))
					curPlayer.chooseCard(this, cards, CardChoice.FLEET_TRADE)?.let { c->
						Utils.assertContains(c, cards)
						curPlayer.getCard(ProgressCardType.MerchantFleet)!!.setUsed()
						curPlayer.merchantFleetTradable = c
						popState()
					}
				}
				State.CHOOSE_PLAYER_TO_SPY_ON -> {
					printinfo(getString("%s choose player to spy on", curPlayer.name))
					val options = requireNotNull(stateOptions)
					assert(!Utils.isEmpty(options))
					curPlayer.choosePlayer(this, options, PlayerChoice.PLAYER_TO_SPY_ON)?.let { playerNum ->
						Utils.assertContains(playerNum, options)
						val p = getPlayerByPlayerNum(playerNum)
						putCardBackInDeck(curPlayer.removeCard(ProgressCardType.Spy)!!)
						popState()
						p.getUnusedCards(CardType.Progress).takeIf { it.isNotEmpty() }?.let { cards ->
							pushStateFront(State.TAKE_CARD_FROM_OPPONENT, p, null, cards, null)
						}
					}
				}
				State.CHOOSE_TRADE_MONOPOLY -> {
					printinfo(getString("%s choose commodity for monopoly", curPlayer.name))
					val c = curPlayer.chooseEnum(this, EnumChoice.COMMODITY_MONOPOLY, CommodityType.values())
					if (c != null) {
						popState()
						putCardBackInDeck(curPlayer.removeCard(ProgressCardType.TradeMonopoly)!!)
						for (p in players) {
							if (p !== curPlayer) {
								val num = p.getCardCount(c)
								if (num > 0) {
									p.removeCards(c, 1)
									curPlayer.incrementResource(c, 1)
									onTakeOpponentCard(curPlayerNum, p.playerNum, Card(c, CardStatus.USABLE))
								}
							}
						}
					}
				}
				State.CHOOSE_GIFT_CARD -> {
					printinfo(getString("%s choose card to give up", curPlayer.name))
					var cards = getStateExtraOptions()?:computeGiftCards(this, curPlayer)
					curPlayer.chooseCard(this, cards, Player.CardChoice.GIVEUP_CARD)?.let { c ->
						Utils.assertContains(c, cards)
						val taker = getStateData<Player>()
						curPlayer.removeCard(c)
						taker.addCard(c)
						printinfo(getString("%1\$s gives a %2\$s card to %3\$s", curPlayer.name, c.name, taker.name))
						onTakeOpponentCard(taker.playerNum, curPlayerNum, c)
						popState()
					}
				}
				State.CHOOSE_ROAD_TO_ATTACK -> {
					assert(rules.knightScoreToDestroyRoad > 0)
					printinfo(getString("%s choose road to attack", curPlayer.name))
					val options = takeIfNotEmpty(stateOptions)?:computeAttackableRoads(this, curPlayerNum, board)
					assert(!Utils.isEmpty(options))
					curPlayer.chooseRoute(this, options, RouteChoice.OPPONENT_ROAD_TO_ATTACK, null)?.let { rIndex ->
						Utils.assertContains(rIndex, options)
						onRouteChosen(curPlayerNum, RouteChoice.OPPONENT_ROAD_TO_ATTACK, rIndex, null)
						val r = board.getRoute(rIndex)
						assert(r.player > 0)
						assert(r.type.isRoad)
						popState()
						pushStateFront(State.ROLL_DICE_ATTACK_ROAD, r)
					}
				}
				State.ROLL_DICE_ATTACK_ROAD -> {
					val r = requireNotNull(getStateData<Route>())
					val victim = getPlayerByPlayerNum(r.player)
					printinfo(getString("%1\$s is attacking %2\$s's road", curPlayer.name, victim.name))
					pushDiceConfig(DiceType.WhiteBlack)
					rollDice()
					val routeIndex = board.getRouteIndex(r)
					val info = computeAttackRoad(routeIndex, this, board, curPlayerNum)
					//score += computeAttackerScoreAgainstRoad(r, getCurPlayerNum(), getBoard());
					onPlayerAttackingOpponent(curPlayerNum, victim.playerNum, getString("Road"), info.knightStrength + getDie(0).num, info.minScore)
					if (getDie(0).num > info.minScore - info.knightStrength) {
						when (info.destroyedType) {
							RouteType.DAMAGED_ROAD -> {
								printinfo(getString("%s has damaged the road", curPlayer.name))
								onRoadDamaged(routeIndex, curPlayerNum, victim.playerNum)
								r.type = RouteType.DAMAGED_ROAD
							}
							RouteType.OPEN         -> {
								printinfo(getString("%s has destroyed the road", curPlayer.name))
								onRoadDestroyed(routeIndex, curPlayerNum, victim.playerNum)
								board.setRouteOpen(r)
								processRouteChange(victim, r)
							}
							else                   -> assert(false)
						}
						updateLongestRoutePlayer()
					} else {
						printinfo(getString("%s failed to destroy the road", curPlayer.name))
					}
					popDiceConfig()
					popState()
				}
				State.CHOOSE_STRUCTURE_TO_ATTACK -> {
					printinfo(getString("%s choose structure to attack", curPlayer.name))
					val options = takeIfNotEmpty(stateOptions)?:computeAttackableStructures(this, curPlayerNum, board)
					curPlayer.chooseVertex(this, options, VertexChoice.OPPONENT_STRUCTURE_TO_ATTACK, null)?.let { vIndex ->
						Utils.assertContains(vIndex, options)
						onVertexChosen(curPlayerNum, VertexChoice.OPPONENT_STRUCTURE_TO_ATTACK, vIndex, null)
						val v = board.getVertex(vIndex)
						popState()
						pushStateFront(State.ROLL_DICE_ATTACK_STRUCTURE, v)
					}
				}
				State.ROLL_DICE_ATTACK_STRUCTURE -> {

					// find all active knights adjacent to structure (may need to be on own road)
					// subtract their combined levels from the min roll needed for successful attack
					// if the die roll is > then this value, then the structure gets reduced by a level
					// if die roll <= to value then all knights are reduced by a level
					val v = getStateData<Vertex>()
					val vIndex = board.getVertexIndex(v)
					assert(v != null)
					val victim = getPlayerByPlayerNum(v.player)
					val info = computeStructureAttack(vIndex, this, board, curPlayerNum)
					printinfo(getString("%1\$s is attacking %2\$s's %3\$s with knight strength %4\$s", curPlayer.name, victim.name, v.type.name, info.knightStrength))
					pushDiceConfig(DiceType.WhiteBlack)
					rollDice()
					onPlayerAttackingOpponent(curPlayerNum, victim.playerNum, v.type.name, info.knightStrength + getDie(0).num, info.minScore)
					val diff = getDie(0).num + info.knightStrength - info.minScore
					if (diff > 0) {
						// knights win
						if (info.destroyedType === VertexType.OPEN) {
							printinfo(getString("%s's Settlement destroyed!", victim.name))
							v.setOpen()
							updatePlayerRoadsBlocked(vIndex)
						} else {
							printinfo(getString("%1\$s's %2\$s reduced to %3\$s", victim.name, v.type.name, info.destroyedType.name))
							v.setPlayerAndType(v.player, info.destroyedType)
						}
					} else if (diff == 0) {
						// draw no change on either side
						printinfo(getString("Attack was a draw"))
					} else {
						// victim wins
						printinfo(getString("%s defends themselves from the attack!", victim.name))
						for (kIndex in info.attackingKnights) {
							val knight = board.getVertex(kIndex)
							when (knight.type) {
								VertexType.BASIC_KNIGHT_INACTIVE                                     -> {
									printinfo(getString("%s's knight lost!", curPlayer.name))
									onPlayerKnightDestroyed(curPlayerNum, kIndex)
									knight.setOpen()
								}
								VertexType.MIGHTY_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_INACTIVE -> {
									printinfo(getString("%s's knight demoted!", curPlayer.name))
									onPlayerKnightDemoted(curPlayerNum, kIndex)
									knight.demoteKnight()
								}
								else                                                                 -> assert(false) // unhandled case
							}
						}
					}
					popDiceConfig()
					popState()
				}
				State.CHOOSE_SHIP_TO_ATTACK -> {
					printinfo(getString("%s choose ship to attack", curPlayer.name))
					val options = takeIfNotEmpty(stateOptions)?:computeAttackableShips(this, curPlayerNum, board)
					curPlayer.chooseRoute(this, options, RouteChoice.OPPONENT_SHIP_TO_ATTACK, null)?.let { rIndex ->
						Utils.assertContains(rIndex, options)
						onRouteChosen(curPlayerNum, RouteChoice.OPPONENT_SHIP_TO_ATTACK, rIndex, null)
						val r = board.getRoute(rIndex)
						popState()
						pushStateFront(State.ROLL_DICE_ATTACK_SHIP, r)
					}
				}
				State.ROLL_DICE_ATTACK_SHIP -> {
					val attacking = getStateData<Route>()
					assert(attacking != null)
					pushDiceConfig(DiceType.WhiteBlack)
					val victim = getPlayerByPlayerNum(attacking.player)
					val dieToWin = computeDiceToWinAttackShip(board, curPlayerNum, attacking.player)
					printinfo(getString("%1\$s needs a %2\$s or better to win.", curPlayer.name, dieToWin))
					rollDice()
					val score = getDie(0).num
					onPlayerAttackingOpponent(curPlayerNum, victim.playerNum, getString("Ship"), score, dieToWin - 1)
					if (score >= dieToWin) {
						printinfo(getString("%1\$s has attacked %2\$s and commandeered their ship!", curPlayer.name, getPlayerByPlayerNum(attacking.player).name))
						onPlayerShipComandeered(curPlayerNum, board.getRouteIndex(attacking))
						board.setPlayerForRoute(attacking, curPlayerNum, attacking.type)
					} else {
						for (i in board.getRouteIndicesAdjacentToRoute(attacking)) {
							val r = board.getRoute(i)
							if (r.player == curPlayerNum && r.type === RouteType.WARSHIP) {
								printinfo(getString("%1\$s has defended their ship from %2\$s's attack.  Warship destroyed.", getPlayerByPlayerNum(attacking.player).name, curPlayer.name))
								onPlayerShipDestroyed(i)
								board.setRouteOpen(r)
								break
							}
						}
					}
					popDiceConfig()
					popState()
				}
				State.CLEAR_FORCED_SETTLEMENTS -> {
					for (vIndex in board.getVertIndicesOfType(0, VertexType.OPEN_SETTLEMENT)) board.getVertex(vIndex).setOpen()
					popState()
				}
			}
		} finally {
			//if (Profiler.ENABLED) Profiler.pop("SOC::runGame[" + state + "]");
			//if (Profiler.ENABLED) Profiler.pop("SOC::runGame");
		}
	}

	class AttackInfo<T>(val attackingKnights: List<Int>, // which knights involved in the attack.  They will have been set to inactive post processing
		val knightStrength: Int,
		val minScore: Int,
		val destroyedType: T
		)

	/**
	 * @param attackerNum
	 * @param victimNum
	 * @param attackingWhat
	 * @param attackerScore
	 * @param victimScore
	 */
	protected open fun onPlayerAttackingOpponent(attackerNum: Int, victimNum: Int, attackingWhat: String, attackerScore: Int, victimScore: Int) {}

	/**
	 * @param routeIndex
	 * @param destroyerNum
	 * @param victimNum
	 */
	protected open fun onRoadDestroyed(routeIndex: Int, destroyerNum: Int, victimNum: Int) {}

	/**
	 * @param routeIndex
	 * @param destroyerNum
	 * @param victimNum
	 */
	protected open fun onRoadDamaged(routeIndex: Int, destroyerNum: Int, victimNum: Int) {}

	/**
	 * Called when a player structure has been attacked and as a result has lost a level.  Settlements get demoted to open vertex.
	 *
	 * @param vIndex
	 * @param newType
	 * @param destroyerNum
	 * @param victimNum
	 */
	protected open fun onStructureDemoted(vIndex: Int, newType: VertexType, destroyerNum: Int, victimNum: Int) {}

	/*
	static int processAttackStructure(Vertex v, int attackerNum, Board b) {
		int score = 0;
		for (int i=0; i<v.getNumAdjacentVerts(); i++) {
			int vIndex = v.getAdjacentVerts()[i];
			Vertex v2 = b.getVertex(vIndex);
			if (v2.isActiveKnight()) {
				score += v2.getType().getKnightLevel();
				v2.deactivateKnight();
			}
		}
		return score;
	}*/
	private fun processRouteChange(p: Player, edge: Route?) {
		val len = board.computeMaxRouteLengthForPlayer(p.playerNum, rules.isEnableRoadBlock)
		p.roadLength = len
		updateLongestRoutePlayer()
		edge?.takeIf { it.player > 0 }?.let { edge ->
			checkForDiscoveredNewTerritory(edge.from)
			checkForDiscoveredNewTerritory(edge.to)
		}
	}

	/**
	 * @param playerNum
	 * @param vertexIndex
	 */
	protected open fun onPlayerConqueredPirateFortress(playerNum: Int, vertexIndex: Int) {}

	/**
	 * @param playerNum
	 * @param playerHealth
	 * @param pirateHealth
	 */
	protected open fun onPlayerAttacksPirateFortress(playerNum: Int, playerHealth: Int, pirateHealth: Int) {}
	private fun processPlayerAttacksPirateFortress(p: Player, vIndex: Int) {
		val v = board.getVertex(vIndex)
		printinfo(getString("%s is attacking a pirate fortress", p.name))
		val playerHealth = board.getRoutesOfType(curPlayerNum, RouteType.WARSHIP).size
		pushDiceConfig(DiceType.WhiteBlack)
		val pirateHealth = getDie(0)
		pirateHealth.roll()
		onDiceRolledPrivate(Utils.asList(pirateHealth))
		onPlayerAttacksPirateFortress(p.playerNum, playerHealth, pirateHealth.num)
		if (playerHealth > pirateHealth.num) {
			// player wins
			assert(v.pirateHealth > 0)
			val h = v.pirateHealth - 1
			v.pirateHealth = h
			if (h <= 0) {
				printinfo(getString("%s has conquered a pirate fortress!", p.name))
				v.setOpen()
				onPlayerConqueredPirateFortress(curPlayerNum, vIndex)
				v.setPlayerAndType(curPlayerNum, VertexType.SETTLEMENT)
				p.addCard(SpecialVictoryType.CapturePirateFortress)
			} else {
				printinfo(getString("%1\$s won and reduced the fortress health to %2\$d", p.name, h))
			}
		} else if (playerHealth == pirateHealth.num) {
			// lose ship adjacent to the fortress
			printinfo(getString("%s's attack results in a draw.  Player loses 1 ship", p.name))
			board.removeShipsClosestToVertex(vIndex, p.playerNum, 1)
			processRouteChange(p, null)
		} else {
			// lose the 2 ships closest to the fortress
			printinfo(getString("%s's attack results in a loss.  Player loses 2 ships", p.name))
			board.removeShipsClosestToVertex(vIndex, p.playerNum, 2)
			processRouteChange(p, null)
		}
		popDiceConfig()
	}

	/**
	 * Not recommended to use as this function modifies player and this data.
	 * call runGame until returns true to process
	 *
	 * @param move
	 */
	fun processMove(move: MoveType) {
		log.debug("processMove: %s", move)
		printinfo(getString("%1\$s choose move %2\$s", curPlayer.name, move.getNameId()))
		when (move) {
			MoveType.INVALID -> TODO("Why???")
			MoveType.ROLL_DICE -> {
				if (rules.isCatanForTwo) {
					pushStateFront(State.PROCESS_NEUTRAL_PLAYER)
				}
				rollDice()
				popState()
				pushStateFront(State.PROCESS_DICE)
				if (isPirateAttacksEnabled) {
					pushStateFront(State.PROCESS_PIRATE_ATTACK)
				}
			}
			MoveType.ROLL_DICE_NEUTRAL_PLAYER -> {
				rollDice()
				popState()
				pushStateFront(State.PROCESS_DICE)
				if (isPirateAttacksEnabled) {
					pushStateFront(State.PROCESS_PIRATE_ATTACK)
				}
			}
			MoveType.DEAL_EVENT_CARD -> {
				if (rules.isCatanForTwo) {
					pushStateFront(State.PROCESS_NEUTRAL_PLAYER)
				}
				val card = nextEventCard
				onEventCardDealt(card)
				if (rules.isEnableCitiesAndKnightsExpansion) {
					val dice = nextDice()
					for (d in nextDice()) {
						d.roll()
					}
					onDiceRolledPrivate(dice)
				}
				popState()
				processDice()
				processEventCard(card)
			}
			MoveType.DEAL_EVENT_CARD_NEUTRAL_PLAYER -> {
				val card = nextEventCard
				onEventCardDealt(card)
				if (rules.isEnableCitiesAndKnightsExpansion) {
					val dice = nextDice()
					for (d in nextDice()) {
						d.roll()
					}
					onDiceRolledPrivate(dice)
				}
				popState()
				processDice()
				processEventCard(card)
			}
			MoveType.REPAIR_ROAD -> {
				curPlayer.adjustResourcesForBuildable(BuildableType.Road, -1)
				curPlayer.removeCard(SpecialVictoryType.DamagedRoad)
				for (r in board.getRoutesForPlayer(curPlayerNum)) {
					if (r.type === RouteType.DAMAGED_ROAD) {
						r.type = RouteType.ROAD
						break
					}
				}
			}
			MoveType.ATTACK_PIRATE_FORTRESS -> {
				pushStateFront(State.CHOOSE_PIRATE_FORTRESS_TO_ATTACK, null, computeAttackablePirateFortresses(board, curPlayer), null)
			}
			MoveType.BUILD_ROAD -> {
				curPlayer.adjustResourcesForBuildable(BuildableType.Road, -1)
				pushStateFront(State.POSITION_ROAD_CANCEL, null, null, object : UndoAction {
					override fun undo() {
						curPlayer.adjustResourcesForBuildable(BuildableType.Road, 1)
					}
				})
			}
			MoveType.BUILD_SHIP -> {
				curPlayer.adjustResourcesForBuildable(BuildableType.Ship, -1)
				pushStateFront(State.POSITION_SHIP_CANCEL, null, null, object : UndoAction {
					override fun undo() {
						curPlayer.adjustResourcesForBuildable(BuildableType.Ship, 1)
					}
				})
			}
			MoveType.BUILD_WARSHIP -> {
				curPlayer.adjustResourcesForBuildable(BuildableType.Warship, -1)
				pushStateFront(State.UPGRADE_SHIP_CANCEL, null, null, object : UndoAction {
					override fun undo() {
						curPlayer.adjustResourcesForBuildable(BuildableType.Warship, 1)
					}
				})
			}
			MoveType.MOVE_SHIP -> pushStateFront(State.CHOOSE_SHIP_TO_MOVE)
			MoveType.BUILD_SETTLEMENT -> {
				curPlayer.adjustResourcesForBuildable(BuildableType.Settlement, -1)
				pushStateFront(State.POSITION_SETTLEMENT_CANCEL, null, null, object : UndoAction {
					override fun undo() {
						curPlayer.adjustResourcesForBuildable(BuildableType.Settlement, 1)
					}
				})
			}
			MoveType.BUILD_CITY -> {
				curPlayer.adjustResourcesForBuildable(BuildableType.City, -1)
				pushStateFront(State.POSITION_CITY_CANCEL, null, null, object : UndoAction {
					override fun undo() {
						curPlayer.adjustResourcesForBuildable(BuildableType.City, 1)
					}
				})
			}
			MoveType.DRAW_DEVELOPMENT -> {
				curPlayer.adjustResourcesForBuildable(BuildableType.Development, -1)
				pickDevelopmentCardFromDeck()
			}
			MoveType.MONOPOLY_CARD -> {
				val removed = curPlayer.removeUsableCard(DevelopmentCardType.Monopoly)!!
				putCardBackInDeck(removed)
				pushStateFront(State.CHOOSE_RESOURCE_MONOPOLY, null, null, object : UndoAction {
					override fun undo() {
						curPlayer.addCard(removed)
						removeCardFromDeck(removed)
					}
				})
			}
			MoveType.YEAR_OF_PLENTY_CARD -> {
				val removed = curPlayer.removeUsableCard(DevelopmentCardType.YearOfPlenty)!!
				putCardBackInDeck(removed)
				mDevelopmentCards.add(removed)
				pushStateFront(State.DRAW_RESOURCE_NOCANCEL)
				curPlayer.setCardsUsable(CardType.Development, false)
				pushStateFront(State.DRAW_RESOURCE_CANCEL, null, null, object : UndoAction {
					override fun undo() {
						curPlayer.addCard(removed)
						removeCardFromDeck(removed)
						curPlayer.setCardsUsable(CardType.Development, true)
						popState()
					}
				})
			}
			MoveType.ROAD_BUILDING_CARD -> {
				val removed = if (rules.isEnableCitiesAndKnightsExpansion)
					curPlayer.removeUsableCard(ProgressCardType.RoadBuilding)!!
				else
					curPlayer.removeUsableCard(DevelopmentCardType.RoadBuilding)!!
				putCardBackInDeck(removed)
				pushStateFront(State.POSITION_ROAD_OR_SHIP_NOCANCEL)
				curPlayer.setCardsUsable(CardType.Development, false)
				pushStateFront(State.POSITION_ROAD_OR_SHIP_CANCEL, null, null, object : UndoAction {
					override fun undo() {
						curPlayer.addCard(removed)
						removeCardFromDeck(removed)
						//        				getCurPlayer().setCardsUsable(CardType.Development, true);
						popState() // pop an extra state since we push the NOCANCEL
					}
				})
			}
			MoveType.BISHOP_CARD -> {
				val removed = curPlayer.removeCard(ProgressCardType.Bishop)!!
				putCardBackInDeck(removed)
				pushStateFront(if (rules.isEnableSeafarersExpansion) State.POSITION_ROBBER_OR_PIRATE_CANCEL else State.POSITION_ROBBER_CANCEL, null, null, object : UndoAction {
					override fun undo() {
						removeCardFromDeck(removed)
						curPlayer.addCard(removed)
					}
				})
			}
			MoveType.WARSHIP_CARD -> {
				val options = board.getRoutesIndicesOfType(curPlayerNum, RouteType.SHIP)
				if (options.isNotEmpty()) {
					val card = curPlayer.removeUsableCard(DevelopmentCardType.Warship)!!
					putCardBackInDeck(card)
					pushStateFront(State.UPGRADE_SHIP_CANCEL, null, options, object : UndoAction {
						override fun undo() {
							removeCardFromDeck(card)
							curPlayer.addCard(card)
						}
					})
				}
			}
			MoveType.SOLDIER_CARD -> {
				val used = curPlayer.getUsableCard(DevelopmentCardType.Soldier)!!
				used.setUsed()
				updateLargestArmyPlayer()
				curPlayer.setCardsUsable(CardType.Development, false)
				pushStateFront(if (rules.isEnableSeafarersExpansion) State.POSITION_ROBBER_OR_PIRATE_CANCEL else State.POSITION_ROBBER_CANCEL, null, null, object : UndoAction {
					override fun undo() {
						used.setUsable()
						updateLargestArmyPlayer()
						curPlayer.setCardsUsable(CardType.Development, true)
					}
				})
			}
			MoveType.ALCHEMIST_CARD -> {
				val dice = nextDice()
				if (curPlayer.setDice(this, dice, 2)) {
					putCardBackInDeck(curPlayer.removeCard(ProgressCardType.Alchemist)!!)
					val ry = getDiceOfType(DiceType.YellowRed, dice)
					val yr = getDiceOfType(DiceType.RedYellow, dice)
					val ev = getDiceOfType(DiceType.Event, dice)
					ev.roll()
					popState()
					onDiceRolledPrivate(Utils.asList(ry, yr, ev))
					printinfo(getString("%1\$s applied Alchemist card on dice %2\$d, %2\$d, %4\$s", curPlayer.name, ry.num, yr.num, fromDieNum(ev.num).getNameId()))
					processDice()
				}
			}
			MoveType.TRADE -> pushStateFront(State.SHOW_TRADE_OPTIONS)
			MoveType.BUILD_CITY_WALL -> {
				curPlayer.adjustResourcesForBuildable(BuildableType.CityWall, -1)
				pushStateFront(State.CHOOSE_CITY_FOR_WALL, null, null, object : UndoAction {
					override fun undo() {
						curPlayer.adjustResourcesForBuildable(BuildableType.CityWall, 1)
					}
				})
			}
			MoveType.ACTIVATE_KNIGHT -> {
				assert(curPlayer.canBuild(BuildableType.ActivateKnight))
				pushStateFront(State.CHOOSE_KNIGHT_TO_ACTIVATE)
			}
			MoveType.HIRE_KNIGHT -> {
				val options: Collection<Int> = computeNewKnightVertexIndices(curPlayerNum, board)
				if (options.size > 0) {
					curPlayer.adjustResourcesForBuildable(BuildableType.Knight, -1)
					pushStateFront(State.POSITION_NEW_KNIGHT_CANCEL, null, options, object : UndoAction {
						override fun undo() {
							curPlayer.adjustResourcesForBuildable(BuildableType.Knight, 1)
						}
					})
				}
			}
			MoveType.MOVE_KNIGHT -> {
				val options: Collection<Int> = computeMovableKnightVertexIndices(this, curPlayerNum, board)
				if (options.size > 0) {
					pushStateFront(State.CHOOSE_KNIGHT_TO_MOVE, null, options, null)
				}
			}
			MoveType.PROMOTE_KNIGHT -> {
				curPlayer.adjustResourcesForBuildable(BuildableType.PromoteKnight, -1)
				pushStateFront(State.CHOOSE_KNIGHT_TO_PROMOTE, null, null, object : UndoAction {
					override fun undo() {
						curPlayer.adjustResourcesForBuildable(BuildableType.PromoteKnight, 1)
					}
				})
			}
			MoveType.IMPROVE_CITY_POLITICS -> {
				processCityImprovement(curPlayer, DevelopmentArea.Politics, 0)
			}
			MoveType.IMPROVE_CITY_SCIENCE -> {
				processCityImprovement(curPlayer, DevelopmentArea.Science, 0)
			}
			MoveType.IMPROVE_CITY_TRADE -> {
				processCityImprovement(curPlayer, DevelopmentArea.Trade, 0)
			}
			MoveType.CONTINUE -> popState()
			MoveType.CRANE_CARD -> {

				// build a city improvement for 1 commodity less than normal
				val options = computeCraneCardImprovements(curPlayer)
				if (options.isNotEmpty()) {
					val card = curPlayer.removeCard(ProgressCardType.Crane)!!
					putCardBackInDeck(card)
					pushStateFront(State.CHOOSE_CITY_IMPROVEMENT, null, null, options, object : UndoAction {
						override fun undo() {
							removeCardFromDeck(card)
							curPlayer.addCard(card)
						}
					})
				}
			}
			MoveType.DESERTER_CARD -> {

				// replace an opponents knight with one of your own
				val knightOptions = computeNewKnightVertexIndices(curPlayerNum, board)
				if (knightOptions.size > 0) {
					val players = computeDeserterPlayers(this, board, curPlayer)
					if (players.size > 0) {
						pushStateFront(State.CHOOSE_PLAYER_FOR_DESERTION, null, players)
					}
				}
			}
			MoveType.DIPLOMAT_CARD -> {
				val allOpenRoutes = computeDiplomatOpenRouteIndices(this, board)
				if (allOpenRoutes.size > 0) {
					pushStateFront(State.CHOOSE_DIPLOMAT_ROUTE, null, allOpenRoutes, null)
				}
			}
			MoveType.ENGINEER_CARD -> {

				// build a city wall for free
				val cities = computeCityWallVertexIndices(curPlayerNum, board)
				if (cities.isNotEmpty()) {
					val card = curPlayer.removeCard(ProgressCardType.Engineer)!!
					putCardBackInDeck(card)
					pushStateFront(State.CHOOSE_CITY_FOR_WALL, null, cities, object : UndoAction {
						override fun undo() {
							curPlayer.addCard(card)
							removeCardFromDeck(card)
						}
					})
				}
			}
			MoveType.HARBOR_CARD -> {

				// You may force each of the other players to make a special trade.
				// You may offer each opponent any 1 Resource Card from your hand. He must exchange it for any
				// 1 Commodity Card from his hand of his choice, if he has any.
				// You must have a resource card to trade and they must have a commodity card to trade
				// You can skip a player if you wish
				val players = computeHarborTradePlayers(curPlayer, this)
				if (players.size > 0) {
					val card = curPlayer.getCard(ProgressCardType.Harbor)
					assert(card != null)
					pushStateFront(State.CHOOSE_HARBOR_PLAYER, card, players)
				}
			}
			MoveType.INTRIGUE_CARD -> {

				// displace an opponents knight that is on your road without moving your own knight
				val verts = computeIntrigueKnightsVertexIndices(curPlayerNum, board)
				if (verts.size > 0) {
					pushStateFront(State.CHOOSE_OPPONENT_KNIGHT_TO_DISPLACE, null, verts, null)
				}
			}
			MoveType.INVENTOR_CARD -> {

				// switch tile tokens of users choice
				pushStateFront(State.CHOOSE_TILE_INVENTOR, null, computeInventorTileIndices(board, this), null)
			}
			MoveType.IRRIGATION_CARD -> {

				// player gets 2 wheat for each structure on a field
				val numGained = 2 * computeNumStructuresAdjacentToTileType(curPlayerNum, board, TileType.FIELDS)
				curPlayer.removeCard(ProgressCardType.Irrigation)?.let {
					putCardBackInDeck(it)
				}
				if (numGained > 0) {
					curPlayer.incrementResource(ResourceType.Wheat, numGained)
					onDistributeResources(curPlayerNum, ResourceType.Wheat, numGained)
				}
			}
			MoveType.MASTER_MERCHANT_CARD -> {

				// take 2 resource or commodity cards from another player who has more victory pts than you
				val players = computeMasterMerchantPlayers(this, curPlayer)
				if (players.size > 0) {
					pushStateFront(State.CHOOSE_PLAYER_MASTER_MERCHANT, null, players, null)
				}
			}
			MoveType.MEDICINE_CARD -> {

				// upgrade to city for cheaper
				if (curPlayer.getCardCount(ResourceType.Ore) >= 2 && curPlayer.getCardCount(ResourceType.Wheat) >= 1) {
					val settlements = board.getSettlementsForPlayer(curPlayerNum)
					if (settlements.size > 0) {
						curPlayer.incrementResource(ResourceType.Ore, -2)
						curPlayer.incrementResource(ResourceType.Wheat, -1)
						val card = curPlayer.removeCard(ProgressCardType.Medicine)!!
						putCardBackInDeck(card)
						pushStateFront(State.POSITION_CITY_CANCEL, null, settlements, object : UndoAction {
							override fun undo() {
								removeCardFromDeck(card)
								curPlayer.addCard(card)
								curPlayer.incrementResource(ResourceType.Ore, 2)
								curPlayer.incrementResource(ResourceType.Wheat, 1)
							}
						})
					}
				}
			}
			MoveType.MERCHANT_CARD -> {
				pushStateFront(State.POSITION_MERCHANT)
			}
			MoveType.MERCHANT_FLEET_CARD -> {
				val tradableCards = computeMerchantFleetCards(curPlayer)
				if (tradableCards.size > 0) {
					pushStateFront(State.CHOOSE_RESOURCE_FLEET, null, null, tradableCards, null)
				}
			}
			MoveType.MINING_CARD -> {

				// player gets 2 ore for each structure on a field
				val numGained = 2 * computeNumStructuresAdjacentToTileType(curPlayerNum, board, TileType.MOUNTAINS)
				if (numGained > 0) {
					curPlayer.incrementResource(ResourceType.Ore, numGained)
					onDistributeResources(curPlayerNum, ResourceType.Ore, numGained)
					curPlayer.removeCard(ProgressCardType.Mining)?.let {
						putCardBackInDeck(it)
					}
				}
			}
			MoveType.RESOURCE_MONOPOLY_CARD -> {
				val remove = curPlayer.removeCard(ProgressCardType.ResourceMonopoly)?.also {
					putCardBackInDeck(it)
				}
				pushStateFront(State.CHOOSE_RESOURCE_MONOPOLY, null, null, object : UndoAction {
					override fun undo() {
						remove?.let { remove ->
							removeCardFromDeck(remove)
							curPlayer.addCard(remove)
						}
					}
				})
			}
			MoveType.SABOTEUR_CARD -> {
				val sabotagePlayers = computeSaboteurPlayers(this, curPlayerNum)
				var done = false
				pushStateFront(State.SET_PLAYER, curPlayerNum)
				for (pNum in sabotagePlayers) {
					val p = getPlayerByPlayerNum(pNum)
					val num = p.unusedCardCount / 2
					if (num > 0) {
						done = true
						var i = 0
						while (i < num) {
							pushStateFront(State.GIVE_UP_CARD, num)
							i++
						}
						pushStateFront(State.SET_PLAYER, pNum)
					}
				}
				if (done) {
					curPlayer.removeCard(ProgressCardType.Saboteur)?.let {
						putCardBackInDeck(it)
					}
				}
			}
			MoveType.SMITH_CARD -> {

				// promote 2 knights for free
				val knights = computePromoteKnightVertexIndices(curPlayer, board)
				if (knights.isNotEmpty()) {
					val removed = curPlayer.removeCard(ProgressCardType.Smith)?.also {
						putCardBackInDeck(it)
					}
					if (knights.size > 1) {
						// remember the knights we have so that we can revert
						val currentKnights = knights.map {
							Pair(it, board.getVertex(it).type)
						}

						// this one we be chosen second
						pushStateFront(State.CHOOSE_KNIGHT_TO_PROMOTE, null, null, object : UndoAction {
							override fun undo() {
								for (k in currentKnights) {
									board.getVertex(k.first).setType(k.second)
								}
								removed?.let {
									curPlayer.addCard(it)
									removeCardFromDeck(it)
								}
							}
						})
					}
					// this one will be chosen first
					pushStateFront(State.CHOOSE_KNIGHT_TO_PROMOTE, null, knights, object : UndoAction {
						override fun undo() {
							if (knights.size > 1) popState()
							removed?.let {
								curPlayer.addCard(it)
								removeCardFromDeck(it)
							}
						}
					})
				}
			}
			MoveType.SPY_CARD -> {

				// steal a players progress cards
				val players = computeSpyOpponents(this, curPlayerNum)
				if (players.size > 0) {
					pushStateFront(State.CHOOSE_PLAYER_TO_SPY_ON, null, players, null)
				}
			}
			MoveType.TRADE_MONOPOLY_CARD -> {
				pushStateFront(State.CHOOSE_TRADE_MONOPOLY)
			}
			MoveType.WARLORD_CARD -> {

				// Activate all knights
				val knights = computeWarlordVertices(board, curPlayerNum)
				if (knights.size > 0) {
					for (vIndex in knights) {
						board.getVertex(vIndex).activateKnight()
					}
					curPlayer.removeCard(ProgressCardType.Warlord)?.let {
						putCardBackInDeck(it)
					}
				}
			}
			MoveType.WEDDING_CARD -> {
				pushStateFront(State.SET_PLAYER, curPlayerNum)
				val opponents = computeWeddingOpponents(this, curPlayer)
				if (opponents.isNotEmpty()) {
					curPlayer.removeCard(ProgressCardType.Wedding)?.let {
						putCardBackInDeck(it)
					}
					for (num in opponents) {
						val p = getPlayerByPlayerNum(num)
						// automatically give
						val cards = p.getCards(CardType.Commodity).toMutableList()
						cards.addAll(p.getCards(CardType.Resource))
						pushStateFront(State.CHOOSE_GIFT_CARD, curPlayer, opponents)
						if (cards.size > 1) pushStateFront(State.CHOOSE_GIFT_CARD, curPlayer)
						pushStateFront(State.SET_PLAYER, p.playerNum)
					}
				}
			}
			MoveType.KNIGHT_ATTACK_ROAD -> {
				pushStateFront(State.CHOOSE_ROAD_TO_ATTACK)
			}
			MoveType.KNIGHT_ATTACK_STRUCTURE -> {
				pushStateFront(State.CHOOSE_STRUCTURE_TO_ATTACK)
			}
			MoveType.ATTACK_SHIP -> {
				pushStateFront(State.CHOOSE_SHIP_TO_ATTACK)
			}
		}
	}

	private fun putCardBackInDeck(card: Card) {
		when (card.cardType) {
			CardType.Development -> mDevelopmentCards.add(card)
			CardType.Progress -> {
				val index: Int = ProgressCardType.values()[card.typeOrdinal].getData().ordinal
				mProgressCards[index].add(card)
			}
			CardType.Commodity, CardType.Resource -> {
			}
//			else -> throw SOCException("Should not happen")
			CardType.SpecialVictory -> TODO()
			CardType.Event -> TODO()
			CardType.BarbarianAttackDevelopment -> TODO()
		}
	}

	private fun removeCardFromDeck(card: Card) {
		val success = when (card.cardType) {
			CardType.Development -> mDevelopmentCards.remove(card)
			CardType.Progress -> {
				val index: Int = ProgressCardType.values()[card.typeOrdinal].getData().ordinal
				mProgressCards[index].remove(card)
			}
			CardType.Commodity, CardType.Resource -> true
			else -> throw SOCException("Should not happen")
		}
		assert(success)
	}

	/**
	 * Called when a trade completed as event for the user to handle if they wish
	 * base method does nothing.
	 *
	 * @param playerNum
	 * @param trade
	 */
	protected open fun onCardsTraded(playerNum: Int, trade: Trade) {}

	/**
	 * Called when a player has discovered a new island for app to add any logic they want.
	 *
	 * @param playerNum
	 * @param islandIndex
	 */
	protected open fun onPlayerDiscoveredIsland(playerNum: Int, islandIndex: Int) {}

	/**
	 * Executed when game is in a good state for saving.
	 */
	protected open fun onShouldSaveGame() {}

	/**
	 * @param playerNum
	 * @param routeIndex
	 */
	protected open fun onPlayerShipUpgraded(playerNum: Int, routeIndex: Int) {}

	/**
	 * Called when a players ship get taken over by another
	 *
	 * @param takerNum
	 * @param shipTakenRouteIndex
	 */
	protected open fun onPlayerShipComandeered(takerNum: Int, shipTakenRouteIndex: Int) {}

	/**
	 * Called when a player ship is destroyed during an attack.
	 *
	 * @param routeIndex
	 */
	protected open fun onPlayerShipDestroyed(routeIndex: Int) {}
	private fun givePlayerSpecialVictoryCard(player: Player, card: SpecialVictoryType) {
		getSpecialVictoryPlayer(card)?.let {
			it.removeCard(card)
		}
		player.addCard(card)
	}

	/**
	 * @param player
	 */
	fun setHarborMasterPlayer(player: Player?) {
		player?.let { givePlayerSpecialVictoryCard(it, SpecialVictoryType.HarborMaster) }
	}

	/**
	 * Return true when it is legal for a player to cancel from their current Move.
	 *
	 * @return
	 */
	fun canCancel(): Boolean {
		return if (mStateStack.empty()) false else state.canCancel
	}

	/**
	 * @return
	 */
	val isPirateAttacksEnabled: Boolean
		get() = rules.isEnableSeafarersExpansion && board.pirateRouteStartTile >= 0

	/**
	 * Typically this operation causes the game to revert a state.
	 */
	open fun cancel() {
		if (!canCancel()) {
			log.error("Calling cancel when cancel not allowed")
			return
		}
		mStateStack.peek()?.undoAction?.let {
			popState()
			it.undo()
		}
	}

	/**
	 * @param playerNum
	 * @param tileIndex
	 */
	protected open fun onDiscoverTerritory(playerNum: Int, tileIndex: Int) {}
	private fun checkForDiscoveredNewTerritory(vIndex: Int) {
		val die = intArrayOf(2, 3, 4, 5, 6, 8, 9, 10, 11, 12)
		for (tile in board.getVertexTiles(vIndex)) {
			if (tile.type === TileType.UNDISCOVERED) {
				val chances = IntArray(TileType.values().size)
				for (t in TileType.values()) {
					chances[t.ordinal] = t.chanceOnUndiscovered
				}
				val index = Utils.chooseRandomFromSet(*chances)
				val newType = TileType.values()[index]
				tile.type = newType
				assert(newType.chanceOnUndiscovered > 0)
				val resourceBonus = rules.numResourcesForDiscoveredTerritory
				onDiscoverTerritory(curPlayerNum, board.getTileIndex(tile))
				when (newType) {
					TileType.GOLD -> {

						// choose random number
						tile.dieNum = die[Utils.rand() % die.size]
						var i = 0
						while (i < resourceBonus) {
							pushStateFront(State.DRAW_RESOURCE_NOCANCEL)
							i++
						}
					}
					TileType.FIELDS, TileType.FOREST, TileType.HILLS, TileType.MOUNTAINS, TileType.PASTURE -> {
						tile.dieNum = die[Utils.rand() % die.size]
						curPlayer.incrementResource(requireNotNull(tile.resource), resourceBonus)
						onDistributeResources(curPlayerNum, requireNotNull(tile.resource), resourceBonus)
					}
					else -> {
					}
				}
				printinfo(getString("%1\$s has discovered a new territory: %2\$s", curPlayer.name, newType))
				curPlayer.incrementDiscoveredTerritory(1)
				updateExplorerPlayer()
				for (r in board.getTileRoutes(tile)) {
					if (tile.isWater) r.isAdjacentToWater = true else if (tile.isLand) r.isAdjacentToLand = true
				}
				for (v in board.getTileVertices(tile)) {
					if (tile.isWater) v.isAdjacentToWater = true
					if (tile.isLand) v.isAdjacentToLand = true
				}
			}
		}
	}

	private fun checkForDiscoveredIsland(vIndex: Int) {
		for (t in board.getVertexTiles(vIndex)) {
			if (t.islandNum > 0) {
				if (!board.isIslandDiscovered(curPlayerNum, t.islandNum)) {
					board.setIslandDiscovered(curPlayerNum, t.islandNum, true)
					printinfo(getString("%s has discovered an island", curPlayer.name))
					onPlayerDiscoveredIsland(curPlayerNum, t.islandNum)
				}
			}
		}
	}

	/**
	 * Called when a player road becomes blocked, resulting is loss of road length.  Only used when Config.ENABLE_ROAD_BLOCK enabled.
	 *
	 * @param playerNum
	 * @param oldLen
	 * @param newLen
	 */
	protected open fun onPlayerRoadLengthChanged(playerNum: Int, oldLen: Int, newLen: Int) {}

	// call this whenever a vertex type changes from open to anything or vise versa
	private fun updatePlayerRoadsBlocked(vIndex: Int) {
		if (rules.isEnableRoadBlock) {
			val pNum = board.checkForPlayerRouteBlocked(vIndex)
			if (pNum > 0) {
				board.clearRouteLenCache()
				val len = board.computeMaxRouteLengthForPlayer(pNum, rules.isEnableRoadBlock)
				val p = getPlayerByPlayerNum(pNum)
				if (len != p.roadLength) {
					onPlayerRoadLengthChanged(p.playerNum, p.roadLength, len)
				}
				p.roadLength = len
				updateLongestRoutePlayer()
			}
		}
	}

	/**
	 * Called when a player get the longest road or overtakes another player.
	 * default method does nothing
	 *
	 * @param oldPlayerNum -1 if newPlayer is the first to get the longest road
	 * @param newPlayerNum player that has the longest road or -1 if this player has lost it
	 * @param maxRoadLen
	 */
	protected open fun onLongestRoadPlayerUpdated(oldPlayerNum: Int, newPlayerNum: Int, maxRoadLen: Int) {}
	private fun updateLongestRoutePlayer() {
		val maxRoadLenPlayer = computeLongestRoadPlayer(this)
		val curLRP = longestRoadPlayer
		if (maxRoadLenPlayer == null) {
			if (curLRP != null) {
				printinfo(getString("%s is blocked and has lost the longest road!", curLRP.name))
				setLongestRoadPlayer(null)
				onLongestRoadPlayerUpdated(curLRP.playerNum, -1, 0)
			}
			return
		}
		if (curLRP != null && maxRoadLenPlayer.playerNum == curLRP.playerNum) return
		val maxRoadLen = maxRoadLenPlayer.roadLength
		if (curLRP == null) {
			printinfo(getString("%s has gained the Longest Road!", maxRoadLenPlayer.name))
			onLongestRoadPlayerUpdated(-1, maxRoadLenPlayer.playerNum, maxRoadLen)
		} else if (maxRoadLenPlayer.roadLength > curLRP.roadLength) {
			printinfo(getString("%1\$s has overtaken %2\$s with the Longest Road!", maxRoadLenPlayer.name, curLRP.name))
			onLongestRoadPlayerUpdated(curLRP.playerNum, maxRoadLenPlayer.playerNum, maxRoadLen)
		}
		setLongestRoadPlayer(maxRoadLenPlayer)
	}

	/**
	 * Called when a player get the largest army or overtakes another player.
	 * default method does nothing
	 *
	 * @param oldPlayerNum -1 if newPlayer is the first to get the largest army
	 * @param newPlayerNum player that has the largest army
	 * @param armySize     current largest army size
	 */
	protected open fun onLargestArmyPlayerUpdated(oldPlayerNum: Int, newPlayerNum: Int, armySize: Int) {}
	private fun updateLargestArmyPlayer() {
		var largestArmyPlayer = computeLargestArmyPlayer(this, board)
		val curLAP = largestArmyPlayer
		if (largestArmyPlayer == null) {
			largestArmyPlayer = null
			return
		}
		if (curLAP != null && largestArmyPlayer.playerNum == curLAP.playerNum) return
		val maxArmySize = largestArmyPlayer.getArmySize(board)
		if (curLAP == null) {
			printinfo(getString("%s Has largest army and gets to take a resource card from another", largestArmyPlayer.name))
			onLargestArmyPlayerUpdated(-1, largestArmyPlayer.playerNum, maxArmySize)
		} else if (largestArmyPlayer.getArmySize(board) > curLAP.getArmySize(board)) {
			printinfo(getString("%1\$s overtakes %2\$s for the largest Army!", largestArmyPlayer.name, curLAP.name))
			onLargestArmyPlayerUpdated(curLAP.playerNum, largestArmyPlayer.playerNum, maxArmySize)
		}
		largestArmyPlayer = largestArmyPlayer
	}

	private fun checkUpdateHarborMaster(v: Vertex, curPlayer: Player) {
		if (rules.isEnableHarborMaster) {
			board.getTilesAdjacentToVertex(v).sumBy {
				if (it.isPort) v.type.harborPts else 0
			}.takeIf { it > 0 }?.let { newPts ->
				val pts = curPlayer.harborPoints + newPts
				curPlayer.harborPoints = pts
				updateHarborMasterPlayer()
			}
		}
	}

	/**
	 * @param oldPlayerNum player losing the points or -1 if this is first time
	 * @param newPlayerNum plyer gaining the points
	 * @param harborPts
	 */
	protected open fun onHarborMasterPlayerUpdated(oldPlayerNum: Int, newPlayerNum: Int, harborPts: Int) {}
	private fun updateHarborMasterPlayer() {
		val harborMaster = computeHarborMaster(this)
		val curHM = harborMaster
		if (harborMaster == null) {
			setHarborMasterPlayer(null)
			return
		}
		if (curHM != null && harborMaster.playerNum == curHM.playerNum) return
		val maxHP = harborMaster.harborPoints
		if (curHM == null) {
			printinfo(getString("%s is the Harbor Master!", harborMaster.name))
			onHarborMasterPlayerUpdated(-1, harborMaster.playerNum, maxHP)
		} else {
			printinfo(getString("%1\$s overthrows %2\$s as the new Harbor Master!", harborMaster.name, curHM.name))
			onHarborMasterPlayerUpdated(curHM.playerNum, harborMaster.playerNum, maxHP)
		}
		setHarborMasterPlayer(harborMaster)
	}

	/**
	 * @param oldPlayerName player losing the points or -1 if this is first time giving points
	 * @param newPlayerNum  player gaining the points
	 * @param harborPts
	 */
	protected open fun onExplorerPlayerUpdated(oldPlayerName: Int, newPlayerNum: Int, harborPts: Int) {}
	private fun updateExplorerPlayer() {
		var explorer = computeExporer(this)
		val curE = explorer
		if (explorer == null) {
			explorer = null
			return
		}
		if (curE != null && explorer.playerNum == curE.playerNum) return
		val maxE = explorer.numDiscoveredTerritories
		if (curE == null) {
			printinfo(getString("%s is an Explorer!", explorer.name))
			onExplorerPlayerUpdated(-1, explorer.playerNum, maxE)
		} else {
			printinfo(getString("%1\$s overtakes %2\$s as the best explorer!", explorer.name, curE.name))
			onExplorerPlayerUpdated(curE.playerNum, explorer.playerNum, maxE)
		}
		explorer = explorer
	}

	/**
	 * Compute the list of players from which 'p' can take a card
	 *
	 * @param p
	 * @param b
	 * @return
	 */
	fun computeRobberTakeOpponentCardOptions(p: Player, b: Board, pirate: Boolean): List<Int> {
		return computeTakeOpponentCardPlayers(mPlayers.toTypedArray(), p, b, pirate)
	}

	/**
	 * @return
	 */
	val players: Iterable<Player>
		get() = mPlayers

	private fun processCityImprovement(p: Player, area: DevelopmentArea, craneAdjust: Int) {
		printinfo(getString("%1\$s is improving their %2\$s", p.name, area.getNameId()))
		var devel = p.getCityDevelopment(area)
		assert(devel < DevelopmentArea.MAX_CITY_IMPROVEMENT)
		devel++
		p.removeCards(requireNotNull(area.commodity), devel - craneAdjust)
		p.setCityDevelopment(area, devel)
		onPlayerCityDeveloped(p.playerNum, area)
		if (checkMetropolis(this, board, devel, curPlayerNum, area)) {
			pushStateFront(State.CHOOSE_METROPOLIS, area)
		}
	}

	/**
	 * @param fromTile
	 * @param toTile
	 */
	protected open fun onPirateSailing(fromTile: Int, toTile: Int) {}

	/**
	 * @param playerNum
	 * @param c
	 */
	protected open fun onCardLost(playerNum: Int, c: Card) {}

	/**
	 * @param playerNum
	 * @param playerStrength
	 * @param pirateStrength
	 */
	protected open fun onPirateAttack(playerNum: Int, playerStrength: Int, pirateStrength: Int) {}
	private fun processPirateAttack() {
		val dice = dice
		var min = 7
		for (d in dice) {
			when (d.type) {
				DiceType.Event -> {
				}
				DiceType.RedYellow, DiceType.WhiteBlack, DiceType.YellowRed -> min = Math.min(min, d.num)
				DiceType.None -> TODO()
			}
		}
		val pirateStrength = min
		requireNotNull(board.getPirateTile()).let { t ->
			while (min-- > 0) {
				val fromTile = board.pirateTileIndex
				var toTile = t.pirateRouteNext
				if (toTile < 0) toTile = board.pirateRouteStartTile
				board.setPirate(-1)
				onPirateSailing(fromTile, toTile)
				board.setPirate(toTile)
			}
			val attacked = BooleanArray(16)
			pushStateFront(State.SET_PLAYER, curPlayerNum)
			for (vIndex in t.getAdjVerts()) {
				val v = board.getVertex(vIndex)
				if (v.player < 1) continue
				if (attacked[v.player]) continue
				if (v.isStructure) {
					val p = getPlayerByPlayerNum(v.player)
					val playerPts = board.getRoutesOfType(v.player, RouteType.WARSHIP).size
					attacked[v.player] = true
					printinfo(getString("Pirate Attack! %1\$s strength %2\$d pirate strength %3\$d", p.name, playerPts, pirateStrength))
					onPirateAttack(p.playerNum, playerPts, pirateStrength)
					if (pirateStrength < playerPts) {
						// player wins the attack
						printinfo(getString("%s has defeated the pirates.  Player takes a resource card of their choice", p.name))
						pushStateFront(State.DRAW_RESOURCE_NOCANCEL)
						pushStateFront(State.SET_PLAYER, v.player)
					} else if (pirateStrength > playerPts) {
						printinfo(getString("Pirates have defeated %s. Player loses 2 random resources cards", p.name))
						var numResources = p.getCardCount(CardType.Resource)
						var i = 0
						while (i < 2 && numResources-- > 0) {
							p.removeRandomUnusedCard(CardType.Resource)?.let { c ->
								onCardLost(p.playerNum, c)
							}
							i++
						}
					} else {
						printinfo(getString("Pirate and %s are of equals strength so attack is nullified", p.name))
					}
				}
			}
		}
	}

	private fun processDice() {
		// roll the dice
		if (productionNum == 7) {
			printinfo(getString("Uh Oh, %s rolled a 7.", curPlayer.name))
			pushStateFront(State.SETUP_GIVEUP_CARDS)
			if (rules.isEnableCitiesAndKnightsExpansion && mBarbarianAttackCount < rules.minBarbarianAttackstoEnableRobberAndPirate) {
				pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM, null, computeOpponentsWithCardsInHand(this, curPlayerNum), null)
			} else if (rules.isEnableRobber) {
				if (rules.isEnableSeafarersExpansion) pushStateFront(State.POSITION_ROBBER_OR_PIRATE_NOCANCEL) else pushStateFront(State.POSITION_ROBBER_NOCANCEL)
			} else if (rules.isEnableSeafarersExpansion) {
				pushStateFront(State.POSITION_PIRATE_NOCANCEL)
			} else {
				pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM, null, computeOpponentsWithCardsInHand(this, curPlayerNum), null)
			}
		} else {

			// after the last player takes a turn for this round need to advance 2 players
			// so that the player after the player who rolled the dice gets to roll next
			pushStateFront(State.NEXT_PLAYER)
			pushStateFront(State.NEXT_PLAYER)
			for (i in 0 until numPlayers - 1) {
				pushStateFront(State.PLAYER_TURN_NOCANCEL)
				pushStateFront(State.NEXT_PLAYER)
			}

			// reset the players ships/development cards usability etc.
			pushStateFront(State.INIT_PLAYER_TURN)

			// do this last so that any states that get pushed are on top
			distributeResources(productionNum)
			if (rules.isEnableCitiesAndKnightsExpansion) {
				when (fromDieNum(getDiceOfType(DiceType.Event, dice).num)) {
					DiceEvent.AdvanceBarbarianShip -> processBarbarianShip()
					DiceEvent.PoliticsCard -> distributeProgressCard(DevelopmentArea.Politics)
					DiceEvent.ScienceCard -> distributeProgressCard(DevelopmentArea.Science)
					DiceEvent.TradeCard -> distributeProgressCard(DevelopmentArea.Trade)
				}
			}
		}
	}

	/**
	 * Called when a player loses their metropolis to another player
	 *
	 * @param loserNum
	 * @param stealerNum
	 * @param area
	 */
	protected open fun onMetropolisStolen(loserNum: Int, stealerNum: Int, area: DevelopmentArea) {}

	/**
	 * @param playerNum
	 * @param type
	 */
	protected open fun onProgressCardDistributed(playerNum: Int, type: ProgressCardType) {}

	/**
	 * @param playerNum
	 * @param type
	 */
	protected open fun onSpecialVictoryCard(playerNum: Int, type: SpecialVictoryType) {}
	private fun distributeProgressCard(area: DevelopmentArea) {
		for (p in players) {
			val dice = dice
			if (mProgressCards[area.ordinal].size > 0 && p.getCardCount(CardType.Progress) < rules.maxProgressCards && p.getCityDevelopment(area) > 0 && p.getCityDevelopment(area) >= getDiceOfType(DiceType.RedYellow, dice).num - 1) {
				val card = mProgressCards[area.ordinal].removeAt(0)
				printinfo(getString("%s draw a progress card", p.name))
				if (card.equals(ProgressCardType.Constitution)) {
					p.addCard(SpecialVictoryType.Constitution)
					onSpecialVictoryCard(p.playerNum, SpecialVictoryType.Constitution)
				} else if (card.equals(ProgressCardType.Printer)) {
					card.setUsed()
					p.addCard(SpecialVictoryType.Printer)
					onSpecialVictoryCard(p.playerNum, SpecialVictoryType.Printer)
				} else {
					p.addCard(card)
					onProgressCardDistributed(p.playerNum, ProgressCardType.values()[card.typeOrdinal])
				}
			}
		}
	}

	/**
	 * Called when the barbarians advance toward catan
	 *
	 * @param distanceAway
	 */
	protected open fun onBarbariansAdvanced(distanceAway: Int) {}

	/**
	 * @param catanStrength
	 * @param barbarianStrength
	 * @param playerStatus
	 */
	protected open fun onBarbariansAttack(catanStrength: Int, barbarianStrength: Int, playerStatus: Array<String>) {}

	/**
	 * @param playerNum
	 * @param tileIndex0
	 * @param tileIndex1
	 */
	protected open fun onTilesInvented(playerNum: Int, tileIndex0: Int, tileIndex1: Int) {}

	private fun processBarbarianShip() {
		barbarianDistance -= 1
		if (barbarianDistance == 0) {
			printinfo(getString("The Barbarians are attacking!"))
			val playerStrength = IntArray(numPlayers + 1)
			var minStrength = Int.MAX_VALUE
			var maxStrength = 0
			for (i in 1..numPlayers) {
				playerStrength[i] = board.getNumVertsOfType(i, VertexType.BASIC_KNIGHT_ACTIVE) * VertexType.BASIC_KNIGHT_ACTIVE.knightLevel + board.getNumVertsOfType(i, VertexType.STRONG_KNIGHT_ACTIVE) * VertexType.STRONG_KNIGHT_ACTIVE.knightLevel + board.getNumVertsOfType(i, VertexType.MIGHTY_KNIGHT_ACTIVE) * VertexType.MIGHTY_KNIGHT_ACTIVE.knightLevel
				val numCities = board.getNumVertsOfType(i, VertexType.CITY, VertexType.WALLED_CITY) //, VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE);
				if (numCities == 0) {
				} //minStrength = Integer.MAX_VALUE; // dont count players who have no pillidgable cities
				else minStrength = Math.min(minStrength, playerStrength[i])
				maxStrength = Math.max(maxStrength, playerStrength[i])
			}
			val catanStrength = CMath.sum(playerStrength)
			val barbarianStrength = computeBarbarianStrength(this, board)
			for (i in playerStrength.indices) {
				if (playerStrength[i] == Int.MAX_VALUE) playerStrength[i] = 0
			}
			val playerStatus = Array(numPlayers + 1) {
				""
			}
			for (p in players) {
				playerStatus[p.playerNum] = getString("Strength %d", playerStrength[p.playerNum])
			}
			if (catanStrength >= barbarianStrength) {
				// find defender
				printinfo(getString("Catan defended itself from the Barbarians!"))
				val defenders: MutableList<Int> = ArrayList()
				for (i in 1 until playerStrength.size) {
					if (playerStrength[i] == maxStrength) {
						defenders.add(i)
					}
				}
				assert(defenders.size > 0)
				if (defenders.size == 1) {
					val defender = getPlayerByPlayerNum(defenders[0])
					defender.addCard(SpecialVictoryType.DefenderOfCatan)
					printinfo(getString("%s receives the Defender of Catan card!", defender.name))
					for (p in players) {
						playerStatus[p.playerNum] = "[" + playerStrength[p.playerNum] + "]"
					}
					playerStatus[defender.playerNum] += getString(" Defender of Catan")
				} else {
					pushStateFront(State.SET_PLAYER, curPlayerNum)
					for (playerNum in defenders) {
						if (getPlayerByPlayerNum(playerNum).getCardCount(CardType.Progress) < rules.maxProgressCards) {
							playerStatus[playerNum] += getString(" Choose Progress Card")
							pushStateFront(State.CHOOSE_PROGRESS_CARD_TYPE)
							pushStateFront(State.SET_PLAYER, playerNum)
						}
					}
				}
			} else {
				printinfo(getString("Catan failed to defend against the Barbarians!"))
				val pilledged: MutableList<Int> = ArrayList()
				for (i in 1 until playerStrength.size) {
					if (playerStrength[i] == minStrength) {
						pilledged.add(i)
					}
				}
				//				assert(pilledged.size() > 0);
				for (playerNum in pilledged) {
					var cities = board.getVertIndicesOfType(playerNum, VertexType.WALLED_CITY)
					if (cities.isNotEmpty()) {
						printinfo(getString("%s has defended their city with a wall", getPlayerByPlayerNum(playerNum).name))
						val cityIndex = Utils.randItem(cities)
						playerStatus[playerNum] += getString(" Defended by Wall")
						val v = board.getVertex(cityIndex)
						v.setPlayerAndType(playerNum, VertexType.CITY)
					} else {
						cities = board.getVertIndicesOfType(playerNum, VertexType.CITY)
						if (cities.isNotEmpty()) {
							printinfo(getString("%s has their city pillaged", getPlayerByPlayerNum(playerNum).name))
							val cityIndex = Utils.randItem(cities)
							playerStatus[playerNum] += getString(" City Pillaged")
							val v = board.getVertex(cityIndex)
							v.setPlayerAndType(playerNum, VertexType.SETTLEMENT)
						}
					}
				}
			}
			onBarbariansAttack(catanStrength, barbarianStrength, playerStatus)
			mBarbarianAttackCount++
			barbarianDistance = rules.barbarianStepsToAttack

			// all knights revert to inactive
			for (v in board.getVertsOfType(0, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.BASIC_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE)) {
				v.setType(v.type.deActivatedType())
			}
		} else {
			onBarbariansAdvanced(barbarianDistance)
		}
	}

	/**
	 * Called when a player takes some resource from another player.  can be called multiple times
	 * in a turn.  default method does nothing.
	 *
	 * @param takerNum
	 * @param giverNum
	 * @param applied
	 * @param amount
	 */
	protected open fun onMonopolyCardApplied(takerNum: Int, giverNum: Int, applied: ICardType<*>, amount: Int) {}
	private fun processMonopoly(type: ICardType<*>) {
		// take the specified resource from all other players
		for (i in 1 until numPlayers) {
			val cur = mPlayers[(mCurrentPlayer + i) % numPlayers]
			var num = cur.getCardCount(type)
			if (num > 0) {
				if (rules.isEnableCitiesAndKnightsExpansion && num > 2) {
					num = 2
				}
				printinfo(getString("%1\$s takes %2\$d %3\$s card from player %4\$s", curPlayer.name, num, type.getNameId(), cur.name))
				onMonopolyCardApplied(curPlayerNum, cur.playerNum, type, num)
				cur.incrementResource(type, -num)
				curPlayer.incrementResource(type, num)
			}
		}
	}

	/**
	 * Called when game over is detected
	 */
	protected open fun onGameOver(winnerNum: Int) {}
	fun save(fileName: String): Boolean {
		try {
			saveToFile(File(fileName))
			return true
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return false
	}

	fun load(fileName: String): Boolean {
		reset()
		val file = File(fileName)
		if (file.exists()) {
			try {
				loadFromFile(file)
				return true
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
		return false
	}

	val helpText: String
		get() = mStateStack.peek()?.let {
			it.state.helpText
		}?:getString("Game not running")	

	/**
	 * @param playerNum
	 * @param knightIndex
	 */
	protected open fun onPlayerKnightDestroyed(playerNum: Int, knightIndex: Int) {}

	/**
	 * @param playerNum
	 * @param knightIndex
	 */
	protected open fun onPlayerKnightDemoted(playerNum: Int, knightIndex: Int) {}

	/**
	 * @param playerNum
	 * @param knightIndex
	 */
	protected open fun onPlayerKnightPromoted(playerNum: Int, knightIndex: Int) {}

	/**
	 * @param playerNum
	 * @param area
	 */
	protected open fun onPlayerCityDeveloped(playerNum: Int, area: DevelopmentArea) {}
	protected open fun onVertexChosen(playerNum: Int, mode: VertexChoice, vertexIndex: Int, v2: Int?) {}
	protected open fun onRouteChosen(playerNum: Int, mode: RouteChoice, routeIndex: Int, shipToMove: Int?) {}
}