package cc.game.soc.core.annotations;

import java.lang.annotation.*;

/**
 * Used by the Rules file so apps can create a dynamic configuration popups and such
 * 
 * The motivation is that new rules are common and it is nice to have config popups 
 * that grow automatically without maintenence.
 * 
 * @author chriscaron
 *
 */

@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface RuleVariable {
	String description();
	int minValue() default 0;
	int maxValue() default 0;
	int valueStep() default 1;
	String separator() default "";
}
