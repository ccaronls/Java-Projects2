package cc.game.superrobotron

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.math.CMath
import cc.lib.utils.random
import java.util.Arrays
import java.util.LinkedList
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt



// Independent transferable
abstract class Robotron() {

	companion object {
		var GAME_VISIBILITY = false
		val MAX_PLAYERS = 8
		val log = LoggerFactory.getLogger(Robotron::class.java)
	}
	
	// ---------------------------------------------------------//
	// STRUCTS //
	// ---------------------------------------------------------//
	// DATA for projectiles -----------------------------
	// TODO: Decide! Objects or arrays!

	// - this.game_level,
	private val wallChanceForLevel: IntArray
		get() = intArrayOf(
			0,
			80,  // - this.game_level,
			2 + gameLevel,
			10 + gameLevel,
			5 + gameLevel,
			5 + gameLevel,
			0 + gameLevel,
			0
		)

	open val instructions = "Use 'WASD' keys to move\nUse mouse to fire"


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
	private val WALL_NORTH = 1
	private val WALL_EAST = 2
	private val WALL_SOUTH = 4
	private val WALL_WEST = 8
	private lateinit var maze_cells: Array<IntArray>
	val maze_verts_x = IntArray(MAZE_NUM_VERTS) // array of x components
	val maze_verts_y = IntArray(MAZE_NUM_VERTS) // array of y components
	private val wall_lookup: Array<Array<Wall>> = Array(MAZE_NUM_VERTS) {
		Array(MAZE_NUM_VERTS) { Wall() }
	}

	// Rectangle of visible maze
	var screen_x = 0
		private set
	var screen_y = 0
		private set

	// rectangle of maze dimension
	private var verts_min_x = 0
	private var verts_min_y = 0
	private var verts_max_x = MAZE_WIDTH
	private var verts_max_y = MAZE_HEIGHT

	// position of ending 'spot' or goal
	private var end_x = 0
	private var end_y = 0

	// PLAYER DATA
	val players = Array(MAX_PLAYERS) { Player() }
	var num_players = 0
	var this_player = 0
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

	// set by collisionScanCircle/Line
	private var collision_info_v0 = -1
	private var collision_info_v1 = -1
	private var collision_info_wallinfo: Wall? = null

	// General use arrays
	val vec = FloatArray(2)
	val transform = FloatArray(4)
	val int_array = IntArray(4)
	val enemy_missle = Array(MAX_ENEMY_MISSLES) { MissileFloat() }
	val tank_missle = Array(MAX_TANK_MISSLES) { MissileInt() }
	val snake_missle = Array(MAX_SNAKE_MISSLES) { MissileSnake() }
	val powerups = Array(MAX_POWERUPS) { Powerup() }

	// ENEMIES ----------------------------
	val enemy_x = IntArray(MAX_ENEMIES)
	val enemy_y = IntArray(MAX_ENEMIES)
	val enemy_type = IntArray(MAX_ENEMIES)
	val enemy_next_update = IntArray(MAX_ENEMIES)
	val enemy_spawned_frame = IntArray(MAX_ENEMIES)
	val enemy_killable = BooleanArray(MAX_ENEMIES)
	val enemy_radius = IntArray(ENEMY_INDEX_NUM)
	private var enemy_robot_speed = 0

	// EXPLOSIN EFFECTS --------------------
	var particles = Array(MAX_PARTICLES) { Particle() }

	// MSGS that drift away ---------------------------
	val msg_x = IntArray(MESSAGES_MAX)
	val msg_y = IntArray(MESSAGES_MAX)
	val msg_string = Array(MESSAGES_MAX) { "" }
	private var cursor_x = 0
	private var cursor_y = 0

	val msg_color = Array<GColor>(MESSAGES_MAX) { GColor.TRANSPARENT }

	// PEOPLE ------------------------------
	val people_x = IntArray(MAX_PEOPLE)
	val people_y = IntArray(MAX_PEOPLE)
	val people_state = IntArray(MAX_PEOPLE) // 0 = unused, 1 =
	val people_type = IntArray(MAX_PEOPLE)

	// north, 2 = east, 3 =
	// south, 4 = west
	private var people_points = PEOPLE_START_POINTS
	private var people_picked_up = 0

	// Zombie tracers ----------------------
	val zombie_tracer_x = IntArray(MAX_ZOMBIE_TRACERS)
	val zombie_tracer_y = IntArray(MAX_ZOMBIE_TRACERS)
	val zombie_tracer_color = Array(MAX_ZOMBIE_TRACERS) { GColor.TRANSPARENT }

	// Player tracers ----------------------
	// Other lookups
	// Maps N=0,E,S,W to a 4 dim array lookup
	val move_dx = intArrayOf(0, 1, 0, -1)
	val move_dy = intArrayOf(-1, 0, 1, 0)
	val move_diag_dx = intArrayOf(1, -1, 1, -1)
	val move_diag_dy = intArrayOf(-1, 1, -1, 1)
	private var throbbing_white = GColor.WHITE.deepCopy()
	private var throbbing_dir = 0 // 0 == darker(), 1 == lighter()
	private var insta_msg_color: GColor = GColor.YELLOW
	private var insta_msg_str: String? = null

	// OPTIMIZATION - Use better Add / Remove object algorithm to get O(1) time
	// vs O(n) time
	private var num_enemies = 0
	private var num_enemy_missles = 0
	private var num_tank_missles = 0
	private var num_snake_missles = 0
	private var num_people = 0
	private var num_zombie_tracers = 0
	private var num_msgs = 0
	private var num_particles = 0
	private var num_powerups = 0
	val button_x = IntArray(BUTTONS_NUM)
	val button_y = IntArray(BUTTONS_NUM)
	private var button_active = -1 // -1 == none, 1 == button 1, 2 == button 2,

	// ...
	private var difficulty = DIFFICULTY_EASY // Easy == 0, Med == 1, Hard == 2
	var screen_width = 0
		private set
	var screen_height = 0
		private set
	private var frameNumber = 0
	lateinit var G: AGraphics

	fun setDimension(screenWidth: Int, screenHeight: Int) {
		screen_height = screenHeight
		screen_width = screenWidth
		setIntroButtonPositionAndDimension()
	}

	// DEBUG Drawing stuff
	// final int [] debug_player_primary_verts = new int[5]; // used by
	// MEMBERS
	// //////////////////////////////////////////////////////////////////////
	// -----------------------------------------------------------------------------------------------
	// draw any debug related stuff
	private fun drawDebug(g: AGraphics) {
		var x: Int
		var y: Int
		val player = player

		g.color = GColor.GREEN
		val verts = IntArray(5)
		if (isDebugEnabled(Debug.DRAW_MAZE_INFO)) {
			computePrimaryVerts(player.x, player.y, verts)
			g.color = GColor.GREEN
			var radius = 10
			for (i in verts.indices) {
				val v = verts[i]
				if (v < 0 || v >= MAZE_NUM_VERTS) continue
				x = maze_verts_x[v] - screen_x
				y = maze_verts_y[v] - screen_y
				g.drawFilledCircle(x, y, radius)
				radius = 3
				g.drawString(v.toString(), (x + 15).toFloat(), (y + 15).toFloat())
			}
			g.color = GColor.ORANGE
			computePrimaryQuadrant(player.x, player.y, verts)
			for (i in 0..3) {
				val v = verts[i]
				if (v < 0 || v >= MAZE_NUM_VERTS) continue
				x = maze_verts_x[v] - screen_x
				y = maze_verts_y[v] - screen_y
				g.drawFilledCircle(x, y, 5)
			}
			g.color = GColor.GREEN
			for (i in maze_verts_x.indices) {
				x = maze_verts_x[i]
				y = maze_verts_y[i]
				if (isOnScreen(x, y)) {
					x -= screen_x
					y -= screen_y
					//g.drawOval(x - 2, y - 2, 4, 4);
					g.drawString(i.toString(), (x + 10).toFloat(), (y + 10).toFloat())
				}
			}
			y = 0
			g.color = GColor.BLUE
			for (i in 0 until MAZE_NUMCELLS_Y) {
				x = 0
				for (j in 0 until MAZE_NUMCELLS_X) {
					g.drawRect((x - screen_x).toFloat(), (y - screen_y).toFloat(), MAZE_CELL_WIDTH.toFloat(), MAZE_CELL_HEIGHT.toFloat())
					x += MAZE_CELL_WIDTH
				}
				y += MAZE_CELL_HEIGHT
			}
			drawDebugWallInfo(g)
		}
		if (isDebugEnabled(Debug.DRAW_PLAYER_INFO)) {
			/*
            g.setColor(GColor.WHITE);
            int px = player.x - screen_x + PLAYER_RADIUS * 2;
            int py = player.y - screen_y - 2;
            int mx = getMouseX();
            int my = getMouseY();
            String msg = "(" + player.x + ", " + player.y + ")"
                       + "\n<" + player.dx + ", " + player.dy + ">";
            msg += "(" + mx + ", " + my + ")";
            g.drawJustifiedString(mx, my, Justify.LEFT, Justify.CENTER, msg);
*/
			computePrimaryVerts(player.x, player.y, verts)
			g.color = GColor.GREEN
			var radius = 10
			for (i in verts.indices) {
				val v = verts[i]
				if (v < 0 || v >= MAZE_NUM_VERTS) continue
				x = maze_verts_x[v] - screen_x
				y = maze_verts_y[v] - screen_y
				g.drawFilledCircle(x, y, radius)
				radius = 7
				g.drawString(v.toString(), (x + 15).toFloat(), (y + 15).toFloat())
			}
			for (i in 1 until verts.size) {
				val info = getWall(verts[i], verts[0])
				if (info.type < 0)
					continue
				val x0 = maze_verts_x[info.v0] - screen_x
				val y0 = maze_verts_y[info.v0] - screen_y
				val x1 = maze_verts_x[info.v1] - screen_x
				val y1 = maze_verts_y[info.v1] - screen_y
				val mx = (x0 + x1) / 2
				val my = (y0 + y1) / 2
				g.color = GColor.WHITE
				g.drawString(info.toString(), mx.toFloat(), my.toFloat())
			}
			g.color = GColor.ORANGE
			computePrimaryQuadrant(player.x, player.y, verts)
			for (i in 0..3) {
				val v = verts[i]
				if (v < 0 || v >= MAZE_NUM_VERTS) continue
				x = maze_verts_x[v] - screen_x
				y = maze_verts_y[v] - screen_y
				g.drawFilledCircle(x, y, 4)
			}
			player.collision_info?.let { collision ->
				val x0 = maze_verts_x[collision.v0] - screen_x
				val y0 = maze_verts_y[collision.v0] - screen_y
				val x1 = maze_verts_x[collision.v1] - screen_x
				val y1 = maze_verts_y[collision.v1] - screen_y
				g.color = GColor.RED
				g.drawLine(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat(), 5f)
			}
		}
		if (isDebugEnabled(Debug.PATH) && player.path.size > 1) {
			var s = player.path[0]
			for (i in 1 until player.path.size) {
				val n = player.path[i]
				val x0 = s[0] * MAZE_CELL_WIDTH + MAZE_CELL_WIDTH / 2 - screen_x
				val y0 = s[1] * MAZE_CELL_HEIGHT + MAZE_CELL_HEIGHT / 2 - screen_y
				val x1 = n[0] * MAZE_CELL_WIDTH + MAZE_CELL_WIDTH / 2 - screen_x
				val y1 = n[1] * MAZE_CELL_HEIGHT + MAZE_CELL_HEIGHT / 2 - screen_y
				s = n
				g.color = GColor.GREEN
				g.drawLine(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat(), 3f)
			}
		}
		g.color = GColor.WHITE
		//String msg = "frame: " + getFrameNumber();
		//g.drawJustifiedString(5, screen_height-5, Justify.LEFT, Justify.BOTTOM, msg);
		//val dx = g.drawJustifiedString(5f, (screen_height - 5).toFloat(), Justify.LEFT, "frame: ").height
		//drawNumberString(g, 5f + dx, screen_height.toFloat() - 5, Justify.LEFT, frameNumber)
	}

	// -----------------------------------------------------------------------------------------------
	private fun getPlayerSpeed(player: Player): Int {
		var speed = PLAYER_SPEED
		speed = Utils.clamp(speed - numSnakesAttached, 1, 100)
		if (player.powerup == POWERUP_SUPER_SPEED) speed += PLAYER_SUPER_SPEED_BONUS
		if (isHulkActiveCharging(player)) speed += PLAYER_HULK_CHARGE_SPEED_BONUS
		return speed
	}

	// -----------------------------------------------------------------------------------------------
	val numSnakesAttached: Int
		get() {
			var num = 0
			for (i in 0 until num_snake_missles) {
				if (snake_missle[i].state == SNAKE_STATE_ATTACHED) num++
			}
			return num
		}

	// -----------------------------------------------------------------------------------------------
	fun addPowerup(x: Int, y: Int, type: Int): Int {
		val p = if (num_powerups < MAX_POWERUPS) {
			log.debug("addPowerup x[" + x + "] y[" + y + "] type [" + getPowerupTypeString(type) + "]")
			powerups[num_powerups++]
		} else {
			val index = random(MAX_POWERUPS)
			log.debug("replace Powerup [" + index + "] with x[" + x + "] y[" + y + "] type [" + getPowerupTypeString(type) + "]")
			powerups[index]
		}
		p.init(x.toFloat(), y.toFloat(), Utils.randFloatX(1f), Utils.randFloatX(1f), 0, type)
		return num_powerups
	}

	// -----------------------------------------------------------------------------------------------
	private fun removePowerup(index: Int) {
		if (index < num_powerups - 1) {
			val p0 = powerups[index]
			val p1 = powerups[num_powerups - 1]
			p0.copy(p1)
		}
		num_powerups -= 1
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawPowerup(g: AGraphics, type: Int, x: Int, y: Int) {
		if (type == POWERUP_KEY) {
			val dim = 20
			g.color = GColor.WHITE
			g.drawImage(imageKey, (x - dim / 2).toFloat(), (y - dim / 2).toFloat(), dim.toFloat(), dim.toFloat())
		} else {
			g.color = throbbing_white
			val r = POWERUP_RADIUS + frameNumber % 12 / 4
			g.drawCircle(x.toFloat(), y.toFloat(), r.toFloat()) //(x - r, y - r, r * 2, r * 2);
			g.color = GColor.RED
			if (type == POWERUP_BONUS_PLAYER) {
				drawStickFigure(g, x, y, POWERUP_RADIUS)
			} else {
				//int h = Utils.getTextHeight();//g.getFontMetrics().getHeight();
				val c = getPowerupTypeString(type)[0]
				//int w = Utils.getTextWidth("" + c);//Utils.cog.getFontMetrics().charWidth(c);
				//g.drawString(String.valueOf(c), x - w / 2 + 1, y + h / 2 - 3);
				g.drawJustifiedString(x.toFloat(), y.toFloat(), Justify.CENTER, Justify.CENTER, c.toString())
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawPowerups(g: AGraphics) {
		for (i in 0 until num_powerups) {
			val p = powerups[i]
			val x = Math.round(p.x - screen_x)
			val y = Math.round(p.y - screen_y)
			drawPowerup(g, p.type, x, y)
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun updatePowerups() {
		var i = 0
		while (i < num_powerups) {
			val p = powerups[i]
			if (!isOnScreen(Math.round(p.x), Math.round(p.y))) {
				removePowerup(i)
				continue
			}
			if (p.duration > POWERUP_MAX_DURATION) {
				removePowerup(i)
				continue
			}
			for (ii in 0 until num_players) {
				val player = players[ii]
				// see if the player is picking me up
				val dx = player.x - p.x
				val dy = player.y - p.y
				val min = (getPlayerRadius(player) + POWERUP_RADIUS).toFloat()
				val len2 = dx * dx + dy * dy
				if (len2 < min * min) {
					val msgX = player.x + random(-30 .. 30)
					val msgY = player.y + random(-30 .. 30)
					addMsg(msgX, msgY, getPowerupTypeString(p.type))
					when (p.type) {
						POWERUP_BONUS_PLAYER -> player.lives += 1
						POWERUP_BONUS_POINTS -> {
							val points = random(1.. POWERUP_BONUS_POINTS_MAX) * POWERUP_POINTS_SCALE
							addPoints(player, points)
							addMsg(msgX + 20, msgY, points.toString())
						}
						POWERUP_KEY          -> {
							player.keys++
							addPlayerMsg(player, "Found a key!")
						}
						else                 -> setPlayerPowerup(player, p.type)
					}
					removePowerup(i)
					continue
				}
				i++
			}
		}
		if (frameNumber % (1000 - gameLevel * 100) == 0) {
			for (i in 0 until num_players) addRandomPowerup(players[i].x, players[i].y, 100)
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun addRandomPowerup(x: Int, y: Int, radius: Int) {
		log.debug("addRandomPowerup x[$x] y[$y] radius + [$radius]")
		val powerup = Utils.chooseRandomFromSet(*POWERUP_CHANCE)

		// final int range = radius * 10;
		val minRange = radius * 2
		val maxRange = radius * 20
		val maxTries = 100
		for (i in 0 until maxTries) {
			var dx = random(minRange .. maxRange)
			var dy = random(minRange .. maxRange)
			if (Utils.flipCoin()) dx = -dx
			if (Utils.flipCoin()) dy = -dy
			val newx = x + dx
			val newy = y + dy
			if (!isInMaze(newx, newy)) continue
			if (collisionScanCircle(newx, newy, POWERUP_RADIUS + 5)) continue
			addPowerup(newx, newy, powerup)
			break
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun setPlayerPowerup(player: Player, type: Int) {
		player.powerup = type
		player.powerup_duration = 0
	}

	// -----------------------------------------------------------------------------------------------
	// remove the tank missle at index
	private fun removeTankMissle(index: Int) {
		num_tank_missles--
		val m1 = tank_missle[index]
		val m2 = tank_missle[num_tank_missles]
		m1.copy(m2)
	}

	// -----------------------------------------------------------------------------------------------
	// append a tank missle
	private fun addTankMissle(x: Int, y: Int) {
		if (num_tank_missles == MAX_TANK_MISSLES) return
		val m = tank_missle[num_tank_missles]
		val index = random(0.. 3)
		m.init(x, y, TANK_MISSLE_SPEED * move_diag_dx[index], TANK_MISSLE_SPEED * move_diag_dy[index], TANK_MISSLE_DURATION)
		num_tank_missles++
	}

	// -----------------------------------------------------------------------------------------------
	// remove the zombie tracer at index
	private fun removeZombieTracer(index: Int) {
		num_zombie_tracers--
		zombie_tracer_x[index] = zombie_tracer_x[num_zombie_tracers]
		zombie_tracer_y[index] = zombie_tracer_y[num_zombie_tracers]
		zombie_tracer_color[index] = zombie_tracer_color[num_zombie_tracers]
	}

	// -----------------------------------------------------------------------------------------------
	// append a zombie tracer
	private fun addZombieTracer(x: Int, y: Int) {
		if (num_zombie_tracers == MAX_ZOMBIE_TRACERS) return
		zombie_tracer_x[num_zombie_tracers] = x
		zombie_tracer_y[num_zombie_tracers] = y
		zombie_tracer_color[num_zombie_tracers] = GColor.WHITE
		num_zombie_tracers++
	}

	// -----------------------------------------------------------------------------------------------
	// remove the zombie tracer at index
	private fun removePlayerTracer(player: Player, index: Int) {
		player.num_tracers--
		player.tracer_x[index] = player.tracer_x[player.num_tracers]
		player.tracer_y[index] = player.tracer_y[player.num_tracers]
		player.tracer_color[index] = player.tracer_color[player.num_tracers]
		player.tracer_dir[index] = player.tracer_dir[player.num_tracers]
	}

	// -----------------------------------------------------------------------------------------------
	// append a zombie tracer
	private fun addPlayerTracer(player: Player, x: Int, y: Int, dir: Int, color: GColor) {
		if (player.num_tracers == PLAYER_SUPERSPEED_NUM_TRACERS) return
		player.tracer_x[player.num_tracers] = x
		player.tracer_y[player.num_tracers] = y
		player.tracer_color[player.num_tracers] = color
		player.tracer_dir[player.num_tracers] = dir
		player.num_tracers++
	}

	// -----------------------------------------------------------------------------------------------
	// replace the snake at index with tail elem, and decrement
	private fun removeSnakeMissle(index: Int) {
		log.debug("Removing snake missle [$index]")
		num_snake_missles--
		val m0 = snake_missle[index]
		val m1 = snake_missle[num_snake_missles]
		m0.copy(m1)
	}

	// -----------------------------------------------------------------------------------------------
	// append a snake missle at x,y
	fun addSnakeMissle(x: Int, y: Int): Int {
		if (num_snake_missles == MAX_SNAKE_MISSLES) return -1
		val m = snake_missle[num_snake_missles]
		m.init(x, y, 0, SNAKE_STATE_CHASE)
		return num_snake_missles++
	}

	// -----------------------------------------------------------------------------------------------
	// Do colliison scan and motion on all missles
	private fun updateSnakeMissles(player: Player) {
		val framesPerSection = SNAKE_SECTION_LENGTH / SNAKE_SPEED
		val playerRadius = getPlayerRadius(player)
		// cache player position
		val px = player.x - playerRadius
		val py = player.y - playerRadius
		val pw = playerRadius * 2
		val ph = playerRadius * 2
		var i = 0
		while (i < num_snake_missles) {
			val ms = snake_missle[i]
			if (ms.duration > SNAKE_DURATION) {
				killSnakeMissle(i)
			}
			ms.duration++
			val factor = SNAKE_HEURISTIC_FACTOR
			when (ms.state) {
				SNAKE_STATE_ATTACHED, SNAKE_STATE_CHASE -> {
					// advance the head
					val dx = move_dx[ms.dir[0]] * SNAKE_SPEED
					val dy = move_dy[ms.dir[0]] * SNAKE_SPEED
					ms.x += dx
					ms.y += dy
					val touchSection = collisionMissileSnakeRect(ms, px.toFloat(), py.toFloat(), pw.toFloat(), ph.toFloat())
					if (ms.state == SNAKE_STATE_CHASE) {
						if (touchSection == 0 && playerHit(player, HIT_TYPE_SNAKE_MISSLE, i)) {
							log.debug("Snake [$i] got the player")
							ms.state = SNAKE_STATE_ATTACHED
						}
						if (touchSection < 0 || !playerHit(player, HIT_TYPE_SNAKE_MISSLE, i)) {
							log.debug("Snake [$i] lost hold of the player")
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
						var newDir = enemyDirectionHeuristic(ms.x, ms.y, factor)
						if (newDir == reverseDir) {
							// choose right or left
							newDir = if (Utils.flipCoin()) {
								(newDir + 1) % 4
							} else {
								(newDir + 3) % 4
							}
						}
						ms.dir[0] = newDir
					}
				}
				SNAKE_STATE_DYING                       -> {
					// shift elems over
					for (ii in 0 until ms.num_sections - 1) {
						ms.dir[ii] = ms.dir[ii + 1]
					}
					ms.num_sections--
					if (ms.num_sections == 0) {
						removeSnakeMissle(i)
						continue
					}
				}
				else                                    -> Utils.unhandledCase(ms.state)
			}
			i++
		}
	}

	// -----------------------------------------------------------------------------------------------
	// return the section that touched or -1 for no collision
	private fun collisionMissileSnakeRect(ms: MissileSnake, x: Float, y: Float, w: Float, h: Float): Int {
		val framesPerSection = SNAKE_SECTION_LENGTH / SNAKE_SPEED
		val headLen = ms.duration % framesPerSection
		val tailLen = framesPerSection - headLen
		var x0 = ms.x
		var y0 = ms.y
		for (i in 0 until ms.num_sections) {
			var len = SNAKE_SECTION_LENGTH
			if (i == 0) len = headLen else if (i == SNAKE_MAX_SECTIONS - 1) len = tailLen
			val rDir = (ms.dir[i] + 2) % 4
			val x1 = x0 + move_dx[rDir] * len
			val y1 = y0 + move_dy[rDir] * len
			val xx = x0.coerceAtMost(x1)
			val yy = y0.coerceAtMost(y1)
			val ww = abs(x0 - x1)
			val hh = abs(y0 - y1)
			if (Utils.isBoxesOverlapping(x, y, w, h, xx.toFloat(), yy.toFloat(), ww.toFloat(), hh.toFloat())) {
				// log.debug("Snake Collision [" + i + "]");
				return i
			}
			x0 = x1
			y0 = y1
		}
		return -1
	}

	// -----------------------------------------------------------------------------------------------
	// Draw all the 'snakes'
	private fun drawSnakeMissles(g: AGraphics) {
		val framesPerSection = SNAKE_SECTION_LENGTH / SNAKE_SPEED
		for (i in 0 until num_snake_missles) {
			val m = snake_missle[i]
			if (!isOnScreen(m.x, m.y)) continue
			var x0 = m.x - screen_x
			var y0 = m.y - screen_y

			// draw the first section
			var d = (m.dir[0] + 2) % 4 // get reverse direction
			var frames = m.duration % framesPerSection
			var x1 = x0 + move_dx[d] * SNAKE_SPEED * frames
			var y1 = y0 + move_dy[d] * SNAKE_SPEED * frames
			g.color = GColor.BLUE
			drawBar(g, x0, y0, x1, y1, SNAKE_THICKNESS, 0)

			// draw the middle sections
			for (ii in 1 until m.num_sections) {
				x0 = x1
				y0 = y1
				d = (m.dir[ii] + 2) % 4
				x1 = x0 + move_dx[d] * SNAKE_SPEED * framesPerSection
				y1 = y0 + move_dy[d] * SNAKE_SPEED * framesPerSection
				drawBar(g, x0, y0, x1, y1, SNAKE_THICKNESS, ii)
			}

			// draw the tail if we are at max sections
			if (m.num_sections == SNAKE_MAX_SECTIONS) {
				d = (d + 2) % 4
				frames = framesPerSection - frames
				x0 = x1
				y0 = y1
				x1 = x0 + move_dx[d] * SNAKE_SPEED * frames
				y1 = y0 + move_dy[d] * SNAKE_SPEED * frames
				drawBar(g, x0, y0, x1, y1, SNAKE_THICKNESS, SNAKE_MAX_SECTIONS - 1)
			}

			// draw the head
			g.color = throbbing_white
			g.drawFilledRect((m.x - screen_x - SNAKE_THICKNESS).toFloat(), (m.y - screen_y - SNAKE_THICKNESS).toFloat(), (SNAKE_THICKNESS * 2).toFloat(), (SNAKE_THICKNESS * 2).toFloat())
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun killSnakeMissle(index: Int) {
		snake_missle[index].state = SNAKE_STATE_DYING
	}

	// -----------------------------------------------------------------------------------------------
	// add the maximum number of people randomly around the maze (but not on an
	// edge)
	private fun addPeople() {
		for (i in 0 until MAX_PEOPLE) {
			while (true) {
				people_x[i] = random(verts_min_x + MAZE_VERTEX_NOISE * 3 .. verts_max_x - MAZE_VERTEX_NOISE * 3)
				people_y[i] = random(verts_min_y + MAZE_VERTEX_NOISE * 3 .. verts_max_y - MAZE_VERTEX_NOISE * 3)
				if (!collisionScanCircle(people_x[i], people_y[i], PEOPLE_RADIUS * 2)) break
			}
			people_state[i] = random(1.. 4)
			people_type[i] = random(PEOPLE_NUM_TYPES)
		}
		num_people = MAX_PEOPLE
	}

	// -----------------------------------------------------------------------------------------------
	// remove the people at an iindex
	private fun removePeople(index: Int) {
		num_people--
		people_x[index] = people_x[num_people]
		people_y[index] = people_y[num_people]
		people_state[index] = people_state[num_people]
		people_type[index] = people_type[num_people]
	}

	// -----------------------------------------------------------------------------------------------
	// remove enemy tank at index
	private fun removeEnemy(index: Int) {
		num_enemies--
		enemy_x[index] = enemy_x[num_enemies]
		enemy_y[index] = enemy_y[num_enemies]
		enemy_type[index] = enemy_type[num_enemies]
		enemy_next_update[index] = enemy_next_update[num_enemies]
		enemy_spawned_frame[index] = enemy_spawned_frame[num_enemies]
		enemy_killable[index] = enemy_killable[num_enemies]
	}

	// -----------------------------------------------------------------------------------------------
	// Add an enemy. If the table is full, replace the oldest GUY.
	private fun addEnemy(x: Int, y: Int, type: Int, killable: Boolean) {
		var best = -1
		var oldest = frameNumber
		if (num_enemies == MAX_ENEMIES) {
			// look for oldest
			for (i in 0 until MAX_ENEMIES) {
				if (isOnScreen(enemy_x[i], enemy_y[i])) continue  // dont swap with guys on the screen
				if (enemy_type[i] >= ENEMY_INDEX_ROBOT_N && enemy_type[i] <= ENEMY_INDEX_ROBOT_W && enemy_next_update[i] < oldest) {
					oldest = enemy_next_update[i]
					best = i
				}
			}
		} else {
			best = num_enemies++
		}
		if (best >= 0) {
			enemy_type[best] = type
			enemy_x[best] = x
			enemy_y[best] = y
			enemy_next_update[best] = frameNumber + random(5 .. 10)
			enemy_spawned_frame[best] = frameNumber + random(10 .. 20)
			enemy_killable[best] = killable
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Draw the tracer when > black and update fade away color
	private fun updateAndDrawZombieTracers(g: AGraphics) {
		val update = frameNumber % ENEMY_ZOMBIE_TRACER_FADE == 0
		var i = 0
		while (i < num_zombie_tracers) {
			if (!isOnScreen(zombie_tracer_x[i], zombie_tracer_y[i]) || zombie_tracer_color[i] == GColor.BLACK) {
				removeZombieTracer(i)
				continue
			}
			g.color = zombie_tracer_color[i]
			drawStickFigure(g, zombie_tracer_x[i] - screen_x, zombie_tracer_y[i] - screen_y, ENEMY_ZOMBIE_RADIUS)
			if (update) zombie_tracer_color[i] = zombie_tracer_color[i].darkened(DARKEN_AMOUNT)
			i++
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Draw the tracer when > black and update fade away color
	private fun updateAndDrawPlayerTracers(player: Player, g: AGraphics) {

		// boolean update = getFrameNumber() % ENEMY_ZOMBIE_TRACER_FADE == 0;
		var i = 0
		while (i < player.num_tracers) {
			if (!isOnScreen(player.tracer_x[i], player.tracer_y[i]) || player.tracer_color[i] == GColor.BLACK) {
				removePlayerTracer(player, i)
				continue
			}
			drawPlayerBody(player, g, player.tracer_x[i] - screen_x, player.tracer_y[i] - screen_y, player.tracer_dir[i], player.tracer_color[i])
			player.tracer_color[i] = player.tracer_color[i].darkened(DARKEN_AMOUNT)
			i++
		}
	}

	// -----------------------------------------------------------------------------------------------
	// this draws the collapsing indicator box a the beginning of play
	private fun drawPlayerHighlight(player: Player, g: AGraphics) {
		val frame = frameNumber - game_start_frame
		val left = (player.x - screen_x) * frame / PLAYER_SPAWN_FRAMES
		val top = (player.y - screen_y) * frame / PLAYER_SPAWN_FRAMES
		val w = screen_width - screen_width * frame / PLAYER_SPAWN_FRAMES
		val h = screen_height - screen_height * frame / PLAYER_SPAWN_FRAMES
		g.color = GColor.RED
		g.drawRect(left.toFloat(), top.toFloat(), w.toFloat(), h.toFloat(), 1f)
	}

	// -----------------------------------------------------------------------------------------------
	// Add points to the player's score and update highscore and players lives
	open fun addPoints(player: Player, amount: Int) {
		val before = player.score / PLAYER_NEW_LIVE_SCORE
		if ((player.score + amount) / PLAYER_NEW_LIVE_SCORE > before) {
			setInstaMsg("EXTRA MAN!")
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
				if (throbbing_white == GColor.BLACK) throbbing_dir = 1
			} else {
				throbbing_white = throbbing_white.lightened(LIGHTEN_AMOUNT)
				if (throbbing_white == GColor.WHITE) throbbing_dir = 0
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Draw the game over text
	private fun drawGameOver(g: AGraphics) {
		val x = screen_width / 2
		val y = screen_height / 2
		g.color = throbbing_white
		g.drawJustifiedString(x.toFloat(), y.toFloat(), Justify.CENTER, "G A M E   O V E R")
	}

	// -----------------------------------------------------------------------------------------------
	// Set the insta message
	private fun setInstaMsg(s: String) {
		insta_msg_color = GColor.YELLOW
		insta_msg_str = s
	}

	// -----------------------------------------------------------------------------------------------
	// draw the score, high score, and number of remaining players
	private fun drawPlayerInfo(player: Player, g: AGraphics) {
		val text_height = 16f
		// draw the score
		var x = TEXT_PADDING.toFloat()
		var y = TEXT_PADDING.toFloat()
		val x0 = x
		var hJust = Justify.LEFT
		g.color = GColor.WHITE
		g.drawJustifiedString(x, y, hJust, String.format("Score %d", player.score))
		y += text_height
		g.drawJustifiedString(x, y, hJust, String.format("Lives X %d", player.lives))
		y += text_height
		g.drawJustifiedString(x, y, hJust, String.format("People X %d", people_picked_up))
		y += text_height
		g.drawJustifiedString(x, y, hJust, String.format("Keys X %d", player.keys))
		x = screen_width.toFloat() / 2
		y = TEXT_PADDING.toFloat()
		hJust = Justify.CENTER
		g.drawJustifiedString(x, y, hJust, String.format("High Score %d", high_score))
		x = screen_width.toFloat()
		y = TEXT_PADDING.toFloat()
		hJust = Justify.RIGHT
		g.drawJustifiedString(x, y, hJust, String.format("Level %d", gameLevel))

		// draw the instmsg
		insta_msg_str?.let { str ->
			if (insta_msg_color == GColor.BLACK) {
				insta_msg_str = null
			} else {
				g.color = insta_msg_color
				g.drawString(insta_msg_str, TEXT_PADDING.toFloat(), (TEXT_PADDING + text_height) * 3)
				if (frameNumber % 3 == 0) insta_msg_color = insta_msg_color.darkened(DARKEN_AMOUNT)
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	// draw the player exploding after being killed
	private fun drawPlayerExploding(player: Player, g: AGraphics) {
		val x = player.x - screen_x + random(-5 .. 5)
		val y = player.y - screen_y + random(-5 .. 5)
		// drawPlayer(g, x, y, 2);
		drawPlayerBody(player, g, x, y, player.dir, GColor.RED)

		// outline the thing that hit the player with a blinking yellow circle
		if (frameNumber % 40 < 20) return
		g.color = GColor.YELLOW
		val rad: Int
		when (player.hit_type) {
			HIT_TYPE_ENEMY        -> {
				rad = getEnemyRadius(player.hit_index) //enemy_radius[enemy_type[hit_index]] + 5;
				g.drawOval((enemy_x[player.hit_index] - screen_x - rad).toFloat(), (enemy_y[player.hit_index] - screen_y - rad).toFloat(), (rad * 2).toFloat(), (rad * 2).toFloat())
			}
			HIT_TYPE_ROBOT_MISSLE -> {
				rad = ENEMY_PROJECTILE_RADIUS + 5
				g.drawOval(Math.round(enemy_missle[player.hit_index].x - screen_x - rad).toFloat(), Math.round(enemy_missle[player.hit_index].y - screen_y - rad).toFloat(), (rad * 2).toFloat(), (rad * 2).toFloat())
			}
			HIT_TYPE_TANK_MISSLE  -> {
				rad = TANK_MISSLE_RADIUS + 5
				g.drawOval((tank_missle[player.hit_index].x - screen_x - rad).toFloat(), (tank_missle[player.hit_index].y - screen_y - rad).toFloat(), (rad * 2).toFloat(), (rad * 2).toFloat())
			}
			else                  -> {
			}
		}
	}

	private fun updatePlayers() {
		for (i in 0 until num_players) {
			updatePlayer(players[i])
		}
	}

	// -----------------------------------------------------------------------------------------------
	// do collision scans and make people walk around stupidly
	private fun updatePeople() {
		val frame_num = frameNumber
		var dx: Int
		var dy: Int
		var i = 0
		while (i < num_people) {
			if (people_state[i] == 0) {
				removePeople(i)
				continue
			}
			if (people_state[i] < 0) {
				// this guy is turning into a zombie
				if (++people_state[i] == 0) {
					// add a zombie
					addEnemy(people_x[i], people_y[i], random(ENEMY_INDEX_ZOMBIE_N.. ENEMY_INDEX_ZOMBIE_W), true)
					removePeople(i)
					continue
				}
				i++
				continue
			}
			if (!isOnScreen(people_x[i], people_y[i])) {
				i++
				continue
			}
			var done = false
			// look for a colliison with the player
			for (ii in 0 until num_players) {
				val player = players[ii]
				if (Utils.isPointInsideCircle(player.x, player.y, people_x[i], people_y[i], PEOPLE_RADIUS + getPlayerRadius(player))) {
					if (isHulkActiveCharging(player)) {
						if (random(0.. 3) == 3) addPlayerMsg(player, "HULK SMASH!")
						addPoints(player, -people_points)
						if (people_picked_up > 0) people_picked_up--
						addParticle(people_x[i], people_y[i], PARTICLE_TYPE_BLOOD, PARTICLE_BLOOD_DURATION, -1, 0)
					} else {
						addMsg(people_x[i], people_y[i], people_points.toString()) // + " points");
						addPoints(player, people_points)
						people_picked_up++
						if (people_points < PEOPLE_MAX_POINTS) people_points += PEOPLE_INCREASE_POINTS * gameLevel
					}
					removePeople(i)
					done = true
					break
				}
			}
			if (done) {
				continue
			}
			if (frame_num % 5 == people_state[i]) {
				dx = move_dx[people_state[i] - 1] * PEOPLE_SPEED
				dy = move_dy[people_state[i] - 1] * PEOPLE_SPEED
				if (collisionScanCircle(people_x[i] + dx, people_y[i] + dy, PEOPLE_RADIUS)) {
					// reverse direction
					if (people_state[i] <= 2) people_state[i] += 2 else people_state[i] -= 2
				} else {
					people_x[i] += dx
					people_y[i] += dy

					// look for random direction changes
					if (random(0.. 10) == 0) people_state[i] = random(1 .. 4)
				}
			}
			i++
		}
	}

	private fun drawPerson(g: AGraphics, index: Int) {
		val type = people_type[index]
		val dir = people_state[index] - 1
		if (dir in 0..3) {
			val dim = 32
			val x = people_x[index] - screen_x
			val y = people_y[index] - screen_y
			drawPerson(g, x, y, dim, type, dir)
		}
	}

	private fun drawPerson(g: AGraphics, x: Int, y: Int, dimension: Int, type: Int, dir: Int) {
		if (dir in 0..3) {
			val animIndex = dir * 4 + frameNumber / 8 % 4
			g.color = GColor.WHITE
			g.drawImage(
				animPeople[type][animIndex],
				(x - dimension / 2).toFloat(),
				(y - dimension / 2).toFloat(),
				dimension.toFloat(),
				dimension.toFloat()
			)
		}
	}

	// -----------------------------------------------------------------------------------------------
	// draw all the people
	private fun drawPeople(g: AGraphics) {
		for (i in 0 until num_people) {
			if (!isOnScreen(people_x[i], people_y[i])) continue
			if (people_state[i] < 0) {
				// turning into a zombie, draw in white, shaking
				g.color = GColor.WHITE
				drawStickFigure(g, people_x[i] - screen_x, people_y[i] - screen_y + random(-2.. 2), PEOPLE_RADIUS)
			} else {
				// normal
				drawPerson(g, i)
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Draw a stupid looking stick figure
	private fun drawStickFigure(g: AGraphics, x0: Int, y0: Int, radius: Int) {
		val x1: Int
		val y1: Int

		// draw the legs
		g.drawLine(x0.toFloat(), y0.toFloat(), (x0 + radius / 2).toFloat(), (y0 + radius).toFloat())
		g.drawLine(x0.toFloat(), y0.toFloat(), (x0 - radius / 2).toFloat(), (y0 + radius).toFloat())
		g.drawLine((x0 - 1).toFloat(), y0.toFloat(), (x0 + radius / 2 - 1).toFloat(), (y0 + radius).toFloat())
		g.drawLine((x0 - 1).toFloat(), y0.toFloat(), (x0 - radius / 2 - 1).toFloat(), (y0 + radius).toFloat())
		// draw the body
		x1 = x0
		y1 = y0 - radius * 2 / 3
		g.drawLine(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat())
		g.drawLine((x0 - 1).toFloat(), y0.toFloat(), (x1 - 1).toFloat(), y1.toFloat())

		// draw the arms
		g.drawLine((x1 - radius * 2 / 3).toFloat(), y1.toFloat(), (x1 + radius * 2 / 3).toFloat(), y1.toFloat())
		g.drawLine((x1 - radius * 2 / 3).toFloat(), (y1 + 1).toFloat(), (x1 + radius * 2 / 3).toFloat(), (y1 + 1).toFloat())

		// draw the head
		g.drawFilledOval((x1 - radius / 4 - 1).toFloat(), (y1 - radius + 1).toFloat(), (radius / 2).toFloat(), (radius / 2 + 2).toFloat())
	}

	// -----------------------------------------------------------------------------------------------
	private fun removeMsg(index: Int) {
		num_msgs--
		msg_x[index] = msg_x[num_msgs]
		msg_y[index] = msg_y[num_msgs]
		msg_string[index] = msg_string[num_msgs]
		msg_color[index] = msg_color[num_msgs]
	}

	// -----------------------------------------------------------------------------------------------
	private fun addPlayerMsg(player: Player, msg: String) {
		val x = player.x + random(10 .. 20)
		val y = player.y + random(10 .. 20)
		addMsg(x, y, msg)
	}

	// -----------------------------------------------------------------------------------------------
	// Add a a message to the table if possible
	private fun addMsg(x: Int, y: Int, str: String) {
		if (num_msgs == MESSAGES_MAX) {
			log.debug("TOO MANY MESSAGES")
			return
		}
		msg_x[num_msgs] = x
		msg_y[num_msgs] = y
		msg_string[num_msgs] = str
		msg_color[num_msgs] = GColor.WHITE
		num_msgs++
	}

	// -----------------------------------------------------------------------------------------------
	// update all the messages
	private fun updateAndDrawMessages(g: AGraphics) {
		val frame_num = frameNumber
		var i = 0
		while (i < num_msgs) {
			if (!isOnScreen(msg_x[i], msg_y[i])) {
				removeMsg(i)
				continue
			}
			g.color = msg_color[i]
			g.drawString(msg_string[i], (msg_x[i] - screen_x).toFloat(), (msg_y[i] - screen_y).toFloat())
			msg_y[i] -= 1
			if (frame_num % MESSAGE_FADE == 0) {
				msg_color[i] = msg_color[i].darkened(DARKEN_AMOUNT)
				if (msg_color[i] == GColor.BLACK) {
					removeMsg(i)
					continue
				}
			}
			i++
		}
	}

	val particle_stars = arrayOf("*", "!", "?", "@")

	// -----------------------------------------------------------------------------------------------
	// add an explosion if possible
	private fun addParticle(x: Int, y: Int, type: Int, duration: Int, playerIndex: Int, angle: Int) {
		if (num_particles == MAX_PARTICLES) return
		val p = particles[num_particles++]
		p.x = x
		p.y = y
		p.star = random(0.. particle_stars.size - 1)
		p.angle = angle
		p.type = type
		p.duration = duration
		p.start_frame = frameNumber
		p.playerIndex = playerIndex
	}

	// -----------------------------------------------------------------------------------------------
	// remove explosion at index
	private fun removeParticle(index: Int) {
		num_particles--
		particles[index].copy(particles[num_particles])
	}

	// -----------------------------------------------------------------------------------------------
	// update and draw all explosions
	private fun updateAndDrawParticles(g: AGraphics) {
		var x: Int
		var y: Int
		var radius: Int
		var width: Int
		var v_spacing: Int
		var j: Int
		var top: Int
		var i = 0
		while (i < num_particles) {
			val p = particles[i]
			var duration = frameNumber - p.start_frame
			if (duration >= p.duration) {
				removeParticle(i)
				continue
			} else if (duration < 1) {
				duration = 1
			}
			if (!isOnScreen(p.x, p.y)) {
				removeParticle(i)
				continue
			}
			x = p.x - screen_x
			y = p.y - screen_y
			when (p.type) {
				PARTICLE_TYPE_BLOOD       -> {
					val pd2 = p.duration / 2
					// draw a missle command type expl
					if (duration <= pd2) {
						g.color = GColor.RED
						// draw expanding disk
						radius = PARTICLE_BLOOD_RADIUS * duration / pd2
						g.drawFilledOval((x - radius).toFloat(), (y - radius).toFloat(), (radius * 2).toFloat(), (radius * 2).toFloat())
					} else {
						// draw 2nd half of explosion sequence
						radius = PARTICLE_BLOOD_RADIUS * (duration - pd2) / pd2
						g.color = GColor.RED
						g.drawFilledOval((x - PARTICLE_BLOOD_RADIUS).toFloat(), (y - PARTICLE_BLOOD_RADIUS).toFloat(), (PARTICLE_BLOOD_RADIUS * 2).toFloat(), (PARTICLE_BLOOD_RADIUS * 2).toFloat())
						g.color = GColor.BLACK
						g.drawFilledOval((x - radius).toFloat(), (y - radius).toFloat(), (radius * 2).toFloat(), (radius * 2).toFloat())
					}
				}
				PARTICLE_TYPE_DYING_ROBOT -> {
					g.color = GColor.DARK_GRAY
					v_spacing = 2 * (PARTICLE_DYING_ROBOT_DURATION - duration + 1)
					width = ENEMY_ROBOT_RADIUS * 3 - (PARTICLE_DYING_ROBOT_DURATION - duration) * 4
					top = -(ENEMY_ROBOT_RADIUS + (PARTICLE_DYING_ROBOT_DURATION - duration))
					j = 0
					while (j < 8) {
						g.drawLine((x - width / 2).toFloat(), (y + top).toFloat(), (x + width / 2).toFloat(), (y + top).toFloat())
						top += v_spacing
						j++
					}
				}
				PARTICLE_TYPE_DYING_TANK  -> {
					g.color = GColor.RED
					v_spacing = 2 * (PARTICLE_DYING_ROBOT_DURATION - duration + 1)
					width = ENEMY_TANK_RADIUS * 3 - (PARTICLE_DYING_ROBOT_DURATION - duration) * 4
					top = -(ENEMY_TANK_RADIUS + (PARTICLE_DYING_ROBOT_DURATION - duration))
					j = 0
					while (j < 8) {
						g.drawLine((x - width / 2).toFloat(), (y + top).toFloat(), (x + width / 2).toFloat(), (y + top).toFloat())
						top += v_spacing
						j++
					}
				}
				PARTICLE_TYPE_PLAYER_STUN -> {
					// we will assume anything else is a stun
					g.color = GColor.WHITE
					val player = players[p.playerIndex]
					val rad = getPlayerRadius(player).toFloat()

					// draw swirling ?
					val px = (player.x - screen_x).toFloat()
					val py = Math.round(player.y - screen_y - rad * 2).toFloat()
					val ry = rad * 0.5f
					val deg = p.angle.toFloat()
					val tx = Math.round(px + rad * CMath.cosine(deg))
					val ty = Math.round(py + ry * CMath.sine(deg))
					p.angle += 25
					val star = particle_stars[p.star]
					g.drawString(star, tx.toFloat(), ty.toFloat())
				}
				else                      -> Utils.unhandledCase(p.type)
			}
			i++
		}
	}

	val WALL_FLAG_VISITED = 256

	/**
	 * Compute the distance in maze coordinates between to points
	 * using the path between the 2 cells
	 *
	 * @param x0
	 * @param y0
	 * @param x1
	 * @param y1
	 * @return
	 */
	private fun computeCellDistance(x0: Int, y0: Int, x1: Int, y1: Int): Int {
		val c0 = IntArray(2)
		val c1 = IntArray(2)
		computeCell(x0, y0, c0)
		computeCell(x1, y1, c1)
		val path: MutableList<IntArray> = LinkedList()
		findPath(maze_cells, c0[0], c0[1], c1[0], c1[1], path)
		var dist = 0
		if (path.size > 1) {
			var sc = path[0]
			var dx = x1 - sc[0] * MAZE_CELL_WIDTH
			var dy = y1 - sc[1] * MAZE_CELL_HEIGHT
			dist += Utils.fastLen(dx, dy)
			for (i in 1 until path.size) {
				val dc = path[i]
				dx = Math.abs(sc[0] - dc[0])
				dy = Math.abs(sc[1] - dc[1])
				dist += dx * MAZE_CELL_WIDTH + dy * MAZE_CELL_HEIGHT
				sc = dc
			}
			dx = x0 - sc[0] * MAZE_CELL_WIDTH
			dy = y0 - sc[1] * MAZE_CELL_HEIGHT
			dist += Utils.fastLen(dx, dy)
		} else {
			dist = Utils.fastLen(x0 - x1, y0 - y1)
		}
		return dist
	}

	// compute the path between 2 cells and return its distance
	private fun distCells(cx0: Int, cy0: Int, cx1: Int, cy1: Int): Int {
		val path: MutableList<IntArray> = LinkedList()
		findPath(maze_cells, cx0, cy0, cx1, cy1, path)
		return path.size
	}

	private fun findPath(cells: Array<IntArray>, cx0: Int, cy0: Int, cx1: Int, cy1: Int, path: MutableList<IntArray>) {
		for (i in cells.indices) {
			for (ii in 0 until cells[i].size) {
				cells[i][ii] = cells[i][ii] and WALL_FLAG_VISITED.inv() // unmark if marked
			}
		}
		findPath_r(cells, cx0, cy0, cx1, cy1, path)
	}

	// TODO: Optimize the search by sorting the direction to choose
	private fun findPath_r(cells: Array<IntArray>, cx0: Int, cy0: Int, cx1: Int, cy1: Int, path: MutableList<IntArray>): Boolean {
		var found = false
		if (cx0 == cx1 && cy0 == cy1) {
			found = true
		} else {
			if (cells[cx0][cy0] and WALL_FLAG_VISITED == 0) {
				// mark this cell as visited
				cells[cx0][cy0] = cells[cx0][cy0] or WALL_FLAG_VISITED
				if (cells[cx0][cy0] and WALL_NORTH == 0) {
					found = findPath_r(cells, cx0, cy0 - 1, cx1, cy1, path)
				}
				if (!found && cells[cx0][cy0] and WALL_SOUTH == 0) {
					found = findPath_r(cells, cx0, cy0 + 1, cx1, cy1, path)
				}
				if (!found && cells[cx0][cy0] and WALL_WEST == 0) {
					found = findPath_r(cells, cx0 - 1, cy0, cx1, cy1, path)
				}
				if (!found && cells[cx0][cy0] and WALL_EAST == 0) {
					found = findPath_r(cells, cx0 + 1, cy0, cx1, cy1, path)
				}
			}
		}
		if (found) {
			path.add(intArrayOf(cx0, cy0))
		}
		return found
	}

	// -----------------------------------------------------------------------------------------------
	// Do enemy hit event on enemy e
	// return true if the source object killed the enemy
	//
	// DO NOT CALL playerHit from this func, inf loop possible!
	private fun enemyHit(player: Player, e: Int, dx: Int, dy: Int): Boolean {
		if (enemy_type[e] == ENEMY_INDEX_GEN) {
			// spawn a bunch of guys in my place
			val count = random(ENEMY_GEN_SPAWN_MIN.. ENEMY_GEN_SPAWN_MAX)
			for (i in 0 until count) {
				addEnemy(enemy_x[e] + random(-10 .. 10), enemy_y[e] + random(-10.. 10), random(ENEMY_INDEX_ROBOT_N .. ENEMY_INDEX_ROBOT_W), true)
			}
			val distGen = computeCellDistance(enemy_x[e], enemy_y[e], end_x, end_y)
			val distStart = computeCellDistance(player.start_x, player.start_y, end_x, end_y)


			// if this is closer to the end than the player start, then make this the start
			//int distGen = Utils.fastLen(enemy_x[e] - end_x, enemy_y[e] - end_x);
			//int distStart = Utils.fastLen(player.start_x - end_x, player.start_y - end_y);
			log.debug("distGen = $distGen, distStart = $distStart")
			if (distGen < distStart) {
				player.start_x = enemy_x[e]
				player.start_y = enemy_y[e]
			}
			addRandomPowerup(enemy_x[e], enemy_y[e], enemy_radius[enemy_type[e]])
			addPoints(player, ENEMY_GEN_POINTS)
			removeEnemy(e)
			return true
		} else if (enemy_type[e] <= ENEMY_INDEX_ROBOT_W) {
			addPoints(player, ENEMY_ROBOT_POINTS)
			addParticle(enemy_x[e], enemy_y[e], PARTICLE_TYPE_DYING_ROBOT, 5, -1, 0)
			removeEnemy(e)
			return true
		} else if (enemy_type[e] <= ENEMY_INDEX_THUG_W) {
			if (!collisionScanCircle(enemy_x[e] + dx, enemy_y[e] + dy, ENEMY_THUG_RADIUS)) {
				enemy_x[e] += dx
				enemy_y[e] += dy
			}
			return false
		} else if (enemy_type[e] == ENEMY_INDEX_BRAIN) {
			// chance for powerup
			addRandomPowerup(enemy_x[e], enemy_y[e], enemy_radius[enemy_type[e]])
			// spawn some blood
			addPoints(player, ENEMY_BRAIN_POINTS)
			addParticle(enemy_x[e] + random(-ENEMY_BRAIN_RADIUS / 2.. ENEMY_BRAIN_RADIUS / 2),
				enemy_y[e] + random(-ENEMY_BRAIN_RADIUS / 2 .. ENEMY_BRAIN_RADIUS / 2),
				PARTICLE_TYPE_BLOOD, PARTICLE_BLOOD_DURATION, -1, 0)
			addParticle(enemy_x[e] + random(-ENEMY_BRAIN_RADIUS / 2.. ENEMY_BRAIN_RADIUS / 2),
				enemy_y[e] + random(-ENEMY_BRAIN_RADIUS / 2 .. ENEMY_BRAIN_RADIUS / 2),
				PARTICLE_TYPE_BLOOD, PARTICLE_BLOOD_DURATION, -1, 0)
			addParticle(enemy_x[e] + random(-ENEMY_BRAIN_RADIUS / 2.. ENEMY_BRAIN_RADIUS / 2),
				enemy_y[e] + random(-ENEMY_BRAIN_RADIUS / 2 .. ENEMY_BRAIN_RADIUS / 2),
				PARTICLE_TYPE_BLOOD, PARTICLE_BLOOD_DURATION, -1, 0)
			removeEnemy(e)
			return true
		} else if (enemy_type[e] <= ENEMY_INDEX_TANK_NW) {
			addPoints(player, ENEMY_TANK_POINTS)
			addParticle(enemy_x[e], enemy_y[e], PARTICLE_TYPE_DYING_TANK, 8, -1, 0)
			removeEnemy(e)
			return true
		}
		return false
	}

	// -----------------------------------------------------------------------------------------------
	// draw all enemies
	private fun drawEnemies(g: AGraphics) {
		// int radius;
		// int mod;
		var debugEnemyDrawn = false
		for (i in 0 until num_enemies) {
			if (!isOnScreen(enemy_x[i], enemy_y[i])) continue

			//if (isVisibilityEnabled()) {
			//    if (!canSee(player.x, player.y, enemy_x[i], enemy_y[i]))
			//        continue;
			//}
			val x0 = enemy_x[i] - screen_x
			val y0 = enemy_y[i] - screen_y
			when (enemy_type[i]) {
				ENEMY_INDEX_GEN                                                                        -> drawGenerator(g, x0, y0)
				ENEMY_INDEX_ROBOT_N, ENEMY_INDEX_ROBOT_E, ENEMY_INDEX_ROBOT_S, ENEMY_INDEX_ROBOT_W     -> drawRobot(g, x0, y0, enemy_type[i] - ENEMY_INDEX_ROBOT_N)
				ENEMY_INDEX_THUG_N, ENEMY_INDEX_THUG_E, ENEMY_INDEX_THUG_S, ENEMY_INDEX_THUG_W         -> drawThug(g, x0, y0, enemy_type[i] - ENEMY_INDEX_THUG_N)
				ENEMY_INDEX_BRAIN                                                                      -> drawBrain(g, x0, y0, ENEMY_BRAIN_RADIUS)
				ENEMY_INDEX_ZOMBIE_N, ENEMY_INDEX_ZOMBIE_E, ENEMY_INDEX_ZOMBIE_S, ENEMY_INDEX_ZOMBIE_W -> {
					g.color = GColor.YELLOW
					drawStickFigure(g, x0, y0, ENEMY_ZOMBIE_RADIUS)
				}
				ENEMY_INDEX_TANK_NE, ENEMY_INDEX_TANK_SE, ENEMY_INDEX_TANK_SW, ENEMY_INDEX_TANK_NW     -> drawTank(g, x0, y0, enemy_type[i] - ENEMY_INDEX_TANK_NE)
				ENEMY_INDEX_JAWS                                                                       -> {
					var index = frameNumber - enemy_next_update[i]
					if (index >= animJaws.size) {
						enemy_next_update[i] = frameNumber + 1
						index = 0
					} else if (index <= 0) {
						index = 0
						var ii = 0
						while (ii < num_players) {
							val player = players[ii]
							val rad2 = (PLAYER_RADIUS * PLAYER_RADIUS + 100 * 100).toFloat()
							if (Utils.distSqPointPoint(player.x.toFloat(), player.y.toFloat(), enemy_x[i].toFloat(), enemy_y[i].toFloat()) > rad2) {
								enemy_next_update[i] = frameNumber + animJaws.size
							}
							ii++
						}
					}
					drawJaws(g, x0, y0, index)
				}
				ENEMY_INDEX_LAVA                                                                       -> drawLavaPit(g, x0, y0, getLavaPitFrame(i))
			}
			if (!debugEnemyDrawn && isDebugEnabled(Debug.DRAW_ENEMY_INFO)) {
				val r = getEnemyRadius(i)
				val x = x0 - r
				val y = y0 - r
				val w = r * 2
				val h = r * 2
				val player = player
				if (Utils.isPointInsideRect(cursor_x.toFloat(), cursor_y.toFloat(), x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())) {
					g.color = GColor.YELLOW
					g.drawOval(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())
					val msg = """
	                	index [$i]
	                	radius [${getEnemyRadius(i)}]
	                	type   [${getEnemyTypeString(enemy_type[i])}]
	                	""".trimIndent()
					g.drawJustifiedString(x.toFloat(), y.toFloat(), Justify.RIGHT, Justify.TOP, msg)
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
	private fun drawRobot(g: AGraphics, x0: Int, y0: Int, dir: Int) {
		g.color = GColor.DARK_GRAY
		val walk = frameNumber % 12 / 4 - 1
		if (dir == 0 || dir == 2) {
			// draw head
			g.drawFilledRect((x0 - 8).toFloat(), (y0 - 14).toFloat(), 16f, 12f)
			// draw the arms
			g.drawFilledRect((x0 - 12).toFloat(), (y0 - 6).toFloat(), 4f, 12f)
			g.drawFilledRect((x0 + 8).toFloat(), (y0 - 6).toFloat(), 4f, 12f)
			// draw the body
			g.drawFilledRect((x0 - 6).toFloat(), (y0 - 2).toFloat(), 12f, 4f)
			g.drawFilledRect((x0 - 4).toFloat(), (y0 + 2).toFloat(), 8f, 6f)
			// draw the legs
			g.drawFilledRect((x0 - 6).toFloat(), (y0 + 8).toFloat(), 4f, (8 + walk).toFloat())
			g.drawFilledRect((x0 + 2).toFloat(), (y0 + 8).toFloat(), 4f, (8 - walk).toFloat())
			// draw the feet
			g.drawFilledRect((x0 - 8).toFloat(), (y0 + 12 + walk).toFloat(), 2f, 4f)
			g.drawFilledRect((x0 + 6).toFloat(), (y0 + 12 - walk).toFloat(), 2f, 4f)
			// draw the eyes if walking S
			if (dir == 2) {
				g.color = throbbing_white
				g.drawFilledRect((x0 - 4).toFloat(), (y0 - 12).toFloat(), 8f, 4f)
			}
		} else {
			// draw the robot sideways

			// draw the head
			g.drawFilledRect((x0 - 6).toFloat(), (y0 - 14).toFloat(), 12f, 8f)
			// draw the body, eyes ect.
			if (dir == 1) {
				// body
				g.drawFilledRect((x0 - 6).toFloat(), (y0 - 6).toFloat(), 10f, 10f)
				g.drawFilledRect((x0 - 8).toFloat(), (y0 + 4).toFloat(), 14f, 4f)
				// draw the legs
				g.drawFilledRect((x0 - 8).toFloat(), (y0 + 8).toFloat(), 4f, (8 + walk).toFloat())
				g.drawFilledRect((x0 + 2).toFloat(), (y0 + 8).toFloat(), 4f, (8 - walk).toFloat())
				// draw feet
				g.drawFilledRect((x0 - 4).toFloat(), (y0 + 12 + walk).toFloat(), 4f, 4f)
				g.drawFilledRect((x0 + 6).toFloat(), (y0 + 12 - walk).toFloat(), 4f, 4f)
				// draw the eyes
				g.color = throbbing_white
				g.drawFilledRect((x0 + 2).toFloat(), (y0 - 12).toFloat(), 4f, 4f)
			} else {
				// body
				g.drawFilledRect((x0 - 4).toFloat(), (y0 - 6).toFloat(), 10f, 10f)
				g.drawFilledRect((x0 - 6).toFloat(), (y0 + 4).toFloat(), 14f, 4f)
				// draw the legs
				g.drawFilledRect((x0 - 6).toFloat(), (y0 + 8).toFloat(), 4f, (8 + walk).toFloat())
				g.drawFilledRect((x0 + 4).toFloat(), (y0 + 8).toFloat(), 4f, (8 - walk).toFloat())
				// draw feet
				g.drawFilledRect((x0 - 10).toFloat(), (y0 + 12 + walk).toFloat(), 4f, 4f)
				g.drawFilledRect(x0.toFloat(), (y0 + 12 - walk).toFloat(), 4f, 4f)
				// draw the eyes
				g.color = throbbing_white
				g.drawFilledRect((x0 - 6).toFloat(), (y0 - 12).toFloat(), 4f, 4f)
			}
			// draw the arm
			g.color = GColor.BLACK
			g.drawFilledRect((x0 - 2).toFloat(), (y0 - 6).toFloat(), 4f, 12f)
		}
	}

	// -----------------------------------------------------------------------------------------------
	// draw a generator
	private fun drawGenerator(g: AGraphics, x: Int, y: Int) {
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

	val brain_pts_x = intArrayOf(
		15, 13, 13, 8, 7, 6, 4, 2, 2, 3, 3, 4, 4, 8, 11, 12, 14, 17, 20, 21, 21, 23, 25, 27, 29, 29, 28, 29, 25, 24, 22, 20,
		20, 18
	)
	val brain_pts_y = intArrayOf(
		16, 19, 21, 21, 20, 21, 21, 19, 15, 14, 11, 9, 7, 4, 4, 2, 1, 2, 2, 3, 3, 4, 8, 7, 8, 13, 14, 16, 21, 20, 21, 20, 18,
		19
	)
	val brain_nerves_x = intArrayOf(8, 6, 10, 11, 13, 17, 15, 17, 16, 16, 18, 19, 21, 21, 23, 22, 23, 25, 26, 28)
	val brain_nerves_y = intArrayOf(7, 10, 10, 12, 12, 2, 4, 7, 9, 10, 10, 11, 9, 7, 8, 20, 18, 18, 17, 19)
	val brain_legs_x = intArrayOf(15, 13, 13, 10, 14, 16, 18, 22, 20, 20, 18)
	val brain_legs_y = intArrayOf(16, 19, 21, 29, 29, 24, 29, 29, 20, 18, 19)
	val brain_nerves_len = intArrayOf(5, 4, 6, 5)

	// -----------------------------------------------------------------------------------------------
	// Draw the Evil Brain
	private fun drawBrain(g: AGraphics, x0: Int, y0: Int, radius: Int) {
		// big head, little arms and legs
		var x0 = x0
		var y0 = y0
		g.color = GColor.BLUE
		g.translate((x0 - radius).toFloat(), (y0 - radius).toFloat())
		g.drawFilledPolygon(brain_pts_x, brain_pts_y, brain_pts_x.size)
		g.color = GColor.RED
		g.drawFilledPolygon(brain_legs_x, brain_legs_y, brain_legs_x.size)
		g.translate(-(x0 - radius).toFloat(), -(y0 - radius).toFloat())

		// draw some glowing lines to look like brain nerves
		g.color = throbbing_white
		x0 -= radius
		y0 -= radius
		var i = 1
		for (l in 0..3) {
			for (c in 0 until brain_nerves_len[l] - 1) {
				g.drawLine((x0 + brain_nerves_x[i - 1]).toFloat(), (y0 + brain_nerves_y[i - 1]).toFloat(), (x0 + brain_nerves_x[i]).toFloat(), (y0 + brain_nerves_y[i]).toFloat())
				i++
			}
			i++
		}
		x0 += radius
		y0 += radius
		// draw the eyes
		g.color = GColor.YELLOW
		g.drawFilledRect((x0 - 5).toFloat(), (y0 + 1).toFloat(), 2f, 2f)
		g.drawFilledRect((x0 + 3).toFloat(), y0.toFloat(), 3f, 2f)
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawTankGen(g: AGraphics, x0: Int, y0: Int) {
		val degrees = (frameNumber % 360).toFloat()
		val dx = Math.round(5.0f * CMath.cosine(degrees))
		val dy = Math.round(5.0f * CMath.sine(degrees))
		g.color = GColor.GREEN
		g.drawRect(
			(x0 - ENEMY_TANK_GEN_RADIUS + dx).toFloat(),
			(y0 - ENEMY_TANK_GEN_RADIUS + dy).toFloat(),
			(ENEMY_TANK_GEN_RADIUS * 2).toFloat(),
			(ENEMY_TANK_GEN_RADIUS * 2).toFloat()
		)
		g.color = GColor.BLUE
		g.drawRect(
			(x0 - ENEMY_TANK_GEN_RADIUS - dx).toFloat(),
			(y0 - ENEMY_TANK_GEN_RADIUS - dy).toFloat(),
			(ENEMY_TANK_GEN_RADIUS * 2).toFloat(),
			(ENEMY_TANK_GEN_RADIUS * 2).toFloat()
		)
	}

	val tank_pts_x = intArrayOf(12, 6, 6, 18, 32, 41, 41, 36, 36, 12)
	val tank_pts_y = intArrayOf(10, 10, 26, 38, 38, 26, 10, 10, 4, 4)

	// -----------------------------------------------------------------------------------------------
	private fun drawTank(g: AGraphics, x0: Int, y0: Int, dir: Int) {
		g.color = GColor.DARK_GRAY
		g.drawFilledRect((x0 - 12).toFloat(), (y0 - 20).toFloat(), 24f, 16f)
		g.color = GColor.RED
		g.translate((x0 - ENEMY_TANK_RADIUS).toFloat(), (y0 - ENEMY_TANK_RADIUS).toFloat())
		g.drawFilledPolygon(tank_pts_x, tank_pts_y, tank_pts_x.size)
		g.translate(-(x0 - ENEMY_TANK_RADIUS).toFloat(), -(y0 - ENEMY_TANK_RADIUS).toFloat())
		g.color = GColor.DARK_GRAY
		g.drawFilledRect((x0 - 12).toFloat(), (y0 - 2).toFloat(), 24f, 4f)
		// draw the wheels
		g.color = GColor.CYAN
		g.drawFilledOval((x0 - 22).toFloat(), (y0 + 6).toFloat(), 12f, 12f)
		g.drawFilledOval((x0 + 10).toFloat(), (y0 + 6).toFloat(), 12f, 12f)
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawJaws(g: AGraphics, x0: Int, y0: Int, index: Int) {
		var index = index
		index %= animJaws.size
		val jw = 32
		val jh = 32
		g.color = GColor.WHITE
		g.drawImage(animJaws[index], (x0 - jw / 2).toFloat(), (y0 - jh / 2).toFloat(), jw.toFloat(), jh.toFloat())
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawLavaPit(g: AGraphics, x0: Int, y0: Int, index: Int) {
		if (index < animLava.size) {
			g.color = GColor.WHITE
			g.drawImage(animLava[index], (
				x0 - ENEMY_LAVA_DIM / 2).toFloat(), (
				y0 - ENEMY_LAVA_DIM / 2).toFloat(),
				ENEMY_LAVA_DIM.toFloat(),
				ENEMY_LAVA_DIM.toFloat())
		}
	}

	// -----------------------------------------------------------------------------------------------
	// draw a dumb looking thug
	private fun drawThug(g: AGraphics, x0: Int, y0: Int, dir: Int) {
		// draw the body
		g.color = GColor.GREEN
		g.drawFilledRect((x0 - 12).toFloat(), (y0 - 12).toFloat(), 24f, 21f)
		g.color = GColor.RED
		if (dir == 0 || dir == 2) {
			// draw 2 arms at the sides
			g.drawFilledRect((x0 - 15).toFloat(), (y0 - 10).toFloat(), 3f, 15f)
			g.drawFilledRect((x0 + 12).toFloat(), (y0 - 10).toFloat(), 3f, 15f)

			// draw 2 legs
			g.drawFilledRect((x0 - 8).toFloat(), (y0 + 9).toFloat(), 5f, 10f)
			g.drawFilledRect((x0 + 3).toFloat(), (y0 + 9).toFloat(), 5f, 10f)
		} else {
			// draw 1 arm in the middle
			g.drawFilledRect((x0 - 3).toFloat(), (y0 - 10).toFloat(), 6f, 15f)

			// draw 1 leg
			g.drawFilledRect((x0 - 3).toFloat(), (y0 + 9).toFloat(), 6f, 10f)
		}
		// draw the head
		g.color = GColor.BLUE
		g.drawFilledRect((x0 - 5).toFloat(), (y0 - 19).toFloat(), 10f, 7f)
	}

	// -----------------------------------------------------------------------------------------------
	// draw a bar with endpoints p0, p1 with thickness t
	private fun drawBar(g: AGraphics, x0: Int, y0: Int, x1: Int, y1: Int, t: Int, index: Int) {
		val w = Math.abs(x0 - x1) + t * 2
		val h = Math.abs(y0 - y1) + t * 2
		val x = Math.min(x0, x1) - t
		val y = Math.min(y0, y1) - t
		g.drawFilledRect(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawStaticField(g: AGraphics, x: Int, y: Int, radius: Int) {
		var x0 = 1f
		var y0 = 0f
		var r0 = Utils.randFloatX(3f) + radius
		val sr = r0
		for (i in 0 until STATIC_FIELD_SECTIONS - 1) {
			val x1 = x0 * STATIC_FIELD_COS_T - y0 * STATIC_FIELD_SIN_T
			val y1 = y0 * STATIC_FIELD_SIN_T + x0 * STATIC_FIELD_COS_T
			val r1 = Utils.randFloatX(3f) + radius
			g.drawLine(Math.round(x + x0 * r0).toFloat(), Math.round(y + y0 * r0).toFloat(), Math.round(x + x1 * r1).toFloat(), Math.round(y + y1 * r1).toFloat())
			x0 = x1
			y0 = y1
			r0 = r1
		}
		g.drawLine(Math.round(x + x0 * r0).toFloat(), Math.round(y + y0 * r0).toFloat(), Math.round(x + 1.0f * sr).toFloat(), Math.round(y + 0 * sr).toFloat())
	}

	// -----------------------------------------------------------------------------------------------
	// do any one time table inits here
	private fun initTables() {
		players[0] = Player()
		num_players = 1
		for (i in 0 until MAX_PARTICLES) {
			particles[i] = Particle()
		}
		for (i in 0 until MAX_ENEMY_MISSLES) enemy_missle[i] = MissileFloat()
		for (i in 0 until MAX_TANK_MISSLES) tank_missle[i] = MissileInt()
		for (i in 0 until MAX_SNAKE_MISSLES) snake_missle[i] = MissileSnake()
		for (i in 0 until MAX_POWERUPS) {
			powerups[i] = Powerup()
		}
		for (i in 0 until MAZE_NUMCELLS_X) {
			for (j in 0 until MAZE_NUMCELLS_Y) {
				val upleft = i + j * (MAZE_NUMCELLS_X + 1)
				val upright = i + 1 + j * (MAZE_NUMCELLS_X + 1)
				val downleft = i + (j + 1) * (MAZE_NUMCELLS_X + 1)
				val downright = i + 1 + (j + 1) * (MAZE_NUMCELLS_X + 1)
				initWall(upleft, upright)
				initWall(upright, downright)
				initWall(downleft, downright)
				initWall(upleft, downleft)
			}
		}
		enemy_radius[ENEMY_INDEX_GEN] = ENEMY_GEN_RADIUS
		enemy_radius[ENEMY_INDEX_ROBOT_W] = ENEMY_ROBOT_RADIUS
		enemy_radius[ENEMY_INDEX_ROBOT_S] = enemy_radius[ENEMY_INDEX_ROBOT_W]
		enemy_radius[ENEMY_INDEX_ROBOT_E] = enemy_radius[ENEMY_INDEX_ROBOT_S]
		enemy_radius[ENEMY_INDEX_ROBOT_N] = enemy_radius[ENEMY_INDEX_ROBOT_E]
		enemy_radius[ENEMY_INDEX_THUG_W] = ENEMY_THUG_RADIUS
		enemy_radius[ENEMY_INDEX_THUG_S] = enemy_radius[ENEMY_INDEX_THUG_W]
		enemy_radius[ENEMY_INDEX_THUG_E] = enemy_radius[ENEMY_INDEX_THUG_S]
		enemy_radius[ENEMY_INDEX_THUG_N] = enemy_radius[ENEMY_INDEX_THUG_E]
		enemy_radius[ENEMY_INDEX_BRAIN] = ENEMY_BRAIN_RADIUS
		enemy_radius[ENEMY_INDEX_ZOMBIE_W] = ENEMY_ZOMBIE_RADIUS
		enemy_radius[ENEMY_INDEX_ZOMBIE_S] = enemy_radius[ENEMY_INDEX_ZOMBIE_W]
		enemy_radius[ENEMY_INDEX_ZOMBIE_E] = enemy_radius[ENEMY_INDEX_ZOMBIE_S]
		enemy_radius[ENEMY_INDEX_ZOMBIE_N] = enemy_radius[ENEMY_INDEX_ZOMBIE_E]
		enemy_radius[ENEMY_INDEX_TANK_NW] = ENEMY_TANK_RADIUS
		enemy_radius[ENEMY_INDEX_TANK_SE] = enemy_radius[ENEMY_INDEX_TANK_NW]
		enemy_radius[ENEMY_INDEX_TANK_SW] = enemy_radius[ENEMY_INDEX_TANK_SE]
		enemy_radius[ENEMY_INDEX_TANK_NE] = enemy_radius[ENEMY_INDEX_TANK_SW]
		enemy_radius[ENEMY_INDEX_JAWS] = ENEMY_JAWS_DIM / 2 - 2
		enemy_radius[ENEMY_INDEX_LAVA] = ENEMY_LAVA_DIM / 2 - 5
	}

	// -----------------------------------------------------------------------------------------------
	private fun getEnemyRadius(enemyIndex: Int): Int {
		val type = enemy_type[enemyIndex]
		var radius = enemy_radius[type]
		if (type == ENEMY_INDEX_LAVA) {
			//if (this.enemy_spawned_frame)
			val frame = getLavaPitFrame(enemyIndex)
			if (frame >= animLava.size) {
				return -1
			}
			// make so the radius follows the expand/contract animation
			val R = (ENEMY_LAVA_DIM / 2).toFloat()
			val F = animLava.size.toFloat()
			val M = R / F
			val f = frame.toFloat()
			radius = if (f < F / 2) {
				Math.round(M * f)
			} else {
				Math.round(-M * f + R)
			}
		}
		return radius
	}

	// -----------------------------------------------------------------------------------------------
	private fun getLavaPitFrame(enemyIndex: Int): Int {
		return (frameNumber + enemy_spawned_frame[enemyIndex]) % (animLava.size + ENEMY_LAVA_CLOSED_FRAMES)
	}

	// -----------------------------------------------------------------------------------------------
	// update all enemies position and collision
	private fun updateEnemies() {
		var p: Int
		var vx: Int
		var vy: Int
		var mag: Int
		var radius: Int
		var closest: Int
		var mindist: Int

		// cache the frame number
		val frame_num = frameNumber
		total_enemies = 0
		var i = 0
		while (i < num_enemies) {

			//if (enemy_type[i] < ENEMY_INDEX_THUG_N || enemy_type[i] > ENEMY_INDEX_THUG_W
			//  && enemy_type[i] != ENEMY_INDEX)
			total_enemies++
			if (!isOnScreen(enemy_x[i], enemy_y[i])) {
				i++
				continue
			}

			// get the radius of this enemy type from the lookup table
			radius = getEnemyRadius(i) //enemy_radius[enemy_type[i]];
			for (ii in 0 until num_players) {
				val player = players[ii]
				if (player.state == PLAYER_STATE_ALIVE) {
					// see if we have collided with the player
					if (radius > 0 && Utils.isPointInsideCircle(player.x, player.y, enemy_x[i], enemy_y[i], getPlayerRadius(player) + radius)) {
						playerHit(player, HIT_TYPE_ENEMY, i)
					}
				}
			}

			// see if we have squashed some people
			if (enemy_type[i] >= ENEMY_INDEX_THUG_N && enemy_type[i] <= ENEMY_INDEX_THUG_W) {
				p = 0
				while (p < num_people) {
					if (people_state[p] > 0 && isOnScreen(people_x[p], people_y[p])
						&& Utils.isPointInsideCircle(people_x[p], people_y[p], enemy_x[i], enemy_y[i], radius + PEOPLE_RADIUS)) {
						addParticle(people_x[p], people_y[p], PARTICLE_TYPE_BLOOD, PARTICLE_BLOOD_DURATION, -1, 0)
						addMsg(enemy_x[i] + random(-10.. 10), enemy_y[i] + random(-10 .. 10), "NOOOO!")
						removePeople(p)
						continue
					}
					p++
				}
			}
			if (enemy_next_update[i] > frame_num) {
				i++
				continue
			}
			when (enemy_type[i]) {
				ENEMY_INDEX_GEN                                                                        -> {
					// create a new guy
					val ex = enemy_x[i] + random(-10 .. 10)
					val ey = enemy_y[i] + random(-10 .. 10)
					val ed = random(ENEMY_INDEX_ROBOT_N.. ENEMY_INDEX_ROBOT_W)
					addEnemy(ex, ey, ed, true)
					enemy_next_update[i] = frame_num + 50 + random(-20.. 20)
				}
				ENEMY_INDEX_ROBOT_N, ENEMY_INDEX_ROBOT_E, ENEMY_INDEX_ROBOT_S, ENEMY_INDEX_ROBOT_W     -> {

					// mag = 0.3f;
					var ii = 0
					while (ii < num_players) {
						val player = players[ii]
						if (player.state == PLAYER_STATE_ALIVE) {
							if (random(0.. 4) == 0) {
								// see if a vector from me too player intersects a wall
								if (collisionScanLine(player.x, player.y, enemy_x[i], enemy_y[i])) {
									enemy_type[i] = ENEMY_INDEX_ROBOT_N + enemyDirectionHeuristic(enemy_x[i], enemy_y[i], 1.0f)
								} else {
									enemy_type[i] = ENEMY_INDEX_ROBOT_N + enemyDirectionHeuristic(enemy_x[i], enemy_y[i], ENEMY_ROBOT_HEURISTIC_FACTOR)
								}
							}
						}
						ii++
					}
					vx = enemy_robot_speed * move_dx[enemy_type[i] - ENEMY_INDEX_ROBOT_N]
					vy = enemy_robot_speed * move_dy[enemy_type[i] - ENEMY_INDEX_ROBOT_N]
					if (collisionScanCircle(enemy_x[i] + vx, enemy_y[i] + vy, ENEMY_ROBOT_RADIUS)) {
						vx = -vx
						vy = -vy
						if (enemy_type[i] < ENEMY_INDEX_ROBOT_N + 2) enemy_type[i] += 2 else enemy_type[i] -= 2
					}
					enemy_x[i] += vx
					enemy_y[i] += vy
					enemy_next_update[i] = frame_num + (ENEMY_ROBOT_MAX_SPEED + difficulty + 1 - enemy_robot_speed) + random(-3.. 3)

					// look for lobbing a missle at the player
					if (random(0 .. 200) < ENEMY_PROJECTILE_FREQ + difficulty + gameLevel) {
						var ii = 0
						while (ii < num_players) {
							val player = players[ii]
							if (player.state == PLAYER_STATE_ALIVE) {
								val dx = player.x - enemy_x[i]
								// int dy = player.y - enemy_y[i];
								if (Math.abs(dx) > 10 && Math.abs(dx) < ENEMY_ROBOT_ATTACK_DIST + gameLevel * 5) {
									enemyFireMissle(player, i)
								}
							}
							ii++
						}
					}
				}
				ENEMY_INDEX_THUG_N, ENEMY_INDEX_THUG_E, ENEMY_INDEX_THUG_S, ENEMY_INDEX_THUG_W         -> {
					vx = (ENEMY_THUG_SPEED + gameLevel / 2) * move_dx[enemy_type[i] - ENEMY_INDEX_THUG_N]
					vy = (ENEMY_THUG_SPEED + gameLevel / 2) * move_dy[enemy_type[i] - ENEMY_INDEX_THUG_N]
					// see if we will walk into wall
					if (collisionScanCircle(enemy_x[i] + vx, enemy_y[i] + vy, radius)) {
						// turn around
						if (enemy_type[i] < ENEMY_INDEX_THUG_S) enemy_type[i] += 2 else enemy_type[i] -= 2
					} else {
						// walk forward
						enemy_x[i] += vx
						enemy_y[i] += vy
					}
					// roll dice
					if (random(0.. 5) == 0) {
						// pick a new directiorn
						enemy_type[i] = ENEMY_INDEX_THUG_N + enemyDirectionHeuristic(enemy_x[i], enemy_y[i], ENEMY_THUG_HEURISTICE_FACTOR)
					}
					enemy_next_update[i] = frame_num + ENEMY_THUG_UPDATE_FREQ + random(-2.. 2)
				}
				ENEMY_INDEX_BRAIN                                                                      -> {
					if (frameNumber % ENEMY_BRAIN_FIRE_FREQ == 0 && random(0..ENEMY_BRAIN_FIRE_CHANCE) <= (1 + difficulty) * gameLevel)
						addSnakeMissle(enemy_x[i], enemy_y[i])

					// search for a person to walk toward
					closest = -1
					mindist = Int.MAX_VALUE
					p = 0
					while (p < num_people) {
						if (people_state[p] <= 0 || !isOnScreen(people_x[p], people_y[p])) {
							p++
							continue
						}
						if (Utils.isPointInsideCircle(enemy_x[i], enemy_y[i], people_x[p], people_y[p], ENEMY_BRAIN_RADIUS + PEOPLE_RADIUS)) {
							// turn this people into a zombie
							people_state[p] = -ENEMY_BRAIN_ZOMBIFY_FRAMES
							enemy_next_update[i] = frame_num + ENEMY_BRAIN_ZOMBIFY_FRAMES
							mindist = 0
							break
						}
						vx = enemy_x[i] - people_x[p]
						vy = enemy_y[i] - people_y[p]
						mag = vx * vx + vy * vy
						if (!collisionScanLine(enemy_x[i], enemy_y[i], enemy_x[i] + vx, enemy_y[i] + vy) && mag < mindist) {
							mag = mindist
							closest = p
						}
						p++
					}
					if (mindist == 0) {
						i++
						continue
					}
					if (closest < 0) {
						val closestPlayer = getClosestPlayer(enemy_x[i], enemy_y[i])
						if (closestPlayer != null) {
							// just move toward player
							vx = closestPlayer.x - enemy_x[i]
							vy = closestPlayer.y - enemy_y[i]
						} else {
							vy = 0
							vx = vy
						}
					} else {
						vx = people_x[closest] - enemy_x[i]
						vy = people_y[closest] - enemy_y[i]
					}
					mag = Utils.fastLen(vx, vy) + 1
					vx = vx * (ENEMY_BRAIN_SPEED + gameLevel / 3) / mag
					vy = vy * (ENEMY_BRAIN_SPEED + gameLevel / 3) / mag
					if (collisionScanCircle(enemy_x[i] + vx, enemy_y[i] + vy, ENEMY_BRAIN_RADIUS)) {
						vx = -vx
						vy = -vy
					}
					enemy_x[i] += vx
					enemy_y[i] += vy
					enemy_next_update[i] = frame_num + ENEMY_BRAIN_UPDATE_SPACING - gameLevel / 2 + random(-3.. 3)
				}
				ENEMY_INDEX_ZOMBIE_N, ENEMY_INDEX_ZOMBIE_E, ENEMY_INDEX_ZOMBIE_S, ENEMY_INDEX_ZOMBIE_W -> {
					enemy_type[i] = ENEMY_INDEX_ZOMBIE_N + enemyDirectionHeuristic(enemy_x[i], enemy_y[i], ENEMY_ZOMBIE_HEURISTIC_FACTOR)
					vx = ENEMY_ZOMBIE_SPEED * move_dx[enemy_type[i] - ENEMY_INDEX_ZOMBIE_N]
					vy = ENEMY_ZOMBIE_SPEED * move_dy[enemy_type[i] - ENEMY_INDEX_ZOMBIE_N]
					if (collisionScanCircle(enemy_x[i] + vx, enemy_y[i] + vy, ENEMY_ZOMBIE_RADIUS)) {
						if (isPerimeterWall(collision_info_v0, collision_info_v1)) {
							// dont remove the edge, this is a perimiter edge,
							// reverse direction of zombie
							if (enemy_type[i] < ENEMY_INDEX_ZOMBIE_S) enemy_type[i] += 2 else enemy_type[i] -= 2
						} else removeEdge(collision_info_v0, collision_info_v1)
					} else {
						addZombieTracer(enemy_x[i], enemy_y[i])
						enemy_x[i] += vx
						enemy_y[i] += vy
					}
					enemy_next_update[i] = frame_num + ENEMY_ZOMBIE_UPDATE_FREQ
				}
				ENEMY_INDEX_TANK_NE, ENEMY_INDEX_TANK_SE, ENEMY_INDEX_TANK_SW, ENEMY_INDEX_TANK_NW     -> {
					vx = ENEMY_TANK_SPEED * move_diag_dx[enemy_type[i] - ENEMY_INDEX_TANK_NE]
					vy = ENEMY_TANK_SPEED * move_diag_dy[enemy_type[i] - ENEMY_INDEX_TANK_NE]
					if (collisionScanCircle(enemy_x[i] + vx, enemy_y[i] + vy, ENEMY_TANK_RADIUS)) {
						wall_lookup[collision_info_v0][collision_info_v1]?.let { info ->
							if (info.type != WALL_TYPE_NORMAL || isPerimeterWall(collision_info_v0, collision_info_v1)) {
								// reverse direction of tank
								if (enemy_type[i] < ENEMY_INDEX_TANK_SW) enemy_type[i] += 2 else enemy_type[i] -= 2
							} else {
								removeEdge(collision_info_v0, collision_info_v1)
							}
						}
					} else {
						enemy_x[i] += vx
						enemy_y[i] += vy

						// look for changing direction for no reason
						if (random(0.. 5) == 0) enemy_type[i] = random(ENEMY_INDEX_TANK_NE .. ENEMY_INDEX_TANK_NW)
					}
					enemy_next_update[i] = frame_num + ENEMY_TANK_UPDATE_FREQ
					if (random(0.. ENEMY_TANK_FIRE_FREQ) == 0) addTankMissle(enemy_x[i], enemy_y[i])
				}
			}
			i++
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun isPerimeterWall(v0: Int, v1: Int): Boolean {
		return isPerimiterVertex(v0) && isPerimiterVertex(v1)
	}

	private fun getClosestPlayer(x: Int, y: Int): Player? {
		var closestPlayer: Player? = null
		var closestDist = Int.MAX_VALUE
		for (ii in 0 until num_players) {
			val player = players[ii]
			if (player.state == PLAYER_STATE_ALIVE) {
				val dist = Utils.fastLen(x - player.x, y - player.y)
				if (closestPlayer == null || dist < closestDist) {
					closestPlayer = player
					closestDist = dist
				}
			}
		}
		return closestPlayer
	}

	// -----------------------------------------------------------------------------------------------
	// return 0,1,2,3 [NESW] for a direction.
	// random is a value between 0.0 and 1.0
	// when random == 1.0, then a totally random direction is choosen
	// when random == 0.0, then the direction toward the player is choosen
	private fun enemyDirectionHeuristic(x: Int, y: Int, randomChance: Float): Int {
		val randDir = random(0.. 3) // pick a random direction
		val player = getClosestPlayer(x, y)
		if (player != null) {
			// figure out the dir to the player
			val dx = player.x - x
			val dy = player.y - y
			val playerDir = getDirection(dx, dy)

			// get a rand value between 0 and 1
			return if (Utils.randFloat(1f) < randomChance) randDir else playerDir
		}
		return 0
	}

	// -----------------------------------------------------------------------------------------------
	// remove the enemy missle at index
	private fun removeEnemyMissle(index: Int) {
		num_enemy_missles--
		val m1 = enemy_missle[index]
		val m2 = enemy_missle[num_enemy_missles]
		m1.copy(m2)
	}

	// -----------------------------------------------------------------------------------------------
	// enemy fires a missle, yeah
	private fun enemyFireMissle(player: Player, enemy: Int) {
		if (num_enemy_missles == MAX_ENEMY_MISSLES) return
		val dy: Float
		val m = enemy_missle[num_enemy_missles++]
		m.x = enemy_x[enemy].toFloat()
		m.y = enemy_y[enemy].toFloat()
		m.dx = (player.x - enemy_x[enemy]).toFloat() / (30.0f + Utils.randFloat(10.0f))
		dy = (player.y - enemy_y[enemy]).toFloat()
		m.dy = dy * dy + 2 * dy
		m.dy = -5.0f
		if (player.x < enemy_x[enemy]) m.dx = -4.0f else m.dx = 4.0f
		m.duration = ENEMY_PROJECTILE_DURATION
	}

	// -----------------------------------------------------------------------------------------------
	// player fires a missle, yeah
	private fun addPlayerMissle(player: Player) {
		if (player.num_missles == PLAYER_MAX_MISSLES) return
		var vx = player.target_dx.toFloat()//player.target_x - player.x).toFloat()
		var vy = player.target_dy.toFloat()//(player.target_y - player.y).toFloat()
		val mag = Math.sqrt((vx * vx + vy * vy).toDouble()).toFloat()
		val scale = PLAYER_MISSLE_SPEED / mag
		vx *= scale
		vy *= scale
		val vxi = Math.round(vx)
		val vyi = Math.round(vy)
		if (!collisionScanLine(player.x, player.y, player.x + vxi, player.y + vyi)) {
			val m = player.missles[player.num_missles++]
			m.init(player.x, player.y, vxi, vyi, PLAYER_MISSLE_DURATION)
		}
	}

	// -----------------------------------------------------------------------------------------------
	// remove the player missle at index
	private fun removePlayerMissle(player: Player, index: Int) {
		player.num_missles--
		val m1 = player.missles[index]
		val m2 = player.missles[player.num_missles]
		m1.copy(m2)
	}

	// -----------------------------------------------------------------------------------------------
	private fun updatePlayerMissles(player: Player) {
		var done: Boolean
		var e: Int
		var x0: Int
		var y0: Int
		var x1: Int
		var y1: Int
		var m: MissileInt?
		var m2: MissileInt?
		var i = 0
		while (i < player.num_missles /* increment later */) {
			m = player.missles[i]
			if (m.duration <= 0) {
				removePlayerMissle(player, i)
				continue
			}
			if (!isOnScreen(m.x, m.y)) {
				removePlayerMissle(player, i)
				continue
			}
			m.x += m.dx
			m.y += m.dy
			m.duration--

			// do collision scans and response

			// look for collision with walls
			if (collisionScanLine(m.x, m.y, m.x + m.dx, m.y + m.dy)) {
				x0 = maze_verts_x[collision_info_v0]
				y0 = maze_verts_y[collision_info_v0]
				x1 = maze_verts_x[collision_info_v1]
				y1 = maze_verts_y[collision_info_v1]
				val v0 = collision_info_v0
				val v1 = collision_info_v1
				val dx = x1 - x0
				val dy = y1 - y0
				var bounceVariation = 0f
				val isPerimiterWall = isPerimiterVertex(v0) && isPerimiterVertex(v1)
				val info = wall_lookup[v0][v1]
				var doBounce = false // flag to indicate bounce the missle
				when (info.type) {
					WALL_TYPE_DOOR           -> {
						if (isMegaGunActive(player) && info.state == DOOR_STATE_LOCKED) {
							val mdx = (x0 + x1) / 2
							val mdy = (y0 + y1) / 2
							if (Utils.isCircleIntersectingLineSeg(m.x, m.y, m.x + m.dx, m.y + m.dy, mdx, mdy, PLAYER_MISSLE_SPEED / 4)) {
								addPlayerMsg(player, "LOCK Destroyed")
								info.state = DOOR_STATE_CLOSED
								removePlayerMissle(player, i)
								continue
							}
						}
						doBounce = true
					}
					WALL_TYPE_NORMAL         -> doBounce = if (isMegaGunActive(player)) {
						if (!isPerimiterWall) wallNormalDamage(info, 1)
						removePlayerMissle(player, i)
						continue
					} else {
						true
					}
					WALL_TYPE_ELECTRIC       -> {
						if (frameNumber < info.frame) break
						if (isMegaGunActive(player)) {
							info.frame = frameNumber + WALL_ELECTRIC_DISABLE_FRAMES
						}
						removePlayerMissle(player, i)
						continue
					}
					WALL_TYPE_INDESTRUCTABLE -> doBounce = true
					WALL_TYPE_PORTAL         -> {
						// TODO: Teleport missles too other portal?
						removePlayerMissle(player, i)
						continue
					}
					WALL_TYPE_RUBBER         -> {
						doBounce = true
						info.frequency = Math.min(info.frequency + RUBBER_WALL_FREQENCY_INCREASE_MISSLE, RUBBER_WALL_MAX_FREQUENCY)
						bounceVariation = info.frequency * 100
					}
					else                     -> Utils.unhandledCase(info.type)
				}
				if (doBounce) {
					bounceVectorOffWall(m.dx, m.dy, dx, dy, int_array)
					if (bounceVariation != 0f) {
						val f_array = floatArrayOf(int_array[0].toFloat(), int_array[1].toFloat())
						val degrees = bounceVariation * if (Utils.flipCoin()) -1 else 1
						CMath.rotateVector(f_array, degrees)
						m.dx = Math.round(f_array[0])
						m.dy = Math.round(f_array[1])
					} else {
						m.dx = int_array[0]
						m.dy = int_array[1]
					}
					// need to check for a collision again
					if (collisionScanLine(m.x, m.y, m.x + m.dx, m.y + m.dy)) {
						removePlayerMissle(player, i)
						continue
					}
				}
			}
			done = false

			// look for collision with enemies
			e = 0
			while (e < num_enemies) {
				if (!isOnScreen(enemy_x[e], enemy_y[e])) {
					e++
					continue
				}
				if (enemy_type[e] == ENEMY_INDEX_JAWS || enemy_type[e] == ENEMY_INDEX_LAVA) {
					e++
					continue
				}
				if (Utils.isCircleIntersectingLineSeg(m.x, m.y, m.x + m.dx, m.y + m.dy, enemy_x[e], enemy_y[e], enemy_radius[enemy_type[e]])) {
					val factor = (ENEMY_THUG_PUSHBACK - difficulty.toFloat()) / PLAYER_MISSLE_SPEED
					val dx = Math.round(player.missles[i].dx * factor)
					val dy = Math.round(player.missles[i].dy * factor)
					if (!enemyHit(player, e, dx, dy)) {
						// bounce the projectile off (if not megaGun)
						if (!isMegaGunActive(player)) {
						}
					}
					if (!isMegaGunActive(player)) {
						removePlayerMissle(player, i)
						done = true
						break
					}
				}
				e++
			}
			if (done) {
				continue
			}

			// look for collisions with enemy_tank_missles
			e = 0
			while (e < num_tank_missles) {
				m2 = tank_missle[e]
				if (!isOnScreen(m2.x, m2.y)) {
					e++
					continue
				}
				if (Utils.isPointInsideCircle(m.x, m.y, m2.x, m2.y, TANK_MISSLE_RADIUS)
					|| Utils.isPointInsideCircle(m.x + m.dx, m.y + m.dy, m2.x, m2.y, TANK_MISSLE_RADIUS)) {
					removePlayerMissle(player, i)
					removeTankMissle(e)
					done = true
					break
				}
				e++
			}
			if (done) {
				continue
			}

			// look for collisions with snake_missles
			val mx = (Math.min(m.x, m.x + m.dx) - 2).toFloat()
			val my = (Math.min(m.y, m.y + m.dy) - 2).toFloat()
			val mw = (Math.abs(m.dx) + 4).toFloat()
			val mh = (Math.abs(m.dy) + 4).toFloat()
			e = 0
			while (e < num_snake_missles) {
				val s = snake_missle[e]
				if (!isOnScreen(s.x, s.y)) {
					e++
					continue
				}
				val cResult = collisionMissileSnakeRect(s, mx, my, mw, mh)
				if (cResult == 0 || isMegaGunActive(player)) {
					killSnakeMissle(e)
					removePlayerMissle(player, i)
					done = true
					break
				} else if (cResult > 0) {
					// split the snakle
					// removeSnakeMissle(e);
					val nIndex = addSnakeMissle((mx + mw / 2).roundToInt(), (my + mh / 2).roundToInt())
					if (nIndex < 0) break
					val newSnake = snake_missle[nIndex]

					// assign
					var cc = 0
					for (ii in cResult until s.num_sections) {
						newSnake.dir[cc++] = s.dir[ii]
					}
					s.num_sections = cResult

					// The missle lives on!
					// removePlayerMissle(e);
					// done = true;
					break
				}
				e++
			}
			i++
		}
	}

	// -----------------------------------------------------------------------------------------------
	// return true if the wall is destroyed
	private fun wallNormalDamage(info: Wall, amount: Int): Boolean {
		info.health -= amount
		if (info.health <= 0) {
			removeEdge(info.v0, info.v1)
			return true
		}
		return false
	}

	// -----------------------------------------------------------------------------------------------
	private fun updateRobotMissles() {
		// update enemy missles, cant collide with walls
		var i = 0
		while (i < num_enemy_missles) {
			val mf = enemy_missle[i]

			// remove the missle if below the player, but not too soon
			//if (mf.duration > 30 && mf.y > player_y + playerRadius + 30) {
			//    removeEnemyMissle(i);
			//    continue;
			//}
			if (mf.duration == 0) {
				removeEnemyMissle(i)
				continue
			}
			mf.x += mf.dx
			mf.y += mf.dy

			// TODO : make gravity more severe when player is close or very
			// below our y
			mf.dy += ENEMY_PROJECTILE_GRAVITY
			mf.duration--
			for (ii in 0 until num_players) {
				val player = players[ii]
				val playerRadius = getPlayerRadius(player)
				if (isBarrierActive(player)) {
					val dx1 = mf.x - player.x
					val dy1 = mf.y - player.y
					val dot1 = dx1 * mf.dx + dy1 * mf.dy
					if (dot1 <= 0
						&& Utils.isPointInsideCircle(Math.round(mf.x), Math.round(mf.y), player.x, player.y, PLAYER_RADIUS_BARRIER + ENEMY_PROJECTILE_RADIUS)) {
						mf.dx *= -1f
						mf.dy *= -1f
					}
				} else if (Utils.isPointInsideCircle(Math.round(mf.x), Math.round(mf.y), player.x, player.y, playerRadius + ENEMY_PROJECTILE_RADIUS)) {
					playerHit(player, HIT_TYPE_ROBOT_MISSLE, i)
				}
			}
			i++
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun updateTankMissles() {
		var x0: Int
		var y0: Int
		var x1: Int
		var y1: Int
		var m: MissileInt?

		// update Tank Missles
		var i = 0
		while (i < num_tank_missles) {
			m = tank_missle[i]
			if (m.duration == 0) {
				removeTankMissle(i)
				continue
			}
			if (!isOnScreen(m.x, m.y)) {
				removeTankMissle(i)
				continue
			}
			var done = false
			for (ii in 0 until num_players) {
				val player = players[ii]
				val playerRadius = getPlayerRadius(player)
				if (isBarrierActive(player)) {
					val dx1 = m.x - player.x
					val dy1 = m.y - player.y
					val dot1 = dx1 * m.dx + dy1 * m.dy
					if (dot1 <= 0 && Utils.isPointInsideCircle(m.x, m.y, player.y, player.y, PLAYER_RADIUS_BARRIER + TANK_MISSLE_RADIUS)) {
						m.dx *= -1
						m.dy *= -1
					}
				} else if (Utils.isPointInsideCircle(m.x, m.y, player.x, player.y, playerRadius + TANK_MISSLE_RADIUS)) {
					playerHit(player, HIT_TYPE_TANK_MISSLE, i)
					done = true
					break
				}
			}
			if (done) {
				continue
			}

			// look for collision with walls
			if (collisionScanCircle(m.x + m.dx, m.y + m.dy, TANK_MISSLE_RADIUS)) {
				x0 = maze_verts_x[collision_info_v0]
				y0 = maze_verts_y[collision_info_v0]
				x1 = maze_verts_x[collision_info_v1]
				y1 = maze_verts_y[collision_info_v1]

				// do the bounce off algorithm
				bounceVectorOffWall(m.dx, m.dy, x1 - x0, y1 - y0, int_array)
				m.dx = int_array[0]
				m.dy = int_array[1]
			}
			m.x += m.dx
			m.y += m.dy
			m.duration--
			i++
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun updateMissles() {
		for (i in 0 until num_players) updatePlayerMissles(players[i])
		updateRobotMissles()
		updateTankMissles()
		for (i in 0 until num_players) updateSnakeMissles(players[i])
	}

	// -----------------------------------------------------------------------------------------------
	// draw all missles to the screen
	private fun drawPlayerMissles(player: Player, g: AGraphics) {
		var thickness = 1
		if (isMegaGunActive(player)) {
			g.color = GColor.CYAN
			thickness = 2
		} else {
			g.color = GColor.BLUE
		}

		// Draw player's missles as lines
		for (i in 0 until player.num_missles) {
			val m = player.missles[i]
			val x = m.x - screen_x
			val y = m.y - screen_y
			g.drawLine(x.toFloat(), y.toFloat(), (x + m.dx).toFloat(), (y + m.dy).toFloat(), thickness.toFloat())
		}
	}

	// -----------------------------------------------------------------------------------------------
	// draw all missles to the screen
	private fun drawEnemyMissles(g: AGraphics) {
		// Draw the enemy missles as orange dots
		g.color = GColor.ORANGE
		var x: Int
		var y: Int
		for (i in 0 until num_enemy_missles) {
			val m = enemy_missle[i]
			x = Math.round(m.x) - ENEMY_PROJECTILE_RADIUS - screen_x
			y = Math.round(m.y) - ENEMY_PROJECTILE_RADIUS - screen_y
			g.drawFilledOval(x.toFloat(), y.toFloat(), (ENEMY_PROJECTILE_RADIUS * 2).toFloat(), (ENEMY_PROJECTILE_RADIUS * 2).toFloat())
		}
		for (i in 0 until num_tank_missles) {
			val m = tank_missle[i]
			x = m.x - TANK_MISSLE_RADIUS - screen_x
			y = m.y - TANK_MISSLE_RADIUS - screen_y
			g.color = GColor.GREEN
			g.drawOval(x.toFloat(), y.toFloat(), (TANK_MISSLE_RADIUS * 2).toFloat(), (TANK_MISSLE_RADIUS * 2).toFloat())
			g.color = GColor.YELLOW
			g.drawOval((x + 2).toFloat(), (y + 2).toFloat(), (TANK_MISSLE_RADIUS * 2 - 4).toFloat(), (TANK_MISSLE_RADIUS * 2 - 4).toFloat())
		}
	}

	// -----------------------------------------------------------------------------------------------
	// draw all missles to the screen
	private fun drawMissles(g: AGraphics) {
		for (i in 0 until num_players) drawPlayerMissles(players[i], g)
		drawEnemyMissles(g)
		drawSnakeMissles(g)
	}

	// -----------------------------------------------------------------------------------------------
	// Update the player powerup
	private fun updatePlayerPowerup(player: Player) {
		if (player.powerup_duration++ > PLAYER_POWERUP_DURATION
			&& (player.powerup != POWERUP_GHOST || !collisionScanCircle(player.x, player.y, getPlayerRadius(player)))) {
			// disable powerup
			player.powerup = -1
			//mgun_collision_v0 = mgun_collision_v1 = -1;
			//mgun_hit_count = 0;
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun isStunned(player: Player): Boolean {
		if (player.state == PLAYER_STATE_SPECTATOR) return false
		val mag2 = player.stun_dx * player.stun_dx + player.stun_dy * player.stun_dy
		return mag2 > 0.1f
	}

	// -----------------------------------------------------------------------------------------------
	private fun setPlayerStunned(playerIndex: Int, dx: Float, dy: Float, force: Float) {
		var len = Utils.fastLen(dx, dy)
		if (len < 1) len = 1f
		val player = players[playerIndex]
		val len_inv = 1.0f / len * force
		player.stun_dx = dx * len_inv
		player.stun_dy = dy * len_inv
		player.hulk_charge_frame = 0
		var angle = 0
		while (angle < 360) {
			addParticle(player.x, player.y, PARTICLE_TYPE_PLAYER_STUN, random(10.. 20), playerIndex, angle)
			angle += 45
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun getCollisionInfo(wallPts: IntArray) {
		wallPts[0] = maze_verts_x[collision_info_v0] // p0.x
		wallPts[1] = maze_verts_y[collision_info_v0] // p0.y
		wallPts[2] = maze_verts_x[collision_info_v1] // p1.x
		wallPts[3] = maze_verts_y[collision_info_v1] // p1.y
	}

	// -----------------------------------------------------------------------------------------------
	private fun playerTouchDoor(player: Player, door: Wall) {
		when (door.state) {
			DOOR_STATE_LOCKED  -> if (!isHulkActive(player) && player.keys > 0) {
				addPlayerMsg(player, "UNLOCKING DOOR")
				door.state = DOOR_STATE_CLOSED
				door.frame = frameNumber
				player.keys--
			}
			DOOR_STATE_CLOSING -> {
				door.state = DOOR_STATE_OPENING
				var framesElapsed = frameNumber - door.frame
				framesElapsed = DOOR_SPEED_FRAMES - framesElapsed
				door.frame = frameNumber - framesElapsed
			}
			DOOR_STATE_CLOSED  -> {
				door.state = DOOR_STATE_OPENING
				door.frame = frameNumber
			}
			DOOR_STATE_OPENING -> {
			}
			DOOR_STATE_OPEN    -> {
			}
			else               -> Utils.unhandledCase(door.state)
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun isWallActive(info: Wall): Boolean {
		return if (info.v0 == info.v1) false
		else when (info.type) {
			in -1..WALL_TYPE_NONE -> false
			WALL_TYPE_ELECTRIC -> info.frame <= frameNumber
			else -> true
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Update the player
	private fun updatePlayer(player: Player) {
		val playerRadius = getPlayerRadius(player)
		Arrays.fill(player.barrier_electric_wall, -1)
		if (player.next_state_frame <= frameNumber) {
			when (player.state) {
				PLAYER_STATE_TELEPORTED -> {
					player.dy = 0
					player.dx = 0
					player.state = PLAYER_STATE_ALIVE
				}
				PLAYER_STATE_SPAWNING   -> player.state = PLAYER_STATE_ALIVE
				PLAYER_STATE_EXPLODING  -> if (player.lives > 0) {
					player.lives--
					resetPlayer(player)
					player.state = PLAYER_STATE_SPAWNING
				} else {
					player.state = PLAYER_STATE_SPECTATOR
				}
			}
		}

		// see if the player has changed the cell location
		val sx = player.cellXY[0]
		val sy = player.cellXY[1]
		computeCell(player.x, player.y, player.cellXY)
		if (player.cellXY[0] != sx || player.cellXY[1] != sy) {
			val endXY = IntArray(2)
			computeCell(end_x, end_y, endXY)
			player.path.clear()
			findPath(maze_cells, player.cellXY[0], player.cellXY[1], endXY[0], endXY[1], player.path)
		}

		// look for player trying to fire a missle
		if (player.state == PLAYER_STATE_ALIVE && player.firing) {
			if (player.last_shot_frame + getPlayerShotFreq(player) < frameNumber) {
				player.last_shot_frame = frameNumber
				if (isHulkActive(player)) {
					// charge!
					playerHulkCharge(player)
				} else {
					addPlayerMissle(player)
				}
			}
		}
		if (player.powerup >= 0) updatePlayerPowerup(player)
		if (isHulkActive(player)) {
			if (player.scale < PLAYER_HULK_SCALE) player.scale += PLAYER_HULK_GROW_SPEED
		} else if (player.scale > 1.0f) {
			player.scale -= PLAYER_HULK_GROW_SPEED
		}
		player.scale = player.scale.coerceIn(1.0f, PLAYER_HULK_SCALE)
		var dx = 0f
		var dy = 0f
		if (player.state != PLAYER_STATE_EXPLODING) {
			if (isHulkActiveCharging(player)) {
				dx = player.hulk_charge_dx.toFloat()
				dy = player.hulk_charge_dy.toFloat()
			} else {
				dx = player.dx.toFloat()
				dy = player.dy.toFloat()
			}

			// try to move the player
			if (isStunned(player)) {
				dx = player.stun_dx
				dy = player.stun_dy
				player.stun_dx *= 0.9f
				player.stun_dy *= 0.9f
				if (dx * dx + dy * dy < 0.1) {
					player.stun_dy = 0f
					player.stun_dx = player.stun_dy
					dy = 0f
					dx = dy
				}
			}
		}
		player.collision_info = null
		if (dx != 0f || dy != 0f) {
			if (GAME_VISIBILITY) updatePlayerVisibility(player)
			player.movement++
			player.dir = getPlayerDir(dx, dy)
			var px = (player.x + dx).roundToInt()
			var py = (player.y + dy).roundToInt()

			// do collision detect against walls
			// working good
			var collision = collisionScanCircle(px, py, playerRadius)
			log.debug("primary verts: ${collision_verts.joinToString()}")
			if (!isPerimeterWall(collision_info_v0, collision_info_v1)) {
				if (canPassThroughWalls(player)) collision = false
			}
			if (collision) {
				var info = wall_lookup[collision_info_v0][collision_info_v1]
				player.collision_info = info
				log.debug("Player hit wall type [" + getWallTypeString(info.type) + "] info [" + info + "]")
				if (info.type == WALL_TYPE_ELECTRIC && isBarrierActive(player)) {
					// no collision
				} else {
					if (doPlayerHitWall(player, info, dx, dy)) return
					val wallx0 = maze_verts_x[collision_info_v0]
					val wally0 = maze_verts_y[collision_info_v0]
					val wallx1 = maze_verts_x[collision_info_v1]
					val wally1 = maze_verts_y[collision_info_v1]
					val pos = floatArrayOf(px.toFloat(), py.toFloat())
					fixPositionFromWall(
						pos,
						wallx0.toFloat(),
						wally0.toFloat(),
						wallx1.toFloat(),
						wally1.toFloat(),
						playerRadius.toFloat()
					)

					// reassign
					px = Math.round(pos[0])
					py = Math.round(pos[1])

					// now search the other walls
					for (i in 1 until collision_verts.size) {
						val v1 = collision_verts[i]
						if (v1 == collision_info_v1) continue
						if (v1 < 0 || v1 >= MAZE_NUM_VERTS) continue
						info = wall_lookup[collision_info_v0][v1]
						if (!isWallActive(info)) continue
						val x1 = maze_verts_x[v1]
						val y1 = maze_verts_y[v1]
						if (info.type == WALL_TYPE_DOOR && collisionDoorCircle(info, wallx0, wally0, x1, y1, px, py, playerRadius) ||
							Utils.isCircleIntersectingLineSeg(wallx0, wally0, x1, y1, px, py, playerRadius)) {
							if (doPlayerHitWall(player, info, dx, dy)) return

							// check the dot product of orig wall and this wall
							val dx0 = wallx1 - wallx0
							val dy0 = wally1 - wally0
							val dx1 = x1 - wallx0
							val dy1 = y1 - wally0
							val dot = dx0 * dx1 + dy0 + dy1
							if (dot > 0) return
							fixPositionFromWall(pos, wallx0.toFloat(), wally0.toFloat(), x1.toFloat(), y1.toFloat(), playerRadius.toFloat())
							// reassign
							px = Math.round(pos[0])
							py = Math.round(pos[1])
							break
						}
					}
				}
			}
			val new_dx = px - player.x
			val new_dy = py - player.y
			player.target_dx = new_dx
			player.target_dy = new_dy
			if (frameNumber % 4 == 0) {
				if (isHulkActiveCharging(player)) {
					addPlayerTracer(player, player.x, player.y, player.dir, GColor.GREEN)
				} else if (player.powerup == POWERUP_SUPER_SPEED) {
					addPlayerTracer(player, player.x, player.y, player.dir, GColor.YELLOW)
				}
			}
			player.x = px
			player.y = py
			if (enemy_robot_speed < ENEMY_ROBOT_MAX_SPEED + difficulty && player.movement % (ENEMY_ROBOT_SPEED_INCREASE - difficulty * 10) == 0) enemy_robot_speed++
		} else {
			player.movement = 0
		}
		screen_x = (player.x - screen_width / 2).coerceIn(-MAZE_VERTEX_NOISE..MAZE_WIDTH - screen_width + MAZE_VERTEX_NOISE)
		screen_y = (player.y - screen_height / 2).coerceIn(-MAZE_VERTEX_NOISE..MAZE_HEIGHT - screen_height + MAZE_VERTEX_NOISE)

		if (game_type == GAME_TYPE_CLASSIC) {
			if (total_enemies == 0 && num_particles == 0) {
				nextLevel()
			}
		} else {
			if (Utils.isPointInsideCircle(player.x, player.y, end_x, end_y, playerRadius + 10)) {
				nextLevel()
			}
		}
	}

	fun nextLevel() {
		gameLevel++
		buildAndPopulateLevel()
	}

	fun prevLevel() {
		gameLevel = (gameLevel - 1).coerceAtLeast(1)
		buildAndPopulateLevel()
	}

	private fun getPlayerIndex(player: Player): Int {
		for (i in 0 until num_players) if (players[i] === player) return i
		assert(false)
		return -1
	}

	// -----------------------------------------------------------------------------------------------
	// return true when the collision is a hit, false if we should
	// fix the position
	private fun doPlayerHitWall(player: Player, info: Wall, dx: Float, dy: Float): Boolean {
		val playerIndex = getPlayerIndex(player)
		when (info.type) {
			WALL_TYPE_ELECTRIC -> if (playerHit(player, HIT_TYPE_ELECTRIC_WALL, 0)) {
				return true
			}
			WALL_TYPE_PORTAL   -> if (!isHulkActive(player)) playerTeleport(player, info.p0, info.p1) else return false
			WALL_TYPE_DOOR     -> {
				playerTouchDoor(player, info)
				return false
			}
			WALL_TYPE_NORMAL   -> {
				if (isHulkActiveCharging(player)) {
					// do damage on the wall
					val damage = random((WALL_NORMAL_HEALTH / 4) + 5)
					if (wallNormalDamage(info, damage)) return true
					setPlayerStunned(playerIndex, -Math.round(dx).toFloat(), -Math.round(dy).toFloat(), 10f)
				}
				return false
			}
			WALL_TYPE_RUBBER   -> {
				setPlayerStunned(playerIndex, -Math.round(dx).toFloat(), -Math.round(dy).toFloat(), 10f)
				return false
			}
			else               -> return false
		}
		return true
	}

	// -----------------------------------------------------------------------------------------------
	private fun fixPositionFromWall(pos: FloatArray, x0: Float, y0: Float, x1: Float, y1: Float, radius: Float) {
		// get distanse^2 too wall
		val d2 = Utils.distSqPointSegment(pos[0], pos[1], x0, y0, x1, y1)

		// compute new dx,dy along collision edge
		val vx = x0 - x1
		val vy = y0 - y1

		// compute normal of wall
		var nx = -vy
		var ny = vx

		// compute vector from w0 too p
		val pwdx = pos[0] - x0
		val pwdy = pos[1] - y0

		// compute dot of pd n
		val dot = nx * pwdx + ny * pwdy

		// reverse direction of normal if neccessary
		if (dot < 0) {
			nx *= -1f
			ny *= -1f
		}

		// normalize the normal
		val nmag = Math.sqrt((nx * nx + ny * ny).toDouble()).toFloat()
		val nmag_inv = 1.0f / nmag
		nx *= nmag_inv
		ny *= nmag_inv

		// compute vector along n to adjust p
		val d_ = radius - Math.sqrt(d2.toDouble()).toFloat()
		val nx_ = nx * d_
		val ny_ = ny * d_

		// compute p prime, the new position, a correct distance
		// from the wall
		val px_ = pos[0] + nx_
		val py_ = pos[1] + ny_

		// reassign
		pos[0] = px_
		pos[1] = py_
	}

	// -----------------------------------------------------------------------------------------------
	private fun playerHulkCharge(player: Player) {
		player.hulk_charge_frame = frameNumber

		//addPlayerMsg("HULK SMASH!");
		player.firing = false
		player.hulk_charge_dy = 0
		player.hulk_charge_dx = 0
		if (player.dx != 0 || player.dy != 0) {
			//log.debug("*** CHARGING ***");
			if (player.dx != 0) {
				if (player.dx < 0) player.hulk_charge_dx = player.dx - PLAYER_HULK_CHARGE_SPEED_BONUS else player.hulk_charge_dx = player.dx + PLAYER_HULK_CHARGE_SPEED_BONUS
			}
			if (player.dy != 0) {
				if (player.dy < 0) player.hulk_charge_dy = player.dy - PLAYER_HULK_CHARGE_SPEED_BONUS else player.hulk_charge_dy = player.dy + PLAYER_HULK_CHARGE_SPEED_BONUS
			}
		} else {
			val speed = getPlayerSpeed(player)
			when (player.dir) {
				DIR_UP    -> player.hulk_charge_dy = -speed
				DIR_RIGHT -> player.hulk_charge_dx = speed
				DIR_DOWN  -> player.hulk_charge_dy = speed
				DIR_LEFT  -> player.hulk_charge_dx = -speed
				else      -> Utils.unhandledCase(player.dir)
			}
		}
	}

	val PLAYER_HULK_CHARGE_FRAMES = 20
	private fun isHulkActiveCharging(player: Player): Boolean {
		return isHulkActive(player) && frameNumber - player.hulk_charge_frame < PLAYER_HULK_CHARGE_FRAMES
	}

	// -----------------------------------------------------------------------------------------------
	private fun enemyTeleport(enemyIndex: Int, v0: Int, v1: Int) {
		val radius = enemy_radius[enemyIndex] + 10
		// get wall defined by v0, v1
		val x0 = maze_verts_x[v0]
		val y0 = maze_verts_y[v0]
		val x1 = maze_verts_x[v1]
		val y1 = maze_verts_y[v1]
		// midpoint
		val mx = (x0 + x1) / 2
		val my = (y0 + y1) / 2
		// delta
		val dx = x1 - x0
		val dy = y1 - y0
		// normal
		val nx = -dy
		val len = Utils.fastLen(nx, dx) - 1
		val len_inv = 1.0f / len
		val newX = mx + Math.round(len_inv * nx * radius)
		val newY = my + Math.round(len_inv * dx * radius)
		enemy_x[enemyIndex] = newX
		enemy_y[enemyIndex] = newY
	}

	// -----------------------------------------------------------------------------------------------
	private fun playerTeleport(player: Player, v0: Int, v1: Int) {
		log.debug("PLAYER TELEPORT v0 = $v0 v1 = $v1")
		//player.teleported = true;
		player.state = PLAYER_STATE_TELEPORTED
		player.next_state_frame = frameNumber + 1
		val radius = getPlayerRadius(player) + 10
		// get wall defined by v0, v1
		val x0 = maze_verts_x[v0]
		val y0 = maze_verts_y[v0]
		val x1 = maze_verts_x[v1]
		val y1 = maze_verts_y[v1]
		// midpoint
		val mx = (x0 + x1) / 2
		val my = (y0 + y1) / 2
		// delta
		val dx = x1 - x0
		val dy = y1 - y0
		// normal
		val nx = -dy
		val len = Utils.fastLen(nx, dx) - 1
		val len_inv = 1.0f / len
		// make the player's motion change to the closest to that of the normal
		if (player.dx != 0 || player.dy != 0) {
			player.dy = 0
			player.dx = player.dy
			// clear the key_down flags
			val speed = getPlayerSpeed(player)
			if (Math.abs(nx) > Math.abs(dx)) {
				player.dx = if (nx < 0) -speed else speed
			} else {
				player.dy = if (dx < 0) -speed else speed
			}
		}
		val newX = mx + Math.round(len_inv * nx * radius)
		val newY = my + Math.round(len_inv * dx * radius)
		player.x = newX
		player.y = newY
	}

	private fun getPlayerShotFreq(player: Player): Int {
		return if (isMegaGunActive(player)) PLAYER_SHOT_FREQ_MEGAGUN else PLAYER_SHOT_FREQ
	}

	// enum
	val HIT_TYPE_ENEMY = 0
	val HIT_TYPE_TANK_MISSLE = 1
	val HIT_TYPE_SNAKE_MISSLE = 2
	val HIT_TYPE_ROBOT_MISSLE = 3
	val HIT_TYPE_ELECTRIC_WALL = 4

	// -----------------------------------------------------------------------------------------------
	// Player hit event
	// return true if player takes damage, false if barrier ect.
	private fun playerHit(player: Player, hitType: Int, index: Int): Boolean {
		if (isInvincible(player)) return false

		//final float playerRadius = getPlayerRadius(player);
		val playerIndex = getPlayerIndex(player)
		when (hitType) {
			HIT_TYPE_ELECTRIC_WALL                      -> return if (isHulkActive(player)) {
				setPlayerStunned(playerIndex, -player.dx.toFloat(), -player.dy.toFloat(), 30f)
				false
			} else if (isBarrierActive(player)) {
				false
			} else {
				player.state = PLAYER_STATE_EXPLODING
				player.next_state_frame = frameNumber + PLAYER_DEATH_FRAMES
				true
			}
			HIT_TYPE_ENEMY                              -> {
				if (isHulkActive(player) || player.scale > 1.2f) {
					if (isHulkActiveCharging(player) && enemyHit(player, index, player.dx, player.dy)) {
						if (random(0 .. 6) == 0) addPlayerMsg(player, "HULK SMASH!")
					} else {
						if (random(10) == 0) {
							addPlayerMsg(player, "Hulk confused ???")
						}
						setPlayerStunned(playerIndex, (player.x - enemy_x[index]).toFloat(), (player.y - enemy_y[index]).toFloat(), ENEMY_ROBOT_FORCE)
					}
					return false
				}
				if (isHulkActive(player)) {
					// unhulk
					setDebugEnabled(Debug.HULK, false)
					if (player.powerup == POWERUP_HULK) player.powerup = -1
					// bounce the missle
					enemy_missle[index].dx *= -1f
					enemy_missle[index].dy *= -1f
					// stun player
					setPlayerStunned(playerIndex, Math.round(enemy_missle[index].dx).toFloat(),
						Math.round(enemy_missle[index].dy).toFloat(),
						ENEMY_PROJECTILE_FORCE)
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
				enemy_missle[index].dx *= -1f
				enemy_missle[index].dy *= -1f
				setPlayerStunned(playerIndex, Math.round(enemy_missle[index].dx).toFloat(),
					Math.round(enemy_missle[index].dy).toFloat(),
					ENEMY_PROJECTILE_FORCE)
				return false
			} else if (!isGhostActive(player)) {
				player.hit_type = hitType
				player.hit_index = index
				player.state = PLAYER_STATE_EXPLODING
				player.next_state_frame = frameNumber + PLAYER_DEATH_FRAMES
				return true
			}
			HIT_TYPE_SNAKE_MISSLE                       ->             // TODO: Snake is attached to player, slowing player down
				//snake_missle[index].state = SNAKE_STATE_ATTACHED;
				return true
			else                                        -> Utils.unhandledCase(hitType)
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
	private fun getPlayerDir(dx: Float, dy: Float): Int {
		if (dx < 0) return DIR_LEFT
		if (dx > 0) return DIR_RIGHT
		return if (dy < 0) DIR_UP else DIR_DOWN
	}

	// -----------------------------------------------------------------------------------------------
	fun getPlayerRadius(player: Player): Int {
		return Math.round(player.scale * PLAYER_RADIUS)
	}

	// -----------------------------------------------------------------------------------------------
	// Draw the Player
	private fun drawPlayer(player: Player, g: AGraphics, px: Int, py: Int, dir: Int) {
		var color: GColor? = null
		if (player.scale == PLAYER_HULK_SCALE) {
			color = GColor.GREEN
		} else if (player.scale > 1.0f) {
			val invScale = Math.round(255.0f * 1.0f - (player.scale - 1.0f))
			color = GColor(invScale, 255, invScale)
		}
		if (game_state == GAME_STATE_PLAY) {
			val numFrames = PLAYER_POWERUP_DURATION - PLAYER_POWERUP_WARNING_FRAMES
			if (player.powerup > 0 && player.powerup_duration > numFrames) {
				if (frameNumber % 8 < 4) color = GColor.RED
			}
		}
		drawPlayerBody(player, g, px + 1, py, dir, color)
		drawPlayerEyes(player, g, px + 1, py, dir)
		drawPlayerBarrier(player, g, px + 1, py)
		/*
        if (isStunned(player)) {
            for (int i=0; i<5; i++)
                this.addParticle(player.x, player.y, PARTICLE_TYPE_PLAYER_STUN, random(10,20), player);
        } */if (Utils.isDebugEnabled()) {
			g.color = GColor.BLUE
			g.drawRect((px - 1).toFloat(), (py - 1).toFloat(), 3f, 3f)
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Draw the Player's Body
	// TODO: Avoid using new by caching all the colors we want
	private fun drawPlayerBody(player: Player, g: AGraphics, px: Int, py: Int, dir: Int, optionalColor: GColor?) {
		var priColor = GColor.LIGHT_GRAY
		var secColor = GColor.DARK_GRAY
		if (isGhostActive(player)) {
			if (optionalColor == null) {
				secColor = GColor(200, 200, 200, 100)
				priColor = secColor // lightgray
			} else {
				secColor = GColor(optionalColor.red,
					optionalColor.green,
					optionalColor.blue,
					100.0f / 255)
				priColor = secColor
			}
		} else {
			if (optionalColor != null) {
				secColor = optionalColor
				priColor = secColor
			}
		}
		val scale = player.scale
		g.color = priColor
		val walk = Math.round(((player.movement % 3).toFloat() - 1) * scale)
		val f1 = Math.round(1.0f * scale)
		val f2 = Math.round(2.0f * scale)
		val f3 = Math.round(3.0f * scale)
		val f4 = Math.round(4.0f * scale)
		val f6 = Math.round(6.0f * scale)
		val f8 = Math.round(8.0f * scale)
		val f10 = Math.round(10.0f * scale)
		val f12 = Math.round(12.0f * scale)
		val f14 = Math.round(14.0f * scale)
		val f16 = Math.round(16.0f * scale)
		val f20 = Math.round(20.0f * scale)
		val f22 = Math.round(22.0f * scale)
		val f24 = Math.round(24.0f * scale)
		// draw head
		g.drawFilledRect((px - f10).toFloat(), (py - f10).toFloat(), f20.toFloat(), f4.toFloat())
		g.drawFilledRect((px - f8).toFloat(), (py - f12).toFloat(), f16.toFloat(), f8.toFloat())
		// draw body
		g.drawFilledRect((px - f4).toFloat(), (py - f14).toFloat(), f8.toFloat(), f22.toFloat())
		if (dir == 0 || dir == 2) {
			g.drawFilledRect((px - f6).toFloat(), (py - f2).toFloat(), f12.toFloat(), f6.toFloat())
			g.drawFilledRect((px - f12).toFloat(), py.toFloat(), f24.toFloat(), f4.toFloat())
			// draw arms
			g.drawFilledRect((px - f12).toFloat(), (py + f4).toFloat(), f4.toFloat(), f6.toFloat())
			g.drawFilledRect((px + f8).toFloat(), (py + f4).toFloat(), f4.toFloat(), f6.toFloat())
			// draw legs
			g.drawFilledRect((px - f6).toFloat(), (py + f6 + walk).toFloat(), f4.toFloat(), f10.toFloat())
			g.drawFilledRect((px + f2).toFloat(), (py + f6 - walk).toFloat(), f4.toFloat(), f10.toFloat())
			g.drawFilledRect((px - f8).toFloat(), (py + f12 + walk).toFloat(), f2.toFloat(), f4.toFloat())
			g.drawFilledRect((px + f6).toFloat(), (py + f12 - walk).toFloat(), f2.toFloat(), f4.toFloat())
		} else if (dir == 1) {
			// body
			g.drawFilledRect((px - f6).toFloat(), (py - f2).toFloat(), f10.toFloat(), f10.toFloat())
			// legs
			g.drawFilledRect((px - f6).toFloat(), (py + f8 + walk).toFloat(), f4.toFloat(), f8.toFloat())
			g.drawFilledRect((px - f2).toFloat(), (py + f12 + walk).toFloat(), f2.toFloat(), f4.toFloat())
			g.color = secColor
			g.drawFilledRect((px + f1).toFloat(), (py + f8 - walk).toFloat(), f4.toFloat(), f8.toFloat())
			g.drawFilledRect((px + f3).toFloat(), (py + f12 - walk).toFloat(), f2.toFloat(), f4.toFloat())
			// arm
			g.color = priColor
			g.drawFilledRect((px - f4).toFloat(), (py - f2).toFloat(), f4.toFloat(), f6.toFloat())
			g.drawFilledRect((px - f2).toFloat(), (py + f2).toFloat(), f4.toFloat(), f4.toFloat())
			g.drawFilledRect(px.toFloat(), (py + f4).toFloat(), f4.toFloat(), f4.toFloat())
		} else {
			// body
			g.drawFilledRect((px - f4).toFloat(), (py - f2).toFloat(), f10.toFloat(), f10.toFloat())
			// legs
			g.drawFilledRect((px + f2).toFloat(), (py + f8 + walk).toFloat(), f4.toFloat(), f8.toFloat())
			g.drawFilledRect(px.toFloat(), (py + f12 + walk).toFloat(), f2.toFloat(), f4.toFloat())
			g.color = secColor
			g.drawFilledRect((px - f6).toFloat(), (py + f6 - walk).toFloat(), f4.toFloat(), f8.toFloat())
			g.drawFilledRect((px - f8).toFloat(), (py + f10 - walk).toFloat(), f2.toFloat(), f4.toFloat())
			// arm
			g.color = priColor
			g.drawFilledRect(px.toFloat(), (py - f2).toFloat(), f4.toFloat(), f6.toFloat())
			g.drawFilledRect((px - f2).toFloat(), (py + f2).toFloat(), f4.toFloat(), f4.toFloat())
			g.drawFilledRect((px - f4).toFloat(), (py + f4).toFloat(), f4.toFloat(), f4.toFloat())
		}

		// yell: "HULK SMASH!" when run over people, robot and walls (not thugs,
		// they bump back)
		// also, cant shoot when hulk
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawPlayerEyes(player: Player, g: AGraphics, px: Int, py: Int, dir: Int) {
		val scale = player.scale

		//int f1 = Math.round(1.0f * scale);
		val f2 = Math.round(2.0f * scale)
		//int f3 = Math.round(3.0f * scale);
		val f4 = Math.round(4.0f * scale)
		//int f6 = Math.round(6.0f * scale);
		val f8 = Math.round(8.0f * scale)
		val f10 = Math.round(10.0f * scale)
		//int f12 = Math.round(12.0f * scale);
		//int f14 = Math.round(14.0f * scale);
		val f16 = Math.round(16.0f * scale)
		//int f20 = Math.round(20.0f * scale);
		//int f22 = Math.round(22.0f * scale);
		val f24 = Math.round(24.0f * scale)
		if (dir == 2) {
			// draw the eye
			g.color = GColor.BLACK
			g.drawFilledRect((px - f8).toFloat(), (py - f10).toFloat(), f16.toFloat(), f4.toFloat())
			g.color = GColor.RED
			var index = frameNumber % 12
			if (index > 6) index = 12 - index
			g.drawFilledRect((px - f8 + index * f2).toFloat(), (py - f10).toFloat(), f4.toFloat(), f4.toFloat())
		} else if (dir == 1) {
			g.color = GColor.BLACK
			g.drawFilledRect(px.toFloat(), (py - f10).toFloat(), f10.toFloat(), f4.toFloat())
			g.color = GColor.RED
			val index = frameNumber % 12
			if (index < 4) g.drawFilledRect((px + index * f2).toFloat(), (py - f10).toFloat(), f4.toFloat(), f4.toFloat()) else if (index >= 8) g.drawFilledRect((px + (f24 - index * f2)).toFloat(), (py - f10).toFloat(), f4.toFloat(), f4.toFloat())
		} else if (dir == 3) {
			g.color = GColor.BLACK
			g.drawFilledRect((px - f10).toFloat(), (py - f10).toFloat(), f10.toFloat(), f4.toFloat())
			g.color = GColor.RED
			val index = frameNumber % 12
			if (index < 4) g.drawFilledRect((px - f10 + index * f2).toFloat(), (py - f10).toFloat(), f4.toFloat(), f4.toFloat()) else if (index >= 8) g.drawFilledRect((px - f10 + (f24 - index * f2)).toFloat(), (py - f10).toFloat(), f4.toFloat(), f4.toFloat())
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawPlayerBarrierElectricWall2(player: Player, g: AGraphics, x: Int, y: Int) {

		// we are touching an electric wall, so we become 1 unit.
		// the end points of the wall are in the array:
		val wx0 = player.barrier_electric_wall[0]
		val wy0 = player.barrier_electric_wall[1]
		val wx1 = player.barrier_electric_wall[2]
		val wy1 = player.barrier_electric_wall[3]

		// compute deltas betwen p and endpofloats
		val dx0 = (x - wx0).toFloat()
		val dy0 = (y - wy0).toFloat()
		val dx1 = (x - wx1).toFloat()
		val dy1 = (y - wy1).toFloat()
		val radius = PLAYER_RADIUS_BARRIER.toFloat()
		for (c in 0..1) {
			var x0 = 1f
			var y0 = 0f
			var r0 = Utils.randFloatX(3f) + radius
			val sr = r0
			for (i in 0 until STATIC_FIELD_SECTIONS - 1) {
				val x1 = x0 * STATIC_FIELD_COS_T - y0 * STATIC_FIELD_SIN_T
				val y1 = y0 * STATIC_FIELD_SIN_T + x0 * STATIC_FIELD_COS_T
				val r1 = Utils.randFloatX(3f) + radius
				val lx0 = Math.round(x0 * r0)
				val ly0 = Math.round(y0 * r0)
				val lx1 = Math.round(x1 * r1)
				val ly1 = Math.round(y1 * r1)
				g.drawLine((x + lx0).toFloat(), (y + ly0).toFloat(), (x + lx1).toFloat(), (y + ly1).toFloat())
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
			val lx0 = Math.round(x0 * r0)
			val ly0 = Math.round(y0 * r0)
			val lx1 = Math.round(sr)
			val ly1 = Math.round(0f)
			g.drawLine((x + lx0).toFloat(), (y + ly0).toFloat(), (x + lx1).toFloat(), (y + ly1).toFloat())
			val dot0 = lx0 * dx0 + ly0 * dy0
			val dot1 = lx0 * dx1 + ly0 * dy1
			if (dot0 <= 0) {
				drawElectricWall_r(g, x + lx0, y + ly0, wx0, wy0, 2)
			}
			if (dot1 <= 0) {
				drawElectricWall_r(g, x + lx0, y + ly0, wx1, wy1, 2)
			}
			g.drawLine((x + lx0).toFloat(), (y + ly0).toFloat(), (x + lx1).toFloat(), (y + ly1).toFloat())
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawPlayerBarrierElectricWall3(player: Player, g: AGraphics, x: Int, y: Int) {

		// we are touching an electric wall, so we become 1 unit.
		// the end points of the wall are in the array:
		val wx0 = player.barrier_electric_wall[0]
		val wy0 = player.barrier_electric_wall[1]
		val wx1 = player.barrier_electric_wall[2]
		val wy1 = player.barrier_electric_wall[3]
		val radius = PLAYER_RADIUS_BARRIER.toFloat()
		for (c in 0..1) {
			var x0 = 1f
			var y0 = 0f
			var r0 = Utils.randFloatX(3f) + radius
			val sr = r0

			// dest point of field to connect wall pts to
			var bx0 = 0
			var by0 = 0
			var bx1 = 0
			var by1 = 0

			// closest static field pt
			var bestDot0 = Float.MAX_VALUE
			var bestDot1 = Float.MAX_VALUE
			for (i in 0 until STATIC_FIELD_SECTIONS) {
				val x1 = x0 * STATIC_FIELD_COS_T - y0 * STATIC_FIELD_SIN_T
				val y1 = y0 * STATIC_FIELD_SIN_T + x0 * STATIC_FIELD_COS_T
				var r1 = Utils.randFloatX(3f) + radius
				if (i == STATIC_FIELD_SECTIONS - 1) {
					r1 = sr
				}
				val lx0 = x + Math.round(x0 * r0)
				val ly0 = y + Math.round(y0 * r0)
				val lx1 = x + Math.round(x1 * r1)
				val ly1 = y + Math.round(y1 * r1)
				g.drawLine(lx0.toFloat(), ly0.toFloat(), lx1.toFloat(), ly1.toFloat())
				val dx0 = lx0 - wx0
				val dy0 = ly0 - wy0
				val dx1 = lx1 - wx1
				val dy1 = ly1 - wy1
				val dot0 = (dx0 * dx0 + dy0 * dy0).toFloat()
				val dot1 = (dx1 * dx1 + dy1 * dy1).toFloat()
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
	private fun drawPlayerBarrierElectricWall1(player: Player, g: AGraphics, px: Int, py: Int) {
		// we are touching an electric wall, so we become 1 unit.
		// the end points of the wall are in the array:
		val wx0 = player.barrier_electric_wall[0].toFloat()
		val wy0 = player.barrier_electric_wall[1].toFloat()
		val wx3 = player.barrier_electric_wall[2].toFloat()
		val wy3 = player.barrier_electric_wall[3].toFloat()

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
		val d0 = Math.sqrt((nx0 * nx0 + ny0 * ny0).toDouble()).toFloat()
		val d1 = Math.sqrt((nx1 * nx1 + ny1 * ny1).toDouble()).toFloat()
		if (d0 > 0.01f && d1 > 0.01f) {
			val d0_inv = 1.0f / d0
			val d1_inv = 1.0f / d1
			val radius = PLAYER_RADIUS_BARRIER.toFloat()
			nx0 = nx0 * d0_inv * radius
			ny0 = ny0 * d0_inv * radius
			nx1 = nx1 * d1_inv * radius
			ny1 = ny1 * d1_inv * radius
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
	private fun drawPlayerBarrier(player: Player, g: AGraphics, px: Int, py: Int) {
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
		var x0: Int
		var y0: Int
		var x1: Int
		var y1: Int
		x0 = bezier_pts_x[0]
		y0 = bezier_pts_y[0]
		for (i in 1 until bezier_pts_x.size) {
			if (i < bezier_pts_x.size / 2) {
				x1 = bezier_pts_x[i] + random(-i .. i)
				y1 = bezier_pts_y[i] + random(-i .. i)
			} else {
				val ii = bezier_pts_x.size - i
				x1 = bezier_pts_x[i] + random(-ii .. ii)
				y1 = bezier_pts_y[i] + random(-ii .. ii)
			}
			g.drawLine(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat())
			x0 = x1
			y0 = y1
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Draw the goal (End)
	private fun drawEnd(g: AGraphics, x: Int, y: Int) {

		// draw an X
		g.color = GColor.CYAN
		val minRadius = 10
		val maxRadius = 22
		val numRings = 3
		val ringSpacing = 4
		val animSpeed = 5 // higher is slower
		for (i in 0 until numRings) {
			val f = frameNumber / animSpeed + i * ringSpacing
			val r = maxRadius - (f % (maxRadius - minRadius) + minRadius)
			g.drawOval((x - r).toFloat(), (y - r).toFloat(), (r * 2).toFloat(), (r * 2).toFloat())
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun getDirection(dx: Int, dy: Int): Int {
		// return 0=N, E=1, S=2, W=3
		var dir = -1
		dir = if (Math.abs(dx) > Math.abs(dy)) {
			if (dx < 0) DIR_LEFT else DIR_RIGHT
		} else {
			if (dy < 0) DIR_UP else DIR_DOWN
		}
		return dir
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawElectricWall_r(g: AGraphics, x0: Int, y0: Int, x1: Int, y1: Int, num: Int) {
		if (num > 0) {
			// find midpoint
			val mx = (x0 + x1) / 2 + random(-num .. num)
			val my = (y0 + y1) / 2 + random(-num .. num)
			drawElectricWall_r(g, x0, y0, mx, my, num - 1)
			drawElectricWall_r(g, mx, my, x1, y1, num - 1)
		} else {
			g.drawLine(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat())
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun getBreakingWallOffset(p: Int, num: Int): Int {
		val min = num * 3 + 10
		val max = num * 4 + 20
		return p * 113 % (max - min) * if (p % 2 == 0) 1 else -1
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawBreakingWall_r(g: AGraphics, x0: Int, y0: Int, x1: Int, y1: Int, num: Int, f: Int) {
		if (num > 0) {
			val xoff = getBreakingWallOffset(f, num)
			val yoff = getBreakingWallOffset(f, num)
			// find midpoint
			val mx = (x0 + x1) / 2 + xoff
			val my = (y0 + y1) / 2 + yoff
			drawBreakingWall_r(g, x0, y0, mx, my, num - 1, f)
			drawBreakingWall_r(g, mx, my, x1, y1, num - 1, f)
		} else {
			g.drawLine(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat(), 3f)
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun collisionBreakingWallLine_r(lx0: Int, ly0: Int, lx1: Int, ly1: Int, wx0: Int, wy0: Int, wx1: Int, wy1: Int, num: Int, f: Int, newDelta: IntArray): Boolean {
		if (num > 0) {
			val xoff = getBreakingWallOffset(f, num)
			val yoff = getBreakingWallOffset(f, num)
			val mx = (wx0 + wx1) / 2 + xoff
			val my = (wy0 + wy1) / 2 + yoff
			if (collisionBreakingWallLine_r(lx0, ly0, lx1, ly1, wx0, wy0, mx, my, num - 1, f, newDelta)) return true
			if (collisionBreakingWallLine_r(lx0, ly0, lx1, ly1, mx, my, wx1, wy1, num - 1, f, newDelta)) return true
		} else {
			if (Utils.isLineSegsIntersecting(lx0, ly0, lx1, ly1, wx0, wy1, wx1, wy1)) {
				newDelta[0] = wx1 - wx0
				newDelta[1] = wy1 - wy0
			}
		}
		return false
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawLineFade_r(g: AGraphics, x0: Int, y0: Int, x1: Int, y1: Int, outer: GColor, inner: GColor, num: Int, factor: Float) {
		val mx = (x0 + x1) / 2
		val my = (y0 + y1) / 2
		if (num > 0) {
			val cm = outer.interpolateTo(inner, factor) //Utils.interpolate(outer, inner, factor);
			drawLineFade_r(g, x0, y0, mx, my, outer, cm, num - 1, factor)
			drawLineFade_r(g, mx, my, x1, y1, cm, inner, num - 1, factor)
		} else {
			g.color = outer
			g.drawLine(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat(), 3f)
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawPortalWall(g: AGraphics, x0: Int, y0: Int, x1: Int, y1: Int) {
		val mx = (x0 + x1) / 2
		val my = (y0 + y1) / 2
		drawLineFade_r(g, x0, y0, mx, my, GColor.LIGHT_GRAY, throbbing_white, 3, 0.5f)
		drawLineFade_r(g, mx, my, x1, y1, throbbing_white, GColor.LIGHT_GRAY, 3, 0.5f)
	}

	// -----------------------------------------------------------------------------------------------
	/*
    private void drawRubberWall(AGraphics g, int x0, int y0, int x1, int y1) {
        float x2=0, y2=0;
        float vx = (x1-x0)*0.5f;
        float vy = (y1-y0)*0.5f;
        float nx = 0, ny = 0;
        g.setColor(GColor.BLUE);

        float factor1 = 0.1f;
        float factor2 = 0.15f;

        int iterations = 9;
        int thickness = 2;

        switch (getFrameNumber()%8) {
        case 0:
        case 4:
            g.drawLine(x0, y0, x1, y1, thickness);
            return;
        case 1:
        case 3:
            nx = -vy*factor1;
            ny =  vx*factor1;
            break;
        case 2:
            nx = -vy*factor2;
            ny =  vx*factor2;
            break;
        case 7:
        case 5:
            nx =  vy*factor1;
            ny = -vx*factor1;
            break;
        case 6:
            nx =  vy*factor2;
            ny = -vx*factor2;
            break;
        }

        x2 = x0 + vx + nx;
        y2 = y0 + vy + ny;

        Utils.computeBezierCurvePoints(bezier_pts_x, bezier_pts_y,x0,y0,x2,y2,x2,y2,x1,y1);

        g.drawLineStrip(bezier_pts_x, bezier_pts_y, thickness);
        if (Utils.isDebugEnabled()) {
            int x = Math.round(x2);
            int y = Math.round(y2);
            g.drawRect(x,y,1,1);
        }
    }*/
	// -----------------------------------------------------------------------------------------------
	private fun drawRubberWall(g: AGraphics, x0: Int, y0: Int, x1: Int, y1: Int, frequency: Float) {
		var x2 = 0f
		var y2 = 0f
		val vx = (x1 - x0) * 0.5f
		val vy = (y1 - y0) * 0.5f
		var nx = 0f
		var ny = 0f
		g.color = GColor.BLUE
		val thickness = 2
		if (frequency < EPSILON) {
			g.drawLine(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat(), thickness.toFloat())
		} else {
			val factor = CMath.sine((frameNumber * 50).toFloat()) * frequency
			nx = -vy * factor
			ny = vx * factor
			x2 = x0 + vx + nx
			y2 = y0 + vy + ny
			Utils.computeBezierCurvePoints(
				bezier_pts_x,
				bezier_pts_y,
				x0.toFloat(),
				y0.toFloat(),
				x2,
				y2,
				x2,
				y2,
				x1.toFloat(),
				y1.toFloat()
			)
			g.drawLineStrip(bezier_pts_x, bezier_pts_y, thickness)
			if (Utils.isDebugEnabled()) {
				val x = Math.round(x2)
				val y = Math.round(y2)
				g.drawRect(x.toFloat(), y.toFloat(), 1f, 1f)
			}
		}
	}

	val bezier_pts_x = IntArray(10)
	val bezier_pts_y = IntArray(10)

	// -----------------------------------------------------------------------------------------------
	private fun drawDoor(g: AGraphics, door: Wall, x0: Int, y0: Int, x1: Int, y1: Int) {
		g.color = DOOR_COLOR
		val framesElapsed = frameNumber - door.frame + 1
		var dx = x1 - x0
		var dy = y1 - y0
		val mx = (x1 + x0) / 2
		val my = (y1 + y0) / 2
		when (door.state) {
			DOOR_STATE_CLOSED -> {
				g.drawLine(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat(), DOOR_THICKNESS.toFloat())
				return
			}
			DOOR_STATE_LOCKED -> {
				g.drawLine(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat(), DOOR_THICKNESS.toFloat())
				g.color = GColor.RED
				g.drawFilledCircle(mx, my, 10)
				return
			}
			DOOR_STATE_OPEN   -> {
				dx /= 4
				dy /= 4
				g.drawLine(x0.toFloat(), y0.toFloat(), (x0 + dx).toFloat(), (y0 + dy).toFloat(), DOOR_THICKNESS.toFloat())
				g.drawLine(x1.toFloat(), y1.toFloat(), (x1 - dx).toFloat(), (y1 - dy).toFloat(), DOOR_THICKNESS.toFloat())
				if (framesElapsed >= DOOR_OPEN_FRAMES) {
					door.state = DOOR_STATE_CLOSING
					door.frame = frameNumber
				}
				return
			}
		}
		val delta = getDoorDelta(dx, dy, framesElapsed, door.state)
		dx = delta[0]
		dy = delta[1]
		g.drawLine(x0.toFloat(), y0.toFloat(), (x0 + dx).toFloat(), (y0 + dy).toFloat(), DOOR_THICKNESS.toFloat())
		g.drawLine(x1.toFloat(), y1.toFloat(), (x1 - dx).toFloat(), (y1 - dy).toFloat(), DOOR_THICKNESS.toFloat())
		if (framesElapsed >= DOOR_SPEED_FRAMES) {
			if (door.state == DOOR_STATE_OPENING) door.state = DOOR_STATE_OPEN else door.state = DOOR_STATE_CLOSED
			door.frame = frameNumber
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun getDoorDelta(dx: Int, dy: Int, framesElapsed: Int, doorState: Int): IntArray {
		var len = Utils.fastLen(dx, dy).toFloat()
		if (len < 1) len = 1f
		val len_inv = 1.0f / len
		var l = DOOR_SPEED_FRAMES_INV * framesElapsed * (len * 0.25f)
		if (doorState == DOOR_STATE_OPENING) l = len / 4 - l
		val delta = IntArray(2)
		delta[0] = Math.round(0.25f * dx + len_inv * dx * l)
		delta[1] = Math.round(0.25f * dy + len_inv * dy * l)
		return delta
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawDebugWallInfo(g: AGraphics) {
		g.color = GColor.GREEN
		for (i in 1 until MAZE_NUM_VERTS) {
			for (j in 0 until i) {
				var x0 = maze_verts_x[i]
				var y0 = maze_verts_y[i]
				var x1 = maze_verts_x[j]
				var y1 = maze_verts_y[j]
				if (!isOnScreen(x0, y0) && !isOnScreen(x1, y1)) continue
				x0 -= screen_x
				y0 -= screen_y
				x1 -= screen_x
				y1 -= screen_y
				val mx = (x0 + x1) / 2
				val my = (y0 + y1) / 2
				val info = wall_lookup[i][j] ?: continue
				if (info.type == WALL_TYPE_NONE) {
					//g.drawLine(x0, y0, x1, y1);
				} else {
					val str = info.toString()
					//g.drawString(str, mx, my);
					g.drawString(str, mx.toFloat(), my.toFloat())
				}
			}
		}
	}

	private fun drawWall(g: AGraphics, info: Wall, x0: Int, y0: Int, x1: Int, y1: Int) {
		when (info.type) {
			WALL_TYPE_NORMAL         ->             // translate and draw the line
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
					g.drawLine(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat(), MAZE_WALL_THICKNESS.toFloat())
				}
			WALL_TYPE_ELECTRIC       -> {
				g.color = GColor.YELLOW
				g.setLineWidth(1f)
				var done = false
				var i = 0
				while (i < num_players) {
					val player = players[i]
					if (isBarrierActive(player) && Utils.isCircleIntersectingLineSeg(x0, y0, x1, y1, player.x - screen_x, player.y - screen_y, PLAYER_RADIUS_BARRIER)) {
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
			WALL_TYPE_INDESTRUCTABLE -> {
				g.color = GColor.DARK_GRAY
				g.drawLine(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat(), (MAZE_WALL_THICKNESS + 2).toFloat())
			}
			WALL_TYPE_PORTAL         -> drawPortalWall(g, x0, y0, x1, y1)
			WALL_TYPE_RUBBER         -> drawRubberWall(g, x0, y0, x1, y1, info.frequency)
			WALL_TYPE_DOOR           -> drawDoor(g, info, x0, y0, x1, y1)
			WALL_TYPE_PHASE_DOOR     -> {
				g.color = GColor.GREEN
				g.drawLine(x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat(), 1f)
			}
			else                     -> Utils.unhandledCase(info.type)
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Render the maze from screen_x/y
	private fun drawMaze(g: AGraphics) {
		var x0: Int
		var x1: Int
		var y0: Int
		var y1: Int
		for (i in 1 until MAZE_NUM_VERTS) {
			for (j in 0 until i) {
				wall_lookup[i][j].let { info ->
					if (!isWallActive(info))
						return@let
					x0 = maze_verts_x[info.v0]
					y0 = maze_verts_y[info.v0]
					x1 = maze_verts_x[info.v1]
					y1 = maze_verts_y[info.v1]
					if (!isOnScreen(x0, y0) && !isOnScreen(x1, y1))
						return@let
					if (GAME_VISIBILITY) {
						val player = player
						if (!info.visible) {
							// see if player can 'see' the wall
							val mx = (x0 + x1) / 2
							val my = (y0 + y1) / 2
							if (canSee(player.x, player.y, mx, my)) {
								info.visible = true
							} else {
								return@let
							}
						}
					}
					x0 -= screen_x
					y0 -= screen_y
					x1 -= screen_x
					y1 -= screen_y
					drawWall(g, info, x0, y0, x1, y1)
					if (info.frequency > 0) {
						info.frequency -= RUBBER_WALL_FREQENCY_COOLDOWN
					}

					// if (debug_draw_edge_arrows) {
					if (isDebugEnabled(Debug.DRAW_MAZE_INFO)) {
						// draw a little arrow head to show the direction of the
						// edge
						vec[0] = (x1 - x0).toFloat()
						vec[1] = (y1 - y0).toFloat()
						val mag = sqrt((vec[0] * vec[0] + vec[1] * vec[1]).toDouble()).toFloat()
						if (mag > EPSILON) {
							vec[0] *= 10 / mag
							vec[1] *= 10 / mag
							CMath.rotateVector(vec, 150f)
							g.drawLine(
								x1.toFloat(),
								y1.toFloat(),
								(x1 + vec[0].roundToInt()).toFloat(),
								(y1 + vec[1].roundToInt()).toFloat(),
								MAZE_WALL_THICKNESS.toFloat()
							)
							CMath.rotateVector(vec, 60f)
							val x2 = x1 + vec[0].roundToInt()
							val y2 = y1 + vec[1].roundToInt()
							g.drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), MAZE_WALL_THICKNESS.toFloat())
						}
					}
				}
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	// layout an even grid of vertices, with some noise
	private fun buildVertices(max_x: Int, max_y: Int, noise: Int) {
		var x = 0
		var y = 0
		var index: Int
		verts_min_x = 0
		verts_min_y = 0
		verts_max_x = 0
		verts_max_y = 0
		for (i in 0 until MAZE_NUMCELLS_Y + 1) {
			for (j in 0 until MAZE_NUMCELLS_X + 1) {
				index = i * (MAZE_NUMCELLS_X + 1) + j
				maze_verts_x[index] = x + random(-noise .. noise)
				maze_verts_y[index] = y + random(-noise .. noise)
				x += MAZE_CELL_WIDTH
				// compute the actual world dimension
				if (verts_min_x > maze_verts_x[index]) verts_min_x = maze_verts_x[index]
				if (verts_max_x < maze_verts_x[index]) verts_max_x = maze_verts_x[index]
				if (verts_min_y > maze_verts_y[index]) verts_min_y = maze_verts_y[index]
				if (verts_max_y < maze_verts_y[index]) verts_max_y = maze_verts_y[index]
			}
			x = 0
			y += MAZE_CELL_HEIGHT
		}
	}

	// -----------------------------------------------------------------------------------------------
	// delete all edges in the maze graph and digraph
	private fun clearAllWalls() {
		for (i in 0 until MAZE_NUM_VERTS - 1) {
			for (ii in i until MAZE_NUM_VERTS) {
				wall_lookup[i][ii] = wall_lookup[ii][i]
				wall_lookup[ii][i].clear()
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	// add an edge to the wall_lookup
	private fun addWall(v0: Int, v1: Int) {
		log.debug("Add wall between " + v0 + " and " + v1);
		val wall = getWall(v0, v1)
		wall.clear()
		wall.v0 = v0
		wall.v1 = v1
		wall.type = WALL_TYPE_INDESTRUCTABLE
	}

	// -----------------------------------------------------------------------------------------------
	private fun getWall(v0: Int, v1: Int): Wall {
		if (v0 in 0 until wall_lookup.size && v1 in 0 until wall_lookup[0].size)
			return wall_lookup[v0][v1]
		return Wall().also {
			it.clear()
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun initWall(v0: Int, v1: Int) {
		val wall = wall_lookup[v1][v0]
		wall_lookup[v0][v1] = wall_lookup[v1][v0]
		wall.v0 = v0
		wall.v1 = v1
		wall.type = WALL_TYPE_NORMAL
	}

	// -----------------------------------------------------------------------------------------------
	// build a new maze and populate with generators. Quantity based on
	// game_level
	private fun buildAndPopulateLevel() {
		enemy_robot_speed = ENEMY_ROBOT_SPEED
		total_enemies = MAX_ENEMIES
		people_picked_up = 0
		addEnemy(0, 0, ENEMY_INDEX_BRAIN, true)
		if (game_type == GAME_TYPE_CLASSIC) {
			buildAndPopulateClassic()
		} else buildAndpopulateRobocraze()
	}

	// -----------------------------------------------------------------------------------------------
	// rearrange the enemies away from the player
	private fun shuffleEnemies() {
		var x: Int
		var y: Int
		var d = 1000
		var r: Int
		val wid = verts_max_x - verts_min_x
		val hgt = verts_max_y - verts_min_y
		for (i in 0 until num_enemies) {
			r = enemy_radius[enemy_type[i]]
			do {
				x = random(r .. wid - r)
				y = random(r .. hgt - r)
				val player = getClosestPlayer(x, y)
				if (player != null) d = Utils.fastLen(player.x - x, player.y - y)
			} while (d < 100 - gameLevel)
			enemy_x[i] = x
			enemy_y[i] = y
		}
	}

	private fun resetPlayer(player: Player) {
		player.num_missles = 0
		player.num_tracers = 0
		player.x = player.start_x
		player.y = player.start_y
		player.powerup = -1
		player.scale = 1.0f
		player.last_shot_frame = 0
		player.stun_dy = 0f
		player.stun_dx = player.stun_dy
		player.hit_index = -1
		player.hit_type = -1
		player.next_state_frame = frameNumber + PLAYER_SPAWN_FRAMES
		player.state = PLAYER_STATE_SPAWNING
	}

	private fun resetLevel() {
		num_tank_missles = 0
		num_enemy_missles = 0
		num_snake_missles = 0
		num_powerups = 0
		num_enemies = 0
		num_zombie_tracers = 0
		num_particles = 0
		num_msgs = 0
		people_points = PEOPLE_START_POINTS
		enemy_robot_speed = ENEMY_ROBOT_SPEED + difficulty + gameLevel / 4
		for (i in 0 until num_players) {
			resetPlayer(players[i])
		}

		//setFrameNumber(0);
		//game_start_frame = 0;
		game_start_frame = frameNumber
	}

	// -----------------------------------------------------------------------------------------------
	// this func is called after a player dies to remove missles, msgs and so forth
	// also called to initialize the level. this contains the data initialization in
	// one place.
	/*
    private void resetLevel(boolean clearEnemies) {
        // remove any existing enemies and missles
        num_tank_missles = 0;
        for (int i=0; i<num_players; i++) {
            players[i].num_missles = 0;
            players[i].num_tracers = 0;
            players[i].x = players[i].start_x;
            players[i].y = players[i].start_y;
            players[i].powerup = -1;
            players[i].scale = 1.0f;
            players[i].last_shot_frame = 0;
        }
        num_enemy_missles = 0;
        num_snake_missles = 0;
        num_powerups = 0;

        if (clearEnemies)
            num_enemies = 0;

        num_zombie_tracers = 0;

        num_particles = 0;
        num_msgs = 0;

        people_points = PEOPLE_START_POINTS;

        enemy_robot_speed = ENEMY_ROBOT_SPEED + difficulty + (game_level / 4);

        for (int i=0; i<num_players; i++) {
            players[i].hit_index = -1;
            players[i].hit_type = -1;
            players[i].next_state_frame = getFrameNumber() + this.PLAYER_SPAWN_FRAMES;
            players[i].state = PLAYER_STATE_SPAWNING;
        }

        //setFrameNumber(0);
        //game_start_frame = 0;
        game_start_frame = getFrameNumber();
    }*/
	// -----------------------------------------------------------------------------------------------
	private fun buildAndPopulateClassic() {
		val wid = WORLD_WIDTH_CLASSIC
		val hgt = WORLD_HEIGHT_CLASSIC

		// build the vertices
		buildVertices(wid, hgt, 0)
		clearAllWalls()

		// put players at the center
		var sx = wid / 2
		val sy = hgt / 2
		for (i in 0 until num_players) {
			val player = players[i]
			player.x = sx
			player.start_x = player.x
			player.y = sy
			player.start_y = player.y
			sx += PLAYER_RADIUS * 2 + 10
		}
		// add all the perimiter edges
		val bottom = (MAZE_NUMCELLS_X + 1) * MAZE_NUMCELLS_Y
		for (i in 0 until MAZE_NUMCELLS_X) {
			addWall(i, i + 1)
			addWall(bottom + i, bottom + i + 1)
		}
		for (i in 0 until MAZE_NUMCELLS_Y) {
			addWall(i * (MAZE_NUMCELLS_X + 1), (i + 1) * (MAZE_NUMCELLS_X + 1))
			addWall(i * (MAZE_NUMCELLS_X + 1) + MAZE_NUMCELLS_X, (i + 1) * (MAZE_NUMCELLS_X + 1) + MAZE_NUMCELLS_X)
		}

		// start the timer for the highlighted player
		resetLevel()

		// add some robots
		var count = MAX_ENEMIES / 2 + (difficulty - 1) * 4 + gameLevel / 2
		for (i in 0 until count) addEnemy(0, 0, random(ENEMY_INDEX_ROBOT_N .. ENEMY_INDEX_ROBOT_W), true)

		// Add some Thugs
		count = ENEMY_GEN_INITIAL + (difficulty - 1) * 4 + gameLevel + random(-5 .. 4 + gameLevel)
		for (i in 0 until count) addEnemy(0, 0, random(ENEMY_INDEX_THUG_N .. ENEMY_INDEX_THUG_W), true)

		// Add Some Brains And / Or Tanks
		if (gameLevel > 1 && gameLevel % 2 == 0) {
			// add brains
			count = ENEMY_GEN_INITIAL + (difficulty - 1) * 4 + gameLevel + random(-5 .. 5 + gameLevel)
			for (i in 0 until count) addEnemy(0, 0, ENEMY_INDEX_BRAIN, true)
		} else if (gameLevel > 2 && gameLevel % 2 == 1) {
			// Add Some tanks
			count = ENEMY_GEN_INITIAL + (difficulty - 1) * 2 + gameLevel + random(-5 .. 5 + gameLevel)
			for (i in 0 until count) addEnemy(0, 0, random(ENEMY_INDEX_TANK_NE .. ENEMY_INDEX_TANK_NW), true)
		}
		shuffleEnemies()
		addPeople()
		buildRandomWalls()
	}

	private lateinit var usedCells: Array<BooleanArray>

	// -----------------------------------------------------------------------------------------------
	private fun addEnemyAtRandomCell(type: Int, dx: Int, dy: Int) {
		var ex = 0
		var ey = 0
		val max_tries = 1000
		var tries = 0
		while (tries < max_tries) {
			val cx = random(MAZE_NUMCELLS_X)
			val cy = random(MAZE_NUMCELLS_Y)
			if (usedCells[cx][cy]) {
				tries++
				continue
			}
			ex = cx * MAZE_CELL_WIDTH + MAZE_CELL_WIDTH / 2
			ey = cy * MAZE_CELL_HEIGHT + MAZE_CELL_HEIGHT / 2
			var ok = true
			for (i in 0 until num_players) {
				val player = players[i]
				if (ex == player.start_x && ey == player.start_y) {
					ok = false
					break
				}
			}
			if (!ok) {
				tries++
				continue
			}
			if (ex == end_x && ey == end_y) {
				tries++
				continue
			}
			usedCells[cx][cy] = true
			break
			tries++
		}
		if (tries < max_tries) {
			addEnemy(ex, ey, type, true)
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun addWallCount(count: IntArray, v0: Int, v1: Int) {
		assert(v0 >= 0 && v0 < MAZE_NUM_VERTS)
		assert(v1 >= 0 && v0 < MAZE_NUM_VERTS)
		assert(count.size == MAZE_NUM_VERTS)
		assert(v0 != v1)
		count[v0]++
		assert(count[v0] <= 8)
		count[v1]++
		assert(count[v1] <= 8)
	}

	// -----------------------------------------------------------------------------------------------
	private fun computeWallEnding(count: IntArray, v0: Int, v1: Int) {
		val wall = getWall(v0, v1)
		if (wall.type != WALL_TYPE_NONE) {
			if (count[v0] == 2 || count[v1] == 2) wall.ending = true
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun buildAndpopulateRobocraze() {

		// build the vertices
		buildVertices(MAZE_WIDTH, MAZE_HEIGHT, MAZE_VERTEX_NOISE)

		maze_cells = Array(MAZE_NUMCELLS_X) {
			IntArray(MAZE_NUMCELLS_Y)
		}
		
		
		// create a temp matrix of cells.  This will hold the maze information.
		// each cell is a 4bit value with north, south, east west bits.
		//int[][] maze_cells = new int[MAZE_NUMCELLS_X][];
		var i: Int
		var j: Int
		
		// init all maze cells such that all the walls are set
		i = 0
		while (i < MAZE_NUMCELLS_X) {
			maze_cells[i] = IntArray(MAZE_NUMCELLS_Y)
			j = 0
			while (j < MAZE_NUMCELLS_Y) {
				maze_cells[i][j] = WALL_NORTH or WALL_SOUTH or WALL_EAST or WALL_WEST
				j++
			}
			i++
		}

		// pick a start and end point
		val cell_start_x = random(0 until MAZE_NUMCELLS_X)
		val cell_start_y = random(0 until MAZE_NUMCELLS_Y)
		var cell_end_x = random(0 until MAZE_NUMCELLS_X)
		var cell_end_y = random(0 until MAZE_NUMCELLS_Y)

		// continue searching until we are not on top of each other
		while (Math.abs(cell_end_x - cell_start_x) < MAZE_NUMCELLS_X / 2 ||
			Math.abs(cell_end_y - cell_start_y) < MAZE_NUMCELLS_Y / 2) {
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
		i = 0
		while (i < MAZE_NUMCELLS_X) {
			j = 0
			while (j < MAZE_NUMCELLS_Y) {

				// compute the vertex indices associated with this cell
				val upleft = i + j * (MAZE_NUMCELLS_X + 1)
				val upright = i + 1 + j * (MAZE_NUMCELLS_X + 1)
				val downleft = i + (j + 1) * (MAZE_NUMCELLS_X + 1)
				val downright = i + 1 + (j + 1) * (MAZE_NUMCELLS_X + 1)

				//.log.debug("cells[i][j]=" + cells[i][j] + ", i=" + i + ", j=" + j + ", upleft=" + upleft + ", upright=" + upright + ", downleft=" + downleft + ", downright=" + downright);
				//.log.debug("wall count=" + Arrays.toString(vertex_wall_count));
				if (maze_cells[i][j] and WALL_NORTH != 0) {
					addWall(upleft, upright)
					addWallCount(vertex_wall_count, upleft, upright)
				}
				if (maze_cells[i][j] and WALL_EAST != 0) {
					addWall(upright, downright)
					addWallCount(vertex_wall_count, upright, downright)
				}
				if (maze_cells[i][j] and WALL_SOUTH != 0) {
					addWall(downleft, downright)
					addWallCount(vertex_wall_count, downleft, downright)
				}
				if (maze_cells[i][j] and WALL_WEST != 0) {
					addWall(upleft, downleft)
					addWallCount(vertex_wall_count, downleft, upleft)
				}
				j++
			}
			i++
		}

		//if (Utils.isDebugEnabled())
		//  log.debug("vertex wall count : " + Utils.toString(vertex_wall_count));

		// now visit all the wall again and set the 'ending' flag for those walls
		// with a vertex that has a wall_count == 1.
		i = 0
		while (i < MAZE_NUMCELLS_X) {
			j = 0
			while (j < MAZE_NUMCELLS_Y) {

				// compute the vertex indices associated with this cell
				val upleft = i + j * (MAZE_NUMCELLS_X + 1)
				val upright = i + 1 + j * (MAZE_NUMCELLS_X + 1)
				val downleft = i + (j + 1) * (MAZE_NUMCELLS_X + 1)
				val downright = i + 1 + (j + 1) * (MAZE_NUMCELLS_X + 1)
				computeWallEnding(vertex_wall_count, upleft, upright)
				computeWallEnding(vertex_wall_count, upright, downright)
				computeWallEnding(vertex_wall_count, downleft, downright)
				computeWallEnding(vertex_wall_count, downleft, upleft)
				j++
			}
			i++
		}
		usedCells = Array(MAZE_NUMCELLS_X) { BooleanArray(MAZE_NUMCELLS_Y) }
		usedCells[cell_start_x][cell_start_y] = true
		usedCells[cell_end_x][cell_end_y] = true

		// position the player at the center starting cell
		var sx = cell_start_x * MAZE_CELL_WIDTH + MAZE_CELL_WIDTH / 2
		var sy = cell_start_y * MAZE_CELL_HEIGHT + MAZE_CELL_HEIGHT / 2
		for (ii in 0 until num_players) {
			val player = players[ii]
			player.x = sx
			player.y = sy
			when (ii % 4) {
				0 -> sy += PLAYER_RADIUS * 2 + 10
				1 -> sx += PLAYER_RADIUS * 2 + 10
				2 -> sy -= PLAYER_RADIUS * 2 + 10
				3 -> sx -= PLAYER_RADIUS * 2 + 10
			}
			// remember the starting point
			player.start_x = player.x
			player.start_y = player.y
		}
		// compute maze coord position of ending cell
		end_x = cell_end_x * MAZE_CELL_WIDTH + MAZE_CELL_WIDTH / 2
		end_y = cell_end_y * MAZE_CELL_HEIGHT + MAZE_CELL_HEIGHT / 2
		resetLevel()

		// Now add a few generators and thugs
		var gen_x: Int
		var gen_y: Int
		val num_gens = ENEMY_GEN_INITIAL + gameLevel + difficulty

		// make sure we dont try to add more gens then there are cells
		if (num_enemies > MAZE_NUMCELLS_X * MAZE_NUMCELLS_Y - 2) num_enemies = MAZE_NUMCELLS_X * MAZE_NUMCELLS_Y - 2
		num_enemies = 0

		// make sure only 1 generator per cell
		i = 0
		while (i < num_gens) {
			addEnemyAtRandomCell(ENEMY_INDEX_GEN, random(-10 .. 10), random(-10 .. 10))
			i++
		}

		// add some jaws
		i = 0
		while (i < 3 + gameLevel) {
			addEnemyAtRandomCell(ENEMY_INDEX_JAWS, random(-20 .. 20), random(-20 .. 20))
			i++
		}

		// add some lava
		i = 0
		while (i < 4 + gameLevel + difficulty) {
			addEnemyAtRandomCell(ENEMY_INDEX_LAVA, random(-10 .. 10), random(-10 .. 10))
			i++
		}

		// now place some thugs by the generators
		i = 0
		while (i < num_gens) {
			gen_x = enemy_x[i] + random(-ENEMY_SPAWN_SCATTER + ENEMY_THUG_RADIUS .. ENEMY_SPAWN_SCATTER - ENEMY_THUG_RADIUS)
			gen_y = enemy_y[i] + random(-ENEMY_SPAWN_SCATTER + ENEMY_THUG_RADIUS .. ENEMY_SPAWN_SCATTER - ENEMY_THUG_RADIUS)
			addEnemy(gen_x, gen_y, random(ENEMY_INDEX_THUG_N .. ENEMY_INDEX_THUG_W), true)
			i++
		}

		// now place some brains or tanks
		if (gameLevel > 1) {
			if (gameLevel % 2 == 0) {
				i = 0
				while (i < num_gens) {
					gen_x = enemy_x[i] + random(-ENEMY_SPAWN_SCATTER + ENEMY_BRAIN_RADIUS.. ENEMY_SPAWN_SCATTER - ENEMY_BRAIN_RADIUS)
					gen_y = enemy_y[i] + random(-ENEMY_SPAWN_SCATTER + ENEMY_BRAIN_RADIUS .. ENEMY_SPAWN_SCATTER - ENEMY_BRAIN_RADIUS)
					addEnemy(gen_x, gen_y, ENEMY_INDEX_BRAIN, true)
					i++
				}
			} else {
				i = 0
				while (i < num_gens) {
					gen_x = enemy_x[i] + random(-ENEMY_SPAWN_SCATTER + ENEMY_TANK_RADIUS .. ENEMY_SPAWN_SCATTER - ENEMY_TANK_RADIUS)
					gen_y = enemy_y[i] + random(-ENEMY_SPAWN_SCATTER + ENEMY_TANK_RADIUS .. ENEMY_SPAWN_SCATTER - ENEMY_TANK_RADIUS)
					addEnemy(gen_x, gen_y, random(ENEMY_INDEX_TANK_NE.. ENEMY_INDEX_TANK_NW), true)
					i++
				}
			}
		}
		addPeople()
		buildRandomWalls()
	}

	// -----------------------------------------------------------------------------------------------
	private fun buildRandomWalls() {
		// portal tracking
		var pv0 = -1
		var pv1 = -1
		val weights = wallChanceForLevel
		for (v0 in 1 until MAZE_NUM_VERTS) {
			for (v1 in 0 until v0) {
				val wall = wall_lookup[v0][v1]
				if (wall.v0 == wall.v1)
					continue
				val perim = isPerimiterVertex(v0) && isPerimiterVertex(v1)
				if (perim) {
					wall.type = WALL_TYPE_INDESTRUCTABLE
					continue
				}
				if (game_type == GAME_TYPE_CLASSIC) {
					continue
				}
				if (wall.type == WALL_TYPE_NONE) {
					if (random(100) < gameLevel) {
						wall.initDoor(DOOR_STATE_CLOSED, v0, v1)
					}
					continue
				}

				// if this is an ending wall, then skip
				// an ending wall has 1 vertex with only 1 wall on it (itself)
				if (wall.ending) continue
				wall.type = Utils.chooseRandomFromSet(*weights)
				when (wall.type) {
					WALL_TYPE_NORMAL -> wall.health = WALL_NORMAL_HEALTH
					WALL_TYPE_DOOR   -> wall.state = DOOR_STATE_LOCKED
					WALL_TYPE_PORTAL -> if (pv0 < 0) {
						pv0 = v0
						pv1 = v1
					} else {
						wall.p0 = pv0
						wall.p1 = pv1
						pv1 = -1
						pv0 = -1
					}
				}
			}
		}
		if (pv0 >= 0) {
			// undo the portal
			wall_lookup[pv0][pv1].type = WALL_TYPE_INDESTRUCTABLE
		}
	}

	// this version picks cells at random and makes the mazes much more difficult
	private fun mazeSearch_r2(cells: Array<IntArray>, x: Int, y: Int, end_x: Int, end_y: Int) {
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
			val dir_list = directionHeuristic(x, y, end_x, end_y)
			for (i in 0..3) {
				val nx = x + move_dx[dir_list[i]]
				val ny = y + move_dy[dir_list[i]]
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
			val nx = x + move_dx[dir_list[i]]
			val ny = y + move_dy[dir_list[i]]
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

	// -----------------------------------------------------------------------------------------------
	// remove the edge from both graphs
	private fun removeEdge(v0: Int, v1: Int) {
		wall_lookup[v0][v1].type = WALL_TYPE_NONE
	}

	// -----------------------------------------------------------------------------------------------
	// Return true if this vertex runs along the perimiter of the maze
	private fun isPerimiterVertex(vertex: Int): Boolean {
		val x = vertex % (MAZE_NUMCELLS_X + 1)
		val y = vertex / (MAZE_NUMCELLS_X + 1)
		return x == 0 || x == MAZE_NUMCELLS_X || y == 0 || y == MAZE_NUMCELLS_Y
	}

	// -----------------------------------------------------------------------------------------------
	// return 4 dim array of ints in range 0-3, each elem occurs only once
	private fun directionHeuristic(x1: Int, y1: Int, x2: Int, y2: Int): IntArray {
		// resulting list
		val d = IntArray(4)

		// Use the transform array (normally used to render) to keep our weights
		// (trying to save mem)
		for (i in 0..3) {
			d[i] = i
			transform[i] = 0.5f
		}
		if (y1 < y2) // tend to move north
			transform[0] += Utils.randFloat(0.4f) - 0.1f else if (y1 > y2) // tend to move south
			transform[2] += Utils.randFloat(0.4f) - 0.1f
		if (x1 < x2) // tend to move west
			transform[3] += Utils.randFloat(0.4f) - 0.1f else if (x1 > x2) // tend to move east
			transform[1] += Utils.randFloat(0.4f) - 0.1f

		// Now bubble sort the list (descending) using our weights to determine order.
		// Elems that have the same weight will be determined by a coin flip

		// temporaries
		var t_f: Float
		var t_i: Int

		// bubblesort
		for (i in 0..2) {
			for (j in i..3) {
				if (transform[i] < transform[j] || transform[i] == transform[j] && Utils.flipCoin()) {
					// swap elems in BOTH arrays
					t_f = transform[i]
					transform[i] = transform[j]
					transform[j] = t_f
					t_i = d[i]
					d[i] = d[j]
					d[j] = t_i
				}
			}
		}
		return d
	}

	// -----------------------------------------------------------------------------------------------
	// return true when x,y is within screen bounds
	private fun isOnScreen(x: Int, y: Int): Boolean {
		if (game_type == GAME_TYPE_CLASSIC) return true
		return if (x < screen_x || y < screen_y || x > screen_x + screen_width || y > screen_y + screen_height) false else true
	}

	// -----------------------------------------------------------------------------------------------
	private fun updatePlayerVisibility_r(verts: IntArray, d: Int) {
		for (i in 0..3) {
			verts[i] += cell_dv[d]
			if (verts[i] < 0 || verts[i] >= MAZE_NUM_VERTS) return
		}
		for (i in 0..3) {
			val ii = (i + 1) % 4
			val info = wall_lookup[verts[i]][verts[ii]]
			info.visible = true
		}
		val dd = (d + 1) % 4
		val info = wall_lookup[verts[d]][verts[dd]]
		if (canSeeThroughWall(info)) updatePlayerVisibility_r(verts, d)
	}

	// -----------------------------------------------------------------------------------------------
	private fun updatePlayerVisibility(player: Player) {
		val verts = IntArray(5)
		computePrimaryQuadrant(player.x, player.y, verts)
		if (Arrays.equals(player.primary_verts, verts)) return  // player verts hasnet changed, no update needed

		//Utils.copyElems(player.primary_verts, verts);
		System.arraycopy(verts, 0, player.primary_verts, 0, player.primary_verts.size)
		for (i in 0..3) {
			val ii = (i + 1) % 4
			val info = wall_lookup[verts[i]][verts[ii]]
			info.visible = true
			if (canSeeThroughWall(info)) {
				// recursize search in this direction
				updatePlayerVisibility_r(verts, i)
				//Utils.copyElems(verts, player.primary_verts);
				System.arraycopy(player.primary_verts, 0, verts, 0, player.primary_verts.size)
			}
		}
	}

	val cell_dv = intArrayOf(-(MAZE_NUMCELLS_X + 1), 1, MAZE_NUMCELLS_X + 1, -1)

	// -----------------------------------------------------------------------------------------------
	private fun canSeeThroughWall(info: Wall): Boolean {
		if (info.type != WALL_TYPE_NONE && info.type != WALL_TYPE_ELECTRIC) {
			if (info.type != WALL_TYPE_DOOR || info.state == DOOR_STATE_CLOSED || info.state == DOOR_STATE_LOCKED) {
				return false
			}
		}
		return true
	}

	// -----------------------------------------------------------------------------------------------
	// return true if there are no walls between sx, sy
	private fun canSee(sx: Int, sy: Int, ex: Int, ey: Int): Boolean {
		var sx = sx
		var sy = sy
		val sv = IntArray(5)
		val ev = IntArray(5)
		computePrimaryQuadrant(sx, sy, sv)
		computePrimaryQuadrant(ex, ey, ev)
		// compute
		val max = 90
		while (true) {
			// if sv == ev, then we are done
			if (Arrays.equals(sv, ev)) return true
			val dx = ex - sx
			val dy = ey - sy
			var d: Int

			//d = (dx*dx) + (dy*dy);

			//if (d < 100*100)
			//  return true;
			if (Math.abs(dx) < max && Math.abs(dy) < max) return true
			d = getDirection(dx, dy)
			val dd = (d + 1) % 4
			val info = wall_lookup[sv[d]][sv[dd]]

			// allow see through electric walls and open(ing)/closing doors
			if (!canSeeThroughWall(info)) return false
			var new_sx = 0
			var new_sy = 0
			for (i in 0..3) {
				sv[i] += cell_dv[d]
				if (sv[i] < 0 || sv[i] >= MAZE_NUM_VERTS) return false
				new_sx += maze_verts_x[sv[i]]
				new_sy += maze_verts_y[sv[i]]
			}
			sx = new_sx / 4
			sy = new_sy / 4
		}
	}

	private fun computeCell(px: Int, py: Int, cellXY: IntArray) {
		val cell_x = px * MAZE_NUMCELLS_X / (verts_max_x - verts_min_x)
		val cell_y = py * MAZE_NUMCELLS_Y / (verts_max_y - verts_min_y)
		cellXY[0] = cell_x
		cellXY[1] = cell_y
	}

	// -----------------------------------------------------------------------------------------------
	private fun computeBaseQuad(px: Int, py: Int, result: IntArray) {
		val cell_x = px * MAZE_NUMCELLS_X / (verts_max_x - verts_min_x)
		val cell_y = py * MAZE_NUMCELLS_Y / (verts_max_y - verts_min_y)
		result[0] = cell_x + cell_y * (MAZE_NUMCELLS_X + 1)
		result[1] = result[0] + 1
		result[3] = result[1] + MAZE_NUMCELLS_X
		result[2] = result[3] + 1
	}

	// -----------------------------------------------------------------------------------------------
	// Put into result the set of vertices that represent the quadrant the point P is inside
	//
	//        r[0]            r[1]
	//
	//
	//
	//
	//
	//        r[2]            r[3]
	//
	// r[4] is not used.
	// P can be any point inside the quadrant
	private fun computePrimaryQuadrant(px: Int, py: Int, result: IntArray) {
		var px = px
		var py = py
		computePrimaryVerts(px, py, result)
		val x0 = maze_verts_x[result[0]]
		val y0 = maze_verts_y[result[0]]
		val pdx = px - x0
		val pdy = py - y0
		/*
        for (int i=1; i<=4; i++) {
            int v = result[i];
            if (v<0 || v>=this.MAZE_NUM_VERTS)
                continue;
            int x = maze_verts_x[v];
            int y = maze_verts_y[v];
            int dx = x-x0;
            int dy = y-y0;
            int dot = dx*pdx+dy*pdy;
            if (dot<=0)
                continue;
            int nx = -dy;
            int ny = dx;
            int dot2 = nx*pdx+ny*pdy;
            if (dot2 < 0) {
                nx=-nx;
                ny=-ny;
            }
            px = x0 + (dx+nx)*2/3;
            py = y0 + (dy+ny)*2/3;
            break;
        }*/


		// determine what quadrant inside the 'wheel' P is in
		for (i in 1..4) {
			var ii = i + 1
			if (ii > 4) ii = 1
			val v1 = result[i]
			val v2 = result[ii]
			if (v1 < 0 || v2 < 0 || v1 >= MAZE_NUM_VERTS || v2 >= MAZE_NUM_VERTS) continue
			val x1 = maze_verts_x[v1]
			val y1 = maze_verts_y[v1]
			val x2 = maze_verts_x[v2]
			val y2 = maze_verts_y[v2]
			val dx1 = x1 - x0
			val dy1 = y1 - y0
			val dx2 = x2 - x0
			val dy2 = y2 - y0
			val nx1 = -dy1
			val nx2 = -dy2
			val d1 = nx1 * pdx + dx1 * pdy
			val d2 = nx2 * pdx + dx2 * pdy
			if (d1 >= 0 && d2 <= 0) {
				px = x0 + (dx1 + dx2) / 2
				py = y0 + (dy1 + dy2) / 2
				break
			}
		}
		computeBaseQuad(px, py, result)
	}

	// -----------------------------------------------------------------------------------------------
	// Put into result to set of vertices that represent a 'wheel'
	//           r[4]
	//            ^
	//            |
	//            |
	//   r[3]<- -r[0]- ->r[1]
	//            |
	//            |
	//            v
	//           r[2]
	//
	// P can be and coord such that dist(P-r[0]) is less than dist(P-r[1,2,3,4])
	//
	// if any point lies outside the world rect, it is < 0
	private fun computePrimaryVerts(px: Int, py: Int, result: IntArray) {
		assert(result.size == 5)
		computeBaseQuad(px, py, result)

		// these are the four in our region, find the closest
		val nearest = computeClosestPrimaryVertex(px, py, result)
		Arrays.fill(result, -1)

		// now build a tree where the closest is elem[0] and elems[1-3] are the 4
		// possible walls that extend from [0].  Set too -1 for those where wall not available
		result[0] = nearest
		// if nearest is on the left edge
		val x = nearest % (MAZE_NUMCELLS_X + 1)
		if (x > 0) {
			result[3] = nearest - 1
		}
		if (x < MAZE_NUMCELLS_X) {
			result[1] = nearest + 1
		}
		result[0] = nearest
		result[2] = nearest + MAZE_NUMCELLS_X + 1
		if (result[2] >= MAZE_NUM_VERTS) result[2] = -1
		result[4] = nearest - (MAZE_NUMCELLS_X + 1) // no test because will automatically be negative
	}

	// -----------------------------------------------------------------------------------------------
	// put the 4 primary verticies associated with this world point into result
	// this is an optimization to reduce time for collision scans
	// we take advanrage of the cell based maze to only scan for collisions in
	// the emediate vicinity of an object. There can only be 4 points in the
	// vicinity, we call these the 'primary vertices'
	private fun computeClosestPrimaryVertex(px: Int, py: Int, primary: IntArray): Int {
		var nearest = -1
		var bestDist = Int.MAX_VALUE.toLong()
		for (i in 0..3) {
			if (primary[i] < 0 || primary[i] >= MAZE_NUM_VERTS) continue
			val dx = (px - maze_verts_x[primary[i]]).toLong()
			val dy = (py - maze_verts_y[primary[i]]).toLong()
			val d = dx * dx + dy * dy
			if (d < bestDist) {
				nearest = primary[i]
				bestDist = d
			}
		}
		assert(nearest >= 0)
		return nearest
	}

	// -----------------------------------------------------------------------------------------------
	private fun initCollision(v0: Int, v1: Int, info: Wall): Boolean {
		collision_info_v0 = v0
		collision_info_v1 = v1
		collision_info_wallinfo = info
		return true
	}

	val collision_verts = IntArray(5)

	// -----------------------------------------------------------------------------------------------
	// return true when a line intersects any edge in the graph
	private fun collisionScanLine(x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
		var x2: Int
		var y2: Int
		var x3: Int
		var y3: Int
		val v0: Int
		var v1: Int
		computePrimaryVerts(x0, y0, collision_verts)
		//int [] newDelta = new int[2];
		v0 = collision_verts[0]
		assert(v0 >= 0)
		for (i in 1 until collision_verts.size) {
			v1 = collision_verts[i]
			if (v1 < 0 || v1 >= MAZE_NUM_VERTS) continue
			val info = wall_lookup[v0][v1]
			if (!isWallActive(info)) continue
			x2 = maze_verts_x[v0]
			y2 = maze_verts_y[v0]
			x3 = maze_verts_x[v1]
			y3 = maze_verts_y[v1]
			when (info.type) {
				WALL_TYPE_DOOR -> if (collisionDoorLine(info, x0, y0, x1, y1, x2, y2, x3, y3)) return initCollision(v0, v1, info)
				else           -> if (Utils.isLineSegsIntersecting(x0, y0, x1, y1, x2, y2, x3, y3)) {
					return initCollision(v0, v1, info)
				}
			}
		}
		return false
	}

	private fun collisionBreakingWallLine(info: Wall, lx0: Int, ly0: Int, lx1: Int, ly1: Int, wx0: Int, wy0: Int, wx1: Int, wy1: Int, newDelta: IntArray): Boolean {
		val health = 1.0f / WALL_NORMAL_HEALTH * info.health
		// if wall is healthy, num==0
		// if wall is med, num 1
		// if wall is about to break, num = 2
		val num = Math.round((1.0f - health) * WALL_BREAKING_MAX_RECURSION)
		// make these values consistent based on garbage input
		//int xoff = (info.v0 * (-1 * info.v1 % 2) % 30);
		//int yoff = (info.v1 * (-1 * info.v0 % 2) % 30);
		return collisionBreakingWallLine_r(lx0, ly0, lx1, ly1, wx0, wy0, wx1, wy1, num, info.v0 + info.v1, newDelta)
	}

	// -----------------------------------------------------------------------------------------------
	private fun collisionDoorLine(door: Wall, lx0: Int, ly0: Int, lx1: Int, ly1: Int, wx0: Int, wy0: Int, wx1: Int, wy1: Int): Boolean {
		val framesElapsed = frameNumber - door.frame
		var dx = wx1 - wx0
		var dy = wy1 - wy0
		when (door.state) {
			DOOR_STATE_LOCKED, DOOR_STATE_CLOSED -> return Utils.isLineSegsIntersecting(lx0, ly0, lx1, ly1, wx0, wy0, wx1, wy1)
			DOOR_STATE_OPEN                      -> {
				dx /= 4
				dy /= 4
			}
			else                                 -> {
				val delta = getDoorDelta(dx, dy, framesElapsed, door.state)
				dx = delta[0]
				dy = delta[1]
			}
		}
		return Utils.isLineSegsIntersecting(lx0, ly0, lx1, ly1, wx0, wy0, wx0 + dx, wy0 + dy) ||
			Utils.isLineSegsIntersecting(lx0, ly0, lx1, ly1, wx1, wy1, wx1 - dy, wy1 - dy)
	}

	// -----------------------------------------------------------------------------------------------
	private fun collisionDoorCircle(door: Wall, x0: Int, y0: Int, x1: Int, y1: Int, px: Int, py: Int, radius: Int): Boolean {
		val framesElapsed = frameNumber - door.frame
		var dx = x1 - x0
		var dy = y1 - y0
		when (door.state) {
			DOOR_STATE_LOCKED, DOOR_STATE_CLOSED -> return Utils.isCircleIntersectingLineSeg(x0, y0, x1, y1, px, py, radius)
			DOOR_STATE_OPEN                      -> {
				dx /= 4
				dy /= 4
			}
			else                                 -> {
				val delta = getDoorDelta(dx, dy, framesElapsed, door.state)
				dx = delta[0]
				dy = delta[1]
			}
		}
		return Utils.isCircleIntersectingLineSeg(x0, y0, x0 + dx, y0 + dy, px, py, radius) ||
			Utils.isCircleIntersectingLineSeg(x1, y1, x1 - dx, y1 - dy, px, py, radius)
	}

	// -----------------------------------------------------------------------------------------------
	// return true when a sphere with radius at px,py is colliding with a wall
	private fun collisionScanCircle(px: Int, py: Int, radius: Int): Boolean {

		// compute the 4 vertices associated with this vertex
		computePrimaryVerts(px, py, collision_verts)
		val v0 = collision_verts[0]
		assert(v0 >= 0)
		for (i in 1 until collision_verts.size) {
			val v1 = collision_verts[i]
//			if (v1 < 0 || v1 >= MAZE_NUM_VERTS) continue
			val info = getWall(v0, v1)//wall_lookup[v0][v1]
			if (!isWallActive(info)) continue
			val x0 = maze_verts_x[v0]
			val y0 = maze_verts_y[v0]
			val x1 = maze_verts_x[v1]
			val y1 = maze_verts_y[v1]
			if (info.type == WALL_TYPE_DOOR) {
				if (collisionDoorCircle(info, x0, y0, x1, y1, px, py, radius)) return initCollision(v0, v1, info)
			} else if (Utils.isCircleIntersectingLineSeg(x0, y0, x1, y1, px, py, radius)) {
				return initCollision(v0, v1, info)
			}
		}
		return false
	}

	// -----------------------------------------------------------------------------------------------
	// put vector V bounced off wall W into result
	private fun bounceVectorOffWall(vx: Int, vy: Int, wx: Int, wy: Int, result: IntArray) {
		// look for best case
		if (wx == 0) {
			result[0] = -vx
			result[1] = vy
			return
		} else if (wy == 0) {
			result[0] = vx
			result[1] = -vy
			return
		}

		// do the bounce off algorithm (Thanx AL)
		// get normal to the wall
		var nx = (-wy).toFloat()
		var ny = wx.toFloat()

		// compute N dot V
		var ndotv = nx * vx.toFloat() + ny * vy.toFloat()

		// make sure the normal is facing toward missle by comparing dot
		// products
		if (ndotv > 0.0f) {
			// reverse direction of N
			nx = -nx
			ny = -ny
		}
		ndotv = Math.abs(ndotv)

		// compute N dot N
		val ndotn = nx * nx + ny * ny

		// compute projection vector
		val px = Math.round(nx * ndotv / ndotn)
		val py = Math.round(ny * ndotv / ndotn)

		// assign new values to missle motion
		result[0] = vx + 2 * px
		result[1] = vy + 2 * py
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
	//abstract fun initImages(g: AGraphics)

	/*
	{
		if (imageKey >= 0) return
		imageKey = g.loadImage("key.gif", GColor.BLACK)
		imageLogo = g.loadImage("logo.gif", GColor.BLACK)
		animJaws = g.loadImageCells("jaws.gif", 32, 32, 8, 9, true, GColor.BLACK)
		animLava = g.loadImageCells("lavapit.gif", 32, 32, 8, 25, true, GColor.BLACK)
		animPeople = arrayOf(
			g.loadImageCells("people.gif", 32, 32, 4, 16, true, GColor.BLACK),
			g.loadImageCells("people2.gif", 32, 32, 4, 16, true, GColor.BLACK),
			g.loadImageCells("people3.gif", 32, 32, 4, 16, true, GColor.BLACK)
		)
	}

	private fun initImages_png(g: AGraphics) {
		imageKey = g.loadImage("pngs/key.png", GColor.BLACK)
		imageLogo = g.loadImage("pngs/logo.png", GColor.BLACK)
		animJaws = g.loadImageCells("pngs/jaws.png", 32, 32, 8, 9, true, GColor.BLACK)
		animLava = g.loadImageCells("pngs/lavapit.png", 32, 32, 8, 25, true, GColor.BLACK)
		animPeople = arrayOf(
			g.loadImageCells("pngs/people.png", 32, 32, 4, 16, true, GColor.BLACK),
			g.loadImageCells("pngs/people2.png", 32, 32, 4, 16, true, GColor.BLACK),
			g.loadImageCells("pngs/people3.png", 32, 32, 4, 16, true, GColor.BLACK)
		)
	}*/

	// -----------------------------------------------------------------------------------------------
	private fun drawIntroPlayers(g: AGraphics, frame: Int) {

		// draw the player modes
		val x1 = screen_width / 3
		val x2 = screen_width / 2
		val x3 = screen_width * 3 / 4
		var sy = 50
		val dy = (screen_height - sy) / 5
		val textColor = GColor.RED
		val dir = DIR_DOWN
		sy += PLAYER_RADIUS / 2
		val player = players[0]
		drawPlayer(player, g, x1, sy, dir)
		sy += dy
		player.powerup = POWERUP_HULK
		player.scale = PLAYER_HULK_SCALE
		drawPlayer(player, g, x1, sy, dir)
		player.scale = 1f
		g.color = textColor
		g.drawString("HULK", x2.toFloat(), sy.toFloat())
		g.drawString("Charge Enemies", x3.toFloat(), sy.toFloat())
		sy += dy
		player.powerup = POWERUP_GHOST
		drawPlayer(player, g, x1, sy, dir)
		g.color = textColor
		g.drawString("GHOST", x2.toFloat(), sy.toFloat())
		g.drawString("Walk through walls", x3.toFloat(), sy.toFloat())
		sy += dy
		player.powerup = POWERUP_BARRIER
		drawPlayer(player, g, x1, sy, dir)
		g.color = textColor
		g.drawString("BARRIER", x2.toFloat(), sy.toFloat())
		g.drawString("Protects against missles", x3.toFloat(), sy.toFloat())
		sy += dy
		player.powerup = -1 // must be last!
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawIntroWallTypes(g: AGraphics, frame: Int) {

		// draw the player modes
		val x1 = screen_width / 3
		val x2 = screen_width / 2
		var sy = 50
		val dy = (screen_height - sy) / WALL_NUM_TYPES
		val textColor = GColor.RED
		val info = Wall()
		info.health = 100
		info.frequency = RUBBER_WALL_MAX_FREQUENCY
		val wallSize = dy / 2 - 3
		sy += wallSize
		for (i in WALL_TYPE_NORMAL until WALL_NUM_TYPES) {
			info.type = i
			drawWall(g, info, x1 - wallSize, sy - wallSize, x1 + wallSize, sy + wallSize)
			g.color = textColor
			g.drawString(getWallTypeString(i), x2.toFloat(), sy.toFloat())
			sy += dy
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawIntroPowerups(g: AGraphics, frame: Int) {

		// draw the player modes
		val x1 = screen_width / 3
		val x2 = screen_width / 2
		var sy = 50
		val dy = (screen_height - sy) / (POWERUP_NUM_TYPES + 1)
		val textColor = GColor.WHITE
		sy += 16
		//this.drawStickFigure(g, x1, sy, this.PEOPLE_RADIUS);
		//this.drawPeople(g)
		val type = frameNumber / 30 % PEOPLE_NUM_TYPES
		val dir = frameNumber / 60 % 4
		g.color = GColor.WHITE
		this.drawPerson(g, x1, sy, 32, type, dir)
		g.color = textColor
		g.drawString("HUMAN", x2.toFloat(), sy.toFloat())
		sy += dy
		for (i in 0 until POWERUP_NUM_TYPES) {
			drawPowerup(g, i, x1, sy)
			g.color = textColor
			g.drawString(getPowerupTypeString(i), x2.toFloat(), sy.toFloat())
			sy += dy
		}
	}

	// -----------------------------------------------------------------------------------------------
	private fun drawIntroEnemies(g: AGraphics, frame: Int) {

		// draw the player modes
		val x1 = screen_width / 3
		val x2 = screen_width / 2
		val x3 = screen_width * 3 / 4
		var sy = 50
		val textColor = GColor.RED
		val dy = (screen_height - sy) / 6
		drawRobot(g, x1, sy, DIR_DOWN)
		g.color = textColor
		g.drawString("ROBOT", x2.toFloat(), sy.toFloat())
		g.drawString(ENEMY_ROBOT_POINTS.toString(), x3.toFloat(), sy.toFloat())
		sy += dy
		drawBrain(g, x1, sy, 10)
		g.color = textColor
		g.drawString("BRAIN", x2.toFloat(), sy.toFloat())
		g.drawString(ENEMY_BRAIN_POINTS.toString(), x3.toFloat(), sy.toFloat())
		sy += dy
		drawGenerator(g, x1, sy)
		g.color = textColor
		g.drawString("GENERATOR", x2.toFloat(), sy.toFloat())
		g.drawString(ENEMY_GEN_POINTS.toString(), x3.toFloat(), sy.toFloat())
		sy += dy
		drawTank(g, x1, sy, DIR_DOWN)
		g.color = textColor
		g.drawString("TANK", x2.toFloat(), sy.toFloat())
		g.drawString(ENEMY_TANK_POINTS.toString(), x3.toFloat(), sy.toFloat())
		sy += dy
		drawThug(g, x1, sy, DIR_DOWN)
		g.color = textColor
		g.drawString("THUG", x2.toFloat(), sy.toFloat())
		g.drawString("INDESTRUCTABLE", x3.toFloat(), sy.toFloat())
		sy += dy
		drawEnd(g, x1, sy)
		g.color = textColor
		g.drawString("NEXT LEVEL PORTAL", x2.toFloat(), sy.toFloat())
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
		val frame = frameNumber % (introSpacingFrames * numIntros)
		if (frame < introSpacingFrames) {
			drawIntroPlayers(g, 0)
		} else if (frame < 2 * introSpacingFrames) {
			drawIntroWallTypes(g, frame - introSpacingFrames)
		} else if (frame < 3 * introSpacingFrames) {
			drawIntroPowerups(g, frame - 2 * introSpacingFrames)
		} else if (frame < 4 * introSpacingFrames) {
			drawIntroEnemies(g, frame - 3 * introSpacingFrames)
		}

		// draw buttons for selecting the gametype
		var y = 0
		for (i in 0 until BUTTONS_NUM) {
			if (button_active == i) {
				g.color = GColor.RED
				g.drawFilledRect(button_x[i].toFloat(), button_y[i].toFloat(), BUTTON_WIDTH.toFloat(), BUTTON_HEIGHT.toFloat())
			}
			var outline = false
			when (Button.values()[i]) {
				Button.Classic   -> if (game_type == GAME_TYPE_CLASSIC) {
					outline = true
				}
				Button.RoboCraze -> if (game_type == GAME_TYPE_ROBOCRAZE) {
					outline = true
				}
				Button.Easy      -> if (difficulty == DIFFICULTY_EASY) {
					outline = true
				}
				Button.Medium    -> if (difficulty == DIFFICULTY_MEDIUM) {
					outline = true
				}
				Button.Hard      -> if (difficulty == DIFFICULTY_HARD) {
					outline = true
				}
				Button.START     -> {
				}
			}
			if (outline) {
				g.color = GColor.GREEN
				g.drawRect((button_x[i] + 1).toFloat(), (button_y[i] + 1).toFloat(), (BUTTON_WIDTH - 2).toFloat(), (BUTTON_HEIGHT - 2).toFloat())
				g.drawRect((button_x[i] + 2).toFloat(), (button_y[i] + 2).toFloat(), (BUTTON_WIDTH - 4).toFloat(), (BUTTON_HEIGHT - 4).toFloat())
			}
			g.color = GColor.CYAN
			g.drawRect(button_x[i].toFloat(), button_y[i].toFloat(), BUTTON_WIDTH.toFloat(), BUTTON_HEIGHT.toFloat())
			g.drawString(Button.values()[i].name, (button_x[i] + 5).toFloat(), (button_y[i] + 5).toFloat())
			y = button_y[i]
		}
		y += BUTTON_HEIGHT + 20
		val x = button_x[0]
		g.color = GColor.CYAN
		g.drawString(instructions, x.toFloat(), y.toFloat())
	}

	private fun drawIntro_old(g: AGraphics) {
		var left = 10
		var top = 20
		val v_spacing = 20
		val h_spacing = screen_width / 3
		val textColor = GColor.BLUE

		// Game name ect
		g.color = GColor.RED
		g.drawString("RoboCraze 2.0", left.toFloat(), top.toFloat())
		top += v_spacing
		g.color = textColor
		g.drawString("ccaron 1/2008", left.toFloat(), top.toFloat())
		top += v_spacing * 2
		// draw the player
		val frameNum = (frameNumber % 200).toFloat()
		val player = players[0]
		if (frameNum < 100) {
			g.drawString("Player", left.toFloat(), top.toFloat())
			player.scale = 1.0f + frameNum / 100.0f
		} else {
			g.drawString("Hulk", left.toFloat(), top.toFloat())
			player.scale = PLAYER_HULK_SCALE - (frameNum - 100) / 100.0f
		}
		drawPlayer(player, g, left + h_spacing, top - PLAYER_RADIUS, 2)
		top += v_spacing + PLAYER_RADIUS * 2
		// draw a people
		g.color = textColor
		g.drawString("Person ($PEOPLE_START_POINTS - $PEOPLE_MAX_POINTS points)", left.toFloat(), top.toFloat())
		g.color = GColor.RED
		drawStickFigure(g, left + h_spacing, top - PEOPLE_RADIUS, PEOPLE_RADIUS)
		top += v_spacing + PEOPLE_RADIUS * 2
		// draw a generator
		drawGenerator(g, left + h_spacing, top - ENEMY_GEN_RADIUS)
		g.color = textColor
		g.drawString("Generator ($ENEMY_GEN_POINTS points)", left.toFloat(), top.toFloat())
		top += v_spacing + ENEMY_GEN_RADIUS
		// draw an enemy guy
		g.color = textColor
		g.drawString("Enemy Guy ($ENEMY_ROBOT_POINTS points)", left.toFloat(), top.toFloat())
		g.color = GColor.LIGHT_GRAY
		drawRobot(g, left + h_spacing, top - ENEMY_ROBOT_RADIUS, 2)
		top += v_spacing + ENEMY_ROBOT_RADIUS * 2
		// draw a thug
		drawThug(g, left + h_spacing, top - ENEMY_THUG_RADIUS, 1)
		g.color = textColor
		g.drawString("Enemy Thug (Indestructable)", left.toFloat(), top.toFloat())
		top += v_spacing + ENEMY_THUG_RADIUS * 2
		// draw a brain
		g.color = textColor
		g.drawString("Enemy Brain ($ENEMY_BRAIN_POINTS points)", left.toFloat(), top.toFloat())
		drawBrain(g, left + h_spacing, top - ENEMY_BRAIN_RADIUS, ENEMY_BRAIN_RADIUS)
		top += v_spacing + ENEMY_BRAIN_RADIUS * 2
		// draw a tank
		g.color = textColor
		g.drawString("Enemy Tank ($ENEMY_TANK_POINTS points)", left.toFloat(), top.toFloat())
		g.color = GColor.DARK_GRAY
		drawTank(g, left + h_spacing, top - ENEMY_TANK_RADIUS, 1)
		top += v_spacing + ENEMY_TANK_RADIUS * 2
		// draw a tank gen
		g.color = textColor
		g.drawString("Tank Generator ($ENEMY_TANK_GEN_POINTS points)", left.toFloat(), top.toFloat())
		g.color = GColor.GREEN
		drawTankGen(g, left + h_spacing, top - ENEMY_TANK_GEN_RADIUS)
		top += v_spacing + ENEMY_TANK_GEN_RADIUS * 2
		// draw the ending X
		g.color = GColor.RED
		drawEnd(g, left + h_spacing, top)
		g.color = textColor
		g.drawString("GOAL", left.toFloat(), (top - 4).toFloat())
		top += v_spacing * 2

		// draw some instructions on how to play
		left = screen_width / 2
		top = 20
		g.color = GColor.LIGHT_GRAY
		g.drawString("Use [AWDS] to move", left.toFloat(), top.toFloat())
		top += v_spacing * 2
		g.drawString("Use Mouse to aim and fire missles", left.toFloat(), top.toFloat())
		top += v_spacing * 2
		g.drawString("Extra man every $PLAYER_NEW_LIVE_SCORE points", left.toFloat(), top.toFloat())

		// draw throbbing white text indicating to the player how to start
		top += v_spacing * 2
		g.color = throbbing_white
		g.drawString("Click on game type to play", left.toFloat(), top.toFloat())

		// draw buttons for selecting the gametype
		for (i in 0 until BUTTONS_NUM) {
			if (button_active == i) {
				g.color = GColor.RED
				g.drawFilledRect(button_x[i].toFloat(), button_y[i].toFloat(), BUTTON_WIDTH.toFloat(), BUTTON_HEIGHT.toFloat())
			}
			if (difficulty + 2 == i) {
				g.color = GColor.GREEN
				g.drawRect((button_x[i] + 1).toFloat(), (button_y[i] + 1).toFloat(), (BUTTON_WIDTH - 2).toFloat(), (BUTTON_HEIGHT - 2).toFloat())
				g.drawRect((button_x[i] + 2).toFloat(), (button_y[i] + 2).toFloat(), (BUTTON_WIDTH - 4).toFloat(), (BUTTON_HEIGHT - 4).toFloat())
			}
			g.color = GColor.CYAN
			g.drawRect(button_x[i].toFloat(), button_y[i].toFloat(), BUTTON_WIDTH.toFloat(), BUTTON_HEIGHT.toFloat())
			g.drawString(Button.values()[i].name, (button_x[i] + 5).toFloat(), (button_y[i] + BUTTON_HEIGHT - 5).toFloat())
		}
	}

	// -----------------------------------------------------------------------------------------------
	fun drawGame(g: AGraphics) {
		g.ortho(0f, screen_width.toFloat(), 0f, screen_height.toFloat())
		frameNumber += 1
		g.clearScreen(GColor.BLACK)
		when (game_state) {
			GAME_STATE_INTRO     -> drawIntro(g)
			GAME_STATE_PLAY      -> {
				updatePlayers()
				updatePeople()
				updateMissles()
				updateEnemies()
				updatePowerups()
				updateAndDrawZombieTracers(g)
				var i = 0
				while (i < num_players) {
					updateAndDrawPlayerTracers(players[i], g)
					i++
				}
				drawMaze(g)
				updateAndDrawMessages(g)
				updateAndDrawParticles(g)
				drawEnemies(g)
				drawPeople(g)
				drawPowerups(g)
				i = 0
				while (i < num_players) {
					val player = players[i]
					when (player.state) {
						PLAYER_STATE_SPAWNING                       -> {
							drawPlayerHighlight(players[0], g)
							drawPlayer(player, g, player.x - screen_x, player.y - screen_y, player.dir)
						}
						PLAYER_STATE_ALIVE, PLAYER_STATE_TELEPORTED -> drawPlayer(player, g, player.x - screen_x, player.y - screen_y, player.dir)
						PLAYER_STATE_EXPLODING                      -> drawPlayerExploding(player, g)
						PLAYER_STATE_SPECTATOR                      -> {
						}
					}
					i++
				}
				drawMissles(g)
				if (game_type != GAME_TYPE_CLASSIC) drawEnd(g, end_x - screen_x, end_y - screen_y)
				drawPlayerInfo(players[0], g)
				if (Utils.isDebugEnabled()) {
					drawDebug(g)
					drawDebugButtons(g)
				}
			}
			GAME_STATE_GAME_OVER -> {
				drawMaze(g)
				drawEnemies(g)
				drawPeople(g)
				drawMissles(g)
				if (game_type != GAME_TYPE_CLASSIC) drawEnd(g, end_x - screen_x, end_y - screen_y)
				drawPlayerInfo(players[0], g)
				drawGameOver(g)
			}
		}
		updateThrobbingWhite()
		//updateFPS(g);
	}

	/*
    private int frameCount = 0;
    private long frameStartTime = 0;
    private int FPS = 0;

    private void updateFPS(AGraphics g) {
        long t = getClock();
        if (frameStartTime == 0) {
            frameStartTime = t;
            frameCount = 1;
        } else {
            long dt = t - frameStartTime;
            if (dt >= 1000) {
                FPS = frameCount;
                frameStartTime = t;
                frameCount = 0;
            }
            frameCount++;
        }
        g.setColor(GColor.WHITE);
        float x = screen_width - 30;
        int y = 10;
        float w = g.drawJustifiedString(x, y, Justify.RIGHT, "FPS:");
        this.drawNumberString(g, Math.round(x+w), y, Justify.RIGHT, FPS);
    }*/
	// -----------------------------------------------------------------------------------------------
	private fun isInMaze(x: Int, y: Int): Boolean {
		val padding = 10
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
		return x > minx && x < maxx && y > miny && y < maxy
	}

	// -----------------------------------------------------------------------------------------------
	// This is placeed close too keyTyped so we can update when necc
	private fun drawDebugButtons(g: AGraphics) {
		val w = screen_width / Debug.values().size
		var x = w / 2
		val y = screen_height - 20
		for (d in Debug.values()) {
			if (isDebugEnabled(d)) g.color = GColor.RED else g.color = GColor.CYAN
			g.drawJustifiedString(x.toFloat(), y.toFloat(), Justify.CENTER, d.indicator)
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
		val player = player
		if (dx < 0) {
			player.dx = -PLAYER_SPEED
		} else if (dx > 0) {
			player.dx = PLAYER_SPEED
		} else {
			player.dx = 0
		}
		if (dy < 0) {
			player.dy = -PLAYER_SPEED
		} else if (dy > 0) {
			player.dy = PLAYER_SPEED
		} else {
			player.dy = 0
		}
	}

	val isPlayerSpectator: Boolean
		get() = player.state == PLAYER_STATE_SPECTATOR

	// -----------------------------------------------------------------------------------------------
	private fun setIntroButtonPositionAndDimension() {
		val x = 8
		var y = 50
		for (i in 0 until BUTTONS_NUM) {
			button_x[i] = x
			button_y[i] = y
			y += BUTTON_HEIGHT + 5
		}
	}

	var baseTime = System.nanoTime()
	abstract val clock: Long
	private var downTime: Long = 0
	fun setCursorPressed(pressed: Boolean) {
		if (pressed) {
			when (game_state) {
				GAME_STATE_INTRO                      -> downTime = clock
				GAME_STATE_PLAY, GAME_STATE_GAME_OVER -> players[0].firing = true
			}
		} else {
			when (game_state) {
				GAME_STATE_INTRO                      -> {
					if (downTime > 0 && clock - downTime < 1000) {
						newGame()
					}
					downTime = 0
				}
				GAME_STATE_PLAY, GAME_STATE_GAME_OVER -> players[0].firing = false
			}
		}
	}

	fun setPlayerMissleVector(dx: Int, dy: Int) {
		player.target_dx = dx
		player.target_dy = dy
	}

	fun setPlayerFiring(firing: Boolean) {
		player.firing = firing
	}

	private fun newPlayerGame(player: Player) {
		player.lives = PLAYER_START_LIVES
		player.score = 0
	}

	private fun newGame() {
		if (button_active >= 0 && button_active < BUTTONS_NUM) {
			when (Button.values()[button_active]) {
				Button.Classic   -> game_type = GAME_TYPE_CLASSIC
				Button.RoboCraze -> game_type = GAME_TYPE_ROBOCRAZE
				Button.Easy      -> difficulty = DIFFICULTY_EASY
				Button.Medium    -> difficulty = DIFFICULTY_MEDIUM
				Button.Hard      -> difficulty = DIFFICULTY_HARD
				Button.START     -> {
					button_active = -1
					gameLevel = 1
					game_state = GAME_STATE_PLAY
					var i = 0
					while (i < num_players) {
						newPlayerGame(players[i])
						i++
					}
					if (Utils.isDebugEnabled()) Utils.setRandomSeed(0) else Utils.setRandomSeed(clock)
					buildAndPopulateLevel()
				}
			}
		}
	}

	fun setCursor(x: Int, y: Int) {
		cursor_x = x
		cursor_y = y
		when (game_state) {
			GAME_STATE_INTRO -> {
				button_active = -1
				var i = 0
				while (i < BUTTONS_NUM) {
					if (x > button_x[i] && x < button_x[i] + BUTTON_WIDTH && y > button_y[i] && y < button_y[i] + BUTTON_HEIGHT) {
						button_active = i
					}
					i++
				}
			}

			GAME_STATE_PLAY, GAME_STATE_GAME_OVER -> {
				players[0].target_dx = (screen_x + x).toFloat().roundToInt() - player.x
				players[0].target_dy = (screen_y + y).toFloat().roundToInt() - player.y
			}
		}
	}

	fun setGameStateIntro() {
		game_state = GAME_STATE_INTRO
	}

	init {
		initTables()
		buildAndPopulateLevel()
	}
}