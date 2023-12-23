package cc.lib.kspreflector

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import java.io.OutputStream


fun OutputStream.print(s: String) {
	write(s.toByteArray())
}

/**
 * Created by Chris Caron on 11/14/23.
 */
class ReflectorProcessor(
	private val codeGenerator: CodeGenerator,
	private val logger: KSPLogger,
	private val options: Map<String, String>,
) : SymbolProcessor {

	lateinit var resolver: Resolver

	val classes: MutableList<KSClassDeclaration> = mutableListOf()

	fun printImports(): String = StringBuffer().apply {
		classes.forEach {
			append("import ").append(it.packageName.asString()).append(".").append(it).append("\n")
		}
	}.toString()

	fun printClasses(): String = StringBuffer().apply {
		classes.forEach {
			append("   Reflector.addAllFields(${it}::class.java)\n")
		}
	}.toString()

	inner class Visitor() : KSVisitorVoid() {

		override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
			logger.warn("classDeclaration: $classDeclaration")
			classes.add(classDeclaration)
		}
	}


	fun generateMethod(file: OutputStream) {
		file.print("""package cc.lib.kreflector

${printImports()}

class ReflectionLoader {
 companion object {
  fun loadClasses() {
${printClasses()}
  }
 }
}""")
	}

	override fun process(resolver: Resolver): List<KSAnnotated> {
		this.resolver = resolver
		val symbols = resolver
			.getSymbolsWithAnnotation("cc.lib.kreflector.Reflect")
			.filterIsInstance<KSClassDeclaration>().toMutableList()

		logger.warn("options=$options")
		logger.warn("symbols=${symbols.joinToString()}")

		if (symbols.isEmpty())
			return emptyList()

		symbols.forEach { symbol ->
			symbol.accept(Visitor(), Unit)
		}

		val file = codeGenerator.createNewFile(
			// Make sure to associate the generated file with sources to keep/maintain it across incremental builds.
			// Learn more about incremental processing in KSP from the official docs:
			// https://kotlinlang.org/docs/ksp-incremental.html
			dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray()),
			packageName = options["package"] ?: "cc.lib.kreflector",
			fileName = "ReflectionLoader"
		)
		generateMethod(file)
		file.close()
		return emptyList()
	}
}
