package cc.applets.zombicide

import cc.lib.game.GDimension
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.swing.AWTGraphics
import cc.lib.swing.AWTRendererComponent
import cc.lib.utils.Table
import cc.lib.zombicide.*
import cc.lib.zombicide.ui.MiniMapMode
import cc.lib.zombicide.ui.UIZBoardRenderer
import cc.lib.zombicide.ui.UIZComponent
import cc.lib.zombicide.ui.UIZombicide.Companion.instance
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import kotlin.math.roundToInt

internal class BoardComponent : AWTRendererComponent<UIZBoardRenderer>(), UIZComponent<AWTGraphics>, WindowListener {
	val log = LoggerFactory.getLogger(javaClass)
	override fun init(g: AWTGraphics) {
		setMouseEnabled(true)
		setGesturesEnabled()
		object : Thread() {
			override fun run() {
				loadImages(g)
			}
		}.start()
	}

	var numImagesLoaded = 0
	var totalImagesToLoad = 1000

	override val initProgress: Float
		get() {
			var progress = numImagesLoaded.toFloat() / totalImagesToLoad
			if (progress >= 1 && loadedTiles.isNotEmpty()) {
				progress = numTilesLoaded.toFloat() / (loadedTiles.size + 1)
			}
			return progress
		}

	fun loadImages(g: AWTGraphics) {
		g.addSearchPath("zombicideandroid/src/main/res/drawable")
		val files = arrayOf(arrayOf<Any>(ZZombieType.Abomination, "zabomination.png"), arrayOf<Any>(ZZombieType.Abomination, "zabomination_outline.png"), arrayOf<Any>(ZZombieType.GreenTwin, "zgreentwin.png"), arrayOf<Any>(ZZombieType.GreenTwin, "zabomination_outline.png"), arrayOf<Any>(ZZombieType.BlueTwin, "zbluetwin.png"), arrayOf<Any>(ZZombieType.BlueTwin, "zabomination_outline.png"), arrayOf<Any>(ZZombieType.Necromancer, "znecro.png"), arrayOf<Any>(ZZombieType.Necromancer, "znecro_outline.png"), arrayOf<Any>(ZZombieType.Walker, "zwalker1.png"), arrayOf<Any>(ZZombieType.Walker, "zwalker1_outline.png"), arrayOf<Any>(ZZombieType.Walker, "zwalker2.png"), arrayOf<Any>(ZZombieType.Walker, "zwalker2_outline.png"), arrayOf<Any>(ZZombieType.Walker, "zwalker3.png"), arrayOf<Any>(ZZombieType.Walker, "zwalker3_outline.png"), arrayOf<Any>(ZZombieType.Walker, "zwalker4.png"), arrayOf<Any>(ZZombieType.Walker, "zwalker4_outline.png"), arrayOf<Any>(ZZombieType.Walker, "zwalker5.png"), arrayOf<Any>(ZZombieType.Walker, "zwalker5_outline.png"), arrayOf<Any>(ZZombieType.Runner, "zrunner1.png"), arrayOf<Any>(ZZombieType.Runner, "zrunner1_outline.png"), arrayOf<Any>(ZZombieType.Runner, "zrunner2.png"), arrayOf<Any>(ZZombieType.Runner, "zrunner2_outline.png"), arrayOf<Any>(ZZombieType.Fatty, "zfatty1.png"), arrayOf<Any>(ZZombieType.Fatty, "zfatty1_outline.png"), arrayOf<Any>(ZZombieType.Fatty, "zfatty2.png"), arrayOf<Any>(ZZombieType.Fatty, "zfatty2_outline.png"), arrayOf<Any>(ZZombieType.Wolfz, "zwulf1.png"), arrayOf<Any>(ZZombieType.Wolfz, "zwulf1_outline.png"), arrayOf<Any>(ZZombieType.Wolfz, "zwulf2.png"), arrayOf<Any>(ZZombieType.Wolfz, "zwulf2_outline.png"), arrayOf<Any>(ZZombieType.Wolfbomination, "zwolfabom.png"), arrayOf<Any>(ZZombieType.Wolfbomination, "zwolfabom_outline.png"), arrayOf<Any>(ZPlayerName.Clovis, "zchar_clovis.png"), arrayOf<Any>(ZPlayerName.Baldric, "zchar_baldric.png"), arrayOf<Any>(ZPlayerName.Ann, "zchar_ann.png"), arrayOf<Any>(ZPlayerName.Nelly, "zchar_nelly.png"), arrayOf<Any>(ZPlayerName.Samson, "zchar_samson.png"), arrayOf<Any>(ZPlayerName.Silas, "zchar_silas.png"), arrayOf<Any>(ZPlayerName.Tucker, "zchar_tucker.png"), arrayOf<Any>(ZPlayerName.Jain, "zchar_jain.png"), arrayOf<Any>(ZPlayerName.Benson, "zchar_benson.png"), arrayOf<Any>(ZPlayerName.Theo, "zchar_theo.png"), arrayOf<Any>(ZPlayerName.Morrigan, "zchar_morrigan.png"), arrayOf<Any>(ZPlayerName.Karl, "zchar_karl.png"), arrayOf<Any>(ZPlayerName.Ariane, "zchar_ariane.png"), arrayOf<Any>(ZPlayerName.Clovis, "zchar_clovis_outline.png"), arrayOf<Any>(ZPlayerName.Baldric, "zchar_baldric_outline.png"), arrayOf<Any>(ZPlayerName.Ann, "zchar_ann_outline.png"), arrayOf<Any>(ZPlayerName.Nelly, "zchar_nelly_outline.png"), arrayOf<Any>(ZPlayerName.Samson, "zchar_samson_outline.png"), arrayOf<Any>(ZPlayerName.Silas, "zchar_silas_outline.png"), arrayOf<Any>(ZPlayerName.Tucker, "zchar_tucker_outline.png"), arrayOf<Any>(ZPlayerName.Jain, "zchar_jain_outline.png"), arrayOf<Any>(ZPlayerName.Benson, "zchar_benson_outline.png"), arrayOf<Any>(ZPlayerName.Theo, "zchar_theo_outline.png"), arrayOf<Any>(ZPlayerName.Morrigan, "zchar_morrigan_outline.png"), arrayOf<Any>(ZPlayerName.Karl, "zchar_karl_outline.png"), arrayOf<Any>(ZPlayerName.Ariane, "zchar_ariane_outline.png"), arrayOf<Any>(ZPlayerName.Ann.name, "zcard_ann.png"), arrayOf<Any>(ZPlayerName.Baldric.name, "zcard_baldric.png"), arrayOf<Any>(ZPlayerName.Clovis.name, "zcard_clovis.png"), arrayOf<Any>(ZPlayerName.Nelly.name, "zcard_nelly.png"), arrayOf<Any>(ZPlayerName.Samson.name, "zcard_samson.png"), arrayOf<Any>(ZPlayerName.Silas.name, "zcard_silas.png"), arrayOf<Any>(ZPlayerName.Tucker.name, "zcard_tucker.png"), arrayOf<Any>(ZPlayerName.Jain.name, "zcard_jain.png"), arrayOf<Any>(ZPlayerName.Benson.name, "zcard_benson.png"), arrayOf<Any>(ZPlayerName.Theo.name, "zcard_theo.png"), arrayOf<Any>(ZPlayerName.Morrigan.name, "zcard_morrigan.png"), arrayOf<Any>(ZPlayerName.Karl.name, "zcard_karl.png"), arrayOf<Any>(ZPlayerName.Ariane.name, "zcard_ariane.png"), arrayOf<Any>(ZIcon.DRAGON_BILE, "zdragonbile_icon.png"), arrayOf<Any>(ZIcon.CLAWS, "zclaws1_icon.png"), arrayOf<Any>(ZIcon.CLAWS, "zclaws2_icon.png"), arrayOf<Any>(ZIcon.CLAWS, "zclaws3_icon.png"), arrayOf<Any>(ZIcon.CLAWS, "zclaws4_icon.png"), arrayOf<Any>(ZIcon.CLAWS, "zclaws5_icon.png"), arrayOf<Any>(ZIcon.CLAWS, "zclaws6_icon.png"), arrayOf<Any>(ZIcon.SHIELD, "zshield_icon.png"), arrayOf<Any>(ZIcon.SLIME, "zslime_icon.png"), arrayOf<Any>(ZIcon.TORCH, "ztorch_icon.png"), arrayOf<Any>(ZIcon.ARROW, "zarrow_icon.png"), arrayOf<Any>(ZIcon.SPAWN_RED, "zspawn_red.png"), arrayOf<Any>(ZIcon.SPAWN_BLUE, "zspawn_blue.png"), arrayOf<Any>(ZIcon.SPAWN_GREEN, "zspawn_green.png"), arrayOf<Any>(ZIcon.SLASH, "zslash1.png"), arrayOf<Any>(ZIcon.SLASH, "zslash2.png"), arrayOf<Any>(ZIcon.SLASH, "zslash3.png"), arrayOf<Any>(ZIcon.SLASH, "zslash4.png"), arrayOf<Any>(ZIcon.SLASH, "zslash5.png"), arrayOf<Any>(ZIcon.FIREBALL, "zfireball.png"), arrayOf<Any>(ZIcon.GRAVESTONE, "zgravestone.png"), arrayOf<Any>(ZIcon.PADLOCK, "zpadlock3.png"), arrayOf<Any>(ZIcon.SKULL, "zskull.png"), arrayOf<Any>(ZIcon.DAGGER, "zdagger_icon.png"), arrayOf<Any>(ZIcon.SWORD, "zsword_icon.png"), arrayOf<Any>(ZIcon.MJOLNIR, "zmjolnir.png"), arrayOf<Any>(ZIcon.BLACKBOOK, "zblack_book.png"))
		val objectToImageMap: MutableMap<Any, MutableList<Int>> = HashMap()
		totalImagesToLoad = files.size + ZIcon.values().size
		for (entry in files) {
			val key = entry[0]
			val file = entry[1] as String
			val id = g.loadImage(file, null, 1)
			if (id >= 0) {
				if (!objectToImageMap.containsKey(key)) {
					objectToImageMap[key] = ArrayList()
				}
				objectToImageMap[key]!!.add(id)
			}
			numImagesLoaded++
			repaint()
		}
		for (type in ZZombieType.values()) {
//            type.imageOptions = Utils.toIntArray(objectToImageMap.get(type));
			//          type.imageDims = new GDimension[type.imageOptions.length];
			val ids: List<Int> = objectToImageMap[type]!!
			type.imageOptions = IntArray(ids.size / 2)
			type.imageOutlineOptions = IntArray(type.imageOptions.size)
			type.imageDims = Array(type.imageOptions.size) {
				GDimension()
			}
			var idx = 0
			var i = 0
			while (i < ids.size) {
				type.imageOptions[idx] = ids[i]
				type.imageOutlineOptions[idx] = ids[i + 1]
				type.imageDims[idx] = GDimension(g.getImage(type.imageOptions[idx]))
				idx++
				i += 2
			}
		}
		for (pl in ZPlayerName.values()) {
			pl.imageId = objectToImageMap[pl]!![0]
			pl.outlineImageId = Utils.getOrNull(objectToImageMap[pl], 1)
			pl.imageDim = GDimension(g.getImage(pl.imageId))
			pl.cardImageId = objectToImageMap[pl.name]!![0]
		}

		// Icons that 'spin'
		for (icon in Utils.toArray<ZIcon>(ZIcon.DRAGON_BILE, ZIcon.TORCH, ZIcon.SWORD, ZIcon.DAGGER)) {
			val ids = IntArray(8)
			ids[0] = objectToImageMap[icon]!![0]
			for (i in 1 until ids.size) {
				val deg = 45 * i
				ids[i] = g.createRotatedImage(ids[0], deg)
				numImagesLoaded++
				repaint()
			}
			icon.imageIds = ids
		}

		// Icons that shoot
		for (icon in Utils.toArray<ZIcon>(ZIcon.ARROW, ZIcon.MJOLNIR)) {
			val ids = IntArray(4)
			val eastId = objectToImageMap[icon]!![0]
			ids[ZDir.EAST.ordinal] = eastId
			ids[ZDir.WEST.ordinal] = g.createRotatedImage(eastId, 180)
			ids[ZDir.NORTH.ordinal] = g.createRotatedImage(eastId, 270)
			ids[ZDir.SOUTH.ordinal] = g.createRotatedImage(eastId, 90)
			icon.imageIds = ids
			numImagesLoaded++
			repaint()
		}
		for (icon in Utils.toArray<ZIcon>(ZIcon.SPAWN_RED, ZIcon.SPAWN_GREEN, ZIcon.SPAWN_BLUE)) {
			val ids = IntArray(4)
			val northId = objectToImageMap[icon]!![0]
			ids[ZDir.NORTH.ordinal] = northId
			ids[ZDir.WEST.ordinal] = g.createRotatedImage(northId, 270)
			ids[ZDir.EAST.ordinal] = g.createRotatedImage(northId, 90)
			ids[ZDir.SOUTH.ordinal] = northId
			icon.imageIds = ids
			numImagesLoaded++
			repaint()
		}

		// Icons that have a single id variation
		for (icon in Utils.toArray<ZIcon>(ZIcon.CLAWS, ZIcon.SHIELD, ZIcon.SLIME, ZIcon.SLASH, ZIcon.FIREBALL, ZIcon.GRAVESTONE, ZIcon.PADLOCK, ZIcon.SKULL, ZIcon.BLACKBOOK)) {
			icon.imageIds = Utils.toIntArray(objectToImageMap[icon])
			numImagesLoaded++
			repaint()
		}
		run {
			val icon = ZIcon.FIRE
			val cells = arrayOf(intArrayOf(0, 0, 56, 84), intArrayOf(56, 0, 131 - 56, 84), intArrayOf(131, 0, 196 - 131, 84), intArrayOf(0, 84, 60, 152 - 84), intArrayOf(60, 84, 122 - 60, 152 - 84), intArrayOf(122, 84, 196 - 122, 152 - 84))
			icon.imageIds = g.loadImageCells("zfire_icons.png", cells)
			numImagesLoaded++
			repaint()
		}
		log.debug("Images: $objectToImageMap")
		numImagesLoaded = totalImagesToLoad
		ZombicideApplet.instance.onAllImagesLoaded()
		renderer.drawTiles = ZombicideApplet.instance.getStringProperty("tiles", "no") == "yes"
		renderer.drawDebugText = ZombicideApplet.instance.getStringProperty("debugText", "no") == "yes"
		renderer.drawRangedAccessibility = ZombicideApplet.instance.getStringProperty("rangedAccessibility", "no") == "yes"
		renderer.drawTowersHighlighted = ZombicideApplet.instance.getStringProperty("drawTowersHighlighted", "no") == "yes"
		renderer.drawZombiePaths = ZombicideApplet.instance.getStringProperty("drawZombiePaths", "no") == "yes"
		renderer.miniMapMode = ZombicideApplet.instance.getEnumProperty("miniMapMode", MiniMapMode::class.java, MiniMapMode.OFF)
		renderer.drawScreenCenter = ZombicideApplet.instance.getStringProperty("drawScreenCenter", "no") == "yes"
		renderer.drawClickable = ZombicideApplet.instance.getStringProperty("drawClickable", "no") == "yes"
		repaint()
	}

	var loadedTiles = IntArray(0)
	var numTilesLoaded = 0
	override fun loadTiles(g: AWTGraphics, tiles: Array<ZTile>, quest: ZQuest) {
		numTilesLoaded = 0
		g.addSearchPath("zombicideandroid/assets")
		object : Thread() {
			override fun run() {
				for (t in loadedTiles) {
					g.deleteImage(t)
				}
				loadedTiles = IntArray(tiles.size)
				for (i in loadedTiles.indices) {
					loadedTiles[i] = g.loadImage("ztile_" + tiles[i].id + ".png", tiles[i].orientation)
					numTilesLoaded++
					repaint()
				}
				renderer.onTilesLoaded(loadedTiles)
				numTilesLoaded++
				repaint()
			}
		}.start()
		repaint()
	}

	@Synchronized
	fun initKeysPresses(options: MutableList<*>) {
		keyMap.clear()
		val it = options.iterator()
		while (it.hasNext()) {
			val obj = it.next()!! as? ZMove ?: continue
			val move = obj
			when (move.type) {
				ZMoveType.WALK_DIR -> {
					when (ZDir.values()[move.integer!!]) {
						ZDir.NORTH -> keyMap[KeyEvent.VK_UP] = move
						ZDir.SOUTH -> keyMap[KeyEvent.VK_DOWN] = move
						ZDir.EAST -> keyMap[KeyEvent.VK_RIGHT] = move
						ZDir.WEST -> keyMap[KeyEvent.VK_LEFT] = move
						ZDir.DESCEND, ZDir.ASCEND -> keyMap[KeyEvent.VK_SLASH] = move
					}
					it.remove()
				}
				ZMoveType.SWITCH_ACTIVE_CHARACTER -> {
					keyMap[KeyEvent.VK_SPACE] = move
					it.remove()
				}
				else -> Unit
			}
		}
	}

	var keyMap: MutableMap<Int, ZMove> = HashMap()

	init {
		setPreferredSize(250, 250)
	}

	override fun keyPressed(e: KeyEvent) {
		val game = instance
		when (e.keyCode) {
			KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_SLASH -> {
				val move = keyMap[e.keyCode]
				if (move != null) {
					game.setResult(move)
					//keyMap.clear()
				}
			}
			KeyEvent.VK_SPACE -> renderer.toggleZoomType()
			KeyEvent.VK_PLUS, KeyEvent.VK_EQUALS -> renderer.zoomAmt(.8f)
			KeyEvent.VK_MINUS, KeyEvent.VK_UNDERSCORE -> renderer.zoomAmt(1.2f)
			KeyEvent.VK_T -> ZombicideApplet.instance.setStringProperty("tiles", if (renderer.toggleDrawTiles()) "yes" else "no")
			KeyEvent.VK_D -> ZombicideApplet.instance.setStringProperty("debugText", if (renderer.toggleDrawDebugText()) "yes" else "no")
			KeyEvent.VK_R -> ZombicideApplet.instance.setStringProperty("rangedAccessibility", if (renderer.toggleDrawRangedAccessibility()) "yes" else "no")
			KeyEvent.VK_P -> ZombicideApplet.instance.setStringProperty("drawZombiePaths", if (renderer.toggleDrawZoombiePaths()) "yes" else "no")
			KeyEvent.VK_H -> ZombicideApplet.instance.setStringProperty("drawTowersHighlighted", if (renderer.toggleDrawTowersHighlighted()) "yes" else "no")
			KeyEvent.VK_M -> ZombicideApplet.instance.setEnumProperty("miniMapMode", renderer.toggleDrawMinimap())
			KeyEvent.VK_C -> ZombicideApplet.instance.setStringProperty("drawScreenCenter", if (renderer.toggleDrawScreenCenter()) "yes" else "no")
			KeyEvent.VK_L -> ZombicideApplet.instance.setStringProperty("drawClickable", if (renderer.toggleDrawClickables()) "yes" else "no")
			else -> renderer.setOverlay(Table()
				.addRow("+ / -", "Zoom in / out", (100 * renderer.zoomPercent).roundToInt())
				.addRow("T", "Toggle draw Tiles", renderer.drawTiles)
				.addRow("D", "Toggle draw Debug text", renderer.drawDebugText)
				.addRow("R", "Toggle show Ranged Accessibility", renderer.drawRangedAccessibility)
				.addRow("P", "Toggle draw zombie paths", renderer.drawZombiePaths)
				.addRow("H", "Toggle draw towers highlighted", renderer.drawTowersHighlighted)
				.addRow("M", "Toggle Minimap mode", renderer.miniMapMode.name)
				.addRow("C", "Toggle draw Center", renderer.drawScreenCenter)
			)
		}
		repaint()
	}

	@Synchronized
	override fun keyReleased(evt: KeyEvent) {
		when (evt.keyCode) {
			KeyEvent.VK_BACK_QUOTE -> {
				renderer.setOverlay(null)
				repaint()
			}
		}
	}

	override fun windowOpened(e: WindowEvent) {}
	override fun windowClosing(e: WindowEvent) {}
	override fun windowClosed(e: WindowEvent) {}
	override fun windowIconified(e: WindowEvent) {}
	override fun windowDeiconified(e: WindowEvent) {}
	override fun windowActivated(e: WindowEvent) {
		log.debug("grabFocus")
		requestFocusInWindow()
	}

	override fun windowDeactivated(e: WindowEvent) {}
}