package cc.game.superrobotron

import cc.lib.game.GColor
import cc.lib.math.CMath

/**
 * Created by Chris Caron on 3/19/25.
 */
// ---------------------------------------------------------//
// CONSTANTS //
// ---------------------------------------------------------//

const val DIR_UP = 0
const val DIR_RIGHT = 1
const val DIR_DOWN = 2
const val DIR_LEFT = 3

const val MAX_PLAYERS = 4

const val PLAYER_STATE_SPAWNING = 0
const val PLAYER_STATE_ALIVE = 1
const val PLAYER_STATE_EXPLODING = 2
const val PLAYER_STATE_TELEPORTED = 3
const val PLAYER_STATE_SPECTATOR = 4

data class ColorSet(val normal: GColor, val hulk: GColor, val gun: GColor)

val PLAYER_COLORS = arrayOf(
	ColorSet(GColor.WHITE, GColor.GREEN, GColor.BLUE),
	ColorSet(GColor.PINK, GColor.RED, GColor.CYAN),
	ColorSet(GColor.ORANGE, GColor.YELLOW, GColor.SLIME_GREEN),
	ColorSet(GColor.DARK_OLIVE, GColor.SKY_BLUE, GColor.RED)
).also {
	if (it.size != MAX_PLAYERS) {
		error("colors array not of size $MAX_PLAYERS")
	}
}


// enum
const val SNAKE_STATE_CHASE = 0
const val SNAKE_STATE_CLIMB_WALL = 1 // slowly climb a wall
const val SNAKE_STATE_CREST_WALL = 2 // crest the wall, start to
const val SNAKE_STATE_DESCEND_WALL = 3
const val SNAKE_STATE_ATTACHED = 4
const val SNAKE_STATE_DYING = 5
const val SNAKE_MAX_SECTIONS = 10 // max number of section

// game types
const val GAME_TYPE_CLASSIC = 0 // No maze
const val GAME_TYPE_ROBOCRAZE = 1
const val WORLD_WIDTH_CLASSIC = 1000f
const val WORLD_HEIGHT_CLASSIC = 1000f

//  final int GAME_TYPE_ROBOCRAZE = 1; // Use Maze, level over when
// player gets to exit (X)
// possible game states
const val GAME_STATE_INTRO = 0
const val GAME_STATE_PLAY = 1
const val GAME_STATE_NEXT_LEVEL = 2
const val GAME_STATE_GAME_OVER = 4


const val MAZE_CELL_DIM = 160f
const val MAZE_NUMCELLS_X = 10
const val MAZE_NUMCELLS_Y = 10
const val MAZE_NUM_VERTS = (MAZE_NUMCELLS_X + 1) * (MAZE_NUMCELLS_Y + 1)
const val MAZE_VERTEX_NOISE = 20f //30 // random noise in placement of
const val MAZE_MARGIN = 25f


//private final int     MAZE_CELL_X = 14;
//private final int     MAZE_CELL_Y = 14;
// vertices
const val MAZE_WALL_THICKNESS = 3f
const val MAZE_WIDTH = MAZE_CELL_DIM * MAZE_NUMCELLS_X //1440; // width of world
const val MAZE_HEIGHT = MAZE_CELL_DIM * MAZE_NUMCELLS_Y //1240; // height of world

// used to make comparisons close to zero
const val EPSILON = 0.00001f
const val PLAYER_SPEED = 8 // pixels per frame
const val PLAYER_SUPER_SPEED_BONUS = 3
const val PLAYER_RADIUS = 16f
const val PLAYER_RADIUS_BARRIER = 25f
const val MAX_PLAYER_MISSLES = 16 // max player missiles on screen at one time
const val PLAYER_DEATH_FRAMES = 70 // number of frames to do the 'death' sequence
const val PLAYER_START_LIVES = 3 // number of lives player starts with
const val PLAYER_NEW_LIVE_SCORE = 25000 // number of points to give an extra player
const val PLAYER_SHOT_FREQ = 4 // number of frames between adjacent
const val PLAYER_SHOT_FREQ_MEGAGUN = 3 // missile shots
const val PLAYER_MISSLE_SPEED = 20 // distance missile travels per frame
const val PLAYER_MISSLE_DURATION = 50 // number of frames a missile exists for
const val PLAYER_SPAWN_FRAMES = 30 // number of frames at beginning of level where player is safe
const val PLAYER_HULK_GROW_SPEED = 0.05f
const val PLAYER_HULK_SCALE = 1.6f
const val PLAYER_HULK_CHARGE_FRAMES = 20
const val PLAYER_HULK_CHARGE_SPEED_BONUS = PLAYER_SPEED
const val PLAYER_POWERUP_DURATION = 500 // frames
const val PLAYER_POWERUP_WARNING_FRAMES = 50 //
const val PLAYER_SUPERSPEED_NUM_TRACERS = 4 // used for POWERUP_SPEED mode
const val MAX_ENEMIES = 64 // max number of enemies on the screen per frame
const val MAX_ENEMY_MISSLES = 16 // max number of enemy missiles on screen per frame

// Snake type projectiles
const val MAX_SNAKE_MISSLES = 8 // max number deployed at one time
const val SNAKE_SECTION_LENGTH = 20 // pixels, should be even multiple of SNAKE_SPEED
const val SNAKE_THICKNESS = 3 // pixels
const val SNAKE_DURATION = 1000 // frames
const val SNAKE_SPEED = 4 // pixels of advancement per frame
const val SNAKE_HEURISTIC_FACTOR = 0.4f

// time
const val MAX_PARTICLES = 32 // maximum explosions on the screen
const val PARTICLE_TYPE_BLOOD = 0
const val PARTICLE_TYPE_DYING_ROBOT = 1
const val PARTICLE_TYPE_DYING_TANK = 2
const val PARTICLE_TYPE_PLAYER_STUN = 3

// if any more particle types are added, then the update function
// will need to get fixed
const val PARTICLES_NUM_TYPES = 4 // MUST BE LAST!
const val PARTICLE_DYING_ROBOT_DURATION = 5
const val PARTICLE_DYING_TANK_DURATION = 9
const val PARTICLE_BLOOD_DURATION = 60
const val PARTICLE_BLOOD_RADIUS = 13f
const val PARTICLE_LINES_DURATION = 8

// enum of enemy types
const val ENEMY_INDEX_GEN = 0 // generator
const val ENEMY_INDEX_ROBOT_N = 1 // always moves toward player
const val ENEMY_INDEX_ROBOT_E = 2
const val ENEMY_INDEX_ROBOT_S = 3
const val ENEMY_INDEX_ROBOT_W = 4
const val ENEMY_INDEX_THUG_N = 5 // indistructable idiot, walks north
const val ENEMY_INDEX_THUG_E = 6 // walks east
const val ENEMY_INDEX_THUG_S = 7 // walks south
const val ENEMY_INDEX_THUG_W = 8 // walks west
const val ENEMY_INDEX_BRAIN = 9 // The evil brain
const val ENEMY_INDEX_ZOMBIE_N = 10 // a brain turns people into zombies
const val ENEMY_INDEX_ZOMBIE_E = 11
const val ENEMY_INDEX_ZOMBIE_S = 12
const val ENEMY_INDEX_ZOMBIE_W = 13
const val ENEMY_INDEX_TANK_NE = 14 // Tanks only move diagonally and
const val ENEMY_INDEX_TANK_SE = 15
const val ENEMY_INDEX_TANK_SW = 16
const val ENEMY_INDEX_TANK_NW = 17
const val ENEMY_INDEX_JAWS = 18
const val ENEMY_INDEX_LAVA = 19
const val ENEMY_INDEX_NUM = 20 // MUST BE LAST
val ENEMY_NAMES = arrayOf(
	"generator",
	"robot", "robot", "robot", "robot",
	"thug", "thug", "thug", "thug",
	"brain",
	"zombie", "zombie", "zombie", "zombie",
	"tank", "tank", "tank", "tank",
	"tankgen", "tankgen", "tankgen", "tankgen",
	"jaws",
	"lavapit"
)
const val ENEMY_SPAWN_SCATTER = 30 // used to scatter thugs and brains around generators
const val ENEMY_ROBOT_RADIUS = 12f
const val ENEMY_ROBOT_SPEED = 7
const val ENEMY_ROBOT_SPEED_INCREASE = 200 // number of player moves to increase robot speed
const val ENEMY_ROBOT_MAX_SPEED = PLAYER_SPEED //12;
const val ENEMY_ROBOT_HEURISTIC_FACTOR = 0.0f // likelihood the robot will choose direction toward player
const val ENEMY_ROBOT_ATTACK_DIST = 300 // max distance a guy will try to shoot from
const val ENEMY_ROBOT_FORCE = 10.0f // stun force from a robot
const val ENEMY_PROJECTILE_FREQ = 2 // used to determine frequency of guy fires
const val ENEMY_PROJECTILE_RADIUS = 4f
const val ENEMY_PROJECTILE_FORCE = 5.0f
const val ENEMY_PROJECTILE_DURATION = 60
const val ENEMY_PROJECTILE_GRAVITY = 0.3f
const val ENEMY_GEN_INITIAL = 15 // starting generators for level 1
const val ENEMY_GEN_SPAWN_MIN = 2 // minimum to spawn when hit
const val ENEMY_GEN_SPAWN_MAX = 4 // maximum to spawn when hit
const val ENEMY_GEN_RADIUS = 10f
const val ENEMY_GEN_PULSE_FACTOR = 3 // gen will pulse +- this
const val ENEMY_TANK_GEN_RADIUS = 15f

// amount from GEN_RADIUS
// NOTE: on HEURISTIC_FACTOR, 0.0 const values make a bot move toward player, 1.0
// makes move totally random
const val ENEMY_THUG_UPDATE_FREQ = 4
const val ENEMY_THUG_SPEED = 6
const val ENEMY_THUG_PUSHBACK = 8.0f // number of units to push the thug on a hit
const val ENEMY_THUG_RADIUS = 17f
const val ENEMY_THUG_HEURISTICE_FACTOR = 0.9f

// ENEMY BRAIN
const val ENEMY_BRAIN_RADIUS = 15f
const val ENEMY_BRAIN_SPEED = 7f
const val ENEMY_BRAIN_ZOMBIFY_FRAMES = 80 // frames taken to zombify a person
const val ENEMY_BRAIN_FIRE_CHANCE = 10 // chance = 1-10-difficulty
const val ENEMY_BRAIN_FIRE_FREQ = 100 // frames
const val ENEMY_BRAIN_UPDATE_SPACING = 10 // frames between updates (with some noise)

// ZOMBIE
const val ENEMY_ZOMBIE_SPEED = 10f
const val ENEMY_ZOMBIE_RADIUS = 10f
const val ENEMY_ZOMBIE_UPDATE_FREQ = 2 // number of frames between updates
const val ENEMY_ZOMBIE_HEURISTIC_FACTOR = 0.2f //
const val ENEMY_ZOMBIE_TRACER_FADE = 3 // higher means slower fade
const val MAX_ZOMBIE_TRACERS = 32

// TANK
const val ENEMY_TANK_RADIUS = 24f
const val ENEMY_TANK_SPEED = 5
const val ENEMY_TANK_UPDATE_FREQ = 4 // frames between movement and update
const val ENEMY_TANK_FIRE_FREQ = 5 // updates between shots fired
const val MAX_TANK_MISSLES = 16

// TANK MISSLE
const val TANK_MISSLE_SPEED = 8
const val TANK_MISSLE_DURATION = 300
const val TANK_MISSLE_RADIUS = 8f

// -- POINTS --
const val ENEMY_GEN_POINTS = 100
const val ENEMY_ROBOT_POINTS = 10
const val ENEMY_BRAIN_POINTS = 50
const val ENEMY_TANK_POINTS = 100
const val ENEMY_TANK_GEN_POINTS = 100
const val POWERUP_POINTS_SCALE = 100
const val POWERUP_BONUS_POINTS_MAX = 50

// -- PEOPLE --
const val MAX_PEOPLE = 32
const val PEOPLE_NUM_TYPES = 3
const val PEOPLE_RADIUS = 10f
const val PEOPLE_SPEED = 4
const val PEOPLE_START_POINTS = 500
const val PEOPLE_INCREASE_POINTS = 100 // * game_level
const val PEOPLE_MAX_POINTS = 10000

// -- MESSGAES --
const val THROBBING_SPEED = 2 // Affects throbbing_white. higher is slower.
const val MESSAGE_FADE = 3 // Affects instaMsgs, higher is slower
const val MAX_MESSAGES = 8 // max number of inst msgs

// -- lava pit --
const val ENEMY_LAVA_CLOSED_FRAMES = 30
const val ENEMY_LAVA_DIM = 64f
const val ENEMY_JAWS_DIM = 32f
const val DIFFICULTY_EASY = 0
const val DIFFICULTY_MEDIUM = 1
const val DIFFICULTY_HARD = 2


val BUTTONS_NUM = Button.values().size
const val BUTTON_WIDTH = 130f
const val BUTTON_HEIGHT = 40f

// for other info on intro screen
const val TEXT_PADDING = 5f // pixels between hud text lines and border
const val SCREEN_MARGIN = 25f

// -- POWERUPS --
// ENUM
const val POWERUP_SUPER_SPEED = 0
const val POWERUP_GHOST = 1
const val POWERUP_BARRIER = 2
const val POWERUP_MEGAGUN = 3
const val POWERUP_HULK = 4
const val POWERUP_BONUS_POINTS = 5
const val POWERUP_BONUS_PLAYER = 6
const val POWERUP_KEY = 7
const val POWERUP_NUM_TYPES = 8 // MUST BE LAST!

// must be NUM_POWERUP_TYPES elems
val POWERUP_CHANCE = intArrayOf(
	2,
	1,
	2,
	2,
	1,
	4,
	1,
	3
)

// The first letter is used as the indicator, unless special
// handling is associated with a powerup
// (as of this writing: POWERUP_BONUS_PLAYER)
val POWERUP_NAMES = arrayOf(
	"SPEEEEEEED",
	"GHOST",
	"BARRIER",
	"MEGAGUN",
	"HULK MODE!",
	"$$",
	"EXTRA MAN!",
	"KEY"
)

fun getPowerupTypeString(type: Int): String {
	return POWERUP_NAMES[type]
}

const val MAX_POWERUPS = 8
const val POWERUP_MAX_DURATION = 500
const val POWERUP_RADIUS = 10f
const val POWERUP_CHANCE_BASE = 1000
const val POWERUP_CHANCE_ODDS = 10 // 10+currentLevel in 1000

// Rubber Walls
const val RUBBER_WALL_MAX_FREQUENCY = 0.15f
const val RUBBER_WALL_FREQUENCY_INCREASE_MISSILE = 0.03f
const val RUBBER_WALL_FREQUENCY_COOLDOWN = 0.002f

// chance
// -- STATIC FIELD --
// TODO: Why cant I increase this number w/out wierd results?
const val STATIC_FIELD_SECTIONS = 8
val STATIC_FIELD_COS_T = CMath.cosine(360.0f / STATIC_FIELD_SECTIONS)
val STATIC_FIELD_SIN_T = CMath.sine(360.0f / STATIC_FIELD_SECTIONS)

// number of repeated shots from megagun required to break wall
const val WALL_NORMAL_HEALTH = 30
const val WALL_BREAKING_MAX_RECURSION = 3

const val DARKEN_AMOUNT = 0.1f
const val LIGHTEN_AMOUNT = 0.1f

const val DOOR_STATE_CLOSED = 0
const val DOOR_STATE_CLOSING = 1
const val DOOR_STATE_OPENING = 2
const val DOOR_STATE_OPEN = 3
const val DOOR_STATE_LOCKED = 4
const val DOOR_NUM_STATES = 5 // MUST BE LAST!!

const val DOOR_SPEED_FRAMES = 50
const val DOOR_SPEED_FRAMES_INV = 1.0f / DOOR_SPEED_FRAMES
const val DOOR_OPEN_FRAMES = 100
const val BROKEN_DOOR_CLOSED_FRAMES = 100
val DOOR_COLOR: GColor by lazy { GColor.CYAN }
const val DOOR_THICKNESS = 2f

// when a megagun hits an electric wall, the wall is disabled for some time
const val WALL_ELECTRIC_DISABLE_FRAMES = 200
const val WALL_TYPE_NONE = 0
const val WALL_TYPE_INDESTRUCTIBLE = 1 // used for perimeter
const val WALL_TYPE_NORMAL = 2 // normal wall
const val WALL_TYPE_ELECTRIC = 3 // electric walls cant be destroyed? (temp disabled)
const val WALL_TYPE_PORTAL = 4 // teleport to other portal walls
const val WALL_TYPE_RUBBER = 5 // ?
const val WALL_TYPE_DOOR = 6 // need a key too open
const val WALL_TYPE_BROKEN_DOOR = 7 // door opens and closes at random intervals
const val WALL_NUM_TYPES = 8 // MUST BE LAST!

val WALL_TYPE_STRINGS: Array<String>
	get() = arrayOf(
		"NONE",
		"INDS",
		"NORM",
		"ELEC",
		"PORT",
		"RUBR",
		"DOOR",
		"BROK"
	)

fun getWallTypeString(type: Int): String {
	return WALL_TYPE_STRINGS.getOrNull(type) ?: "$type"
}

val DOOR_STATE_STRINGS: Array<String>
	get() = arrayOf(
		"CLOSED",
		"CLOSING",
		"OPENING",
		"OPEN",
		"LOCKED"
	)

fun getDoorStateString(state: Int): String {
	return DOOR_STATE_STRINGS.getOrNull(state) ?: "$state"
}

val PLAYER_STATE_STRINGS: Array<String>
	get() = arrayOf(
		"SPAWNING", "ALIVE", "EXPLODING", "TELEPORTED", "SPECTATOR"
	)

fun getPlayerStateString(state: Int): String {
	return PLAYER_STATE_STRINGS.getOrNull(state) ?: "$state"
}

fun isPerimeterVertex(vertex: Int): Boolean {
	val x = vertex % (MAZE_NUMCELLS_X + 1)
	val y = vertex / (MAZE_NUMCELLS_X + 1)
	return x == 0 || x == MAZE_NUMCELLS_X || y == 0 || y == MAZE_NUMCELLS_Y
}

val BRAIN_PTS_X = intArrayOf(
	15, 13, 13, 8, 7, 6, 4, 2, 2, 3, 3, 4, 4, 8, 11, 12, 14, 17, 20, 21, 21, 23, 25, 27, 29, 29, 28, 29, 25, 24, 22, 20,
	20, 18
)

val BRAIN_PTS_Y = intArrayOf(
	16, 19, 21, 21, 20, 21, 21, 19, 15, 14, 11, 9, 7, 4, 4, 2, 1, 2, 2, 3, 3, 4, 8, 7, 8, 13, 14, 16, 21, 20, 21, 20, 18,
	19
)

val BRAIN_NERVES_X = intArrayOf(8, 6, 10, 11, 13, 17, 15, 17, 16, 16, 18, 19, 21, 21, 23, 22, 23, 25, 26, 28)
val BRAIN_NERVES_Y = intArrayOf(7, 10, 10, 12, 12, 2, 4, 7, 9, 10, 10, 11, 9, 7, 8, 20, 18, 18, 17, 19)
val BRAIN_LEGS_X = intArrayOf(15, 13, 13, 10, 14, 16, 18, 22, 20, 20, 18)
val BRAIN_LEGS_Y = intArrayOf(16, 19, 21, 29, 29, 24, 29, 29, 20, 18, 19)
val BRAIN_NERVES_LEN = intArrayOf(5, 4, 6, 5)

val ENEMY_RADIUS = floatArrayOf(
	ENEMY_GEN_RADIUS,
	ENEMY_ROBOT_RADIUS, ENEMY_ROBOT_RADIUS, ENEMY_ROBOT_RADIUS, ENEMY_ROBOT_RADIUS,
	ENEMY_THUG_RADIUS, ENEMY_THUG_RADIUS, ENEMY_THUG_RADIUS, ENEMY_THUG_RADIUS,
	ENEMY_BRAIN_RADIUS,
	ENEMY_ZOMBIE_RADIUS, ENEMY_ZOMBIE_RADIUS, ENEMY_ZOMBIE_RADIUS, ENEMY_ZOMBIE_RADIUS,
	ENEMY_TANK_RADIUS, ENEMY_TANK_RADIUS, ENEMY_TANK_RADIUS, ENEMY_TANK_RADIUS,
	ENEMY_JAWS_DIM / 2 - 2,
	ENEMY_LAVA_DIM / 2 - 5
).also {
	require(it.size == ENEMY_INDEX_NUM)
}

// enum
const val HIT_TYPE_ENEMY = 0
const val HIT_TYPE_TANK_MISSLE = 1
const val HIT_TYPE_SNAKE_MISSLE = 2
const val HIT_TYPE_ROBOT_MISSLE = 3
const val HIT_TYPE_ELECTRIC_WALL = 4

const val WALL_FLAG_VISITED = 256

val CELL_LOOKUP_DV = intArrayOf(
	-(MAZE_NUMCELLS_X + 1), // UP
	1, // RIGHT
	MAZE_NUMCELLS_X + 1, // LEFT
	-1 // DOWN
)

val PARTICLE_STARS = arrayOf("*", "!", "?", "@")

val TANK_PTS_X = intArrayOf(12, 6, 6, 18, 32, 41, 41, 36, 36, 12)
val TANK_PTS_Y = intArrayOf(10, 10, 26, 38, 38, 26, 10, 10, 4, 4)

