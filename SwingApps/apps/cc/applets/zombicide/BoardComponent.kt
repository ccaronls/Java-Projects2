package cc.applets.zombicide

import cc.lib.game.GDimension
import cc.lib.logger.LoggerFactory
import cc.lib.swing.AWTGraphics
import cc.lib.swing.AWTRendererComponent
import cc.lib.utils.Table
import cc.lib.utils.launchIn
import cc.lib.zombicide.ZDir
import cc.lib.zombicide.ZIcon
import cc.lib.zombicide.ZMove
import cc.lib.zombicide.ZMoveType
import cc.lib.zombicide.ZPlayerName
import cc.lib.zombicide.ZQuest
import cc.lib.zombicide.ZSiegeTypeEngineType
import cc.lib.zombicide.ZTile
import cc.lib.zombicide.ZZombieType
import cc.lib.zombicide.ui.MiniMapMode
import cc.lib.zombicide.ui.UIZBoardRenderer
import cc.lib.zombicide.ui.UIZComponent
import cc.lib.zombicide.ui.UIZombicide
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import kotlin.math.roundToInt

internal class BoardComponent : AWTRendererComponent<UIZBoardRenderer>(), UIZComponent<AWTGraphics>, WindowListener {
	val log = LoggerFactory.getLogger(javaClass)
	override fun init(g: AWTGraphics) {
		setMouseEnabled(true)
		setGesturesEnabled()
		launchIn {
			loadImages(g)
		}
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
		val files = arrayOf(
			// abominations
			arrayOf<Any>(ZZombieType.Abomination, "zabomination.png", "zabomination_outline.png"),
			arrayOf<Any>(ZZombieType.GreenTwin, "zgreentwin.png", "zabomination_outline.png"),
			arrayOf<Any>(ZZombieType.BlueTwin, "zbluetwin.png", "zabomination_outline.png"),
			arrayOf<Any>(ZZombieType.Wolfbomination, "zwolfabom.png", "zwolfabom_outline.png"),
			arrayOf<Any>(ZZombieType.OrcAbomination, "zorc_abomination.png", "zorc_abomination_outline.png")
			// necromancers
			,
			arrayOf<Any>(ZZombieType.Necromancer, "znecro.png", "znecro_outline.png"),
			arrayOf<Any>(ZZombieType.OrcNecromancer, "zorc_necro.png", "zorc_necro_outline.png"),
			arrayOf<Any>(ZZombieType.RatKing, "zrat_king.png", "zrat_king_outline.png")
			// dragons
			,
			arrayOf<Any>(ZZombieType.NecromanticDragon, "znecro_dragon.png", "znecro_dragon_outline.png"),
			// lord of skulls
			arrayOf<Any>(ZZombieType.LordOfSkulls, "zlordofskulls.png", "zlordofskulls_outline.png"),
			arrayOf<Any>(
				ZZombieType.Ogre,
				"zgloom_abom.png",
				"zgloom_abom_outline.png",
				"zdoom_abom.png",
				"zdoom_abom_outline.png"
			)

			// walkers
			,
			arrayOf<Any>(ZZombieType.Walker, "zwalker1.png", "zwalker1_outline.png"),
			arrayOf<Any>(ZZombieType.Walker, "zwalker2.png", "zwalker2_outline.png"),
			arrayOf<Any>(ZZombieType.Walker, "zwalker3.png", "zwalker3_outline.png"),
			arrayOf<Any>(ZZombieType.Walker, "zwalker4.png", "zwalker4_outline.png"),
			arrayOf<Any>(ZZombieType.Walker, "zwalker5.png", "zwalker5_outline.png"),
			arrayOf<Any>(ZZombieType.OrcWalker, "zorc_walker1.png", "zorc_walker1_outline.png"),
			arrayOf<Any>(ZZombieType.OrcWalker, "zorc_walker2.png", "zorc_walker2_outline.png"),
			arrayOf<Any>(ZZombieType.OrcWalker, "zorc_walker3.png", "zorc_walker3_outline.png"),
			arrayOf<Any>(ZZombieType.OrcWalker, "zorc_walker4.png", "zorc_walker4_outline.png"),
			arrayOf<Any>(ZZombieType.OrcWalker, "zorc_walker5.png", "zorc_walker5_outline.png"),
			arrayOf<Any>(ZZombieType.SpectralWalker, "zspectral_walker1.png", "zspectral_walker1_outline.png"),
			arrayOf<Any>(ZZombieType.SpectralWalker, "zspectral_walker2.png", "zspectral_walker2_outline.png"),
			arrayOf<Any>(ZZombieType.SpectralWalker, "zspectral_walker3.png", "zspectral_walker3_outline.png")
			// runners
			,
			arrayOf<Any>(ZZombieType.Runner, "zrunner1.png", "zrunner1_outline.png"),
			arrayOf<Any>(ZZombieType.Runner, "zrunner2.png", "zrunner2_outline.png"),
			arrayOf<Any>(ZZombieType.OrcRunner, "zorc_runner1.png", "zorc_runner1_outline.png"),
			arrayOf<Any>(ZZombieType.OrcRunner, "zorc_runner2.png", "zorc_runner2_outline.png")
			// fattys
			,
			arrayOf<Any>(ZZombieType.Fatty, "zfatty1.png", "zfatty1_outline.png"),
			arrayOf<Any>(ZZombieType.Fatty, "zfatty2.png", "zfatty2_outline.png"),
			arrayOf<Any>(ZZombieType.OrcFatty, "zorc_fatty1.png", "zorc_fatty1_outline.png"),
			arrayOf<Any>(ZZombieType.OrcFatty, "zorc_fatty2.png", "zorc_fatty2_outline.png")
			// wolfz
			,
			arrayOf<Any>(ZZombieType.Wolfz, "zwulf1.png", "zwulf1_outline.png"),
			arrayOf<Any>(ZZombieType.Wolfz, "zwulf2.png", "zwulf2_outline.png")
			// crowz, ratz
			,
			arrayOf<Any>(ZZombieType.Ratz, "zrats.png", "zrats_outline.png"),
			arrayOf<Any>(ZZombieType.Crowz, "zmurder_crowz.png", "zmurder_crowz_outline.png"),
			arrayOf<Any>(ZZombieType.SwampTroll, "zswamp_troll.png", "zswamp_troll_outline.png")
			// characters
			,
			arrayOf<Any>(ZPlayerName.Clovis, "zchar_clovis.png", "zchar_clovis_outline.png"),
			arrayOf<Any>(ZPlayerName.Baldric, "zchar_baldric.png", "zchar_baldric_outline.png"),
			arrayOf<Any>(ZPlayerName.Ann, "zchar_ann.png", "zchar_ann_outline.png"),
			arrayOf<Any>(ZPlayerName.Nelly, "zchar_nelly.png", "zchar_nelly_outline.png"),
			arrayOf<Any>(ZPlayerName.Samson, "zchar_samson.png", "zchar_samson_outline.png"),
			arrayOf<Any>(ZPlayerName.Silas, "zchar_silas.png", "zchar_silas_outline.png"),
			arrayOf<Any>(ZPlayerName.Tucker, "zchar_tucker.png", "zchar_tucker_outline.png"),
			arrayOf<Any>(ZPlayerName.Jain, "zchar_jain.png", "zchar_jain_outline.png"),
			arrayOf<Any>(ZPlayerName.Benson, "zchar_benson.png", "zchar_benson_outline.png"),
			arrayOf<Any>(ZPlayerName.Theo, "zchar_theo.png", "zchar_theo_outline.png"),
			arrayOf<Any>(ZPlayerName.Morrigan, "zchar_morrigan.png", "zchar_morrigan_outline.png"),
			arrayOf<Any>(ZPlayerName.Karl, "zchar_karl.png", "zchar_karl_outline.png"),
			arrayOf<Any>(ZPlayerName.Ariane, "zchar_ariane.png", "zchar_ariane_outline.png"),
			arrayOf<Any>(ZPlayerName.Arnaud, "zchar_arnaud.png", "zchar_arnaud_outline.png"),
			arrayOf<Any>(ZPlayerName.Seli, "zchar_seli.png", "zchar_seli_outline.png")

			// characters card
			,
			arrayOf<Any>(ZPlayerName.Ann.name, "zcard_ann.png"),
			arrayOf<Any>(ZPlayerName.Baldric.name, "zcard_baldric.png"),
			arrayOf<Any>(ZPlayerName.Clovis.name, "zcard_clovis.png"),
			arrayOf<Any>(ZPlayerName.Nelly.name, "zcard_nelly.png"),
			arrayOf<Any>(ZPlayerName.Samson.name, "zcard_samson.png"),
			arrayOf<Any>(ZPlayerName.Silas.name, "zcard_silas.png"),
			arrayOf<Any>(ZPlayerName.Tucker.name, "zcard_tucker.png"),
			arrayOf<Any>(ZPlayerName.Jain.name, "zcard_jain.png"),
			arrayOf<Any>(ZPlayerName.Benson.name, "zcard_benson.png"),
			arrayOf<Any>(ZPlayerName.Theo.name, "zcard_theo.png"),
			arrayOf<Any>(ZPlayerName.Morrigan.name, "zcard_morrigan.png"),
			arrayOf<Any>(ZPlayerName.Karl.name, "zcard_karl.png"),
			arrayOf<Any>(ZPlayerName.Ariane.name, "zcard_ariane.png"),
			arrayOf<Any>(ZPlayerName.Arnaud.name, "zcard_arnaud.png"),
			arrayOf<Any>(ZPlayerName.Seli.name, "zcard_seli.png")
			// icons
			,
			arrayOf<Any>(ZIcon.DRAGON_BILE, "zdragonbile_icon.png"),
			arrayOf<Any>(ZIcon.CLAWS, "zclaws1_icon.png"),
			arrayOf<Any>(ZIcon.CLAWS, "zclaws2_icon.png"),
			arrayOf<Any>(ZIcon.CLAWS, "zclaws3_icon.png"),
			arrayOf<Any>(ZIcon.CLAWS, "zclaws4_icon.png"),
			arrayOf<Any>(ZIcon.CLAWS, "zclaws5_icon.png"),
			arrayOf<Any>(ZIcon.CLAWS, "zclaws6_icon.png"),
			arrayOf<Any>(ZIcon.SHIELD, "zshield_icon.png"),
			arrayOf<Any>(ZIcon.SLIME, "zslime_icon.png"),
			arrayOf<Any>(ZIcon.RUBBLE, "zrubble_icon3.png"),
			arrayOf<Any>(ZIcon.TORCH, "ztorch_icon.png"),
			arrayOf<Any>(ZIcon.ARROW, "zarrow_icon.png"),
			arrayOf<Any>(ZIcon.BOLT, "zbolt_icon.png"),
			arrayOf<Any>(ZIcon.SPAWN_RED, "zspawn_red.png"),
			arrayOf<Any>(ZIcon.SPAWN_BLUE, "zspawn_blue.png"),
			arrayOf<Any>(ZIcon.SPAWN_GREEN, "zspawn_green.png"),
			arrayOf<Any>(ZIcon.SPAWN_NECRO, "zspawn_necro.png"),
			arrayOf<Any>(ZIcon.SLASH, "zslash1.png"),
			arrayOf<Any>(ZIcon.SLASH, "zslash2.png"),
			arrayOf<Any>(ZIcon.SLASH, "zslash3.png"),
			arrayOf<Any>(ZIcon.SLASH, "zslash4.png"),
			arrayOf<Any>(ZIcon.SLASH, "zslash5.png"),
			arrayOf<Any>(ZIcon.FIREBALL, "zfireball.png"),
			arrayOf<Any>(ZIcon.GRAVESTONE, "zgravestone.png"),
			arrayOf<Any>(ZIcon.PADLOCK, "zpadlock3.png"),
			arrayOf<Any>(ZIcon.NOISE, "znoise_icon.png"),
			arrayOf<Any>(ZIcon.SKULL, "zskull.png"),
			arrayOf<Any>(ZIcon.DAGGER, "zdagger_icon.png"),
			arrayOf<Any>(ZIcon.BOULDER, "zboulder_icon.png"),
			arrayOf<Any>(ZIcon.SWORD, "zsword_icon.png"),
			arrayOf<Any>(ZIcon.MJOLNIR, "zmjolnir.png"),
			arrayOf<Any>(ZIcon.BLACKBOOK, "zblack_book.png"),
			arrayOf<Any>(ZIcon.SPEAR, "zspear.png")
			//,arrayOf<Any>(ZIcon.FIRE, "zfire_icons.png") <-- TODO: Handle special case cell loader
			,
			arrayOf<Any>(ZSiegeTypeEngineType.CATAPULT, "catapult.png"),
			arrayOf<Any>(ZSiegeTypeEngineType.CATAPULT, "catapult_outline.png"),
			arrayOf<Any>(ZSiegeTypeEngineType.BALLISTA, "ballista.png"),
			arrayOf<Any>(ZSiegeTypeEngineType.BALLISTA, "ballista_outline.png")
		)
		val objectToImageMap: MutableMap<Any, MutableList<Int>> = HashMap()
		totalImagesToLoad = files.size
		for (entry in files) {
			val key = entry[0]
			for (i in 1 until entry.size) {
				val file = entry[i] as String
				val id = g.loadImage(file, null, 1)
				require(id >= 0)
				objectToImageMap.getOrPut(key) { ArrayList() }.add(id)
				numImagesLoaded++
			}
			repaint()
		}
		for (type in ZZombieType.entries) {
//            type.imageOptions = Utils.toIntArray(objectToImageMap.get(type));
			//          type.imageDims = new GDimension[type.imageOptions.length];
			val ids: List<Int> = objectToImageMap[type] ?: error("Missing key $type")
			println("Processing $type")
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
				type.imageDims[idx] = GDimension(g.getImage(type.imageOptions[idx])!!)
				idx++
				i += 2
			}
		}
		for (pl in ZPlayerName.entries) {
			objectToImageMap[pl]?.let {
				pl.imageId = it[0]
				pl.outlineImageId = it[1]
				pl.imageDim = GDimension(g.getImage(pl.imageId)!!)
			} ?: error("Missing player type $pl")
			pl.cardImageId = objectToImageMap[pl.name]?.get(0) ?: error("Missing card ${pl.name}")
		}

		// Icons that 'spin'
		for (icon in arrayOf(
			ZIcon.DRAGON_BILE,
			ZIcon.TORCH,
			ZIcon.SWORD,
			ZIcon.DAGGER,
			ZIcon.BOULDER
		)) {
			val ids = IntArray(8)
			ids[0] = objectToImageMap[icon]?.get(0) ?: error("Missing icon $icon")
			for (i in 1 until ids.size) {
				val deg = 45 * i
				ids[i] = g.createRotatedImage(ids[0], deg)
				numImagesLoaded++
				repaint()
			}
			icon.imageIds = ids
		}

		// Icons that shoot
		for (icon in arrayOf(ZIcon.ARROW, ZIcon.MJOLNIR, ZIcon.BOLT, ZIcon.SPEAR)) {
			val ids = IntArray(4)
			val eastId = objectToImageMap[icon]?.get(0) ?: error("Missing icon $icon")
			ids[ZDir.EAST.ordinal] = eastId
			ids[ZDir.WEST.ordinal] = g.createRotatedImage(eastId, 180)
			ids[ZDir.NORTH.ordinal] = g.createRotatedImage(eastId, 270)
			ids[ZDir.SOUTH.ordinal] = g.createRotatedImage(eastId, 90)
			icon.imageIds = ids
			numImagesLoaded++
			repaint()
		}
		for (icon in arrayOf(ZIcon.SPAWN_RED, ZIcon.SPAWN_GREEN, ZIcon.SPAWN_BLUE, ZIcon.SPAWN_NECRO)) {
			val ids = IntArray(4)
			val northId = objectToImageMap[icon]?.get(0) ?: error("Missing icon $icon")
			ids[ZDir.NORTH.ordinal] = northId
			ids[ZDir.WEST.ordinal] = g.createRotatedImage(northId, 270)
			ids[ZDir.EAST.ordinal] = g.createRotatedImage(northId, 90)
			ids[ZDir.SOUTH.ordinal] = northId
			icon.imageIds = ids
			numImagesLoaded++
			repaint()
		}

		// Icons that have a single id variation
		for (icon in arrayOf(
			ZIcon.CLAWS,
			ZIcon.SHIELD,
			ZIcon.SLIME,
			ZIcon.RUBBLE,
			ZIcon.SLASH,
			ZIcon.FIREBALL,
			ZIcon.GRAVESTONE,
			ZIcon.PADLOCK,
			ZIcon.NOISE,
			ZIcon.SKULL,
			ZIcon.BLACKBOOK
		)) {
			icon.imageIds =
				intArrayOf(*objectToImageMap[icon]?.toIntArray() ?: error("No icon $icon"))
			numImagesLoaded++
			repaint()
		}
		run {
			val icon = ZIcon.FIRE
			val cells = arrayOf(
				intArrayOf(0, 0, 56, 84),
				intArrayOf(56, 0, 131 - 56, 84),
				intArrayOf(131, 0, 196 - 131, 84),
				intArrayOf(0, 84, 60, 152 - 84),
				intArrayOf(60, 84, 122 - 60, 152 - 84),
				intArrayOf(122, 84, 196 - 122, 152 - 84)
			)
			icon.imageIds = g.loadImageCells("zfire_icons.png", cells)
			numImagesLoaded++
			repaint()
		}
		for (type in ZSiegeTypeEngineType.entries) {
			objectToImageMap[type]?.let {
				type.imageId = it[0]
				type.imageOutlineId = it[1]
				type.dimension = GDimension(g.getImage(it[0])!!)
				numImagesLoaded += 2
			} ?: run {
				error("No type $type")
			}
		}
		log.debug("Images: $objectToImageMap")
		numImagesLoaded = totalImagesToLoad
		ZombicideApplet.instance.onAllImagesLoaded()
		renderer.drawTiles = ZombicideApplet.instance.getStringProperty("tiles", "no") == "yes"
		renderer.drawDebugText =
			ZombicideApplet.instance.getStringProperty("debugText", "no") == "yes"
		renderer.drawRangedAccessibility =
			ZombicideApplet.instance.getStringProperty("rangedAccessibility", "no") == "yes"
		renderer.drawTowersHighlighted =
			ZombicideApplet.instance.getStringProperty("drawTowersHighlighted", "no") == "yes"
		renderer.drawZombiePaths =
			ZombicideApplet.instance.getStringProperty("drawZombiePaths", "no") == "yes"
		renderer.miniMapMode = ZombicideApplet.instance.getEnumProperty(
			"miniMapMode",
			MiniMapMode::class.java,
			MiniMapMode.OFF
		)
		renderer.drawScreenCenter =
			ZombicideApplet.instance.getStringProperty("drawScreenCenter", "no") == "yes"
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

	fun initKeysPresses(options: MutableList<*>) {
		keyMap.clear()
		val it = options.iterator()
		while (it.hasNext()) {
			val move = it.next() as? ZMove ?: continue
			when (move.type) {
				ZMoveType.WALK_DIR -> {
					when (ZDir.entries[move.integer!!]) {
						ZDir.NORTH -> keyMap[KeyEvent.VK_UP] = move
						ZDir.SOUTH -> keyMap[KeyEvent.VK_DOWN] = move
						ZDir.EAST -> keyMap[KeyEvent.VK_RIGHT] = move
						ZDir.WEST -> keyMap[KeyEvent.VK_LEFT] = move
						ZDir.DESCEND, ZDir.ASCEND -> keyMap[KeyEvent.VK_SLASH] = move
						else -> Unit
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
		log.debug("initKeyPressed: $keyMap")
	}

	var keyMap: MutableMap<Int, ZMove> = HashMap()

	init {
		setPreferredSize(250, 250)
	}

	override fun onKeyTyped(e: KeyEvent) {
		log.debug("onKeyTyped: ${e.keyChar}:${e.keyCode}")
		val game = UIZombicide.instance
		keyMap[e.keyCode]?.let { move ->
			game.setResult(move)
			return
		}
		when (e.keyCode) {
			KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_UP -> {}
			KeyEvent.VK_Z -> renderer.toggleZoomType()
			KeyEvent.VK_PLUS, KeyEvent.VK_EQUALS -> renderer.zoomAmt(.8f)
			KeyEvent.VK_MINUS, KeyEvent.VK_UNDERSCORE -> renderer.zoomAmt(1.2f)
			KeyEvent.VK_T -> ZombicideApplet.instance.setStringProperty(
				"tiles",
				if (renderer.toggleDrawTiles()) "yes" else "no"
			)

			KeyEvent.VK_D -> ZombicideApplet.instance.setStringProperty(
				"debugText",
				if (renderer.toggleDrawDebugText()) "yes" else "no"
			)

			KeyEvent.VK_R -> ZombicideApplet.instance.setStringProperty(
				"rangedAccessibility",
				if (renderer.toggleDrawRangedAccessibility()) "yes" else "no"
			)

			KeyEvent.VK_P -> ZombicideApplet.instance.setStringProperty(
				"drawZombiePaths",
				if (renderer.toggleDrawZoombiePaths()) "yes" else "no"
			)

			KeyEvent.VK_H -> ZombicideApplet.instance.setStringProperty(
				"drawTowersHighlighted",
				if (renderer.toggleDrawTowersHighlighted()) "yes" else "no"
			)

			KeyEvent.VK_M -> ZombicideApplet.instance.setEnumProperty(
				"miniMapMode",
				renderer.toggleDrawMinimap()
			)

			KeyEvent.VK_C -> ZombicideApplet.instance.setStringProperty(
				"drawScreenCenter",
				if (renderer.toggleDrawScreenCenter()) "yes" else "no"
			)

			KeyEvent.VK_L -> ZombicideApplet.instance.setStringProperty(
				"drawClickable",
				if (renderer.toggleDrawClickables()) "yes" else "no"
			)

			KeyEvent.VK_ESCAPE -> renderer.setOverlay(null)
			KeyEvent.VK_BACK_QUOTE -> {
				renderer.setOverlay(null)
				repaint()
			}

			else -> renderer.setOverlay(
				Table()
					.addRow("Z", "Toggle Zoom Type")
					.addRow("+ / -", "Zoom in / out", (100 * renderer.zoomPercent).roundToInt())
					.addRow("T", "Toggle draw Tiles", renderer.drawTiles)
					.addRow("D", "Toggle draw Debug text", renderer.drawDebugText)
					.addRow(
						"R",
						"Toggle show Ranged Accessibility",
						renderer.drawRangedAccessibility
					)
					.addRow("P", "Toggle draw zombie paths", renderer.drawZombiePaths)
					.addRow("H", "Toggle draw towers highlighted", renderer.drawTowersHighlighted)
					.addRow("M", "Toggle Minimap mode", renderer.miniMapMode.name)
					.addRow("C", "Toggle draw Center", renderer.drawScreenCenter)
			)
		}
		repaint()
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