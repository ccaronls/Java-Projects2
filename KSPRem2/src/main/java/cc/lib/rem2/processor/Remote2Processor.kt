package cc.lib.rem2.processor

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
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import java.io.OutputStream
import kotlin.reflect.KClass

class Remote2Processor(
	codeGenerator: CodeGenerator,
	logger: KSPLogger,
	options: Map<String, String>
) : BaseProcessor(codeGenerator, logger, options) {

	val remoteType by lazy {
		resolver.getClassDeclarationByName(
			"cc.lib.ksp.remote.IRemote2"
		)!!.asStarProjectedType().makeNullable()
	}

	fun KSType.isRemote(): Boolean {
		return remoteType.isAssignableFrom(this)
	}

	override fun getClassFileName(symbol: String): String {
		return symbol + "Remote"
	}

	override val annotationClass: KClass<*> = Remote::class
	override val packageName: String = "cc.lib.remote2.impl"

	override fun process(symbol: KSClassDeclaration, file: OutputStream) {
		symbol.accept(Visitor(file), Unit)
	}

	fun getMethodSignature(decl: KSFunctionDeclaration): String {
		return decl.parameters.joinToString { "${it.name!!.asString()} : ${it.type.resolve().toFullyQualifiedName()}" }
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
					?: throw IllegalArgumentException("$classDeclaration does not extend cc.lib.rem.context.Remote interface")

			val classDeclarationParams = "${classDeclaration.primaryConstructor!!.parameters.joinToString()}"

			val baseDeclaration = getClassFileName(baseMirrorClass.toString())

			val methods = classDeclaration.getAllFunctions().map { decl ->
				decl to //decl.getAnnotationsByType(RemoteFunction::class).firstOrNull()
					decl.annotations.firstOrNull { it.shortName.asString() == "RemoteFunction" }
			}.filter { it.second != null && it.first.validate() }
				.map { it.first to it.first.getAnnotationsByType(RemoteFunction::class).first() }.toList()

			file.print(
				"""package ${classDeclaration.packageName.asString()}
					
import cc.lib.ksp.mirror.*
import com.google.gson.*
import com.google.gson.stream.*		
import kotlinx.serialization.json.Json

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

				val retType = m.returnType!!
				val retTypeResolved = retType.resolve()
				retTypeResolved.validateOrThrowDeferred()
				val retTypeQualified = retTypeResolved.makeNotNullable().toFullyQualifiedName()
				val retNextStr = if (retTypeResolved.isMirrored()) {
					"nextMirroredOrNull<$retTypeQualified>"
				} else if (retTypeResolved.isEnum()) {
					"enumValueOf(r.nextString())"
				} else if (retTypeResolved.isIData()) {
					"nextDataOrNull<$retTypeQualified>"
				} else "next${retType}OrNull"

				if (!(retTypeResolved.isMarkedNullable || retTypeResolved.isUnit())) {
					throw IllegalArgumentException("Invalid return type $retType. RemoteMethods must be Unit or nullable")
				}

				if (!m.modifiers.contains(Modifier.SUSPEND)) {
					throw IllegalArgumentException("${m.simpleName.asString()} must be a suspend type")
				}

				val retStr = if (retTypeResolved.isUnit()) "" else " : $retTypeQualified?"
				val funName = m.simpleName.asString()

				file.print(
					"""
	override suspend fun $funName($paramSignature)$retStr {
		context?.writer?.let { w ->
			w.beginObject()
			w.name("method").value("$funName")
			w.name("params")
			w.beginArray()
"""
				)
				m.parameters.forEach { param ->
					val resolvedType = param.type.resolve()
					if (resolvedType.isIData()) {
						file.print("\t\t\tw.value(Json.encodeToString(${resolvedType.makeNotNullable()}.serializer(), $param))\n")
					} else {
						file.print("\t\t\tw.valueOrNull($param)\n")
					}
				}
				file.print(
					"""
			w.endArray()
			w.endObject()
		}
"""
				)
				if (!retTypeResolved.isUnit()) {
					file.print(
						"""
		return context?.waitForResult()?.let { w->
			w.beginObject()
			val result = w.nextName("result").$retNextStr()
			w.endObject()
			result
		}
		"""
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
	@Throws(Exception::class)
	final override suspend fun executeLocally() {
		context?.reader?.let { r ->
			r.beginObject()
			val method = r.nextName("method").nextString()
			r.nextName("params").beginArray()
			val result : Any? = when (method) {"""
			)
			methods.forEach { (m, a) ->
				val args = m.parameters.map { param ->
					val resolved = param.type.resolve()
					resolved.validateOrThrowDeferred()

					val OrNull = if (resolved.isNullable()) "OrNull" else ""
					if (resolved.isMirrored()) {
						"r.nextMirrored$OrNull()"
					} else if (resolved.isEnum()) {
						"enumValueOf(r.nextString())"
					} else if (resolved.isIData()) {
						"r.nextData$OrNull<${resolved.toFullyQualifiedName()}>()"
					} else {
						"r.next${param.type}$OrNull()"
					}
				}
				val funName = m.simpleName.asString()
				file.print(
					"""
				"$funName" -> $funName(${args.joinToString()})"""
				)
			}
			file.print(
				"""
				else -> throw IllegalArgumentException("Unknown method : " + method) 
			}
			r.endArray()
			r.endObject()
			if (result !is Unit) {
				context?.setResult {
					it.beginObject()
					it.name("result").valueOrNull(result)
					it.endObject()
				}
			}
		}
	}"""
			)
			file.print("\n}")
		}
	}
}