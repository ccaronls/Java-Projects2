package cc.lib.kreflector

import java.io.IOException

class OverridesSerialize : KReflector<OverridesSerialize> {
	var msg: String? = null
	var x = 0

	constructor() {}
	constructor(msg: String?, x: Int) : super() {
		this.msg = msg
		this.x = x
	}

	@Throws(IOException::class)
	public override fun serialize(out: RPrintWriter) {
		out.println("$msg $x")
	}

	@Throws(IOException::class)
	public override fun deserialize(reader: RBufferedReader) {
		reader.readLine()?.trim { it <= ' ' }?.let { text ->
			val parts = text.split("[ ]+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			msg = parts[0]
			x = parts[1].toInt()
		}
	}

	override fun equals(o: Any?): Boolean {
		if (o == null || o !is OverridesSerialize) return false
		val os = o
		return msg == os.msg && x == os.x
	}
}