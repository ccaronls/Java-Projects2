package cc.lib.kspreflector

import cc.lib.ksp.helper.SimpleProcessor
import cc.lib.ksp.reflector.Alternates
import cc.lib.ksp.reflector.Dirty
import cc.lib.ksp.reflector.IDirtyReflector
import cc.lib.ksp.reflector.IReflector
import cc.lib.ksp.reflector.Omit
import cc.lib.ksp.reflector.Reflect
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Modifier
import java.io.OutputStream
import kotlin.reflect.KClass

/**
 * Created by Chris Caron on 11/14/23.
 */
class ReflectorProcessor2(
	codeGenerator: CodeGenerator,
	logger: KSPLogger,
	options: Map<String, String>,
) : SimpleProcessor(codeGenerator, logger, options) {

	val reflectorType: KSType
		get() = resolver.getClassDeclarationByName(
			IReflector::class.qualifiedName!!
		)!!.asStarProjectedType().makeNullable()

	val dirtyReflectorType: KSType
		get() = resolver.getClassDeclarationByName(
			IDirtyReflector::class.qualifiedName!!
		)!!.asStarProjectedType().makeNullable()

	fun KSType.isReflector(): Boolean {
		return reflectorType.isAssignableFrom(this) || isDirtyReflector()
	}

	fun KSType.isDirtyReflector(): Boolean {
		return dirtyReflectorType.isAssignableFrom(this)
	}

	fun KSType.isReflectorArrayType(): Boolean {
		return isArrayType() && getTypeArgumentOrNull()?.isReflector() ?: false
	}

	fun String.setIndent(indent: String): String {
		return replace("\t", INDENT).trimIndent().prependIndent(indent)
	}

	inner class Visitor(private val file: OutputStream) : KSVisitorVoid() {

		@OptIn(KspExperimental::class)
		override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
			logger.warn("classDeclaration: $classDeclaration")

			val classTypeName = getClassFileName(classDeclaration)
			if (classTypeName == classDeclaration.toString())
				throw IllegalArgumentException("Declaration $classDeclaration needs to be qualified with 'name' in the annotation or be prefixed with 'A' or 'I' or suffixed with 'R' to disambiguate from derived class")

			val isAbstract = classDeclaration.getDeclaredFunctions().firstOrNull { it.isAbstract } != null

			val classType = classDeclaration.asStarProjectedType().also {
				if (!it.isReflector())
					throw IllegalArgumentException("$classDeclaration must extend IReflector or IDirtyReflector")
			}

			logger.warn("superType :$classType")

			val fields = classDeclaration.getDeclaredProperties().filter {
				it.isAnnotationPresent(Omit::class).not()
					&& it.modifiers.contains(Modifier.JAVA_TRANSIENT).not()
			}.toList()

			val dirtyFields = classDeclaration.getDeclaredProperties().filter {
				it.isAnnotationPresent(Dirty::class)
			}.toList()

			if (dirtyFields.isNotEmpty() && !classType.isDirtyReflector()) {
				throw IllegalArgumentException("$classDeclaration has @Dirty annotations but does not extend IDirtyReflector")
			}

			fun printFields(): String = StringBuffer().apply {
				fields.filter { it.modifiers.any { it in arrayOf(Modifier.ABSTRACT) } }.forEach { field ->
					append("abstract var $field : ${field.type.resolve()}\n")
				}
			}.toString().setIndent(INDENT)

			fun getReaderTypeMethod(type: String): String {
				return when (type) {
					"Float" -> "Double().toFloat()"
					else -> "$type()"
				}
			}

			//////////////////////////////////////////
			// EQUALS ////////////////////////////////
			//////////////////////////////////////////

			fun printEqualsContent(indent: String) = StringBuffer().apply {
				var and = ""
				fields.forEach {
					val name = it.getName()
					val type = it.type.resolve()
					if (type.isArrayType()) {
						append(indent).append("$and$name.contentEquals(it.$name)\n")
					} else {
						append(indent).append("$name == it.$name\n")
					}
					and = "&& "
				}
			}.toString()

			//////////////////////////////////////////
			// TO_STRING /////////////////////////////
			//////////////////////////////////////////

			fun printToStringContent(indent: String) = StringBuffer().apply {
				fields.forEach {
					val name = it.getName()
					val type = it.type.resolve()
					append(indent).append("""buf.append(indent+"$INDENT").append("$name=")""")
					if (type.isReflectorArrayType()) {
						append(""".append($name?.joinToString(separator = "\n", prefix = "[\n", postfix = "\n  ]\n") {  
						   it.toString(indent + "$INDENT")
				       })""").append("\n")
					} else if (type.isArrayType()) {
						append(""".append($name?.joinToString(prefix = "[", postfix = "]")).append("\n")""").append("\n")
					} else if (type.isReflector()) {
						append(""".append($name?.toString(indent + "$INDENT")).append("\n")""").append("\n")
					} else if (type.isString()) {
						append(""".append("\"").append($name).append("\"\n")""").append("\n")
					} else {
						append(""".append($name).append("\n")""").append("\n")
					}
				}
			}.toString()

			//////////////////////////////////////////
			// FROM_JSON /////////////////////////////
			//////////////////////////////////////////

			fun printFromJsonForType(name: String, type: KSType, indent: String): String = StringBuffer().apply {
				logger.warn("printFromJsonForType $name, $type, reflector: ${type.isReflector()}")
				if (type.isList()) {
					val listType = type.getTypeArgumentOrThrow(name)
					append("""
						// type.isList()
						reader.beginObject()
						val size = reader.nextName("size").nextInt()
						$name = ReflectorContext.newInstance<MutableList<$listType>>(reader.nextName()).also { list ->
							reader.beginArray()
							for (i in 0 until size) {
								val obj : $listType
								${printFromJsonForType("obj", listType, indent + INDENT)}
								list.add(obj)
							}
							reader.endArray()
						}
						reader.endObject()
					""".setIndent(indent))
				} else if (type.isMap()) {
					TODO()
				} else if (type.isReflector()) {
					if (type.isNullable()) {
						append(""" // type is nullable Reflector
									$name = reader.checkNull { 
										reader.beginObject()
										ReflectorContext.newInstance<${type.makeNotNullable()}>(reader.nextName("type").nextString()).also {
											reader.nextName("object")
											it.fromJson(reader)
											reader.endObject()
										}
									}
								""".setIndent(indent))

					} else {
						append(""" // type is non-nullable reflector
									reader.beginObject()
									$name = ReflectorContext.newInstance<${type.makeNotNullable()}>(reader.nextName("type").nextString()).also {
										reader.nextName("object")
										it.fromJson(reader)
									}
									reader.endObject()
								""".setIndent(indent))
					}
					append("\n")
				} else if (type.isPrimitiveArray()) {
					val primitiveType = type.toString().removeSuffix("Array")
					append("{ // else if type.isPrimitiveArray() \n")
					append("""
						         reader.beginObject()
						         val size = reader.nextName("size").nextInt()
						         reader.nextName("array").beginArray()
						         $name = ${primitiveType}Array(size) { reader.next${primitiveType.replace("Float", "Double().toFloat")}() }
						         reader.endArray()
								 reader.endObject()
					        }""".setIndent(indent))
					append("\n")
				} else if (type.isReflectorArrayType()) {
					append(""" // type is array of reflectors
									{
								         reader.beginObject()
								         val size = reader.nextName("size").nextInt()
								         reader.nextName("array").beginArray()
								         $name = Array(size) { 
										    reader.beginObject()
										    ReflectorContext.newInstance<IReflector>(reader.nextName()).also {
										it.fromJson(reader)
										reader.endObject()
			                        }
								}
						         reader.endArray()
								 reader.endObject()
					        }""".setIndent(indent))
					append("\n")
				} else if (type.isEnum()) {
					append("""
						// type is enum
						$name = enumValueOf<$type>(reader.nextString())
						""".setIndent(indent)).append("\n")
				} else if (type.isEnumArray()) {
					append("""
								// type is enum array
						         reader.beginObject()
						         val size = reader.nextName("size").nextInt()
						         reader.nextName("array").beginArray()
						         $name = Array(size) { enumValueOf<${type.getTypeArgumentOrThrow(name)}>(reader.nextString()) }
						         reader.endArray()
								 reader.endObject()
					        """.setIndent(indent)).append("\n")
				} else {
					append("""
						// using else case for $type
						$name = reader.next${getReaderTypeMethod(type.toString())}
						""".setIndent(indent)).append("\n")
				}

			}.toString()


			fun printFromJsonContent(indent: String): String = StringBuffer().apply {
				fields.forEach { field ->
					val name = field.getName()
					val type = field.type.resolve()
					field.getAnnotationsByType(Alternates::class).forEach {
						append("\"${it.variation}\"")
						it.additional.forEach {
							append(",\n\"$it\"")
						}
						append(",\n")
					}
					append("\"$name\" -> {\n")
					append(printFromJsonForType(name, type, indent))
					append(indent).append("}\n")
				}

			}.toString().setIndent(indent)

			//////////////////////////////////////////
			// TO_JSON ///////////////////////////////
			//////////////////////////////////////////

			fun printToJsonForType(name: String, type: KSType, indent: String): String = StringBuffer().apply {
				if (type.isPrimitive()) {
					append("writer.value($name)\n")
				} else if (type.isPrimitiveArray()) {
					append("""
									writer.beginObject()
	                                writer.name("size").value($name.size)
								    writer.name("array").beginArray()
								    $name.forEach { 
										writer.value(it)
								    }
								    writer.endArray()
									writer.endObject()
								""".setIndent(indent))
				} else if (type.isReflectorArrayType()) {
					append("""
									writer.beginObject()
	                                writer.name("size").value($name.size)
								    writer.name("array").beginArray()
								    $name.forEach {
										writer.beginObject()
										writer.name(it.getClassId())
										writer.beginObject()
										it.toJson(writer)
										writer.endObject()
										writer.endObject()
								    }
								    writer.endArray()
									writer.endObject()
								""".setIndent(indent))
				} else if (type.isReflector()) {
					append(
						"""
							   $name?.let {
								   writer.beginObject()
								   writer.name("type").value(it.getClassId())
								   writer.name("object")
								   writer.beginObject()
								   it.toJson(writer)
								   writer.endObject()
								   writer.endObject()
							   }?:writer.nullValue()

								""".setIndent("")
					)
				} else if (type.isEnum()) {
					append("writer.value($name.name)\n")
				} else if (type.isEnumArray()) {
					append("""
									writer.beginObject()
	                                writer.name("size").value($name.size)
								    writer.name("array").beginArray()
								    $name.forEach { 
										writer.value(it.name)
								    }
								    writer.endArray()
									writer.endObject()
								""".setIndent(indent))
				} else if (type.isList()) {
					if (type.arguments.isEmpty())
						throw IllegalArgumentException("Cannot handle generic lists")
					val listParam = type.arguments[0].type!!.resolve()
					append("\n")
					append("""
						writer.beginObject()
						writer.name("size").value($name.size)
						writer.name("${type.getSimpleClassName()}")
						writer.beginArray()
						$name.forEach {
							${printToJsonForType(name = "it", type = listParam, indent = indent + INDENT)}
						}
						writer.endArray()
						writer.endObject()
						""".setIndent(indent))
				} else if (type.isMap()) {
					append("""
						writer.beginObject()
						${throw Exception("TODO: Implement")}
						writer.endObject()
						""".setIndent(indent))
				} else {
					throw Exception("Dont know how to handle object named $name type $type")
				}
				append("\n")
			}.toString()

			// ----------------------------------------------------

			fun printToJsonContent(indent: String): String = StringBuffer().apply {
				fields.forEach { field ->
					val type = field.type.resolve()
					val name = field.getName()
					logger.warn("field $name is of type $type")
					append("writer.name(\"$field\")\n")
					append(printToJsonForType(name, type, indent))
				}
			}.toString().setIndent(indent)

			//////////////////////////////////////////
			// PRIMARY BLOCK /////////////////////////
			//////////////////////////////////////////

			logger.warn("fields = ${fields.joinToString()}")

			file.print(
				"""package ${classDeclaration.packageName.asString()}
				
import com.google.gson.*
import com.google.gson.stream.*
import cc.lib.ksp.reflector.*

${if (isAbstract) "abstract" else ""} class $classTypeName${classDeclaration.getParamsSignature()} : $classDeclaration(${classDeclaration.getParams()}) {
${printFields()}

	${
					if (!isAbstract) """
   override fun getClassId() = _CLASS_ID
   """ else ""
				}

   override fun toJson(writer : JsonWriter) {
${printToJsonContent("      ")}
      super.toJson(writer)
   }
   
   override fun fromJson(reader : JsonReader, name : String) {
      when (name) {
${printFromJsonContent("         ")}	  
	     else -> super.fromJson(reader, name)
	  }
   }
   
   override fun toString(indent : String) : String = StringBuffer().also { buf ->
       buf.append(indent).append(_CLASS_ID).append(" {\n")
	   buf.append(super.toString(indent + "  "))
${printToStringContent("       ")}
	   buf.append(indent).append("}\n")
   }.toString()
   
   ${
					if (fields.isNotEmpty()) """
   	override fun equals(other: Any?): Boolean {
		return (other as? $classTypeName)?.let {
${printEqualsContent("         ")}
		}?:super.equals(other)
	}""" else ""
				}
	

   
   override fun toString() = toString("")

	companion object {
		const val _CLASS_ID = "$classTypeName"
			${
					if (!isAbstract) """
			init {
				ReflectorContext.register("$classTypeName") { $classTypeName() }
			}""" else ""
				}
		}

}
"""
			)
		}

	}

	override val annotationClass: KClass<*> = Reflect::class
	override val packageName: String = "cc.lib.rem.impl"

	override fun process(symbol: KSClassDeclaration, file: OutputStream) {
		symbol.accept(Visitor(file), Unit)
	}

	@OptIn(KspExperimental::class)
	override fun getClassFileName(symbol: KSClassDeclaration): String {
		return symbol.getAnnotationsByType(Reflect::class).first().className.takeIf {
			it.isNotBlank()
		} ?: symbol.simpleName.asString().trimStart('I', 'A').trimEnd('R') + "Impl"
	}
}
