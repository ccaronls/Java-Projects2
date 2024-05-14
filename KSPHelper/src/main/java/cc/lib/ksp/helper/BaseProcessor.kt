package cc.lib.ksp.helper

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSType
import java.io.OutputStream

abstract class BaseProcessor(
	private val codeGenerator: CodeGenerator,
	private val logger: KSPLogger,
	private val options: Map<String, String>
) : SymbolProcessor {

	abstract val resolver: Resolver

	val unitType by lazy {
		resolver.getClassDeclarationByName("kotlin.Unit")!!.asStarProjectedType()
	}

	fun KSType.isUnit(): Boolean {
		return unitType.isAssignableFrom(this)
	}

	fun KSType.getNullable(): String {
		return if (isMarkedNullable) "?" else ""
	}

}

fun OutputStream.print(s: String) {
	write(s.toByteArray())
}
