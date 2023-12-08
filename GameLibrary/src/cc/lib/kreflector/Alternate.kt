package cc.lib.kreflector

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Target(AnnotationTarget.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
annotation class Alternate(val variations: Array<String>)