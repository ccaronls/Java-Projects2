package cc.game.soc.ui

import cc.game.soc.core.*
import cc.game.soc.core.Dice
import cc.game.soc.core.Player.*
import cc.game.soc.ui.NetCommon.cypher
import cc.lib.annotation.Keep
import cc.lib.game.*
import cc.lib.logger.LoggerFactory
import cc.lib.math.Vector2D
import cc.lib.net.AClientConnection
import cc.lib.net.AGameServer
import cc.lib.net.GameCommand
import cc.lib.net.GameServer
import cc.lib.ui.UIComponent
import cc.lib.utils.Lock

/**
 * Created by chriscaron on 2/22/18.
 */
abstract class UISOC protected constructor(playerComponents: Array<UIPlayerRenderer>, boardRenderer: UIBoardRenderer, diceRenderer: UIDiceRenderer, console: UIConsoleRenderer, eventCardRenderer: UIEventCardRenderer, barbarianRenderer: UIBarbarianRenderer) : SOC(), MenuItem.Action, AGameServer.Listener, AClientConnection.Listener {
	val server = GameServer(serverName, NetCommon.PORT, NetCommon.VERSION, cypher, NetCommon.MAX_CONNECTIONS)
	private val playerComponents: Array<UIPlayerRenderer>
	val uIBoard: UIBoardRenderer
	private val diceRenderer: UIDiceRenderer
	private val console: UIConsoleRenderer
	private val eventCardRenderer: UIEventCardRenderer
	private val barbarianRenderer: UIBarbarianRenderer
	private var returnValue: Any? = null
	private val lock = Lock()

	@Keep
	fun setReturnValue(o: Any?) {
		returnValue = o
		lock.releaseAll()
	}

	fun initUI() {
		playerComponents.forEach {
			it.reset()
		}
		diceRenderer.reset()
		console.reset()
		eventCardRenderer.reset()
		barbarianRenderer.reset()
	}

	override fun runGame() {
		if (server.isConnected) {
			completeMenu()
			super.runGame()
			try {
				server.broadcastCommand(GameCommand(NetCommon.SVR_TO_CL_UPDATE).setArg("diff", toString()))
			} catch (e: Exception) {
				e.printStackTrace()
			}
		} else {
			super.runGame()
		}
	}

	fun chooseMoveMenu(moves: Collection<MoveType>): MoveType? {
		clearMenu()
		val it = moves.iterator()
		while (it.hasNext()) {
			val move = it.next()
			addMenuItem(CHOOSE_MOVE, move.getNameId(), move.getHelpText(rules), move)
		}
		completeMenu()
		return waitForReturnValue<MoveType?>(null)
	}

	fun chooseRouteType(): RouteChoiceType? {
		clearMenu()
		addMenuItem(CHOOSE_ROAD, RouteChoiceType.ROAD_CHOICE)
		addMenuItem(CHOOSE_SHIP, RouteChoiceType.SHIP_CHOICE)
		completeMenu()
		return waitForReturnValue<RouteChoiceType?>(null)
	}

	fun getPlayerColor(playerNum: Int): GColor {
		return if (playerNum < 1)
			GColor.DARK_GRAY
		else (getPlayerByPlayerNum(playerNum) as UIPlayer).color
	}

	fun chooseVertex(vertexIndices: Collection<Int?>, choice: VertexChoice?, knightToMove: Int?): Int? {
		clearMenu()
		addMenuItem(ACCEPT)
		uIBoard.pickHandler = (object : PickHandler {
			override fun onPick(b: UIBoardRenderer, pickedValue: Int) {
				b.pickHandler = null
				setReturnValue(pickedValue)
			}

			override fun onHighlighted(b: UIBoardRenderer, g: APGraphics, highlightedIndex: Int) {
				val v = board.getVertex(highlightedIndex)
				g.color = getPlayerColor(curPlayerNum)
				when (choice) {
					null -> Unit
					VertexChoice.SETTLEMENT -> b.drawSettlement(g, v, v.player, true)
					VertexChoice.CITY -> b.drawCity(g, v, v.player, true)
					VertexChoice.CITY_WALL -> b.drawWalledCity(g, v, v.player, true)
					VertexChoice.KNIGHT_DESERTER,
					VertexChoice.KNIGHT_DISPLACED,
					VertexChoice.KNIGHT_MOVE_POSITION,
					VertexChoice.KNIGHT_TO_MOVE,
					VertexChoice.OPPONENT_KNIGHT_TO_DISPLACE -> {
						var knightLevel = v.type.knightLevel
						var active = v.type.isKnightActive
						if (knightToMove != null) {
							val vv = board.getVertex(knightToMove)
							knightLevel = vv.type.knightLevel
							active = vv.type.isKnightActive
						}
						b.drawKnight(g, v, v.player, knightLevel, active, false)
						g.color = GColor.RED
						b.drawCircle(g, v)
					}
					VertexChoice.KNIGHT_TO_ACTIVATE                                                                                                                                       -> b.drawKnight(g, v, v.player, v.type.knightLevel, true, true)
					VertexChoice.KNIGHT_TO_PROMOTE                                                                                                                                        -> b.drawKnight(g, v, v.player, v.type.knightLevel + 1, v.type.isKnightActive, true)
					VertexChoice.NEW_KNIGHT                                                                                                                                               -> {
						b.drawKnight(g, v, v.player, 1, false, true)
						g.color = GColor.RED
						b.drawCircle(g, v)
					}
					VertexChoice.POLITICS_METROPOLIS                                                                                                                                      -> b.drawMetropolisPolitics(g, v, v.player, true)
					VertexChoice.SCIENCE_METROPOLIS                                                                                                                                       -> b.drawMetropolisScience(g, v, v.player, true)
					VertexChoice.TRADE_METROPOLIS                                                                                                                                         -> b.drawMetropolisTrade(g, v, v.player, true)
					VertexChoice.PIRATE_FORTRESS                                                                                                                                          -> b.drawPirateFortress(g, v, true)
					VertexChoice.OPPONENT_STRUCTURE_TO_ATTACK                                                                                                                             -> b.drawVertex(g, v, v.type, v.player, true)
				}
			}

			override fun onDrawPickable(b: UIBoardRenderer, g: APGraphics, index: Int) {
				val v = board.getVertex(index)
				val color = getPlayerColor(curPlayerNum).withAlpha(RenderConstants.pickableAlpha)
				g.color = color
				when (choice) {
					null -> Unit
					VertexChoice.SETTLEMENT -> b.drawSettlement(g, v, 0, false)
					VertexChoice.CITY                                                                                                                        -> b.drawCity(g, v, 0, false)
					VertexChoice.CITY_WALL                                                                                                                   -> b.drawWalledCity(g, v, 0, false)
					VertexChoice.KNIGHT_DESERTER, VertexChoice.KNIGHT_DISPLACED, VertexChoice.KNIGHT_MOVE_POSITION, VertexChoice.OPPONENT_KNIGHT_TO_DISPLACE -> {
						var knightLevel = v.type.knightLevel
						var active = v.type.isKnightActive
						if (knightToMove != null) {
							val vv = board.getVertex(knightToMove)
							knightLevel = vv.type.knightLevel
							active = vv.type.isKnightActive
						}
						b.drawKnight(g, v, 0, knightLevel, active, false)
						g.color = GColor.YELLOW
						b.drawCircle(g, v)
						g.color = color
					}
					VertexChoice.NEW_KNIGHT                                                                                                                  -> b.drawKnight(g, v, 0, 1, false, false)
					VertexChoice.KNIGHT_TO_MOVE, VertexChoice.KNIGHT_TO_ACTIVATE, VertexChoice.KNIGHT_TO_PROMOTE                                             -> {
						b.drawKnight(g, v, v.player, v.type.knightLevel, v.type.isKnightActive, false)
						g.color = GColor.YELLOW
						b.drawCircle(g, v)
						g.color = color
					}
					VertexChoice.POLITICS_METROPOLIS                                                                                                         -> b.drawMetropolisPolitics(g, v, 0, false)
					VertexChoice.SCIENCE_METROPOLIS                                                                                                          -> b.drawMetropolisScience(g, v, 0, false)
					VertexChoice.TRADE_METROPOLIS                                                                                                            -> b.drawMetropolisTrade(g, v, 0, false)
					VertexChoice.PIRATE_FORTRESS                                                                                                             -> b.drawPirateFortress(g, v, false)
					VertexChoice.OPPONENT_STRUCTURE_TO_ATTACK                                                                                                -> {
						g.color = getPlayerColor(v.player).withAlpha(RenderConstants.pickableAlpha)
						b.drawVertex(g, v, v.type, 0, false)
					}
				}
			}

			override fun onDrawOverlay(b: UIBoardRenderer, g: APGraphics) {}
			override fun isPickableIndex(b: UIBoardRenderer, index: Int): Boolean {
				return vertexIndices.contains(index)
			}

			override val pickMode: PickMode
				get() = PickMode.PM_VERTEX
		})
		completeMenu()
		return waitForReturnValue<Int?>(null)
	}

	fun chooseRoute(edges: Collection<Int?>, choice: RouteChoice?, shipToMove: Route?): Int? {
		clearMenu()
		addMenuItem(ACCEPT)
		val shipType = if (shipToMove == null) RouteType.SHIP else shipToMove.type
		assert(shipType.isVessel)
		if (shipToMove != null) {
			shipToMove.type = RouteType.OPEN
		}
		uIBoard.pickHandler = (object : PickHandler {
			override fun onPick(b: UIBoardRenderer, pickedValue: Int) {
				b.pickHandler = null
				if (shipToMove != null) {
					shipToMove.type = shipType
				}
				setReturnValue(pickedValue)
			}

			override fun onHighlighted(b: UIBoardRenderer, g: APGraphics, highlightedIndex: Int) {
				val route = board.getRoute(highlightedIndex)
				g.color = getPlayerColor(curPlayerNum)
				when (choice) {
					null -> Unit
					RouteChoice.OPPONENT_ROAD_TO_ATTACK -> {
						g.color = getPlayerColor(route.player)
						b.drawEdge(g, route, route.type, 0, true)
					}
					RouteChoice.OPPONENT_SHIP_TO_ATTACK -> TODO()
					RouteChoice.ROAD, RouteChoice.ROUTE_DIPLOMAT -> b.drawRoad(g, route, true)
					RouteChoice.SHIP                             -> b.drawVessel(g, shipType, route, true)
					RouteChoice.SHIP_TO_MOVE                     -> b.drawEdge(g, route, route.type, curPlayerNum, true)
					RouteChoice.UPGRADE_SHIP                     -> b.drawWarShip(g, route, true)
				}
			}

			override fun onDrawPickable(b: UIBoardRenderer, g: APGraphics, index: Int) {
				val route = board.getRoute(index)
				g.color = getPlayerColor(curPlayerNum).withAlpha(RenderConstants.pickableAlpha)
				when (choice) {
					null -> Unit
					RouteChoice.OPPONENT_SHIP_TO_ATTACK,
					RouteChoice.OPPONENT_ROAD_TO_ATTACK -> {
						g.color = getPlayerColor(route.player).withAlpha(RenderConstants.pickableAlpha)
						b.drawEdge(g, route, route.type, 0, false)
					}
					RouteChoice.ROAD, RouteChoice.ROUTE_DIPLOMAT -> b.drawRoad(g, route, false)
					RouteChoice.SHIP -> b.drawVessel(g, shipType, route, false)
					RouteChoice.SHIP_TO_MOVE -> b.drawEdge(g, route, route.type, 0, false)
					RouteChoice.UPGRADE_SHIP -> b.drawWarShip(g, route, false)
				}
			}

			override fun onDrawOverlay(b: UIBoardRenderer, g: APGraphics) {
				// TODO Auto-generated method stub
			}

			override fun isPickableIndex(b: UIBoardRenderer, index: Int): Boolean {
				return edges.contains(index)
			}

			override val pickMode: PickMode
				get() = PickMode.PM_EDGE
		})
		completeMenu()
		return waitForReturnValue<Int?>(null)
	}

	fun chooseTile(tiles: Collection<Int?>, choice: TileChoice?): Int? {
		clearMenu()
		addMenuItem(ACCEPT)
		val robberTile = board.getRobberTile()
		val merchantTileIndex = board.merchantTileIndex
		val merchantTilePlayer = board.merchantPlayer
		board.setRobber(-1)
		board.setMerchant(-1, 0)
		uIBoard.pickHandler = (object : PickHandler {
			override fun onPick(b: UIBoardRenderer, pickedValue: Int) {
				b.pickHandler = null
				board.setRobberTile(robberTile)
				board.setMerchant(merchantTileIndex, merchantTilePlayer)
				setReturnValue(pickedValue)
			}

			override fun onHighlighted(b: UIBoardRenderer, g: APGraphics, highlightedIndex: Int) {
				val t = board.getTile(highlightedIndex)
				when (choice) {
					null -> Unit
					TileChoice.INVENTOR -> {
						g.color = GColor.YELLOW
						b.drawTileOutline(g, t, RenderConstants.thickLineThickness)
					}
					TileChoice.MERCHANT                  -> b.drawMerchant(g, t, curPlayerNum)
					TileChoice.ROBBER, TileChoice.PIRATE -> if (t.isWater) b.drawPirate(g, t) else b.drawRobber(g, t)
				}
			}

			override fun onDrawPickable(b: UIBoardRenderer, g: APGraphics, index: Int) {
				val t = board.getTile(index)
				when (choice) {
					null -> Unit
					TileChoice.ROBBER, TileChoice.PIRATE -> if (t.isWater) b.drawPirate(g, t, GColor.TRANSLUSCENT_BLACK) else b.drawRobber(g, t, GColor.LIGHT_GRAY.withAlpha(RenderConstants.pickableAlpha))
					TileChoice.INVENTOR                  -> {
						g.color = GColor.RED
						g.drawFilledCircle(t.x, t.y, g.textHeight * 2 + 10)
						if (t.dieNum > 0) b.drawCellProductionValue(g, t.x, t.y, t.dieNum)
					}
					TileChoice.MERCHANT                  -> {
						g.color = getPlayerColor(curPlayerNum).withAlpha(RenderConstants.pickableAlpha)
						b.drawMerchant(g, t, 0)
					}
				}
			}

			override fun onDrawOverlay(b: UIBoardRenderer, g: APGraphics) {
				// TODO Auto-generated method stub
			}

			override fun isPickableIndex(b: UIBoardRenderer, index: Int): Boolean {
				return tiles.contains(index)
			}

			override val pickMode: PickMode
				get() = PickMode.PM_TILE
		})
		completeMenu()
		return waitForReturnValue<Int?>(null)
	}

	fun chooseTradeMenu(trades: Collection<Trade>): Trade? {
		clearMenu()
		val it = trades.iterator()
		while (it.hasNext()) {
			val trade = it.next()
			val str = trade.getType().getNameId() + " X " + trade.amount
			addMenuItem(CHOOSE_TRADE, str, "", trade)
		}
		completeMenu()
		return waitForReturnValue<Trade?>(null)
	}

	fun choosePlayerMenu(players: Collection<Int>, mode: PlayerChoice): Int? {
		clearMenu()
		for (num in players) {
			val player = getPlayerByPlayerNum(num)
			when (mode) {
				PlayerChoice.PLAYER_FOR_DESERTION -> {
					val numKnights = board.getNumKnightsForPlayer(player.playerNum)
					addMenuItem(CHOOSE_PLAYER, getString("%1\$s X %2\$d Knights", player.name, numKnights), "", num)
				}
				PlayerChoice.PLAYER_TO_SPY_ON -> addMenuItem(CHOOSE_PLAYER, getString("%1\$s X %2\$d Progress Cards", player.name, player.getUnusedCardCount(CardType.Progress)), "", num)
				PlayerChoice.PLAYER_TO_FORCE_HARBOR_TRADE, PlayerChoice.PLAYER_TO_GIFT_CARD, PlayerChoice.PLAYER_TO_TAKE_CARD_FROM -> addMenuItem(CHOOSE_PLAYER, getString("%1\$s X %2\$d Cards", player.name, player.totalCardsLeftInHand), "", num)
				else                                                                                                               -> {
					System.err.println("ERROR: Unhandled case '$mode'")
					addMenuItem(CHOOSE_PLAYER, getString("%1\$s X %2\$d Cards", player.name, player.totalCardsLeftInHand), "", num)
				}
			}
		}
		completeMenu()
		return waitForReturnValue<Int?>(null)
	}

	fun chooseCardMenu(cards: Collection<Card>): Card? {
		clearMenu()
		for (type in cards) {
			addMenuItem(CHOOSE_CARD, type.name, type.getHelpText(rules)?:"", type)
		}
		completeMenu()
		return waitForReturnValue<Card?>(null)
	}

	fun <T : Enum<T>> chooseEnum(choices: List<T>): T? {
		clearMenu()
		val it = choices.iterator()
		while (it.hasNext()) {
			val choice: Enum<T> = it.next()
			addMenuItem(CHOOSE_MOVE, (choice as ILocalized).getNameId(), "", choice)
		}
		completeMenu()
		return waitForReturnValue(null)
	}

	fun <T> waitForReturnValue(defaultValue: T?): T? {
		returnValue = defaultValue
		lock.acquireAndBlock()
		return returnValue as T?
	}

	protected abstract val serverName: String
	protected abstract fun addMenuItem(item: MenuItem, title: String, helpText: String, extra: Any?)
	abstract fun clearMenu()
	abstract fun redraw()
	fun refreshComponents() {
		val r = getDiceRenderer()
		r.setDice(dice)
		eventCardRenderer.setEventCard(topEventCard)
		barbarianRenderer.setDistance(barbarianDistance)
	}

	open fun chooseOptimalPath(optimal: BotNode?, leafs: List<BotNode>): BotNode? {
		return optimal
	}

	fun addMenuItem(item: MenuItem) {
		addMenuItem(item, item.title, item.helpText, null)
	}

	fun addMenuItem(item: MenuItem, extra: Any?) {
		addMenuItem(item, item.title, item.helpText, extra)
	}

	open fun completeMenu() {
		if (canCancel()) {
			addMenuItem(CANCEL)
		}
	}

	fun getDiceRenderer(): UIDiceRenderer {
		return if (rules.isEnableEventCards) eventCardRenderer.diceComps else diceRenderer
	}

	fun getSetDiceMenu(die: List<Dice>, num: Int): Boolean {
		clearMenu()
		val r = getDiceRenderer()
		r.setDice(die)
		r.setPickableDice(num)
		addMenuItem(SET_DICE)
		completeMenu()
		val result = waitForReturnValue<Any?>(null) as IntArray?
		if (result != null) {
			for (i in result.indices) {
				die[i].setNum(result[i], true)
			}
			r.setPickableDice(0)
			return true
		}
		return false
	}

	var isRunning = false
		private set

	@Synchronized
	fun startGameThread() {
		log.debug("Entering thread")
		assert(!isRunning)
		isRunning = true
		refreshComponents()
		object : Thread() {
			override fun run() {
				try {
					while (isRunning) {
						val enterTime = System.currentTimeMillis()
						runGame()
						if (isRunning) {
							redraw()
							val exitTime = System.currentTimeMillis()
							//make sure we take at least 1 second in between rungame calls
							val dt = (exitTime - enterTime).toInt()
							if (dt in 1..999) {
								lock.acquireAndBlock(dt.toLong())
							}
						}
					}
				} catch (e: Throwable) {
					log.error(e)
					onRunError(e)
				}
				isRunning = false
				log.debug("Exiting thread")
			}
		}.start()
	}

	@Synchronized
	fun stopRunning() {
		if (isRunning) {
			isRunning = false
			notifyWaitObj()
		}
		setReturnValue(null)
		uIBoard.reset()
		console.clear()
	}

	protected open fun onRunError(e: Throwable) {
		e.printStackTrace()
	}

	// in game options
	val CANCEL: MenuItem
	val ACCEPT: MenuItem
	val CHOOSE_MOVE: MenuItem
	val CHOOSE_PLAYER: MenuItem
	val CHOOSE_CARD: MenuItem
	val CHOOSE_TRADE: MenuItem
	val CHOOSE_SHIP: MenuItem
	val CHOOSE_ROAD: MenuItem
	val SET_DICE: MenuItem
	override fun onAction(item: MenuItem, extra: Any?) {
		if (item == CANCEL) {
			uIBoard.pickHandler = null
			cancel()
			notifyWaitObj()
		} else if (item == SET_DICE) {
			returnValue = getDiceRenderer().pickedDiceNums
			notifyWaitObj()
		} else if (item == ACCEPT) {
			if (uIBoard.isPicked) {
				clearMenu()
				uIBoard.acceptPicked()
			}
		} else {
			clearMenu()
			returnValue = extra
			notifyWaitObj()
		}
	}

	fun notifyWaitObj() {
		lock.releaseAll()
	}

	@Keep
	override fun printinfo(playerNum: Int, txt: String) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, playerNum, txt)
		super.printinfo(playerNum, txt)
		console.addText(getPlayerColor(playerNum), txt)
	}

	@Keep
	override fun onDiceRolled(dice: List<Dice>) {
		log.debug("onDiceRolled: $dice")
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, dice)
		val dr = getDiceRenderer()
		val copy = deepCopy(dice)
		dr.spinDice(3000, copy)
		log.debug("Set dice: $dice")
		dr.setDice(dice)
		super.onDiceRolled(dice)
	}

	@Keep
	override fun onEventCardDealt(card: EventCard) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, card)
		eventCardRenderer.setEventCard(card)
		super.onEventCardDealt(card)
	}

	@Keep
	override fun onBarbariansAdvanced(distanceAway: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, distanceAway)
		barbarianRenderer.setDistance(distanceAway)
		super.onBarbariansAdvanced(distanceAway)
	}

	@Keep
	override fun onBarbariansAttack(catanStrength: Int, barbarianStrength: Int, playerStatus: Array<String>) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, catanStrength, barbarianStrength, playerStatus)
		barbarianRenderer.onBarbarianAttack(catanStrength, barbarianStrength, playerStatus)
		val str = StringBuffer(getString("Barbarian Attack!\n\nBarbarian Strength %1\$d\nCatan Strength %2\$d\n", barbarianStrength, catanStrength))
		for (p in players) {
			str.append(p.name).append(" ").append(playerStatus[p.playerNum]).append("\n")
		}
		showOkPopup(getString("Barbarian Attack"), str.toString())
		super.onBarbariansAttack(catanStrength, barbarianStrength, playerStatus)
		uIBoard.getComponent<UIComponent>().redraw()
	}

	private fun getRendererForPlayerNum(playerNum: Int): UIPlayerRenderer {
		for (pr in playerComponents) {
			if (pr.playerNum == playerNum) return pr
		}
		log.error("No player component for playerNum: %d", playerNum)
		return playerComponents[0]
	}

	protected fun addCardAnimation(playerNum: Int, text: String?) {
		val comp = getRendererForPlayerNum(playerNum)
		val cardHeight = (uIBoard.getComponent<UIComponent>().getHeight() / 5).toFloat()
		val cardWidth = cardHeight * 2 / 3
		val compPt = comp.getComponent<UIComponent>().getViewportLocation()
		val boardPt = uIBoard.getComponent<UIComponent>().getViewportLocation()
		val dv: Vector2D = compPt.sub(boardPt)
		val spacing = cardWidth / 4

		// center the card vertically against its player getComponent()
		var _y = compPt.y - boardPt.y + comp.getComponent<UIComponent>().getHeight() / 2 - cardHeight / 2
		val W = comp.numCardAnimations * cardWidth + spacing - (comp.numCardAnimations - 1) * spacing
		val x = if (boardPt.X() < compPt.x) uIBoard.getComponent<UIComponent>().getWidth() - cardWidth - W else W
		if (_y < 0) {
			_y = 0f
		} else if (_y + cardHeight > uIBoard.getComponent<UIComponent>().getHeight()) {
			_y = uIBoard.getComponent<UIComponent>().getHeight() - cardHeight
		}
		val y = _y
		val color = (getPlayerByPlayerNum(playerNum) as UIPlayer).color
		comp.numCardAnimations++
		uIBoard.addAnimation(true, object : UIAnimation(4000) {
			override fun draw(g: AGraphics, position: Float, dt: Float) {
				var alpha = 1f
				if (elapsedTime < 500) {
					alpha = elapsedTime.toFloat() / 500
				} else if (duration > 3500) {
					alpha = (4000 - elapsedTime).toFloat() / 500
				}
				uIBoard.drawCard(color, g, text, x, y, cardWidth, cardHeight, alpha)
			}

			override fun onDone() {
				super.onDone()
				comp.numCardAnimations--
			}
		}, false)
		Utils.waitNoThrow(this, 500)
	}

	@Keep
	override fun onCardPicked(playerNum: Int, card: Card) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, playerNum, card)
		var txt = card.cardType.getNameId()
		val player = getPlayerByPlayerNum(playerNum)
		if ((player as UIPlayer).isInfoVisible) {
			txt = card.name
		}
		addCardAnimation(playerNum, txt)
		super.onCardPicked(playerNum, card)
	}

	@Keep
	override fun onDistributeResources(player: Int, type: ResourceType, amount: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, type, amount)
		addCardAnimation(player, type.getNameId() + "\nX " + amount)
		super.onDistributeResources(player, type, amount)
	}

	@Keep
	override fun onDistributeCommodity(player: Int, type: CommodityType, amount: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, type, amount)
		addCardAnimation(player, type.getNameId() + "\nX " + amount)
		super.onDistributeCommodity(player, type, amount)
	}

	@Keep
	override fun onProgressCardDistributed(player: Int, type: ProgressCardType) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, type)
		var txt: String? = type.getNameId()
		if (!(getPlayerByPlayerNum(player) as UIPlayer).isInfoVisible) {
			txt = CardType.Progress.getNameId()
		}
		addCardAnimation(player, txt)
		super.onProgressCardDistributed(player, type)
	}

	@Keep
	override fun onSpecialVictoryCard(player: Int, type: SpecialVictoryType) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, type)
		addCardAnimation(player, type.getNameId())
		super.onSpecialVictoryCard(player, type)
	}

	@Keep
	override fun onLargestArmyPlayerUpdated(oldPlayer: Int, newPlayer: Int, armySize: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, oldPlayer, newPlayer, armySize)
		if (newPlayer > 0) addCardAnimation(newPlayer, getString("Largest Army"))
		if (oldPlayer > 0) addCardAnimation(oldPlayer, getString("Largest Army Lost!"))
		super.onLargestArmyPlayerUpdated(oldPlayer, newPlayer, armySize)
	}

	@Keep
	override fun onLongestRoadPlayerUpdated(oldPlayer: Int, newPlayer: Int, maxRoadLen: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, oldPlayer, newPlayer, maxRoadLen)
		if (newPlayer > 0) addCardAnimation(newPlayer, getString("Longest Road"))
		if (oldPlayer > 0) addCardAnimation(oldPlayer, getString("Longest Road Lost!"))
		super.onLongestRoadPlayerUpdated(oldPlayer, newPlayer, maxRoadLen)
	}

	@Keep
	override fun onHarborMasterPlayerUpdated(oldPlayer: Int, newPlayer: Int, harborPts: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, oldPlayer, newPlayer, harborPts)
		if (newPlayer > 0) addCardAnimation(newPlayer, getString("Harbor Master"))
		if (oldPlayer > 0) addCardAnimation(oldPlayer, getString("Harbor Master Lost!"))
		super.onHarborMasterPlayerUpdated(oldPlayer, newPlayer, harborPts)
	}

	@Keep
	override fun onMonopolyCardApplied(taker: Int, giver: Int, type: ICardType<*>, amount: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, taker, giver, type, amount)
		addCardAnimation(giver, """
 	${type.getNameId()}
 	- $amount
 	""".trimIndent())
		addCardAnimation(taker, """
 	${type.getNameId()}
 	+ $amount
 	""".trimIndent())
		super.onMonopolyCardApplied(taker, giver, type, amount)
	}

	@Keep
	override fun onPlayerPointsChanged(player: Int, changeAmount: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, changeAmount)
		super.onPlayerPointsChanged(player, changeAmount)
	}

	@Keep
	override fun onTakeOpponentCard(taker: Int, giver: Int, card: Card) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, taker, giver, card)
		addCardAnimation(giver, """
 	${card.name}
 	-1
 	""".trimIndent())
		addCardAnimation(taker, """
 	${card.name}
 	+1
 	""".trimIndent())
		super.onTakeOpponentCard(taker, giver, card)
	}

	@Keep
	override fun onPlayerRoadLengthChanged(p: Int, oldLen: Int, newLen: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, p, oldLen, newLen)
		if (oldLen > newLen) addCardAnimation(p, getString("Route Reduced!\n-%d", oldLen - newLen)) else addCardAnimation(p, getString("Route Increased!\n+%d", newLen - oldLen))
		super.onPlayerRoadLengthChanged(p, oldLen, newLen)
	}

	@Keep
	override fun onCardsTraded(player: Int, trade: Trade) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, trade)
		addCardAnimation(player, getString("Trade\n %1\$s\n -%2\$d", trade.getType(), trade.amount))
		super.onCardsTraded(player, trade)
	}

	@Keep
	override fun onPlayerDiscoveredIsland(player: Int, island: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, island)
		addCardAnimation(player, getString("Island %s\nDiscovered!", island))
		super.onPlayerDiscoveredIsland(player, island)
	}

	@Keep
	override fun onDiscoverTerritory(player: Int, tile: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, tile)
		addCardAnimation(player, getString("Territory\nDiscovered"))
		uIBoard.clearCached()
		super.onDiscoverTerritory(player, tile)
	}

	@Keep
	override fun onMetropolisStolen(loser: Int, stealer: Int, area: DevelopmentArea) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, loser, stealer, area)
		addCardAnimation(loser, getString("Metropolis\n%s\nLost!", area.getNameId()))
		addCardAnimation(stealer, getString("Metropolis\n%s\nLost!", area.getNameId()))
		super.onMetropolisStolen(loser, stealer, area)
	}

	@Keep
	override fun onTilesInvented(player: Int, tile0: Int, tile1: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, tile0, tile1)
		uIBoard.clearCached()
		uIBoard.startTilesInventedAnimation(board.getTile(tile0), board.getTile(tile1))
		uIBoard.clearCached()
		super.onTilesInvented(player, tile0, tile1)
	}

	@Keep
	override fun onPlayerShipUpgraded(playerNum: Int, routeIndex: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, playerNum, routeIndex)
		super.onPlayerShipUpgraded(playerNum, routeIndex)
	}

	@Keep
	override fun onPirateSailing(fromTile: Int, toTile: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, fromTile, toTile)
		uIBoard.addAnimation(object : UIAnimation(800) {
			public override fun draw(g: AGraphics, position: Float, dt: Float) {
				val v: Vector2D = Vector2D.newTemp(board.getTile(fromTile)).scaledBy(1 - position).add(Vector2D.newTemp(board.getTile(toTile)).scaledBy(position))
				uIBoard.drawPirate(g, v)
			}
		}, true)
		super.onPirateSailing(fromTile, toTile)
	}

	@Keep
	override fun onCardLost(playerNum: Int, c: Card) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, playerNum, c)
		addCardAnimation(playerNum, """
 	${c.name}
 	-1
 	""".trimIndent())
		super.onCardLost(playerNum, c)
	}

	@Keep
	override fun onPirateAttack(playerNum: Int, playerStrength: Int, pirateStrength: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, playerNum, playerStrength, pirateStrength)
		showOkPopup(getString("Pirate Attack"), getString("Pirates attack %1\$s\nPlayer Strength %2\$d\nPirate Strength %3\$d\n", getPlayerByPlayerNum(playerNum).name, playerStrength, pirateStrength))
		super.onPirateAttack(playerNum, playerStrength, pirateStrength)
	}

	/**
	 * Show a popup and block until a button is pressed and return the index of the button pressed
	 *
	 * @param title
	 * @param message
	 * @return
	 */
	abstract fun showOkPopup(title: String, message: String)
	protected abstract fun showChoicePopup(title: String, choices: List<String>): String?

	@Keep
	override fun onPlayerConqueredPirateFortress(p: Int, v: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, p, v)
		showOkPopup(getString("Pirate Attack"), getString("Player %s has conquered the fortress!", getPlayerByPlayerNum(p).name))
		super.onPlayerConqueredPirateFortress(p, v)
	}

	@Keep
	override fun onPlayerAttacksPirateFortress(p: Int, playerHealth: Int, pirateHealth: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, p, playerHealth, pirateHealth)
		var result: String? = null
		result = if (playerHealth > pirateHealth) getString("Player damages the fortress") else if (playerHealth < pirateHealth) getString("Player loses battle and 2 ships") else getString("Battle is a draw.  Player lost a ship")
		showOkPopup(getString("Pirate Attack"), getString("%1\$s attacks the pirate fortress!\nPlayer Strength %2\$d\nPirate Strength %3\$d\nResult:\n%4\$s",
			getPlayerByPlayerNum(p).name, playerHealth, pirateHealth, result))
		super.onPlayerAttacksPirateFortress(p, playerHealth, pirateHealth)
	}

	@Keep
	override fun onAqueduct(playerNum: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, playerNum)
		addCardAnimation(playerNum, getString("Aqueduct Ability!"))
		super.onAqueduct(playerNum)
	}

	@Keep
	override fun onPlayerAttackingOpponent(attackerNum: Int, victimNum: Int, attackingWhat: String, attackerScore: Int, victimScore: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, attackerNum, victimNum, attackingWhat, attackerScore, victimNum)
		val attacker = getPlayerByPlayerNum(attackerNum)
		val victim = getPlayerByPlayerNum(victimNum)
		showOkPopup(getString("Player Attack!"), getString("%1\$s is attacking %2\$s's %3\$s\n%4\$s's score : %5\$d\n%6\$s's score : %7\$d", attacker.name, victim.name, attackingWhat, attacker.name, attackerScore, victim.name, victimScore))
		super.onPlayerAttackingOpponent(attackerNum, victimNum, attackingWhat, attackerScore, victimScore)
	}

	@Keep
	override fun onRoadDestroyed(rIndex: Int, destroyer: Int, victim: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, rIndex, destroyer, victim)
		addCardAnimation(victim, getString("Road Destroyed"))
		super.onRoadDestroyed(rIndex, destroyer, victim)
	}

	@Keep
	override fun onStructureDemoted(vIndex: Int, newType: VertexType, destroyer: Int, victim: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, vIndex, newType, destroyer, victim)
		addCardAnimation(victim, getString("%1\$s Reduced to %2\$s", board.getVertex(vIndex).type.name, newType.name))
		super.onStructureDemoted(vIndex, newType, destroyer, victim)
	}

	@Keep
	override fun onExplorerPlayerUpdated(oldPlayer: Int, newPlayer: Int, harborPts: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, oldPlayer, newPlayer, harborPts)
		if (oldPlayer > 0) addCardAnimation(oldPlayer, getString("Explorer Lost!"))
		addCardAnimation(newPlayer, getString("Explorer Gained!"))
		super.onExplorerPlayerUpdated(oldPlayer, newPlayer, harborPts)
	}

	@Keep
	override fun onPlayerKnightDestroyed(player: Int, knight: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, knight)
		addFloatingTextAnimation(getPlayerByPlayerNum(player) as UIPlayer, board.getVertex(knight), getString("Knight\nDestroyed"))
		super.onPlayerKnightDestroyed(player, knight)
	}

	@Keep
	override fun onPlayerKnightDemoted(player: Int, knightIndex: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, knightIndex)
		val knight = board.getVertex(knightIndex)
		addFloatingTextAnimation(getPlayerByPlayerNum(player) as UIPlayer, knight, getString("Demoted to\n%s", knight.type.name))
		super.onPlayerKnightDemoted(player, knightIndex)
	}

	fun addFloatingTextAnimation(p: UIPlayer, v: IVector2D?, msg: String?) {
		uIBoard.addAnimation(object : UIAnimation(5000) {
			var dim: GDimension? = null
			public override fun draw(g: AGraphics, position: Float, dt: Float) {
				val width = uIBoard.getComponent<UIComponent>().getWidth().toFloat()
				val height = uIBoard.getComponent<UIComponent>().getHeight().toFloat()
				val margin = RenderConstants.textMargin
				val m2 = margin * 2
				if (dim == null) {
					dim = g.getTextDimension(msg, width)
				}

				// draw a rectangle in either upper half of screen or lower half of screen
				// with an arrow pointing to the vertex to be modified.
				val mv: Vector2D = g.transform(v)
				g.pushMatrix()
				g.setIdentity()
				g.color = GColor.LIGHT_GRAY
				if (mv.y < height / 2) {
					// lower half
					g.drawFilledRect(width / 2 - dim!!.width / 2 - margin, height * 2 / 3, dim!!.width + m2, dim!!.height + m2)
					// arrow
					g.begin()
					g.vertex(width / 2 - margin, height * 2 / 3)
					g.vertex(width / 2 + margin, height * 2 / 3)
					g.vertex(mv)
					g.drawTriangles()
					g.end()
					g.translate(width / 2, height * 2 / 3 + margin)
				} else {
					// upper half
					val hgt = height / 3 - dim!!.height - m2
					g.drawFilledRect(width / 2 - dim!!.width / 2 - margin, hgt, dim!!.width + m2, dim!!.height + m2)
					// arrow
					g.begin()
					g.vertex(width / 2 - margin, hgt + g.textHeight)
					g.vertex(width / 2 + margin, hgt + g.textHeight)
					g.vertex(mv)
					g.drawTriangles()
					g.end()
					g.translate(width / 2, height / 3 - dim!!.height - margin)
				}
				if (timeRemaining < 1000) {
					g.color = p.color.withAlpha(0.001f * timeRemaining)
				} else {
					g.color = p.color
				}
				g.drawJustifiedString(0f, 0f, Justify.CENTER, Justify.TOP, msg)
				g.popMatrix()
			}
		}, false)
	}

	@Keep
	override fun onPlayerKnightPromoted(player: Int, knightIndex: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, player, knightIndex)
		val knight = board.getVertex(knightIndex)
		addFloatingTextAnimation(getPlayerByPlayerNum(player) as UIPlayer, knight, getString("Promoted to\n%s", knight.type.name))
		super.onPlayerKnightPromoted(player, knightIndex)
	}

	@Keep
	override fun onPlayerCityDeveloped(p: Int, area: DevelopmentArea) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, p, area)
		addCardAnimation(p, """
 	${area.getNameId()}

 	${area.getLevelName(getPlayerByPlayerNum(p).getCityDevelopment(area))}
 	""".trimIndent())
		super.onPlayerCityDeveloped(p, area)
	}

	@Keep
	override fun onRoadDamaged(r: Int, destroyer: Int, victim: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, r, destroyer, victim)
		val road = board.getRoute(r)
		addFloatingTextAnimation(getPlayerByPlayerNum(road.player) as UIPlayer, board.getRouteMidpoint(road), getString("Road Damaged.\nCannot build another\nuntil it is repaired"))
		super.onRoadDamaged(r, destroyer, victim)
	}

	@Keep
	override fun onPlayerShipComandeered(taker: Int, shipTaken: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, taker, shipTaken)
		val ship = board.getRoute(shipTaken)
		val tPlayer = getPlayerByPlayerNum(taker)
		addFloatingTextAnimation(getPlayerByPlayerNum(ship.player) as UIPlayer, board.getRouteMidpoint(ship), getString("Ship Commandeered by %s", tPlayer.name))
		super.onPlayerShipComandeered(taker, shipTaken)
	}

	@Keep
	override fun onPlayerShipDestroyed(r: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, r)
		val ship = board.getRoute(r)
		addFloatingTextAnimation(getPlayerByPlayerNum(ship.player) as UIPlayer, board.getRouteMidpoint(ship), getString("Ship Destroyed"))
		super.onPlayerShipDestroyed(r)
	}

	@Keep
	override fun onGameOver(winnerNum: Int) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, winnerNum)
		uIBoard.addAnimation(object : UIAnimation(700, -1, true) {
			override fun draw(g: AGraphics, position: Float, dt: Float) {
				val player = getPlayerByPlayerNum(winnerNum) as UIPlayer
				val txt = getString("%s WINS!!!", player.name)
				val width = g.getTextWidth(txt)
				val ratio = g.viewportWidth / (width * 2)
				val oldHgt = g.textHeight
				val targetHeight = ratio * oldHgt
				val minHeight = targetHeight * 0.8f
				val maxHeight = targetHeight * 1.2f
				g.textHeight = minHeight + (maxHeight - minHeight) * position
				g.color = player.color
				g.pushMatrix()
				g.setIdentity()
				g.translate((g.viewportWidth / 2).toFloat(), (g.viewportHeight / 2).toFloat())
				g.drawJustifiedString(0f, 0f, Justify.CENTER, Justify.CENTER, txt)
				g.popMatrix()
				g.textHeight = oldHgt
			}
		}, false)
		isRunning = false
		super.onGameOver(winnerNum)
	}

	override fun onConnected(conn: AClientConnection) {
		var player: UIPlayer? = null
		for (p in players) {
			if (p !is UIPlayerUser) {
				val uip = p as UIPlayer
				if (uip.connection == null) {
					uip.connect(conn)
					player = uip
				} else {
					if (uip.connection!!.isConnected && uip.connection!!.name == conn.name) return  // already connected
				}
			}
		}
		if (player == null) {
			if (isRunning) {
				conn.disconnect(getString("Game Full"))
			} else if (numPlayers >= rules.maxPlayers) {
				conn.disconnect(getString("Game Full"))
			} else {
				// made it here so it means we were not able to assign, so add a new player!
				player = UIPlayer()
				player.color = availableColors.values.iterator().next()
				player.connect(conn)
				addPlayer(player)
			}
		}
		if (player != null) {
			conn.addListener(this)
			try {
				conn.sendCommand(GameCommand(NetCommon.SVR_TO_CL_INIT)
					.setArg("numPlayers", numPlayers)
					.setArg("playerNum", player.playerNum)
					.setArg("soc", this))
			} catch (e: Exception) {
				e.printStackTrace()
			}
			printinfo(0, getString("Player %s has joined", conn.name))
		}
	}

	override fun onReconnected(conn: AClientConnection) {
		printinfo(0, getString("Player %s has disconnected", conn.name))
	}

	override fun onCommand(c: AClientConnection, cmd: GameCommand) {}
	override fun onDisconnected(c: AClientConnection, reason: String) {}
	override fun onCancelled(c: AClientConnection, id: String) {
		cancel()
	}

	/**
	 *
	 * @param board
	 */
	override fun setBoard(board: Board) {
		super.setBoard(board)
		uIBoard.getComponent<UIComponent>().redraw()
	}

	open val isAITuningEnabled: Boolean
		get() = false
	val availableColors: Map<String, GColor>
		get() {
			val colors = allColors
			for (i in 0 until numPlayers) {
				val color = getPlayerColor(i)
				for ((key, value) in colors) {
					if (value == color) {
						colors.remove(key)
						break
					}
				}
			}
			return colors
		}
	private val animTime: Long
		private get() = 1500

	fun startStructureAnimation(playerNum: Int, vertex: Vertex?, type: VertexType?) {
		if (vertex == null) return
		val board = instance!!.uIBoard
		vertex.setOpen()
		board.addAnimation(object : UIAnimation(animTime) {
			public override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.color = getPlayerColor(playerNum)
				g.pushMatrix()
				g.translate(vertex)
				g.translate(0f, (1 - position) * board.structureRadius * 5)
				g.scale(1f, position)
				when (type) {
					VertexType.SETTLEMENT          -> board.drawSettlement(g, Vector2D.ZERO, playerNum, false)
					VertexType.CITY                -> board.drawCity(g, Vector2D.ZERO, playerNum, false)
					VertexType.WALLED_CITY         -> board.drawWalledCity(g, Vector2D.ZERO, playerNum, false)
					VertexType.METROPOLIS_POLITICS -> board.drawMetropolisPolitics(g, Vector2D.ZERO, playerNum, false)
					VertexType.METROPOLIS_SCIENCE  -> board.drawMetropolisScience(g, Vector2D.ZERO, playerNum, false)
					VertexType.METROPOLIS_TRADE -> board.drawMetropolisTrade(g, Vector2D.ZERO, playerNum, false)
					else -> TODO("Unhandled type $type")
				}
				g.popMatrix()
			}
		}, true)
	}

	fun startMoveShipAnimation(playerNum: Int, source: Route?, target: Route?, soc: SOC?) {
		if (source == null || target == null || soc == null) return
		val shipType = source.type
		source.type = RouteType.OPEN
		val comp = (soc as UISOC).uIBoard
		comp.addAnimation(object : UIAnimation(animTime) {
			public override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.color = getPlayerColor(playerNum)
				g.pushMatrix()
				//render.translate(mp);
				//render.scale(1, position);
				val startV = soc.board.getRouteMidpoint(source)
				val endV = soc.board.getRouteMidpoint(target)
				val curV: Vector2D = startV.add(endV.sub(startV).scaledBy(position))
				val startAng = comp.getEdgeAngle(source).toFloat()
				val endAng = comp.getEdgeAngle(target).toFloat()
				val curAng = startAng + (endAng - startAng) * position
				comp.drawVessel(g, shipType, curV, Math.round(curAng), false)
				g.popMatrix()
			}
		}, true)
	}

	fun startBuildShipAnimation(playerNum: Int, edge: Route?, soc: SOC?) {
		if (edge == null || soc == null) return
		val comp = (soc as UISOC).uIBoard
		comp.addAnimation(object : UIAnimation(animTime) {
			public override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.color = getPlayerColor(playerNum)
				g.pushMatrix()
				//render.translate(mp);
				g.scale(1f, position)
				comp.drawShip(g, edge, false)
				g.popMatrix()
			}
		}, true)
	}

	fun startUpgradeShipAnimation(playerNum: Int, ship: Route?) {
		if (ship == null) return
		val comp = instance!!.uIBoard
		ship.type = RouteType.OPEN
		comp.addAnimation(object : UIAnimation(2000) {
			public override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.color = getPlayerColor(playerNum)
				g.pushMatrix()
				//render.translate(mp);
				g.scale(1f, 1.0f - position)
				comp.drawShip(g, ship, false)
				g.popMatrix()
				g.pushMatrix()
				//render.translate(mp);
				g.scale(1f, position)
				comp.drawWarShip(g, ship, false)
				g.popMatrix()
			}
		}, true)
	}

	fun startRoadAnimation(playerNum: Int, edge: Route?, soc: SOC?) {
		if (edge == null || soc == null) return
		val comp = instance!!.uIBoard
		if (edge != null) {
			comp.addAnimation(object : UIAnimation(animTime) {
				val A = soc.board.getVertex(edge.from)
				val B = soc.board.getVertex(edge.to)
				public override fun draw(g: AGraphics, position: Float, dt: Float) {
					val from: Vertex
					val to: Vertex
					if (A.player == playerNum || soc.board.isVertexAdjacentToPlayerRoad(edge.from, playerNum)) {
						from = A
						to = B
					} else {
						from = B
						to = A
					}
					val dx = (to.x - from.x) * position
					val dy = (to.y - from.y) * position
					g.begin()
					g.vertex(from)
					g.vertex(from.x + dx, from.y + dy)
					g.color = getPlayerColor(playerNum)
					g.drawLines(RenderConstants.thickLineThickness)
				}
			}, true)
		}
	}

	fun startKnightAnimation(playerNum: Int, vertex: Vertex) {
		val comp = instance!!.uIBoard
		comp.addAnimation(object : UIAnimation(animTime) {
			public override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.color = getPlayerColor(playerNum)
				g.pushMatrix()
				g.translate(vertex.x, position * vertex.y)
				g.scale((2f - position) * Math.cos(((1 - position) * 20).toDouble()).toFloat(), 2f - position)
				comp.drawKnight(g, Vector2D.ZERO, playerNum, 1, false, false)
				g.popMatrix()
			}
		}, true)
	}

	fun startMoveKnightAnimation(playerNum: Int, fromVertex: Vertex, toVertex: Vertex?) {
		val comp = instance!!.uIBoard
		val knightType = fromVertex.type
		fromVertex.setOpen()
		comp.addAnimation(object : UIAnimation(animTime) {
			public override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.color = getPlayerColor(playerNum)
				g.pushMatrix()
				val pos: IVector2D = Vector2D.newTemp(fromVertex).add(Vector2D.newTemp(toVertex).sub(fromVertex).scaledBy(position))
				g.translate(pos)
				comp.drawKnight(g, Vector2D.ZERO, playerNum, knightType.knightLevel, knightType.isKnightActive, false)
				g.popMatrix()
			}
		}, true)
	}

	override fun onVertexChosen(playerNum: Int, mode: VertexChoice, vIndex: Int, v2: Int?) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, playerNum, mode, vIndex, v2)
		val v = board.getVertex(vIndex) ?: return
		when (mode) {
			VertexChoice.CITY -> startStructureAnimation(playerNum, v, VertexType.CITY)
			VertexChoice.CITY_WALL -> startStructureAnimation(playerNum, v, VertexType.WALLED_CITY)
			VertexChoice.KNIGHT_DESERTER -> {
			}
			VertexChoice.KNIGHT_DISPLACED, VertexChoice.KNIGHT_MOVE_POSITION -> if (v2 != null) startMoveKnightAnimation(playerNum, board.getVertex(v2), v)
			VertexChoice.NEW_KNIGHT -> startKnightAnimation(playerNum, v)
			VertexChoice.KNIGHT_TO_ACTIVATE -> {
			}
			VertexChoice.KNIGHT_TO_MOVE -> {
			}
			VertexChoice.KNIGHT_TO_PROMOTE -> {
			}
			VertexChoice.OPPONENT_KNIGHT_TO_DISPLACE -> {
			}
			VertexChoice.POLITICS_METROPOLIS -> startStructureAnimation(playerNum, v, VertexType.METROPOLIS_POLITICS)
			VertexChoice.SCIENCE_METROPOLIS -> startStructureAnimation(playerNum, v, VertexType.METROPOLIS_SCIENCE)
			VertexChoice.SETTLEMENT -> startStructureAnimation(playerNum, v, VertexType.SETTLEMENT)
			VertexChoice.TRADE_METROPOLIS -> startStructureAnimation(playerNum, v, VertexType.METROPOLIS_TRADE)
			VertexChoice.PIRATE_FORTRESS -> {
			}
			VertexChoice.OPPONENT_STRUCTURE_TO_ATTACK -> {
			}
		}
	}

	override fun onRouteChosen(playerNum: Int, mode: RouteChoice, routeIndex: Int, shipToMove: Int?) {
		server.broadcastExecuteOnRemote(NetCommon.SOC_ID, playerNum, mode, routeIndex, shipToMove)
		val route = board.getRoute(routeIndex)
		when (mode) {
			RouteChoice.ROAD -> startRoadAnimation(playerNum, route, this)
			RouteChoice.ROUTE_DIPLOMAT -> {
			}
			RouteChoice.SHIP -> if (shipToMove != null) {
				startMoveShipAnimation(playerNum, board.getRoute(shipToMove), route, this)
			} else {
				startBuildShipAnimation(playerNum, route, this)
			}
			RouteChoice.SHIP_TO_MOVE -> {
			}
			RouteChoice.UPGRADE_SHIP -> startUpgradeShipAnimation(playerNum, route)
			RouteChoice.OPPONENT_ROAD_TO_ATTACK, RouteChoice.OPPONENT_SHIP_TO_ATTACK -> {
			}
		}
	}

	companion object {
		private val log = LoggerFactory.getLogger(UISOC::class.java)
		lateinit var instance: UISOC
			private set
		val allColors: MutableMap<String, GColor>
			get() {
				val colors = HashMap<String, GColor>()
				colors["RED"] = GColor.RED
				colors["GREEN"] = GColor.GREEN
				colors["BLUE"] = GColor.BLUE.lightened(.2f)
				colors["YELLOW"] = GColor.YELLOW
				colors["ORANGE"] = GColor.ORANGE
				colors["PINK"] = GColor.PINK
				return colors
			}
	}

	init {
		instance = this
		server.addListener(this)
		this.playerComponents = playerComponents
		uIBoard = boardRenderer
		this.diceRenderer = diceRenderer
		this.eventCardRenderer = eventCardRenderer
		this.barbarianRenderer = barbarianRenderer
		this.console = console
		MAX_PLAYERS = playerComponents.size
		CANCEL = MenuItem(getString("Cancel"), getString("Cancel current operation"), this)
		ACCEPT = MenuItem(getString("Accept"), "", this)
		CHOOSE_MOVE = MenuItem("--", "", this)
		CHOOSE_PLAYER = MenuItem("--", "", this)
		CHOOSE_CARD = MenuItem("--", "", this)
		CHOOSE_TRADE = MenuItem("--", "", this)
		CHOOSE_SHIP = MenuItem(getString("Ships"), getString("Show ship choices"), this)
		CHOOSE_ROAD = MenuItem(getString("Roads"), getString("Show road choices"), this)
		SET_DICE = MenuItem(getString("Set Dice"), getString("Click the dice to set value manually"), this)
	}
}