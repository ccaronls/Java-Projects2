package cc.lib.ksp.remote

@Target(AnnotationTarget.FUNCTION)
annotation class RemoteFunction(
	val callSuper: Boolean = false
)