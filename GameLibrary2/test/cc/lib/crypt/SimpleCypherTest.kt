package cc.lib.crypt

import cc.lib.utils.Profiler
import junit.framework.TestCase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

open class SimpleCypherTest : TestCase() {
	@Throws(Exception::class)
	override fun setUp() {
		Profiler.ENABLED = false
		println(
			"""
	---------------------------------------
	Start Test: $name
	---------------------------------------
	
	""".trimIndent()
		)
	}

	@Throws(Exception::class)
	override fun tearDown() {
		println(
			"""
	        	----------------------------------------
	        	End Test: $name
	        	
	        	""".trimIndent()
		)
		Profiler.dumpTimes(System.out)
		println(
			"----------------------------------------\n"
		)
	}

	@JvmField
	var cypher: Cypher? = null

	open fun getCypher(): Cypher? {
		return cypher ?: SimpleCypher.generateCypher(0).also {
			cypher = it
		}
	}

	@Throws(Exception::class)
	protected fun loadFile(fileName: String): ByteArrayOutputStream {
		println("Loading file: $fileName")
		val file = ByteArrayOutputStream()

		// read test file into String
		val `in` = javaClass.classLoader.getResourceAsStream(fileName)
		doRead(`in`, file)
		println("File length: " + file.size())
		return file
	}

	@Throws(Exception::class)
	fun testSmallStrings() {
		val buffer = ByteArrayOutputStream()
		val out = DataOutputStream(EncryptionOutputStream(buffer, getCypher()))
		out.writeUTF("A")
		out.flush()
		val inBuffer = ByteArrayInputStream(buffer.toByteArray())
		val `in` = DataInputStream(EncryptionInputStream(inBuffer, getCypher()))
		val x = `in`.readUTF()
		assertEquals(x, "A")
	}

	@Throws(Exception::class)
	open fun testDataStream() {
		val buffer = ByteArrayOutputStream()
		val out = DataOutputStream(EncryptionOutputStream(buffer, getCypher()))
		out.writeBoolean(true)
		out.writeBoolean(false)
		out.flush()
		out.writeFloat(1234567.0f)
		out.writeInt(136348168)
		out.flush()
		out.writeLong(99999999999999999L)
		out.writeUTF("Hello")
		out.flush()
		out.close()
		//BitVector vec = new BitVector(buffer.toByteArray());
		println("Length of encrypted buffer: " + buffer.size())
		val inBuffer = ByteArrayInputStream(buffer.toByteArray())
		val `in` = DataInputStream(EncryptionInputStream(inBuffer, getCypher()))
		assertEquals(true, `in`.readBoolean())
		assertEquals(false, `in`.readBoolean())
		assertEquals(5 + 2 + 8 + 4 + 4, `in`.available())
		assertEquals(1234567.0f, `in`.readFloat())
		assertEquals(5 + 2 + 8 + 4, `in`.available())
		assertEquals(136348168, `in`.readInt())
		assertEquals(5 + 2 + 8, `in`.available())
		assertEquals(99999999999999999L, `in`.readLong())
		assertEquals(5 + 2, `in`.available())
		assertEquals("Hello", `in`.readUTF())
		`in`.close()
	}

	/*
    public void testAllFiles() throws Exception {
        String [] files = new File("resources").list();
        for (String f: files) {
            ByteArrayOutputStream file = loadFile("resources/" + f);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            OutputStream out = new EncryptionOutputStream(bytes, getgetCypher()());
            out.write(file.toByteArray());
            out.close();
            
            byte [] byteArray = bytes.toByteArray();
            System.out.println("Encoded file length: " + byteArray.length);

            ByteArrayOutputStream decoded = new ByteArrayOutputStream();
            ByteArrayInputStream inBytes = new ByteArrayInputStream(byteArray);
            InputStream in = new EncryptionInputStream(inBytes, getgetCypher()());
            doRead(in, decoded);
            assertEquals(file.size(), decoded.size());
            int len = Math.min(file.size(), decoded.size());
            byte [] arr1 = file.toByteArray();
            byte [] arr2 = decoded.toByteArray();
            for (int i=0; i<len; i++) {
                assertEquals("At position '" + i + "'", arr1[i], arr2[i]);
            }
        }
    }
    */
	@Throws(IOException::class, InterruptedException::class)
	private fun doRead(`in`: InputStream, out: OutputStream) {
		val buffer = ByteArray(1024)
		while (true) {
			val num = `in`.read(buffer)
			if (num < 0) break
			out.write(buffer, 0, num)
		}
		`in`.close()
		out.flush()
		out.close()
		Thread.sleep(100)
	}

	@Throws(Exception::class)
	fun doTestTextFile(file: String) {
		processTextFile(file)
	}

	@Throws(Exception::class)
	protected fun processTextFile(file: String) {
		val bytes = ByteArrayOutputStream()
		val out: OutputStream = EncryptionOutputStream(bytes, getCypher())
		out.write(file.toByteArray())
		out.close()
		val byteArray = bytes.toByteArray()
		println("Encoded file length: " + byteArray.size)
		val decoded = ByteArrayOutputStream()
		val inBytes = ByteArrayInputStream(byteArray)
		val `in`: InputStream = EncryptionInputStream(inBytes, getCypher())
		doRead(`in`, decoded)
		assertEquals(file.length, decoded.size())
		val len = Math.min(file.length, decoded.size())
		val arr1 = file.toByteArray()
		val arr2 = decoded.toByteArray()
		for (i in 0 until len) {
			assertEquals("At position '$i'", arr1[i], arr2[i])
		}
	}

	@Throws(Exception::class)
	open fun testBinaryFile() {
		val file = loadFile("signed_forms.pdf")
		val bytes = ByteArrayOutputStream()
		val out: OutputStream = EncryptionOutputStream(bytes, getCypher())
		out.write(file.toByteArray())
		out.close()
		val byteArray = bytes.toByteArray()
		println("Encoded file length: " + byteArray.size)
		val decoded = ByteArrayOutputStream()
		val inBytes = ByteArrayInputStream(byteArray)
		val `in`: InputStream = EncryptionInputStream(inBytes, getCypher())
		doRead(`in`, decoded)
		assertEquals(file.size(), decoded.size())
		val len = Math.min(file.size(), decoded.size())
		val arr1 = file.toByteArray()
		val arr2 = decoded.toByteArray()
		for (i in 0 until len) {
			assertEquals("At position '$i'", arr1[i], arr2[i])
		}
	}
}
