package cc.game.superrobotron

import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.IVector2D
import cc.lib.ksp.binaryserializer.BinarySerializable
import cc.lib.ksp.binaryserializer.BinaryType
import cc.lib.ksp.binaryserializer.IBinarySerializable
import cc.lib.ksp.binaryserializer.readFloat
import cc.lib.ksp.binaryserializer.readInt
import cc.lib.ksp.binaryserializer.readUByte
import cc.lib.ksp.binaryserializer.writeFloat
import cc.lib.ksp.binaryserializer.writeInt
import cc.lib.ksp.binaryserializer.writeUByte
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.reflector.Omit
import cc.lib.reflector.Reflector
import cc.lib.utils.random
import java.nio.ByteBuffer
import java.util.LinkedList

val WORLD_BOX =
	GRectangle(-MAZE_VERTEX_NOISE, -MAZE_VERTEX_NOISE, MAZE_WIDTH + 2 * MAZE_VERTEX_NOISE, MAZE_HEIGHT + 2 * MAZE_VERTEX_NOISE)

abstract class Object {
	val pos = object : MutableVector2D() {
		// TEMP CODE TO BE REMOVED
		override fun assign(v: IVector2D): MutableVector2D {
			require(v in WORLD_BOX)
			return super.assign(v)
		}
	}
}

@BinarySerializable("Missile")
abstract class AMissile : Object() {
	val dv = MutableVector2D()

	@BinaryType(UShort::class)
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
	@BinaryType(UShort::class)
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

	constructor() : this(-1, -1, -1)

	companion object {
		init {
			addAllFields(Wall::class.java)
		}

		val filterTypes = arrayOf(WallType.INDESTRUCTABLE, WallType.PORTAL)
	}

	var type: WallType = WallType.NONE  // all
	var state = 0    // door
	var frame = 0    // door, electric
	var health = 0    // normal
	var frequency = 0f  // for rubber walls

	var visible: Boolean = false
	var ending: Boolean = false
	var portalId = 0 // id of the wall this portal teleports us to

	@Omit
	var nearV: Int = 0
		set(value) {
			field = value
			farV = if (value == v0) v1 else v0
		}

	@Omit
	var farV: Int = -1
		private set

	@Omit
	var adjacent: Wall? = null

	fun initPortal(target: Wall) {
		portalId = target.id
		target.portalId = id
	}

	fun isPerimeter(): Boolean = isPerimeterVertex(v0) && isPerimeterVertex(v1)


	override fun toString(): String {
		var str = type.str + "[$id] "
		if (farV >= 0) {
			str += "$nearV -> $farV"
		} else {
			str += "$v0 <-> $v1"
		}
		if (ending) str += " END"
		when (type) {
			WallType.NORMAL -> str += ", health=$health"
			WallType.ELECTRIC -> str += ", frame=$frame"
			WallType.BROKEN_DOOR,
			WallType.DOOR -> str += ", state=${getDoorStateString(state)}, frame=$frame"

			WallType.PORTAL -> str += ", portal id:$portalId]"
			WallType.RUBBER -> str += ", freq=$frequency"
			else -> Unit
		}
		return str
	}

	override fun equals(other: Any?): Boolean {
		(other as? Wall)?.let {
			return id == other.id
				&& type == other.type
				&& state == other.state
		}
		return super.equals(other)
	}

	override fun hashCode(): Int {
		return id.hashCode()
	}

	override fun copy(other: Wall) {
		TODO("Not yet implemented")
	}

	override fun serialize(output: ByteBuffer) {
		output.writeUByte(type.ordinal)
		when (type) {
			WallType.NONE,
			WallType.PORTAL,
			WallType.INDESTRUCTABLE -> Unit

			WallType.NORMAL -> {
				output.writeUByte(health)
			}

			WallType.ELECTRIC -> output.writeInt(frame) // electric walls cant be destroyed? (temp disabled)
			WallType.RUBBER -> output.writeFloat(frequency)
			WallType.DOOR,
			WallType.BROKEN_DOOR -> {
				output.writeUByte(state)
				output.writeInt(frame)
			}
		}
	}

	override fun deserialize(input: ByteBuffer) {
		type = WallType.values()[input.readUByte()]
		when (type) {
			WallType.NONE,
			WallType.PORTAL,
			WallType.INDESTRUCTABLE -> Unit

			WallType.NORMAL -> health = input.readUByte()
			WallType.ELECTRIC -> frame = input.readInt()
			WallType.RUBBER -> frequency = input.readFloat()
			WallType.DOOR,
			WallType.BROKEN_DOOR -> {
				state = input.readUByte()
				frame = input.readInt()
			}
		}
	}

	override fun contentEquals(other: Wall): Boolean {
		return super.equals(other)
	}
}

enum class RoboConnectionStatus(val code: String) {
	HOST("H"),
	CONNECTED("C"),
	DISCONNECTED("D")
}

@BinarySerializable("Player")
abstract class APlayer : Object() {

	// Rectangle of visible maze
	@Transient
	val screen = GRectangle().withDimension(800, 600)

	@Transient
	var displayName = ""

	@Transient
	var status = RoboConnectionStatus.DISCONNECTED

	@BinaryType(UByte::class)
	var dir = DIR_DOWN
	var scale = 1f
	var score = 0

	@BinaryType(UByte::class)
	var lives = 0

	@Transient
	val motion_dv = object : MutableVector2D() {
		override fun assign(v: IVector2D): MutableVector2D {
			require(v.magSquared() < 50 * 50) { "trying to assign motion_dv $v is too big" }
			return super.assign(v)
		}
	}

	@Transient
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

	@Transient
	var firing = false
	val start_v = MutableVector2D()

	//int killed_frame = 0; // the frame the player was killed
	@Transient
	var missles = ManagedArray(Array(MAX_PLAYER_MISSLES) { Missile() })
	var hulk_charge_frame = 0

	//boolean teleported = false;
	@BinaryType(UByte::class)
	var state = PLAYER_STATE_SPAWNING
	var next_state_frame = 0
		protected set
	val stun_dv = MutableVector2D()

	@Transient
	val tracer = ManagedArray(Array(PLAYER_SUPERSPEED_NUM_TRACERS) { Tracer() })

	@Omit
	var barrier_electric_wall_id = -1

	@BinaryType(java.lang.Byte::class)
	var hit_type = -1 // these are the 'reset' values
		protected set

	@BinaryType(java.lang.Byte::class)
	var hit_index = -1
		protected set
	var cur_cell = IntArray(2) { -1 }

	@Transient
	var path: MutableList<IntArray> = LinkedList()
	var last_shot_frame = 0
	var people_picked_up = 0

	val radius: Float
		get() = (scale * PLAYER_RADIUS)

	val isAlive: Boolean
		get() = state == PLAYER_STATE_ALIVE || state == PLAYER_STATE_SPAWNING

	fun explode(frameNumber: Int, hitType: Int = -1, hitIndex: Int = -1) {
		this.hit_index = hitIndex
		this.hit_type = hitType
		state = PLAYER_STATE_EXPLODING
		next_state_frame = frameNumber + PLAYER_DEATH_FRAMES
	}

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
		barrier_electric_wall_id = -1
		people_picked_up = 0
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
	var color = GColor.TRANSPARENT.copyOf()

	@BinaryType(UByte::class)
	var dir = 0

	fun init(pos: Vector2D, color: GColor, dir: Int = 0) {
		this.pos.assign(pos)
		this.color = color.copyOf()
		this.dir = dir
	}

}

@BinarySerializable("People")
abstract class APeople : Object() {
	@BinaryType(UByte::class)
	var type: Int = 0

	@BinaryType(java.lang.Byte::class)
	var state: Int = 0

	fun init() {
		type = random(0 until PEOPLE_NUM_TYPES)
		state = random(1..4)
	}
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
	var color = GColor.TRANSPARENT.copyOf()

	fun init(pos: Vector2D, str: String, color: GColor = GColor.WHITE) {
		this.pos.assign(pos)
		this.msg = str
		this.color.copy(color)
	}

	override fun toString(): String {
		return "Message: $msg [$pos]"
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