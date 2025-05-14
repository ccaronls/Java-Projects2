package cc.lib.reflector

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Alternate(vararg val variations: String)