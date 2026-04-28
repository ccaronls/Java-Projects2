package cc.lib.rem.processor

import cc.lib.ksp.helper.SimpleProcessor
import cc.lib.ksp.remote.IRemote
import cc.lib.ksp.remote.Remote
import cc.lib.ksp.remote.RemoteFunction
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
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

	fun KSType.isRemote(): Boolean {
		return remoteType.isAssignableFrom(this)
	}

	override fun getClassFileName(symbol: String): String {
		return symbol.trimStart('I') + "Remote"
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
			val classArgs = classDeclaration.primaryConstructor?.let {
				getMethodSignature(it)
			} ?: ""

			classDeclaration.superTypes.firstOrNull { it.resolve().isRemote() }?.resolve()
				?: throw java.lang.IllegalArgumentException("$classDeclaration does not extend ${IRemote::class.qualifiedName} interface")
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

			val id: String = classDeclaration.getAnnotationsByType(Remote::class).first().id

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
				methods.forEach { (m, a) ->
					val funName = m.simpleName.asString().capitalize()
					it.append("""
@NetCommand
interface SvrExecuteRemote${funName} : ISvrExecuteRemote""")
					if (m.parameters.isNotEmpty()) {
						it.append(""" {
${printNetCommandParams(m.parameters)}
}""")
					}
				}
			}.toString()

			fun printMethods() = StringBuffer().also {
				methods.forEach { (m, a) ->

					logger.warn("process method $m, $a")

					val paramSignature = getMethodSignature(m)

					logger.warn("- param signature: $paramSignature")

					val params = m.parameters.joinToString()
					val comma = if (params.isBlank()) "" else ", "

					val retType = m.returnType!!
					val retTypeResolved = retType.resolve()
					if (!(retTypeResolved.isMarkedNullable || retTypeResolved.isUnit())) {
						throw Exception("Invalid return type $retType. RemoteMethods must be Unit or nullable")
					}

					if (a.callSuper && (!m.isOpen() || !retTypeResolved.isUnit())) {
						throw Exception("cannot call super on an abstract remote methods or one with a return type")
					}

					if (classDeclaration.classKind == ClassKind.CLASS && !m.isOpenOrAbstract()) {
						throw Exception("${m.simpleName.asString()} must be declared open or abstract")
					}

					val funName = m.simpleName.asString()
					val blocking = !retTypeResolved.isUnit() || m.modifiers.contains(Modifier.SUSPEND)
					val returns = !retTypeResolved.isUnit()
					if (returns && a.callSuper)
						throw java.lang.IllegalArgumentException("Method '$funName' cannot be marked callSuper==true if it returns a value")

					// 3 cases:
					// - execute a non-suspend non-blocking call with no return
					// - execute a blocking suspend call with no return
					// - execute a blocking suspend call with a return

					if (!blocking && returns) {
						throw java.lang.IllegalArgumentException("Method '$funName' returns a value but not marked suspend")
					} else if (!blocking && !returns) {
						it.append("""
    override fun $funName($paramSignature) {							
		executeRemotely(SvrExecuteRemote${funName.capitalize()}Impl($params${comma}_remoteId, false))""")
						if (a.callSuper) {
							it.append("\n       super.$funName($params)")
						}
						it.append("\n   }")
					} else if (blocking && !returns) {
						it.append("""
    override suspend fun $funName($paramSignature) {							
		executeRemotelyBlocking(SvrExecuteRemote${funName.capitalize()}Impl($params${comma}_remoteId, false))""")
						if (a.callSuper) {
							it.append("\n       super.$funName($params)")
						}
						it.append("\n   }")
					} else { // blocking && returns
						it.append("""
	override suspend fun $funName($paramSignature) : $retType? {
		return executeRemotelyBlocking(SvrExecuteRemote${funName.capitalize()}Impl($params${comma}_remoteId, true)) as $retType${getTypeTemplates(retType)}?
	}""")
					}
				}
			}.toString()

			fun printExecLocallyEntries() = StringBuffer().also {
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
			
abstract class $classTypeName($classArgs) : ${classDeclaration.getSignature()} {
	override val _remoteId = "$id"
	
	${printMethods()}

	final override suspend fun executeLocally(cmd: ISvrExecuteRemote): Any? {
		return when (cmd) {
${printExecLocallyEntries()}
		    else -> throw IllegalArgumentException(cmd.toString())
		}
	}			
}
"""
			)
		}
	}
}
