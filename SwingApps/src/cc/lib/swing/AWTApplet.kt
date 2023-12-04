package cc.lib.swing

import cc.lib.reflector.Reflector
import java.net.MalformedURLException
import java.net.URL
import javax.swing.JApplet

abstract class AWTApplet : JApplet() {
	override fun init() {
		Reflector.DISABLED = true
		AWTImageMgr.applet = this
		initApp()
	}

	@Throws(MalformedURLException::class)
	abstract fun getAbsoluteURL(imagePath: String): URL
	protected abstract fun initApp()
	open fun <T : Enum<T>> getEnumListProperty(property: String, clazz: Class<T>, defaultList: List<T>): List<T> {
		return defaultList
	}

	open fun getStringProperty(property: String, defaultValue: String): String {
		return defaultValue
	}

	open fun setStringProperty(s: String, v: String) {}
	open fun <T : Enum<T>> setEnumListProperty(s: String, l: Collection<T>) {}
	open fun setIntProperty(s: String, value: Int) {}
	open fun getIntProperty(s: String, defaultValue: Int): Int {
		return defaultValue
	}

	open fun <T : Enum<T>> getEnumProperty(s: String, clazz: Class<T>, defaultValue: T): T {
		return defaultValue
	}

	open fun <T : Enum<T>> setEnumProperty(s: String, value: T) {}
}