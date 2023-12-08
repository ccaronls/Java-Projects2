package cc.lib.kreflector

import java.lang.Boolean
import kotlin.Any
import kotlin.String

/**
 * Created by Chris Caron on 12/1/23.
 */
val byteArchiver: Archiver = object : AArchiver() {
	override fun parse(s: String): Any {
		return (if (s == "null") null else s.toByte())!!
	}
}
val integerArchiver: Archiver = object : AArchiver() {
	override fun parse(s: String): Any {
		return (if (s == "null") null else s.toInt())!!
	}
}
val longArchiver: Archiver = object : AArchiver() {
	override fun parse(s: String): Any {
		return (if (s == "null") null else s.toLong())!!
	}
}
val floatArchiver: Archiver = object : AArchiver() {
	override fun parse(s: String): Any {
		return (if (s == "null") null else s.toFloat())!!
	}
}
val doubleArchiver: Archiver = object : AArchiver() {
	override fun parse(s: String): Any {
		return (if (s == "null") null else s.toDouble())!!
	}
}
val booleanArchiver: Archiver = object : AArchiver() {
	override fun parse(s: String): Any {
		return (if (s == "null") null else Boolean.parseBoolean(s))!!
	}
}
val stringArchiver: Archiver = StringArchiver()
val enumArchiver: Archiver = EnumArchiver()
val dirtyArchiver: Archiver = DirtyArchiver<Any?>()
val collectionArchiver: Archiver = CollectionArchiver()
val mapArchiver: Archiver = MapArchiver()
val arrayArchiver: Archiver = ArrayArchiver()
val archivableArchiver: Archiver = ArchivableArchiver()