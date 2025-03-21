package cc.game.superrobotron

import cc.lib.game.GColor
import cc.lib.math.CMath

/**
 * Created by Chris Caron on 3/19/25.
 */
// ---------------------------------------------------------//
// CONSTANTS //
// ---------------------------------------------------------//

val DIR_UP = 0
val DIR_RIGHT = 1
val DIR_DOWN = 2
val DIR_LEFT = 3

val PLAYER_STATE_SPAWNING = 0
val PLAYER_STATE_ALIVE = 1
val PLAYER_STATE_EXPLODING = 2
val PLAYER_STATE_TELEPORTED = 3
val PLAYER_STATE_SPECTATOR = 4

// enum
val SNAKE_STATE_CHASE = 0
val SNAKE_STATE_CLIMB_WALL = 1 // slowly climb a wall
val SNAKE_STATE_CREST_WALL = 2 // crest the wall, start to
val SNAKE_STATE_DESCEND_WALL = 3
val SNAKE_STATE_ATTACHED = 4
val SNAKE_STATE_DYING = 5
val SNAKE_MAX_SECTIONS = 10 // max number of section

// game types
val GAME_TYPE_CLASSIC = 0 // No maze
val GAME_TYPE_ROBOCRAZE = 1
val WORLD_WIDTH_CLASSIC = 1000
val WORLD_HEIGHT_CLASSIC = 1000

//  final int GAME_TYPE_ROBOCRAZE = 1; // Use Maze, level over when
// player gets to exit (X)
// possible game states
val GAME_STATE_INTRO = 0
val GAME_STATE_PLAY = 1

//private final int   GAME_STATE_PLAYER_HIT = 2;
val GAME_STATE_GAME_OVER = 3
val MAZE_CELL_WIDTH = 160
val MAZE_CELL_HEIGHT = 160
val MAZE_NUMCELLS_X = 10
val MAZE_NUMCELLS_Y = 10
val MAZE_NUM_VERTS = (MAZE_NUMCELLS_X + 1) * (MAZE_NUMCELLS_Y + 1)
val MAZE_VERTEX_NOISE = 20 //30 // random noise in placement of

//private final int     MAZE_CELL_X = 14;
//private final int     MAZE_CELL_Y = 14;
// vertices
val MAZE_WALL_THICKNESS = 3
val MAZE_WIDTH = MAZE_CELL_WIDTH * MAZE_NUMCELLS_X //1440; // width of world
val MAZE_HEIGHT = MAZE_CELL_HEIGHT * MAZE_NUMCELLS_Y //1240; // height of world

// used to make comparisons close to zero
val EPSILON = 0.00001f
val PLAYER_SPEED = 8 // pixels per frame
val PLAYER_SUPER_SPEED_BONUS = 3
val PLAYER_RADIUS = 16
val PLAYER_RADIUS_BARRIER = 25
val PLAYER_MAX_MISSLES = 16 // max player missles on screen at one time
val PLAYER_DEATH_FRAMES = 70 // number of frames to do the 'death' sequence
val PLAYER_START_LIVES = 3 // number of lives player starts with
val PLAYER_NEW_LIVE_SCORE = 25000 // number of points to give an extra player
val PLAYER_SHOT_FREQ = 4 // number of frames between adjacent
val PLAYER_SHOT_FREQ_MEGAGUN = 3 // missle shots
val PLAYER_MISSLE_SPEED = 20 // distance missle travels per frame
val PLAYER_MISSLE_DURATION = 50 // number of frames a missle exists for
val PLAYER_SPAWN_FRAMES = 30 // number of frames at beginning of level where player is safe
val PLAYER_HULK_GROW_SPEED = 0.05f
val PLAYER_HULK_SCALE = 1.6f
val PLAYER_HULK_CHARGE_SPEED_BONUS = PLAYER_SPEED
val PLAYER_POWERUP_DURATION = 500 // frames
val PLAYER_POWERUP_WARNING_FRAMES = 50 //
val PLAYER_SUPERSPEED_NUM_TRACERS = 4 // used for POWERUP_SPEED mode
val MAX_ENEMIES = 64 // max number of enemies on the screen per frame
val MAX_ENEMY_MISSLES = 16 // max number of enemy missles on screen per frame

// Snake type projectiles
val MAX_SNAKE_MISSLES = 8 // max number deployed at one time
val SNAKE_SECTION_LENGTH = 20 // pixels, should be even multiple of SNAKE_SPEED
val SNAKE_THICKNESS = 3 // pixels
val SNAKE_DURATION = 1000 // frames
val SNAKE_SPEED = 4 // pixels of advancement per frame
val SNAKE_HEURISTIC_FACTOR = 0.4f

// time
val MAX_PARTICLES = 8 // maximum explosions on the screen
val PARTICLE_TYPE_BLOOD = 0
val PARTICLE_TYPE_DYING_ROBOT = 1
val PARTICLE_TYPE_DYING_TANK = 2
val PARTICLE_TYPE_PLAYER_STUN = 3

// if any more particle types are added, then the update funciton
// will need to get fixed
val PARTICLES_NUM_TYPES = 4 // MUST BE LAST!
val PARTICLE_DYING_ROBOT_DURATION = 5
val PARTICLE_DYING_TANK_DURATION = 9
val PARTICLE_BLOOD_DURATION = 60
val PARTICLE_BLOOD_RADIUS = 13
val PARTICLE_LINES_DURATION = 8

// enum of enemy types
val ENEMY_INDEX_GEN = 0 // generator
val ENEMY_INDEX_ROBOT_N = 1 // always moves toward player
val ENEMY_INDEX_ROBOT_E = 2
val ENEMY_INDEX_ROBOT_S = 3
val ENEMY_INDEX_ROBOT_W = 4
val ENEMY_INDEX_THUG_N = 5 // indistructable idiot, walks north
val ENEMY_INDEX_THUG_E = 6 // walks east
val ENEMY_INDEX_THUG_S = 7 // walks south
val ENEMY_INDEX_THUG_W = 8 // walks west
val ENEMY_INDEX_BRAIN = 9 // The evil brain
val ENEMY_INDEX_ZOMBIE_N = 10 // a brain turns people into zombies
val ENEMY_INDEX_ZOMBIE_E = 11
val ENEMY_INDEX_ZOMBIE_S = 12
val ENEMY_INDEX_ZOMBIE_W = 13
val ENEMY_INDEX_TANK_NE = 14 // Tanks only move diagonally and
val ENEMY_INDEX_TANK_SE = 15
val ENEMY_INDEX_TANK_SW = 16
val ENEMY_INDEX_TANK_NW = 17
val ENEMY_INDEX_TANK_GEN_NE = 18 // TankGens only move diagonally
val ENEMY_INDEX_TANK_GEN_SE = 19
val ENEMY_INDEX_TANK_GEN_SW = 20
val ENEMY_INDEX_TANK_GEN_NW = 21
val ENEMY_INDEX_JAWS = 22
val ENEMY_INDEX_LAVA = 23
val ENEMY_INDEX_NUM = 24 // MUST BE LAST
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
val ENEMY_SPAWN_SCATTER = 30 // used to scatter thugs and brains around generators
val ENEMY_ROBOT_RADIUS = 12
val ENEMY_ROBOT_SPEED = 7
val ENEMY_ROBOT_SPEED_INCREASE = 200 // number of player moves to increase robot speed
val ENEMY_ROBOT_MAX_SPEED = PLAYER_SPEED //12;
val ENEMY_ROBOT_HEURISTIC_FACTOR = 0.0f // likelyhood the robot will choose direction toward player
val ENEMY_ROBOT_ATTACK_DIST = 300 // max distance a guy will try to shoot from
val ENEMY_ROBOT_FORCE = 10.0f // stun force from a robot
val ENEMY_PROJECTILE_FREQ = 2 // used to determine frequency of guy fires
val ENEMY_PROJECTILE_RADIUS = 4
val ENEMY_PROJECTILE_FORCE = 5.0f
val ENEMY_PROJECTILE_DURATION = 60
val ENEMY_PROJECTILE_GRAVITY = 0.3f
val ENEMY_GEN_INITIAL = 15 // starting generators for level 1
val ENEMY_GEN_SPAWN_MIN = 3 // minimum to spawn when hit
val ENEMY_GEN_SPAWN_MAX = 10 // maximum to spawn when hit
val ENEMY_GEN_RADIUS = 10
val ENEMY_GEN_PULSE_FACTOR = 3 // gen will pulse +- this
val ENEMY_TANK_GEN_RADIUS = 15

// amount from GEN_RADIUS
// NOTE: on HEURISTIC_FACTOR, 0.0 values make a bot move toward player, 1.0
// makes move totally random
val ENEMY_THUG_UPDATE_FREQ = 4
val ENEMY_THUG_SPEED = 6
val ENEMY_THUG_PUSHBACK = 8.0f // number of units to push the thug on a hit
val ENEMY_THUG_RADIUS = 17
val ENEMY_THUG_HEURISTICE_FACTOR = 0.9f

// ENEMY BRAIN
val ENEMY_BRAIN_RADIUS = 15
val ENEMY_BRAIN_SPEED = 7
val ENEMY_BRAIN_ZOMBIFY_FRAMES = 80 // frames taken to zombify a person
val ENEMY_BRAIN_FIRE_CHANCE = 10 // chance = 1-10-difficulty
val ENEMY_BRAIN_FIRE_FREQ = 100 // frames
val ENEMY_BRAIN_UPDATE_SPACING = 10 // frames between updates (with some noise)

// ZOMBIE
val ENEMY_ZOMBIE_SPEED = 10
val ENEMY_ZOMBIE_RADIUS = 10
val ENEMY_ZOMBIE_UPDATE_FREQ = 2 // number of frames between updates
val ENEMY_ZOMBIE_HEURISTIC_FACTOR = 0.2f //
val ENEMY_ZOMBIE_TRACER_FADE = 3 // higher means slower fade
val MAX_ZOMBIE_TRACERS = 32

// TANK
val ENEMY_TANK_RADIUS = 24
val ENEMY_TANK_SPEED = 5
val ENEMY_TANK_UPDATE_FREQ = 4 // frames between movement and update
val ENEMY_TANK_FIRE_FREQ = 5 // updates between shots fired
val MAX_TANK_MISSLES = 16

// TANK MISSLE
val TANK_MISSLE_SPEED = 8
val TANK_MISSLE_DURATION = 300
val TANK_MISSLE_RADIUS = 8

// -- POINTS --
val ENEMY_GEN_POINTS = 100
val ENEMY_ROBOT_POINTS = 10
val ENEMY_BRAIN_POINTS = 50
val ENEMY_TANK_POINTS = 100
val ENEMY_TANK_GEN_POINTS = 100
val POWERUP_POINTS_SCALE = 100
val POWERUP_BONUS_POINTS_MAX = 50

// -- PEOPLE --
val MAX_PEOPLE = 32
val PEOPLE_NUM_TYPES = 3
val PEOPLE_RADIUS = 10
val PEOPLE_SPEED = 4
val PEOPLE_START_POINTS = 500
val PEOPLE_INCREASE_POINTS = 100 // * game_level
val PEOPLE_MAX_POINTS = 10000

// -- MESSGAES --
val THROBBING_SPEED = 2 // Affects throbbing_white. higher is slower.
val MESSAGE_FADE = 3 // Affects instaMsgs, higher is slower
val MESSAGES_MAX = 4 // max number of inst msgs

// -- lava pit --
val ENEMY_LAVA_CLOSED_FRAMES = 30
val ENEMY_LAVA_DIM = 64
val ENEMY_JAWS_DIM = 32
val DIFFICULTY_EASY = 0
val DIFFICULTY_MEDIUM = 1
val DIFFICULTY_HARD = 2


val BUTTONS_NUM = Button.values().size
val BUTTON_WIDTH = 130
val BUTTON_HEIGHT = 40

// for other info on intro screen
val TEXT_PADDING = 5 // pixels between hud text lines and border

// -- POWERUPS --
// ENUM
val POWERUP_SUPER_SPEED = 0
val POWERUP_GHOST = 1
val POWERUP_BARRIER = 2
val POWERUP_MEGAGUN = 3
val POWERUP_HULK = 4
val POWERUP_BONUS_POINTS = 5
val POWERUP_BONUS_PLAYER = 6
val POWERUP_KEY = 7
val POWERUP_NUM_TYPES = 8 // MUST BE LAST!

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
val powerup_names = arrayOf(
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
	return powerup_names[type]
}

val MAX_POWERUPS = 8
val POWERUP_MAX_DURATION = 500
val POWERUP_RADIUS = 10
val POWERUP_CHANCE_BASE = 1000
val POWERUP_CHANCE_ODDS = 10 // 10+currentLevel in 1000

// Rubber Walls
val RUBBER_WALL_MAX_FREQUENCY = 0.15f
val RUBBER_WALL_FREQENCY_INCREASE_MISSLE = 0.03f
val RUBBER_WALL_FREQENCY_COOLDOWN = 0.002f

// chance
// -- STATIC FIELD --
// TODO: Why cant I increase this number w/out wierd results?
val STATIC_FIELD_SECTIONS = 8
val STATIC_FIELD_COS_T = CMath.cosine(360.0f / STATIC_FIELD_SECTIONS)
val STATIC_FIELD_SIN_T = CMath.sine(360.0f / STATIC_FIELD_SECTIONS)

// number of repeated shots from megagun required to break wall
val WALL_NORMAL_HEALTH = 30
val WALL_BREAKING_MAX_RECURSION = 3

val DARKEN_AMOUNT = 0.2f
val LIGHTEN_AMOUNT = 0.2f

val DOOR_STATE_LOCKED = 0
val DOOR_STATE_CLOSED = 1
val DOOR_STATE_CLOSING = 2
val DOOR_STATE_OPENING = 3
val DOOR_STATE_OPEN = 4
val DOOR_SPEED_FRAMES = 50
val DOOR_SPEED_FRAMES_INV = 1.0f / DOOR_SPEED_FRAMES
val DOOR_OPEN_FRAMES = 100
val DOOR_OPEN_FRAMES_INV = 1.0f / DOOR_OPEN_FRAMES
var DOOR_COLOR = GColor.CYAN
val DOOR_THICKNESS = 2

// when a megagun hits an electric wall, the wall is disabled for some time
const val WALL_ELECTRIC_DISABLE_FRAMES = 200
const val WALL_TYPE_NONE = 0
const val WALL_TYPE_NORMAL = 1 // normal wall
const val WALL_TYPE_ELECTRIC = 2 // electric walls cant be destroyed? (temp disabled)
const val WALL_TYPE_INDESTRUCTABLE = 3 // used for perimiter
const val WALL_TYPE_PORTAL = 4 // teleport to other portal walls
const val WALL_TYPE_RUBBER = 5 // ?
const val WALL_TYPE_DOOR = 6 // need a key too open
const val WALL_TYPE_PHASE_DOOR = 7 // door opens and closes at random intervals
const val WALL_NUM_TYPES = 8 // MUST BE LAST!


val wall_type_strings = arrayOf(
	"NONE",
	"Normal",
	"Electric",
	"Indestructable",
	"Portal",
	"Rubber",
	"Door",
	"Phase Door"
)

fun getWallTypeString(type: Int): String {
	if (type in wall_type_strings.indices)
		return wall_type_strings[type]
	return "UNKNOWN"
}

val door_state_strings = arrayOf(
	"LOCKED",
	"CLOSED",
	"CLOSING",
	"OPENING",
	"OPEN"
)

fun getDoorStateString(state: Int): String {
	return door_state_strings[state]
}
