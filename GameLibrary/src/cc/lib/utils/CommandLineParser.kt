package cc.lib.utils

/**
 * Created by Chris Caron on 3/31/26.
 */
abstract class CommandLineParser(args: Array<String>) {

	private val options = mutableMapOf<Char, String>()

	init {
		if (args.isEmpty()) {
			onZeroArgs()
		} else {
			args.forEach {
				if (it.startsWith("-")) {
					val opt = it.toCharArray()[1]
					if (options.containsKey(opt)) {
						onOption(opt)
					} else {
						println("Unknown option '$it'")
						printUsage()
					}
				} else {
					onArg(it)
				}
			}
		}
	}

	abstract fun onZeroArgs()

	abstract fun onArg(arg: String)

	open fun onOption(option: Char) {
		TODO()
	}

	fun addOption(c: Char, description: String) {}

	open fun printUsage() {
		println("Implement printUsage")
	}
}