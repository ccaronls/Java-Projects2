package cc.lib.mirror.annotation

/**
 * Created by Chris Caron on 11/14/23.
 */
@Target(AnnotationTarget.CLASS)
annotation class Mirror(val packageName: String)