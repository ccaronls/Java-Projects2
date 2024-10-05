package cc.lib.rem.processor

import cc.lib.ksp.helper.BaseProcessor
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
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import java.io.OutputStream
import kotlin.reflect.KClass

class RemoteProcessor(
	codeGenerator: CodeGenerator,
	logger: KSPLogger,
	options: Map<String, String>
) : BaseProcessor(codeGenerator, logger, options) {

	val remoteType by lazy {
		resolver.getClassDeclarationByName(
			"cc.lib.ksp.remote.IRemote"
		)!!.asStarProjectedType().makeNullable()
	}

	fun KSType.isRemote(): Boolean {
		return remoteType.isAssignableFrom(this)
	}

	override fun getClassFileName(symbol: String): String {
		return symbol + "Remote"
	}

	override val annotationClass: KClass<*> = Remote::class
	override val packageName: String = "cc.lib.remote.impl"

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

	inner class Visitor(private val file: OutputStream) : KSVisitorVoid() {

		@OptIn(KspExperimental::class)
		override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {

			logger.warn("Process class: $classDeclaration")
			if (!(classDeclaration.isAbstract() || classDeclaration.isOpen())) {
				logger.error("- Class declaration must be open or abstract")
				return
			}

			val classTypeName = getClassFileName(classDeclaration.toString())
			val classArgs = getMethodSignature(classDeclaration.primaryConstructor!!)

			val baseMirrorClass: KSType =
				classDeclaration.superTypes.firstOrNull { it.resolve().isRemote() }?.resolve()
					?: throw Exception("$classDeclaration does not extend cc.lib.rem.context.Remote interface")

			val classDeclarationParams = "${classDeclaration.primaryConstructor!!.parameters.joinToString()}"

			val baseDeclaration = getClassFileName(baseMirrorClass.toString())

			val methods = classDeclaration.getAllFunctions().map { decl ->
				decl to //decl.getAnnotationsByType(RemoteFunction::class).firstOrNull()
					decl.annotations.firstOrNull { it.shortName.asString() == "RemoteFunction" }
			}.filter { it.second != null && it.first.validate() }
				.map { it.first to it.first.getAnnotationsByType(RemoteFunction::class).first() }.toList()
			file.print(
				"""package ${classDeclaration.packageName.asString()}
				
"""
			)
			imports.forEach {
				file.print("import $it\n")
			}
			file.print(
				"""
			
abstract class $classTypeName($classArgs) : $classDeclaration($classDeclarationParams) {	
"""
			)
			methods.forEach { (m, a) ->

				logger.warn("process method $m, $a")

				val paramSignature = getMethodSignature(m)

				logger.warn("- param signature: $paramSignature")

				val params =
					if (m.parameters.isNotEmpty()) ", ${m.parameters.joinToString()}" else ""

				val retType = m.returnType!!
				val retTypeResolved = retType.resolve()
				if (!(retTypeResolved.isMarkedNullable || retTypeResolved.isUnit())) {
					logger.error("Invalid return type $retType. RemoteMethods must be Unit or nullable")
					return
				}

				if (!m.modifiers.contains(Modifier.SUSPEND)) {
					logger.error("${m.simpleName.asString()} must be a suspend type")
					return
				}

				val ret = if (retTypeResolved.isUnit()) "" else "return"
				val result = if (retTypeResolved.isUnit()) "null" else "${m.returnType}::class.java"
				val cast =
					if (retTypeResolved.isUnit()) "" else " as $retType${getTypeTemplates(retType)}?"
				val retStr = if (retTypeResolved.isUnit()) "" else " : $retType?"
				val funName = m.simpleName.asString()

				file.print(
					"""
	override suspend fun $funName($paramSignature)$retStr {
	   $ret executeRemotely("$funName", $result$params)$cast"""
				)
				if (a.callSuper) {
					file.print(
						"""
		super.$funName(${params.trimStart(',')})"""
					)
				}
				file.print(
					"""
	}
"""
				)
			}

			file.print(
				"""
   final override suspend fun executeLocally(method : String, vararg args : Any?) : Any? {
      return when (method) {
"""
			)
			methods.forEach { (m, a) ->
				val args = m.parameters.mapIndexed { index, param ->
					"args[$index] as ${param.type}${
						getTypeTemplates(param.type)
					}${param.type.resolve().getNullable()}"
				}
				val funName = m.simpleName.asString()
				file.print(
					"""
         "$funName" -> $funName(${args.joinToString()})"""
				)
			}
			file.print(
				"""	
	     else -> throw NoSuchMethodError(method)
	  }
   }

}
"""
			)
		}
	}
}