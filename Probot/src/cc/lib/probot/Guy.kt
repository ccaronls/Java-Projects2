package cc.lib.probot

import cc.lib.game.GColor
import cc.lib.utils.Reflector

class Guy : Reflector<Guy> {
	companion object {
		init {
			addAllFields(Guy::class.java)
		}
	}

	var posx = 0
	var posy = 2
	var dir = Direction.Right
	var color = GColor.RED

	constructor() {}
	constructor(posx: Int, posy: Int, dir: Direction, color: GColor) {
		this.posx = posx
		this.posy = posy
		this.dir = dir
		this.color = color
	}
}