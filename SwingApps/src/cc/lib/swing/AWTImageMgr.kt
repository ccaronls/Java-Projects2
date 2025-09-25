package cc.lib.swing

import cc.lib.game.GDimension
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.math.CMath
import cc.lib.utils.GException
import cc.lib.utils.getOrNull
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.*
import java.io.*
import java.util.*
import javax.swing.ImageIcon


class AWTImageMgr {
	private val log = LoggerFactory.getLogger(javaClass)

	private fun createMissingAssetImage(): Image {
		val dim = 64
		val img = BufferedImage(dim, dim, BufferedImage.TYPE_INT_RGB)
		val g = img.createGraphics()
		g.color = Color.LIGHT_GRAY
		g.fillRect(0, 0, 64, 64)
		g.color = Color.BLUE
		g.drawRect(1, 1, dim - 2, dim - 2)
		//        g.setFont(Font.getFont(Font.MONOSPACED));
		g.font = g.font.deriveFont(16)
		g.drawString("MISSING", 5, 18)
		g.drawString("ASSET", 5, 34)
		return img
	}

	private val images = mutableListOf<Image?>(
		createMissingAssetImage()
	) // loaded images

	/* Returns an ImageIcon, or null if the path was invalid. 
	private static ImageIcon createImageIcon(String path) {
	    URL imgURL = Utils.class.getResource(path);
	    if (imgURL != null) {
	        return new ImageIcon(imgURL);
	    } else {
	        System.err.println("Couldn't find file: " + path);
	        return null;
	    }
	}*/
	/* */
	@Throws(Exception::class)
	fun loadImageFromFile(name: String): Image {
		FileInputStream(File(name)).use {
			val buffer = ByteArray(it.available())
			it.read(buffer)
			return ImageIcon(buffer).image
		}
	}

	@Throws(Exception::class)
	fun loadImageFile(file: File): Image {
		FileInputStream(file).use {
			val buffer = ByteArray(it.available())
			it.read(buffer)
			return ImageIcon(buffer).image
		}
	}

	@Throws(Exception::class)
	private fun loadImageFromSearchPaths(name: String): Image {
		for (path in paths) {
			try {
				FileInputStream(File(path, name)).use { reader ->
					val buffer = ByteArray(reader.available())
					reader.read(buffer)
					return ImageIcon(buffer).image
				}
			} catch (e: FileNotFoundException) {
				log.debug("Not found in search path '" + path + "':" + e.message)
			} catch (e: IOException) {
				throw e
			}
		}
		throw FileNotFoundException(name)
	}

	/* */
	@Throws(Exception::class)
	private fun loadImageFromResource(name: String): Image {
		//if (applet != null)
		//  return loadImageFromApplet(name);
		val bytes = ByteArrayOutputStream()
		try {
			javaClass.classLoader.getResourceAsStream(name).use { reader ->
				val buffer = ByteArray(1024)
				while (true) {
					val len = reader.read(buffer)
					if (len < 0) break
					bytes.write(buffer, 0, len)
				}
				return ImageIcon(bytes.toByteArray()).image
			}
		} catch (e: NullPointerException) {
			throw FileNotFoundException(name)
		}
	}

	@Throws(Exception::class)
	private fun loadImageFromApplet(name: String): Image? {
		log.debug("load image from applet")
		return try {
			ImageIcon(AWTImageMgr::class.java.getResource(name)).image
		} catch (e: Exception) {
			System.err.println("Not found via Applet: " + e.message)
			null
		}
	}

	private val paths: MutableList<String> = ArrayList()
	fun addSearchPath(s: String) {
		paths.add(0, s)
	}

	@Synchronized
	fun loadImage(fileOrResourceName: String, transparent: Color?): Int {
		val id = images.size
		val (image: Image, source: String) = try {
			try {
				Pair(loadImageFromFile(fileOrResourceName), "File")
			} catch (e: FileNotFoundException) {
				try {
					Pair(loadImageFromSearchPaths(fileOrResourceName), "Search Paths")
				} catch (ee: FileNotFoundException) {
					Pair(loadImageFromResource(fileOrResourceName), "Resources")
				}
			}
		} catch (e: FileNotFoundException) {
			log.error("File '" + fileOrResourceName + "' Not found on file paths or resources. Working dir is: " + File(".").absolutePath)
			throw GException("File not found '$fileOrResourceName'")
		} catch (e: Exception) {
			log.error(e.javaClass.simpleName + ":" + e.message)
			throw GException("Cannot load image '$fileOrResourceName'")
		}
		log.debug("Loaded $fileOrResourceName id[$id] from $source with resolution ${image.getWidth(null)} x ${image.getHeight(null)}")
		if (transparent != null) {
			return addImage(transform(image, AWTTransparencyFilter(transparent)))
		}
		return addImage(image)
	}

	/**
	 * Return an array 'num_cells' in length that is filled with ids to subimages
	 * of source where each subimage is width x height in dimension.  When
	 * celled is true, then each subimage is assumed to be bordered by 1 pixel
	 * border and the border is ommited.
	 *
	 * @param source
	 * @param width
	 * @param height
	 * @param num_cells_x
	 * @param num_cells
	 * @param celled
	 * @return
	 */
	@Synchronized
	fun loadImageCells(source: Image, width: Int, height: Int, num_cells_x: Int, num_cells: Int, celled: Boolean): IntArray {
		val cellDelta = if (celled) 1 else 0
		var x = cellDelta
		var y = cellDelta
		val result = IntArray(num_cells)
		var nx = 0
		for (i in 0 until num_cells) {
			result[i] = newSubImage(source, x, y, width, height)
			if (++nx == num_cells_x) {
				nx = 0
				x = if (celled) 1 else 0
				y += height + cellDelta
			} else {
				x += width + cellDelta
			}
		}
		return result
	}

	/**
	 * Convenience method
	 * @param file
	 * @param width
	 * @param height
	 * @param num_cells_x
	 * @param num_cells
	 * @param celled
	 * @return
	 */
	@Synchronized
	fun loadImageCells(file: String, width: Int, height: Int, num_cells_x: Int, num_cells: Int, celled: Boolean, transparentColor: Color?): IntArray {
		return loadImageCells(loadImage(file, transparentColor), width, height, num_cells_x, num_cells, celled)
	}

	@Synchronized
	fun loadImageCells(file: String, cells: Array<IntArray>): IntArray {
		val srcId = loadImage(file)
		getImage(srcId)?.let { source ->
			val result = IntArray(cells.size)
			for (i in result.indices) {
				val x = cells[i][0]
				val y = cells[i][1]
				val w = cells[i][2]
				val h = cells[i][3]
				result[i] = newSubImage(source, x, y, w, h)
			}
			deleteImage(srcId)
			return result
		}
		return intArrayOf()
	}

	/**
	 * Convenience method, use getSourceImage(sourceId) as source Image.
	 *
	 * @param sourceId
	 * @param width width of each sub image
	 * @param height height of each subimage
	 * @param numx number of cells on each row
	 * @param num number of cells total
	 * @param celled true of each cell has a 1 pixel border
	 * @return
	 */
	@Synchronized
	fun loadImageCells(sourceId: Int, width: Int, height: Int, numx: Int, num: Int, celled: Boolean): IntArray {
		getImage(sourceId)?.let { source ->
			return loadImageCells(source, width, height, numx, num, celled)
		}
		return intArrayOf()
	}

	/**
	 *
	 * @param fileName
	 * @return
	 */
	fun loadImage(fileName: String): Int {
		return loadImage(fileName, null)
	}

	/**
	 *
	 * @param id
	 * @param color
	 *
	fun setTransparent(id: Int, color: Color?) {
	val meta = images[id]
	var image = meta.source
	image = transform(image, AWTTransparencyFilter(color))
	meta.source = image
	}*/

	/**
	 *
	 * @param id
	 * @return
	 */
	fun getImage(id: Int): Image? = images.getOrNull(id)

	/**
	 *
	 * @param source
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @return
	 */
	fun newSubImage(source: Image, x: Int, y: Int, w: Int, h: Int): Int {
		val cropped = transform(source, CropImageFilter(x, y, w, h))
		return addImage(cropped)
	}

	/**
	 *
	 * @param image
	 * @return
	 */
	fun addImage(image: Image): Int {
		return images.indexOfFirst { it == null }.takeIf { it >= 0 }?.let {
			images[it] = image
			return it
		} ?: run {
			images.add(image)
			return images.size - 1
		}
	}

	/**
	 *
	 * @param id
	 */
	fun deleteImage(id: Int) {
		images[id] = null
	}

	fun deleteAll() {
		while (images.size > 1) images.removeAt(images.size - 1)
	}

	/*
	 * 
	 */
	@Synchronized
	fun transform(image: Image, filter: ImageFilter): Image {
		val p: ImageProducer = FilteredImageSource(image!!.source, filter)
		val newImage = Toolkit.getDefaultToolkit().createImage(p)
		waitForIt(newImage)
		return newImage
	}

	/**
	 * Only 0, 90, 180 and 270 supported at this time
	 *
	 * @param sourceId
	 * @param degrees
	 * @param comp
	 * @return
	 */
	fun newRotatedImage(sourceId: Int, degrees: Int, comp: Component?): Int {
		val image = getImage(sourceId)
		if (image == null || degrees == 0) return sourceId
		val srcWid = image.getWidth(comp)
		val srcHgt = image.getHeight(comp)
		val srcDim = GDimension(srcWid.toFloat(), srcHgt.toFloat())
		val dstDim = srcDim.rotated(degrees.toFloat())
		val dstWid = Math.ceil(dstDim.width.toDouble()).toInt()
		val dstHgt = Math.ceil(dstDim.height.toDouble()).toInt()
		val rotated = BufferedImage(dstWid, dstHgt, BufferedImage.TYPE_INT_ARGB)
		val G = rotated.graphics as Graphics2D
		val T = AffineTransform()
		T.translate((dstWid / 2).toDouble(), (dstHgt / 2).toDouble())
		T.rotate((CMath.DEG_TO_RAD * degrees).toDouble())
		T.translate((-srcWid / 2).toDouble(), (-srcHgt / 2).toDouble())
		G.drawImage(image, T, null)
		return addImage(rotated)
	}

	@Synchronized
	fun newImage(pixels: IntArray?, w: Int, h: Int): Int {
		val img = Toolkit.getDefaultToolkit().createImage(MemoryImageSource(w, h, pixels, 0, w))
		return addImage(img)
	}

	fun replaceImage(key: Int, img: Image) {
		images[key] = img
	}

	/*
	 *
	 */
	private fun waitForIt(image: Image) {
		Utils.waitNoThrow(this, 100)
	}

	companion object {
		var applet: AWTApplet? = null
	}
}