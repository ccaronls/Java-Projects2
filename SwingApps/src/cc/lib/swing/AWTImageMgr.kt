package cc.lib.swing

import cc.lib.game.GDimension
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.math.CMath
import cc.lib.utils.GException
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.*
import java.io.*
import java.util.*
import javax.swing.ImageIcon
import kotlin.math.abs


class AWTImageMgr {
	private val log = LoggerFactory.getLogger(javaClass)

	internal class ScaledImage(val image: Image?, val w: Int, val h: Int)
	internal class Meta(_source: Image?, val copies: Int) {

		var source: Image? = null
			set(value) {
				field = value
				scaledVersion.clear()
			}
		val scaledVersion = LinkedList<ScaledImage>()

		init {
			source = _source
		}

	}

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

	private val images: MutableList<Meta> = object : ArrayList<Meta>() {
		init {
			// id==0 should always be the 'missing asset' image
			add(Meta(createMissingAssetImage(), 1)) // make sure we start id 1 (not zero)
		}
	} // loaded images

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
		return loadImage(fileOrResourceName, transparent, 2)
	}

	/**
	 *
	 * @param fileOrResourceName
	 * @param transparent
	 * @return
	 */
	@Synchronized
	fun loadImage(fileOrResourceName: String, transparent: Color?, maxCopies: Int): Int {
		val id = images.size
		log.debug("Loading image %d : %s ...", id, fileOrResourceName)
		val image: Image = try {
			try {
				loadImageFromFile(fileOrResourceName).also {
					log.debug("Image '$fileOrResourceName' loaded from file")
				}
			} catch (e: FileNotFoundException) {
				try {
					loadImageFromSearchPaths(fileOrResourceName).also {
						log.debug("Image '$fileOrResourceName' loaded from search paths")
					}
				} catch (ee: FileNotFoundException) {
					loadImageFromResource(fileOrResourceName).also {
						log.debug("Image '$fileOrResourceName' loaded from resources")
					}
				}
			}
		} catch (e: FileNotFoundException) {
			log.error("File '" + fileOrResourceName + "' Not found on file paths or resources. Working dir is: " + File(".").absolutePath)
			throw GException("File not found '$fileOrResourceName'")
		} catch (e: Exception) {
			log.error(e.javaClass.simpleName + ":" + e.message)
			throw GException("Cannot load image '$fileOrResourceName'")
		}
		if (transparent != null) {
			return addImage(transform(image, AWTTransparencyFilter(transparent)))
		}
		return addImage(image, maxCopies)
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
		getSourceImage(srcId)?.let { source ->
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
		getSourceImage(sourceId)?.let { source ->
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
	 * Get an image resized if neccessary to the specified dimension.
	 * The resize op only happens when the dimensions change.
	 * @param id
	 * @param width
	 * @param height
	 * @return
	 */
	@Synchronized
	fun getImage(id: Int, width: Int, height: Int, comp: Component): Image? {
		val meta = images[id]
		for (si in meta.scaledVersion) {
			val dw = abs(width - si.w)
			val dh = abs(height - si.h)
			if (dw <= 1 && dh <= 1) {
				return si.image
			}
		}
		meta.source?.let { image ->
			val curW = image.getWidth(comp)
			val curH = image.getHeight(comp)
			if (width >= 8 && width <= 1024 * 8 && height >= 8 && height <= 1024 * 8) {
				//log.debug("Resizing image [" + id + "] from " + curW + ", " + curH + " too " + width + ", " + height);
				log.debug("Resizing image [%d] from %d, %d too %d, %d", id, curW, curH, width, height)
				transform(image, ReplicateScaleFilter(width, height)).apply {
					meta.scaledVersion.addFirst(ScaledImage(this, width, height))
					if (meta.scaledVersion.size > meta.copies) {
						meta.scaledVersion.removeLast()
					}
				}
			}
			return image
		}
		return null
	}

	/**
	 *
	 * @param id
	 * @return
	 */
	fun getImage(id: Int): Image? {
		val meta = images[id]
		return if (meta.scaledVersion.size == 0) meta.source else meta.scaledVersion.first.image
	}

	/**
	 * Render an image at the specified location and dimension
	 * @param g
	 * @param id
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	fun drawImage(g: Graphics, comp: Component, id: Int, x: Int, y: Int, w: Int, h: Int) {
		try {
			val image = getImage(id, w, h, comp)
			g.drawImage(image, x, y, comp)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	/**
	 *
	 * @param id
	 * @return
	 */
	fun getSourceImage(id: Int): Image? {
		if (id < 0 || id >= images.size)
			return null
		return images[id].source
	}

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
	 * @param id
	 * @return
	 */
	fun getWidth(id: Int): Int {
		val meta = images[id]
		return if (meta.scaledVersion.size > 0) {
			meta.scaledVersion.first.w
		} else meta.source!!.getWidth(null)
	}

	/**
	 *
	 * @param id
	 * @return
	 */
	fun getHeight(id: Int): Int {
		val meta = images[id]
		return if (meta.scaledVersion.size > 0) {
			meta.scaledVersion.first.h
		} else meta.source!!.getHeight(null)
	}

	/**
	 *
	 * @param image
	 * @return
	 */
	@JvmOverloads
	fun addImage(image: Image, maxCopies: Int = 2): Int {
		return images.indexOfFirst { it.source == null }.takeIf { it >= 0 }?.let {
			images[it].source = image
			return it
		} ?: run {
			images.add(Meta(image, maxCopies))
			return images.size - 1
		}
	}

	/**
	 *
	 * @param id
	 */
	fun deleteImage(id: Int) {
		val meta = images[id]
		meta.source = null
		meta.scaledVersion.clear()
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