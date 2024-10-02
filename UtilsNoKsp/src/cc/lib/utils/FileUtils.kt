package cc.lib.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


@Throws(IOException::class)
fun String.toFile(dest: File) {
	if (dest.isDirectory) throw IOException("stringToFile writing to an existing directory '$dest'")
	val out = FileOutputStream(dest)
	out.use { out ->
		out.write(toByteArray())
	}
}

/**
 *
 * @param in
 * @return
 * @throws IOException
 */
@Throws(IOException::class)
fun InputStream.toString(): String {
	return use {
		val buffer = ByteArray(available())
		read(buffer)
		String(buffer)
	}
}

/**
 *
 * @param fileName
 * @return
 * @throws IOException
 */
@Throws(IOException::class)
fun String.openFileOrResource(): InputStream {
	return javaClass.classLoader.getResourceAsStream(this) ?: FileInputStream(File(this))
}

/**
 *
 * @param file
 * @return
 * @throws IOException
 */
@Throws(IOException::class)
fun File.toString(): String {
	return FileInputStream(this).toString()
}

/**
 * This is a recursive function with stack depth <= to maxNum
 * rename a file such that the file of form root.ext is copied too root.num.ext when num is integer between 0 and maxNum.
 * All other files of form root.num+n.ext are renamed accordingly with the file falling off when backups exceed maxNum
 *
 * @param fileName
 * @param maxNum
 * @return true if the file exists and was successfully renamed
 */
fun String.backupFile(maxNum: Int): Boolean {
	val root = stripExtension()
	val extension = fileExtension()
	return backupFileR(this, root, extension, maxNum)
}

/**
 *
 * @param file
 * @param maxNum
 * @return
 */
private fun File.backupFile(file: File, maxNum: Int): Boolean {
	return if (file.exists()) {
		file.absolutePath.backupFile(maxNum)
	} else false
}

private fun backupFileR(fileName: String, root: String, extension: String, maxNum: Int): Boolean {
	var root = root
	val file = File(fileName)
	if (file.exists()) {
		val index = root.lastIndexOf('.')
		try {
			var num = Integer.valueOf(root.substring(index + 1))
			if (num++ < maxNum) {
				root = root.substring(0, index) + "." + num
				val newName = root + extension
				backupFileR(newName, root, extension, maxNum)
				return file.renameTo(File(newName))
			}
		} catch (e: NumberFormatException) {
			root += ".0"
			val newName = root + extension
			backupFileR(newName, root, extension, maxNum)
			return file.renameTo(File(newName))
		}
	}
	return false
}

/**
 * @see cc.lib.utils.FileUtils.restoreFile
 * @param file
 * @return
 */
fun File.restore(): Boolean {
	return absolutePath.restoreFile()
}

/**
 *
 * @param file
 * @return
 */
fun File.hasBackup(file: File): Boolean {
	return absolutePath.hasBackupFile()
}

/**
 *
 * @param file
 * @return
 */
fun deleteFileAndBackups(file: File): Long {
	val fileName = file.absolutePath
	val root = file.name.stripExtension()
	val ext = fileName.fileExtension()
	val dir = file.parentFile
	if (!dir.isDirectory) return -1
	val files = dir.listFiles { dir, name -> name.startsWith(root) && name.endsWith(ext) }
	if (files.isEmpty()) return 0
	var bytesDeleted: Long = 0
	for (f in files) {
		val size = f.length()
		if (f.delete()) bytesDeleted += size
	}
	return bytesDeleted
}

/**
 *
 * @param fileName
 * @return
 */
fun String.hasBackupFile(): Boolean {
	val root = stripExtension()
	val ext = fileExtension()
	return File("$root.0$ext").exists()
}

/**
 * Reverse operation of backupFile.  For filename of form root.ext, if there exists a file of form
 * root.0.ext, then it will be renamed to fileName.  For all other files of form root.n.ext they will
 * be renamed too root.n-1.ext.
 */
fun String.restoreFile(): Boolean {
	val root = stripExtension()
	val ext = fileExtension()
	return restoreFileR(this, root, ext)
}

private fun restoreFileR(fileName: String, root: String, ext: String): Boolean {
	//log.verbose("restore file $fileName")
	val file = File(fileName)
	val index = root.lastIndexOf('.')
	var num = 0
	var fileNamePrefix = root
	try {
		num = Integer.valueOf(root.substring(index + 1))
		fileNamePrefix = root.substring(0, index)
		num++
	} catch (e: NumberFormatException) {
	}
	val copiedName = "$fileNamePrefix.$num$ext"
	val copiedFile = File(copiedName)
	var success = false
	if (copiedFile.exists()) {
		file.delete()
		success = copiedFile.renameTo(file)
		//log.verbose("Renaming $copiedFile too $fileName returns $success")
		restoreFileR(copiedName, "$fileNamePrefix.$num", ext)
	}
	return success
}

/**
 * Return the string extension from a file including the dot '.' or empty string when no extension exists
 * @param fileName
 * @return
 */
fun String.fileExtension(): String {
	val dot = lastIndexOf('.')
	return if (dot <= 0) "" else substring(dot)
}

/**
 * Return the filename minus the extension including dot '.'
 * dot must be beyond the front of the filename to omit empty extension hidden files from getting whole filename stripped.
 * @param fileName
 * @return
 */
fun String.stripExtension(): String {
	val dot = lastIndexOf('.')
	return if (dot <= 0) this else substring(0, dot)
}

/**
 *
 * @param dir
 */
@Throws(IOException::class)
fun File.deleteDirContents(matching: String = "*") {
	deleteDirContents(Pattern.compile(matching.replace("*", ".*")))
}

@Throws(IOException::class)
fun File.deleteDirContents(pattern: Pattern) {
	if (!isDirectory) throw IOException("Not a directory " + absolutePath)
	for (f in listFiles()) {
		if (f.isDirectory) {
			deleteDirContents(pattern)
		} else if (pattern.matcher(f.name).matches()) {
			//log.verbose("Deleting '" + f.absolutePath + "'")
			if (!f.delete()) throw IOException("Failed to delete file '" + f.absolutePath + "'")
		}
	}
}

/**
 *
 * @param target
 * @param files
 * @throws IOException
 */
@Throws(IOException::class)
fun File.zipFiles(files: Collection<File>) {
	// Based on stackoverflow answer:
	// http://stackoverflow.com/a/14350841/642160
	val zos = ZipOutputStream(FileOutputStream(this))
	try {
		val byteBuffer = ByteArray(1024)
		for (file in files) {
			val name = file.name
			val entry = ZipEntry(name)
			zos.putNextEntry(entry)
			var fis: FileInputStream? = null
			try {
				fis = FileInputStream(file)
				var bytesRead = -1
				while (fis.read(byteBuffer).also { bytesRead = it } != -1) {
					zos.write(byteBuffer, 0, bytesRead)
				}
				zos.flush()
			} finally {
				try {
					fis?.close()
				} catch (e: IOException) {
					// Ignore. close shouldn't freaking throw exceptions!
				}
			}
			zos.closeEntry()
		}
		zos.flush()
	} finally {
		zos.close()
	}
}

@Throws(IOException::class)
fun InputStream.streamTo(out: OutputStream): Long {
	var bytesCopied: Long = 0
	val buffer = ByteArray(1024)
	while (true) {
		val len = read(buffer)
		if (len < 0) break
		bytesCopied += len.toLong()
		out.write(buffer, 0, len)
	}
	return bytesCopied
}

@Throws(IOException::class)
fun InputStream.streamTo(outFile: File) {
	val out: OutputStream = FileOutputStream(outFile)
	out.use { out ->
		streamTo(out)
	}
}

@Throws(IOException::class)
fun File.streamTo(out: OutputStream) {
	out.use { out ->
		FileInputStream(this).streamTo(out)
	}
}

fun File.tryCopyTo(dest: File) {
	try {
		copyFile(dest)
	} catch (e: Exception) {
		e.printStackTrace()
	}
}

@Throws(IOException::class)
fun File.copyFile(dest: File) {
	var dest = dest
	val `in`: InputStream = FileInputStream(this)
	if (dest.isDirectory) {
		dest = File(dest, name)
	}
	try {
		val out: OutputStream = FileOutputStream(dest)
		try {
			`in`.streamTo(out)
		} finally {
			out.close()
		}
	} finally {
		`in`.close()
	}
}

@Throws(IOException::class)
fun Class<*>.getOrCreateSettingsDirectory(): File {
	var homeDir = File(System.getProperty("user.home"))
	if (!homeDir.isDirectory) {
		System.err.println("Failed to find users home dir: '$homeDir")
		homeDir = File(".")
	}
	val pkg = canonicalName.replace('.', '/')
	val settingsDir = File(homeDir, "settings/$pkg")
	if (!settingsDir.isDirectory) {
		if (!settingsDir.mkdirs())
			throw IOException("Failed to create settings directory: " + settingsDir.absolutePath)
		//else log.info("Created settings directory: " + settingsDir.absolutePath)
	}
	return settingsDir
}

fun File.getFileAndBackups(): List<File> {
	val fileName = absolutePath
	val root = name.stripExtension()
	val ext = fileName.fileExtension()
	val dir = parentFile
	if (!dir.isDirectory)
		return emptyList()
	val files = dir.listFiles { dir, name ->
		name.startsWith(root) && name.endsWith(ext)
	}
	return listOf(*files)
}
