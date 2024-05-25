package cc.lib.ksp.reflector

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Created by Chris Caron on 5/19/24.
 */
interface Reflector<T> {

	/**
	 * Implementors should serialize their fields and then call super.toGson to
	 * serialize out fields they inherit
	 */
	fun toGson(writer: JsonWriter) {}

	/**
	 * Implementors should match the incoming name to theirs fields if possible
	 * or cal super.fromGson
	 */
	fun fromGson(reader: JsonReader, name: String) {
		System.err.println("Unhandled name $name")
		reader.skipValue()
	}

	//fun <T> deepCopy(other: T) : T

}