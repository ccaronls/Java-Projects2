package cc.lib.utils

import cc.lib.game.AGraphics
import cc.lib.game.GDimension
import cc.lib.game.IDimension
import cc.lib.game.IMeasurable
import cc.lib.math.Vector2D

interface ITableItem : IMeasurable {
	fun draw(g: AGraphics): IDimension
	val borderWidth: Int
}

class TableImage(val id: Int, val maxWidth: Int = Int.MAX_VALUE) : ITableItem {
	override fun measure(g: AGraphics): IDimension {
		val img = g.getImage(id)
		if (img.width > maxWidth) {
			return GDimension(maxWidth, 1).withAspect(img.aspect)
		}
		return g.getImage(id)
	}

	override fun draw(g: AGraphics): IDimension {
		val dim = measure(g)
		g.drawImage(id, dim)
		return dim
	}

	override val borderWidth: Int
		get() = 0
}

