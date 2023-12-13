package cc.lib.mirror.processor

import cc.lib.mirror.annotation.DirtyType
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.OutputStream

/**
 * Created by Chris Caron on 11/14/23.
 */
class MirrorProcessor(
	private val codeGenerator: CodeGenerator,
	private val logger: KSPLogger,
	private val options: Map<String, String>
) : SymbolProcessor {

	lateinit var resolver: Resolver

	val listType by lazy {
		resolver.getClassDeclarationByName("kotlin.collections.List")!!.asStarProjectedType()
	}

	val mapType by lazy {
		resolver.getClassDeclarationByName("kotlin.collections.Map")!!.asStarProjectedType()
	}

	val mirrorType by lazy {
		resolver.getClassDeclarationByName(
			"cc.lib.mirror.context.Mirrored")!!.asStarProjectedType().makeNullable()
	}

	val mirroredArrayType by lazy {
		resolver.getClassDeclarationByName(
			"cc.lib.mirror.context.MirroredArray")!!.asStarProjectedType().makeNullable()
	}

	val arrayType by lazy {
		resolver.getClassDeclarationByName("kotlin.Array")!!.asStarProjectedType()
	}

	fun KSType.isMirrored(): Boolean {
		return mirrorType.isAssignableFrom(this)
	}

	fun KSType.isList(): Boolean {
		return listType.isAssignableFrom(this)
	}

	fun KSType.isMap(): Boolean {
		return mapType.isAssignableFrom(this)
	}

	fun KSType.isArray(): Boolean {
		return arrayType.isAssignableFrom(this)
	}

	fun KSType.isMirroredArray(): Boolean {
		return mirroredArrayType.isAssignableFrom(this)
	}

	fun KSType.isEnum(): Boolean {
		resolver.getClassDeclarationByName(this.declaration.qualifiedName!!)?.let {
			return it.classKind == ClassKind.ENUM_CLASS
		}
		throw Exception("cannot get Class Declaration for ${toString()}")
	}

	fun KSType.isUnit(): Boolean {
		return toString() == "Unit"
	}

	fun getClassFileName(symbol: String): String {
		if (symbol.startsWith("I"))
			return symbol.substring(1) + "Impl"
		return symbol + "Impl"
	}

	fun OutputStream.print(s: String) {
		write(s.toByteArray())
	}

	fun KSPropertyDeclaration.getName(): String = simpleName.asString()

//	fun KSPropertyDeclaration.getDirtyName(): String = "_${simpleName.asString()}DirtyFlag"

	inner class Visitor(private val file: OutputStream) : KSVisitorVoid() {

		override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
			logger.warn("class declaration: $classDeclaration : ${
				classDeclaration.superTypes.map {
					it.resolve().toString()
				}.joinToString()
			}")
			if (classDeclaration.classKind != ClassKind.INTERFACE) {
				throw Exception("Invalid $classDeclaration, Only interface can be annotated with @Mirror")
			}

			logger.warn("$classDeclaration subClasses: ${classDeclaration.superTypes.map { it.resolve() }.joinToString()}")

			val baseMirrorClass: KSType = classDeclaration.superTypes.firstOrNull { it.resolve().isMirrored() }?.resolve()
				?: run {
					throw Exception("Must have cc.lib.mirror.context.Mirrored as a supertype $classDeclaration")
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

			val methods = classDeclaration.getAllFunctions().filter { decl ->
				decl.annotations.firstOrNull { it.shortName.asString() == "MirroredFunction" } != null && decl.validate()
			}.toList()

			methods.forEach { funcDecl ->
				if (!funcDecl.modifiers.contains(Modifier.SUSPEND)) {
					throw Exception("Function $funcDecl must have suspend modifier")
				}

				val rt = funcDecl.returnType?.resolve()!!
				if (!rt.isUnit() && !rt.isMarkedNullable) {
					throw Exception("Function $funcDecl must have Unit or nullable return type")
				}

				funcDecl.parameters.forEach {
					with(it.type.resolve()) {
						if (isMirrored() && !isMarkedNullable) {
							throw Exception("Mirrored function params must be nullable")
						}
					}
				}
			}

			val filteredMethods = methods.map {
				it to Pair(it.parameters.map {
					Pair(it.name!!, it.type.resolve())
				}.toList(), it.returnType!!.resolve())
			}.toMap()

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

			fun printDeclarations(i: String): String = StringBuffer().also {
				indent = i
				when (dirtyType) {
					DirtyType.NEVER -> Unit
					DirtyType.COMPLEX -> it.appendLn("private val $dirtyFlagFieldName = java.util.BitSet(${mapped.size})")
					DirtyType.ANY -> it.append("private var $dirtyFlagFieldName = false")
				}
				mapped.toList().forEachIndexed { index, pair ->
					val prop = pair.first
					val type = pair.second
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
								it.appendLn("      field = value.toMirroredList()")
								it.appendLn("   }")
							}
							DirtyType.ANY -> {
								it.appendLn("   set(value) {")
								it.appendLn("      $dirtyFlagFieldName = $dirtyFlagFieldName || (value != field)")
								it.appendLn("      field = value.toMirroredList()")
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
						if (propType.isMirrored() || propType.isList() || propType.isMap()) {
							appendLn("{   writer.name(\"$nm\")")
							appendLn("   ($nm as Mirrored).toGson(writer, dirtyOnly)")
							appendLn("}")
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
						appendLns("""
"$nm" -> ($nm as MirrorMap).fromGson(reader)""")
					} else {
						appendLns("""
"$nm" -> $nm = checkForNullOr(reader, ${propType.defaultValue()}) {
   reader.${propType.gsonTypeName()}()
}""")
					}
				}
			}.toString()

			fun printToStringContent(i: String): String = StringBuffer().apply {
				indent = i
				mapped.forEach { (prop, propType) ->
					val nm = prop.simpleName.asString()
					if (propType.isMirrored()) {
						appendLn("buffer.append(indent).append(\"$nm:\")")
						appendLn("if ($nm == null) buffer.append(\"null\")")
						appendLn("else {")
						appendLn("   buffer.append(\"{\\n\")")
						appendLn("   $nm!!.toString(buffer, \"\$indent\$INDENT\")")
						appendLn("   buffer.append(indent).append(\"}\\n\")")
						appendLn("}")
					} else if (propType.isList()) {
						appendLn("buffer.append(indent).append(\"$nm \")")
						appendLn("($nm as MirroredList).toString(buffer, \"\$indent\$INDENT\")")
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
				mapped.filter { it.value.isMirrored() || it.value.isList() }.keys.forEach {
					appendLn("(${it.getName()} as Mirrored?)?.markClean()")
				}
			}.toString()

			fun printIsDirtyContent(i: String): String = StringBuffer().apply {
				indent = i
				when (dirtyType) {
					DirtyType.NEVER -> appendLn("return false")
					DirtyType.ANY -> {
						appendLn("if ($dirtyFlagFieldName) return true")
						mapped.filter { it.value.isMirrored() || it.value.isList() }.keys.map {
							appendLn("if ((${it.getName()} as Mirrored?)?.isDirty() == true)")
							appendLn("   $dirtyFlagFieldName = true")
						}
						appendLn("return $dirtyFlagFieldName")
					}
					DirtyType.COMPLEX -> {
						append("${indent}return !$dirtyFlagFieldName.isEmpty")
						mapped.filter { it.value.isList() }.keys.map {
							appendLn("    || (${it.getName()} as MirroredList<*>?)?.isDirty() == true")
						}
						mapped.filter { it.value.isMirrored() }.keys.map {
							appendLn("    || ${it.getName()}?.isDirty() == true")
						}
						append("\n")
					}
				}
			}.toString()

			fun printIsEqualsContent(i: String): String = StringBuffer().apply {
				indent = i
				mapped.forEach {
					val name = it.key.getName()
					if (it.value.isList() || it.value.isMirrored() || it.value.isMap()) {
						appendLn("if (!isContentsEquals($name as Mirrored?, other.$name)) return false")
					} else {
						appendLn("if ($name != other.$name) return false")
					}
				}
			}.toString()

			fun printMethods(i: String): String = StringBuffer().apply {
				indent = i
				if (methods.isNotEmpty()) {
					appendLns("""
	override fun getFunctionDelegate() : FunctionDelegate? = _functionDelegate

	private val _functionDelegate = object : FunctionDelegate() {
		override suspend fun executeLocally(function: String, reader: JsonReader) : Boolean {
			return when (function) {""")
					filteredMethods.forEach { funcDecl, params ->
						appendLns("""
				"$funcDecl" -> {
					$funcDecl(""")
						var comma = false
						// TODO: Simplify this
						funcDecl.parameters.forEach {
							if (comma) {
								append(",")
							} else {
								comma = true
							}
							val t = it.type.resolve()
							if (t.isMirroredArray()) {
								TODO()
							} else if (t.isMirrored()) {
								appendLns("""
				   checkForNullOr(reader, null) { reader ->
				      readMirrored<${t.toString().trimEnd('?')}>(null, reader)
				   }""")
							} else if (t.isEnum()) {
								TODO()
							} else if (t.isList()) {
								TODO()
							} else {
								appendLns("""
					reader.${t.gsonTypeName()}()""")
							}
						}
						append(")")

						if (!params.second.isUnit()) {
							val serializeResultStmt = if (params.second.isMirrored()) {
								"writeMirrored(result, this, false)"
							} else {
								"value(result)"
							}

							appendLns(
								""".also { result ->
						getFunctionDelegate()?.serializerFactory?.newResponseSerializer()?.let { serializer ->
						    serializer.start("$funcDecl").apply {

							    if (result == null) {
								    nullValue()
							    } else {
								    $serializeResultStmt
							    }

							    }
							    serializer.end("$funcDecl")
					        }
					}""")
						}


						appendLns("""
					true
				 }
""")
					}

					appendLns("""
				else -> super.executeLocally(function, reader)
			}
		}
	}
""")
					filteredMethods.forEach { funcDecl, types ->
						val rt = funcDecl.returnType?.resolve() ?: "Unit"
						val parameters: String = types.first.map {
							//	logger.warn("param: $param")
							//	val nullability = if (param.type.resolve().isMarkedNullable) "?" else ""
							//	"$param : ${param.type}$nullability"
							"${it.first.asString()} : ${it.second}"
						}.joinToString(",")
						appendLns("""
	override suspend fun $funcDecl($parameters) : $rt {
		getFunctionDelegate()?.let { delegate ->
		    delegate.serializerFactory?.newFunctionSerializer()?.let { serializer ->
				serializer.start("$funcDecl").apply {""")
						types.first.forEach {
							val name = it.first.asString()
							if (it.second.isMirrored()) {
								appendLns("""
					if ($name == null) {
						nullValue()
					} else {
						writeMirrored($name, this, false)
					}""")
							} else {
								appendLns("""
					value($name)""")
							}
						}
						appendLns("""
				}
				serializer.end("$funcDecl")
			}""")
						if (!types.second.isUnit()) {
							appendLns("""
		    return delegate.waitForResponse("doSomethingAndReturn") {
				it.${types.second.gsonTypeName()}()
		    }
		}
		return null
	}""")
						} else {
							appendLns("""
		}
	}""")
						}
					}
				}
			}.toString()

			val classTypeName = getClassFileName(classDeclaration.toString())
			file.print(
				"""				
package ${classDeclaration.packageName.asString()}
				
import com.google.gson.*
import com.google.gson.stream.*
import cc.lib.mirror.context.*
import kotlinx.coroutines.*
				
abstract class $classTypeName() : ${baseDeclaration}(), $classDeclaration {				

${printDeclarations("    ")}

	override fun toGson(writer: JsonWriter, dirtyOnly: Boolean) {
${printJsonWriterContent("     ")}
	  super<$baseDeclaration>.toGson(writer, dirtyOnly)
	}
	
	override fun fromGson(reader: JsonReader, name : String) {
	   when (name) {
${printJsonReaderContent("         ")}		
		  else -> super<$baseDeclaration>.fromGson(reader, name)
	   }
	}
	
	override fun markClean() {
	   super<$baseDeclaration>.markClean()
${printMarkCleanContent("      ")}
	}
	
	override fun isDirty() : Boolean {
${printIsDirtyContent("      ")}
		 || super<$baseDeclaration>.isDirty()
	}
	
	override fun toString(buffer: StringBuffer, indent: String) {
${printToStringContent("      ")}
       super<$baseDeclaration>.toString(buffer, indent)
	}
	
	override fun contentEquals(other : Any?) : Boolean {
		if (other == null) return false
		if (other !is $classDeclaration) return false
${printIsEqualsContent("        ")}
		return super<$baseDeclaration>.contentEquals(other)
	}
	
${printMethods("   ")}
}				
""")
		}
	}

	override fun process(resolver: Resolver): List<KSAnnotated> {
		this.resolver = resolver
		val symbols = resolver
			.getSymbolsWithAnnotation("cc.lib.mirror.annotation.Mirror")
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
			"(Mutable|Mirrored)?List<(.+)>" to "listOf<$2>().toMirroredList()",
			"(Mutable|Mirrored)?Map<(.+)>" to "mapOf<$2>().toMirroredMap()",
			"MirroredArray<(.*)>" to "arrayOf<$1>().toMirroredArray()"
		).map {
			it.key.toRegex() to it.value
		}

		fun match(value: String): String {
			val options = defaultValueRegExMap.map {
				it.first.matchEntire(value) to it.second
			}.filter { it.first != null }

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
