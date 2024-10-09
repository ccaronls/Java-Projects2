package cc.lib.annotation

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Target(AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class RuleMeta(
	val description: String,
	val variation: String,
	val minValue: Int = 0,
	val maxValue: Int = 0,
	val order: Int = 1000
)
