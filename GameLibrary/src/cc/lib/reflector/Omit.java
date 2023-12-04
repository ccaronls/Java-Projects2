package cc.lib.reflector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to Omit field for usage by Reflector
 * <p>
 * If a class extends Reflector, and that class has called:
 * static {
 * addAllFields(...)
 * }
 * <p>
 * then this annotation is the same as:
 * <p>
 * int myField;
 * <p>
 * static {
 * omitField("myField");
 * }
 *
 * @author chriscaron
 */

@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Omit {
}