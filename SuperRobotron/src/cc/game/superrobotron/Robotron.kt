@file:Suppress("LocalVariableName", "LocalVariableName")

package cc.game.superrobotron

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.ksp.binaryserializer.readInt
import cc.lib.ksp.binaryserializer.writeInt
import cc.lib.logger.LoggerFactory
import cc.lib.math.CMath
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.reflector.Omit
import cc.lib.reflector.RBufferedReader
import cc.lib.reflector.Reflector
import cc.lib.utils.flipCoin
import cc.lib.utils.increment
import cc.lib.utils.random
import cc.lib.utils.randomFloat
import cc.lib.utils.randomFloatPlusOrMinus
import cc.lib.utils.rotate
import cc.lib.utils.squared
import cc.lib.utils.unhandledCase
import java.nio.ByteBuffer
import java.util.Arrays
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * Variation on the great robotron game from 1982
 *
 * The robotron traverses a maze to escape through the portal
 * all the while battling bot, brains, tanks and thugs.
 *
 * But this time our hero has some new tricks up his sleeve!
 */
@Suppress("SpellCheckingInspection", "LocalVariableName", "FunctionName", "PrivatePropertyName", "PropertyName")
abstract class Robotron : Reflector<Robotron>() {

	companion object {

		init {
			addAllFields(Robotron::class.java)
		}

		var GAME_VISIBILITY = false
		val log = LoggerFactory.getLogger(Robotron::class.java)

		private val WALL_NORTH = 1
		private val WALL_EAST = 2
		private val WALL_SOUTH = 4
		private val WALL_WEST = 8

		private val move_dv = arrayOf(
			Vector2D(0, -1),
			Vector2D(1, 0),
			Vector2D(0, 1),
			Vector2D(-1, 0),
		)
		private val move_diag_dv = arrayOf(
			Vector2D(1, 1),
			Vector2D(1, -1),
			Vector2D(-1, 1),
			Vector2D(-1, -1),
		)
	}

	@Omit
	open val instructions = "Use 'WASD' keys to move\nUse mouse to fire"

	@Omit
	open val drawDebugButtons = true

	// ---------------------------------------------------------//
	// GLOBAL DATA //
	// ---------------------------------------------------------//
	// maze data.  Each cell is a bit flag
	// a cell is a bitflag that provides info on what walls remain for that
	// cell. adjacent cells actually have 2 walls between them, so breaking
	// down a wall means removing opposing walls from the cells. For
	// example,
	// if cell A is directly above cell B, the the SOUTH wall of cell A
	// overlaps the NORTH wall of cell B. To create a path between these
	// cells means removing the SOUTH cell from A and the NORTH cell from B.
	// wall flags organized in CCW order, this is important DO NOT
	// REORGANIZE!
	private val maze_cells = Array(MAZE_NUMCELLS_X) {
		IntArray(MAZE_NUMCELLS_Y)
	}

	@Omit
	private val workingVerts = Array(64) { MutableVector2D() }
	@Omit
	private var curWorkingVert = 0

	init {
		Vector2D.getFromPool = {
			workingVerts[curWorkingVert].also {
				curWorkingVert = curWorkingVert.rotate(workingVerts.size)
			}
		}
	}

	val maze_verts = Array(MAZE_NUM_VERTS) { Vector2D.ZERO }

	private enum class WallDir {
		RIGHT,
		BOTTOM,
		LEFT,
		TOP
	}

	val walls: Array<Array<Wall?>>

	init {
		val arr = Array(MAZE_NUM_VERTS) { Array<Wall?>(4) { null } }
		var id = 1
		fun addWall(v0: Int, v1: Int, dir: WallDir) {
			require(v0 != v1)
			if (v0 in 0 until MAZE_NUM_VERTS && v1 in 0 until MAZE_NUM_VERTS) {
				val wall = Wall(id++, v0, v1)
				wall.type = WALL_TYPE_NONE
				require(!arr[v0].contains(wall)) { "Vertex $v0 already has $wall" }
				require(!arr[v1].contains(wall))
				arr[v0][dir.ordinal] = wall
				arr[v1][dir.increment(2).ordinal] = wall
			}
		}

		for (i in 0..MAZE_NUMCELLS_Y) {
			for (ii in 0..MAZE_NUMCELLS_X) {
				val v = i * (MAZE_NUMCELLS_X + 1) + ii
				if (ii < MAZE_NUMCELLS_X)
					addWall(v, v + 1, WallDir.RIGHT)
				if (i < MAZE_NUMCELLS_Y)
					addWall(v, v + MAZE_NUMCELLS_X + 1, WallDir.BOTTOM)
			}
		}

		walls = arr
	}

	private val verts_min_v = MutableVector2D(0, 0)
	private val verts_max_v = MutableVector2D(MAZE_WIDTH, MAZE_HEIGHT)

	// rectangle of maze dimension
	private val verts_min_x: Float
		get() = verts_min_v.x
	private val verts_min_y: Float
		get() = verts_min_v.y
	private val verts_max_x: Float
		get() = verts_max_v.x
	private val verts_max_y: Float
		get() = verts_max_v.y

	// position of ending 'spot' or goal
	private var end_cell = intArrayOf(0, 0)
	private var start_cell = intArrayOf(0, 0)

	// PLAYER DATA
	@Omit
	val players = ManagedArray(Array(MAX_PLAYERS) { Player() }).also {
		it.add()
	}

	@Omit
	var this_player = 0
		set(value) {
			require(value < MAX_PLAYERS)
			field = value
			while (players.size <= value)
				players.add()
			player.screen.setDimension(screen_dim)
		}

	@Omit
	val screen_dim = GDimension(MAZE_WIDTH, MAZE_HEIGHT)

	val player: Player
		get() {
			return players[this_player]
		}

	open var high_score = 0

	// down on mouse button and false
	// when released
	// GAME STATE --------------------------
	private var game_state = GAME_STATE_INTRO // current game state
	private var game_start_frame = 0 // the frame the match started
	private var game_type = GAME_TYPE_ROBOCRAZE // current game type
	var gameLevel = 1 // current level
		private set

	// -----------------------------------------------------------------------------------------------

	private var total_enemies = 0 // number of non-thugs remaining

	// General use arrays
	@Omit
	val enemy_missiles = ManagedArray(Array(MAX_ENEMY_MISSLES) { Missile() })
	@Omit
	val tank_missiles = ManagedArray(Array(MAX_TANK_MISSLES) { Missile() })
	@Omit
	val snake_missiles = ManagedArray(Array(MAX_SNAKE_MISSLES) { MissileSnake() })

	@Omit
	val powerups = ManagedArray(Array(MAX_POWERUPS) { Powerup() })

	// ENEMIES ----------------------------
	@Omit
	val enemies = ManagedArray(Array(MAX_ENEMIES) { Enemy() })
	private var enemy_robot_speed = 0

	// EXPLOSIN EFFECTS --------------------
	@Omit
	var particles = ManagedArray(Array(MAX_PARTICLES) { Particle() })

	// MSGS that drift away ---------------------------
	@Omit
	val messages = ManagedArray(Array(MAX_MESSAGES) { Message() })

	@Omit
	val cursor = AtomicReference(MutableVector2D())

	// PEOPLE ------------------------------
	@Omit
	val people = ManagedArray(Array(MAX_PEOPLE) { People() })

	// north, 2 = east, 3 =
	// south, 4 = west
	private var people_points = PEOPLE_START_POINTS

	// Zombie tracers ----------------------
	@Omit
	val zombie_tracers = ManagedArray(Array(MAX_ZOMBIE_TRACERS) { Tracer() })

	// Player tracers ----------------------
	// Other lookups
	// Maps N=0,E,S,W to a 4 dim array lookup

	@Omit
	private var throbbing_white = GColor.WHITE
	@Omit
	private var throbbing_dir = 0 // 0 == darker(), 1 == lighter()
	private var quick_msg_color = GColor.YELLOW
	private var quick_msg_str: String? = null

	@Omit
	private val button_x = FloatArray(BUTTONS_NUM)
	@Omit
	private val button_y = FloatArray(BUTTONS_NUM)
	@Omit
	private var button_active = -1 // -1 == none, 1 == button 1, 2 == button 2,

	// ...
	private var difficulty = DIFFICULTY_EASY // Easy == 0, Med == 1, Hard == 2
	private var frameNumber = 0

	@Omit
	lateinit var G: AGraphics

	@Omit
	var client: IRoboClient? = null
	@Omit
	var server: IRoboServer? = null

	val screen_x: Float
		get() = player.screen.left
	val screen_y: Float
		get() = player.screen.top
	val screen_width: Float
		get() = player.screen.width
	val screen_height: Float
		get() = player.screen.height

	private fun wallChanceForLevel(level: Int): IntArray = intArrayOf(
		0,
		80,  // - this.game_level,
		2 + level,
		10 + level,
		5 + level,
		5 + level,
		0 + level,
		0 + level
	)

	fun setDimension(screenWidth: Int, screenHeight: Int) {
		screen_dim.assign(screenWidth, screenHeight)
		player.screen.setDimension(screen_dim)
		setIntroButtonPositionAndDimension()
	}

	// DEBUG Drawing stuff
	// final int [] debug_player_primary_verts = new int[5]; // used by
	// MEMBERS
	// //////////////////////////////////////////////////////////////////////
	// -----------------------------------------------------------------------------------------------
	// draw any debug related stuff
	private fun drawDebug(g: AGraphics) {
		if (isDebugEnabled(Debug.DRAW_MAZE_INFO)) {
			drawDebugWallInfo(g)
			g.color = GColor.WHITE
			g.pushMatrix()
			g.setIdentity()
			g.drawJustifiedString(
				10f,
				screen_height / 2,
				Justify.LEFT,
				Justify.CENTER,
				"""TOP V: ${computeTopLeftCornerVertex()}
					screen x: ${player.screen.left}
					screen y: ${player.screen.top}
					maze w: ${verts_max_x - verts_min_x}
					maze_h: ${verts_max_y - verts_min_y}
					player pos: ${player.pos}""".trimIndent()
			)
			g.popMatrix()
			for (v in 0 until MAZE_NUM_VERTS) {
				g.drawString("$v", maze_verts[v])
			}
			computeBaseQuad(player.pos).forEach {
				g.drawFilledCircle(maze_verts[it], 5f)
			}
		}
		if (isDebugEnabled(Debug.DRAW_PLAYER_INFO)) {
			computeWallCandidates(player.pos, false).forEach {
				val nearV = maze_verts[it.nearV]
				val farV = maze_verts[it.farV]
				val mpv = nearV.midPoint(farV)
				g.color = GColor.BLUE
				g.drawLine(nearV, farV, 1f)
				g.color = GColor.WHITE
				g.drawString(getWallTypeString(it.type), mpv)
				g.color = GColor.YELLOW
				g.drawCircle(nearV, 10f)
				g.color = GColor.ORANGE
				g.drawCircle(farV, 5f)
			}

			g.pushMatrix()
			g.setIdentity()
			g.setColor(GColor.WHITE)
			g.drawJustifiedString(screen_width - 10, screen_height / 2, Justify.RIGHT, Justify.CENTER,
				"""$this_player : ID
					${getPlayerStateString(player.state)}
					""".trimIndent())
			g.popMatrix()
		}
		if (isDebugEnabled(Debug.PATH) && player.path.size > 1) {
			var s = player.path[0]
			for (i in 1 until player.path.size) {
				val n = player.path[i]
				val x0 = s[0] * MAZE_CELL_DIM + MAZE_CELL_DIM / 2
				val y0 = s[1] * MAZE_CELL_DIM + MAZE_CELL_DIM / 2
				val x1 = n[0] * MAZE_CELL_DIM + MAZE_CELL_DIM / 2
				val y1 = n[1] * MAZE_CELL_DIM + MAZE_CELL_DIM / 2
				s = n
				g.color = GColor.GREEN
				g.drawLine(x0, y0, x1, y1, 3f)
			}
		}
		g.color = GColor.WHITE
	}

	fun Player.speed(): Float {
		var speed = PLAYER_SPEED
		speed = (speed - numSnakesAttached).coerceIn(1..100)
		if (powerup == POWERUP_SUPER_SPEED) speed += PLAYER_SUPER_SPEED_BONUS
		if (isHulkActiveCharging(player)) speed += PLAYER_HULK_CHARGE_SPEED_BONUS
		return speed.toFloat()
	}

	// -----------------------------------------------------------------------------------------------
	val numSnakesAttached: Int
		get() = snake_missiles.count { it.state == SNAKE_STATE_ATTACHED }

	// -----------------------------------------------------------------------------------------------
	fun addPowerup(pos: Vector2D, type: Int) {
		(powerups.addOrNull() ?: powerups.random()).init(pos, type)
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawPowerUp(g: AGraphics, type: Int, x: Float, y: Float) {
		if (type == POWERUP_KEY) {
			val dim = 20f
			g.color = GColor.WHITE
			g.drawImage(imageKey, (x - dim / 2), (y - dim / 2), dim, dim)
		} else {
			g.color = throbbing_white
			val r = POWERUP_RADIUS + frameNumber % 12 / 4
			g.drawCircle(x, y, r)
			g.color = GColor.RED
			if (type == POWERUP_BONUS_PLAYER) {
				drawStickFigure(g, x, y, POWERUP_RADIUS)
			} else {
				val c = getPowerupTypeString(type)[0]
				g.drawJustifiedString(x, y, Justify.CENTER, Justify.CENTER, c.toString())
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawPowerups(g: AGraphics) {
		powerups.filter { it.isOnScreen() }.forEach {
			drawPowerUp(g, it.type, it.pos.x, it.pos.y)
		}
	}

	private fun drawPlayers(g: AGraphics) {
		players.forEachIndexed { id, player ->
			when (player.state) {
				PLAYER_STATE_SPAWNING -> {
					drawPlayerHighlight(player, g)
					drawPlayer(player, g, player.pos.x, player.pos.y, player.dir, PLAYER_COLORS[id])
				}

				PLAYER_STATE_ALIVE, PLAYER_STATE_TELEPORTED -> {
					drawPlayer(player, g, player.pos.x, player.pos.y, player.dir, PLAYER_COLORS[id])
				}

				PLAYER_STATE_EXPLODING -> drawPlayerExploding(player, g)
				PLAYER_STATE_SPECTATOR -> {
				}
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun updatePowerups() {
		with(powerups.iterator()) {
			while (hasNext()) {
				val p = next()
				if (!p.isOnScreen() || p.duration > POWERUP_MAX_DURATION) {
					remove()
					continue
				}
				players.forEach { player ->
					// see if the player is picking me up
					val dv = player.pos.sub(p.pos)
					val min = (player.radius + POWERUP_RADIUS)
					val len2 = dv.magSquared()
					if (len2 < min * min) {
						val msgV = player.pos.withJitter(30f, 30f)
						messages.add().init(msgV, getPowerupTypeString(p.type))
						when (p.type) {
							POWERUP_BONUS_PLAYER -> player.lives += 1
							POWERUP_BONUS_POINTS -> {
								val points = random(1..POWERUP_BONUS_POINTS_MAX) * POWERUP_POINTS_SCALE
								addPoints(player, points)
								messages.add().init(msgV.add(20f, 0f), points.toString())
							}

							POWERUP_KEY -> {
								player.keys++
								addPlayerMsg(player, "Found a key!")
							}

							else -> setPlayerPowerup(player, p.type)
						}
						remove()
					}
				}
			}
		}
		if (frameNumber % (1000 - gameLevel * 100) == 0) {
			players.forEach {
				addRandomPowerup(it.pos, 100f)
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun addRandomPowerup(pos: Vector2D, radius: Float) {
		log.debug("addRandomPowerup $pos radius + [$radius]")
		val powerup = Utils.chooseRandomFromSet(*POWERUP_CHANCE)

		// final int range = radius * 10;
		val minRange = radius * 2
		val maxRange = radius * 20
		val maxTries = 100
		for (i in 0 until maxTries) {
			var dx = randomFloat(minRange, maxRange)
			var dy = randomFloat(minRange, maxRange)
			if (flipCoin()) dx = -dx
			if (flipCoin()) dy = -dy
			val newV = pos.add(dx, dy)
			if (!isInMaze(newV))
				continue
			collisionScanCircle(newV, POWERUP_RADIUS + 5) ?: run {
				addPowerup(newV, powerup)
				return
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun setPlayerPowerup(player: Player, type: Int) {
		player.powerup = type
		player.powerup_duration = 0
	}

	// -----------------------------------------------------------------------------------------------
	// append a tank missile
	private fun addTankMissile(pos: Vector2D) {
		val index = random(0..3)
		tank_missiles.addOrNull()?.init(
			pos, move_diag_dv[index].scaledBy(TANK_MISSLE_SPEED), TANK_MISSLE_DURATION
		)
	}

	// -----------------------------------------------------------------------------------------------
	// append a zombie tracer
	private fun addZombieTracer(pos: Vector2D) {
		zombie_tracers.addOrNull()?.init(pos, GColor.WHITE)
	}

	// -----------------------------------------------------------------------------------------------
	// append a zombie tracer
	private fun addPlayerTracer(player: Player, pos: Vector2D, dir: Int, color: GColor) {
		player.tracer.addOrNull()?.init(pos, color, dir)
	}

	// -----------------------------------------------------------------------------------------------
	// append a snake missile at x,y
	fun addSnakeMissile(pos: Vector2D): MissileSnake? {
		return snake_missiles.addOrNull()?.also {
			it.init(pos, 0, SNAKE_STATE_CHASE)
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Do colliison scan and motion on all missiles
	private fun updateSnakeMissiles(player: Player) {
		val framesPerSection = SNAKE_SECTION_LENGTH / SNAKE_SPEED
		val playerRadius = player.radius
		// cache player position
		val px = player.pos.x - playerRadius
		val py = player.pos.y - playerRadius
		val pw = playerRadius * 2
		val ph = playerRadius * 2
		val iter = snake_missiles.iterator()
		while (iter.hasNext()) {
			val ms = iter.next()
			if (ms.duration > SNAKE_DURATION) {
				ms.kill()
			}
			ms.duration++
			val factor = SNAKE_HEURISTIC_FACTOR
			when (ms.state) {
				SNAKE_STATE_ATTACHED, SNAKE_STATE_CHASE -> {
					// advance the head
					val dv = move_dv[ms.dir[0]] * SNAKE_SPEED
					ms.pos += dv
					val touchSection = collisionMissileSnakeRect(ms, player.pos, pw, ph)
					if (ms.state == SNAKE_STATE_CHASE) {
						if (touchSection == 0 && playerHit(player, HIT_TYPE_SNAKE_MISSLE, iter.index)) {
							//log.debug("Snake [$i] got the player")
							ms.state = SNAKE_STATE_ATTACHED
						}
						if (touchSection < 0 || !playerHit(player, HIT_TYPE_SNAKE_MISSLE, iter.index)) {
							//log.debug("Snake [$i] lost hold of the player")
							ms.state = SNAKE_STATE_CHASE
						}
					}
					if (ms.duration % framesPerSection == 0) {
						// make a new section

						// shift all directions over
						var d = SNAKE_MAX_SECTIONS - 1
						while (d > 0) {
							ms.dir[d] = ms.dir[d - 1]
							d--
						}
						if (ms.num_sections < SNAKE_MAX_SECTIONS)
							ms.num_sections++

						// choose a new direction that is not a reverse (it looks
						// bad
						// when a snake 'backsup' on itself)
						val reverseDir = (ms.dir[0] + 2) % 4
						var newDir = enemyDirectionHeuristic(ms.pos, factor)
						if (newDir == reverseDir) {
							// choose right or left
							newDir = if (flipCoin()) {
								(newDir + 1) % 4
							} else {
								(newDir + 3) % 4
							}
						}
						ms.dir[0] = newDir
					}
				}

				SNAKE_STATE_DYING -> {
					// shift elems over
					for (ii in 0 until ms.num_sections - 1) {
						ms.dir[ii] = ms.dir[ii + 1]
					}
					ms.num_sections--
					if (ms.num_sections == 0) {
						iter.remove()
					}
				}

				else -> require(false) { "Unhandled case ${ms.state}" }
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	// return the section that touched or -1 for no collision
	private fun collisionMissileSnakeRect(ms: MissileSnake, v: Vector2D, w: Float, h: Float): Int {
		val framesPerSection = SNAKE_SECTION_LENGTH / SNAKE_SPEED
		val headLen = ms.duration % framesPerSection
		val tailLen = framesPerSection - headLen
		var v0: Vector2D = ms.pos
		for (i in 0 until ms.num_sections) {
			var len = SNAKE_SECTION_LENGTH
			if (i == 0)
				len = headLen
			else if (i == SNAKE_MAX_SECTIONS - 1)
				len = tailLen
			val rDir = (ms.dir[i] + 2) % 4
			val v1 = v0 + move_dv[rDir] * len.toFloat()
			val vv = v0.coerceAtMost(v1)
			val dim = (v0 - v1).absEq()
			if (Utils.isBoxesOverlapping(v, w, h, vv, dim.x, dim.y)) {
				// log.debug("Snake Collision [" + i + "]");
				return i
			}
			v0 = v1
		}
		return -1
	}

	// -----------------------------------------------------------------------------------------------
	// Draw all the 'snakes'
	private fun drawSnakeMissiles(g: AGraphics) {
		val framesPerSection = (SNAKE_SECTION_LENGTH / SNAKE_SPEED).toFloat()
		snake_missiles.filter { it.isOnScreen() }.forEach { m ->
			var v0: Vector2D = m.pos

			// draw the first section
			var d = (m.dir[0] + 2) % 4 // get reverse direction
			var frames = m.duration % framesPerSection
			var v1 = v0 + move_dv[d] * SNAKE_SPEED * frames
			g.color = GColor.BLUE
			drawBar(g, v0.x, v0.y, v1.x, v1.y, SNAKE_THICKNESS, 0)

			// draw the middle sections
			for (ii in 1 until m.num_sections) {
				v0 = v1
				d = (m.dir[ii] + 2) % 4
				v1 = v0 + move_dv[d] * SNAKE_SPEED * framesPerSection
				drawBar(g, v0.x, v0.y, v1.x, v1.y, SNAKE_THICKNESS, ii)
			}

			// draw the tail if we are at max sections
			if (m.num_sections == SNAKE_MAX_SECTIONS) {
				d = (d + 2) % 4
				frames = framesPerSection - frames
				v0 = v1
				v1 = v0 + move_dv[d] * SNAKE_SPEED * frames
				drawBar(g, v0.x, v0.y, v1.x, v1.y, SNAKE_THICKNESS, SNAKE_MAX_SECTIONS - 1)
			}

			// draw the head
			g.color = throbbing_white
			g.drawFilledRect(
				(m.pos.x - SNAKE_THICKNESS),
				(m.pos.y - SNAKE_THICKNESS),
				(SNAKE_THICKNESS * 2f),
				(SNAKE_THICKNESS * 2f)
			)
		}
	}

	// -----------------------------------------------------------------------------------------------
	// add the maximum number of people randomly around the maze (but not on an
	// edge)
	private fun addPeople() {
		while (people.isNotFull()) {
			val p = people.add()
			while (true) {
				p.pos.randomEq(
					randomFloat(verts_min_x + MAZE_VERTEX_NOISE * 3, verts_max_x - MAZE_VERTEX_NOISE * 3),
					randomFloat(verts_min_y + MAZE_VERTEX_NOISE * 3, verts_max_y - MAZE_VERTEX_NOISE * 3)
				)
				collisionScanCircle(p.pos, PEOPLE_RADIUS * 2) ?: break
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Add an enemy. If the table is full, replace the oldest GUY.
	private fun addEnemy(v: Vector2D, type: Int) {
		enemies.addOrNull()?.let { e ->
			e.pos.assign(v)
			e.type = type
			e.next_update = frameNumber + random(5..10)
			e.spawned_frame = frameNumber + random(10..20)
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Draw the tracer when > black and update fade away color
	private fun updateAndDrawZombieTracers(g: AGraphics) {
		val update = frameNumber % ENEMY_ZOMBIE_TRACER_FADE == 0
		zombie_tracers.removeIf { !it.isOnScreen() }
		val iter = zombie_tracers.iterator()
		while (iter.hasNext()) {
			val z = iter.next()
			g.color = z.color
			drawStickFigure(g, z.pos.x, z.pos.y, ENEMY_ZOMBIE_RADIUS)
			if (update)
				z.color = z.color.darkened(DARKEN_AMOUNT)
			if (z.color == GColor.BLACK)
				iter.remove()
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Draw the tracer when > black and update fade away color
	private fun updateAndDrawPlayerTracers(player: Player, g: AGraphics) {
		player.tracer.removeIf { !it.isOnScreen() }
		val iter = player.tracer.iterator()
		while (iter.hasNext()) {
			val t = iter.next()
			drawPlayerBody(player, g, t.pos.x, t.pos.y, t.dir, t.color)
			t.color = t.color.darkened(DARKEN_AMOUNT)
			if (t.color == GColor.BLACK)
				iter.remove()
		}
	}

	// -----------------------------------------------------------------------------------------------
	// this draws the collapsing indicator box a the beginning of play
	private fun drawPlayerHighlight(player: Player, g: AGraphics) {
		val scale = (1f - (frameNumber - game_start_frame).toFloat() / PLAYER_SPAWN_FRAMES).coerceIn(0f..1f)
		val left = player.pos.x - screen_width / 2 * scale
		val top = player.pos.y - screen_height / 2 * scale
		val width = screen_width * scale
		val height = screen_height * scale
		g.color = GColor.RED
		g.drawRect(left, top, width, height, 1f)
	}

	// -----------------------------------------------------------------------------------------------
	// Add points to the player's score and update highscore and players lives
	open fun addPoints(player: Player, amount: Int) {
		val before = player.score / PLAYER_NEW_LIVE_SCORE
		if ((player.score + amount) / PLAYER_NEW_LIVE_SCORE > before) {
			setQuickMsg("EXTRA MAN!")
			player.lives++
		}
		player.score += amount
		if (player.score > high_score)
			high_score = player.score
	}

	// -----------------------------------------------------------------------------------------------
	// Update the throbbing white text
	private fun updateThrobbingWhite() {
		if (frameNumber % THROBBING_SPEED == 0) {
			if (throbbing_dir == 0) {
				throbbing_white = throbbing_white.darkened(DARKEN_AMOUNT)
				if (throbbing_white == GColor.BLACK)
					throbbing_dir = 1
			} else {
				throbbing_white = throbbing_white.lightened(LIGHTEN_AMOUNT)
				if (throbbing_white == GColor.WHITE)
					throbbing_dir = 0
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Draw the game over text
	private fun drawGameOver(g: AGraphics) {
		val x = screen_width / 2
		val y = screen_height / 2
		g.color = throbbing_white
		g.drawJustifiedString(x, y, Justify.CENTER, "G A M E   O V E R")
	}

	// -----------------------------------------------------------------------------------------------
	// Set the quick message
	private fun setQuickMsg(s: String) {
		quick_msg_color = GColor.YELLOW
		quick_msg_str = s
	}

	// -----------------------------------------------------------------------------------------------
	// draw the score, high score, and number of remaining players
	private fun drawPlayerInfo(player: Player, g: AGraphics) {
		val text_height = 16f
		// draw the score
		var x = TEXT_PADDING
		var y = TEXT_PADDING
		var hJust = Justify.LEFT
		g.color = GColor.WHITE
		g.drawJustifiedString(x, y, hJust, String.format("Score %d", player.score))
		y += text_height
		g.drawJustifiedString(x, y, hJust, String.format("Lives X %d", player.lives))
		y += text_height
		g.drawJustifiedString(x, y, hJust, String.format("People X %d", player.people_picked_up))
		y += text_height
		g.drawJustifiedString(x, y, hJust, String.format("Keys X %d", player.keys))
		x = screen_width / 2
		y = TEXT_PADDING
		hJust = Justify.CENTER
		g.drawJustifiedString(x, y, hJust, String.format("High Score %d", high_score))
		x = screen_width
		y = TEXT_PADDING
		hJust = Justify.RIGHT
		g.drawJustifiedString(x, y, hJust, String.format("Level %d", gameLevel))

		// draw the instmsg
		quick_msg_str?.let { str ->
			if (quick_msg_color == GColor.BLACK) {
				quick_msg_str = null
			} else {
				g.color = quick_msg_color
				g.drawString(quick_msg_str, TEXT_PADDING, (TEXT_PADDING + text_height) * 3)
				if (frameNumber % 3 == 0)
					quick_msg_color = quick_msg_color.darkened(DARKEN_AMOUNT)
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	// draw the player exploding after being killed
	private fun drawPlayerExploding(player: Player, g: AGraphics) {
		val v = player.pos.withJitter(5f, 5f)
		// drawPlayer(g, x, y, 2);
		drawPlayerBody(player, g, v.x, v.y, player.dir, GColor.RED)

		// outline the thing that hit the player with a blinking yellow circle
		if (frameNumber % 40 < 20) return
		g.color = GColor.YELLOW
		val rad: Float
		when (player.hit_type) {
			HIT_TYPE_ENEMY -> {
				val enemy = enemies[player.hit_index]
				rad = enemy.radius()
				g.drawCircle(enemy.pos, rad)
			}

			HIT_TYPE_ROBOT_MISSLE -> {
				rad = ENEMY_PROJECTILE_RADIUS + 5
				val em = enemy_missiles[player.hit_index]
				g.drawCircle(em.pos, rad)
			}

			HIT_TYPE_TANK_MISSLE -> {
				rad = TANK_MISSLE_RADIUS + 5
				val tm = tank_missiles[player.hit_index]
				g.drawCircle(tm.pos, rad)
			}

			else -> {
			}
		}
	}

	private fun updatePlayers() {
		players.forEach {
			updatePlayer(it)
		}
	}

	// -----------------------------------------------------------------------------------------------
	// do collision scans and make people walk around stupidly
	private fun updatePeople() {
		val iter = people.iterator()
		here@ while (iter.hasNext()) {
			val p = iter.next()
			if (p.state == 0) {
				iter.remove()
				continue
			}
			if (p.state < 0) {
				// this guy is turning into a zombie
				if (++p.state == 0) {
					// add a zombie
					addEnemy(p.pos, random(ENEMY_INDEX_ZOMBIE_N..ENEMY_INDEX_ZOMBIE_W))
					iter.remove()
					continue
				}
				continue
			}
			if (!p.isOnScreen()) {
				continue
			}
			// look for a colliison with the player
			for (player in players) {
				if (player.pos.isWithinDistance(p.pos, PEOPLE_RADIUS + player.radius)) {
					if (isHulkActiveCharging(player)) {
						if (random(0..3) == 3) addPlayerMsg(player, "HULK SMASH!")
						addPoints(player, -people_points)
						if (player.people_picked_up > 0)
							player.people_picked_up--
						addParticle(p.pos, PARTICLE_TYPE_BLOOD, PARTICLE_BLOOD_DURATION, -1, 0)
					} else {
						addMsg(p.pos, people_points.toString()) // + " points");
						addPoints(player, people_points)
						player.people_picked_up++
						if (people_points < PEOPLE_MAX_POINTS) people_points += PEOPLE_INCREASE_POINTS * gameLevel
					}
					iter.remove()
					continue@here
				}
			}
			if (frameNumber % 5 == p.state) {
				val dv = move_dv[p.state - 1] * PEOPLE_SPEED
				collisionScanCircle(p.pos + dv, PEOPLE_RADIUS)?.let {
					// reverse direction
					if (p.state <= 2) p.state += 2 else p.state -= 2
				} ?: run {
					p.pos += dv

					// look for random direction changes
					if (random(0..10) == 0)
						p.state = random(1..4)
				}
			}
		}
	}

	private fun drawPerson(g: AGraphics, p: People) {
		val dir = p.state - 1
		if (dir in 0..3) {
			val dim = 32f
			drawPerson(g, p.pos.x, p.pos.y, dim, p.type, dir)
		}
	}

	private fun drawPerson(g: AGraphics, x: Float, y: Float, dimension: Float, type: Int, dir: Int) {
		if (dir in 0..3) {
			val animIndex = dir * 4 + frameNumber / 8 % 4
			g.color = GColor.WHITE
			g.drawImage(
				animPeople[type][animIndex],
				(x - dimension / 2),
				(y - dimension / 2),
				dimension,
				dimension
			)
		}
	}

	// -----------------------------------------------------------------------------------------------
	// draw all the people
	private fun drawPeople(g: AGraphics) {
		people.forEach { p ->
			if (p.state < 0) {
				g.color = GColor.WHITE
				drawStickFigure(g, p.pos.x, p.pos.y + random(-2..2), PEOPLE_RADIUS)
			} else {
				drawPerson(g, p)
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Draw a stupid looking stick figure
	private fun drawStickFigure(g: AGraphics, x0: Float, y0: Float, radius: Float) {
		// draw the legs
		g.drawLine(x0, y0, (x0 + radius / 2), (y0 + radius))
		g.drawLine(x0, y0, (x0 - radius / 2), (y0 + radius))
		g.drawLine((x0 - 1), y0, (x0 + radius / 2 - 1), (y0 + radius))
		g.drawLine((x0 - 1), y0, (x0 - radius / 2 - 1), (y0 + radius))
		// draw the body
		val x1 = x0
		val y1 = y0 - radius * 2 / 3
		g.drawLine(x0, y0, x1, y1)
		g.drawLine((x0 - 1), y0, (x1 - 1), y1)

		// draw the arms
		g.drawLine((x1 - radius * 2 / 3), y1, (x1 + radius * 2 / 3), y1)
		g.drawLine((x1 - radius * 2 / 3), (y1 + 1), (x1 + radius * 2 / 3), (y1 + 1))

		// draw the head
		g.drawFilledOval((x1 - radius / 4 - 1), (y1 - radius + 1), (radius / 2), (radius / 2 + 2))
	}

	// -----------------------------------------------------------------------------------------------
	private fun addPlayerMsg(player: Player, msg: String) {
		val v = player.pos.randomized(10f, 20f)
		addMsg(v, msg)
	}

	// -----------------------------------------------------------------------------------------------
	// Add a a message to the table if possible
	private fun addMsg(v: Vector2D, str: String) {
		messages.addOrNull()?.init(v, str, GColor.WHITE)
	}

	// -----------------------------------------------------------------------------------------------
	// update all the messages
	private fun updateAndDrawMessages(g: AGraphics) {
		val iter = messages.iterator()
		while (iter.hasNext()) {
			val msg = iter.next()
			if (!msg.isOnScreen()) {
				iter.remove()
				continue
			}

			g.color = msg.color
			g.drawString(msg.msg, msg.pos)
			msg.pos.subEq(0f, 1f)
			if (frameNumber % MESSAGE_FADE == 0) {
				msg.color = msg.color.darkened(DARKEN_AMOUNT)
				if (msg.color == GColor.BLACK) {
					iter.remove()
					continue
				}
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	// add an explosion if possible
	private fun addParticle(v: Vector2D, type: Int, duration: Int, playerIndex: Int, angle: Int) {
		particles.addOrNull()?.let { p ->
			p.pos.assign(v)
			p.star = random(PARTICLE_STARS.indices)
			p.angle = angle
			p.type = type
			p.duration = duration
			p.start_frame = frameNumber
			p.playerIndex = playerIndex
		}
	}

	// -----------------------------------------------------------------------------------------------
	// update and draw all explosions
	private fun updateAndDrawParticles(g: AGraphics) {
		val iter = particles.iterator()
		while (iter.hasNext()) {
			val p = iter.next()
			var duration = (frameNumber - p.start_frame).coerceAtLeast(0)
			if (duration >= p.duration) {
				iter.remove()
				continue
			} else if (duration < 1) {
				duration = 1
			}
			if (!p.isOnScreen()) {
				iter.remove()
				continue
			}
			val v: Vector2D = p.pos
			when (p.type) {
				PARTICLE_TYPE_BLOOD -> {
					val pd2 = p.duration / 2
					// draw a missile command type expl
					if (duration <= pd2) {
						g.color = GColor.RED
						// draw expanding disk
						val radius = PARTICLE_BLOOD_RADIUS * (duration / pd2).toFloat()
						g.drawFilledCircle(v, radius)
					} else {
						// draw 2nd half of explosion sequence
						val radius = PARTICLE_BLOOD_RADIUS * ((duration - pd2) / pd2).toFloat()
						g.color = GColor.RED
						g.drawFilledCircle(v, PARTICLE_BLOOD_RADIUS)
						g.color = GColor.BLACK
						g.drawFilledCircle(v, radius)
					}
				}

				PARTICLE_TYPE_DYING_ROBOT -> {
					g.color = GColor.DARK_GRAY
					val v_spacing = 2 * (PARTICLE_DYING_ROBOT_DURATION - duration + 1)
					val width = ENEMY_ROBOT_RADIUS * 3 - (PARTICLE_DYING_ROBOT_DURATION - duration) * 4
					var top = -(ENEMY_ROBOT_RADIUS + (PARTICLE_DYING_ROBOT_DURATION - duration))
					val x = p.pos.x
					val y = p.pos.y
					for (i in 0 until 8) {
						g.drawLine((x - width / 2), (y + top), (x + width / 2), (y + top))
						top += v_spacing
					}
				}

				PARTICLE_TYPE_DYING_TANK -> {
					g.color = GColor.RED
					val v_spacing = 2 * (PARTICLE_DYING_ROBOT_DURATION - duration + 1)
					val width = ENEMY_TANK_RADIUS * 3 - (PARTICLE_DYING_ROBOT_DURATION - duration) * 4
					var top = -(ENEMY_TANK_RADIUS + (PARTICLE_DYING_ROBOT_DURATION - duration))
					val x = p.pos.x
					val y = p.pos.y
					for (i in 0 until 8) {
						g.drawLine((x - width / 2), (y + top), (x + width / 2), (y + top))
						top += v_spacing
					}
				}

				PARTICLE_TYPE_PLAYER_STUN -> {
					// we will assume anything else is a stun
					g.color = GColor.WHITE
					val player = players[p.playerIndex]
					val rad = player.radius

					// draw swirling ?
					val px = player.pos.x
					val py = player.pos.y - rad * 2
					val ry = rad * 0.5f
					val deg = p.angle.toFloat()
					val tx = px + rad * CMath.cosine(deg)
					val ty = py + ry * CMath.sine(deg)
					p.angle = p.angle.rotate(25, 360)
					val star = PARTICLE_STARS[p.star]
					g.drawString(star, tx, ty)
				}

				else -> require(false) { "Unhandled case ${p.type}" }
			}
		}
	}

	/**
	 * Compute the distance in maze coordinates between two points
	 * using the path between the 2 cells
	 *
	 * @param c0
	 * @param c1
	 * @return
	 */
	private fun computeCellDistance(c0: IntArray, c1: IntArray): Int {
		return findPath(c0, c1).size
	}


	fun findPath(c0: IntArray, c1: IntArray, path: MutableList<IntArray> = mutableListOf()): MutableList<IntArray> {
		fun findPath_r(cells: Array<IntArray>, cx0: Int, cy0: Int, cx1: Int, cy1: Int): Boolean {
			if (cx0 !in cells.indices || cy0 !in cells[0].indices)
				return false
			var found = false
			if (cx0 == cx1 && cy0 == cy1) {
				found = true
			} else {
				if (cells[cx0][cy0] and WALL_FLAG_VISITED == 0) {
					// mark this cell as visited
					cells[cx0][cy0] = cells[cx0][cy0] or WALL_FLAG_VISITED
					if (cells[cx0][cy0] and WALL_NORTH == 0) {
						found = findPath_r(cells, cx0, cy0 - 1, cx1, cy1)
					}
					if (!found && cells[cx0][cy0] and WALL_SOUTH == 0) {
						found = findPath_r(cells, cx0, cy0 + 1, cx1, cy1)
					}
					if (!found && cells[cx0][cy0] and WALL_WEST == 0) {
						found = findPath_r(cells, cx0 - 1, cy0, cx1, cy1)
					}
					if (!found && cells[cx0][cy0] and WALL_EAST == 0) {
						found = findPath_r(cells, cx0 + 1, cy0, cx1, cy1)
					}
				}
			}
			if (found) {
				path.add(intArrayOf(cx0, cy0))
			}
			return found
		}

		maze_cells.forEach {
			for (i in it.indices) {
				it[i] = it[i] and WALL_FLAG_VISITED.inv()
			}
		}
		if (!findPath_r(maze_cells, c0[0], c0[1], c1[0], c1[1]))
			path.clear()
		return path
	}


	// -----------------------------------------------------------------------------------------------
	// Do enemy hit event on enemy e
	// return true if the source object killed the enemy
	//
	// DO NOT CALL playerHit from this func, inf loop possible!
	private fun enemyHit(player: Player, enemy: Enemy, dv: Vector2D): Boolean {
		if (enemy.type == ENEMY_INDEX_GEN) {
			// spawn a bunch of guys in my place
			val count = random(ENEMY_GEN_SPAWN_MIN..ENEMY_GEN_SPAWN_MAX + gameLevel)
			for (i in 0 until count) {
				addEnemy(enemy.pos.randomized(-10, 10),
					random(ENEMY_INDEX_ROBOT_N..ENEMY_INDEX_ROBOT_W)
				)
			}
			val cell = computeCell(enemy.pos)
			val distGen = computeCellDistance(cell, end_cell)
			val distStart = computeCellDistance(player.start_cell, end_cell)

			// if this is closer to the end than the player start, then make this the start
			//int distGen = Utils.fastLen(enemy.x - end_x, enemy.y - end_x);
			//int distStart = Utils.fastLen(player.start_x - end_x, player.start_y - end_y);
			log.debug("distGen = $distGen, distStart = $distStart")
			if (distGen < distStart) {
				player.start_v.assign(enemy.pos)
			}
			addRandomPowerup(enemy.pos, enemy.radius())
			addPoints(player, ENEMY_GEN_POINTS)
			return true
		} else if (enemy.type <= ENEMY_INDEX_ROBOT_W) {
			addPoints(player, ENEMY_ROBOT_POINTS)
			addParticle(enemy.pos, PARTICLE_TYPE_DYING_ROBOT, 5, -1, 0)
			return true
		} else if (enemy.type <= ENEMY_INDEX_THUG_W) {
			collisionScanCircle(enemy.pos + dv, ENEMY_THUG_RADIUS) ?: run {
				enemy.pos += dv
			}
			return false
		} else if (enemy.type == ENEMY_INDEX_BRAIN) {
			// chance for powerup
			addRandomPowerup(enemy.pos, enemy.radius())
			// spawn some blood
			addPoints(player, ENEMY_BRAIN_POINTS)
			repeat(3) {
				addParticle(enemy.pos.withJitter(ENEMY_BRAIN_RADIUS / 2),
					PARTICLE_TYPE_BLOOD, PARTICLE_BLOOD_DURATION, -1, 0
				)
			}
			return true
		} else if (enemy.type <= ENEMY_INDEX_TANK_NW) {
			addPoints(player, ENEMY_TANK_POINTS)
			addParticle(enemy.pos, PARTICLE_TYPE_DYING_TANK, 8, -1, 0)
			return true
		}
		return false
	}

	// -----------------------------------------------------------------------------------------------
	// draw all enemies
	private fun drawEnemies(g: AGraphics) {
		var debugEnemyDrawn = false
		val (cursor_x, cursor_y) = cursor.get()
		enemies.filter { it.isOnScreen() }.forEachIndexed { enemyIndex, e ->
			val x0 = e.pos.x
			val y0 = e.pos.y
			when (e.type) {
				ENEMY_INDEX_GEN -> drawGenerator(g, x0, y0)
				ENEMY_INDEX_ROBOT_N, ENEMY_INDEX_ROBOT_E, ENEMY_INDEX_ROBOT_S, ENEMY_INDEX_ROBOT_W -> drawRobot(
					g, x0, y0, e.type - ENEMY_INDEX_ROBOT_N
				)

				ENEMY_INDEX_THUG_N, ENEMY_INDEX_THUG_E, ENEMY_INDEX_THUG_S, ENEMY_INDEX_THUG_W -> drawThug(
					g, x0, y0, e.type - ENEMY_INDEX_THUG_N
				)

				ENEMY_INDEX_BRAIN -> drawBrain(g, x0, y0, ENEMY_BRAIN_RADIUS)
				ENEMY_INDEX_ZOMBIE_N, ENEMY_INDEX_ZOMBIE_E, ENEMY_INDEX_ZOMBIE_S, ENEMY_INDEX_ZOMBIE_W -> {
					g.color = GColor.YELLOW
					drawStickFigure(g, x0, y0, ENEMY_ZOMBIE_RADIUS)
				}

				ENEMY_INDEX_TANK_NE, ENEMY_INDEX_TANK_SE, ENEMY_INDEX_TANK_SW, ENEMY_INDEX_TANK_NW -> drawTank(
					g, x0, y0, e.type - ENEMY_INDEX_TANK_NE
				)

				ENEMY_INDEX_JAWS -> {
					var index = frameNumber - e.next_update
					if (index >= animJaws.size) {
						e.next_update = frameNumber + 1
						index = 0
					} else if (index <= 0) {
						index = 0
						players.forEach { player ->
							if (player.pos.distSquaredTo(e.pos) > (PLAYER_RADIUS + 100).squared()) {
								e.next_update = frameNumber + animJaws.size
							}
						}
					}
					drawJaws(g, x0, y0, index)
				}

				ENEMY_INDEX_LAVA -> drawLavaPit(g, x0, y0, e.lavaPitFrame())
			}
			if (!debugEnemyDrawn && isDebugEnabled(Debug.DRAW_ENEMY_INFO)) {
				val r = e.radius()
				val x = x0 - r
				val y = y0 - r
				val w = r * 2
				val h = r * 2
				if (Utils.isPointInsideRect(cursor_x, cursor_y, x, y, w, h)) {
					g.color = GColor.YELLOW
					g.drawOval(x, y, w, h)
					val msg = """
	                	index [$enemyIndex]
	                	radius [${e.radius()}]
	                	type   [${getEnemyTypeString(e.type)}]
	                	""".trimIndent()
					g.drawJustifiedString(x, y, Justify.RIGHT, Justify.TOP, msg)
					debugEnemyDrawn = true
				}
			}
		}
	}

	private fun getEnemyTypeString(enemy_type: Int): String {
		return ENEMY_NAMES[enemy_type]
	}

	// -----------------------------------------------------------------------------------------------
	// draw a robot at x0,y0 facing direction dir [0123] == [NESW]
	private fun drawRobot(g: AGraphics, x0: Float, y0: Float, dir: Int) {
		g.color = GColor.DARK_GRAY
		val walk = 0f + (frameNumber % 12 / 4 - 1)
		if (dir == 0 || dir == 2) {
			// draw head
			g.drawFilledRect((x0 - 8), (y0 - 14), 16f, 12f)
			// draw the arms
			g.drawFilledRect((x0 - 12), (y0 - 6), 4f, 12f)
			g.drawFilledRect((x0 + 8), (y0 - 6), 4f, 12f)
			// draw the body
			g.drawFilledRect((x0 - 6), (y0 - 2), 12f, 4f)
			g.drawFilledRect((x0 - 4), (y0 + 2), 8f, 6f)
			// draw the legs
			g.drawFilledRect((x0 - 6), (y0 + 8), 4f, (8 + walk))
			g.drawFilledRect((x0 + 2), (y0 + 8), 4f, (8 - walk))
			// draw the feet
			g.drawFilledRect((x0 - 8), (y0 + 12 + walk), 2f, 4f)
			g.drawFilledRect((x0 + 6), (y0 + 12 - walk), 2f, 4f)
			// draw the eyes if walking S
			if (dir == 2) {
				g.color = throbbing_white
				g.drawFilledRect((x0 - 4), (y0 - 12), 8f, 4f)
			}
		} else {
			// draw the robot sideways

			// draw the head
			g.drawFilledRect((x0 - 6), (y0 - 14), 12f, 8f)
			// draw the body, eyes ect.
			if (dir == 1) {
				// body
				g.drawFilledRect((x0 - 6), (y0 - 6), 10f, 10f)
				g.drawFilledRect((x0 - 8), (y0 + 4), 14f, 4f)
				// draw the legs
				g.drawFilledRect((x0 - 8), (y0 + 8), 4f, (8 + walk))
				g.drawFilledRect((x0 + 2), (y0 + 8), 4f, (8 - walk))
				// draw feet
				g.drawFilledRect((x0 - 4), (y0 + 12 + walk), 4f, 4f)
				g.drawFilledRect((x0 + 6), (y0 + 12 - walk), 4f, 4f)
				// draw the eyes
				g.color = throbbing_white
				g.drawFilledRect((x0 + 2), (y0 - 12), 4f, 4f)
			} else {
				// body
				g.drawFilledRect((x0 - 4), (y0 - 6), 10f, 10f)
				g.drawFilledRect((x0 - 6), (y0 + 4), 14f, 4f)
				// draw the legs
				g.drawFilledRect((x0 - 6), (y0 + 8), 4f, (8 + walk))
				g.drawFilledRect((x0 + 4), (y0 + 8), 4f, (8 - walk))
				// draw feet
				g.drawFilledRect((x0 - 10), (y0 + 12 + walk), 4f, 4f)
				g.drawFilledRect(x0, (y0 + 12 - walk), 4f, 4f)
				// draw the eyes
				g.color = throbbing_white
				g.drawFilledRect((x0 - 6), (y0 - 12), 4f, 4f)
			}
			// draw the arm
			g.color = GColor.BLACK
			g.drawFilledRect((x0 - 2), (y0 - 6), 4f, 12f)
		}
	}

	// -----------------------------------------------------------------------------------------------
	// draw a generator
	private fun drawGenerator(g: AGraphics, x: Float, y: Float) {
		drawTankGen(g, x, y)
		/*
         int radius;
         // draw a throbbing circle in yellow
          int mod = getFrameNumber() % (ENEMY_GEN_PULSE_FACTOR * 4);
          if (mod < ENEMY_GEN_PULSE_FACTOR) // shrinking
          radius = ENEMY_GEN_RADIUS - mod;
          else if (mod >= 15) // shrinking
          radius = ENEMY_GEN_RADIUS + ENEMY_GEN_PULSE_FACTOR - (mod - ENEMY_GEN_PULSE_FACTOR * 3);
          else
          // expanding
           radius = ENEMY_GEN_RADIUS - ENEMY_GEN_PULSE_FACTOR + (mod - ENEMY_GEN_PULSE_FACTOR);
           Utils.YELLOW.set();
           Utils.drawFilledOval(x - radius, y - radius, radius * 2, radius * 2);*/
	}

	// -----------------------------------------------------------------------------------------------
	// Draw the Evil Brain
	private fun drawBrain(g: AGraphics, x: Float, y: Float, radius: Float) {
		// big head, little arms and legs
		var x0 = x
		var y0 = y
		g.color = GColor.BLUE
		g.translate((x0 - radius), (y0 - radius))
		g.drawFilledPolygon(BRAIN_PTS_X, BRAIN_PTS_Y, BRAIN_PTS_X.size)
		g.color = GColor.RED
		g.drawFilledPolygon(BRAIN_LEGS_X, BRAIN_LEGS_Y, BRAIN_LEGS_X.size)
		g.translate(-(x0 - radius), -(y0 - radius))

		// draw some glowing lines to look like brain nerves
		g.color = throbbing_white
		x0 -= radius
		y0 -= radius
		var i = 1
		for (l in 0..3) {
			for (c in 0 until BRAIN_NERVES_LEN[l] - 1) {
				g.drawLine(
					(x0 + BRAIN_NERVES_X[i - 1]),
					(y0 + BRAIN_NERVES_Y[i - 1]),
					(x0 + BRAIN_NERVES_X[i]),
					(y0 + BRAIN_NERVES_Y[i])
				)
				i++
			}
			i++
		}
		x0 += radius
		y0 += radius
		// draw the eyes
		g.color = GColor.YELLOW
		g.drawFilledRect((x0 - 5), (y0 + 1), 2f, 2f)
		g.drawFilledRect((x0 + 3), y0, 3f, 2f)
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawTankGen(g: AGraphics, x0: Float, y0: Float) {
		val degrees = (frameNumber % 360).toFloat()
		val dx = 5.0f * CMath.cosine(degrees)
		val dy = 5.0f * CMath.sine(degrees)
		g.color = GColor.GREEN
		g.drawRect(
			x0 - ENEMY_TANK_GEN_RADIUS + dx,
			y0 - ENEMY_TANK_GEN_RADIUS + dy,
			ENEMY_TANK_GEN_RADIUS * 2,
			ENEMY_TANK_GEN_RADIUS * 2
		)
		g.color = GColor.BLUE
		g.drawRect(
			x0 - ENEMY_TANK_GEN_RADIUS - dx,
			y0 - ENEMY_TANK_GEN_RADIUS - dy,
			ENEMY_TANK_GEN_RADIUS * 2,
			ENEMY_TANK_GEN_RADIUS * 2
		)
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawTank(g: AGraphics, x0: Float, y0: Float, dir: Int) {
		g.color = GColor.DARK_GRAY
		g.drawFilledRect((x0 - 12), (y0 - 20), 24f, 16f)
		g.color = GColor.RED
		g.translate((x0 - ENEMY_TANK_RADIUS), (y0 - ENEMY_TANK_RADIUS))
		g.drawFilledPolygon(TANK_PTS_X, TANK_PTS_Y, TANK_PTS_X.size)
		g.translate(-(x0 - ENEMY_TANK_RADIUS), -(y0 - ENEMY_TANK_RADIUS))
		g.color = GColor.DARK_GRAY
		g.drawFilledRect((x0 - 12), (y0 - 2), 24f, 4f)
		// draw the wheels
		g.color = GColor.CYAN
		g.drawFilledOval((x0 - 22), (y0 + 6), 12f, 12f)
		g.drawFilledOval((x0 + 10), (y0 + 6), 12f, 12f)
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawJaws(g: AGraphics, x0: Float, y0: Float, index: Int) {
		val jw = 32f
		val jh = 32f
		g.color = GColor.WHITE
		g.drawImage(animJaws[index % animJaws.size], (x0 - jw / 2), (y0 - jh / 2), jw, jh)
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawLavaPit(g: AGraphics, x0: Float, y0: Float, index: Int) {
		if (index < animLava.size) {
			g.color = GColor.WHITE
			g.drawImage(
				animLava[index], (
					x0 - ENEMY_LAVA_DIM / 2), (
					y0 - ENEMY_LAVA_DIM / 2),
				ENEMY_LAVA_DIM,
				ENEMY_LAVA_DIM
			)
		}
	}

	// -----------------------------------------------------------------------------------------------
	// draw a dumb looking thug
	private fun drawThug(g: AGraphics, x0: Float, y0: Float, dir: Int) {
		// draw the body
		g.color = GColor.GREEN
		g.drawFilledRect((x0 - 12), (y0 - 12), 24f, 21f)
		g.color = GColor.RED
		if (dir == 0 || dir == 2) {
			// draw 2 arms at the sides
			g.drawFilledRect((x0 - 15), (y0 - 10), 3f, 15f)
			g.drawFilledRect((x0 + 12), (y0 - 10), 3f, 15f)

			// draw 2 legs
			g.drawFilledRect((x0 - 8), (y0 + 9), 5f, 10f)
			g.drawFilledRect((x0 + 3), (y0 + 9), 5f, 10f)
		} else {
			// draw 1 arm in the middle
			g.drawFilledRect((x0 - 3), (y0 - 10), 6f, 15f)

			// draw 1 leg
			g.drawFilledRect((x0 - 3), (y0 + 9), 6f, 10f)
		}
		// draw the head
		g.color = GColor.BLUE
		g.drawFilledRect((x0 - 5), (y0 - 19), 10f, 7f)
	}

	// -----------------------------------------------------------------------------------------------
	// draw a bar with endpoints p0, p1 with thickness t
	private fun drawBar(g: AGraphics, x0: Float, y0: Float, x1: Float, y1: Float, t: Int, index: Int) {
		val w = abs(x0 - x1) + t * 2
		val h = abs(y0 - y1) + t * 2
		val x = min(x0, x1) - t
		val y = min(y0, y1) - t
		g.drawFilledRect(x, y, w, h)
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawStaticField(g: AGraphics, x: Float, y: Float, radius: Float) {
		var x0 = 1f
		var y0 = 0f
		var r0 = randomFloat(3f) + radius
		val sr = r0
		for (i in 0 until STATIC_FIELD_SECTIONS - 1) {
			val x1 = x0 * STATIC_FIELD_COS_T - y0 * STATIC_FIELD_SIN_T
			val y1 = y0 * STATIC_FIELD_SIN_T + x0 * STATIC_FIELD_COS_T
			val r1 = randomFloatPlusOrMinus(3) + radius
			g.drawLine(x + x0 * r0, y + y0 * r0, x + x1 * r1, y + y1 * r1)
			x0 = x1
			y0 = y1
			r0 = r1
		}
		g.drawLine(x + x0 * r0, y + y0 * r0, x + 1.0f * sr, y + 0 * sr)
	}

	fun Enemy.radius(): Float {
		var radius = ENEMY_RADIUS[type]
		if (type == ENEMY_INDEX_LAVA) {
			//if (this.enemy_spawned_frame)
			val frame = lavaPitFrame()
			if (frame >= animLava.size) {
				return 0f
			}
			// make so the radius follows the expand/contract animation
			val R = (ENEMY_LAVA_DIM / 2)
			val F = animLava.size
			val M = R / F
			val f = frame
			radius = if (f < F / 2) {
				M * f
			} else {
				-M * f + R
			}
		}
		return radius
	}

	// -----------------------------------------------------------------------------------------------
	private fun Enemy.lavaPitFrame(): Int {
		return (frameNumber + spawned_frame) % (animLava.size + ENEMY_LAVA_CLOSED_FRAMES)
	}

	private fun Object.moveIfPossible(v: Vector2D, speed: Float): Wall? {
		val mag = speed / (v.magFast() + 1)
		collisionScanCircle(v, mag)?.let {
			return it
		}
		pos += v * mag
		return null
	}

	// -----------------------------------------------------------------------------------------------
	// update all enemies position and collision
	private fun updateEnemies() {

		// cache the frame number
		val frame_num = frameNumber
		total_enemies = 0
		val iter = enemies.iterator()
		while (iter.hasNext()) {
			val e = iter.next()
			total_enemies++
			if (!e.isOnScreen())
				continue

			// get the radius of this enemy type from the lookup table
			val radius = e.radius()
			players.forEach { player ->
				if (player.state == PLAYER_STATE_ALIVE) {
					// see if we have collided with the player
					if (radius > 0 && player.pos.isWithinDistance(e.pos, player.radius + radius)) {
						playerHit(player, HIT_TYPE_ENEMY, iter.index)
					}
				}
			}

			// see if we have squashed some people
			if (e.type in ENEMY_INDEX_THUG_N..ENEMY_INDEX_THUG_W) {
				val piter = people.iterator()
				while (piter.hasNext()) {
					val p = piter.next()
					if (p.state > 0 && p.isOnScreen() && p.pos.isWithinDistance(e.pos, radius + PEOPLE_RADIUS)) {
						addParticle(p.pos, PARTICLE_TYPE_BLOOD, PARTICLE_BLOOD_DURATION, -1, 0)
						addMsg(e.pos.withJitter(10f), "NOOOO!")
						piter.remove()
					}
				}
			}
			if (e.next_update > frame_num) {
				continue
			}
			when (e.type) {
				ENEMY_INDEX_GEN -> {
					// create a new guy
					val epos = e.pos.withJitter(10f)
					val ed = random(ENEMY_INDEX_ROBOT_N..ENEMY_INDEX_ROBOT_W)
					addEnemy(epos, ed)
					e.next_update = frame_num + 50 + random(-20..20)
				}

				ENEMY_INDEX_ROBOT_N, ENEMY_INDEX_ROBOT_E, ENEMY_INDEX_ROBOT_S, ENEMY_INDEX_ROBOT_W -> {
					players.forEach { player ->
						if (player.state == PLAYER_STATE_ALIVE) {
							if (random(0..4) == 0) {
								// see if a vector from me too player intersects a wall
								collisionScanLine(player.pos, e.pos)?.let {
									e.type = ENEMY_INDEX_ROBOT_N + enemyDirectionHeuristic(e.pos, 1.0f)
								} ?: run {
									e.type =
										ENEMY_INDEX_ROBOT_N + enemyDirectionHeuristic(e.pos, ENEMY_ROBOT_HEURISTIC_FACTOR)
								}
							}
						}
					}
					val v = move_dv[e.type - ENEMY_INDEX_ROBOT_N] * enemy_robot_speed
					collisionScanCircle(e.pos + v, ENEMY_ROBOT_RADIUS)?.let {
						v.scaleEq(-1f)
						if (e.type < ENEMY_INDEX_ROBOT_N + 2) e.type += 2 else e.type -= 2
					}
					e.pos += v
					e.next_update =
						frame_num + (ENEMY_ROBOT_MAX_SPEED + difficulty + 1 - enemy_robot_speed) + random(-3..3)

					// look for lobbing a missile at the player
					if (random(0..400) < ENEMY_PROJECTILE_FREQ + difficulty + gameLevel) {
						players.forEach { player ->
							if (player.state == PLAYER_STATE_ALIVE) {
								val dx = player.pos.x - e.pos.x
								if (abs(dx) > 10 && abs(dx) < ENEMY_ROBOT_ATTACK_DIST + gameLevel * 5) {
									enemyFireMissile(player, e)
								}
							}
						}
					}
				}

				ENEMY_INDEX_THUG_N, ENEMY_INDEX_THUG_E, ENEMY_INDEX_THUG_S, ENEMY_INDEX_THUG_W -> {
					val dv = move_dv[e.type - ENEMY_INDEX_THUG_N] * (ENEMY_THUG_SPEED + gameLevel / 2)
					// see if we will walk into wall
					collisionScanCircle(e.pos + dv, radius)?.let {
						// turn around
						if (e.type < ENEMY_INDEX_THUG_S) e.type += 2 else e.type -= 2
					} ?: run {
						// walk forward
						e.pos += dv
					}
					// roll dice
					if (random(0..5) == 0) {
						// pick a new directiorn
						e.type = ENEMY_INDEX_THUG_N + enemyDirectionHeuristic(e.pos, ENEMY_THUG_HEURISTICE_FACTOR)
					}
					e.next_update = frame_num + ENEMY_THUG_UPDATE_FREQ + random(-2..2)
				}

				ENEMY_INDEX_BRAIN -> {
					if (frameNumber % ENEMY_BRAIN_FIRE_FREQ == 0 && random(0..ENEMY_BRAIN_FIRE_CHANCE) <= (1 + difficulty) * gameLevel)
						addSnakeMissile(e.pos)

					people.filter { it.state != 0 && it.isOnScreen() }.minByOrNull {
						it.pos.sub(e.pos).magFast()
					}?.let { p ->
						if (e.pos.isWithinDistance(p.pos, ENEMY_BRAIN_RADIUS + PEOPLE_RADIUS)) {
							p.state = -1
							e.next_update = frame_num + ENEMY_BRAIN_ZOMBIFY_FRAMES
						} else {
							e.moveIfPossible(p.pos - e.pos, ENEMY_BRAIN_SPEED + gameLevel / 3)
						}
					} ?: run {
						getClosestPlayer(e.pos)?.let { p ->
							e.moveIfPossible(p.pos - e.pos, ENEMY_BRAIN_SPEED + gameLevel / 3)
						}
					}
					e.next_update = frame_num + ENEMY_BRAIN_UPDATE_SPACING - gameLevel / 2 + random(-3..3)
				}

				ENEMY_INDEX_ZOMBIE_N, ENEMY_INDEX_ZOMBIE_E, ENEMY_INDEX_ZOMBIE_S, ENEMY_INDEX_ZOMBIE_W -> {
					e.type = ENEMY_INDEX_ZOMBIE_N + enemyDirectionHeuristic(e.pos, ENEMY_ZOMBIE_HEURISTIC_FACTOR)
					val dv = move_dv[e.type - ENEMY_INDEX_ZOMBIE_N] * ENEMY_ZOMBIE_SPEED
					collisionScanCircle(e.pos + dv, ENEMY_ZOMBIE_RADIUS)?.let { wall ->
						if (wall.isPerimeter()) {
							// dont remove the edge, this is a perimiter edge,
							// reverse direction of zombie
							if (e.type < ENEMY_INDEX_ZOMBIE_S) e.type += 2 else e.type -= 2
						} else wall.type = WALL_TYPE_NONE
					} ?: run {
						addZombieTracer(e.pos)
						e.pos += dv
					}
					e.next_update = frame_num + ENEMY_ZOMBIE_UPDATE_FREQ
				}

				ENEMY_INDEX_TANK_NE, ENEMY_INDEX_TANK_SE, ENEMY_INDEX_TANK_SW, ENEMY_INDEX_TANK_NW -> {
					val dv = move_diag_dv[e.type - ENEMY_INDEX_TANK_NE] * ENEMY_TANK_SPEED
					collisionScanCircle(e.pos + dv, ENEMY_TANK_RADIUS)?.let { wall ->
						if (wall.type != WALL_TYPE_NORMAL || wall.isPerimeter()) {
							// reverse direction of tank
							if (e.type < ENEMY_INDEX_TANK_SW) e.type += 2 else e.type -= 2
						} else {
							wall.type = WALL_TYPE_NONE
						}
					} ?: run {
						e.pos += dv

						// look for changing direction for no reason
						if (random(0..5) == 0) e.type = random(ENEMY_INDEX_TANK_NE..ENEMY_INDEX_TANK_NW)
					}
					e.next_update = frame_num + ENEMY_TANK_UPDATE_FREQ
					if (random(0..ENEMY_TANK_FIRE_FREQ) == 0)
						addTankMissile(e.pos)
				}
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun isPerimeterWall(v0: Int, v1: Int): Boolean {
		return isPerimeterVertex(v0) && isPerimeterVertex(v1)
	}

	private fun getClosestPlayer(v: Vector2D): Player? {
		return players.filter { it.isAlive }.minByOrNull { player ->
			v.sub(player.pos).magFast()
		}
	}

	// -----------------------------------------------------------------------------------------------
	// return 0,1,2,3 [NESW] for a direction.
	// random is a value between 0.0 and 1.0
	// when random == 1.0, then a totally random direction is choosen
	// when random == 0.0, then the direction toward the player is choosen
	private fun enemyDirectionHeuristic(pos: Vector2D, randomChance: Float): Int {
		val randDir = random(0..3) // pick a random direction
		return getClosestPlayer(pos)?.let { player ->
			// figure out the dir to the player
			val dv = player.pos - pos
			val playerDir = getDirection(dv)

			// get a rand value between 0 and 1
			if (randomFloat(1) < randomChance) randDir else playerDir
		} ?: 0
	}

	// -----------------------------------------------------------------------------------------------
	// enemy fires a missile, yeah
	private fun enemyFireMissile(player: Player, e: Enemy) {
		enemy_missiles.addOrNull()?.let { m ->
			m.pos.assign(e.pos)
			val dv = player.pos - e.pos
			m.dv.assign(if (player.pos.x < e.pos.x) 4.0f else 4.0f, -5f)
			m.duration = ENEMY_PROJECTILE_DURATION
		}
	}

	// -----------------------------------------------------------------------------------------------
	// player fires a missile, yeah
	private fun addPlayerMissile(player: Player) {
		if (player.missles.isFull())
			return
		val dv = player.target_dv.normalized() * PLAYER_MISSLE_SPEED
		collisionScanLine(player.pos, player.pos + dv) ?: run {
			player.missles.add().init(player.pos, dv, PLAYER_MISSLE_DURATION)
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun updatePlayerMissiles(player: Player) {
		val playerMissleIter = player.missles.iterator()
		here@ while (playerMissleIter.hasNext()) {
			val m = playerMissleIter.next()
			if (m.duration <= 0) {
				playerMissleIter.remove()
				continue
			}
			if (!m.isOnScreen()) {
				playerMissleIter.remove()
				continue
			}
			m.pos += m.dv
			m.duration--

			// do collision scans and response

			// look for collision with walls
			val info = collisionScanLine(m.pos, m.pos + m.dv)
			if (info != null) {
				val v0 = info.v0()
				val v1 = info.v1()
				val dv = v1 - v0
				var bounceVariation = 0f
				val isPerimiterWall = info.isPerimeter()

				var doBounce = false // flag to indicate bounce the missile
				when (info.type) {
					WALL_TYPE_BROKEN_DOOR,
					WALL_TYPE_DOOR -> {
						if (isMegaGunActive(player) && info.state == DOOR_STATE_LOCKED) {
							val mid = (v0 + v1) / 2f
							if (Utils.isCircleIntersectingLineSeg(m.pos, m.pos + m.dv, mid, 0.25f * PLAYER_MISSLE_SPEED)) {
								addPlayerMsg(player, "LOCK Destroyed")
								info.state = DOOR_STATE_CLOSED
								playerMissleIter.remove()
								continue
							}
						}
						doBounce = true
					}

					WALL_TYPE_NORMAL -> doBounce = if (isMegaGunActive(player)) {
						if (!isPerimiterWall)
							wallNormalDamage(info, 1)
						playerMissleIter.remove()
						continue
					} else {
						true
					}

					WALL_TYPE_ELECTRIC -> {
						if (frameNumber < info.frame) break
						if (isMegaGunActive(player)) {
							info.frame = frameNumber + WALL_ELECTRIC_DISABLE_FRAMES
						}
						playerMissleIter.remove()
						continue
					}

					WALL_TYPE_INDESTRUCTIBLE -> doBounce = true
					WALL_TYPE_PORTAL -> {
						// TODO: Teleport missiles too other portal?
						playerMissleIter.remove()
						continue
					}

					WALL_TYPE_RUBBER -> {
						doBounce = true
						info.frequency = min(info.frequency + RUBBER_WALL_FREQUENCY_INCREASE_MISSILE, RUBBER_WALL_MAX_FREQUENCY)
						bounceVariation = info.frequency * 100
					}

					else -> unhandledCase(info.type)
				}
				if (doBounce) {
					val result = bounceVectorOffWall(m.dv, dv)
					if (bounceVariation != 0f) {
						val degrees = bounceVariation * if (flipCoin()) -1 else 1
						result.rotateEq(degrees)
					}
					m.dv.assign(result)
					// need to check for a collision again
					if (collisionScanLine(m.pos, m.pos + m.dv) != null) {
						playerMissleIter.remove()
						continue
					}
				}
			}

			val enemyIter = enemies.iterator()
			while (enemyIter.hasNext()) {
				val enemy = enemyIter.next()
				if (!enemy.isOnScreen())
					continue
				if (enemy.type == ENEMY_INDEX_JAWS || enemy.type == ENEMY_INDEX_LAVA) {
					continue
				}
				if (Utils.isCircleIntersectingLineSeg(m.pos, m.pos + m.dv, enemy.pos, ENEMY_RADIUS[enemy.type])) {
					var factor = (ENEMY_THUG_PUSHBACK - difficulty.toFloat()).coerceAtLeast(4f) / PLAYER_MISSLE_SPEED
					if (isMegaGunActive(player))
						factor *= 2
					val dv = m.dv * factor
					if (enemyHit(player, enemy, dv)) {
						enemyIter.remove()
					}

					if (!isMegaGunActive(player)) {
						playerMissleIter.remove()
						continue@here
					}
				}
			}

			// look for collisions with enemy_tank_missiles
			val tankMissileIter = tank_missiles.iterator()
			while (tankMissileIter.hasNext()) {
				val m2 = tankMissileIter.next()
				if (!m2.isOnScreen()) {
					continue
				}
				if (m.pos.isWithinDistance(m2.pos, TANK_MISSLE_RADIUS)
					|| m.pos.add(m.dv).isWithinDistance(m2.pos, TANK_MISSLE_RADIUS)
				) {
					playerMissleIter.remove()
					tankMissileIter.remove()
					continue@here
				}
			}

			// look for collisions with snake_missiles
			val mv = m.pos.coerceAtMost(m.pos + m.dv).subEq(2f, 2f)
			val dim = m.dv.abs().add(4f, 4f)
			val snakeMissileIter = snake_missiles.iterator()
			while (snakeMissileIter.hasNext()) {
				val s = snakeMissileIter.next()
				if (!s.isOnScreen()) {
					continue
				}
				val cResult = collisionMissileSnakeRect(s, mv, dim.x, dim.y)
				if (cResult == 0 || isMegaGunActive(player)) {
					s.kill()
					playerMissleIter.remove()
					continue@here
				} else if (cResult > 0) {
					// split the snake?
					// removeSnakeMissile(e);
					addSnakeMissile(mv + dim / 2f)?.let { newSnake ->
						// assign
						var cc = 0
						for (ii in cResult until s.num_sections) {
							newSnake.dir[cc++] = s.dir[ii]
						}
						s.num_sections = cResult
					}
					// The missile lives on!
					// removePlayerMissile(e);
					// done = true;
				}
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	// return true if the wall is destroyed
	private fun wallNormalDamage(info: Wall, amount: Int): Boolean {
		info.health -= amount
		if (info.health <= 0) {
			info.type = WALL_TYPE_NONE
			return true
		}
		return false
	}

	// -----------------------------------------------------------------------------------------------
	private fun updateRobotMissiles() {
		// update enemy missiles, cant collide with walls
		val iter = enemy_missiles.iterator()
		while (iter.hasNext()) {
			val mf = iter.next()

			// remove the missile if below the player, but not too soon
			//if (mf.duration > 30 && mf.y > player_y + playerRadius + 30) {
			//    removeEnemyMissile(i);
			//    continue;
			//}
			if (mf.duration == 0) {
				iter.remove()
				continue
			}
			mf.pos += mf.dv

			// TODO : make gravity more severe when player is close or very
			// below our y
			mf.dv.addEq(0, ENEMY_PROJECTILE_GRAVITY)
			mf.duration--
			players.forEachIndexed { index, player ->
				val playerRadius = player.radius
				if (isBarrierActive(player)) {
					val dv1 = mf.pos - player.pos
					val dot1 = dv1.dot(mf.pos)
					if (dot1 <= 0 && mf.pos.isWithinDistance(player.pos, PLAYER_RADIUS_BARRIER + ENEMY_PROJECTILE_RADIUS)) {
						mf.dv.scaleEq(-1, -1)
					}
				} else if (mf.pos.isWithinDistance(player.pos, playerRadius + ENEMY_PROJECTILE_RADIUS)) {
					playerHit(player, HIT_TYPE_ROBOT_MISSLE, index)
				}
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun updateTankMissiles() {
		val iter = tank_missiles.iterator()
		here@ while (iter.hasNext()) {
			val m = iter.next()
			if (m.duration == 0) {
				iter.remove()
				continue
			}
			if (!m.isOnScreen()) {
				iter.remove()
				continue
			}
			for (i in players.indices) {
				val player = players[i]
				val playerRadius = player.radius
				if (isBarrierActive(player)) {
					val dv1 = m.pos - player.pos
					val dot1 = dv1.dot(m.pos)
					if (dot1 <= 0 && m.pos.isWithinDistance(player.pos, PLAYER_RADIUS_BARRIER + TANK_MISSLE_RADIUS)) {
						m.dv.subEq(-1, -1)
					}
				} else if (m.pos.isWithinDistance(player.pos, playerRadius + TANK_MISSLE_RADIUS)) {
					playerHit(player, HIT_TYPE_TANK_MISSLE, i)
					continue@here
				}
			}

			// look for collision with walls
			collisionScanCircle(m.pos + m.dv, TANK_MISSLE_RADIUS)?.let { info ->
				// do the bounce off algorithm
				m.dv.assign(bounceVectorOffWall(m.dv, info.v1() - info.v0()))
			}
			m.pos += m.dv
			m.duration--
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun updateMissiles() {
		players.forEachIndexed { id, player ->
			updatePlayerMissiles(player)
			server?.broadcastPlayerMissiles(id, player.missles)
		}
		updateRobotMissiles()
		updateTankMissiles()
		players.forEach {
			updateSnakeMissiles(it)
		}
		server?.broadcastEnemyMissiles(enemy_missiles, tank_missiles, snake_missiles)
	}

	// -----------------------------------------------------------------------------------------------
	// draw all missiles to the screen
	private fun drawPlayerMissiles(player: Player, g: AGraphics, colors: ColorSet) {
		var thickness = 1f
		if (isMegaGunActive(player)) {
			g.color = GColor.CYAN
			thickness = 2f
		} else {
			g.color = colors.gun
		}

		player.missles.forEach { m ->
			g.drawLine(m.pos, m.pos + m.dv, thickness)
		}
	}

	// -----------------------------------------------------------------------------------------------
	// draw all missiles to the screen
	private fun drawEnemyMissiles(g: AGraphics) {
		// Draw the enemy missiles as orange dots
		g.color = GColor.ORANGE
		enemy_missiles.forEach { m ->
			g.drawFilledCircle(m.pos, ENEMY_PROJECTILE_RADIUS)
		}
		tank_missiles.forEach { m ->
			g.color = GColor.GREEN
			g.drawCircle(m.pos, TANK_MISSLE_RADIUS)
			g.color = GColor.YELLOW
			g.drawCircle(m.pos.add(2, 2), TANK_MISSLE_RADIUS - 2)
		}
	}

	// -----------------------------------------------------------------------------------------------
	// draw all missiles to the screen
	private fun drawMissiles(g: AGraphics) {
		players.forEachIndexed { idx, pl ->
			drawPlayerMissiles(pl, g, PLAYER_COLORS[idx])
		}
		drawEnemyMissiles(g)
		drawSnakeMissiles(g)
	}

	// -----------------------------------------------------------------------------------------------
	// Update the player powerup
	private fun updatePlayerPowerup(player: Player) {
		if (player.powerup_duration++ > PLAYER_POWERUP_DURATION
			&& (player.powerup != POWERUP_GHOST || collisionScanCircle(player.pos, player.radius) == null)
		) {
			player.powerup = -1
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun isStunned(player: Player): Boolean {
		if (player.state == PLAYER_STATE_SPECTATOR) return false
		return player.stun_dv.magSquared() > 0.1f
	}

	// -----------------------------------------------------------------------------------------------
	private fun setPlayerStunned(playerIndex: Int, dv: Vector2D, force: Float) {
		val player = players[playerIndex]
		player.stun_dv.assign(dv.normalized() * force)
		player.hulk_charge_frame = 0
		var angle = 0
		while (angle < 360) {
			addParticle(player.pos, PARTICLE_TYPE_PLAYER_STUN, random(10..20), playerIndex, angle)
			angle += 45
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun playerTouchDoor(player: Player, door: Wall) {
		when (door.state) {
			DOOR_STATE_LOCKED -> if (!isHulkActive(player) && player.keys > 0) {
				addPlayerMsg(player, "UNLOCKING DOOR")
				door.state = DOOR_STATE_OPENING
				door.frame = 0
				player.keys--
			}

			DOOR_STATE_CLOSING -> {
				door.state = DOOR_STATE_OPENING
				door.frame = 0
			}

			DOOR_STATE_CLOSED -> {
				door.state = DOOR_STATE_OPENING
				door.frame = 0
			}

			DOOR_STATE_OPENING -> {
			}

			DOOR_STATE_OPEN -> {
			}

			else -> Utils.unhandledCase(door.state)
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Update the player
	private fun updatePlayer(player: Player) {
		val playerRadius = player.radius
		Arrays.fill(player.barrier_electric_wall, -1f)
		if (player.next_state_frame <= frameNumber) {
			when (player.state) {
				PLAYER_STATE_TELEPORTED -> {
					player.motion_dv.zeroEq()
					player.state = PLAYER_STATE_ALIVE
				}

				PLAYER_STATE_SPAWNING -> player.state = PLAYER_STATE_ALIVE
				PLAYER_STATE_EXPLODING -> if (player.lives > 0) {
					player.lives--
					player.reset(frameNumber)
					player.state = PLAYER_STATE_SPAWNING
				} else {
					player.state = PLAYER_STATE_SPECTATOR
				}
			}
		}

		// see if the player has changed the cell location
		val sx = player.cur_cell[0]
		val sy = player.cur_cell[1]
		computeCell(player.pos, player.cur_cell)
		if (player.cur_cell[0] != sx || player.cur_cell[1] != sy) {
			player.path.clear()
			findPath(player.cur_cell, end_cell, player.path)
		}

		// look for player trying to fire a missile
		if (player.state == PLAYER_STATE_ALIVE && player.firing) {
			if (player.last_shot_frame + getPlayerShotFreq(player) < frameNumber) {
				player.last_shot_frame = frameNumber
				if (isHulkActive(player)) {
					// charge!
					playerHulkCharge(player)
				} else {
					addPlayerMissile(player)
				}
			}
		}
		if (player.powerup >= 0)
			updatePlayerPowerup(player)
		if (isHulkActive(player)) {
			if (player.scale < PLAYER_HULK_SCALE) player.scale += PLAYER_HULK_GROW_SPEED
		} else if (player.scale > 1.0f) {
			player.scale -= PLAYER_HULK_GROW_SPEED
		}
		player.scale = player.scale.coerceIn(1.0f, PLAYER_HULK_SCALE)
		if (player.state != PLAYER_STATE_EXPLODING) {
			val dv: MutableVector2D = if (isStunned(player)) {
				player.stun_dv *= 0.9
				if (player.stun_dv.magSquared() < 0.1) {
					player.stun_dv.normEq().setY(0)
				}
				player.stun_dv
			} else {
				player.motion_dv
			}

			if (dv.magSquared() > 1) {
				if (GAME_VISIBILITY)
					updatePlayerVisibility(player)
				player.movement.rotate(1000)
				player.dir = getPlayerDir(dv)
				val pv = player.pos + dv

				// do collision detect against walls
				// working good
				var wall = collisionScanCircle(pv, playerRadius)
				if (wall?.isPerimeter() == false && canPassThroughWalls(player))
					wall = null
				wall?.let { info ->
					//log.debug("Player hit wall type [" + getWallTypeString(info.type) + "] info [" + info + "]")
					//if (info.type == WALL_TYPE_ELECTRIC && isBarrierActive(player)) {
					// no collision
					//} else {
					if (doPlayerHitWall(player, info, dv))
						return
					val wallv0 = info.near()
					val wallv1 = info.far()
					fixPositionFromWall(pv, wallv0, wallv1, playerRadius)

					// now search the other walls
					for (it in wall_candidates.filter { it != info && it.isActive() }) {
						// val wallv = it.far()
						if (collisionWallCircle(it, pv, playerRadius)) {
							if (doPlayerHitWall(player, info, dv))
								return

							// check the dot product of orig wall and this wall
							val wf = it.far()
							val dv0 = wallv1 - wallv0
							val dv1 = wf - wallv0
							val dot = dv0.dot(dv1)
							if (dot > 0)
								return
							fixPositionFromWall(pv, wallv0, wf, playerRadius)
							break
						}
					}
				}
				player.target_dv.assign(pv - player.pos)
				if (frameNumber % 4 == 0) {
					if (isHulkActiveCharging(player)) {
						addPlayerTracer(player, player.pos, player.dir, GColor.GREEN)
					} else if (player.powerup == POWERUP_SUPER_SPEED) {
						addPlayerTracer(player, player.pos, player.dir, GColor.YELLOW)
					}
				}
				player.pos.assign(pv)
				if (
					enemy_robot_speed < ENEMY_ROBOT_MAX_SPEED + difficulty &&
					player.movement % (ENEMY_ROBOT_SPEED_INCREASE - difficulty * 10) == 0
				) enemy_robot_speed++
			} else {
				player.movement = 0
			}
		}

		val margin = 25f

		val t =
			player.pos.sub(screen_width / 2, screen_height / 2).coerceIn(verts_min_v.sub(margin, margin), verts_max_v.add(margin, margin))
		player.screen.setTopLeftPosition(t)

		if (game_type == GAME_TYPE_CLASSIC) {
			if (total_enemies == 0 && particles.isEmpty()) {
				nextLevel()
			}
		} else {
			val end = cellToMazeCoords(end_cell)
			if (player.pos.isWithinDistance(end, playerRadius + 10)) {
				nextLevel()
			}
		}
	}

	fun nextLevel() {
		frameNumber = 0
		game_start_frame
		gameLevel++
		buildAndPopulateLevel()
	}

	fun prevLevel() {
		gameLevel = (gameLevel - 1).coerceAtLeast(1)
		buildAndPopulateLevel()
	}

	private fun getPlayerIndex(player: Player): Int = players.indexOf(player)

	// -----------------------------------------------------------------------------------------------
	// return true when the collision is a hit, false if we should
	// fix the position
	private fun doPlayerHitWall(player: Player, info: Wall, dv: Vector2D): Boolean {
		val playerIndex = getPlayerIndex(player)
		when (info.type) {
			WALL_TYPE_ELECTRIC -> {
				return playerHit(player, HIT_TYPE_ELECTRIC_WALL, 0)
			}

			WALL_TYPE_PORTAL -> {
				if (!isHulkActive(player))
					return playerTeleport(player, info.portalId)
			}

			WALL_TYPE_DOOR -> {
				playerTouchDoor(player, info)
				return false
			}

			WALL_TYPE_NORMAL -> {
				if (isHulkActiveCharging(player)) {
					// do damage on the wall
					val damage = random((WALL_NORMAL_HEALTH / 4) + 5)
					if (wallNormalDamage(info, damage)) return true
					setPlayerStunned(playerIndex, -dv, 10f)
				}
				return false
			}

			WALL_TYPE_RUBBER -> {
				setPlayerStunned(playerIndex, -dv, 10f)
				return false
			}

			else -> return false
		}
		return true
	}

	// -----------------------------------------------------------------------------------------------
	private fun fixPositionFromWall(pos: MutableVector2D, v0: Vector2D, v1: Vector2D, radius: Float) {
		// get distanse^2 too wall
		val d2 = Utils.distSqPointSegment(pos.x, pos.y, v0.x, v0.y, v1.x, v1.y)

		// compute new dx,dy along collision edge
		val dv = v0 - v1

		val nv = dv.norm()

		// compute vector from w0 too p
		val pwdv = pos - v0

		// compute dot of pd n
		val dot = nv.dot(pwdv)

		// reverse direction of normal if neccessary
		if (dot < 0) {
			nv.scaleEq(-1)
		}

		// normalize the normal
		nv.normalizedEq()

		// compute vector along n to adjust p
		val d_ = radius - sqrt(d2)
		val nv_ = nv * d_

		// compute p prime, the new position, a correct distance
		// from the wall
		//val pv_ = pos + nv_

		// reassign
		pos += nv_ //.assign(pv_)
	}

	fun Float.isZero(): Boolean = abs(this) < EPSILON

	fun Float.isNotZero(): Boolean = abs(this) >= EPSILON

	// -----------------------------------------------------------------------------------------------
	private fun playerHulkCharge(player: Player) {
		player.hulk_charge_frame = frameNumber

		//addPlayerMsg("HULK SMASH!");
		player.firing = false

		// if player is currently moving, then their speed is automatically scaled
		if (player.motion_dv.isZero) {
			val speed = player.speed()
			when (player.dir) {
				DIR_UP -> player.motion_dv.assign(0, -speed)
				DIR_RIGHT -> player.motion_dv.assign(speed, 0)
				DIR_DOWN -> player.motion_dv.assign(0, speed)
				DIR_LEFT -> player.motion_dv.assign(-speed, 0)
				else -> Utils.unhandledCase(player.dir)
			}
		}
	}

	private fun isHulkActiveCharging(player: Player): Boolean {
		return isHulkActive(player) && frameNumber - player.hulk_charge_frame < PLAYER_HULK_CHARGE_FRAMES
	}

	// -----------------------------------------------------------------------------------------------
	// make so teleporting from opposite sides of a wall is symmetrical
	private fun enemyTeleport(e: Enemy, wv0: Int, wv1: Int) {
		val radius = e.radius()
		val n0 = maze_verts[wv0] + maze_verts[wv1]

		val v0 = maze_verts[wv0]
		val v1 = maze_verts[wv1]
		val mp = v0.midPoint(v1)

		val dv = v1 - v0
		// normal
		val n1 = dv.norm()
		if (n1.dot(n0) > 0)
			n1.scaleEq(-1f)
		val len = dv.magFast().coerceAtLeast(1f)
		e.pos.assign(mp + (n1 * radius / len))
	}

	// -----------------------------------------------------------------------------------------------
	// when teleporting
	private fun playerTeleport(player: Player, targetId: Int): Boolean {
		val wall = getWallById(targetId) ?: return false
		log.debug("PLAYER TELEPORT $targetId")
		player.state = PLAYER_STATE_TELEPORTED
		player.next_state_frame = frameNumber + 1
		val radius = player.radius + 10
		// get wall defined by v0, v1

		// get player normal to current wall
		val dot0 = getWallById(wall.portalId)?.let {
			it.dv().normEq().dot(it.v0() - player.pos)
		} ?: return false

		val n = wall.dv().normEq()
		val dot1 = wall.dv().normEq().dot(wall.v0() - player.pos)

		log.debug("dot0: $dot0, dot1: $dot1")

		if (dot1.sign != dot0.sign)
			n.scaleEq(-1f)
		val len = (n.magFast() - 1).coerceAtLeast(1f)

		// make the player's motion change to the closest to that of the normal
		player.motion_dv.zeroEq()
		n.scaleEq(radius / len)
		player.pos.assign(wall.mid().addEq(n))
		return true
	}

	private fun getPlayerShotFreq(player: Player): Int {
		return if (isMegaGunActive(player)) PLAYER_SHOT_FREQ_MEGAGUN else PLAYER_SHOT_FREQ
	}

	// -----------------------------------------------------------------------------------------------
	// Player hit event
	// return true if player takes damage, false if barrier ect.
	private fun playerHit(player: Player, hitType: Int, index: Int): Boolean {
		if (isInvincible(player)) return false

		//final float playerRadius = player.radius;
		val playerIndex = getPlayerIndex(player)
		when (hitType) {
			HIT_TYPE_ELECTRIC_WALL -> return if (isHulkActive(player)) {
				setPlayerStunned(playerIndex, -player.motion_dv, 30f)
				false
			} else if (isBarrierActive(player)) {
				false
			} else {
				player.state = PLAYER_STATE_EXPLODING
				player.next_state_frame = frameNumber + PLAYER_DEATH_FRAMES
				true
			}

			HIT_TYPE_ENEMY -> {
				if (isHulkActive(player) || player.scale > 1.2f) {
					if (isHulkActiveCharging(player) && enemyHit(player, enemies[index], player.motion_dv)) {
						if (random(0..6) == 0) addPlayerMsg(player, "HULK SMASH!")
					} else {
						if (random(10) == 0) {
							addPlayerMsg(player, "Hulk confused ???")
						}
						setPlayerStunned(playerIndex, player.pos - enemies[index].pos, ENEMY_ROBOT_FORCE)
					}
					return false
				}
				if (isHulkActive(player)) {
					// unhulk
					setDebugEnabled(Debug.HULK, false)
					if (player.powerup == POWERUP_HULK) player.powerup = -1
					// bounce the missile
					enemy_missiles[index].dv *= -1
					// stun player
					setPlayerStunned(playerIndex, enemy_missiles[index].dv, ENEMY_PROJECTILE_FORCE)
					return false
				} else if (!isGhostActive(player)) {
					player.hit_type = hitType
					player.hit_index = index
					player.state = PLAYER_STATE_EXPLODING
					player.next_state_frame = frameNumber + PLAYER_DEATH_FRAMES
					return true
				}
			}

			HIT_TYPE_TANK_MISSLE, HIT_TYPE_ROBOT_MISSLE -> if (isHulkActive(player)) {
				setDebugEnabled(Debug.HULK, false)
				if (player.powerup == POWERUP_HULK) player.powerup = -1
				enemy_missiles[index].dv *= -1
				setPlayerStunned(playerIndex, enemy_missiles[index].dv, ENEMY_PROJECTILE_FORCE)
				return false
			} else if (!isGhostActive(player)) {
				player.hit_type = hitType
				player.hit_index = index
				player.state = PLAYER_STATE_EXPLODING
				player.next_state_frame = frameNumber + PLAYER_DEATH_FRAMES
				return true
			}

			HIT_TYPE_SNAKE_MISSLE -> // TODO: Snake is attached to player, slowing player down
				//snake_missile[index].state = SNAKE_STATE_ATTACHED;
				return true

			else -> Utils.unhandledCase(hitType)
		}
		return false
	}

	// -----------------------------------------------------------------------------------------------
	//boolean isElectricWallActive(WallInfo info) {
	//  return info.frame < getFrameNumber();
	//}
	// -----------------------------------------------------------------------------------------------
	private fun isInvincible(player: Player): Boolean {
		return isDebugEnabled(Debug.INVINCIBLE)
	}

	// -----------------------------------------------------------------------------------------------
	private fun isGhostActive(player: Player): Boolean {
		return isDebugEnabled(Debug.GHOST) || player.powerup == POWERUP_GHOST
	}

	private fun canPassThroughWalls(player: Player): Boolean {
		return isGhostActive(player) || isSpectator(player)
	}

	// -----------------------------------------------------------------------------------------------
	private fun isSpectator(player: Player): Boolean {
		return player.state == PLAYER_STATE_SPECTATOR
	}

	// -----------------------------------------------------------------------------------------------
	private fun isBarrierActive(player: Player): Boolean {
		return isDebugEnabled(Debug.BARRIER) || player.powerup == POWERUP_BARRIER
	}

	// -----------------------------------------------------------------------------------------------
	private fun isMegaGunActive(player: Player): Boolean {
		return player.powerup == POWERUP_MEGAGUN
	}

	// -----------------------------------------------------------------------------------------------
	private fun isHulkActive(player: Player): Boolean {
		return isDebugEnabled(Debug.HULK) || player.powerup == POWERUP_HULK
	}

	// -----------------------------------------------------------------------------------------------
	// return 0,1,2,3 for player direction [NESW]
	private fun getPlayerDir(dv: Vector2D): Int {
		if (dv.x < 0) return DIR_LEFT
		if (dv.x > 0) return DIR_RIGHT
		return if (dv.y < 0) DIR_UP else DIR_DOWN
	}

	// -----------------------------------------------------------------------------------------------
	// Draw the Player
	private fun drawPlayer(player: Player, g: AGraphics, px: Float, py: Float, dir: Int, colors: ColorSet) {
		val factor = 1f / (PLAYER_HULK_SCALE - 1f)
		val color = colors.normal.interpolateTo(colors.hulk, (player.scale - 1f) * factor)
		if (player.powerup > 0 && player.powerup_duration > PLAYER_POWERUP_DURATION - PLAYER_POWERUP_WARNING_FRAMES) {
			if (frameNumber % 8 < 4)
				color.invert()
		}
		drawPlayerBody(player, g, px + 1, py, dir, color)
		drawPlayerEyes(player, g, px + 1, py, dir)
		drawPlayerBarrier(player, g, px + 1, py)
		if (isDebugEnabled(Debug.DRAW_PLAYER_INFO)) {
			g.color = GColor.BLUE
			g.drawRect((px - 1), (py - 1), 3f, 3f)
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Draw the Player's Body
	// TODO: Avoid using new by caching all the colors we want
	private fun drawPlayerBody(player: Player, g: AGraphics, px: Float, py: Float, dir: Int, color: GColor) {
		var priColor = color
		var secColor = color.darkened(.7f)
		if (isGhostActive(player)) {
			priColor = priColor.withAlpha(100)
			secColor = priColor
		}
		val scale = player.scale
		g.color = priColor
		val walk = ((player.movement % 3 - 1) * scale)
		val f1 = 1.0f * scale
		val f2 = 2.0f * scale
		val f3 = 3.0f * scale
		val f4 = 4.0f * scale
		val f6 = 6.0f * scale
		val f8 = 8.0f * scale
		val f10 = 10.0f * scale
		val f12 = 12.0f * scale
		val f14 = 14.0f * scale
		val f16 = 16.0f * scale
		val f20 = 20.0f * scale
		val f22 = 22.0f * scale
		val f24 = 24.0f * scale
		// draw head
		g.drawFilledRect((px - f10), (py - f10), f20, f4)
		g.drawFilledRect((px - f8), (py - f12), f16, f8)
		// draw body
		g.drawFilledRect((px - f4), (py - f14), f8, f22)
		when (dir) {
			DIR_UP, DIR_DOWN -> {
				g.drawFilledRect((px - f6), (py - f2), f12, f6)
				g.drawFilledRect((px - f12), py, f24, f4)
				// draw arms
				g.drawFilledRect((px - f12), (py + f4), f4, f6)
				g.drawFilledRect((px + f8), (py + f4), f4, f6)
				// draw legs
				g.drawFilledRect((px - f6), (py + f6 + walk), f4, f10)
				g.drawFilledRect((px + f2), (py + f6 - walk), f4, f10)
				g.drawFilledRect((px - f8), (py + f12 + walk), f2, f4)
				g.drawFilledRect((px + f6), (py + f12 - walk), f2, f4)
			}

			DIR_RIGHT -> {
				// body
				g.drawFilledRect((px - f6), (py - f2), f10, f10)
				// legs
				g.drawFilledRect((px - f6), (py + f8 + walk), f4, f8)
				g.drawFilledRect((px - f2), (py + f12 + walk), f2, f4)
				g.color = secColor
				g.drawFilledRect((px + f1), (py + f8 - walk), f4, f8)
				g.drawFilledRect((px + f3), (py + f12 - walk), f2, f4)
				// arm
				g.color = priColor
				g.drawFilledRect((px - f4), (py - f2), f4, f6)
				g.drawFilledRect((px - f2), (py + f2), f4, f4)
				g.drawFilledRect(px, (py + f4), f4, f4)
			}

			DIR_LEFT -> {
				// body
				g.drawFilledRect((px - f4), (py - f2), f10, f10)
				// legs
				g.drawFilledRect((px + f2), (py + f8 + walk), f4, f8)
				g.drawFilledRect(px, (py + f12 + walk), f2, f4)
				g.color = secColor
				g.drawFilledRect((px - f6), (py + f6 - walk), f4, f8)
				g.drawFilledRect((px - f8), (py + f10 - walk), f2, f4)
				// arm
				g.color = priColor
				g.drawFilledRect(px, (py - f2), f4, f6)
				g.drawFilledRect((px - f2), (py + f2), f4, f4)
				g.drawFilledRect((px - f4), (py + f4), f4, f4)
			}
		}

		// yell: "HULK SMASH!" when run over people, robot and walls (not thugs,
		// they bump back)
		// also, cant shoot when hulk
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawPlayerEyes(player: Player, g: AGraphics, px: Float, py: Float, dir: Int) {
		val scale = player.scale

		//int f1 = 1.0f * scale;
		val f2 = 2.0f * scale
		//int f3 = 3.0f * scale;
		val f4 = 4.0f * scale
		//int f6 = 6.0f * scale;
		val f8 = 8.0f * scale
		val f10 = 10.0f * scale
		//int f12 = 12.0f * scale;
		//int f14 = 14.0f * scale;
		val f16 = 16.0f * scale
		//int f20 = 20.0f * scale;
		//int f22 = 22.0f * scale;
		val f24 = 24.0f * scale
		if (dir == 2) {
			// draw the eye
			g.color = GColor.BLACK
			g.drawFilledRect((px - f8), (py - f10), f16, f4)
			g.color = GColor.RED
			var index = frameNumber % 12
			if (index > 6)
				index = 12 - index
			g.drawFilledRect((px - f8 + index * f2), (py - f10), f4, f4)
		} else if (dir == 1) {
			g.color = GColor.BLACK
			g.drawFilledRect(px, (py - f10), f10, f4)
			g.color = GColor.RED
			val index = frameNumber % 12
			if (index < 4) g.drawFilledRect(
				(px + index * f2),
				(py - f10),
				f4,
				f4
			) else if (index >= 8) g.drawFilledRect((px + (f24 - index * f2)), (py - f10), f4, f4)
		} else if (dir == 3) {
			g.color = GColor.BLACK
			g.drawFilledRect((px - f10), (py - f10), f10, f4)
			g.color = GColor.RED
			val index = frameNumber % 12
			if (index < 4) g.drawFilledRect(
				(px - f10 + index * f2),
				(py - f10),
				f4,
				f4
			) else if (index >= 8) g.drawFilledRect((px - f10 + (f24 - index * f2)), (py - f10), f4, f4)
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawPlayerBarrierElectricWall2(player: Player, g: AGraphics, x: Float, y: Float) {

		// we are touching an electric wall, so we become 1 unit.
		// the end points of the wall are in the array:
		val wx0 = player.barrier_electric_wall[0]
		val wy0 = player.barrier_electric_wall[1]
		val wx1 = player.barrier_electric_wall[2]
		val wy1 = player.barrier_electric_wall[3]

		// compute deltas betwen p and endpofloats
		val dx0 = (x - wx0)
		val dy0 = (y - wy0)
		val dx1 = (x - wx1)
		val dy1 = (y - wy1)
		val radius = PLAYER_RADIUS_BARRIER
		for (c in 0..1) {
			var x0 = 1f
			var y0 = 0f
			var r0 = Utils.randFloatPlusOrMinus(3f) + radius
			val sr = r0
			for (i in 0 until STATIC_FIELD_SECTIONS - 1) {
				val x1 = x0 * STATIC_FIELD_COS_T - y0 * STATIC_FIELD_SIN_T
				val y1 = y0 * STATIC_FIELD_SIN_T + x0 * STATIC_FIELD_COS_T
				val r1 = Utils.randFloatPlusOrMinus(3f) + radius
				val lx0 = x0 * r0
				val ly0 = y0 * r0
				val lx1 = x1 * r1
				val ly1 = y1 * r1
				g.drawLine((x + lx0), (y + ly0), (x + lx1), (y + ly1))
				if (random(5) == 0) {
					val dot0 = lx0 * dx0 + ly0 * dy0
					val dot1 = lx0 * dx1 + ly0 * dy1
					if (dot0 <= 0) {
						drawElectricWall_r(g, x + lx0, y + ly0, wx0, wy0, 2)
					}
					if (dot1 <= 0) {
						drawElectricWall_r(g, x + lx0, y + ly0, wx1, wy1, 2)
					}
				}
				x0 = x1
				y0 = y1
				r0 = r1
			}
			val lx0 = x0 * r0
			val ly0 = y0 * r0
			val lx1 = sr
			val ly1 = 0f
			g.drawLine((x + lx0), (y + ly0), (x + lx1), (y + ly1))
			val dot0 = lx0 * dx0 + ly0 * dy0
			val dot1 = lx0 * dx1 + ly0 * dy1
			if (dot0 <= 0) {
				drawElectricWall_r(g, x + lx0, y + ly0, wx0, wy0, 2)
			}
			if (dot1 <= 0) {
				drawElectricWall_r(g, x + lx0, y + ly0, wx1, wy1, 2)
			}
			g.drawLine((x + lx0), (y + ly0), (x + lx1), (y + ly1))
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawPlayerBarrierElectricWall3(player: Player, g: AGraphics, x: Float, y: Float) {

		// we are touching an electric wall, so we become 1 unit.
		// the end points of the wall are in the array:
		val wx0 = player.barrier_electric_wall[0]
		val wy0 = player.barrier_electric_wall[1]
		val wx1 = player.barrier_electric_wall[2]
		val wy1 = player.barrier_electric_wall[3]
		val radius = PLAYER_RADIUS_BARRIER
		for (c in 0..1) {
			var x0 = 1f
			var y0 = 0f
			var r0 = Utils.randFloatPlusOrMinus(3f) + radius
			val sr = r0

			// dest point of field to connect wall pts to
			var bx0 = 0f
			var by0 = 0f
			var bx1 = 0f
			var by1 = 0f

			// closest static field pt
			var bestDot0 = Float.MAX_VALUE
			var bestDot1 = Float.MAX_VALUE
			for (i in 0 until STATIC_FIELD_SECTIONS) {
				val x1 = x0 * STATIC_FIELD_COS_T - y0 * STATIC_FIELD_SIN_T
				val y1 = y0 * STATIC_FIELD_SIN_T + x0 * STATIC_FIELD_COS_T
				var r1 = Utils.randFloatPlusOrMinus(3f) + radius
				if (i == STATIC_FIELD_SECTIONS - 1) {
					r1 = sr
				}
				val lx0 = x + x0 * r0
				val ly0 = y + y0 * r0
				val lx1 = x + x1 * r1
				val ly1 = y + y1 * r1
				g.drawLine(lx0, ly0, lx1, ly1)
				val dx0 = lx0 - wx0
				val dy0 = ly0 - wy0
				val dx1 = lx1 - wx1
				val dy1 = ly1 - wy1
				val dot0 = (dx0 * dx0 + dy0 * dy0)
				val dot1 = (dx1 * dx1 + dy1 * dy1)
				if (dot0 < bestDot0) {
					bestDot0 = dot0
					bx0 = lx0
					by0 = ly0
				}
				if (dot1 < bestDot1) {
					bestDot1 = dot1
					bx1 = lx0
					by1 = ly0
				}
				x0 = x1
				y0 = y1
				r0 = r1
			}
			//drawElectricWall_r(g, bx0, by0, wx0, wy0, 2)
			drawElectricWall_r(g, bx1, by1, wx1, wy1, 2)
		}
	}

	// this version draw bezier curves around the player, looks ok, could be better
	private fun drawPlayerBarrierElectricWall1(player: Player, g: AGraphics, px: Float, py: Float) {
		// we are touching an electric wall, so we become 1 unit.
		// the end points of the wall are in the array:
		val wx0 = player.barrier_electric_wall[0]
		val wy0 = player.barrier_electric_wall[1]
		val wx3 = player.barrier_electric_wall[2]
		val wy3 = player.barrier_electric_wall[3]

		// compute deltas betwen p and endpoints
		val dx0 = px - wx0
		val dy0 = py - wy0
		val dx1 = px - wx3
		val dy1 = py - wy3

		// get perp to delta
		var nx0 = -dy0
		var ny0 = dx0
		var nx1 = -dy1
		var ny1 = dx1

		// get length of normals
		val d0 = sqrt(nx0 * nx0 + ny0 * ny0)
		val d1 = sqrt(nx1 * nx1 + ny1 * ny1)
		if (d0 > 0.01f && d1 > 0.01f) {
			val d0_inv = 1.0f / d0
			val d1_inv = 1.0f / d1
			val radius = PLAYER_RADIUS_BARRIER
			nx0 *= d0_inv * radius
			ny0 *= d0_inv * radius
			nx1 *= d1_inv * radius
			ny1 *= d1_inv * radius
			var wx1 = wx0 + dx0 + nx0
			var wy1 = wy0 + dy0 + ny0
			var wx2 = wx3 + dx1 - nx1
			var wy2 = wy3 + dy1 - ny1
			Utils.computeBezierCurvePoints(bezier_pts_x, bezier_pts_y, wx0, wy0, wx1, wy1, wx2, wy2, wx3, wy3)
			drawBezierField(g)
			drawBezierField(g)
			drawBezierField(g)
			wx1 = wx0 + dx0 - nx0
			wy1 = wy0 + dy0 - ny0
			wx2 = wx3 + dx1 + nx1
			wy2 = wy3 + dy1 + ny1
			Utils.computeBezierCurvePoints(bezier_pts_x, bezier_pts_y, wx0, wy0, wx1, wy1, wx2, wy2, wx3, wy3)
			drawBezierField(g)
			drawBezierField(g)
			drawBezierField(g)
		}
	}

	val playerBarrierElectricWallVersion = 3

	// -----------------------------------------------------------------------------------------------
	private fun drawPlayerBarrier(player: Player, g: AGraphics, px: Float, py: Float) {
		if (isBarrierActive(player)) {
			g.color = GColor.YELLOW
			if (player.barrier_electric_wall[0] >= 0) {
				when (playerBarrierElectricWallVersion) {
					1 -> drawPlayerBarrierElectricWall1(player, g, px, py)
					2 -> drawPlayerBarrierElectricWall2(player, g, px, py)
					3 -> drawPlayerBarrierElectricWall3(player, g, px, py)
				}
			} else {
				// draw 3 times for effect
				drawStaticField(g, px, py, PLAYER_RADIUS_BARRIER)
				drawStaticField(g, px, py, PLAYER_RADIUS_BARRIER - 1)
				drawStaticField(g, px, py, PLAYER_RADIUS_BARRIER + 1)
			}
		}
	}

	private fun drawBezierField(g: AGraphics) {
		var x0: Float
		var y0: Float
		var x1: Float
		var y1: Float
		x0 = bezier_pts_x[0]
		y0 = bezier_pts_y[0]
		for (i in 1 until bezier_pts_x.size) {
			if (i < bezier_pts_x.size / 2) {
				x1 = bezier_pts_x[i] + random(-i..i)
				y1 = bezier_pts_y[i] + random(-i..i)
			} else {
				val ii = bezier_pts_x.size - i
				x1 = bezier_pts_x[i] + random(-ii..ii)
				y1 = bezier_pts_y[i] + random(-ii..ii)
			}
			g.drawLine(x0, y0, x1, y1)
			x0 = x1
			y0 = y1
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Draw the goal (End)
	private fun drawEnd(g: AGraphics, x: Float, y: Float) {

		// draw an X
		g.color = GColor.CYAN
		val minRadius = 10f
		val maxRadius = 22f
		val numRings = 3
		val ringSpacing = 4
		val animSpeed = 5 // higher is slower
		for (i in 0 until numRings) {
			val f = frameNumber / animSpeed + i * ringSpacing
			val r = maxRadius - (f % (maxRadius - minRadius) + minRadius)
			g.drawCircle(x, y, r)
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun getDirection(v: Vector2D): Int {
		// return 0=N, E=1, S=2, W=3
		return if (abs(v.x) > abs(v.y)) {
			if (v.x < 0) DIR_LEFT else DIR_RIGHT
		} else {
			if (v.y < 0) DIR_UP else DIR_DOWN
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawElectricWall_r(g: AGraphics, x0: Float, y0: Float, x1: Float, y1: Float, num: Int) {
		if (num > 0) {
			// find midpoint
			val mx = (x0 + x1) / 2 + random(-num..num)
			val my = (y0 + y1) / 2 + random(-num..num)
			drawElectricWall_r(g, x0, y0, mx, my, num - 1)
			drawElectricWall_r(g, mx, my, x1, y1, num - 1)
		} else {
			g.drawLine(x0, y0, x1, y1)
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun getBreakingWallOffset(p: Int, num: Int): Int {
		val min = num * 3 + 10
		val max = num * 4 + 20
		return p * 113 % (max - min) * if (p % 2 == 0) 1 else -1
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawBreakingWall_r(g: AGraphics, x0: Float, y0: Float, x1: Float, y1: Float, num: Int, f: Int) {
		if (num > 0) {
			val xoff = getBreakingWallOffset(f, num)
			val yoff = getBreakingWallOffset(f, num)
			// find midpoint
			val mx = (x0 + x1) / 2 + xoff
			val my = (y0 + y1) / 2 + yoff
			drawBreakingWall_r(g, x0, y0, mx, my, num - 1, f)
			drawBreakingWall_r(g, mx, my, x1, y1, num - 1, f)
		} else {
			g.drawLine(x0, y0, x1, y1, 3f)
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawLineFade_r(
		g: AGraphics,
		x0: Float,
		y0: Float,
		x1: Float,
		y1: Float,
		outer: GColor,
		inner: GColor,
		num: Int,
		factor: Float
	) {
		val mx = (x0 + x1) / 2
		val my = (y0 + y1) / 2
		if (num > 0) {
			val cm = inner.interpolateTo(outer, factor) //Utils.interpolate(outer, inner, factor);
			drawLineFade_r(g, x0, y0, mx, my, outer, cm, num - 1, factor)
			drawLineFade_r(g, mx, my, x1, y1, cm, inner, num - 1, factor)
		} else {
			g.color = outer
			g.drawLine(x0, y0, x1, y1, 3f)
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawPortalWall(g: AGraphics, x0: Float, y0: Float, x1: Float, y1: Float) {
		val mx = (x0 + x1) / 2
		val my = (y0 + y1) / 2
		drawLineFade_r(g, x0, y0, mx, my, GColor.LIGHT_GRAY, throbbing_white, 3, 0.5f)
		drawLineFade_r(g, mx, my, x1, y1, throbbing_white, GColor.LIGHT_GRAY, 3, 0.5f)
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawRubberWall(g: AGraphics, x0: Float, y0: Float, x1: Float, y1: Float, frequency: Float) {
		var x2 = 0f
		var y2 = 0f
		val vx = (x1 - x0) * 0.5f
		val vy = (y1 - y0) * 0.5f
		var nx = 0f
		var ny = 0f
		g.color = GColor.BLUE
		val thickness = 2f
		if (frequency < EPSILON) {
			g.drawLine(x0, y0, x1, y1, thickness)
		} else {
			val factor = CMath.sine(frameNumber * 50f) * frequency
			nx = -vy * factor
			ny = vx * factor
			x2 = x0 + vx + nx
			y2 = y0 + vy + ny
			Utils.computeBezierCurvePoints(bezier_pts_x, bezier_pts_y,
				x0, y0, x2, y2, x2, y2, x1, y1)
			g.drawLineStrip(bezier_pts_x, bezier_pts_y, thickness)
			if (isDebugEnabled(Debug.DRAW_MAZE_INFO)) {
				val x = x2
				val y = y2
				g.drawRect(x, y, 1f, 1f)
			}
		}
	}

	@Omit
	private val bezier_pts_x = FloatArray(10)
	@Omit
	private val bezier_pts_y = FloatArray(10)

	// -----------------------------------------------------------------------------------------------
	private fun drawDoor(g: AGraphics, door: Wall, x0: Float, y0: Float, x1: Float, y1: Float) {
		g.color = DOOR_COLOR
		door.frame++
		var dx = x1 - x0
		var dy = y1 - y0
		val mx = (x1 + x0) / 2
		val my = (y1 + y0) / 2
		when (door.state) {
			DOOR_STATE_CLOSED -> {
				g.drawLine(x0, y0, x1, y1, DOOR_THICKNESS)
				if (door.type == WALL_TYPE_BROKEN_DOOR) {
					if (door.frame > BROKEN_DOOR_CLOSED_FRAMES) {
						door.state = DOOR_STATE_OPENING
						door.frame = 0
					}
				}
			}

			DOOR_STATE_LOCKED -> {
				g.drawLine(x0, y0, x1, y1, DOOR_THICKNESS)
				g.color = GColor.RED
				g.drawFilledCircle(mx, my, 10f)
			}

			DOOR_STATE_OPEN -> {
				dx /= 4
				dy /= 4
				g.drawLine(x0, y0, (x0 + dx), (y0 + dy), DOOR_THICKNESS)
				g.drawLine(x1, y1, (x1 - dx), (y1 - dy), DOOR_THICKNESS)
				if (door.frame > DOOR_OPEN_FRAMES) {
					door.state = DOOR_STATE_CLOSING
					door.frame = 0
				}
			}

			else -> {
				val delta = getDoorDelta(Vector2D.newTemp(dx, dy), door.frame, door.state)
				dx = delta.x
				dy = delta.y
				g.drawLine(x0, y0, (x0 + dx), (y0 + dy), DOOR_THICKNESS)
				g.drawLine(x1, y1, (x1 - dx), (y1 - dy), DOOR_THICKNESS)
				if (door.frame >= DOOR_SPEED_FRAMES) {
					if (door.state == DOOR_STATE_OPENING) {
						door.state = DOOR_STATE_OPEN
					} else {
						door.state = DOOR_STATE_CLOSED
					}
					door.frame = 0
				}
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun getDoorDelta(dv: Vector2D, framesElapsed: Int, doorState: Int): MutableVector2D {
		val len = dv.magFast().coerceAtLeast(1f)
		val len_inv = 1.0f / len
		var l = DOOR_SPEED_FRAMES_INV * framesElapsed * (len * 0.25f)
		if (doorState == DOOR_STATE_OPENING) l = len / 4 - l
		return dv * .25f + dv * len_inv * l
	}

	@Omit
	private val renderDirs = arrayOf(WallDir.RIGHT, WallDir.BOTTOM)

	private fun drawOnScreenWalls(g: AGraphics, cb: (AGraphics, Wall, Vector2D) -> Unit) {
		for (v in 0 until MAZE_NUM_VERTS) {
			for (dir in renderDirs) {
				getWall(v, dir)?.takeIf { it.isOnScreen() }?.let {
					it.nearV = v
					cb(g, it, it.v0())
				}
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawDebugWallInfo(g: AGraphics) {
		drawOnScreenWalls(g) { g, it, v ->
			val v0 = maze_verts[it.farV]
			g.color = GColor.CYAN
			val cv = v.add(v0).scaleEq(.5f)
			g.drawJustifiedString(cv, Justify.LEFT, Justify.CENTER, it.toString())
		}
	}

	private fun drawWall(g: AGraphics, info: Wall, x0: Float, y0: Float, x1: Float, y1: Float) {
		when (info.type) {
			WALL_TYPE_NORMAL ->             // translate and draw the line
				if (info.health < WALL_NORMAL_HEALTH) {
					// give a number between 0-1 that is how much health we have
					val health = 1.0f / WALL_NORMAL_HEALTH * info.health
					val c = (255.0f * health).roundToInt()
					g.color = GColor(255, c, c)
					// if wall is healthy, num==0
					// if wall is med, num 1
					// if wall is about to break, num = 2
					val num = ((1.0f - health) * WALL_BREAKING_MAX_RECURSION).roundToInt()
					// make these values consistent based on garbage input
					//int xoff = (info.v0 * (-1 * info.v1 % 2) % 30);
					//int yoff = (info.v1 * (-1 * info.v0 % 2) % 30);
					drawBreakingWall_r(g, x0, y0, x1, y1, num, info.v0 + info.v1)
				} else {
					g.color = GColor.LIGHT_GRAY
					g.drawLine(x0, y0, x1, y1, MAZE_WALL_THICKNESS)
				}

			WALL_TYPE_ELECTRIC -> {
				g.color = GColor.YELLOW
				g.setLineWidth(1f)
				var done = false
				var i = 0
				while (i < players.size) {
					val player = players[i]
					if (isBarrierActive(player) && Utils.isCircleIntersectingLineSeg(
							x0, y0, x1, y1, player.pos.x, player.pos.y, PLAYER_RADIUS_BARRIER)
					) {
						// draw the electric wall at the perimeter of the barrier, so the electric
						// wall and the barrier become 1 unit.  TODO: make ememies die from electric walls.
						// the actual rendering is done by drawPlayerBarrier
						player.barrier_electric_wall[0] = x0
						player.barrier_electric_wall[1] = y0
						player.barrier_electric_wall[2] = x1
						player.barrier_electric_wall[3] = y1
						done = true
						break
					}
					i++
				}
				if (!done) { // draw twice for effect
					drawElectricWall_r(g, x0, y0, x1, y1, 3)
					drawElectricWall_r(g, x0, y0, x1, y1, 3)
				}
			}

			WALL_TYPE_INDESTRUCTIBLE -> {
				g.color = GColor.DARK_GRAY
				g.drawLine(x0, y0, x1, y1, (MAZE_WALL_THICKNESS + 2))
			}

			WALL_TYPE_PORTAL -> drawPortalWall(g, x0, y0, x1, y1)
			WALL_TYPE_RUBBER -> drawRubberWall(g, x0, y0, x1, y1, info.frequency)
			WALL_TYPE_BROKEN_DOOR,
			WALL_TYPE_DOOR -> drawDoor(g, info, x0, y0, x1, y1)
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Render the maze from screen_x/y
	private fun drawMaze(g: AGraphics) {

		fun canSeePast(info: Wall, v0: Vector2D, v1: Vector2D): Boolean {
			return if (GAME_VISIBILITY && !info.visible) {
				// see if player can 'see' the wall
				val mp = v0.midPoint(v1)
				if (canSee(player.pos, mp)) {
					info.visible = true
					true
				} else {
					false
				}
			} else true
		}

		drawOnScreenWalls(g) { g, info, v ->
			val v0 = info.v0()
			val v1 = info.v1()
			if (canSeePast(info, v0, v1)) {
				drawWall(g, info, v0.x, v0.y, v1.x, v1.y)
				if (info.frequency > 0) {
					info.frequency -= RUBBER_WALL_FREQUENCY_COOLDOWN
				}
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	// layout an even grid of vertices, with some noise
	private fun buildVertices(noise: Float) {
		var x = 0f
		var y = 0f
		var index: Int
		for (i in 0 until MAZE_NUMCELLS_Y + 1) {
			for (j in 0 until MAZE_NUMCELLS_X + 1) {
				index = i * (MAZE_NUMCELLS_X + 1) + j
				maze_verts[index] = Vector2D(x + randomFloat(-noise, noise), y + randomFloat(-noise, noise))
				x += MAZE_CELL_DIM
				// compute the actual world dimension
			}
			x = 0f
			y += MAZE_CELL_DIM
		}
		verts_min_v.assign(Vector2D.MAX)
		maze_verts.forEach {
			verts_min_v.minEq(it)
		}
		verts_max_v.assign(Vector2D.MIN)
		maze_verts.forEach {
			verts_max_v.maxEq(it)
		}
	}

	// -----------------------------------------------------------------------------------------------
	// delete all edges in the maze graph and digraph
	fun clearAllWalls() {
		walls.forEach {
			it.filterNotNull().forEach {
				if (it.isPerimeter()) {
					it.type = WALL_TYPE_INDESTRUCTIBLE
				} else {
					it.type = WALL_TYPE_NONE
				}
			}
		}
	}


	// -----------------------------------------------------------------------------------------------
	fun getWall(v0: Int, v1: Int): Wall? {
		return walls.getOrNull(v0)?.firstOrNull { it?.v0 == v1 || it?.v1 == v1 }
	}

	private fun getWall(v: Int, dir: WallDir): Wall? {
		return walls.getOrNull(v)?.get(dir.ordinal)
	}

	@delegate:Omit
	val wall_lookup: Map<Int, Wall> by lazy {
		walls.flatten().filterNotNull().associateBy { it.id }
	}

	private fun getWallById(id: Int): Wall? {
		return wall_lookup[id]
	}

	// -----------------------------------------------------------------------------------------------
	// build a new maze and populate with generators. Quantity based on
	// game_level
	private fun buildAndPopulateLevel() {
		enemy_robot_speed = ENEMY_ROBOT_SPEED
		total_enemies = MAX_ENEMIES
//		addEnemy(0, 0, ENEMY_INDEX_BRAIN, true)
		if (game_type == GAME_TYPE_CLASSIC) {
			buildAndPopulateClassic()
		} else buildAndPopulateRobocraze()
	}

	// -----------------------------------------------------------------------------------------------
	// rearrange the enemies away from the player
	private fun shuffleEnemies() {
		val wid = verts_max_x - verts_min_x
		val hgt = verts_max_y - verts_min_y
		enemies.forEach { e ->
			val r = e.radius()
			var d = 1000f
			do {
				e.pos.assign(
					randomFloat(r, wid - r),
					randomFloat(r, hgt - r)
				)
				getClosestPlayer(e.pos)?.let {
					d = it.pos.sub(e.pos).magFast()
				}
			} while (d < 100 - gameLevel)
		}
	}

	private fun resetLevel() {
		tank_missiles.clear()
		enemy_missiles.clear()
		snake_missiles.clear()
		powerups.clear()
		enemies.clear()
		zombie_tracers.clear()
		particles.clear()
		messages.clear()
		people_points = PEOPLE_START_POINTS
		enemy_robot_speed = ENEMY_ROBOT_SPEED + difficulty + gameLevel / 4
		players.forEach {
			it.reset(frameNumber)
		}
		game_start_frame = frameNumber
	}

	// -----------------------------------------------------------------------------------------------
	private fun buildAndPopulateClassic() {
		val wid = WORLD_WIDTH_CLASSIC
		val hgt = WORLD_HEIGHT_CLASSIC

		// build the vertices
		buildVertices(0f)
		clearAllWalls()

		// put players at the center
		var sx = wid / 2
		var sy = hgt / 2

		if (players.size > 1)
			sx -= PLAYER_RADIUS
		if (players.size > 2)
			sy -= PLAYER_RADIUS

		players.forEachIndexed { index, player ->
			player.pos.assign(sx, sy)
			computeCell(player.pos, player.start_cell)
			sx += PLAYER_RADIUS
			if (index % 2 == 0) {
				sx -= 2 * PLAYER_RADIUS
			} else {
				sy += PLAYER_RADIUS
			}
		}
		// add all the perimiter edges
		val bottom = (MAZE_NUMCELLS_X + 1) * MAZE_NUMCELLS_Y
		for (i in 0 until MAZE_NUMCELLS_X) {
			getWall(i, i + 1)?.type = WALL_TYPE_INDESTRUCTIBLE
			getWall(bottom + i, bottom + i + 1)?.type = WALL_TYPE_INDESTRUCTIBLE
		}
		for (i in 0 until MAZE_NUMCELLS_Y) {
			getWall(i * (MAZE_NUMCELLS_X + 1), (i + 1) * (MAZE_NUMCELLS_X + 1))?.type = WALL_TYPE_INDESTRUCTIBLE
			getWall(i * (MAZE_NUMCELLS_X + 1) + MAZE_NUMCELLS_X, (i + 1) * (MAZE_NUMCELLS_X + 1) + MAZE_NUMCELLS_X)?.type =
				WALL_TYPE_INDESTRUCTIBLE
		}

		// start the timer for the highlighted player
		resetLevel()

		// add some robots
		var count = MAX_ENEMIES / 2 + (difficulty - 1) * 4 + gameLevel / 2
		val startV = Vector2D.ZERO
		for (i in 0 until count)
			addEnemy(startV, random(ENEMY_INDEX_ROBOT_N..ENEMY_INDEX_ROBOT_W))

		// Add some Thugs
		count = ENEMY_GEN_INITIAL + (difficulty - 1) * 4 + gameLevel + random(-5..4 + gameLevel)
		for (i in 0 until count)
			addEnemy(startV, random(ENEMY_INDEX_THUG_N..ENEMY_INDEX_THUG_W))

		// Add Some Brains And / Or Tanks
		if (gameLevel > 1 && gameLevel % 2 == 0) {
			// add brains
			count = ENEMY_GEN_INITIAL + (difficulty - 1) * 4 + gameLevel + random(-5..5 + gameLevel)
			for (i in 0 until count) addEnemy(startV, ENEMY_INDEX_BRAIN)
		} else if (gameLevel > 2 && gameLevel % 2 == 1) {
			// Add Some tanks
			count = ENEMY_GEN_INITIAL + (difficulty - 1) * 2 + gameLevel + random(-5..5 + gameLevel)
			for (i in 0 until count) addEnemy(startV, random(ENEMY_INDEX_TANK_NE..ENEMY_INDEX_TANK_NW))
		}
		shuffleEnemies()
		addPeople()
	}

	fun isCellsEq(c0: IntArray, c1: IntArray): Boolean {
		require(c0.size == 2)
		require(c1.size == 2)
		return c0[0] == c1[0] && c0[1] == c1[1]
	}

	fun cellToMazeCoords(cell: IntArray): MutableVector2D {
		return Vector2D.newTemp(
			cell[0] * MAZE_CELL_DIM + MAZE_CELL_DIM / 2,
			cell[1] * MAZE_CELL_DIM + MAZE_CELL_DIM / 2
		)
	}

	fun setCell(cell: IntArray, x: Int, y: Int) {
		cell[0] = x
		cell[1] = y
	}

	// -----------------------------------------------------------------------------------------------
	private fun addEnemyAtRandomCell(type: Int, usedCells: Array<BooleanArray>) {
		var max_tries = 1000
		var cell = intArrayOf(0, 0)
		here@ while (max_tries > 0) {
			cell = intArrayOf(random(MAZE_NUMCELLS_X), random(MAZE_NUMCELLS_Y))
			val cx = cell[0]
			val cy = cell[1]
			if (usedCells[cx][cy]) {
				max_tries--
				continue
			}
			for (player in players) {
				if (isCellsEq(player.cur_cell, cell)) {
					max_tries--
					continue@here
				}
			}

			if (isCellsEq(end_cell, cell)) {
				max_tries--
				continue@here
			}

			usedCells[cx][cy] = true
			break
		}
		val pos = cellToMazeCoords(cell)
		addEnemy(pos, type)
	}

	// -----------------------------------------------------------------------------------------------
	private fun addWallCount(count: IntArray, v0: Int, v1: Int) {
		require(v0 >= 0 && v0 < MAZE_NUM_VERTS)
		require(v1 >= 0 && v0 < MAZE_NUM_VERTS)
		require(count.size == MAZE_NUM_VERTS)
		require(v0 != v1)
		count[v0]++
		require(count[v0] <= 8)
		count[v1]++
		require(count[v1] <= 8)
	}

	// -----------------------------------------------------------------------------------------------
	private fun computeWallEnding(count: IntArray, v0: Int, v1: Int) {
		getWall(v0, v1)?.let { wall ->
			if (wall.type != WALL_TYPE_NONE) {
				if (count[v0] == 2 || count[v1] == 2) wall.ending = true
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	fun buildAndPopulateRobocraze() {

		// build the vertices
		buildVertices(MAZE_VERTEX_NOISE)

		// create a temp matrix of cells.  This will hold the maze information.
		// each cell is a 4bit value with north, south, east west bits.
		//int[][] maze_cells = new int[MAZE_NUMCELLS_X][];
		maze_cells.forEach {
			it.fill(WALL_NORTH or WALL_SOUTH or WALL_EAST or WALL_WEST)
		}

		// pick a start and end point
		val cell_start_x = random(0 until MAZE_NUMCELLS_X)
		val cell_start_y = random(0 until MAZE_NUMCELLS_Y)
		var cell_end_x = random(0 until MAZE_NUMCELLS_X)
		var cell_end_y = random(0 until MAZE_NUMCELLS_Y)

		// continue searching until we are not on top of each other
		while (abs(cell_end_x - cell_start_x) < MAZE_NUMCELLS_X / 2 ||
			abs(cell_end_y - cell_start_y) < MAZE_NUMCELLS_Y / 2
		) {
			cell_end_x = random(0 until MAZE_NUMCELLS_X)
			cell_end_y = random(0 until MAZE_NUMCELLS_Y)
		}

		// Start the recursive maze generation algorithm.  this method knocks
		// down walls until all cells have been touched.
		mazeSearch_r(maze_cells, cell_start_x, cell_start_y, cell_end_x, cell_end_y)

		// now that we have a maze organized as a group of cells, convert to
		// graph
		clearAllWalls()
		val vertex_wall_count = IntArray(MAZE_NUM_VERTS) { 0 }
		for (i in 0 until MAZE_NUMCELLS_X) {
			for (j in 0 until MAZE_NUMCELLS_Y) {
				// compute the vertex indices associated with this cell
				val upleft = i + j * (MAZE_NUMCELLS_X + 1)
				val upright = i + 1 + j * (MAZE_NUMCELLS_X + 1)
				val downleft = i + (j + 1) * (MAZE_NUMCELLS_X + 1)
				val downright = i + 1 + (j + 1) * (MAZE_NUMCELLS_X + 1)

				//.log.debug("cells[i][j]=" + cells[i][j] + ", i=" + i + ", j=" + j + ", upleft=" + upleft + ", upright=" + upright + ", downleft=" + downleft + ", downright=" + downright);
				//.log.debug("wall count=" + Arrays.toString(vertex_wall_count));
				if (maze_cells[i][j] and WALL_NORTH != 0) {
					getWall(upleft, upright)?.type = WALL_TYPE_INDESTRUCTIBLE
					addWallCount(vertex_wall_count, upleft, upright)
				}
				if (maze_cells[i][j] and WALL_EAST != 0) {
					getWall(upright, downright)?.type = WALL_TYPE_INDESTRUCTIBLE
					addWallCount(vertex_wall_count, upright, downright)
				}
				if (maze_cells[i][j] and WALL_SOUTH != 0) {
					getWall(downleft, downright)?.type = WALL_TYPE_INDESTRUCTIBLE
					addWallCount(vertex_wall_count, downleft, downright)
				}
				if (maze_cells[i][j] and WALL_WEST != 0) {
					getWall(upleft, downleft)?.type = WALL_TYPE_INDESTRUCTIBLE
					addWallCount(vertex_wall_count, downleft, upleft)
				}
			}
		}

		for (i in 0 until MAZE_NUMCELLS_X) {
			for (j in 0 until MAZE_NUMCELLS_Y) {
				// compute the vertex indices associated with this cell
				val upleft = i + j * (MAZE_NUMCELLS_X + 1)
				val upright = i + 1 + j * (MAZE_NUMCELLS_X + 1)
				val downleft = i + (j + 1) * (MAZE_NUMCELLS_X + 1)
				val downright = i + 1 + (j + 1) * (MAZE_NUMCELLS_X + 1)
				computeWallEnding(vertex_wall_count, upleft, upright)
				computeWallEnding(vertex_wall_count, upright, downright)
				computeWallEnding(vertex_wall_count, downleft, downright)
				computeWallEnding(vertex_wall_count, downleft, upleft)
			}
		}
		val usedCells = Array(MAZE_NUMCELLS_X) { BooleanArray(MAZE_NUMCELLS_Y) }
		usedCells[cell_start_x][cell_start_y] = true
		usedCells[cell_end_x][cell_end_y] = true

		// position the player at the center starting cell
		val sx = cell_start_x * MAZE_CELL_DIM + MAZE_CELL_DIM / 2
		val sy = cell_start_y * MAZE_CELL_DIM + MAZE_CELL_DIM / 2
		/*
		 * Support up to 4 players that start in the same cell adjacent
		 * to each other in one of these formations:
		 *
		 * 1:     1
		 *
		 * 2:     1
		 *        2
		 *
		 * 3:    1 2
		 *        3
		 *
		 * 4:    1 2
		 *       3 4
		 */
		val spacing = PLAYER_RADIUS + 5
		val formations = arrayOf(
			arrayOf(
				Vector2D(sx, sy)
			),
			arrayOf(
				Vector2D(sx, sy - spacing),
				Vector2D(sx, sy + spacing)
			),
			arrayOf(
				Vector2D(sx, sy - spacing),
				Vector2D(sx - spacing, sy + spacing),
				Vector2D(sx + spacing, sy + spacing)
			),
			arrayOf(
				Vector2D(sx - spacing, sy - spacing),
				Vector2D(sx + spacing, sy - spacing),
				Vector2D(sx - spacing, sy + spacing),
				Vector2D(sx + spacing, sy + spacing)
			)
		)
		val formation = formations[players.size - 1]
		players.forEachIndexed { index, player ->
			setCell(player.start_cell, cell_start_x, cell_start_y)
			player.start_v.assign(formation[index])
			player.pos.assign(player.start_v)
		}
		// compute maze coord position of ending cell
		setCell(start_cell, cell_start_x, cell_start_y)
		setCell(end_cell, cell_end_x, cell_end_y)

		resetLevel()

		// Now add a few generators and thugs
		val num_gens = ENEMY_GEN_INITIAL + gameLevel + difficulty

		enemies.clear()

		// make sure only 1 generator per cell
		repeat(num_gens) {
			addEnemyAtRandomCell(ENEMY_INDEX_GEN, usedCells)
		}

		// add some jaws
		repeat(3 + gameLevel) {
			addEnemyAtRandomCell(ENEMY_INDEX_JAWS, usedCells)
		}

		repeat(4 + gameLevel + difficulty) {
			addEnemyAtRandomCell(ENEMY_INDEX_LAVA, usedCells)
		}

//		fun scatter(radius: Float): Float = randomFloat(-ENEMY_SPAWN_SCATTER + radius, ENEMY_SPAWN_SCATTER - radius)

		// now place some thugs by the generators
		enemies.filter { it.type == ENEMY_INDEX_GEN }.forEach { e ->
			val p = e.pos.withJitter(ENEMY_SPAWN_SCATTER + ENEMY_THUG_RADIUS)
			addEnemy(p, random(ENEMY_INDEX_THUG_N..ENEMY_INDEX_THUG_W))
		}

		// now place some brains or tanks
		if (gameLevel > 1) {
			if (gameLevel % 2 == 0) {
				enemies.filter { it.type == ENEMY_INDEX_GEN }.forEach { e ->
					addEnemy(e.pos.withJitter(ENEMY_SPAWN_SCATTER + ENEMY_BRAIN_RADIUS), ENEMY_INDEX_BRAIN)
				}
			} else {
				enemies.filter { it.type == ENEMY_INDEX_GEN }.forEach { e ->
					addEnemy(e.pos.withJitter(ENEMY_SPAWN_SCATTER + ENEMY_TANK_RADIUS), random(ENEMY_INDEX_TANK_NE..ENEMY_INDEX_TANK_NW))
				}
			}
		}
		addPeople()
		buildRandomWalls()
	}

	fun initNewPlayer(player: Player) {
		setCell(player.start_cell, start_cell[0], start_cell[1])
		player.screen.setDimension(screen_dim)
		val sx = start_cell[0] * MAZE_CELL_DIM + MAZE_CELL_DIM / 2
		val sy = start_cell[1] * MAZE_CELL_DIM + MAZE_CELL_DIM / 2
		player.start_v.assign(sx, sy)
		player.pos.assign(player.start_v)
	}

	// -----------------------------------------------------------------------------------------------
	fun buildRandomWalls() {
		require(game_type != GAME_TYPE_CLASSIC)
		var portalWall: Wall? = null
		for (v0 in 1 until MAZE_NUM_VERTS) {
			for (v1 in 0 until v0) {
				getWall(v0, v1)?.let { wall ->
					require(wall.v0 != wall.v1)
					if (wall.isPerimeter()) {
						wall.type = WALL_TYPE_INDESTRUCTIBLE
						return@let
					}
					if (wall.type == WALL_TYPE_NONE) {
						if (random(100) < gameLevel) {
							wall.type = WALL_TYPE_BROKEN_DOOR
						} else if (random(100) < gameLevel) {
							wall.type = WALL_TYPE_DOOR
						}
						return@let
					}

					// if this is an ending wall, then skip
					// an ending wall has 1 vertex with only 1 wall on it (itself)
					if (wall.ending)
						return@let
					wall.type = Utils.chooseRandomFromSet(*wallChanceForLevel(gameLevel))
					when (wall.type) {
						WALL_TYPE_NORMAL -> wall.health = WALL_NORMAL_HEALTH
						WALL_TYPE_DOOR -> {
							wall.type = WALL_TYPE_DOOR
							wall.state = DOOR_STATE_LOCKED
						}

						WALL_TYPE_PORTAL -> portalWall?.let {
							wall.initPortal(it)
							portalWall = null
						} ?: run { portalWall = wall }
					}
				}
			}
		}
		// revert a dangling portal
		portalWall?.type = WALL_TYPE_NORMAL
	}

	// this version picks cells at random and makes the mazes much more difficult
	private fun mazeSearch_r2(cells: Array<IntArray>, x: Int, y: Int, ex: Int, ey: Int) {
		var x = x
		var y = y
		val list = ArrayList<IntArray>(256)
		list.add(intArrayOf(x, y))
		while (!list.isEmpty()) {
			val index = random(list.size)
			val xy = list.removeAt(index)
			x = xy[0]
			y = xy[1]

			// get an array of directions in descending order of priority
			val dir_list = directionHeuristic(x, y, ex, ey)
			for (i in 0..3) {
				val nx = x + move_dv[dir_list[i]].Xi()
				val ny = y + move_dv[dir_list[i]].Yi()
				if (nx < 0 || ny < 0 || nx >= MAZE_NUMCELLS_X || ny >= MAZE_NUMCELLS_Y) continue

				// Ignore cells already touched
				if (cells[nx][ny] != 15) continue

				// break wall
				cells[x][y] = cells[x][y] and (1 shl dir_list[i]).inv()
				val dir2 = (dir_list[i] + 2) % 4
				cells[nx][ny] = cells[nx][ny] and (1 shl dir2).inv()
				//mazeSearch_r(cells, nx, ny, end_x, end_y);
				list.add(intArrayOf(nx, ny))
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Recursive DFS search routine to create a maze
	// this version makes mazes easy to solve
	private fun mazeSearch_r(cells: Array<IntArray>, x: Int, y: Int, end_x: Int, end_y: Int) {
		// this check isnt really neccessary
		if (x < 0 || y < 0 || x >= MAZE_NUMCELLS_X || y >= MAZE_NUMCELLS_Y) return

		// .log.debug("Searching " + x + " " + y + " " + direction);

		// get an array of directions in descending order of priority
		val dir_list = directionHeuristic(x, y, end_x, end_y)
		for (i in 0..3) {
			val nx = x + move_dv[dir_list[i]].Xi()
			val ny = y + move_dv[dir_list[i]].Yi()
			if (nx < 0 || ny < 0 || nx >= MAZE_NUMCELLS_X || ny >= MAZE_NUMCELLS_Y) continue

			// Ignore cells already touched
			if (cells[nx][ny] != 15) continue

			// break wall
			cells[x][y] = cells[x][y] and (1 shl dir_list[i]).inv()
			val dir2 = (dir_list[i] + 2) % 4
			cells[nx][ny] = cells[nx][ny] and (1 shl dir2).inv()

			// DFS Recursive search
			if (gameLevel - difficulty > 10) mazeSearch_r2(cells, nx, ny, end_x, end_y) else mazeSearch_r(cells, nx, ny, end_x, end_y)
		}
	}

	private fun Wall.isOnScreen(): Boolean = isOnScreen(v0) || isOnScreen(v1)

	private fun Wall.v0() = maze_verts[v0]
	private fun Wall.v1() = maze_verts[v1]
	private fun Wall.dv() = v1() - v0()
	private fun Wall.mid() = (v1() + v0()) / 2

	private fun Wall.near() = maze_verts[nearV]
	private fun Wall.far() = maze_verts[farV]

	private fun Wall.isActive(): Boolean = when (type) {
		in -1..WALL_TYPE_NONE -> false
		WALL_TYPE_ELECTRIC -> frame <= frameNumber
		else -> true
	}


	// -----------------------------------------------------------------------------------------------
	// return 4 dim array of ints in range 0-3, each elem occurs only once
	//   the heuristic will repulse the start end end cells causing a
	//   longer path solution
	private fun directionHeuristic(x1: Int, y1: Int, x2: Int, y2: Int): IntArray {
		// resulting list
		val h = Array(4) { 5f }

		if (y1 < y2) // tend to move north
			h[0] += randomFloat(0.4f) - 0.1f
		else if (y1 > y2) // tend to move south
			h[2] += randomFloat(0.4f) - 0.1f
		if (x1 < x2) // tend to move west
			h[3] += randomFloat(0.4f) - 0.1f
		else if (x1 > x2) // tend to move east
			h[1] += randomFloat(0.4f) - 0.1f

		return h.mapIndexed { index, fl -> Pair(index, fl) }.sortedWith { a, b ->
			if (a.second < b.second || a.second == b.second && flipCoin()) -1 else 1
		}.map { it.first }.toIntArray()
	}

	// -----------------------------------------------------------------------------------------------
	// return true when x,y is within screen bounds
	private fun isOnScreen(v: Vector2D): Boolean {
		if (game_type == GAME_TYPE_CLASSIC) return true
		return players.any { it.screen.contains(v) }
	}

	private fun isOnScreen(v: Int): Boolean = isOnScreen(maze_verts[v])

	private fun Object.isOnScreen(): Boolean = isOnScreen(pos)

	// -----------------------------------------------------------------------------------------------
	private fun updatePlayerVisibility_r(verts: IntArray, d: Int) {
		for (i in 0..3) {
			verts[i] += CELL_LOOKUP_DV[d]
			if (verts[i] < 0 || verts[i] >= MAZE_NUM_VERTS) return
		}
		for (i in 0..3) {
			val ii = (i + 1) % 4
			getWall(verts[i], verts[ii])?.visible = true
		}
		val dd = (d + 1) % 4
		getWall(verts[d], verts[dd])?.let { wall ->
			if (canSeeThroughWall(wall))
				updatePlayerVisibility_r(verts, d)
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun updatePlayerVisibility(player: Player) {
		val cell = computeCell(player.pos)
		if (isCellsEq(cell, player.cur_cell))
			return

		val quad = computeBaseQuad(player.pos)
		//Utils.copyElems(player.primary_verts, verts);
		for (i in 0..3) {
			val ii = (i + 1) % 4
			getWall(quad[i], quad[ii])?.let { info ->
				info.visible = true
				if (canSeeThroughWall(info)) {
					// recursize search in this direction
					updatePlayerVisibility_r(quad, i)
				}
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun canSeeThroughWall(info: Wall): Boolean {
		return when (info.type) {
			WALL_TYPE_NONE, WALL_TYPE_ELECTRIC -> true
			WALL_TYPE_DOOR, WALL_TYPE_BROKEN_DOOR ->
				info.state != DOOR_STATE_CLOSED && info.state != DOOR_STATE_LOCKED

			else -> false
		}
	}

	// -----------------------------------------------------------------------------------------------
	// return true if there are no walls between sx, sy
	private fun canSee(sv: Vector2D, ev: Vector2D): Boolean {
		val max = MAZE_CELL_DIM * 2 / 3
		if (sv.sub(ev).magFast() < max)
			return true
		val sCell = IntArray(4)
		val eCell = IntArray(4)
		computeBaseQuad(sv, sCell)
		computeBaseQuad(ev, eCell)
		val cur_v = sv.add(0, 0)
		// compute
		while (true) {
			// if sv == ev, then we are done
			if (sCell.contentEquals(eCell))
				return true
			val dv = ev - cur_v

			if (abs(dv.x) < max && abs(dv.y) < max)
				return true
			val d = getDirection(dv)
			val dd = (d + 1) % 4
			getWall(sCell[d], sCell[dd])?.let { info ->

				// allow see through electric walls and open(ing)/closing doors
				if (!canSeeThroughWall(info)) return false
				var new_sx = 0f
				var new_sy = 0f
				for (i in 0..3) {
					sCell[i] += CELL_LOOKUP_DV[d]
					if (sCell[i] < 0 || sCell[i] >= MAZE_NUM_VERTS)
						return false
					new_sx += maze_verts[sCell[i]].x
					new_sy += maze_verts[sCell[i]].y
				}
				cur_v.scaleEq(.25 * new_sx, .25 * new_sy)
			}
		}
	}

	//private val compute_cell_result = intArrayOf(0,0)
	private fun computeCell(v: Vector2D, cell: IntArray = IntArray(2)): IntArray {
		val cx = v.x * MAZE_NUMCELLS_X / (verts_max_x - verts_min_x)
		val cy = v.y * MAZE_NUMCELLS_Y / (verts_max_y - verts_min_y)
		cell[0] = cx.toInt()
		cell[1] = cy.toInt()
		return cell
	}

	// for any given point in the maze, get the top left corver vertex from that point
	private fun computeTopLeftCornerVertex(pos: Vector2D = player.screen.topLeft): Int {
		val cx = (MAZE_NUMCELLS_X * (pos.x / (verts_max_x - verts_min_x))).toInt().coerceIn(0 until MAZE_NUMCELLS_X)
		val cy = (MAZE_NUMCELLS_Y * (pos.y / (verts_max_y - verts_min_y))).toInt().coerceIn(0 until MAZE_NUMCELLS_Y)
		return (cx + cy * (MAZE_NUMCELLS_X + 1)).coerceIn(0 until MAZE_NUM_VERTS)
	}

	// -----------------------------------------------------------------------------------------------
	// Put into array the 4 vertices around the point px,py
	//
	//     r[0]                   r[1]
	//
	//                      P
	//
	//
	//
	//
	//     r[2]                   r[3]
	//
	// P can be anywhere inside the rect
	// -----------------------------------------------------------------------------------------------
	//private val working_quad_array = intArrayOf(0,0,0,0)
	private fun computeBaseQuad(pos: Vector2D, result: IntArray = IntArray(4)): IntArray {
		result[0] = computeTopLeftCornerVertex(pos)
		result[1] = result[0] + 1
		result[3] = result[1] + MAZE_NUMCELLS_X
		result[2] = result[3] + 1
		return result
	}

	// -----------------------------------------------------------------------------------------------
	//
	// Assuming P is always inside the maze, test there are always 2 edge candidates
	// P will always be inside the triangle formed by the 3 vertices forming the 2 edges
	// Scenario 1:                      Scenario 2:
	//
	// v[0]-------v[1]                 v[1]--------v[0]
	//  |                                            |
	//  |   P                                     P  |
	//  |                                            |
	// v[2]                                         v[2]
	//
	// Scenario 3:                      Scenario 4:
	//
	//            v[2]                 v[2]
	//              |                    |   P
	//            P |                    |
	//              |                    |
	// v[1]-------v[0]                 v[0]--------v[1]
	//
	// ordering of walls not guaranteed. There could be 0 resulted if none are active
	// -----------------------------------------------------------------------------------------------
	@Omit
	private val wall_candidates = mutableListOf<Wall>()

	//private val wall_candidates_result = IntArray(4) { -1 }
	private fun computeWallCandidates(pos: Vector2D, activeOnly: Boolean = true): List<Wall> {

		fun getWallCandidate(v: Int, dir: WallDir) = getWall(v, dir)?.also {
			it.adjacent = null
			it.nearV = v
		}

		val quad = computeBaseQuad(pos)
		val nearest = quad.minBy {
			pos.sub(maze_verts[it]).magFast()
		}
		val cv = maze_verts[nearest]

		wall_candidates.clear()
		val candidates = mutableListOf<Wall?>()
		if (pos.x > cv.x) {
			if (pos.y > cv.y) {
				// scenario 1
				candidates.add(getWallCandidate(nearest, WallDir.RIGHT))
				candidates.add(getWallCandidate(nearest, WallDir.BOTTOM))
			} else {
				// scenario 4:
				candidates.add(getWallCandidate(nearest, WallDir.RIGHT))
				candidates.add(getWallCandidate(nearest, WallDir.TOP))
			}
		} else {
			if (pos.y > cv.y) {
				// scenario 2:
				candidates.add(getWallCandidate(nearest, WallDir.LEFT))
				candidates.add(getWallCandidate(nearest, WallDir.BOTTOM))
			} else {
				// scenario 3:
				candidates.add(getWallCandidate(nearest, WallDir.LEFT))
				candidates.add(getWallCandidate(nearest, WallDir.TOP))
			}
		}
		if (activeOnly)
			candidates.removeIf { it?.isActive() == false }

		wall_candidates.addAll(candidates.filterNotNull())

		if (wall_candidates.size == 2) {
			// they are each other's adjacent
			wall_candidates[0].adjacent = wall_candidates[1]
			wall_candidates[1].adjacent = wall_candidates[0]
		}

		return wall_candidates
	}


	// -----------------------------------------------------------------------------------------------
	// return true when a line intersects any edge in the graph
	private fun collisionScanLine(v0: Vector2D, v1: Vector2D): Wall? {
		return computeWallCandidates(v0).firstOrNull {
			when (it.type) {
				WALL_TYPE_BROKEN_DOOR,
				WALL_TYPE_DOOR -> collisionDoorLine(it, v0, v1)

				else -> Utils.isLineSegsIntersecting(v0, v1, it.v0(), it.v1()) != 0
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun collisionDoorLine(
		door: Wall,
		v0: Vector2D,
		v1: Vector2D,
	): Boolean {
		val dv = door.v1() - door.v0()
		val wv0 = door.v0()
		val wv1 = door.v1()
		when (door.state) {
			DOOR_STATE_LOCKED, DOOR_STATE_CLOSED ->
				return Utils.isLineSegsIntersecting(v0, v1, wv0, wv1) != 0

			DOOR_STATE_OPEN -> {
				dv /= 4f
			}

			else -> {
				dv.assign(getDoorDelta(dv, door.frame, door.state))
			}
		}
		return Utils.isLineSegsIntersecting(v0, v1, wv0, wv0 + dv) != 0 ||
			Utils.isLineSegsIntersecting(v0, v1, wv1, wv1 - dv) != 0
	}

	private fun isCircleIntersectingLineSeg(
		l0: Vector2D,
		l1: Vector2D,
		center: Vector2D,
		radius: Float
	): Boolean {
		val d2 = Utils.distSqPointSegment(center.x, center.y, l0.x, l0.y, l1.x, l1.y)
		val r2 = radius * radius
		return (d2 < r2)
	}

	// -----------------------------------------------------------------------------------------------
	private fun collisionDoorCircle(
		door: Wall,
		pos: Vector2D,
		radius: Float
	): Boolean {
		val dv = door.v1().sub(door.v0())
		when (door.state) {
			DOOR_STATE_LOCKED,
			DOOR_STATE_CLOSED -> return isCircleIntersectingLineSeg(door.v0(), door.v1(), pos, radius)

			DOOR_STATE_OPEN -> {
				dv.scaleEq(.25f)
			}

			else -> {
				dv.assign(getDoorDelta(dv, door.frame, door.state))
			}
		}
		return isCircleIntersectingLineSeg(door.v0(), door.v0().add(dv), pos, radius) ||
			isCircleIntersectingLineSeg(door.v1(), door.v1().sub(dv), pos, radius)
	}

	// -----------------------------------------------------------------------------------------------
	// return true when a sphere with radius at px,py is colliding with a wall
	private fun collisionScanCircle(pos: Vector2D, radius: Float): Wall? {
		return computeWallCandidates(pos).firstOrNull {
			collisionWallCircle(it, pos, radius)
		}
	}

	private fun collisionWallCircle(wall: Wall, pos: Vector2D, radius: Float): Boolean {
		return when (wall.type) {
			WALL_TYPE_DOOR, WALL_TYPE_BROKEN_DOOR -> collisionDoorCircle(wall, pos, radius)
			else -> isCircleIntersectingLineSeg(wall.v0(), wall.v1(), pos, radius)
		}
	}

	// -----------------------------------------------------------------------------------------------
	// put vector V bounced off wall W into result
	private fun bounceVectorOffWall(v: Vector2D, w: Vector2D): MutableVector2D {
		// look for best case
		if (w.x == 0f) {
			return v.scaledBy(-1f, 1f)
		} else if (w.y == 0f) {
			return v.scaledBy(1f, -1f)
		}

		// do the bounce off algorithm (Thanx AL)
		// get normal to the wall
		val n = w.norm()

		// compute N dot V
		var ndotv = n.dot(v)

		// make sure the normal is facing toward missile by comparing dot
		// products
		if (ndotv > 0.0f) {
			// reverse direction of N
			n.scaleEq(-1f)
		}
		ndotv = abs(ndotv)

		// compute N dot N
		val ndotn = n.dot(n)

		// compute projection vector
		val p = n * ndotv / ndotn

		// assign new values to missile motion
		return v + p * 2
	}

	open fun initGraphics(g: AGraphics) {
		G = g
	}

	abstract val imageKey: Int
	abstract val imageLogo: Int
	abstract val animJaws: IntArray
	abstract val animLava: IntArray
	abstract val animPeople: Array<IntArray>

	// -----------------------------------------------------------------------------------------------
	private fun drawIntroPlayers(g: AGraphics) {

		// draw the player modes
		val x1 = screen_width / 3
		val x2 = screen_width / 2
		val x3 = screen_width * 3 / 4
		var sy = 50f
		val dy = (screen_height - sy) / 5
		val textColor = GColor.RED
		val dir = DIR_DOWN
		sy += PLAYER_RADIUS / 2
		val player = players[0]
		drawPlayer(player, g, x1, sy, dir, PLAYER_COLORS[0])
		sy += dy
		player.powerup = POWERUP_HULK
		player.scale = PLAYER_HULK_SCALE
		drawPlayer(player, g, x1, sy, dir, PLAYER_COLORS[0])
		player.scale = 1f
		g.color = textColor
		g.drawString("HULK", x2, sy)
		g.drawString("Charge Enemies", x3, sy)
		sy += dy
		player.powerup = POWERUP_GHOST
		drawPlayer(player, g, x1, sy, dir, PLAYER_COLORS[0])
		g.color = textColor
		g.drawString("GHOST", x2, sy)
		g.drawString("Walk through walls", x3, sy)
		sy += dy
		player.powerup = POWERUP_BARRIER
		drawPlayer(player, g, x1, sy, dir, PLAYER_COLORS[0])
		g.color = textColor
		g.drawString("BARRIER", x2, sy)
		g.drawString("Protects against missiles", x3, sy)
		sy += dy
		player.powerup = -1 // must be last!
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawIntroWallTypes(g: AGraphics) {

		// draw the player modes
		val x1 = screen_width / 3
		val x2 = screen_width / 2
		var sy = 50f
		val dy = (screen_height - sy) / WALL_NUM_TYPES
		val textColor = GColor.RED
		val info = Wall(0, 0, 0)
		info.health = 100
		info.frequency = RUBBER_WALL_MAX_FREQUENCY
		info.frame = frameNumber % BROKEN_DOOR_CLOSED_FRAMES
		val wallSize = dy / 2 - 3
		sy += wallSize
		for (i in WALL_TYPE_NORMAL until WALL_NUM_TYPES) {
			info.type = i
			if (info.type == WALL_TYPE_BROKEN_DOOR)
				info.state = DOOR_STATE_OPENING
			else if (info.type == WALL_TYPE_DOOR)
				info.state = DOOR_STATE_LOCKED
			drawWall(g, info, x1 - wallSize, sy - wallSize, x1 + wallSize, sy + wallSize)
			g.color = textColor
			g.drawString(getWallTypeString(i), x2, sy)
			sy += dy
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawIntroPowerUps(g: AGraphics) {

		// draw the player modes
		val x1 = screen_width / 3
		val x2 = screen_width / 2
		var sy = 50f
		val dy = (screen_height - sy) / (POWERUP_NUM_TYPES + 1)
		val textColor = GColor.WHITE
		sy += 16f
		//this.drawStickFigure(g, x1, sy, this.PEOPLE_RADIUS);
		//this.drawPeople(g)
		val type = frameNumber / 30 % PEOPLE_NUM_TYPES
		val dir = frameNumber / 60 % 4
		g.color = GColor.WHITE
		this.drawPerson(g, x1, sy, 32f, type, dir)
		g.color = textColor
		g.drawString("HUMAN", x2, sy)
		sy += dy
		for (i in 0 until POWERUP_NUM_TYPES) {
			drawPowerUp(g, i, x1, sy)
			g.color = textColor
			g.drawString(getPowerupTypeString(i), x2, sy)
			sy += dy
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawIntroEnemies(g: AGraphics) {

		// draw the player modes
		val x1 = screen_width / 3
		val x2 = screen_width / 2
		val x3 = screen_width * 3 / 4
		var sy = 50f
		val textColor = GColor.RED
		val dy = (screen_height - sy) / 6
		drawRobot(g, x1, sy, DIR_DOWN)
		g.color = textColor
		g.drawString("ROBOT", x2, sy)
		g.drawString(ENEMY_ROBOT_POINTS.toString(), x3, sy)
		sy += dy
		drawBrain(g, x1, sy, 10f)
		g.color = textColor
		g.drawString("BRAIN", x2, sy)
		g.drawString(ENEMY_BRAIN_POINTS.toString(), x3, sy)
		sy += dy
		drawGenerator(g, x1, sy)
		g.color = textColor
		g.drawString("GENERATOR", x2, sy)
		g.drawString(ENEMY_GEN_POINTS.toString(), x3, sy)
		sy += dy
		drawTank(g, x1, sy, DIR_DOWN)
		g.color = textColor
		g.drawString("TANK", x2, sy)
		g.drawString(ENEMY_TANK_POINTS.toString(), x3, sy)
		sy += dy
		drawThug(g, x1, sy, DIR_DOWN)
		g.color = textColor
		g.drawString("THUG", x2, sy)
		g.drawString("INDESTRUCTABLE", x3, sy)
		sy += dy
		drawEnd(g, x1, sy)
		g.color = textColor
		g.drawString("NEXT LEVEL PORTAL", x2, sy)
		sy += dy
	}

	// -----------------------------------------------------------------------------------------------
	// Draw the intro screen showing the elems of the game and controls
	private fun drawIntro(g: AGraphics) {
		if (imageLogo >= 0) {
			g.color = GColor.WHITE
			g.drawImage(imageLogo, 0f, 0f, 256f, 256f)
		}
		val introSpacingFrames = 100
		val numIntros = 4
		val frame = frameNumber % (introSpacingFrames * (numIntros + 1))
		if (frame < introSpacingFrames) {
			drawIntroPlayers(g)
		} else if (frame < 2 * introSpacingFrames) {
			drawIntroWallTypes(g)
		} else if (frame < 3 * introSpacingFrames) {
			drawIntroPowerUps(g)
		} else if (frame < 4 * introSpacingFrames) {
			drawIntroEnemies(g)
		}

		// draw buttons for selecting the gametype
		var y = 0f
		for (i in 0 until BUTTONS_NUM) {
			if (button_active == i) {
				g.color = GColor.RED
				g.drawFilledRect(button_x[i], button_y[i], BUTTON_WIDTH, BUTTON_HEIGHT)
			}
			var outline = false
			when (Button.entries[i]) {
				Button.Classic -> if (game_type == GAME_TYPE_CLASSIC) {
					outline = true
				}

				Button.RoboCraze -> if (game_type == GAME_TYPE_ROBOCRAZE) {
					outline = true
				}

				Button.Easy -> if (difficulty == DIFFICULTY_EASY) {
					outline = true
				}

				Button.Medium -> if (difficulty == DIFFICULTY_MEDIUM) {
					outline = true
				}

				Button.Hard      -> if (difficulty == DIFFICULTY_HARD) {
					outline = true
				}

				Button.START -> {
				}
			}
			if (outline) {
				g.color = GColor.GREEN
				g.drawRect((button_x[i] + 1), (button_y[i] + 1), (BUTTON_WIDTH - 2), (BUTTON_HEIGHT - 2))
				g.drawRect((button_x[i] + 2), (button_y[i] + 2), (BUTTON_WIDTH - 4), (BUTTON_HEIGHT - 4))
			}
			g.color = GColor.CYAN
			g.drawRect(button_x[i], button_y[i], BUTTON_WIDTH, BUTTON_HEIGHT)
			g.drawString(Button.entries[i].name, (button_x[i] + 5), (button_y[i] + 5))
			y = button_y[i]
		}
		y += BUTTON_HEIGHT + 20
		val x = button_x[0]
		g.color = GColor.CYAN
		g.drawString(instructions, x, y)
	}

	fun drawCursor(g: AGraphics) {
		val (cursor_x, cursor_y) = cursor.get()
		val radius = 8f
		g.pushMatrix()
		g.translate(-radius * 2f, -radius * 2f)
		g.color = GColor.BLUE
		val COS_45 = .707f
		val cursor = Vector2D(cursor_x, cursor_y)
		val dv = MutableVector2D(COS_45, COS_45) * radius
		g.drawCircle(cursor, radius, 1f)
		g.drawLine(cursor + dv, cursor - dv, 1f)
		dv.scaleEq(-1, 1)
		g.drawLine(cursor + dv, cursor - dv, 1f)
		g.popMatrix()
	}

	// -----------------------------------------------------------------------------------------------
	@Synchronized
	fun drawGame(g: AGraphics, millisPerFrame: Int) {
		g.ortho(0f, screen_width, 0f, screen_height)
		frameNumber += 1
		g.clearScreen(GColor.BLACK)
		client?.sendInputs(player.motion_dv, player.target_dv, player.firing)
		drawCursor(g)
		when (game_state) {
			GAME_STATE_INTRO -> drawIntro(g)
			GAME_STATE_PLAY -> {
				g.pushMatrix()
				g.translate(-screen_x, -screen_y)
				client ?: updatePlayers()
				server?.broadcastPlayers(players)
				client ?: updatePeople()
				server?.broadcastPeople(people)
				client ?: updateMissiles()
				client ?: updateEnemies()
				server?.broadcastEnemies(enemies)
				client ?: updatePowerups()
				server?.broadcastPowerups(powerups)
				updateAndDrawZombieTracers(g)
				if (game_type == GAME_TYPE_ROBOCRAZE)
					server?.broadcastWalls(wall_lookup.values.filterNot { it.type == WALL_TYPE_INDESTRUCTIBLE })
				players.forEach {
					updateAndDrawPlayerTracers(it, g)
				}
				drawMaze(g)
				updateAndDrawMessages(g)
				updateAndDrawParticles(g)
				drawEnemies(g)
				drawPeople(g)
				drawPowerups(g)
				drawPlayers(g)
				drawMissiles(g)
				if (game_type != GAME_TYPE_CLASSIC) {
					val endV = cellToMazeCoords(end_cell)
					drawEnd(g, endV.x, endV.y)
				}
				drawDebug(g)
				g.popMatrix()
				drawPlayerInfo(player, g)
				if (drawDebugButtons) {
					drawDebugButtons(g)
				}
			}

			GAME_STATE_GAME_OVER -> {
				g.pushMatrix()
				g.translate(-screen_x, -screen_y)
				drawMaze(g)
				drawEnemies(g)
				drawPeople(g)
				drawMissiles(g)
				if (game_type != GAME_TYPE_CLASSIC) {
					val endV = cellToMazeCoords(end_cell)
					drawEnd(g, endV.x, endV.y)
				}
				g.popMatrix()
				drawPlayerInfo(player, g)
				drawGameOver(g)
			}
		}
		updateThrobbingWhite()
	}

	// -----------------------------------------------------------------------------------------------
	private fun isInMaze(pos: Vector2D): Boolean {
		val padding = 10f
		var minx = padding
		var miny = padding
		var maxx = WORLD_WIDTH_CLASSIC - padding
		var maxy = WORLD_HEIGHT_CLASSIC - padding
		if (game_type != GAME_TYPE_CLASSIC) {
			minx = MAZE_VERTEX_NOISE + padding
			miny = MAZE_VERTEX_NOISE + padding
			maxx = MAZE_WIDTH - MAZE_VERTEX_NOISE - padding
			maxy = MAZE_HEIGHT - MAZE_VERTEX_NOISE - padding
		}
		return pos.x > minx && pos.x < maxx && pos.y > miny && pos.y < maxy
	}

	// -----------------------------------------------------------------------------------------------
	// This is placeed close too keyTyped so we can update when necc
	private fun drawDebugButtons(g: AGraphics) {
		val w = screen_width / Debug.entries.size
		var x = w / 2
		val y = screen_height - 20
		for (d in Debug.entries) {
			if (isDebugEnabled(d)) g.color = GColor.RED else g.color = GColor.CYAN
			g.drawJustifiedString(x, y, Justify.CENTER, d.indicator)
			x += w
		}
	}

	private var debug_enabled_flag = 0

	// enum --
	enum class Debug  // show path to the end
		(val indicator: String) {
		DRAW_MAZE_INFO("Maze Info"),
		DRAW_PLAYER_INFO("Player Info"),
		DRAW_ENEMY_INFO("Enemy Info"),
		INVINCIBLE("Invincible"),  // cant die
		GHOST("Ghost"),  // cant die and walk through walls
		BARRIER("Barrier"),  // barrier allows walk through walls and protection from projectiles
		HULK("Hulk"),  // can smash through walls and enemies
		PATH("Path");
	}

	fun isDebugEnabled(debug: Debug): Boolean {
		return debug_enabled_flag and (1 shl debug.ordinal) != 0
	}

	fun setDebugEnabled(debug: Debug, enabled: Boolean) {
		debug_enabled_flag = if (enabled) {
			debug_enabled_flag or (1 shl debug.ordinal)
		} else {
			debug_enabled_flag and (1 shl debug.ordinal).inv()
		}
	}

	fun setPlayerMovement(dx: Int, dy: Int) {
		val speed = player.speed()
		if (dx < 0) {
			player.motion_dv.setX(-speed)
		} else if (dx > 0) {
			player.motion_dv.setX(speed)
		} else {
			player.motion_dv.setX(0)
		}
		if (dy < 0) {
			player.motion_dv.setY(-speed)
		} else if (dy > 0) {
			player.motion_dv.setY(speed)
		} else {
			player.motion_dv.setY(0)
		}
	}

	fun setPlayerMovement(dx: Float, dy: Float) {
		val speed = player.speed()
		player.motion_dv.assign(dx, dy).scaleEq(speed)
	}

	val isPlayerSpectator: Boolean
		get() = player.state == PLAYER_STATE_SPECTATOR

	// -----------------------------------------------------------------------------------------------
	private fun setIntroButtonPositionAndDimension() {
		val x = 8f
		var y = 50f
		for (i in 0 until BUTTONS_NUM) {
			button_x[i] = x
			button_y[i] = y
			y += BUTTON_HEIGHT + 5
		}
	}

	abstract val clock: Long

	@Omit
	private var downTime: Long = 0
	fun setCursorPressed(pressed: Boolean) {
		if (pressed) {
			when (game_state) {
				GAME_STATE_INTRO -> downTime = clock
				GAME_STATE_PLAY -> player.firing = true
			}
		} else {
			when (game_state) {
				GAME_STATE_INTRO -> {
					if (downTime > 0 && clock - downTime < 1000) {
						newGame()
					}
					downTime = 0
				}

				GAME_STATE_PLAY -> player.firing = false
			}
		}
	}

	fun setPlayerMissileVector(dx: Float, dy: Float) {
		player.target_dv.assign(dx, dy)
	}

	fun setPlayerFiring(firing: Boolean) {
		player.firing = firing
	}

	private fun newPlayerGame(player: Player) {
		player.lives = PLAYER_START_LIVES
		player.score = 0
	}

	private fun newGame() {
		if (button_active in 0..<BUTTONS_NUM) {
			when (Button.entries[button_active]) {
				Button.Classic -> game_type = GAME_TYPE_CLASSIC
				Button.RoboCraze -> game_type = GAME_TYPE_ROBOCRAZE
				Button.Easy -> difficulty = DIFFICULTY_EASY
				Button.Medium -> difficulty = DIFFICULTY_MEDIUM
				Button.Hard -> difficulty = DIFFICULTY_HARD
				Button.START -> {
					button_active = -1
					gameLevel = 1
					game_state = GAME_STATE_PLAY
					players.forEach {
						newPlayerGame(it)
					}
					buildAndPopulateLevel()
				}
			}
			server?.broadcastNewGame(this)
		}
	}

	fun setCursor(x: Int, y: Int) {
		cursor.get().assign(x, y)
		when (game_state) {
			GAME_STATE_INTRO -> {
				client?.let { return }
				button_active = -1
				var i = 0
				while (i < BUTTONS_NUM) {
					if (x > button_x[i] && x < button_x[i] + BUTTON_WIDTH && y > button_y[i] && y < button_y[i] + BUTTON_HEIGHT) {
						button_active = i
					}
					i++
				}
			}

			GAME_STATE_PLAY -> {
				player.screen.topLeft.add(x, y, player.target_dv) -= player.pos
			}
		}
	}

	fun setGameStateIntro() {
		game_state = GAME_STATE_INTRO
	}

	fun gameOver() {
		game_state = GAME_STATE_GAME_OVER
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) && other is Robotron
			&& players.contentEquals(other.players)
			&& enemy_missiles.contentEquals(other.enemy_missiles)
			&& tank_missiles.contentEquals(other.tank_missiles)
			&& snake_missiles.contentEquals(other.snake_missiles)
			&& powerups.contentEquals(other.powerups)
			&& enemies.contentEquals(other.enemies)
			&& particles.contentEquals(other.particles)
			&& messages.contentEquals(other.messages)
			&& people.contentEquals(other.people)
	}

	override fun deserialize(input: RBufferedReader) {
		// there may be a way to avoid this by making sure that, at the end of the operation,
		// for all walls: w[v0][v1] === w[v1][v0] (same object)
		throw UnsupportedOperationException("Must use merge because of the structure of the walls digraph cannot allow multiple instances of same wall.")
	}

	fun serialize(buffer: ByteBuffer) {
		buffer.writeInt(high_score)
	}

	fun deserialze(buffer: ByteBuffer) {
		high_score = buffer.readInt()
	}

}