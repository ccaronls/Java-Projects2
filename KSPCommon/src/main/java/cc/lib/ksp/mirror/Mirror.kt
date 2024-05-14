package cc.lib.ksp.mirror

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
annotation class Mirror(val dirtyType: DirtyType = DirtyType.COMPLEX)
