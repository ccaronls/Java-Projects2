package cc.console

import cc.lib.swing.AWTImageMgr
import cc.lib.utils.FileUtils
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.system.exitProcess

fun Image.toBufferedImage(): BufferedImage {
	if (this is BufferedImage) {
		return this
	}

	// Create a buffered image with transparency
	val bimage = BufferedImage(getWidth(null), getHeight(null), BufferedImage.TYPE_INT_ARGB)

	// Draw the image on to the buffered image
	val bGr = bimage.createGraphics()
	bGr.drawImage(this, 0, 0, null)
	bGr.dispose()

	// Return the buffered image
	return bimage
}

fun BufferedImage.scale(newWidth: Int, newHeight: Int): BufferedImage {
	val targetAspect = newWidth.toFloat() / newHeight.toFloat()
	val sourceAspect = aspectRatio()

	var x=0
	var y=0
	var w=0
	var h=0

	if (targetAspect > sourceAspect) {
		w = width
		h = (width.toFloat() / targetAspect).roundToInt()
		y = height/2 - h/2
	} else {
		h = height
		w = (targetAspect*height).roundToInt()
		x = width/2 - w/2
	}

	return getSubimage(x, y, w, h).toBufferedImage().getScaledInstance(newWidth, newHeight, 1).toBufferedImage()
}

fun BufferedImage.copyFrom(other: BufferedImage, x: Int, y: Int) {
	val srcBuf = (other.raster.dataBuffer as DataBufferInt).data
	val dstBuf = (raster.dataBuffer as DataBufferInt).data

	var dstOffset = x+y * width
	var srcOffset = 0

	for (h in 0 until other.height) {
		System.arraycopy(srcBuf, srcOffset, dstBuf, dstOffset, other.width)
		srcOffset += other.width
		dstOffset += width
	}

}

fun Image.aspectRatio() =  getWidth(null).toFloat() / getHeight(null)

fun Int.toRGBA() : IntArray {
	val a = shr(24).and(0xff)
	val r = shr(16).and(0xff)
	val g = shr(8).and(0xff)
	val b = shr(0).and(0xff)
	return intArrayOf(r, g, b, a)
}

fun Int.toGrayScale() : Float {
	val rgb = toRGBA()
	return rgb[0].toFloat() * .3f + rgb[1].toFloat() * .59f + rgb[2].toFloat() * .11f
}

fun colorDist(c0: IntArray, c1: IntArray) : Double {

	/*
	long rmean = ( (long)e1.r + (long)e2.r ) / 2;
long r = (long)e1.r - (long)e2.r;
long g = (long)e1.g - (long)e2.g;
long b = (long)e1.b - (long)e2.b;
return sqrt((((512+rmean)*r*r)>>8) + 4*g*g + (((767-rmean)*b*b)>>8));
	 */

	val rmean = (c0[0] + c1[0]) / 2
	val r = c0[0] - c1[0]
	val g = c0[1] - c1[1]
	val b = c0[2] - c1[2]
	return sqrt((((512 + rmean) * r * r).shr(8) + 4 * g * g + (((767 - rmean) * b * b).shr(8)).toDouble()))
}

fun colorDistSimple(c0: IntArray, c1:IntArray) : Double {
	val dr = abs(c0[0] - c1[0]).toDouble().div(255.0)
	val dg = abs(c0[1] - c1[1]).toDouble().div(255.0)
	val db = abs(c0[2] - c1[2]).toDouble().div(255.0)

	return dr*dr+dg*dg+db*db
}

fun colorDistGrayScale(c0: IntArray, c1:IntArray) : Double {
	val g0 = c0[0].toDouble() * .3 + c0[1].toDouble() * .59 + c0[2].toDouble() * .11
	val g1 = c1[0].toDouble() * .3 + c1[1].toDouble() * .59 + c1[2].toDouble() * .11
	return abs(g0-g1);
}

fun compareImagesStdDev(i0: IntArray, i1: IntArray) : Double {
	var sum = 0.0
	var sum2 = 0.0
	for (i in i0.indices) {
		val c0 = i0[i].toRGBA()
		val c1 = i1[i].toRGBA()
		val d = colorDistSimple(c0, c1)
		sum += d*d
		sum2 += d
	}
	return sqrt(sum * 1.0/(i0.size-1))
	//return sum2 / i0.size
}

fun rgb2lab(rgba: IntArray): IntArray {
	//http://www.brucelindbloom.com
	var r: Float
	var g: Float
	var b: Float
	val X: Float
	val Y: Float
	val Z: Float
	val fx: Float
	val fy: Float
	val fz: Float
	val xr: Float
	val yr: Float
	val zr: Float
	val Ls: Float
	val `as`: Float
	val bs: Float
	val eps = 216f / 24389f
	val k = 24389f / 27f
	val Xr = 0.964221f // reference white D50
	val Yr = 1.0f
	val Zr = 0.825211f

	// RGB to XYZ
	r = rgba[0] / 255f //R 0..1
	g = rgba[1] / 255f //G 0..1
	b = rgba[2] / 255f //B 0..1

	// assuming sRGB (D65)
	r = if (r <= 0.04045) r / 12 else ((r + 0.055) / 1.055).pow(2.4).toFloat()
	g = if (g <= 0.04045) g / 12 else ((g + 0.055) / 1.055).pow(2.4).toFloat()
	b = if (b <= 0.04045) b / 12 else ((b + 0.055) / 1.055).pow(2.4).toFloat()
	X = 0.436052025f * r + 0.385081593f * g + 0.143087414f * b
	Y = 0.222491598f * r + 0.71688606f * g + 0.060621486f * b
	Z = 0.013929122f * r + 0.097097002f * g + 0.71418547f * b

	// XYZ to Lab
	xr = X / Xr
	yr = Y / Yr
	zr = Z / Zr
	fx = if (xr > eps) xr.toDouble().pow(1 / 3.0).toFloat() else ((k * xr + 16.0) / 116.0).toFloat()
	fy = if (yr > eps) yr.toDouble().pow(1 / 3.0).toFloat() else ((k * yr + 16.0) / 116.0).toFloat()
	fz = if (zr > eps) zr.toDouble().pow(1 / 3.0).toFloat() else ((k * zr + 16.0) / 116).toFloat()
	Ls = 116 * fy - 16
	`as` = 500 * (fx - fy)
	bs = 200 * (fy - fz)
	val lab = IntArray(3)
	lab[0] = (2.55 * Ls + .5).toInt()
	lab[1] = (`as` + .5).toInt()
	lab[2] = (bs + .5).toInt()
	return lab
}

// very expensive, results not noticeably better
fun colorDist_nope(c0: IntArray, c1: IntArray) : Double {
	val lab0 = rgb2lab(c0)
	val lab1 = rgb2lab(c1)
	return sqrt((lab1[0]-lab0[0]).toDouble().pow(2) + (lab1[1]-lab0[1]).toDouble().pow(2) + (lab1[2]-lab0[2]).toDouble().pow(2))
}

fun <T> MutableMap<T, Int>.increment(item: T, amt: Int) {
	val cur = get(item)?:0
	put(item, cur+amt)
}

fun computeAvgRGB(buffer: IntArray) : IntArray {
	var r = 0L
	var g = 0L
	var b = 0L
	buffer.forEach {
		val c = it.toRGBA()
		r += c[0]
		g += c[1]
		b += c[2]
	}
	val n = buffer.size
	return intArrayOf((r/n).toInt(), (g/n).toInt(), (b/n).toInt())
}

/**
 * Created by Chris Caron on 11/13/22.
 */
class PhotoMosaic(val targetImg: BufferedImage, val cellWidthPixels: Int, val cellHeightPixels: Int, val sourceDir : File) {
	companion object {

		val images = AWTImageMgr()

		fun usage() {
			println("Usage: PhotoMosaic <target image file> <cell width pixels> <cell height pixels> <input files directory> <cache dir>")
		}


		@JvmStatic
		fun main(args: Array<String>) {
			// args are:
			// input image
			// cells wide
			// cell tall
			// cache directory
			// input image directory(s)
			if (args.size < 4) {
				usage()
				exitProcess(1)
			}

			val target = File(args[0])
			if (!target.isFile) {
				println("$target is not a readable file")
				usage()
				exitProcess(1)
			}
			var targetImg = try {
				images.loadImageFile(target).toBufferedImage()
			} catch (e: Exception) {
				println("file $target image failed to load\n$e")
				exitProcess(1)
			}

			var cells = intArrayOf(0, 0)
			args[1].toIntOrNull()?.let {
				cells[0] = it
			}?:run {
				print("${args[1]} is not an int")
				usage()
				exitProcess(1)
			}
			args[2].toIntOrNull()?.let {
				cells[1] = it
			}?:run {
				print("${args[2]} is not an int")
				usage()
				exitProcess(1)
			}

			println("Sample images size ${cells.contentToString()}")

			val cacheDir = File(args[3])
			if (!cacheDir.isDirectory) {
				if (!cacheDir.exists()) {
					if (!cacheDir.mkdir()) {
						println("$cacheDir not present and cannot be created")
						usage()
						exitProcess(1)
					}
				} else {
					println("$cacheDir is not a directory")
					usage()
					exitProcess(1)
				}
			}

			val sourceDirs = ArrayList<File>()
			for (i in 4 until args.size) {
				val dir = File(args[i])
				if (!dir.isDirectory) {
					println("$dir is not a directory")
					usage()
					exitProcess(1)
				}
				sourceDirs.add(dir)
			}

			println("source image dimension is ${targetImg.width} x ${targetImg.height}")

			val extraWidth = targetImg.width % cells[0]
			val extraHeight = targetImg.height % cells[1]

			if (extraHeight > 0 || extraWidth > 0) {
				val newWidth = if (extraWidth > 0)  (cells[0] - extraWidth) else 0
				val newHeight = if (extraHeight > 0)  (cells[1] - extraHeight) else 0
				targetImg = targetImg.getScaledInstance(targetImg.width + newWidth, targetImg.height + newHeight, 1).toBufferedImage()
				println("Target image scaled to ${targetImg.width}x${targetImg.height}")
			}

			/*

			THIS IS SOPHIAS FIRST LINE OF CODE

			mom go cra-cra


			 */

			val prop = Properties()
			val propsFile = File(cacheDir, "props.txt")

			val expectedCellSize : IntArray = try {
				FileInputStream(propsFile).use { input ->
					prop.load(input)
					intArrayOf(
						prop.getProperty("cellWidth").toInt(),
						prop.getProperty("cellHeight").toInt(),
					)
				}

			} catch (e : Exception) {
				e.printStackTrace()
				intArrayOf(0,0)
			}

			if (!expectedCellSize.contentEquals(cells)) {
				println("Rebuilding cache to accommodate new cell size")
				cacheDir.listFiles().forEach {
					it.delete()
				}
				prop.setProperty("cellWidth", cells[0].toString())
				prop.setProperty("cellHeight", cells[1].toString())
				FileOutputStream(propsFile).use {
					prop.store(it, "expected cell size")
				}
			}

			try {
				with(PhotoMosaic(targetImg, cells[0], cells[1], cacheDir)) {
					sourceDirs.forEach {
						process(it)
					}
					computeAvgRgb()
					val resultImg = generate()
					val output = File(target.parent, FileUtils.stripExtension(target.name) + "_mosaic_${cellWidthPixels}x$cellHeightPixels.png")
					if (ImageIO.write(resultImg, "png", output)) {
						println("Success!! Wrote mosaic to $output")
					}
				}
			} catch (e: Exception) {
				e.printStackTrace()
				print("Error:\n$e")
				exitProcess(1)
			}
		}
	}

	var maxOccurances = 1
	val used = mutableMapOf<File, Int>()
	val skipUsed = true
	val colorFunc = ::colorDistSimple
	val buf0 = IntArray(cellWidthPixels*cellHeightPixels)
	val buf1 = IntArray(cellWidthPixels*cellHeightPixels)

	init {
	}

	@Throws
	private fun match(target: BufferedImage, sourceDir: File, x: Int, y: Int) : File {
		target.getRGB(x, y, cellWidthPixels, cellHeightPixels, buf0, 0, cellWidthPixels)
		val targetAvgRgb = computeAvgRGB(buf0)
		return sourceDir.listFiles { file -> file.path.endsWith(".png") && (!skipUsed || used[file]?:0 < maxOccurances) }?.minByOrNull {
			val image = images.loadImageFile(it).toBufferedImage()
			image.getRGB(0, 0, cellWidthPixels, cellHeightPixels, buf1, 0, image.width)
			compareImagesStdDev(buf0, buf1)
			//val d = avgRGBMap[it]!!
			//colorDistSimple(targetAvgRgb, d)
		}?.also {
			used.increment(it, 1)
		}?:run {
			throw Exception("Failed to match")
		}
	}

	/**
	 * Process a directory of images into another directory cropping and scale to consistent dimension
	 * determined by cellWidth/Height Pixels
	 */
	@Throws
	fun process(inputDir: File) {
		// now process all the source images into the cache directory
		val suffix = "_${cellWidthPixels}x$cellHeightPixels.png"
		inputDir.listFiles().filter { path -> path.path.endsWith(".png") }.forEach {
			val processedFile = File(sourceDir, FileUtils.stripExtension(it.name) + suffix)
			if (!processedFile.exists()) {
				println("Processing $it to $processedFile")
				val img = try {
					images.loadImageFile(it).toBufferedImage()
				} catch (e: Exception) {
					throw Exception("Failed to load image $it", e)
				}
				with(img.scale(cellWidthPixels, cellHeightPixels)) {
					if (!ImageIO.write(this, "png", processedFile)) {
						throw Exception("Failed to write $processedFile")
					}
				}
			}
		}
	}

	val avgRGBMap = mutableMapOf<File, IntArray>()

	@Throws
	fun computeAvgRgb() {
		sourceDir.listFiles { file -> file.path.endsWith(".png") }.forEach {
			with (images.loadImageFile(it).toBufferedImage()) {
				getRGB(0, 0, cellWidthPixels, cellHeightPixels, buf1, 0, width)
				avgRGBMap.put(it, computeAvgRGB(buf1))
			}
		}
	}

	@Throws
	fun generate() : BufferedImage {
		if (skipUsed) {
			val numNeeded = (targetImg.width * targetImg.height) / (cellWidthPixels * cellHeightPixels)
			val numHave = sourceDir.list().filter { it.endsWith(".png") }.size
			println("have $numHave and need $numNeeded")
			while (numHave * maxOccurances < numNeeded) {
				maxOccurances++
			}
			println("max occurrences of each: $maxOccurances")
		}
		// now we have everything we need to generate an new image from the target and the source
		val resultImg = BufferedImage(targetImg.width, targetImg.height, BufferedImage.TYPE_INT_ARGB)
		// get the pixels from the source image
		for (y in 0 until targetImg.height step cellHeightPixels) {
			for (x in 0 until targetImg.width step cellWidthPixels) {
				// find the best image thats fits into this cell
				val file = match(targetImg, sourceDir, x, y)
				val img = try {
					images.loadImageFile(file).toBufferedImage()
				} catch (e: Exception) {
					throw Exception("Failed to load image $file", e)
				}
				resultImg.copyFrom(img, x, y)
				print(".")
			}
			println()
		}
		return resultImg
	}
}