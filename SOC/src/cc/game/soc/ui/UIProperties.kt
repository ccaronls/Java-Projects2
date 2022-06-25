package cc.game.soc.ui

import cc.lib.game.GColor
import cc.lib.logger.LoggerFactory
import java.io.*
import java.util.*

class UIProperties : Properties() {
	private var fileName: String? = null
	fun getColorProperty(key: String, defaultValue: GColor): GColor {
		try {
			return GColor.fromString(getProperty(key, defaultValue.toString()))
		} catch (e: Exception) {
			e.printStackTrace()
			log.error(e.message)
		}
		return defaultValue
	}

	/**
	 *
	 */
	fun setProperty(key: String, file: File) {
		var fileName = file.path
		fileName = fileName.replace(File.separatorChar, '/')
		setProperty(key, fileName)
	}

	fun getIntProperty(key: String, defaultValue: Int): Int {
		return getProperty(key, defaultValue.toString())?.toInt()?:defaultValue
	}

	@Synchronized
	override fun setProperty(key: String, value: String): Any {
		val r = super.setProperty(key, value)
		save()
		return r
	}

	fun setProperty(key: String, value: Int) {
		setProperty(key, value.toString())
	}

	fun getBooleanProperty(key: String, defaultValue: Boolean): Boolean {
		try {
			return java.lang.Boolean.parseBoolean(getProperty(key, defaultValue.toString()))
		} catch (e: Exception) {
			log.error(e.message)
		}
		return defaultValue
	}

	fun setProperty(key: String, selected: Boolean) {
		setProperty(key, if (selected) "true" else "false")
	}
	fun getFloatProperty(key: String, defaultValue: Float): Float {
		return getProperty(key, defaultValue.toString())?.toFloat()?:defaultValue
	}

	override fun getProperty(key: String, defaultValue: String?): String? {
		if (!containsKey(key)) {
			if (defaultValue == null) return null
			put(key, defaultValue)
			save()
		}
		return getProperty(key)
	}

	fun getListProperty(key: String): List<String> {
		if (!containsKey(key)) {
			return emptyList()
		}
		val items = getProperty(key).split(",".toRegex()).toTypedArray()
		val list = ArrayList<String>()
		for (i in items.indices) {
			list.add(items[i].trim { it <= ' ' })
		}
		return list
	}

	fun addListItem(key: String, value: String) {
		if (!containsKey(key)) {
			setProperty(key, value)
		} else {
			var cur = getProperty(key)
			cur += ","
			cur += value
			setProperty(key, cur)
		}
	}

	@Throws(IOException::class)
	fun load(fileName: String) {
		this.fileName = fileName
		log.debug("Loading properties '$fileName'")
		var `in`: InputStream? = null
		try {
			`in` = FileInputStream(fileName)
			this.load(`in`)
		} catch (e: IOException) {
			log.error(e.message)
		} finally {
			try {
				`in`!!.close()
			} catch (e: Exception) {
			}
		}
	}

	@Synchronized
	fun save() {
		log.debug("Saving properties '$fileName'")
		var out: PrintWriter? = null
		try {
			out = PrintWriter(FileWriter(fileName))
			out.println("# Application settings properties")
			out.println("# " + Date())
			(keys.toList() as List<String>).sorted().forEach {
				out.println("$it = ${getProperty(it)}")
			}

			//this.store(out, "Properties file for SOC GUI");
		} catch (e: Exception) {
			e.printStackTrace()
		} finally {
			try {
				out!!.close()
			} catch (e: Exception) {
			}
		}
	}

	fun addAll(props: Properties?) {
		super.putAll(props!!)
		save()
	}

	companion object {
		private val log = LoggerFactory.getLogger(UIProperties::class.java)
	}
}