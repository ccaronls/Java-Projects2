package cc.lib.mirror.processor

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
		return mirrorType.isAssignableFrom(this).also {
			logger.warn("isMirrored $this =?= $mirrorType is $it")
		}
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

	inner class Visitor(private val file: OutputStream) : KSVisitorVoid() {

		override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
			logger.warn("class declaration: $classDeclaration : ${classDeclaration.superTypes.joinToString()}")
			if (classDeclaration.classKind != ClassKind.INTERFACE) {
				logger.error("Only interface can be annotated with @Function", classDeclaration)
				return
			}

			val annotation: KSAnnotation = classDeclaration.annotations.first {
				it.shortName.asString() == "Mirror"
			}

			val packageNameArgument: KSValueArgument = annotation.arguments
				.first { arg -> arg.name?.asString() == "packageName" }

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

			fun printDeclarations(i: String): String = StringBuffer().also {
				indent = i
				mapped.forEach { (prop, type) ->
					//if (type.nullability != Nullability.NULLABLE)
					//	logger.error("Only nullable properties allowed")
					it.appendLn("override var ${prop.getName()} : ${type} = ${type.defaultValue()}")
				}
			}.toString()

			fun printJsonWriterContent(i: String) = StringBuffer().apply {

				fun encode(s: String, type: KSType): String = when (type.toString()) {
					"Char" -> "\"$$s\""
					else -> s
				}

				indent = i
				mapped.forEach { (prop, propType) ->
					val nm = prop.simpleName.asString()
					if (propType.isMirrored()) {
						appendLns("""
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
							""")
					} else if (propType.isEnum()) {
						if (propType.nullability != Nullability.NULLABLE) {
							logger.error("enum property $nm must be nullable")
							return@forEach
						}
						appendLn("writer.name(\"$nm\").value($nm?.name)")
					} else {
						val value = encode(nm, propType)
						appendLn("writer.name(\"$nm\").value($value)")
					}
				}
			}.toString()

			fun printJsonReaderContent(i: String) = StringBuffer().apply {

				fun KSType.typeName(): String {
					val nm = toString().trimEnd('?')
					return when (nm) {
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
							""""$nm" -> if (reader.peek() == JsonToken.NULL) {
   $nm = null
   reader.nextNull()
} else {
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
}""")
					} else if (propType.isEnum()) {
						appendLns("""
"$nm" -> if (reader.peek() == JsonToken.NULL) {
   $nm = ${propType.defaultValue()}
   reader.nextNull()
} else {
   $nm = enumValueOf<${propType.makeNotNullable()}>(reader.nextString())
}""")
					} else {
						appendLns("""
"$nm" -> if (reader.peek() == JsonToken.NULL) {
   $nm = ${propType.defaultValue()}
   reader.nextNull()
} else {
   $nm = reader.next${propType.typeName()}()
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
						appendLn("   $nm!!.toString(buffer, \"\$indent   \")")
						appendLn("   buffer.append(indent).append(\"}\\n\")")
						appendLn("}")
					} else {
						appendLn("buffer.append(indent).append(\"$nm = $$nm\\n\")")
					}
				}
			}.toString()

			file.print(
				"""				
package ${packageNameArgument.value}
				
import com.google.gson.*
import com.google.gson.stream.*
import cc.lib.mirror.context.*
				
abstract class ${getClassFileName(classDeclaration.toString())}() : MirrorImplBase(), ${classDeclaration} {				

${printDeclarations("    ")}

	override fun toGson(writer: JsonWriter) {
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
