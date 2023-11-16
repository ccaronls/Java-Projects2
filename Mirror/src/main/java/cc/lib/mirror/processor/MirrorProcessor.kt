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

	val collectionType by lazy {
		resolver.getClassDeclarationByName("java.util.Collection")!!.asStarProjectedType()
	}

	val mirrorType by lazy {
		resolver.getClassDeclarationByName(
			"cc.lib.mirror.context.Mirrored")!!.asStarProjectedType().makeNullable()
	}

	val arrayType by lazy {
		resolver.getClassDeclarationByName("kotlin.Array")!!.asStarProjectedType()
	}


	fun KSType.isMirrored(): Boolean {
		return mirrorType.isAssignableFrom(this)
	}

	fun KSType.isCollection(): Boolean {
		return collectionType.isAssignableFrom(this)
	}

	fun KSType.isArray(): Boolean {
		return arrayType.isAssignableFrom(this)
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

			val packageName: String = annotation.arguments
				.first { arg -> arg.name?.asString() == "packageName" }.value as String

			val dirtyType: DirtyType = DirtyType.valueOf((annotation.arguments
				.first { arg -> arg.name?.asString() == "dirtyType" }.value as KSType).toString().substringAfterLast('.'))

			logger.warn("dirtyType = $dirtyType")
			// Getting the list of member properties of the annotated interface.
			val properties: Sequence<KSPropertyDeclaration> = classDeclaration.getAllProperties()
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
					//if (type.nullability != Nullability.NULLABLE)
					//	logger.error("Only nullable properties allowed")
					it.appendLn("override var ${prop.getName()} : $type = ${type.defaultValue()}")
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
					if (propType.isMirrored()) {
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
						appendLn("   writer.name(\"$nm\").value($value)")
					}
				}
			}.toString()

			fun printJsonReaderContent(i: String) = StringBuffer().apply {

				fun KSType.typeName(): String {
					return when (val nm = toString().trimEnd('?')) {
						"Float" -> "Double().toFloat"
						"Short" -> "Int().toShort"
						"Byte" -> "Int().toByte"
						"Char" -> "String().first"
						else -> nm
					}
				}

				indent = i
				mapped.forEach { (prop, propType) ->
					val nm = prop.simpleName.asString()
					if (propType.isMirrored()) {
						appendLns(
							""""$nm" -> $nm = checkForNullOr(reader, null) { reader ->
   reader.beginObject()
   reader.nextString("type")
   val clazz = getClassForName(reader.nextString())
   reader.nextString("value")
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
					} else {
						appendLns("""
"$nm" -> $nm = checkForNullOr(reader, ${propType.defaultValue()}) {
   reader.next${propType.typeName()}()
}""")
					}
				}
			}.toString()

			fun printToStringContent(i: String): String = StringBuffer().apply {
				indent = i
				mapped.forEach { (prop, propType) ->
					val nm = prop.simpleName.asString()
					if (propType.isMirrored()) {
						appendLn("buffer.append(indent).append(\"$nm = \")")
						appendLn("if ($nm == null) buffer.append(\"null\")")
						appendLn("else {")
						appendLn("   buffer.append(\"{\\n\")")
						appendLn("   $nm!!.toString(buffer, \"\$indent\$INDENT\")")
						appendLn("   buffer.append(indent).append(\"}\\n\")")
						appendLn("}")
					} else {
						appendLn("buffer.append(indent).append(\"$nm = $$nm\\n\")")
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
				mapped.filter { it.value.isMirrored() }.keys.forEach {
					appendLn("${it.getName()}?.markClean()")
				}
			}.toString()

			fun printIsDirtyContent(i: String): String = StringBuffer().apply {
				indent = i
				when (dirtyType) {
					DirtyType.NEVER -> appendLn("return false")
					DirtyType.ANY -> {
						appendLn("if ($DIRTY_FLAG_FIELD) return true")
						mapped.filter { it.value.isMirrored() }.keys.map {
							appendLn("if (${it.getName()}?.isDirty() == true)")
							appendLn("   $DIRTY_FLAG_FIELD = true")
						}
						appendLn("return $DIRTY_FLAG_FIELD")
					}
					DirtyType.COMPLEX -> {
						append("${indent}return !$DIRTY_FLAG_FIELD.isEmpty()")
						mapped.filter { it.value.isMirrored() }.keys.map {
							append("|| ${it.getName()}?.isDirty() == true")
						}
						append("\n")
					}
				}
			}.toString()

			file.print(
				"""				
package $packageName
				
import com.google.gson.*
import com.google.gson.stream.*
import cc.lib.mirror.context.*
				
abstract class ${getClassFileName(classDeclaration.toString())}() : MirrorImplBase(), $classDeclaration {				

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
