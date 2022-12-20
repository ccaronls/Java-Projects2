package cc.lib.zombicide.ui

import cc.lib.game.*
import cc.lib.logger.LoggerFactory
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.ui.IButton
import cc.lib.ui.UIComponent
import cc.lib.ui.UIRenderer
import cc.lib.utils.Grid
import cc.lib.utils.Table
import cc.lib.zombicide.*
import cc.lib.zombicide.ZDir.Companion.compassValues
import cc.lib.zombicide.ZDir.Companion.elevationValues
import cc.lib.zombicide.anims.OverlayTextAnimation
import cc.lib.zombicide.anims.ZoomAnimation
import cc.lib.zombicide.ui.UIZombicide.UIMode
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

open class UIZBoardRenderer<T : AGraphics>(component: UIZComponent<*>) : UIRenderer(component) {
	val preActor: MutableList<ZAnimation> = ArrayList()
	val postActor: MutableList<ZAnimation> = ArrayList()
	val overlayAnimations: MutableList<ZAnimation> = ArrayList()
	var zoomAnimation: ZAnimation? = null
	val clickables: MutableMap<IShape, MutableList<IButton>> = HashMap()
	var highlightedCell: Grid.Pos? = null
	var highlightedResult: Any? = null
	var highlightedActor: ZActor<*>? = null
	var highlightedDoor: ZDoor? = null
	var selectedCell: Grid.Pos? = null
	var highlightedShape: IShape? = null
	var highlightedMoves: List<IButton>? = null
	var actorsAnimating = false
	private var overlayToDraw: Any? = null
	var drawTiles = false
	var drawDebugText = false
	var drawRangedAccessibility = false
	var drawTowersHighlighted = false
	var drawZombiePaths = false
	var miniMapMode = 1
	var dragOffset = MutableVector2D(Vector2D.ZERO)
	var dragStart = Vector2D.ZERO
	val game: UIZombicide
		get() = UIZombicide.instance
	val board: ZBoard
		get() = game.board
	val quest: ZQuest?
		get() = if (game.questInitialized) game.quest else null

	@get:Synchronized
	val isAnimating: Boolean
		get() = actorsAnimating || postActor.size > 0 || preActor.size > 0 || overlayAnimations.size > 0 || zoomAnimation != null

	@Synchronized
	fun addPreActor(a: ZAnimation) {
		preActor.add(a.start())
		redraw()
	}

	@Synchronized
	fun addPostActor(a: ZAnimation) {
		postActor.add(a.start())
		redraw()
	}

	@Synchronized
	fun addOverlay(a: ZAnimation) {
		overlayAnimations.add(a)
		redraw()
	}

	private fun addClickable(rect: IShape, move: IButton) {
		var moves = clickables[rect]
		if (moves == null) {
			moves = ArrayList()
			clickables[rect] = moves
		}
		moves.add(move)
	}

	var submenu: List<IButton>? = null
	fun processSubMenu(cur: ZCharacter, options: List<IButton>) {
		if (ENABLE_ENHANCED_UI) {
			submenu = options
			redraw()
		}
	}

	fun processMoveOptions(cur: ZCharacter, options: List<ZMove>) {
		clickables.clear()
		if (!ENABLE_ENHANCED_UI) return
		for (move in options) {
			when (move.type) {
				ZMoveType.END_TURN -> {
				}
				ZMoveType.INVENTORY -> {
				}
				ZMoveType.TRADE -> for (c in (move.list as List<ZCharacter>)) addClickable(c.getRect(), ZMove(move, c))
				ZMoveType.WALK -> for (zoneIdx in (move.list as List<Int>)) addClickable(board.getZone(zoneIdx), ZMove(move, zoneIdx, zoneIdx))
				ZMoveType.WALK_DIR -> {
				}
				ZMoveType.MELEE_ATTACK, ZMoveType.RANGED_ATTACK, ZMoveType.MAGIC_ATTACK -> for (w in (move.list as List<ZWeapon>)) {
					for ((actionType, _, _, minRange, maxRange) in w.type.stats) {
						for (zoneIdx in board.getAccessableZones(cur.occupiedZone, minRange, maxRange, actionType)) {
							addClickable(board.getZone(zoneIdx), ZMove(move, w, zoneIdx))
						}
					}
				}
				ZMoveType.THROW_ITEM -> {
					val zones = board.getAccessableZones(cur.occupiedZone, 0, 1, ZActionType.THROW_ITEM)
					for (item in (move.list as List<ZEquipment<*>>)) {
						for (zoneIdx in zones) {
							addClickable(cur.getRect(), ZMove(move, item, zoneIdx))
						}
					}
				}
				ZMoveType.RELOAD -> addClickable(cur.getRect(), move)
				ZMoveType.OPERATE_DOOR -> for (door in (move.list as List<ZDoor>)) {
					addClickable(door.getRect(board).grow(.1f), ZMove(move, door))
				}
				ZMoveType.SEARCH, ZMoveType.CONSUME, ZMoveType.EQUIP, ZMoveType.UNEQUIP, ZMoveType.GIVE, ZMoveType.TAKE, ZMoveType.DISPOSE -> addClickable(cur.getRect(), move)
				ZMoveType.TAKE_OBJECTIVE -> addClickable(board.getZone(cur.occupiedZone), move)
				ZMoveType.DROP_ITEM, ZMoveType.PICKUP_ITEM -> for (e in (move.list as List<ZEquipment<*>>)) {
					addClickable(cur.getRect(), ZMove(move, e))
				}
				ZMoveType.MAKE_NOISE -> addClickable(cur.getRect(), move)
				ZMoveType.SHOVE -> for (zoneIdx in (move.list as List<Int>)) {
					addClickable(board.getZone(zoneIdx), ZMove(move, zoneIdx))
				}
				ZMoveType.REROLL -> {
				}
				ZMoveType.KEEP_ROLL -> {
				}
				ZMoveType.ENCHANT -> {
					board.getAllCharacters().filter {
						it.isAlive && board.canSee(cur.occupiedZone, it.occupiedZone)
					}.forEach { c ->
						(move.list as List<ZSpell>).forEach { spell ->
							addClickable(c.getRect(), ZMove(move, spell, c.type))
						}
					}
				}
				ZMoveType.BORN_LEADER -> for (c in (move.list as List<ZCharacter>)) {
					addClickable(c.getRect(), ZMove(move, c.type, c.type))
				}
				ZMoveType.BLOODLUST_MELEE -> for (w in cur.meleeWeapons) {
					for (zoneIdx in (move.list as List<Int>)) {
						addClickable(board.getZone(zoneIdx), ZMove(move, zoneIdx, w))
					}
				}
				ZMoveType.BLOODLUST_RANGED -> for (w in cur.rangedWeapons) {
					for (zoneIdx in (move.list as List<Int>)) {
						addClickable(board.getZone(zoneIdx), ZMove(move, zoneIdx, w))
					}
				}
				ZMoveType.BLOODLUST_MAGIC -> for (w in cur.magicWeapons) {
					for (zoneIdx in (move.list as List<Int>)) {
						addClickable(board.getZone(zoneIdx), ZMove(move, zoneIdx, w))
					}
				}
				else                                                                                                                       -> log.warn("Unhandled case: %s", move.type)
			}
		}
	}

	@Synchronized
	fun drawAnimations(anims: MutableList<ZAnimation>, g: AGraphics) {
		synchronized(anims) {
			val it = anims.iterator()
			while (it.hasNext()) {
				val a = it.next()
				if (a.isDone) {
					it.remove()
				} else {
					if (!a.isStarted) a.start<AAnimation<AGraphics>>()
					a.update(g)
				}
			}
		}
	}

	fun drawActors(g: AGraphics, game: UIZombicide, mx: Float, my: Float): ZActor<*>? {
		var picked: ZActor<*>? = null
		var distFromCenter = 0f
		actorsAnimating = false
		val options: Set<ZActor<*>> = when (game.uiMode) {
			UIMode.PICK_ZOMBIE,
			UIMode.PICK_CHARACTER -> game.options.toSet() as Set<ZActor<*>>
			else -> emptySet()
		}

		fun drawActor(a : ZActor<*>) {
			val img = g.getImage(a.imageId)
			if (img != null) {
				val rect = a.getRect(board)
				// draw box under character of the color of the user who is owner
				if (rect.contains(mx, my)) {
					val dist = rect.center.sub(mx, my).magSquared()
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
					actorsAnimating = true
				} else {
					var outline: GColor? = null
					game.currentCharacter?.let {
						if (a is ZCharacter && a.type === it) {
							outline = GColor.GREEN
						} else if (a === picked)
							outline = GColor.CYAN
						else if (options.contains(a))
							outline = GColor.YELLOW
						else if (a.isAlive)
							outline = GColor.WHITE
					}
					drawActor(g as T, a, outline)
				}
				g.removeFilter()
			}
		}

		board.getAllActors().filter { !it.isAnimating }.forEach {
			drawActor(it)
		}
		board.getAllActors().filter { it.isAnimating }.forEach {
			drawActor(it)
		}
		return picked
	}

	protected open fun drawActor(g: T, actor: ZActor<*>, outline: GColor?) {
		if (outline != null) {
			if (actor.outlineImageId > 0) {
				g.pushMatrix()
				g.setTintFilter(GColor.WHITE, outline)
				g.drawImage(actor.outlineImageId, actor.getRect())
				g.removeFilter()
				g.popMatrix()
			} else {
				g.color = outline
				g.drawRect(actor.getRect(), 1f)
			}
		}
		actor.draw(g)
	}

	fun pickZone(g: AGraphics, mouseX: Int, mouseY: Int): Int {
		for (cell in board.getCells()) {
			if (cell.contains(mouseX.toFloat(), mouseY.toFloat())) {
				return cell.zoneIndex
			}
		}
		return -1
	}

	fun pickDoor(g: AGraphics, doors: List<ZDoor?>, mouseX: Float, mouseY: Float): ZDoor? {
		var picked: ZDoor? = null
		for (door in doors) {
			val doorRect = door!!.getRect(board).grow(.1f)
			if (doorRect.contains(mouseX, mouseY)) {
				g.color = GColor.RED
				picked = door
				// highlight the other side if this is a vault
				g.drawRect(door.otherSide.getRect(board).grow(.1f), 2f)
			} else {
				g.color = GColor.YELLOW
			}
			g.drawRect(doorRect, 2f)
		}
		return picked
	}

	fun pickSpawn(g: AGraphics, areas: List<ZSpawnArea?>, mouseX: Float, mouseY: Float): ZSpawnArea? {
		var picked: ZSpawnArea? = null
		for (area in areas) {
			val areaRect = area!!.rect.grownBy(.1f)
			if (areaRect.contains(mouseX, mouseY)) {
				g.color = GColor.RED
				picked = area
			} else {
				g.color = GColor.YELLOW
			}
			g.drawRect(areaRect, 2f)
		}
		return picked
	}

	fun drawZoneOutline(g: AGraphics, board: ZBoard, zoneIndex: Int) {
		val zone = board.getZone(zoneIndex)
		for (cellPos in zone.getCells()) {
			g.drawRect(board.getCell(cellPos), 2f)
		}
	}

	fun drawZoneFilled(g: AGraphics, board: ZBoard, zoneIndex: Int) {
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
		val maxNoise = board.getMaxNoiseLevelZone()
		if (maxNoise != null) {
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
		g.drawLine(redX.topLeft, redX.bottomRight, lineThickness.toFloat())
		g.drawLine(redX.topRight, redX.bottomLeft, lineThickness.toFloat())
	}

	/**
	 * return zone highlighted by mouseX, mouseY
	 *
	 * @param g
	 * @param mouseX
	 * @param mouseY
	 * @return
	 */
	fun drawZones(g: AGraphics, mouseX: Float, mouseY: Float, miniMap: Boolean): Int {
		var result = -1
		for (i in 0 until board.getNumZones()) {
			val zone = board.getZone(i)
			for (pos in zone.getCells()) {
				val cell = board.getCell(pos)
				if (cell.isCellTypeEmpty) continue
				when (zone.type) {
					ZZoneType.TOWER -> if (miniMap || drawTowersHighlighted) {
						g.color = GColor.SKY_BLUE.withAlpha((cell.scale - 1) * 3)
						g.drawFilledRect(cell)
					}
				}
				try {
					drawCellWalls(g, pos, 1f, miniMap)
				} catch (e: Exception) {
					log.error("Problem draw cell walls pos: $pos")
					throw e
				}
				if (cell.contains(mouseX, mouseY)) {
					result = i
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
										quest?.drawBlackObjective(game, g, cell, zone)
									}
								}
							}
							ZCellType.OBJECTIVE_BLUE, ZCellType.OBJECTIVE_GREEN, ZCellType.OBJECTIVE_RED -> if (zone.isObjective) {
								drawObjectiveX(g, type.color, lineThickness, cell)
							}
							ZCellType.EXIT -> text += "EXIT"
						}
					}
				}
				for (area in cell.spawnAreas) {
					drawSpawn(g, cell, area)
				}
				if (zone.isDragonBile) {
					if (miniMap) {
						g.color = GColor.SLIME_GREEN
						g.drawFilledRect(cell)
					} else g.drawImage(ZIcon.SLIME.imageIds[0], cell)
				}
				if (!miniMap && text.length > 0) {
					g.color = GColor.YELLOW
					g.drawJustifiedStringOnBackground(cell.center, Justify.CENTER, Justify.CENTER, text, GColor.TRANSLUSCENT_BLACK, 10f, 2f)
				}
				if (!miniMap && game.uiMode == UIMode.PICK_ZONE && !game.options.contains(i)) {
					g.color = GColor.TRANSLUSCENT_BLACK
					g.drawFilledRect(cell)
				}
			}
		}
		return result
	}

	fun drawSpawn(g: AGraphics, rect: IRectangle, area: ZSpawnArea) {
		val dir = area.dir
		val id = area.icon.imageIds[dir.ordinal]
		val img = g.getImage(id)
		area.rect = GRectangle(rect).scale(.8f).fit(img, dir.horz, dir.vert)
		g.drawImage(id, area.rect)
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
	}

	fun drawCellWalls(g: AGraphics, cellPos: Grid.Pos?, scale: Float, miniMap: Boolean) {
		val cell = board.getCell(cellPos!!)
		g.pushMatrix()
		val center: Vector2D = cell.center
		g.translate(center)
		val v0: Vector2D = cell.topLeft.subEq(center)
		val v1: Vector2D = cell.topRight.subEq(center)
		g.scale(scale)
		val doorColor = GColor.ORANGE
		for (dir in compassValues) {
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
					val door = board.findDoor(cellPos, dir)
					g.color = GColor.BLACK
					val vaultRect = door.getRect(board)
					g.color = door.lockedColor
					vaultRect.drawFilled(g)
					g.color = GColor.YELLOW
					vaultRect.drawOutlined(g, vaultLineThickness)
				}
				ZWallFlag.OPEN -> {
					val door = board.findDoor(cellPos, dir)
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
		}
	}

	fun drawMiniMap(g: AGraphics) {
		if (miniMapMode == 0) return
		g.pushMatrix()
		g.ortho()
		val xscale = 0.25f * (width / board.columns)
		val yscale = 0.5f * (height / board.rows)
		val padding = 10f
		when (miniMapMode) {
			1 -> {
				g.translate(padding, padding)
				g.scale(Math.min(xscale, yscale))
			}
			2 -> {
				g.translate(width - padding, padding)
				g.scale(Math.min(xscale, yscale))
				g.translate(-board.columns.toFloat(), 0f)
			}
			3 -> {
				g.translate(padding, height - padding)
				g.scale(Math.min(xscale, yscale))
				g.translate(0f, -board.rows.toFloat())
			}
			4 -> {
				g.translate(width - padding, height - padding)
				g.scale(Math.min(xscale, yscale))
				g.translate(0f, -board.rows.toFloat())
				g.translate(-board.columns.toFloat(), 0f)
			}
		}
		g.setTransparencyFilter(.6f)
		g.color = GColor.TRANSLUSCENT_BLACK
		g.drawFilledRect(0f, 0f, board.columns.toFloat(), board.rows.toFloat())
		drawZones(g, -1f, -1f, true)
		// draw the actors
		for (a in board.getAllActors()) {
			if (a is ZCharacter) {
				g.color = a.color
			} else if (a is ZZombie) {
				g.color = GColor.LIGHT_GRAY
			}
			val rect = a.getRect(board)
			g.drawFilledOval(rect.scale(.8f))
		}
		g.removeFilter()
		g.popMatrix()
	}

	fun pickCell(g: AGraphics, mouseX: Float, mouseY: Float): Grid.Pos? {
		val it = board.getCellsIterator()
		while (it.hasNext()) {
			val cell = it.next()
			if (cell.isCellTypeEmpty) continue
			if (cell.contains(mouseX, mouseY)) {
				return it.pos
			}
		}
		return null
	}

	fun drawDebugText(g: AGraphics, mouseX: Float, mouseY: Float) {
		val it = board.getCellsIterator()
		while (it.hasNext()) {
			val cell = it.next()
			if (cell.isCellTypeEmpty) continue
			var text = "Zone " + cell.zoneIndex
			for (type in ZCellType.values()) {
				if (cell.isCellType(type)) {
					when (type) {
						ZCellType.NONE, ZCellType.VAULT_DOOR_VIOLET, ZCellType.VAULT_DOOR_GOLD -> {
						}
						ZCellType.OBJECTIVE_RED, ZCellType.OBJECTIVE_BLUE, ZCellType.OBJECTIVE_GREEN, ZCellType.OBJECTIVE_BLACK -> text += """

 	${type.name.substring(10)}
 	""".trimIndent()
						else                                                                                                    -> text += """

 	$type
 	""".trimIndent()
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
			for (a in cell.occupant) {
				text += "/n/n${a.name()}".trimIndent()
			}
			if (cell.vaultId > 0) {
				text += "/n/nvaultFlag ${cell.vaultId}".trimIndent()
			}
			g.drawJustifiedStringOnBackground(cell.center, Justify.CENTER, Justify.CENTER, text, GColor.TRANSLUSCENT_BLACK, 10f, 3f)
		}
	}

	fun drawNoTiles(g: AGraphics, mouseX: Float, mouseY: Float): Grid.Pos? {
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

	fun pickButtons(g: AGraphics, pos: Vector2D, moves: List<IButton>?, mouseX: Int, mouseY: Int): IButton? {
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
		var buttonWidth = 0f
		val padding = g.textHeight / 2
		for (m in moves) {
			buttonWidth = Math.max(0f, g.getTextWidth(m.label))
		}
		buttonWidth += padding * 2
		if (top.x + buttonWidth > g.viewportWidth) {
			top.x = g.viewportWidth - buttonWidth
		}
		val button = GRectangle(top, buttonWidth, buttonHeight)
		for (m in moves) {
			if (button.contains(mouseX.toFloat(), mouseY.toFloat())) {
				g.color = GColor.RED
				move = m
			} else {
				g.color = GColor.YELLOW
			}
			button.drawRounded(g, buttonHeight / 4)
			g.drawJustifiedString(button.x + padding, button.y + buttonHeight / 2, Justify.LEFT, Justify.CENTER, m.label)
			button.y += buttonHeight
		}
		return move
	}

	fun pickMove(g: APGraphics, mouse: IVector2D, screenMouseX: Int, screenMouseY: Int): IButton? {
		var picked: IButton? = null
		if (submenu != null) {
			highlightedMoves = submenu
		} else {
			for ((shape, value) in clickables) {
				if (shape.contains(mouse.x, mouse.y)) {
					highlightedShape = shape
					highlightedMoves = value
				} else {
					g.color = GColor.YELLOW.withAlpha(32)
					shape.drawFilled(g)
				}
			}
		}
		if (highlightedShape != null) {
			g.color = GColor.RED
			g.setLineWidth(2f)
			highlightedShape!!.drawOutlined(g)
			val cntr = highlightedShape!!.center
			g.transform(cntr)
			g.pushMatrix()
			g.setIdentity()
			g.ortho()
			picked = pickButtons(g, cntr, highlightedMoves, screenMouseX, screenMouseY)
			g.popMatrix()
		}
		return picked
	}

	//game.getBoard().getZone(game.getCurrentCharacter().getOccupiedZone()).getCenter();
	val boardCenter: IVector2D
		get() {
			game.currentCharacter?.let {
				return it.character.getRect().center
			}
			val rect = GRectangle()
			board.getAllCharacters().takeIf { it.isNotEmpty() }?.forEach { c ->
				rect.addEq(c.getRect())
			}
			return rect.center
		}

	var tiles: Array<ZTile> = arrayOf()
	var tileIds = IntArray(0)
	fun clearTiles() {
		tiles = arrayOf()
		tileIds = IntArray(0)
	}

	fun onTilesLoaded(ids: IntArray) {
		tileIds = ids
	}

	open fun onLoading() {}
	open fun onLoaded() {}
	var zoomPercent = 0f

	/**
	 *
	 * @param targetZoomPercent 0 is fully zoomed out and 1 is fully zoomed in
	 */
	@Synchronized
	fun animateZoomTo(targetZoomPercent: Float) {
		clearDragOffset()
		targetZoomPercent.coerceIn(0f, 1f).let {
			if (zoomPercent != it) {
				zoomAnimation = ZoomAnimation(boardCenter, this, it).start()
				redraw()
			}
		}
	}

	lateinit var zoomedRect: GRectangle
		private set

	fun getZoomedRectangle(g: AGraphics, center: IVector2D?): GRectangle {
		log.verbose("getZoomedRectangle cntr: %s", center)
		val dim = board.getDimension()
		val aspect = dim.aspect
		val zoomed: GDimension
		val viewport = g.viewport
		// produce a value between 0-1 where
		//   0 means zoomAmt is 0 and
		//   1 is zoomAmt results in a rect with min side of 3
		val zoomAmtMin = Math.min(dim.getWidth(), dim.getHeight())
		val zoomAmtMax = 3f
		val zoomAmt = (zoomAmtMin - zoomAmtMax) * zoomPercent
		val newW = dim.width - zoomAmt * aspect
		val newH = dim.height - zoomAmt
		val vAspect = viewport.aspect
		zoomed = if (vAspect > aspect) {
			GDimension(newW, newH * aspect / vAspect)
		} else {
			GDimension(newW * vAspect / aspect, newH)
		}
		val rect = GRectangle(zoomed).withCenter(center)
		/*
        if (rect.x >= 0) {
            rect.x = 0;
        } else if (rect.x + rect.w < dim.width) {
            rect.x = dim.width - rect.w;
        }
        if (rect.y >= 0) {
            rect.y = 0;
        } else if (rect.y + rect.h < dim.height) {
            rect.y = dim.height - rect.h;
        }*/rect.x = Utils.clamp(rect.x, 0f, dim.width - rect.w)
		rect.y = Utils.clamp(rect.y, 0f, dim.height - rect.h)
		return rect.also { zoomedRect = it }
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
		//g.setColor(GColor.RED);
		//rect.drawOutlined(g, 5);
		val img = g.getImage(ZIcon.GRAVESTONE.imageIds[0])
		g.drawImage(ZIcon.GRAVESTONE.imageIds[0], rect.fit(img))
	}

	override fun draw(g: APGraphics, mouseX: Int, mouseY: Int) {
		//log.debug("mouseX=" + mouseX + " mouseY=" + mouseY);
		if (!UIZombicide.initialized) {
			drawNoBoard(g)
			return
		}
		highlightedActor = null
		highlightedCell = null
		highlightedResult = null
		highlightedDoor = null
		g.setIdentity()
		val center = boardCenter
		val rect = getZoomedRectangle(g, center)
		val dragViewport = dragOffset.scaledBy(-rect.width / g.viewportWidth, -rect.height / g.viewportHeight)

		//log.debug("dragViewport = " + dragViewport);// + " topL = " + topL + "  bottomR = "+ bottomR);
		val boardRect: IDimension = board.getDimension()
		rect.moveBy(dragViewport)
		if (rect.x < 0) {
			rect.x = 0f
		} else if (rect.x + rect.w > boardRect.width) {
			rect.x = boardRect.width - rect.w
		}
		if (rect.y < 0) {
			rect.y = 0f
		} else if (rect.y + rect.h > boardRect.height) {
			rect.y = boardRect.height - rect.h
		}

		//log.debug("Rect = " + rect);
		g.ortho(rect)
		val mouse = g.screenToViewport(mouseX, mouseY)
		if (drawTiles && tiles.isEmpty() && quest?.tiles?.size != 0) {
			tiles = quest!!.tiles
			onLoading()
			(getComponent<UIComponent>() as UIZComponent<AGraphics>).loadTiles(g, tiles )
			return
		}
		val cellPos = drawNoTiles(g, mouseX.toFloat(), mouseY.toFloat())
		if (drawTiles) {
			for (i in tileIds.indices) {
				g.drawImage(tileIds[i], tiles[i].quadrant)
			}
		}
		val highlightedZone = drawZones(g, mouse.X(), mouse.Y(), false)
		quest!!.drawQuest(game, g)
		drawNoise(g)
		drawAnimations(preActor, g)
		highlightedActor = drawActors(g, game, mouse.X(), mouse.Y())
		if (drawDebugText) drawDebugText(g, mouseX.toFloat(), mouseY.toFloat())
		if (drawZombiePaths) {
			highlightedActor?.let {
				if (it is ZZombie) {
					val path = when (it.type) {
						ZZombieType.Necromancer -> game.getZombiePathTowardNearestSpawn(it)
						else                    -> game.getZombiePathTowardVisibleCharactersOrLoudestZone(it)
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
					val visibleZones = board.getAccessableZones(it.occupiedZone, 0, 4, ZActionType.RANGED)
					for (zoneIdx in visibleZones) {
						val zone = board.getZone(zoneIdx)
						g.color = GColor.YELLOW
						zone.rectangle.drawOutlined(g, 2)
					}
				}
			}
		}
		if (drawRangedAccessibility && highlightedZone >= 0) {
			g.color = GColor.BLUE.withAlpha(.5f)
			for (zIndex in board.getAccessableZones(highlightedZone, 1, 5, ZActionType.RANGED)) {
				drawZoneFilled(g, board, zIndex)
			}
		}
		drawAnimations(postActor, g)
		drawQuestLabel(g)
		when (game.uiMode) {
			UIMode.PICK_ZOMBIE, UIMode.PICK_CHARACTER -> {

				//g.setColor(GColor.YELLOW);
				//for (ZActor a : (List<ZActor>)game.getOptions()) {
				//    a.getRect(getBoard()).drawOutlined(g, 1);
				//}
				if (game.options.contains(highlightedActor)) {
					highlightedResult = highlightedActor
				}
			}
			UIMode.PICK_ZONE -> {
				if (highlightedZone >= 0 && game.options.contains(highlightedZone)) {
					highlightedResult = highlightedZone
					g.color = GColor.YELLOW
					drawZoneOutline(g, board, highlightedZone)
				} else if (cellPos != null) {
					val cell = board.getCell(cellPos)
					var i = 0
					while (i < game.options.size) {
						if (cell.zoneIndex == game.options[i] as Int) {
							highlightedCell = cellPos
							highlightedResult = cell.zoneIndex
							break
						}
						i++
					}
				}
			}
			UIMode.PICK_DOOR -> {
				highlightedResult = pickDoor(g, game.options as List<ZDoor>, mouse.X(), mouse.Y())
			}
			UIMode.PICK_MENU -> {
				highlightedResult = pickMove(g, mouse, mouseX, mouseY)
			}
			UIMode.PICK_SPAWN -> {
				highlightedResult = pickSpawn(g, game.options as List<ZSpawnArea>, mouse.X(), mouse.Y())
			}
		}
		highlightedCell?.let {
			val cell = board.getCell(it)
			g.color = GColor.RED.withAlpha(32)
			drawZoneOutline(g, board, cell.zoneIndex)
		}
		g.pushMatrix()
		g.setIdentity()
		g.ortho()
		g.color = GColor.WHITE
		g.drawJustifiedStringOnBackground(10f, height - 10, Justify.LEFT, Justify.BOTTOM, game.boardMessage, GColor.TRANSLUSCENT_BLACK, borderThickness)
		drawAnimations(overlayAnimations, g)
		drawOverlay(g)
		g.popMatrix()
		game.characterRenderer.redraw()
		if (zoomAnimation?.isDone == true) {
			zoomAnimation = null
		} else {
			zoomAnimation?.update(g)
		}
		if (isAnimating)
			redraw()
		else {
			drawMiniMap(g)
			lock.withLock {
				condition.signal()
			}
//			synchronized(this) { notify() }
		}
	}

	val lock = ReentrantLock()
	val condition = lock.newCondition()

	fun waitForAnimations() {
		if (isAnimating || null != board.getAllActors().firstOrNull { it.isAnimating }) {
			lock.withLock {
				condition.await(20, TimeUnit.SECONDS)
			}
		//Utils.waitNoThrow(this, 20000)
		}
	}

	protected fun drawOverlay(g: AGraphics) {
		drawOverlayObject(overlayToDraw, g)
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
		//        g.setColor(GColor.YELLOW);
//        IVector2D cntr = new Vector2D(getWidth()/2, getHeight()/2);
//        Table t = (Table)obj;
//        t.draw(g, cntr, Justify.CENTER, Justify.CENTER);
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
		if (obj == null) {
			overlayToDraw = null
		} else if (obj is Table) {
			overlayToDraw = obj
		} else if (obj is ZPlayerName) {
			overlayToDraw = obj.cardImageId
		} else if (obj is AImage) {
			overlayToDraw = obj
		} else if (obj is ZAnimation) {
			addOverlay(obj)
		} else {
			overlayToDraw = obj.toString()
		}
		redraw()
	}

	override fun onClick() {
		overlayToDraw = null
		submenu = null
		if (game.isGameRunning()) {
			game.setResult(highlightedResult)
		} else {
			highlightedDoor?.toggle(board)?:run {
				highlightedCell?.let {
					selectedCell = if (highlightedCell == selectedCell) {
						null
					} else {
						highlightedCell
					}
				}
			}
		}
		redraw()
	}

	fun toggleDrawTiles(): Boolean {
		return !drawTiles.also { drawTiles = it }
	}

	fun toggleDrawDebugText(): Boolean {
		return !drawDebugText.also { drawDebugText = it }
	}

	fun toggleDrawTowersHighlighted(): Boolean {
		return !drawTowersHighlighted.also { drawTowersHighlighted = it }
	}

	fun toggleDrawZoombiePaths(): Boolean {
		return !drawZombiePaths.also { drawZombiePaths = it }
	}

	fun toggleDrawRangedAccessibility(): Boolean {
		return !drawRangedAccessibility.also { drawRangedAccessibility = it }
	}

	fun toggleDrawMinimap(): Int {
		return try {
			(miniMapMode + 1) % 5.also { miniMapMode = it }
		} finally {
			redraw()
		}
	}

	override fun onDragStart(x: Float, y: Float) {
		dragStart = Vector2D(x, y)
	}

	override fun onDragEnd() {
		//dragOffset = Vector2D.ZERO;
		redraw()
	}

	override fun onDragMove(x: Float, y: Float) {
		val v = Vector2D(x, y)
		val dv: Vector2D = v.sub(dragStart)
		dragOffset.addEq(dv)
		dragStart = v
		redraw()
	}

	fun scroll(dx: Float, dy: Float) {
		dragOffset.addEq(dx, dy)
		redraw()
	}

	val numOverlayTextAnimations: Int
		get() = Utils.count(overlayAnimations) { a: ZAnimation? -> a is OverlayTextAnimation }

	fun clearDragOffset() {
		dragOffset.zero()
	}

	companion object {
		const val ENABLE_ENHANCED_UI = false
		val log = LoggerFactory.getLogger(UIZBoardRenderer::class.java)
	}
}