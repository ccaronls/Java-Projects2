package cc.net2

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.ksp.netcmd.NetCommand

enum class TestEnum {
	ONE,
	TWO,
	THREE
}

@NetCommand
interface TestCmd : INetCommand {
	val a: Int
	val b: Float
	val c: String
	val d: Double
	val e: Long
	val f: ByteArray
	val g: Byte
	val h: Short
	val i: UShort
	val j: ULong
	val k: UInt
	val l: Boolean
	val m: UByte
	val n: TestEnum

	val aa: Int?
	val bb: Float?
	val cc: String?
	val dd: Double?
	val ee: Long?
	val ff: ByteArray?
	val gg: Byte?
	val hh: Short?
	val ii: UShort?
	val jj: ULong?
	val kk: UInt?
	val ll: Boolean?
	val mm: UByte?
	val nn: TestEnum?

}

@NetCommand
interface TestCmdSmall : INetCommand {
	val v: String
}

@NetCommand
interface TestCmdNullable : INetCommand {
	val a: String?
	val b: Int?
	val c: ByteArray?
}
