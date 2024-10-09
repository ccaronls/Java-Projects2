package cc.lib.crypt

import cc.lib.utils.streamTo
import cc.lib.utils.streamToString
import junit.framework.TestCase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Arrays
import java.util.Random

class HuffmanTest : SimpleCypherTest() {
	@Throws(Exception::class)
	override fun setUp() {
		super.setUp()
		cypher = null
	}

	@Throws(Exception::class)
	override fun tearDown() {
		super.tearDown()
		cypher = null
	}

	@Throws(Exception::class)
	override fun getCypher(): Cypher {
		return cypher ?: HuffmanEncoding().also { encoding ->
			encoding.generateRandomCounts(System.currentTimeMillis())
			encoding.generate()
			cypher = encoding
		}
	}

	@Throws(Exception::class)
	fun testRandomNumbers() {
		val kb = "1234567890-=`~!@#$%^&*()_+qwertyuiop[]\\asdfghjkl;'zxcvbnm,.//QWERTYUIOP{}|ASDFGHJKL:\"ZXCVBNM<>?\n\r\t"
		val enc = HuffmanEncoding()
		enc.importCounts(kb)
		enc.generateRandomCounts(3287923)
		//enc.generateRandomCountsFromExisitngOccurances(871270);
		enc.generate()
		val out = ByteArrayOutputStream()
		val eout = EncryptionOutputStream(out, enc)
		val dout = DataOutputStream(eout)
		val r = Random()
		r.setSeed(0)
		for (i in 0..999) {
			val ii = r.nextInt()
			dout.writeInt(ii)
		}
		dout.flush()
		var din = DataInputStream(EncryptionInputStream(ByteArrayInputStream(out.toByteArray()), enc))
		r.setSeed(0)
		for (i in 0..999) {
			if (i % 100 == 0) print(".")
			val ii = r.nextInt()
			TestCase.assertEquals(din.readInt(), ii)
		}

		// longs
		out.reset()
		r.setSeed(340)
		for (i in 0..999) {
			val ii = r.nextLong()
			dout.writeLong(ii)
		}
		dout.flush()
		din = DataInputStream(EncryptionInputStream(ByteArrayInputStream(out.toByteArray()), enc))
		r.setSeed(340)
		for (i in 0..999) {
			if (i % 100 == 0) print(".")
			val ii = r.nextLong()
			TestCase.assertEquals(din.readLong(), ii)
		}

		// floats
		out.reset()
		r.setSeed(94058)
		for (i in 0..999) {
			val ii = r.nextFloat()
			dout.writeFloat(ii)
		}
		dout.flush()
		din = DataInputStream(EncryptionInputStream(ByteArrayInputStream(out.toByteArray()), enc))
		r.setSeed(94058)
		for (i in 0..999) {
			if (i % 100 == 0) print(".")
			val ii = r.nextFloat()
			TestCase.assertEquals(din.readFloat(), ii)
		}

		// doubles
		out.reset()
		r.setSeed(23498)
		for (i in 0..999) {
			val ii = r.nextDouble()
			dout.writeDouble(ii)
		}
		dout.flush()
		din = DataInputStream(EncryptionInputStream(ByteArrayInputStream(out.toByteArray()), enc))
		r.setSeed(23498)
		for (i in 0..999) {
			if (i % 100 == 0) print(".")
			val ii = r.nextDouble()
			TestCase.assertEquals(din.readDouble(), ii)
		}
	}

	/*
    @Override
    public void testTextFile() throws Exception {
        HuffmanEncoding encoding = new HuffmanEncoding();
        encoding.importCounts(getClass().getClassLoader().getResourceAsStream("cyphertest.txt"));
        encoding.generate(true);
        cypher = encoding;
        super.testTextFile();
    }
*/
	@Throws(Exception::class)
	override fun testBinaryFile() {
		val encoding = HuffmanEncoding()
		encoding.importCounts(javaClass.classLoader.getResourceAsStream("librarybookszoom.jpg"))
		encoding.generate()
		cypher = encoding
		super.testBinaryFile()
	}

	@Throws(Exception::class)
	override fun testDataStream() {
		val encoding = HuffmanEncoding()
		//encoding.importCounts(new File("resources/streamvideo.3gp"));
		encoding.generateRandomCounts(0)
		encoding.generate()
		//encoding.debugDump(System.out);
		cypher = encoding
		super.testDataStream()
	}

	@Throws(Exception::class)
	fun testSOCFile() {
		val txt = loadFile("socsavegame.txt").toString()
		val encoding = HuffmanEncoding()
		encoding.importCounts(javaClass.classLoader.getResourceAsStream("socsavegame.txt"))
		encoding.generate()
		cypher = encoding
		processTextFile(txt)
	}

	@Throws(Exception::class)
	fun testDominosFile() {
		val txt = loadFile("dominos.save").toString()
		val encoding = HuffmanEncoding()
		encoding.importCounts(javaClass.classLoader.getResourceAsStream("dominos.save"))
		//encoding.keepAllOccurances();
		encoding.generate()
		cypher = encoding
		processTextFile(txt)
		encoding.saveEncoding(File("/tmp/encoding"))
		System.out.println(File("/tmp/encoding").streamToString())
		encoding.printEncodingAsCode(System.out)
	}

	@Throws(Exception::class)
	fun testDominosFromPredeterminedCounts() {
		val counts = intArrayOf(
			3884,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			476,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			42,
			430,
			1,
			152,
			130,
			160,
			83,
			48,
			58,
			26,
			35,
			41,
			198,
			1,
			1,
			1,
			285,
			1,
			1,
			1,
			5,
			1,
			1,
			35,
			8,
			1,
			1,
			1,
			1,
			1,
			1,
			17,
			29,
			1,
			1,
			50,
			1,
			1,
			1,
			46,
			1,
			37,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			240,
			63,
			320,
			62,
			366,
			3,
			51,
			40,
			313,
			11,
			6,
			175,
			191,
			154,
			247,
			319,
			1,
			120,
			123,
			179,
			36,
			13,
			1,
			37,
			36,
			1,
			102,
			1,
			102,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1,
			1
		)
		cypher = HuffmanEncoding(counts)
		processTextFile(loadFile("dominos.save2").toString())
	}

	@Throws(Exception::class)
	fun testGenRandomCountsForKB() {
		val kb = "1234567890-=`~!@#$%^&*()_+qwertyuiop[]\\asdfghjkl;'zxcvbnm,.//QWERTYUIOP{}|ASDFGHJKL:\"ZXCVBNM<>?\n\r\t"
		val enc = HuffmanEncoding()
		enc.importCounts(kb)
		enc.generateRandomCountsFromExisitngOccurances(System.currentTimeMillis())
		enc.generate()
		val c0 = enc.counts
		enc.generate()
		val c1 = enc.counts
		TestCase.assertTrue(Arrays.equals(c0, c1))
		enc.printEncodingAsCode(System.out)
	}

	@Throws(Exception::class)
	fun testJPG() {
		val tmp1 = File.createTempFile("tmp", "jpg", File("/tmp/"))
		javaClass.classLoader.getResourceAsStream("librarybookszoom.jpg").streamTo(tmp1)
		println("File size of tmp1: " + tmp1.length())
		val enc = HuffmanEncoding()
		enc.importCounts(javaClass.classLoader.getResourceAsStream("librarybookszoom.jpg"))
		enc.generate()
		val buf = ByteArray(1024)
		val encrypted = ByteArrayOutputStream()
		var bytesRead: Long = 0
		javaClass.classLoader.getResourceAsStream("librarybookszoom.jpg")
			.use { inFile -> EncryptionOutputStream(encrypted, enc).use { out -> bytesRead = inFile.streamTo(out) } }
		println("Bytes Read: $bytesRead")
		println("Compressed Size: " + encrypted.size())
		val tmp2 = File.createTempFile("tmp", "jpg", File("/tmp/"))
		EncryptionInputStream(
			ByteArrayInputStream(encrypted.toByteArray()),
			enc
		).use { inFile -> FileOutputStream(tmp2).use { out -> inFile.streamTo(out) } }
		println("File size of tmp2: " + tmp2.length())
	}
}
