package cc.lib.logger

import cc.lib.utils.GException
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintStream
import java.io.PrintWriter

/**
 * Created by chriscaron on 2/27/18.
 */
abstract class LoggerFactory {
	enum class LogLevel {
		SILENT,
		ERROR,
		INFO,
		DEBUG,
		VERBOSE;

		val isSilent: Boolean
			get() = ordinal > logLevel.ordinal
	}

	abstract fun getLogger(name: String): Logger

	companion object {
		@JvmField
		var logLevel = LogLevel.DEBUG
		var factory: LoggerFactory = object : LoggerFactory() {
			override fun getLogger(name: String): Logger {
				return object : Logger {
					override fun error(msg: String, vararg args: Any) {
						var msg = msg
						if (LogLevel.ERROR.isSilent) return
						if (args.isNotEmpty()) msg = String.format(msg, *args)
						System.err.println("E[$name]:$msg")
					}

					override fun debug(msg: String, vararg args: Any) {
						var msg = msg
						if (LogLevel.DEBUG.isSilent) return
						if (args.isNotEmpty()) msg = String.format(msg, *args)
						println("D[$name]:$msg")
					}

					override fun info(msg: String, vararg args: Any) {
						var msg = msg
						if (LogLevel.INFO.isSilent) return
						if (args.isNotEmpty()) msg = String.format(msg, *args)
						println("I[$name]:$msg")
					}

					override fun error(e: Throwable) {
						if (LogLevel.ERROR.isSilent) return
						error("%s:%s", e.javaClass.simpleName, e.message!!)
						for (s in e.stackTrace) {
							error(s.toString())
						}
					}

					override fun warn(msg: String, vararg args: Any) {
						var msg = msg
						if (LogLevel.INFO.isSilent) return
						if (args.isNotEmpty()) msg = String.format(msg, *args)
						System.err.println("W[$name]:$msg")
					}

					override fun verbose(msg: String, vararg args: Any) {
						var msg = msg
						if (LogLevel.VERBOSE.isSilent) return
						if (args.isNotEmpty()) msg = String.format(msg, *args)
						System.err.println("V[$name]:$msg")
					}
				}
			}
		}

		private fun getName(clazz: Class<*>): String {
			return if (!clazz.simpleName.isEmpty()) clazz.simpleName else getName(
				clazz.superclass
			)
		}

		fun getLogger(prefix: String, clazz: Class<*>): Logger {
			return factory.getLogger(prefix + ":" + getName(clazz))
		}

		@JvmStatic
		fun getLogger(clazz: Class<*>): Logger {
			return factory.getLogger(getName(clazz))
		}

		fun setFileLogger(outFile: File) {
			setFileLogger(outFile, true)
		}

		fun setFileLogger(outFile: File, includeConsole: Boolean) {
			factory = object : LoggerFactory() {
				var out: PrintWriter? = null
				fun write(txt: String?, o2: PrintStream) {
					if (includeConsole) o2.println(txt)
					try {
						if (out == null) {
							if (!outFile.exists()) {
								try {
									if (!outFile.createNewFile()) throw GException("Cannot create file '$outFile'")
								} catch (e: IOException) {
									throw GException(e)
								}
							}
							out = PrintWriter(FileWriter(outFile))
						}
						out!!.println(txt)
						out!!.flush()
						if (outFile.length() > 5 * 1024 * 1024) {
							val newFile = File(outFile.parent, outFile.name + ".0")
							newFile.delete()
							if (!outFile.renameTo(newFile)) throw GException("Failed to rename $outFile to $newFile")
							if (!outFile.createNewFile()) throw GException("Failed to create file: $outFile")
						}
					} catch (e: IOException) {
						e.printStackTrace()
					}
				}

				override fun getLogger(name: String): Logger {
					return object : Logger {
						override fun error(msg: String, vararg args: Any) {
							write("E[" + name + "]:" + String.format(msg, *args), System.err)
						}

						override fun debug(msg: String, vararg args: Any) {
							write("D[" + name + "]:" + String.format(msg, *args), System.out)
						}

						override fun info(msg: String, vararg args: Any) {
							write("I[" + name + "]:" + String.format(msg, *args), System.out)
						}

						override fun error(e: Throwable) {
							error("%s:%s\n%s", e.javaClass.simpleName, e.message!!)
							for (s in e.stackTrace) {
								error(s.toString())
							}
						}

						override fun warn(msg: String, vararg args: Any) {
							write("W[" + name + "]:" + String.format(msg, *args), System.err)
						}

						override fun verbose(msg: String, vararg args: Any) {
							write("W[" + name + "]:" + String.format(msg, *args), System.err)
						}
					}
				}
			}
		}
	}
}
