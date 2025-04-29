package cc.lib.binaryserializable.processor

import cc.lib.ksp.binaryserializer.BinarySerializable
import cc.lib.ksp.binaryserializer.BinaryType
import cc.lib.ksp.binaryserializer.IBinarySerializable
import cc.lib.ksp.helper.BaseProcessor
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitorVoid
import java.io.OutputStream
import kotlin.reflect.KClass

class BinarySerializerProcessor(
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

	val binarySerializableType by lazy {
		resolver.getClassDeclarationByName(IBinarySerializable::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()
	}

	fun KSType.isBinarySerializable(): Boolean {
		return binarySerializableType.isAssignableFrom(this)
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

	private fun getWriteMethodForType(name: String, type: String): String {
		//logger.warn("getDataTypeForWrite $name:$type")
		return "write${type.removePrefix("kotlin.")}($name)"
	}

	private fun getReadMethodForType(name: String, type: String): String {
		return "read${type.removePrefix("kotlin.")}()"
	}

	fun <T> MutableList<T>.takeAndRemove(n: Int): List<T> {
		if (size < n) {
			return take(size).also {
				clear()
			}
		}
		return take(n).also {
			repeat(n) { removeAt(0) }
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
				.filter { it.isMutable || !it.type.resolve().isPrimitive() }
				.toList()

			properties.firstOrNull { it.type.resolve().isNullable() }?.let {
				throw IllegalArgumentException("Nullable Property '$it' of $classDeclaration not supported")
			}

			logger.warn("properties: ${properties.joinToString { it.simpleName.asString() }}")

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
				//logger.warn("printCopyBody: $classDeclaration")
				properties.forEach { property ->
					val name = property.simpleName.asString()
					val resolvedType = property.type.resolve()
					if (resolvedType.isPrimitive()) {
						it.append("\t\t$name = other.$name\n")
					} else if (resolvedType.isArrayType()) {
						it.append("\t\tother.$name.copyInto($name)\n")
					} else {
						it.append("\t\t$name.copy(other.$name)\n")
					}
				}
			}.toString().trimEnd()

			fun getPropertyType(decl: KSPropertyDeclaration): KSType {
				//logger.warn("getPropertyType $decl")
				decl.getAnnotationsByType(BinaryType::class).firstOrNull()?.let {
					logger.warn("$decl has BinaryType ${it.classType}")
					return resolver.getClassDeclarationByName(it.classType.qualifiedName.toString())!!.asType(emptyList())
				}
				return decl.type.resolve()
			}

			fun printContentEqualsBody(): String = StringBuffer().also { sb ->
				properties.joinToString("\n\t\t\t&& ") { property ->
					val name = property.simpleName.asString()
					val resolvedType = getPropertyType(property)
					val type = resolvedType.declaration.qualifiedName!!.asString()
					//logger.warn("analyze property $property -> $name:$resolvedType:$type")
					if (resolvedType.isArrayType()) {
						"$name.contentEquals(other.$name)"
					} else if (resolvedType.isBinarySerializable()) {
						"$name.contentEquals(other.$name)"
					} else {
						"$name == other.$name"
					}
				}.also {
					sb.append("\t\treturn $it")
				}
			}.toString()

			fun printSerializeBody(): String = StringBuffer().also {
				//logger.warn("printSerializeBody: $classDeclaration")
				val bools = mutableListOf<KSPropertyDeclaration>()
				properties.forEach { property ->
					val name = property.simpleName.asString()
					val resolvedType = getPropertyType(property)
					val type = resolvedType.declaration.qualifiedName!!.asString()
					//logger.warn("analyze property $property -> $name:$resolvedType:$type")
					if (resolvedType.isBinarySerializable()) {
						it.append("\t\t$name.serialize(output)\n")
					} else if (resolvedType.isArrayType()) {
						it.append("\t\toutput.writeShort(${name}.size)\n")
						val method = getWriteMethodForType("it", resolvedType.arrayElementTypeString())
						it.append("\t\t${name}.forEach { output.$method }\n")
					} else if (resolvedType.isBoolean()) {
						bools.add(property) // assemble booleans so we can optimize their serialization
					} else if (resolvedType.isPrimitive()) {
						val method = getWriteMethodForType(name, type)
						it.append("\t\toutput.$method\n")
					} else {
						it.append("\t\t$name.serialize(output)\n")
						//throw IllegalArgumentException("Dont know how to generate serialize method for $classDeclaration.$name:$type")
					}
				}
				while (bools.isNotEmpty()) {
					when (bools.size) {
						1 -> it.append("\t\toutput.writeBoolean(${bools.takeAndRemove(1)[0].simpleName.asString()})\n")
						in 2..8 -> {
							val str = bools.takeAndRemove(8).joinToString { it.simpleName.asString() }
							it.append("\t\toutput.writeByte(IBinarySerializable.boolsToInt($str))\n")
						}

						in 9..16 -> {
							val str = bools.takeAndRemove(16).joinToString { it.simpleName.asString() }
							it.append("\t\toutput.writeShort(IBinarySerializable.boolsToInt($str))\n")
						}

						in 17..32 -> {
							val str = bools.takeAndRemove(32).joinToString { it.simpleName.asString() }
							it.append("\t\toutput.writeInt(IBinarySerializable.boolsToInt($str))\n")
						}
					}
				}
			}.toString().trimEnd()

			fun printDeserializeBody(): String = StringBuffer().also {
				//logger.warn("printDeserializeBody: $classDeclaration")
				val bools = mutableListOf<KSPropertyDeclaration>()
				properties.forEach { property ->
					val name = property.simpleName.asString()
					val resolvedType = getPropertyType(property)
					val realType = property.type.resolve()
					val type = resolvedType.declaration.qualifiedName!!.asString()
					val converter = "" //if (realType == resolvedType) "" else ".to$realType()"
					val defaultValue = resolvedType.defaultValue(property)
					if (resolvedType.isBinarySerializable()) {
						it.append("\t\t$name.deserialize(input)\n")
					} else if (resolvedType.isArrayType()) {
						val method = getReadMethodForType(name, resolvedType.arrayElementTypeString())
						it.append("\t\t$name = $type(input.readShort()) { input.$method }\n")
					} else if (resolvedType.isBoolean()) {
						bools.add(property)
					} else if (resolvedType.isPrimitive()) {
						val method = getReadMethodForType(name, type)
						it.append("\t\t$name = input.$method$converter\n")
					} else {
						it.append("\t\t$name.deserialize(input)\n")
						//throw IllegalArgumentException("Dont know how to generate deserialize method for $classDeclaration.$name:$type")
					}
				}
				while (bools.isNotEmpty()) {
					when (bools.size) {
						1 -> it.append("\t\t${bools.takeAndRemove(1)[0].simpleName.asString()} = input.readBoolean()\n")
						in 2..8 -> {
							it.append("\t\twith (IBinarySerializable.boolsFromInt(input.readUnsignedByte(), 8)) {\n")
							bools.takeAndRemove(8).forEachIndexed { index, p ->
								it.append("\t\t\t${p.simpleName.asString()} = get($index)\n")
							}
							it.append("\t\t}\n")
						}

						in 9..16 -> {
							it.append("\t\twith (IBinarySerializable.boolsFromInt(input.readUnsignedShort(), 16)) {\n")
							bools.takeAndRemove(16).forEachIndexed { index, p ->
								it.append("\t\t\t${p.simpleName.asString()} = get($index)\n")
							}
							it.append("\t\t}\n")
						}

						in 17..32 -> {
							it.append("\t\twith (IBinarySerializable.boolsFromInt(input.readUnsignedInt(), 32)) {\n")
							bools.takeAndRemove(32).forEachIndexed { index, p ->
								it.append("\t\t\t${p.simpleName.asString()} = get($index)\n")
							}
							it.append("\t\t}\n")
						}
					}
				}
			}.toString().trimEnd()

			/*
			fun printSizeBytesBody(): String = StringBuffer().also {
				var size = 0
				var bools = 0
				val other = mutableListOf<String>()
				properties.forEach { property ->
					val name = property.simpleName.asString()
					val resolvedType = getPropertyType(property)
					if (resolvedType.isBoolean()) {
						bools++
					} else if (resolvedType.isA(intType) || resolvedType.isA(uintType)) {
						size += 32
					} else if (resolvedType.isA(shortType) || resolvedType.isA(ushortType)) {
						size += 16
					} else if (resolvedType.isA(charType) || resolvedType.isA(byteType) || resolvedType.isA(ubyteType)) {
						size += 8
					} else if (resolvedType.isBinarySerializable()) {
						other.add("$name.size")
					}
				}
				if (bools > 0)
					size += (bools / 8).coerceAtLeast(8)
				if (other.size > 0)
					it.append("$size + ${other.joinToString("+")}")
				else
					it.append("$size")
			}.toString().trimEnd()*/

			fun printSizeBytesBody(): String = StringBuffer().also {
				var dynamic = false
				val bools = properties.count { it.type.resolve().isBoolean() }
				var str = properties.filter { !it.type.resolve().isBoolean() }.joinToString(" +\n\t\t\t") { property ->
					val name = property.simpleName.asString()
					val resolvedType = property.type.resolve()
					val type = resolvedType.declaration.qualifiedName!!.asString()
					if (resolvedType.isString()) {
						dynamic = true
						"16 + ${name}.length()"
					} else if (resolvedType.isPrimitive()) {
						"${type}.SIZE_BYTES"
					} else if (resolvedType.isBinarySerializable()) {
						dynamic = true
						"${type}.SIZE_BYTES"
					} else if (resolvedType.isArrayType()) {
						"$name.size *$name[0].SIZE_BYTES"
					} else {
						"${type}.SIZE_BYTES"
						//throw IllegalArgumentException("Dont know how to determine size of $type")
					}
				}

				if (bools > 0)
					str += "+ ${(bools / 8).coerceAtLeast(8)}"

				if (dynamic) {
					it.append("""by lazy {
					$str
					}""")
				} else {
					it.append("=$str")
				}

			}.toString().trimEnd()

			fun KSType.isDynamicSize(): Boolean =
				isString() || isCollection() || (isArrayType() && arrayElementType().isDynamicSize())

			fun printStaticSizeBody(): String {
				properties.firstOrNull {
					it.type.resolve().isDynamicSize()
				}?.let {
					return "false"
				}
				return "true"
			}


			imports.add("cc.lib.ksp.binaryserializer.*")
			imports.add("java.nio.ByteBuffer")

			val constructorParamDecl = classDeclaration.primaryConstructor!!.parameters.joinToString(", ") { it.toString() }
			val constructorParams = classDeclaration.primaryConstructor!!.parameters.joinToString(", ") { "${it} : ${it.type}" }

			file.print(
				"""package ${classDeclaration.packageName.asString()}
	
${imports.joinToString("\n") { "import $it" }}				
			
class $classTypeName($constructorParams) : $classDeclaration($constructorParamDecl), IBinarySerializable<$classTypeName> {

	companion object {
		const val STATIC_SIZE = ${printStaticSizeBody()}
	}
	
	override fun copy(other : $classTypeName) {
${printCopyBody()}
	}

    override fun serialize(output : ByteBuffer) {
${printSerializeBody()}
	}
	
	override fun deserialize(input : ByteBuffer) {
${printDeserializeBody()}
	}
	
	override fun contentEquals(other : $classTypeName) : Boolean {
${printContentEqualsBody()}
	}

}
"""
			)
		}
	}
}