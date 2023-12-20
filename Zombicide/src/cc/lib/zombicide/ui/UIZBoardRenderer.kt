package cc.lib.zombicide.ui

import cc.lib.game.*
import cc.lib.logger.LoggerFactory
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.ui.IButton
import cc.lib.ui.UIRenderer
import cc.lib.utils.*
import cc.lib.utils.Grid.Pos
import cc.lib.zombicide.*
import cc.lib.zombicide.ZDir.Companion.compassValues
import cc.lib.zombicide.ZDir.Companion.elevationValues
import cc.lib.zombicide.anims.HoverMessage
import cc.lib.zombicide.anims.OverlayTextAnimation
import cc.lib.zombicide.anims.ZoomAnimation
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class MiniMapMode {
	OFF,
	UL,
	UR,
	BL,
	BR
}

enum class ZoomType {
	UNDEFINED, // the user has zoomed to an unknown position
	CROP_FIT, // max amount of board is shown that fills screen
	X2,  // zoom in half way between CROP_FIT and MAX
	MAX, // max zoom shows 3 cells square
	FILL_FIT // whole board is shown with blank areas 'filled'
}

open class UIZBoardRenderer(component: UIZComponent<*>) : UIRenderer(component) {
	private val preActor: MutableList<ZAnimation> = Collections.synchronizedList(ArrayList())
	private val postActor: MutableList<ZAnimation> = Collections.synchronizedList(ArrayList())
	private val overlayAnimations: MutableList<ZAnimation> = Collections.synchronizedList(ArrayList())
	private var zoomAnimation: ZAnimation? = null
		set(value) {
			if (value != null && field == null) {
				listeners.toList().forEach {
					it.onAnimateZoomBegin()
				}
			} else if (value == null && field != null) {
				listeners.toList().forEach {
					it.onAnimateZoomEnd(_zoomedRect)
				}
			}

			field = value
		}
	private val clickables: MutableMap<IRectangle, MutableList<IButton>> = TreeMap()
	private var highlightedCell: Pos? = null
		private set
	private var highlightedResult: Any? = null
		private set
	var highlightedActor: ZActor? = null
		private set

	//	var highlightedDoor: ZDoor? = null
	private var selectedCell: Grid.Pos? = null
	private var highlightedShape: IRectangle? = null

	//	var highlightedMoves: List<IButton>? = null
//	var highlightedMovesPos: Vector2D? = null
	private var actorsAnimating = false
	private var overlayToDraw: Any? = null
	var drawTiles = false
	var drawDebugText = false
	var drawRangedAccessibility = false
	var drawTowersHighlighted = false
	var drawZombiePaths = false
	var drawScreenCenter = false
	var drawClickable = false
	var miniMapMode = MiniMapMode.OFF

	//private var dragOffset = MutableVector2D(Vector2D.ZERO)
	//private var dragStart = Vector2D.ZERO
	var isFocussed = false
	var savedCenter: IVector2D? = null
	var lastUsage = 0
	var tiles: Array<ZTile> = arrayOf()
	var tileIds = IntArray(0)

	//	var zoomPercent = 0f
	private val actorMap = mutableMapOf<ZActor, MutableList<HoverMessage>>()
	var desiredZoomType = ZoomType.CROP_FIT
	var currentZoomType = ZoomType.UNDEFINED
	private val board: ZBoard
		get() = UIZombicide.instance.board
	var quest: ZQuest? = null
		set(value) {
			if (field?.quest != value?.quest) {
				clearTiles()
			}
			field = value
		}

	var boardMessage: String? = null
		set(value) {
			field = value
			redraw()
		}
	private val listeners = HashSet<Listener>()
	private val R = Renderer(this)

	private val zoomRectStack = Stack<GRectangle>().also {
		it.push(GRectangle())
	}

	private val _zoomedRect: GRectangle
		get() = zoomRectStack.peek()!!

	private var viewport = GDimension()

	fun pushZoomRect() {
		zoomRectStack.push(_zoomedRect.deepCopy())
	}

	fun popZoomRect() {
		if (zoomRectStack.size > 1) {
			animateZoomTo(zoomRectStack[zoomRectStack.size - 2])
			waitForAnimations()
			zoomRectStack.pop()
		}
	}

	fun popAllZoomRects() {
		if (zoomRectStack.size > 1) {
			animateZoomTo(zoomRectStack[0])
			waitForAnimations()
			while (zoomRectStack.size > 1)
				zoomRectStack.pop()
		}
	}

	fun getZoomedRect(): GRectangle = GRectangle(_zoomedRect)

	fun setZoomedRect(rect: IRectangle) {
		_zoomedRect.set(rect)
	}

	fun addListener(listener: Listener) {
		listeners.add(listener)
	}

	fun removeListener(listener: Listener) {
		listeners.remove(listener)
	}

	var currentCharacter: ZCharacter? = null
		set(value) {
			log.debug("Set current character $value")
			value?.let {
				setHighlightActor(it)
			}
			field = value
			redraw()
		}


	val numOverlayTextAnimations: Int
		get() = Utils.count(overlayAnimations) { a: ZAnimation? -> a is OverlayTextAnimation }

	interface Listener {
		fun onClick(obj: Any?) {}

		fun onActorHighlighted(actor: ZActor?) {}

		fun onAnimateZoomBegin() {}

		fun onAnimateZoomEnd(rect: IRectangle) {}
	}

	val isAnimating: Boolean
		get() = actorsAnimating
			|| postActor.size > 0
			|| preActor.size > 0
			|| overlayAnimations.size > 0
			|| zoomAnimation != null

	fun stopAnimations() {
		preActor.clear()
		postActor.clear()
		overlayAnimations.clear()
		board.getAllActors().forEach {
			it.stopAnimating()
		}
		zoomAnimation = null
	}

	fun setHighlightActor(actor: ZActor?) {
		if (actor != highlightedActor) {
			log.debug("highlighted actor: $actor")
			highlightedActor = actor
			if (actor != null) {
				val rect = board.getZone(actor.occupiedZone)
				if (!_zoomedRect.contains(rect)) {
					animateZoomDelta(_zoomedRect.getDeltaToContain(rect), 200)
				}
			}
			redraw()
		}
	}

	fun addPreActor(a: ZAnimation) {
		preActor.add(a.start())
		redraw()
	}

	fun addPostActor(a: ZAnimation) {
		postActor.add(a.start())
		redraw()
	}

	fun addHoverMessage(txt: String, actor: ZActor) {
		val list = actorMap.getOrPut(actor) { Collections.synchronizedList(mutableListOf()) }
		list.add(HoverMessage(this, txt, actor).also {
			if (list.size == 0)
				it.start<HoverMessage>()
			postActor.add(it)
			redraw()
		})
	}

	fun fireNextHoverMessage(actor: ZActor) {
		actorMap.get(actor)?.let { list ->
			list.removeFirstOrNull()
			list.firstOrNull()?.start<HoverMessage>()
		}
	}

	fun addOverlay(a: ZAnimation) {
		overlayAnimations.add(a)
		redraw()
	}

	private fun addClickable(rect: IRectangle, move: IButton) {
		with(clickables.getOrSet(rect) { ArrayList() }) {
			add(move)
		}
	}

	fun zoomAmt(percent: Float) {
		val targetRect = _zoomedRect.scaledBy(percent)
		currentZoomType = ZoomType.UNDEFINED
		animateZoomTo(targetRect)
	}

	val zoomPercent: Float
		get() {
			if (viewport.area > 0) {
				return _zoomedRect.area / viewport.area
			}
			return 0f
		}

	private fun getZoomRectForType(type: ZoomType): GRectangle = when (type) {
		ZoomType.UNDEFINED -> TODO("This shouldn't happen")
		ZoomType.CROP_FIT -> {
			val d = max(board.width, board.height)
			viewport.cropFit(GRectangle(0f, 0f, d, d).withCenter(boardCenter))
		}
		ZoomType.X2 -> {
			val d = .5f * (min(board.width, board.height) + 3f)
			GRectangle(0f, 0f, d, d).setAspectReduce(viewportAspect).withCenter(boardCenter)
		}
		ZoomType.MAX -> viewport.fitInner(GRectangle(0f, 0f, 3f, 3f).withCenter(boardCenter))
		ZoomType.FILL_FIT -> {
			val d = max(board.width, board.height)
			viewport.fillFit(GRectangle(0f, 0f, d, d).withCenter(boardCenter))
		}
	}

	fun toggleZoomType(noAnimate: Boolean = false) {
		if (desiredZoomType == currentZoomType) {
			currentZoomType.increment(1, ZoomType.values().filter {
				it != ZoomType.UNDEFINED
			}.toTypedArray()).also {
				desiredZoomType = it
			}
		}
		getZoomRectForType(desiredZoomType).also {
			if (noAnimate) {
				_zoomedRect.set(it)
				clampZoomRect()
				redraw()
			} else {
				animateZoomTo(it)
			}
		}
		currentZoomType = desiredZoomType
	}

	fun processSubMenu(cur: ZCharacter, options: List<IButton>) {
		/*
		if (ENABLE_ONBOARD_SUBMENU && highlightedShape != null) {
			val moves = options.toMutableList()
			moves.add(object : IButton {
				override fun getTooltipText(): String = ""
				override fun getLabel(): String = "Cancel"
			})
			highlightedMoves = moves
			clickables.clear()
			redraw()
		}*/
	}

	fun processMoveOptions(cur: ZCharacter, options: List<ZMove>) {
		clickables.clear()
		if (!ENABLE_ONBOARD_MENU) return
		for (move in options) {
			when (move.type) {
				ZMoveType.TRADE -> move.list?.map { board.getCharacter(it as ZPlayerName) }?.forEach { c: ZCharacter ->
					addClickable(c.getRect(), ZMove(move, c, "Trade ${c.label}"))
				}
				ZMoveType.WALK -> move.list?.forEachAs { zoneIdx: Int ->
					addClickable(board.getZone(zoneIdx), ZMove(move, zoneIdx, zoneIdx))
				}
				ZMoveType.MELEE_ATTACK, ZMoveType.RANGED_ATTACK, ZMoveType.MAGIC_ATTACK -> move.list?.forEachAs { w: ZWeapon ->
					for (stat in w.type.stats.filter { it.actionType == move.action }) {
						for (zoneIdx in board.getAccessibleZones(cur.occupiedZone, stat.minRange, stat.maxRange, stat.actionType)) {
							addClickable(board.getZone(zoneIdx), ZMove(move, w, zoneIdx, "${move.type.shortName} ${w.label}"))
						}
					}
				}
				ZMoveType.THROW_ITEM -> {
					val zones = board.getAccessibleZones(cur.occupiedZone, 0, 1, ZActionType.THROW_ITEM)
					move.list?.forEachAs { item: ZEquipment<*> ->
						for (zoneIdx in zones) {
							addClickable(cur.getRect(), ZMove(move, item, zoneIdx, "Throw ${item.label}"))
						}
					}
				}
				ZMoveType.OPERATE_DOOR -> move.list?.forEachAs { door: ZDoor ->
					val rect = door.getRect(board).grow(if (door.isJammed) .1f else 0f)
					addClickable(
						rect,
						ZMove(move, door,
							if (door.isClosed(board))
								"Open"
							else
								"Close"
						)
					)
				}

				ZMoveType.BARRICADE -> move.list?.forEachAs { door: ZDoor ->
					addClickable(door.getRect(board).grow(.1f), ZMove(move, door, "Barricade"))
				}
				//ZMoveType.SEARCH, ZMoveType.CONSUME, ZMoveType.EQUIP, ZMoveType.UNEQUIP, ZMoveType.GIVE, ZMoveType.TAKE, ZMoveType.DISPOSE -> addClickable(cur.getRect(), move)
				ZMoveType.TAKE_OBJECTIVE -> addClickable(board.getZone(cur.occupiedZone), move)
				//ZMoveType.DROP_ITEM -> move.list?.forEachAs { e :ZEquipment<*> ->
				//	addClickable(cur.getRect(), ZMove(move, e, "Drop ${e.label}"))
				//}
				//ZMoveType.PICKUP_ITEM -> move.list?.forEachAs { e :ZEquipment<*> ->
				//	addClickable(cur.getRect(), ZMove(move, e, "Pickup ${e.label}"))
				//}
				//ZMoveType.MAKE_NOISE -> addClickable(cur.getRect(), move)
				ZMoveType.SHOVE -> move.list?.forEachAs { zoneIdx: Int ->
					addClickable(board.getZone(zoneIdx), ZMove(move, zoneIdx, "Shove"))
				}
				ZMoveType.BORN_LEADER, // Born leader can be a spell?
				ZMoveType.WALK_DIR,
					//ZMoveType.REROLL,
					//ZMoveType.KEEP_ROLL,
				ZMoveType.USE_SLOT -> Unit
				ZMoveType.ENCHANT -> {
					board.getAllCharacters().filter {
						it.isAlive && board.canSee(cur.occupiedZone, it.occupiedZone)
					}.forEach { c ->
						move.list?.forEachAs { spell: ZSpell ->
							addClickable(c.getRect(), ZMove(move, spell, c.type, spell.label))
						}
					}
				}
//				ZMoveType.BORN_LEADER -> for (c in (move.list as List<ZCharacter>)) {
//					addClickable(c.getRect(), ZMove(move, c.type, c.type, "))
//				}
				ZMoveType.BLOODLUST_MELEE -> for (w in cur.meleeWeapons) {
					move.list?.forEachAs { zoneIdx: Int ->
						addClickable(board.getZone(zoneIdx), ZMove(move, zoneIdx, w, "Bloodlust ${w.label}"))
					}
				}
				ZMoveType.BLOODLUST_RANGED -> for (w in cur.rangedWeapons) {
					move.list?.forEachAs { zoneIdx: Int ->
						addClickable(board.getZone(zoneIdx), ZMove(move, zoneIdx, w, "Bloodlust ${w.label}"))
					}
				}
				ZMoveType.BLOODLUST_MAGIC -> for (w in cur.magicWeapons) {
					move.list?.forEachAs { zoneIdx: Int ->
						addClickable(board.getZone(zoneIdx), ZMove(move, zoneIdx, w, "Bloodlust ${w.label}"))
					}
				}
				else -> addClickable(cur.getRect(), move)
			}
		}
		/*
		if (clickables.size == 1) {
			with(clickables.entries.first()) {
				if (highlightedShape == null)
					highlightedShape = key
				highlightedMoves = value
			}
		} else {
			highlightedShape = null
			highlightedMoves = null
			highlightedMovesPos = null
		}*/
	}

	fun drawAnimations(anims: MutableList<ZAnimation>, g: AGraphics) {
		anims.toList().forEach { it ->
			if (it.isDone) {
				anims.remove(it)
			} else {
				if (!it.isStarted) it.start<AAnimation<AGraphics>>()
				it.update(g)
			}
		}
	}

	fun drawActors(g: AGraphics): ZActor? {
		var picked: ZActor? = null
		var distFromCenter = 0f
		var numActorsAnimating = 0

		fun drawActor(a: ZActor) {
			val img = g.getImage(a.imageId)
			if (img != null) {
				val rect = a.getRect()
				if (rect.contains(mouseV)) {
					val dist = rect.center.sub(mouseV).magSquared()
					if (picked == null || picked !is ZCharacter || dist < distFromCenter) {
						picked = a
						distFromCenter = dist
					}
				}
				if (a.isInvisible) {
					g.setTransparencyFilter(.5f)
				}
				if (a.isAnimating) {
					a.drawOrAnimate(g)
					if (drawScreenCenter) {
						g.color = GColor.MAGENTA
						g.drawRect(a.getRect())
					}
					numActorsAnimating++
				} else {
					var outline: GColor = GColor.WHITE
					highlightAnimationScale = 1f
					currentCharacter?.let {
						if (a == it) {
							outline = GColor.GREEN
						} else if (a === picked)
							outline = GColor.CYAN
						else if (a.pickable)
							outline = GColor.YELLOW
					}

					if (ANIMATE_HIGHLIGHTED_ACTOR) {
						highlightedActor?.let {
							if (it == a) {
								highlightAnimation.update(g)
								outline = g.color
								redraw()
							}
						}
					}
					drawActor(g, a, outline)
				}
				g.removeFilter()
				if (false) {
					g.color = GColor.YELLOW.withAlpha(.5f)
					g.drawRect(a.getRect(), 1f)
					g.color = GColor.YELLOW
					g.drawRect(a.position.toRect(board))
				}
			}
		}

		with (board.getAllActors().filter { it.isVisible }) {
			filter { !it.isAnimating }.forEach {
				drawActor(it)
			}

			filter { it.isAnimating }.forEach {
				drawActor(it)
			}
		}

		picked?.takeIf { !it.isAnimating }?.let {
			drawActor(it) // draw the highlighted actor over the top to see its stats
		}

		listeners.toList().forEach {
			it.onActorHighlighted(picked ?: highlightedActor)
		}

		actorsAnimating = numActorsAnimating > 0

		return picked
	}

	var highlightAnimationScale = 1f
	val highlightAnimation = object : AAnimation<AGraphics>(1000, -1, true) {
		override fun draw(g: AGraphics, position: Float, dt: Float) {
			g.color = GColor.TRANSPARENT.interpolateTo(GColor.CYAN, position)
			//g.scale(1f + .2f*position)
			//highlightAnimationScale = 1f + .2f*position
		}
	}.start<AAnimation<AGraphics>>()

	protected open fun drawActor(g: AGraphics, actor: ZActor, outline: GColor?) {
		if (outline != null) {
			if (actor.outlineImageId > 0) {
				g.pushMatrix()
				g.setTintFilter(GColor.WHITE, outline)
				if (ANIMATE_HIGHLIGHTED_ACTOR) {
					g.drawImage(actor.outlineImageId, actor.getRect().scaledBy(highlightAnimationScale))
				} else {
					g.drawImage(actor.outlineImageId, actor.getRect())
				}
				g.removeFilter()
				g.popMatrix()
			} else {
				g.color = outline
				g.drawRect(actor.getRect(), 1f)
			}
		}
		actor.draw(g)
	}

	fun drawZoneOutline(g: AGraphics, zoneIndex: Int) {
		val zone = board.getZone(zoneIndex)
		g.pushMatrix()
		g.translate(zone.center)
		g.scale(.95f)
		g.translate(zone.center.scaledBy(-1f))
		g.drawRect(zone, 2f)
		g.popMatrix()
	}

	fun drawZoneFilled(g: AGraphics, zoneIndex: Int) {
		val zone = board.getZone(zoneIndex)
		for (cellPos in zone.getCells()) {
			g.drawFilledRect(board.getCell(cellPos))
		}
	}

	fun drawNoise(g: AGraphics) {
		g.color = GColor.BLACK
		for (zone in board.zones) {
			if (zone.noiseLevel > 0) {
				g.color = GColor.BLACK
				g.drawJustifiedString(zone.center, Justify.CENTER, Justify.CENTER, java.lang.String.valueOf(zone.noiseLevel))
			}
		}
		board.getMaxNoiseLevelZones().forEach { maxNoise ->
			var color = GColor(GColor.BLACK)
			var radius = 0.5f
			val dr = radius / 5
			radius = dr
			for (i in 0..4) {
				g.color = color
				g.drawCircle(maxNoise.center, radius, 0f)
				color = color.lightened(.1f)
				radius += dr
			}
		}
	}

	private fun drawObjectiveX(g: AGraphics, color: GColor, lineThickness: Float, cell: ZCell) {
		// draw a big red X om the center of the cell
		val redX = GRectangle(cell).scaledBy(.25f, .25f)
		g.color = color
		g.drawLine(redX.topLeft, redX.bottomRight, lineThickness)
		g.drawLine(redX.topRight, redX.bottomLeft, lineThickness)
	}

	/**
	 * return zone highlighted by mouseX, mouseY
	 *
	 * @param g
	 * @param mouseX
	 * @param mouseY
	 * @return
	 */
	fun drawZones(g: AGraphics, miniMap: Boolean): Pos? {
		var result: Pos? = null
		board.zones.forEach { zone ->
			for (pos in zone.getCells()) {
				val cell = board.getCell(pos)
				if (cell.isCellTypeEmpty) continue
				when (zone.type) {
					ZZoneType.TOWER -> if (miniMap || drawTowersHighlighted) {
						g.color = GColor.SKY_BLUE.withAlpha((cell.scale - 1) * 3)
						g.drawFilledRect(cell)
					}
					else -> Unit
				}

				val d = g.pushDepth
				try {
					drawCellWalls(g, pos, compassValues, 1f, miniMap)
				} catch (e: Exception) {
					log.error("Problem draw cell walls pos: $pos")
					throw e
				}
				if (d != g.pushDepth) {
					throw GException("push depth of of sync in drawCellWalls")
				}
				if (cell.contains(mouseV)) {
					result = pos
					g.color = GColor.RED.withAlpha(32)
					g.drawFilledRect(cell)
				}
				var text = ""
				val lineThickness = if (miniMap) 2f else 10f
				for (type in ZCellType.values()) {
					if (cell.isCellType(type)) {
						when (type) {
							ZCellType.OBJECTIVE_BLACK -> {
								if (zone.isObjective) {
									if (miniMap) {
										drawObjectiveX(g, type.color, lineThickness, cell)
									} else {
										quest?.drawBlackObjective(board, g, cell, zone)
									}
								}
							}
							ZCellType.OBJECTIVE_BLUE, ZCellType.OBJECTIVE_GREEN, ZCellType.OBJECTIVE_RED -> if (zone.isObjective) {
								drawObjectiveX(g, type.color, lineThickness, cell)
							}
							ZCellType.EXIT -> text += "EXIT"
							else -> Unit
						}
					}
				}
				for (area in cell.spawnAreas) {
					with (drawSpawn(g, cell, area)) {
						if (zone.isTargetForEscape && area.isEscapableForNecromancers) {
							g.color = GColor.RED
							g.drawRect(this, 2f)
						}
					}
				}
				if (zone.isDragonBile) {
					if (miniMap) {
						g.color = GColor.SLIME_GREEN
						g.drawFilledRect(cell)
					} else g.drawImage(ZIcon.SLIME.imageIds[0], cell)
				}
				if (!miniMap && text.isNotEmpty()) {
					g.color = GColor.YELLOW
					g.drawJustifiedStringOnBackground(cell.center, Justify.CENTER, Justify.CENTER, text, GColor.TRANSLUSCENT_BLACK, 10f, 2f)
				}
				if (!miniMap && zone.pickable) {
					g.color = GColor.TRANSLUSCENT_BLACK
					g.drawFilledRect(cell)
				}
			}
			zone.doors.filter { it.pickable }.forEach { door ->
				val doorRect = door.getRect(board).grow(.1f)
				if (doorRect.contains(mouseV)) {
					g.color = GColor.RED
					highlightedResult = door
					// highlight the other side if this is a vault
					g.drawRect(door.otherSide.getRect(board).grow(.1f), 2f)
				} else {
					g.color = GColor.YELLOW
				}
				g.drawRect(doorRect.grow(.1f), 2f)
			}
			if (zone.pickable) {
				g.color = GColor.YELLOW
				drawZoneOutline(g, zone.zoneIndex)
				if (zone.contains(mouseV))
					highlightedResult = zone.zoneIndex
			}
			if (zone.type == ZZoneType.VAULT) {
				quest?.getVaultItems(zone.zoneIndex)?.takeIf { it.size > 0 }?.let { items ->
					"?".repeat(items.size).apply {
						g.color = GColor.WHITE
						g.drawJustifiedString(zone.center, Justify.CENTER, Justify.CENTER, this)
					}
					g.drawCircle(zone.center, .25f)
				}
			}
		}
		return result
	}

	fun drawSpawn(g: AGraphics, rect: IRectangle, area: ZSpawnArea) : IRectangle {
		val dir = area.dir
		val id = area.icon.imageIds[dir.ordinal]
		val img = g.getImage(id)
		area.rect = GRectangle(rect).scale(.8f).fit(img, dir.horz, dir.vert)
		g.drawImage(id, area.rect)
		if (area.pickable) {
			if (area.rect.contains(mouseV)) {
				g.color = GColor.RED
				highlightedResult = area
			} else {
				g.color = GColor.YELLOW
			}
			g.drawRect(area.rect, 2f)
		}
		if (drawDebugText) {
			var txt = ""
			if (area.isCanSpawnNecromancers) txt += "\nNecros"
			if (area.isEscapableForNecromancers) txt += "\nEscapable"
			if (area.isCanBeRemovedFromBoard) txt += "\nDestroyable"
			//String txt = String.format("spawnsNecros:%s\nEscapable:%s\nRemovable:%s\n", area.isCanSpawnNecromancers(), area.isEscapableForNecromancers(), area.isCanBeRemovedFromBoard());
			g.color = GColor.YELLOW
			val oldHeight = g.setTextHeight(10f)
			g.drawString(txt.trim { it <= ' ' }, area.rect.topLeft)
			g.textHeight = oldHeight
		}

		return area.rect
	}

	fun drawCellWalls(g: AGraphics, cellPos: Pos, dirs: Array<ZDir>, scale: Float, miniMap: Boolean) {
		val cell = board.getCell(cellPos)
		g.pushMatrix()
		val center: Vector2D = cell.center
		g.translate(center)
		val v0: Vector2D = cell.topLeft.subEq(center)
		val v1: Vector2D = cell.topRight.subEq(center)
		g.scale(scale)
		val doorColor = GColor.ORANGE
		for (dir in dirs) {
			val dv: Vector2D = v1.sub(v0).scaleEq(.33f)
			val dv0: Vector2D = v0.add(dv)
			val dv1: Vector2D = dv0.add(dv)
			g.pushMatrix()
			g.rotate(dir.rotation.toFloat())
			g.color = GColor.BLACK
			val wallThickness = if (miniMap) 1 else 3
			when (cell.getWallFlag(dir)) {
				ZWallFlag.WALL -> g.drawLine(v0, v1, wallThickness.toFloat())
				ZWallFlag.RAMPART -> g.drawDashedLine(v0, v1, wallThickness.toFloat(), if (miniMap) 2f else 20f)
				ZWallFlag.OPEN -> {
					g.drawLine(v0, dv0, wallThickness.toFloat())
					g.drawLine(dv1, v1, wallThickness.toFloat())
					if (dir === ZDir.SOUTH || dir === ZDir.WEST) {
						g.color = doorColor
						g.drawLine(dv0, dv0.add(dv.scaledBy(.5f).rotate(145f)), (wallThickness + 1).toFloat())
						g.drawLine(dv1, dv1.sub(dv.scaledBy(.5f).rotate(-145f)), (wallThickness + 1).toFloat())
					}
				}
				ZWallFlag.LOCKED -> {
					if (dir === ZDir.SOUTH || dir === ZDir.WEST) {
						g.drawLine(v0, v1, wallThickness.toFloat())
						val door = board.findDoor(cellPos, dir)
						g.color = door.lockedColor
						g.drawLine(dv0, dv1, (wallThickness + 1).toFloat())
						if (!miniMap) {
							val padlock = GRectangle(0f, 0f, .2f, .2f).withCenter(dv1.add(dv0).scaleEq(.5f))
							val img = g.getImage(ZIcon.PADLOCK.imageIds[0])
							g.drawImage(ZIcon.PADLOCK.imageIds[0], padlock.fit(img))
						}
					}
				}
				ZWallFlag.CLOSED -> if (dir === ZDir.SOUTH || dir === ZDir.WEST) {
					g.drawLine(v0, v1, wallThickness.toFloat())
					g.color = doorColor
					g.drawLine(dv0, dv1, (wallThickness + 1).toFloat())
				}
				else -> Unit
			}
			g.popMatrix()
		}
		g.popMatrix()
		val vaultLineThickness = if (miniMap) 1 else 2
		for (dir in elevationValues) {
			when (cell.getWallFlag(dir)) {
				ZWallFlag.LOCKED -> {
					val door = board.findDoor(cellPos, dir)
					g.color = GColor.BLACK
					val vaultRect = door.getRect(board)
					g.color = door.lockedColor
					vaultRect.drawFilled(g)
					g.color = GColor.RED
					vaultRect.drawOutlined(g, vaultLineThickness)
					if (!miniMap) {
						val rect = door.getRect(board).scale(.7f)
						val img = g.getImage(ZIcon.PADLOCK.imageIds[0])
						g.drawImage(ZIcon.PADLOCK.imageIds[0], rect.fit(img))
					}
				}
				ZWallFlag.CLOSED -> {
					board.findDoorOrNull(cellPos, dir)?.let { door ->
						g.color = GColor.BLACK
						val vaultRect = door.getRect(board)
						g.color = door.lockedColor
						vaultRect.drawFilled(g)
						g.color = GColor.YELLOW
						vaultRect.drawOutlined(g, vaultLineThickness)
					}
				}
				ZWallFlag.OPEN -> {
					board.findDoorOrNull(cellPos, dir)?.let { door ->
						g.color = GColor.BLACK
						val vaultRect = door.getRect(board)
						vaultRect.drawFilled(g)
						// draw the 'lid' opened
						g.begin()
						g.vertex(vaultRect.topRight)
						g.vertex(vaultRect.topLeft)
						val dh = vaultRect.h / 3
						val dw = vaultRect.w / 5
						if (dir === ZDir.ASCEND) {
							// open 'up'
							g.moveTo(-dw, -dh)
							g.moveTo(vaultRect.w + dw * 2, 0f)
						} else {
							// open 'down
							g.moveTo(dw, dh)
							g.moveTo(vaultRect.w - dw * 2, 0f)
						}
						g.color = door.lockedColor
						g.drawTriangleFan()
						g.color = GColor.YELLOW
						g.end()
						vaultRect.drawOutlined(g, vaultLineThickness)
						g.drawLineLoop(vaultLineThickness.toFloat())
					}
				}
				else -> Unit
			}
		}
	}

	fun drawMiniMap(g: AGraphics) {
		g.pushMatrix()
		g.ortho()
		try {
			val xscale = 0.25f * (width / board.columns)
			val yscale = 0.5f * (height / board.rows)
			val padding = 10f
			when (miniMapMode) {
				MiniMapMode.OFF -> return
				MiniMapMode.UL -> {
					g.translate(padding, padding)
					g.scale(Math.min(xscale, yscale))
				}
				MiniMapMode.UR -> {
					g.translate(width - padding, padding)
					g.scale(Math.min(xscale, yscale))
					g.translate(-board.columns.toFloat(), 0f)
				}
				MiniMapMode.BL -> {
					g.translate(padding, height - padding)
					g.scale(Math.min(xscale, yscale))
					g.translate(0f, -board.rows.toFloat())
				}
				MiniMapMode.BR -> {
					g.translate(width - padding, height - padding)
					g.scale(Math.min(xscale, yscale))
					g.translate(0f, -board.rows.toFloat())
					g.translate(-board.columns.toFloat(), 0f)
				}
			}
			g.setTransparencyFilter(.6f)
			g.color = GColor.TRANSLUSCENT_BLACK
			g.drawFilledRect(0f, 0f, board.columns.toFloat(), board.rows.toFloat())
			val d = g.pushDepth
			drawZones(g, true)
			if (d != g.pushDepth) {
				throw GException("push depth out of sync in drawZones")
			}
			// draw the actors
			for (a in board.getAllActors()) {
				if (a is ZCharacter) {
					g.color = a.color ?: GColor.BLACK
				} else if (a is ZZombie) {
					g.color = GColor.LIGHT_GRAY
				}
				val rect = a.getRect(board)
				g.drawFilledOval(rect.scale(.8f))
			}
		} finally {
			g.removeFilter()
			g.popMatrix()
		}
	}

	fun drawDebugText(g: AGraphics) {
		val it = board.getCellsIterator()
		while (it.hasNext()) {
			val cell = it.next()
			if (cell.isCellTypeEmpty) continue
			var text: String = when (cell.environment) {
				ZCell.ENV_BUILDING -> "Building "
				ZCell.ENV_VAULT -> "Vault "
				ZCell.ENV_TOWER -> "Tower "
				ZCell.ENV_OUTDOORS -> "Outside "
				else -> "??? "
			} + "Z${cell.zoneIndex} [${cell.X()},${cell.Y()}]"
			for (type in ZCellType.values()) {
				if (cell.isCellType(type)) {
					when (type) {
						ZCellType.NONE, ZCellType.VAULT_DOOR_VIOLET, ZCellType.VAULT_DOOR_GOLD -> {
						}
						ZCellType.OBJECTIVE_RED, ZCellType.OBJECTIVE_BLUE, ZCellType.OBJECTIVE_GREEN, ZCellType.OBJECTIVE_BLACK ->
							text += "\n\n${type.name.substring(10)}\n".trimIndent()
						else -> text += "\n\n$type\n".trimIndent()
					}
				}
			}
			/*
            if (cell.contains(mouseX, mouseY)) {
                List<Integer> accessible = getBoard().getAccessableZones(cell.zoneIndex, 1, 5, ZActionType.MOVE);
                text = "1 Unit away:\n" + accessible;
                //returnCell = it.getPos();//new int[] { col, row };
                List<Integer> accessible2 = getBoard().getAccessableZones(cell.zoneIndex, 2, 5, ZActionType.MAGIC);
                text += "\n2 Units away:\n" + accessible2;
            }*/g.color = GColor.CYAN
			for (a in cell.getOccupants(board)) {
				text += "\n\n${a.name()}".trimIndent()
			}
			if (cell.vaultId > 0) {
				text += "\n\nvaultFlag ${cell.vaultId}".trimIndent()
			}
			g.drawJustifiedStringOnBackground(cell.center, Justify.CENTER, Justify.CENTER, text, GColor.TRANSLUSCENT_BLACK, 10f, 3f)
		}
	}

	fun drawNoTiles(g: AGraphics): Grid.Pos? {
		g.clearScreen()
		val returnCell: Grid.Pos? = null
		val it = board.getCellsIterator()
		while (it.hasNext()) {
			val cell = it.next()
			if (cell.isCellTypeEmpty) continue
			when (cell.environment) {
				ZCell.ENV_BUILDING -> g.color = GColor.DARK_GRAY
				ZCell.ENV_OUTDOORS, ZCell.ENV_TOWER -> g.color = GColor.LIGHT_GRAY
				ZCell.ENV_VAULT -> g.color = GColor.BROWN
			}
			g.drawFilledRect(cell)
		}
		return returnCell
	}

	fun pickButtons(g: AGraphics, pos: Vector2D, moves: List<IButton>?): IButton? {
		val height = moves!!.size * g.textHeight * 2
		val top = pos.sub(0f, height / 2)
		if (top.Y() < 0) {
			top.y = 0f
		} else if (top.Y() + height > g.viewportHeight) {
			top.y = g.viewportHeight - height
		}
		// draw buttons to the right is pos id on left side and to the left if user on the right side
		var move: IButton? = null
		val buttonHeight = g.textHeight * 2
		val padding = g.textHeight / 2
		val buttonWidth = moves.maxOf { g.getTextWidth(it.label) } + (padding * 2)
		if (top.x + buttonWidth > g.viewportWidth) {
			top.x = g.viewportWidth - buttonWidth
		}
		val button = GRectangle(top, buttonWidth, buttonHeight)
		for (m in moves) {
			with(buttonHeight / 4) {
				g.color = GColor.TRANSLUSCENT_BLACK
				g.drawFilledRoundedRect(button, this)
				if (button.contains(mouseX.toFloat(), mouseY.toFloat())) {
					g.color = GColor.RED
					move = m
				} else {
					g.color = GColor.YELLOW
				}
				g.drawRoundedRect(button, 1f, this)
			}
			button.drawRounded(g, buttonHeight / 4)
			g.drawJustifiedString(button.x + padding, button.y + buttonHeight / 2, Justify.LEFT, Justify.CENTER, m.label)
			button.y += buttonHeight
		}
		return move
	}
/*
	fun pickMove(g: APGraphics): IButton? {

		highlightedShape?.let {
			g.color = GColor.RED
			g.setLineWidth(2f)
			it.drawOutlined(g)
			try {
				with(highlightedMovesPos ?: it.center) {
					g.transform(this)
					g.pushMatrix()
					g.setIdentity()
					g.ortho()
					pickButtons(g, this, highlightedMoves, screenMouseX, screenMouseY)?.let {
						return pickMove@ it
					}
				}
			} finally {
				g.popMatrix()
			}
		}

		for ((shape, value) in clickables) {
			if (shape.contains(mouse.x, mouse.y)) {
				if (shape != highlightedShape) {
					highlightedShape = shape
					highlightedMoves = value
					highlightedMovesPos = Vector2D(screenMouseX.toFloat(), screenMouseY.toFloat())
				}
				break
			} else {
				g.color = GColor.YELLOW.withAlpha(32)
				shape.drawFilled(g)
			}
		}
		return null
	}*/

	//game.getBoard().getZone(game.getCurrentCharacter().getOccupiedZone()).getCenter();
	val boardCenter: IVector2D
		get() = computeBoardCenter().also {
			//log.debug("board center: $it")
		}

	fun logIf(usage : Int, msg : String) {
		if (lastUsage == usage)
			return

		lastUsage=usage
		log.debug(msg)
	}

	private fun computeBoardCenter() : IVector2D {
		currentCharacter?.takeIf { it.isAnimating }?.let {
			logIf(0, "0:Using animating current character")
			return it
		}
		savedCenter?.takeIf { it is ZCharacter && it.isAnimating || isFocussed }?.let {
			logIf(1, "1:Using animating savedCenter")
			return it
		}
		highlightedActor?.takeIf { it.isAlive }?.let {
			logIf(2, "2:Using highlightedActor")
			return it
		}

		currentCharacter?.let {
			return it.also {
				logIf(3, "3:Using current character")
				savedCenter = it
			}
		}

		savedCenter?.let {
			logIf(4, "4:Using saved center")
			return it
		}
		board.getAllCharacters().takeIf { it.isNotEmpty() }?.let {
			val rect = GRectangle()
			it.forEach { c ->
				rect.addEq(c.getRect())
			}
			logIf(5, "5:Using center of all characters")
			return rect.center
		}

		logIf(6, "5:Using board center")
		return board.getLogicalCenter()
	}

	fun clearTiles() {
		tiles = arrayOf()
		tileIds = IntArray(0)
	}

	fun onTilesLoaded(ids: IntArray) {
		tileIds = ids
		savedCenter = null
		if (_zoomedRect.isEmpty)
			_zoomedRect.set(0f, 0f, board.width, board.height)
		onSizeChanged(viewportWidth, viewportHeight)
	}

	open fun onLoading() {}
	open fun onLoaded() {}

	/**
	 *
	 * @param targetZoomPercent 0 is fully zoomed out and 1 is fully zoomed in
	 */
	fun animateZoomTo(newRect: IRectangle) {
		zoomAnimation = ZoomAnimation(clampRect(GRectangle(newRect)), this).start()
		redraw()
	}

	fun animateZoomToIfNotContained(newRect: IRectangle) {
		clampRect(GRectangle(newRect)).also { rect ->
			if (!getZoomedRect().contains(rect)) {
				zoomAnimation = ZoomAnimation(rect, this).start()
				waitForAnimations()
			}
		}
	}

	fun animateZoomTo(type: ZoomType) {
		zoomAnimation = ZoomAnimation(clampRect(getZoomRectForType(type)), this).start()
		redraw()
	}

	fun animateZoomDelta(delta: IVector2D, speed: Long) {
		zoomAnimation = ZoomAnimation(
			clampRect(getZoomedRect().moveBy(delta)),
			this, speed).start()
		redraw()
	}

	fun drawQuestLabel(g: AGraphics) {
		g.color = GColor.BLACK
		if (quest != null) {
			val height = g.textHeight
			g.textHeight = 24f //setFont(bigFont);
			g.setTextStyles(AGraphics.TextStyle.BOLD, AGraphics.TextStyle.ITALIC)
			g.drawJustifiedString(10f, getHeight() - 10 - g.textHeight, Justify.LEFT, Justify.BOTTOM, quest!!.name)
			g.textHeight = height
			g.setTextStyles(AGraphics.TextStyle.NORMAL)
		}
	}

	fun drawPlayerName(name: String?) {}

	private fun drawNoBoard(g: AGraphics) {
		g.ortho()
		g.clearScreen(GColor.WHITE)
		val cntr = Vector2D((g.viewportWidth / 2).toFloat(), (g.viewportHeight / 2).toFloat())
		val minDim = GDimension((g.viewportWidth / 4).toFloat(), (g.viewportHeight / 4).toFloat())
		val maxDim = GDimension((g.viewportWidth / 2).toFloat(), (g.viewportHeight / 2).toFloat())
		val rect = GRectangle().withDimension(minDim.interpolateTo(maxDim, 1f)).withCenter(cntr)
		val img = g.getImage(ZIcon.GRAVESTONE.imageIds[0])
		g.drawImage(ZIcon.GRAVESTONE.imageIds[0], rect.fit(img))
	}

	var mouseX = -1
	var mouseY = -1
	val mouseV = MutableVector2D(-1f, -1f)

	override fun draw(g: APGraphics, mx: Int, my: Int) {
		mouseX = mx
		mouseY = my
		actorsAnimating = true

		if (!UIZombicide.initialized) {
			drawNoBoard(g)
			return
		}
		highlightedCell = null
		highlightedResult = null
		g.setIdentity()

		g.ortho(_zoomedRect)
		mouseV.set(g.screenToViewport(mx, my))
		quest?.let {
			if (drawTiles && tiles.isEmpty() && it.tiles.isNotEmpty()) {
				tiles = it.tiles
				onLoading()
				getComponent<UIZComponent<AGraphics>>().loadTiles(g, tiles, it)
				return
			}
		}
		val cellPos = drawNoTiles(g)
		if (drawTiles) {
			for (i in tileIds.indices) {
				g.drawImage(tileIds[i], tiles[i].quadrant)
			}
		}
		highlightedCell = drawZones(g, false)
		quest?.drawQuest(board, g)
		drawNoise(g)
		drawAnimations(preActor, g)
		drawActors(g)?.let { picked ->
			highlightedActor = picked
		}
		if (drawDebugText)
			drawDebugText(g)
		if (drawZombiePaths) {
			highlightedActor?.let {
				if (it is ZZombie) {
					val path = when (it.type) {
						ZZombieType.Necromancer -> board.getZombiePathTowardNearestSpawn(it)
						else -> board.getZombiePathTowardVisibleCharactersOrLoudestZone(it)
					}
					g.begin()
					g.color = GColor.YELLOW
					val start: Vector2D = it.getRect().center
					g.vertex(start)
					val next = MutableVector2D(start)
					for (dir in path) {
						next.addEq(dir.dx.toFloat(), dir.dy.toFloat())
						g.vertex(next)
					}
					g.drawLineStrip(3f)
				} else if (highlightedActor is ZCharacter) {
					val visibleZones = board.getAccessibleZones(it.occupiedZone, 0, 4, ZActionType.RANGED)
					for (zoneIdx in visibleZones) {
						val zone = board.getZone(zoneIdx)
						g.color = GColor.YELLOW
						zone.drawOutlined(g, 2)
					}
				}
			}
		}
		highlightedCell?.transform { board.getZone(it) }?.let { zone: ZZone ->
			if (drawRangedAccessibility) {
				g.color = GColor.BLUE.withAlpha(.5f)
				for (zIndex in board.getAccessibleZones(zone.zoneIndex, 1, 5, ZActionType.RANGED)) {
					drawZoneFilled(g, zIndex)
				}
			}
			if (zone.pickable) {
				highlightedResult = zone.zoneIndex
			}
		}
		drawAnimations(postActor, g)
		drawQuestLabel(g)
		if (highlightedActor?.pickable == true) {
			highlightedResult = highlightedActor
		}

		highlightedCell?.let {
			val cell = board.getCell(it)
			g.color = GColor.RED.withAlpha(32)
			drawZoneOutline(g, cell.zoneIndex)
		}
		drawMessage(g)
		drawOverlay(g)

		if (zoomAnimation?.isDone == true) {
			zoomAnimation = null
		} else {
			zoomAnimation?.update(g)
		}
		if (isAnimating) {
			redraw()
		} else {
			val d = g.pushDepth
			drawMiniMap(g)
			lock.withLock {
				condition.signal()
			}
			if (d != g.pushDepth) {
				throw GException("push depth out of sync in drawMiniMap")
			}
		}

		if (drawScreenCenter) {
			g.color = GColor.RED
			g.drawCircle(boardCenter, .25f)
		}

		if (drawClickable) {
			g.color = GColor.RED
			clickables.keys.forEach() { shape ->
				shape.drawOutlined(g)
			}
		}
	}

	val lock = ReentrantLock()
	val condition = lock.newCondition()

	fun waitForAnimations() {
		actorsAnimating = true
		redraw()
		do {
			lock.withLock {
				condition.await(1, TimeUnit.SECONDS)
			}
		} while (isAnimating)
	}

	protected fun drawOverlay(g: AGraphics) {
		drawOverlayObject(overlayToDraw, g)
	}

	protected fun drawMessage(g: AGraphics) {
		g.pushMatrix()
		g.setIdentity()
		g.ortho()
		g.color = GColor.WHITE
		boardMessage?.let { message ->
			g.drawJustifiedStringOnBackground(10f, height - 10, Justify.LEFT, Justify.BOTTOM, message, GColor.TRANSLUSCENT_BLACK, borderThickness)
			drawAnimations(overlayAnimations, g)
		}
		if (drawDebugText) {
			g.drawJustifiedStringOnBackground(width - 10, 10f, Justify.RIGHT, Justify.TOP,
				"""$desiredZoomType/$currentZoomType/${(100f * zoomPercent).roundToInt()}
				   ZoomRect: $_zoomedRect
				   Viewport: $viewport
				   Board: ${board.width} x ${board.height}
				""".trimMargin(), GColor.TRANSLUSCENT_BLACK, borderThickness)
		}
		g.popMatrix()
	}

	private fun drawOverlayObject(obj: Any?, g: AGraphics) {
		// overlay
		if (obj == null) return
		if (obj is Int) {
			val id = obj
			if (id >= 0) {
				val img = g.getImage(id)
				var rect = GRectangle(0f, 0f, width, height)
				rect.scale(.9f, .9f)
				rect = rect.fit(img, Justify.LEFT, Justify.CENTER)
				g.drawImage(id, rect)
			}
		} else if (obj is Table) {
			g.pushMatrix()
			g.setIdentity()
			g.ortho()
			g.color = GColor.YELLOW
			val cntr: IVector2D = Vector2D(width / 2, height / 2)
			obj.draw(g, cntr, Justify.CENTER, Justify.CENTER)
			g.popMatrix()
			/*
            g.setColor(GColor.RED);
            GRectangle r = new GRectangle(d).withCenter(cntr);
            g.drawRect(r);
            g.drawRect(0, 0, g.getViewportWidth(), g.getViewportHeight());
            */
		} else if (obj is String) {
			g.color = GColor.WHITE
			g.drawWrapStringOnBackground(width / 2, height / 2, width / 2, Justify.CENTER, Justify.CENTER, obj as String?, GColor.TRANSLUSCENT_BLACK, 10f)
		} else if (obj is ZAnimation) {
			val a = obj
			if (!a.isStarted) {
				a.start<AAnimation<AGraphics>>()
			}
			if (!a.isDone) {
				a.update(g)
				redraw()
			}
		} else if (obj is AImage) {
		} else if (obj is List<*>) {
			drawOverlayList(obj.filterIsInstance<IMeasurable>(), g)
		}
	}

	private fun drawOverlayList(list: List<IMeasurable>, g: AGraphics) {
		var width = 0f
		var height = 0f
		g.pushMatrix()
		g.setIdentity()
		g.ortho()
		for (m in list) {
			val dim = m.measure(g)
			width += dim.width
			height = Math.max(height, dim.height)
		}
		val padding = 20f
		width += padding * (list.size - 1)
		g.transform(g.viewportWidth / 2 - width / 2, (g.viewportHeight / 2).toFloat())
		for (m in list) {
			drawOverlayObject(m, g)
		}
		g.popMatrix()
	}

	val borderThickness: Float
		get() = 5f

	fun setOverlay(obj: Any?) {
		overlayToDraw = when (obj) {
			null,
			is AImage,
			is Table -> obj
			is ZPlayerName -> obj.cardImageId
			is ZAnimation -> {
				addOverlay(obj)
				null
			}
			else -> obj.toString()
		}
		redraw()
	}

	override fun onClick() {
		listeners.forEach {
			it.onClick(highlightedResult)
		}
	}

	fun toggleDrawTiles(): Boolean {
		return drawTiles.not().also { drawTiles = it }
	}

	fun toggleDrawDebugText(): Boolean {
		return drawDebugText.not().also { drawDebugText = it }
	}

	fun toggleDrawTowersHighlighted(): Boolean {
		return drawTowersHighlighted.not().also { drawTowersHighlighted = it }
	}

	fun toggleDrawScreenCenter(): Boolean {
		return drawScreenCenter.not().also { drawScreenCenter = it }
	}

	fun toggleDrawClickables(): Boolean {
		return drawClickable.not().also { drawClickable = it }
	}

	fun toggleDrawZoombiePaths(): Boolean {
		return drawZombiePaths.not().also { drawZombiePaths = it }
	}

	fun toggleDrawRangedAccessibility(): Boolean {
		return drawRangedAccessibility.not().also { drawRangedAccessibility = it }
	}

	fun toggleDrawMinimap(): MiniMapMode {
		miniMapMode = miniMapMode.increment(1)
		redraw()
		return miniMapMode
	}

	fun clampZoomRect() {
		clampRect(_zoomedRect)
	}

	fun clampRect(rect: GRectangle): GRectangle {
		if (rect.w > viewport.width || rect.h >= viewport.height) {
			rect.setDimension(viewport)
		} else if (rect.w < 3 && rect.h < 3) {
			rect.setDimension(GDimension(3f, 3f)).aspect = viewportAspect
		} else {
			// grow rect to match the aspect ratio of viewPort
			rect.aspect = viewportAspect
		}
		val minX = (board.width / 2 - rect.width / 2).coerceAtMost(0f)
		val minY = (board.height / 2 - rect.height / 2).coerceAtMost(0f)
		val maxX = (board.width / 2 + rect.width / 2).coerceAtLeast(board.width) - rect.w
		val maxY = (board.height / 2 + rect.height / 2).coerceAtLeast(board.height) - rect.h
		if (minX <= maxX)
			rect.x = rect.x.coerceIn(minX, maxX)
		if (minY <= maxY)
			rect.y = rect.y.coerceIn(minY, maxY)
		return rect
	}

	override fun onTouch(x: Int, y: Int) {
		overlayToDraw = null
	}

	override fun onTouchUp(x: Int, y: Int) {
		highlightedResult?.let { obj ->
			listeners.forEach {
				it.onClick(obj)
			}
		}
	}

	var dragStartV = Vector2D()

	override fun onDragStart(x: Int, y: Int) {
		if (highlightedShape == null) {
			R.setOrtho(_zoomedRect)
			dragStartV = R.untransform(x.toFloat(), y.toFloat())
		}
	}

	override fun onDragMove(x: Int, y: Int) {
		val v = R.untransform(x.toFloat(), y.toFloat())
		val dv = dragStartV.sub(v)
		_zoomedRect.moveBy(dv)
		dragStartV = v
		clampZoomRect()
		currentZoomType = ZoomType.UNDEFINED
		redraw()
	}

	override fun onZoom(scale: Float) {
		_zoomedRect.scale(scale)
		clampZoomRect()
		currentZoomType = ZoomType.UNDEFINED
		redraw()
	}

	override fun onSizeChanged(w: Int, h: Int) {
		if (board.isEmpty())
			return

		if (viewportAspect <= 0)
			return

		if (board.aspect < viewportAspect) {
			viewport = GDimension(viewportAspect * board.height, board.height)
		} else {
			viewport = GDimension(board.width, board.width / viewportAspect)
		}

		currentZoomType = ZoomType.UNDEFINED
		toggleZoomType(true)
	}

	override fun onFocusChanged(focussed: Boolean) {
		isFocussed = focussed
		if (!isFocussed) {
			setHighlightActor(null)
		}
	}

	companion object {
		const val ENABLE_ONBOARD_MENU = false
		const val ENABLE_ONBOARD_SUBMENU = false
		const val ANIMATE_HIGHLIGHTED_ACTOR = false
		val log = LoggerFactory.getLogger(UIZBoardRenderer::class.java)
	}
}