package cc.lib.ksp.mirror

/**
 * Mirror annotation is used by MirrorProcessor to generate serializable objects with some distinct benefits
 *
 * The main benefit is the notion of 'dirtiness' that always for smaller cargo loads by only serializing
 * data marked as 'dirty'
 *
 * for each 'var' type, a property is generated with customized 'set' that will trac k dirtiness for whole object
 * for each 'val' type, a var is generated with a protected setter
 *
 * The following methods are generated:
 * fromGson
 * toGson
 * copyFrom
 * contentEquals
 * hashValue
 * isDirty
 * markClean
 * toString
 *
 *
 *
 */


enum class DirtyType {
	// ALWAYS, TODO() // class will not track dirtiness but always return true from isDirty
	NEVER, // class will not track dirtiness nor ever return true from isDirty
	ANY,   // class will track dirtiness with a single boolean. If anything dirty then all are dirty
	COMPLEX // class will track dirtiness on a per field basis
}

/**
 * Created by Chris Caron on 11/14/23.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Mirror(val dirtyType: DirtyType = DirtyType.COMPLEX)
