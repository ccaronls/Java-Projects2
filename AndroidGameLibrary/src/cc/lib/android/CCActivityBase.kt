package cc.lib.android

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.EnvironmentCompat
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import cc.lib.game.Utils
import cc.lib.logger.Logger
import cc.lib.logger.LoggerFactory
import cc.lib.utils.GException
import cc.lib.utils.toFile
import java.io.File
import java.util.*

/**
 * Base class has support for inApp billing, polling and various helper methods
 *
 *
 * @author ccaron
 */
open class CCActivityBase : AppCompatActivity() {

	open val TAG = javaClass.simpleName

	companion object {
		init {
			LoggerFactory.factory = object : LoggerFactory() {
				override fun getLogger(name: String): Logger {
					return AndroidLogger(name)
				}
			}
		}

		const val PERMISSION_REQUEST_CODE = 1001
	}

	@JvmField
    val log: Logger = AndroidLogger(javaClass.toString())

	val preferredLocale : Locale by lazy {
		if (Build.VERSION.SDK_INT < 24)
			resources.configuration.locale
		else
			resources.configuration.locales.get(0)
	}

	protected open fun getLayoutFactory(): LayoutFactory? = null

	protected open fun onLayoutCreated(binding: ViewDataBinding, viewModel: ViewModel?) {
		throw GException("If you override getLayoutFactory then you must handle this callback")
	}

	override fun onCreate(bundle: Bundle?) {
		super.onCreate(bundle)
		getSdCardPaths(this, true)?.forEach { volumePath ->
			Log.d("CCActivityBase", "volumePath:$volumePath")
		}
		if (BuildConfig.DEBUG) {
			Utils.setDebugEnabled()
		}
		getLayoutFactory()?.let { factory ->
			factory.build()
			onLayoutCreated(factory.binding, factory.viewModel)
		}
	}

	/**
	 * DO NOT CALL FROM onResume!!!!
	 *
	 * @param permissions
	 */
	fun checkPermissions(code: Int, vararg permissions: String) {
		var permissions = permissions
		if (Build.VERSION.SDK_INT >= 23 && permissions.isNotEmpty()) {
			val permissionsToRequest: MutableList<String> = ArrayList()
			for (p in permissions) {
				if (checkCallingOrSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
					permissionsToRequest.add(p)
				}
			}
			if (permissionsToRequest.size > 0) {
				permissions = permissionsToRequest.toTypedArray()
				requestPermissions(permissions, code)
				return
			}
		}
		onAllPermissionsGranted(code)
	}

	open fun checkPermissions(vararg permissions: String) {
		checkPermissions(PERMISSION_REQUEST_CODE, *permissions)
	}

	fun checkExternalStoragePermissions(code: Int) {
		checkPermissions(code, Manifest.permission.WRITE_EXTERNAL_STORAGE)
	}

	protected open fun onAllPermissionsGranted() {}
	protected open fun onAllPermissionsGranted(code: Int) {
		if (code == PERMISSION_REQUEST_CODE) onAllPermissionsGranted()
	}

	protected open fun onPermissionLimited(permissionsNotGranted: List<String>) {
		newDialogBuilder().setTitle("Cannot Launch")
			.setMessage("The following permissions are not granted and app cannot run;\n$permissionsNotGranted")
			.setNegativeButton(R.string.popup_button_ok) { dialogInterface: DialogInterface?, i: Int -> finish() }.show().setCanceledOnTouchOutside(false)
		Toast.makeText(this, "The following permissions are not granted: $permissionsNotGranted", Toast.LENGTH_LONG).show()
	}

	override fun onPause() {
		super.onPause()
		stopPolling()
	}

	// ************ HELPERS ****************
	val prefs: SharedPreferences
		get() = PreferenceManager.getDefaultSharedPreferences(this)
	val isPortrait: Boolean
		get() = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

	fun hideKeyboard() {
		(getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)?.let { imm ->
			val focused = content.findFocus()
			if (focused != null) {
				imm.hideSoftInputFromWindow(focused.windowToken, 0)
			}
		}
	}

	open val content: View
		get() = window.decorView.findViewById(android.R.id.content)

	// ************ POLLING ****************
	private var pollingTimer: Timer? = null

	/**
	 * Start polling.  onPoll will be called on UI thread at regular intervals until the
	 * activity is paused or user calls stopPolling.
	 *
	 * @param intervalSeconds
	 */
	protected fun startPolling(intervalSeconds: Int) {
		if (pollingTimer == null) {
			pollingTimer = Timer()
			pollingTimer!!.schedule(object : TimerTask() {
				override fun run() {
					runOnUiThread(pollRunnable)
				}
			}, (intervalSeconds * 1000).toLong(), (intervalSeconds * 1000).toLong())
		}
	}

	/**
	 * This is visible because there are cases when we want to stop polling even when not paused.
	 */
	protected fun stopPolling() {
		if (pollingTimer != null) {
			pollingTimer!!.cancel()
			pollingTimer = null
		}
	}

	private val pollRunnable = Runnable { onPoll() }

	/**
	 * Override this method to handle your polling needs.  Base method just logs to LogCat a warning.
	 */
	protected open fun onPoll() {
		log.warn("onPoll not handled")
	}

	/**
	 * Convenience to get the users currently configured locale
	 * @return
	 */
	val locale: Locale
		get() = resources.configuration.locale

	fun dumpAssets() {
		dumpAssetsR("")
	}

	private fun dumpAssetsR(folder: String) {
		try {
			val files = assets.list(folder)
			Log.d("Assets", """
 	Contents of $folder:
 	${Arrays.toString(files)}
 	""".trimIndent())
			for (f in files!!) {
				if (f.indexOf('.') < 0) {
					if (folder.isNotEmpty()) dumpAssetsR("$folder/$f") else dumpAssetsR(f)
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	open fun newDialogBuilder(): AlertDialog.Builder {
		// to get the Holo.Dark theme use: new ContextThemeWrapper(this, android.R.style.Theme_Holo_Dialog)
		return object : AlertDialog.Builder(this) {
			override fun show(): AlertDialog {
				val d = super.show()
				d.setCanceledOnTouchOutside(false)
				return d
			}
		}
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		val permissionsNotGranted: MutableList<String> = ArrayList()
		for (i in permissions.indices) {
			if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
				permissionsNotGranted.add(permissions[i])
			}
		}
		if (permissionsNotGranted.size > 0) {
			onPermissionLimited(permissionsNotGranted)
		} else {
			onAllPermissionsGranted(requestCode)
		}
	}

	val appVersionFromManifest: String
		get() {
			try {
				var version = packageManager.getPackageInfo(packageName, 0).versionName
				if (BuildConfig.DEBUG) {
					version += " DEBUG"
				}
				return version
			} catch (e: Exception) {
				e.printStackTrace()
			}
			return "Unknown"
		}

	fun setKeepScreenOn(enabled: Boolean) {
		if (enabled) {
			window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		} else {
			window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		}
	}

	fun showEditTextInputPopup(title: String?, defaultValue: String?, hint: String?, maxChars: Int, callabck: Utils.Callback<String?>) {
		val et = EditText(this)
		et.hint = hint
		et.setText(defaultValue)
		et.filters = arrayOf<InputFilter>(LengthFilter(maxChars))
		newDialogBuilder().setTitle(title)
			.setView(et)
			.setNegativeButton(R.string.popup_button_cancel, null)
			.setPositiveButton(R.string.popup_button_ok) { dialog: DialogInterface?, which: Int ->
				val txt = et.text.toString()
				callabck.onDone(txt)
			}.show()
	}

	fun hideNavigationBar() {
		val decorView = window.decorView
		val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			or View.SYSTEM_UI_FLAG_FULLSCREEN)
		decorView.systemUiVisibility = uiOptions
	}

	val externalStorageDirectory: File
		get() {
			arrayOf(
				"sdcard"
			).forEach { folder ->
				with (folder.toFile()) {
					if (exists() && isDirectory)
						return this
				}
			}

			return getSdCardPaths(this, true)?.firstOrNull()?.toFile()?:getExternalFilesDir(null) as File
		}

	/**
	 * returns a list of all available sd cards paths, or null if not found.
	 *
	 * @param includePrimaryExternalStorage set to true if you wish to also include the path of the primary external storage
	 */
	fun getSdCardPaths(context: Context, includePrimaryExternalStorage: Boolean): List<String>? {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
			val storageVolumes = storageManager.storageVolumes
			if (!storageVolumes.isNullOrEmpty()) {
				val primaryVolume = storageManager.primaryStorageVolume
				val result = ArrayList<String>(storageVolumes.size)
				for (storageVolume in storageVolumes) {
					val volumePath = getVolumePath(storageVolume) ?: continue
					if (storageVolume.uuid == primaryVolume.uuid || storageVolume.isPrimary) {
						if (includePrimaryExternalStorage)
							result.add(volumePath)
						continue
					}
					result.add(volumePath)
				}
				return if (result.isEmpty()) null else result
			}
		}
		val externalCacheDirs = ContextCompat.getExternalCacheDirs(context)
		if (externalCacheDirs.isEmpty())
			return null
		if (externalCacheDirs.size == 1) {
			if (externalCacheDirs[0] == null)
				return null
			val storageState = EnvironmentCompat.getStorageState(externalCacheDirs[0])
			if (Environment.MEDIA_MOUNTED != storageState)
				return null
			if (!includePrimaryExternalStorage && Environment.isExternalStorageEmulated())
				return null
		}
		val result = ArrayList<String>()
		if (externalCacheDirs[0] != null && (includePrimaryExternalStorage || externalCacheDirs.size == 1))
			result.add(getRootOfInnerSdCardFolder(context, externalCacheDirs[0]))
		for (i in 1 until externalCacheDirs.size) {
			val file = externalCacheDirs[i] ?: continue
			val storageState = EnvironmentCompat.getStorageState(file)
			if (Environment.MEDIA_MOUNTED == storageState)
				result.add(getRootOfInnerSdCardFolder(context, externalCacheDirs[i]))
		}
		return if (result.isEmpty()) null else result
	}

	fun getRootOfInnerSdCardFolder(context: Context, inputFile: File): String {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
			storageManager.getStorageVolume(inputFile)?.let {
				val result = getVolumePath(it)
				if (result != null)
					return result
			}
		}
		var file: File = inputFile
		val totalSpace = file.totalSpace
		while (true) {
			val parentFile = file.parentFile
			if (parentFile == null || parentFile.totalSpace != totalSpace || !parentFile.canRead())
				return file.absolutePath
			file = parentFile
		}
	}

	@RequiresApi(Build.VERSION_CODES.N)
	fun getVolumePath(storageVolume: StorageVolume): String? {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
			return storageVolume.directory?.absolutePath
		try {
			val storageVolumeClazz = StorageVolume::class.java
			val getPath = storageVolumeClazz.getMethod("getPath")
			return getPath.invoke(storageVolume) as String
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return null
	}
}