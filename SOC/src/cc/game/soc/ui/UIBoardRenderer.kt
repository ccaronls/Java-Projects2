package cc.game.soc.ui

import cc.game.soc.core.*
import cc.lib.game.*
import cc.lib.math.CMath
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.ui.UIComponent
import java.util.*

open class UIBoardRenderer(component: UIComponent) : UIRenderer(component) {
	
	lateinit var board: Board
	
	protected fun getPlayerColor(playerNum: Int): GColor {
		return UISOC.instance.getPlayerColor(playerNum)
	}

	private var desertImage = 0
	private var waterImage = 0
	private var goldImage = 0
	private var undiscoveredImage = 0
	private var foresthexImage = 0
	private var hillshexImage = 0
	private var mountainshexImage = 0
	private var pastureshexImage = 0
	private var fieldshexImage = 0
	private lateinit var knightImages: IntArray
	private val outlineColorDark = GColor.BLACK
	private val outlineColorLight = GColor.WHITE
	private val textColor = GColor.CYAN
	private var pickMode = PickMode.PM_NONE
	private var pickedValue = -1
	private var pickHandler: PickHandler? = null
	var renderFlag = 0
	private val animations = LinkedList<AAnimation<AGraphics>>()
	private var edgeInfoIndex = -1
	private var cellInfoIndex = -1
	private var vertexInfoIndex = -1
	fun initImages(desertImage: Int, waterImage: Int, goldImage: Int, undiscoveredImage: Int, foresthexImage: Int, hillshexImage: Int, mountainshexImage: Int, pastureshexImage: Int, fieldshexImage: Int,
	               knightBasicInactive: Int, knightBasicActive: Int, knightStrongInactive: Int, knightStrongActive: Int, knightMightlyInactive: Int, knightMightlyActive: Int) {
		this.desertImage = desertImage
		this.waterImage = waterImage
		this.goldImage = goldImage
		this.undiscoveredImage = undiscoveredImage
		this.foresthexImage = foresthexImage
		this.hillshexImage = hillshexImage
		this.mountainshexImage = mountainshexImage
		this.pastureshexImage = pastureshexImage
		this.fieldshexImage = fieldshexImage
		knightImages = intArrayOf(
			knightBasicInactive,
			knightBasicActive,
			knightStrongInactive,
			knightStrongActive,
			knightMightlyInactive,
			knightMightlyActive
		)
	}

	fun getPickHandler(): PickHandler? {
		return pickHandler
	}

	fun setRenderFlag(flag: RenderFlag, enabled: Boolean) {
		renderFlag = if (enabled) renderFlag or (1 shl flag.ordinal) else renderFlag and (1 shl flag.ordinal).inv()
		//getProperties().setProperty("renderFlag", renderFlag);
		getComponent<UIComponent>().redraw()
	}

	fun getRenderFlag(flag: RenderFlag): Boolean {
		return renderFlag and (1 shl flag.ordinal) != 0
	}

	fun addAnimation(anim: UIAnimation, block: Boolean) {
		addAnimation(false, anim, block)
	}

	fun addAnimation(front: Boolean, anim: UIAnimation, block: Boolean) {
		synchronized(animations) { if (front) animations.addFirst(anim) else animations.addLast(anim) }
		getComponent<UIComponent>().redraw()
		if (!anim.isStarted)
			anim.start<UIAnimation>()
		if (block) {
			anim.block(anim.duration + 500)
		}
	}

	//fun getBoard(): Board? {
	//	return if (UISOC.getInstance() == null) board else UISOC.getInstance().board
	//}

	@JvmOverloads
	fun drawTileOutline(g: AGraphics, cell: Tile, thickness: Float = RenderConstants.thinLineThickness) {
		g.begin()
		for (i in cell.adjVerts) {
			val v = board.getVertex(i)
			g.vertex(v.x, v.y)
		}
		g.drawLineLoop(thickness)
	}

	fun startTilesInventedAnimation(tile0: Tile, tile1: Tile) {
		val NUM_PTS = 30
		val t1 = tile1.dieNum
		val t0 = tile0.dieNum
		val pts0 = arrayOfNulls<Vector2D>(NUM_PTS + 1)
		val pts1 = arrayOfNulls<Vector2D>(NUM_PTS + 1)
		val mid: Vector2D = Vector2D.newTemp(tile0).add(tile1).scaleEq(0.5f)
		val d: Vector2D = Vector2D.newTemp(mid).sub(tile0).scaleEq(0.5f)
		val mid0: Vector2D = Vector2D.newTemp(tile0).add(d)
		val mid1: Vector2D = Vector2D.newTemp(tile1).sub(d)
		val dv = d.norm().scaleEq(0.7f)
		Utils.computeBezierCurvePoints(pts0, NUM_PTS, tile0, mid0.add(dv), mid1.add(dv), tile1)
		pts0[NUM_PTS] = pts0[NUM_PTS - 1]
		Utils.computeBezierCurvePoints(pts1, NUM_PTS, tile1, mid1.sub(dv), mid0.sub(dv), tile0)
		pts1[NUM_PTS] = pts1[NUM_PTS - 1]
		tile0.dieNum = 0
		tile1.dieNum = 0
		addAnimation(object : UIAnimation(3000) {
			fun drawChit(g: AGraphics, pts: Array<Vector2D?>, position: Float, num: Int) {
				val index0 = (position * (NUM_PTS - 2)).toInt()
				val index1 = index0 + 1
				val delta = position * (NUM_PTS - 2) - index0
				assert(position in 0.0..1.0)
				val pos: Vector2D = pts[index0]!!.scaledBy(1.0f - delta).add(pts[index1]!!.scaledBy(delta))
				drawCellProductionValue(g, pos.x, pos.y, num)
			}

			public override fun draw(g: AGraphics, position: Float, dt: Float) {
				drawChit(g, pts0, position, t1)
				drawChit(g, pts1, position, t0)
			}
		}, true)
		tile0.dieNum = t0
		tile1.dieNum = t1
	}

	enum class FaceType {
		SETTLEMENT,
		CITY,
		CITY_WALL,
		PIRATE_FORTRESS,
		SHIP,
		WAR_SHIP,
		KNIGHT_ACTIVE_BASIC,
		KNIGHT_ACTIVE_STRONG,
		KNIGHT_ACTIVE_MIGHTY,
		KNIGHT_INACTIVE_BASIC,
		KNIGHT_INACTIVE_STRONG,
		KNIGHT_INACTIVE_MIGHTY,
		METRO_TRADE,
		METRO_POLITICS,
		METRO_SCIENCE,
		MERCHANT,
		ROBBER
	}

	private class Face internal constructor(added: GColor, val darkenAmount: Float, vararg verts: Int) {
		val numVerts: Int
		val xVerts: IntArray
		val yVerts: IntArray
		val added: GColor
		lateinit var structures: Array<FaceType>

		internal constructor(darkenAmount: Float, vararg verts: Int) : this(GColor.TRANSPARENT, darkenAmount, *verts) {}

		fun setFaceTypes(vararg structures: FaceType): Face {
			this.structures = arrayOf(*structures)
			return this
		}

		init {
			numVerts = verts.size / 2
			xVerts = IntArray(numVerts)
			yVerts = IntArray(numVerts)
			this.added = added
			var index = 0
			var i = 0
			while (i < numVerts * 2) {
				xVerts[index] = verts[i]
				yVerts[index] = verts[i + 1]
				index++
				i += 2
			}
		}
	}

	val structureRadius: Float
		get() = board.tileWidth / (6 * 3)

	fun drawSettlement(g: AGraphics, pos: IVector2D?, playerNum: Int, outline: Boolean) {
		if (playerNum > 0) g.color = getPlayerColor(playerNum)
		drawFaces(g, pos, 0f, structureRadius, FaceType.SETTLEMENT, outline)
	}

	fun drawCity(g: AGraphics, pos: IVector2D?, playerNum: Int, outline: Boolean) {
		if (playerNum > 0) g.color = getPlayerColor(playerNum)
		drawFaces(g, pos, 0f, structureRadius, FaceType.CITY, outline)
	}

	fun drawWalledCity(g: AGraphics, pos: IVector2D?, playerNum: Int, outline: Boolean) {
		if (playerNum > 0) g.color = getPlayerColor(playerNum)
		drawFaces(g, pos, 0f, structureRadius, FaceType.CITY_WALL, outline)
	}

	fun drawMetropolisTrade(g: AGraphics, pos: IVector2D?, playerNum: Int, outline: Boolean) {
		if (playerNum > 0) g.color = getPlayerColor(playerNum)
		drawFaces(g, pos, 0f, structureRadius, FaceType.METRO_TRADE, outline)
	}

	fun drawMetropolisPolitics(g: AGraphics, pos: IVector2D?, playerNum: Int, outline: Boolean) {
		if (playerNum > 0) g.color = getPlayerColor(playerNum)
		drawFaces(g, pos, 0f, structureRadius, FaceType.METRO_POLITICS, outline)
	}

	fun drawMetropolisScience(g: AGraphics, pos: IVector2D?, playerNum: Int, outline: Boolean) {
		if (playerNum > 0) g.color = getPlayerColor(playerNum)
		drawFaces(g, pos, 0f, structureRadius, FaceType.METRO_SCIENCE, outline)
	}

	fun drawMerchant(g: AGraphics, t: Tile, playerNum: Int) {
		if (playerNum > 0) g.color = getPlayerColor(playerNum)
		drawFaces(g, t, 0f, structureRadius, FaceType.MERCHANT, false)
		g.color = GColor.WHITE
		val txt = """

	       	2:1
	       	${t.resource.name}
	       	""".trimIndent()
		val v: Vector2D = g.transform(t)
		g.drawJustifiedString((v.Xi() - 2).toFloat(), v.Yi() - 2 - g.textHeight * 2, Justify.CENTER, Justify.TOP, txt)
	}

	fun drawKnight_image(g: AGraphics, _x: Float, _y: Float, playerNum: Int, level: Int, active: Boolean, outline: Boolean) {
		val x = Math.round(_x)
		val y = Math.round(_y)
		val r = (board.tileWidth / 8).toInt() + 1
		val r2 = r + 3
		val index = level * (if (active) 2 else 1) - 1
		g.drawOval((x - r2 / 2).toFloat(), (y - r2 / 2).toFloat(), r2.toFloat(), r2.toFloat())
		g.drawImage(knightImages[index], (x - r / 2).toFloat(), (y - r / 2).toFloat(), r.toFloat(), r.toFloat())
	}

	val knightRadius: Float
		get() = board.tileWidth * 2 / (8 * 3 * 3)

	fun drawKnight(g: AGraphics, pos: IVector2D?, playerNum: Int, level: Int, active: Boolean, outline: Boolean) {
		if (playerNum > 0) g.color = getPlayerColor(playerNum)
		var structure: FaceType? = null
		when (level) {
			0, 1 -> structure = if (active) FaceType.KNIGHT_ACTIVE_BASIC else FaceType.KNIGHT_INACTIVE_BASIC
			2 -> structure = if (active) FaceType.KNIGHT_ACTIVE_STRONG else FaceType.KNIGHT_INACTIVE_STRONG
			3 -> structure = if (active) FaceType.KNIGHT_ACTIVE_MIGHTY else FaceType.KNIGHT_INACTIVE_MIGHTY
			else -> assert(false)
		}
		val radius = knightRadius
		drawFaces(g, pos, 0f, radius, structure, outline)
	}

	fun drawCircle(g: AGraphics, pos: IVector2D?) {
		g.pushMatrix()
		g.begin()
		g.translate(pos)
		var angle = 0
		val rad = board.tileWidth / 5
		g.scale(rad, rad)
		val pts = 10
		for (i in 0 until pts) {
			g.vertex(CMath.cosine(angle.toFloat()), CMath.sine(angle.toFloat()))
			angle += 360 / pts
		}
		g.drawLineLoop(2f)
		g.popMatrix()
	}

	fun drawPirateFortress(g: AGraphics, v: Vertex, outline: Boolean) {
		val mv = g.transform(v)
		var x = mv.Xi() - 10
		var y = mv.Yi() - 10
		for (i in 0 until v.pirateHealth) {
			val rw = 40
			val rh = 30
			g.color = GColor.GRAY
			g.drawFilledOval(x.toFloat(), y.toFloat(), rw.toFloat(), rh.toFloat())
			g.color = GColor.RED
			g.drawOval(x.toFloat(), y.toFloat(), rw.toFloat(), rh.toFloat())
			x += 10
			y += 5
		}
		g.color = GColor.GRAY
		drawFaces(g, v, 0f, structureRadius, FaceType.PIRATE_FORTRESS, outline)
	}

	fun drawFaces(g: AGraphics, pos: IVector2D?, angle: Float, radius: Float, structure: FaceType?, outline: Boolean) {
		//final float xRad = 3; // actual radius as defined above
		//float scale = radius / xRad;
		drawFaces(g, pos, angle, radius, radius, structure, outline)
	}

	private val faceMap = HashMap<FaceType?, Array<Face>>()
	private fun getStructureFaces(s: FaceType?): Array<Face> {
		var faces = faceMap[s]
		if (faces == null) {
			val a = ArrayList<Face>()
			for (i in structureFaces.indices) {
				if (structureFaces[i].structures != null) {
					if (Arrays.binarySearch(structureFaces[i].structures, s) >= 0) {
						a.add(structureFaces[i])
					}
				}
			}
			faces = a.toTypedArray()
			faceMap[s] = faces
		}
		return faces
	}

	fun drawFaces(g: AGraphics, pos: IVector2D?, angle: Float, w: Float, h: Float, structure: FaceType?, outline: Boolean) {
		g.pushMatrix()
		g.translate(pos)
		g.rotate(angle)
		g.scale(w, -h)
		val saveColor = g.color
		val faces = getStructureFaces(structure)
		for (face in faces) {
			g.begin()
			for (i in 0 until face.numVerts) g.vertex(face.xVerts[i].toFloat(), face.yVerts[i].toFloat())
			if (outline) {
				g.color = outlineColorDark
				g.drawLineLoop(2f)
			}
			val c = face.added.add(saveColor.darkened(face.darkenAmount))
			g.color = c
			g.drawTriangleFan()
		}
		g.color = saveColor
		g.popMatrix()
	}

	private fun pickEdge(g: APGraphics, mouseX: Int, mouseY: Int): Int {
		g.begin()
		for (index in 0 until board.numRoutes) {
			g.setName(index)
			//renderEdge(g, getBoard().getRoute(index));
			g.vertex(board.getRouteMidpoint(board.getRoute(index)))
		}
		return g.pickClosest(mouseX, mouseY) //g.pickLines(mouseX, mouseY, Math.round(RenderConstants.thickLineThickness*2));
	}

	private fun pickVertex(g: APGraphics, mouseX: Int, mouseY: Int): Int {
		g.begin()
		for (index in 0 until board.numAvailableVerts) {
			g.setName(index)
			val v = board.getVertex(index)
			g.vertex(v)
		}
		return g.pickClosest(mouseX, mouseY) //g.pickPoints(mouseX, mouseY, 10);
	}

	private fun pickTile(g: APGraphics, mouseX: Int, mouseY: Int): Int {
		g.begin()
		//final int dim = Math.round(getBoard().getTileWidth() * getComponent().getWidth());
		for (index in 0 until board.numTiles) {
			g.setName(index)
			val cell = board.getTile(index)
			g.vertex(cell)
		}
		return g.pickClosest(mouseX, mouseY) //g.pickPoints(mouseX, mouseY, dim);
	}

	private fun renderEdge(g: AGraphics, e: Route) {
		val v0 = board.getVertex(e.from)
		val v1 = board.getVertex(e.to)
		g.vertex(v0)
		g.vertex(v1)
	}

	private fun renderDamagedEdge(g: AGraphics, e: Route) {
		val v0 = board.getVertex(e.from)
		val v1 = board.getVertex(e.to)
		// choose v1 or v0 based on which endpoint is touching another route or structure of ours
		var v = v0
		if (v1.player == e.player && v1.isStructure) {
			v = v1
		} else if (v0.player != e.player || !v0.isStructure) {
			for (r in board.getRoutesAdjacentToVertex(e.to)) {
				if (r != e) {
					if (r.player == e.player) {
						v = v1
					}
				}
			}
		}
		g.begin()
		g.vertex(v0)
		val mp = board.getRouteMidpoint(e)
		g.vertex(mp)
		val dv: Vector2D = Vector2D.newTemp(mp).subEq(v0).normEq().addEq(v)
		g.vertex(dv)
	}

	fun drawDamagedRoad(g: AGraphics, e: Route, outline: Boolean) {
		g.begin()
		if (outline) {
			val old = g.color
			g.color = outlineColorDark
			renderDamagedEdge(g, e)
			g.drawLines(RenderConstants.thickLineThickness + 2)
			g.color = old
		}
		renderDamagedEdge(g, e)
		g.drawLineStrip(RenderConstants.thickLineThickness)
	}

	fun drawEdge(g: AGraphics, e: Route, type: RouteType?, playerNum: Int, outline: Boolean) {
		if (playerNum > 0) g.color = getPlayerColor(playerNum)
		when (type) {
			RouteType.OPEN -> {
			}
			RouteType.DAMAGED_ROAD -> drawDamagedRoad(g, e, outline)
			RouteType.ROAD -> drawRoad(g, e, outline)
			RouteType.SHIP -> drawShip(g, e, outline)
			RouteType.WARSHIP -> drawWarShip(g, e, outline)
		}
	}

	fun drawVertex(g: AGraphics, v: Vertex, type: VertexType, playerNum: Int, outline: Boolean) {
		when (type) {
			VertexType.OPEN -> {
			}
			VertexType.PIRATE_FORTRESS -> drawPirateFortress(g, v, outline)
			VertexType.OPEN_SETTLEMENT -> {
				g.color = GColor.LIGHT_GRAY
				drawSettlement(g, v, playerNum, outline)
			}
			VertexType.SETTLEMENT -> drawSettlement(g, v, playerNum, outline)
			VertexType.CITY -> drawCity(g, v, playerNum, outline)
			VertexType.WALLED_CITY -> drawWalledCity(g, v, playerNum, outline)
			VertexType.METROPOLIS_SCIENCE -> drawMetropolisScience(g, v, playerNum, outline)
			VertexType.METROPOLIS_POLITICS -> drawMetropolisPolitics(g, v, playerNum, outline)
			VertexType.METROPOLIS_TRADE -> drawMetropolisTrade(g, v, playerNum, outline)
			VertexType.BASIC_KNIGHT_ACTIVE, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE -> drawKnight(g, v, playerNum, type.knightLevel, type.isKnightActive, outline)
		}
	}

	fun drawRoad(g: AGraphics, e: Route, outline: Boolean) {
		g.begin()
		if (outline) {
			val old = g.color
			g.color = outlineColorDark
			renderEdge(g, e)
			g.drawLines(RenderConstants.thickLineThickness + 2)
			g.color = old
		}
		renderEdge(g, e)
		g.drawLineStrip(RenderConstants.thickLineThickness)
	}

	fun getEdgeAngle(e: Route): Int {
		var v0 = board.getVertex(e.from)
		var v1 = board.getVertex(e.to)
		if (v1.x < v0.x) {
			val t = v0
			v0 = v1
			v1 = t
		}
		// we want eight 60, 300 or 0
		return Math.round(Vector2D.newTemp(v1).sub(v0).angleOf())
	}

	val shipRadius: Float
		get() = board.tileWidth / (8 * 3)
	val robberRadius: Float
		get() = board.tileWidth / (7 * 3)

	fun drawShip(g: AGraphics, e: Route, outline: Boolean) {
		val mp: IVector2D = board.getRouteMidpoint(e)
		drawFaces(g, mp, getEdgeAngle(e).toFloat(), shipRadius, FaceType.SHIP, outline)
	}

	fun drawShip(g: AGraphics, v: IVector2D?, angle: Int, outline: Boolean) {
		drawFaces(g, v, angle.toFloat(), shipRadius, FaceType.SHIP, outline)
	}

	fun drawWarShip(g: AGraphics, v: IVector2D?, angle: Int, outline: Boolean) {
		drawFaces(g, v, angle.toFloat(), shipRadius, FaceType.WAR_SHIP, outline)
	}

	fun drawWarShip(g: AGraphics, e: Route, outline: Boolean) {
		val mp: IVector2D = board.getRouteMidpoint(e)
		drawWarShip(g, mp, getEdgeAngle(e), outline)
	}

	fun drawVessel(g: AGraphics, type: RouteType?, e: Route, outline: Boolean) {
		val mp: IVector2D = board.getRouteMidpoint(e)
		drawVessel(g, type, mp, getEdgeAngle(e), outline)
	}

	fun drawVessel(g: AGraphics, type: RouteType?, v: IVector2D?, angle: Int, outline: Boolean) {
		when (type) {
			RouteType.SHIP -> drawShip(g, v, angle, outline)
			RouteType.WARSHIP -> drawWarShip(g, v, angle, outline)
		}
	}

	@JvmOverloads
	fun drawRobber(g: AGraphics, cell: Tile?, color: GColor? = GColor.LIGHT_GRAY) {
		g.color = color
		drawFaces(g, cell, 0f, robberRadius, FaceType.ROBBER, false)
	}

	@JvmOverloads
	fun drawPirate(g: AGraphics, v: IVector2D?, color: GColor? = GColor.BLACK) {
		g.color = color
		drawFaces(g, v, 0f, robberRadius, FaceType.WAR_SHIP, false)
	}

	/**
	 * Set the current pick mode.
	 *
	 * @param handler
	 */
	fun setPickHandler(handler: PickHandler?) {
		if (handler == null) {
			pickMode = PickMode.PM_NONE
		} else {
			pickMode = handler.pickMode
		}
		pickHandler = handler
		pickedValue = -1
		getComponent<UIComponent>().redraw()
	}

	fun drawIslandOutlined(g: AGraphics, tileIndex: Int) {
		val islandEdges = board.findIslandShoreline(tileIndex)
		g.begin()
		for (eIndex in islandEdges) {
			renderEdge(g, board.getRoute(eIndex))
		}
		g.drawLines(5f)
		val cell = board.getTile(tileIndex)
		if (cell.islandNum > 0) {
			drawIslandInfo(g, board.getIsland(cell.islandNum))
		}
	}

	fun drawIslandInfo(g: AGraphics, i: Island) {
		g.begin()
		g.color = GColor.BLUE
		for (eIndex in i.shoreline) {
			renderEdge(g, board.getRoute(eIndex))
		}
		g.drawLines(5f)
		val midpoint = MutableVector2D()
		var num = 0
		for (eIndex in i.shoreline) {
			midpoint.addEq(board.getRouteMidpoint(board.getRoute(eIndex)))
			num++
		}
		midpoint.scaleEq(1.0f / num)
		g.transform(midpoint)
		val txt = """
	       	ISLAND
	       	${i.num}
	       	""".trimIndent()
		val dim = g.getTextDimension(txt, Float.POSITIVE_INFINITY)
		g.color = GColor(0f, 0f, 0f, 0.5f)
		val x = midpoint.X() - (dim.width / 2 + 5)
		val y = midpoint.Y() - (dim.height / 2 + 5)
		val w = dim.width + 10
		val h = dim.height + 10
		g.drawFilledRect(x, y, w, h)
		g.color = GColor.WHITE
		g.drawJustifiedString(midpoint.Xi().toFloat(), midpoint.Yi().toFloat(), Justify.CENTER, Justify.CENTER, txt)
	}

	private fun drawTilesOutlined(g: AGraphics) {
		//GColor outlineColor = getProperties().getColorProperty("outlineColor", GColor.WHITE);
		//GColor textColor = getProperties().getColorProperty("textcolor", GColor.CYAN);
		val v = floatArrayOf(0f, 0f)
		for (i in 0 until board.numTiles) {
			val cell = board.getTile(i)
			g.transform(cell.x, cell.y, v)
			val x = Math.round(v[0])
			val y = Math.round(v[1])
			g.color = outlineColorLight
			drawTileOutline(g, cell)
			g.color = textColor
			val name = cell.type.getName()
			when (cell.type) {
				TileType.NONE -> {
				}
				TileType.DESERT -> g.drawJustifiedString(x.toFloat(), y.toFloat(), Justify.CENTER, Justify.CENTER, name)
				TileType.WATER -> g.drawJustifiedString(x.toFloat(), y.toFloat(), Justify.CENTER, Justify.CENTER, name)
				TileType.PORT_ORE, TileType.PORT_SHEEP, TileType.PORT_WHEAT, TileType.PORT_WOOD, TileType.PORT_BRICK -> g.drawJustifiedString(x.toFloat(), y.toFloat(), Justify.CENTER, Justify.CENTER, """
 	2:1
 	${cell.resource.name}
 	""".trimIndent())
				TileType.PORT_MULTI -> g.drawJustifiedString(x.toFloat(), y.toFloat(), Justify.CENTER, Justify.CENTER, "3:1\n?")
				TileType.GOLD -> g.drawJustifiedString(x.toFloat(), y.toFloat(), Justify.CENTER, Justify.CENTER, """
 	$name
 	${cell.dieNum}
 	""".trimIndent())
				TileType.RANDOM_RESOURCE_OR_DESERT, TileType.RANDOM_RESOURCE, TileType.RANDOM_PORT_OR_WATER, TileType.RANDOM_PORT -> g.drawJustifiedString(x.toFloat(), y.toFloat(), Justify.CENTER, Justify.CENTER, name)
				TileType.FIELDS, TileType.FOREST, TileType.HILLS, TileType.MOUNTAINS, TileType.PASTURE -> g.drawJustifiedString(x.toFloat(), y.toFloat(), Justify.CENTER, Justify.CENTER, cell.resource.name + "\n" + cell.dieNum.toString())
				TileType.UNDISCOVERED -> {
				}
			}
		}
	}

	private fun drawTilesTextured(g: AGraphics) {
		val cellD: Vector2D = Vector2D(board.tileWidth, board.tileHeight).scaledBy(0.5f)
		g.textHeight = RenderConstants.textSizeSmall
		g.setTextStyles(AGraphics.TextStyle.BOLD)
		for (i in 0 until board.numTiles) {
			val cell = board.getTile(i)
			val v0: Vector2D = Vector2D(cell).sub(cellD)
			val v1: Vector2D = Vector2D(cell).add(cellD)
			when (cell.type) {
				TileType.NONE -> {
					g.color = outlineColorLight
					drawTileOutline(g, cell)
				}
				TileType.DESERT -> g.drawImage(desertImage, v0, v1)
				TileType.WATER -> g.drawImage(waterImage, v0, v1)
				TileType.PORT_WHEAT, TileType.PORT_WOOD, TileType.PORT_BRICK, TileType.PORT_ORE, TileType.PORT_SHEEP -> {
					g.drawImage(waterImage, v0, v1)
					g.color = textColor
					g.drawJustifiedString(cell.x, cell.y, Justify.CENTER, Justify.CENTER, """
 	2:1
 	${cell.resource.name}
 	""".trimIndent())
				}
				TileType.PORT_MULTI -> {
					g.drawImage(waterImage, v0, v1)
					g.color = textColor
					g.drawJustifiedString(cell.x, cell.y, Justify.CENTER, Justify.CENTER, "3:1\n?")
				}
				TileType.GOLD -> g.drawImage(goldImage, v0, v1)
				TileType.UNDISCOVERED -> g.drawImage(undiscoveredImage, v0, v1)
				TileType.RANDOM_RESOURCE_OR_DESERT, TileType.RANDOM_RESOURCE, TileType.RANDOM_PORT_OR_WATER, TileType.RANDOM_PORT -> {
					g.color = outlineColorLight
					drawTileOutline(g, cell)
					g.color = textColor
					g.drawJustifiedString(cell.x, cell.y, Justify.CENTER, Justify.CENTER, cell.type.name)
				}
				TileType.FIELDS -> g.drawImage(fieldshexImage, v0, v1)
				TileType.FOREST -> g.drawImage(foresthexImage, v0, v1)
				TileType.HILLS -> g.drawImage(hillshexImage, v0, v1)
				TileType.MOUNTAINS -> g.drawImage(mountainshexImage, v0, v1)
				TileType.PASTURE -> g.drawImage(pastureshexImage, v0, v1)
			}
			if (cell.dieNum > 0) {
				drawCellProductionValue(g, cell.x, cell.y, cell.dieNum)
			}
		}
	}

	override fun onClick() {
		// allow the accept button to do this work now
		if (pickedValue >= 0) {
			pickHandler!!.onPick(this, pickedValue)
			getComponent<UIComponent>().redraw()
			pickedValue = -1
		}
	}

	fun drawCellProductionValue(g: AGraphics, x: Float, y: Float, num: Int) {
		val radius = g.textHeight * 2
		g.color = GColor.BLACK
		g.begin()
		g.vertex(x, y)
		g.drawPoints(radius)
		g.end()
		g.color = GColor.CYAN
		g.drawJustifiedString(x, y, Justify.CENTER, Justify.CENTER, num.toString())
	}

	override fun draw(g: APGraphics, pickX: Int, pickY: Int) {
		val width = getComponent<UIComponent>().width
		val height = getComponent<UIComponent>().height
		if (width <= 10 || height <= 10) return  // avoid images getting resized excessively if the window is getting resized
		g.ortho()
		g.pushMatrix()
		g.setIdentity()
		try {
			val dim = Math.min(width, height).toFloat()
			g.translate((width / 2).toFloat(), (height / 2).toFloat())
			g.scale(dim, dim)
			g.translate(-0.5f, -0.5f)
			if (pickX >= 0 && pickY >= 0) doPick(g, pickX, pickY)
			val enterTime = System.currentTimeMillis()
			if (!getRenderFlag(RenderFlag.DONT_DRAW_TEXTURES)) {
				drawTilesTextured(g)
			}
			if (getRenderFlag(RenderFlag.DRAW_CELL_OUTLINES)) {
				drawTilesOutlined(g)
			}
			if (pickMode === PickMode.PM_TILE) {
				for (i in 0 until board.numTiles) {
					if (pickHandler!!.isPickableIndex(this, i)) {
						if (i == pickedValue) {
							pickHandler!!.onHighlighted(this, g, i)
						} else {
							pickHandler!!.onDrawPickable(this, g, i)
						}
					}
				}
			}
			if (!getRenderFlag(RenderFlag.DONT_DRAW_ROADS)) {
				// draw the roads
				for (i in 0 until board.numRoutes) {
					if (pickMode === PickMode.PM_EDGE) {
						if (pickHandler!!.isPickableIndex(this, i)) {
							if (i == pickedValue) {
								pickHandler!!.onHighlighted(this, g, i)
							} else {
								pickHandler!!.onDrawPickable(this, g, i)
							}
							continue
						}
					}
					val e = board.getRoute(i)
					g.color = getPlayerColor(e.player)
					drawEdge(g, e, e.type, e.player, false)
				}
			}

			// draw the structures
			if (!getRenderFlag(RenderFlag.DONT_DRAW_STRUCTURES)) {
				for (i in 0 until board.numAvailableVerts) {
					if (pickMode === PickMode.PM_VERTEX) {
						if (pickHandler!!.isPickableIndex(this, i)) {
							if (i == pickedValue) {
								pickHandler!!.onHighlighted(this, g, i)
							} else {
								pickHandler!!.onDrawPickable(this, g, i)
							}
							continue
						}
					}
					val v = board.getVertex(i)
					drawVertex(g, v, v.type, v.player, false)
				}
			}
			val robberTile = board.robberTileIndex
			val pirateTile = board.pirateTileIndex
			val merchantTile = board.merchantTileIndex
			val merchantPlayer = board.merchantPlayer
			if (pickedValue >= 0) {
				pickHandler!!.onHighlighted(this, g, pickedValue)
			}
			if (robberTile >= 0) drawRobber(g, board.getTile(robberTile))
			if (pirateTile >= 0) drawPirate(g, board.getTile(pirateTile))
			if (merchantTile >= 0) drawMerchant(g, board.getTile(merchantTile), merchantPlayer)
			if (pickMode !== PickMode.PM_NONE) pickHandler!!.onDrawOverlay(this, g)
			synchronized(animations) {
				val it = animations.iterator()
				while (it.hasNext()) {
					val a = it.next()
					a.update(g)
					getComponent<UIComponent>().redraw()
					if (a.isDone) {
						it.remove()
					}
				}
			}
			if (getRenderFlag(RenderFlag.DRAW_CELL_CENTERS)) {
				for (i in 0 until board.numTiles) {
					val c = board.getTile(i)
					g.vertex(c.x, c.y)
				}
				g.color = GColor.YELLOW
				g.drawPoints(8f)
			}
			if (getRenderFlag(RenderFlag.NUMBER_CELLS)) {
				g.color = GColor.RED
				for (i in 0 until board.numTiles) {
					val c = board.getTile(i)
					g.drawJustifiedStringOnBackground(c.x, c.y, Justify.CENTER, Justify.TOP, i.toString(), GColor.TRANSLUSCENT_BLACK, 5f)
				}
			}
			if (getRenderFlag(RenderFlag.NUMBER_VERTS)) {
				g.color = GColor.WHITE
				for (i in 0 until board.numAvailableVerts) {
					val v = board.getVertex(i)
					g.drawJustifiedStringOnBackground(v.x, v.y, Justify.CENTER, Justify.TOP, i.toString(), GColor.TRANSLUSCENT_BLACK, 5f)
				}
			}
			if (getRenderFlag(RenderFlag.NUMBER_EDGES)) {
				g.color = GColor.YELLOW
				for (i in 0 until board.numRoutes) {
					val e = board.getRoute(i)
					val m = MutableVector2D(board.getRouteMidpoint(e))
					g.drawJustifiedStringOnBackground(m.x, m.y, Justify.CENTER, Justify.TOP, i.toString(), GColor.TRANSLUSCENT_BLACK, 5f)
				}
			}
			if (getRenderFlag(RenderFlag.SHOW_ISLAND_INFO)) {
				for (i in board.islands) {
					drawIslandInfo(g, i)
				}
			}
			if (getRenderFlag(RenderFlag.SHOW_CELL_INFO)) {
				if (cellInfoIndex >= 0) {
					drawTileInfo(g, cellInfoIndex)
				}
			}
			if (getRenderFlag(RenderFlag.SHOW_EDGE_INFO)) {
				if (edgeInfoIndex >= 0) {
					drawEdgeInfo(g, edgeInfoIndex)
				}
			}
			if (getRenderFlag(RenderFlag.SHOW_VERTEX_INFO)) {
				if (vertexInfoIndex >= 0) {
					drawVertexInfo(g, vertexInfoIndex)
				}
			}
			if (pickMode === PickMode.PM_CUSTOM) {
				val handler = pickHandler as CustomPickHandler?
				for (i in 0 until handler!!.numElements) {
					if (i == pickedValue) {
						handler.onHighlighted(this, g, i)
					} else {
						handler.onDrawPickable(this, g, i)
					}
				}
			}

			// notify anyone waiting on me
			//synchronized(this) { notifyAll() }
		} catch (e: Exception) {
			e.printStackTrace()
		}
		g.popMatrix()
	}

	private fun doPick(g: APGraphics, pickX: Int, pickY: Int) {
		var index = -1
		when (pickMode) {
			PickMode.PM_NONE -> {
			}
			PickMode.PM_EDGE -> index = pickEdge(g, pickX, pickY)
			PickMode.PM_VERTEX -> index = pickVertex(g, pickX, pickY)
			PickMode.PM_TILE -> index = pickTile(g, pickX, pickY)
			PickMode.PM_CUSTOM -> index = (pickHandler as CustomPickHandler?)!!.pickElement(this, g, pickX, pickY)
		}
		if (index >= 0 && pickHandler!!.isPickableIndex(this, index)) {
			pickedValue = index
		} else {
			// pickedValue = -1;
			// preserve the most recent
		}
		if (getRenderFlag(RenderFlag.SHOW_CELL_INFO)) {
			cellInfoIndex = pickTile(g, pickX, pickY)
		}
		if (getRenderFlag(RenderFlag.SHOW_EDGE_INFO)) {
			edgeInfoIndex = pickEdge(g, pickX, pickY)
		}
		if (getRenderFlag(RenderFlag.SHOW_VERTEX_INFO)) {
			vertexInfoIndex = pickVertex(g, pickX, pickY)
		}
	}

	private fun drawInfo(g: AGraphics, v: IVector2D, info: String) {
		g.color = GColor.WHITE
		g.drawWrapStringOnBackground(v.x, v.y, (g.viewportWidth / 2).toFloat(), info, GColor.TRANSLUSCENT_BLACK, 4f)
	}

	private fun drawTileInfo(g: AGraphics, cellIndex: Int) {
		if (cellIndex < 0) return
		val cell = board.getTile(cellIndex)
		var info = """CELL $cellIndex
  ${cell.type}
adj:${cell.adjVerts}"""
		if (cell.resource != null) {
			info += """
  ${cell.resource}"""
		}
		if (cell.islandNum > 0) {
			info += """
  Island ${cell.islandNum}"""
		}
		drawInfo(g, cell, info)
	}

	private fun drawEdgeInfo(g: AGraphics, edgeIndex: Int) {
		if (edgeIndex < 0) return
		val edge = board.getRoute(edgeIndex)
		var info = "EDGE $edgeIndex"
		if (edge.player > 0) info += """
  Player ${edge.player}"""
		info += """
  ${edge.flagsString}"""
		info += """
  ang=${getEdgeAngle(edge)}"""
		info += """
  tiles=${edge.getTile(0)}/${edge.getTile(1)}"""
		drawInfo(g, board.getRouteMidpoint(edge), info)
	}

	private fun drawVertexInfo(g: AGraphics, vertexIndex: Int) {
		if (vertexIndex < 0) return
		val vertex = board.getVertex(vertexIndex)
		var info = "VERTEX $vertexIndex"
		if (vertex.isAdjacentToWater) info += " WAT"
		if (vertex.isAdjacentToLand) info += " LND"
		info += if (vertex.player > 0) {
			"""
  Player ${vertex.player}
  ${if (vertex.isCity) "City +2" else "Settlement +1"}"""
		} else {
			val pNum = board.checkForPlayerRouteBlocked(vertexIndex)
			"\n  Blocks player $pNum's roads"
		}
		drawInfo(g, vertex, info)
	}

	@JvmOverloads
	fun drawCard(color: GColor?, g: AGraphics, txt: String?, x: Float, y: Float, cw: Float, ch: Float, alpha: Float = 1f) {
		g.pushMatrix()
		g.setIdentity()
		//g.drawImage(cardFrameImage, x, y, cw, ch);
		val border = RenderConstants.thickLineThickness
		g.color = GColor.BLUE.withAlpha(alpha)
		g.drawFilledRoundedRect(x - border, y - border, cw + border * 2, ch + border * 2, cw / 4 + border)
		g.color = GColor.CYAN.darkened(0.2f).withAlpha(alpha)
		g.drawFilledRoundedRect(x, y, cw, ch, cw / 4)
		g.color = color
		g.drawWrapString(x + cw / 2, y + ch / 2, cw - border * 2, Justify.CENTER, Justify.CENTER, txt)
		g.popMatrix()
	}

	val isPicked: Boolean
		get() = pickHandler != null && pickedValue >= 0

	fun acceptPicked() {
		if (isPicked) pickHandler!!.onPick(this, pickedValue)
	}

	/**
	 * Delete screen capture and force a full redraw
	 */
	fun clearCached() {}
	fun reset() {
		setPickHandler(null)
		animations.clear()
	}

	companion object {
		private val structureFaces = arrayOf( // house front
			Face(0.0f, 0, 0, 2, 3, 4, 0, 4, -4, 0, -4).setFaceTypes(FaceType.SETTLEMENT, FaceType.CITY, FaceType.CITY_WALL, FaceType.PIRATE_FORTRESS),  // house roof
			Face(0.25f, 0, 0, 2, 3, 0, 4, -2, 1).setFaceTypes(FaceType.SETTLEMENT, FaceType.CITY, FaceType.CITY_WALL, FaceType.PIRATE_FORTRESS),  // house side
			Face(0.45f, 0, 0, -2, 1, -2, -3, 0, -4).setFaceTypes(FaceType.SETTLEMENT, FaceType.CITY, FaceType.CITY_WALL, FaceType.PIRATE_FORTRESS),  // city front panel
			Face(0.0f, 0, 0, -2, 0, -2, -4, 0, -4).setFaceTypes(FaceType.CITY, FaceType.CITY_WALL, FaceType.PIRATE_FORTRESS),  // city side panel
			Face(0.45f, -2, 0, -4, 1, -4, -3, -2, -4).setFaceTypes(FaceType.CITY, FaceType.CITY_WALL, FaceType.PIRATE_FORTRESS),  // city roof
			Face(0.1f, 0, 0, -2, 1, -4, 1, -2, 0).setFaceTypes(FaceType.CITY, FaceType.CITY_WALL, FaceType.PIRATE_FORTRESS),  // walled city
			// wall right
			Face(0.6f, 4, 0, 6, -2, 4, -2).setFaceTypes(FaceType.CITY_WALL, FaceType.PIRATE_FORTRESS),  // wall front
			Face(0.1f, 6, -2, 6, -5, -3, -5, -3, -2).setFaceTypes(FaceType.CITY_WALL, FaceType.PIRATE_FORTRESS),  // wall left
			Face(0.5f, -5, 0, -3, -2, -3, -5, -5, -3).setFaceTypes(FaceType.CITY_WALL, FaceType.PIRATE_FORTRESS),  // pirate flag
			Face(0.7f, -5, 1, -3, 0, -3, 4, -5, 5).setFaceTypes(FaceType.PIRATE_FORTRESS),
			Face(0.9f, -3, 0, -3, 4, 0, 5, 0, 1).setFaceTypes(FaceType.PIRATE_FORTRESS),
			Face(0.0f, -4, 4, -3, 3, -4, 3).setFaceTypes(FaceType.PIRATE_FORTRESS),
			Face(0.0f, -2, 4, -1, 4, -1, 3, -2, 3).setFaceTypes(FaceType.PIRATE_FORTRESS),
			Face(0.0f, -4, 2, -1, 2, -3, 1).setFaceTypes(FaceType.PIRATE_FORTRESS),  // ship
			// hull bottom
			Face(0.5f, -4, 0, 4, 0, 3, -2, -3, -2).setFaceTypes(FaceType.SHIP, FaceType.WAR_SHIP),
			Face(0.3f, -4, 0, -2, 1, 2, 1, 4, 0, 2, -1, -2, -1).setFaceTypes(FaceType.SHIP, FaceType.WAR_SHIP),  // traingle sail (ship)
			Face(0.1f, 1, 0, 1, 5, -2, 0).setFaceTypes(FaceType.SHIP),  // square sail (warship)
			Face(0.2f, 2, 2, 2, 6, -2, 4, -2, 0).setFaceTypes(FaceType.WAR_SHIP),
			Face(0.0f, 0, 2, 1, 3, 1, 5, -1, 4, -1, 2).setFaceTypes(FaceType.WAR_SHIP),  // 3D Shield
			// full darkened (inactive)
			Face(0.2f, 4, 5, 3, 5, 0, 4, 1, 4).setFaceTypes(FaceType.KNIGHT_INACTIVE_BASIC, FaceType.KNIGHT_INACTIVE_STRONG, FaceType.KNIGHT_INACTIVE_MIGHTY),
			Face(0.1f, -2, 5, -3, 5, -3, 0, -2, -1).setFaceTypes(FaceType.KNIGHT_INACTIVE_BASIC, FaceType.KNIGHT_INACTIVE_STRONG, FaceType.KNIGHT_INACTIVE_MIGHTY),
			Face(0.8f, 1, -4, 4, -1, 4, 5, 1, 4).setFaceTypes(FaceType.KNIGHT_INACTIVE_BASIC, FaceType.KNIGHT_INACTIVE_STRONG, FaceType.KNIGHT_INACTIVE_MIGHTY),
			Face(0.6f, 1, -4, -2, -1, -2, 5, 1, 4).setFaceTypes(FaceType.KNIGHT_INACTIVE_BASIC, FaceType.KNIGHT_INACTIVE_STRONG, FaceType.KNIGHT_INACTIVE_MIGHTY),  // SAA only lightened
			Face(0.2f, 4, 5, 3, 5, 0, 4, 1, 4).setFaceTypes(FaceType.KNIGHT_ACTIVE_BASIC, FaceType.KNIGHT_ACTIVE_STRONG, FaceType.KNIGHT_ACTIVE_MIGHTY),
			Face(0.1f, -2, 5, -3, 5, -3, 0, -2, -1).setFaceTypes(FaceType.KNIGHT_ACTIVE_BASIC, FaceType.KNIGHT_ACTIVE_STRONG, FaceType.KNIGHT_ACTIVE_MIGHTY),
			Face(0.1f, 1, -4, 4, -1, 4, 5, 1, 4).setFaceTypes(FaceType.KNIGHT_ACTIVE_BASIC, FaceType.KNIGHT_ACTIVE_STRONG, FaceType.KNIGHT_ACTIVE_MIGHTY),
			Face(0.1f, 1, -4, -2, -1, -2, 5, 1, 4).setFaceTypes(FaceType.KNIGHT_ACTIVE_BASIC, FaceType.KNIGHT_ACTIVE_STRONG, FaceType.KNIGHT_ACTIVE_MIGHTY),  // single sword, strong dark (inactive)
			// blade
			Face(GColor(255, 255, 255, 0), 0.8f, 0, -4, 1, -6, 2, -4, 2, 8, 0, 8).setFaceTypes(FaceType.KNIGHT_INACTIVE_STRONG),  // hilt
			Face(GColor(255, 255, 255, 0), 0.8f, -2, 5, 4, 5, 4, 4, -2, 4).setFaceTypes(FaceType.KNIGHT_INACTIVE_STRONG),  // SAA lightened for active
			// blade
			Face(GColor(160, 160, 160, 0), 0.1f, 0, -4, 1, -6, 2, -4, 2, 8, 0, 8).setFaceTypes(FaceType.KNIGHT_ACTIVE_STRONG),  // hilt
			Face(GColor(100, 100, 100, 0), 0.1f, -2, 5, 4, 5, 4, 4, -2, 4).setFaceTypes(FaceType.KNIGHT_ACTIVE_STRONG),  // double crossed sword mighty dark (inactive)
			// handle at top right
			// blade
			Face(0.8f, 5, 8, 7, 7, -1, -5, -3, -6, -3, -4).setFaceTypes(FaceType.KNIGHT_INACTIVE_MIGHTY),  // hilt
			Face(0.8f, 3, 7, 2, 6, 6, 3, 7, 4).setFaceTypes(FaceType.KNIGHT_INACTIVE_MIGHTY),  // handle at top left
			// blade
			Face(0.8f, -6, 7, -4, 8, 4, -4, 4, -6, 2, -5).setFaceTypes(FaceType.KNIGHT_INACTIVE_MIGHTY),  // hilt
			Face(0.8f, -6, 4, -5, 3, -1, 6, -2, 7).setFaceTypes(FaceType.KNIGHT_INACTIVE_MIGHTY),  // SAA lightened for active
			// handle at top right
			// blade
			Face(0.2f, 5, 8, 7, 7, -1, -5, -3, -6, -3, -4).setFaceTypes(FaceType.KNIGHT_ACTIVE_MIGHTY),  // hilt
			Face(0.1f, 3, 7, 2, 6, 6, 3, 7, 4).setFaceTypes(FaceType.KNIGHT_ACTIVE_MIGHTY),  // handle at top left
			// blade
			Face(0.2f, -6, 7, -4, 8, 4, -4, 4, -6, 2, -5).setFaceTypes(FaceType.KNIGHT_ACTIVE_MIGHTY),  // hilt
			Face(0.1f, -6, 4, -5, 3, -1, 6, -2, 7).setFaceTypes(FaceType.KNIGHT_ACTIVE_MIGHTY),  // Trade Metropolis
			// right most building
			Face(0.2f, 2, -4, 5, -4, 5, 4, 2, 5).setFaceTypes(FaceType.METRO_TRADE),
			Face(0.4f, 1, 5, 2, 4, 2, 0, 1, 0).setFaceTypes(FaceType.METRO_TRADE),
			Face(0.0f, 2, 4, 5, 4, 4, 5, 1, 5).setFaceTypes(FaceType.METRO_TRADE),  // middle building
			Face(0.0f, 3, 0, 1, 2, 1, 0).setFaceTypes(FaceType.METRO_TRADE),
			Face(0.2f, -2, 4, 1, 4, 1, 0, -2, 0).setFaceTypes(FaceType.METRO_TRADE),
			Face(0.2f, -2, 0, 3, 0, 3, -5, -2, -5).setFaceTypes(FaceType.METRO_TRADE),
			Face(0.4f, -2, 4, -2, -5, -4, -3, -4, 6).setFaceTypes(FaceType.METRO_TRADE),
			Face(0.0f, -2, 4, -4, 6, -1, 6, 1, 4).setFaceTypes(FaceType.METRO_TRADE),  // left most building
			Face(0.2f, -3, 1, -5, 1, -5, -4, -3, -4).setFaceTypes(FaceType.METRO_TRADE),
			Face(0.4f, -6, 2, -5, 1, -5, -4, -6, -3).setFaceTypes(FaceType.METRO_TRADE),
			Face(0.0f, -3, 1, -4, 2, -6, 2, -5, 1).setFaceTypes(FaceType.METRO_TRADE),  // Politics Metropolis
			// roofs from right to left
			Face(0.0f, 5, 0, 7, -1, 4, -1, 2, 0).setFaceTypes(FaceType.METRO_POLITICS),
			Face(0.1f, 1, 3, -1, 4, -3, 3, -1, 2).setFaceTypes(FaceType.METRO_POLITICS),
			Face(0.2f, -1, 2, -2, 0, -4, 1, -3, 3).setFaceTypes(FaceType.METRO_POLITICS),
			Face(0.3f, -2, 0, -4, 1, -4, 0, -2, -1).setFaceTypes(FaceType.METRO_POLITICS),
			Face(0.0f, -4, 0, -7, 0, -5, -1, -2, -1).setFaceTypes(FaceType.METRO_POLITICS),  // front
			Face(0.5f, 1, 3, 3, 2, 4, 0, 4, -1, -2, -1, -2, 0, -1, 2).setFaceTypes(FaceType.METRO_POLITICS),
			Face(0.5f, 7, -1, 7, -4, -5, -4, -5, -1).setFaceTypes(FaceType.METRO_POLITICS),  // left wall
			Face(0.6f, -7, 0, -5, -1, -5, -4, -7, -3).setFaceTypes(FaceType.METRO_POLITICS),  // door
			Face(1.0f, 0, 0, 2, 0, 2, -4, 0, -4).setFaceTypes(FaceType.METRO_POLITICS),  // Science metropolis
			// column sides
			Face(0.8f, -3, 3, 2, 3, 2, -2, -3, -2).setFaceTypes(FaceType.METRO_SCIENCE),  // base top
			Face(0.7f, 2, -1, 3, -2, -2, -2, -3, -1).setFaceTypes(FaceType.METRO_SCIENCE),  // base angled side
			Face(0.2f, -3, -1, -2, -2, -3, -3, -4, -2).setFaceTypes(FaceType.METRO_SCIENCE),  // base side
			Face(0.4f, -4, -2, -4, -3, -3, -4, -3, -3).setFaceTypes(FaceType.METRO_SCIENCE),  // base front
			Face(0.5f, -2, -2, 3, -2, 4, -3, 4, -4, -3, -4, -3, -3).setFaceTypes(FaceType.METRO_SCIENCE),  // right column front
			Face(0.5f, 2, 2, 3, 2, 3, -2, 2, -2).setFaceTypes(FaceType.METRO_SCIENCE),  // center column front
			Face(0.5f, 0, 2, 1, 2, 1, -2, 0, -2).setFaceTypes(FaceType.METRO_SCIENCE),  // left column front
			Face(0.5f, -2, 2, -1, 2, -1, -2, -2, -2).setFaceTypes(FaceType.METRO_SCIENCE),  // roof top
			Face(0.0f, -2, 5, 1, 5, 2, 4, -1, 4).setFaceTypes(FaceType.METRO_SCIENCE),  // roof left
			Face(0.2f, -2, 5, -1, 4, -4, 2, -5, 3).setFaceTypes(FaceType.METRO_SCIENCE),  // roof front
			Face(0.5f, -4, 2, -1, 4, 2, 4, 5, 2).setFaceTypes(FaceType.METRO_SCIENCE),  // Merchant
			// Dome
			Face(0.0f, 1, 3, 3, 2, 4, 0, 4, -4, -4, -4, -4, 0, -3, 2, -1, 3).setFaceTypes(FaceType.MERCHANT),  // door
			Face(1.0f, 1, -1, 1, -4, -1, -4, -1, -1).setFaceTypes(FaceType.MERCHANT),  // door flap
			Face(0.2f, 1, -1, -1, -4, -2, -1).setFaceTypes(FaceType.MERCHANT),  // flag
			Face(0.0f, 0, 3, 0, 6, 3, 4).setFaceTypes(FaceType.MERCHANT),  // Robber
			// Head
			Face(0.0f, 3, 2, 3, -4, -3, -4, -3, 2).setFaceTypes(FaceType.ROBBER),  // Right Hat
			Face(1.0f, 0, 2, 4, 2, 4, 3, 2, 3, 1, 5, 0, 4).setFaceTypes(FaceType.ROBBER),  // Left Hat
			Face(1.0f, 0, 2, -4, 2, -4, 3, -2, 3, -1, 5, 0, 4).setFaceTypes(FaceType.ROBBER),  // Right Eye
			Face(1.0f, 1, 0, 1, 1, 3, 1, 3, 0).setFaceTypes(FaceType.ROBBER),  // Left Eye
			Face(1.0f, -1, 0, -1, 1, -3, 1, -3, 0).setFaceTypes(FaceType.ROBBER),  // Right Coat
			Face(1.0f, 1, -2, 4, 0, 4, -5, 1, -5).setFaceTypes(FaceType.ROBBER),  // LeftCoat
			Face(1.0f, -1, -2, -4, 0, -4, -5, -1, -5).setFaceTypes(FaceType.ROBBER))
	}
}