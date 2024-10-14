package cc.lib.android

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.UriMatcher
import android.database.AbstractCursor
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileNotFoundException
import java.util.Arrays

/**
 * This file allows external applications to read files we place into the cache directory.
 *
 * The benefit of this is that we dont need to expose potentially sensitive data by copying files to external storage
 *
 * Need following entry on the manifest inside <application> tag
 *
 * <provider android:name="CachedFileProvider" android:authorities="@string/cached_file_provider_authority" android:exported="true"></provider>
 *
 * @author chriscaron
</application> */
class EmailHelper : ContentProvider() {
	// UriMatcher used to match against incoming requests
	private var uriMatcher: UriMatcher? = null
	override fun onCreate(): Boolean {
		uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

		// Add a URI to the matcher which will match against the form
		// 'content://com.stephendnicholas.gmailattach.provider/*'
		// and return 1 in the case that the incoming Uri matches this pattern
		uriMatcher!!.addURI(getAuthority(context), "files/*", 1)
		uriMatcher!!.addURI(getAuthority(context), "cache/*", 2)
		return true
	}

	@Throws(FileNotFoundException::class)
	private fun getFile(uri: Uri): File {
		Log.d(TAG, "Matching URI: $uri")
		val n = uriMatcher!!.match(uri)
		return when (n) {
			1 -> {


				// The desired file name is specified by the last segment of the
				// path
				// E.g.
				// 'content://com.stephendnicholas.gmailattach.provider/Test.txt'
				// Take this and build the path to the file
				val segment = uri.lastPathSegment
				val fileLocation = (context!!.filesDir.toString() + File.separator
					+ segment)
				Log.d(TAG, "Returning file descriptor for file: $fileLocation")
				val file = File(fileLocation)
				if (!file.exists()) throw FileNotFoundException("Does not exist: " + file.absolutePath)
				if (!file.isFile) throw FileNotFoundException("Not a file: " + file.absolutePath)
				if (!file.canRead()) throw FileNotFoundException("Not readable: " + file.absolutePath)
				Log.d(
					TAG,
					"File is: " + file.absolutePath + " size=" + getHumanReadableFileSize(
						context, file
					)
				)
				file
			}

			2 -> {


				// The desired file name is specified by the last segment of the
				// path
				// E.g.
				// 'content://com.stephendnicholas.gmailattach.provider/Test.txt'
				// Take this and build the path to the file
				val segment = uri.lastPathSegment
				val fileLocation = (context!!.cacheDir.toString() + File.separator
					+ segment)
				Log.d(TAG, "Returning file descriptor for file: $fileLocation")
				val file = File(fileLocation)
				if (!file.exists()) throw FileNotFoundException("Does not exist: " + file.absolutePath)
				if (!file.isFile) throw FileNotFoundException("Not a file: " + file.absolutePath)
				if (!file.canRead()) throw FileNotFoundException("Not readable: " + file.absolutePath)
				Log.d(
					TAG,
					"File is: " + file.absolutePath + " size=" + getHumanReadableFileSize(
						context, file
					)
				)
				file
			}

			else -> {
				Log.e(TAG, "Unsupported uri: '$uri'.")
				throw FileNotFoundException(
					"Unsupported uri: "
						+ uri.toString()
				)
			}
		}
	}

	@Throws(FileNotFoundException::class)
	override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
		Log.i(TAG, "openFile uri=$uri mode=$mode")
		val file = getFile(uri)
		return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
	}

	override fun update(uri: Uri, contentvalues: ContentValues?, s: String?, `as`: Array<String>?): Int {
		Log.d(TAG, "update uri=" + uri + " s=" + s + " as=" + Arrays.toString(`as`))
		return 0
	}

	override fun delete(uri: Uri, s: String?, `as`: Array<String>?): Int {
		Log.d(TAG, "delete uri=" + uri + " s=" + s + " as=" + Arrays.toString(`as`))
		return 0
	}

	override fun insert(uri: Uri, contentvalues: ContentValues?): Uri? {
		Log.d(TAG, "insert uri=$uri")
		return null
	}

	override fun getType(uri: Uri): String? {
		try {
			val file = getFile(uri)
			val name = file.name
			val dot = name.lastIndexOf('.')
			if (dot > 0) {
				val extension = file.name.substring(dot)
				val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
				Log.d(TAG, "getType uri=$uri ext=$extension mimeType=$mimeType")
				if (mimeType == null) return "text/plain"
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return null
	}

	override fun query(uri: Uri, projection: Array<String>?, s: String?, as1: Array<String>?, s1: String?): Cursor? {
		Log.d(
			TAG,
			"query uri=" + uri + " proj=" + Arrays.toString(projection) + " s=" + s + " as1=" + Arrays.toString(as1) + " s1=" + s1
		)
		try {
			val file = getFile(uri)

			// observe logs to determine the columns email app asks for
			val cols = arrayOf("_display_name", "_size")
			val vals = arrayOf(file.name, file.length().toString())
			return object : AbstractCursor() {
				override fun isNull(column: Int): Boolean {
					return column < 0 && column >= cols.size
				}

				override fun getString(column: Int): String {
					return vals[column]
				}

				override fun getShort(column: Int): Short {
					return vals[column].toShort()
				}

				override fun getLong(column: Int): Long {
					return java.lang.Long.valueOf(vals[column])
				}

				override fun getInt(column: Int): Int {
					return Integer.valueOf(vals[column])
				}

				override fun getFloat(column: Int): Float {
					return java.lang.Float.valueOf(vals[column])
				}

				override fun getDouble(column: Int): Double {
					return java.lang.Double.valueOf(vals[column])
				}

				override fun getCount(): Int {
					return 1
				}

				override fun getColumnNames(): Array<String> {
					return cols
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return null
	}

	companion object {
		private const val TAG = "EmailHelper"
		fun getAuthority(context: Context?): String {
			return context!!.applicationContext.packageName + ".provider"
		}

		/**
		 * Open email application
		 * @param context
		 * @param to
		 * @param subject
		 * @param body
		 * @param attachmentInCacheDir the file attachment to include.  Must be located in the cache dir (@see Context.getCacheDir())
		 */
		fun sendEmail(context: Context, attachmentInCacheDir: File?, to: String?, subject: String?, body: String?) {
			val intent = Intent(Intent.ACTION_SEND) //TO, Uri.fromParts("mailto",to, null));
			if (to != null) intent.setData(Uri.fromParts("mailto", to, null))
			intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
			intent.putExtra(Intent.EXTRA_SUBJECT, subject)
			intent.putExtra(Intent.EXTRA_TEXT, body)
			intent.setType("message/email")
			if (attachmentInCacheDir != null) {
				Log.d(TAG, "Attachment path: " + attachmentInCacheDir.absolutePath)
				val auth = getAuthority(context)
				val uri = FileProvider.getUriForFile(context, auth, attachmentInCacheDir)
				//Uri uri = Uri.parse("content://" + auth + "/" + attachmentInCacheDir.getName());
				intent.putExtra(Intent.EXTRA_STREAM, uri) //Uri.parse("content://" + uri));
				intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
				intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
				/*
            try {
                String mime = Files.probeContentType(attachmentInCacheDir.toPath());
                intent.setType(mime);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Failed to determine mimetype");
            }*/
			}
			context.startActivity(Intent.createChooser(intent, "Send Email"))
		}
	}
}
