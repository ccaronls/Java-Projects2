package cc.game.superrobotron

import cc.lib.game.GColor
import cc.lib.ksp.binaryserializer.BinarySerializable
import cc.lib.utils.random
import java.util.LinkedList
import kotlin.math.max
import kotlin.math.min

abstract class Object {
	var x = 0f
	var y = 0f
}

@BinarySerializable("Missile")
abstract class AMissileFloat : Object() {
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
abstract class AMissileSnake : Object() {

	// counter
	var duration: Int = 0
	var state: Int = 0

	// direction of each section
	var dir: IntArray = IntArray(SNAKE_MAX_SECTIONS)
	var num_sections = 0

	fun init(x: Float, y: Float, duration: Int, state: Int) {
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

	fun kill() {
		state = SNAKE_STATE_DYING
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

	// populated by last collision check with the vertex used as nearest
	@Transient
	var nearV: Int = 0
		set(value) {
			field = value
			farV = if (value == v0) v1 else v0
		}
	@Transient
	var farV: Int = -1
		private set
	@Transient
	var adjacent: Wall? = null


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
		nearV = -1
		farV = -1
		adjacent = null
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

	override fun equals(other: Any?): Boolean {
		(other as? Wall)?.let {
			return min(v0, v1) == min(it.v0, it.v1) && max(v0, v1) == max(it.v0, it.v1)
		}
		return super.equals(other)
	}
}

@BinarySerializable("Player")
abstract class APlayer : Object() {
	var dx = 0f
	var dy = 0f
	var dir = DIR_DOWN
	var scale = 1f
	var score = 0
	var lives = 0
	var target_dx = 0f
	var target_dy = 0f
	var start_cell = intArrayOf(0, 0)
	var powerup = -1
	var powerup_duration = 0
	var keys = 0
	var movement = 0
	var firing = false
	var start_x: Float = 0f
	var start_y: Float = 0f

	//int killed_frame = 0; // the frame the player was killed
	@Transient
	var missles = ManagedArray(Array(PLAYER_MAX_MISSLES) { Missile() })
	var num_missles = 0
	var hulk_charge_dx = 0f
	var hulk_charge_dy = 0f
	var hulk_charge_frame = 0

	//boolean teleported = false;
	var state = PLAYER_STATE_SPAWNING
	var next_state_frame = 0
	var stun_dx = 0f
	var stun_dy = 0f

	@Transient
	val tracer = ManagedArray(Array(PLAYER_SUPERSPEED_NUM_TRACERS) { Tracer() })

	@Transient
	var barrier_electric_wall = floatArrayOf(-1f, -1f, -1f, -1f)
	var hit_type = -1 // these are the 'reset' values
	var hit_index = -1
	var cur_cell = IntArray(2) { -1 }

	@Transient
	var path: MutableList<IntArray> = LinkedList()
	var last_shot_frame = 0

	val radius: Float
		get() = (scale * PLAYER_RADIUS)

	val isAlive: Boolean
		get() = state == PLAYER_STATE_ALIVE

	fun reset(frameNumber: Int) {
		missles.clear()
		tracer.clear()
		x = start_x
		y = start_y
		powerup = -1
		scale = 1.0f
		last_shot_frame = 0
		stun_dy = 0f
		stun_dx = 0f
		hit_index = -1
		hit_type = -1
		next_state_frame = frameNumber + PLAYER_SPAWN_FRAMES
		state = PLAYER_STATE_SPAWNING
		barrier_electric_wall.fill(-1f)
	}
}

@BinarySerializable("Particle")
abstract class AParticle : Object() {
	var type: Int = 0
	var duration: Int = 0
	var start_frame: Int = 0
	var star: Int = 0
	var angle: Float = 0f
	var playerIndex: Int = 0
}

@BinarySerializable("Tracer")
abstract class ATracer() : Object() {
	var color = GColor.TRANSPARENT
	var dir = 0

	fun init(x: Float, y: Float, color: GColor, dir: Int = 0) {
		this.x = x
		this.y = y
		this.color = color
		this.dir = dir
	}

}

@BinarySerializable("People")
abstract class APeople : Object() {
	var type: Int = 0
	var state: Int = 0
}

@BinarySerializable("Enemy")
abstract class AEnemy : Object() {
	var type: Int = 0
	var next_update: Int = 0
	var spawned_frame = 0
	var killable: Boolean = true
}

@BinarySerializable("Message")
abstract class AMessage : Object() {

	var msg = ""
	var color = GColor.TRANSPARENT

	fun init(x: Float, y: Float, str: String, color: GColor = GColor.WHITE) {
		this.x = x
		this.y = y
		this.msg = str
		this.color = color
	}

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