package cc.game.zombicide.ui

import cc.game.zombicide.ZActionType
import cc.game.zombicide.ZActor
import cc.game.zombicide.ZAnimation
import cc.game.zombicide.ZBoard
import cc.game.zombicide.ZCell
import cc.game.zombicide.ZCellType
import cc.game.zombicide.ZCharacter
import cc.game.zombicide.ZDir
import cc.game.zombicide.ZEnvironmentType
import cc.game.zombicide.ZIcon
import cc.game.zombicide.ZPlayerName
import cc.game.zombicide.ZQuest
import cc.game.zombicide.ZSpawnArea
import cc.game.zombicide.ZTile
import cc.game.zombicide.ZUser
import cc.game.zombicide.ZWallFlag
import cc.game.zombicide.ZZombie
import cc.game.zombicide.anims.OverlayTextAnimation
import cc.game.zombicide.anims.ZoomAnimation
import cc.lib.game.AGraphics
import cc.lib.game.AImage
import cc.lib.game.APGraphics
import cc.lib.game.GAnimation
import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.game.GRectangle
import cc.lib.game.IDimension
import cc.lib.game.IMeasurable
import cc.lib.game.IRectangle
import cc.lib.game.IShape
import cc.lib.game.IVector2D
import cc.lib.game.Justify
import cc.lib.game.Renderer
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.net.NetConnectQuality
import cc.lib.timer.GlobalTimer
import cc.lib.ui.UIKeyCode
import cc.lib.ui.UIRenderer
import cc.lib.utils.GException
import cc.lib.utils.Grid.Pos
import cc.lib.utils.KLock
import cc.lib.utils.Table
import cc.lib.utils.increment
import cc.lib.utils.launchIn
import cc.lib.utils.peekOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Stack
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

inline fun logCodeLocation() {
	Exception().stackTrace.filter { it.toString().contains("cc.game") }.take(5).forEach {
		UIZBoardRenderer.log.info(it.toString())
	}
}


enum class MiniMapMode {
	OFF,
	UL,
	UR,
	BL,
	BR
}

enum class ZoomType {
	/**
	 * the user has zoomed to an unknown position
	 */
	UNDEFINED,

	/**
	 * max amount of board is shown that fills screen
	 */
	CROP_FIT,

	/**
	 * zoom in half way between CROP_FIT and MAX
	 */
	X2,

	/**
	 * max zoom shows 3 cells square
	 */
	MAX,

	/**
	 * whole board is shown with blank areas 'filled'
	 */
	FILL_FIT
}

open class UIZBoardRenderer(component: UIZComponent<*>) : UIRenderer(component) {
	interface Listener {
		fun onActorHighlighted(actor: ZActor<*>?) {}

		fun onAnimateZoomBegin() {}

		fun onAnimateZoomEnd(rect: IRectangle) {}
	}

	inner class HoverMessage(private val msg: String, val rect: IShape) : ZAnimation(2500L) {

		private val start: Vector2D
		private val dv: Vector2D
		private var nextFired = false

		private lateinit var hJust: Justify

		override fun onStarted(g: AGraphics, reversed: Boolean) {
			g.pushTextHeight(HOVER_TEXT_HEIGHT, false)
			val tv: Vector2D = rect.center.toViewport(g)
			val width = g.getTextWidth(msg) / 2
			hJust = if (tv.x + width > g.viewportWidth) {
				Justify.RIGHT
			} else if (tv.x - width < 0) {
				Justify.LEFT
			} else {
				Justify.CENTER
			}
			g.popTextHeight()
		}

		override fun onDone() {
			if (!nextFired)
				fireNextHoverMessage(rect)
		}

		override fun draw(g: AGraphics, position: Float, dt: Float) {
			g.pushTextHeight(HOVER_TEXT_HEIGHT, false)
			val v: Vector2D = rect.center.add(start).add(dv.scaledBy(position))
			g.color = GColor.YELLOW.withAlpha(1f - position)
			g.drawJustifiedString(v, hJust, Justify.CENTER, msg)
			g.popTextHeight()
			if (!nextFired && position > .5f) {
				fireNextHoverMessage(rect)
				nextFired = true
			}
		}

		init {
			val offset = .3f
			val mag = .5f
			val angle = rect.center.sub(getZoomedRect().center).angleOf().roundToInt()
			when (angle) {
				in 0..90 -> {
					// UR quadrant
					start = Vector2D(-offset, 0f)
					dv = Vector2D(0f, -mag)
				}

				in 90..180 -> {
					// UL quadrant
					start = Vector2D(offset, 0f)
					dv = Vector2D(0f, -mag)
				}

				in 180..270 -> {
					// LL quadrant
					start = Vector2D(-offset, 0f)
					dv = Vector2D(0f, mag)
				}

				else -> {
					// LR quadrant
					start = Vector2D(offset, 0f)
					dv = Vector2D(0f, mag)
				}
			}
		}
	}

	private val preActor: MutableList<ZAnimation> = ArrayList()
	private val postActor: MutableList<ZAnimation> = ArrayList()
	private val overlayAnimations: MutableList<ZAnimation> = ArrayList()
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

	private var highlightedCell: Pos? = null
		private set
	var highlightedResult: UIZButton? = null
		private set

	//	var highlightedDoor: ZDoor? = null
	private var selectedCell: Pos? = null

	private var actorsAnimating = false
		set(value) {
			field = value
		}
	private var overlayToDraw: Any? = null
	var drawTiles = false
	var drawDebugText = false
	var drawRangedAccessibility = false
	var drawTowersHighlighted = false
	var drawZombiePaths = false
	var drawScreenCenter = false
	var drawClickable = false
	var miniMapMode = MiniMapMode.OFF

	var isFocussed = false
	var savedCenter: IVector2D? = null
	var lastUsage = 0
	var tiles: Array<ZTile> = arrayOf()
	var tileIds = IntArray(0)

	private val hoverMap = mutableMapOf<IShape, MutableList<HoverMessage>>()
	var desiredZoomType = ZoomType.CROP_FIT
	var currentZoomType = ZoomType.UNDEFINED
	var board: ZBoard = ZBoard()
		set(value) {
			if (field == value)
				return
			field = value
			initViewport()
			zoomRectStack.clear()
			_zoomedRect = getZoomRectForType(desiredZoomType)
			zoomRectStack.push(_zoomedRect)
			redraw()
		}

	var quest: ZQuest? = null
		set(value) {
			if (field?.quest != value?.quest) {
				clearTiles()
			}
			field = value
			redraw()
		}

	var boardMessageColor = GColor.WHITE
	var boardMessage: String? = null
		set(value) {
			boardMessage
			field = value
			redraw()
		}
	private val listeners = HashSet<Listener>()
	private val R = Renderer(this)

	private val zoomRectStack by lazy {
		Stack<GRectangle>().also {
			require(!board.isEmpty)
			it.push(GRectangle(0, 0, board.width, board.height))
		}
	}

	private var _zoomedRect = GRectangle(0, 0, 1, 1)
		set(value) {
			field = value
			log.info("_zoomRect set to $value from:")
//			logCodeLocation()
		}

	private var viewport = GDimension()

	val zoomPercent: Float
		get() {
			if (viewport.area > 1) {
				return _zoomedRect.area / viewport.area
			}
			return 0f
		}

	private val pickStack = Stack<List<UIZButton>>()

	fun pickStackClear() {
		pickStack.clear()
	}

	fun pickStackPush(list: List<UIZButton>) {
		launchIn {
			waitForAnimations(1000)
			animateZoomTo(*list.toTypedArray())
			list.firstOrNull()?.let {
				if (isFocussed)
					mouseV.assign(it.center)
				redraw()
			}
			pickStack.push(list)
		}
	}

	fun pickStackPop(): Boolean {
		if (pickStack.size > 1) {
			pickStack.pop()
			val list = pickStack.peek()
			animateZoomTo(*list.toTypedArray())
			list.firstOrNull()?.let { button ->
				if (isFocussed)
					mouseV.assign(button.center)
			}
			redraw()
			return true
		}
		return false
	}

	private val pickableRects: List<UIZButton>
		get() = pickStack.peekOrNull() ?: emptyList()

	var currentCharacter: ZCharacter? = null
		private set

	val numOverlayTextAnimations: Int
		get() = Utils.count(overlayAnimations) { a: ZAnimation? -> a is OverlayTextAnimation }


	val isAnimating: Boolean
		get() = actorsAnimating
			|| postActor.size > 0
			|| preActor.size > 0
			|| overlayAnimations.size > 0
			|| zoomAnimation != null

	var highlightAnimationScale = 1f
	val highlightAnimation = object : GAnimation(1000, -1, true) {
		override fun draw(g: AGraphics, position: Float, dt: Float) {
			g.color = GColor.CYAN.interpolateTo(GColor.TRANSPARENT, position)
		}
	}.start()

	val boardCenter: IVector2D
		get() = computeBoardCenter().also {
			//log.debug("board center: $it")
		}

	val mouseV = MutableVector2D(-1f, -1f)

	private var dragStartV = Vector2D()

	private val zoomRectMutex = Mutex()

	suspend fun pushZoomRect() {
		zoomRectMutex.withLock {
			zoomRectStack.push(_zoomedRect.deepCopy())
		}
	}

	suspend fun popZoomRect() {
		zoomRectMutex.withLock {
			if (zoomRectStack.size > 1) {
				animateZoomTo(zoomRectStack[zoomRectStack.size - 2])
				waitForAnimations()
				zoomRectStack.pop()
				_zoomedRect = zoomRectStack.peek().deepCopy()
			}
		}
	}

	suspend fun popAllZoomRects() {
		zoomRectMutex.withLock {
			if (zoomRectStack.size > 1) {
				waitForAnimations()
				animateZoomTo(zoomRectStack[0])
				waitForAnimations()
				while (zoomRectStack.size > 1)
					zoomRectStack.pop()
				_zoomedRect = zoomRectStack.peek().deepCopy()
			}
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

	fun setCurrentCharacter(c: ZCharacter?) {
		log.debug("Set current character ${c?.name()}")
		c?.let {
			setHighlightActor(it)
		}
		currentCharacter = c
		redraw()
	}

	fun stopAnimations() {
		preActor.clear()
		postActor.clear()
		overlayAnimations.clear()
		board.getAllActors().forEach {
			it.stopAnimating()
		}
		zoomAnimation = null
	}

	fun setHighlightActor(actor: ZActor<*>?) {
		if (highlightedResult != actor) {
			log.debug("highlighted actor: $actor")
			highlightedResult = actor
			actor?.let {
				mouseV.assign(it.center)
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

	fun addHoverMessage(rect: IShape, txt: String, vararg params: Any?) {
		val list = hoverMap.getOrPut(rect) { mutableListOf() }
		list.add(HoverMessage(String.format(txt, *params), rect))
		fireNextHoverMessage(rect)
	}

	private fun fireNextHoverMessage(rect: IShape) {
		hoverMap[rect]?.let { list ->
			list.firstOrNull()?.let {
				if (it.isRunning || it.isDone) {
					list.removeFirstOrNull()
					fireNextHoverMessage(rect)
					return
				} else {
					it.start()
					postActor.add(it)
					redraw()
				}

			} ?: hoverMap.remove(rect)
		}
	}

	fun addOverlay(a: ZAnimation) {
		overlayAnimations.add(a)
		redraw()
	}

	fun zoomAmt(percent: Float) {
		log.debug("zoomAmt: $percent")
		val targetRect = _zoomedRect.scaledBy(percent)
		currentZoomType = ZoomType.UNDEFINED
		animateZoomTo(targetRect)
	}

	private fun getZoomRectForType(type: ZoomType): GRectangle = when (type) {
		ZoomType.UNDEFINED -> TODO("This shouldn't happen")
		ZoomType.CROP_FIT -> {
			val d = max(board.width, board.height)
			viewport.cropFit(GRectangle(0f, 0f, d, d).withCenter(boardCenter))
		}

		ZoomType.X2 -> {
			val d = .5f * (min(board.width, board.height) + 3f)
			GRectangle(0f, 0f, d, d).setAspectFill(viewportAspect).withCenter(boardCenter)
		}
		ZoomType.MAX -> viewport.fitInner(GRectangle(0f, 0f, 3f, 3f).withCenter(boardCenter))
		ZoomType.FILL_FIT -> {
			val d = max(board.width, board.height)
			viewport.fillFit(GRectangle(0f, 0f, d, d).withCenter(boardCenter))
		}
	}

	fun toggleZoomType(noAnimate: Boolean = false) {
		if (desiredZoomType == currentZoomType) {
			currentZoomType.increment(1, ZoomType.entries.filter {
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

	fun drawAnimations(anims: MutableList<ZAnimation>, g: AGraphics) {
		anims.toList().forEach { it ->
			if (it.isDone) {
				anims.remove(it)
			} else {
				if (!it.isStarted) it.start()
				it.update(g)
			}
		}
	}

	fun drawActors(g: AGraphics): ZActor<*>? {
		var picked: ZActor<*>? = null
		var distFromCenter = 0f
		var numActorsAnimating = 0

		fun drawActor(a: ZActor<*>) {
			g.getImage(a.imageId)
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
				if (drawDebugText) {
					g.color = GColor.MAGENTA
					g.drawRect(a.getRect())
				}
				numActorsAnimating++
			} else {
				var outline: GColor = GColor.WHITE
				highlightAnimationScale = 1f
				if (currentCharacter == a) {
					outline = GColor.GREEN
				} else if (a === picked) {
					outline = GColor.CYAN
				} else if (a in pickableRects) {
					outline = GColor.YELLOW
				}

				if (a == highlightedResult) {
					outline = GColor.RED
				}

				if (ANIMATE_HIGHLIGHTED_ACTOR) {
					(highlightedResult as? ZActor<*>?)?.let {
						if (it == a) {
							highlightAnimation.update(g)
							outline = g.color
							redraw()
						}
					}
				}
				drawActor(g, a, outline)
				g.removeFilter()
				if (false) {
					g.color = GColor.YELLOW.withAlpha(.5f)
					g.drawRect(a.getRect(), 1f)
					g.color = GColor.YELLOW
					g.drawRect(a.position.toRect(board))
				}
			}
		}

		with(board.getAllActors().filter { it.isRendered }) {
			filter { !it.isAnimating }.forEach {
				drawActor(it)
			}

			filter { it.isAnimating }.forEach {
				drawActor(it)
			}
		}

		picked?.takeIf { !it.isAnimating }?.let { actor ->
			drawActor(actor) // draw the highlighted actor over the top to see its stats
			(actor as? ZZombie)?.let { zombie ->
				zombie.getTargetZone(board)?.let {
					g.color = GColor.YELLOW
					drawPath(g, zombie, board.getShortestPath(zombie, it.zoneIndex))
				}
			}
		}

		picked?.let { actor ->
			listeners.toList().forEach {
				it.onActorHighlighted(actor)
			}
			if (drawDebugText) {
				g.color = GColor.YELLOW
				g.pushTextHeight(DEBUG_TEXT_HEIGHT, false)
				g.drawString(
					"""
					name: ${actor.type}
					zone: ${actor.occupiedZone}
					pos: ${actor.occupiedCell}
					""".trimIndent(), actor.topLeft,
					bkColor = GColor.TRANSLUSCENT_BLACK,
					border = 3f
				)
				g.popTextHeight()
			}
		}

		actorsAnimating = numActorsAnimating > 0

		return picked
	}

	protected open fun drawActor(g: AGraphics, actor: ZActor<*>, outline: GColor?) {
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
		val old = g.setLineWidth(2f)
		zone.drawOutlined(g)
		g.setLineWidth(old)
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
				val iconRect = GRectangle(zone.getRect().topLeft, .2f, .2f)
				g.drawImage(ZIcon.NOISE.imageIds[0], iconRect)
				if (zone.noiseLevel > 1) {
					g.color = GColor.WHITE
					g.drawJustifiedString(
						iconRect.topRight,
						Justify.LEFT,
						Justify.TOP,
						"X ${zone.noiseLevel}"
					)
				}
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
	 * @param miniMap
	 * @return
	 */
	fun drawZones(g: AGraphics, miniMap: Boolean): Pos? {
		var result: Pos? = null
		board.zones.forEach { zone ->
			for (pos in zone.getCells()) {
				val cell = board.getCell(pos)
				if (cell.isCellTypeEmpty) continue
				when (zone.type) {
					ZEnvironmentType.TOWER -> if (miniMap || drawTowersHighlighted) {
						g.color = GColor.SKY_BLUE.withAlpha(cell.scale / 3)
						g.drawFilledRect(cell)
					}
					else -> Unit
				}

				val d = g.pushDepth
				try {
					drawCellWalls(g, pos, ZDir.compassValues, 1f, miniMap)
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
				for (type in ZCellType.entries) {
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
							ZCellType.RUBBLE -> g.drawImage(ZIcon.RUBBLE.imageIds[0], cell)
							else -> Unit
						}
					}
				}
				for (area in cell.spawnAreas) {
					with (drawSpawn(g, cell, area)) {
						//board.getAllZombies().firstOrNull { it.escapeZone == area }?.let {
						//	g.color = GColor.RED
						//	g.drawRect(this, 2f)
						//}
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
					g.drawJustifiedStringOnBackground(
						cell.center,
						Justify.CENTER,
						Justify.CENTER,
						text,
						GColor.TRANSLUSCENT_BLACK,
						10f,
						2f
					)
				}
				if (!miniMap && zone.type == ZEnvironmentType.HOARD) {
					g.color = GColor.WHITE
					val msg: String = board.hoard.let {
						if (it.isEmpty()) {
							"EMPTY"
						} else it.map { (type, count) ->
							"$type X $count"
						}.joinToString("\n")
					}

					g.drawJustifiedString(zone.center, Justify.CENTER, Justify.CENTER, msg)
				}
			}
			if (zone.type == ZEnvironmentType.VAULT) {
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
		area.setRect(rect.scaledBy(.8f).fit(img, dir.horz, dir.vert))
		g.drawImage(id, area.getRect())
		if (drawDebugText) {
			var txt = ""
			if (area.isCanSpawnNecromancers) txt += "\nNecros"
			if (area.isEscapableForNecromancers) txt += "\nEscapable"
			if (area.isCanBeRemovedFromBoard) txt += "\nDestroyable"
			//String txt = String.format("spawnsNecros:%s\nEscapable:%s\nRemovable:%s\n", area.isCanSpawnNecromancers(), area.isEscapableForNecromancers(), area.isCanBeRemovedFromBoard());
			g.color = GColor.YELLOW
			g.pushTextHeight(DEBUG_TEXT_HEIGHT, false)
			g.drawString(txt.trim { it <= ' ' }, area.getRect().topLeft)
			g.popTextHeight()
		}

		return area.getRect()
	}

	/*
		inner class DoorAnimation(val pos : Pos, val dir : ZDir) : AAnimation<AGraphics>(1500) {

			val cell = board.getCell(pos)
			val center = cell.center
			val v0 = cell.topLeft.subEq(center)
			val v1 = cell.topRight.subEq(center)

			override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.pushColor(GColor.ORANGE)
				g.pushMatrix()
				g.translate(center)
				val dv: Vector2D = v1.sub(v0).scaleEq(.33f)
				val dv0: Vector2D = v0.add(dv)
				val dv1: Vector2D = dv0.add(dv)
				g.rotate(dir.rotation.toFloat())
				g.drawLine(v0, dv0, 3f)
				g.drawLine(dv1, v1, 3f)
				g.drawLine(dv0, dv0.add(dv.scaledBy(.5f).rotate(145f * position)), 4f)
				g.drawLine(dv1, dv1.sub(dv.scaledBy(.5f).rotate(-145f * position)), 4f)
				g.popMatrix()
				g.popColor()
			}
		}

		inner class VaultDoorAnimation(val pos : Pos, val dir : ZDir) : AAnimation<AGraphics>(2500) {

			var door = board.findDoor(pos, dir)
			val center = board.getCell(pos).center

			override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.pushColor(GColor.BLACK)
				g.pushMatrix()
				g.translate(center)
				val vaultRect = door.getRect(board)
				vaultRect.drawFilled(g)
				// draw the 'lid' opened
				g.begin()
				g.vertex(vaultRect.topRight)
				g.vertex(vaultRect.topLeft)
				val dh = position * vaultRect.h / 3
				val dw = position * vaultRect.w / 5
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
				vaultRect.drawOutlined(g, 2)
				g.drawLineLoop(2f)
				g.popMatrix()
				g.popColor()
			}
		}

		private val doorsMap = mutableMapOf<ZDoor, AAnimation<AGraphics>>()

		fun openDoor(door : ZDoor) {
			doorsMap[door]?.start<AAnimation<AGraphics>>()
		}

		fun closeDoor(door : ZDoor) {
			doorsMap[door]?.startReverse<AAnimation<AGraphics>>()
		}

		fun drawDoors(g : AGraphics) {
			doorsMap.values.forEach {
				it.update(g)
			}
		}

		fun initDoorAnimations() {
			if (doorsMap.isEmpty()) {
				board.zones.forEach { zone ->
					zone.doors.forEach { door ->
						when (door.moveDirection) {
							ZDir.NORTH,
							ZDir.SOUTH,
							ZDir.EAST,
							ZDir.WEST -> {
								DoorAnimation(door.cellPosStart, door.moveDirection).apply {
									if (!door.isClosed(board)) {
										start<AAnimation<AGraphics>>()
									} else {
										startReverse()
									}.apply {
										doorsMap.getOrPut(door) { this }
										doorsMap.getOrPut(door.otherSide) { this }
									}
								}
							}
							ZDir.ASCEND -> doorsMap.getOrDefault(door, VaultDoorAnimation(door.cellPosStart, door.moveDirection))
							ZDir.DESCEND -> doorsMap.getOrDefault(door, VaultDoorAnimation(door.cellPosStart, door.moveDirection))
						}
					}
				}
			}

		}
	*/
	fun drawCellWalls(g: AGraphics, cellPos: Pos, dirs: Array<ZDir>, scale: Float, miniMap: Boolean) {
		val cell = board.getCell(cellPos)
		g.pushMatrix()
		val center = cell.center
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
							val padlock =
								GRectangle(0f, 0f, .2f, .2f).withCenter(dv1.add(dv0).scaleEq(.5f))
							drawPadlock(g, padlock)
//							val img = g.getImage(ZIcon.PADLOCK.imageIds[0])
//							g.drawImage(ZIcon.PADLOCK.imageIds[0], padlock.fit(img))
						}
					}
				}

				ZWallFlag.CLOSED -> if (dir === ZDir.SOUTH || dir === ZDir.WEST) {
					g.drawLine(v0, v1, wallThickness.toFloat())
					g.color = doorColor
					g.drawLine(dv0, dv1, (wallThickness + 1).toFloat())
				}

				ZWallFlag.HEDGE -> if (dir === ZDir.SOUTH || dir === ZDir.WEST) {
					g.color = GColor.GREEN
					g.drawLine(v0, v1, wallThickness.toFloat() * 2)
				}

				ZWallFlag.LEDGE -> {
					if (board.getZone(cell.zoneIndex).type == ZEnvironmentType.WATER) {
						// draw a 'ledge' down into the water
						g.begin()
						g.pushMatrix()
						g.vertex(v0)
						g.vertex(v1)
						g.translate(0.1f, 0.1f)
						g.vertex(v0)
						g.translate(0.8f, 0f)
						g.vertex(v0)
						g.drawQuadStrip()
						g.popMatrix()
						g.end()
					}
				}

				ZWallFlag.NONE -> Unit
			}
			g.popMatrix()
		}
		g.popMatrix()
		val vaultLineThickness = if (miniMap) 1 else 2
		for (dir in ZDir.elevationValues) {
			when (cell.getWallFlag(dir)) {
				ZWallFlag.LOCKED -> {
					val door = board.findDoor(cellPos, dir)
					g.color = GColor.BLACK
					val vaultRect = door.getRect()
					g.color = door.lockedColor
					vaultRect.drawFilled(g)
					g.color = GColor.RED
					vaultRect.drawOutlined(g, vaultLineThickness)
					if (!miniMap) {
						drawPadlock(g, door.getRect().scaledBy(.7f))
					}
				}
				ZWallFlag.CLOSED -> {
					board.findDoorOrNull(cellPos, dir)?.let { door ->
						g.color = GColor.BLACK
						val vaultRect = door.getRect()
						g.color = door.lockedColor
						vaultRect.drawFilled(g)
						g.color = GColor.YELLOW
						vaultRect.drawOutlined(g, vaultLineThickness)
					}
				}
				ZWallFlag.OPEN -> {
					board.findDoorOrNull(cellPos, dir)?.let { door ->
						g.color = GColor.BLACK
						val vaultRect = door.getRect()
						vaultRect.drawFilled(g)
						// draw the 'lid' opened
						g.begin()
						g.vertex(vaultRect.topRight)
						g.vertex(vaultRect.topLeft)
						val dh = vaultRect.height / 3
						val dw = vaultRect.width / 5
						if (dir === ZDir.ASCEND) {
							// open 'up'
							g.moveTo(-dw, -dh)
							g.moveTo(vaultRect.width + dw * 2, 0f)
						} else {
							// open 'down
							g.moveTo(dw, dh)
							g.moveTo(vaultRect.width - dw * 2, 0f)
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
				g.color = when (a) {
					is ZCharacter -> a.color
					is ZZombie -> GColor.LIGHT_GRAY
					else -> GColor.BLACK
				}
				val rect = a.getRect(board)
				g.drawFilledOval(rect.scaleEq(.8f))
			}
		} finally {
			g.removeFilter()
			g.popMatrix()
		}
	}

	fun drawPadlock(g: AGraphics, rect: IRectangle) {
		val img = g.getImage(ZIcon.PADLOCK.imageIds[0])
		g.drawImage(ZIcon.PADLOCK.imageIds[0], rect.fit(img))
	}

	fun drawDebugText(g: AGraphics, pos: Pos) {
		val cell = board.getCell(pos)
		if (cell.isCellTypeEmpty)
			return
		g.pushTextHeight(DEBUG_TEXT_HEIGHT, false)
		var text = "${cell.environment} Z${cell.zoneIndex} [${cell.left},${cell.top}]"
		for (type in ZCellType.entries) {
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
		g.color = GColor.CYAN
		for (a in cell.getOccupants(board)) {
			text += "\n\n${a.name()}".trimIndent()
		}
		if (cell.vaultId > 0) {
			text += "\n\nvaultFlag ${cell.vaultId}".trimIndent()
		}
		g.drawJustifiedStringOnBackground(cell.center, Justify.CENTER, Justify.CENTER, text, GColor.TRANSLUSCENT_BLACK, 10f, 3f)
		g.popTextHeight()
	}

	fun drawNoTiles(g: AGraphics): Pos? {
		g.clearScreen()
		val returnCell: Pos? = null
		val it = board.getCellsIterator()
		while (it.hasNext()) {
			val cell = it.next()
			if (cell.isCellTypeEmpty) continue
			g.color = cell.environment.color
			g.drawFilledRect(cell)
		}
		return returnCell
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
			return it.center
		}
		highlightedResult?.let {
			logIf(2, "2:Using highlightedActor")
			return it.center
		}

		currentCharacter?.let {
			savedCenter = it.center
			return savedCenter!!
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
		onSizeChanged(viewportWidth, viewportHeight)
	}

	open fun onLoading() {}
	open fun onLoaded() {}

	/**
	 *
	 */
	fun animateZoomTo(vararg newRects: IRectangle) {
		zoomAnimation = ZoomAnimation(clampRect(GRectangle().also { rect ->
			newRects.forEach {
				rect.addEq(it)
			}

		}), this).start()
		redraw()
	}

	fun animateZoomTo(type: ZoomType) {
		zoomAnimation = ZoomAnimation(clampRect(getZoomRectForType(type)), this).start()
		redraw()
	}

	fun drawQuestLabel(g: AGraphics) {
		g.color = GColor.BLACK
		if (quest != null) {
			g.pushTextHeight(24f, false) //setFont(bigFont);
			g.setTextStyles(AGraphics.TextStyle.BOLD, AGraphics.TextStyle.ITALIC)
			g.drawJustifiedString(
				10f,
				height - 10 - g.textHeight,
				Justify.LEFT,
				Justify.BOTTOM,
				quest!!.name
			)
			g.popTextHeight()
			g.setTextStyles(AGraphics.TextStyle.NORMAL)
		}
	}

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

	override fun draw(g: AGraphics) {
		synchronized(UIZombicide.instance.synchronizeLock) {
			drawPrivate(g)
		}
	}

	override fun updateMouseOrTouch(g: APGraphics, mx: Int, my: Int) {
		g.setIdentity()
		g.ortho(_zoomedRect)
		highlightedCell = null
		highlightedResult = null
		mouseV.assign(g.screenToViewport(mx, my))
	}

	private fun drawPrivate(g: AGraphics) {

		g.setIdentity()
		val highlightedActor = highlightedResult as? ZActor<*>?
		if (zoomAnimation?.isDone != false) {
			UIZombicide.instance.currentCharacter?.takeIf { it.isAnimating && it.animations[0].isCentered }?.let {
				clampRect(_zoomedRect.setCenter(it.center))
			}
		}
		g.ortho(_zoomedRect)
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
		if (drawDebugText)
			highlightedCell?.let {
				drawDebugText(g, it)
			}
		drawActors(g)

		highlightedActor?.let { actor ->
			if (drawRangedAccessibility) {
				g.color = GColor.BLUE.withAlpha(.5f)
				for (zIndex in board.getAccessibleZones(actor, 1, 5, ZActionType.RANGED)) {
					drawZoneFilled(g, zIndex)
				}
			}
		}

		drawAnimations(postActor, g)

		highlightedCell?.let {
			val cell = board.getCell(it)
			g.color = GColor.RED.withAlpha(32)
			drawZoneOutline(g, cell.zoneIndex)
		}

		if (drawScreenCenter) {
			g.color = GColor.RED
			g.drawCircle(boardCenter, .25f)
		}

		highlightedResult = pickableRects.firstOrNull { it.contains(mouseV) }
		pickableRects.firstOrNull()?.let {
			g.color = GColor.TRANSLUSCENT_BLACK
			it.parent?.menuRect?.drawFilled(g)
		}

		pickableRects.forEach {
			it.draw(g, UIZombicide.instance, false)
		}

		highlightedResult?.draw(g, UIZombicide.instance, true)

		drawOverlay(g)

		if (zoomAnimation?.isDone == true) {
			zoomAnimation = null
		} else {
			zoomAnimation?.update(g)
		}
		if (isAnimating) {
			redraw()
		} else {
			drawMiniMap(g)
			animLock.releaseAll()
		}
	}

	private val animLock = KLock()
	//private var animDoneContinue: Continuation<Any?>? = null

	suspend fun waitForAnimations(maxWaitSecs: Int = 4) {
		animLock.reset()
		redraw()
		val t = GlobalTimer.currentTimeMillis()
		log.debug("waitForAnimations $maxWaitSecs start $t")
		animLock.acquireAndBlock(maxWaitSecs * 1000L)
		val done = GlobalTimer.currentTimeMillis() - t
		log.debug("waitForAnimations done after $done millis")
	}

	suspend fun wait(msecs: Int) {
		delay(msecs.toLong())
	}

	fun drawPath(g: AGraphics, actor: ZActor<*>, path: List<ZDir>) {
		g.begin()
		val start = actor.getRect().center
		var curCell = actor.occupiedCell
		g.vertex(start)
		val next = MutableVector2D(start)
		for (dir in path) {
			when (dir) {
				ZDir.ASCEND,
				ZDir.DESCEND -> {
					val cell = board.getCell(curCell)
					g.vertex(cell.center)
					g.drawLineStrip(3f)
					g.begin()
					board.getZone(cell.zoneIndex).doors.first {
						it.isVault && it.cellPosStart == curCell
					}.apply {
						val nextCell = board.getCell(cellPosEnd)
						next.assign(nextCell.center)
						curCell = cellPosEnd
						g.vertex(next)
					}
				}

				else -> {
					curCell = Pos(curCell.row + dir.dy, curCell.column + dir.dx)
					next.addEq(dir.dx.toFloat(), dir.dy.toFloat())
					g.vertex(next)
				}
			}
		}
		g.drawLineStrip(3f)
	}

	protected fun drawOverlay(g: AGraphics) {
		g.setIdentity()
		g.ortho()
		drawMessage(g)
		drawAnimations(overlayAnimations, g)
		drawOverlayObject(overlayToDraw, g)
		if (UIZombicide.instance.isGameRunning())
			drawDeckInfo(g)
		drawConnectedUsers(g)
		drawQuestLabel(g)
	}

	protected fun drawDeckInfo(g: AGraphics) {
		g.pushTextHeight(18f, false)
		val padding = 5f
		val radius = 5f
		val dim = GDimension(g.getTextWidth("WW"), g.textHeight).withAspect(1f / 1.5f)
		g.pushMatrix()
		g.translate(viewportWidth - padding - dim.width, viewportHeight / 2 - padding - dim.height)
		drawDeck(g, dim, radius, GColor.RED, UIZombicide.instance.spawnDeckSize, "SPAWN")
		g.translate(0f, dim.height + padding)
		drawDeck(g, dim, radius, GColor.BLUE, UIZombicide.instance.lootDeckSize, "LOOT")
		g.translate(0f, dim.height + padding)
		UIZombicide.instance.hoardSize.takeIf { it > 0 }?.let { count ->
			drawDeck(g, dim, radius, GColor.GREEN, count, "HOARD")
		}
		g.popMatrix()
		g.popTextHeight()
	}

	protected fun drawConnectedUsers(g: AGraphics) {
		g.pushMatrix()
		val padding = 5f
		val barWidth = 10f
		g.translate(viewportWidth - padding, viewportHeight - padding)
		UIZombicide.instance.connectedUsersFlow.value.reversed().forEach { user ->
			g.pushMatrix()
			g.translate(-barWidth, -padding)
			drawConnectionStatus(g, user.status, barWidth, g.textHeight, 2f)
			g.translate(-padding, padding)
			g.color = ZUser.getUserColor(user.colorId)
			val text = "" //String.format("%s%s [%s]", if (user.startUser) ">> " else "", user.name, user.type.code)
			g.drawString(
				text, 0, 0,
				hJust = Justify.RIGHT,
				vJust = Justify.BOTTOM,
				bkColor = GColor.TRANSLUSCENT_BLACK,
				border = 3
			)
			g.popMatrix()
			g.translate(0f, -g.textHeight - padding)
		}
		g.popMatrix()
	}

	fun drawConnectionStatus(
		g: AGraphics,
		quality: NetConnectQuality,
		width: Float,
		height: Float,
		padding: Float
	) {
		val barHeight = (height / 3) - (padding / 2)
		// first bar
		g.pushMatrix()
		for (bar in 1..3) {
			g.color = quality.color
			g.drawFilledRect(0f, 0f, width, barHeight)
			g.translate(0f, -barHeight - padding)
		}
		g.popMatrix()
	}

	private fun drawDeck(
		g: AGraphics,
		dimension: IDimension,
		radius: Float,
		color: GColor,
		count: Int,
		description: String
	) {
		val c = g.color
		val w = dimension.width
		val h = dimension.height
		g.color = color
		g.drawFilledRoundedRect(0f, 0f, w, h, radius)
		g.color = GColor.WHITE
		g.setLineWidth(2f)
		g.drawRoundedRect(0f, 0f, w, h, radius)
		g.pushMatrix()
		g.translate(w / 2, h / 2)
		g.drawJustifiedString(0f, 0f, Justify.CENTER, Justify.CENTER, "$count")
		g.popMatrix()
		g.pushMatrix()
		g.translate(-radius, h / 2)
		g.pushTextHeight(12f, false)
		g.drawJustifiedStringOnBackground(
			0f,
			0f,
			Justify.RIGHT,
			Justify.CENTER,
			description,
			GColor.TRANSLUSCENT_BLACK,
			radius
		)
		g.popTextHeight()
		g.popMatrix()
		g.color = c
	}

	protected fun drawMessage(g: AGraphics) {
		g.color = GColor.WHITE
		boardMessage?.let { message ->
			g.drawJustifiedStringOnBackground(
				10f,
				height - 10,
				Justify.LEFT,
				Justify.BOTTOM,
				message,
				GColor.TRANSLUSCENT_BLACK,
				borderThickness
			)
		}
		if (drawDebugText) {
			val game = UIZombicide.instance
			g.drawString(
				text = """$desiredZoomType/$currentZoomType/${(100f * zoomPercent).roundToInt()}
				   ZoomRect: $_zoomedRect
				   Viewport: $viewport
				   Board: ${board.width} x ${board.height}
				   User: ${game.currentUserName} [${game.currentUserColorId}]
				""".trimMargin(),
				x = 10,
				y = 10,
				bkColor = GColor.TRANSLUSCENT_BLACK,
				border = 3
			)
		}
	}

	private fun drawOverlayObject(obj: Any?, g: AGraphics) {
		// overlay
		if (obj == null) return
		if (obj is Int) {
			val id = obj
			if (id >= 0) {
				val img = g.getImage(id)
				var rect = GRectangle(0f, 0f, width, height)
				rect.scaleEq(.9f, .9f)
				rect = rect.fit(img, Justify.LEFT, Justify.CENTER)
				g.drawImage(id, rect)
			}
		} else if (obj is Table) {
			g.color = GColor.YELLOW
			val cntr: IVector2D = Vector2D(width / 2, height / 2)
			g.pushTextHeight(20f, false)
			obj.draw(g, cntr, Justify.CENTER, Justify.CENTER)
			g.popTextHeight()
		} else if (obj is String) {
			g.color = GColor.WHITE
			g.drawWrapStringOnBackground(width / 2, height / 2, width / 2, Justify.CENTER, Justify.CENTER, obj, GColor.TRANSLUSCENT_BLACK, 10f)
		} else if (obj is ZAnimation) {
			val a = obj
			if (!a.isStarted) {
				a.start()
			}
			if (!a.isDone) {
				a.update(g)
				redraw()
			}
		} else if (obj is AImage) {
			obj.draw(g, viewport.width / 2 - obj.width / 2, viewport.height / 2 - obj.height / 2)
		} else if (obj is List<*>) {
			drawOverlayList(obj.filterIsInstance<IMeasurable>(), g)
		}
	}

	private fun drawOverlayList(list: List<IMeasurable>, g: AGraphics) {
		var width = 0f
		var height = 0f
		for (m in list) {
			val dim = m.measure(g)
			width += dim.width
			height = height.coerceAtLeast(dim.height)
		}
		val padding = 20f
		width += padding * (list.size - 1)
		g.transform(g.viewportWidth / 2 - width / 2, (g.viewportHeight / 2).toFloat())
		for (m in list) {
			drawOverlayObject(m, g)
		}
	}

	val borderThickness: Float
		get() = 5f

	fun hideOrSetOverlay(obj: Any?) {
		if (overlayToDraw != null) {
			overlayToDraw = null
		} else if (obj != null) {
			setOverlay(obj)
		}
		redraw()
	}

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
		highlightedResult?.onClick()
		highlightedCell = null
		highlightedResult = null
		redraw()
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
		if (rect.width > viewport.width || rect.height >= viewport.height) {
			rect.dimension = viewport
		} else {
			rect.setDimension(rect.width.coerceAtLeast(3f), rect.height.coerceAtLeast(3f))
		}

		// grow rect to match the aspect ratio of viewPort
		rect.setAspectCrop(viewportAspect)

		val minX = (board.width / 2 - rect.width / 2).coerceAtMost(0f)
		val minY = (board.height / 2 - rect.height / 2).coerceAtMost(0f)
		val maxX = (board.width / 2 + rect.width / 2).coerceAtLeast(board.width) - rect.width
		val maxY = (board.height / 2 + rect.height / 2).coerceAtLeast(board.height) - rect.height
		log.verbose("clampRect rect: $rect, board: ${board.width}x${board.height} minX:$minX, maxX:$maxX, minY:$minY, maxY:$maxY")
		if (minX <= maxX)
			rect.left = rect.left.coerceIn(minX, maxX)
		else
			rect.left = minX
		if (minY <= maxY)
			rect.top = rect.top.coerceIn(minY, maxY)
		else
			rect.top = minY
		//log.debug("rect after: $rect")
		return rect
	}

	override fun onDragStart(x: Int, y: Int) {
		R.setOrtho(_zoomedRect)
		dragStartV = R.untransform(x.toFloat(), y.toFloat())
	}

	override fun onDragMove(x: Int, y: Int) {
		val v = R.untransform(x.toFloat(), y.toFloat())
		if (v.isNaN)
			return
		val dv = dragStartV.sub(v)
		_zoomedRect.moveBy(dv)
		dragStartV = v
		clampZoomRect()
		currentZoomType = ZoomType.UNDEFINED
		redraw()
	}

	override fun onZoom(scale: Float) {
		_zoomedRect.scaleEq(scale)
		clampZoomRect()
		currentZoomType = ZoomType.UNDEFINED
		redraw()
	}

	override fun onSizeChanged(w: Int, h: Int) {
		if (board.isEmpty)
			return

		if (viewportAspect <= 0)
			return

		initViewport()

		currentZoomType = ZoomType.UNDEFINED
		toggleZoomType(true)
	}

	private fun initViewport() {
		viewport = if (board.aspect < viewportAspect) {
			GDimension(viewportAspect * board.height, board.height)
		} else {
			GDimension(board.width, board.width / viewportAspect)
		}
	}

	override fun onFocusChanged(focussed: Boolean) {
		log.debug("onFocusChanged $focussed")
		isFocussed = focussed
		if (!isFocussed) {
			setHighlightActor(null)
			highlightedResult = null
		} else {
			pickableRects.firstOrNull()?.let {
				mouseV.assign(it.center)
			}
		}
		redraw()
	}

	private fun isInDirection(rect: IRectangle, dir: UIKeyCode, center: IVector2D): Boolean = when (dir) {
		UIKeyCode.UP -> rect.center.y < center.y
		UIKeyCode.DOWN -> rect.center.y > center.y
		UIKeyCode.LEFT -> rect.center.x < center.x
		UIKeyCode.RIGHT -> rect.center.x > center.x
		else -> false
	}

	private var moving = false

	fun animateMoveContinuouslyUntilStopped(dx: Float, dy: Float, zoom: Float) {
		moving = true
		launchIn {
			while (moving) {
				animateZoomTo(_zoomedRect.movedBy(dx, dy).scaledBy(zoom))
				waitForAnimations(1000)
			}
		}
	}

	override fun onKeyLongPressRelease(code: UIKeyCode) {
		moving = false
		stopAnimations()
		super.onKeyLongPressRelease(code)
	}

	override fun onKeyLongPress(code: UIKeyCode): Boolean {
		val moveAmt = .4f
		when (code) {
			UIKeyCode.RIGHT -> animateMoveContinuouslyUntilStopped(moveAmt, 0f, 1f)
			UIKeyCode.LEFT -> animateMoveContinuouslyUntilStopped(-moveAmt, 0f, 1f)
			UIKeyCode.UP -> animateMoveContinuouslyUntilStopped(0f, -moveAmt, 1f)
			UIKeyCode.DOWN -> animateMoveContinuouslyUntilStopped(0f, moveAmt, 1f)
			UIKeyCode.CENTER -> animateMoveContinuouslyUntilStopped(0f, 0f, .8f)
			UIKeyCode.BACK -> animateMoveContinuouslyUntilStopped(0f, 0f, 1.2f)
			else -> return false
		}
		return true
	}

	override fun onKeyTyped(code: UIKeyCode): Boolean {
		when (code) {
			UIKeyCode.UP,
			UIKeyCode.DOWN,
			UIKeyCode.LEFT,
			UIKeyCode.RIGHT -> {
				highlightedResult?.let { shape ->
					pickableRects.filter { isInDirection(it.enclosingRect(), code, shape.center) }.minByOrNull {
						it.center.sub(shape.center).magSquared()
					}?.let {
						mouseV.assign(it.center)
					} ?: return false
				} ?: run {
					highlightedResult = pickableRects.firstOrNull()
				}
			}

			UIKeyCode.CENTER -> {
				highlightedResult?.let {
					it.onClick()
				}
			}

			UIKeyCode.BACK -> {
				if (pickStackPop()) {
					return true
				} else {
					UIZombicide.instance.focusOnMainMenu()
				}
			}

			else -> return false
		}

		return true
	}

	companion object {
		const val HOVER_TEXT_HEIGHT = 20f
		const val DEBUG_TEXT_HEIGHT = 10f
		const val ANIMATE_HIGHLIGHTED_ACTOR = false
		val log = LoggerFactory.getLogger(UIZBoardRenderer::class.java)
	}
}