package cc.experiments

import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.Utils
import cc.lib.math.Vector2D
import cc.lib.swing.AWTComponent
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTGraphics
import cc.lib.swing.AWTImage
import cc.lib.swing.BlendComposite
import cc.lib.utils.KFileUtils.getOrCreateSettingsDirectory
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.event.KeyEvent
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File

class BitmapOutlineTest internal constructor() : AWTComponent() {
	val frame: AWTFrame
	var abomId = 0
	var outlineid = 0
	override fun init(g: AWTGraphics) {
		setMouseEnabled(true)
		abomId = g.loadImage("zabomination.png")
		curBComposite = frame.getIntProperty("curB", 0)
		curAComposite = frame.getIntProperty("curA", 0)
		curAComposite2 = frame.getIntProperty("curA2", 0)
		build(g)
	}

	fun build(g: AWTGraphics) {
		val img = g.getImage(abomId) as AWTImage?
		val aspect = img!!.aspect
		val width = Math.round(img.width)
		val height = Math.round(img.height)

		// Create a new bitmap that same size as original
		val image = BufferedImage(width, height,
			BufferedImage.TYPE_INT_ARGB)
		val G = image.createGraphics()
		val orig = G.composite

		// render the original image ontop of newimaage, scaled up and in complete white
		//G.setColor(Color.WHITE);
		//G.fillRect(0, 0, image.getWidth(), image.getHeight());
		G.composite = allBlend[curBComposite]
		val M = AffineTransform()
		M.setToIdentity()
		M.translate((width / 2).toDouble(), (height / 2).toDouble())
		M.scale(1.1, 1.05)
		M.translate((-width / 2).toDouble(), (-height / 2).toDouble())
		G.drawImage(img.image, M, this)
		//G.drawImage(img.getImage(), 0, 0, image.getWidth(), image.getHeight(), this);
		G.composite = allAlpha[curAComposite2]
		G.color = Color.WHITE
		G.fillRect(0, 0, width, height)
		G.composite = allAlpha[curAComposite]
		G.drawImage(img.image, 0, 0, img.image.getWidth(this), img.image.getHeight(this), this)
		G.composite = orig
		outlineid = g.addImage(image)

		/*
        Image filtered = g.getImageMgr().transform(image, new RGBImageFilter() {
            @Override
            public int filterRGB(int x, int y, int rgb) {
                GColor c = GColor.fromRGB(rgb);

                float red=  c.getRed();
                float grn = c.getGreen();
                float blu = c.getBlue();
                float alpha = c.getAlpha();

                if (red == 1 && grn == 1 && blu == 1)
                    return 0;

                return -1;
            }
        });

        targetid = g.addImage(filtered);
*/repaint()
	}

	var idx = 0
	var curBComposite = 0
	var curAComposite = 0
	var curAComposite2 = 0
	var curOutlineColor = 0
	override fun paint(g: AWTGraphics) {
		if (outlineid == 0) {
			build(g)
		}
		g.backgroundColor = GColor.LIGHT_GRAY
		g.clearScreen()
		val center = Vector2D(width / 2, height / 2)
		g.clearScreen()
		val img = g.getImage(outlineid)
		val rect = GRectangle(img!!).withCenter(center)
		g.drawImage(outlineid, center)
		//g.setTransparencyFilter(.5f);
		//g.drawImage(abomId, center);
		//g.removeFilter();
		g.color = outlineColors[curOutlineColor]
		//g.setAlphaComposite(1, AlphaComposite.SRC_IN);
		g.setComposite(BlendComposite.Multiply)
		g.drawFilledRect(rect)
		g.color = GColor.RED
		rect.drawOutlined(g)
		val txt = String.format("Alpha Compisite: %s\nAlpha Compisite2: %s\nBlendComposite: %s",
			alphaRule[allAlpha[curAComposite].rule],
			alphaRule[allAlpha[curAComposite2].rule],
			allBlend[curBComposite].mode)
		g.drawJustifiedString(10f, 10f, txt)

		// Freeze, with Dst out gives desired effect
	}

	@Synchronized
	override fun onKeyReleased(evt: KeyEvent) {
		//curComposite = (curComposite+1) % all.length;
		when (evt.keyCode) {
			KeyEvent.VK_A -> {
				curAComposite = (curAComposite + 1) % allAlpha.size
				frame.setProperty("curA", curAComposite)
			}

			KeyEvent.VK_B -> {
				curBComposite = (curBComposite + 1) % allBlend.size
				frame.setProperty("curB", curBComposite)
			}

			KeyEvent.VK_C -> {
				curAComposite2 = (curAComposite2 + 1) % allAlpha.size
				frame.setProperty("curA2", curAComposite2)
			}

			KeyEvent.VK_SPACE -> curOutlineColor = (curOutlineColor + 1) % outlineColors.size
		}
		outlineid = 0
		repaint()
	}

	var outlineColors = arrayOf(
		GColor.YELLOW, GColor.GREEN, GColor.RED
	)
	var allBlend = arrayOf(
		BlendComposite.Add,
		BlendComposite.Average,
		BlendComposite.Blue,
		BlendComposite.Color,
		BlendComposite.ColorBurn,
		BlendComposite.ColorDodge,
		BlendComposite.Darken,
		BlendComposite.Difference,
		BlendComposite.Exclusion,
		BlendComposite.Freeze,
		BlendComposite.Glow,
		BlendComposite.Green,
		BlendComposite.HardLight,
		BlendComposite.Heat,
		BlendComposite.Hue,
		BlendComposite.InverseColorBurn,
		BlendComposite.InverseColorDodge,
		BlendComposite.Lighten,
		BlendComposite.Luminosity,
		BlendComposite.Multiply,
		BlendComposite.Negation,
		BlendComposite.Overlay,
		BlendComposite.Red,
		BlendComposite.Reflect,
		BlendComposite.Saturation,
		BlendComposite.Screen,
		BlendComposite.SoftBurn,
		BlendComposite.SoftDodge,
		BlendComposite.SoftLight,
		BlendComposite.Stamp,
		BlendComposite.Subtract
	)
	var allAlpha = arrayOf(
		AlphaComposite.Clear,
		AlphaComposite.Dst,
		AlphaComposite.DstAtop,
		AlphaComposite.DstIn,
		AlphaComposite.DstOut,
		AlphaComposite.DstOver,
		AlphaComposite.Src,
		AlphaComposite.SrcAtop,
		AlphaComposite.SrcIn,
		AlphaComposite.SrcOut,
		AlphaComposite.SrcOver,
		AlphaComposite.Xor
	)
	var alphaRule = arrayOf(
		"???",
		"Clear",
		"SRC",
		"SrcOver",
		"DstOver",
		"SrcIn",
		"DstIn",
		"SrcOut",
		"DstOUt",
		"Dst",
		"SrcAtop",
		"DstAtop",
		"Xor")

	init {
		frame = object : AWTFrame("Image Rotate Test") {
			override fun onWindowClosing() {
				try {
					//app.figures.saveToFile(app.figuresFile);
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
		}
		frame.add(this)
		frame.centerToScreen(600, 600)
		val file = File(BitmapOutlineTest::class.java.getOrCreateSettingsDirectory(), "gui.properties")
		frame.setPropertiesFile(file)
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			BitmapOutlineTest()
		}
	}
}
