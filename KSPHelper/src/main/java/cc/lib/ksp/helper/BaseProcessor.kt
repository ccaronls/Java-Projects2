package cc.lib.ksp.helper

import cc.lib.ksp.mirror.IData
import cc.lib.ksp.mirror.Mirrored
import cc.lib.ksp.mirror.MirroredArray
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import java.io.OutputStream

abstract class BaseProcessor(
	val codeGenerator: CodeGenerator,
	val logger: KSPLogger,
	val options: Map<String, String>
) : SymbolProcessor {

	abstract val resolver: Resolver

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

}

fun OutputStream.print(s: String) {
	write(s.toByteArray())
}
