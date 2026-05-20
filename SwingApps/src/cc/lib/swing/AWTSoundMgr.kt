package cc.lib.swing

import cc.lib.logger.LoggerFactory
import cc.lib.utils.getOrNull
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine

object AWTSoundMgr {

	val log = LoggerFactory.getLoggerForName("AWTSoundMgr")

	private val searchPaths = mutableListOf<File>().also {
		it.add(File("."))
	}

	fun addSearchPath(path: String) {
		File(path).also {
			if (!it.isDirectory)
				throw Exception("Unknown directory $${it.absolutePath}")
			searchPaths.add(it)
		}
	}

	/**
	 *
	 * @param fileName
	 * @return
	 */
	fun loadAudio(fileName: String): Int {
		val path = searchPaths.map {
			File(it, fileName)
		}.firstOrNull { it.exists() } ?: error("file $fileName not found in search paths in ${searchPaths.joinToString()}")

		// From file
		var stream = AudioSystem.getAudioInputStream(path)

		// At present, ALAW and ULAW encodings must be converted
		// to PCM_SIGNED before it can be played
		var format = stream.format
		if (format.encoding !== AudioFormat.Encoding.PCM_SIGNED) {
			format = AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED,
				format.sampleRate,
				format.sampleSizeInBits * 2,
				format.channels,
				format.frameSize * 2,
				format.frameRate,
				true) // big endian
			stream = AudioSystem.getAudioInputStream(format, stream)
		}

		// Create the clip
		val info = DataLine.Info(
			Clip::class.java, stream.format, stream.frameLength.toInt() * format.frameSize)
		val clip = AudioSystem.getLine(info) as Clip

		// This method does not return until the audio file is completely loaded
		clip.open(stream)
		sounds.add(clip)
		log.info("Loaded $fileName -> ${sounds.size}")
		return sounds.size - 1
	}

	/**
	 *
	 * @param id
	 */
	fun playSound(id: Int, loops: Int = 0) {
		sounds.getOrNull(id)?.let {
			it.stop()
			it.framePosition = 0
			it.loop(loops)
			it.start()
		} ?: log.error("Invalid sound id $id")
	}

	private val sounds = ArrayList<Clip>(32)
}