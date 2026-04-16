package cc.lib.game

import kotlin.math.roundToInt

/**
 * Created by Chris Caron on 9/23/25.
 *
 * When an image represents frames of an animated sprite, we can use this class.
 * Frames are assumed to be from left to right repeating
 */
abstract class AnimatedImage(
	val id: Int,
	tileDimen: GDimension,
	durationMSecs: Long,
	repeats: Int = -1,
	oscillateOnRepeat: Boolean = false
) : GAnimation(durationMSecs, repeats, oscillateOnRepeat) {

	private var tilesCountX = 0
	private var tilesCountY = 0
	private var tile = -1
	private val tileW = tileDimen.width.toInt()
	private val tileH = tileDimen.height.toInt()
	private var tileX = 0
	private var tileY = 0

	protected var yPos = 0

	override fun onStarted(g: AGraphics, reversed: Boolean) {
		if (tile < 0) {
			val img = g.getImage(id)
			tilesCountX = (img.width / tileW).roundToInt().coerceAtLeast(1)
			tilesCountY = (img.height / tileH).roundToInt().coerceAtLeast(1)
			tile = g.newSubImage(id, 0, 0, tileW, tileH)
		}
	}

	override fun draw(g: AGraphics, position: Float, dt: Float) {
		val idx = (position * (tilesCountX - 1)).roundToInt()
		val newTileX = idx * tileW
		val newTileY = yPos * tileH
		if (newTileX != tileX || newTileY != tileY) {
			tileX = newTileX
			tileY = newTileY
			g.moveSubImage(tile, id, tileX, tileY, tileW, tileH)
		}
		g.drawImage(tile)
	}
}