package cc.lib.utils

import kotlin.Pair

/**
 * Created by Chris Caron on 3/31/26.
 */
abstract class CommandLineParser(args: Array<String>) {

	private val options = mutableMapOf<Char, String>().also {
		it.put('h', "display help")
	}

	init {
		if (args.isEmpty()) {
			onZeroArgs()
		} else {
			args.forEachIndexed { idx, it ->
				if (it.startsWith("-")) {
					val opt = it.toCharArray()[1]
					if (options.containsKey(opt)) {
						onOption(opt)
					} else {
						println("Unknown option '$it'")
						printUsage()
					}
				} else {
					onArg(idx, it)
				}
			}
		}
	}

	abstract fun onZeroArgs()

	abstract fun onArg(index: Int, arg: String)

	open fun onOption(opt: Char) {
		TODO()
	}

	fun addOption(c: Char, description: String, action: () -> Unit): CommandLineParser {
		options[c] = description
		return this
	}

	open fun printUsage() {
		println("Implement printUsage")
	}
}