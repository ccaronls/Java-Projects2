package cc.lib.netcmd.processor

import cc.lib.ksp.helper.BaseProcessor
import cc.lib.ksp.helper.KSPProcessorException
import cc.lib.ksp.netcmd.INetCommand
import cc.lib.ksp.netcmd.NetCommand
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSVisitorVoid
import java.io.OutputStream

class NetCommandProcessor(
	codeGenerator: CodeGenerator,
	logger: KSPLogger,
	options: Map<String, String>
) : BaseProcessor(codeGenerator, logger, options) {

	val netCommandType: KSType
		get() = resolver.getClassDeclarationByName(INetCommand::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	fun KSType.isNetCommand(): Boolean {
		return netCommandType.isAssignableFrom(this)
	}

	val NET_COMMAND_REGISTRY_SUFFIX = "net_command_registry_name"
	val PACKAGE = "package"
	val SERIALIZED_NAME = "serializedName"

	val allNetCommands = mutableListOf<String>()

	override fun process(): List<KSAnnotated> {
		val isTestBuild = resolver.getAllFiles()
			.any { it.filePath.contains("/test/") }
		val netCommands = resolver
			.getSymbolsWithAnnotation(NetCommand::class.qualifiedName!!)
			.filterIsInstance<KSClassDeclaration>().toMutableList()

		var registrySuffix = options[NET_COMMAND_REGISTRY_SUFFIX]
			?: throw KSPProcessorException("Missing option '$NET_COMMAND_REGISTRY_SUFFIX'")
		val packageLocation = options[PACKAGE]
			?: throw KSPProcessorException("Missing option: '$PACKAGE' needed to know where to write the registry")

		if (isTestBuild)
			registrySuffix += "Test"
		val registryClassName = "NetCommandRegistry${registrySuffix.capitalize()}"

		if (netCommands.isNotEmpty()) {
			netCommands.forEach {
				allNetCommands.add(generateCommand(it))
			}
		} else if (allNetCommands.isNotEmpty()) {
			generateRegistry(packageLocation, registryClassName, allNetCommands)
			allNetCommands.clear()
		}

		return emptyList()
	}

	private fun generateCommand(symbol: KSClassDeclaration): String {
		symbol.accept(VisitorNetCommand(createFile(symbol)), Unit)
		return "${symbol.packageName.asString()}.${getDerivedClassFileName(symbol)}"
	}

	private fun generateRegistry(packageName: String, registryName: String, netCommands: List<String>) {
		val file = codeGenerator.createNewFile(
			// Make sure to associate the generated file with sources to keep/maintain it across incremental builds.
			// Learn more about incremental processing in KSP from the official docs:
			// https://kotlinlang.org/docs/ksp-incremental.html
			dependencies = Dependencies.ALL_FILES,
			packageName = packageName,
			fileName = registryName
		)

		fun printNetCmds(): String = StringBuffer().also {
			netCommands.forEach { decl ->
				it.append("      factory.register(${decl}._ID, ${decl}::read)\n")
			}
		}.toString().trimEnd()

		file.print(
			"""package $packageName
				
import cc.lib.net.*
			
class $registryName(factory: INetCommandFactory) {
   init {
${printNetCmds()}
   }
}
"""
		)
	}

	inner class VisitorNetCommand(private val file: OutputStream) : KSVisitorVoid() {

		override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {

			logger.warn("Process class: $classDeclaration")
			if (!(classDeclaration.classKind == ClassKind.INTERFACE || classDeclaration.isAbstract())) {
				throw KSPProcessorException("- Class declaration must be abstract or interface")
			}

			classDeclaration.superTypes.firstOrNull {
				it.resolve().isNetCommand()
			} ?: throw KSPProcessorException("$classDeclaration does not extend INetCommand")

			val classTypeName = getDerivedClassFileName(classDeclaration)
			logger.warn("classTypeName=$classTypeName")

			val properties = classDeclaration.getAllProperties().filter {
				it.toString() != SERIALIZED_NAME
			}

			fun printProperties() = StringBuffer().also {
				logger.warn(("properties: ${properties.joinToString()}"))

				properties.forEach { property ->
					logger.warn("Processing property: $property")
					val mod = if (property.isMutable) "var" else "val"
					val name = property.toString()
					val type = property.type.resolve()
					it.append("  override $mod $name : ${type.withPackageQualifiers()},\n")
				}

			}.toString().trimEnd()

			fun printWriter() = StringBuffer().also {
				properties.forEach { property ->
					val type = property.type.resolve()
					val name = if (type.isNullable()) {
						it.append("         $property?.let { writeByte(1)\n")
						"it"
					} else property.toString()
					if (type.isByteArray()) {
						it.append("         writeInt($name.size)\n")
						it.append("         write($name)\n")
					} else if (type.isIntArray()) {
						it.append("         writeInt($name.size)\n")
						it.append("         $name.forEach { writeInt(it) }\n")
					} else if (type.isFloatArray()) {
						it.append("         writeInt($name.size)\n")
						it.append("         $name.forEach { writeFloat(it) }\n")
					} else if (type.isArrayOfAny()) {
						it.append("         writeInt($name.size)\n")
						it.append("         $name.forEach { INetCommand.encode(this, it) }\n")
					} else if (type.isString()) {
						it.append("         writeUTF($name)\n")
					} else if (type.isShort() || type.isUShort()) {
						it.append("         writeShort($name.toInt())\n")
					} else if (type.isUInt()) {
						it.append("         writeInt($name.toInt())\n")
					} else if (type.isULong()) {
						it.append("         writeLong($name.toLong())\n")
					} else if (type.isByte() || type.isUByte()) {
						it.append("         writeByte($name.toInt())\n")
					} else if (type.isPrimitive()) {
						it.append("         write${type.makeNotNullable().toString().capitalize()}($name)\n")
					} else if (type.isEnum()) {
						it.append("         writeUTF($name.name)\n")
					} else if (type.isNetCommand()) {
						it.append("         $name.write(this)\n")
					} else {
						it.append("         INetCommand.encode(this, $name)\n")
					}
					if (type.isNullable()) {
						it.append("         }?:writeByte(0)\n")
					}

				}

			}.toString().trimEnd()

			fun printToString() = StringBuffer().also {
				var delim = "\"\""
				properties.forEach { decl ->
					it.append("      append($delim).append(INetCommand.print($decl))\n")
					delim = "\", \""
				}
			}.toString().trimEnd()

			fun printEquals() = StringBuffer().also {
				properties.forEach { prop ->
					val delim = "            && "
					if (prop.type.resolve().isArrayType()) {
						it.append("${delim}$prop?.contentEquals(it.$prop) != false\n")
					} else {
						it.append("${delim}$prop == it.$prop\n")
					}
				}

			}.toString().trimEnd()

			fun printReader() = StringBuffer().also {
				properties.forEach { property ->
					val type = property.type.resolve()
					it.append("             ")
					if (type.isNullable()) {
						it.append("if (readByte().toInt() == 0) null else ")
					}
					if (type.isIntArray()) {
						it.append("IntArray(readInt()) { readInt() },\n")
					} else if (type.isFloatArray()) {
						it.append("FloatArray(readInt()) { readFloat() },\n")
					} else if (type.isByteArray()) {
						it.append("ByteArray(readInt()).also {readFully(it) },\n")
					} else if (type.isArrayOfAny()) {
						it.append("Array(readInt()) { INetCommand.decode(this) as ${type.arrayElementType()}},\n")
					} else if (type.isString()) {
						it.append("readUTF(),\n")
					} else if (type.isUShort()) {
						it.append("readUnsignedShort().toUShort(),\n")
					} else if (type.isULong()) {
						it.append("readLong().toULong(),\n")
					} else if (type.isUByte()) {
						it.append("readUnsignedByte().toUByte(),\n")
					} else if (type.isUInt()) {
						it.append("readInt().toUInt(),\n")
					} else if (type.isPrimitive()) {
						it.append("read${type.makeNotNullable().toString().capitalize()}(),\n")
					} else if (type.isEnum()) {
						it.append("${type.makeNotNullable().declaration.qualifiedName!!.asString()}.valueOf(readUTF()),\n")
					} else if (type.isNetCommand()) {
						it.append("factory.read(this, factory)\n")
					} else {
						it.append("INetCommand.decode(this) as ${type.withPackageQualifiers()},\n")
					}
				}
			}.toString().trimEnd()

			file.print(
				"""package ${classDeclaration.packageName.asString()}
				
import cc.lib.net.impl.*
import cc.lib.ksp.netcmd.*
				
class $classTypeName(
${printProperties()}
) : ${classDeclaration.getSignature()} {
					
   override val $SERIALIZED_NAME = _ID					
					
   override fun write(stream : java.io.OutputStream) {
      with (stream.toDataOutputStream()) {
         writeUTF(_ID)
${printWriter()}
	  }
   }   
   
   override fun toString(): String = StringBuffer().apply {
      append(_ID).append(" {")
${printToString()}
	  append("}")								
   }.toString()

	override fun equals(other: Any?) : Boolean = (other as? $classTypeName)?.let {
		$SERIALIZED_NAME == other.$SERIALIZED_NAME
${printEquals()}	
    } ?: super.equals(other)

	override fun hashCode(): Int {
		return serializedName.hashCode()
	}

   companion object {
   	   
      val _ID = "$classDeclaration"
   
	  fun read(input : java.io.InputStream, factory: cc.lib.net.INetCommandFactory) : $classDeclaration = with (input.toDataInputStream()) { 
         $classTypeName(
${printReader()}
         )
      }		 
   }
}"""
			)
		}
	}
}