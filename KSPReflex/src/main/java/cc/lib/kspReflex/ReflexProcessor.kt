package cc.lib.kspReflex

import cc.lib.ksp.helper.KSPProcessorException
import cc.lib.ksp.helper.SimpleProcessor
import cc.lib.ksp.reflex.Alternates
import cc.lib.ksp.reflex.Dirty
import cc.lib.ksp.reflex.IDirtyReflex
import cc.lib.ksp.reflex.IReflex
import cc.lib.ksp.reflex.Omit
import cc.lib.ksp.reflex.Reflex
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
			IReflex::class.qualifiedName!!
		)!!.asStarProjectedType().makeNullable()

	val dirtyReflectorType: KSType
		get() = resolver.getClassDeclarationByName(
			IDirtyReflex::class.qualifiedName!!
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
		return replace("\t", INDENT).trimIndent().prependIndent(indent).lines().filter {
			it.isNotBlank()
		}.joinToString("\n")
	}

	fun StringBuffer.newline(): StringBuffer = append("\n")

	inner class Visitor(private val file: OutputStream) : KSVisitorVoid() {

		@OptIn(KspExperimental::class)
		override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
			logger.warn("classDeclaration: $classDeclaration")

			val classTypeName = getClassFileName(classDeclaration)
			if (classTypeName == classDeclaration.toString())
				throw KSPProcessorException("Declaration $classDeclaration needs to be qualified with 'name' in the annotation or be prefixed with 'A' or 'I' or suffixed with 'R' to disambiguate from derived class")

			val isAbstract = classDeclaration.getDeclaredFunctions().firstOrNull { it.isAbstract } != null

			val classType = classDeclaration.asStarProjectedType().also {
				if (!it.isReflector())
					throw KSPProcessorException("$classDeclaration must extend IReflex or IDirtyReflector")
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
				throw KSPProcessorException("$classDeclaration has @Dirty annotations but does not extend IDirtyReflector")
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
						append("""// isReflectorArrayType $type
							buf.append($name?.joinToString(separator = ",", prefix = "[", postfix = "]") {  
						   it?.toString(indent + "$INDENT")?:"null"
				       })""").newline()
					} else if (type.isArrayType()) {
						append(""" // isArrayType $type
							buf.append($name?.joinToString(prefix = "[", postfix = "]")).append("\n")""".trimIndent()).newline()
					} else if (type.isReflector()) {
						append(""" // isReflector $type
							buf.append($name?.toString(indent + "$INDENT")).append("\n")""".trimIndent()).newline()
					} else if (type.isString()) {
						append(""" // isString $type
							buf.append("\"").append($name).append("\"\n")""".trimIndent()).newline()
					} else {
						append(""" // else $type
							.append($name).append("\n")""".trimIndent()).newline()
					}
				}
			}.toString()

			//////////////////////////////////////////
			// FROM_JSON /////////////////////////////
			//////////////////////////////////////////

			fun printFromJsonForType(name: String, type: KSType, indent: String): String = StringBuffer().apply {
				logger.warn("printFromJsonForType $name, $type, reflector: ${type.isReflector()}")
				val name_eq = if (name.isBlank()) "" else "$name ="
				if (type.isList()) {
					val listType = type.getTypeArgumentOrThrow(name)
					append("""
						// type.isList $type
						$name_eq reader.checkNull { 
							reader.beginObject()
							val size = reader.nextName("size").nextInt()
							RFLX.newInstance<MutableList<$listType>>(reader.nextName()).also { list ->
								reader.beginArray()
								repeat(size) {
									val obj : $listType
									${printFromJsonForType("obj", listType, indent + INDENT)}
									list.add(obj)
								}
								reader.endArray()
								reader.endObject()
							}
						} as $type
					""".setIndent(indent))
				} else if (type.isMap()) {
					val (keyType, valueType) = type.getTypeArgumentsOrThrow(name, 2)
					append(
						"""
							// isMap $type
							$name_eq reader.checkNull { 
								reader.beginObject()
								val size = reader.nextName("size").nextInt()
								RFLX.newInstance<MutableMap<$keyType, $valueType>>(reader.nextName()).also { map ->
									reader.beginArray()
									repeat(size) { 
										val key : $keyType
										${printFromJsonForType("key", keyType, indent + INDENT)}
										val value : $valueType
										${printFromJsonForType("value", valueType, indent + INDENT)}
										map[key] = value
									}
									reader.endArray()
									reader.endObject()
								}
							} as $type
							""".setIndent(indent)
					)
				} else if (type.isReflector()) {
					append(""" 
								// type isReflector $type
								$name_eq reader.checkNull { 
									reader.beginObject()
									RFLX.newInstance<${type.makeNotNullable()}>(reader.nextName("type").nextString()).also {
										reader.nextName("object")
										it.fromJson(reader)
										reader.endObject()
									}
								} as $type
							""".setIndent(indent))
					newline()
				} else if (type.isPrimitiveArray()) {
					val primitiveType = type.makeNotNullable().toString().removeSuffix("Array")
					append("""
								// type.isPrimitiveArray() $type
						         reader.beginObject()
						         val size = reader.nextName("size").nextInt()
						         reader.nextName("array").beginArray()
						         $name_eq ${primitiveType}Array(size) { reader.next${primitiveType.replace("Float", "Double().toFloat")}() }
						         reader.endArray()
								 reader.endObject()
					        """.setIndent(indent))
					newline()
				} else if (type.isArrayType()) {
					val arrayType = type.getTypeArgumentOrThrow(name)
					append(""" 
						// type is array of ??? $arrayType
						reader.checkNull() {
					         reader.beginObject()
					         val size = reader.nextName("size").nextInt()
					         reader.nextName("array").beginArray()
					         $name_eq Array(size) { 
							    ${printFromJsonForType("", arrayType, indent + INDENT)}
										
							    //reader.beginObject()
							    //RFLX.newInstance<IReflex>(reader.nextName("type").nextString()).also {
								//	reader.nextName("object")
								//	it.fromJson(reader)
								//	reader.endObject()
			                    //} as ${type.getTypeArgumentOrThrow(name)}
							 }
					         reader.endArray()
							 reader.endObject()
						}
						""".setIndent(indent)).newline()
				} else if (type.isEnum()) {
					append("""
						// type is enum $type
						$name_eq reader.checkNull { enumValueOf<${type.makeNotNullable()}>(reader.nextString()) } as $type
						""".setIndent(indent)).newline()
				} else if (type.isEnumArray()) {
					append("""
								// type is enum array $type
								$name_eq reader.checkNull {
							         reader.beginObject()
							         val size = reader.nextName("size").nextInt()
							         reader.nextName("array").beginArray()
							         Array(size) { enumValueOf<${type.getTypeArgumentOrThrow(name)}>(reader.nextString()) }.also {
								         reader.endArray()
										 reader.endObject()
									}
								 } as $type
					        """.setIndent(indent)).newline()
				} else {
					append("""
						// using else case for $type
						$name_eq reader.checkNull { reader.next${getReaderTypeMethod(type.makeNotNullable().toString())} } as $type
						""".setIndent(indent)).newline()
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
								$name?.let { $name ->
									// isPrimitiveArray $type
									writer.beginObject()
	                                writer.name("size").value($name.size)
								    writer.name("array").beginArray()
								    $name.forEach { 
										writer.value(it)
								    }
								    writer.endArray()
									writer.endObject()
								}?:writer.nullValue()
								""".setIndent(indent))
				} else if (type.isReflectorArrayType()) {
					append("""
								// isReflectorArrayType
								$name?.let { $name ->
									writer.beginObject()
	                                writer.name("size").value($name.size)
								    writer.name("array").beginArray()
								    $name.forEach {
									""".setIndent(indent)).newline()
					append(printToJsonForType("it", type.getTypeArgumentOrThrow(name), indent + INDENT))
					append("""
								    }
								    writer.endArray()
									writer.endObject()
								}?:writer.nullValue()
								""".setIndent(indent))
				} else if (type.isReflector()) {
					append(
						"""
							    // isReflector
							    $name?.let { $name ->
								   writer.beginObject()
								   writer.name("type").value($name.getClassId())
								   writer.name("object")
								   writer.beginObject()
								   $name.toJson(writer)
								   writer.endObject()
								   writer.endObject()
							   }?:writer.nullValue()

								""".setIndent("")
					)
				} else if (type.isEnum()) {
					append(""" 
						// isEnum $type
						writer.value($name?.name)
						""".setIndent(indent))
				} else if (type.isEnumArray()) {
					append("""
									// isEnumArray $type
									$name?.let { $name ->
										writer.beginObject()
		                                writer.name("size").value($name.size)
									    writer.name("array").beginArray()
									    $name.forEach { 
											writer.value(it.name)
									    }
									    writer.endArray()
										writer.endObject()
									}?:writer.nullValue()
								""".setIndent(indent))
				} else if (type.isList()) {
					if (type.arguments.isEmpty())
						throw KSPProcessorException("Cannot handle generic lists")
					val listParam = type.arguments[0].type!!.resolve()
					newline()
					append("""
						// isList
						$name?.let { $name ->
							writer.beginObject()
							writer.name("size").value($name.size)
							writer.name("${type.getSimpleClassName()}")
							writer.beginArray()
							$name.forEach {
							""".setIndent(indent)).newline().append("""
								${printToJsonForType(name = "it", type = listParam, indent = indent + INDENT)}
							}""".setIndent(indent)).newline().append("""
							writer.endArray()
							writer.endObject()
						}?:writer.nullValue()
						""".setIndent(indent))
				} else if (type.isMap()) {
					val (keyType, valueType) = type.getTypeArgumentsOrThrow(name, 2)
					append("""
						// isMap
						$name?.let { $name ->
							writer.beginObject()
							writer.name("size").value($name.size)
							writer.name("${type.getSimpleClassName()}")
							writer.beginArray()
							$name.entries.forEach { (key, value) ->
								${printToJsonForType(name = "key", type = keyType, indent = indent + INDENT)}
								${printToJsonForType(name = "value", type = valueType, indent = indent + INDENT)}
							}
							writer.endArray()
							writer.endObject()
						}?:writer.nullValue()
						""".setIndent(indent))
				} else {
					throw KSPProcessorException("Don't know how to handle object named $name type $type")
				}
				newline()
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
import cc.lib.ksp.reflex.*

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
				RFLX.register("$classTypeName") { $classTypeName() }
			}""" else ""
				}
		}

}
"""
			)
		}

	}

	override val annotationClass: KClass<*> = Reflex::class
//	override val packageName: String = "cc.lib.reflex.impl"

	override fun process(symbol: KSClassDeclaration, out: OutputStream) {
		symbol.accept(Visitor(out), Unit)
	}

	@OptIn(KspExperimental::class)
	override fun getClassFileName(symbol: KSClassDeclaration): String {
		return symbol.getAnnotationsByType(Reflex::class).first().className.takeIf {
			it.isNotBlank()
		} ?: symbol.simpleName.getShortName().trimStart('I', 'A').trimEnd('R') + "Impl"
	}
}
