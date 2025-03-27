package cc.lib.binaryserializable.processor

import cc.lib.ksp.binaryserializer.BinarySerializable
import cc.lib.ksp.helper.BaseProcessor
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitorVoid
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.OutputStream
import kotlin.reflect.KClass

class Processor(
	codeGenerator: CodeGenerator,
	logger: KSPLogger,
	options: Map<String, String>
) : BaseProcessor(codeGenerator, logger, options) {

	override fun getClassFileName(symbol: String): String {
		return symbol + "BinarySerializer"
	}

	override val annotationClass: KClass<*> = BinarySerializable::class
	override val packageName: String = "cc.lib.binaryserializer.impl"

	override fun process(symbol: KSClassDeclaration, file: OutputStream) {
		symbol.accept(Visitor(file), Unit)
	}

	fun getTypeTemplates(ref: KSTypeReference): String {
		logger.warn("getTypeTemplates $ref->${ref.resolve()}")
		with(ref.resolve().arguments) {
			if (isEmpty()) return ""
			return "<${joinToString { it.type.toString() }}>"
		}
	}

	fun getMethodSignature(decl: KSFunctionDeclaration): String {
		return decl.parameters.joinToString { "${it.name!!.asString()} : ${it.type.resolve()}" }
	}

	private fun getDataType(name: String, type: String?): String {
		return when (type) {
			"kotlin.Int" -> "Int"
			"kotlin.Long" -> "Long"
			"kotlin.Float" -> "Float"
			"kotlin.Double" -> "Double"
			"kotlin.Boolean" -> "Boolean"
			"kotlin.String" -> "UTF"
			else -> throw IllegalArgumentException("getDataType($name): Unsupported type: $type")
		}
	}

	inner class Visitor(private val file: OutputStream) : KSVisitorVoid() {

		@OptIn(KspExperimental::class)
		override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {

			logger.warn("Process class: $classDeclaration")
			if (!(classDeclaration.isAbstract() || classDeclaration.isOpen())) {
				logger.error("- Class declaration must be an interface")
				return
			}

			val classTypeName = classDeclaration.getAnnotationsByType(BinarySerializable::class).first().className

			val properties = classDeclaration.getAllProperties()
				.filter { it.getAnnotationsByType(Transient::class).firstOrNull() == null } // Filter out transient fields
				.filter { it.isMutable }
				.toList()

			properties.firstOrNull { it.type.resolve().isNullable() }?.let {
				throw IllegalArgumentException("Nullable Property '$it' of $classDeclaration not supported")
			}

			fun printProperties(): String = StringBuffer().also {
				properties.forEach { property ->
					val name = property.simpleName.asString()
					val resolvedType = property.type.resolve()
					val type = resolvedType.declaration.qualifiedName?.asString()
					val defaultValue = resolvedType.defaultValue(property)
					it.append("   var $name : $type = $defaultValue\n")
				}
			}.toString().trimEnd()

			fun printCopyBody(): String = StringBuffer().also {
				properties.forEach { property ->
					val name = property.simpleName.asString()
					val resolvedType = property.type.resolve()
					it.append("\t\t$name = other.$name\n")
				}
			}.toString().trimEnd()

			fun printSerializeBody(): String = StringBuffer().also {
				properties.forEach { property ->
					val name = property.simpleName.asString()
					val resolvedType = property.type.resolve()
					val type = resolvedType.declaration.qualifiedName?.asString()
					if (resolvedType.isArrayType()) {
						it.append("\t\toutput.writeShort(${name}.size)\n")
						val arrayType = getDataType(name, resolvedType.arrayElementType())
						it.append("\t\t${name}.forEach { output.write${arrayType}(it) }\n")
					} else if (!resolvedType.isPrimitive()) {
						it.append("\t\t$name.serialize(output)\n")
					} else {
						val dataType = getDataType(name, type)
						it.append("\t\toutput.write${dataType}($name)\n")
					}
				}
			}.toString().trimEnd()

			fun printDeserializeBody(): String = StringBuffer().also {
				properties.forEach { property ->
					val name = property.simpleName.asString()
					val resolvedType = property.type.resolve()
					val type = resolvedType.declaration.qualifiedName?.asString()
					val defaultValue = resolvedType.defaultValue(property)
					if (resolvedType.isArrayType()) {
						val arrayType = getDataType(name, resolvedType.arrayElementType())
						it.append("\t\t$name = $type(input.readShort().toInt()) { input.read$arrayType() }\n")
					} else if (!resolvedType.isPrimitive()) {
						it.append("\t\t$name.deserialize(input)\n")
					} else {
						val dataType = getDataType(name, type)
						it.append("\t\t$name = input.read${dataType}()\n")
					}
				}
			}.toString().trimEnd()

			fun printSizeBytesBody(): String = StringBuffer().also {
				it.append("\t\treturn ")
				it.append(properties.joinToString(" +\n\t\t\t") { property ->
					val name = property.simpleName.asString()
					val resolvedType = property.type.resolve()
					val type = resolvedType.declaration.qualifiedName!!.asString()
					if (resolvedType.isString()) {
						"16 + ${name}.length()"
					} else if (resolvedType.isPrimitive()) {
						"${type}.SIZE_BYTES"
					} else {
						logger.error("Dont know how to determine size of $type")
						""
					}
				})
			}.toString().trimEnd()

			file.print(
				"""package ${classDeclaration.packageName.asString()}
				
"""
			)

			imports.add(DataInputStream::class.qualifiedName.toString())
			imports.add(DataOutputStream::class.qualifiedName.toString())

			imports.forEach {
				file.print("import $it\n")
			}
			file.print(
				"""
import cc.lib.ksp.binaryserializer.IBinarySerializable				
			
class $classTypeName : $classDeclaration(), IBinarySerializable<$classTypeName> {

	override fun copy(other : $classTypeName) {
${printCopyBody()}
	}

    override fun serialize(output : DataOutputStream) {
${printSerializeBody()}
	}
	
	override fun deserialize(input : DataInputStream) {
${printDeserializeBody()}
	}
}
"""
			)
		}
	}
}