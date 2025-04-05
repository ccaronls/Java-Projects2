package cc.game.superrobotron

import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.kreflector.Reflector
import cc.lib.ksp.binaryserializer.BinarySerializable
import cc.lib.ksp.binaryserializer.BinaryType
import cc.lib.ksp.binaryserializer.IBinarySerializable
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.utils.random
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.LinkedList

abstract class Object {
	val pos = MutableVector2D()
}

@BinarySerializable("Missile")
abstract class AMissile : Object() {
	val dv = MutableVector2D()
	@BinaryType(UByte::class)
	var duration: Int = 0

	fun init(pos: Vector2D, dv: Vector2D, duration: Int) {
		this.pos.assign(pos)
		this.dv.assign(dv)
		this.duration = duration
	}
}

@BinarySerializable("MissileSnake")
abstract class AMissileSnake : Object() {

	// counter
	@BinaryType(UByte::class)
	var duration: Int = 0

	@BinaryType(UByte::class)
	var state: Int = 0

	// direction of each section
	var dir: IntArray = IntArray(SNAKE_MAX_SECTIONS)

	@BinaryType(UByte::class)
	var num_sections = 0

	fun init(pos: Vector2D, duration: Int, state: Int) {
		this.pos.assign(pos)
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

		if (pos != other.pos) return false
		if (duration != other.duration) return false
		if (state != other.state) return false
		return dir.contentEquals(other.dir)
	}

	fun kill() {
		state = SNAKE_STATE_DYING
	}
}


@BinarySerializable("Powerup")
abstract class APowerup : Object() {
	@BinaryType(UByte::class)
	var type: Int = 0
	@BinaryType(UByte::class)
	var duration: Int = 0

	fun init(pos: Vector2D, type: Int) {
		this.pos.assign(pos)
		this.type = type
		this.duration = 0
	}
}

/*
Wall size: 34
Player size: 129
Missile size: 20
Powerup size: 24
Enemy size: 21
 */

class Wall(val id: Int, val v0: Int, val v1: Int) : Reflector<Wall>(), IBinarySerializable<Wall> { // Wall ids start at 1
	var type = 0 // all
	var state = 0   // door
	var frame = 0   // door, electric
	var health = 0   // normal
	var frequency: Float = 0f   // for rubber walls
	var visible: Boolean = false
	var ending: Boolean = false
	var portalId = 0 // id of the wall this portal teleports us to

	var nearV: Int = 0
		set(value) {
			field = value
			farV = if (value == v0) v1 else v0
		}
	var farV: Int = -1
		private set
	var adjacent: Wall? = null

	fun initPortal(target: Wall) {
		portalId = target.id
		target.portalId = id
	}

	fun isPerimeter(): Boolean = isPerimeterVertex(v0) && isPerimeterVertex(v1)


	override fun toString(): String {
		var str = getWallTypeString(type) + "[$id] " + nearV + "->" + farV
		if (ending) str += "END"
		when (type) {
			WALL_TYPE_NORMAL -> str += "\nhealth=$health"
			WALL_TYPE_ELECTRIC -> str += "\nframe=$frame"
			WALL_TYPE_BROKEN_DOOR,
			WALL_TYPE_DOOR -> str += """
                                    state=${getDoorStateString(state)}
									frame=$frame""".trimIndent()

			WALL_TYPE_PORTAL -> str += "\nportal id:$portalId]"
			WALL_TYPE_RUBBER -> str += "\nfreq=$frequency"
		}
		return str
	}

	override fun equals(other: Any?): Boolean {
		(other as? Wall)?.let {
			return id == other.id
		}
		return super.equals(other)
	}

	override fun hashCode(): Int {
		return id.hashCode()
	}

	override fun copy(other: Wall) {
		TODO("Not yet implemented")
	}

	override fun serialize(output: DataOutputStream) {
		output.writeByte(type)
		when (type) {
			WALL_TYPE_NONE,
			WALL_TYPE_PORTAL,
			WALL_TYPE_INDESTRUCTIBLE -> Unit

			WALL_TYPE_NORMAL -> output.writeShort(health)
			WALL_TYPE_ELECTRIC -> output.writeInt(frame) // electric walls cant be destroyed? (temp disabled)
			WALL_TYPE_RUBBER -> output.writeFloat(frequency)
			WALL_TYPE_DOOR,
			WALL_TYPE_BROKEN_DOOR -> {
				output.writeByte(state)
				output.writeInt(frame.toInt())
			}
		}
	}

	override fun deserialize(input: DataInputStream) {
		type = input.readUnsignedByte()
		when (type) {
			WALL_TYPE_NONE,
			WALL_TYPE_PORTAL,
			WALL_TYPE_INDESTRUCTIBLE -> Unit

			WALL_TYPE_NORMAL -> health = input.readUnsignedShort()
			WALL_TYPE_ELECTRIC -> frame = input.readInt()
			WALL_TYPE_RUBBER -> frequency = input.readFloat()
			WALL_TYPE_DOOR,
			WALL_TYPE_BROKEN_DOOR -> {
				state = input.readUnsignedByte()
				frame = input.readInt()
			}
		}
	}
}

@BinarySerializable("Player")
abstract class APlayer : Object() {
	val dv = MutableVector2D()

	// Rectangle of visible maze
	@Transient
	val screen = GRectangle()
	@BinaryType(UByte::class)
	var dir = DIR_DOWN
	var scale = 1f
	var score = 0

	@BinaryType(UByte::class)
	var lives = 0
	val target_dv = MutableVector2D()
	var start_cell = intArrayOf(0, 0)
	@BinaryType(java.lang.Byte::class)
	var powerup = -1
	@BinaryType(UShort::class)
	var powerup_duration = 0
	@BinaryType(UByte::class)
	var keys = 0
	@BinaryType(UShort::class)
	var movement = 0
	var firing = false
	val start_v = MutableVector2D()

	//int killed_frame = 0; // the frame the player was killed
	var missles = ManagedArray(Array(MAX_PLAYER_MISSLES) { Missile() })
	var hulk_charge_frame = 0

	//boolean teleported = false;
	@BinaryType(UByte::class)
	var state = PLAYER_STATE_SPAWNING
	var next_state_frame = 0
	val stun_dv = MutableVector2D()

	@Transient
	val tracer = ManagedArray(Array(PLAYER_SUPERSPEED_NUM_TRACERS) { Tracer() })

	@Transient
	var barrier_electric_wall = floatArrayOf(-1f, -1f, -1f, -1f)
	@BinaryType(java.lang.Byte::class)
	var hit_type = -1 // these are the 'reset' values
	@BinaryType(java.lang.Byte::class)
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
		pos.assign(start_v)
		powerup = -1
		scale = 1.0f
		last_shot_frame = 0
		stun_dv.zeroEq()
		hit_index = -1
		hit_type = -1
		next_state_frame = frameNumber + PLAYER_SPAWN_FRAMES
		state = PLAYER_STATE_SPAWNING
		barrier_electric_wall.fill(-1f)
	}
}

@BinarySerializable("Particle")
abstract class AParticle : Object() {
	@BinaryType(UByte::class)
	var type: Int = 0
	@BinaryType(UByte::class)
	var duration = 0
	var start_frame = 0
	@BinaryType(UByte::class)
	var star: Int = 0
	@BinaryType(UShort::class)
	var angle: Int = 0
	@BinaryType(UByte::class)
	var playerIndex: Int = 0
}

@BinarySerializable("Tracer")
abstract class ATracer() : Object() {
	var color = GColor.TRANSPARENT

	@BinaryType(UByte::class)
	var dir = 0

	fun init(pos: Vector2D, color: GColor, dir: Int = 0) {
		this.pos.assign(pos)
		this.color = color
		this.dir = dir
	}

}

@BinarySerializable("People")
abstract class APeople : Object() {
	@BinaryType(UByte::class)
	var type: Int = 0
	@BinaryType(java.lang.Byte::class)
	var state: Int = 0
}

@BinarySerializable("Enemy")
abstract class AEnemy : Object() {
	@BinaryType(UByte::class)
	var type: Int = 0
	var next_update = 0
	var spawned_frame = 0
}

@BinarySerializable("Message")
abstract class AMessage : Object() {

	var msg = ""
	var color = GColor.TRANSPARENT

	fun init(pos: Vector2D, str: String, color: GColor = GColor.WHITE) {
		this.pos.assign(pos)
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