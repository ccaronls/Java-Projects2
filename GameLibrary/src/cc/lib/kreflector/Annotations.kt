package cc.lib.kreflector

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Use this annotation to Omit field for usage by Reflector
 *
 *
 * If a class extends Reflector, and that class has called:
 * static {
 * addAllFields(...)
 * }
 *
 *
 * then this annotation is the same as:
 *
 *
 * int myField;
 *
 *
 * static {
 * omitField("myField");
 * }
 *
 * @author chriscaron
 */
@Target(AnnotationTarget.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
annotation class Omit

@Target(AnnotationTarget.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
annotation class Alternate(val variations: Array<String>)