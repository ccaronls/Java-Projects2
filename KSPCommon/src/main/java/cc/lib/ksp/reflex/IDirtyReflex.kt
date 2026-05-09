package cc.lib.ksp.reflex

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Created by Chris Caron on 4/30/26.
 */
interface IDirtyReflex : IReflex, IDirty {

	/**
	 * Will only write values that have a dirty delegate or are themselves IDirtyReflector(s)
	 * who are 'isDirty() == true'
	 */
	fun writeDirty(writer: JsonWriter)

	/**
	 * Merge will attempt to preserve objects rather than over-write
	 */
	fun merge(reader: JsonReader)

	/**
	 * Will write all objects if this object 'isDirty()'
	 */
	fun writeAllIfDirty(writer: JsonWriter) {
		if (isDirty()) {
			toJson(writer)
		}
	}
}