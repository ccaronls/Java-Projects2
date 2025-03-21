package cc.game.superrobotron

import cc.lib.game.GColor
import cc.lib.ksp.binaryserializer.BinarySerializable
import cc.lib.utils.random
import java.util.LinkedList

@BinarySerializable("MissileInt")
abstract class AMissileInt {
	var x: Int = 0
	var y: Int = 0
	var dx: Int = 0
	var dy: Int = 0
	var duration: Int = 0

	fun init(x: Int, y: Int, dx: Int, dy: Int, duration: Int) {
		this.x = x
		this.y = y
		this.dx = dx
		this.dy = dy
		this.duration = duration
	}
}

@BinarySerializable("MissileFloat")
abstract class AMissileFloat {
	var x: Float = 0f
	var y: Float = 0f
	var dx: Float = 0f
	var dy: Float = 0f
	var duration: Int = 0

	fun init(x: Float, y: Float, dx: Float, dy: Float, duration: Int) {
		this.x = x
		this.y = y
		this.dx = dx
		this.dy = dy
		this.duration = duration
	}
}

@BinarySerializable("MissileSnake")
abstract class AMissileSnake {
	// position of head
	var x: Int = 0
	var y: Int = 0

	// counter
	var duration: Int = 0
	var state: Int = 0

	// direction of each section
	var dir: IntArray = IntArray(SNAKE_MAX_SECTIONS)
	var num_sections = 0

	fun init(x: Int, y: Int, duration: Int, state: Int) {
		this.x = x
		this.y = y
		this.duration = duration
		this.state = state
		this.num_sections = 0
		dir[1] = random(0..3)
		dir[0] = dir[1]
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as AMissileSnake

		if (x != other.x) return false
		if (y != other.y) return false
		if (duration != other.duration) return false
		if (state != other.state) return false
		return dir.contentEquals(other.dir)
	}

	override fun hashCode(): Int {
		var result = x
		result = 31 * result + y
		result = 31 * result + duration
		result = 31 * result + state
		result = 31 * result + dir.contentHashCode()
		return result
	}
}


@BinarySerializable("Powerup")
abstract class APowerup : AMissileFloat() {
	var type: Int = 0

	fun init(x: Float, y: Float, dx: Float, dy: Float, duration: Int, type: Int) {
		super.init(x, y, dx, dy, duration)
		this.type = type
	}
}

@BinarySerializable("Wall")
abstract class AWall {
	var v0: Int = 0
	var v1: Int = 0
	var type: Int = 0 // all
	var state: Int = 0   // door
	var frame: Int = 0   // door, electric
	var health: Int = 0   // normal
	var p0: Int = 0
	var p1: Int = 0 // portal
	var frequency: Float = 0f   // for rubber walls
	var visible: Boolean = false
	var ending: Boolean = false

	fun clear() {
		health = 0
		frame = health
		state = frame
		type = -1
		v1 = 0
		v0 = 0
		visible = false
		ending = false
		frequency = 0f
	}

	fun initDoor(state: Int, v0: Int, v1: Int) {
		type = WALL_TYPE_DOOR
		this.state = state
		this.v0 = v0
		this.v1 = v1
	}

	fun init(type: Int, v0: Int, v1: Int) {
		this.type = type
		this.v0 = v0
		this.v1 = v1
	}

	override fun toString(): String {
		var str = getWallTypeString(type) + " [" + v0 + ", " + v1 + "]"
		if (ending) str += "\nEND"
		when (type) {
			WALL_TYPE_NORMAL -> str += "\nhealth=$health"
			WALL_TYPE_ELECTRIC -> str += "\nframe=$frame"
			WALL_TYPE_DOOR -> str += """
 state=${getDoorStateString(state)}
frame=$frame"""

			WALL_TYPE_PORTAL -> str += "\np0=$p0, p1=$p1]"
			WALL_TYPE_RUBBER -> str += "\nfreq=$frequency"
		}
		return str
	}
}

@BinarySerializable("Player")
abstract class APlayer {
	var x = 0
	var y = 0
	var dx = 0
	var dy = 0
	var dir = DIR_DOWN
	var scale = 1f
	var score = 0
	var lives = 0
	var target_dx = 0
	var target_dy = 0
	var start_x = 0
	var start_y = 0
	var powerup = -1
	var powerup_duration = 0
	var keys = 0
	var movement = 0
	var firing = false
	@Transient
	var primary_verts = intArrayOf(-1, -1, -1, -1)

	//int killed_frame = 0; // the frame the player was killed
	@Transient
	var missles = Array(PLAYER_MAX_MISSLES) { MissileInt() }
	var num_missles = 0
	var hulk_charge_dx = 0
	var hulk_charge_dy = 0
	var hulk_charge_frame = 0

	//boolean teleported = false;
	var state = PLAYER_STATE_SPAWNING
	var next_state_frame = 0
	var stun_dx = 0f
	var stun_dy = 0f
	@Transient
	var tracer_x = IntArray(PLAYER_SUPERSPEED_NUM_TRACERS)
	@Transient
	var tracer_y = IntArray(PLAYER_SUPERSPEED_NUM_TRACERS)
	var num_tracers = 0
	@Transient
	var tracer_color = Array(PLAYER_SUPERSPEED_NUM_TRACERS) { GColor.TRANSPARENT }
	@Transient
	var tracer_dir = IntArray(PLAYER_SUPERSPEED_NUM_TRACERS)
	@Transient
	var barrier_electric_wall = intArrayOf(-1, -1, -1, -1)
	var hit_type = -1 // these are the 'reset' values
	var hit_index = -1
	var cellXY = IntArray(2)
	@Transient
	var path: MutableList<IntArray> = LinkedList()
	var last_shot_frame = 0

	@Transient
	var collision_info: AWall? = null

	init {
		//for (i in 0 until PLAYER_MAX_MISSLES) missles[i] = MissileInt()
	}
}

@BinarySerializable("Particle")
abstract class AParticle {
	var x: Int = 0
	var y: Int = 0
	var type: Int = 0
	var duration: Int = 0
	var start_frame: Int = 0
	var star: Int = 0
	var angle: Int = 0
	var playerIndex: Int = 0
}

// -- BUTTONS ON INTRO SCREEN --
enum class Button {
	Classic,
	RoboCraze,
	Easy,
	Medium,
	Hard,
	START
}
