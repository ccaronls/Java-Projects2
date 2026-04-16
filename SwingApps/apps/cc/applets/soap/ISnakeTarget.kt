package cc.applets.soap

import java.awt.Graphics

interface ISnakeTarget {
	fun move()
	fun draw(g: Graphics?)
	fun canEat(): Boolean
}
