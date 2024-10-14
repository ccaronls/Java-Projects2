package cc.lib.android

import android.util.Log
import cc.lib.logger.Logger
import cc.lib.utils.EllipsisStyle
import cc.lib.utils.formatSafe
import cc.lib.utils.truncate
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream

/**
 * Created by chriscaron on 3/11/18.
 */
class AndroidLogger(val name: String) : Logger {
	@Synchronized
	private fun writeFile(level: String, msg: String) {
		if (out != null) {
			try {
				out!!.print(name)
				out!!.print(":")
				out!!.print(level)
				out!!.print(" - ")
				out!!.println(msg)
				out!!.flush()
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	override fun debug(msg: String, vararg args: Any) {
		val str: String = msg.formatSafe(args)
		Log.d(name, str.truncate(maxLen, maxLines, EllipsisStyle.INFO))
		writeFile("D", str)
	}

	override fun info(msg: String, vararg args: Any) {
		val str: String = msg.formatSafe(args)
		Log.i(name, str.truncate(maxLen, maxLines, EllipsisStyle.INFO))
		writeFile("I", str)
	}

	override fun error(msg: String, vararg args: Any) {
		val str: String = msg.formatSafe(args)
		Log.e(name, str.truncate(maxLen, maxLines, EllipsisStyle.INFO))
		writeFile("E", str)
	}

	override fun error(e: Throwable) {
		val str = e.javaClass.simpleName + ":" + e.message
		Log.e(name, str)
		Log.e(name, e.toString())
		writeFile("E", str)
		out?.let { e.printStackTrace(it) }
	}

	override fun warn(msg: String, vararg args: Any) {
		val str = String.format(msg, *args)
		Log.w(name, str)
		writeFile("W", str)
	}

	override fun verbose(msg: String, vararg args: Any) {
		val str = String.format(msg, *args)
		Log.v(name, str)
		writeFile("v", str)
	}

	companion object {
		var maxLen = 1024
		var maxLines = 30
		private var out: PrintStream? = null

		/**
		 * Allows sending log output to file
		 * @param file
		 * @throws IOException
		 */
		fun setLogFile(file: File?) {
			if (out == null) {
				try {
					out = PrintStream(BufferedOutputStream(FileOutputStream(file)))
				} catch (e: Exception) {
					throw RuntimeException(e)
				}
			}
		}
	}
}
