package cc.lib.kreflector

import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.utils.GException
import java.io.*
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import java.util.zip.CRC32
import java.util.zip.Checksum

/**
 * Derive from this class to handle copying, equals, serializing, deserializing, diffing and more.
 *
 * Serialization of primitives, arrays and subclasses of Reflector are supported.
 * Also collections are supported if their data types are one of the afore mentioned
 * New types or can be added if they implement an Archiver.
 *
 * Ways to use this class:
 *
 * 1. Extend Reflector and Don't override serialize and deserialize, simply call addAllField within a static
 * block of your class. Fields you dont want serialized should be annotated with @Omit
 *
 * primitives, primitive arrays, Object Arrays, enums, Collections, Maps, other Reflectors.
 * Fields of external types need to have an archiver attached like so:
 * class A extends Reflector<A> {
 * java.awt.Color color;
 * static {
 * addField(A.class, "color", new Reflector.AArchiver() {
 *
 * @Override
 * public Object parse(String value) throws Exception {
 * return Utils.stringToColor(value);
 * }
 *
 * @Override
 * public String getStringValue(Object obj) {
 * return Utils.colorToString((Color)obj);
 * }
 * };
 * }
 * }
 *
 * 2. Extend this class and override serialize/deserialize.  Use println/readLine to read pre trimmed strings
 * from reader.  When reader returns null, there is no more input.  Example:
 * class A extends Reflector<A> {
 * String a;
 * int b;
 *
 * public void serialize(PrintWriter out) {
 * out.println(a);
 * out.println(b);
 * }
 *
 * public void deserialize(BufferedReader in) {
 * a = in.readLine();
 * b = Integer.parseInt(in.readLine());
 * }
 * }
 *
 * 3. User static serialze/deserialize methods on known objects, enums, arrays, primitives, maps, collections
 *
 * Known Limitations:
 *
 * - Reflector cannot handle an Object array. Use a Collection if you want serialize unrelated types.
 * - Use caution when hashing reflector objects. If you rely on Object.hashCode, then objects can get
 * lost after new instances are created for serialization. Consider implementing hashCode in reflector
 * objects you intend to be hashed.
 *
 *
 * NOTE: Derived classes must support a public zero argument constructor for de-serialization
 * @author ccaron
</A></A> */
open class Reflector<T> : IDirty {
	@Synchronized
	@Throws(IOException::class)
	protected open fun serialize(out: RPrintWriter) {
		try {
			val values = getValues(javaClass, false)
			for (field in values!!.keys) {
				val archiver = values[field]
				field.isAccessible = true
				val obj = field[this@Reflector]
				val name = getName(field)
				if (obj == null) {
					out.p(name).println("=null")
					continue
				}
				out.p(name).p("=").p(archiver!![field, this])
				serializeObject(obj, out, false)
			}
		} catch (e: IOException) {
			throw e
		} catch (e: Exception) {
			throw ParseException(out.lineNum, e)
		}
	}

	/**
	 * Override this to do extra handling. Derived should call super.
	 *
	 * @param out
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun serialize(out: OutputStream) {
		serialize(RPrintWriter(out))
	}

	/**
	 *
	 * @param out
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun serialize(out: PrintWriter) {
		serialize(RPrintWriter(out))
	}

	/**
	 *
	 * @param text
	 * @throws IOException
	 */
	@Synchronized
	@Throws(IOException::class)
	fun deserialize(text: String) {
		val reader = RBufferedReader(StringReader(text))
		try {
			deserializeInternal(reader, false)
		} catch (e: IOException) {
			throw e
		} catch (e: Exception) {
			throw ParseException(reader.lineNum, e)
		} finally {
			reader.close()
		}
	}

	/**
	 * Same as deserialize with KEEP_INSTANCES enabled.
	 *
	 * @param diff
	 * @throws IOException
	 */
	@Synchronized
	@Throws(IOException::class)
	fun merge(diff: String) {
		merge(RBufferedReader(StringReader(diff)))
	}

	/**
	 *
	 * @param in
	 * @throws IOException
	 */
	@Synchronized
	@Throws(IOException::class)
	fun deserialize(reader: InputStream) {
		val reader = RBufferedReader(InputStreamReader(reader))
		try {
			deserializeInternal(reader, false)
		} catch (e: IOException) {
			throw e
		} catch (e: Exception) {
			throw ParseException(reader.lineNum, e)
		} finally {
			reader.close()
		}
	}

	/**
	 * initialize fields of this object there are explicitly added by addField for this class type.
	 *
	 * @param _in
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun deserialize(_in: Reader) {
		var reader: RBufferedReader? = null
		reader = if (_in is RBufferedReader) {
			_in
		} else {
			RBufferedReader(_in!!)
		}
		try {
			deserializeInternal(reader, false)
		} catch (e: IOException) {
			throw e
		} catch (e: Exception) {
			throw ParseException(reader!!.lineNum, e)
		} finally {
			reader!!.close()
		}
	}

	/**
	 * initialize fields of this object there are explicitly added by addField for this class type.
	 *
	 * @param in
	 * @throws IOException
	 */
	@Throws(IOException::class)
	protected open fun deserialize(reader: RBufferedReader) {
		try {
			deserializeInternal(reader, false)
		} catch (e: IOException) {
			throw e
		} catch (e: Exception) {
			throw ParseException(reader.lineNum, e)
		}
	}

	@Synchronized
	@Throws(IOException::class)
	fun merge(reader: InputStream) {
		merge(RBufferedReader(InputStreamReader(reader)))
	}

	@Synchronized
	@Throws(IOException::class)
	fun merge(reader: BufferedReader) {
		merge((reader as? RBufferedReader ?: RBufferedReader(reader!!)))
	}

	@Throws(IOException::class)
	protected fun merge(reader: RBufferedReader) {
		try {
			deserializeInternal(reader, true)
		} catch (e: IOException) {
			throw e
		} catch (e: Exception) {
			throw ParseException(reader.lineNum, e)
		}
	}

	@Throws(Exception::class)
	private fun deserializeInternal(reader: RBufferedReader, keepInstances: Boolean) {
		val values = getValues(javaClass, false)
		val depth = reader!!.depth
		while (true) {
			if (reader.depth > depth) if (reader.readLine() != null) throw ParseException(reader.lineNum, " Expected closing '}'")
			val line = reader.readLineOrEOF() ?: break
			var parts: Array<String>? = line.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			if (parts!!.size < 2) throw ParseException(reader.lineNum, " not of form 'name=value'")
			val name = parts[0].trim { it <= ' ' }
			for (field in values!!.keys) {
				if (fieldMatches(field, name)) {
					val archiver = values[field]
					val instance = field[this]
					archiver!![instance, field, parts!![1], this] = keepInstances
					val obj = field[this]
					if (obj is Reflector<*>) {
						val ref = obj as Reflector<T>
						if (keepInstances) ref.merge(reader) else ref.deserialize(reader)
					} else if (field[this@Reflector] is DirtyDelegate<*>) {
						(obj as DirtyDelegate<*>).deserialize(reader, keepInstances)
					} else if (field.type.isArray) {
						if (obj != null) {
							val arrayArchiver = getArchiverForType(obj.javaClass.componentType)
							arrayArchiver.deserializeArray(obj, reader, keepInstances)
						}
					} else if (isSubclassOf(field.type, MutableCollection::class.java)) {
						val collection = obj as MutableCollection<Any>
						if (collection != null) deserializeCollection(collection, reader, keepInstances)
					} else if (isSubclassOf(field.type, MutableMap::class.java)) {
						val map = obj as MutableMap<Any, Any?>
						if (map != null) deserializeMap(map, reader, keepInstances)
					}
					parts = null
					break
				}
			}
			if (parts != null) {
				if (THROW_ON_UNKNOWN) throw ParseException(reader.lineNum, "Unknown field: " + name + " not in fields: " + values.keys)
				log.error("Unknown field: $name not found in class: $javaClass") // + " not in fields: " + values.keySet());
				// skip ahead until depth matches current depth
				while (reader.depth > depth) {
					reader.readLineOrEOF()
				}
			}
		}
	}

	fun deepEquals(a: Reflector<T>?): Boolean {
		if (a === this) return true
		return if (a == null) false else try {
			val values = getValues(javaClass, false)
			for (f in values!!.keys) {
				try {
					if (!isEqual(f[this], f[a])) return false
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
			true
		} catch (e: Exception) {
			super.equals(a)
		}
	}

	override fun equals(obj: Any?): Boolean {
		return isEqual(this, obj)
	}

	override fun toString(): String {
		val buf = StringWriter()
		try {
			RPrintWriter(buf).use { reader -> serialize(reader) }
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return buf.toString()
	}

	fun toStringNumbered(): String {
		val buf = StringWriter()
		try {
			RPrintWriter(buf, true).use { reader -> serialize(reader) }
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return buf.toString()
	}

	/**
	 * Deep copy all fields explicitly set be addField.  Any other fields are NOT deep copied, nor initialized.
	 *
	 * @return
	 */
	fun deepCopy(): T {
		return try {
			val copy: Any = javaClass.newInstance()
			getValues(javaClass, false).keys.forEach { f ->
				f.isAccessible = true
				f[copy] = deepCopy(f[this])
			}
			copy as T
		} catch (e: Exception) {
			throw GException("Failed to deep copy", e)
		}
	}

	/**
	 * Collections, Arrays and Maps are shallow copied from this into a new instance
	 *
	 * @return
	 */
	fun shallowCopy(): T {
		return try {
			val copy: Any = javaClass.newInstance()
			val values = getValues(javaClass, false)
			for (f in values!!.keys) {
				f.isAccessible = true
				f[copy] = shallowCopy(f[this])
			}
			copy as T
		} catch (e: Exception) {
			throw GException("Failed to shallow copy")
		}
	}

	/**
	 * @param other
	 */
	@Synchronized
	fun copyFrom(other: Reflector<T>): T {
		if (other === this) {
			log.error("Copying from self?")
			return this as T
		}
		return try {
			val values = getValues(javaClass, false)
			val otherValues = getValues(other.javaClass, false)
			for (f in values!!.keys) {
				if (!otherValues!!.containsKey(f)) continue
				f.isAccessible = true
				val o = f[this]
				if (o != null && o is Reflector<*>) {
					val n = f[other] as Reflector<T>
					if (n != null) (o as Reflector<T>).copyFrom(n) else f[this] = null
				} else {
					f[this] = deepCopy(f[other])
				}
			}
			this as T
		} catch (e: Exception) {
			throw GException("Failed to deep copy", e)
		}
	}

	/**
	 * Convenience method
	 *
	 * @param file
	 * @throws IOException
	 */
	@Synchronized
	@Throws(IOException::class)
	fun saveToFile(file: File) {
		log.debug("saving to file %s", file.absolutePath)
		FileOutputStream(file).use { out -> serialize(out) }
	}

	/**
	 * Convenience method to attempt a save to file and fail silently on error
	 *
	 * @param file
	 */
	fun trySaveToFile(file: File) {
		try {
			saveToFile(file)
		} catch (e: Exception) {
			log.error(e)
		}
	}

	/**
	 * Convenience method
	 *
	 * @param file
	 * @throws IOException
	 */
	@Synchronized
	@Throws(IOException::class)
	fun loadFromFile(file: File) {
		loadFromFile(file, false)
	}

	/**
	 * Convenience method
	 *
	 * @param file
	 * @throws IOException
	 */
	@Synchronized
	@Throws(IOException::class)
	fun mergeFromFile(file: File) {
		loadFromFile(file, true)
	}

	/**
	 *
	 * @param file
	 * @param keepInstances
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun loadFromFile(file: File, keepInstances: Boolean) {
		log.debug("Loading from file %s", file.absolutePath)
		val reader = RBufferedReader(InputStreamReader(FileInputStream(file)))
		try {
			if (keepInstances) {
				merge(reader)
			} else {
				deserialize(reader)
			}
		} finally {
			reader.close()
		}
	}

	/**
	 * Convenience method to load from file and fail silently
	 *
	 * @param file
	 * @returns true on success and false on failure
	 */
	fun tryLoadFromFile(file: File): Boolean {
		try {
			loadFromFile(file, false)
			return true
		} catch (e: FileNotFoundException) {
			log.error(e.message)
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return false
	}

	/**
	 * Override this for some classes that should be considered immutable
	 *
	 * @return
	 */
	open fun isImmutable(): Boolean = false

	/**
	 * @param
	 * @return
	 */
	override fun isDirty(): Boolean {
		return false
	}

	override fun markClean() {}

	@Throws(IOException::class)
	override fun serializeDirty(out: RPrintWriter) {
		serialize(out)
	}

	@Throws(IOException::class)
	fun serializeDirty(out: OutputStream?) {
		serialize(RPrintWriter(out))
	}

	@Throws(IOException::class)
	fun serializeDirtyToString(): String {
		val buf = StringWriter()
		RPrintWriter(buf).use { out -> serializeDirty(out) }
		return buf.toString()
	}

	@Throws(IOException::class)
	fun serializeToString(): String {
		val buf = StringWriter()
		RPrintWriter(buf).use { out -> serialize(out) }
		return buf.toString()
	}

	@Throws(
		ClassNotFoundException::class,
		IllegalAccessException::class,
		InstantiationException::class
	)
	open fun newCollectionInstance(name: String): MutableCollection<Any> {
		when (name) {
			"java.util.Collections.SynchronizedRandomAccessList" -> return Collections.synchronizedList(
				ArrayList<Any>()
			)
		}
		return getClassForName(name)!!.newInstance() as MutableCollection<Any>
	}

	open fun newMapInstance(name: String): MutableMap<Any, Any> =
		getClassForName(name)!!.newInstance() as MutableMap<Any, Any>

	/**
	 * CRC32 Checksum
	 * @return
	 */
	val checksum: Long
		get() {
			val crc32: Checksum = CRC32()
			try {
				serialize(object : OutputStream() {
					override fun write(b: Int) {
						crc32.update(b)
					}
				})
			} catch (e: IOException) {
				e.printStackTrace()
			}
			return crc32.value
		}

	companion object {
		var DISABLED = false
		var ARRAY_DIMENSION_VARIATIONS = 2

		/**
		 * Turn this on to throw exceptions on any unknown fields.  Default is off.
		 */
		var THROW_ON_UNKNOWN = false

		/**
		 * Strip package name qualifier from serialize and deserialize
		 */
		var STRIP_PACKAGE_QUALIFIER = false
		private val log = LoggerFactory.getLogger(Reflector::class.java)
		private val classValues: MutableMap<Class<*>, MutableMap<Field, Archiver>> = HashMap()
		private val classMap: MutableMap<String, Class<*>> = HashMap()
		private val subclassOfCache: MutableMap<Class<*>, MutableMap<Class<*>, Boolean>> = HashMap()

		@Throws(Exception::class)
		fun findEnumEntry(enumClass: Class<*>, value: String?): Enum<*>? {
			if (value == null || value == "null") return null
			val constants: Array<Enum<*>> = (enumClass as Class<Enum<*>>).getEnumConstants()
			for (e in constants) {
				if (e.name == value) {
					return e
				}
				// TODO : Is there a way to use variation annotation on emum?
			}
			throw Exception("Failed to find enum value: '" + value + "' in available constants: " + Arrays.asList(*constants))
		}

		private val canonicalNameCache: MutableMap<Class<*>, String> = HashMap()
		fun getCanonicalName(clazz: Class<*>): String {
			return getCanonicalNameOrNull(clazz)
				?: throw GException("cannot getCannonicalName for : $clazz")
		}

		private fun getCanonicalNameOrNull(clazz: Class<*>): String? {
			var clazz = clazz
			var name: String?
			if (STRIP_PACKAGE_QUALIFIER) {
				if (clazz!!.isAnonymousClass) {
					clazz = clazz.superclass
				}
				if (canonicalNameCache[clazz].also { name = it } != null) {
					return name
				}
				if (!classMap.containsKey(clazz!!.simpleName)) {
					name = clazz.simpleName
					classMap[name!!] = clazz
				} else {
					name = clazz.canonicalName
				}
			} else {
				if (canonicalNameCache[clazz].also { name = it } != null) {
					return name
				}
				val isArray = clazz!!.isArray
				val isEnum = clazz.isEnum
				val isAnnotation = clazz.isAnnotation
				val isAnonymous = clazz.isAnonymousClass
				val isInterface = clazz.isInterface
				val isLocal = clazz.isLocalClass
				val isMember = clazz.isMemberClass
				val isPrimitive = clazz.isPrimitive
				val isSynthetic = clazz.isSynthetic
				val enclosingClass = clazz.enclosingClass
				val declatingClass = clazz.declaringClass
				val superClass = clazz.superclass
				if (isAnonymous || superClass != null && superClass.isEnum) {
					clazz = superClass
				}
				//while (DirtyDelegate.class.isAssignableFrom(clazz)) {
				//    clazz = clazz.getSuperclass();
				//}
				name = clazz!!.canonicalName
				if (name == null) return null
			}
			canonicalNameCache[clazz] = name!!
			return name
		}

		@Throws(ClassNotFoundException::class, IllegalAccessException::class, InstantiationException::class)
		fun newCollectionInstance(name: String): MutableCollection<Any> {
			when (name) {
				"java.util.Collections.SynchronizedRandomAccessList" -> return Collections.synchronizedList(
					ArrayList()
				)
			}
			return getClassForName(name)!!.newInstance() as MutableCollection<Any>
		}

		// TODO: Support more than 3D arrays?
		// TODO: byte?
		init {
			classMap["byte"] = Byte::class.javaPrimitiveType!!
			classMap["byte[]"] = ByteArray::class.java
			classMap["[B"] = ByteArray::class.java
			classMap["byte[][]"] = Array<ByteArray>::class.java
			classMap["[[B"] = Array<ByteArray>::class.java
			classMap["byte[][][]"] = Array<Array<ByteArray>>::class.java
			classMap["[[[B"] = Array<Array<ByteArray>>::class.java
			classMap["int"] = Int::class.javaPrimitiveType!!
			classMap["int[]"] = IntArray::class.java
			classMap["[I"] = IntArray::class.java
			classMap["int[][]"] = Array<IntArray>::class.java
			classMap["[[I"] = Array<IntArray>::class.java
			classMap["int[][][]"] = Array<IntArray>::class.java
			classMap["[[[I"] = Array<IntArray>::class.java
			classMap["float"] = Float::class.javaPrimitiveType!!
			classMap["float[]"] = FloatArray::class.java
			classMap["[F"] = FloatArray::class.java
			classMap["float[][]"] = Array<FloatArray>::class.java
			classMap["[[F"] = Array<FloatArray>::class.java
			classMap["float[][][]"] = Array<Array<FloatArray>>::class.java
			classMap["[[[F"] = Array<Array<FloatArray>>::class.java
			classMap["long"] = Long::class.javaPrimitiveType!!
			classMap["long[]"] = LongArray::class.java
			classMap["[L"] = LongArray::class.java
			classMap["long[][]"] = Array<LongArray>::class.java
			classMap["[[L"] = Array<LongArray>::class.java
			classMap["long[][][]"] = Array<Array<LongArray>>::class.java
			classMap["[[[L"] = Array<Array<LongArray>>::class.java
			classMap["double"] = Double::class.javaPrimitiveType!!
			classMap["double[]"] = DoubleArray::class.java
			classMap["[D"] = DoubleArray::class.java
			classMap["double[][]"] = Array<DoubleArray>::class.java
			classMap["[[D"] = Array<DoubleArray>::class.java
			classMap["double[][][]"] = Array<Array<DoubleArray>>::class.java
			classMap["[[[D"] = Array<Array<DoubleArray>>::class.java
			classMap["boolean"] = Boolean::class.javaPrimitiveType!!
			classMap["boolean[]"] = BooleanArray::class.java
			classMap["[Z"] = BooleanArray::class.java
			classMap["boolean[][]"] = Array<BooleanArray>::class.java
			classMap["[[Z"] = Array<BooleanArray>::class.java
			classMap["boolean[][][]"] = Array<Array<BooleanArray>>::class.java
			classMap["[[[Z"] = Array<Array<BooleanArray>>::class.java
			classMap["java.util.Arrays.ArrayList"] = ArrayList::class.java
			classMap["java.lang.String[]"] = Array<String>::class.java
			classMap["java.lang.String[][]"] = Array<Array<String>>::class.java
			classMap["java.lang.String[][][]"] = Array<Array<Array<String>>>::class.java
			classMap["java.lang.Boolean[]"] = Array<Boolean>::class.java
			classMap["java.lang.Boolean[][]"] = Array<Array<Boolean>>::class.java
			classMap["java.lang.Boolean[][][]"] = Array<Array<Array<Boolean>>>::class.java
			classMap["java.lang.Integer[]"] = Array<Int>::class.java
			classMap["java.lang.Integer[][]"] = Array<Array<Int>>::class.java
			classMap["java.lang.Integer[][][]"] = Array<Array<Array<Int>>>::class.java
			classMap["java.lang.Float[]"] = Array<Float>::class.java
			classMap["java.lang.Float[][]"] = Array<Array<Float>>::class.java
			classMap["java.lang.Float[][][]"] = Array<Array<Array<Float>>>::class.java
			classMap["java.lang.Double[]"] = Array<Double>::class.java
			classMap["java.lang.Double[][]"] = Array<Array<Double>>::class.java
			classMap["java.lang.Double[][][]"] = Array<Array<Array<Double>>>::class.java
			classMap["java.lang.Long[]"] = Array<Long>::class.java
			classMap["java.lang.Long[][]"] = Array<Array<Long>>::class.java
			classMap["java.lang.Long[][][]"] = Array<Array<Array<Long>>>::class.java
			classMap["java.lang.Byte[]"] = Array<Byte>::class.java
			classMap["java.lang.Byte[][]"] = Array<Array<Byte>>::class.java
			classMap["java.lang.Byte[][][]"] = Array<Array<Array<Byte>>>::class.java
			if (STRIP_PACKAGE_QUALIFIER) {
				classMap["String[]"] = Array<String>::class.java
				classMap["String[][]"] = Array<Array<String>>::class.java
				classMap["String[][][]"] = Array<Array<Array<String>>>::class.java
				classMap["Boolean[]"] = Array<Boolean>::class.java
				classMap["Boolean[][]"] = Array<Array<Boolean>>::class.java
				classMap["Boolean[][][]"] = Array<Array<Array<Boolean>>>::class.java
				classMap["Integer[]"] = Array<Int>::class.java
				classMap["Integer[][]"] = Array<Array<Int>>::class.java
				classMap["Integer[][][]"] = Array<Array<Array<Int>>>::class.java
				classMap["Float[]"] = Array<Float>::class.java
				classMap["Float[][]"] = Array<Array<Float>>::class.java
				classMap["Float[][][]"] = Array<Array<Array<Float>>>::class.java
				classMap["Double[]"] = Array<Double>::class.java
				classMap["Double[][]"] = Array<Array<Double>>::class.java
				classMap["Double[][][]"] = Array<Array<Array<Double>>>::class.java
				classMap["Long[]"] = Array<Long>::class.java
				classMap["Long[][]"] = Array<Array<Long>>::class.java
				classMap["Long[][][]"] = Array<Array<Array<Long>>>::class.java
				classMap["Byte[]"] = Array<Byte>::class.java
				classMap["Byte[][]"] = Array<Array<Byte>>::class.java
				classMap["Byte[][][]"] = Array<Array<Array<Byte>>>::class.java
			}
			registerClass(ArrayList::class.java)
		}

		/**
		 * This method is usefull for inner classes
		 *
		 * @param clazz
		 */
		fun registerClass(clazz: Class<*>, vararg alternateNames: String) {
			val sClazz = getCanonicalName(clazz)
			val lastDot = sClazz.lastIndexOf(".")
			if (STRIP_PACKAGE_QUALIFIER) {
				if (lastDot > 0) {
					val simpleName = sClazz.substring(lastDot + 1)
					if (simpleName.isNotEmpty() && !classMap.containsKey(simpleName)) {
						classMap[simpleName] = clazz
					}
				}
				for (alt in alternateNames) {
					if (classMap.containsKey(alt)) throw GException("Alternate name " + alt + " is already mapped to: " + classMap[alt])
					classMap[alt] = clazz
				}
			} else {
				for (alt in alternateNames) {
					val altName = sClazz.substring(0, lastDot + 1) + alt
					classMap[altName] = clazz
				}
			}
			addArrayTypes(clazz)
			classMap[sClazz] = clazz
		}

		@Throws(ClassNotFoundException::class)
		fun getClassForName(forName: String): Class<*> {
			return classMap[forName] ?: run {
				//return Reflector.class.getClassLoader().loadClass(forName);
				val clazz = Reflector::class.java.classLoader.loadClass(forName)
				classMap[forName] = clazz
				return clazz
			}
		}

		private fun inheritValues(clazz: Class<*>?, values: MutableMap<Field, Archiver>) {
			if (clazz == null || clazz == Archiver::class.java) return
			if (classValues.containsKey(clazz)) {
				values.putAll(classValues[clazz]!!)
			}
			inheritValues(clazz.superclass, values)
		}

		fun getValues(clazz: Class<*>, createIfDNE: Boolean): MutableMap<Field, Archiver> {
			var clazz = clazz
			return try {
				if (getCanonicalNameOrNull(clazz) == null) {
					clazz = if (clazz.superclass != null) clazz.superclass else clazz.enclosingClass
				}
				var values: MutableMap<Field, Archiver>? = null
				if (classValues.containsKey(clazz)) {
					values = classValues[clazz]
				} else if (createIfDNE) {
					// reject unsupported classes
					if (clazz.isAnonymousClass || clazz.isSynthetic) throw GException("Synthetic and anonymous classes not supported")
					// test newInstance works for this clazz
					if (!Modifier.isAbstract(clazz.modifiers)) {
						if (clazz.isArray) {
							throw GException("array?")
						}
						clazz.newInstance()
					}
					values = TreeMap(fieldComparator)
					// now inherit any values in base classes that were added
					inheritValues(clazz.superclass, values)
					classValues[clazz] = values
					var arrName = getCanonicalName(clazz)
					var arrClass = clazz
					for (i in 0 until ARRAY_DIMENSION_VARIATIONS) {
						arrName += "[]"
						arrClass = java.lang.reflect.Array.newInstance(arrClass, 0).javaClass
						classMap[arrName] = arrClass
					}
				} else if (clazz.superclass == null) {
					//log.warn("Cannot find any fields to archive (did you add an addField(...) method in your class?)");
					values = HashMap<Field, Archiver>()
				} else {
					//log.warn("No values for " + clazz.getName());
					return getValues(clazz.superclass, createIfDNE)
				}
				values!!
			} catch (e: GException) {
				throw e
			} catch (e: Exception) {
				throw GException("Cannot instantiate " + getCanonicalName(clazz) + ". Is it public? Does it have a public 0 argument constructor?", e)
			}
		}

		private val fieldComparator: Comparator<Field> = object : Comparator<Field> {
			override fun compare(o1: Field, o2: Field): Int {
				return o1.name.compareTo(o2.name)
			}
		}

		fun isSubclassOf(subClass: Class<*>, baseClass: Class<*>): Boolean {
			var baseCache = subclassOfCache[subClass] ?: HashMap<Class<*>, Boolean>().also {
				subclassOfCache[subClass] = it
			}
			baseCache[baseClass]?.let {
				return it
			}
			val result = if (subClass == Any::class.java || getCanonicalName(subClass) == getCanonicalName(Any::class.java))
				false
			else if (subClass == baseClass || subClass == baseClass || getCanonicalName(subClass) == getCanonicalName(baseClass))
				true
			else if (baseClass.isAssignableFrom(subClass))
				true
			else
				isSubclassOf(subClass.superclass, baseClass)
			baseCache[baseClass] = result
			return result
		}

		fun getArchiverForType(clazz: Class<*>): Archiver {
			registerClass(clazz)
			return if (clazz == Byte::class.java || clazz == Byte::class.javaPrimitiveType) {
				byteArchiver
			} else if (clazz == Boolean::class.java || clazz == Boolean::class.javaPrimitiveType) {
				booleanArchiver
			} else if (clazz == Long::class.java || clazz == Long::class.javaPrimitiveType) {
				longArchiver
			} else if (clazz == Double::class.java || clazz == Double::class.javaPrimitiveType) {
				doubleArchiver
			} else if (clazz == Int::class.java || clazz == Int::class.javaPrimitiveType) {
				integerArchiver
			} else if (clazz == Float::class.java || clazz == Float::class.javaPrimitiveType) {
				floatArchiver
			} else if (clazz == String::class.java) {
				stringArchiver
			} else if (clazz.isEnum || isSubclassOf(clazz, Enum::class.java)) {
				addArrayTypes(clazz)
				enumArchiver
			} else if (isSubclassOf(clazz, Reflector::class.java)) {
				archivableArchiver
			} else if (isSubclassOf(clazz, MutableCollection::class.java)) {
				collectionArchiver
			} else if (isSubclassOf(clazz, MutableMap::class.java)) {
				mapArchiver
			} else if (clazz.isArray) {
				// add enums if this is an enum
				addArrayTypes(clazz)
				arrayArchiver
			} else if (isSubclassOf(clazz, DirtyDelegate::class.java)) {
				dirtyArchiver
			} else {
				throw GException("No reflector available for class: $clazz")
			}
		}

		private fun addArrayTypes(clazz: Class<*>) {
			var clazz = clazz
			if (clazz.isAnnotation) return
			var nm = clazz.name
			if (classMap.containsKey(nm)) return
			if (nm.endsWith("\$Companion")) return
			if (nm.startsWith("cc.lib.utils.Reflector$")) return
			if (nm.indexOf("java.lang.Object") > 0) throw GException("Arrays of Objects not supported")
			classMap[nm] = clazz
			classMap[nm.replace('$', '.')] = clazz
			var nm2 = nm
			val lBrack = nm.lastIndexOf('[')
			if (lBrack > 0) {
				nm = nm.substring(lBrack + 2, nm.length - 1)
				nm2 = nm.replace('$', '.')
			}
			try {
				clazz = Class.forName(nm)
				classMap[nm2] = clazz
				clazz = java.lang.reflect.Array.newInstance(clazz, 1).javaClass
				classMap["$nm2[]"] = clazz
				clazz = java.lang.reflect.Array.newInstance(clazz, 1).javaClass
				classMap["$nm2[][]"] = clazz
			} catch (e: ClassNotFoundException) {
				e.printStackTrace()
			}
		}

		/**
		 * Add a field of a specific class to be included in the archivable handler.
		 * Supported Type:
		 * All Primitives
		 * Strings
		 * Enums
		 * Arrays
		 * Collections
		 * Classes derived from Archivable
		 *
		 *
		 * Also, fields are inherited.
		 *
		 * @param clazz
		 * @param name
		 */
		fun addField(clazz: Class<*>, name: String) {
			try {
				val field = clazz.getDeclaredField(name)
				if (field.getAnnotation(Omit::class.java) != null) {
					log.debug("Field '$name' has been omitted using Omit annotation.")
					return
				}
				if (Modifier.isStatic(field.modifiers)) throw GException("Cannot add static fields")
				field.isAccessible = true
				val archiver = getArchiverForType(field.type)
				val values = getValues(clazz, true)
				if (values.containsKey(field)) throw GException(
					"Duplicate field.  Field '" + name + "' has already been included for class: " + getCanonicalName(
						clazz
					)
				)
				values.put(field, archiver)
				var nm = clazz.name
				nm = Utils.chopEnd(nm, "\$delegate")
				nm = nm.replace('$', '.')
				classMap[nm] = clazz
				//log.debug("Added field '" + name + "' for " + clazz);
			} catch (e: GException) {
				throw e
			} catch (e: NoSuchFieldException) {
				if (THROW_ON_UNKNOWN) throw GException("Failed to add field '$name'", e)
				log.warn("Field '$name' not found for class: $clazz")
			} catch (e: Exception) {
				throw GException("Failed to add field '$name'", e)
			}
		}

		/**
		 * Remove a field for archiving for a specific class.  If field not present, then no effect.
		 * If field not one of clazz, then runtime exception thrown.
		 *
		 * @param clazz
		 * @param name
		 */
		fun removeField(clazz: Class<*>, name: String) {
			try {
				val field = clazz.getDeclaredField(name)
				val values = getValues(clazz, false)
				values.remove(field)
			} catch (e: Exception) {
				throw GException(e)
			}
		}

		/**
		 * Add a field of a specific class with a custom achiver.
		 *
		 * @param clazz
		 * @param name
		 * @param archiver
		 * @throws Exception
		 */
		fun addField(clazz: Class<*>, name: String, archiver: Archiver) {
			try {
				val field = clazz.getDeclaredField(name)
				val values = getValues(clazz, true)
				if (values.containsKey(field)) throw GException(
					"Duplicate field.  Field '" + name + "' has already been included for class: " + getCanonicalName(
						clazz
					)
				)
				values.put(field, archiver)
			} catch (e: GException) {
				throw e
			} catch (e: Exception) {
				throw GException("Failed to add field '$name'", e)
			}
		}

		/**
		 * @param clazz
		 */
		fun addAllFields(clazz: Class<*>) {
			if (DISABLED) return
			registerClass(clazz)
			addArrayTypes(clazz)
			try {
				val fields = clazz.declaredFields
				for (f in fields) {
					if (f.name.endsWith("Companion")) continue
					if (Modifier.isStatic(f.modifiers)) continue
					addField(clazz, f.name)
				}
				for (e in clazz.classes) {
					addArrayTypes(e) // add enclosed classes
				}
			} catch (e: GException) {
				throw e
			} catch (e: Exception) {
				throw GException("Failed to add all fields in " + clazz.name, e)
			}
		}

		/**
		 * Convenience
		 *
		 * @param obj
		 * @return
		 * @throws IOException
		 */
		@Throws(IOException::class)
		fun serializeObject(obj: Any): String {
			val out = StringWriter()
			serializeObject(obj, RPrintWriter(out))
			return out.buffer.toString()
		}

		/**
		 * Allows serializing of non-reflector types
		 *
		 * @param obj
		 * @param out
		 * @throws Exception
		 */
		@Throws(IOException::class)
		fun serializeObject(obj: Any, out: PrintWriter) {
			val _out: RPrintWriter = if (out is RPrintWriter) out else RPrintWriter(out)
			if (obj.javaClass.isArray) {
				val num = java.lang.reflect.Array.getLength(obj)
				_out.p(getCanonicalName(obj.javaClass)).p(" ").p(num)
			} else if (obj is IDirtyCollection<*>) {
				_out.print(getCanonicalName(obj.backing!!.javaClass))
			} else {
				_out.print(getCanonicalName(obj.javaClass))
			}
			try {
				serializeObject(obj, _out, true)
			} catch (e: IOException) {
				throw e
			} catch (e: Exception) {
				throw ParseException(_out.lineNum, e)
			}
		}

		/**
		 * This version will derive the object type from the top level element.
		 *
		 * @param file
		 * @param <T>
		 * @return
		 * @throws IOException
		</T> */
		@Throws(IOException::class)
		fun <T> deserializeFromFile(file: File): T? {
			val reader: RBufferedReader = if (file.exists()) {
				RBufferedReader(InputStreamReader(FileInputStream(file)))
			} else {
				RBufferedReader(
					InputStreamReader(
						Reflector::class.java.classLoader.getResourceAsStream(file.name)
							?: throw FileNotFoundException(file.absolutePath)
					)
				)
			}
			return try {
				_deserializeObject(reader, false) as T?
			} catch (e: IOException) {
				throw e
			} catch (e: Exception) {
				throw ParseException(reader.lineNum, e)
			} finally {
				reader.close()
			}
		}

		/**
		 * @param o
		 * @param file
		 * @throws IOException
		 */
		@Throws(IOException::class)
		fun <T> serializeToFile(o: Any, file: File) {
			FileOutputStream(file).use { out -> serializeObject(o, RPrintWriter(out)) }
		}

		/**
		 * Convenience
		 *
		 * @param str
		 * @param <T>
		 * @return
		 * @throws IOException
		</T> */
		@Throws(IOException::class)
		fun <T> deserializeFromString(str: String): T? {
			val reader = RBufferedReader(StringReader(str))
			return try {
				_deserializeObject(reader, false) as T?
			} catch (e: IOException) {
				throw e
			} catch (e: Exception) {
				throw ParseException(reader.lineNum, e)
			} finally {
				reader.close()
			}
		}

		/**
		 * @param _in
		 * @param <T>
		 * @return
		 * @throws IOException
		</T> */
		@Throws(IOException::class)
		fun <T> deserializeFromInputStream(_in: InputStream): T? {
			val reader = RBufferedReader(InputStreamReader(_in))
			return try {
				_deserializeObject(reader, false) as T?
			} catch (e: IOException) {
				throw e
			} catch (e: Exception) {
				throw ParseException(reader.lineNum, e)
			} finally {
				reader.close()
			}
		}

		/**
		 * Get a non reflector object from serialized output (see serializeObject)
		 *
		 * @param in
		 * @param <T>
		 * @return
		 * @throws IOException
		</T> */
		@Throws(IOException::class)
		fun <T> deserializeObject(reader: BufferedReader): T? {
			val _in: RBufferedReader = if (reader is RBufferedReader)
				reader
			else
				RBufferedReader(reader)
			return try {
				val o = _deserializeObject(_in, false)
				o as T?
			} catch (e: IOException) {
				throw e
			} catch (e: Exception) {
				throw ParseException(_in.lineNum, e)
			}
		}

		@Throws(IOException::class)
		fun mergeObject(target: Any, reader: String) {
			mergeObject(target, RBufferedReader(StringReader(reader)))
		}

		@Throws(IOException::class)
		fun <T> mergeObject(target: T, _in: RBufferedReader): T? {
			var target: T? = target
			try {
				if (target == null) {
					return deserializeObject<T>(_in)
				} else if (target.javaClass.isArray) {
					deserializeArray(target, _in, true)
				} else {
					val startDepth = _in.depth
					val line = _in.readLine()
					if (line == "null") {
						return null
					} else {
						val parts = line!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
						if (parts.size > 1) {
							val num = parts[1].toInt()
							val clazz = getClassForName(parts[0])
							if (target == null || java.lang.reflect.Array.getLength(target) != num) {
								//Class<?> clazz = getClassForName(parts[0]);
								target = java.lang.reflect.Array.newInstance(clazz, num) as T
							}
							getArchiverForType(clazz).deserializeArray(target!!, _in, true)
						} else {
							val clazz: Class<*>?
							clazz = target?.javaClass ?: getClassForName(parts[0])
							return parse(target, clazz, _in, true) as T?
						}
						if (_in.depth > startDepth) if (_in.readLineOrEOF() != null) throw ParseException(_in.lineNum, " Expected closing '}'")
					}
				}
			} catch (e: IOException) {
				throw e
			} catch (e: Exception) {
				throw ParseException(_in.lineNum, e)
			}
			return target
		}

		@Throws(IOException::class)
		fun <T> mergeObject(reader: BufferedReader): T? {
			val _in: RBufferedReader = if (reader is RBufferedReader)
				reader
			else
				RBufferedReader(reader)
			return try {
				val o = _deserializeObject(_in, true)
				o as T?
			} catch (e: IOException) {
				throw e
			} catch (e: Exception) {
				throw ParseException(_in.lineNum, e)
			} finally {
				_in.close()
			}
		}

		@Throws(Exception::class)
		private fun _deserializeObject(reader: RBufferedReader, keepInstances: Boolean): Any? {
			val line = reader.readLineOrEOF()
			if (line == "null") return null
			val parts = line!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			if (parts.size < 1) throw ParseException(reader.lineNum, "Not of form <class> <len>? {")
			val clazz = getClassForName(parts[0])
			if (parts.size > 1) {
				val a = getArchiverForType(clazz.componentType)
				val len = parts[1].toInt()
				val o = java.lang.reflect.Array.newInstance(clazz.componentType, len)
				a.deserializeArray(o, reader, keepInstances)
				return o
			}
			return parse(null, clazz, reader, keepInstances)
		}

		@Throws(IOException::class)
		fun serializeCollection(c: Collection<*>, out: RPrintWriter) {
			out.push()
			for (o in c) {
				if (o != null && o.javaClass.isArray) {
					val len = java.lang.reflect.Array.getLength(o)
					out.p(o.javaClass.componentType.name).p(" ").p(len)
				} else {
					if (o == null) {
						out.println("null")
						continue
					}
					out.p(getCanonicalName(o.javaClass))
				}
				serializeObject(o, out, true)
			}
			out.pop()
		}

		@Throws(IOException::class)
		fun serializeMap(m: MutableMap<Any, Any>, out: RPrintWriter) {
			out.push()
			for (entry in m.entries) {
				var o = entry.key
				out.print(getCanonicalName(o.javaClass))
				serializeObject(o, out, true)
				o = entry.value
				if (o == null) {
					out.println("null")
				} else {
					out.print(getCanonicalName(o.javaClass))
					serializeObject(o, out, true)
				}
			}
			out.pop()
		}

		/**
		 * @param obj
		 * @param out
		 * @param printObjects
		 * @throws Exception
		 */
		@Throws(IOException::class)
		fun serializeObject(obj: Any?, out: RPrintWriter, printObjects: Boolean) {
			if (obj == null) {
				out.println("null")
			} else if (obj is DirtyDelegate<*>) {
				obj.serialize(out, printObjects)
			} else if (obj is Reflector<*>) {
				out.push()
				obj.serialize(out)
				out.pop()
			} else if (obj is Collection<*>) {
				serializeCollection(obj, out)
			} else if (obj is MutableMap<*, *>) {
				serializeMap(obj as MutableMap<Any, Any>, out)
			} else if (obj.javaClass.isArray) {
				val compArchiver = getArchiverForType(obj.javaClass.componentType)
				out.push()
				compArchiver.serializeArray(obj, out)
				out.pop()
			} else if (printObjects) {
				out.push()
				if (obj is String) {
					out.p("\"").p(encodeString(obj)).println("\"")
				} else {
					out.println(obj)
				}
				out.pop()
			} else {
				out.println()
			}
		}

		@Throws(IOException::class)
		fun encodeString(s: String): String {
			return URLEncoder.encode(s, "UTF-8").replace("\n", "%0A").replace("\t", "%09")
		}

		@Throws(IOException::class)
		fun decodeString(reader: String): String {
			return URLDecoder.decode(reader, "UTF-8")
		}

		protected fun getName(f: Field): String {
			return Utils.chopEnd(f.name, "\$delegate")
		}

		private fun isEnum(clazz: Class<*>): Class<*>? {
			return if (clazz.isEnum) {
				clazz
			} else if (clazz.superclass != null) {
				isEnum(clazz.superclass!!)
			} else null
		}

		@Throws(IOException::class)
		private fun parse(current: Any?, clazz: Class<*>, reader: RBufferedReader, keepInstances: Boolean): Any? {
			return try {
				val enumClazz = isEnum(clazz)
				if (enumClazz != null) return findEnumEntry(enumClazz, reader!!.readLineOrEOF())
				if (clazz.isArray) {
					throw ParseException(reader.lineNum, "This method not to be called for array types")
				}
				if (isSubclassOf(clazz, Int::class.java)) {
					return reader.readLineAndClosedParen().toInt()
				}
				if (isSubclassOf(clazz, Float::class.java)) {
					return reader.readLineAndClosedParen().toFloat()
				}
				if (isSubclassOf(clazz, Long::class.java)) {
					return reader.readLineAndClosedParen().toLong()
				}
				if (isSubclassOf(clazz, Double::class.java)) {
					return reader.readLineAndClosedParen().toDouble()
				}
				if (isSubclassOf(clazz, Boolean::class.java)) {
					return java.lang.Boolean.parseBoolean(reader.readLineAndClosedParen())
				}
				if (isSubclassOf(clazz, Char::class.java)) {
					return reader.readLineAndClosedParen().trim { it <= ' ' }[0]
				}
				if (isSubclassOf(clazz, Reflector::class.java)) {
					val a: Reflector<*>
					a = if (!keepInstances || current == null) clazz.newInstance() as Reflector<*> else current as Reflector<*>
					if (keepInstances) a.merge(reader) else a.deserialize(reader)
					return a
				}
				if (isSubclassOf(clazz, MutableMap::class.java)) {
					val map = clazz.newInstance() as MutableMap<Any, Any?>
					deserializeMap(map, reader, keepInstances)
					return map
				}
				if (isSubclassOf(clazz, MutableCollection::class.java)) {
					var c = current as? MutableCollection<Any>?
					c = if (!keepInstances || c == null) clazz.newInstance() as MutableCollection<Any> else c
					deserializeCollection(c, reader, keepInstances)
					return c
				}
				if (isSubclassOf(clazz, String::class.java)) {
					val sin = reader.readLineAndClosedParen() ?: return null
					return decodeString(sin.substring(1, sin.length - 1))
				}
				// try to create from a string constructor
				val cons = clazz.getConstructor(String::class.java)
				val arg = reader.readLineOrEOF()
				cons.newInstance(arg)
			} catch (e: Exception) {
				throw ParseException(reader.lineNum, e)
			}
		}

		fun isImmutable(o: Any): Boolean {
			if (o is String || o is Number) return true
			return if (o is Reflector<*> && o.isImmutable()) true else false
		}

		@Throws(IOException::class)
		fun deserializeCollection(c: MutableCollection<Any>, reader: RBufferedReader, keepInstances: Boolean) {
			val startDepth = reader.depth
			var it: MutableIterator<*>? = null
			if (!keepInstances || c.size == 0 || isImmutable(c.iterator().next())) c.clear() else {
				it = c.iterator()
			}
			while (true) {
				try {
					val line = reader.readLineOrEOF() ?: break
					var entry: Any? = null
					var doAdd = true
					if (it != null && it.hasNext()) {
						entry = it.next()
						doAdd = false
					}
					if (doAdd) {
						it = null
					}
					if (line != "null") {
						val parts = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
						if (parts.size > 1) {
							val num = parts[1].toInt()
							val clazz = getClassForName(parts[0])
							if (!keepInstances || entry == null || java.lang.reflect.Array.getLength(entry) != num) {
								//Class<?> clazz = getClassForName(parts[0]);
								entry = java.lang.reflect.Array.newInstance(clazz, num)
							}
							getArchiverForType(clazz).deserializeArray(entry!!, reader, keepInstances)
						} else {
							var clazz: Class<*>?
							clazz = if (!keepInstances || entry == null) getClassForName(parts[0]) else entry.javaClass
							entry = parse(entry, clazz, reader, keepInstances)
						}
						if (reader.depth > startDepth) if (reader.readLineOrEOF() != null) throw ParseException(reader.lineNum, " Expected closing '}'")
					}
					if (doAdd) c.add(entry!!)
				} catch (e: IOException) {
					throw e
				} catch (e: Exception) {
					throw ParseException(reader.lineNum, e)
				}
			}
			while (it != null && it.hasNext()) {
				it.next()
				it.remove() // remove any remaining in the collection
			}
		}

		private fun fieldMatches(field: Field, name: String): Boolean {
			if (getName(field) == name) return true
			val alt = field.getAnnotation(Alternate::class.java)
			if (alt != null) {
				for (variation in alt.variations) {
					if (name == variation) return true
				}
			}
			return false
		}

		@Synchronized
		@Throws(IOException::class)
		private fun deserializeArray(array: Any, reader: RBufferedReader, keepInstances: Boolean) {
			val startDepth = reader.depth
			val len = java.lang.reflect.Array.getLength(array)
			for (i in 0 until len) {
				try {
					val line = reader.readLineOrEOF() ?: break
					var entry: Any? = null
					if (line != "null") {
						val parts = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
						if (parts.size > 1) {
							val num = parts[1].toInt()
							val clazz = getClassForName(parts[0])
							if (!keepInstances || entry == null || java.lang.reflect.Array.getLength(entry) != num) {
								//Class<?> clazz = getClassForName(parts[0]);
								entry = java.lang.reflect.Array.newInstance(clazz, num)
							}
							getArchiverForType(clazz).deserializeArray(entry!!, reader, keepInstances)
						} else {
							var clazz: Class<*>?
							clazz = if (!keepInstances || entry == null) getClassForName(parts[0]) else entry.javaClass
							entry = parse(entry, clazz, reader, keepInstances)
						}
						if (reader.depth > startDepth) if (reader.readLineOrEOF() != null) throw ParseException(reader.lineNum, " Expected closing '}'")
					}
					java.lang.reflect.Array.set(array, i, entry)
				} catch (e: IOException) {
					throw e
				} catch (e: Exception) {
					throw ParseException(reader.lineNum, e)
				}
			}
		}

		@Throws(IOException::class)
		fun deserializeMap(c: MutableMap<Any, Any?>, reader: RBufferedReader, keepInstances: Boolean) {
			val startDepth = reader.depth
			while (true) {
				var line = reader.readLineOrEOF()
				if (line == null || line == "null") break
				try {
					var clazz = getClassForName(line)
					val key = parse(null, clazz, reader, keepInstances)
						?: throw ParseException(reader.lineNum, "null key in map")
					if (reader.depth > startDepth) {
						line = reader.readLineOrEOF()
						if (line != null) throw ParseException(reader.lineNum, "Expected closing }")
					}
					line = reader.readLineOrEOF()
					if (line == null) throw ParseException(reader.lineNum, "Missing value from key/value pair in map")
					var value: Any? = null
					if (line != "null") {
						clazz = getClassForName(line)
						value = parse(c[key], clazz, reader, keepInstances)
						if (reader.depth > startDepth) {
							line = reader.readLineOrEOF()
							if (line != null) throw ParseException(reader.lineNum, "Expected closing }")
						}
					}
					c[key] = value
				} catch (e: IOException) {
					throw e
				} catch (e: Exception) {
					throw ParseException(reader.lineNum, e)
				}
			}
		}

		private fun isArraysEqual(a: Any, b: Any): Boolean {
			val lenA = java.lang.reflect.Array.getLength(a)
			val lenB = java.lang.reflect.Array.getLength(b)
			if (lenA != lenB) return false
			for (i in 0 until lenA) {
				if (!isEqual(java.lang.reflect.Array.get(a, i), java.lang.reflect.Array.get(b, i))) return false
			}
			return true
		}

		private fun isCollectionsEqual(c0: Collection<*>, c1: Collection<*>): Boolean {
			if (c0.size != c1.size) return false
			val i0 = c0.iterator()
			val i1 = c1.iterator()
			while (i0.hasNext()) {
				if (!isEqual(i0.next(), i1.next())) return false
			}
			return true
		}

		private fun isMapsEqual(m0: MutableMap<Any, Any>, m1: MutableMap<Any, Any>): Boolean {
			if (m0.size != m1.size) return false
			for (key in m0.keys) {
				if (!m1.containsKey(key)) return false
				if (!isEqual(m0[key], m1[key])) return false
			}
			return true
		}

		fun isEqual(a: Any?, b: Any?): Boolean {
			if (a === b) return true
			if (a == null || b == null) return false
			if (a.javaClass != b.javaClass) return false
			if (a is Reflector<*> && b is Reflector<*>) {
				return (a as Reflector<Any>).deepEquals(b as Reflector<Any>)
			}
			if (a.javaClass.isArray && b.javaClass.isArray) return isArraysEqual(a, b)
			if (a is Collection<*> && b is Collection<*>) {
				return isCollectionsEqual(a, b)
			}
			if (a is MutableMap<*, *> && b is MutableMap<*, *>) {
				return isMapsEqual(a as MutableMap<Any, Any>, b as MutableMap<Any, Any>)
			}
			return if (a.javaClass != b.javaClass) false else a == b
		}

		fun <T> deepCopy(o: T): T {
			return try {
				if (o is Reflector<*>) return (o as Reflector<*>).deepCopy() as T
				if (o!!.javaClass.isArray) {
					val len = java.lang.reflect.Array.getLength(o)
					val arr = java.lang.reflect.Array.newInstance(o.javaClass.componentType, len)
					for (i in 0 until len) {
						val oo = deepCopy(java.lang.reflect.Array.get(o, i))
						java.lang.reflect.Array.set(arr, i, oo)
					}
					return arr as T
				}
				if (o is DirtyCollection<*>) {
					val oldCollection = o as Collection<Any>
					val newCollection = newCollectionInstance(getCanonicalName(o.backing.javaClass))
					for (oo in oldCollection) {
						newCollection.add(deepCopy(oo))
					}
					return DirtyCollection(newCollection) as T
				} else if (o is Collection<*>) {
					val oldCollection = o as Collection<Any>
					val newCollection = newCollectionInstance(getCanonicalName(o.javaClass))
					for (oo in oldCollection) {
						newCollection.add(deepCopy(oo))
					}
					return newCollection as T
				} else if (o is DirtyMap<*, *>) {
					val map = o as MutableMap<Any, Any>
					val newMap = o.backing.javaClass.newInstance() as MutableMap<Any, Any>
					val it: Iterator<*> = map.entries.iterator()
					while (it.hasNext()) {
						val (key, value) = it.next() as MutableMap.MutableEntry<Any, Any>
						newMap[deepCopy(key)] = deepCopy(value)
					}
					return DirtyMap(newMap) as T
				} else if (o is MutableMap<*, *>) {
					val map = o as MutableMap<Any, Any>
					val newMap = o.javaClass.newInstance() as MutableMap<Any, Any>
					val it: Iterator<*> = map.entries.iterator()
					while (it.hasNext()) {
						val (key, value) = it.next() as MutableMap.MutableEntry<Any, Any>
						newMap[deepCopy(key)] = deepCopy(value)
					}
					return newMap as T
				}
				// TODO: Test that this is a primitive, enum otherwise error
				// Hopefully this is a primitive, enum 
				//System.err.println("Dont know how to deepCopy: " + o.getClass() + ": " + o);
				o
			} catch (e: Exception) {
				throw GException(e)
			}
		}

		@Throws(Exception::class)
		fun shallowCopy(o: Any?): Any? {
			if (o == null) {
				return null
			}
			if (o is Collection<*>) {
				val copy = o.javaClass.newInstance() as MutableCollection<Any>
				copy.addAll(o as Collection<Any>)
				return copy
			}
			if (o is MutableMap<*, *>) {
				val copy = o.javaClass.newInstance() as MutableMap<Any, Any>
				copy.putAll(o as MutableMap<Any, Any>)
				return copy
			}
			if (o.javaClass.isArray) {
				val len = java.lang.reflect.Array.getLength(o)
				val copy = java.lang.reflect.Array.newInstance(o.javaClass, len)
				for (i in 0 until len) {
					java.lang.reflect.Array.set(copy, i, java.lang.reflect.Array.get(o, i))
				}
				return copy
			}
			return if (o is Reflector<*>) {
				o.shallowCopy()
			} else o
		}

		fun dump() {
			log.info("classMap=" + classMap.toString().replace(',', '\n'))
			log.info("classValues=" + classValues.toString().replace(',', '\n'))
			log.info("canonicalNameCache=" + canonicalNameCache.toString().replace(',', '\n'))
		}

//		init {
//			ReflectionLoader.loadClasses()
//		}
	}
}