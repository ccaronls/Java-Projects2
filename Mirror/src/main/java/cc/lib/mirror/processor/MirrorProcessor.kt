package cc.lib.mirror.processor

import com.google.devtools.ksp.KspExperimental
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

	@OptIn(KspExperimental::class)
	val collectionType by lazy {
		resolver.getClassDeclarationByName("java.util.Collection")!!.asStarProjectedType()
	}

	@OptIn(KspExperimental::class)
	val mirrorType by lazy {
		resolver.getClassDeclarationByName(
			"cc.lib.mirror.context.Mirrored")!!.asStarProjectedType().makeNullable()
	}

	fun KSType.isMirrored(): Boolean {
		return mirrorType.isAssignableFrom(this).also {
			logger.warn("isMirrored $this =?= $mirrorType is $it")
		}
	}

	fun KSType.isCollection(): Boolean {
		return collectionType.isAssignableFrom(this)
	}

	fun getClassFileName(symbol: String): String {
		if (symbol.startsWith("I"))
			return symbol.substring(1) + "Impl"
		return symbol + "Impl"
	}

	fun OutputStream.print(s: String) {
		write(s.toByteArray())
	}

	fun KSPropertyDeclaration.typeName(): String {
		val nm = this.type.resolve().toString().trimEnd('?')
		if (nm == "Float")
			return "Double().toFloat"
		return nm
	}
/*
	private inline fun <reified T> KSType.isAssignableFrom(): Boolean {
		val classDeclaration = requireNotNull(resolver.getClassDeclarationByName<T>()) {
			"Unable to resolve ${KSClassDeclaration::class.simpleName} for type ${T::class.simpleName}"
		}
		val decl = classDeclaration.asStarProjectedType()
		logger.warn("isAssignableFrom $this = $classDeclaration")
		return isAssignableFrom(decl)
	}*/

	inner class Visitor(private val file: OutputStream) : KSVisitorVoid() {

		override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
			logger.warn("class declaration: $classDeclaration")
			if (classDeclaration.classKind != ClassKind.INTERFACE) {
				logger.error("Only interface can be annotated with @Function", classDeclaration)
				return
			}

			// Getting the @Function annotation object.
			val annotation: KSAnnotation = classDeclaration.annotations.first {
				it.shortName.asString() == "Mirror"
			}

			val packageNameArgument: KSValueArgument = annotation.arguments
				.first { arg -> arg.name?.asString() == "packageName" }

			file.print("package ${packageNameArgument.value}\n\n")
			file.print("import com.google.gson.*\n")
			file.print("import com.google.gson.stream.*\n")
			file.print("\n")
			file.print("open class ${getClassFileName(classDeclaration.toString())}(var context : cc.lib.mirror.context.MirrorContext?=null) : ${classDeclaration}, cc.lib.mirror.context.Mirrored() {\n")

			// Getting the list of member properties of the annotated interface.
			val properties: Sequence<KSPropertyDeclaration> = classDeclaration.getAllProperties()
				.filter { it.validate() }

			properties.forEach { prop ->
				visitPropertyDeclaration(prop, Unit)
			}

			val mapped = properties.map { Pair(it, it.type.resolve()) }.toMap()

			file.print("\n   override fun toGson(writer: JsonWriter) {\n")
			file.print("\n      writer.beginObject()\n")
			mapped.forEach { prop, propType ->
				val nm = prop.simpleName.asString()
				if (propType.isMirrored()) {
					file.print("\n     writer.name(\"$nm\")")
					file.print("\n     $nm?.toGson(writer)?:writer.nullValue()")
				} else
					file.print("      writer.name(\"$nm\").value($nm)\n")
			}
			file.print("\n      writer.endObject()")
			file.print("\n   }\n")
			file.print("\n   override fun fromGson(reader: JsonReader) {")
			file.print("\n      reader.beginObject()")
			file.print("\n      while (reader.hasNext()) {")
			file.print("\n         when (reader.nextName()) {\n")
			mapped.forEach { prop, propType ->
				val nm = prop.simpleName.asString()
				if (propType.isMirrored()) {
					file.print(
						"""           "$nm" -> if (reader.peek() == JsonToken.NULL) {
				$nm = null
				reader.nextNull()
			} else {
				if ($nm == null) {
					$nm = ${propType.makeNotNullable()}().also {
						it.fromGson(reader)
					}
				}
			}
""")
				} else {
					//visitPropertyDeclaration(prop, Unit)
					file.print("         \"$nm\" -> $nm = reader.next${prop.typeName()}()\n")
				}
			}
			file.print("         }")
			file.print("\n      }")
			file.print("\n      reader.endObject()")
			file.print("\n   }")
			file.print("\n   fun toString(buffer: StringBuffer, indent: String) {")
			mapped.forEach { prop, propType ->
				//visitPropertyDeclaration(prop, Unit)
				val nm = prop.simpleName.asString()
				if (propType.isMirrored()) {
					file.print("\n         buffer.append(indent).append(\"$nm = \")")
					file.print("\n         if ($nm == null) buffer.append(\"null\")")
					file.print("\n         else {")
					file.print("\n            buffer.append(\"{\\n\")")
					file.print("\n            $nm!!.toString(buffer, \"\$indent   \")")
					file.print("\n            buffer.append(indent).append(\"}\\n\")")
					file.print("\n         }")
				} else {
					file.print("\n         buffer.append(indent).append(\"$nm = $$nm\\n\")")
				}
			}
			file.print("\n   }")
			file.print("\n\n   override fun toString() : String = StringBuffer().also { toString(it, \"\") }.toString()")
			file.print("\n}")

		}

		override fun visitTypeReference(typeReference: KSTypeReference, data: Unit) {
			logger.warn("visitTypeReference $typeReference")
		}

		override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
			if (property.type.resolve().nullability != Nullability.NULLABLE)
				logger.error("Only nullable properties allowed")
			file.print("   override var ${property.simpleName.asString()} : ${property.type}? = null\n")
		}

		override fun visitTypeArgument(typeArgument: KSTypeArgument, data: Unit) {
			logger.warn("visitTypeArgument $typeArgument")
		}

		override fun visitDeclaration(declaration: KSDeclaration, data: Unit) {
			logger.warn("visitDeclaration $declaration")
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
			packageName = "cc.mirror.impl",
			fileName = "${getClassFileName(symbol.simpleName.asString())}"
		)
		//file.print("package cc.mirror\n")

		symbol.accept(Visitor(file), Unit)
		file.close()
		return symbols
	}
}
