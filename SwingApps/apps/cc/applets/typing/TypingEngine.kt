package cc.applets.typing

import cc.lib.game.GColor
import cc.lib.reflector.Omit
import cc.lib.reflector.Reflector
import cc.lib.utils.KFileUtils.openFileOrResource
import cc.lib.utils.randomWeighted
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.stream.Collectors
import kotlin.math.roundToInt

enum class PromptResult(val color: GColor) {
	NOT_TYPED(GColor.WHITE),
	CORRECT(GColor.GREEN),
	INCORRECT(GColor.RED)
}

data class PromptElement(val c: Char, var result: PromptResult = PromptResult.NOT_TYPED)

typealias PromptType = List<PromptElement>

interface ITypeEngine {
	fun getPrompt(): PromptType

	fun newPrompt()
	fun start()

	fun pause()

	fun onKeyTyped(key: Char)
	fun getFeedback(): Feedback
}

class KeyStats(
	val key: Char = Char(0),
	var attempts: Int = 0,
	var correct: Int = 0,
	var reactionTimeMs: Long = 0L
) : Reflector<KeyStats>() {

	companion object {
		init {
			addAllFields<KeyStats>()
		}
	}

	fun accuracy(): Double = if (attempts == 0) 0.0 else correct.toDouble() / attempts

	fun avgReactionTimeMs(): Int = if (correct == 0) 5000 else (reactionTimeMs / correct).toInt()

	fun getWeight(): Double {
		if (attempts == 0)
			return .5
		val avg = avgReactionTimeMs()
		return accuracy() + .5 * (5000 - (avg.coerceAtMost(5000))) / 5000
	}
}

data class Feedback(
	val wordsPerMinute: Double,
	val accuracy: Int,
	val typingLevel: Int,
	val streak: Int
) {
	fun toDisplayString(): String {
		val accuracyText = "Accuracy: %d%%".format(accuracy)
		val wpmText = "Speed: %.1f WPM".format(wordsPerMinute)
		val levelText = "Typing Level: ${typingLevel + 1}"

		return """
			$accuracyText
			$wpmText
			$levelText
			Streak: $streak
		""".trimIndent()
	}
}

/**
 * Created by Chris Caron on 10/24/25.
 */
class TypingEngine(listener: Listener? = null, wordPoolFile: String? = null) : Reflector<TypingEngine>(), ITypeEngine {

	interface Listener {
		fun onProgress(current: Int, max: Int)

		fun onCorrect()

		fun onIncorrect()

		fun onLevelUp()

		fun onLevelDown()
	}

	companion object {
		init {
			addAllFields<TypingEngine>()
		}
	}

	@Omit
	private val listeners = mutableSetOf<WeakReference<Listener>>()

	private var level = 0
	private var streak = 0

	@Omit
	private var prompt: PromptType

	@Omit
	private var promptIndex = 0

	@Omit
	private val wordPools = mutableListOf(
		listOf(
			"a", "s", "d", "f", "g", "h", "j", "k", "l" // middle row
		),
		listOf(
			"q", "w", "e", "r", "t", "y", "u", "i", "o", "p" // top row
		),
		listOf(
			"z", "x", "c", "v", "b", "n", "m" // bottom row
		)
	)
	private val keys = mutableMapOf<Char, KeyStats>()
	private var totalAttempts = 0
	private var totalCorrect = 0

	@Omit
	private var timer = 0L

	@Omit
	private var sessionStartTime = 0L

	private var sessionTimer = 0L

	@Omit
	private var wpm: Double = 0.0

	@Omit
	private var running = false


	init {
		// load word pool

		// init keys
		for (i in 33 until 127) {
			keys[Char(i)] = KeyStats(Char(i))
		}

		listener?.let {
			addListener(it)
		}
		wordPoolFile?.let {
			initWordPool(it)
		}
		prompt = createPrompt()
	}

	private fun List<String>.randomCapitals(num: Int): List<String> {
		var i = 0
		return shuffled().map {
			if (i++ < num) {
				it.capitalize()
			} else it
		}.shuffled()
	}

	fun addListener(listener: Listener) {
		listeners.add(WeakReference(listener))
	}

	/**
	 * Load from a dictionary. dictionary assumed to be ordered by commonality
	 */
	@Throws(IOException::class)
	fun initWordPool(file: String) {
		listeners.forEach { it.get()?.onProgress(1, 7) }
		file.openFileOrResource().reader().buffered().lines().use { words ->
			wordPools.add(words.filter { it.length < 3 }.limit(50L).collect(Collectors.toList())) // 50 most common 1 or 2 letter words
			listeners.forEach { it.get()?.onProgress(2, 7) }
		}
		file.openFileOrResource().reader().buffered().lines().use { words ->
			wordPools.add(words.filter { it.length in 2..3 }.skip(25).limit(50L).collect(Collectors.toList())) // 50 common 2-3 letter words
			listeners.forEach { it.get()?.onProgress(3, 7) }
		}

		file.openFileOrResource().reader().buffered().lines().use { words ->
			wordPools.add(words.filter { it.length == 4 }.limit(50L).collect(Collectors.toList()))
			listeners.forEach { it.get()?.onProgress(4, 7) }
		}
		file.openFileOrResource().reader().buffered().lines().use { words ->
			wordPools.add(words.filter { it.length == 4 }.limit(50L).collect(Collectors.toList()))
			listeners.forEach { it.get()?.onProgress(5, 7) }
		}
		file.openFileOrResource().reader().buffered().lines().use { words ->
			wordPools.add(words.filter { it.length == 5 }.limit(50L).collect(Collectors.toList()))
			listeners.forEach { it.get()?.onProgress(6, 7) }
		}
		file.openFileOrResource().reader().buffered().lines().use { words ->
			wordPools.add(words.filter { it.length in 4..7 }.skip(50).limit(50L).collect(Collectors.toList()).randomCapitals(20))
			listeners.forEach { it.get()?.onProgress(7, 7) }
		}
	}

	private fun getWordWeight(word: String): Int {
		if (word.isEmpty())
			return 0
		val sum = word.toList().sumOf { keys[it]?.getWeight() ?: 0.5 }
		return (100 * (sum / word.length)).roundToInt()
	}

	fun getKeyWeight(key: Char): Double {
		return keys[key]?.getWeight() ?: .5
	}

	override fun newPrompt() {
		prompt = createPrompt()
	}

	private fun createPrompt(): PromptType {
		val prompt = wordPools[level.coerceIn(0, wordPools.lastIndex)].randomWeighted {
			100 - getWordWeight(it)
		}.toList().map { PromptElement(it) }
		promptIndex = 0
		timer = System.currentTimeMillis()
		println("new prompt: \"${prompt.map { it.c }.joinToString("")}\"")
		return prompt
	}

	override fun getPrompt(): PromptType = prompt

	override fun start() {
		sessionStartTime = System.currentTimeMillis()
		running = true
		newPrompt()
	}

	override fun pause() {
		running = false
	}

	/**
	 * return true for correct, false otherwise
	 */
	override fun onKeyTyped(key: Char) {
		val time = System.currentTimeMillis() - timer
		prompt.getOrNull(promptIndex)?.let { (char, result) ->
			val correct = key == char
			if (correct) {
				streak++
				totalCorrect++
			} else {
				streak = 0
			}
			keys[char]?.let {
				it.attempts++
				if (correct) {
					it.correct++
					it.reactionTimeMs += time
				}
				timer = System.currentTimeMillis()
			}
			prompt[promptIndex].result = if (correct) PromptResult.CORRECT else PromptResult.INCORRECT
			promptIndex++
		}
		if (prompt.all { it.result != PromptResult.NOT_TYPED }) {
			if (prompt.any { it.result == PromptResult.INCORRECT }) {
				listeners.forEach {
					it.get()?.onIncorrect()
				}
			} else {
				listeners.forEach {
					it.get()?.onCorrect()
				}
			}

			totalAttempts++
			if (totalAttempts > 10 && computeAccuracy() > .8 && computeWPM() > 30) {
				level++
				listeners.forEach {
					it.get()?.onLevelUp()
				}
				totalAttempts = 0
				totalCorrect = 0
			} else if (level > 1 && totalAttempts > 20 && computeAccuracy() < .2 && computeWPM() < 5) {
				level--
				listeners.forEach {
					it.get()?.onLevelDown()
				}
				totalAttempts = 0
				totalCorrect = 0
			}
		}
	}

	override fun getFeedback(): Feedback = Feedback(
		computeWPM(),
		(computeAccuracy() * 100).roundToInt(),
		computeTypingLevel(),
		streak
	)

	fun computeAccuracy(): Double {
		if (totalAttempts == 0)
			return 0.0
		return totalCorrect.toDouble() / totalAttempts
	}

	fun computeTypingLevel(): Int {
		val accuracy = computeAccuracy()
		val wpm = computeWPM()
		return when {
			accuracy > 0.95 && wpm > 70 -> 5
			accuracy > 0.90 && wpm > 50 -> 4
			accuracy > 0.85 && wpm > 35 -> 3
			accuracy > 0.75 && wpm > 20 -> 2
			else -> 1
		}
	}

	fun computeWPM(): Double {
		if (running) {
			sessionTimer = System.currentTimeMillis() - sessionStartTime
		}
		val elapsedMinutes = sessionTimer / 60000.0
		wpm = if (elapsedMinutes <= 0) 0.0 else (totalAttempts / 5.0) / elapsedMinutes
		return wpm
	}
}