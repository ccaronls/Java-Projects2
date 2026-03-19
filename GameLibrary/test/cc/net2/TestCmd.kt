package cc.net2

import cc.lib.ksp.netcmd.INetCommand
import cc.lib.ksp.netcmd.NetCommand

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
