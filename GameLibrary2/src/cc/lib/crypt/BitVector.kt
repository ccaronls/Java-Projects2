package cc.lib.crypt

import java.util.Arrays

/**
 * Class for managing a set of bits
 *
 * Ordering:
 *
 * example:
 * assume array = new byte[3];
 * then, BitVector(array) becomes:
 *
 * byte[2] byte[1] byte[0]
 * ^                     ^
 * bit 24                bit 0
 *
 * @author ccaron
 */
data class BitVector(private var buffer: IntArray = IntArray(32)) {

	/**
	 *
	 * @return
	 */
	var len = 0
		private set
	private var begin = 0

	/*
     * 
     */
	private fun grow(newSize: Int) {
		var ns = (newSize / 32 + 1)
		if (ns < 32) ns = 32
		val newBuffer = IntArray(ns)
		for (i in buffer.indices) newBuffer[i] = buffer[i]
		buffer = newBuffer
	}

	private val maxBits: Int
		get() = buffer.size * 32 - begin

	/**
	 *
	 * @param v
	 */
	constructor(v: BitVector) : this(v.buffer.clone()) {
		len = v.len
		begin = v.begin
	}

	/**
	 *
	 */
	constructor(bytes: ByteArray, end: Int = bytes.size) : this(bytes, 0, end)

	/**
	 *
	 * @param bytes
	 * @param start
	 * @param end
	 */
	constructor(bytes: ByteArray, start: Int, end: Int) : this((end - start) * 8) {
		val num = end - start
		for (i in start until num) {

			// this can be faster
			for (b in 0..7) {
				val value = bytes[i].toInt() and (1 shl b) != 0
				set(len++, value)
			}
		}
	}
	/**
	 *
	 * @param words
	 * @param num
	 */
	/**
	 *
	 * @param words
	 */
	@JvmOverloads
	constructor(words: IntArray, num: Int) : this(words, 0, num)

	/**
	 *
	 * @param words
	 * @param start
	 * @param end
	 */
	constructor(words: IntArray, start: Int, end: Int) : this((end - start) * 32) {
		val num = end - start
		for (i in 0 until num) {
			for (b in 0..31) {
				val value = words[i + start] and (1 shl b) != 0
				set(len++, value)
			}
		}
	}

	/**
	 *
	 * @param numBytes
	 */
	constructor(numBytes: Int) : this(IntArray(numBytes)) {
		len = 0
		begin = 0
	}

	/**
	 * Set the length of this vector too 0
	 */
	fun clear() {
		begin = 0
		len = begin
		buffer.fill(0)
	}

	/**
	 * get the bit at index
	 * @param index
	 * @return
	 */
	operator fun get(index: Int): Boolean {
		val _index = index + begin
		if (index >= len) throw IndexOutOfBoundsException(index.toString())
		return buffer[(_index shr 5)] and (1 shl (_index and 0x1f)) != 0 // optimized
	}

	/**
	 *
	 * @param index
	 * @param value
	 */
	operator fun set(index: Int, value: Boolean) {
		val _index = index + begin
		if (index < 0 || index >= maxBits) throw IndexOutOfBoundsException(index.toString())
		if (!value) buffer[(_index shr 5)] =
			buffer[(_index shr 5)] and (1 shl (_index and 0x1f)).inv() else buffer[(_index shr 5)] =
			buffer[(_index shr 5)] or (1 shl (_index and 0x1f))
	}

	/**
	 *
	 * @param value
	 */
	fun pushBack(value: Boolean) {
		if (len == maxBits) {
			if (begin > 0) {
				privShiftRight(begin)
			} else {
				// grow
				grow(len * 2)
			}
		}
		set(len, value)
		len++
	}

	/**
	 *
	 * @return
	 */
	fun popBack(): Boolean {
		val rv = get(len - 1)
		len -= 1
		return rv
	}

	val first: Boolean
		get() = get(0)
	val last: Boolean
		get() = get(len - 1)

	// fix the buffer such that all bits have been shifted and begin is reset to 1
	private fun privShiftRight(numBits: Int) {
		if (numBits <= 0 || numBits > len) throw RuntimeException("Invalid value for rightShift [$numBits]")
		if (numBits >= len) {
			throw RuntimeException("Invalid value for rightShift [$numBits]")
		}
		var s0 = 0
		var s1 = numBits / 32
		val n = len / 32
		val nb = numBits and 0x1f // optimized %32
		val upperMask = (1 shl 32 - nb) - 1
		for (i in s1 until n) {
			buffer[s0] = buffer[s1] shr nb and upperMask
			buffer[s0] = buffer[s0] or (buffer[s1 + 1] and upperMask shl 32 - nb)
			s0++
			s1++
		}
		buffer[s0] = buffer[s0] shr nb
		begin -= numBits
	}

	/**
	 *
	 * @param bits
	 * @param numBits
	 */
	fun pushBack(bits: Int, numBits: Int) {
		require(!(numBits > 32)) { "Invalid value for numBits '$numBits' <= 32" }
		for (i in 0 until numBits) {
			val x = if (bits and (1 shl i) == 0) false else true
			pushBack(x)
		}
	}
	/**
	 *
	 * @param startIndex
	 * @return
	 */
	/**
	 *
	 * @param startIndex
	 * @return
	 */
	/**
	 *
	 * @return
	 */
	@JvmOverloads
	fun toByte(startIndex: Int = 0, endIndex: Int = startIndex + 8): Byte {
		var endIndex = endIndex
		if (endIndex < startIndex) throw IllegalArgumentException("endIndex [$endIndex] is < startIndex [$startIndex]")
		endIndex -= startIndex
		endIndex = Math.min(8, endIndex)
		endIndex = Math.min(len - startIndex, endIndex)
		var result: Byte = 0
		for (i in 0 until endIndex) {
			//result <<= 1;
			if (get(startIndex + i)) result = (result.toInt() or (0x1 shl i)).toByte()
		}
		return result
	}
	/**
	 *
	 * @param startIndex
	 * @return
	 */
	/**
	 *
	 * @return
	 */
	@JvmOverloads
	fun toInt(startIndex: Int = 0): Int {
		if (startIndex < 0 || startIndex > len) throw IllegalArgumentException("Invalid value for startIndex (0<=$startIndex<$len)")
		var result = 0
		var i = 0
		while (i + startIndex < len && i < 32) {

			//result <<= 1;
			if (get(startIndex + i)) result = result or (0x1 shl i)
			i++
		}
		return result
	}

	/*
     *  (non-Javadoc)
     * @see java.lang.Object#toString()
     */
	override fun toString(): String {
		val buf = StringBuffer()
		for (i in 0 until len) {
			buf.append(if (get(i)) "1" else "0")
		}
		return buf.toString()
	}

	/**
	 *
	 * @param value
	 */
	fun pushFront(value: Boolean) {
		if (begin == 0) {
			if (len >= maxBits) {
				grow(len * 2)
			}
			for (i in len downTo 1) set(i, get(i - 1))
		} else {
			begin -= 1
		}
		set(0, value)
	}

	/**
	 * Shift bits such that the total number of bit decreases.  Bits are popped from the front.
	 *
	 * WARNING: CPU Intensive!! O((len-numBits)/32).
	 * This function can be slow when len is large and shift length is small.
	 *
	 * @param numBits
	 */
	@Synchronized
	fun shiftRight(numBits: Int) {
		if (numBits < 0 || numBits > len) throw RuntimeException("Invalid value for rightShift [$numBits]")
		if (numBits == 0) return
		if (numBits >= len) {
			Arrays.fill(buffer, 0)
			begin = 0
			len = begin
			return
		}
		begin += numBits // done.
		len -= numBits
	}

	/**
	 * Convenience.  Shift left for negative numBits and right for positive.
	 * fill value only relevant when numBits < 0
	 * @param numBits
	 * @param fillValue
	 */
	fun shift(numBits: Int, fillValue: Boolean) {
		if (numBits < 0) shiftLeft(-numBits, fillValue) else if (numBits > 0) shiftRight(numBits)
	}

	/**
	 *
	 * @param num
	 * @param padValue
	 */
	fun shiftLeft(num: Int, padValue: Boolean) {
		if (num == 0) return
		if (num + len > maxBits) grow((len + num) * 2)
		for (i in len + num - 1 downTo num) this[i] = get(i - num)
		for (i in 0 until num) set(i, padValue)
		len += num
	}

	/**
	 * Return a BitVector that is the INTERSECTION of 2 inputs.
	 * The result will be have len that is the MIN of the inputs' lengths.
	 * @param rhs
	 * @return
	 */
	fun and(rhs: BitVector): BitVector {
		val minLen = Math.min(len, rhs.len)
		val result = BitVector(minLen)
		for (i in 0 until minLen) {
			val value = get(i) && rhs[i]
			result[i] = value
		}
		return result
	}

	/**
	 * Return BitVector that is the UNION of 2 inputs.
	 * the result will have length that s the MAX of the inputs' lengths.
	 * @param rhs
	 * @return
	 */
	fun or(rhs: BitVector): BitVector {
		val minLen = Math.min(len, rhs.len)
		val result = BitVector(minLen)
		var i = 0
		i = 0
		while (i < minLen) {
			val value = get(i) || rhs[i]
			result[i] = value
			i++
		}
		while (i < len) {
			result[i] = get(i)
			i++
		}
		while (i < rhs.len) {
			result[i] = rhs[i]
			i++
		}
		return result
	}

	/**
	 *
	 * @param rhs
	 * @return
	 */
	fun xor(rhs: BitVector): BitVector {
		val minLen = Math.min(len, rhs.len)
		val result = BitVector(minLen)
		var i = 0
		i = 0
		while (i < minLen) {
			val value = (get(i) || rhs[i]) && !(get(i) && rhs[i])
			result[i] = value
			i++
		}
		while (i < len) {
			result[i] = get(i)
			i++
		}
		while (i < rhs.len) {
			result[i] = rhs[i]
			i++
		}
		return result
	}

	/**
	 * Append bits from another too this
	 * @param toAppend
	 */
	fun append(toAppend: BitVector) {
		val minLen = len + toAppend.len
		if (minLen > maxBits) {
			grow(minLen)
		}
		var i = len
		var j = 0
		while (i < minLen && j < toAppend.len) {
			set(i, toAppend[j])
			i++
			j++
		}
		len = minLen
	}

	/**
	 *
	 * @param bits
	 */
	fun append(bits: Byte) {
		for (i in 0..7) {
			val value = bits.toInt() and (1 shl i) != 0
			this.pushBack(value)
		}
	}

	/**
	 *
	 * @param bits
	 * @param numBits
	 */
	fun append(bits: Int, numBits: Int) {
		var i = 0
		while (i < 32 && i < numBits) {
			val value = bits and (1 shl i) != 0
			pushBack(value)
			i++
		}
	}

	override fun equals(o: Any?): Boolean {
		return (o as? BitVector?)?.let {
			if (it.len != len) false
			for (i in 0 until len) {
				if (get(i) != it[i]) false
			}
			true
		} ?: false
	}

	fun getBits(chunk: ByteArray, start: Int, end: Int) {
		var s = 0
		var i = start
		while (i < end && s < len) {
			chunk[i] = toByte(s, s + 8)
			s += 8
			i++
		}
	}

	fun getBits(chunk: ByteArray) {
		getBits(chunk, 0, chunk.size)
	}
}
