package cc.lib.rem.annotation

/**
 * Created by Chris Caron on 11/14/23.
 */
@Target(AnnotationTarget.CLASS)
annotation class Remote

@Target(AnnotationTarget.FUNCTION)
annotation class RemoteFunction(
	val callSuper: Boolean = false
)