package cc.lib.ksp.reflector

import com.google.gson.stream.JsonReader

/**
 * Created by Chris Caron on 5/20/24.
 */
class ReflectorContext {

	companion object {
		private val classes = mutableMapOf<String, () -> Reflector<*>>()

		fun register(name: String, creator: () -> Reflector<*>) {
			if (classes.containsKey(name)) {
				throw IllegalArgumentException("Duplicate class name: $name")
			}
			classes[name] = creator
		}

		fun create(name: String): Reflector<*> {
			return classes[name]!!.invoke()
		}

		fun <T : Reflector<T>> deserialize(reader: JsonReader): T {
			return with(create(reader.nextName())) {
				reader.beginObject()
				while (reader.hasNext())
					fromGson(reader, reader.nextName())
				reader.endObject()
			} as T
		}
	}
}