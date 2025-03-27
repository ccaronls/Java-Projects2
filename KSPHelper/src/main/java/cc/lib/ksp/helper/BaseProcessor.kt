package cc.lib.ksp.helper

import cc.lib.ksp.mirror.IData
import cc.lib.ksp.mirror.Mirrored
import cc.lib.ksp.mirror.MirroredArray
import cc.lib.utils.streamTo
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.reflect.KClass

class DeferException(msg: String) : Exception(msg)

abstract class BaseProcessor(
	val codeGenerator: CodeGenerator,
	val logger: KSPLogger,
	val options: Map<String, String>
) : SymbolProcessor {

	lateinit var resolver: Resolver

	val listType by lazy {
		resolver.getClassDeclarationByName(List::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()
	}

	val mapType by lazy {
		resolver.getClassDeclarationByName(Map::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()
	}

	val mirrorType by lazy {
		resolver.getClassDeclarationByName(
			Mirrored::class.qualifiedName!!
		)!!.asStarProjectedType().makeNullable()
	}

	val idataType by lazy {
		resolver.getClassDeclarationByName(
			IData::class.qualifiedName!!
		)!!.asStarProjectedType().makeNullable()
	}

	val mirroredArrayType by lazy {
		resolver.getClassDeclarationByName(
			MirroredArray::class.qualifiedName!!
		)!!.asStarProjectedType().makeNullable()
	}

	val arrayType by lazy {
		resolver.getClassDeclarationByName(Array::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()
	}

	val collectionType by lazy {
		resolver.getClassDeclarationByName(Collection::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()
	}

	val booleanType by lazy {
		resolver.getClassDeclarationByName(Boolean::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()
	}

	val byteType by lazy {
		resolver.getClassDeclarationByName(Byte::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()
	}

	val charType by lazy {
		resolver.getClassDeclarationByName(Char::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()
	}

	val shortType by lazy {
		resolver.getClassDeclarationByName(Short::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()
	}

	val intType by lazy {
		resolver.getClassDeclarationByName(Int::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()
	}

	val floatType by lazy {
		resolver.getClassDeclarationByName(Float::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()
	}

	val doubleType by lazy {
		resolver.getClassDeclarationByName(Double::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()
	}

	val stringType by lazy {
		resolver.getClassDeclarationByName(String::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()
	}

	val longType by lazy {
		resolver.getClassDeclarationByName(Long::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()
	}

	fun KSType.isMirrored(): Boolean {
		return mirrorType.isAssignableFrom(this)
	}

	fun KSType.isString(): Boolean {
		return stringType.isAssignableFrom(this)
	}

	fun KSType.isIData(): Boolean {
		return idataType.isAssignableFrom(this)
	}

	fun KSType.isPrimitive(): Boolean {
		return booleanType.isAssignableFrom(this) ||
			intType.isAssignableFrom(this) ||
			stringType.isAssignableFrom(this) ||
			longType.isAssignableFrom(this) ||
			floatType.isAssignableFrom(this) ||
			byteType.isAssignableFrom(this) ||
			charType.isAssignableFrom(this) ||
			shortType.isAssignableFrom(this) ||
			doubleType.isAssignableFrom(this)
	}

	fun KSType.isList(): Boolean {
		return listType.isAssignableFrom(this)
	}

	fun KSType.isMap(): Boolean {
		return mapType.isAssignableFrom(this)
	}

	fun KSType.isArray(): Boolean {
		return arrayType.isAssignableFrom(this)
	}

	fun KSType.isCollection(): Boolean {
		return collectionType.isAssignableFrom(this)
	}

	fun KSType.isArrayType(): Boolean {
		return declaration.qualifiedName?.asString()?.startsWith("kotlin.Array") == true ||
			declaration.qualifiedName?.asString()?.endsWith("Array") == true
	}

	fun KSType.isMirroredArray(): Boolean {
		return mirroredArrayType.isAssignableFrom(this)
	}

	fun KSType.isEnum(): Boolean {
		return resolver.getClassDeclarationByName(this.declaration.qualifiedName!!)!!.classKind == ClassKind.ENUM_CLASS
	}

	val unitType by lazy {
		resolver.getClassDeclarationByName("kotlin.Unit")!!.asStarProjectedType()
	}

	fun KSType.isUnit(): Boolean {
		return unitType.isAssignableFrom(this)
	}

	fun KSType.getNullable(): String {
		return if (isMarkedNullable) "?" else ""
	}

	fun KSType.isNullable(): Boolean = nullability == Nullability.NULLABLE

	fun KSType.getTypeArguments(): String {
		if (arguments.isEmpty()) return ""
		return "<${arguments.joinToString { it.type!!.resolve().toFullyQualifiedName() }}>"
	}

	/**
	 * Gets string version of the type with nullability qualifier and template arguments
	 */
	fun KSType.toFullyQualifiedName(): String {
		var qualifiedName = (declaration as? KSClassDeclaration)?.qualifiedName?.asString()
			?: throw IllegalArgumentException("Cannot get fully qualified name for $this")
		var name = if (qualifiedName.startsWith("kotlin")) {
			(declaration as? KSClassDeclaration)?.simpleName?.asString()!!
		} else qualifiedName
		name += getTypeArguments()
		if (isNullable())
			name = "$name?"
		logger.warn("Fully qualified name for $this : $name")
		return name
	}

	fun KSType.validateOrThrowDeferred() {
		(declaration as? KSClassDeclaration)?.superTypes?.forEach {
			if (it.resolve().isError)
				throw DeferException("Class $it is an error")
		}
	}

	fun KSPropertyDeclaration.getName(): String = simpleName.asString()

	fun KSType.arrayElementType(): String {
		return if (declaration.qualifiedName?.asString()?.startsWith("kotlin.Array") == true) {
			arguments[0].type?.resolve()?.declaration?.qualifiedName?.asString()
				?: throw IllegalArgumentException("Unknown array type")
		} else if (declaration.qualifiedName?.asString()?.endsWith("Array") == true) {
			"kotlin." + declaration.simpleName.asString().removeSuffix("Array")
		} else {
			throw IllegalArgumentException("Not an array type")
		}
	}

	fun KSType.defaultValue(decl: KSPropertyDeclaration): String {
		if (nullability == Nullability.NULLABLE)
			return "null"
		try {
			return match(toString()) ?: (decl.type.resolve().toFullyQualifiedName() + "()")
		} catch (e: Exception) {
			throw IllegalArgumentException("No default value for '${decl.getName()} : ${toString()}'. Please mark this property as nullable.")
		}
	}


	fun OutputStream.print(s: String) {
		write(s.toByteArray())
	}

	protected val imports = mutableSetOf<String>()

	abstract val annotationClass: KClass<*>

	abstract val packageName: String

	@Throws
	abstract fun process(symbol: KSClassDeclaration, file: OutputStream)

	abstract fun getClassFileName(symbol: String): String

	final override fun process(resolver: Resolver): List<KSAnnotated> {
		this.resolver = resolver
		val symbols = resolver
			.getSymbolsWithAnnotation(annotationClass.qualifiedName!!)
			.filterIsInstance<KSClassDeclaration>().toMutableList()

		logger.warn("options=$options")
		logger.warn("symbols=${symbols.joinToString()}")

		options["imports"]?.let {
			imports.addAll(it.split(";"))
		}

		if (symbols.isEmpty()) return emptyList()

		val symbol = symbols[0]

		// TODO: Copy this process to base processor
		val tmpFile = File.createTempFile("/tmp/", "kspXXXXX.kt")
		try {
			FileOutputStream(tmpFile).use { os ->
				//symbol.accept(Visitor(os), Unit)
				process(symbol, os)
			}
			symbols.removeFirst()
			val file = codeGenerator.createNewFile(
				// Make sure to associate the generated file with sources to keep/maintain it across incremental builds.
				// Learn more about incremental processing in KSP from the official docs:
				// https://kotlinlang.org/docs/ksp-incremental.html
				dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray()),
				packageName = options["package"] ?: packageName,
				fileName = getClassFileName(symbol.simpleName.asString())
			)
			tmpFile.streamTo(file)
		} catch (e: DeferException) {
			// try again next time
			logger.warn("Deferring symbol: $symbol because ${e.message}")
		} catch (e: Exception) {
			logger.error("${symbol.location}:" + e.message!!)
			return emptyList()
		}
		tmpFile.delete()

		return symbols

	}

	companion object {
		private val defaultValueRegExMap = mapOf(
			"Byte" to "0",
			"Short" to "0",
			"Int" to "0",
			"Long" to "0L",
			"Float" to "0f",
			"Double" to "0.0",
			"Boolean" to "false",
			"String" to "\"\"",
			"Char" to "'0'",
			"(Mutable|Mirrored)?List<(in|out)?(.+)>" to "listOf<$3>().toMirroredList()",
			"(Mutable|Mirrored)?Map<(in|out)?(.+)>" to "mapOf<$3>().toMirroredMap()",
			"MirroredArray<(in|out)?(.*)>" to "arrayOf<$2>().toMirroredArray()"
		).map {
			it.key.toRegex() to it.value
		}

		private fun match(value: String): String? {
			val options = defaultValueRegExMap.map {
				it.first.matchEntire(value) to it.second
			}.filter { it.first != null }

			if (options.isEmpty())
				return null
			if (options.size != 1) {
				throw IllegalArgumentException("Expecting 1 option for $value but found: ${options.joinToString { it.first!!.value }}")
			}

			val matcher = options[0].first!!
			var result = options[0].second
			for (idx in 1 until matcher.groupValues.size) {
				result = result.replace("$$idx", matcher.groupValues[idx])
			}

			return result
		}
	}

}
