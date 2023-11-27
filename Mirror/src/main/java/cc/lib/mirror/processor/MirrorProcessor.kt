package cc.lib.mirror.processor

import cc.lib.mirror.annotation.DirtyType
import com.google.devtools.ksp.getClassDeclarationByName
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
		logger.error("cannot get Class Declaration for ${toString()}")
		return false
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
				logger.error("Only interface can be annotated with @Mirror", classDeclaration)
				return
			}

			classDeclaration.superTypes.firstOrNull { it.resolve().isMirrored() } ?: run {
				logger.error("Must have cc.lib.mirror.context.Mirrored as a supertype", classDeclaration)
				return
			}

			val annotation: KSAnnotation = classDeclaration.annotations.first {
				it.shortName.asString() == "Mirror"
			}

			val dirtyType: DirtyType = DirtyType.valueOf((annotation.arguments
				.first { arg -> arg.name?.asString() == "dirtyType" }.value as KSType).toString().substringAfterLast('.'))

			logger.warn("dirtyType = $dirtyType")
			// Getting the list of member properties of the annotated interface.
			val properties: Sequence<KSPropertyDeclaration> = classDeclaration.getAllProperties()
				.filter { it.validate() }

			val methods = classDeclaration.getAllFunctions().filter {
				it.annotations.firstOrNull { it.shortName.asString() == "MirroredFunction" } != null && it.validate()
			}.toList()

			methods.forEach { funcDecl ->
				if (funcDecl.modifiers.contains(Modifier.SUSPEND)) {
					logger.error("Function $funcDecl must not have suspend modifier")
					return@forEach
				}

				val rt = funcDecl.returnType?.resolve()!!
				if (rt.toString() != "Unit" && !rt.isMarkedNullable) {
					logger.error("Function $funcDecl must have Unit or nullable return type")
					return@forEach
				}
			}

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
				when (toString()) {
					"Byte",
					"Short",
					"Int" -> return "0"
					"Long" -> return "0L"
					"Float" -> return "0f"
					"Double" -> return "0.0"
					"Boolean" -> return "false"
					"String" -> return "\"\""
					"Char" -> return "'0'"
					"List<Int>", "List<Int>", "MutableList<Int>" -> return "DirtyListInt()"
					"List<Long>", "MutableList<Long>" -> return "DirtyListLong()"
					"List<Float>", "MutableList<Float>" -> return "DirtyListFloat()"
					"List<Double>", "MutableList<Double>" -> return "DirtyListDouble()"
					"List<String>", "MutableList<String>" -> return "DirtyListString()"
					"List<Char>", "MutableList<Char>" -> return "DirtyListChar()"
					"List<Byte>", "MutableList<Byte>" -> return "DirtyListByte()"
					"List<Short>", "MutableList<Short>" -> return "DirtyListShort()"
					"List<Boolean>", "MutableList<Boolean>" -> return "DirtyListBoolean()"
					"MirroredArray<Int>" -> return "DirtyArrayInt()"
					"MirroredArray<Long>" -> return "DirtyArrayLong()"
					"MirroredArray<Float>" -> return "DirtyArrayFloat()"
					"MirroredArray<Double>" -> return "DirtyArrayDouble()"
					"MirroredArray<String>" -> return "DirtyArrayString()"
					"MirroredArray<Char>" -> return "DirtyArrayChar()"
					"MirroredArray<Byte>" -> return "DirtyArrayByte()"
					"MirroredArray<Short>" -> return "DirtyArrayShort()"
					"MirroredArray<Boolean>" -> return "DirtyArrayBoolean()"
				}
				logger.error("No default type for $this")
				return "error"
			}

			val DIRTY_FLAG_FIELD = "_dirtyFlag"

			fun printDeclarations(i: String): String = StringBuffer().also {
				indent = i
				when (dirtyType) {
					DirtyType.NEVER -> Unit
					DirtyType.COMPLEX -> it.appendLn("private val $DIRTY_FLAG_FIELD = java.util.BitSet(${mapped.size})")
					DirtyType.ANY -> it.append("private var $DIRTY_FLAG_FIELD = false")
				}
				mapped.toList().forEachIndexed { index, pair ->
					val prop = pair.first
					val type = pair.second
					val name = prop.getName()
					if (type.isArray()) {
						logger.error("standard Arrays not supported. Use MirroredArray")
					} else if (type.isList()) {
						val argType = type.arguments[0].type!!.resolve().starProjection()
						logger.warn("list argType: $argType, isEnum=${argType.isEnum()}")
						var field = "DirtyList$argType(value)"
						if (argType.isEnum()) {
							field = "DirtyListEnum($argType.values(), value)"
							it.appendLn("override var $name : $type = DirtyListEnum($argType.values())")
						} else if (argType.isMirrored()) {
							field = "DirtyListMirrored(value)"
							it.appendLn("override var $name : $type = DirtyListMirrored()")
						} else if (argType.isList()) {
							field = "DirtyListList(value)"
							it.appendLn("override var $name : $type = DirtyListList()")
						} else {
							it.appendLn("override var $name : $type = ${type.defaultValue()}")
						}
						when (dirtyType) {
							DirtyType.NEVER -> Unit
							DirtyType.COMPLEX -> {
								it.appendLn("   set(value) {")
								it.appendLn("      if (value != field) $DIRTY_FLAG_FIELD.set($index)")
								it.appendLn("      field = $field")
								it.appendLn("   }")
							}
							DirtyType.ANY -> {
								it.appendLn("   set(value) {")
								it.appendLn("      $DIRTY_FLAG_FIELD = $DIRTY_FLAG_FIELD || (value != field)")
								it.appendLn("      field = value")
								it.appendLn("   }")
							}
						}
					} else {
						it.appendLn("override var $name : $type = ${type.defaultValue()}")
						when (dirtyType) {
							DirtyType.NEVER -> Unit
							DirtyType.COMPLEX -> {
								it.appendLn("   set(value) {")
								it.appendLn("      if (value != field) $DIRTY_FLAG_FIELD.set($index)")
								it.appendLn("      field = value")
								it.appendLn("   }")
							}
							DirtyType.ANY -> {
								it.appendLn("   set(value) {")
								it.appendLn("      $DIRTY_FLAG_FIELD = $DIRTY_FLAG_FIELD || (value != field)")
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
							DirtyType.ANY -> appendLn("if (!dirtyOnly || $DIRTY_FLAG_FIELD || $nm?.isDirty() == true) {")
							DirtyType.COMPLEX -> appendLn("if (!dirtyOnly || $DIRTY_FLAG_FIELD.get($index) || $nm?.isDirty() == true) {")
						}
						appendLns(
							"""
    writer.name("$nm")
	$nm?.let {
	   writer.beginObject()
	   writer.name("type").value(getCanonicalName(it::class.java))
	   writer.name("value")
	   it.toGson(writer)
	   writer.endObject()
	}?:run {
	   writer.nullValue()
	}
}
""")
					} else if (propType.isEnum()) {
						if (propType.nullability != Nullability.NULLABLE) {
							logger.error("enum property $nm must be nullable")
							return@forEachIndexed
						}
						when (dirtyType) {
							DirtyType.NEVER -> Unit
							DirtyType.ANY -> appendLn("if (!dirtyOnly || $DIRTY_FLAG_FIELD)")
							DirtyType.COMPLEX -> appendLn("if (!dirtyOnly || $DIRTY_FLAG_FIELD.get($index))")
						}
						appendLn("   writer.name(\"$nm\").value($nm?.name)")
					} else {
						val value = encode(nm, propType)
						when (dirtyType) {
							DirtyType.NEVER -> Unit
							DirtyType.ANY -> appendLn("if (!dirtyOnly || $DIRTY_FLAG_FIELD)")
							DirtyType.COMPLEX -> appendLn("if (!dirtyOnly || $DIRTY_FLAG_FIELD.get($index))")
						}
						if (propType.isMirroredArray()) {
							appendLn("{   writer.name(\"$nm\")")
							appendLn("   $nm.toGson(writer, dirtyOnly)")
							appendLn("}")
						} else if (propType.isList()) {
							appendLn("{   writer.name(\"$nm\")")
							appendLn("   ($nm as DirtyList).toGson(writer, dirtyOnly)")
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
   $nm?.fromGson(reader)?:run {
	   $nm = (clazz.newInstance() as ${propType.makeNotNullable()}).also {
		  it.fromGson(reader)
	   }
   }
   reader.endObject()
   $nm
}""")
					} else if (propType.isEnum()) {
						appendLns("""
"$nm" -> $nm = checkForNullOr(reader, null) {
   enumValueOf<${propType.makeNotNullable()}>(reader.nextString())
}""")
					} else if (propType.isList()) {
						appendLns("""
"$nm" -> ($nm as DirtyList).fromGson(reader)""")
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
						appendLn("($nm as DirtyList).toString(buffer, \"\$indent\$INDENT\")")
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
						appendLn("$DIRTY_FLAG_FIELD = false")
					}
					DirtyType.COMPLEX -> {
						appendLn("$DIRTY_FLAG_FIELD.clear()")
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
						appendLn("if ($DIRTY_FLAG_FIELD) return true")
						mapped.filter { it.value.isMirrored() || it.value.isList() }.keys.map {
							appendLn("if ((${it.getName()} as Mirrored?)?.isDirty() == true)")
							appendLn("   $DIRTY_FLAG_FIELD = true")
						}
						appendLn("return $DIRTY_FLAG_FIELD")
					}
					DirtyType.COMPLEX -> {
						append("${indent}return !$DIRTY_FLAG_FIELD.isEmpty")
						mapped.filter { it.value.isList() }.keys.map {
							appendLn("    || (${it.getName()} as DirtyList<*>?)?.isDirty() == true")
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
					if (it.value.isList() || it.value.isMirrored()) {
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
		override fun executeLocally(function: String, reader: JsonReader) {
			when (function) {""")
					methods.forEach { funcDecl ->
						appendLns("""
				"$funcDecl" -> {
					$funcDecl(""")
						var comma = false
						funcDecl.parameters.forEach {
							if (comma) {
								append(",")
							} else {
								comma = true
							}
							val t = it.type.resolve()!!
							if (t.isMirroredArray()) {
								TODO()
							} else if (t.isMirrored()) {
								val bangbang = if (t.isMarkedNullable) "" else "!!"
								appendLns("""
				   checkForNullOr(reader, null) { reader ->
				      reader.beginObject()
					  reader.nextName("type")
					  (getClassForName(reader.nextString()).newInstance() as $t).apply {
					     reader.nextName("value")
						 fromGson(reader)
						 reader.endObject()
					  }
				   }$bangbang
""")
							} else if (t.isEnum()) {
								TODO()
							} else if (t.isList()) {
								TODO()
							} else {
								appendLns("""
					reader.${t.gsonTypeName()}()""")
							}
						}
						appendLns("""
					)
				 }
""")
					}

					appendLns("""}
		}
	}
""")
					methods.forEach { funcDecl ->
						val rt = funcDecl.returnType?.resolve() ?: "Unit"
						val parameters: String = funcDecl.parameters.map { param ->
							logger.warn("param: $param")
							val nullability = if (param.type.resolve().isMarkedNullable) "?" else ""
							"$param : ${param.type}$nullability"
						}.joinToString(",")
						appendLns("""
	override fun $funcDecl($parameters) : $rt {
		_functionDelegate.executor?.let {
			with(it.start("$funcDecl")) {
""")
						funcDecl.parameters.forEach {
							if (it.type.resolve().isMirrored()) {
								appendLns("""
				$it?.let {
					beginObject()
					name("type").value(getCanonicalName($it::class.java))
					name("value")
					$it.toGson(this, false)
					endObject()
				}?:run {
					nullValue()
				}""")
							} else {
								appendLns("""
				value($it)""")
							}
						}
						appendLns("""
			}
			it.end()
		}
	}""")
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
				
abstract class $classTypeName() : MirrorImplBase(), $classDeclaration {				

${printDeclarations("    ")}

	override fun toGson(writer: JsonWriter, dirtyOnly: Boolean) {
		writer.beginObject()
${printJsonWriterContent("        ")}		
		writer.endObject()
	}
	
	override fun fromGson(reader: JsonReader) {
		reader.beginObject()
		while (reader.hasNext()) {
			when (reader.nextName()) {
${printJsonReaderContent("              ")}		
				else -> reader.skipValue()
			}
		}
		reader.endObject()
	}
	
	override fun markClean() {
${printMarkCleanContent("      ")}
	}
	
	override fun isDirty() : Boolean {
${printIsDirtyContent("      ")}
	}
	
	override fun toString(buffer: StringBuffer, indent: String) {
${printToStringContent("      ")}
	}
	
	override fun contentEquals(other : Any?) : Boolean {
		if (other == null) return false
		if (other !is $classDeclaration) return false
${printIsEqualsContent("        ")}
		return true
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
}
