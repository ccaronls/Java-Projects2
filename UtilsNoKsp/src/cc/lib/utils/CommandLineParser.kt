package cc.lib.utils

import java.io.File
import java.io.FileNotFoundException
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Handy utility to perform command line parsing similar to stdarg
 *
 * Does type checking, allows for complex param specification and argument specification
 * AND generates formatted usage text based on user defined formats.
 *
 * @author Chris Caron
 */
class CommandLineParser {
	private enum class Type(val c: Char, val desc: String) {
		STRING('s', "<String>"),
		INT('i', "<Int>"),
		FILE('f', "<File>"),
		DIR('d', "<Dir>");

		@Throws(Exception::class)
		fun validate(s: String?) {
			when (this) {
				STRING -> {}
				INT -> s!!.toInt()
				FILE -> if (!File(s).isFile) throw FileNotFoundException(s)
				DIR -> if (!File(s).isDirectory) throw FileNotFoundException("Not a directory: s")
			}
		}

		companion object {
			val allParams: String
				get() {
					var params = ""
					for (i in entries.toTypedArray().indices) {
						params += entries[i].c
					}
					return params
				}

			fun getParam(c: Char): Type {
				var c = c
				c = c.lowercaseChar()
				for (i in entries.toTypedArray().indices) {
					if (entries[i].c == c) return entries[i]
				}
				throw IllegalArgumentException("Invalid Type specification [$c], must be one of [$allParams]")
			}
		}
	}

	private inner class Param {
		var c = 0.toChar()
		var type: Type? = null
		var msg = ""
		var value: String? = null
		var specified = false
		override fun toString(): String {
			return "c=$c, type=$type, msg=$msg, value=$value, specified=$specified"
		}
	}

	private inner class Arg {
		var required = false
		var defaultValue = ""
		var value: String? = null
		var desc = ""
		var type: Type? = null

		internal constructor()
		internal constructor(value: String?) {
			this.value = value
		}

		override fun toString(): String {
			return "required=$required, defaultValue=$defaultValue, value=$value, desc=$desc, type=$type"
		}
	}

	private val args = ArrayList<Arg>()
	private val params: HashMap<Char, Param> = LinkedHashMap()
	private var formatIndex = 0
	private var hasArgsFormat = false
	private var argRequired = true

	/**
	 * Create a parser that isolates OPTIONS parameters as specified by params format
	 * (kind of like stdarg)
	 *
	 * Example paramsFormat:
	 * "a$ihrv$sF$fd$d"
	 *
	 * Means that following are accepted:
	 * -a <int>
	 * -h
	 * -r
	 * -v <string>
	 * -F <file>
	 * -d <dir>
	 *
	 * you can also add info to you params by including a quoted string after the param
	 * Example:
	 *
	 * "a"Enable alpha"b$i"Enable <num> betas"
	 *
	 * Produces this message from getParamUsage()
	 *
	 * -a          Enable apha
	 * -b <int>    Enable <num> betas
	 *
	 * @param paramsFormat a descriptive string describing the options.
	</num></int></num></dir></file></string></int> */
	constructor(paramsFormat: String) {
		var paramsFormat = paramsFormat
		while (paramsFormat.length > 0) {
			paramsFormat = parseParam(paramsFormat)
		}
	}

	/**
	 * Create a command line parser that takes no OPTIONS paramaters
	 *
	 */
	constructor()

	private val typePattern = Pattern.compile("^[$][" + Type.allParams + "]")
	private val stringPattern = Pattern.compile("^\"[^\"]*\"")
	private val defaultValuePattern = Pattern.compile("^\\([^)]*\\)")
	private fun parseParam(format: String): String {
		var format = format
		val c = format[0]
		format = format.substring(1)
		formatIndex++
		require(Character.isLetter(c)) { "Invalid params format at index [$formatIndex], expected letter" }
		val newParam: Param = Param()
		newParam.c = c
		var matcher = typePattern.matcher(format)
		if (matcher.find()) {
			val type = Type.getParam(matcher.group()[1])
			newParam.type = type
			formatIndex += 2
			format = format.substring(2)
		}
		matcher = stringPattern.matcher(format)
		if (matcher.find()) {
			val msg = matcher.group()
			formatIndex += msg.length
			newParam.msg = msg.substring(1, msg.length - 1)
			format = format.substring(msg.length)
		}
		params[c] = newParam
		return format
	}
	/**
	 *
	 * Parse a comamnd line specified by argv with formatting support.
	 *
	 * Example Format: i"num widgets"f"input file"D(/tmp)S(string)"the name"
	 *
	 * Means:
	 *
	 * 2 required params: 1 integer, 1 file
	 * 2 optional params: 1 directory (default '/tmp'), 1 string (default 'string')
	 *
	 * Usage outputs:
	 *
	 * PARAMS:
	 *
	 * <Int>						num widgets
	 * <File>						input file
	 * [Dir]			/tmp
	 * [String]			'string'	the name
	 *
	 * @param argv params typically passed in through main
	 * @param format a descriptive string defined the required an optional parameters
	 * @throws IllegalArgumentException if an invalid parameter is found
	</File></Int> */
	/**
	 * Parse a command line with no formating support
	 *
	 * @param argv
	 * @throws IllegalArgumentException if an invalid parameter is found
	 */
	@JvmOverloads
	@Throws(IllegalArgumentException::class)
	fun parse(argv: Array<String>, format: String? = null) {
		var argv = argv
		var i = 0
		var param: Param? = null
		argv = resolveQuotes(argv)
		try {
			i = 0
			while (i < argv.size) {
				if (argv[i].startsWith("-")) {
					param = params[argv[i][1]]
					requireNotNull(param) { "Invalid option [" + argv[i] + "]" }
					param.specified = true
					if (param.type != null) {
						if (argv[i].length > 2) {
							param.value = argv[i].substring(2)
						} else {
							param.value = argv[++i]
						}
						if (stringPattern.matcher(param.value).matches()) {
							param.value = param.value!!.substring(1, param.value!!.length - 1)
						}
						param.type!!.validate(param.value)
					}
				} else {
					break
				}
				i++
			}
			if (format == null) parseArgs(argv, i) else parseArgs(argv, i, format)
		} catch (e: FileNotFoundException) {
			throw IllegalArgumentException("Not a file [" + e.message + "]")
		} catch (e: NumberFormatException) {
			throw IllegalArgumentException("Not a number [" + e.message + "]")
		} catch (e: Exception) {
			throw IllegalArgumentException(e.message)
		}
	}

	private fun resolveQuotes(argv: Array<String>): Array<String> {
		val buf = StringBuffer()
		val newArgs = ArrayList<String>()
		var quoted = false
		for (i in argv.indices) {
			if (quoted) {
				buf.append(argv[i])
				if (argv[i].indexOf("\"") >= 0) {
					quoted = false
					newArgs.add(buf.substring(1, buf.length - 1).toString())
					buf.setLength(0)
				}
			} else {
				if (argv[i].startsWith("\"")) {
					quoted = true
					buf.append(argv[i])
				} else {
					newArgs.add(argv[i])
				}
			}
		}
		return newArgs.toTypedArray<String>()
	}

	// package access for so Junit can test this func individually
	fun parseArgsFormat(format: String) {
		var format = format
		while (format.length > 0) format = parseArgFormat(format)
	}

	/*
     * 
     */
	private fun parseArgFormat(format: String): String {
		var format = format
		val c = format[0]
		require(Character.isLetter(c)) { "Invalid format at index $formatIndex, expected letter, found [$c]" }
		formatIndex++
		val arg: Arg = Arg()
		args.add(arg)
		arg.type = Type.getParam(c)
		format = format.substring(1)
		var matcher: Matcher? = null
		if (Character.isUpperCase(c)) {
			argRequired = false
			matcher = defaultValuePattern.matcher(format)
			if (matcher.find()) {
				arg.required = false
				val value = matcher.group()
				arg.defaultValue = value.substring(1, value.length - 1)
				format = format.substring(value.length)
				formatIndex += value.length
			}
		} else require(argRequired) { "Invalid format at index $formatIndex, cannot specify required parms after non-required" }
		arg.required = argRequired
		matcher = stringPattern.matcher(format)
		if (matcher.find()) {
			val value = matcher.group()
			arg.desc = value.substring(1, value.length - 1)
			formatIndex += value.length
			format = format.substring(value.length)
		}
		return format
	}

	private fun parseArgs(argv: Array<String>, index: Int) {
		for (i in index until argv.size) {
			args.add(Arg(argv[i]))
		}
	}

	@Throws(Exception::class)
	private fun parseArgs(argv: Array<String>, index: Int, format: String) {
		var index = index
		formatIndex = 0
		hasArgsFormat = true
		var argsIndex = 0
		parseArgsFormat(format)
		while (index < argv.size) {
			require(!(argsIndex >= args.size)) { "Too may arguments" }
			val arg = args[argsIndex]
			arg.value = argv[index]
			arg.type!!.validate(arg.value)
			index++
			argsIndex++
		}
		if (index < args.size) {
			if (args[index].required) throw IllegalArgumentException("Not enough arguments specified.  Expected " + args[index].type + " [" + args[index].desc + "]")
		}
	}

	val numArgs: Int
		/**
		 * Return number of arguments parsed, including default args.
		 *
		 * @return
		 */
		get() = args.size

	/**
	 *
	 * @param index
	 * @return
	 */
	fun getArg(index: Int): String? {
		val arg = args[index]
		return if (arg.value == null) arg.defaultValue else arg.value
	}

	/**
	 *
	 * @param c
	 * @return
	 */
	fun getParamValue(c: Char): String? {
		return params[c]!!.value
	}

	/**
	 *
	 * @param c
	 * @return
	 */
	fun getParamSpecified(c: Char): Boolean {
		val p = params.get(c)
			?: throw IllegalArgumentException("'" + c + "' is not a valid param, is one of " + params.keys)
		return params[c]!!.specified
	}

	/**
	 *
	 * @param clazz
	 */
	fun printUsage(clazz: Class<*>) {
		printUsage(clazz.simpleName)
	}

	/**
	 *
	 * @param appName
	 */
	fun printUsage(appName: String?) {
		println(getUsage(appName))
	}

	/**
	 *
	 * @param clazz
	 * @return
	 */
	fun getUsage(clazz: Class<*>): String {
		return getUsage(clazz.simpleName)
	}

	/**
	 *
	 * @param appName
	 * @return
	 */
	fun getUsage(appName: String?): String {
		val buf = StringBuffer()
		buf.append("USAGE: ").append(appName).append(" ")
		if (params.size > 0) {
			buf.append("[OPTIONS] ")
		}
		if (hasArgsFormat) {
			for (i in args.indices) {
				val arg = args[i]
				var desc: String = arg.desc
				if (desc == null) desc = arg.type!!.name
				if (arg.required) {
					buf.append("<").append(desc).append(">")
				} else {
					buf.append("[").append(desc).append("]")
				}
				buf.append(" ")
			}
		}
		buf.append("\n\n")
		if (params.size > 0) {
			buf.append("[OPTIONS]\n").append(paramUsage).append("\n")
		}
		if (args.size > 0 && hasArgsFormat) {
			buf.append("[ARGS]\n").append(argsUsage).append("\n")
		}
		return buf.toString()
	}

	val argsUsage: String
		/**
		 *
		 * @return
		 */
		get() {
			var spacing1 = 18
			val spacing2 = 15
			//int spacing3 = 15;
			val buf = StringBuffer()
			for (i in args.indices) {
				val arg = args[i]
				spacing1 = Math.max(spacing1, arg.desc.length + 1)
			}
			for (i in args.indices) {
				var s = 0
				val arg = args[i]
				val desc = arg.desc //"arg" + i;
				buf.append(desc)
				s += desc.length
				while (s++ < spacing1) buf.append(" ")
				var req = "required"
				if (!arg.required) req = "optional (" + arg.defaultValue + ")  "
				buf.append(req)
				s = req.length
				while (s++ < spacing2) buf.append(" ")
				buf.append(arg.type!!.name)
				s = arg.type!!.name.length
				//while (s++ < spacing3)
				//	buf.append(" ");
				buf.append("\n") //arg.desc).append("\n");    		
			}
			return buf.toString()
		}
	val paramUsage: String
		/**
		 *
		 * @return
		 */
		get() {
			val buf = StringBuffer()
			val spacing = 15
			for (param in params.values) {
				buf.append("-").append(param.c).append(" ")
				var s = 0
				if (param.type != null) {
					val desc = param.type!!.desc
					buf.append(desc)
					s = desc.length
				}
				while (s++ < spacing) {
					buf.append(" ")
				}
				buf.append(param.msg).append("\n")
			}
			return buf.toString()
		}

	/*
     *  (non-Javadoc)
     * @see java.lang.Object#toString()
     */
	override fun toString(): String {
		return "PARAMS=$params, ARGS=$args, formatIndex=$formatIndex"
	}
}
