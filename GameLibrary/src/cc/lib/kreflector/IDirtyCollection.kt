package cc.lib.kreflector

/**
 * Created by Chris Caron on 12/7/23.
 */
interface IDirtyCollection<T> : IDirty {
	val backing: T
}