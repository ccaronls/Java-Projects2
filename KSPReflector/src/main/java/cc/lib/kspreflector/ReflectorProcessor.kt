package cc.lib.kspreflector

import cc.lib.ksp.helper.BaseProcessor
import cc.lib.ksp.helper.print
import cc.lib.ksp.reflector.Alternates
import cc.lib.ksp.reflector.Omit
import cc.lib.ksp.reflector.Reflect
import cc.lib.ksp.reflector.Reflector
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Modifier
import java.io.OutputStream

/**
 * Created by Chris Caron on 11/14/23.
 */
class ReflectorProcessor(
	codeGenerator: CodeGenerator,
	logger: KSPLogger,
	options: Map<String, String>,
) : BaseProcessor(codeGenerator, logger, options) {

	override lateinit var resolver: Resolver

	val reflectorType by lazy {
		resolver.getClassDeclarationByName(
			Reflector::class.qualifiedName!!
		)!!.asStarProjectedType().makeNullable()
	}

	fun KSType.isReflector(): Boolean {
		return reflectorType.isAssignableFrom(this)
	}

	fun String.setIndent(indent: String): String {
		return replace("\t", "   ").trimIndent().prependIndent(indent)
	}


	inner class Visitor(private val file: OutputStream) : KSVisitorVoid() {

		@OptIn(KspExperimental::class)
		override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
			logger.warn("classDeclaration: $classDeclaration")

			val classTypeName = classDeclaration.toString() + "Reflector"

			val superType = classDeclaration.superTypes.firstOrNull {
				it.resolve().isReflector()
			}?.resolve()?.toString() ?: "Reflector<$classTypeName>"//: throw Exception("Cannot find a reflector base type")

			logger.warn("superType :$superType")

			val fields = classDeclaration.getDeclaredProperties().filter {
				it.isAnnotationPresent(Omit::class).not() && it.modifiers.contains(Modifier.JAVA_TRANSIENT).not()
			}

			fun printFields(): String = StringBuffer().apply {
				fields.forEach { field ->
					append("abstract var $field : ${field.type.resolve()}\n")
				}
			}.toString().prependIndent("   ")

			fun getReaderTypeMethod(type: String): String {
				return when (type) {
					"Float" -> "Double().toFloat()"
					else -> "$type()"
				}
			}

			fun printFromGsonContent(indent: String): String = StringBuffer().apply {
				fields.forEach {
					val type = it.type.resolve()
					it.getAnnotationsByType(Alternates::class).forEach {
						append("\"${it.variation}\"")
						it.additional.forEach {
							append(",\n\"$it\"")
						}
						append(",\n")
					}
					append("\"$it\" -> ")
					if (type.isList()) {
						append("{\n")
						append("   reader.beginArray()\n")
						if (type.arguments.isEmpty())
							throw IllegalArgumentException("Cannot handle generic lists")
						val listParam = type.arguments[0].type!!
						if (listParam.resolve().isReflector()) {
							TODO()
						} else {
							append(
								"""
							$it = mutableListOf<$listParam>().also {
					            while (reader.hasNext()) {
						            it.add(reader.next${getReaderTypeMethod(listParam.toString())})
					            }
				            }""".setIndent("   ")
							)
							append("\n")
						}
						append("   reader.endArray()\n")
						append("}\n")
					} else if (type.isMap()) {
						TODO()
					} else if (type.isReflector()) {
						append("{\n")
						append("   reader.beginObject()\n")

						append("   reader.endObject()\n")
						append("}\n")
					} else if (type.isArray()) {
						TODO()
					} else {
						append("$it = reader.next${getReaderTypeMethod(it.type.toString())}\n")
					}
				}

			}.toString().prependIndent(indent)

			fun printToGsonContent(indent: String): String = StringBuffer().apply {
				fields.forEach {
					val type = it.type.resolve()
					append("writer.name(\"$it\")")
					with(type) {
						if (isPrimitive()) {
							append(".value($it)\n")
						} else if (isArray()) {
							append("\nwriter.beginArray()\n")
							TODO()
							append("\nwriter.endArray()\n")
						} else if (isReflector()) {
							append("\n")
							append(
								"""
							   $it?.let {
								   writer.beginObject()
								   it.toGson(writer)
								   writer.endObject()
							   }?:writer.nullValue()

								""".setIndent("")
							)
						} else if (isList()) {
							if (type.arguments.isEmpty())
								throw IllegalArgumentException("Cannot handle generic lists")
							val listParam = type.arguments[0].type!!
							if (listParam.resolve().isReflector()) {
								TODO()
							} else {
								append("\n")
								append(
									"""
								$it?.let {
								   writer.beginArray()
								   it.forEach {
									   writer.value(it)
								   }
								   writer.endArray()
							   }?:writer.nullValue()
									""".trimIndent().prependIndent("   ")
								)
								append("\n")
							}
						} else if (isMap()) {
							append("\nwriter.beginArray()\n")
							TODO()
							append("\nwriter.endArray()\n")
						} else {
							throw Exception("Dont know how to handle object type $this")
						}
					}
				}
			}.toString().prependIndent(indent)


			logger.warn("fields = ${fields.joinToString()}")

			file.print(
				"""package ${classDeclaration.packageName.asString()}
				
import com.google.gson.*
import com.google.gson.stream.*
import cc.lib.ksp.reflector.*

abstract class $classTypeName() : $superType {
${printFields()}

   override fun toGson(writer : JsonWriter) {
${printToGsonContent("      ")}
      super.toGson(writer)
   }
   
   override fun fromGson(reader : JsonReader, name : String) {
      when (name) {
${printFromGsonContent("         ")}	  
	     else -> super.fromGson(reader, name)
	  }
   }

	companion object {
		init {
			ReflectorContext.register("$classDeclaration") { $classDeclaration() }
		}
	}

}
"""
			)
		}

	}
	override fun process(resolver: Resolver): List<KSAnnotated> {
		this.resolver = resolver
		val symbols = resolver
			.getSymbolsWithAnnotation(Reflect::class.qualifiedName!!)
			.filterIsInstance<KSClassDeclaration>().toMutableList()

		logger.warn("processing Reflector annotations")
		logger.warn("options=$options")
		logger.warn("symbols=${symbols.joinToString()}")

		if (symbols.isEmpty())
			return emptyList()

		val symbol = symbols.removeAt(0)

		val file = codeGenerator.createNewFile(
			// Make sure to associate the generated file with sources to keep/maintain it across incremental builds.
			// Learn more about incremental processing in KSP from the official docs:
			// https://kotlinlang.org/docs/ksp-incremental.html
			dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray()),
			packageName = options["package"] ?: "cc.mirror.impl",
			fileName = symbol.simpleName.asString() + "Reflector"
		)
		symbol.accept(Visitor(file), Unit)
		file.close()
		return symbols
	}
}
