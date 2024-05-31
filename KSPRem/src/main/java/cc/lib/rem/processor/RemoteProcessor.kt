package cc.lib.rem.processor

import cc.lib.ksp.remote.Remote
import cc.lib.ksp.remote.RemoteFunction
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import java.io.OutputStream

class RemoteProcessor(
	private val codeGenerator: CodeGenerator,
	private val logger: KSPLogger,
	private val options: Map<String, String>
) : SymbolProcessor {

	lateinit var resolver: Resolver

	val remoteType by lazy {
		resolver.getClassDeclarationByName(
			"cc.lib.ksp.remote.IRemote"
		)!!.asStarProjectedType().makeNullable()
	}

	val unitType by lazy {
		resolver.getClassDeclarationByName("kotlin.Unit")!!.asStarProjectedType()
	}

	fun KSType.isRemote(): Boolean {
		return remoteType.isAssignableFrom(this)
	}

	fun KSType.isUnit(): Boolean {
		return unitType.isAssignableFrom(this)
	}

	fun KSType.getNullable(): String {
		return if (isMarkedNullable) "?" else ""
	}

	fun getClassFileName(symbol: String): String {
		return symbol + "Remote"
	}

	val imports = mutableSetOf<String>()

	override fun process(resolver: Resolver): List<KSAnnotated> {
		this.resolver = resolver
		val symbols = resolver
			.getSymbolsWithAnnotation(Remote::class.qualifiedName!!)
			.filterIsInstance<KSClassDeclaration>().toMutableList()

		logger.warn("options=$options")
		logger.warn("symbols=${symbols.joinToString()}")

		options["imports"]?.let {
			imports.addAll(it.split("[, ]"))
		}

		if (symbols.isEmpty()) return emptyList()

		val symbol = symbols.removeAt(0)

		val file = codeGenerator.createNewFile(
			// Make sure to associate the generated file with sources to keep/maintain it across incremental builds.
			// Learn more about incremental processing in KSP from the official docs:
			// https://kotlinlang.org/docs/ksp-incremental.html
			dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray()),
			packageName = options["package"] ?: "cc.lib.remote.impl",
			fileName = getClassFileName(symbol.simpleName.asString())
		)
		symbol.accept(Visitor(file), Unit)
		file.close()
		return symbols

	}

	fun OutputStream.print(s: String) {
		write(s.toByteArray())
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

		/*
	return decl.parameters.joinToString {
		"${it.name?.asString()}:${it.type}${
			getTypeTemplates(it.type)
		}${it.type.resolve().getNullable()}"
	}*/
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

			/*			val filteredMethods = methods.map {
							it to Pair(it.parameters.map {
								Pair(it.name!!, it.type.resolve())
							}.toList(), it.returnType!!.resolve())
						}.toMap()*/

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

				val ret = if (retTypeResolved.isUnit()) "" else "return"
				val result = if (retTypeResolved.isUnit()) "null" else "${m.returnType}::class.java"
				val cast =
					if (retTypeResolved.isUnit()) "" else " as $retType${getTypeTemplates(retType)}?"
				val retStr = if (retTypeResolved.isUnit()) "" else " : $retType?"
				val funName = m.simpleName.asString()

				file.print(
					"""
	override fun $funName($paramSignature)$retStr {
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
   final override fun executeLocally(method : String, vararg args : Any?) : Any? {
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