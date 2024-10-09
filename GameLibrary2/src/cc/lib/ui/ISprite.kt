package cc.lib.ui

import cc.lib.game.AGraphics

/**
 * Created by Chris Caron on 6/30/23.
 */
interface ISprite {

	fun draw(g: AGraphics)

	fun zOrder(): Int

}