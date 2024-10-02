package cc.lib.mirror.processor

import cc.lib.ksp.helper.BaseProcessor
import cc.lib.ksp.helper.print
import cc.lib.ksp.mirror.DirtyType
import cc.lib.ksp.mirror.Mirror
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.validate
import java.io.OutputStream

/**
 * Created by Chris Caron on 11/14/23.
 */
class MirrorProcessor(
	codeGenerator: CodeGenerator,
	logger: KSPLogger,
	options: Map<String, String>
) : BaseProcessor(codeGenerator, logger, options) {

	override lateinit var resolver: Resolver

	fun getClassFileName(symbol: String): String {
		if (symbol.startsWith("I"))
			return symbol.substring(1) + "Impl"
		return symbol + "Impl"
	}

	fun KSType.isMirroredOrStructure() = isMirrored() || isList() || isMap()

	fun KSPropertyDeclaration.getName(): String = simpleName.asString()

//	fun KSPropertyDeclaration.getDirtyName(): String = "_${simpleName.asString()}DirtyFlag"

	inner class Visitor(private val file: OutputStream) : KSVisitorVoid() {

		override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
			logger.warn(
				"class declaration: $classDeclaration : ${
					classDeclaration.superTypes.map {
						it.resolve().toString()
					}.joinToString()
				}"
			)
			if (classDeclaration.classKind != ClassKind.INTERFACE) {
				throw Exception("$classDeclaration must be an interface")
			}

			if (classDeclaration.typeParameters.isNotEmpty()) {
				throw Exception("$classDeclaration cannot have template parameters")
			}

			logger.warn("$classDeclaration subClasses: ${classDeclaration.superTypes.map { it.resolve() }.joinToString()}")

			val baseMirrorClass: KSType = classDeclaration.superTypes.firstOrNull { it.resolve().isMirrored() }?.resolve()
				?: run {
					throw Exception("$classDeclaration must have cc.lib.mirror.context.Mirrored as one if its supertypes")
				}

			val baseDeclaration = getClassFileName(baseMirrorClass.toString())

			logger.warn("baseMirrorClass: $baseMirrorClass: $baseDeclaration")

			val annotation: KSAnnotation = classDeclaration.annotations.first {
				it.shortName.asString() == "Mirror"
			}

			val dirtyType: DirtyType = DirtyType.valueOf((annotation.arguments
				.first { arg -> arg.name?.asString() == "dirtyType" }.value as KSType).toString().substringAfterLast('.'))

			logger.warn("dirtyType = $dirtyType")
			// Getting the list of member properties of the annotated interface.
			val properties: Sequence<KSPropertyDeclaration> = classDeclaration.getDeclaredProperties()
				.filter { it.validate() }

			val mapped = properties.map { Pair(it, it.type.resolve()) }.toMap()
			var indent = ""

			fun StringBuffer.appendLn(txt: String) {
				if (isNotEmpty())
					append("\n")
				append(indent).append(txt)
			}

			fun StringBuffer.appendLns(txt: String) {
				append(indent).append(txt.replace("\n", "\n$indent"))
			}

			fun KSType.gsonTypeName(): String {
				return "next" + when (val nm = toString().trimEnd('?')) {
					"Float" -> "Double().toFloat"
					"Short" -> "Int().toShort"
					"Byte" -> "Int().toByte"
					"Char" -> "String().first"
					else -> nm
				}
			}

			fun KSType.defaultValue(): String {
				if (nullability == Nullability.NULLABLE)
					return "null"
				return match(toString()).also {
					logger.warn("Matched ${toString()} to $it")
				}
			}

			val dirtyFlagFieldName = "_dirtyFlag"
			val classTypeName = getClassFileName(classDeclaration.toString())

			fun printDeclarations(i: String): String = StringBuffer().also {
				indent = i
				when (dirtyType) {
					DirtyType.NEVER -> Unit
					DirtyType.COMPLEX -> it.appendLn("private val $dirtyFlagFieldName = java.util.BitSet(${mapped.size})")
					DirtyType.ANY -> it.appendLn("private var $dirtyFlagFieldName = false")
				}
				mapped.toList().forEachIndexed { index, pair ->
					val prop = pair.first
					val type = pair.second
					val nullable = if (type.nullability == Nullability.NULLABLE) "?" else ""
					val name = prop.getName()
					if (type.isArray()) {
						throw Exception("standard Arrays not supported. Use MirroredArray")
					} else if (type.isList()) {
						it.appendLn("override var $name : $type = ${type.defaultValue()}")
						when (dirtyType) {
							DirtyType.NEVER -> Unit
							DirtyType.COMPLEX -> {
								it.appendLn("   set(value) {")
								it.appendLn("      if (value != field) $dirtyFlagFieldName.set($index)")
								it.appendLn("      field = value$nullable.toMirroredList()")
								it.appendLn("   }")
							}

							DirtyType.ANY -> {
								it.appendLn("   set(value) {")
								it.appendLn("      $dirtyFlagFieldName = $dirtyFlagFieldName || (value != field)")
								it.appendLn("      field = value$nullable.toMirroredList()")
								it.appendLn("   }")
							}
						}
					} else if (type.isMap()) {
						it.appendLn("override var $name : $type = ${type.defaultValue()}")
						when (dirtyType) {
							DirtyType.NEVER -> Unit
							DirtyType.COMPLEX -> {
								it.appendLn("   set(value) {")
								it.appendLn("      if (value != field) $dirtyFlagFieldName.set($index)")
								it.appendLn("      field = value$nullable.toMirroredMap()")
								it.appendLn("   }")
							}

							DirtyType.ANY -> {
								it.appendLn("   set(value) {")
								it.appendLn("      $dirtyFlagFieldName = $dirtyFlagFieldName || (value != field)")
								it.appendLn("      field = value$nullable.toMirroredMap()")
								it.appendLn("   }")
							}
						}
					} else {
						it.appendLn("override var $name : $type = ${type.defaultValue()}")
						when (dirtyType) {
							DirtyType.NEVER -> Unit
							DirtyType.COMPLEX -> {
								it.appendLn("   set(value) {")
								it.appendLn("      if (value != field) $dirtyFlagFieldName.set($index)")
								it.appendLn("      field = value")
								it.appendLn("   }")
							}

							DirtyType.ANY -> {
								it.appendLn("   set(value) {")
								it.appendLn("      $dirtyFlagFieldName = $dirtyFlagFieldName || (value != field)")
								it.appendLn("      field = value")
								it.appendLn("   }")
							}
						}
					}
				}
			}.toString()

			fun printJsonWriterContent(i: String) = StringBuffer().apply {

				fun encode(s: String, type: KSType): String = when (type.toString()) {
					"Char" -> "\"$$s\""
					else -> s
				}

				indent = i
				mapped.toList().forEachIndexed { index, pair ->
					val prop = pair.first
					val propType = pair.second
					val nm = prop.simpleName.asString()
					if (propType.isMirrored() && !propType.isMirroredArray()) {
						when (dirtyType) {
							DirtyType.NEVER -> appendLn("if (true) {")
							DirtyType.ANY -> appendLn("if (!dirtyOnly || $dirtyFlagFieldName || $nm?.isDirty() == true) {")
							DirtyType.COMPLEX -> appendLn("if (!dirtyOnly || $dirtyFlagFieldName.get($index) || $nm?.isDirty() == true) {")
						}
						appendLns(
							"""
    writer.name("$nm")
	writeMirrored($nm, writer, dirtyOnly)
}
""")
					} else if (propType.isEnum()) {
						if (propType.nullability != Nullability.NULLABLE) {
							throw Exception("enum property $nm must be nullable")
						}
						when (dirtyType) {
							DirtyType.NEVER -> Unit
							DirtyType.ANY -> appendLn("if (!dirtyOnly || $dirtyFlagFieldName)")
							DirtyType.COMPLEX -> appendLn("if (!dirtyOnly || $dirtyFlagFieldName.get($index))")
						}
						appendLn("   writer.name(\"$nm\").value($nm?.name)")
					} else {
						val value = encode(nm, propType)
						when (dirtyType) {
							DirtyType.NEVER -> Unit
							DirtyType.ANY -> appendLn("if (!dirtyOnly || $dirtyFlagFieldName)")
							DirtyType.COMPLEX -> appendLn("if (!dirtyOnly || $dirtyFlagFieldName.get($index))")
						}
						if (propType.isMirroredOrStructure()) {
							appendLn("{   writer.name(\"$nm\")")
							appendLn("   ($nm as Mirrored).toGson(writer, dirtyOnly)")
							appendLn("}")
						} else if (propType.isIData()) {
							appendLns(
								""" 
{ 
	writer.name("$nm")
	if ($value == null) 
		writer.nullValue() 
	else 
		writer.value(Json.encodeToString(${propType.makeNotNullable()}.serializer(), $nm!!))
}"""
							)
						} else {
							appendLn("   writer.name(\"$nm\").value($value)")
						}
					}
				}
			}.toString()

			fun printJsonReaderContent(i: String) = StringBuffer().apply {

				indent = i
				mapped.forEach { (prop, propType) ->
					val nm = prop.simpleName.asString()
					if (propType.isMirroredArray()) {
						appendLns("""
"$nm" -> $nm.fromGson(reader)""")
					} else if (propType.isMirrored()) {
						appendLns("""
"$nm" -> $nm = checkForNullOr(reader, null) { reader ->
   reader.beginObject()
   reader.nextName("type")
   val clazz = getClassForName(reader.nextString())
   reader.nextName("value")
   reader.beginObject()
   val obj = $nm?:clazz.newInstance() as ${propType.makeNotNullable()}
   while (reader.hasNext()) {
      obj.fromGson(reader, reader.nextName())
   }
   reader.endObject()
   reader.endObject()
   obj
}""")
					} else if (propType.isEnum()) {
						appendLns("""
"$nm" -> $nm = checkForNullOr(reader, null) {
   enumValueOf<${propType.makeNotNullable()}>(reader.nextString())
}""")
					} else if (propType.isList()) {
						appendLns("""
"$nm" -> ($nm as MirroredList).fromGson(reader)""")
					} else if (propType.isMap()) {
						appendLns(
							"""
"$nm" -> ($nm as MirroredMap).fromGson(reader)"""
						)
					} else if (propType.isIData()) {
						appendLns(
							"""
"$nm" -> $nm = checkForNullOr(reader, null) {
   Json.decodeFromString(reader.nextString())
}"""
						)
					} else {
						appendLns(
							"""
"$nm" -> $nm = checkForNullOr(reader, ${propType.defaultValue()}) {
   reader.${propType.gsonTypeName()}()
}"""
						)
					}
				}
			}.toString()

			fun printToStringContent(i: String): String = StringBuffer().apply {
				indent = i
				mapped.forEach { (prop, propType) ->
					val nm = prop.simpleName.asString()
					if (propType.isList()) {
						appendLn("buffer.append(indent).append(\"$nm \")")
						appendLn("($nm as MirroredList).toString(buffer, \"\$indent\$INDENT\")")
					} else if (propType.isMap()) {
						appendLn("buffer.append(indent).append(\"$nm \")")
						appendLn("($nm as MirroredMap).toString(buffer, \"\$indent\$INDENT\")")
					} else if (propType.isMirroredArray()) {
						appendLn("buffer.append(indent).append(\"$nm \")")
						appendLn("($nm as MirroredArray).toString(buffer, \"\$indent\$INDENT\")")
					} else if (propType.isMirrored()) {
						appendLn("buffer.append(indent).append(\"$nm:\")")
						appendLn("$nm?.let {")
						appendLn("   buffer.append(\"{\\n\")")
						appendLn("   it.toString(buffer, \"\$indent\$INDENT\")")
						appendLn("   buffer.append(indent).append(\"}\\n\")")
						appendLn("}?:buffer.append(\"null\")")
					} else {
						appendLn("buffer.append(indent).append(\"$nm:$$nm\\n\")")
					}
				}
			}.toString()

			fun printMarkCleanContent(i: String): String = StringBuffer().apply {
				indent = i
				when (dirtyType) {
					DirtyType.NEVER -> Unit
					DirtyType.ANY -> {
						appendLn("$dirtyFlagFieldName = false")
					}

					DirtyType.COMPLEX -> {
						appendLn("$dirtyFlagFieldName.clear()")
					}
				}

				mapped.filter { it.value.isMirroredOrStructure() }.keys.forEach {
					appendLn("(${it.getName()} as Mirrored?)?.markClean()")
				}
			}.toString()

			fun printIsDirtyContent(i: String): String = StringBuffer().apply {
				indent = i
				when (dirtyType) {
					DirtyType.NEVER -> appendLn("return false")
					DirtyType.ANY -> {
						appendLn("if ($dirtyFlagFieldName) return true")
						mapped.filter { it.value.isMirroredOrStructure() }.keys.map {
							appendLn("if ((${it.getName()} as Mirrored?)?.isDirty() == true)")
							appendLn("   $dirtyFlagFieldName = true")
						}
						appendLn("return $dirtyFlagFieldName")
					}
					DirtyType.COMPLEX -> {
						append("${indent}return !$dirtyFlagFieldName.isEmpty")
						mapped.entries.forEachIndexed { index, entry ->
							if (entry.value.isList()) {
								appendLn("    || ((${entry.key.getName()} as MirroredList<*>?)?.isDirty() == true).also {")
								appendLn("      $dirtyFlagFieldName[$index] = it }")
							} else if (entry.value.isMap()) {
								appendLn("    || ((${entry.key.getName()} as MirroredMap<*,*>?)?.isDirty() == true).also {")
								appendLn("      $dirtyFlagFieldName[$index] = it }")
							} else if (entry.value.isMirroredArray()) {
								appendLn("    || ((${entry.key.getName()} as MirroredArray<*>?)?.isDirty() == true).also {")
								appendLn("      $dirtyFlagFieldName[$index] = it }")
							} else if (entry.value.isMirrored()) {
								appendLn("    || (${entry.key.getName()}?.isDirty() == true).also {")
								appendLn("      $dirtyFlagFieldName[$index] = it }")
							}
						}
						append("\n")
					}
				}
			}.toString()

			fun printIsEqualsContent(i: String): String = StringBuffer().apply {
				indent = i
				mapped.forEach {
					val name = it.key.getName()
					if (it.value.isMirroredOrStructure() || it.value.isArray()) {
						appendLn("if (!isContentsEquals($name as Mirrored?, other.$name)) return false")
					} else {
						appendLn("if ($name != other.$name) return false")
					}
				}
			}.toString()

			fun printCopyFromContent(i: String): String = StringBuffer().apply {
				indent = i
				appendLn("(other as $classTypeName).let {")
				mapped.forEach {
					val name = it.key.getName()
					if (it.value.isMirrored() || it.value.isList() || it.value.isMap()) {
						val nullable = it.value.getNullable()
						appendLn("\t$name = (other.$name as Mirrored$nullable)$nullable.deepCopy()")
					} else {
						appendLn("\t$name = other.$name")
					}
				}
				appendLn("}")
				appendLn("super<$baseDeclaration>.copyFrom(other)")
			}.toString()

			file.print(
				"""				
package ${classDeclaration.packageName.asString()}
				
import com.google.gson.*
import com.google.gson.stream.*
import cc.lib.ksp.mirror.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
				
abstract class $classTypeName() : $baseDeclaration(), $classDeclaration {				

${printDeclarations("\t")}

	override fun toGson(writer: JsonWriter, dirtyOnly: Boolean) {
${printJsonWriterContent("\t\t")}
	  super<$baseDeclaration>.toGson(writer, dirtyOnly)
	}
	
	override fun fromGson(reader: JsonReader, name : String) {
	   when (name) {
${printJsonReaderContent("\t\t")}		
		  else -> super<$baseDeclaration>.fromGson(reader, name)
	   }
	}
	
	override fun markClean() {
	   super<$baseDeclaration>.markClean()
${printMarkCleanContent("\t\t")}
	}
	
	override fun isDirty() : Boolean {
${printIsDirtyContent("\t\t")}
		 || super<$baseDeclaration>.isDirty()
	}
	
	override fun toString(buffer: StringBuffer, indent: String) {
${printToStringContent("\t\t")}
       super<$baseDeclaration>.toString(buffer, indent)
	}
	
	override fun contentEquals(other : Any?) : Boolean {
		if (other == null) return false
		if (other !is $classDeclaration) return false
${printIsEqualsContent("\t\t")}
		return super<$baseDeclaration>.contentEquals(other)
	}
		
	override fun <T> copyFrom(other : T) {
${printCopyFromContent("\t\t")}
	}
}				
"""
			)
		}
	}

	/*

	override fun copy(other : Any) {
		if (other !is $classDeclaration)
			throw IllegalArgumentException("Call only copy classes of type $classDeclaration")
${printCopyContent("          ")}
		super<$baseDeclaration>.copy(other)
	}

	 */

	override fun process(resolver: Resolver): List<KSAnnotated> {
		this.resolver = resolver
		val symbols = resolver
			.getSymbolsWithAnnotation(Mirror::class.qualifiedName!!)
			.filterIsInstance<KSClassDeclaration>().toMutableList()

		logger.warn("options=$options")
		logger.warn("symbols=${symbols.joinToString()}")

		if (symbols.isEmpty()) return emptyList()

		val symbol = symbols.removeAt(0)

		val file = codeGenerator.createNewFile(
			// Make sure to associate the generated file with sources to keep/maintain it across incremental builds.
			// Learn more about incremental processing in KSP from the official docs:
			// https://kotlinlang.org/docs/ksp-incremental.html
			dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray()),
			packageName = options["package"] ?: "cc.mirror.impl",
			fileName = getClassFileName(symbol.simpleName.asString())
		)
		symbol.accept(Visitor(file), Unit)
		file.close()
		return symbols
	}

	companion object {
		val defaultValueRegExMap = mapOf(
			"Byte" to "0",
			"Short" to "0",
			"Int" to "0",
			"Long" to "0L",
			"Float" to "0f",
			"Double" to "0.0",
			"Boolean" to "false",
			"String" to "\"\"",
			"Char" to "'0'",
			"(Mutable|Mirrored)?List<(in|out)?(.+)>" to "listOf<$3>().toMirroredList()",
			"(Mutable|Mirrored)?Map<(in|out)?(.+)>" to "mapOf<$3>().toMirroredMap()",
			"MirroredArray<(in|out)?(.*)>" to "arrayOf<$2>().toMirroredArray()"
		).map {
			it.key.toRegex() to it.value
		}

		fun match(value: String): String {
			val options = defaultValueRegExMap.map {
				it.first.matchEntire(value) to it.second
			}.filter { it.first != null }

			if (options.isEmpty())
				throw Exception("Expecting 1 option for $value but found none")
			if (options.size != 1) {
				throw Exception("Expecting 1 option for $value but found: ${options.joinToString { it.first!!.value }}")
			}

			val matcher = options[0].first!!
			var result = options[0].second
			for (idx in 1 until matcher.groupValues.size) {
				result = result.replace("$$idx", matcher.groupValues[idx])
			}

			return result
		}
	}
}
