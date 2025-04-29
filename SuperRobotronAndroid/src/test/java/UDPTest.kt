import cc.game.superrobotron.*
import cc.lib.ksp.binaryserializer.*
import cc.lib.utils.contains
import cc.lib.utils.random
import cc.lib.utils.randomFloat
import org.junit.Assert.*
import org.junit.Test

/**
 * Created by Chris Caron on 4/11/25.
 */
class UDPTest {


	@Test
	fun test0() {

		val buffer = UDPCommon.createBuffer()

		for (i in Byte.MIN_VALUE..Byte.MAX_VALUE) {
			buffer.rewind()
			buffer.writeByte(i)
			buffer.rewind()
			assertEquals(i, buffer.readByte())
		}

		for (i in UByte.MIN_VALUE.toInt()..UByte.MAX_VALUE.toInt()) {
			buffer.rewind()
			buffer.writeUByte(i)
			buffer.rewind()
			assertEquals(i, buffer.readUByte())
		}

		for (i in Short.MIN_VALUE..Short.MAX_VALUE) {
			buffer.rewind()
			buffer.writeShort(i)
			buffer.rewind()
			assertEquals(i, buffer.readShort())
		}

		for (i in UShort.MIN_VALUE.toInt()..Short.MAX_VALUE.toInt()) {
			buffer.rewind()
			buffer.writeUShort(i)
			buffer.rewind()
			assertEquals(i, buffer.readUShort())
		}

		for (i in Int.MIN_VALUE..Int.MAX_VALUE) {
			buffer.rewind()
			buffer.writeInt(i)
			buffer.rewind()
			assertEquals(i, buffer.readInt())
		}

		for (i in UInt.MIN_VALUE..UInt.MAX_VALUE step 0xffff) {
			buffer.rewind()
			buffer.writeUInt(i)
			buffer.rewind()
			assertEquals(i, buffer.readUInt())
		}

		for (i in Long.MIN_VALUE..Long.MAX_VALUE step 0xffffffffffff) {
			buffer.rewind()
			buffer.writeLong(i)
			buffer.rewind()
			assertEquals(i, buffer.readLong())
		}
		for (i in ULong.MIN_VALUE..ULong.MAX_VALUE step 0xffffffffffff) {
			buffer.rewind()
			buffer.writeULong(i)
			buffer.rewind()
			assertEquals(i, buffer.readULong())
		}

		/*
		val buffer = ByteArrayOutputStream(1024)
		val output = DataOutputStream(buffer)
		output.writeUnsignedByte(0xff)
		val inStr = ByteArrayInputStream(buffer.toByteArray())
		val input = DataInputStream(inStr)
		assertEquals(0xff, input.readUnsignedByte())

		buffer.reset()
		inStr.reset()
		output.writeShort(0xffff)
		assertEquals(0xffff, input.readUnsignedShort())
*/

	}

	private fun serializeObjects(r1: Robotron, r2: Robotron) {
		val buffer = UDPCommon.createBuffer()
		arrayOf(
			Pair(r1.players, r2.players),
			Pair(r1.tank_missiles, r2.tank_missiles),
			Pair(r1.enemies, r2.enemies),
			Pair(r1.messages, r2.messages),
			Pair(r1.particles, r2.particles),
			Pair(r1.powerups, r2.powerups),
			Pair(r1.snake_missiles, r2.snake_missiles),
			Pair(r1.zombie_tracers, r2.zombie_tracers),
			Pair(r1.people, r2.people)
		).forEach {
			buffer.clear()
			it.first.serialize(buffer)
			buffer.flip()
			it.second.deserialize(buffer)
		}
	}

	@Test
	fun test1() {

		val r1 = TRobo()
		val r2 = TRobo()

		assertEquals(r1, r2)

		r1.buildAndPopulateRobocraze()

		r1.walls.forEach {
			it.filterNotNull().forEach { w ->
				assertTrue(r1.getWall(w.v0, w.v1) === r1.getWall(w.v1, w.v0))
			}
		}

		run {
			val w = r1.walls.flatten().filterNotNull().filter { !it.isPerimeter() && it.type != WALL_TYPE_NONE }
			for (t in WALL_TYPE_NORMAL until WALL_NUM_TYPES) {
				if (!w.contains { it.type == t }) {
					w.random().type = t
				}
			}
		}

		assertNotEquals(r1, r2)

		val str = r1.serializeToString()
//		println(str)
		r2.merge(str)

		// the managed arrays will not be synced yet
		assertNotEquals(r1, r2)

		val buffer = UDPCommon.createBuffer()

		arrayOf(
			Pair(r1.players, r2.players),
			Pair(r1.tank_missiles, r2.tank_missiles),
			Pair(r1.enemies, r2.enemies),
			Pair(r1.messages, r2.messages),
			Pair(r1.particles, r2.particles),
			Pair(r1.powerups, r2.powerups),
			Pair(r1.snake_missiles, r2.snake_missiles),
			Pair(r1.zombie_tracers, r2.zombie_tracers),
			Pair(r1.people, r2.people)
		).forEach {
			buffer.clear()
			it.first.serialize(buffer)
			buffer.flip()
			it.second.deserialize(buffer)
		}

		r1.wall_lookup
		r2.wall_lookup

		assertEquals(r1, r2)

		assertTrue(r1.wall_lookup.toList().toTypedArray().contentEquals(r2.wall_lookup.toList().toTypedArray()))

		val r1Walls = r1.walls.flatten().filterNotNull().toTypedArray()
		val r2Walls = r2.walls.flatten().filterNotNull().toTypedArray()

		assertTrue(r1Walls.contentEquals(r2Walls))

		for (w in WALL_TYPE_NORMAL until WALL_NUM_TYPES) {
			do {
				val it = r1Walls.random().takeIf { it.type == w } ?: continue
				when (it.type) {
					WALL_TYPE_NORMAL -> it.health = random(100)
					WALL_TYPE_ELECTRIC -> it.frame = random(1000)
					WALL_TYPE_RUBBER -> it.frequency = randomFloat(10f)
					WALL_TYPE_BROKEN_DOOR,
					WALL_TYPE_DOOR -> it.state = random(DOOR_NUM_STATES)

					else -> break
				}

				println("testing wall type: ${getWallTypeString(it.type)}")
				buffer.clear()
				UDPCommon.serverWriteWalls(listOf(it), buffer)
				val written = buffer.position()
				buffer.flip()
				UDPCommon.clientReadWalls(r2.wall_lookup, buffer)

				val read = buffer.position()

				assertEquals(written, read)
				assertTrue(r1Walls.contentEquals(r2Walls))

				assertTrue("", r1.deepEquals(r2))
				break


			} while (true)
		}
	}

	@Test
	fun test2() {
		val r1 = TRobo()
		val r2 = TRobo()

		assertEquals(r1, r2)

		for (i in 0 until 10) {
			r1.buildAndPopulateRobocraze()
			assertNotEquals(r1, r2)
			r2.merge(r1.serializeToString())
			serializeObjects(r1, r2)
			assertEquals(r1, r2)
		}
	}


	class TRobo : Robotron() {
		override val imageKey: Int
			get() = TODO("Not yet implemented")
		override val imageLogo: Int
			get() = TODO("Not yet implemented")
		override val animJaws: IntArray
			get() = TODO("Not yet implemented")
		override val animLava: IntArray
			get() = TODO("Not yet implemented")
		override val animPeople: Array<IntArray>
			get() = TODO("Not yet implemented")
		override val clock: Long
			get() = TODO("Not yet implemented")
	}


}