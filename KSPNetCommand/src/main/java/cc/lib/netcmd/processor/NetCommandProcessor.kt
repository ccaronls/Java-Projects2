package cc.lib.netcmd.processor

import cc.lib.ksp.helper.BaseProcessor
import cc.lib.ksp.netcmd.INetCommand
import cc.lib.ksp.netcmd.NetCommand
import com.google.devtools.ksp.getClassDeclarationByName
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

	val netCommandType by lazy {
		resolver.getClassDeclarationByName(
			INetCommand::class.qualifiedName!!
		)!!.asStarProjectedType().makeNullable()
	}

	fun KSType.isNetCommand(): Boolean {
		return netCommandType.isAssignableFrom(this)
	}

	val NET_COMMAND_REGISTRY_SUFFIX = "net_command_registry_name"
	val PACKAGE = "package"
	val SERIALIZED_NAME = "serializedName"

	override fun process(): List<KSAnnotated> {
		val isTestBuild = resolver.getAllFiles()
			.any { it.filePath.contains("/test/") }
		val netCommands = resolver
			.getSymbolsWithAnnotation(NetCommand::class.qualifiedName!!)
			.filterIsInstance<KSClassDeclaration>().toMutableList()

		if (netCommands.isNotEmpty()) {
			var registrySuffix = options[NET_COMMAND_REGISTRY_SUFFIX]
				?: throw IllegalArgumentException("Missing option '$NET_COMMAND_REGISTRY_SUFFIX'")
			val packageLocation = options[PACKAGE]
				?: throw java.lang.IllegalArgumentException("Missing option: '$PACKAGE' needed to know where to write the registry")
			val netCommandImpls = mutableListOf<String>()
			netCommands.forEach {
				netCommandImpls.add(generateCommand(it))
			}
			if (isTestBuild)
				registrySuffix += "Test"
			logger.warn("registrySuffix: $registrySuffix")
			generateRegistry(packageLocation, "NetCommandRegistry${registrySuffix.capitalize()}", netCommandImpls)
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
		}.toString()

		file.print(
			"""package $packageName
				
import cc.lib.net2.*
			
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
			if (classDeclaration.classKind != ClassKind.INTERFACE) {
				throw Exception("- Class declaration must be interface")
			}

			classDeclaration.superTypes.firstOrNull {
				it.resolve().isNetCommand()
			} ?: throw java.lang.IllegalArgumentException("$classDeclaration does not extend INetCommand")

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
					it.append("  override $mod $name : $type,\n")
				}

			}.toString()

			fun printWriter() = StringBuffer().also {
				properties.forEach { property ->
					var name = property.toString()
					val type = property.type.resolve()
					if (type.isNullable()) {
						it.append("         $name?.let { writeByte(1)\n")
						name = "it"
					}
					if (type.isArrayOfAny()) {
						it.append("         writeInt($name.size)\n")
						it.append("         $name.forEach { INetCommand.encode(this, it) }\n")
					} else if (type.isByteArray()) {
						it.append("         writeInt($name.size)\n")
						it.append("         write($name)\n")
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
					} else {
						it.append("         INetCommand.encode(this, $name)\n")
					}
					if (type.isNullable()) {
						it.append("         }?:writeByte(0)\n")
					}

				}

			}.toString()

			fun printToString() = StringBuffer().also {
				var delim = "\"\""
				properties.forEach { decl ->
					it.append("      append($delim).append(INetCommand.print($decl))\n")
					delim = "\", \""
				}
			}.toString()

			fun printEquals() = StringBuffer().also {
				properties.forEach { prop ->
					val delim = "            && "
					if (prop.type.resolve().isArrayType()) {
						it.append("${delim}$prop?.contentEquals(it.$prop) != false\n")
					} else {
						it.append("${delim}$prop == it.$prop\n")
					}
				}

			}.toString()

			fun printReader() = StringBuffer().also {
				properties.forEach { property ->
					val type = property.type.resolve()
					it.append("             ")
					if (type.isNullable()) {
						it.append("if (readByte().toInt() == 0) null else ")
					}
					if (type.isArrayOfAny()) {
						it.append("Array(readInt()) { INetCommand.decode(this) },\n")
					} else if (type.isArrayType()) {
						it.append("ByteArray(readInt()).also {readUntilFull(it) },\n")
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
					} else {
						it.append("INetCommand.decode(this),\n")
					}
				}
			}.toString()

			file.print(
				"""package ${classDeclaration.packageName.asString()}
				
import cc.lib.net2.impl.*
import cc.lib.ksp.netcmd.*
				
class $classTypeName(
${printProperties()}
) : $classDeclaration {
					
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

	override fun equals(other: Any?) = (other as? $classTypeName)?.let {
		return $SERIALIZED_NAME == other.$SERIALIZED_NAME
${printEquals()}	
    } ?: super.equals(other)

   companion object {
   	   
      val _ID = "$classDeclaration"
   
	  fun read(input : java.io.InputStream) : $classDeclaration = with (input.toDataInputStream()) { 
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