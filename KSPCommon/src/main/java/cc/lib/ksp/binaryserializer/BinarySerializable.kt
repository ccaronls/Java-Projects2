package cc.lib.ksp.binaryserializer

import kotlin.reflect.KClass

/**
 * Created by Chris Caron on 3/18/25.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class BinarySerializable(
	val className: String
)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class BinaryType(
	val classType: KClass<*>
)