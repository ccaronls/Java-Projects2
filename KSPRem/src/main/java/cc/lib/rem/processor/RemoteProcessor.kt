package cc.lib.rem.processor

import cc.lib.ksp.helper.SimpleProcessor
import cc.lib.ksp.remote.Remote
import cc.lib.ksp.remote.RemoteFunction
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import java.io.OutputStream
import kotlin.reflect.KClass

class RemoteProcessor(
	codeGenerator: CodeGenerator,
	logger: KSPLogger,
	options: Map<String, String>
) : SimpleProcessor(codeGenerator, logger, options) {

	val remoteType by lazy {
		resolver.getClassDeclarationByName(
			"cc.lib.ksp.remote.IRemote"
		)!!.asStarProjectedType().makeNullable()
	}

	val remoteSuspendType by lazy {
		resolver.getClassDeclarationByName(
			"cc.lib.ksp.remote.IRemoteSuspend"
		)!!.asStarProjectedType().makeNullable()
	}

	fun KSType.isRemote(): Boolean {
		return remoteType.isAssignableFrom(this)
	}

	fun KSType.isRemoteSuspend(): Boolean {
		return remoteSuspendType.isAssignableFrom(this)
	}

	fun KSType.isRemoteOrSuspendRemote(): Boolean {
		return remoteType.isAssignableFrom(this) || remoteSuspendType.isAssignableFrom(this)
	}


	override fun getClassFileName(symbol: String): String {
		return symbol + "Remote"
	}

	override val annotationClass: KClass<*> = Remote::class
	override val packageName: String = "cc.lib.remote.impl"

	override fun process(symbol: KSClassDeclaration, file: OutputStream) {
		symbol.accept(Visitor(file), Unit)
	}

	inner class Visitor(private val file: OutputStream) : KSVisitorVoid() {

		@OptIn(KspExperimental::class)
		override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {

			logger.warn("Process class: $classDeclaration")
			if (!(classDeclaration.isAbstract() || classDeclaration.isOpen())) {
				throw Exception("- Class declaration must be open or abstract")
			}

			val classTypeName = getClassFileName(classDeclaration.toString())
			val classArgs = getMethodSignature(classDeclaration.primaryConstructor!!)

			val baseMirrorClass: KSType =
				classDeclaration.superTypes.firstOrNull { it.resolve().isRemoteOrSuspendRemote() }?.resolve()
					?: throw java.lang.IllegalArgumentException("$classDeclaration does not extend cc.lib.rem.context.Remote or cc.lib.rem.context.RemoteSuspend interface")

			val needsSuspend = baseMirrorClass.isRemoteSuspend()

			val suspendType = if (needsSuspend) "suspend" else ""

			val classDeclarationParams = "${classDeclaration.primaryConstructor!!.parameters.joinToString()}"

			val methods = classDeclaration.getAllFunctions().map { decl ->
				decl to decl.annotations.firstOrNull { it.shortName.asString() == "RemoteFunction" }
			}.filter { it.second != null && it.first.validate() }
				.map {
					it.first to it.first.getAnnotationsByType(RemoteFunction::class).first()
				}.toList()
			methods.map { it.first.simpleName.asString() }.groupBy { it }.toList().firstOrNull {
				it.second.size > 1
			}?.let {
				throw IllegalArgumentException("Duplicate method name [${it.first}] not supported")
			}

			val useNet: Boolean = classDeclaration.getAnnotationsByType(Remote::class).first().useNetCmd

			file.print(
				"""package ${classDeclaration.packageName.asString()}
				
"""
			)
			imports.forEach {
				file.print("import $it\n")
			}

			fun printNetCommandParams(params: List<KSValueParameter>) = StringBuffer().also {
				params.forEach { param ->
					it.append("   val ${param.name!!.asString()} : ${param.type.resolve().withPackageQualifiers()}\n")
				}
			}.toString().trimEnd()

			fun printNetCommands() = StringBuffer().also {
				if (useNet) {
					methods.forEach { (m, a) ->
						val funName = m.simpleName.asString().capitalize()
						it.append("""
@NetCommand
interface SvrExecuteRemote${funName} : ISvrExecuteRemote {
${printNetCommandParams(m.parameters)}
}
""")
					}
				}
			}.toString()

			fun printMethods() = StringBuffer().also {
				methods.forEach { (m, a) ->

					logger.warn("process method $m, $a")

					val paramSignature = getMethodSignature(m)

					logger.warn("- param signature: $paramSignature")

					val params = m.parameters.joinToString()

					val retType = m.returnType!!
					val retTypeResolved = retType.resolve()
					if (!(retTypeResolved.isMarkedNullable || retTypeResolved.isUnit())) {
						throw Exception("Invalid return type $retType. RemoteMethods must be Unit or nullable")
					}

					if (a.callSuper && (!m.isOpen() || !retTypeResolved.isUnit())) {
						throw Exception("cannot call super on an abstract remote methods or one with a return type")
					}

					if (!m.isOpenOrAbstract()) {
						throw Exception("${m.simpleName.asString()} must be declared open or abstract")
					}

					m.modifiers.contains(Modifier.SUSPEND).also {
						if (needsSuspend && !it) {
							throw Exception("${m.simpleName.asString()} must have suspend modifier")
						} else if (!needsSuspend && it) {
							throw Exception("${m.simpleName.asString()} must have not suspend modifier")
						}
					}

					val ret = if (retTypeResolved.isUnit()) "" else "return"
					val result = if (retTypeResolved.isUnit()) "null" else "${m.returnType}::class.java"
					val resultBool = if (retTypeResolved.isUnit()) "false" else "true"
					val cast =
						if (retTypeResolved.isUnit()) "" else " as $retType${getTypeTemplates(retType)}?"
					val retStr = if (retTypeResolved.isUnit()) "" else " : $retType?"
					val funName = m.simpleName.asString()

					it.append(
						"""
	override $suspendType fun $funName($paramSignature)$retStr {""")
					if (useNet) {
						it.append("""
	   $ret executeRemotely(SvrExecuteRemote${funName.capitalize()}Impl($params, getRemoteId(), genRequestId(), $resultBool))$cast""")
					} else {
						it.append("""
	   $ret executeRemotely("$funName", $result, $params)$cast""")
					}
					if (a.callSuper) {
						it.append(
							"""
		super.$funName($params)"""
						)
					}
					it.append(
						"""
	}
""")
				}
			}.toString()

			fun printExecLocallyEntries() = StringBuffer().also {
				methods.forEach { (m, a) ->
					val args = m.parameters.mapIndexed { index, param ->
						"args[$index] as ${param.type.resolve().withPackageQualifiers()}${param.type.resolve().getNullable()}"
					}
					val funName = m.simpleName.asString()
					it.append(
						"""
         "$funName" -> $funName(${args.joinToString()})"""
					)
				}

			}.toString()

			fun printExecLocallyEntries2() = StringBuffer().also {
				methods.forEach { (m, a) ->
					val args = m.parameters.mapIndexed { index, param ->
						"cmd.$param"
					}
					val funName = m.simpleName.asString()
					it.append(
						"            is SvrExecuteRemote${funName.capitalize()} -> $funName(${args.joinToString()})\n"
					)
				}

			}.toString().trimEnd()


			file.print("""
import cc.lib.ksp.netcmd.*
import cc.lib.ksp.remote.*
			
${printNetCommands()}
			
abstract class $classTypeName($classArgs) : $classDeclaration($classDeclarationParams) {	
	${printMethods()}
""")
			if (!useNet) {
				file.print("""
    final override $suspendType fun executeLocally(method : String, vararg args : Any?) : Any? {
        return when (method) {
		  ${printExecLocallyEntries()}
	      else -> throw NoSuchMethodError(method)
	    }
   }
""")
			} else {
				file.print("""
	final override $suspendType fun executeLocally(cmd: ISvrExecuteRemote): Any? {
		return when (cmd) {
${printExecLocallyEntries2()}
		    else -> throw IllegalArgumentException(cmd.toString())
		}
	}""")
			}
			file.print("""			
}
"""
			)
		}
	}
}
