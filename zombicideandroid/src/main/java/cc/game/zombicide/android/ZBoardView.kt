package cc.game.zombicide.android

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import cc.lib.android.DroidGraphics
import cc.lib.android.UIComponentView
import cc.lib.game.GDimension
import cc.lib.zombicide.*
import cc.lib.zombicide.ui.UIZBoardRenderer
import cc.lib.zombicide.ui.UIZComponent

class ZBoardView(context: Context, attrs: AttributeSet) : UIComponentView<UIZBoardRenderer<DroidGraphics>>(context, attrs), UIZComponent<DroidGraphics> {
	var progress = 0
	var numImages = 21
	fun initZombieImages(g: DroidGraphics, t: ZZombieType, vararg ids: Int) {
		t.imageOptions = IntArray(ids.size / 2)
		t.imageOutlineOptions = IntArray(ids.size / 2)
		run {
			var i = 0
			var ii = 0
			while (i < ids.size) {
				t.imageOptions[ii] = ids[i]
				t.imageOutlineOptions[ii] = ids[i + 1]
				i += 2
				ii++
			}
		}
		t.imageDims = Array(ids.size / 2) {
			GDimension(g.getImage(ids[it * 2]))
		}
	}

	override fun preDrawInit(g: DroidGraphics) {
		g.textHeight = resources.getDimension(R.dimen.board_view_text_size)
		g.setLineThicknessModePixels(false)
		//g.getPaint().setStrokeWidth(getResources().getDimension(R.dimen.default_line_width));
		super.preDrawInit(g)
	}

	fun initCharacter(g: DroidGraphics, pl: ZPlayerName, cardImageId: Int, charImageId: Int, outlineImageId: Int) {
		pl.imageId = charImageId
		pl.cardImageId = cardImageId
		pl.imageDim = GDimension(g.getImage(charImageId))
		pl.outlineImageId = outlineImageId
	}

	var tileIds = IntArray(0)
	private fun deleteTiles(g: DroidGraphics) {
		for (t in tileIds) {
			if (t > 0) g.deleteImage(t)
		}
		tileIds = IntArray(0)
	}

	override fun loadTiles(g: DroidGraphics, tiles: Array<ZTile>) {
		progress = 0
		numImages = tiles.size
		val renderer = renderer as UIZBoardRenderer<*>?
		deleteTiles(g)
		try {
			tileIds = IntArray(tiles.size)
			for (i in tiles.indices) {
				val id = g.loadImage("ztile_" + tiles[i].id + ".png")
				if (id < 0) throw Exception("Failed to load " + tiles[i].id)
				if (tiles[i].orientation == 0) {
					tileIds[i] = id
					continue
				}
				tileIds[i] = g.newRotatedImage(id, tiles[i].orientation)
				g.deleteImage(id)
			}
			renderer!!.onTilesLoaded(tileIds)
			renderer.onLoaded()
			redraw()
		} catch (e: Exception) {
			deleteTiles(g)
			e.printStackTrace()
		}
	}

	override fun loadAssets(g: DroidGraphics) {
		initCharacter(g, ZPlayerName.Baldric, R.drawable.zcard_baldric, R.drawable.zchar_baldric, R.drawable.zchar_baldric_outline)
		initCharacter(g, ZPlayerName.Benson, R.drawable.zcard_benson, R.drawable.zchar_benson, R.drawable.zchar_benson_outline)
		initCharacter(g, ZPlayerName.Jain, R.drawable.zcard_jain, R.drawable.zchar_jain, R.drawable.zchar_jain_outline)
		initCharacter(g, ZPlayerName.Tucker, R.drawable.zcard_tucker, R.drawable.zchar_tucker, R.drawable.zchar_tucker_outline)
		initCharacter(g, ZPlayerName.Silas, R.drawable.zcard_silas, R.drawable.zchar_silas, R.drawable.zchar_silas_outline)
		initCharacter(g, ZPlayerName.Samson, R.drawable.zcard_samson, R.drawable.zchar_samson, R.drawable.zchar_samson_outline)
		initCharacter(g, ZPlayerName.Nelly, R.drawable.zcard_nelly, R.drawable.zchar_nelly, R.drawable.zchar_nelly_outline)
		initCharacter(g, ZPlayerName.Ann, R.drawable.zcard_ann, R.drawable.zchar_ann, R.drawable.zchar_ann_outline)
		initCharacter(g, ZPlayerName.Clovis, R.drawable.zcard_clovis, R.drawable.zchar_clovis, R.drawable.zchar_clovis_outline)
		initCharacter(g, ZPlayerName.Karl, R.drawable.zcard_karl, R.drawable.zchar_karl, R.drawable.zchar_karl_outline)
		initCharacter(g, ZPlayerName.Ariane, R.drawable.zcard_ariane, R.drawable.zchar_ariane, R.drawable.zchar_ariane_outline)
		initCharacter(g, ZPlayerName.Morrigan, R.drawable.zcard_morrigan, R.drawable.zchar_morrigan, R.drawable.zchar_morrigan_outline)
		initCharacter(g, ZPlayerName.Theo, R.drawable.zcard_theo, R.drawable.zchar_theo, R.drawable.zchar_theo_outline)
		initZombieImages(g, ZZombieType.Walker,
			R.drawable.zwalker1, R.drawable.zwalker1_outline,
			R.drawable.zwalker2, R.drawable.zwalker2_outline,
			R.drawable.zwalker3, R.drawable.zwalker3_outline,
			R.drawable.zwalker4, R.drawable.zwalker4_outline,
			R.drawable.zwalker5, R.drawable.zwalker5_outline)
		initZombieImages(g, ZZombieType.Abomination, R.drawable.zabomination, R.drawable.zabomination_outline)
		initZombieImages(g, ZZombieType.BlueTwin, R.drawable.zbluetwin, R.drawable.zabomination_outline)
		initZombieImages(g, ZZombieType.GreenTwin, R.drawable.zgreentwin, R.drawable.zabomination_outline)
		initZombieImages(g, ZZombieType.Necromancer, R.drawable.znecro, R.drawable.znecro_outline)
		initZombieImages(g, ZZombieType.Runner, R.drawable.zrunner1, R.drawable.zrunner1_outline, R.drawable.zrunner2, R.drawable.zrunner2_outline)
		initZombieImages(g, ZZombieType.Fatty, R.drawable.zfatty1, R.drawable.zfatty1_outline, R.drawable.zfatty2, R.drawable.zfatty2_outline)
		initZombieImages(g, ZZombieType.Wolfz, R.drawable.zwulf1, R.drawable.zwulf1_outline, R.drawable.zwulf2, R.drawable.zwulf2_outline)
		initZombieImages(g, ZZombieType.Wolfbomination, R.drawable.zwolfabom, R.drawable.zwolfabom_outline)
		initZombieImages(g, ZZombieType.Wolfz, R.drawable.zwulf1, R.drawable.zwulf1_outline, R.drawable.zwulf2, R.drawable.zwulf2_outline)

		// Fire images are extracted from larger main image
		val cells = arrayOf(intArrayOf(0, 0, 56, 84), intArrayOf(56, 0, 131 - 56, 84), intArrayOf(131, 0, 196 - 131, 84), intArrayOf(0, 84, 60, 152 - 84), intArrayOf(60, 84, 122 - 60, 152 - 84), intArrayOf(122, 84, 196 - 122, 152 - 84))
		numImages = ZIcon.values().size
		ZIcon.FIRE.imageIds = g.loadImageCells(R.drawable.zfire_icons, cells)
		publishProgress(++progress)
		ZIcon.CLAWS.imageIds = intArrayOf(
			R.drawable.zclaws1_icon,
			R.drawable.zclaws2_icon,
			R.drawable.zclaws3_icon,
			R.drawable.zclaws4_icon,
			R.drawable.zclaws5_icon,
			R.drawable.zclaws6_icon
		)
		publishProgress(++progress)
		ZIcon.SLASH.imageIds = intArrayOf(
			R.drawable.zslash1,
			R.drawable.zslash2,
			R.drawable.zslash3,
			R.drawable.zslash4,
			R.drawable.zslash5
		)
		publishProgress(++progress)

		// Icons that spin around
		for (pair in arrayOf(
			Pair(ZIcon.DRAGON_BILE, R.drawable.zdragonbile_icon),
			Pair(ZIcon.TORCH, R.drawable.ztorch_icon),
			Pair(ZIcon.DAGGER, R.drawable.zdagger_icon),
			Pair(ZIcon.SWORD, R.drawable.zsword_icon))) {
			pair.first.imageIds = IntArray(8)
			val ids = pair.first.imageIds
			ids[0] = pair.second
			for (i in 1 until ids.size) {
				val deg = 45 * i
				ids[i] = g.newRotatedImage(ids[0], deg)
			}
			publishProgress(++progress)
		}

		// Icons that only have a single image
		for (pair in arrayOf(
			Pair(ZIcon.SHIELD, R.drawable.zshield_icon),
			Pair(ZIcon.SLIME, R.drawable.zslime_icon),
			Pair(ZIcon.FIREBALL, R.drawable.zfireball),
			Pair(ZIcon.GRAVESTONE, R.drawable.zgravestone),
			Pair(ZIcon.PADLOCK, R.drawable.zpadlock3),
			Pair(ZIcon.SKULL, R.drawable.zskull)
		)) {
			pair.first.imageIds = intArrayOf(pair.second)
			publishProgress(++progress)
		}

		// Directional, like projectiles
		for (pair in arrayOf(
			Pair(ZIcon.ARROW, R.drawable.zarrow_icon)
		)) {
			pair.first.imageIds = IntArray(4)
			val ids = pair.first.imageIds
			ids[ZDir.NORTH.ordinal] = g.newRotatedImage(pair.second, 270)
			ids[ZDir.SOUTH.ordinal] = g.newRotatedImage(pair.second, 90)
			ids[ZDir.EAST.ordinal] = pair.second
			ids[ZDir.WEST.ordinal] = g.newRotatedImage(pair.second, 180)
			publishProgress(++progress)
		}
		for (pair in arrayOf(
			Pair(ZIcon.SPAWN_RED, R.drawable.zspawn_red),
			Pair(ZIcon.SPAWN_BLUE, R.drawable.zspawn_blue),
			Pair(ZIcon.SPAWN_GREEN, R.drawable.zspawn_green))) {
			pair.first.imageIds = IntArray(4)
			val ids = pair.first.imageIds
			ids[ZDir.NORTH.ordinal] = pair.second
			ids[ZDir.SOUTH.ordinal] = pair.second
			ids[ZDir.WEST.ordinal] = g.newRotatedImage(pair.second, 270)
			ids[ZDir.EAST.ordinal] = g.newRotatedImage(pair.second, 90)
			publishProgress(++progress)
		}
		Log.i("IMAGES", "Loaded total of $progress")
		publishProgress(numImages)
	}

	fun publishProgress(p: Int) {
		progress = p
		postInvalidate()
		Thread.sleep(100)
	}

	override fun getProgress(): Float {
		return progress.toFloat() / numImages
	}

	override fun onLoading() {
		renderer.onLoading()
	}

	override fun onLoaded() {
		renderer.onLoaded()
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
	}
}