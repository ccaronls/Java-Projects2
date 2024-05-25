package cc.lib.ksp.reflector

@Target(AnnotationTarget.FIELD)
annotation class Alternates(val variation: String, vararg val additional: String)