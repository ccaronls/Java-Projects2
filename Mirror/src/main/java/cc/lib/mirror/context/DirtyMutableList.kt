package cc.lib.mirror.context

/**
 * Created by Chris Caron on 11/15/23.
 */
abstract class DirtyMutableList<T>(private val list: List<T>) : MutableList<T> {
}