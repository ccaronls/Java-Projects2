package cc.lib.ksp.helper

import cc.lib.ksp.netcmd.ISerializable
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
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
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

	val listType: KSType
		get() = resolver.getClassDeclarationByName(List::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val mapType: KSType
		get() = resolver.getClassDeclarationByName(Map::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val arrayType: KSType
		get() = resolver.getClassDeclarationByName(Array::class.qualifiedName!!)!!.asStarProjectedType()

	val anyArrayType: KSType
		get() = resolver.getClassDeclarationByName(Array<Any?>::class.qualifiedName!!)!!.asStarProjectedType()

	val byteArrayType: KSType
		get() = resolver.getClassDeclarationByName(ByteArray::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val intArrayType: KSType
		get() = resolver.getClassDeclarationByName(IntArray::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val floatArrayType: KSType
		get() = resolver.getClassDeclarationByName(FloatArray::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val longArrayType: KSType
		get() = resolver.getClassDeclarationByName(LongArray::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val doubleArrayType: KSType
		get() = resolver.getClassDeclarationByName(DoubleArray::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val booleanArrayType: KSType
		get() = resolver.getClassDeclarationByName(BooleanArray::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val collectionType: KSType
		get() = resolver.getClassDeclarationByName(Collection::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val booleanType: KSType
		get() = resolver.getClassDeclarationByName(Boolean::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val byteType: KSType
		get() = resolver.getClassDeclarationByName(Byte::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val ubyteType: KSType
		get() = resolver.getClassDeclarationByName(UByte::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val charType: KSType
		get() = resolver.getClassDeclarationByName(Char::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val shortType: KSType
		get() = resolver.getClassDeclarationByName(Short::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val ushortType: KSType
		get() = resolver.getClassDeclarationByName(UShort::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val intType: KSType
		get() = resolver.getClassDeclarationByName(Int::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val uintType: KSType
		get() = resolver.getClassDeclarationByName(UInt::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val floatType: KSType
		get() = resolver.getClassDeclarationByName(Float::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val doubleType: KSType
		get() = resolver.getClassDeclarationByName(Double::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val stringType: KSType
		get() = resolver.getClassDeclarationByName(String::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val longType: KSType
		get() = resolver.getClassDeclarationByName(Long::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val ulongType: KSType
		get() = resolver.getClassDeclarationByName(ULong::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	val serializableType: KSType
		get() = resolver.getClassDeclarationByName(ISerializable::class.qualifiedName!!)!!.asStarProjectedType().makeNullable()

	fun KSType.isA(decl: KSType) = decl.isAssignableFrom(this)

	fun KSType.isString(): Boolean {
		return stringType.isAssignableFrom(this)
	}

	fun KSType.isBoolean(): Boolean {
		return booleanType.isAssignableFrom(this)
	}

	fun KSType.isPrimitive(): Boolean {
		return booleanType.isAssignableFrom(this) ||
			charType.isAssignableFrom(this) ||
			byteType.isAssignableFrom(this) ||
			ubyteType.isAssignableFrom(this) ||
			shortType.isAssignableFrom(this) ||
			ushortType.isAssignableFrom(this) ||
			intType.isAssignableFrom(this) ||
			uintType.isAssignableFrom(this) ||
			longType.isAssignableFrom(this) ||
			ulongType.isAssignableFrom(this) ||
			floatType.isAssignableFrom(this) ||
			doubleType.isAssignableFrom(this) ||
			stringType.isAssignableFrom(this)
	}

	fun KSType.isShort() = shortType.isAssignableFrom(this)
	fun KSType.isUShort() = ushortType.isAssignableFrom(this)
	fun KSType.isLong() = longType.isAssignableFrom(this)
	fun KSType.isULong() = ulongType.isAssignableFrom(this)
	fun KSType.isByte() = byteType.isAssignableFrom(this)
	fun KSType.isUByte() = ubyteType.isAssignableFrom(this)
	fun KSType.isInt() = intType.isAssignableFrom(this)
	fun KSType.isUInt() = uintType.isAssignableFrom(this)

	fun KSType.isList(): Boolean {
		return listType.isAssignableFrom(this)
	}

	fun KSType.isMap(): Boolean {
		return mapType.isAssignableFrom(this)
	}

	/**
	 * Is IntArrya, FloatArray, etc.
	 */
	fun KSType.isPrimitiveArray(): Boolean {
		return listOf(byteArrayType, intArrayType, floatArrayType, longArrayType, doubleArrayType, booleanArrayType).any {
			it.isAssignableFrom(this)
		}
	}

	fun KSType.isByteArray(): Boolean {
		return byteArrayType.isAssignableFrom(this)
	}

	fun KSType.isIntArray(): Boolean {
		return intArrayType.isAssignableFrom(this)
	}

	fun KSType.isFloatArray(): Boolean {
		return floatArrayType.isAssignableFrom(this)
	}

	/**
	 * Is Array<Any?>
	 */
	fun KSType.isArrayOfAny(): Boolean {
		return anyArrayType.isAssignableFrom(this)
	}

	fun KSType.isCollection(): Boolean {
		return collectionType.isAssignableFrom(this)
	}

	/**
	 * Match the widest possible classes of Array (TODO: Test this)
	 */
	fun KSType.isArrayType(): Boolean {
		return declaration.qualifiedName?.asString() == "kotlin.Array" || arrayType.isAssignableFrom(this) || isPrimitiveArray()
	}

	fun KSType.isSerializable(): Boolean {
		return serializableType.isAssignableFrom(this)
	}

	fun KSType.isEnum(): Boolean {
		return resolver.getClassDeclarationByName(this.declaration.qualifiedName!!)!!.classKind == ClassKind.ENUM_CLASS
	}

	fun KSType.isEnumArray(): Boolean {
		return isArrayType() && getTypeArgumentOrNull()?.isEnum() ?: false
	}

	val unitType: KSType
		get() = resolver.getClassDeclarationByName("kotlin.Unit")!!.asStarProjectedType()

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

	fun KSType.getTypeArgumentOrThrow(name: String): KSType = arguments.firstOrNull()?.type?.resolve()
		?: throw Exception("field $name type $this expecting a type argument but has none")

	fun KSType.getTypeArgumentOrNull(): KSType? = arguments.firstOrNull()?.type?.resolve()

	fun KSFunctionDeclaration.isOpen(): Boolean = modifiers.contains(Modifier.OPEN)

	fun KSFunctionDeclaration.isAbstract(): Boolean = modifiers.contains(Modifier.ABSTRACT)

	fun KSFunctionDeclaration.isOpenOrAbstract(): Boolean = isOpen() || isAbstract()


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
//		logger.warn("Fully qualified name for $this : $name")
		return name
	}

	fun KSType.getSimpleClassName(): String {
		return declaration.qualifiedName?.asString()?.substringAfterLast('.')
			?: throw Exception("Cannot get Simple class name for $this")
	}

	fun KSType.withPackageQualifiers(): String {
		val q = (declaration as? KSClassDeclaration)?.qualifiedName?.asString()!!
		return q.substringBeforeLast('.') + "." + toString()
	}

	fun KSType.validateOrThrowDeferred() {
		(declaration as? KSClassDeclaration)?.superTypes?.forEach {
			if (it.resolve().isError)
				throw DeferException("Class $it is an error")
		}
	}

	fun getTypeTemplates(ref: KSTypeReference): String {
//		logger.warn("getTypeTemplates $ref->${ref.resolve()}")
		with(ref.resolve().arguments) {
			if (isEmpty()) return ""
			return "<${joinToString { it.type.toString() }}>"
		}
	}

	fun getMethodSignature(decl: KSFunctionDeclaration): String {
		return decl.parameters.joinToString { "${it.name!!.asString()} : ${it.type.resolve().withPackageQualifiers()}" }
	}


	fun KSPropertyDeclaration.getName(): String = simpleName.asString()

	fun KSType.arrayElementTypeString(): String {
		return if (declaration.qualifiedName?.asString()?.startsWith("kotlin.Array") == true) {
			arguments[0].type?.resolve()?.declaration?.qualifiedName?.asString()
				?: throw IllegalArgumentException("Unknown array type")
		} else if (declaration.qualifiedName?.asString()?.endsWith("Array") == true) {
			"kotlin." + declaration.simpleName.asString().removeSuffix("Array")
		} else {
			throw IllegalArgumentException("Not an array type")
		}
	}

	fun KSType.arrayElementType(): KSType {
		try {
			return if (declaration.qualifiedName?.asString()?.startsWith("kotlin.Array") == true) {
				resolver.getClassDeclarationByName(arguments[0].type!!.resolve()!!.declaration!!.qualifiedName!!)?.asType(emptyList())
					?: throw IllegalArgumentException("Unknown array type")
			} else if (declaration.qualifiedName?.asString()?.endsWith("Array") == true) {
				resolver.getClassDeclarationByName("kotlin." + declaration.simpleName.asString().removeSuffix("Array"))!!.asType(emptyList())
			} else {
				throw IllegalArgumentException("Not an array type")
			}
		} catch (e: Exception) {
			throw IllegalArgumentException("Cannot determine array type from ${toFullyQualifiedName()}")
		}
	}

	/**
	 * Find a default value for a property
	 */
	fun KSType.defaultValue(): String {
		if (nullability == Nullability.NULLABLE)
			return "null"
		try {
			return match(toString()) ?: (toFullyQualifiedName() + "()")
		} catch (e: Exception) {
			throw IllegalArgumentException("No default value for '${this} : ${toString()}'. Please mark this property as nullable.")
		}
	}


	fun OutputStream.print(s: String) {
		write(s.toByteArray())
	}

	fun KClass<*>.isA(other: KClass<*>): Boolean {
		return qualifiedName == other.qualifiedName
	}

	fun KSClassDeclaration.getSignature(): String {
		val params = primaryConstructor?.let {
			"(${it.parameters.joinToString()} )"
		} ?: ""
		return toString() + params
	}

	/**
	 * Get the params signature for a constructor method including defaults
	 */
	fun KSClassDeclaration.getParamsSignature(): String {
		return primaryConstructor?.let { cons ->
			"(${
				cons.parameters.joinToString {
					"$it : ${it.type} = ${it.type.resolve().defaultValue()}"
				}
			} )"
		} ?: ""
	}

	/**
	 * Get the params as they would be passed to a base class
	 */
	fun KSClassDeclaration.getParams(): String {
		return primaryConstructor?.let { cons ->
			cons.parameters.joinToString()
		} ?: ""
	}


	fun createFile(symbol: KSClassDeclaration): OutputStream {
		return codeGenerator.createNewFile(
			// Make sure to associate the generated file with sources to keep/maintain it across incremental builds.
			// Learn more about incremental processing in KSP from the official docs:
			// https://kotlinlang.org/docs/ksp-incremental.html
			dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray()),
			packageName = symbol.packageName.asString(),
			fileName = getDerivedClassFileName(symbol).also {
				logger.warn("created file $it")
			}
		)
	}

	open fun getDerivedClassFileName(symbol: KSClassDeclaration): String {
		// if interface strip off leading 'I'
		var result = symbol.simpleName.getShortName()
		if (symbol.classKind == ClassKind.INTERFACE) {
			result = result.removePrefix("I")
		}

		// append Impl
		result += "Impl"
		return result
	}

	private var preProcessed = false

	final override fun process(resolver: Resolver): List<KSAnnotated> {
		this.resolver = resolver
		if (!preProcessed) {
			preProcess()
			preProcessed = true
		}
		return process()
	}

	/**
	 * Called once before the first process
	 */
	protected open fun preProcess() {}

	abstract fun process(): List<KSAnnotated>

	companion object {

		const val INDENT = "   "

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

abstract class SimpleProcessor(
	codeGenerator: CodeGenerator,
	logger: KSPLogger,
	options: Map<String, String>
) : BaseProcessor(codeGenerator, logger, options) {

	protected val imports = mutableSetOf<String>()

	abstract val annotationClass: KClass<*>

	abstract val packageName: String

	abstract fun getClassFileName(symbol: KSClassDeclaration): String

	abstract fun process(symbol: KSClassDeclaration, out: OutputStream)

	final override fun process(): List<KSAnnotated> {
		val symbols = resolver
			.getSymbolsWithAnnotation(annotationClass.qualifiedName!!)
			.filterIsInstance<KSClassDeclaration>().toMutableList()

//		logger.warn("options=$options")
//		logger.warn("symbols=${symbols.joinToString()}")

		options["imports"]?.let {
			imports.addAll(it.split(";"))
		}

		if (symbols.isEmpty()) {
			return emptyList()
		}

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
				packageName = options["package"] ?: symbol.packageName.toString(),
				fileName = getClassFileName(symbol)
			)
			tmpFile.streamTo(file)
		} catch (e: DeferException) {
			// try again next time
			logger.warn("Deferring symbol: $symbol because ${e.message}")
		} catch (e: java.lang.IllegalArgumentException) {
			logger.error("${symbol.location}: ${e.javaClass.simpleName}.${e.message}")
			return emptyList()
		} catch (e: Exception) {
			logger.error(e.toString())
			e.stackTrace.forEach {
				logger.error(it.toString())
			}
			return emptyList()
		}
		tmpFile.delete()

		return symbols

	}

}
