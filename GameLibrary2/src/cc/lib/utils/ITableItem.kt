package cc.lib.utils

import cc.lib.game.AGraphics
import cc.lib.game.IDimension
import cc.lib.game.IMeasurable

interface ITableItem : IMeasurable {
	fun draw(g: AGraphics): IDimension
	val borderWidth: Int
}
