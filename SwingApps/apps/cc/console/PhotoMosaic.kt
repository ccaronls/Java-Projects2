package cc.console

import cc.lib.swing.AWTImageMgr
import cc.lib.utils.FileUtils
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File
import java.io.FileFilter
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.system.exitProcess


/**
 * Created by Chris Caron on 11/13/22.
 */
class PhotoMosaic {
	companion object {

		val images = AWTImageMgr()

		fun usage() {
			println("Usage: PhotoMosaic <target image file> <cell width pixels> <cell height pixels> <input files directory> <cache dir>")
		}

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

		fun BufferedImage.scale(newWidth: Int, newHeight:Int): BufferedImage {
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

		val used = mutableSetOf<File>()
		val skipUsed = false

		fun match(target: BufferedImage, sourceDir: File, x: Int, y: Int) : File {
			return sourceDir.listFiles{ file -> !skipUsed || !used.contains(file) }?.minByOrNull {
				val image = images.loadImageFile(it).toBufferedImage()
				var pixels = 0L
				var diff = 0.0
				for (xx in 0 until image.width) {
					for (yy in 0 until image.height) {
						val p0 = target.getRGB(x+xx, y+yy).toRGBA()
						val p1 = image.getRGB(xx, yy).toRGBA()

						//diff += abs(p0-p1)
						diff += colorDist(p0, p1)
						pixels ++
					}
				}
				diff / pixels
			}?.also {
				used.add(it)
			}?:run {
				println("Failed to match")
				exitProcess(1)
			}
		}

		fun Int.toRGBA() : IntArray {
			val a = shr(24).and(0xff)
			val r = shr(16).and(0xff)
			val g = shr(8).and(0xff)
			val b = shr(0).and(0xff)
			return intArrayOf(r, g, b, a)
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
			return sqrt((((512+rmean)*r*r).shr(8) + 4*g*g + (((767-rmean)*b*b).shr(8)).toDouble()))

		}

		@JvmStatic
		fun main(args: Array<String>) {
			// args are:
			// input image
			// cells wide
			// cell tall
			// input image directory
			// cache directory
			if (args.size < 4) {
				usage()
				exitProcess(1)
			}

			val target = File(args[0])
			if (!target.isFile) {
				println("$target is not a readbale file")
				usage()
				exitProcess(1)
			}
			val targetImg = try {
				images.loadImageFile(target).toBufferedImage()
			} catch (e : Exception) {
				println("file $target image failed to load\n$e")
				exitProcess(1)
			}

			var cells = arrayOf(0, 0)
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

			val sourceDir = File(args[3])
			if (!sourceDir.isDirectory) {
				println("$sourceDir is not a directory")
				usage()
				exitProcess(1)
			}

			val cacheDir = File(args[4])
			if (!cacheDir.isDirectory) {
				println("$cacheDir is not a directory")
				usage()
				exitProcess(1)
			}

			println("source image dimension is ${targetImg.width} x ${targetImg.height}")

			if (targetImg.width % cells[0] != 0) {
				println("source image width is not evenly divisible by ${cells[0]}")
				exitProcess(1)
			}

			if (targetImg.height % cells[1] != 0) {
				println("source image height is not evenly divisible by ${cells[1]}")
				exitProcess(1)
			}

			// now process all the source images into the cache directory
			val suffix = "_${cells[0]}x${cells[1]}.png"
			sourceDir.listFiles().forEach {
				val processedFile = File(cacheDir, FileUtils.stripExtension(it.name) + suffix)
				if (!processedFile.exists()) {
					println("Processing $it to $processedFile")
					val img = try {
						images.loadImageFile(it).toBufferedImage()
					} catch (e: Exception) {
						println("Failed to load image $it\n$e")
						exitProcess(1)
					}
					with (img.scale(cells[0], cells[1])) {
						if (!ImageIO.write(this, "png", processedFile)) {
							println("Failed to write $processedFile")
							exitProcess(1)
						}
					}
				}
			}

			// now we have everything we need to generate an new image from the target and the source
			val resultImg = BufferedImage(targetImg.width, targetImg.height, BufferedImage.TYPE_INT_ARGB)
			// get the pixels from the source image
			for (x in 0 until targetImg.width step cells[0]) {
				for (y in 0 until targetImg.height step cells[1]) {
					// find the best image thats fits into this cell
					val file = match(targetImg, cacheDir, x, y)
					val img = try {
						images.loadImageFile(file).toBufferedImage()
					} catch (e : Exception) {
						println("Failed to load image $file")
						exitProcess(1)
					}
					resultImg.copyFrom(img, x, y)
					print(".")
				}
				println()
			}
			val output = File(target.parent, FileUtils.stripExtension(target.name) + "_mosaic.png")
			if (ImageIO.write(resultImg, "png", output)) {
				println("Success!! Wrote mosaic to $output")
			}
		}
	}


}