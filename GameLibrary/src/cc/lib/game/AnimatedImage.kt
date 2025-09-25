package cc.lib.game

import kotlin.math.roundToInt

/**
 * Created by Chris Caron on 9/23/25.
 *
 * When an image represents frames of an animated sprite, we can use this class
 */
abstract class AnimatedImage(
	val id: Int,
	val dimen: GDimension,
	durationMSecs: Long,
	repeats: Int = -1,
	oscillateOnRepeat: Boolean = false
) : GAnimation(durationMSecs, repeats, oscillateOnRepeat) {

	private var tilesX = 0
	private var tilesY = 0
	private var tile = -1

	protected var yPos = 0

	override fun onStarted(g: AGraphics, revered: Boolean) {
		if (tile < 0) {
			val img = g.getImage(id)
			tilesX = (img.width / dimen.width).roundToInt().coerceAtLeast(1)
			tilesY = (img.height / dimen.height).roundToInt().coerceAtLeast(1)
			tile = g.newSubImage(id, 0, 0, dimen.width.toInt(), dimen.height.toInt())
		}
	}

	override fun draw(g: AGraphics, position: Float, dt: Float) {
		val idx = (position * (tilesX - 1)).roundToInt()
		g.moveSubImage(tile, id, idx * dimen.width.toInt(), yPos * dimen.height.toInt(), dimen.width.toInt(), dimen.height.toInt())
		g.drawImage(tile)
	}
}