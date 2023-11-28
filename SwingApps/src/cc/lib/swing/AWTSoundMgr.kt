package cc.lib.swing

import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine

object AWTSoundMgr {
	/**
	 *
	 * @param file_name
	 * @return
	 */
	fun loadAudio(file_name: String): Int {
		try {
			// From file
			var stream = AudioSystem.getAudioInputStream(File(file_name))

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
			return sounds.size - 1
		} catch (e: Exception) {
			System.err.println("EXCEPTION $e caught loading sounds $file_name")
		}
		return -1
	}

	/**
	 *
	 * @param id
	 */
	fun playSound(id: Int) {
		if (id < 0) return
		sounds[id]?.start()
	}

	private val sounds = ArrayList<Clip>(32)
}